package fiji.packaging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TarPackager extends Packager {
	protected OutputStream out;
	protected Set<String> directories = new HashSet<String>();
	protected byte[] header = new byte[0x200];
	protected int epoch = (int)(System.currentTimeMillis() / 1000);
	protected int fileOffset, fileSize;

	@Override
	public String getExtension() {
		return ".tar";
	}

	@Override
	public void open(OutputStream out) throws IOException {
		this.out = out;
	}

	@Override
	public void putNextEntry(String name, boolean executable, int size) throws IOException {
		handleDirectory(name);
		writeHeader(name, executable ? 0755 : 0644, size, 0);
		fileSize = size;
		fileOffset = 0;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (fileOffset + len > fileSize)
			throw new IOException("Unaligned file");
		out.write(b, off, len);
		fileOffset += len;
	}

	@Override
	public void closeEntry() throws IOException {
		if (fileOffset != fileSize)
			throw new IOException("Short file");
		int remainder = (fileSize & 0x1ff);
		if (remainder > 0) {
			Arrays.fill(header, (byte)0);
			out.write(header, 0, 0x200 - remainder);
		}
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	protected void handleDirectory(String name) throws IOException {
		int slash = name.lastIndexOf('/', name.length() - 2);
		if (slash < 0)
			return;
		name = name.substring(0, slash + 1);
		if (directories.contains(name))
			return;
		handleDirectory(name);
		writeHeader(name, 0777, 0, 5 /* directory */);
		directories.add(name);
	}

	protected void writeHeader(String name, int mode, int size, int type) throws IOException {
		// initialize to NULs
		Arrays.fill(header, (byte)0);

		if (name.length() > 99) {
			// write extended header
			String headerName = "ext-header." + name.hashCode();
			String extendedHeader = makeExtendedHeader("path", name);
			int extSize = extendedHeader.length();

			Arrays.fill(header, (byte)0);
			System.arraycopy(headerName.getBytes("ASCII"), 0, header, 0, headerName.length());
			digits(0666, 0x64, 8); // mode
			digits(1000, 0x6c, 8); // uid
			digits(1000, 0x74, 8); // gid
			digits(extSize, 0x7c, 12); // size
			digits(epoch, 0x88, 12); // timestamp
			header[0x9c] = 'x'; // extended header
			System.arraycopy("ustar".getBytes("ASCII"), 0, header, 0x101, 5); // magic

			checksumHeader();
			out.write(header);
			Arrays.fill(header, (byte)0);

			out.write(extendedHeader.getBytes("ASCII"));
			// pad
			if ((extSize & 0x1ff) > 0)
				out.write(header, 0, 0x200 - (extSize & 0x1ff));

			name = "ext-name." + name.hashCode();
		}

		System.arraycopy(name.getBytes("ASCII"), 0, header, 0, name.length()); // name
		digits(mode, 0x64, 8); // mode
		digits(1000, 0x6c, 8); // uid
		digits(1000, 0x74, 8); // gid
		digits(size, 0x7c, 12); // size
		digits(epoch, 0x88, 12); // timestamp
		if (type != 0)
			header[0x9c] = (byte)(0x30 + type);

		checksumHeader();

		out.write(header);
	}

	protected String makeExtendedHeader(final String key, final String value) {
		String result = key + "=" + value + "\n";
		int len = result.length() + 2;
		while (len != ("" + len + " " + result).length())
			len = ("" + len + " " + result).length();
		return "" + len + " " + result;
	}

	protected void checksumHeader() {
		// checksum
		Arrays.fill(header, 0x94, 0x9c, (byte)0x20); // checksum
		int checksum = 0;
		for (int i = 0; i < header.length; i++)
			checksum += header[i] & 0xff;
		// pretend it to be decimal
		digits(checksum, 0x94, 7);
	}

	protected void digits(long number, int offset, int len) {
		number = Long.parseLong(Long.toOctalString(number));
		header[offset + len - 1] = 0;
		for (int i = len - 2; i >= 0; i--, number = number / 10)
			header[offset + i] = (byte)(0x30 + (number % 10));
	}
}