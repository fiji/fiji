package fiji;

import ij.IJ;
import ij.ImageJ;
import ij.Menus;

import java.io.File;
import java.io.IOException;

public class MainClassForDebugging {
	static int foo;
	static String className;

	public static void main(String args[]) {
		ImageJ ij = null;
		if (IJ.getInstance() == null) {
			ij = new ImageJ();
			ij.setTitle("Fiji (Debugging)");
		}
		try {
			className = findClassName(join(args));
			IJ.runPlugIn(className, "");
		} catch(Exception e) { e.printStackTrace(); }
		if (ij != null)
			ij.dispose();

	}

	public static String join(String[] args) {
		if (args.length == 0)
			return "";
		String result = args[0];
		for (int i = 1; i < args.length; i++)
			result += " " + args[i];
		return result;
	}

	public static String findClassName(String path) throws IOException {
		if (path.endsWith(".java"))
			path = path.substring(0, path.length() - 5);
		File pluginsPath =
			new File(Menus.getPlugInsPath()).getCanonicalFile();
		File file = new File(path).getCanonicalFile();
		path = file.getName();
		while ((file = file.getParentFile()) != null &&
				file.compareTo(pluginsPath) != 0)
			path = file.getName() + "." + path;
		return path;
	}
}
