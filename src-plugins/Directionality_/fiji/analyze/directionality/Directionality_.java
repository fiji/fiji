package fiji.analyze.directionality;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


public class Directionality_ implements PlugInFilter {
	
	/*
	 * FIELDS
	 */
	protected ImagePlus imp;
	protected int width, height, small_side, long_side, npady, npadx, step, pad_size;
	private FloatProcessor padded_square_block; 

	public void run(ImageProcessor _ip) {
		final FloatProcessor ip = (FloatProcessor) _ip; // we can do that, for the flag of this plugin is set accordingly
		
		ImageProcessor square_block;
		Roi square_roi;
		FHT fft, pspectrum;
		// If the image is not square, split it in small square padding all the image
		for (int ix = 0; ix<npadx; ix++) {
			
			for (int iy = 0; iy<npady; iy++) {
				
				// Extract a square block from the image
				square_roi = new Roi( ix*step, iy*step, small_side, small_side );
				ip.setRoi(square_roi);
				square_block = ip.crop();
				padded_square_block.setValue(0.0);
				padded_square_block.fill();
				padded_square_block.insert(square_block, 0, 0);
				// Computes its FFT
				fft = new FHT(square_block);
				fft.setShowProgress(false);
				fft.transform();
				pspectrum = fft.conjugateMultiply(fft);
				new ImagePlus("FFT", pspectrum).show();				
			}
		}
		
		
	}

	public int setup(String arg, ImagePlus _imp) {
		// Assign fields
		this.imp = _imp;
		this.width = _imp.getWidth();
		this.height = _imp.getHeight();
		// Compute dimensions
		if ( width == height) {
			npadx = 1;
			npady = 1;
			long_side = width;
			small_side = width;
			step = 0;
		} else {
			small_side = Math.min(width, height);
			long_side  = Math.max(width, height);
			int npad = long_side / small_side + 1;
			if (width == long_side) {
				npadx = npad;
				npady = 1;
			} else {
				npadx = 1;
				npady = npad;
			}
			int delta = (long_side - small_side);
			step = delta / (npad-1);
		}
		// Computes power of 2 image dimension
		pad_size = 2;
        while(pad_size<small_side) pad_size *= 2;
		padded_square_block = new FloatProcessor(pad_size, pad_size);
		
		// Flags
		return DOES_ALL
			+ DOES_STACKS
			+ CONVERT_TO_FLOAT
			+ NO_CHANGES
			+ PARALLELIZE_STACKS;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private final double[] getBlackmanPeriodicWindow(int n) {
		final double[] window = new double[n];
		for (int i = 0; i < window.length; i++) {
			window[i] = 0.42 - 0.5 * Math.cos(2*Math.PI*i/n) + 0.08 * Math.cos(4*Math.PI/n);
		}		
		return window;
	}

}
