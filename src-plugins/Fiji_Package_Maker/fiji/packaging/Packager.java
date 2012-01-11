package fiji.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Packager {
	protected byte[] buffer = new byte[16384];

	public abstract String getExtension();

	public abstract void open(OutputStream out) throws IOException;
	public abstract void putNextEntry(String name, int size) throws IOException;
	public abstract void write(byte[] b, int off, int len) throws IOException;
	public abstract void closeEntry() throws IOException;
	public abstract void close() throws IOException;

	public void write(InputStream in) throws IOException {
		for (;;) {
			int count = in.read(buffer);
			if (count < 0)
				break;
			write(buffer, 0, count);
		}
		in.close();
	}

	protected static boolean isLauncher(String fileName) {
		if (fileName.startsWith("Fiji.app/"))
			fileName = fileName.substring(9);
		if (fileName.startsWith("Contents/MacOS/"))
			fileName = fileName.substring(15);
		if (fileName.endsWith(".exe"))
			fileName = fileName.substring(0, fileName.length() - 4);
		return fileName.equals("ImageJ") || fileName.equals("fiji") ||
			fileName.startsWith("ImageJ-") || fileName.startsWith("fiji-");
	}
}