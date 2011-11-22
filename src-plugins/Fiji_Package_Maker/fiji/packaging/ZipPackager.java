package fiji.packaging;

import java.io.IOException;
import java.io.OutputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipPackager extends Packager {
	protected MarkExecutableOutputStream out;
	protected ZipOutputStream zip;

	@Override
	public String getExtension() {
		return ".zip";
	}

	@Override
	public void open(OutputStream out) {
		this.out = new MarkExecutableOutputStream(out);
		zip = new ZipOutputStream(this.out);
	}

	@Override
	public void putNextEntry(String name, int /* ignored */ size) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		zip.write(b, off, len);
	}

	public void closeEntry() throws IOException {
		zip.closeEntry();
	}

	public void close() throws IOException {
		out.tocStartsNow();
		zip.close();
	}

	/**
	 * This class works around java.util.zip.ZipFile's inability to set the external file attributes
	 */
	private class MarkExecutableOutputStream extends OutputStream {
		protected OutputStream out;
		protected byte[] tocEntry;
		protected int tocEntryOffset, tocEntryLen;
		protected final static int BASE_ENTRY_LENGTH = 46;

		public MarkExecutableOutputStream(OutputStream out) {
			this.out = out;
			tocEntry = null;
			tocEntryOffset = tocEntryLen = 0;
		}

		public void tocStartsNow() {
			tocEntry = new byte[BASE_ENTRY_LENGTH + 1024];
			tocEntryOffset = tocEntryLen = 0;
		}

		@Override
		public void write(int b) throws IOException {
			if (tocEntry == null)
				out.write(b);
			else {
				tocEntry[tocEntryOffset++] = (byte)b;
				handleTOCEntry();
			}
		}

		@Override
		public void write(byte[] b) throws IOException {
			if (tocEntry == null)
				out.write(b);
			else
				write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (tocEntry == null)
				out.write(b, off, len);
			else {
				System.arraycopy(b, off, tocEntry, tocEntryOffset, len);
				tocEntryOffset += len;
				handleTOCEntry();
			}
		}

		@Override
		public void close() throws IOException {
			if (tocEntry == null)
				throw new IOException("Did not write TOC correctly!");
			if (tocEntryOffset > 0) {
				if (getU16(0x00) == 0x4b50 && getU16(0x02) == 0x0605)
					// end of central directory
					out.write(tocEntry, 0, tocEntryOffset);
				else
					throw new IOException("Incomplete TOC!");
			}
			out.close();
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		protected void handleTOCEntry() throws IOException {
			if (tocEntryLen == 0) {
				if (tocEntryOffset >= BASE_ENTRY_LENGTH) {
					if (getU16(0x00) != 0x4b50 || getU16(0x02) != 0x0201)
						return;
					tocEntryLen = BASE_ENTRY_LENGTH
						+ getFileNameLength() + getU16(0x1e) + getU16(0x20);
					if (tocEntryLen > tocEntry.length)
						throw new IOException("ZIP entry too long!");
				}
			}
			else if (tocEntryOffset >= tocEntryLen) {
				String fileName = getFileName();
				if (isLauncher(fileName)) {
					int fileMode = 0100755;
					tocEntry[0x28] = (byte)(fileMode & 0xff);
					tocEntry[0x29] = (byte)((fileMode >> 8) & 0xff);
					// say that we're Unix-compatible
					tocEntry[0x05] = 0x03;
				}
				out.write(tocEntry, 0, tocEntryLen);
				tocEntryOffset -= tocEntryLen;
				if (tocEntryOffset > 0)
					throw new IOException("Unaligned TOC entry!");
				tocEntryLen = 0;
			}
		}

		protected String getFileName() {
			return new String(tocEntry, 0x2e, getFileNameLength());
		}

		protected int getFileNameLength() {
			return getU16(0x1c);
		}

		protected int getU16(int offset) {
			return (tocEntry[offset] & 0xff) | ((tocEntry[offset + 1] & 0xff) << 8);
		}
	}
}