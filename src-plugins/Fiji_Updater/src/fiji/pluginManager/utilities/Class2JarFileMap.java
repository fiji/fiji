package fiji.pluginManager.utilities;
import ij.IJ;
import ij.ImageJ;
import ij.Menus;

import java.io.File;
import java.io.IOException;

import java.util.Enumeration;
import java.util.HashMap;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Class2JarFileMap extends HashMap<String, String> {
	String fijiDirectory;

	public Class2JarFileMap() {
		fijiDirectory = getFijiDirectory();
		addDirectory("plugins");
		addDirectory("jars");
	}

	private String getFijiDirectory() {
		return PluginData.stripSuffix(PluginData.stripSuffix(Menus.getPlugInsPath(),
				File.separator), File.separator + "plugins").replace('\\', '/');
	}

	private void addDirectory(String directory) {
		File dir = new File(fijiDirectory + "/" + directory);
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
		JarFile file = new JarFile(fijiDirectory + "/" + jar);
		Enumeration entries = file.entries();
		while (entries.hasMoreElements()) {
			String name =
				((JarEntry)entries.nextElement()).getName();
			if (name.endsWith(".class"))
				addClass(PluginData.stripSuffix(name,
					".class").replace('/', '.'), jar);
		}
	}

	private void addClass(String className, String jar) {
		if (containsKey(className))
			IJ.log("Warning: class " + className + " was found both"
				+ " in " + get(className) + " and in " + jar);
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