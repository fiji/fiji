package fiji;

import java.io.File;
import java.io.IOException;

import java.net.JarURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class InspectJar implements Iterable<JarEntry> {
	List<JarFile> jarFiles;

	public InspectJar() {
		jarFiles = new ArrayList<JarFile>();
	}

	public InspectJar(JarFile jarFile) {
		this();
		jarFiles.add(jarFile);
	}

	public InspectJar(File file) throws IOException {
		this(new JarFile(file));
	}

	public InspectJar(String file) throws IOException {
		this(new JarFile(file));
	}

	public InspectJar(String[] files) throws IOException {
		this(new JarFile(files[0]));

		for (int i = 1; i < files.length; i++)
			addJar(files[i]);
	}

	public InspectJar(URL url) throws IOException {
		this();
		addJar(url);
	}

	public void addJar(JarFile jarFile) {
		jarFiles.add(jarFile);
	}

	public void addJar(URL url) throws IOException {
		JarURLConnection jarCon = (JarURLConnection)url.openConnection();
		jarFiles.add(jarCon.getJarFile());
		// TODO: make sure that connections are closed (finalize()?)
	}

	public void addJar(String file) throws IOException {
		if (file.startsWith("http:") || file.startsWith("https:") || file.startsWith("file:"))
			addJar(new URL("jar:" + file + "!/"));
		else if (file.startsWith("jar:")) {
			int bang = file.indexOf("!/");
			if (bang < 0)
				addJar(new URL(file + "!/"));
			else
				addJar(new URL(file.substring(0, bang + 2)));
		}
		else
			jarFiles.add(new JarFile(file));
	}

	public void addClassPath() {
		for (String path : System.getProperty("java.class.path")
				.split(File.pathSeparator)) try {
			addJar(path);
		} catch (IOException e) { /* ignore */ }
		for (String path : System.getProperty("sun.boot.class.path")
				.split(File.pathSeparator))
			if (!path.endsWith("/sunrsasign.jar") &&
					path.endsWith(".jar")) try {
				addJar(path);
			} catch (IOException e) { /* ignore */ }
	}

	protected class EntryIterator implements Iterator<JarEntry> {
		Iterator<JarFile> jarIterator;
		Enumeration<JarEntry> enumeration;

		protected EntryIterator() {
			jarIterator = jarFiles.iterator();
			if (jarIterator.hasNext())
				enumeration = jarIterator.next().entries();
		}

		public boolean hasNext() {
			while (enumeration != null && !enumeration.hasMoreElements())
				if (jarIterator.hasNext())
					enumeration = jarIterator.next().entries();
				else
					enumeration = null;
			return enumeration != null;
		}

		public JarEntry next() {
			return enumeration.nextElement();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public Iterator<JarEntry> iterator() {
		return new EntryIterator();
	}

	protected class ClassNameIterator implements Iterator<String> {
		protected Iterator<JarEntry> iter;
		protected String current;
		protected boolean noInnerClasses;

		protected ClassNameIterator(boolean noInnerClasses) {
			this.noInnerClasses = noInnerClasses;
			iter = iterator();
			findNext();
		}

		protected boolean findNext() {
			while (iter.hasNext()) {
				JarEntry entry = iter.next();
				if (entry == null)
					break;
				current = entry.getName();
				if (!current.endsWith(".class") || current.indexOf('-') >= 0)
					continue;
				if (noInnerClasses && current.indexOf('$') >= 0)
					continue;
				current = current.substring(0,
					current.length() - 6).replace('/', '.');
				return true;
			}
			current = null;
			return false;
		}

		public boolean hasNext() {
			return current != null;
		}

		public String next() {
			String result = current;
			findNext();
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public Iterator<String> classNameIterator(boolean noInnerClasses) {
		return new ClassNameIterator(noInnerClasses);
	}

	public Iterator<String> classNameIterator() {
		return classNameIterator(true);
	}

	public Iterable<String> classNames(final boolean noInnerClasses) {
		return new Iterable<String>() {
			public Iterator<String> iterator() {
				return classNameIterator(noInnerClasses);
			}
		};
	}

	public static Iterable<String> getClassNames(File file) throws IOException {
		final InspectJar inspector = new InspectJar(file);
		return new Iterable<String>() {
			public Iterator<String> iterator() {
				return inspector.classNameIterator();
			}
		};
	}

	public static Iterable<String> getClassNames(String file) throws IOException {
		return getClassNames(new File(file));
	}

	public static Iterable<String> getClassNames(String[] files) throws IOException {
		final InspectJar inspector = new InspectJar(files);
		return new Iterable<String>() {
			public Iterator<String> iterator() {
				return inspector.classNameIterator();
			}
		};
	}

	public static void main(String[] args) {
		String ijDir = System.getProperty("ij.dir");
		try {
			for (String className : getClassNames(new String[] {
				ijDir + "/jars/ij-launcher.jar",
				ijDir + "/jars/zs.jar",
				ijDir + "/plugins/CLI_.jar"
			}))
				System.err.println("class: " + className);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
