package fiji.scripting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileFunctions {
	public List<String> extractSourceJar(String path) throws IOException {
		String baseName = new File(path).getName();
		if (baseName.endsWith(".jar") || baseName.endsWith(".zip"))
			baseName = baseName.substring(0, baseName.length() - 4);
		String baseDirectory = System.getProperty("fiji.dir")
			+ "/src-plugins/" + baseName + "/";

		List<String> result = new ArrayList<String>();
		JarFile jar = new JarFile(path);
		for (JarEntry entry : Collections.list(jar.entries())) {
			if (entry.getName().endsWith(".class"))
				continue;
			String destination = baseDirectory + entry.getName();
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
}