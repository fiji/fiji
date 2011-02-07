package fiji;

import java.io.File;

import java.util.HashSet;
import java.util.Set;

/**
 * This class helps you with accessing libraries using JNI
 */
public class JNI {
	public static final String platform;
	public static final String libraryDirectory;
	public static final String libraryPrefix;
	public static final String libraryExtension;
	public static final String fallbackLibraryExtension;

	protected static Set<String> loadedLibraries = new HashSet<String>();

	static {
		String os = System.getProperty("os.name");
		boolean is64bit = System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux")) {
			platform = "linux" + (is64bit ? "64" : "32");
			libraryPrefix = "lib";
			libraryExtension = ".so";
			fallbackLibraryExtension = null;
		}
		else if (osName.equals("Mac OS X")) {
			platform = "macosx";
			libraryPrefix = "lib";
			libraryExtension = ".dylib";
			fallbackLibraryExtension = ".jnilib";
		}
		else if (osName.startsWith("Windows")) {
			platform = "win" + (is64bit ? "64" : "32");
			libraryPrefix = "";
			libraryExtension = ".dll";
			fallbackLibraryExtension = null;
		}
		else {
			System.err.println("Unknown platform: " + osName);
			platform = null;
			libraryPrefix = "lib";
			libraryExtension = ".so";
			fallbackLibraryExtension = null;
		}

		libraryDirectory = System.getProperty("fiji.dir") + "/lib/"
			+ (platform != null ? platform + "/" : "");
	}

	public static void loadLibrary(String name) {
		if (loadedLibraries.contains(name))
			return;
		File library = new File(libraryDirectory, libraryPrefix + name + libraryExtension);
		if (fallbackLibraryExtension != null && !library.exists())
			library = new File(libraryDirectory, libraryPrefix + name + fallbackLibraryExtension);
		if (library.exists())
			System.load(library.getAbsolutePath());
		else
			System.loadLibrary(name);
		loadedLibraries.add(name);
	}
}
