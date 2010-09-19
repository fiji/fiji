package ffmpeg;

/*
 * Base class to handle loading the FFMPEG libraries.
 */

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import ij.IJ;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.URL;

import fiji.ffmpeg.AVCODEC;
import fiji.ffmpeg.AVFORMAT;
import fiji.ffmpeg.AVUTIL;

public class FFMPEG {
	protected AVUTIL AVUTIL;
	protected AVCODEC AVCODEC;
	protected AVFORMAT AVFORMAT;
	//SWScaleLibrary SWSCALE;

	public boolean loadFFMPEG() {
		return loadFFMPEG(true);
	}

	public static void showException(Throwable e) {
		CharArrayWriter charArray = new CharArrayWriter();
		PrintWriter writer = new PrintWriter(charArray);
		e.printStackTrace(writer);
		IJ.log(charArray.toString());
	}

	public boolean loadFFMPEG(boolean addSearchPath) {

		if (AVFORMAT != null)
			return true;

		if (addSearchPath && !addSearchPath())
			return false;

		try {
			AVUTIL = Native.loadLibrary("avutil", AVUTIL.class);
			AVCODEC = Native.loadLibrary("avcodec", AVCODEC.class);
			AVFORMAT = Native.loadLibrary("avformat", AVFORMAT.class);
		} catch (UnsatisfiedLinkError e) {
			showException(e);
			return false;
		}
		return true;
	}

	private boolean addSearchPath() {
		String platform = (IJ.isMacOSX() ? "macosx" :
				(IJ.isWindows() ? "win"
					+ (IJ.is64Bit() ? "64" : "32")
				 : "linux" + (IJ.is64Bit() ? "64" : "")));
		String extension = IJ.isMacOSX() ? "dylib" :
			IJ.isWindows() ? "dll" : "so";
		String[] libs = null;
		String[] versions = null;
		if (IJ.isMacOSX()) {
			libs = new String[] { "libffmpeg" };
		} else if (IJ.isWindows()) {
			libs = new String[] {
				"avutil", "avcodec", "avformat"
			};
			versions = new String[] { "-49", "-52", "-52" };
		} else {
			libs = new String[] {
				"libavutil", "libavcodec", "libavformat"
			};
		}

		String[] targets = new String[libs.length];
		for (int i = 0; i < libs.length; i++) {
			if (versions == null)
				targets[i] = libs[i] + "." + extension;
			else
				targets[i] = libs[i] + versions[i]
					+ "." + extension;
			libs[i] = "/" + platform + "/" + libs[i]
				+ "." + extension;
		}

		URL location = getClass().getResource(libs[0]);
		if (location == null) {
			String dir = IJ.getDirectory("imagej");
			if (dir == null)
				return false;
			System.setProperty("jna.library.path", dir);
			return true;
		}
		File tmp = getTempDirectory();
		if (tmp == null)
			return false;
		if (!copyTempFile(location, new File(tmp, targets[0])))
			return true;
		for (int i = 1; i < libs.length; i++)
			if (!copyTempFile(getClass().getResource(libs[i]),
					new File(tmp, targets[i])))
				return true;
		System.setProperty("jna.library.path", tmp.getAbsolutePath());
		if (IJ.isMacOSX()) {
			String[] names = { "util", "codec", "format" };
			for (int i = 0; i < names.length; i++)
				symlink("libffmpeg.dylib", tmp.getAbsolutePath()
						+ "/libav" + names[i]
						+ ".dylib");
		}
		return true;
	}

	protected static File getTempDirectory() {
		try {
			File tmp = File.createTempFile("ffmpeg", "");
			if (!tmp.delete() || !tmp.mkdirs())
				return null;
			tmp.deleteOnExit();
			return tmp;
		} catch (IOException e) {
			return null;
		}
	}

	protected static boolean copyTempFile(URL source, File target) {
		try {
			InputStream in = source.openStream();
			target.deleteOnExit();
			OutputStream out = new FileOutputStream(target);
			byte[] buffer = new byte[1<<16];
			for (;;) {
				int len = in.read(buffer);
				if (len < 0)
					break;
				out.write(buffer, 0, len);
			}
			in.close();
			out.close();
			return true;
		} catch (IOException e) {
			showException(e);
			return false;
		}
	}

	interface libc extends Library {
		int symlink(String source, String target);
	}

	protected static libc libc;

	public static int symlink(String source, String target) {
		if (libc == null)
			libc = (libc)Native.loadLibrary("c", libc.class);
		return libc.symlink(source, target);
	}
}
