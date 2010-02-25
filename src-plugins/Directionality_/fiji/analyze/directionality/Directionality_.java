package fiji.analyze.directionality;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


public class Directionality_ implements PlugInFilter {
	
	/*
	 * FIELDS
	 */
	
	private static final float FREQ_THRESHOLD = 5.0f; // get rid of pixels too close to the center in the FFT spectrum
	private static final boolean DEBUG = true;
	
	protected ImagePlus imp;
	protected ImageStack filters;
	protected FloatProcessor window, r, theta;
	protected int width, height, small_side, long_side, npady, npadx, step, pad_size, nbins;
	/** The bin centers, in degrees */
	protected float[] bins;
	
	private FloatProcessor padded_square_block; 
	private float[] window_pixels;

	/*
	 * RUN METHOD
	 */
	
	public void run(ImageProcessor _ip) {
		final FloatProcessor ip = (FloatProcessor) _ip; // we can do that, for the flag of this plugin is set accordingly
		final Roi original_square = new Roi((pad_size-small_side)/2, (pad_size-small_side)/2, small_side, small_side); 

		float[] fpx, spectrum_px;
		float[] directionality = new float[nbins];
		ImageProcessor square_block;
		Roi square_roi;
		FHT fft, pspectrum;		
		FloatProcessor small_pspectrum;
		
		// If the image is not square, split it in small square padding all the image
		for (int ix = 0; ix<npadx; ix++) {
			
			for (int iy = 0; iy<npady; iy++) {
				
				// Extract a square block from the image
				square_roi = new Roi( ix*step, iy*step, small_side, small_side );
				ip.setRoi(square_roi);
				square_block = ip.crop();
				
				// Window the block
				float[] block_pixels = (float[]) square_block.getPixels();
				for (int i = 0; i < block_pixels.length; i++) {
					block_pixels[i] *= window_pixels[i]; 
				}
				
				// Pad the block with a power of 2 size
				padded_square_block.setValue(0.0);
				padded_square_block.fill();
				padded_square_block.insert(square_block, 0, 0);
				
				// Computes its FFT
				fft = new FHT(padded_square_block);
				fft.setShowProgress(false);
				fft.transform();
				fft.swapQuadrants();
				
				// Get a centered power spectrum with right size
				pspectrum = fft.conjugateMultiply(fft);
				pspectrum .setRoi(original_square);
				small_pspectrum = (FloatProcessor) pspectrum .crop();
				spectrum_px = (float[]) small_pspectrum.getPixels(); 
				
				if (DEBUG) {
					displayLog(small_pspectrum);
				}
				
				// Computes angular density
				for (int bin=0; bin<nbins; bin++) {
					fpx = (float[]) filters.getPixels(bin+1);
					for (int i = 0; i < spectrum_px.length; i++) {
						directionality[bin] += spectrum_px[i] * fpx[i]; // will sum out with every block
					}
				}
			}
		}
		
		// Normalize directionality
		float sum = directionality[0];
		for (int i = 1; i < directionality.length; i++) {
			sum += directionality[i];
		}
		for (int i = 0; i < directionality.length; i++) {
			directionality[i] = directionality[i] / sum;
		}
		
		// Plot it
		Plot dir_histogram = new Plot("Directionality histogram", "Theta", "Density", bins, directionality);
		PlotWindow dh_window = dir_histogram.show();
		
	}

	/*
	 * SETUP METHOD
	 */
	
