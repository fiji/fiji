package fiji.build;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class JarClassLoader extends ClassLoader {
	Map<String, JarFile> jarFilesMap;
	List<String> jarFilesNames;
	List<JarFile> jarFilesObjects;
	HashMap<String, Class<?>> cache;

	JarClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
		jarFilesMap = new HashMap<String, JarFile>();
		jarFilesNames = new ArrayList<String>(10);
		jarFilesObjects = new ArrayList<JarFile>(10);
		cache = new HashMap<String, Class<?>>();
	}

	public URL getResource(String name) {
		int n = jarFilesNames.size();
		for (int i = n - 1; i >= 0; --i) {
			JarFile jar = jarFilesObjects.get(i);
			String file = jarFilesNames.get(i);
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
		int n = jarFilesNames.size();
		for (int i = n - 1; i >= 0; --i) {
			JarFile jar = jarFilesObjects.get(i);
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

	public Class<?> forceLoadClass(String name)
			throws ClassNotFoundException {
		return loadClass(name, true, true);
	}

	public Class<?> loadClass(String name)
			throws ClassNotFoundException {
		return loadClass(name, true);
	}

	public synchronized Class<?> loadClass(String name,
			boolean resolve) throws ClassNotFoundException {
		return loadClass(name, resolve, false);
	}

	public synchronized Class<?> loadClass(String name,
				boolean resolve, boolean forceReload)
			throws ClassNotFoundException {
		Class<?> cached = forceReload ? null : cache.get(name);
		if (cached != null)
			return cached;
		Class<?> result;
		try {
			if (!forceReload) {
				result = super.loadClass(name, resolve);
				if (result != null)
					return result;
			}
		} catch (Exception e) { }
		String path = name.replace('.', '/') + ".class";
		InputStream input = getResourceAsStream(path, !true);
		if (input == null)
			throw new ClassNotFoundException(name);
		try {
			byte[] buffer = Util.readStream(input);
			input.close();
			result = defineClass(name,
					buffer, 0, buffer.length);
			if (result.getPackage() == null) {
				String packageName = name.substring(0, name.lastIndexOf('.'));
				definePackage(packageName, null, null, null, null, null, null, null);
			}
			cache.put(name, result);
			return result;
		} catch (IOException e) {
			result = forceReload ?
				super.loadClass(name, resolve) : null;
			return result;
		}
	}
}