package fiji.ffmpeg;

import imagej.util.FileUtils;

import java.io.File;

public class CompileFFMPEG {
	public final static String FFMPEG_COMMIT = "6c3d021891a942403eb644eae0e6378a0dcf8b3c";

	public static void main(String[] args) {
		if (args.length != 1)
			throw new RuntimeException("Need the path to the target directory");
		File target = new File(args[0]);
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
		if (!configMak.exists() || configure.lastModified() > configMak.lastModified())
			exec(ffmpeg, "./configure", "--enable-gpl", "--enable-shared", "--extra-ldflags=-Wl,-rpath,\\\\\\$\\$\\$\\$ORIGIN/");
		exec(ffmpeg, "make", "V=1", "-j");

		// Generate the JNA wrapper classes
		File generatedSources = new File(target, "classes");
		if (!generatedSources.isDirectory())
			generatedSources.mkdirs();
		new GenerateFFMPEGClasses().generate(ffmpeg, generatedSources);
	}

	protected static void exec(File workingDirectory, String... args) {
		FileUtils.exec(workingDirectory, System.err, System.out, args);
	}
}
