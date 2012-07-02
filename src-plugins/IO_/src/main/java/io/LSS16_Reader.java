package io;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.plugin.PlugIn;

import ij.process.ByteProcessor;

import ij.io.OpenDialog;

import java.awt.image.IndexColorModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class LSS16_Reader extends ImagePlus implements PlugIn {
	/** Expects path as argument, or will ask for a file path. */
	public void run(final String arg) {
		File file = null;
		if (arg != null && arg.length() > 0)
			file = new File(arg);
		else {
			OpenDialog od =
				new OpenDialog("Choose .lss file", null);
			String directory = od.getDirectory();
			if (null == directory)
				return;
			file = new File(directory + "/" + od.getFileName());
		}

		try {
			FileInputStream in = new FileInputStream(file);
			int signature = readIntLE(in);
			if (signature != 0x1413f33d) {
				IJ.error("Wrong signature!");
				return;
			}
			int w = readShortLE(in);
			int h = readShortLE(in);
			byte[] reds = new byte[16];
			byte[] greens = new byte[16];
			byte[] blues = new byte[16];
			for (int i = 0; i < 16; i++) {
				reds[i] = (byte)(in.read() * 255 / 63);
				greens[i] = (byte)(in.read() * 255 / 63);
				blues[i] = (byte)(in.read() * 255 / 63);
			}
			IndexColorModel cmap = new IndexColorModel(8, 16,
				reds, greens, blues);

			byte[] pixels = new byte[w * h];
			byte lastColor;
			int k = 0;
			for (int j = 0; j < h; j++) {
				lastColor = 0;
				nextNybble = -1;
				for (int i = 0; i < w; ) {
					byte nybble = readNybble(in);
					if (nybble != lastColor) {
						pixels[k++] = nybble;
						lastColor = nybble;
						i++;
					} else {
						int count = readCount(in);
						while (count > 0 && i < w) {
							pixels[k++] = nybble;
							i++;
							count--;
						}
					}
				}
			}

			String name = file.getName();
			ByteProcessor proc =
				new ByteProcessor(w, h, pixels, cmap);
			setProcessor(name, proc);
		} catch (IOException e) {
			IJ.error("Could not read '"
					+ file.getAbsolutePath() + "'");
		}
	}

	public static int readIntLE(FileInputStream in) throws IOException {
		byte[] buffer = new byte[4];
		in.read(buffer);
		return (buffer[3] & 0xff) << 24 |
			(buffer[2] & 0xff) << 16 |
			(buffer[1] & 0xff) << 8 |
			(buffer[0] & 0xff);
	}

	public static int readShortLE(FileInputStream in) throws IOException {
		byte[] buffer = new byte[2];
		in.read(buffer);
		return (buffer[1] & 0xff) << 8 |
			(buffer[0] & 0xff);
	}

	protected int nextNybble = -1;
	protected byte readNybble(FileInputStream in) throws IOException {
		int result = nextNybble;
		if (result < 0) {
			int next = in.read();
			result = next & 0xf;
			nextNybble = (next & 0xf0) >> 4;
		} else
			nextNybble = -1;
		return (byte)result;
	}

	protected int readCount(FileInputStream in) throws IOException {
		int count = readNybble(in) & 0xff;
		if (count > 0)
			return count;
		return (readNybble(in) & 0xf) + ((readNybble(in) & 0xf) << 4)
			+ 16;
	}
}
