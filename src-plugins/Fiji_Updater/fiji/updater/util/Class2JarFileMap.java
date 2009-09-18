package fiji.updater.util;

import ij.IJ;
import ij.ImageJ;

import java.io.File;
import java.io.IOException;

import java.util.Enumeration;
import java.util.HashMap;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Class2JarFileMap extends HashMap<String, String> {
	public Class2JarFileMap() {
		try {
			addJar("ij.jar");
			addJar("misc/Fiji.jar");
		} catch (IOException e) {
			e.printStackTrace();
		}
		addDirectory("plugins");
		addDirectory("jars");
	}

	private void addDirectory(String directory) {
		File dir = new File(Util.fijiRoot + "/" + directory);
		if (!dir.isDirectory())
			return;
		String[] list = dir.list();
		for (int i = 0; i < list.length; i++) {
			String path = directory + "/" + list[i];
			if (list[i].endsWith(".jar")) try {
				addJar(path);
			} catch (IOException e) {
				IJ.log("Warning: could not open " + path);
			}
			else
				addDirectory(path);
		}
	}

	private void addJar(String jar) throws IOException {
		JarFile file = new JarFile(Util.fijiRoot + "/" + jar);
		Enumeration entries = file.entries();
		while (entries.hasMoreElements()) {
			String name =
				((JarEntry)entries.nextElement()).getName();
			if (name.endsWith(".class"))
				addClass(Util.stripSuffix(name,
					".class").replace('/', '.'), jar);
		}
	}

	/*
	 * batik.jar contains these, for backwards compatibility, but we
	 * do not want to have batik.jar as a dependency for every XML
	 * handling plugin...
	 */
	private boolean ignore(String name, String jar) {
		if (jar.endsWith("/batik.jar"))
			return name.startsWith("org.xml.") ||
				name.startsWith("org.w3c.") ||
				name.startsWith("javax.xml.");
		return false;
	}

	private void addClass(String className, String jar) {
		if (ignore(className, jar))
			return;
		if (containsKey(className)) {
			if (!className.startsWith("com.sun.medialib.codec.") &&
					!className.startsWith("org.mozilla."))
				IJ.log("Warning: class " + className
						+ " was found both"
						+ " in " + get(className)
						+ " and in " + jar);
		}
		else
			put(className, jar);
	}

	public static void main(String[] args) {
		if (IJ.getInstance() == null)
			new ImageJ();

		Class2JarFileMap map = new Class2JarFileMap();

		if (args.length == 0)
			for (String className : map.keySet())
				System.out.println("class " + className
					+ " is in " + map.get(className));
		else
			for (int i = 0; i < args.length; i++)
				System.out.println("class " + args[i]
					+ " is in " + map.get(args[i]));
	}
}
