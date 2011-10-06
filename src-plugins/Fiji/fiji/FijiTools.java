package fiji;

import ij.IJ;

import java.awt.Frame;

import java.io.File;

import java.lang.reflect.Constructor;

public class FijiTools {
	public static String getFijiDir() throws ClassNotFoundException {
		String path = System.getProperty("fiji.dir");
		if (path != null)
			return path;
		final String prefix = "file:";
		final String suffix = "/jars/Fiji.jar!/fiji/FijiTools.class";
		path = Class.forName("fiji.FijiTools")
			.getResource("FijiTools.class").getPath();
		if (path.startsWith(prefix))
			path = path.substring(prefix.length());
		if (path.endsWith(suffix))
			path = path.substring(0,
				path.length() - suffix.length());
		return path;
	}

	public static boolean isFijiDeveloper() {
		try {
			return new File(getFijiDir(), "fiji.c").exists();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean openEditor(String title, String body) {
		try {
			Class clazz = IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			Constructor ctor = clazz.getConstructor(new Class[] { String.class, String.class });
			Frame frame = (Frame)ctor.newInstance(new Object[] { title, body });
			frame.setVisible(true);
			return true;
		} catch (Exception e) {
			IJ.handleException(e);
		}
		return false;
	}
}