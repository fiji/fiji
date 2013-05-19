package ij3d;

import javax.vecmath.Color3f;

import java.awt.Color;
import java.awt.image.IndexColorModel;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;



public class ColorTable {

	public static Color3f getColor(String name) {
		for(int i = 0; i < colors.length; i++) {
			if(colorNames[i].equals(name)){
				return colors[i];
			}
		}
		return null;
	}

	public static String[] colorNames = new String[]{"None", "Black",
				"White", "Red", "Green", "Blue", "Cyan",
				"Magenta", "Yellow"};

	public static Color3f[] colors = {
				null,
				new Color3f(0,    0,    0),
				new Color3f(1f, 1f, 1f),
				new Color3f(1f, 0,    0),
				new Color3f(0,    1f, 0),
				new Color3f(0,    0,    1f),
				new Color3f(0,    1f, 1f),
				new Color3f(1f, 0,    1f),
				new Color3f(1f, 1f, 0)};

	public static boolean isRedCh(String color) {
		return color.equals("White") || color.equals("Red") ||
				color.equals("Magenta") || color.equals("Yellow");
	}

	public static boolean isGreenCh(String color) {
		return color.equals("White") || color.equals("Green") ||
				color.equals("Cyan") || color.equals("Yellow");
	}

	public static boolean isBlueCh(String color) {
		return color.equals("White") || color.equals("Blue") ||
				color.equals("Cyan") || color.equals("Magenta");
	}

	public static String getColorName(Color3f col) {
		for(int i = 1; i < colors.length; i++) {
			if(colors[i].equals(col))
				return colorNames[i];
		}
		return "None";
	}

	public static int getHistogramMax(ImagePlus imp) {
		int d = imp.getStackSize();
		int[] hist = new int[256];
		for(int i = 0; i < d; i++) {
			int[] h = imp.getStack().getProcessor(i+1).
						getHistogram();
			for(int j = 0; j < hist.length; j++) {
				hist[j] += h[j];
			}
		}
		int max = -1, maxIndex = -1;
		for(int j = 0; j < hist.length; j++) {
			if(hist[j] > max) {
				max = hist[j];
				maxIndex = j;
			}
		}
		return maxIndex;
	}

	public static IndexColorModel getOpaqueIndexedColorModel(ImagePlus imp,
				boolean[] ch) {


		IndexColorModel cmodel = (IndexColorModel)
					imp.getProcessor().getColorModel();
		int N = cmodel.getMapSize();
		byte[] r = new byte[N];
		byte[] g = new byte[N];
		byte[] b = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		for(int i = 0; i < N; i++) {
			r[i] = ch[0] ? r[i] : 0;
			g[i] = ch[1] ? g[i] : 0;
			b[i] = ch[2] ? b[i] : 0;
		}
		IndexColorModel c = new IndexColorModel(8, N, r, g, b);
		return c;
	}

	public static IndexColorModel getIndexedColorModel(ImagePlus imp,
				boolean[] ch) {


		IndexColorModel cmodel = (IndexColorModel)
					imp.getProcessor().getColorModel();
		int N = cmodel.getMapSize();
		byte[] r = new byte[N];
		byte[] g = new byte[N];
		byte[] b = new byte[N];
		byte[] a = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		// index in cmodel which has most pixels:
		// this is asumed to be the background value
		int histoMax = getHistogramMax(imp);
		int[] sumInt = new int[N];
		int maxInt = 0;
		for(int i = 0; i < N; i++) {
			sumInt[i] = 0;
			if(ch[0]) sumInt[i] += ((int)r[i] & 0xff);
			if(ch[1]) sumInt[i] += ((int)g[i] & 0xff);
			if(ch[2]) sumInt[i] += ((int)b[i] & 0xff);
			maxInt = sumInt[i] > maxInt ? sumInt[i] : maxInt;
		}

		float scale = 255f / maxInt;
		for(int i = 0; i < N; i++) {
			byte meanInt = (byte)(scale * sumInt[i]);
			r[i] = ch[0] ? r[i] : 0;
			g[i] = ch[1] ? g[i] : 0;
			b[i] = ch[2] ? b[i] : 0;
			a[i] = meanInt;
		}
		a[histoMax] = (byte)0;
		IndexColorModel c = new IndexColorModel(8, N, r, g, b, a);
		return c;
	}

