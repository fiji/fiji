
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.gui.GenericDialog;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

import mpicbg.imglib.type.numeric.RealType;

/**
 * Stochastic Image Denoising Plugin
 *
 * Implementation of the stochastic image denoising algotithm as proposed by:
 *
 *   Francisco Estrada, David Fleet, Allan Jepson
 *   Stochastic Image Denoising
 *   British Mashine Vision Conference 2009
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 */



/**
 * Plugin interface to the stochastic image denoising algorithm.
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 * @version 0.1
 */
public class Stochastic_Denoise<T extends RealType<T>> implements PlugInFilter {

	// the image to process
	private Image<T> image;

	// the ImagePlus version of it
	private ImagePlus imp;

	// the denoised image
	private Image<T> denoised;

	// the ImagePlus version of it
	private ImagePlus dns;

	// image dimensions
	private int[] dimensions;

	// the algorithm implementation
	private StochasticDenoise<T> stochasticDenoise;

	private final double SIGMA_INIT = 0.15f;
	private final int    PATHS_INIT = 20;

	// number of random walks per pixel
	private int numSamples = PATHS_INIT;

	// minimal probability for a path
	private double minProb = 1e-6f;

	// parameter of the probability function
	private double sigma = SIGMA_INIT;

	/**
	 * This method gets called by ImageJ / Fiji to determine
	 * whether the current image is of an appropriate type.
	 *
	 * @param arg can be specified in plugins.config
	 * @param img is the currently opened image
	 */
	public int setup(String arg, ImagePlus imp) {

		this.imp = imp;

		return DOES_8G | DOES_8C | DOES_RGB | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {

		IJ.log("Starting plugin Stochastic Denoise");

		// read image
		//imp = WindowManager.getCurrentImage();
		//if (imp == null) {
			//IJ.showMessage("Please open an image first.");
			//return;
		//}

		// ask for parameters
		GenericDialog gd = new GenericDialog("Settings");
		gd.addNumericField("noise standard deviation:", sigma, 2);
		gd.addSlider("number of samples:", 1, 100, numSamples);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
	
		sigma      = gd.getNextNumber();
		numSamples = (int)gd.getNextNumber();
	
		image      = ImagePlusAdapter.wrap(imp);
		dimensions = image.getDimensions();
		int width  = dimensions[0];
		int height = dimensions[1];
		int slices = 1;
		if (dimensions.length > 2)
			slices = dimensions[2];
	
		// prepare segmentation image
		dns = imp.createImagePlus();
		ImageStack stack = new ImageStack(width, height);
		for (int s = 1; s <= slices; s++) {
			ImageProcessor duplProcessor = imp.getStack().getProcessor(s).duplicate();
			stack.addSlice("", duplProcessor);
		}
		dns.setStack(stack);
		dns.setDimensions(1, slices, 1);
		if (slices > 1)
			dns.setOpenAsHyperStack(true);
	
		dns.setTitle("denoised " + imp.getTitle());
	
		denoised = ImagePlusAdapter.wrap(dns);
	
		// set up algorithm
		stochasticDenoise = new StochasticDenoise<T>();
		stochasticDenoise.setParameters(numSamples, minProb, sigma);
	
		Thread processThread = new Thread(new Runnable() {
			public void run() {
				stochasticDenoise.process(image, denoised);
				dns.show();
				dns.updateAndDraw();
			}
		});
		processThread.start();

		// wait for the thread to finish
		try {
			processThread.join();
		} catch (InterruptedException e) {
			processThread.interrupt();
		}
	}
}
