package fiji.ffmpeg;

import imagej.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class CompileFFMPEG {
	public final static String FFMPEG_COMMIT = "6c3d021891a942403eb644eae0e6378a0dcf8b3c";

	protected static boolean isWindows, isLinux, isMacOSX;
	protected static boolean is64Bit;
	protected static String narSuffix;

	static {
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Win"))
			isWindows = true;
		else if (osName.startsWith("Linux"))
			isLinux = true;
		else if (osName.indexOf("OS X") > 0)
			isMacOSX = true;
		String arch = System.getProperty("os.arch");
		is64Bit = arch != null && arch.indexOf("64") >= 0;

		narSuffix = "-"
				+ (is64Bit ? (isMacOSX ? "x86_64" : "amd64") : (isWindows ? "x86" : "i386"))
				+ "-"
				+ (isWindows ? "Windows" : isMacOSX ? "MacOSX" : "Linux")
				+ "-gcc-shared";
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length != 2)
			throw new RuntimeException("Need the path to the target directory and ${artifactId}-${version}");
		File target = new File(args[0]);
		String artifactPrefix = args[1];
		if (!target.isDirectory())
			throw new RuntimeException("Invalid target directory: " + args[0]);

		// Make sure FFMPEG is checked out
		File ffmpeg = new File(target, "ffmpeg");
		if (!ffmpeg.exists())
			exec(target, "git", "clone", "git://fiji.sc/ffmpeg.git");
		if (!FileUtils.exec(ffmpeg, System.err, null, "git", "rev-parse", "HEAD").equals(FFMPEG_COMMIT + "\n"))
			exec(ffmpeg, "git", "checkout", FFMPEG_COMMIT);

		// Configure & make FFMPEG
		File configure = new File(ffmpeg, "configure");
		File configMak = new File(ffmpeg, "config.mak");
		String mOption = "-m" + (is64Bit ? "64" : "32");
		if (configMak.exists() && !narSuffix.equals(getNarSuffixOfLastBuild(ffmpeg))) {
			exec(ffmpeg, "make", "clean");
			configMak.delete();
		}
		if (!configMak.exists() || configure.lastModified() > configMak.lastModified()) {
			exec(ffmpeg, "./configure",
					"--enable-gpl",
					"--enable-shared",
					"--extra-ldflags=" + mOption + " -Wl,-rpath,\\\\\\$\\$\\$\\$ORIGIN/",
					"--extra-cflags=" + mOption);
			writeNarSuffix(ffmpeg);
		}
		exec(ffmpeg, "make", "V=1", "-j");

		// Compile our helper
		File helperDir = new File(ffmpeg, "libavlog");
		if (!helperDir.exists())
			helperDir.mkdirs();
		File helper = new File(target, "../src/other/c/avlog.c");
		exec(ffmpeg, "gcc", "-fPIC", "-shared", "-I.", "-Wl,-rpath,$ORIGIN/", "-o", "libavlog/libavlog.so.0", helper.getAbsolutePath());
		PrintStream out = new PrintStream(new FileOutputStream(new File(helperDir, "avlog.h")));
		out.println("#define LIBAVLOG_VERSION_MAJOR 0");
		out.println("void avSetLogCallback(void (*callback)(const char *line));");

		// Generate the JNA wrapper classes
		File generatedSources = new File(target, "classes");
		if (!generatedSources.isDirectory())
			generatedSources.mkdirs();
		GenerateFFMPEGClasses generator = new GenerateFFMPEGClasses();
		generator.generate(ffmpeg, generatedSources);

		// Pretend to be NAR
		File jarFile = new File(target, artifactPrefix + narSuffix + ".nar");
		byte[] buffer = new byte[65536];
		JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile));
		for (String lib : new String[] { "avutil", "avcore", "avdevice", "swscale", /* "avfilter", */ "avcodec", "avformat", "avlog" }) {
			String version = generator.majorVersions.get("LIB" + lib.toUpperCase() + "_VERSION_MAJOR");
			String fileName;
			if (isWindows)
				fileName = lib + (version == null ? "" : "-" + version) + ".dll";
			else
				fileName = "lib" + lib + (isMacOSX ? ".dylib" : ".so") + (version == null ? "" : "." + version);
			JarEntry entry = new JarEntry(fileName);
			jar.putNextEntry(entry);
			FileInputStream in = new FileInputStream(new File(ffmpeg, "lib" + lib + "/" + fileName));
			for (;;) {
				int count = in.read(buffer);
				if (count < 0)
					break;
				jar.write(buffer, 0, count);
			}
			in.close();
			jar.closeEntry();
		}
		jar.close();
		System.err.println("Wrote " + jarFile);
	}

	private static String getNarSuffixOfLastBuild(final File ffmpeg) throws IOException {
		final File file = new File(ffmpeg, ".nar-suffix");
		if (!file.exists())
			return null;
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		final String line = reader.readLine();
		reader.close();
		return line;
	}

	private static void writeNarSuffix(final File ffmpeg) throws FileNotFoundException {
		PrintStream out = new PrintStream(new FileOutputStream(new File(ffmpeg, ".nar-suffix")));
		out.print(narSuffix);
		out.close();
	}

	protected static void exec(File workingDirectory, String... args) {
		FileUtils.exec(workingDirectory, System.err, System.out, args);
	}
}
