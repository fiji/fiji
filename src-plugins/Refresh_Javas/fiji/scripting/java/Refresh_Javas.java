package fiji.scripting.java;

import common.RefreshScripts;

import ij.IJ;
import ij.ImagePlus;
import ij.Menus;

import ij.io.PluginClassLoader;

import ij.text.TextWindow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.InputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This plugin looks for Java sources in plugins/ and turns them into
 * transparently-compiling plugins.
 *
 * That means that whenever the .java file is newer than the .class file,
 * it is compiled before it is called.
 */
public class Refresh_Javas extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".java", "Java");
		setVerbose(false);
		super.run(arg);
	}

	public void runScript(InputStream istream) {
		// TODO
		IJ.log("Refresh_Javas cannot work with streams at the moment.");
	}

	/** Compile and run an ImageJ plugin */
	public void runScript(String path) {
		String c = path;
		if (c.endsWith(".java")) {
			c = c.substring(0, c.length() - 5);
			try {
				if (!upToDate(path, c + ".class") &&
						!compile(path))
					return;
			} catch(Exception e) {
				IJ.error("Could not invoke javac compiler for "
					+ path + ": " + e);
				return;
			}
		}
		try {
			File plugins = new File(Menus.getPlugInsPath())
				.getCanonicalFile();
			File file = new File(c).getCanonicalFile();
			c = file.getName();
			while ((file = file.getParentFile()) != null &&
					!file.equals(plugins))
				c = file.getName() + "." + c;
		} catch (Exception e) { e.printStackTrace(); }
		runPlugin(c.replace('/', '.'));
	}

	boolean upToDate(String source, String target) {
		File sourceFile = new File(source);
		File targetFile = new File(target);
		if (!targetFile.exists())
			return false;
		if (!sourceFile.exists())
			return true;
		return sourceFile.lastModified() < targetFile.lastModified();
	}

	static Method javac;

	boolean compile(String path) throws ClassNotFoundException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {
		String[] arguments = { path };
		String classPath = getPluginsClasspath();
		if (!classPath.equals(""))
			arguments = new String[] {
				"-g", "-classpath", classPath, path
			};
		if (javac == null) {
			String className = "com.sun.tools.javac.Main";
			ClassLoader loader = getClass().getClassLoader();
			Class main = loader.loadClass(className);
			Class[] argsType = new Class[] {
				arguments.getClass(),
				PrintWriter.class
			};
			javac = main.getMethod("compile", argsType);
		}

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(buffer);
		Object result = javac.invoke(null,
			new Object[] { arguments, out });

		if (result.equals(new Integer(0)))
			return true;

		new TextWindow("Could not compile " + path,
				buffer.toString(), 640, 480);
		return false;
	}

	void runPlugin(String className) {
		new PlugInExecuter(className);
	}
}
