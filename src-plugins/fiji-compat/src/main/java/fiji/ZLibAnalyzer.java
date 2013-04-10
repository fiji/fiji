package fiji;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.zip.InflaterInputStream;

public class ZLibAnalyzer {
	public static int copy(InputStream input, PrintStream out, byte[] buf)
			throws IOException {
		for (;;) {
			int len = input.read(buf);
			if (len < 0)
				return 0;
			out.write(buf, 0, len);
		}
	}

	/*
	 * This function opens the given file, and finds the nth
	 * zlib signature (0x78 0x01), and tries to decrypt the stream.
	 */
	public static int inflate(String path, int nth, PrintStream out)
			throws IOException {
		InputStream input =
			new BufferedInputStream(new FileInputStream(path));
		byte[] buffer = new byte[16384];
		long offset = 0;
		for (;;) {
			input.mark(buffer.length + 1);
			int len = input.read(buffer);
			if (len < 0) {
				input.close();
				return -1;
			}
			if (len < 2) {
				int len2 = input.read(buffer,
						len, buffer.length - len);
				if (len2 < 0) {
					input.close();
					return -1;
				}
				len += len2;
			}
			for (int i = 0; i < len - 1; i++)
				if (buffer[i] == 0x78 &&
						(buffer[i + 1] == 0x01 ||
						 buffer[i + 1] == (byte)0x9c)) {
					if (--nth > 0)
						continue;
					System.err.println("Found stream @ "
						+ (offset + i));
					input.reset();
					input.skip(offset + i);
					final InputStream inflater = new InflaterInputStream(input);
					int result = copy(inflater, out, buffer);
					inflater.close();
					return result;
				}
			offset += len;
		}
	}

	public static void main(String[] args) {
		if (args.length != 1 && args.length != 2) {
			System.err.println("Usage: Analyze <file> [<nth>]");
			System.exit(1);
		}

		try {
			int nth = args.length < 2 ?
				-1 : Integer.parseInt(args[1]);
			if (nth < 0)
				for (nth = -nth; ; nth++) try {
					inflate(args[0], nth, System.out);
					break;
				} catch (Exception e) { e.printStackTrace(); }
			else
				inflate(args[0], nth, System.out);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
