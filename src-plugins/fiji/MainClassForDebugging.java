package fiji;

import ij.IJ;
import ij.ImageJ;
import ij.Menus;

import java.io.File;

public class MainClassForDebugging {
	static int foo;
	static String className;

	public static void main(String args[]) {
		if (IJ.getInstance() == null)
			new ImageJ();
		String path = "";
		int i;
		for (i = 0; i < args.length - 1; i++)
			path += args[i] + " ";
		path += args[i];
		className = findClassName(path);
		try {
			IJ.runPlugIn(className, "");
		} catch(Exception e) { e.printStackTrace(); }
		IJ.getInstance().dispose();

	}

	public static String findClassName(String path) {
		String c1 = path;
		if (path.endsWith(".java"))
			path = path.substring(0, path.length() - 5);
		String pluginsPath = Menus.getPlugInsPath();
		File f1 = new File(pluginsPath);
		if (!pluginsPath.endsWith(File.separator))
			pluginsPath += File.separator;
		boolean check = false;
		for (;;) {
			int lastSlash = c1.lastIndexOf(File.separator);
			if (lastSlash < 0)
				break;
			if (new File(c1).equals(f1)) {
				check = true;
				break;
			}
			c1 = c1.substring(0, lastSlash);
		}
		if (check) {
			path = path.substring(c1.length());
			while (path.startsWith(File.separator))
				path = path.substring(1);
		}
		return path.replace('/', '.') ;
	}
}
