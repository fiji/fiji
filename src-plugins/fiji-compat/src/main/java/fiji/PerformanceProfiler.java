/*
 * #%L
 * A light-weight, javassist-backed performance profiler.
 * %%
 * Copyright (C) 2013 Johannes Schindelin.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package fiji;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Loader;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.Translator;

/**
 * A Javassist-backed performance profiler.
 * 
 * <p>
 * This class implements an easy-to-use, relatively light-weight performance
 * profiler that does not need operating system support or special start-up
 * parameters to the Java Virtual Machine.
 * </p>
 * 
 * Use it in one of the following ways:
 * <ul>
 * <li>
 * <p>
 * insert the following line at the start of the main() method calling the code
 * to profile:<br />
 * <code>if (PerformanceProfiler.startProfiling(args)) return;</code><br />
 * Subsequently, you can start/stop profiling by calling
 * {@link #setActive(boolean)}, print a report using
 * {@link #report(PrintStream)}, or print a report to a file with
 * {@link #report(File, int)}.
 * </p>
 * <p>
 * Printing a report will reset the counters and stop profiling. If you want to
 * make sure the counters are reset and profiling is stopped, but not to print
 * anything, just call <code>PerformanceProfiler.report(null);</code>.
 * </p>
 * <li>Call PerformanceProfiler as main class, passing as parameter the name of
 * the main class to profile and optionally any parameters you want to pass to
 * that main class.
 * </ul>
 * 
 * <p>
 * The idea behind the profiler is that a Javassist-specific class
 * {@link Loader} with a {@link Translator} is used to load the main class and
 * all its dependent classes. The translator instruments every method such that
 * a method-specific counter is incremented, and the total time spent in the
 * method is recorded, too. A list of instrumented methods with their counters
 * is maintained globally.
 * </p>
 * 
 * <p>
 * To record the time spent in a method, either the {@link #getNanos()} method
 * (thread-specific, but unfortunately very, very slow) or the
 * {@link #getNanosQnD()} (not thread-specific, but does not dominate even small
 * methods' timing) is used. The latter is the default.
 * </p>
 * 
 * @author Johannes Schindelin
 */
public class PerformanceProfiler implements Translator {
	private Set<String> only, skip;
	private boolean fastButInaccurateTiming = true;

	protected static final boolean debug = false;
	private static Loader loader;
	private static Field activeField;
	private static Map<CtBehavior, Integer> counters;
	protected static Method realReport;
	private static ThreadMXBean bean;

	/**
	 * The constructor.
	 * 
	 * It will look for a white-space-delimited list of classes to profile in
	 * the environment variable PERFORMANCE_PROFILE_ONLY.
	 */
	public PerformanceProfiler() {
		this(System.getenv("PERFORMANCE_PROFILE_ONLY"));
	}

	private PerformanceProfiler(String only) {
		this(only == null ? null : Arrays.asList(only.split(" +")));
	}

	private PerformanceProfiler(Collection<String> only) {
		if (only != null) {
			this.only = new HashSet<String>();
			this.only.addAll(only);
		}
		skip = new HashSet<String>();
	}

	// public methods

	/**
	 * Convenience method to call in your main(String[]) method.
	 * 
	 * Use it like this:
	 * 
	 * {@code if (PerformanceProfiler.startProfiling(null, args)) return; }
	 * 
	 * It will start the profiling class loader, run the main method of the
	 * calling class (which will call startProfiling() again, but that will now
	 * return false since it is already profiling) and return true.
	 * 
	 * @param mainClass the main class, or null to refer to the caller's class
	 * @param args the arguments for the main method
	 * @return true if profiling was started, false if profiling is already active
	 * @throws Throwable
	 */
	public static boolean startProfiling(String mainClass, final String... args) throws Throwable {
		if (PerformanceProfiler.class.getClassLoader() == loader) return false;
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		if (mainClass == null) mainClass = stack[2].getClassName();
		doMain(mainClass, args);
		return true;
	}

