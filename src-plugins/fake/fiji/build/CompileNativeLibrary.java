package fiji.build;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CompileNativeLibrary extends Rule {
	public static final String fijiDir;
	public static final boolean hostIs64Bit;
	public static final String hostPlatform;
	public static final String hostLibraryDirectory;
	public static final String hostLibraryPrefix;
	public static final String hostLibraryExtension;

	static {
		fijiDir = System.getProperty("fiji.dir");
		String os = System.getProperty("os.name");
		hostIs64Bit = System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux")) {
			hostPlatform = "linux" + (hostIs64Bit ? "64" : "32");
			hostLibraryPrefix = "lib";
			hostLibraryExtension = ".so";
		}
		else if (osName.equals("Mac OS X")) {
			hostPlatform = "macosx";
			hostLibraryPrefix = "lib";
			hostLibraryExtension = ".dylib";
		}
		else if (osName.startsWith("Windows")) {
			hostPlatform = "win" + (hostIs64Bit ? "64" : "32");
			hostLibraryPrefix = "";
			hostLibraryExtension = ".dll";
		}
		else {
			String arch = hostIs64Bit ? "64" : "32";
			hostPlatform = osName.toLowerCase() + (osName.endsWith(arch) ? "" : arch);
			hostLibraryPrefix = "lib";
			hostLibraryExtension = ".so";
		}

		hostLibraryDirectory = getLibraryDirectory(hostPlatform);
	}

	public static String getLibraryDirectory(String platform) {
		return fijiDir + "/lib/" + (platform != null ? platform + "/" : "");
	}

	public static String makeTargetPath(String libraryBaseName, String cwd) {
		String result = hostLibraryDirectory + hostLibraryPrefix + libraryBaseName + hostLibraryExtension;
		if (!cwd.endsWith("/"))
			cwd += "/";
		if (result.startsWith(cwd))
			result = result.substring(cwd.length());
		return result;
	}

	protected boolean linkCPlusPlus = false;
	protected String libraryBaseName;
	protected File jarFile, buildDir;
	protected String platform;

	/**
	 * A rule for compiling native libraries.
	 *
	 * @param parser the enclosing Fakefile parser
	 * @param libraryBaseName the baseName (without the "lib" prefix and the file extension
	 * @param jarFile the jarFile to scan for native methods
	 * @param buildDir the directory into which the intermediate files should go
	 * @param prerequisites a list of .h, .c, .cxx or .cpp files to consider for compilation
	 */
	public CompileNativeLibrary(Parser parser, String libraryBaseName, File jarFile, File buildDir, List<String> prerequisites) {
		this(parser, libraryBaseName, makeTargetPath(libraryBaseName, parser.cwd.getAbsolutePath()), hostPlatform, jarFile, buildDir, prerequisites);
	}

	/**
	 * A rule for compiling native libraries.
	 *
	 * Use this version of the constructor if you want to cross-compile.
	 *
	 * @param parser the enclosing Fakefile parser
	 * @param libraryBaseName the baseName (without the "lib" prefix and the file extension
	 * @param target the path to the library to be generated
	 * @param platform the platform for which to compile (e.g. "win32")
	 * @param jarFile the jarFile to scan for native methods
	 * @param buildDir the directory into which the intermediate files should go
	 * @param prerequisites a list of .h, .c, .cxx or .cpp files to consider for compilation
	 */
	public CompileNativeLibrary(Parser parser, String libraryBaseName, String target, String platform, File jarFile, File buildDir, List<String> prerequisites) {
		super(parser, target, prerequisites);

		this.platform = platform;
		this.libraryBaseName = libraryBaseName;
		this.jarFile = jarFile;
		this.buildDir = buildDir;

		for (String prereq : prerequisites)
			if (isCxx(prereq)) {
				linkCPlusPlus = true;
				break;
			}
	}

	protected static List<String> filterNativeSources(List<String> fileNames) {
		List<String> result = new ArrayList<String>();
		for (String fileName : fileNames)
			if (isSource(fileName) || isHeader(fileName))
				result.add(fileName);
		return result;
	}

	protected static boolean isCxx(String name) {
		return name.endsWith(".cxx") || name.endsWith(".cpp");
	}

	protected static boolean isSource(String name) {
		return name.endsWith(".c") || isCxx(name);
	}

	protected static boolean isHeader(String name) {
		return name.endsWith(".h") || name.endsWith(".hxx") || name.endsWith(".hpp");
	}

	protected File createTempDirectory() throws IOException {
		File result = File.createTempFile("jni-dir-", "");
		result.delete();
		result.mkdirs();
		return result;
	}

	@Override
	public void action() throws FakeException {
		File tempDirectory = null;
		if (buildDir == null) try {
			tempDirectory = buildDir = createTempDirectory();
		} catch (IOException e) {
			throwException("Could not create temporary directory", e);
		}
		File libDir = new File(target).getParentFile();
		if (!libDir.isDirectory() && !libDir.mkdirs())
			throw new FakeException("Could not make directory for " + target);

		List<String> arguments = new ArrayList<String>();
		arguments.add(linkCPlusPlus ? gxx() : gcc());
		addFlags(arguments);
		try {
			javah(jarFile, arguments);
		} catch (IOException e) {
			throwException("Could not run JavaH", e);
		}
		addSources(arguments);
		arguments.add("-o");
		arguments.add(target);
		addLibs(arguments);
		try {
			parser.fake.execute(arguments, parser.cwd, getVarBool("VERBOSE"));
		} catch (IOException e) {
			throwException("Could not run '" + Util.join(arguments, "' '") + "'", e);
		}
		if (tempDirectory != null)
			parser.fake.deleteRecursively(tempDirectory);
	}

	protected void throwException(String message, Throwable t) throws FakeException {
		if (getVarBool("VERBOSE"))
			t.printStackTrace(parser.fake.err);
		throw new FakeException(message + ": " + t.getMessage());
	}

	protected void addFlags(List<String> arguments) throws FakeException {
		if (platform.equals("win32")) {
			arguments.add("-shared");
			arguments.add("-m32");
		}
		else if (platform.equals("win64")) {
			arguments.add("-shared");
			arguments.add("-m64");
		}
		else if (platform.equals("linux") || platform.equals("linux32")) {
			arguments.add("-shared");
			arguments.add("-fPIC");
			arguments.add("-Wl,-rpath,$ORIGIN/");
			arguments.add("-m32");
		}
		else if (platform.equals("linux64")) {
			arguments.add("-shared");
			arguments.add("-fPIC");
			arguments.add("-Wl,-rpath,$ORIGIN/");
			arguments.add("-m64");
		}
		else if (platform.equals("macosx")) {
			arguments.add("-arch");
			arguments.add("i386");
			arguments.add("-arch");
			arguments.add("x86_64");
			arguments.add("-bundle");
			arguments.add("-dynamic");
			arguments.add("-Wl,-rpath,$ORIGIN/");
		}

		if (getVarBool("DEBUG"))
			arguments.add("-g");
		String value = getVar("CFLAGS");
		if (value != null)
			arguments.addAll(Util.splitCommandLine(value));
		value = getVar("LDFLAGS");
		if (value != null)
			arguments.addAll(Util.splitCommandLine(value));

		Set<String> includeDirs = new HashSet<String>();
		includeDirs.add(fijiDir + "/includes");
		includeDirs.add(buildDir.getAbsolutePath());
		for (String prereq : prerequisites)
			if (isHeader(prereq))
				includeDirs.add(new File(Util.makePath(parser.cwd, prereq)).getParent());

		for (String dir : includeDirs) {
			arguments.add("-I");
			arguments.add(dir);
		}

		// MacOSX' linker does not like a space after "-L"
		arguments.add("-L" + getLibraryDirectory(platform));
	}

	protected void javah(File jarFile, List<String> arguments) throws FakeException, IOException {
		if (jarFile == null)
			return;
		JarFile jar = new JarFile(jarFile);
		for (JarEntry entry : Collections.list(jar.entries())) {
			String name = entry.getName();
			if (name.endsWith(".class")) {
				String className = name.substring(0, name.length() - 6).replace('/', '.');
				byte[] buffer = Util.readStream(jar.getInputStream(entry));
				if (containsNativeMethods(buffer))
					javah(jarFile, className, arguments);
			}
		}
	}

	/* We cannot use a class loader here because the .jar might need any number of dependencies */
	protected boolean containsNativeMethods(byte[] buffer) {
		ByteCodeAnalyzer analyzer = new ByteCodeAnalyzer(buffer, true);
		return analyzer.hasNativeMethods();
	}

	protected void javah(File jarFile, String className, List<String> arguments) throws FakeException, IOException {
		parser.fake.execute(getVarBool("VERBOSE"), parser.cwd,
			System.getProperty("fiji.executable"),
			"--javah", "-classpath", jarFile.getAbsolutePath(),
			"-d", buildDir.getAbsolutePath(), className);
		arguments.add("-I");
		arguments.add(buildDir.getAbsolutePath());
	}

	protected void addSources(List<String> arguments) {
		for (String prereq : prerequisites)
			if (isSource(prereq))
				arguments.add(Util.makePath(parser.cwd, prereq));
	}

	protected void addLibs(List<String> arguments) throws FakeException {
		String libs = getVar("LIBS");
		if (libs != null)
			arguments.addAll(Util.splitCommandLine(libs));
	}

	@Override
	public String getVar(String key, String subkey) {
		String result = parser.getVariable(key + "(" + libraryBaseName + ")");
		if (result != null)
			return result;
		if (jarFile != null) {
			result = parser.getVariable(key + "(" + jarFile.getName() + ")");
			if (result != null)
				return result;
		}
		return super.getVar(key, subkey);
	}

	protected String gcc() {
		return getenv("CC", "gcc");
	}

	protected String gxx() {
		return getenv("CXX", "g++");
	}

	protected String getenv(String key, String fallback) {
		String value = System.getenv(key);
		return value == null ? fallback : value;
	}
}
