package fiji.python;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.math.BigDecimal;
import java.math.BigInteger;
import fiji.scripting.java.Refresh_Javas;
import fiji.FijiClassLoader;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Method;
import java.io.OutputStreamWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.regex.Pattern;
import ij.IJ;


/* An utility class to inline java code inside any script of any language.
 * The code, passed on as a String, is placed into a static method without
 * arguments and with a default Object return value. The bindings (read: objects passed to itfrom the scripting language runtime, in a map), are placed
 * into static final private fields of the newly generated class.
 * Then the class is compiled. No reflection is used anywhere.
 * 
 * An example in jython:

from fiji.python import Weave

nums = [1.0, 2.0, 3.0, 4.0]

w = Weave(
	"""
	double sum = 0;
	for (Double d : (java.util.List<Double>)nums) {
		sum += d;
	}
	return sum;
	""",
	{"nums" : nums})

print w.call()
 *
 *
 * It is safe to invoke the call() method numerous times. The bindings
 * will be read as given. If you want to change the content of the bindings,
 * make them arrays or collections, and retrieve (and cast) their contents
 * within the inlined java code.
 *
 * The java-embedding approach is intended for short snippets of code,
 * but any code suitable for the code block of a static method is allowed.
 *
 */
public class Weave {

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

	static public final <T extends Object> Callable<T> inline(final String code, final Map<String,?> bindings) throws Exception
	{
		return inline(code, bindings, null);
	}
	static public final <T extends Object> Callable<T> inline(final String code, final Map<String,?> bindings, final Class<T> returnType) throws Exception
	{
		final StringBuilder sb = new StringBuilder(4096);
		final int k = K.incrementAndGet();
		final String className = "weave.gen" + k;
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
			String guessedClass = guessClass(e.getValue());
			sb.append("static private final ")
			  .append(guessedClass).append(' ')
			  .append(name).append(" = (").append(guessedClass)
			  .append(") fiji.python.Weave.steal(\"")
			  .append(className).append("\" ,\"")
			  .append(name).append("\");\n");
			Weave.put(className, name, e.getValue());

			System.out.println("binding is: " + (e.getValue() == null ? null : e.getValue().getClass()));
		}
		// 3. Method that runs the desired code
		sb.append("public final ").append(rt.getName())
		  .append(" call() { ").append(code).append("\n}}");
		// 4. Save the file to a temporary directory
		String tmpDir = System.getProperty("java.io.tmpdir").replace('\\', '/');
		if (!tmpDir.endsWith("/")) tmpDir += "/";
		final File f = new File(tmpDir + "/weave/gen" + k + ".java");
		if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
			throw new Exception("Could not create directories for " + f);
		}
		OutputStreamWriter dos = null;
		try {
			// Encode in "Latin 1"
			dos = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f), sb.length()), "8859_1");
			dos.write(sb.toString(), 0, sb.length());
			dos.flush();
		} finally {
			if (null != dos) dos.close();
		}
		// 5. Compile the file, removing first any class file names that match
		final Pattern pclass = Pattern.compile("^gen" + k + ".*class$");
		for (File fc : f.getParentFile().listFiles()) {
			if (pclass.matcher(fc.getName()).matches()) {
				if (!fc.delete()) {
					System.out.println("Failed to delete file " + f.getAbsolutePath());
				}
			}
		}
		new Refresh_Javas().compile(f.getAbsolutePath(), null);

		// 6. Set the temporary files for deletion when the JVM exits
		// The .java file
		f.deleteOnExit();
		// The .class files
		for (File fc : f.getParentFile().listFiles()) {
			if (pclass.matcher(fc.getName()).matches()) {
				fc.deleteOnExit();
			}
		}
		// 7. Load the class file
		URLClassLoader loader = new URLClassLoader(new URL[]{new URL("file://" + tmpDir)}, IJ.getClassLoader());

		return (Callable<T>) loader.loadClass(className).newInstance();
	}

	static private final String guessClass(final Object ob) {
		if (null == ob) return "Object";
		if (ob instanceof List) return "java.util.List";
		if (ob instanceof Set) return "java.util.Set";
		if (ob instanceof Map) return "java.util.Map";
		if (ob instanceof String) return "String";
		if (ob instanceof Number) {
			if (ob instanceof Double) return "double";
			if (ob instanceof Float) return "float";
			if (ob instanceof Long) return "long";
			if (ob instanceof Integer) return "int";
			if (ob instanceof Short) return "short";
			if (ob instanceof Byte) return "byte";
			if (ob instanceof Character) return "char";
			if (ob instanceof BigDecimal) return "java.math.BigDecimal";
			if (ob instanceof BigInteger) return "java.math.BigInteger";
			return "Number";
		}
		if (ob instanceof double[]) return "double[]";
		if (ob instanceof float[]) return "float[]";
		if (ob instanceof long[]) return "long[]";
		if (ob instanceof int[]) return "int[]";
		if (ob instanceof short[]) return "short[]";
		if (ob instanceof byte[]) return "byte[]";
		if (ob instanceof char[]) return "char[]";
		if (ob instanceof Object[]) return "Object[]";
		return "Object";
	}
}
