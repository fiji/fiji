package fiji.updater.ui.ij1;

import ij.IJ;

import java.io.IOException;
import java.io.OutputStream;

public class IJLogOutputStream extends OutputStream {
	public byte[] buffer = new byte[16384];
	public int len;

	protected synchronized void ensure(int length) {
		if (buffer.length >= length)
			return;

		int newLength = buffer.length * 3 / 2;
		if (newLength < length)
			newLength = length + 16;
		byte[] newBuffer = new byte[newLength];
		System.arraycopy(buffer, 0, newBuffer, 0, len);
		buffer = newBuffer;
	}

	public synchronized void write(int b) {
		ensure(len + 1);
		buffer[len++] = (byte)b;
		if (b == '\n')
			flush();
	}

	public synchronized void write(byte[] buffer) {
		write(buffer, 0, buffer.length);
	}

	public synchronized void write(byte[] buffer, int offset, int length) {
		int eol = length;
		while (eol > 0)
			if (buffer[eol - 1] == '\n')
				break;
			else
				eol--;
		if (eol >= 0) {
			ensure(len + eol);
			System.arraycopy(buffer, offset, this.buffer, len, eol);
			len += eol;
			flush();
			length -= eol;
			if (length == 0)
				return;
			offset += eol;
		}
		ensure(len + length);
		System.arraycopy(buffer, offset, this.buffer, len, length);
		len += length;
	}

	public void close() {
		flush();
	}

	public synchronized void flush() {
		if (len > 0) {
			if (buffer[len - 1] == '\n')
				len--;
			IJ.log(new String(buffer, 0, len));
		}
		len = 0;
	}

	public static void main(String[] args) {
		OutputStream out = new IJLogOutputStream();
		try {
			out.write("Hello, World!".getBytes());
			IJ.log("Check");
			out.flush();
			IJ.log("Check2");
			out.write("Hello,".getBytes());
			IJ.log("Check3");
			out.write(32);
			IJ.log("Check4");
			out.write("World!!!\nHow are you?".getBytes());
			IJ.log("Check5");
			out.close();
		}
		catch (IOException e) {
			IJ.handleException(e);
		}
	}
}