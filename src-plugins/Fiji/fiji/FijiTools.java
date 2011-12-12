package fiji;

import ij.IJ;

import java.awt.Frame;

import java.io.File;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


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

	public static boolean openStartupMacros() {
		try {
			File macros = new File(getFijiDir(), "macros");
			File txt = new File(macros, "StartupMacros.txt");
			File ijm = new File(macros, "StartupMacros.ijm");
			File fiji = new File(macros, "StartupMacros.fiji.ijm");
			if (txt.exists()) {
				if (openEditor(txt, fiji))
					return true;
			}
			else if (ijm.exists() || fiji.exists()) {
				if (openEditor(ijm, fiji))
					return true;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean openEditor(File file, File templateFile) {
		try {
			Class clazz = IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			Constructor ctor = clazz.getConstructor(new Class[] { File.class, File.class });
			Frame frame = (Frame)ctor.newInstance(new Object[] { file, templateFile });
			frame.setVisible(true);
			return true;
		} catch (Exception e) {
			IJ.handleException(e);
		}
		return false;
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

		try {
			Class clazz = IJ.getClassLoader().loadClass("ij.plugin.frame.Editor");
			Constructor ctor = clazz.getConstructor(new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE });
			Object ed = ctor.newInstance(new Object[] { 16, 60, 0, 3 });
			Method method = clazz.getMethod(title.endsWith(".ijm") ? "createMacro" : "create", new Class[] { String.class, String.class });
			method.invoke(ed, new Object[] { title, body });
			return true;
		} catch (Exception e) {
			IJ.handleException(e);
		}

		return false;
	}

	public static String getFileExtension(String path) {
		int dot = path.lastIndexOf('.');
		if (dot < 0)
			return "";
		int slash = path.lastIndexOf('/');
		int backslash = path.lastIndexOf('\\');
		if (dot < slash || dot < backslash)
			return "";
		return path.substring(dot + 1);
	}
}