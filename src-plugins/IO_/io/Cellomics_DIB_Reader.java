package io;

import com.jcraft.jzlib.ZInputStream;

import ij.IJ;
import ij.ImagePlus;

import ij.io.OpenDialog;

import ij.plugin.PlugIn;

import ij.process.ShortProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/*
 * Cellomics-generated DIB files can be uncompressed or zlib-compressed.
 *
 * The code to interpret uncompressed ones was translated from Matlab
 * code written by Jeff Mather.
 *
 * The analysis how to decompress the zlib-compressed DIBs was performed
 * by Johannes Schindelin.
 */
public class Cellomics_DIB_Reader extends ImagePlus implements PlugIn {
	protected int size, width, height, compression, sizeImage;
	protected int xPelsPerMeter, yPelsPerMeter, clrUsed, clrImportant;
	protected short planes, bitCount;
	protected byte[] header = new byte[48], buffer;

	public void run(String arg) {
		if (arg == null || arg.length() == 0) {
			OpenDialog od =
				new OpenDialog("Choose a .C01 file", null);
			String directory = od.getDirectory();
			if (null == directory)
				return;
			arg = directory + "/" + od.getFileName();
		}
		try {
			read(arg);
			short[] pixels = new short[width * height];
			for (int i = 0; i < pixels.length; i++)
				pixels[i] = (short)
					((buffer[i * 2] & 0xff) |
					 ((buffer[i * 2 + 1] & 0xff) << 8));
			String title = new File(arg).getName();
			ShortProcessor processor =
				new ShortProcessor(width, height, pixels, null);
			setProcessor(title, processor);
		} catch (IOException e) {
			IJ.error("Could not read file: " + e);
		}
	}

	public void read(String path) throws IOException {
		InputStream stream = new FileInputStream(path);
		if (stream.read(header, 0, 4) < 4)
			throw new IOException("Short file");
		size = getInt(0);
		if (size == 0x00000028)
			readUncompressed(stream);
		else if (size == 0x10000000) {
			InputStream uncompressed = new ZInputStream(stream);
			if (uncompressed.read(header, 0, 4) < 4)
				throw new IOException("Short file");
			size = getInt(0);
			if (size != 0x0000000028)
				throw new RuntimeException("Unrecognized "
					+ "compressed DIB");
			readUncompressed(uncompressed);
			uncompressed.close();
		}
		else
			throw new RuntimeException("Unrecognized DIB" + size);
		stream.close();
	}

	public void toggleEndianness() {
		for (int i = 0; i < buffer.length; i += 2) {
			byte b = buffer[i];
			buffer[i] = buffer[i + 1];
			buffer[i + 1] = b;
		}
	}

	/* This assumes that the size has been read from the stream already. */
	protected void readUncompressed(InputStream stream) throws IOException {
		if (stream.read(header, 0, 48) < 48)
			throw new IOException("Short file");
		width = getInt(0);
		height = getInt(4);
		planes = getShort(8);
		bitCount = getShort(10);
		compression = getInt(12);
		sizeImage = getInt(16);
		xPelsPerMeter = getInt(20);
		yPelsPerMeter = getInt(24);
		clrUsed = getInt(28);
		clrImportant = getInt(32);

		buffer = new byte[width * height * bitCount / 8];
		int offset = 0;
		while (offset < buffer.length) {
			int len = stream.read(buffer, offset,
					buffer.length - offset);
			if (len <= 0)
				throw new IOException("Short file (expected "
						+ buffer.length + ", got "
						+ offset + ")");
			offset += len;
		}
	}

	protected int getInt(int offset) {
		return (header[offset] & 0xff) |
			((header[offset + 1] & 0xff) << 8) |
			((header[offset + 2] & 0xff) << 16) |
			((header[offset + 3] & 0xff) << 24);
	}

	protected short getShort(int offset) {
		return (short)((header[offset] & 0xff) |
			((header[offset + 1] & 0xff) << 8));
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getBitDepth() {
		return bitCount;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public void outputAsPGM(String path, PrintStream out) {
		try {
			read(path);
			toggleEndianness();
			out.println("P5");
			out.println("# Generated from a Cellomics DIB");
			out.println("" + getWidth() + " " + getHeight());
			out.println("4096");
			out.write(getBuffer());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: <program> <file>");
			System.exit(1);
		}
		Cellomics_DIB_Reader reader = new Cellomics_DIB_Reader();
		reader.outputAsPGM(args[0], System.out);
	}
}