	public int setup(String arg, ImagePlus _imp) {
		
		nbins = 90;
		if (arg.contains("nbins=")) {
			int narg = arg.indexOf("nbins=");
			String nbins_str = arg.substring(narg+"nbins=".length());
			try {
				nbins = Integer.parseInt(nbins_str);
			} catch (NumberFormatException nfe) {
				IJ.error("Directionality: bad argument for number of bins: "+nbins_str);
				return DONE;
			}
		} 
		
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
		
		// Prepare windowing
		window = getBlackmanProcessor(small_side, small_side);
		window_pixels = (float[]) window.getPixels();
		
		// Prepare polar coordinates
		r = makeRMatrix(small_side, small_side);
		theta = makeThetaMatrix(small_side, small_side);
		
		// Prepare filters
		filters = makeFftFilters(nbins);
		
		// Prepare bins
		bins = new float[nbins];
		for (int i = 0; i < nbins; i++) {
			bins[i] = (float) (i * Math.PI / nbins / Math.PI * 180);
		}
		
		if (DEBUG) {
			new ImagePlus("Filters", filters).show();
		}
		
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
	
	
	private final ImageStack makeFftFilters(final int nbins) {
		final ImageStack filters = new ImageStack(small_side, small_side, nbins);
		float[] pixels;
		
		final float[] r_px = (float[]) r.getPixels();
		final float[] theta_px = (float[]) theta.getPixels();
		
		double current_r, current_theta, theta_c, angular_part;
		final double theta_bw = Math.PI/(nbins-1);
		
		for (int i=1; i<= nbins; i++) {
			
			pixels = new float[small_side*small_side];
			theta_c = (i-1) * Math.PI / nbins;
			
			for (int index = 0; index < pixels.length; index++) {

				current_r = r_px[index];
				if ( current_r < FREQ_THRESHOLD || current_r > small_side/2) {
					continue;
				}
				
				current_theta = theta_px[index];
				if ( Math.abs(current_theta-theta_c) < theta_bw) {
					angular_part = Math.cos( (current_theta-theta_c) / theta_bw * Math.PI / 2.0) ;
					angular_part = angular_part * angular_part;
					
				} else if ( 
						Math.abs(current_theta-(theta_c-Math.PI)) < theta_bw
						|| Math.abs(current_theta-2*Math.PI-(theta_c-Math.PI)) < theta_bw
						) {
					angular_part = Math.cos( (current_theta-theta_c) / theta_bw * Math.PI / 2.0) ;
					if (nbins % 2 == 0) {
						angular_part = 1 - angular_part * angular_part;
					} else {
						angular_part = angular_part * angular_part;
					}
				} else 	{
					continue; // leave it to 0
				}
				
				pixels[index] = (float) angular_part; // only angular for now 

			}
			
			filters.setPixels(pixels, i);
		}
		return filters;
	}
	
	/*
	 * STATIC METHODS
	 */
	
	public static final void displayLog(FloatProcessor ip) {
		final FloatProcessor log10 = new FloatProcessor(ip.getWidth(), ip.getHeight());
		final float[] log10_pixels = (float[]) log10.getPixels();
		final float[] pixels = (float[]) ip.getPixels();
		for (int i = 0; i < pixels.length; i++) {
			log10_pixels[i] = (float) Math.log10(1+pixels[i]); 
		}
		new ImagePlus("Log10 of "+ip.toString(), log10).show();
		log10.resetMinAndMax();
	}
	
	public static final double[] getBlackmanPeriodicWindow1D(final int n) {
		final double[] window = new double[n];
		for (int i = 0; i < window.length; i++) {
			window[i] = 0.42 - 0.5 * Math.cos(2*Math.PI*i/n) + 0.08 * Math.cos(4*Math.PI/n);
		}		
		return window;
	}

	public static  final FloatProcessor getBlackmanProcessor(final int nx, final int ny) {
		final FloatProcessor bpw = new FloatProcessor(nx, ny);
		final float[] pixels = (float[]) bpw.getPixels();
		final double[] bpwx = getBlackmanPeriodicWindow1D(nx);
		final double[] bpwy = getBlackmanPeriodicWindow1D(nx);
		int ix,iy;
		for (int i = 0; i < pixels.length; i++) {
			iy = i / nx;
			ix = i % nx;
			pixels[i] = (float) (bpwx[ix] * bpwy[iy]);
		}
		return bpw;
	}
	
	public static final FloatProcessor makeRMatrix(final int nx, final int ny) {
		final FloatProcessor r = new FloatProcessor(nx, ny);
		final float[] pixels = (float[]) r.getPixels();
		final float xc = nx / 2.0f;
		final float yc = ny / 2.0f;
		int ix, iy;
		for (int i = 0; i < pixels.length; i++) {
			iy = i / nx;
			ix = i % nx;
			pixels[i] = (float) Math.sqrt( (ix-xc)*(ix-xc) + (iy-yc)*(iy-yc));
		}		
		return r;
	}
	
	public static final FloatProcessor makeThetaMatrix(final int nx, final int ny) {
		final FloatProcessor theta = new FloatProcessor(nx, ny);
		final float[] pixels = (float[]) theta.getPixels();
		final float xc = nx / 2.0f;
		final float yc = ny / 2.0f;
		int ix, iy;
		for (int i = 0; i < pixels.length; i++) {
			iy = i / nx;
			ix = i % nx;
			pixels[i] = (float) Math.atan2( iy-yc, ix-xc);
		}		
		return theta;
	}
	
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/gel.gif");
		imp.show();
		
		Directionality_ da = new Directionality_();
		da.setup("nbins=45", imp);
		da.run(imp.getProcessor().toFloat(0, null));
	}
	
}
