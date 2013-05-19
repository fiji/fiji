package io;

/*
 * This ImageJ plugin exports an image as an Encapsulated PostScript (EPS) file.
 * Based on the "EPS_Writer" plugin by Johannes Schindelin (2006), this plugin
 * also supports the export of binary grayscale images as B/W bitmap files.
 * All other images, including 16-bit (short) and 32-bit (float) grayscale images
 * are exported with 8-bit depth, noting that EPS files with 16-bit images are not
 * rendered properly by some PostScript interpreters. 16/32-bit grayscale images
 * are automatically normalized to their max-min range. Lookup tables of grayscale
 * images are ignored.
 *
 * Author: Wilhelm Burger (wilbur@ieee.org, www.imagingbook.com)
 * License: public domain.
 * Date: 2010/12/12
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.YesNoCancelDialog;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.image.ColorModel;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Export_EPS implements PlugInFilter {

	static final String extension = ".eps";
	static final int epsLineLength = 38;

	private ImagePlus img;
	private int imgType = 0;
	private String imgTitle = "";
	private Calibration imgCalib;
	private int width, height;
	private double realWidth, realHeight;
	private boolean saveAsBitmap = false;

	public int setup(String arg0, ImagePlus img) {
		this.img = img;
		return DOES_ALL + NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		if (img.getStackSize() != 1) {
			IJ.error("Can only save 2D images!");
			return;
		}
		imgTitle = stripFileExtension(img.getTitle());
		SaveDialog sd = new SaveDialog("Save image as EPS", imgTitle, extension);
		String dir = sd.getDirectory();
		String name = sd.getFileName();
		if(name == null || name.length() == 0)
			return;
		if (ip.isBinary()) {
			YesNoCancelDialog bd = new YesNoCancelDialog(null, "EPS Export", "Save this image as binary bitmap?");
			if (bd.cancelPressed())
				return;
			saveAsBitmap = bd.yesPressed();
		}

		width = ip.getWidth();
		height = ip.getHeight();
		imgType = img.getType();
		imgCalib = img.getCalibration();
		realWidth = width * imgCalib.pixelWidth;
		realHeight = height * imgCalib.pixelHeight;
		ip.resetMinAndMax();

		IJ.showStatus("Writing EPS " + dir + name + "...");
		try {
			FileWriter output = new FileWriter(dir + name);
			writeHeader(output, name);
			writeImage(output, ip);
			writeTrailer(output);
			output.flush();
			output.close();
		} catch(IOException e) {
			e.printStackTrace();
			IJ.error("Error writing EPS file " + dir + name);
		}
		IJ.showStatus("");
	}

	boolean isGrayType(int type) {
		switch (type) {
			case ImagePlus.GRAY8 : case ImagePlus.GRAY16 : case ImagePlus.GRAY32: return true;
			default: return false;
		}
	}

	void writeHeader(Writer out, String fileName) throws IOException {
		DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US);
        String date = dateFormat.format(new java.util.Date());

		double x0 = imgCalib.xOrigin;
		double y0 = imgCalib.yOrigin;
		double x1 = imgCalib.xOrigin + realWidth;
		double y1 = imgCalib.yOrigin + realHeight;
		int u0 = (int) Math.floor(x0);
		int v0 = (int) Math.floor(y0);
		int u1 = (int) Math.ceil(x1);
		int v1 = (int) Math.ceil(y1);

		out.write("%!PS-Adobe-2.0 EPSF-1.2\n");
		out.write("%%Title: " + fileName + "\n");
		out.write("%%Creator: " + this.getClass().getSimpleName() + " ImageJ Plugin by W. Burger 2010.12\n");
		out.write("%%CreationDate: " + date + "\n");
		out.write("%%BoundingBox: " + u0 + " " + v0 + " " + u1 + " " + v1 + "\n");
		out.write("%%HiResBoundingBox: " + x0 + " " + y0 + " " + x1 + " " + y1 + "\n");
		out.write("%%EndComments\n");
		out.write("save\n");
		out.write("countdictstack mark newpath\n");
		out.write("/showpage {} def\n");
		out.write("/setpagedevice {pop} def\n");
	}

	void writeImage(Writer out, ImageProcessor ip) throws IOException {
		boolean isGray = isGrayType(imgType);
		int spp = (isGray) ? 1 : 3;			// samples per pixel
		int bps = (saveAsBitmap) ? 1 : 8;	// bits per sample
		out.write("/imgstring " + spp + " string def\n"
			+ width + " " + height + " " + bps + " [ "
			+ (1 / imgCalib.pixelWidth) + " 0 0 "
			+ (-1 / imgCalib.pixelHeight) + " "
			+ imgCalib.xOrigin + " " + (realHeight - imgCalib.yOrigin)
			+ " ] { currentfile imgstring "
			+ "readhexstring pop } ");

		switch (spp) {
		case (1): out.write("image"); break;
		case (3): out.write("false 3 colorimage"); break;
		}

		switch (imgType) {
		case ImagePlus.GRAY8:
			if (saveAsBitmap)
				writeBitmap(out, (ByteProcessor) ip);
			else
				writeGray(out, (ByteProcessor) ip);
			break;
		case ImagePlus.GRAY16:
			writeGray(out, (ShortProcessor) ip);
			break;
		case ImagePlus.GRAY32:
			writeGray(out, (FloatProcessor) ip);
			break;
		case ImagePlus.COLOR_RGB:
			writeColor(out, (ColorProcessor) ip);
			break;
		case ImagePlus.COLOR_256:
			writeColor(out, (ByteProcessor) ip);
			break;
		default:
			throw new Error("Unknown image type");
		}
	}

	void writeTrailer(Writer out) throws IOException {
		out.write("\n%%Trailer\n");
		out.write("cleartomark countdictstack exch sub { end } repeat\n");
		out.write("restore\n");
		out.write("%%EOF\n");
	}

	void writeBitmap(Writer out, ByteProcessor bp) throws IOException {
		int w = bp.getWidth();
		int h = bp.getHeight();
		int n = (w % 8 == 0) ? w/8 : w/8 + 1;
		int[] line = new int[w];
		byte[] packedLine = new byte[n];
		for (int v = 0; v < h; v++) {
			// encode and write one image line
			bp.getRow(0, v, line, w);
			packOneLine(line, packedLine);
			for (int i = 0; i < packedLine.length; i++) {
				if ((i % epsLineLength) == 0)
					out.write("\n");
				int p = packedLine[i] & 0xFF;
				out.write(toHex(p));
			}
		}
	}

	void packOneLine (int[] line, byte[] packed) {
		int i = 0;
		for (int j=0; j<packed.length; j++) {
			int b = 0;	// pack one byte
			for (int bit=0; bit<8; bit++) {
				b = b << 1;
				if (i < line.length) {
					if (line[i] > 0)
						b = b | 0x01;
					i = i + 1;
				}
			}
			packed[j] = (byte) (b & 0xFF);
		}
	}

	void writeGray(Writer out, ByteProcessor bp) throws IOException {
		byte[] pixels = (byte[]) bp.getPixels();
		for (int i = 0; i < pixels.length; i++) {
			if ((i % epsLineLength) == 0)
				out.write("\n");
			int p = pixels[i] & 0xFF;
			out.write(toHex(p));
		}
	}

	void writeGray(Writer out, ShortProcessor sp) throws IOException {
		float min = (float) sp.getMin();
		float max = (float) sp.getMax();
		float offset = min;
		float scale =  1;
		if (max - min > 0.001) {
			scale = 255 * (max - min);
		}
		short[] pixels = (short[]) sp.getPixels();
		for (int i = 0; i < pixels.length; i++) {
			if ((i % epsLineLength) == 0)
				out.write("\n");
			int p = pixels[i] & 0xFFFF;
			// normalize to [0,255]
			int pn = (int) Math.round((p - offset) * scale);
			out.write(toHex(pn));
		}
	}

	void writeGray(Writer out, FloatProcessor fp) throws IOException {
		float min = (float) fp.getMin();
		float max = (float) fp.getMax();
		float offset = min;
		float scale =  1;
		if (max - min > 0.001) {
			scale = 255 * (max - min);
		}
		float[] pixels = (float[]) fp.getPixels();
		for (int i = 0; i < pixels.length; i++) {
			if ((i % epsLineLength) == 0)
				out.write("\n");
			int pn = (int) Math.round((pixels[i] - offset) * scale);
			out.write(toHex(pn));
		}
	}

	void writeColor(Writer out, ColorProcessor cp) throws IOException {
		final int lineLength = epsLineLength/3;
		int[] pixels = (int[])cp.getPixels();
		for (int i = 0; i < pixels.length; i++) {
			if ((i % lineLength) == 0)
				out.write("\n");
			int c = pixels[i];
			out.write(toHex((c >> 16) & 0xff));
			out.write(toHex((c >> 8) & 0xff));
			out.write(toHex(c & 0xff));
		}
	}

	void writeColor(Writer out, ByteProcessor bp) throws IOException {
		final int lineLength = epsLineLength/3;
		ColorModel cm = bp.getColorModel();
		byte[] pixels = (byte[])bp.getPixels();
		for (int i = 0; i < pixels.length; i++) {
			if ((i % lineLength) == 0)
				out.write("\n");
			int value = 0xff & pixels[i];
			out.write(toHex((cm.getRed(value) & 0xff)));
			out.write(toHex((cm.getGreen(value) & 0xff)));
			out.write(toHex(cm.getBlue(value) & 0xff));
		}
	}

	private final char[] hexChar =
		{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	private final char hexByte[] = new char[2];

	char[] toHex(int m) {
		// m is assumed to be in [0,255]
		hexByte[0] = hexChar[(m >> 4) & 0xf];
		hexByte[1] = hexChar[m & 0xf];
		return hexByte;
	}

	String stripFileExtension(String fileName) {
		int dotInd = fileName.lastIndexOf('.');
		// if dot is in the first position,
		// we are dealing with a hidden file rather than an extension
		return (dotInd > 0) ? fileName.substring(0, dotInd) : fileName;
	}
}
