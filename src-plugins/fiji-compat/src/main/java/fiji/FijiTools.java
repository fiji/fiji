package fiji;

import ij.IJ;

import java.awt.Frame;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.net.URL;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

public class FijiTools {
	/**
	 * Get the path of the Fiji directory
	 *
	 * @Deprecated
	 */
	public static String getFijiDir() {
		return getImageJDir();
	}

	public static String getImageJDir() {
		String path = System.getProperty("ij.dir");
		if (path != null)
			return path;
		final String prefix = "file:";
		final String suffix = "/jars/fiji-compat.jar!/fiji/FijiTools.class";
		path = fiji.FijiTools.class
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
			return new File(getImageJDir(), "ImageJ.c").exists();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean openStartupMacros() {
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
		return false;
	}

	public static boolean openEditor(File file, File templateFile) {
		try {
			Class<?> clazz = IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			Constructor<?> ctor = clazz.getConstructor(new Class[] { File.class, File.class });
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
			Class<?> clazz = IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			Constructor<?> ctor = clazz.getConstructor(new Class[] { String.class, String.class });
			Frame frame = (Frame)ctor.newInstance(new Object[] { title, body });
			frame.setVisible(true);
			return true;
		} catch (Exception e) {
			IJ.handleException(e);
		}

		try {
			Class<?> clazz = IJ.getClassLoader().loadClass("ij.plugin.frame.Editor");
			Constructor<?> ctor = clazz.getConstructor(new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE });
			Object ed = ctor.newInstance(new Object[] { 16, 60, 0, 3 });
			Method method = clazz.getMethod(title.endsWith(".ijm") ? "createMacro" : "create", new Class[] { String.class, String.class });
			method.invoke(ed, new Object[] { title, body });
			return true;
		} catch (Exception e) {
			IJ.handleException(e);
		}

		return false;
	}

	/**
	 * Calls the Fiji Script Editor for text files.
	 * 
	 * A couple of sanity checks are needed, e.g. that the script editor is in the class path
	 * and that it agrees that the file is binary, that there is no infinite loop ponging back
	 * and forth between the TextEditor's and the Opener's open() methods.
	 * 
	 * @param path the path to the candidate file
	 * @return whether we opened it in the script editor
	 */
	public static boolean maybeOpenEditor(String path) {
		try {
			Class<?> textEditor = ij.IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			if (path.indexOf("://") < 0 &&
					!getFileExtension(path).equals("") &&
					!((Boolean)textEditor.getMethod("isBinary", new Class[] { String.class }).invoke(null, path)).booleanValue() &&
					!stackTraceContains("fiji.scripting.TextEditor.open(") &&
					IJ.runPlugIn("fiji.scripting.Script_Editor", path) != null)
				return true;
		} catch (Throwable t) {
			t.printStackTrace();
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

	public static boolean stackTraceContains(String needle) {
		final StringWriter writer = new StringWriter();
		final PrintWriter out = new PrintWriter(writer);
		new Exception().printStackTrace(out);
		out.close();
		return writer.toString().indexOf(needle) >= 0;
	}

	public static boolean handleNoSuchMethodError(NoSuchMethodError error) {
		String message = error.getMessage();
		int paren = message.indexOf("(");
		if (paren < 0)
			return false;
		int dot = message.lastIndexOf(".", paren);
		if (dot < 0)
			return false;
		String path = message.substring(0, dot).replace('.', '/') + ".class";
		Set<String> urls = new LinkedHashSet<String>();
		try {
			Enumeration<URL> e = IJ.getClassLoader().getResources(path);
			while (e.hasMoreElements())
				urls.add(e.nextElement().toString());
			e = IJ.getClassLoader().getResources("/" + path);
			while (e.hasMoreElements())
				urls.add(e.nextElement().toString());
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}

		if (urls.size() == 0)
			return false;
		StringWriter writer = new StringWriter();
		error.printStackTrace(new PrintWriter(writer));
		StringBuffer buffer = writer.getBuffer();
		buffer.append("\nThe class ").append(message.substring(0, dot)).append(" can be found here:\n");
		for (String url : urls) {
			if (url.startsWith("jar:"))
				url = url.substring(4);
			if (url.startsWith("file:"))
				url = url.substring(5);
			int bang = url.indexOf("!");
			if (bang < 0)
				buffer.append(url);
			else
				buffer.append(url.substring(0, bang));
			buffer.append("\n");
		}
		if (urls.size() > 1)
			buffer.append("\nWARNING: multiple locations found!\n");
		IJ.log(buffer.toString());
		IJ.error("Could not find method " + message + "\n(See Log for details)\n");
		return true;
	}
}
