package fiji.scripting;

import fiji.FijiClassLoader;

import ij.IJ;
import ij.Macro;

import ij.plugin.PlugIn;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

public class Script_Editor implements PlugIn {

	public void run(String path) {
		addToolsJarToClassPath();
		if (path == null || path.equals(""))
			path = Macro.getValue(Macro.getOptions(), "path", null);
		if (path == null)
			path = Macro.getValue(Macro.getOptions(), "open", null);
		new TextEditor(path);
	}

	final private static String gitwebURL =
		"http://pacific.mpi-cbg.de/cgi-bin/gitweb.cgi?p=java/";

	public static String getPlatform() {
		boolean is64bit =
			System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "-amd64" : "");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32");
		System.err.println("Unknown platform: " + osName);
		return osName;
	}

	private String getNewestJavaSubdirectory(String baseURL) {
		try {
			URL url = new URL(baseURL + ";a=tree");
			InputStream input = url.openStream();
			int off = 0, len = 16384;
			byte[] buffer = new byte[len];
			for (;;) {
				int count = input.read(buffer, off, len);
				if (count < 0)
					break;
				off += count;
				len -= count;
			}
			final String content = new String(buffer, 0, off);
			final String key = ";a=tree;f=";
			off = content.indexOf(key);
			if (off >= 0) {
				off += key.length();
				int end = content.indexOf(';', off);
				return content.substring(off, end);
			}
		} catch (IOException e) { e.printStackTrace(); }
		return "";
	}

	public void addToolsJarToClassPath() {
		try {
			if (Class.forName("com.sun.jdi.VirtualMachine") != null)
				return;
		} catch (ClassNotFoundException e) { }

		try {
			// make sure it is a FijiClassLoader
			FijiClassLoader loader =
				(FijiClassLoader)IJ.getClassLoader();

			String baseURL = gitwebURL + getPlatform() + ".git";
			URL url = new URL(baseURL + ";a=blob_plain;f="
				+ getNewestJavaSubdirectory(baseURL)
				+ "/lib/tools.jar");
			URL[] urls = new URL[] { url };
			loader.addFallBack(new URLClassLoader(urls));
			return;
		} catch (Exception e) { e.printStackTrace(); }
		//TODO: disable debug
		IJ.showStatus("Could not find debugging support library");
	}
}
