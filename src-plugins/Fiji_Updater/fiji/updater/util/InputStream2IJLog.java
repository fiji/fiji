package fiji.updater.util;

import ij.IJ;

import java.io.IOException;
import java.io.InputStream;

public class InputStream2IJLog extends Thread {
	protected InputStream in;
	public StringBuffer out;

	public InputStream2IJLog(InputStream in) {
		this.in = in;
		out = new StringBuffer();
		start();
	}

	public void run() {
		byte[] buffer = new byte[16384];
		try {
			for (;;) {
				int count = in.read(buffer);
				if (count < 0)
					break;
				handle(buffer, 0, count);
			}
			in.close();
		} catch (IOException e) {
			IJ.handleException(e);
		}
		if (out.length() > 0)
			handleLine(out.toString());
	}

	protected void handle(byte[] buffer, int offset, int length) {
		for (int i = 0; i < length; i++)
			if (buffer[offset + i] == '\n') {
				out.append(new String(buffer, offset, i));
				handleLine(out.toString());
				out.setLength(0);

				offset += i + 1;
				length -= i + 1;
				i = -1;
			}
		out.append(new String(buffer, offset, length));
	}

	protected void handleLine(String line) {
		IJ.log(line);
	}
}