package fiji;

import ij.IJ;
import ij.Menus;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CheckClassVersions {
	boolean verbose = false;

	public static void main(String[] args) {
		run(args);
	}

	public static void run(String[] args) {
		run(args, "false");
	}

	public static void run(String[] args, String verbose) {
		if (args == null || args.length == 0)
			args = new String[] { Menus.getPlugInsPath() };
		CheckClassVersions checker = new CheckClassVersions();
		checker.verbose = verbose.equals("true");
		for (int i = 0; i < args.length; i++)
			checker.getClassVersions(args[i]);
		checker.print();
	}

	static class VersionNumber implements Comparable {
		int major, minor;

		VersionNumber(int major, int minor) {
			this.major = major;
			this.minor = minor;
		}

		public int compareTo(Object other) {
			VersionNumber o = (VersionNumber)other;
			return major != o.major ?
				o.major - major : o.minor - minor;
		}

		public String toString() {
			return "1." + (major - 44);
		}
	}

	Map classes = new TreeMap();
	void addClass(int major, int minor, String name) {
		VersionNumber version = new VersionNumber(major, minor);
		List list = (List)classes.get(version);
		if (list == null) {
			list = new ArrayList();
			classes.put(version, list);
		}
		list.add(name);
	}

	final String maxVersion = "1.5";
	void print() {
		Iterator iter = new TreeSet(classes.keySet()).iterator();
		while (iter.hasNext()) {
			VersionNumber version = (VersionNumber)iter.next();
			List list = (List)classes.get(version);
			System.out.println("" + list.size()
					+ " classes require at least "
					+ "Java version " + version
					+ " (class version " + version.major
					+ "." + version.minor + ")");
			if (verbose || version.toString().compareTo(maxVersion)
					> 0) {
				Collections.sort(list);
				Iterator iter2 = list.iterator();
				while (iter2.hasNext())
					System.out.println("\t" + iter2.next());
				System.out.println("");
			}
		}
	}

	void getClassVersions(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			if (!path.endsWith(File.separator))
				path += File.separator;
			String[] list = file.list();
			for (int i = 0; i < list.length; i++)
				getClassVersions(path + list[i]);
		} else if (path.endsWith(".jar")) {
			try {
				ZipFile jarFile = new ZipFile(file);
				Enumeration list = jarFile.entries();
				while (list.hasMoreElements()) {
					ZipEntry entry =
						(ZipEntry)list.nextElement();
					String name = entry.getName();
					if (!name.endsWith(".class"))
						continue;
					getClassVersion(path
						+ "(" + name + ")",
						jarFile.getInputStream(entry));
				}
			} catch (Exception e) {
				System.err.println("Invalid jar file: '"
					+ path + "'");
			}
		} else if (path.endsWith(".class")) {
			try {
				getClassVersion(path,
					new FileInputStream(file));
			} catch (Exception e) {
				System.err.println("Could not open file: '"
					+ path + "'");
			}
		}
	}

	void getClassVersion(String path, InputStream stream)
			throws IOException {
		DataInputStream data = new DataInputStream(stream);

		if (data.readInt() != 0xcafebabe)
			System.err.println("Invalid class: " + path);
		else {
			int minor = data.readShort();
			int major = data.readShort();
			addClass(major, minor, path);
		}
		data.close();
	}

	public static float getClassVersion(InputStream stream) throws IOException {
		DataInputStream data = new DataInputStream(stream);
		if (data.readInt() != 0xcafebabe)
			throw new RuntimeException("Not a class!");
		int minor = data.readShort() & 0xffff;
		int major = data.readShort() & 0xffff;
		data.close();
		return major + minor / 100000.0f;
	}
}
