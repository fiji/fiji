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
		return fileName.equals("Fiji.app/fiji") ||
			fileName.equals("Fiji.app/fiji.exe") ||
			fileName.startsWith("Fiji.app/fiji-") ||
			fileName.startsWith("Fiji.app/Contents/MacOS/fiji-");
	}
}