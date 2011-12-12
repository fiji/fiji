package fiji.updater.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStream2OutputStream extends Thread {
	protected InputStream in;
	protected OutputStream out;

	public InputStream2OutputStream(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
		start();
	}

	public void run() {
		byte[] buffer = new byte[16384];
		try {
			for (;;) {
				int count = in.read(buffer);
				if (count < 0)
					break;
				out.write(buffer, 0, count);
			}
			in.close();
		} catch (IOException e) {
			UserInterface.get().handleException(e);
		}
	}
}