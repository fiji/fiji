package fiji.scripting.java;

import fiji.FijiClassLoader;
import fiji.User_Plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This class should have been public instead of being hidden in
 * ij/plugin/Compiler.java.
 */
public class PlugInExecutor {
	ClassLoader classLoader;

	public PlugInExecutor() {}

	/*
	 * We cannot use ImageJ's class loader as delegate class loader,
	 * as we possibly want to _override_ classes from the plugins/
	 * directory.
	 */
	public PlugInExecutor(String classPath) throws MalformedURLException {
		this(classPath.split(File.pathSeparator));
	}

	public PlugInExecutor(String[] paths) throws MalformedURLException {
		URL[] urls = new URL[paths.length];
		for (int i = 0; i < urls.length; i++)
			urls[i] = new File(paths[i]).toURI().toURL();
		classLoader = new URLClassLoader(urls);
		classLoader.setDefaultAssertionStatus(true);
	}

	/** Create a new object that runs the specified plugin
		in a separate thread. */
	public void runThreaded(final String plugin) {
		Thread thread = new Thread() {
			public void run() {
				PlugInExecutor.this.run(plugin);
			}
		};
		thread.setPriority(Math.max(thread.getPriority()-2,
					Thread.MIN_PRIORITY));
		thread.start();
	}

	public void run(String plugin) {
		run(plugin, "");
	}

	public void run(String plugin, String arg) {
		run(plugin, arg, false);
	}

	public void run(String plugin, String arg, boolean newClassLoader) {
		try {
			IJ.resetEscape();
			tryRun(plugin, arg, newClassLoader);
		} catch(Throwable e) {
			IJ.showStatus("");
			IJ.showProgress(1.0);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null) imp.unlock();
			String msg = e.getMessage();
			if (e instanceof RuntimeException && msg!=null &&
					msg.equals(Macro.MACRO_CANCELED))
				return;
			IJ.handleException(e);
		}
	}

	public void tryRun(String plugin, String arg, boolean newClassLoader)
			throws ClassNotFoundException, IOException,
				IllegalAccessException,
				InvocationTargetException,
				NoSuchMethodException {
		tryRun(plugin, arg, null, newClassLoader);
	}

	private final static Pattern wrongClassNamePattern =
		Pattern.compile("^([^ ]*) \\(wrong name: ([^ ]*)\\)$");

	public void tryRun(String plugin, String arg, String jarPath, boolean newClassLoader)
			throws ClassNotFoundException, IOException,
				IllegalAccessException,
				InvocationTargetException,
				NoSuchMethodException {
		ClassLoader classLoader = newClassLoader ?
			new FijiClassLoader(new String[] {
				IJ.getDirectory("plugins"),
				System.getProperty("fiji.dir") + "/jars"
			}) : getClassLoader();
		if (newClassLoader && jarPath != null)
			((FijiClassLoader)classLoader).addPath(jarPath);
		Class clazz;
		try {
			clazz = classLoader.loadClass(plugin);
		} catch (NoClassDefFoundError e) {
			Matcher matcher = wrongClassNamePattern.matcher(e.getMessage());
			if (!matcher.matches())
				throw e;
			String correctClassName = matcher.group(2);

			String path = plugin.replace('.', '/') + ".class";
			URL url = classLoader.getResource(path);
			if (url == null)
				throw new RuntimeException("Could not find resource for class " + plugin, e);
			String classPath = url.toString();
			if (classPath.startsWith("file:"))
				classPath = classPath.substring(5);
			classPath += plugin.replace('.', '/');
			classPath = classPath.substring(0, classPath.length() - correctClassName.length());
			((FijiClassLoader)classLoader).addPath(jarPath);
			clazz = classLoader.loadClass(correctClassName);
		}
		try {
			if (PlugIn.class.isAssignableFrom(clazz)) {
				Object object = clazz.newInstance();
				((PlugIn)object).run(arg);
				return;
			}
			if (PlugInFilter.class.isAssignableFrom(clazz)) {
				Object object = clazz.newInstance();
				ImagePlus image = WindowManager.getCurrentImage();
				if (image != null && image.isLocked()) {
					if (!IJ.showMessageWithCancel("Unlock image?", "The image '" + image.getTitle()
							+ "'appears to be unlocked... Unlock?"))
						return;
					image.unlock();
				}
				new PlugInFilterRunner(object,
						plugin, arg);
				return;
			}
			runMain(clazz, arg);
		} catch (Throwable e) {
			e.printStackTrace();
			Throwable cause = e.getCause();
			if (cause != null) {
				System.err.print("Cause: ");
				cause.printStackTrace();
			}
		}
	}

	public void runOneOf(String jar, boolean newClassLoader)
			throws ClassNotFoundException, IOException,
				IllegalAccessException,
				InvocationTargetException,
				NoSuchMethodException {
		List list = new User_Plugins().getJarPluginList(new File(jar), "");
		String plugin = null;
		if (list.size() == 1)
			plugin = ((String[])list.get(0))[2];
		else if (list.size() > 1) {
			plugin = ((String[])list.get(0))[2];
			String[] names = new String[list.size()];
			for (int i = 0; i < names.length; i++) {
				names[i] = ((String[])list.get(i))[1];
				if (plugin != null && !((String[])list
						.get(i))[2].equals(plugin))
					plugin = null;
			}
			if (plugin == null) {
				GenericDialog gd = new GenericDialog("Choose plugin to run");
				gd.addChoice("Plugin", names, names[0]);
				gd.showDialog();
				if (!gd.wasCanceled())
					plugin = ((String[])list.get(gd.getNextChoiceIndex()))[2];
			}
		}
		if (plugin != null) {
			String arg = "";
			int paren = plugin.indexOf("(\"");
			if (paren > 0 && plugin.endsWith("\")")) {
				arg = plugin.substring(paren + 2,
						plugin.length() - 2);
				plugin = plugin.substring(0, paren);
			}
			tryRun(plugin, arg, jar, newClassLoader);
		}
	}

	ClassLoader getClassLoader() {
		if (classLoader == null)
			classLoader = IJ.getClassLoader();
		return classLoader;
	}

	void runMain(Class clazz, String arg) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		String[] args = new String[] { arg };
		Method main = clazz.getMethod("main",
				new Class[] { args.getClass() });
		main.invoke(null, (Object)args);
	}
}
