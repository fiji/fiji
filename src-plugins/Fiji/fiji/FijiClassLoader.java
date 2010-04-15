package fiji;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;

public class FijiClassLoader extends ClassLoader {
	Map filesMap;
	List filesNames;
	List filesObjects;
	Map cache;
	List<ClassLoader> fallBacks;

	public FijiClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
		filesMap = new HashMap();
		filesNames = new ArrayList(10);
		filesObjects = new ArrayList(10);
		cache = new HashMap();
		fallBacks = new ArrayList<ClassLoader>();
	}

	public FijiClassLoader(String path) throws IOException {
		this();
		addPath(path);
	}

	public FijiClassLoader(Iterable<String> paths) throws IOException {
		this();
		for (String path : paths)
			addPath(path);
	}

	public FijiClassLoader(String[] paths) throws IOException {
		this();
		for (String path : paths)
			addPath(path);
	}

	public void addPath(String path) throws IOException {
		if (path.endsWith("/.rsrc"))
			return;
		if (filesMap.containsKey(path))
			return;
		File file = new File(path);
		if (file.isDirectory()) {
			filesMap.put(path, file);
			filesNames.add(path);
			filesObjects.add(file);
			String[] paths = file.list();
			for (int i = 0; i < paths.length; i++)
				addPath(path + File.separator + paths[i]);
		}
		else if (path.endsWith(".jar")) {
			JarFile jar = new JarFile(file);
			filesMap.put(path, jar);
			filesNames.add(path);
			filesObjects.add(jar);
		}
	}

	public void addFallBack(ClassLoader loader) {
		fallBacks.add(loader);
	}

	public void removeFallBack(ClassLoader loader) {
		fallBacks.remove(loader);
	}

	public URL getResource(String name) {
		int n = filesNames.size();
		for (int i = n - 1; i >= 0; --i) {
			Object item = filesObjects.get(i);
			if (item instanceof File) {
				File file = new File((File)item, name);
				try {
					if (file.exists())
						return file.toURL();
				} catch (MalformedURLException e) {}
				continue;
			}

			JarFile jar = (JarFile)item;
			String file = (String)filesNames.get(i);
			if (jar.getEntry(name) == null)
				continue;
			String url = "file:///"
				+ file.replace('\\', '/')
				+ "!/" + name;
			try {
				return new URL("jar", "", url);
			} catch (MalformedURLException e) { }
		}
		return getSystemResource(name);
	}

	public InputStream getResourceAsStream(String name) {
		return getResourceAsStream(name, false);
	}

	public InputStream getResourceAsStream(String name,
			boolean nonSystemOnly) {
		int n = filesNames.size();
		for (int i = n - 1; i >= 0; --i) {
			Object item = filesObjects.get(i);
			if (item instanceof File) {
				File f = new File((File)item, name);
				try {
					if (f.exists())
						return new FileInputStream(f);
				} catch (IOException e) {}
				continue;
			}
			JarFile jar = (JarFile)item;
			JarEntry entry = jar.getJarEntry(name);
			if (entry == null)
				continue;
			try {
				return jar.getInputStream(entry);
			} catch (IOException e) { }
		}
		if (nonSystemOnly)
			return null;
		return super.getResourceAsStream(name);
	}

	public Class forceLoadClass(String name)
		throws ClassNotFoundException {
			return loadClass(name, true, true);
		}

	public Class loadClass(String name)
		throws ClassNotFoundException {
			return loadClass(name, true);
		}

	public synchronized Class loadClass(String name,
			boolean resolve) throws ClassNotFoundException {
		return loadClass(name, resolve, false);
	}

	public synchronized Class loadClass(String name, boolean resolve,
			boolean forceReload) throws ClassNotFoundException {
		Object cached = forceReload ? null : cache.get(name);
		if (cached != null)
			return (Class)cached;
		Class result;
		try {
			if (!forceReload) {
				result = super.loadClass(name, resolve);
				if (result != null)
					return result;
			}
		} catch (Exception e) { }
		String path = name.replace('.', '/') + ".class";
		try {
			InputStream input = getResourceAsStream(path, !true);
			if (input != null) {
				byte[] buffer = readStream(input);
				input.close();
				result = defineClass(name,
						buffer, 0, buffer.length);
				cache.put(name, result);
				return result;
			}
		} catch (IOException e) { e.printStackTrace(); }
		for (ClassLoader fallBack : fallBacks) {
			result = fallBack.loadClass(name);
			if (result != null)
				return result;
		}
		return super.loadClass(name, resolve);
	}

	static byte[] readStream(InputStream input) throws IOException {
		byte[] buffer = new byte[1024];
		int offset = 0, len = 0;
		for (;;) {
			if (offset == buffer.length)
				buffer = realloc(buffer,
						2 * buffer.length);
			len = input.read(buffer, offset,
					buffer.length - offset);
			if (len < 0)
				return realloc(buffer, offset);
			offset += len;
		}
	}

	static byte[] realloc(byte[] buffer, int newLength) {
		if (newLength == buffer.length)
			return buffer;
		byte[] newBuffer = new byte[newLength];
		System.arraycopy(buffer, 0, newBuffer, 0,
				Math.min(newLength, buffer.length));
		return newBuffer;
	}
}
