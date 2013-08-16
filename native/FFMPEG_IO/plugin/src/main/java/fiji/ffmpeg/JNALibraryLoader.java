package fiji.ffmpeg;

import ij.IJ;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class JNALibraryLoader {
	protected static File libraryDirectory;
	protected static String baseURL;

	public JNALibraryLoader() {
		if (baseURL == null) {
			String classFile = "fiji/ffmpeg/JNALibraryLoader.class";
			URL url = getClass().getResource("/" + classFile);
			if (url != null) {
				String string = url.toString();
				if (string.endsWith(classFile))
					baseURL = string.substring(0, string.length() - classFile.length());
			}
		}
		if (libraryDirectory == null)
			libraryDirectory = new File(new File(System.getProperty("ij.dir"), "lib"), getPlatform());

	}

	public static void showException(Throwable e) {
		CharArrayWriter charArray = new CharArrayWriter();
		PrintWriter writer = new PrintWriter(charArray);
		e.printStackTrace(writer);
		IJ.log(charArray.toString());
	}

	protected static String getPlatform() {
		return IJ.isMacOSX() ? "macosx" :
			(IJ.isWindows() ? "win"
				+ (IJ.is64Bit() ? "64" : "32")
			 : "linux" + (IJ.is64Bit() ? "64" : "32"));
	}

	protected static String getLibraryName(String name, int version) {
		if (IJ.isWindows())
			return name + "-" + version + ".dll";
		return "lib" + name + "." + (IJ.isMacOSX() ? "dylib" : "so") + "." + version;
	}

	protected static File getTempLibraryDirectory() {
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

	@SuppressWarnings("unchecked")
	protected<T> T loadLibrary(String name, int version, Class<T> libraryClass) {
		String fileName = getLibraryName(name, version);
		File file = new File(libraryDirectory, fileName);
		if (!file.exists()) {
			if (baseURL == null)
				throw new RuntimeException("Could not determine .jar");
			try {
				copy(new URL(baseURL + getPlatform() + "/" + fileName), file);
			} catch (Exception e) {
				throw new RuntimeException("Could not extract " + fileName + ": " + e);
			}
		}

		NativeLibrary.addSearchPath(name, libraryDirectory.getAbsolutePath());
		return (T)Native.loadLibrary(name, libraryClass);
	}

	protected static boolean copy(URL source, File target) {
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

	protected interface libc extends Library {
		int symlink(String source, String target);
	}

	protected static libc libc;

	public static int symlink(String source, String target) {
		if (libc == null)
			libc = (libc)Native.loadLibrary("c", libc.class);
		return libc.symlink(source, target);
	}
}
