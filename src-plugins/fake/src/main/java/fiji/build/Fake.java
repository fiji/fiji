/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */
package fiji.build;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.zip.ZipException;

import fiji.build.minimaven.JarClassLoader;
import fiji.build.minimaven.JavaCompiler;
import fiji.build.minimaven.JavaCompiler.CompileError;

public class Fake {
	protected static String fijiBuildJar;
	protected static long mtimeFijiBuild;
	public PrintStream out = System.out, err = System.err;
	protected JavaCompiler javac;

	public static void main(String[] args) {
		MiniMaven.ensureIJDirIsSet();
		if (runPrecompiledFakeIfNewer(args))
			return;
		try {
			new Fake().make(null, null, args);
		} catch (FakeException e) {
			System.err.println("Could not instantiate Fiji Build:");
			e.printStackTrace();
		}
	}

	public static boolean runPrecompiledFakeIfNewer(String[] args) {
		String url = Fake.class.getResource("Fake.class").toString();
		String prefix = "jar:file:";
		String suffix = "/fake.jar!/fiji/build/Fake.class";
		if (!url.startsWith(prefix) || !url.endsWith(suffix))
			return false;
		url = url.substring(9, url.length() - suffix.length());
		File precompiled = new File(url + "/precompiled/fake.jar");
		if (!precompiled.exists())
			return false;
		File current = new File(url + "/fake.jar");
		if (!current.exists() || current.lastModified() >=
				precompiled.lastModified())
			return false;
		System.err.println("Please copy the precompiled fake.jar "
			+ "over the current fake.jar; the latter is older!");
		System.exit(1);
		return true;
	}

	final static Set<String> variableNames = new HashSet<String>();

	public Fake() throws FakeException {
		variableNames.add("DEBUG");
		variableNames.add("JAVAVERSION");
		variableNames.add("SHOWDEPRECATION");
		variableNames.add("VERBOSE");
		variableNames.add("IGNOREMISSINGFAKEFILES");
		variableNames.add("CFLAGS");
		variableNames.add("CXXFLAGS");
		variableNames.add("LDFLAGS");
		variableNames.add("MAINCLASS");
		variableNames.add("INCLUDESOURCE");
	}

	public final static String ijHome;

	static {
		ijHome = discoverImageJHome();
	}

