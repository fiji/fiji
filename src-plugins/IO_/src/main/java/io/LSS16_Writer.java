// Save the active image as SYSLINUX .lss file
package io;

import ij.IJ;
import ij.ImagePlus;
import ij.LookUpTable;
import ij.WindowManager;

import ij.io.SaveDialog;

import ij.plugin.PlugIn;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class LSS16_Writer implements PlugIn {

	public void run (String arg) {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.error("No image is open");
			return;
		}
		byte[] pixels = null;
		try {
			pixels = (byte[])image.getProcessor().getPixels();
		} catch (ClassCastException e) {
			IJ.error("Can only handle 8-bit images");
			return;
		}
		int w = image.getWidth();
		int h = image.getHeight();

		byte[] colors = new byte[256];
		for (int i = 0; i < colors.length; i++)
			colors[i] = (byte)-1;
		int currentColor = 0;
		for (int i = 0; i < w * h; i++) {
			int value = pixels[i] & 0xff;
			if (colors[value] < 0) {
				if (currentColor > 15) {
					IJ.error("This image needs more than "
						+ "16 colors.\n"
						+ "Please convert to RGB and "
						+ "then to 8-bit Color, "
						+ "reducing the number of "
						+ "colors.");
					return;
				}
				colors[value] = (byte)currentColor++;
			}
		}
		// order colors
		currentColor = 0;
		for (int i = 0; i < colors.length; i++)
			if (colors[i] >= 0)
				colors[i] = (byte)currentColor++;

		String name = image.getTitle();
		String path = arg;
		if (path == null || path.length() < 1) {
			SaveDialog sd = new SaveDialog("Save as LSS16",
					name, ".lss");
			String directory = sd.getDirectory();
			if (directory == null)
				return;

			if (!directory.endsWith("/"))
				directory += "/";
			name = sd.getFileName();
			path = directory + name;
		}

		try {
			FileOutputStream out = new FileOutputStream(path);
			writeIntLE(out, 0x1413f33d);
			writeShortLE(out, w);
			writeShortLE(out, h);
			writeColorTable(out, colors, image.createLut());
			writeImage(out, w, h, pixels, colors);
			out.close();
		} catch (IOException e) {
			IJ.error("Could not write to '" + path + "'");
		}
	}

	protected void writeIntLE(FileOutputStream out, int i)
			throws IOException {
		out.write(i & 0xff);
		out.write((i & 0xff00) >> 8);
		out.write((i & 0xff0000) >> 16);
		out.write((i & 0xff000000) >> 24);
	}

	protected void writeShortLE(FileOutputStream out, int s)
			throws IOException {
		out.write(s & 0xff);
		out.write((s & 0xff00) >> 8);
	}

	protected void writeByte(FileOutputStream out, byte b)
			throws IOException {
		out.write(b & 0xff);
	}

	protected void writeColor(FileOutputStream out, byte color)
			throws IOException {
		writeByte(out, (byte)((color & 0xff) * 63 / 255));
	}

	protected int lastNybble = -1;
	protected void writeNybble(FileOutputStream out, int n)
			throws IOException {
		if (lastNybble < 0) {
			lastNybble = n;
			return;
		}
		writeByte(out, (byte)(n << 4 | lastNybble));
		lastNybble = -1;
	}
	protected void flushNybble(FileOutputStream out) throws IOException {
		if (lastNybble >= 0)
			writeNybble(out, 0);
	}

	protected void writeColorTable(FileOutputStream out,
			byte[] colors, LookUpTable lut) throws IOException {
		byte[] reds = lut.getReds();
		byte[] greens = lut.getGreens();
		byte[] blues = lut.getBlues();
		int count = 0;
		for (int i = 0; i < colors.length; i++)
			if (colors[i] >= 0) {
				writeColor(out, reds[i]);
				writeColor(out, greens[i]);
				writeColor(out, blues[i]);
				count++;
			}
		while (count++ < 16) {
			writeByte(out, (byte)0);
			writeByte(out, (byte)0);
			writeByte(out, (byte)0);
		}
	}

	protected void writeImage(FileOutputStream out,
			int w, int h, byte[] pixels, byte[] colors)
			throws IOException {
		int k = 0;
		for (int j = 0; j < h; j++) {
			int last = 0;
			for (int i = 0; i < w; i++) {
				int value = colors[pixels[k]];
				writeNybble(out, value);
				if (value != last) {
					last = value;
					k++;
					continue;
				}
				int count = 1;
				while (i < w && colors[pixels[k++]]
						== value && count < 255 + 16) {
					i++;
					count++;
				}
				if (count < 16)
					writeNybble(out, count);
				else {
					count -= 16;
					writeNybble(out, 0);
					writeNybble(out, count & 0xf);
					writeNybble(out, (count & 0xf0) >> 4);
				}
			}
			flushNybble(out);
		}
	}
}
