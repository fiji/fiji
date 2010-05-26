package fiji.scripting;

import ij.IJ;

import ij.gui.GenericDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.JOptionPane;

public class FileFunctions {
	protected TextEditor parent;

	public FileFunctions(TextEditor parent) {
		this.parent = parent;
	}

	public List<String> extractSourceJar(String path) throws IOException {
		String baseName = new File(path).getName();
		if (baseName.endsWith(".jar") || baseName.endsWith(".zip"))
			baseName = baseName.substring(0, baseName.length() - 4);
		String baseDirectory = System.getProperty("fiji.dir")
			+ "/src-plugins/" + baseName + "/";

		List<String> result = new ArrayList<String>();
		JarFile jar = new JarFile(path);
		for (JarEntry entry : Collections.list(jar.entries())) {
			String name = entry.getName();
			if (name.endsWith(".class") || name.endsWith("/"))
				continue;
			String destination = baseDirectory + name;
			copyTo(jar.getInputStream(entry), destination);
			result.add(destination);
		}
		return result;
	}

	protected void copyTo(InputStream in, String destination)
			throws IOException {
		File file = new File(destination);
		makeParentDirectories(file);
		copyTo(in, new FileOutputStream(file));
	}

	protected void copyTo(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[16384];
		for (;;) {
			int count = in.read(buffer);
			if (count < 0)
				break;
			out.write(buffer, 0, count);
		}
		in.close();
		out.close();
	}

	protected void makeParentDirectories(File file) {
		File parent = file.getParentFile();
		if (!parent.exists()) {
			makeParentDirectories(parent);
			parent.mkdir();
		}
	}

	/*
	 * This just checks for a NUL in the first 1024 bytes.
	 * Not the best test, but a pragmatic one.
	 */
	public boolean isBinaryFile(String path) {
		try {
			InputStream in = new FileInputStream(path);
			byte[] buffer = new byte[1024];
			int offset = 0;
			while (offset < buffer.length) {
				int count = in.read(buffer, offset, buffer.length - offset);
				if (count < 0)
					break;
				else
					offset += count;
			}
			in.close();
			while (offset > 0)
				if (buffer[--offset] == 0)
					return true;
		} catch (IOException e) { }
		return false;
	}

	protected static String fijiDir;

	/**
	 * Make a sensible effort to get the path of the source for a class.
	 */
	public String getSourcePath(String className) throws ClassNotFoundException {
		if (fijiDir == null)
			fijiDir = System.getProperty("fiji.dir");

		// First, let's try to get the .jar file for said class.
		String result = getJar(className);
		if (result == null)
			return findSourcePath(className);

		// try the simple thing first
		int slash = result.lastIndexOf('/'), backSlash = result.lastIndexOf('\\');
		String baseName = result.substring(Math.max(slash, backSlash) + 1, result.length() - 4);
		String dir = fijiDir + "/src-plugins/" + baseName;
		String path = dir + "/" + className.replace('.', '/') + ".java";
		if (new File(path).exists())
			return path;
		if (new File(dir).isDirectory())
			for (;;) {
				int dot = className.lastIndexOf('.');
				if (dot < 0)
					break;
				className = className.substring(0, dot);
				path = dir + "/" + className.replace('.', '/') + ".java";
			}

		return null;
	}

	public String getJar(String className) {
		try {
			Class clazz = Class.forName(className);
			String baseName = className;
			int dot = baseName.lastIndexOf('.');
			if (dot > 0)
				baseName = baseName.substring(dot + 1);
			baseName += ".class";
			String url = clazz.getResource(baseName).toString();
			int dotJar = url.indexOf("!/");
			if (dotJar < 0)
				return null;
			int offset = url.startsWith("jar:file:") ? 9 : 0;
			return url.substring(offset, dotJar);
		} catch (Exception e) {
			return null;
		}
	}

	protected static Map<String, List<String>> class2source;

