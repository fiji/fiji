package fiji;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.Translator;

public class MemoryProfiler implements Translator {
	protected static final boolean debug = false;
	protected Set<String> only;

	public MemoryProfiler() {
		this(System.getenv("MEMORY_PROFILE_ONLY"));
	}

	public MemoryProfiler(String only) {
		this(only == null ? null : Arrays.asList(only.split(" +")));
	}

	public MemoryProfiler(Collection<String> only) {
		if (only != null) {
			this.only = new HashSet<String>();
			this.only.addAll(only);
		}
	}

	public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
	}

	public void onLoad(ClassPool pool, String classname) throws NotFoundException {
		// do not instrument yourself
		if (classname.equals(getClass().getName()))
			return;

		if (only != null && !only.contains(classname))
			return;

		if (debug)
			System.err.println("instrumenting " + classname);

		CtClass cc = pool.get(classname);
		if (cc.isFrozen())
			return;

		try {
			// instrument all methods and constructors
			for (CtMethod method : cc.getMethods())
				handle(method);
			for (CtConstructor constructor : cc.getConstructors())
				handle(constructor);
		}
		catch (RuntimeException e) {
			if (!e.getMessage().endsWith(" class is frozen"))
				e.printStackTrace();
		}
	}

	protected void handle(CtBehavior behavior) {
		try {
			if (debug)
				System.err.println("instrumenting " + behavior.getClass().getName() + "." + behavior.getName());
			if (behavior.isEmpty())
				return;
			behavior.addLocalVariable("memoryBefore", CtClass.longType);
			behavior.insertBefore("memoryBefore = fiji.MemoryProfiler.get();");
			behavior.insertAfter("fiji.MemoryProfiler.report(memoryBefore);");
		}
		catch (CannotCompileException e) {
			if (!e.getMessage().equals("no method body"))
				e.printStackTrace();
		}
	}

	protected static Runtime runtime = Runtime.getRuntime();

	public static long get() {
		gc();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	public static void report(long memoryBefore) {
		gc();
		StackTraceElement[] trace = new Exception().getStackTrace();
		StackTraceElement last = trace.length > 1 ? trace[1] : new StackTraceElement("null", "null", "null", -1);
		long current = get();
		System.err.println("MemoryProfiler: " + (current - memoryBefore) + " " + current + " " + last.getClassName() + "." + last.getMethodName() + "(" + last.getFileName() + ":" + last.getLineNumber() + ")");
	}

	public static void gc() {
		System.gc();
		System.gc();
	}

	public static void main(String[] args) throws Throwable {
		Thread.currentThread().setContextClassLoader(MemoryProfiler.class.getClassLoader());

		if (args.length == 0) {
			System.err.println("Usage: java " + MemoryProfiler.class + " <main-class> [<argument>...]");
			System.exit(1);
		}

		String mainClass = args[0];
		String[] mainArgs = new String[args.length - 1];
		System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);

		Loader loader = new Loader();
		loader.addTranslator(ClassPool.getDefault(), new MemoryProfiler());
		gc();
		loader.run(mainClass, mainArgs);
	}
}
