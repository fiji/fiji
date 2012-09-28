package fiji.scripting;

import java.util.Map;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.Queue;
import java.math.BigDecimal;
import java.math.BigInteger;
import fiji.scripting.java.Refresh_Javas;
import fiji.scripting.Script_Editor;
import fiji.scripting.TextEditor;
import fiji.FijiClassLoader;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Modifier;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.regex.Pattern;
import ij.IJ;


/* An utility class to inline java code inside any script of any language.
 * The code, passed on as a String, is placed into a static method without
 * arguments and with a default Object return value. The bindings (read: objects passed to itfrom the scripting language runtime, in a map), are placed
 * into static final private fields of the newly generated class.
 * Then the class is compiled. No reflection is used anywhere.
 * 
 * An example in jython:

from fiji.scripting import Weaver

nums = [1.0, 2.0, 3.0, 4.0]

w = Weaver.inline(
	"""
	double sum = 0;
	for (Double d : (java.util.List<Double>)nums) {
		sum += d;
	}
	return sum;
	""",
	{"nums" : nums},
	Double)

print w.call()
 *
 *
 * It is safe to invoke the call() method numerous times. The bindings
 * will be read as given. If you want to change the content of the bindings,
 * make them arrays or collections, and retrieve (and cast) their contents
 * within the inlined java code.
 *
 * The return type is optional.
 *
 * The java-embedding approach is intended for short snippets of code,
 * but any code suitable for the code block of a static method is allowed.
 *
 */
public class Weaver {

	static private final AtomicInteger K = new AtomicInteger(0);
	static private final Map<String,Map<String,Object>> bindings = new HashMap<String,Map<String,Object>>();

	static public final Object steal(final String className, final String binding) {
		synchronized (bindings) {
			final Map<String,Object> m = bindings.get(className);
			if (null == m) {
				System.out.println("No binding '" + binding + "' for class '" + className + "'");
				return null;
			}
			final Object b = m.remove(binding);
			if (m.isEmpty()) bindings.remove(className);
			return b;
		}
	}

	static private final void put(final String className, final String binding, final Object ob) {
		if (null == binding) return;
		synchronized (bindings) {
			Map<String,Object> m = bindings.get(className);
			if (null == m) {
				m = new HashMap<String,Object>();
				bindings.put(className, m);
			}
			m.put(binding, ob);
		}
	}

	static public final <T extends Object> Callable<T> inline(final String code, final Map<String,?> bindings) throws Throwable
	{
		return inline(code, bindings, null, false);
	}
	static public final <T extends Object> Callable<T> inline(final String code, final Map<String,?> bindings,
			final boolean showJavaCode) throws Throwable
	{
		return inline(code, bindings, null, showJavaCode);
	}
	static public final <T extends Object> Callable<T> inline(final String code, final Map<String,?> bindings,
			final Class<T> returnType) throws Throwable
	{
		return inline(code, bindings, returnType, false);
	}
	static public final <T extends Object> Callable<T> inline(final String code, final Map<String,?> bindings,
			final Class<T> returnType, final boolean showJavaCode) throws Throwable
	{
		// Buffer to store the contents of the java file
		final StringBuilder sb = new StringBuilder(4096);
		// Acquire a unique number to generate a unique class name
		final int k = K.incrementAndGet();
		final String className = "weave.gen" + k;
		// Parse the return type, and ensure it's not null
		final Class<?> rt = null == returnType ? Object.class : returnType;
		// 1. Header of the java file
		sb.append("package weave;\n")
		  .append("import java.util.concurrent.Callable;\n")
		  .append("public final class gen").append(k)
		  .append(" implements Callable<").append(rt.getName())
		  .append(">{\n");
		// 2. Setup fields to represent the bindings
		for (final Map.Entry<String,?> e : bindings.entrySet()) {
			final String name = e.getKey();
			Type t = guessPublicClass(e.getValue());
			sb.append("static private final ")
			  .append(t.type).append(' ')
			  .append(name).append(" = (").append(t.cast)
			  .append(") fiji.scripting.Weaver.steal(\"")
			  .append(className).append("\" ,\"")
			  .append(name).append("\");\n");
			Weaver.put(className, name, e.getValue());

			System.out.println("binding is: " + (e.getValue() == null ? null : e.getValue().getClass()));
		}
		// 3. Method that runs the desired code
		sb.append("public final ").append(rt.getName())
		  .append(" call() { ").append(code).append("\n}}");
		// 4. Save the file to a temporary directory
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		final File f = new File(tmpDir, "/weave/gen" + k + ".java");
		if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
			throw new Exception("Could not create directories for " + f);
		}
		if (showJavaCode) {
			// Not from the not-yet-saved file (that file "doesn't exist" ever)
			TextEditor ted = Script_Editor.getInstance();
			if (null == ted) ted = new TextEditor("gen" + k + ".java", sb.toString());
			else ted.newTab(sb.toString(), ".java");
		}
		Writer writer = new FileWriter(f);
		writer.write(sb.toString());
		writer.close();
		// 5. Compile the file, removing first any class file names that match
		final Pattern pclass = Pattern.compile("^gen" + k + ".*class$");
		for (File fc : f.getParentFile().listFiles()) {
			if (pclass.matcher(fc.getName()).matches()) {
				if (!fc.delete()) {
					System.out.println("Failed to delete file " + f.getAbsolutePath());
				}
			}
		}
		try {
			Refresh_Javas compiler = new Refresh_Javas();
			OutputStream out = new IJLogOutputStream();
			compiler.setOutputStreams(out, out);
			compiler.compile(f.getAbsolutePath(), null);
		} finally {
			// 6. Set the temporary files for deletion when the JVM exits
			// The .java file
/*
			f.deleteOnExit();
			// The .class files, if any
			for (File fc : f.getParentFile().listFiles()) {
				if (pclass.matcher(fc.getName()).matches()) {
					fc.deleteOnExit();
				}
			}
*/
		}
		// 7. Load the class file
		URLClassLoader loader = new URLClassLoader(new URL[]{tmpDir.toURI().toURL()}, IJ.getClassLoader());

