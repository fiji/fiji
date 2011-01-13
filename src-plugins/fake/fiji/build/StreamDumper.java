package fiji.build;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class StreamDumper extends Thread {
	InputStream in;
	OutputStream out;

	StreamDumper(InputStream in, PrintStream out) {
		this.in = in;
		this.out = out;
	}

	public void run() {
		byte[] buffer = new byte[65536];
		for (;;) {
			try {
				int len = in.read(buffer, 0, buffer.length);
				if (len < 0)
					break;
				if (len > 0)
					out.write(buffer, 0, len);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}