/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;

import process3d.DistanceTransform3D;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;

import vib.app.FileGroup;
import vib.app.gui.FileGroupDialog;

/**
 * Rohlfing, Maurer (2007): Shape-based averaging
 */
public class RohlfingSBA implements PlugIn {

	private FileGroup fg;
	private ImagePlus D_min;
	private ImagePlus output;

	private int w, h, d, L, K;

	public void run(String arg) {

		Pattern macOSPattern = Pattern.compile("^Mac ?OS.*$",Pattern.CASE_INSENSITIVE);
		String osName = (String)System.getProperties().get("os.name");
		if( osName != null && macOSPattern.matcher(osName).matches() ) {
			IJ.error("The Shaped-Based Averaging plugin "+
				 "is currently disabled on Mac OS due to Bug 29.");
			return;
		}

		GenericDialog gd = new GenericDialog("Rohlfing");
		fg = new FileGroup("files");
		FileGroupDialog fgd = new FileGroupDialog(fg, false);
		gd.addPanel(fgd);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		init();
		doit();
	}

	ImagePlus calculate( ImagePlus a, String operation, ImagePlus b ) {
		String parameter=operation+" 32 stack";
		ImageCalculatorRevised calculator=new ImageCalculatorRevised();
		return calculator.calculateResult(parameter,a,b);
	}

	public void setFileGroup( FileGroup fg ) {
		this.fg = fg;
	}

	public ImagePlus doit() {
		ImagePlus D = null;
		IJ.showProgress(0.0);
		for(int l = 0; l < L; l++) {
			/*
			if(l != 0 && l != 85 && l != 120
				&& l != 132 && l != 153 && l != 170) {
				continue;
			}
			*/
			/*
			if( ! (l == 255 || (l % 16) == 0) )
				continue;
			*/
			System.out.println("At level "+l);
			// Sum up the distance maps of all input images
			for(int k = 0; k < K; k++) {
				System.out.println("  Doing image "+k);
				if(D == null) {
					D = d_kl(l, k);
				} else {
					ImagePlus tmp=d_kl(l, k);
					ImagePlus result=calculate(D,"add",tmp);
					D.close();
					tmp.close();
					D = result;
				}
				IJ.showProgress((l*K+k)/(double)(L*K));
			}
			for(int z = 0; z < d; z++) {
				// Devide it by the number of input images
				D.getStack().getProcessor(z+1).multiply(1.0/K);
				float[] D_p = (float[])D.getStack()
						.getProcessor(z+1).getPixels();
				float[] D_minp = (float[])D_min.getStack()
						.getProcessor(z+1).getPixels();
				byte[] output_p = (byte[])output.getStack()
						.getProcessor(z+1).getPixels();
				// if average distance is smaller than
				// min distance, output is l
				for(int i = 0; i < w*h; i++) {
					if(D_p[i] < D_minp[i]) {
						output_p[i] = (byte)l;
						D_minp[i] = D_p[i];
					}
				}
			}
		}
		IJ.showProgress(1);
		output.show();
		return output;
	}

	private ImagePlus d_kl(int l, int k) {
		File file = fg.get(k);
		ImagePlus image = BatchOpener.openFirstChannel( file.getAbsolutePath() );
		// Remember: need signed dist transform
		// Inside EDT
		ImagePlus binary = createBinary(image, l);
		image.close();
		ImagePlus im1 = new DistanceTransform3D()
			.getTransformed(binary, 0);
		// Outside EDT
		ImagePlus im2 = new DistanceTransform3D()
			.getTransformed(binary, 255);
		binary.close();
		// Subtract Inside EDT from Outside EDT
		ImagePlus result=calculate(im2,"sub",im1);
		im1.close();
		im2.close();
		return result;
	}

	private ImagePlus createBinary(ImagePlus image, int value) {
		int w = image.getWidth(), h = image.getWidth();
		int d = image.getStackSize();
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			byte[] f = new byte[w*h];
			byte[] p = (byte[])image.getStack()
					.getProcessor(z+1).getPixels();
			for(int i = 0; i < w*h; i++)
				f[i] = (int)(p[i]&0xff) == value ? (byte)255 : 0;
			stack.addSlice("", new ByteProcessor(w, h, f, null));
		}
		return new ImagePlus("Label_" + (int)(value&0xff), stack);
	}

	public void init() {
		// Open the first image just to get the dimensions:
		File file = fg.get(0);
		ImagePlus image = BatchOpener.openFirstChannel( file.getAbsolutePath() );
		w = image.getWidth();
		h = image.getHeight();
		d = image.getStackSize();
		image.close();

		// The number of labels:
		L = 256;
		// The number of images:
		K = fg.size();

		// Initialize the output values to L:
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			stack.addSlice("", new ByteProcessor(w, h));
		}
		output = new ImagePlus("Output", stack);

		// Initialize D_min to "infinity":
		stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			float[] f = new float[w*h];
			Arrays.fill( f, Float.MAX_VALUE );
			stack.addSlice("", new FloatProcessor(w, h, f, null));
		}
		D_min = new ImagePlus("D_min", stack);
	}
}