	/**
	 * The main method.
	 * 
	 * Use the class as a main class to start profiling any other main class
	 * contained in the class path.
	 * 
	 * @param args the main class to profile, followed by the arguments to pass to the main method
	 * @throws Throwable
	 */
	public static void main(final String... args) throws Throwable {
		Thread.currentThread().setContextClassLoader(PerformanceProfiler.class.getClassLoader());

		if (args.length == 0) {
			System.err.println("Usage: java " + PerformanceProfiler.class + " <main-class> [<argument>...]");
			System.exit(1);
		}

		String mainClass = args[0];
		String[] mainArgs = new String[args.length - 1];
		System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);

		doMain(mainClass, mainArgs);
	}

	/**
	 * Start or stop profiling
	 * 
	 * This method initializes the profiling class loader if necessary.
	 * 
	 * @param active
	 */
	public static void setActive(boolean active) {
		if (loader == null) init();
		try {
			activeField.setBoolean(null, active);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reports whether profiling is in progress
	 * 
	 * @return whether we're profilin'
	 */
	public static boolean isActive() {
		try {
			return activeField.getBoolean(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// reporting

	/**
	 * Writes a profile report to a file.
	 * 
	 * This method simply calls {@link #report(PrintStream, int)}.
	 * 
	 * @param file where to write to
	 * @param column what column to sort by
	 * @throws FileNotFoundException
	 */
	public static void report(File file, final int column) throws FileNotFoundException {
		final PrintStream stream = new PrintStream(new FileOutputStream(file));
		report(stream, column);
		stream.close();
	}

	/**
	 * Writes a report.
	 * 
	 * The methods are sorted by the total time spent in them or the methods
	 * they called.
	 * 
	 * @param writer
	 *            where to write to.
	 */
	public static void report(PrintStream writer) {
		report(writer, 3);
	}

	/**
	 * Writes a report.
	 * 
	 * The methods can be sorted by:
	 * 
	 * <ul>
	 * <li>the name (column == 0)
	 * <li>the number they were called (column == 1)
	 * <li>the average time in nanoseconds (column == 2)
	 * <li>the total time in nanoseconds (column = 3)
	 *  
	 * @param writer
	 *            where to write to.
	 */
	public static void report(PrintStream writer, final int column) {
		assert(CtBehavior.class.getClassLoader() != loader);
		synchronized(PerformanceProfiler.class) {
			if (!isActive()) {
				return;
			}
			setActive(false);
			final List<Row> rows = writer == null || column < 1 || column > 3 ?
					null : new ArrayList<Row>();
			final List<CtBehavior> behaviors = new ArrayList<CtBehavior>(counters.keySet());
			for (CtBehavior behavior : behaviors) try {
				int i = counters.get(behavior);
				Class<?> clazz = loader.loadClass(behavior.getDeclaringClass().getName());
				Field counter = clazz.getDeclaredField(toCounterName(i));
				counter.setAccessible(true);
				long count = counter.getLong(null);
				if (count == 0) continue;
				Field nanosField = clazz.getDeclaredField(toNanosName(i));
				nanosField.setAccessible(true);
				if (writer != null) {
					long nanos = nanosField.getLong(null);
					if (rows != null) {
						rows.add(new Row(behavior, count, nanos));
					} else {
						writer.println(Row.toString(behavior, count, nanos));
					}
				}
				counter.set(null, 0l);
				nanosField.set(null, 0l);
			} catch (Throwable e) {
				System.err.println("Problem with " + behavior.getLongName() + ":");
				if (e instanceof InvocationTargetException &&
						e.getCause() != null && e.getCause() instanceof NoClassDefFoundError) {
					System.err.println("Class not found: " + e.getCause().getMessage());
					break;
				}
				if (e instanceof ClassFormatError) {
					System.err.println("Class format error: " + behavior.getDeclaringClass().getName());
					break;
				}
				e.printStackTrace();
			}
			if (rows != null && writer != null) {
				final Comparator<Row> comparator;
				if (column == 1) {
					comparator = new Comparator<Row>() {
						@Override
						public int compare(Row a, Row b) {
							return -Double.compare(a.count, b.count);
						}
					};
				} else if (column == 2) {
					comparator = new Comparator<Row>() {
						@Override
						public int compare(Row a, Row b) {
							return -Double.compare(a.count, b.count);
						}
					};
				} else {
					comparator = new Comparator<Row>() {
						@Override
						public int compare(Row a, Row b) {
							return -Double.compare(a.nanos, b.nanos);
						}
					};
				}
				Collections.sort(rows, comparator);
				for (final Row row : rows) {
					writer.println(row.toString());
				}
			}
		}
	}

	// timing

	/**
	 * Gets nanoseconds per thread for relative time measurements
	 * 
	 * @return nanocseconds
	 */
	public final static long getNanos() {
		return bean.getCurrentThreadCpuTime();
	}

	/**
	 * A much faster, but less accurate version of {@link #getNanos()}
	 * 
	 * The problem with this is that it does not measure the current Thread's CPU cycles.
	 * 
	 * @return nanoseconds for relative time measurements
	 */
	public final static long getNanosQnD() {
		return System.nanoTime();
	}

	// Translator methods

	@Override
	public synchronized void start(ClassPool pool) throws NotFoundException, CannotCompileException {
		// ignore
	}

	@Override
	public synchronized void onLoad(ClassPool pool, String classname) throws NotFoundException {
		// do not instrument yourself
		if (classname.equals(getClass().getName())) {
			return;
		}

		// do not instrument anything javassist
		if (classname.startsWith("javassist.")) {
			return;
		}

		if (only != null && !only.contains(classname)) {
			return;
		}
		if (skip != null && skip.contains(classname)) {
			return;
		}

		if (debug)
			System.err.println("instrumenting " + classname);

		CtClass cc = pool.get(classname);
		if (cc.isFrozen())
			return;

		// instrument all methods and constructors
		if (debug)
			System.err.println("Handling class " + cc.getName());
		handle(cc, cc.getClassInitializer());
		for (CtMethod method : cc.getDeclaredMethods())
			handle(cc, method);
		for (CtConstructor constructor : cc.getDeclaredConstructors())
			handle(cc, constructor);
	}

	// private methods and classes

	private static class BehaviorComparator implements Comparator<CtBehavior> {

		@Override
		public int compare(CtBehavior a, CtBehavior b) {
			return a.getLongName().compareTo(b.getLongName());
		}

	}

	private static void init() {
		assert(loader == null);
		try {
			counters = new TreeMap<CtBehavior, Integer>(new BehaviorComparator());
			ClassPool pool = ClassPool.getDefault();
			pool.appendClassPath(new ClassClassPath(PerformanceProfiler.class));
			loader = new Loader(PerformanceProfiler.class.getClassLoader(), pool);

			// initialize a couple of things int the "other" PerformanceProfiler "instance"
			CtClass that = pool.get(PerformanceProfiler.class.getName());

			// add the "active" flag
			CtField active = new CtField(CtClass.booleanType, "active", that);
			active.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
			that.addField(active);

			// make report() work in the other "instance"
			realReport = PerformanceProfiler.class.getMethod("report", PrintStream.class, Integer.TYPE);
			CtMethod realReportMethod = that.getMethod("report", "(Ljava/io/PrintStream;I)V");
			realReportMethod.insertBefore("reportCaller($1, 3); realReport.invoke(null, $args); return;");

			Class<?> thatClass = loader.loadClass(that.getName());

			// get a reference to the "active" flag for use in setActive() and isActive()
			activeField = thatClass.getField("active");

			// make getNanos() work
			bean = ManagementFactory.getThreadMXBean();

			// make setActive() and isActive() work in the other "instance", too
			for (String fieldName : new String[] { "loader", "activeField", "counters", "realReport", "bean" }) {
				Field thisField = PerformanceProfiler.class.getDeclaredField(fieldName);
				thisField.setAccessible(true);
				Field thatField = thatClass.getDeclaredField(fieldName);
				thatField.setAccessible(true);
				thatField.set(null, thisField.get(null));
			}

			// add the class definition translator
			loader.addTranslator(pool, new PerformanceProfiler());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates the field name for the counters.
	 * 
	 * @param i running counter
	 * @return the field name
	 */

	private static String toCounterName(int i) {
		return "__counter" + i + "__";
	}


	/**
	 * Generates the field name for the nanoseconds.
	 * 
	 * @param i running counter
	 * @return the field name
	 */
	private static String toNanosName(int i) {
		return "__nanos" + i + "__";
	}

	/**
	 * Instruments a constructor or method.
	 * 
	 * @param clazz the declaring class
	 * @param behavior the constructor or method
	 */

	private synchronized void handle(CtClass clazz, CtBehavior behavior) {
		if (behavior == null) return;
		try {
			if (clazz != behavior.getDeclaringClass()) {
				if (debug)
					System.err.println("Skipping superclass' method: " + behavior.getName()
							+ " (" + behavior.getDeclaringClass().getName() + " is superclass of " + clazz);
				return;
			}
			if (debug)
				System.err.println("instrumenting " + behavior.getClass().getName() + "." + behavior.getName());
			if (behavior.isEmpty())
				return;

			int i;
			for (i = 1; ; i++) {
				if (!hasField(clazz, toCounterName(i)) && !hasField(clazz, toNanosName(i))) {
					break;
				}
			}
			final String counterFieldName = toCounterName(i);
			final String nanosFieldName = toNanosName(i);

			CtField counterField = new CtField(CtClass.longType, counterFieldName, clazz);
			counterField.setModifiers(Modifier.STATIC);
			clazz.addField(counterField);
			CtField nanosField = new CtField(CtClass.longType, nanosFieldName, clazz);
			nanosField.setModifiers(Modifier.STATIC);
			clazz.addField(nanosField);

			final String thisName = getClass().getName();
			final String that = clazz.getName() + ".";
			final String getNanos = thisName
					+ (fastButInaccurateTiming ?
							".getNanosQnD()" : ".getNanos()");
			behavior.addLocalVariable("__startTime__", CtClass.longType);
			behavior.insertBefore("__startTime__ = " + thisName + ".active ? " + getNanos + " : -1;");
			behavior.insertAfter("if (__startTime__ != -1) {"
					+ that + counterFieldName + "++;"
					+ that + nanosFieldName + " += " + getNanos + " - __startTime__;"
					+ "}");
			assert(behavior.getClass().getClassLoader() != loader);
			counters.put(behavior, i);
		}
		catch (CannotCompileException e) {
			if (!e.getMessage().equals("no method body")) {
				System.err.println("Problem with " + behavior.getLongName() + ":");
				if (e.getCause() != null && e.getCause() instanceof NotFoundException) {
					System.err.println("(could not find " + e.getCause().getMessage() + ")");
				} else {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Checks whether the given class contains a field with the given name.
	 * 
	 * @param clazz the class
	 * @param name the field name
	 * @return whether the class has the field
	 */

	private static boolean hasField(final CtClass clazz, final String name) {
		try {
			return clazz.getField(name) != null;
		} catch (NotFoundException e) {
			return false;
		}
	}

	/**
	 * Reports a caller.
	 * 
	 * @param writer where to write to
	 * @param level how many levels to go back in the stack trace
	 */
	protected static void reportCaller(PrintStream writer, int level) {
		if (writer == null) {
			return;
		}
		final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		if (stack == null || stack.length <= level || stack[level] == null) {
			writer.println("Could not determine caller");
		} else {
			final StackTraceElement caller = stack[level];
			writer.println("Report called by " + caller.toString());
		}
	}

	/**
	 * A row of the report.
	 * 
	 * Contains the method or constructor, how often it has been called, and how
	 * much time was spent in it or its callees.
	 * 
	 * @author Johannes Schindelin
	 */
	private static class Row {
		private final CtBehavior behavior;
		private final long count, nanos;

		public Row(CtBehavior behavior, long count, long nanos) {
			this.behavior = behavior;
			this.count = count;
			this.nanos = nanos;
		}

		@Override
		public String toString() {
			return toString(behavior, count, nanos);
		}

		public static String toString(CtBehavior behavior, long count, long nanos) {
			return behavior.getLongName() + "; " + count + "x; average: "
					+ formatNanos(nanos / count) + "; total: "
					+ formatNanos(nanos);
		}
	}

	private static String formatNanos(long nanos) {
		if (nanos < 1000) return "" + nanos + "ns";
		if (nanos < 1000000) return (nanos / 1000.0) + "µs";
		if (nanos < 1000000000) return (nanos / 1000000.0) + "ms";
		return (nanos / 1000000000.0) + "s";
	}

	private static void doMain(final String mainClass, final String... args) throws Throwable {
		setActive(true);
		loader.run(mainClass, args);
		report(System.err);
	}
}
