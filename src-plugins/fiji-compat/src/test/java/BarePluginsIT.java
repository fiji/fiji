
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.junit.Test;
import org.scijava.util.ClassUtils;
import org.scijava.util.FileUtils;

import test.Dependency;
import test.Missing_Dependency;
import test.Test_PlugIn;

import fiji.IJ1Patcher;

/**
 * Verifies that <i>.class</i> files can live in a couple of crazy places in
 * ImageJ 1.x's plugins/ directory.
 * <p>
 * This class needs to be in the default package because it cannot otherwise
 * access the plugin classes that live in said package.
 * </p>
 * 
 * @author Johannes Schindelin
 */
public class BarePluginsIT {

	private static boolean verbose;

	static {
		final String env = System.getenv("VERBOSE_FIJI_COMPAT_IT");
		verbose = env != null &&
			env.matches("(?i)true||\\+?[1-9][0-9]*");
	}

	@Test
	public void testBarePlugins() throws Exception {
		final String testClasses = ClassUtils.getLocation(BarePluginsIT.class).getPath();
		assertTrue(testClasses.endsWith("/target/test-classes/"));
		final String basedir = testClasses.substring(0, testClasses.length() - "test-classes/".length());
		assertTrue(new File(basedir).isDirectory());

		final File ijRoot = new File(basedir, "target/ijRoot");

		assertTrue("Could not delete " + ijRoot,
			!ijRoot.isDirectory() || FileUtils.deleteRecursively(ijRoot));
		assertTrue("Could not make " + ijRoot, ijRoot.mkdirs());

		final File plugins = new File(ijRoot, "plugins");
		final File sub = new File(plugins, "test");
		assertTrue("Could not make " + sub, sub.mkdirs());
		final File jars = new File(ijRoot, "jars");
		assertTrue("Could not make " + jars, jars.mkdirs());

		copyClass(Bare_PlugIn.class, plugins);
		copyClass(Test_PlugIn.class, plugins);
		copyClass(Another_Bare_PlugIn.class, sub);
		copyClass(Missing_Dependency.class, plugins);
		writeChangedMethodPlugin(plugins);

		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		new IJ1Patcher().run();

		final ClassLoader loader = makeRestrictedClassLoader(Bare_PlugIn.class,
				Test_PlugIn.class, Another_Bare_PlugIn.class,
				Missing_Dependency.class, Dependency.class);
		final Class<?> self = loader.loadClass(getClass().getName());
		final Object object = self.newInstance();
		final Method method = self.getDeclaredMethod("run", File.class);
		method.invoke(object, ijRoot);
	}

	/**
	 * Makes a class loader that can load only the specified class from the
	 * corresponding class path element.
	 * <p>
	 * To prevent the classes to be found via the class path (and instead
	 * require ImageJ 1.x' plugin class loader to find them), we must have a
	 * class loader that can load this test class, and all the fiji-compat
	 * classes and dependencies, but nothing else from fiji-compat's
	 * <tt>src/test/java</tt> classes.
	 * </P
	 * .
	 * 
	 * @param clazz
	 *            the class to restrict to
	 * @return the class loader
	 */
	private ClassLoader makeRestrictedClassLoader(final Class<?>... exclude) {
		final ClassLoader loader = getClass().getClassLoader();
		assertTrue(loader instanceof URLClassLoader);

		final Set<String> blacklist = new HashSet<String>();
		for (final Class<?> clazz : exclude) {
			blacklist.add(clazz.getName());
		}

		final ClassLoader result = new URLClassLoader(((URLClassLoader)loader).getURLs(), loader.getParent()) {
			@Override
			public Class<?> loadClass(final String name) throws ClassNotFoundException {
				if (blacklist.contains(name)) throw new ClassNotFoundException("Blacklisted: " + name);
				return super.loadClass(name);
			}
		};

		for (final Class<?> clazz : exclude) try {
			assertTrue(result.loadClass(clazz.getName()) == null);
		} catch (ClassNotFoundException e) {
			assertTrue(e.getMessage().startsWith("Blacklist"));
		}

		return result;
	}

