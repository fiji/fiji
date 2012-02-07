package fiji;

/**
 * A helper to use Javassist effectively
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import java.util.jar.JarOutputStream;

import java.util.zip.ZipEntry;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.InstructionPrinter;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

import javassist.expr.MethodCall;

public abstract class JavassistHelper implements Runnable {

	protected Set<String> classNames = new HashSet<String>();
	protected static LinkedHashMap<String, CtClass> definedClasses = new LinkedHashMap<String, CtClass>();
	protected static ClassPool pool;
	protected static boolean frozen;

	static {
		pool = ClassPool.getDefault();
		pool.appendClassPath(new ClassClassPath(JavassistHelper.class));
	}

	protected CtClass get(String className) throws NotFoundException {
		if (!definedClasses.containsKey(className)) {
			definedClasses.put(className, pool.get(className));
			classNames.add(className);
		}
		return definedClasses.get(className);
	}

	protected void add(CtClass clazz) {
		classNames.add(clazz.getName());
		definedClasses.put(clazz.getName(), clazz);
	}

	public static void defineClasses() throws CannotCompileException {
		if (frozen) {
			new Exception("Attempted to defined patched classes again").printStackTrace();
			return;
		}
		for (String name : definedClasses.keySet())
			definedClasses.get(name).toClass();
		frozen = true;
	}

	final public void run() {
		if (frozen) {
			System.err.println("Attempted to patch classes again: " + getClass().getName());
			return;
		}
		try {
			instrumentClasses();
		} catch (BadBytecode e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (CannotCompileException e) {
			System.err.println(e.getMessage() + "\n" + e.getReason());
			e.printStackTrace();
			Throwable cause = e.getCause();
			if (cause != null)
				cause.printStackTrace();
		}
	}

	public abstract void instrumentClasses() throws BadBytecode, CannotCompileException, NotFoundException;

	protected String getLatestArg(MethodCall call, int skip) throws BadBytecode, NotFoundException {
		int[] indices = new int[skip + 1];
		int counter = 0;

		MethodInfo info = ((CtMethod)call.where()).getMethodInfo();
		CodeIterator iterator = info.getCodeAttribute().iterator();
		int currentPos = call.indexOfBytecode();
		while (iterator.hasNext()) {
			int pos = iterator.next();
			if (pos >= currentPos)
				break;
			switch (iterator.byteAt(pos)) {
			case Opcode.LDC:
				indices[(counter++) % indices.length] = iterator.byteAt(pos + 1);
				break;
			case Opcode.LDC_W:
				indices[(counter++) % indices.length] = iterator.u16bitAt(pos + 1);
				break;
			}
		}
		if (counter < skip)
			return null;
		return info.getConstPool().getStringInfo(indices[(indices.length + counter - skip) % indices.length]);
	}

	protected boolean hasClass(String name) {
		try {
			return pool.get(name) != null;
		} catch (NotFoundException e) {
			return false;
		}
	}

	protected boolean hasField(CtClass clazz, String name) {
		try {
			return clazz.getField(name) != null;
		} catch (NotFoundException e) {
			return false;
		}
	}

	protected boolean hasMethod(CtClass clazz, String name, String signature) {
		try {
			return clazz.getMethod(name, signature) != null;
		} catch (NotFoundException e) {
			return false;
		}
	}

	protected static String stripPackage(String className) {
		int lastDot = -1;
		for (int i = 0; ; i++) {
			if (i >= className.length())
				return className.substring(lastDot + 1);
			char c = className.charAt(i);
			if (c == '.' || c == '$')
				lastDot = i;
			else if (c >= 'A' && c <= 'Z')
				; // continue
			else if (c >= 'a' && c <= 'z')
				; // continue
			else if (i > lastDot + 1 && c >= '0' && c <= '9')
				; // continue
			else
				return className.substring(lastDot + 1);
		}
	}

	public static void verify(CtClass clazz, PrintStream output) {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(stream);
			clazz.getClassFile().write(out);
			out.flush();
			out.close();
			verify(stream.toByteArray(), output);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void verify(byte[] bytecode, PrintStream out) {
		try {
			ClassLoader loader = new FijiClassLoader(true);
			Class readerClass = loader.loadClass("jruby.objectweb.asm.ClassReader");
			java.lang.reflect.Constructor ctor = readerClass.getConstructor(new Class[] { bytecode.getClass() });
			Object reader = ctor.newInstance(bytecode);
			Class checkerClass = loader.loadClass("jruby.objectweb.asm.util.CheckClassAdapter");
			java.lang.reflect.Method verify = checkerClass.getMethod("verify", new Class[] { readerClass, Boolean.TYPE, PrintWriter.class });
			verify.invoke(null, new Object[] { reader, false, new PrintWriter(out) });
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void verify(PrintStream out) {
		for (String name : classNames) {
			out.println("Verifying class " + name);
			verify(definedClasses.get(name), out);
		}
	}

	public static void disassemble(CtClass clazz, PrintStream out) {
		disassemble(clazz, out, false);
	}

	public static void disassemble(CtClass clazz, PrintStream out, boolean evenSuperclassMethods) {
		out.println("Class " + clazz.getName());
		for (CtConstructor ctor : clazz.getConstructors()) try {
			disassemble(ctor.toMethod(ctor.getName(), clazz), out);
		} catch (CannotCompileException e) {
			e.printStackTrace(out);
		}
		for (CtMethod method : clazz.getDeclaredMethods())
			if (evenSuperclassMethods || method.getDeclaringClass().equals(clazz))
				disassemble(method, out);
	}

	public static void disassemble(CtMethod method, PrintStream out) {
		out.println(method.getLongName());
		new InstructionPrinter(out).print(method);
		out.println("");
	}

	public void writeJar(File path) throws IOException {
		JarOutputStream jar = new JarOutputStream(new FileOutputStream(path));
		DataOutputStream dataOut = new DataOutputStream(jar);
		for (String name : classNames) {
			CtClass clazz = definedClasses.get(name);
			ZipEntry entry = new ZipEntry(clazz.getName().replace('.', '/') + ".class");
			jar.putNextEntry(entry);
			clazz.getClassFile().write(dataOut);
			dataOut.flush();
		}
		jar.close();
	}
}