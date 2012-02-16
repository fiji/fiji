package fiji.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Util {
	public static List<String> splitCommandLine(String program)
			throws FakeException {
		List<String> result = new ArrayList<String>();
		if (program == null)
			return result;
		int len = program.length();
		String current = "";

		for (int i = 0; i < len; i++) {
			char c = program.charAt(i);
			if (isQuote(c)) {
				int i2 = findClosingQuote(program,
						c, i + 1, len);
				current += program.substring(i + 1, i2);
				i = i2;
				continue;
			}
			if (c == ' ' || c == '\t') {
				if (current.equals(""))
					continue;
				result.add(current);
				current = "";
			} else
				current += c;
		}
		if (!current.equals(""))
			result.add(current);
		return result;
	}

	public static int findClosingQuote(String s, char quote,
			int index, int len) throws FakeException {
		for (int i = index; i < len; i++) {
			char c = s.charAt(i);
			if (c == quote)
				return i;
			if (isQuote(c))
				i = findClosingQuote(s, c, i + 1, len);
		}
		String spaces = "               ";
		for (int i = 0; i < index; i++)
			spaces += " ";
		throw new FakeException("Unclosed quote: "
			+ s + "\n" + spaces + "^");
	}

	public static boolean isQuote(char c) {
		return c == '"' || c == '\'';
	}

	public static void touchFile(String target) throws IOException {
		long now = new Date().getTime();
		new File(target).setLastModified(now);
	}

	public static byte[] readStream(InputStream input) throws IOException {
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

	public static byte[] readFile(String fileName) {
		try {
			if (fileName.startsWith("jar:file:")) {
				URL url = new URL(fileName);
				return readStream(url.openStream());
			}
			File file = new File(fileName);
			if (!file.exists())
				return null;
			InputStream in = new FileInputStream(file);
			byte[] buffer = new byte[(int)file.length()];
			in.read(buffer);
			in.close();
			return buffer;
		} catch (Exception e) { return null; }
	}

	public static void copyFile(String source, String target, File cwd)
			throws FakeException {
		if (target.equals(source))
			return;
		try {
			target = makePath(cwd, target);
			source = makePath(cwd, source);
			File parent = new File(target).getParentFile();
			if (!parent.exists())
				parent.mkdirs();
			OutputStream out = new FileOutputStream(target);
			InputStream in = new FileInputStream(source);
			byte[] buffer = new byte[1<<16];
			for (;;) {
				int len = in.read(buffer);
				if (len < 0)
					break;
				out.write(buffer, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			throw new FakeException("Could not copy "
				+ source + " to " + target + ": " + e);
		}
	}

	public static int compare(File source, File target) {
		if (source.length() != target.length())
			return target.length() > source.length() ? 1 : -1;
		int result = 0;
		try {
			InputStream sourceIn = new FileInputStream(source);
			InputStream targetIn = new FileInputStream(target);
			byte[] buf1 = new byte[1<<16];
			byte[] buf2 = new byte[1<<16];
			while (result == 0) {
				int len = sourceIn.read(buf1);
				if (len < 0)
					break;
				int off = 0, count = 0;
				while (len > 0 && count >= 0) {
					count = targetIn.read(buf2, off, len);
					off += count;
					len -= count;
				}
				if (count < 0) {
					result = 1;
					break;
				}
				for (int i = 0; i < off; i++)
					if (buf1[i] != buf2[i]) {
						result = (buf2[i] & 0xff)
							- (buf1[i] & 0xff);
						break;
					}
			}
			sourceIn.close();
			targetIn.close();
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Could not compare "
				+ source + " to " + target + ": " + e);
		}
	}

	public static void delete(File file) throws FakeException {
		if (!file.delete())
			throw new FakeException("Could not delete "
					+ file.getPath());
	}

	public  static boolean isDirEmpty(String path) {
		String[] list = new File(path).list();
		return list == null || list.length == 0;
	}

	public static byte[] realloc(byte[] buffer, int newLength) {
		if (newLength == buffer.length)
			return buffer;
		byte[] newBuffer = new byte[newLength];
		System.arraycopy(buffer, 0, newBuffer, 0,
				Math.min(newLength, buffer.length));
		return newBuffer;
	}

	public static List<String> uniq(List<String> list) {
		return new ArrayList<String>(new HashSet<String>(list));
	}

	public static String join(List<String> list) {
		return join(list, " ");
	}

	public static String stripPrefix(String string, String prefix) {
		if (!string.startsWith(prefix))
			return string;
		return string.substring(prefix.length());
	}

	public static String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	public static String join(List<String> list, String separator) {
		Iterator<String> iter = list.iterator();
		String result = iter.hasNext() ? iter.next().toString() : "";
		while (iter.hasNext())
			result += separator + iter.next();
		return result;
	}

	public static String join(String[] list, String separator) {
		String result = list.length > 0 ? list[0] : "";
		for (int i = 1; i < list.length; i++)
			result += separator + list[i];
		return result;
	}

	public static String[] split(String string, String delimiter) {
		if (string == null || string.equals(""))
			return new String[0];
		List<String> list = new ArrayList<String>();
		int offset = 0;
		for (;;) {
			int nextOffset = string.indexOf(delimiter, offset);
			if (nextOffset < 0) {
				list.add(string.substring(offset));
				break;
			}
			list.add(string.substring(offset, nextOffset));
			offset = nextOffset + 1;
		}
		String[] result = new String[list.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = list.get(i);
		return result;
	}

	public static String[] splitPaths(String pathList) {
		String[] paths = Util.split(pathList, ":");
		if (":".equals(File.pathSeparator))
			return paths;
		// by mistake, c:\blub could have been separated
		int j = 0;
		for (int i = 0; i < paths.length; i++, j++)
			if (i + 1 < paths.length && paths[i].length() == 1 && "\\/".indexOf(paths[i + 1].charAt(0)) >= 0)
				paths[j] = paths[i] + ":" + paths[++i];
			else if (j < i)
				paths[j] = paths[i];
		if (j == paths.length)
			return paths;
		String[] newPaths = new String[j];
		System.arraycopy(paths, 0, newPaths, 0, j);
		return newPaths;
	}

	public static String pathListToNative(String pathList) {
		if (":".equals(File.pathSeparator))
			return pathList;
		return join(splitPaths(pathList), File.pathSeparator);
	}

	public static boolean moveFileOutOfTheWay(String file) throws FakeException {
		return moveFileOutOfTheWay(new File(file));
	}

	public static boolean moveFileOutOfTheWay(File file) throws FakeException {
		if (!file.exists())
			return false;
		if (file.delete())
			return false;
		if (file.renameTo(new File(file.getPath() + ".old")))
			return true;
		throw new FakeException("Could not move " + file
				+ " out of the way");
	}

	public static String getPlatform() {
		boolean is64bit = System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "64" : "32");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32");
		//System.err.println("Unknown platform: " + osName);
		return osName.toLowerCase();
	}

	public static boolean isAbsolutePath(String path) {
		boolean isWindows = getPlatform().startsWith("win");
		if (isWindows)
			return path.length() > 1 && path.charAt(1) == ':';
		return path.startsWith("/");
	}

	public static String makePath(File cwd, String path) {
		String prefix = "", suffix = "";
		if (path.startsWith("jar:file:")) {
			prefix = "jar:file:";
			int exclamation = path.indexOf('!');
			suffix = path.substring(exclamation);
			path = path.substring(prefix.length(), exclamation);
		}
		if (isAbsolutePath(path))
			return prefix + path + suffix;
		if (path.equals("."))
			return prefix + cwd.toString() + suffix;
		if (cwd.toString().equals("."))
			return prefix + (path.equals("") ? "." : path) + suffix;
		return prefix + new File(cwd, path).toString() + suffix;
	}

	public static boolean getBool(String string) {
		return string != null &&
			(string.equalsIgnoreCase("true") ||
			 string.equals("1") || string.equals("2"));
	}
}