	public static IndexColorModel getOpaqueAverageGrayColorModel(
			ImagePlus imp, boolean[] ch) {

		IndexColorModel cmodel = (IndexColorModel)
					imp.getProcessor().getColorModel();
		int N = cmodel.getMapSize();
		byte[] r = new byte[N];
		byte[] g = new byte[N];
		byte[] b = new byte[N];
		byte[] a = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		int[] sumInt = new int[N];
		int maxInt = 0;
		for(int i = 0; i < N; i++) {
			sumInt[i] = 0;
			if(ch[0]) sumInt[i] += ((int)r[i] & 0xff);
			if(ch[1]) sumInt[i] += ((int)g[i] & 0xff);
			if(ch[2]) sumInt[i] += ((int)b[i] & 0xff);
			maxInt = sumInt[i] > maxInt ? sumInt[i] : maxInt;
		}

		float scale = 255f / maxInt;
		for(int i = 0; i < N; i++) {
			byte meanInt = (byte)(scale * sumInt[i]);
			float colFac = (float)sumInt[i] / (float)maxInt;
				r[i] = meanInt;
				g[i] = meanInt;
				b[i] = meanInt;
		}
		IndexColorModel c =
				new IndexColorModel(8, N, r, g, b);
		return c;

	}

	public static IndexColorModel getAverageGrayColorModel(
			ImagePlus imp, boolean[] ch) {

		IndexColorModel cmodel = (IndexColorModel)
					imp.getProcessor().getColorModel();
		int N = cmodel.getMapSize();
		byte[] r = new byte[N];
		byte[] g = new byte[N];
		byte[] b = new byte[N];
		byte[] a = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		// index in cmodel which has most pixels:
		// this is asumed to be the background value
		int histoMax = getHistogramMax(imp);
		int[] sumInt = new int[N];
		int maxInt = 0;
		for(int i = 0; i < N; i++) {
			sumInt[i] = 0;
			if(ch[0]) sumInt[i] += ((int)r[i] & 0xff);
			if(ch[1]) sumInt[i] += ((int)g[i] & 0xff);
			if(ch[2]) sumInt[i] += ((int)b[i] & 0xff);
			maxInt = sumInt[i] > maxInt ? sumInt[i] : maxInt;
		}

		float scale = 255f / maxInt;
		for(int i = 0; i < N; i++) {
			byte meanInt = (byte)(scale * sumInt[i]);
			float colFac = (float)sumInt[i] / (float)maxInt;
				r[i] = meanInt;
				g[i] = meanInt;
				b[i] = meanInt;
				a[i] = meanInt;
		}
		a[histoMax] = (byte)0;
		IndexColorModel c =
				new IndexColorModel(8, N, r, g, b, a);
		return c;

	}

	public static ImagePlus adjustChannels(ImagePlus imp, boolean[] ch) {

		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		int[] weight = new int[3];
		IndexColorModel cmodel =
			(IndexColorModel)imp.getProcessor().getColorModel();
		int N = cmodel.getMapSize();
		byte[] r = new byte[N];
		byte[] g = new byte[N];
		byte[] b = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		float sum = 0;
		for(int i = 0; i < 3; i++) {
			if(ch[i]) {
				weight[i] = 1;
				sum++;
			}
		}

		ImageStack res = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			byte[] bytes =
			(byte[])imp.getStack().getProcessor(z+1).getPixels();
			byte[] newB = new byte[w * h];
			for(int i = 0; i < w*h; i++) {
				int index = bytes[i] & 0xff;
				int value = (weight[0] * (int)(r[index]&0xff) +
					weight[1] * (int)(g[index]&0xff) +
					weight[2] * (int)(b[index]&0xff));
				newB[i] = (byte)(value/sum);

			}
			res.addSlice("", new ByteProcessor(w, h, newB, null));
		}
		ImagePlus newImage = new ImagePlus(imp.getTitle(), res);
		newImage.setCalibration(imp.getCalibration());
		return newImage;
	}

	public static void debug(IndexColorModel cmodel) {
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		for(int i = 0; i < 256; i++) {
			System.out.println((r[i] & 0xff) + "\t" +
						 (g[i] & 0xff) + "\t" +
						 (b[i] & 0xff));
		}
	}
}