	protected static String discoverImageJHome() {
		URL url = Fake.class.getResource("Fake.class");
		String ijHome;
		try {
			ijHome = URLDecoder.decode(url.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Could not discover the ImageJ root directory");
		}
		if (Util.getPlatform().startsWith("win"))
			ijHome = ijHome.replace('\\', '/');
		if (!ijHome.endsWith("/Fake.class"))
			throw new RuntimeException("unexpected URL: " + url);
		ijHome = ijHome.substring(0, ijHome.length() - 10);
		if (ijHome.endsWith("/fiji/build/"))
			ijHome = ijHome.substring(0, ijHome.length() - 11);
		int slash = ijHome.lastIndexOf('/', ijHome.length() - 2);
		if (ijHome.startsWith("jar:file:") &&
				ijHome.endsWith(".jar!/")) {
			fijiBuildJar = ijHome.substring(9,
					ijHome.length() - 2);
			mtimeFijiBuild = new File(fijiBuildJar).lastModified();
			ijHome = ijHome.substring(9, slash + 1);
		}
		else if (ijHome.startsWith("file:/")) {
			ijHome = ijHome.substring(5, slash + 1);
			if (ijHome.endsWith("/src-plugins/fake/target/"))
				ijHome = Util.stripSuffix(ijHome, "src-plugins/fake/target/");
			else if (ijHome.endsWith("/src-plugins/"))
				ijHome = Util.stripSuffix(ijHome, "src-plugins/");
			else if (ijHome.endsWith("/build/jars/"))
				ijHome = Util.stripSuffix(ijHome, "build/jars/");
		}
		if (Util.getPlatform().startsWith("win") && ijHome.startsWith("/"))
			ijHome = ijHome.substring(1);
		if (ijHome.endsWith("precompiled/"))
			ijHome = ijHome.substring(0, ijHome.length() - 12);
		else if (ijHome.endsWith("jars/"))
			ijHome = ijHome.substring(0, ijHome.length() - 5);
		else if (ijHome.endsWith("plugins/"))
			ijHome = ijHome.substring(0, ijHome.length() - 8);

		return ijHome;
	}

	protected static void setDefaultProperty(String key, String value) {
		if (null == System.getProperty(key))
			System.setProperty(key,value);
	}

	protected static void discoverJython() throws IOException {
		String pythonHome = ijHome + "jars";
		setDefaultProperty("python.home", pythonHome);
		setDefaultProperty("python.cachedir.skip", "false");
		String jythonJar = pythonHome + "/jython.jar";
		getClassLoader(ijHome + "/jars/jna.jar");
		getClassLoader(jythonJar);
	}

	protected static void discoverBeanshell() throws IOException {
		String bshJar = ijHome + "/jars/bsh.jar";
		getClassLoader(bshJar);
	}

	protected List<String> discoverJars() throws FakeException {
		List<String> jars = new ArrayList<String>();
		File cwd = new File(".");
		/*
		 * Since View5D contains an ImageCanvas (d'oh!) which would
		 * be picked up instead of ImageJ's, we cannot blindly
		 * include all plugin's jars...
		 */
		// expandGlob(ijHome + "plugins/**/*.jar", jars, cwd);
		expandGlob(ijHome + "jars/**/*.jar", jars, cwd, 0, null);
		if (Util.getPlatform().startsWith("win")) {
			String[] paths =
				Util.split(System.getProperty("java.ext.dirs"),
						File.pathSeparator);
			for (int i = 0; i < paths.length; i++) {
				if (!new File(paths[i]).exists())
					continue;
				expandGlob(paths[i].replace('\\', '/')
						+ "/*.jar", jars, cwd, 0, null);
			}
		}

		return jars;
	}

	protected String discoverClassPath() throws FakeException {
		return Util.join(discoverJars(), File.pathSeparator);
	}

	// keep this synchronized with imagej.updater.core.FileObject
	private final static Pattern versionPattern = Pattern.compile("(.+?)(-\\d+(\\.\\d+|\\d{7})+[a-z]?\\d?(-[A-Za-z0-9.]+|\\.GA)*)(\\.jar(-[a-z]*)?)");

	public static Matcher matchVersionedFilename(String filename) {
		return versionPattern.matcher(filename);
	}

	public static File[] getAllVersions(final File directory, final String filename) {
		final Matcher matcher = matchVersionedFilename(filename);
		if (!matcher.matches()) {
			final File file = new File(directory, filename);
			return file.exists() ? new File[] { file } : null;
		}
		final String baseName = matcher.group(1);
		return directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				if (!name.startsWith(baseName))
					return false;
				final Matcher matcher2 = matchVersionedFilename(name);
				return matcher2.matches() && baseName.equals(matcher2.group(1));
			}
		});
	}

	/* input defaults to reading the Fakefile, cwd to "." */
	public Parser parse(InputStream input, File cwd) throws FakeException {
		return new Parser(this, input, cwd);
	}

	/* input defaults to reading the Fakefile, cwd to "." */
	public void make(InputStream input, File cwd, String[] args) {
		try {
			Parser parser = parse(input, cwd);

			// filter out variable definitions
			int firstArg = 0;
			while (firstArg < args.length &&
					args[firstArg].indexOf('=') >= 0)
				firstArg++;

			List<String> list = null;
			if (args.length > firstArg) {
				list = new ArrayList<String>();
				for (int i = firstArg; i < args.length; i++)
					list.add(args[i]);
			}
			Rule all = parser.parseRules(list);

			for (int i = 0; i < firstArg; i++) {
				int equal = args[i].indexOf('=');
				parser.setVariable(args[i].substring(0, equal),
						args[i].substring(equal + 1));
			}

			for (Rule rule : all.getDependenciesRecursively())
				if (rule.getVarBool("rebuild"))
					rule.clean(false);

			String parallel = all.getVar("parallel");
			if (parallel != null)
				all.makeParallel(Integer.parseInt(parallel));
			else
				all.make();
		}
		catch (FakeException e) {
			System.err.println(e);
			System.exit(1);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	// several utility functions

	protected int expandGlob(String glob, Collection<String> list, File cwd,
			long newerThan, String buildDir) throws FakeException {
		if (glob == null)
			return 0;
		// find first wildcard
		int star = glob.indexOf('*'), qmark = glob.indexOf('?');

		// no wildcard?
		if (star < 0 && qmark < 0) {
			list.add(glob);
			return 1;
		}

		if (qmark >= 0 && qmark < star)
			star = qmark;
		boolean starstar = glob.substring(star).startsWith("**/");

		int prevSlash = glob.lastIndexOf('/', star);
		int nextSlash = glob.indexOf('/', star);

		String parentPath =
			prevSlash < 0 ? "" : glob.substring(0, prevSlash + 1);
		if (buildDir != null && parentPath.equals(buildDir))
			return 0;

		File parentDirectory = new File(Util.makePath(cwd, parentPath));
		if (!parentDirectory.exists())
			throw new FakeException("Directory '" + parentDirectory
				+ "' not found");

		String pattern = nextSlash < 0 ?
			glob.substring(prevSlash + 1) :
			glob.substring(prevSlash + 1, nextSlash);

		String remainder = nextSlash < 0 ?
			null : glob.substring(nextSlash + 1);

		int count = 0;

		if (starstar) {
			count += expandGlob(parentPath + remainder, list,
						cwd, newerThan, buildDir);
			remainder = "**/" + remainder;
			pattern = "*";
		}

		String[] names = parentDirectory.list(new GlobFilter(pattern,
					newerThan));
		Arrays.sort(names);

		for (int i = 0; i < names.length; i++) {
			String path = parentPath + names[i];
			if (starstar && names[i].startsWith("."))
				continue;
			if (names[i].equals(".git")
					|| names[i].equals(".DS_Store")
					|| names[i].equals(".classpath")
					|| names[i].equals(".project")
					|| names[i].equals(".settings")
					|| names[i].equals(".directory")
					|| names[i].endsWith(".form")
					|| names[i].endsWith(".swp")
					|| names[i].endsWith(".swo")
					|| names[i].endsWith("~"))
				continue;
			File file = new File(Util.makePath(cwd, path));
			if (nextSlash < 0) {
				if (file.isDirectory())
					continue;
				list.add(path);
				count++;
			}
			else if (file.isDirectory())
				count += expandGlob(path + "/" + remainder,
						list, cwd, newerThan, buildDir);
		}

		return count;
	}

	Set<String> expandToSet(String glob, File cwd) throws FakeException {
		Set<String> result = new HashSet<String>();
		String[] globs = Util.split(glob, " ");
		for (int i = 0; i < globs.length; i++)
			expandGlob(globs[i], result, cwd, 0, null);
		return result;
	}

	/*
	 * Sort .class entries at end of the given list
	 *
	 * Due to the recursive nature of java2classFiles(), the sorting of
	 * the glob expansion is not enough.
	 */
	protected void sortClassesAtEnd(List<String> list) {
		int size = list.size();
		if (size == 0 || !isClass(list, size - 1))
			return;
		int start = size - 1;
		while (start > 0 && isClass(list, start - 1))
			start--;
		List<String> classes = new ArrayList<String>();
		classes.addAll(list.subList(start, size));
		Collections.sort(classes);
		while (size > start)
			list.remove(--size);
		list.addAll(classes);
	}
	final protected boolean isClass(List<String> list, int index) {
		return list.get(index).endsWith(".class");
	}


	/*
	 * This function inspects a .class file for a given .java file,
	 * infers the package name and all used classes, and adds to "all"
	 * the class file names of those classes used that have been found
	 * in the same class path.
	 */
	protected void java2classFiles(String java, File cwd,
			File buildDir, List<String> result, Set<String> all) {
		if (java.endsWith(".java"))
			java = java.substring(0, java.length() - 5) + ".class";
		else if (!java.endsWith(".class")) {
			if (!all.contains(java)) {
				if (buildDir == null)
					sortClassesAtEnd(result);
				result.add(java);
				all.add(java);
			}
			return;
		}
		byte[] buffer = Util.readFile(Util.makePath(cwd, java));
		if (buffer == null) {
			if (!java.endsWith("/package-info.class"))
				err.println("Warning: " + java
					+ " does not exist.  Skipping...");
			return;
		}
		ByteCodeAnalyzer analyzer = new ByteCodeAnalyzer(buffer);
		String fullClass = analyzer.getPathForClass() + ".class";
		if (!java.endsWith(fullClass))
			throw new RuntimeException("Huh? " + fullClass
					+ " is not a suffix of " + java);
		java = java.substring(0, java.length() - fullClass.length());
		for (String className : analyzer.getClassNames()) {
			String path = java + className + ".class";
			if (new File(Util.makePath(cwd, path)).exists() &&
					!all.contains(path)) {
				result.add(path);
				all.add(path);
				java2classFiles(path, cwd, buildDir,
						result, all);
			}
		}
	}

	protected static void addRecursively(File dir, List<String> result, Set<String> all) {
		String[] files = dir.list();
		if (files == null || files.length == 0)
			return;
		Arrays.sort(files);
		for (int i = 0; i < files.length; i++) {
			File file = new File(dir, files[i]);
			if (file.isDirectory())
				addRecursively(file, result, all);
			else if (files[i].endsWith(".class")) {
				result.add(file.getAbsolutePath());
				all.add(file.getAbsolutePath());
			}
		}
	}

	protected String getPrefix(File cwd, String path) {
		try {
			InputStream input = new FileInputStream(Util.makePath(cwd, path));
			InputStreamReader inputReader =
				new InputStreamReader(input);
			BufferedReader reader = new BufferedReader(inputReader);
			for (;;) {
				String line = reader.readLine();
				if (line == null)
					break;
				else
					line = line.trim();
				if (!line.startsWith("package "))
					continue;
				line = line.substring(8);
				if (line.endsWith(";"))
					line = line.substring(0,
							line.length() - 1);
				line = line.replace(".", "/");
				int slash = path.lastIndexOf("/");
				int backslash = path.lastIndexOf("\\");
				if (backslash > slash)
					slash = backslash;
				if (path.endsWith(line + path.substring(slash)))
					return path.substring(0, path.length() -
							line.length() -
							path.length() + slash);
			}
			int slash = path.lastIndexOf('/');
			return path.substring(0, slash + 1);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/* discovers all the .class files for a given set of .java files */
	protected List<String> java2classFiles(List<String> javas, File cwd,
			File buildDir, Set<String> exclude, Set<String> noCompile)
			throws FakeException {
		List<String> result = new ArrayList<String>();
		Set<String> all = new HashSet<String>();
		if (buildDir != null) {
			addRecursively(buildDir, result, all);
			Collections.sort(result);
		}
		String lastJava = null;
		for (String file : javas) {
			if (exclude.contains(file))
				continue;
			boolean dontCompile = noCompile.contains(file);
			if (buildDir != null) {
				if (!dontCompile && file.endsWith(".java")) {
					lastJava = file;
					continue;
				}
				if (lastJava != null) {
					String prefix = getPrefix(cwd, lastJava);
					if (prefix != null)
						result.add(prefix);
					else
						err.println("Error: "
							+ lastJava);
					lastJava = null;
				}
			}
			if (dontCompile) {
				if (!all.contains(file)) {
					result.add(file);
					all.add(file);
				}
				continue;
			}
			java2classFiles(file, cwd, buildDir, result, all);
		}
		if (buildDir == null)
			sortClassesAtEnd(result);
		return result;
	}

	// returns all .java files in the list, and returns a list where
	// all the .java files have been replaced by their .class files.
	protected List<String> compileJavas(List<String> javas, File cwd, File buildDir,
			String javaVersion, boolean debug, boolean verbose,
			boolean showDeprecation, String extraClassPath,
			Set<String> exclude, Set<String> noCompile)
			throws FakeException {
		List<String> arguments = new ArrayList<String>();
		arguments.add("-encoding");
		arguments.add("UTF8");
		if (debug)
			arguments.add("-g");
		if (buildDir != null) {
			buildDir.mkdirs();
			arguments.add("-d");
			arguments.add(buildDir.getAbsolutePath());
		}
		if (javaVersion != null && !javaVersion.equals("")) {
			arguments.add("-source");
			arguments.add(javaVersion);
			arguments.add("-target");
			arguments.add(javaVersion);
		}
		if (showDeprecation) {
			arguments.add("-deprecation");
			arguments.add("-Xlint:unchecked");
		}
		if (extraClassPath != null && !extraClassPath.equals("")) {
			arguments.add("-classpath");
			arguments.add(Util.pathListToNative(extraClassPath));
		}
		String extDirs = System.getProperty("java.ext.dirs");
		if (extDirs != null && !extDirs.equals(""))
			arguments.add("-Djava.ext.dirs=" + extDirs);
		int optionCount = arguments.size();
		for (String path : javas)
			if (path.endsWith(".java") && !exclude.contains(path))
				arguments.add(Util.makePath(cwd, path));

		/* Do nothing if there is nothing to do ;-) */
		if (optionCount == arguments.size())
			return javas;

		String[] args = arguments.toArray(new
				String[arguments.size()]);

		if (verbose) {
			String output = "Compiling .java files: javac";
			for (int i = 0; i < args.length; i++)
				output += " " + args[i];
			err.println(output);
		}

		if (javac == null)
			javac = new JavaCompiler(err, out);
		try {
			javac.call(args, verbose);
		} catch (CompileError e) {
			throw new FakeException(e.getMessage(), e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new FakeException("Compile error: " + e);
		}

		List<String> result = java2classFiles(javas, cwd, buildDir, exclude,
			noCompile);
		return result;
	}

	protected static void addPluginsConfigToJar(JarOutputStream jar,
			String configPath) throws IOException {
		if (configPath == null)
			return;

		JarEntry entry = new JarEntry("plugins.config");
		jar.putNextEntry(entry);
		byte[] buffer = Util.readFile(configPath);
		jar.write(buffer, 0, buffer.length);
		jar.closeEntry();
	}

	// TODO: we really need string pairs; real path and desired path.
	protected void makeJar(String path, String mainClass, List<String> files,
			File cwd, File buildDir, String configPath,
			String stripPath, boolean verbose) throws FakeException {
		path = Util.makePath(cwd, path);
		if (verbose) {
			String output = "Making " + path;
			if (mainClass != null)
				output += " with main-class " + mainClass;
			output += " from";
			for (String file : files)
				output += " " + file;
			err.println(output);
		}
		Manifest manifest = null;
		if (mainClass != null) {
			String text = "Manifest-Version: 1.0\nMain-Class: "
				+ mainClass + "\n";
			InputStream input =
				new ByteArrayInputStream(text.getBytes());
			try {
				manifest = new Manifest(input);
			} catch(Exception e) { }
		}

		try {
			/*
			 * Avoid SIGBUS when writing fake.jar: it may be
			 * in use (mmap()ed), and overwriting that typically
			 * results in a crash.
			 */
			String origPath = null;
			try {
				if (Util.moveFileOutOfTheWay(path)) {
					origPath = path;
					path += ".new";
				}
			} catch (FakeException e) {
				path = moveToUpdateDirectory(path);
			}

			OutputStream out = new FileOutputStream(path);
			JarOutputStream jar = manifest == null ?
				new JarOutputStream(out) :
				new JarOutputStream(out, manifest);

			addPluginsConfigToJar(jar, configPath);
			String lastBase = stripPath;
			for (String realName : files) {
				if (realName.endsWith(".jar/")) {
					copyJar(Util.stripSuffix(Util.makePath(cwd,
						realName), "/"), jar,
						verbose);
					continue;
				}
				if (realName.endsWith("/") || realName.endsWith("\\") ||
						realName.equals("")) {
					lastBase = realName;
					continue;
				}
				String name = realName;
				int bracket = name.indexOf('[');
				if (bracket >= 0 && name.endsWith("]")) {
					realName = name.substring(bracket + 1,
						name.length() - 1);
					name = name.substring(0, bracket);
				}
				byte[] buffer = Util.readFile(Util.makePath(cwd,
								realName));
				if (buffer == null)
					throw new FakeException("File "
						+ realName + " does not exist,"
						+ " could not make " + path);
				if (realName.endsWith(".class")) {
					ByteCodeAnalyzer analyzer =
						new ByteCodeAnalyzer(buffer);
					name = analyzer.getPathForClass()
						+ ".class";
				}
				else if (lastBase != null &&
						name.startsWith(lastBase)) {
					if (!lastBase.equals(stripPath))
						throw new FakeException("strip "
							+ "path mismatch: "
							+ lastBase + " != "
							+ stripPath);
					name = name
						.substring(lastBase.length());
				}

				JarEntry entry = new JarEntry(name);
				writeJarEntry(entry, jar, buffer);
			}

			jar.close();
			if (origPath != null)
				throw new FakeException("Could not remove "
					+ origPath
					+ " before building it anew\n"
					+ "Stored it as " + path
					+ " instead.");
		} catch (Exception e) {
			new File(path).delete();
			e.printStackTrace();
			throw new FakeException("Error writing "
				+ path + ": " + e);
		}
	}

	public void copyJar(String inJar, JarOutputStream out, boolean verbose)
			throws Exception {
		File file = new File(inJar);
		InputStream input = new FileInputStream(file);
		JarInputStream in = new JarInputStream(input);

		JarEntry entry;
		while ((entry = in.getNextJarEntry()) != null) {
			String name = entry.getName();
			if (name.equals("META-INF/MANIFEST.MF")) {
				in.closeEntry();
				continue;
			}
			byte[] buf = Util.readStream(in);
			in.closeEntry();
			entry.setCompressedSize(-1);
			writeJarEntry(entry, out, buf);
		}
		in.close();
	}

	public void writeJarEntry(JarEntry entry, JarOutputStream out,
			byte[] buf) throws ZipException, IOException {
		try {
			out.putNextEntry(entry);
			out.write(buf, 0, buf.length);
			out.closeEntry();
		} catch (ZipException e) {
			String msg = e.getMessage();
			if (!msg.startsWith("duplicate entry: ")) {
				err.println("Error writing "
						+ entry.getName());
				throw e;
			}
			err.println("ignoring " + msg);
		}
	}

	public static void copyFile(String source, String target, File cwd)
			throws FakeException {
		if (target.equals(source))
			return;
		try {
			if (!target.startsWith("/"))
				target = cwd + "/" + target;
			if (!source.startsWith("/"))
				source = cwd + "/" + source;
			File parent = new File(target).getParentFile();
			if (!parent.exists())
				parent.mkdirs();
			OutputStream out = new FileOutputStream(target);
			InputStream in = new FileInputStream(source);
			byte[] buffer = new byte[1<<16];
			for (;;) {
				int len = in.read(buffer);
				if (len < 0)
					break;
				out.write(buffer, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			throw new FakeException("Could not copy "
				+ source + " to " + target + ": " + e);
		}
	}

	public static int compare(File source, File target) {
		if (source.length() != target.length())
			return target.length() > source.length() ? 1 : -1;
		int result = 0;
		try {
			InputStream sourceIn = new FileInputStream(source);
			InputStream targetIn = new FileInputStream(target);
			byte[] buf1 = new byte[1<<16];
			byte[] buf2 = new byte[1<<16];
			while (result == 0) {
				int len = sourceIn.read(buf1);
				if (len < 0)
					break;
				int off = 0, count = 0;
				while (len > 0 && count >= 0) {
					count = targetIn.read(buf2, off, len);
					off += count;
					len -= count;
				}
				if (count < 0) {
					result = 1;
					break;
				}
				for (int i = 0; i < off; i++)
					if (buf1[i] != buf2[i]) {
						result = (buf2[i] & 0xff)
							- (buf1[i] & 0xff);
						break;
					}
			}
			sourceIn.close();
			targetIn.close();
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Could not compare "
				+ source + " to " + target + ": " + e);
		}
	}

	// the parameter "file" is only used to set the cwd
	protected void execute(List<String> arguments, String file,
			boolean verbose) throws IOException, FakeException {
		execute(arguments, new File(file).getParentFile(), verbose);
	}

	protected void execute(String[] args, String file,
			boolean verbose) throws IOException, FakeException {
		execute(args, new File(file).getParentFile(), verbose);
	}

	protected void execute(List<String> arguments, File dir, boolean verbose)
			throws IOException, FakeException {
		String[] args = new String[arguments.size()];
		arguments.toArray(args);
		execute(args, dir, verbose);
	}

	protected void execute(boolean verbose, File dir, String... args) throws IOException, FakeException {
		execute(args, dir, verbose);
	}

	protected void execute(String[] args, File dir, boolean verbose)
			throws IOException, FakeException {
		if (verbose) {
			String output = "Executing:";
			for (int i = 0; i < args.length; i++)
				output += " '" + args[i] + "'";
			err.println(output);
		}

		if (args[0].endsWith(".py")) {
			String args0orig = args[0];
			args[0] = Util.makePath(dir, args[0]);
			if (executePython(args, out, err))
				return;
			if (verbose)
				err.println("Falling back to Python ("
					+ "Jython was not found in classpath)");
			args[0] = args0orig;
		}

		if (args[0].endsWith(".bsh")) {
			String args0orig = args[0];
			args[0] = Util.makePath(dir, args[0]);
			if (executeBeanshell(args, out, err))
				return;
			if (verbose)
				err.println("Falling back to calling it with BeanShell ("
					+ "bsh.jar was not found in classpath)");
			args[0] = args0orig;
		}

		/* stupid, stupid Windows... */
		if (Util.getPlatform().startsWith("win")) {
			// handle .sh scripts
			if (args[0].endsWith(".sh")) {
				String[] newArgs = new String[args.length + 1];
				newArgs[0] = "sh.exe";
				System.arraycopy(args, 0, newArgs, 1, args.length);
				args = newArgs;
			}
			for (int i = 0; i < args.length; i++)
				args[i] = quoteArg(args[i]);
			// stupid, stupid, stupid Windows taking all my time!!!
			if (args[0].startsWith("../"))
				args[0] = new File(dir,
						args[0]).getAbsolutePath();
			else if ((args[0].equals("bash") || args[0].equals("sh")) && Util.getPlatform().equals("win64")) {
				String[] newArgs = new String[args.length + 2];
				newArgs[0] = System.getenv("WINDIR") + "\\SYSWOW64\\cmd.exe";
				newArgs[1] = "/C";
				System.arraycopy(args, 0, newArgs, 2, args.length);
				args = newArgs;
				if (verbose) {
					String output = "Executing (win32 on win64 using SYSWOW64\\cmd.exe):";
					for (int i = 0; i < args.length; i++)
						output += " '" + args[i] + "'";
					err.println(output);
				}

			}
		}

		Process proc = Runtime.getRuntime().exec(args, null, dir);
		new StreamDumper(proc.getErrorStream(), err).start();
		new StreamDumper(proc.getInputStream(), out).start();
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new FakeException(e.getMessage());
		}
		int exitValue = proc.exitValue();
		if (exitValue != 0)
			throw new FakeException("Failed: " + exitValue);
	}

	private static String quotables = " \"\'";
	public static String quoteArg(String arg) {
		return quoteArg(arg, quotables);
	}

	public static String quoteArg(String arg, String quotables) {
		for (int j = 0; j < arg.length(); j++) {
			char c = arg.charAt(j);
			if (quotables.indexOf(c) >= 0) {
				String replacement;
				if (c == '"') {
					if (System.getenv("MSYSTEM") != null)
						replacement = "\\" + c;
					else
						replacement = "'" + c + "'";
				}
				else
					replacement = "\"" + c + "\"";
				arg = arg.substring(0, j)
					+ replacement
					+ arg.substring(j + 1);
				j += replacement.length() - 1;
			}
		}
		return arg;
	}

	// Jython initialization needs to be synchronized; use a simple String for that
	private static String dummyJython = "jython";
	protected static Constructor<?> jythonCreate;
	protected static Method jythonExec, jythonExecfile, jythonSetOut, jythonSetErr;

	protected static boolean executePython(String[] args, PrintStream out, PrintStream err) throws FakeException {
		synchronized (dummyJython) {
			if (jythonExecfile == null) try {
				discoverJython();
				ClassLoader loader = getClassLoader();
				Thread.currentThread().setContextClassLoader(loader);
				String className = "org.python.util.PythonInterpreter";
				Class<?> main = loader.loadClass(className);
				Class<?>[] argsType = new Class[] { };
				jythonCreate = main.getConstructor(argsType);
				argsType = new Class[] { args[0].getClass() };
				jythonExec = main.getMethod("exec", argsType);
				argsType = new Class[] { args[0].getClass() };
				jythonExecfile = main.getMethod("execfile", argsType);
				argsType = new Class[] { OutputStream.class };
				jythonSetOut = main.getMethod("setOut", argsType);
				jythonSetErr = main.getMethod("setErr", argsType);
			} catch (Exception e) {
				return false;
			}

			try {
				Object instance =
					jythonCreate.newInstance(new Object[] { });
				jythonSetOut.invoke(instance, new Object[] { out });
				jythonSetErr.invoke(instance, new Object[] { err });
				String init = "import sys\n" +
					"sys.argv = [";
				for (int i = 0; i < args.length; i++)
					init += (i > 0 ? ", " : "")
						+ "\"" + quoteArg(args[i], "\"") + "\"";
				init += "]\n";
				jythonExec.invoke(instance, new Object[] { init });
				String sysPath = "sys.path.insert(0, '"
					+ new File(args[0]).getParent() + "')";
				jythonExec.invoke(instance, new Object[] { sysPath });
				jythonExecfile.invoke(instance,
						new Object[] { args[0] });
			} catch (InvocationTargetException e) {
				e.getTargetException().printStackTrace();
				throw new FakeException("Jython failed");
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	protected static Constructor<?> bshCreate;
	protected static Method bshEvalString, bshEvalReader, bshSet;

	protected static boolean executeBeanshell(String[] args, PrintStream out, PrintStream err)
			throws FakeException {
		if (bshCreate == null) try {
			discoverBeanshell();
			ClassLoader loader = getClassLoader();
			String className = "bsh.Interpreter";
			Class<?> main = loader.loadClass(className);
			Class<?>[] argsType = new Class[] { Reader.class, PrintStream.class, PrintStream.class, boolean.class };
			bshCreate = main.getConstructor(argsType);
			argsType = new Class[] { String.class };
			bshEvalString = main.getMethod("eval", argsType);
			argsType = new Class[] { Reader.class };
			bshEvalReader = main.getMethod("eval", argsType);
			argsType = new Class[] { String.class, Object.class };
			bshSet = main.getMethod("set", argsType);
		} catch (Exception e) {
			return false;
		}

		try {
			Object instance =
				bshCreate.newInstance(new Object[] { (Reader)null, out, err, Boolean.FALSE });
			String path = args[0];
			String[] bshArgs = new String[args.length - 1];
			System.arraycopy(args, 1, bshArgs, 0, bshArgs.length);
			bshSet.invoke(instance, new Object[] { "bsh.args", bshArgs });
			bshEvalReader.invoke(instance, new Object[] { new FileReader(path) });
		} catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
			throw new FakeException("Beanshell failed");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	protected void fakeOrMake(File cwd, String directory, boolean verbose,
			boolean ignoreMissingFakefiles, String toolsPath,
			String classPath, String fallBackFakefile,
			File buildDir, String defaultTarget)
			throws FakeException {
		String[] files = new File(directory).list();
		if (files == null || files.length == 0)
			return;
		files = null;

		String fakeFile = cwd.getPath() + '/' + directory + '/' + Parser.path;
		boolean tryFake = new File(fakeFile).exists();
		if (!tryFake) {
			fakeFile = fallBackFakefile;
			tryFake = new File(fakeFile).exists();
		}
		if (ignoreMissingFakefiles && !tryFake &&
				!(new File(directory + "/Makefile").exists())) {
			if (verbose)
				err.println("Ignore " + directory);
			return;
		}
		err.println((tryFake ? "Build" : "Mak") + "ing in "
			+ directory + (directory.endsWith("/") ? "" : "/"));

		try {
			if (tryFake) {
				// Try "Fake"
				Parser parser = parseFakefile(new File(cwd, directory), new File(fakeFile), verbose, toolsPath, classPath, buildDir);
				Rule all = parser.parseRules(null);
				if (defaultTarget != null) {
					Rule rule = all.getRule(defaultTarget);
					if (rule != null)
						all = rule;
				}
				all.make();
			} else
				throw new FakeException("Make no longer supported!");
		} catch (Exception e) {
			if (!(e instanceof FakeException))
				e.printStackTrace();
			throw new FakeException((tryFake ?  "Fake" : "make")
				+ " failed: " + e);
		}
		err.println("Leaving " + directory);
	}

	protected Parser parseFakefile(File cwd, File fakefile, boolean verbose, String toolsPath, String classPath, File buildDir) throws FakeException {
		try {
			Parser parser = new Parser(this, new FileInputStream(fakefile), cwd);
			if (verbose)
				parser.setVariable("VERBOSE", "true");
			if (toolsPath != null)
				parser.variables.put("TOOLSPATH", parser.expandVariables(toolsPath));
			if (classPath != null)
				parser.variables.put("CLASSPATH", parser.expandVariables(classPath));
			else // let's not add all of Fiji's classes to SubFakes' classpaths by default
				parser.variables.remove("CLASSPATH");
			if (buildDir != null)
				parser.setVariable("BUILDDIR", buildDir.getAbsolutePath());
			parser.cwd = cwd;
			return parser;
		} catch (IOException e) {
			e.printStackTrace();
			throw new FakeException(e.getMessage());
		}
	}

	protected boolean jarUpToDate(String source, String target,
			boolean verbose) {
		JarFile targetJar, sourceJar;

		try {
			targetJar = new JarFile(target);
		} catch(IOException e) {
			if (verbose)
				err.println(target
						+ " does not exist yet");
			return false;
		}
		try {
			sourceJar = new JarFile(source);
		} catch(IOException e) {
			return true;
		}

		for (JarEntry entry : Collections.list(sourceJar.entries())) {
			JarEntry other =
				(JarEntry)targetJar.getEntry(entry.getName());
			if (other == null) {
				if (verbose)
					err.println(target
						+ " lacks the file "
						+ entry.getName());
				return false;
			}
			if (entry.getTime() > other.getTime()) {
				if (verbose)
					err.println(target + " is not "
						+ "up-to-date because of "
						+ entry.getName());
				return false;
			}
		}
		try {
			targetJar.close();
			sourceJar.close();
		} catch(IOException e) { }

		return true;
	}

	private static JarClassLoader classLoader;

	public static ClassLoader getClassLoader() throws IOException {
		return getClassLoader(null);
	}

	protected static ClassLoader getClassLoader(String jarFile)
			throws IOException {
		if (classLoader == null)
			classLoader = new JarClassLoader();
		if (jarFile != null && jarFile.endsWith(".jar")) {
			File file = new File(jarFile);
			if (!file.exists()) {
				final String baseName = Util.stripSuffix(file.getName(), ".jar");
				File[] list = file.getParentFile().listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						if (!name.startsWith(baseName) || !name.endsWith(".jar")) return false;
						final Matcher matcher = matchVersionedFilename(name);
						return matcher.matches() && matcher.group(1).equals(baseName);
					}
				});
				if (list != null && list.length > 0)
					jarFile = list[0].getPath();
			}
			classLoader.add(jarFile);
		}
		return classLoader;
	}

	public void deleteRecursively(File dir) {
		try {
			File[] list = dir.listFiles();
			if (list != null)
				for (int i = 0; i < list.length; i++) {
					if (list[i].isDirectory())
						deleteRecursively(list[i]);
					else
						Util.delete(list[i]);
				}
			Util.delete(dir);
		} catch (FakeException e) {
			out.println("Error: " + e.getMessage());
		}
	}

	public static String moveToUpdateDirectory(String path) throws FakeException {
		return new File(path).getAbsolutePath();
	}

	public static File moveToUpdateDirectory(File file) throws FakeException {
		String absolute = file.getAbsolutePath().replace('\\', '/');
		if (!absolute.startsWith(ijHome))
			throw new FakeException("The file " + file
					+ " could not be deleted!");
		int len = ijHome.length();
		File result = new File(absolute.substring(0, len)
			+ "update/" + absolute.substring(len));
		result.getParentFile().mkdirs();
		return result;
	}

	public String prefixPaths(File cwd, String pathList,
			boolean skipVariables) {
		if (pathList == null || pathList.equals(""))
			return pathList;
		String[] paths = Util.splitPaths(pathList);
		for (int i = 0; i < paths.length; i++)
			if (!skipVariables || !paths[i].startsWith("$"))
				paths[i] = Util.makePath(cwd, paths[i]);
		return Util.join(paths, ":");
	}

	public static String stripImageJHome(String string) {
		if (string == null)
			return string;
		String slashes = string.replace('\\', '/');
		if (slashes.startsWith(ijHome))
			return Util.stripPrefix(slashes, ijHome);
		return string;
	}
}