		try {
			return (Callable<T>) loader.loadClass(className).newInstance();
		} catch (Throwable t) {
			IJ.handleException(t);
			throw t;
		}
	}

	static private class Type {
		final String type, cast;
		private Type(String type, String cast) {
			this.type = type;
			this.cast = cast;
		}
		private Type(String type) {
			this(type, type);
		}
	}

	/** Return the most specific yet public Class of the class hierarchy of {@param ob}. */
	static private final Type guessPublicClass(final Object ob) {
		if (null == ob) return new Type("Object");
		Class<?> c = ob.getClass();
		// Avoid boxing/unboxing: is done only once
		if (Long.class == c) return new Type("long", "Long");
		if (Double.class == c) return new Type("double", "Double");
		if (Float.class == c) return new Type("float", "Float");
		if (Byte.class == c) return new Type("byte", "Byte");
		if (Short.class == c) return new Type("short", "Short");
		if (Integer.class == c) return new Type("int", "Integer");
		if (Character.class == c) return new Type("char", "Character");

		// While not named class and not public, inspect its super class:
		while (c.isAnonymousClass() || 0 == (c.getModifiers() | Modifier.PUBLIC)) {
			c = c.getSuperclass();
		}

		// If it's an array, inspect if it's native, or fix the name
		if (c.isArray()) {
			String s = c.getSimpleName();
			// native array?
			if (s.toLowerCase().equals(s)) {
				Pattern pat = Pattern.compile("^(\\[\\])+$");
				for (String name : new String[]{"byte", "char", "short", "int", "long", "float", "double"}) {
					if (s.startsWith(name) && pat.matcher(s.substring(name.length())).matches()) {
						return new Type(s);
					}
				}
			}
			// Not native: transform "[[Ljava.util.List;" into "java.util.List[][]"
			String name = c.getName();
			int nBrackets = name.indexOf('L');
			StringBuilder sb = new StringBuilder(32);
			sb.append(name, nBrackets+1, name.length() -1); // +1 to skip 'L'
			for (int i=0; i<nBrackets; i++) sb.append('[').append(']');
			return new Type(sb.toString());
		}
		return new Type(c.getName());
	}

	protected static class IJLogOutputStream extends OutputStream {
		public byte[] buffer = new byte[16384];
		public int len;

		protected synchronized void ensure(int length) {
			if (buffer.length >= length)
				return;

			int newLength = buffer.length * 3 / 2;
			if (newLength < length)
				newLength = length + 16;
			byte[] newBuffer = new byte[newLength];
			System.arraycopy(buffer, 0, newBuffer, 0, len);
			buffer = newBuffer;
		}

		public synchronized void write(int b) {
			ensure(len + 1);
			buffer[len++] = (byte)b;
			if (b == '\n')
				flush();
		}

		public synchronized void write(byte[] buffer) {
			write(buffer, 0, buffer.length);
		}

		public synchronized void write(byte[] buffer, int offset, int length) {
			int eol = length;
			while (eol > 0)
				if (buffer[eol - 1] == '\n')
					break;
				else
					eol--;
			if (eol >= 0) {
				ensure(len + eol);
				System.arraycopy(buffer, offset, this.buffer, len, eol);
				len += eol;
				flush();
				length -= eol;
				if (length == 0)
					return;
				offset += eol;
			}
			ensure(len + length);
			System.arraycopy(buffer, offset, this.buffer, len, length);
			len += length;
		}

		public void close() {
			flush();
		}

		public synchronized void flush() {
			if (len > 0) {
				if (buffer[len - 1] == '\n')
					len--;
				IJ.log(new String(buffer, 0, len));
			}
			len = 0;
		}
	}
}
