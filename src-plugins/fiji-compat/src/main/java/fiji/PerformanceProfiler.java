package fiji;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
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

public class PerformanceProfiler implements Translator {
	private Set<String> only, skip;
	private boolean fastButInaccurateTiming = true;

	protected static final boolean debug = false;
	private static Loader loader;
	private static Field activeField;
	private static Map<CtBehavior, Integer> counters;
	protected static Method realReport;
	private static ThreadMXBean bean;

	private static class BehaviorComparator implements Comparator<CtBehavior> {

		@Override
		public int compare(CtBehavior a, CtBehavior b) {
			return a.getLongName().compareTo(b.getLongName());
		}

	}

	public static void init() {
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
			realReport = PerformanceProfiler.class.getMethod("report", PrintStream.class);
			CtMethod realReportMethod = that.getMethod("report", "(Ljava/io/PrintStream;)V");
			realReportMethod.insertBefore("realReport.invoke(null, $args); return;");

			Class<?> thatClass = that.toClass(loader, null);

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

	public PerformanceProfiler() {
		this(System.getenv("PERFORMANCE_PROFILE_ONLY"));
	}

	public PerformanceProfiler(String only) {
		this(only == null ? null : Arrays.asList(only.split(" +")));
	}

	public PerformanceProfiler(Collection<String> only) {
		if (only != null) {
			this.only = new HashSet<String>();
			this.only.addAll(only);
		}
		skip = new HashSet<String>();
	}

	@Override
	public synchronized void start(ClassPool pool) throws NotFoundException, CannotCompileException {
		// ignore
	}

	@Override
	public void onLoad(ClassPool pool, String classname) throws NotFoundException {
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
		for (CtMethod method : cc.getDeclaredMethods())
			handle(cc, method);
		for (CtConstructor constructor : cc.getDeclaredConstructors())
			handle(cc, constructor);
	}

	private static String toCounterName(int i) {
		return "__counter" + i + "__";
	}

	private static String toNanosName(int i) {
		return "__nanos" + i + "__";
	}

	private void handle(CtClass clazz, CtBehavior behavior) {
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
			final String getNanos = thisName + (fastButInaccurateTiming ? ".getNanosQnD()" : ".getNanos()");
			behavior.addLocalVariable("__startTime__", CtClass.longType);
			behavior.insertBefore("__startTime__ = " + getNanos + ";");
			behavior.insertAfter("if (" + thisName + ".active) {"
					+ that + counterFieldName + "++;"
					+ that + nanosFieldName + " += " + getNanos + " - __startTime__;"
					+ "}");
			counters.put(behavior, i);
		}
		catch (CannotCompileException e) {
			if (!e.getMessage().equals("no method body"))
				e.printStackTrace();
		}
	}

	private static boolean hasField(final CtClass clazz, final String name) {
		try {
			return clazz.getField(name) != null;
		} catch (NotFoundException e) {
			return false;
		}
	}

	public static void setActive(boolean active) {
		if (loader == null) init();
		try {
			activeField.setBoolean(null, active);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isActive() {
		try {
			return activeField.getBoolean(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

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
			return behavior.getLongName() + "; " + count + "x; average: " + formatNanos(nanos / count) + "; total: " + formatNanos(nanos);
		}
	}

	public static void report(PrintStream writer) {
		report(writer, 3);
	}

	public static void report(PrintStream writer, final int column) {
		synchronized(PerformanceProfiler.class) {
			if (!isActive()) {
				return;
			}
			setActive(false);
			final List<Row> rows = writer == null || column < 1 || column > 3 ?
					null : new ArrayList<Row>();
			for (CtBehavior behavior : counters.keySet()) try {
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
			} catch (Exception e) {
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

	private static String formatNanos(long nanos) {
		if (nanos < 1000) return "" + nanos + "ns";
		if (nanos < 1000000) return (nanos / 1000.0) + "µs";
		if (nanos < 1000000000) return (nanos / 1000000.0) + "ms";
		return (nanos / 1000000000.0) + "s";
	}

	public static void main(String[] args) throws Throwable {
		Thread.currentThread().setContextClassLoader(PerformanceProfiler.class.getClassLoader());

		if (args.length == 0) {
			System.err.println("Usage: java " + PerformanceProfiler.class + " <main-class> [<argument>...]");
			System.exit(1);
		}

		String mainClass = args[0];
		String[] mainArgs = new String[args.length - 1];
		System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);

		setActive(true);
		loader.run(mainClass, mainArgs);
		report(System.err);
	}
}
