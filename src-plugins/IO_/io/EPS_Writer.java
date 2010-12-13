package io;

/*
 * This plugin writes a PS/EPSF.
 * put into the public domain by Johannes Schindelin in 2006.
 */
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.image.ColorModel;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EPS_Writer implements PlugIn {

	public void run(String arg) {
		ImagePlus img=WindowManager.getCurrentImage();
		if(img==null) {
			IJ.error("Which image?");
			return;
		}
		if (img.getStackSize() != 1) {
			IJ.error("Can only save 2D images!");
			return;
		}
		String title=img.getTitle();
		int length=title.length();
		for(int i=2;i<5;i++)
			if(length>i+1 && title.charAt(length-i)=='.') {
				title=title.substring(0,length-i);
				break;
			}
		String extension = ".eps";
		SaveDialog od = new SaveDialog("EPS Writer", title, extension);
		String dir=od.getDirectory();
		String name=od.getFileName();
		if(name==null)
			return;

		Calibration cal = img.getCalibration();
		ImageProcessor processor = img.getProcessor();
		Object pixels = processor.getPixels();
		min = processor.getMin();
		max = processor.getMax();
		int type = img.getType();
		boolean isGray = (type == img.GRAY8 ||
				type == img.GRAY16 ||
				type == img.GRAY32);
		int w = img.getWidth(), h = img.getHeight();
		double realW = w * cal.pixelWidth;
		double realH = h * cal.pixelHeight;
		String now = new SimpleDateFormat().format(new Date());
		int spp = (isGray ? 1 : 3);
		int bps = (type == img.GRAY32  || type == img.GRAY16 ? 16 : 8);

		String preamble = "%!PS-Adobe-2.0 EPSF-1.2\n"
			+ "%%Creator: (ImageJ PS Writer)\n"
			+ "%%Title: (" + name + ")\n"
			+ "%%CreationDate: (" + now + ")\n"
			+ "%%Pages: 1\n"
			+ "%%BoundingBox: "
			+ cal.xOrigin + " " + cal.yOrigin + " "
			+ (cal.xOrigin + realW) + " "
			+ (cal.yOrigin + realH) + "\n"
			+ "%%EndComments\n"
			+ "\n"
			+ "%%BeginProlog\n"
			+ "save countdictstack mark newpath /showpage {} "
			+ "def /setpagedevice {pop} def\n"
			+ "%%EndProlog\n"
			+ "%%Page 1 1\n"
			+ "\n"
			+ "/imgstring " + spp + " string def\n"
			+ w + " " + h + " " + bps + " [ "
			+ (1 / cal.pixelWidth) + " 0 0 "
			+ (-1 / cal.pixelHeight) + " "
			+ cal.xOrigin + " " + (realH - cal.yOrigin)
			+ " ] { currentfile imgstring "
			+ "readhexstring pop } ";

		if (isGray)
			preamble += "image";
		else
			preamble += "false 3 colorimage";

		String trailer = "\n%%Trailer\n"
			+ "cleartomark countdictstack exch sub { end } "
			+ "repeat restore\n"
			+ "%%EOF\n";

		IJ.showStatus("Writing PS "+dir+name+"...");

		try {
			//OutputStream fileOutput =
			//	new FileOutputStream(dir + name);
			//DataOutputStream output =
			//	new DataOutputStream(fileOutput);
			FileWriter output = new FileWriter(dir + name);

			output.write(preamble);
			switch (type) {
				case ImagePlus.GRAY8:
					writeGray(output, pixels, w, h);
					break;
				case ImagePlus.GRAY16:
					writeGray16(output, pixels, w, h);
					break;
				case ImagePlus.GRAY32:
					writeGray32(output, pixels, w, h);
					break;
				case ImagePlus.COLOR_RGB:
					writeColor(output, processor, w, h);
					break;
				case ImagePlus.COLOR_256:
					writeColor8(output, processor, w, h);
					break;
			}
			output.write(trailer);
			output.flush();
			output.close();
		} catch(IOException e) {
			e.printStackTrace();
			IJ.error("Error writing file");
		}
		IJ.showStatus("");
	}

	double min, max;

	final static String[] hex = {
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
		"a", "b", "c", "d", "e", "f"
	};

	String toHex(byte b) {
		int i = (b < 0 ? b + 256 : b);
		i = (int)((i - min) * 255 / max);
		return hex[i >> 4] + hex[i & 0xf];
	}

	String toHex(short s) {
		int i = (s < 0 ? s + 65536 : s);
		i = (int)((i - min) * 65535 / max);
		return hex[i >> 12] + hex[(i >> 8) & 0xf]
			+ hex[(i >> 4) & 0xf] + hex[i & 0xf];
	}

	String toHex(float f) {
		int i = (int)((f - min) * 65535 / max);
		return hex[i >> 12] + hex[(i >> 8) & 0xf]
			+ hex[(i >> 4) & 0xf] + hex[i & 0xf];
	}

	void writeGray(Writer output, Object pixels, int w, int h)
			throws IOException {
		byte[] p = (byte[])pixels;
		for (int i = 0; i < w * h; i++) {
			if ((i % 38) == 0)
				output.write("\n");
			output.write(toHex(p[i]));
		}
	}

	void writeGray16(Writer output, Object pixels, int w, int h)
			throws IOException {
		short[] p = (short[])pixels;
		for (int i = 0; i < w * h; i++) {
			if ((i % 38) == 0)
				output.write("\n");
			output.write(toHex(p[i]));
		}
	}

	void writeGray32(Writer output, Object pixels, int w, int h)
			throws IOException {
		float[] p = (float[])pixels;
		for (int i = 0; i < w * h; i++) {
			if ((i % 38) == 0)
				output.write("\n");
			output.write(toHex(p[i]));
		}
	}

	void writeColor(Writer output, ImageProcessor processor, int w, int h)
			throws IOException {
		int[] p = (int[])processor.getPixels();
		int count = 0;
		min = 0;
		max = 255;
		for (int i = 0; i < w * h; i++) {
			if ((count % 39) == 0)
				output.write("\n");
			int c = p[i];
			output.write(toHex((byte)((c >> 16) & 0xff)));
			output.write(toHex((byte)((c >> 8) & 0xff)));
			output.write(toHex((byte)(c & 0xff)));
			count += 3;
		}
	}

	void writeColor8(Writer output, ImageProcessor processor, int w, int h)
			throws IOException {
		ColorModel cm = processor.getColorModel();
		byte[] p = (byte[])processor.getPixels();
		int count = 0;
		min = 0;
		max = 255;
		for (int i = 0; i < w * h; i++) {
			if ((count % 39) == 0)
				output.write("\n");
			int value = (p[i] < 0 ? p[i] + 256 : p[i]);
			output.write(toHex((byte)cm.getRed(value)));
			output.write(toHex((byte)cm.getGreen(value)));
			output.write(toHex((byte)cm.getBlue(value)));
			count += 3;
		}
	}
};