	public String findSourcePath(String className) {
		if (class2source == null) {
			if (JOptionPane.showConfirmDialog(parent,
					"The class " + className + " was not found "
					+ "in the CLASSPATH. Do you want me to search "
					+ "for the source?",
					"Question", JOptionPane.YES_OPTION)
					!= JOptionPane.YES_OPTION)
				return null;
			if (fijiDir == null)
				fijiDir = System.getProperty("fiji.dir");
			class2source = new HashMap<String, List<String>>();
			findJavaPaths(new File(fijiDir), "");
		}
		int dot = className.lastIndexOf('.');
		String baseName = className.substring(dot + 1);
		List<String> paths = class2source.get(baseName);
		if (paths == null || paths.size() == 0) {
			JOptionPane.showMessageDialog(parent, "No source for class '"
					+ className + "' was not found!");
			return null;
		}
		if (dot >= 0) {
			String suffix = "/" + className.replace('.', '/') + ".java";
			paths = new ArrayList<String>(paths);
			Iterator<String> iter = paths.iterator();
			while (iter.hasNext())
				if (!iter.next().endsWith(suffix))
					iter.remove();
			if (paths.size() == 0) {
				JOptionPane.showMessageDialog(parent, "No source for class '"
						+ className + "' was not found!");
				return null;
			}
		}
		if (paths.size() == 1)
			return fijiDir + "/" + paths.get(0);
		String[] names = paths.toArray(new String[paths.size()]);
		GenericDialog gd = new GenericDialog("Choose path", parent);
		gd.addChoice("path", names, names[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return null;
		return fijiDir + "/" + gd.getNextChoice();
	}

	protected void findJavaPaths(File directory, String prefix) {
		String[] files = directory.list();
		if (files == null)
			return;
		Arrays.sort(files);
		for (int i = 0; i < files.length; i++)
			if (files[i].endsWith(".java")) {
				String baseName = files[i].substring(0, files[i].length() - 5);
				List<String> list = class2source.get(baseName);
				if (list == null) {
					list = new ArrayList<String>();
					class2source.put(baseName, list);
				}
				list.add(prefix + "/" + files[i]);
			}
			else if ("".equals(prefix) &&
					(files[i].equals("full-nightly-build") ||
					 files[i].equals("livecd") ||
					 files[i].equals("java") ||
					 files[i].equals("nightly-build") ||
					 files[i].equals("other") ||
					 files[i].equals("work") ||
					 files[i].startsWith("chroot-")))
				// skip known non-source directories
				continue;
			else {
				File file = new File(directory, files[i]);
				if (file.isDirectory())
					findJavaPaths(file, prefix + "/" + files[i]);
			}
	}

	public boolean newPlugin() {
		GenericDialog gd = new GenericDialog("New Plugin");
		gd.addStringField("Plugin_name", "", 30);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		String name = gd.getNextString();
		if (!newPlugin(name))
			return false;
		return true;
	}

	public boolean newPlugin(String name) {
		String originalName = name;

		name = name.replace(' ', '_');
		if (name.indexOf('_') < 0)
			name += "_";

		File file = new File(System.getProperty("fiji.dir")
			+ "/src-plugins/" + name + "/" + name + ".java");
		File dir = file.getParentFile();
		if ((!dir.exists() && !dir.mkdirs()) || !dir.isDirectory())
			return error("Could not make directory '"
				+ dir.getAbsolutePath() + "'");

		String jar = "plugins/" + name + ".jar";
		addToGitignore(jar);
		addPluginJarToFakefile(jar);

		File pluginsConfig = new File(dir, "plugins.config");
		parent.open(pluginsConfig.getAbsolutePath());
		if (parent.getEditorPane().getDocument().getLength() == 0)
			parent.getEditorPane().insert(
				"# " + originalName + "\n"
				+ "\n"
				+ "# Author: \n"
				+ "\n"
				+ "Plugins, \"" + originalName + "\", " + name + "\n", 0);
		parent.open(file.getAbsolutePath());
		if (parent.getEditorPane().getDocument().getLength() == 0)
			parent.getEditorPane().insert(
				"import ij.ImagePlus;\n"
				+ "\n"
				+ "import ij.plugin.filter.PlugInFilter;\n"
				+ "\n"
				+ "import ij.process.ImageProcessor;\n"
				+ "\n"
				+ "public class " + name + " implements PlugInFilter {\n"
				+ "\tImagePlus image;\n"
				+ "\n"
				+ "\tpublic int setup(String arg, ImagePlus image) {\n"
				+ "\t\tthis.image = image;\n"
				+ "\t\treturn DOES_ALL;\n"
				+ "\t}\n"
				+ "\n"
				+ "\tpublic void run(ImageProcessor ip) {\n"
				+ "\t\t// Do something\n"
				+ "\t}\n"
				+ "}", 0);
		return true;
	}

	public boolean addToGitignore(String name) {
		if (!name.startsWith("/"))
			name = "/" + name;
		if (!name.endsWith("\n"))
			name += "\n";

		File file = new File(System.getProperty("fiji.dir"), ".gitignore");
		if (!file.exists())
			return false;

		try {
			String content = readStream(new FileInputStream(file));
			if (content.startsWith(name) || content.indexOf("\n" + name) >= 0)
				return false;
	
			FileOutputStream out = new FileOutputStream(file, true);
			if (!content.endsWith("\n"))
				out.write("\n".getBytes());
			out.write(name.getBytes());
			out.close();
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return error("Failure writing " + file);
		}
	}

	public boolean addPluginJarToFakefile(String name) {
		File file = new File(System.getProperty("fiji.dir"), "Fakefile");
		if (!file.exists())
			return false;

		try {
			String content = readStream(new FileInputStream(file));
			int start = content.indexOf("\nPLUGIN_TARGETS=");
			if (start < 0)
				return false;
			int end = content.indexOf("\n\n", start);
			if (end < 0)
				end = content.length();
			int offset = content.indexOf("\n\t" + name, start);
			if (offset < end && offset > start)
				return false;
	
			FileOutputStream out = new FileOutputStream(file);
			out.write(content.substring(0, end).getBytes());
			if (content.charAt(end - 1) != '\\')
				out.write(" \\".getBytes());
			out.write("\n\t".getBytes());
			out.write(name.getBytes());
			out.write(content.substring(end).getBytes());
			out.close();
	
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return error("Failure writing " + file);
		}
	}

	protected String readStream(InputStream in) throws IOException {
		StringBuffer buf = new StringBuffer();
		byte[] buffer = new byte[65536];
		for (;;) {
			int count = in.read(buffer);
			if (count < 0)
				break;
			buf.append(new String(buffer, 0, count));
		}
		in.close();
		return buf.toString();
	}

	/**
	 * Get a list of files from a directory (recursively)
	 */
	public void listFilesRecursively(File directory, String prefix, List<String> result) {
		for (File file : directory.listFiles())
			if (file.isDirectory())
				listFilesRecursively(file, prefix + file.getName() + "/", result);
			else if (file.isFile())
				result.add(prefix + file.getName());
	}

	/**
	 * Get a list of files from a directory or within a .jar file
	 *
	 * The returned items will only have the base path, to get at the
	 * full URL you have to prefix the url passed to the function.
	 */
	public List<String> getResourceList(String url) {
		List<String> result = new ArrayList<String>();

		if (url.startsWith("jar:")) {
			int bang = url.indexOf("!/");
			String jarURL = url.substring(4, bang);
			if (jarURL.startsWith("file:"))
				jarURL = jarURL.substring(5);
			String prefix = url.substring(bang + 2);
			int prefixLength = prefix.length();

			try {
				JarFile jar = new JarFile(jarURL);
				Enumeration<JarEntry> e = jar.entries();
				while (e.hasMoreElements()) {
					JarEntry entry = e.nextElement();
					if (entry.getName().startsWith(prefix))
						result.add(entry.getName().substring(prefixLength));
				}
			} catch (IOException e) {
				IJ.handleException(e);
			}
		}
		else
			listFilesRecursively(new File(url), "", result);
		return result;
	}

	protected boolean error(String message) {
		JOptionPane.showMessageDialog(parent, message);
		return false;
	}
}