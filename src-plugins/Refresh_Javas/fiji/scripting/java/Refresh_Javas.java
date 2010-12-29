package fiji.scripting.java;

import common.RefreshScripts;

import fiji.build.Fake;
import fiji.build.FakeException;
import fiji.build.Parser;
import fiji.build.Rule;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Menus;

import ij.gui.GenericDialog;

import ij.io.PluginClassLoader;

import ij.text.TextWindow;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringBufferInputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.List;

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
		if (arg == null || arg.equals("")) {
			arg = Macro.getOptions();
			if (arg != null)
				arg = arg.trim();
		}
		super.run(arg);
	}

	public void runScript(InputStream istream) {
		// TODO
		IJ.log("Refresh_Javas cannot work with streams at the moment.");
	}

	/** Compile and run an ImageJ plugin */
	public void runScript(String path) {
		compileAndRun(path, false);
	}

	/** Compile and optionally run an ImageJ plugin */
	public void compileAndRun(String path, boolean compileOnly) {
		boolean isFakefile = path.endsWith("/Fakefile");
		String c = path;
		if (c.endsWith(".java") || isFakefile) {
			try {
				String[] result = isFakefile ?
					fake(new FileInputStream(path), new File(path).getParentFile(), null, null) :
					fake(path);
				if (result != null) {
					if (!compileOnly)
						runPlugin(result[1], result[0], true);
					return;
				}
				if (isFakefile) {
					err.write(("Could not compile " + path + "\n").getBytes());
					return;
				}
			} catch (FakeException e) {
				if (!e.getMessage().equals("Canceled")) try {
					err.write(e.getMessage().getBytes());
				} catch (IOException e2) {
					e.printStackTrace();
				}
				return;
			} catch (Exception e) {
				e.printStackTrace(new PrintStream(err));
				return;
			}
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
			if (!compileOnly && file == null) {
				runOutOfTreePlugin(path);
				return;
			}
			if (!compileOnly)
				runPlugin(c.replace('/', '.'));
		} catch (Exception e) {
			e.printStackTrace(new PrintStream(err));
		}
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

	protected String[] unshift(String[] list, String[] add) {
		String[] result = new String[list.length + add.length];
		System.arraycopy(add, 0, result, 0, add.length);
		System.arraycopy(list, 0, result, add.length, list.length);
		return result;
	}

	/* returns the class name and .jar on success, null otherwise */
	public String[] fake(String path) throws FakeException {
		File file = new File(path);
		try {
			file = file.getCanonicalFile();
		}
		catch (IOException e) {
			e.printStackTrace(new PrintStream(err));
			return null;
		}
		File dir = file.getParentFile();
		path = file.getName();
		String name = path;

		File fakefile;
		for (;;) {
			if (dir == null)
				return null;
			fakefile = new File(dir, "Fakefile");
			if (fakefile.exists())
				break;
			path = dir.getName() + "/" + path;
			dir = dir.getParentFile();
		}

		if (name.endsWith(".java"))
			name = name.substring(0, name.length() - 5);

		try {
			String[] result = fake(new FileInputStream(fakefile), dir, name, null, path, false);
			if (result != null)
				return result;
		}
		catch (FileNotFoundException e) {
			e.printStackTrace(new PrintStream(err));
		}

		String absolutePath = file.getAbsolutePath();
		String srcPluginsDir = new File(System.getProperty("fiji.dir"), "src-plugins").getAbsolutePath();
		if (!absolutePath.startsWith(srcPluginsDir + File.separator))
			return null;

		String base = absolutePath.substring(srcPluginsDir.length() + 1);
		int slash = base.indexOf(File.separator);
		if (slash > 0)
			base = base.substring(0, slash);
		String target = "plugins/" + base + ".jar";
		String rule = "all <- " + target + "\n"
			+ "\n"
			+ target + " <- src-plugins/" + base + "/**/*.java\n";
		return fake(new StringBufferInputStream(rule), dir, name, target);
	}

	/* returns the class name and .jar on success, null otherwise */
	public String[] fake(InputStream fakefile, File dir, String name, String target) throws FakeException {
		return fake(fakefile, dir, name, target, false);
	}

	/* returns the class name and .jar on success, null otherwise */
	public String[] fake(InputStream fakefile, File dir, String name, String target, boolean includeSource) throws FakeException {
		return fake(fakefile, dir, name, target, null, includeSource);
	}

	/* returns the class name and .jar on success, null otherwise */
	public String[] fake(InputStream fakefile, File dir, String name, String target, String relativeSourcePath, boolean includeSource) throws FakeException {
		Fake fake = new Fake();
		fake.out = new PrintStream(out);
		fake.err = new PrintStream(err);
		Parser parser = fake.parse(fakefile, dir);
		parser.parseRules(null);

		if (target == null) {
			if (relativeSourcePath != null)
				relativeSourcePath = relativeSourcePath.replace(File.separator.charAt(0), '/');
                        List<String> targets = new ArrayList<String>();
                        for (String key : parser.getAllRules().keySet()) {
				Rule rule = parser.getRule(key);
                                if (rule.getClass().getName().endsWith("$CompileJar") &&
						(relativeSourcePath == null ||
						 rule.getPrerequisites().contains(relativeSourcePath)))
                                        targets.add(key);
                        }
                        if (targets.size() == 0) {
				try {
	                                err.write(("Warning: no .jar targets found for '"
						+ relativeSourcePath + "' in '"
						+ new File(dir, "Fakefile").getAbsolutePath()
						+ "'\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
                                return null;
                        }
                        if (targets.size() == 1)
                                target = targets.get(0);
                        else {
                                String[] array = targets.toArray(new String[targets.size()]);
                                GenericDialog gd = new GenericDialog("Target");
                                gd.addChoice("Target", array, array[0]);
                                gd.showDialog();
                                if (gd.wasCanceled())
                                        throw new FakeException("Canceled");
                                target = gd.getNextChoice();
                        }
		}

		if (parser.getRule(target) == null)
			return null;
		parser.setVariable("debug", "true");
		parser.setVariable("buildDir", "build");
		if (includeSource)
			parser.setVariable("includeSource(" + target + ")", "true");
		parser.getRule(target).make();

		if (relativeSourcePath != null) try {
			String packageName = getPackageName(new File(dir, relativeSourcePath).getAbsolutePath());
			if (packageName != null)
				name = packageName + "." + name;
		} catch (IOException e) {
			e.printStackTrace(new PrintStream(err));
		}
		return new String[] { name, new File(dir, target).getAbsolutePath() };
	}

	static Method javac;

	boolean compile(String path) throws ClassNotFoundException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {
		return compile(path, null);
	}

	public boolean compile(String path, String outPath)
			throws ClassNotFoundException, NoSuchMethodException,
			       IllegalAccessException,
			       InvocationTargetException {
		return compile(path, outPath, null);
	}

	public boolean compile(String path, String outPath, String[] extraArgs)
			throws ClassNotFoundException, NoSuchMethodException,
			       IllegalAccessException,
			       InvocationTargetException {
		String[] arguments = { "-g", path };
		if (extraArgs != null)
			arguments = unshift(arguments, extraArgs);
		String classPath = getPluginsClasspath();
		if (!classPath.equals(""))
			arguments = unshift(arguments,
				new String[] { "-classpath", classPath });
		if (outPath != null)
			arguments = unshift(arguments,
				new String[] { "-d", outPath });
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

		ByteArrayOutputStream buffer = err == System.err ?
			new ByteArrayOutputStream() : null;
		PrintWriter out = new PrintWriter(buffer == null ?
				err : buffer);
		Object result = javac.invoke(null,
			new Object[] { arguments, out });

		if (result.equals(new Integer(0)))
			return true;

		if (buffer != null)
			new TextWindow("Could not compile " + path,
					buffer.toString(), 640, 480);
		return false;
	}

	void runPlugin(String className) {
		new PlugInExecutor().run(className);
	}

	void runPlugin(String className, boolean newClassLoader) {
		new PlugInExecutor().run(className, "", newClassLoader);
	}

	void runPlugin(String path, String className, boolean newClassLoader)
			throws Exception {
		PlugInExecutor executor = new PlugInExecutor(path);
		if (className == null)
			executor.runOneOf(path, newClassLoader);
		else try {
			executor.tryRun(className, "", path, newClassLoader);
		} catch (NoSuchMethodException e) {
			executor.runOneOf(path, newClassLoader);
		}
	}

	void runOutOfTreePlugin(String path) throws IOException,
			MalformedURLException {
		String className = new File(path).getName();
		if (className.endsWith(".java"))
			className = className.substring(0,
					className.length() - 5);

		String packageName = null;
		try {
			packageName = getPackageName(path);
		} catch (IOException e) {
			IJ.error("Could not read " + path);
			return;
		}
		String classPath = getPluginsClasspath();
		File directory = new File(path).getCanonicalFile()
			.getParentFile();
		if (packageName != null) {
			int dot = -1;
			do {
				className = directory.getName()
					+ "." + className;
				directory = directory.getParentFile();
				dot = packageName.indexOf('.', dot + 1);
			} while (dot > 0);
		}
		if (classPath == null || classPath.equals(""))
			classPath = directory.getPath();
		else {
			// make sure classes from this directory are found first
			if (!classPath.startsWith(File.pathSeparator))
				classPath = File.pathSeparator + classPath;
			classPath = directory.getPath() + classPath;
		}

		new PlugInExecutor(classPath).run(className);
	}

	public String getPackageName(String path) throws IOException {
		InputStream in = new FileInputStream(path);
		InputStreamReader streamReader = new InputStreamReader(in);
		BufferedReader reader = new BufferedReader(streamReader);

		boolean multiLineComment = false;
		String line;
		while ((line = reader.readLine()) != null) {
			if (multiLineComment) {
				int endOfComment = line.indexOf("*/");
				if (endOfComment < 0)
					continue;
				line = line.substring(endOfComment + 2);
				multiLineComment = false;
			}
			line = line.trim();
			while (line.startsWith("/*")) {
				int endOfComment = line.indexOf("*/", 2);
				if (endOfComment < 0) {
					multiLineComment = true;
					break;
				}
				line = line.substring(endOfComment + 2).trim();
			}
			if (multiLineComment)
				continue;
			if (line.startsWith("package ")) {
				int endOfPackage = line.indexOf(';');
				if (endOfPackage < 0)
					break;
				in.close();
				return line.substring(8, endOfPackage);
			}
			if (!line.equals("") && !line.startsWith("//"))
				break;
		}
		in.close();
		return null;
	}
}
