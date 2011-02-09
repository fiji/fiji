//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio, Jesse Berent, Niels Quack
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
//==============================================================================

package edf;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import imageware.Builder;
import imageware.FMath;
import imageware.ImageWare;

public class Tools {

	/**
	 * Determine the number of scale and the size of the image
	 * for the wavelet-based methods.
	 */
	public static int[] computeScaleAndPowerTwoSize(int nx, int ny) {
		int scale = 1;
		int mx = nx;
		int my = ny;
		int lx = nx;
		int scalex = 0;
		while( lx > 1) {
			scalex++;
			if ( lx % 2 == 0)
				lx = lx / 2;
			else
				lx = (lx+1)/2;
		}
		int ly = ny;
		int scaley = 0;
		while( ly > 1) {
			scaley++;
			if ( ly % 2 == 0)
				ly = ly / 2;
			else
				ly = (ly+1)/2;
		}
		scale = (scalex < scaley ? scalex : scaley);
		mx = lx * FMath.round(Math.pow(2, scalex));
		my = ly * FMath.round(Math.pow(2, scaley));
		return new int[]{scale,mx,my};
	}

	/**
	 * Extends the image to [mx, my]
	 */
	public static ImageWare extend(ImageWare in, int mx, int my) {
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nz = in.getSizeZ();

		ImageWare out = Builder.create(mx, my, nz, in.getType());
		int a = (mx-nx)/2;
		int b = (my-ny)/2;
		out.putXYZ(a, b, 0, in);
		return out;
	}

	/**
	 * Crop to the original size.
	 */
	public static ImageWare crop(ImageWare in, int nx, int ny) {
		int mx = in.getSizeX();
		int my = in.getSizeY();
		int mz = in.getSizeZ();

		int a = (mx-nx)/2;
		int b = (my-ny)/2;
		ImageWare out = Builder.create(nx, ny, mz, in.getType());
		in.getXYZ(a, b, 0, out);
		return out;
	}

	/**
	 *
	 */
	public static boolean isPowerOf2(int n){
		return n>0 && ( n & (n-1))==0;
	}

	/**
	 *
	 */
	public static ImageProcessor getImageProcessor(ImageWare iw){
		ImageProcessor ip = null;

		Object[] pixels = iw.getVolume();
		int size = iw.getSizeX() * iw.getSizeY();

		switch(iw.getType()){
		case ImageWare.BYTE:
			ip = new ByteProcessor(iw.getSizeX(),iw.getSizeY());
			byte[] bsrc = new byte[size];
			for (int k=0; k<size; k++) {
				bsrc[k] = ((byte[])pixels[0])[k];
			}
			ip.setPixels(bsrc);
			break;
		case ImageWare.SHORT:
			ip = new ShortProcessor(iw.getSizeX(),iw.getSizeY());
			short[] ssrc = new short[size];
			for (int k=0; k<size; k++)
				ssrc[k] = ((short[])pixels[0])[k];
			ip.setPixels(ssrc);
			break;
		case ImageWare.FLOAT:
			ip = new FloatProcessor(iw.getSizeX(),iw.getSizeY());
			float[] fsrc = new float[size];
			for (int k=0; k<size; k++)
				fsrc[k] = ((float[])pixels[0])[k];
			ip.setPixels(fsrc);
			break;
		default:
			throw new RuntimeException("Error creating processor.");
		}
		return ip;
	}


	/**
	 *
	 */
	public static void waveletDenoising(ImageWare coeff, double rateDenoising){

		double th = computeThreshold(coeff, rateDenoising);

		LogSingleton log = LogSingleton.getInstance();
		log.acknowledge();
		log.start("Denoisng (Hard Threshold :" + rateDenoising+ "%)...");

		int nxa = coeff.getWidth();
		int nya = coeff.getHeight();
		for (int i = 0; i<nxa; i++)
			for (int j = 0; j<nya; j++)
				if (Math.abs(coeff.getPixel(i, j, 0)) < th){
					coeff.putPixel(i, j, 0, 0.0);
				}
		log.acknowledge();
	}

	/**
	 * Compute the threshold corresponding to a percentage of values
	 * (rate) in an image.
	 * The answer is a approximation based on a cumulative histogram.
	 *
	 * @param image	image
	 * @param rate	percentage of null values [0%..100%]
	 * @return		the desired threshold
	 */
	private static double computeThreshold(ImageWare image, double rate) {
		int nx = image.getWidth();
		int ny = image.getHeight();
		ImageWare absImage = image.duplicate();
		absImage.abs();

		if (rate <= 0.0) {
			return absImage.getMinimum();
		}
		if (rate >= 100.0) {
			return absImage.getMaximum();
		}
		double[][] histogram = generateHistogram(absImage, 10000);
		int index = 0;
		double sum   = 0.0;
		double thresholdGoal = (rate * nx * ny) / 100.0;
		while (sum <= thresholdGoal) {
			sum += histogram[index][1];
			index++;
		}

		double nbPointFore = sum;
		double nbPointBack = sum - histogram[index-1][1];
		double dist;

		if (nbPointFore == nbPointBack)
			dist = 0.5;
		else
			dist = (thresholdGoal - nbPointBack) / (nbPointFore-nbPointBack);

		return dist*histogram[index][0] + (1.0-dist)*histogram[index-1][0];
	}

	/**
	 * Generate a histogram with a specified number of bins.
	 * The result is an array of two columns, the first the position
	 * of quantized levels and the second contains the number of
	 * pixel corresponding to the quantized levels.
	 *
	 * @param image		input image
	 * @param numberBins	number of bins
	 * @return			the histogram array [numberBins][2]
	 */
	private static double[][] generateHistogram(ImageWare image, final int numberBins) {
		if (numberBins <= 0) {
			throw new ArrayStoreException("Unexpected number of bins.");
		}

		int nx = image.getWidth();
		int ny = image.getHeight();

		double maxi = image.getMaximum();
		double mini = image.getMinimum();
		double histogram[][] = new double[numberBins][2];
		for (int h=0; h<numberBins; h++)
			histogram[h][1] = 0.0;

		if (maxi <= mini) {
			for (int h=0; h<numberBins; h++){
				histogram[h][1] = (double)nx*ny;
				histogram[h][0] = mini;
			}
			return histogram;

		}

		double step = (maxi-mini) / numberBins;
		double b = mini + step/2.0;
		for (int h=0; h<numberBins; h++)
			histogram[h][0] = h*step + b;

		int index;
		double val;
		for (int x=0; x<nx; x++) {
			for (int y=0; y<ny; y++) {
				val = image.getPixel(x,y,0);
				index = (int)Math.floor( (val - mini) / step);
				if (index >= numberBins)
					index = numberBins-1;
				histogram[index][1]++;
			}
		}
		return histogram;
	}
}