	/**
	 * The real test.
	 * <p>
	 * This method must be run in a class loader that cannot see the plugins.
	 * </p>
	 * 
	 * @param ijRoot
	 *            the fake ImageJ 1.x root directory containing the plugins/
	 *            directory
	 */
	public void run(final File ijRoot) {
		try {
			assertTrue(Bare_PlugIn.class.getName() == null);
		} catch (NoClassDefFoundError e) {
			// alright, let's go on!
		}

		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		new IJ1Patcher().run();

		System.err.println("Redirecting stdout");

		final PrintStream stdout = System.out;
		final ByteArrayOutputStream teeBuffer = new ByteArrayOutputStream() {
			public void reset() {
				if (verbose) try {
					stdout.write(toByteArray());
				} catch (IOException e) {
					e.printStackTrace();
				}
				super.reset();
			}
		};
		final PrintStream tee = new PrintStream(teeBuffer);
		System.setOut(tee);

		final String ijPath = ijRoot.getAbsolutePath();
		System.setProperty("ij.dir", ijPath);
		System.setProperty("plugins.dir", ijPath);

		assertPlugInOutput("Bare PlugIn", "Hello (bare) world!", ijPath, stdout, tee, teeBuffer);
		assertPlugInOutput("Test PlugIn", "Hello (test) world!", ijPath, stdout, tee, teeBuffer);
		assertPlugInOutput("Another Bare PlugIn", "Hello (another bare) world!", ijPath, stdout, tee, teeBuffer);
		assertPlugInOutput("Missing Dependency", "java.lang.NoClassDefFoundError: test/Dependency", ijPath, stdout, tee, teeBuffer);
		assertPlugInOutput("Changed Method Signature",
			"There was a problem with the class ij.ImageJ which can be found here:", ijPath, stdout, tee, teeBuffer);

		teeBuffer.reset();
	}

	private void copyClass(final Class<?> clazz, final File targetDirectory) throws FileNotFoundException, IOException {
		final String className = clazz.getName();
		final String path = className.replace('.', '/') + ".class";
		final File target = new File(targetDirectory, path);
		final URL url = clazz.getResource("/" + path);
		System.err.println("Copying " + url + " to " + target);
		copyStream(url.openStream(), target);
	}

	private void writeChangedMethodPlugin(final File targetDirectory) throws CannotCompileException, IOException, NotFoundException {
		final ClassPool pool = new ClassPool();
		pool.appendClassPath(new LoaderClassPath(getClass().getClassLoader().getParent()));
		final CtClass ijClass = pool.makeClass("ij.ImageJ");
		ijClass.addMethod(CtNewMethod.make("public static void main(double value) {" +
			"throw new java.lang.RuntimeException(\"Should not be called\");" +
			"}", ijClass));
		final File target = new File(targetDirectory, "ij/ImageJ.class");
		copyStream(new ByteArrayInputStream(ijClass.toBytecode()), target);
		final CtClass pluginClass = pool.makeInterface("ij.plugin.PlugIn");
		pluginClass.addMethod(CtNewMethod.abstractMethod(CtClass.voidType,
				"run", new CtClass[] { pool.get("java.lang.String") },
				new CtClass[0], pluginClass));
		final CtClass changedMethodPlugin = pool.makeClass("Changed_Method_Signature");
		changedMethodPlugin.addInterface(pluginClass);
		changedMethodPlugin.addMethod(CtNewMethod.make("public void run(java.lang.String arg) {" +
				"ij.ImageJ.main(1.23);" +
				"}", changedMethodPlugin));
		final File pluginTarget = new File(targetDirectory, changedMethodPlugin.getName() + ".class");
		copyStream(new ByteArrayInputStream(changedMethodPlugin.toBytecode()), pluginTarget);

	}

	@SuppressWarnings("unused")
	private void copyJar(final Class<?> clazz, final File targetDirectory) throws IOException {
		final String path = clazz.getName().replace('.', '/') + ".class";
		final String url = clazz.getResource("/" + path).toString();
		final int bang = url == null ? -1 : url.indexOf("!/");
		assertTrue(clazz.getName() + " is not in a .jar: " + url,
			url != null && url.startsWith("jar:file:") && bang > 0);
		final File source = new File(url.substring(9, bang));
		copyStream(new FileInputStream(source), new File(targetDirectory, source.getName()));
	}

	private void copyStream(final InputStream in, final File target) throws IOException {
	        final File parent = target.getParentFile();
	        if (!parent.isDirectory()) parent.mkdirs();
	        final OutputStream out = new FileOutputStream(target);
	        copyStream(in, out);
	}

	private void copyStream(final InputStream in, final OutputStream out) throws IOException {
	        final byte[] buffer = new byte[65536];
	        for (;;) {
	                int count = in.read(buffer);
	                if (count < 0) break;
	                out.write(buffer, 0, count);
	        }
	        in.close();
	        out.close();
	}

	private void assertPlugInOutput(final String plugin, final String expectedFirstLine, final String ijPath,
			final PrintStream stdout, final PrintStream tee, final ByteArrayOutputStream teeBuffer) {
		System.err.println("Running " + plugin);
		tee.flush();
		stdout.flush();
		teeBuffer.reset();
		System.setProperty("ij1.plugin.dirs", "/non-existing/");
		fiji.Main.main(new String[] {
			"-ijpath", ijPath,
			"-eval", "run(\"" + plugin + "\");",
			"-batch-no-exit"
		});
		final String output = teeBuffer.toString();
		final int eol = output.indexOf('\n');
		final String firstLine = eol < 0 ? output : output.substring(0, eol);
		assertTrue("Ran " + plugin + ", expected " + expectedFirstLine + ", got " + firstLine,
			expectedFirstLine.equals(firstLine));
		teeBuffer.reset();
	}

}
