
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import ij.plugin.PlugIn;

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
public class Stochastic_Denoise<T extends RealType<T>> implements PlugIn {

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

	// number of random walks per pixel
	private int numSamples = 30;

	// minimal probability for a path
	private float minProb = 1e-6f;

	// parameter of the probability function
	private float sigma = 30.0f;

	public void run(String arg) {

		IJ.log("Starting plugin Stochastic Denoise");

		// read image
		imp   = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Please open an image first.");
			return;
		}

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

		// start algorithm
		stochasticDenoise.process(image, denoised);

		dns.show();
		dns.updateAndDraw();
	}
}
