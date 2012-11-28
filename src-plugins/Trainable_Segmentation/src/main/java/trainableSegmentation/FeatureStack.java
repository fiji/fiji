package trainableSegmentation;
/** 
 * This class is intended for the Trainable_Segmentation plugin. It creates and holds
 * different feature images for the classification. Possible filters include:
 * - Gaussian blur
 * - Gradient magnitude
 * - Hessian
 * - Difference of Gaussian
 * - Orientation filter to detect membranes and then its projection
 * - Mean
 * - Variance
 * - Minimum
 * - Maximum
 * - Anisotropic diffusion
 * - Bilateral filter
 * - Lipschitz filter
 * - Linear Kuwahara filter
 * - Gabor filters
 * - High order derivative filters
 * - Laplacian filter
 * - Eigenvalues of the Structure tensor
 * - Color features: HSB (if the input image is RGB)
 * - Entropy
 * - Neighbors 
 * 
 * filters to come:
 * - histogram patch
 * - BEL type edge detector
 * 
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Verena Kaynig (verena.kaynig@inf.ethz.ch), Ignacio Arganda-Carreras (iarganda@mit.edu)
 *          Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.algorithm.fft.FourierConvolution;

import anisotropic_diffusion.Anisotropic_Diffusion_2D;

import stitching.FloatArray2D;
import trainableSegmentation.filters.Entropy_Filter;
import trainableSegmentation.filters.Kuwahara;
import trainableSegmentation.filters.Lipschitz_;
import trainableSegmentation.utils.Utils;

import vib.BilateralFilter;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.plugin.ZProjector;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.RankFilters;

import imagescience.feature.Differentiator;
import imagescience.feature.Laplacian;
import imagescience.feature.Structure;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;


/**
 * This class stores the stacks of features that will be used during the trainable/weka segmentations.  
 */
public class FeatureStack
{
	/** original input image */
	private ImagePlus originalImage = null;
	/** stack of feature images (created by filtering) */
	private ImageStack wholeStack = null;
	/** image width */
	private int width = 0;
	/** image height */
	private int height = 0;
	/** minmum sigma/radius used in the filters */
	private float minimumSigma = 1;
	/** maximum sigma/radius used in the filters */
	private float maximumSigma = 16;
	
	/** Gaussian filter flag index */
	public static final int GAUSSIAN 				=  0;
	/** Sobel filter flag index */
	public static final int SOBEL 					=  1;
	/** Hessian filter flag index */
	public static final int HESSIAN 				=  2;
	/** Difference of Gaussians filter flag index */
	public static final int DOG						=  3;
	/** Membrane filter flag index */
	public static final int MEMBRANE				=  4;
	/** Variance filter flag index */
	public static final int VARIANCE				=  5;
	/** Mean filter flag index */
	public static final int MEAN					=  6;
	/** Minimum filter flag index */
	public static final int MINIMUM					=  7;
	/** Maximum filter flag index */
	public static final int MAXIMUM					=  8;
	/** Median filter flag index */
	public static final int MEDIAN					=  9;
	/** Anisotropic diffusion filter flag index */
	public static final int ANISOTROPIC_DIFFUSION 	= 10;
	/** Anisotropic diffusion filter flag index */
	public static final int BILATERAL 				= 11;
	/** Lipschitz filter flag index */
	public static final int LIPSCHITZ 				= 12;
	/** Kuwahara filter flag index */
	public static final int KUWAHARA				= 13;
	/** Gabor filter flag index */					
	public static final int GABOR					= 14;
	/** Derivatives filter flag index */
	public static final int DERIVATIVES				= 15;
	/** Laplacian filter flag index */
	public static final int LAPLACIAN				= 16;
	/** structure tensor filter flag index */
	public static final int STRUCTURE				= 17;
	/** entropy filter flag index */
	public static final int ENTROPY					= 18;
	/** neighbors feature flag index */
	public static final int NEIGHBORS				= 19;
	
	/** names of available filters */
	public static final String[] availableFeatures 
		= new String[]{	"Gaussian_blur", "Sobel_filter", "Hessian", "Difference_of_gaussians", 
					   	"Membrane_projections","Variance","Mean", "Minimum", "Maximum", "Median", 
					   	"Anisotropic_diffusion", "Bilateral", "Lipschitz", "Kuwahara", "Gabor" , 
					   	"Derivatives", "Laplacian", "Structure", "Entropy", "Neighbors"};
	/** flags of filters to be used */
	private boolean[] enableFeatures = new boolean[]{
			true, 	/* Gaussian_blur */
			true, 	/* Sobel_filter */
			true, 	/* Hessian */
			true, 	/* Difference_of_gaussians */
			true, 	/* Membrane_projections */
			false, 	/* Variance */
			false, 	/* Mean */
			false, 	/* Minimum */
			false, 	/* Maximum */
			false, 	/* Median */
			false,	/* Anisotropic_diffusion */
			false, 	/* Bilateral */
			false, 	/* Lipschitz */
			false, 	/* Kuwahara */
			false,	/* Gabor */
			false, 	/* Derivatives */
			false, 	/* Laplacian */
			false,	/* Structure */
			false,	/* Entropy */
			false	/* Neighbors */
	};
	
	/** use neighborhood flag */
	private boolean useNeighbors = false;
	/** expected membrane thickness (in pixels) */
	private int membraneSize = 1;	
	/** size of the patch to use to enhance membranes (in pixels, NxN) */
	private int membranePatchSize = 19;
	/** number of rotating angles for membrane, Kuwahara and Gabor features */
	private int nAngles = 10;
	
	private int minDerivativeOrder = 2;
	private int maxDerivativeOrder = 5;
	
	/** flag to specify the use of color features */
	final boolean colorFeatures;
	
	/** executor service to produce concurrent threads */
	ExecutorService exe = Executors.newFixedThreadPool( Prefs.getThreads() );
	
	/**
	 * Construct object to store stack of image features
	 * @param image original image
	 */
	public FeatureStack(ImagePlus image)
	{
		if( image.getType() == ImagePlus.COLOR_RGB)
		{
			originalImage = new ImagePlus("original image", image.getProcessor() );
			colorFeatures = true;
		}
		else
		{
			originalImage = new ImagePlus("original image", image.getProcessor().duplicate().convertToFloat() );
			colorFeatures = false;
		}
		width = image.getWidth();
		height = image.getHeight();
		wholeStack = new ImageStack(width, height);		
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());		
	}
	
	/**
	 * Initialize empty feature stack
	 * @param width image width
	 * @param height image height
	 * @param colorFeatures color image flat
	 */
	public FeatureStack(int width, int height, boolean colorFeatures)
	{
		this.width = width;
		this.height = height;
		wholeStack = new ImageStack(width, height);
		this.colorFeatures = colorFeatures;
	}
	
	/**
	 * Construct object to store stack of image features
	 * @param ip original image
	 */
	public FeatureStack(ImageProcessor ip)
	{
		if( ip instanceof ColorProcessor)
		{
			originalImage = new ImagePlus("original image", ip );
			colorFeatures = true;
		}
		else
		{
			originalImage = new ImagePlus("original image", ip.duplicate().convertToFloat() );
			colorFeatures = false;
		}
		
		width = ip.getWidth();
		height = ip.getHeight();
		wholeStack = new ImageStack(width, height);
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());
	}

	/**
	 * Shut down the executor service
	 */
	public void shutDownNow()
	{
		exe.shutdownNow();
	}
	
	/**
	 * Display feature stack
	 */
	public void show(){
		ImagePlus showStack = new ImagePlus("featureStack", wholeStack);
		showStack.show();
	}
	/**
	 * Get stack size
	 * @return number of slices in the stack
	 */
	public int getSize(){
		return wholeStack.getSize();
	}
	/**
	 * Get slice label
	 * @param index slice index (from 1 to max size)
	 * @return slice label
	 */
	public String getSliceLabel(int index){
		return wholeStack.getSliceLabel(index);
	}
	/**
	 * Get stack height
	 * @return stack height
	 */
	public int getHeight(){
		return wholeStack.getHeight();
	}
	/**
	 * Get stack width
	 * @return stack width
	 */
	public int getWidth(){
		return wholeStack.getWidth();
	}
	
	/**
	 * Check the use of the neighbors as features
	 * 
	 * @return true if the neighbors are being used
	 */
	public boolean useNeighborhood()
	{
		return this.useNeighbors;
	}
	
	/**
	 * Set the use of the neighbors as features
	 * @param useNeighbors flag to decide the use of neighbors
	 */
	public void setUseNeighbors( boolean useNeighbors )
	{
		this.useNeighbors = useNeighbors;
	}
	
	/**
	 * Set the membrane patch size (it must be an odd number)
	 * @param patchSize membrane patch size
	 */
	public void setMembranePatchSize(int patchSize)
	{
		if(patchSize % 2 == 0)
			patchSize ++;
		this.membranePatchSize = patchSize;
	}
	
	/**
	 * Add Gaussian blur slice to current stack
	 * @param sigma Gaussian radius
	 */
	public void addGaussianBlur(float sigma)
	{
		ImageProcessor ip = originalImage.getProcessor().duplicate();
		GaussianBlur gs = new GaussianBlur();
		//gs.blur(ip, sigma);
		gs.blurGaussian(ip, 0.4 * sigma, 0.4 * sigma,  0.0002);
		wholeStack.addSlice(availableFeatures[GAUSSIAN] + "_" + sigma, ip);
	}
	/**
	 * Calculate Gaussian filter concurrently
	 * @param originalImage original input image
	 * @param sigma Gaussian sigma
	 * @return result image
	 */
	public Callable<ImagePlus> getGaussianBlur(
			final ImagePlus originalImage,
			final float sigma)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				ImageProcessor ip = originalImage.getProcessor().duplicate();
				GaussianBlur gs = new GaussianBlur();
				//gs.blur(ip, sigma);
				gs.blurGaussian(ip, 0.4 * sigma, 0.4 * sigma,  0.0002);
				return new ImagePlus (availableFeatures[GAUSSIAN] + "_" + sigma, ip);
			}
		};
	}
	
	/**
	 * Add entropy filter to current stack
	 * @param radius radius to use (in pixels)
	 * @param numBins number of bins to use in the histogram
	 */
	public void addEntropy(int radius, int numBins)
	{
		ImageProcessor ip = originalImage.getProcessor().duplicate();
		Entropy_Filter filter = new Entropy_Filter();
		wholeStack.addSlice(availableFeatures[ENTROPY] + "_" + radius + "_" + numBins, filter.getEntropy(ip, radius, numBins));
	}
	/**
	 * Calculate entropy filter filter concurrently
	 * @param originalImage original input image
	 * @param radius radius to use (in pixels)
	 * @param numBins number of bins to use in the histogram
	 * @return result image
	 */
	public Callable<ImagePlus> getEntropy(
			final ImagePlus originalImage,
			final int radius,
			final int numBins)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				ImageProcessor ip = originalImage.getProcessor().duplicate();
				Entropy_Filter filter = new Entropy_Filter();
				return new ImagePlus (availableFeatures[ENTROPY] + "_" + radius + "_" + numBins, filter.getEntropy(ip, radius, numBins));
			}
		};
	}
	
	/**
	 * Add 8 neighbors of the original image as features
	 */
	public void addNeighbors(
			final int minSigma,
			final int maxSigma)
	{
		// Test: add neighbors of original image
				
		ImagePlus[] channels = extractChannels(originalImage);

		ImagePlus[] results = new ImagePlus[ channels.length ];

		for(int ch=0; ch < channels.length; ch++)
		{
			ImageStack result = new ImageStack( originalImage.getWidth(), originalImage.getHeight() );
			
			for(int sigma = minSigma; sigma <=maxSigma; sigma *= 2)
			{
				double[][] neighborhood = new double[8][originalImage.getWidth() * originalImage.getHeight()];		

				for(int y=0, n=0; y<originalImage.getHeight(); y++)
					for(int x=0; x<originalImage.getWidth(); x++, n++)					
					{
						for(int i = -1 * sigma, k=0;  i < (sigma+1); i += sigma)
							for(int j = -1 * sigma; j < (sigma+1); j += sigma)
							{
								if(i==0 && j==0)
									continue;				
								neighborhood[k][n] = getPixelMirrorConditions(channels[ ch ].getProcessor(), x+i, y+j);
								k++;
							}
					}


				for(int i=0; i<8; i++)
					result.addSlice(availableFeatures[ NEIGHBORS] + "_" + sigma +"_" +  i, new FloatProcessor( originalImage.getWidth(), originalImage.getHeight(), neighborhood[ i ]));						
			}
			results[ ch ] = new ImagePlus("Neighbors", result);
		}
		ImagePlus merged = mergeResultChannels(results);
		
		for(int i=1; i<=merged.getImageStackSize(); i++)
			wholeStack.addSlice(merged.getImageStack().getSliceLabel(i), merged.getImageStack().getPixels(i));
	}
	
	/**
	 * Calculate 8 neighbors  concurrently
	 * @param originalImage original input image
	 * @return result image
	 */
	public Callable<ImagePlus> getNeighbors(
			final ImagePlus originalImage,
			final int minSigma,
			final int maxSigma)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				// Test: add neighbors of original image
				ImagePlus[] channels = extractChannels(originalImage);

				ImagePlus[] results = new ImagePlus[ channels.length ];

				for(int ch=0; ch < channels.length; ch++)
				{
					ImageStack result = new ImageStack( originalImage.getWidth(), originalImage.getHeight() );
					for(int sigma = minSigma; sigma <=maxSigma; sigma *= 2)
					{
						double[][] neighborhood = new double[8][originalImage.getWidth() * originalImage.getHeight()];		

						for(int y=0, n=0; y<originalImage.getHeight(); y++)
							for(int x=0; x<originalImage.getWidth(); x++, n++)
							{
								for(int i=-1 * sigma, k=0;  i < (sigma+1); i+=sigma)
									for(int j = -1 * sigma; j < (sigma+1); j+=sigma)
									{
										if(i==0 && j==0)
											continue;				
										neighborhood[k][n] = getPixelMirrorConditions(channels[ ch ].getProcessor(), x+i, y+j);
										k++;
									}
							}

						
						for(int i=0; i<8; i++)
							result.addSlice(availableFeatures[ NEIGHBORS] + "_" + sigma +"_" +  i, new FloatProcessor( originalImage.getWidth(), originalImage.getHeight(), neighborhood[ i ]));						
					}
					results[ ch ] = new ImagePlus("Neighbors", result);
				}
				return mergeResultChannels(results);
			}
		};
	}
	
	
	/**
	 * Add variance-filtered image to the stack (single thread version)
	 * @param radius variance filter radius
	 */
	public void addVariance(float radius)
	{
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);

		ImagePlus[] results = new ImagePlus[ channels.length ];

		for(int ch=0; ch < channels.length; ch++)
		{
			final ImageProcessor ip = channels[ ch ].getProcessor().duplicate();
			final RankFilters filter = new RankFilters();
			filter.rank(ip, radius, RankFilters.VARIANCE);
			results[ ch ] = new ImagePlus(availableFeatures[VARIANCE]+ "_"  + radius, ip);
		}
		ImagePlus merged = mergeResultChannels(results);
		wholeStack.addSlice(merged.getTitle(), merged.getProcessor());
	}
	/**
	 * Calculate variance filter concurrently
	 * @param originalImage original input image
	 * @param radius for variance filter
	 * @return result image
	 */
	public Callable<ImagePlus> getVariance(
			final ImagePlus originalImage,
			final float radius)
	{
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);

				ImagePlus[] results = new ImagePlus[ channels.length ];

				for(int ch=0; ch < channels.length; ch++)
				{
					final ImageProcessor ip = channels[ ch ].getProcessor().duplicate();
					final RankFilters filter = new RankFilters();
					filter.rank(ip, radius, RankFilters.VARIANCE);
					results[ ch ] = new ImagePlus(availableFeatures[VARIANCE]+ "_"  + radius, ip);
				}
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Add mean filter to current stack
	 * @param radius radius to use 
	 */
	public void addMean(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MEAN);
		wholeStack.addSlice(availableFeatures[MEAN]+ "_"  + radius, ip);
	}
	
	/**
	 * Get original image after mean filtering (to be called from an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param radius filter radius
	 * @return filtered image
	 */
	public Callable<ImagePlus> getMean(
			final ImagePlus originalImage,
			final float radius)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				final RankFilters filter = new RankFilters();
				filter.rank(ip, radius, RankFilters.MEAN);
				return new ImagePlus (availableFeatures[MEAN]+ "_"  + radius, ip);
			}
		};
	}
	
	public void addMin(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MIN);
		wholeStack.addSlice(availableFeatures[MINIMUM]+ "_"  + radius, ip);
	}
	
	/**
	 * Get original image after minimum filtering (to be called from an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param radius filter radius
	 * @return filtered image
	 */
	public Callable<ImagePlus> getMin(
			final ImagePlus originalImage,
			final float radius)
	{		
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				final RankFilters filter = new RankFilters();
				filter.rank(ip, radius, RankFilters.MIN);
				return new ImagePlus (availableFeatures[MINIMUM]+ "_"  + radius, ip);
			}
		};
	}
	
	/**
	 * Get original image after Gaussian blur and minimum filtering (to be called from an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param blurRadius filter radius for Gaussian blur
	 * @param minRadius radius of minimum filter
	 * @return filtered image
	 */
/*	public Callable<ImagePlus> getBlurMin(
			final ImagePlus originalImage,
			final float blurRadius,
			final float minRadius)
	{
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				GaussianBlur gs = new GaussianBlur();
				gs.blur(ip, blurRadius);
				final RankFilters filter = new RankFilters();
				filter.rank(ip, minRadius, RankFilters.MIN);
				return new ImagePlus (availableFeatures[BLUR_MINIMUM]+ "_"  + blurRadius + "_"  + minRadius, ip);
			}
		};
	}
	*/
	/**
	 * Get original image after Gaussian blur and maximum filtering (to be called from an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param blurRadius filter radius for Gaussian blur
	 * @param minRadius radius of maximum filter
	 * @return filtered image
	 */
/*	public Callable<ImagePlus> getBlurMax(
			final ImagePlus originalImage,
			final float blurRadius,
			final float maxRadius)
	{
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				GaussianBlur gs = new GaussianBlur();
				gs.blur(ip, blurRadius);
				final RankFilters filter = new RankFilters();
				filter.rank(ip, maxRadius, RankFilters.MAX);
				return new ImagePlus (availableFeatures[BLUR_MAXIMUM]+ "_"  + blurRadius + "_"  + maxRadius, ip);
			}
		};
	}
	*/
	public void addMax(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MAX);
		wholeStack.addSlice(availableFeatures[MAXIMUM]+ "_"  + radius, ip);
	}
	
	/**
	 * Get original image after maximum filtering (to be called from an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param radius filter radius
	 * @return filtered image
	 */
	public Callable<ImagePlus> getMax(
			final ImagePlus originalImage,
			final float radius)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				final RankFilters filter = new RankFilters();
				filter.rank(ip, radius, RankFilters.MAX);
				return new ImagePlus (availableFeatures[MAXIMUM]+ "_"  + radius, ip);
			}
		};
	}
	
	public void addMedian(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MEDIAN);
		wholeStack.addSlice(availableFeatures[MEDIAN]+ "_"  + radius, ip);
	}
	
	/**
	 * Get original image after median filtering (to be called from an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param radius filter radius
	 * @return filtered image
	 */
	public Callable<ImagePlus> getMedian(
			final ImagePlus originalImage,
			final float radius)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				final RankFilters filter = new RankFilters();
				filter.rank(ip, radius, RankFilters.MEDIAN);
				return new ImagePlus (availableFeatures[MEDIAN]+ "_"  + radius, ip);
			}
		};
	}
	
	/**
	 * Write feature names in a file
	 * 
	 * @param filename output file name
	 */
	public void writeConfigurationToFile(String filename)
	{
		try{
			BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream(filename) ) );
			try{	
				for (int i=1; i <= wholeStack.getSize(); i++)
				{
					out.write(wholeStack.getSliceLabel(i));
					out.newLine();
				}
				out.close();
			}
			catch(IOException e){System.out.println("IOException");}
		}
		catch(FileNotFoundException e){System.out.println("File not found!");}
	}
	
	/**
	 * Add Sobel filter version of the original image
	 * 
	 * @param sigma radius of the Gaussian blue applied previous to the Sobel filtering
	 */
	public void addGradient(float sigma)
	{
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{

			GaussianBlur gs = new GaussianBlur();
			ImageProcessor ip_x = channels[ch].getProcessor().duplicate().convertToFloat();
			//gs.blur(ip_x, sigma);
			gs.blurGaussian(ip_x, 0.4 * sigma, 0.4 * sigma,  0.0002);
			Convolver c = new Convolver();
			float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
			c.convolveFloat(ip_x, sobelFilter_x, 3, 3);

			ImageProcessor ip_y = channels[ch].getProcessor().duplicate().convertToFloat();
			//gs.blur(ip_y, sigma);
			gs.blurGaussian(ip_y, 0.4 * sigma, 0.4 * sigma,  0.0002);
			c = new Convolver();
			float[] sobelFilter_y = {1f,0f,-1f,2f,0f,-2f,1f,0f,-1f};
			c.convolveFloat(ip_y, sobelFilter_y, 3, 3);

			ImageProcessor ip = new FloatProcessor(width, height);

			for (int x=0; x<width; x++){
				for (int y=0; y<height; y++){
					float s_x = ip_x.getf(x,y);
					float s_y = ip_y.getf(x,y);
					ip.setf(x,y, (float) Math.sqrt(s_x*s_x + s_y*s_y));
				}
			}
			results[ ch ] = new ImagePlus(availableFeatures[SOBEL]+ "_"  +sigma, ip);
		}
		
		ImagePlus merged = mergeResultChannels(results);		
		wholeStack.addSlice(merged.getTitle(), merged.getImageStack().getProcessor(1));
	}
	
	/**
	 * Get Sobel filter version of the original image (to be called from an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param sigma radius of the Gaussian blur applied previous to the Sobel filtering
	 * @return filtered image
	 */
	public Callable<ImagePlus> getGradient(
			final ImagePlus originalImage,
			final float sigma)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ImagePlus[] results = new ImagePlus[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{

					GaussianBlur gs = new GaussianBlur();
					ImageProcessor ip_x = channels[ch].getProcessor().duplicate().convertToFloat();
					//gs.blur(ip_x, sigma);
					gs.blurGaussian(ip_x, 0.4 * sigma, 0.4 * sigma,  0.0002);
					Convolver c = new Convolver();
					float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
					c.convolveFloat(ip_x, sobelFilter_x, 3, 3);

					ImageProcessor ip_y = channels[ch].getProcessor().duplicate().convertToFloat();
					//gs.blur(ip_y, sigma);
					gs.blurGaussian(ip_y, 0.4 * sigma, 0.4 * sigma,  0.0002);
					c = new Convolver();
					float[] sobelFilter_y = {1f,0f,-1f,2f,0f,-2f,1f,0f,-1f};
					c.convolveFloat(ip_y, sobelFilter_y, 3, 3);

					ImageProcessor ip = new FloatProcessor(width, height);

					for (int x=0; x<width; x++){
						for (int y=0; y<height; y++){
							float s_x = ip_x.getf(x,y);
							float s_y = ip_y.getf(x,y);
							ip.setf(x,y, (float) Math.sqrt(s_x*s_x + s_y*s_y));
						}
					}
					results[ ch ] = new ImagePlus(availableFeatures[SOBEL]+ "_"  +sigma, ip);
				}
				
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Add Hessian features from original image (single thread version).
	 * The features include a scalar representing the Hessian, the trace, determinant, 
	 * 1st eigenvalue, 2nd eigenvalue, orientation, gamma-normalized square eigenvalue difference
	 * and the square of Gamma-normalized eigenvalue difference
	 * 
	 * @param sigma radius of the Gaussian filter to use
	 */
	public void addHessian(float sigma)
	{
		float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
		float[] sobelFilter_y = {1f,0f,-1f,2f,0f,-2f,1f,0f,-1f};
		Convolver c = new Convolver();				
		GaussianBlur gs = new GaussianBlur();
	
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{		
			ImageProcessor ip_x = channels[ch].getProcessor().duplicate().convertToFloat();
			gs.blurGaussian(ip_x, 0.4 * sigma, 0.4 * sigma,  0.0002);		
			c.convolveFloat(ip_x, sobelFilter_x, 3, 3);		

			ImageProcessor ip_y = channels[ch].getProcessor().duplicate().convertToFloat();
			gs.blurGaussian(ip_y, 0.4 * sigma, 0.4 * sigma,  0.0002);
			c = new Convolver();
			c.convolveFloat(ip_y, sobelFilter_y, 3, 3);

			ImageProcessor ip_xx = ip_x.duplicate();
			c.convolveFloat(ip_xx, sobelFilter_x, 3, 3);		

			ImageProcessor ip_xy = ip_x.duplicate();
			c.convolveFloat(ip_xy, sobelFilter_y, 3, 3);		

			ImageProcessor ip_yy = ip_y.duplicate();
			c.convolveFloat(ip_yy, sobelFilter_y, 3, 3);		

			ImageProcessor ip = new FloatProcessor(width, height);
			ImageProcessor ipTr = new FloatProcessor(width, height);
			ImageProcessor ipDet = new FloatProcessor(width, height);
			//ImageProcessor ipRatio = new FloatProcessor(width, height);
			ImageProcessor ipEig1 = new FloatProcessor(width, height);
			ImageProcessor ipEig2 = new FloatProcessor(width, height);
			ImageProcessor ipOri = new FloatProcessor(width, height);
			ImageProcessor ipSed = new FloatProcessor(width, height);
			ImageProcessor ipNed = new FloatProcessor(width, height);

			final double t = Math.pow(1, 0.75);

			for (int x=0; x<width; x++){
				for (int y=0; y<height; y++)
				{
					// a
					float s_xx = ip_xx.getf(x,y);
					// b, c
					float s_xy = ip_xy.getf(x,y);
					// d
					float s_yy = ip_yy.getf(x,y);
					// Hessian module: sqrt (a^2 + b*c + d^2)
					ip.setf(x,y, (float) Math.sqrt(s_xx*s_xx + s_xy*s_xy+ s_yy*s_yy));
					// Trace: a + d
					final float trace = (float) s_xx + s_yy;
					ipTr.setf(x,y,  trace);
					// Determinant: a*d - c*b
					final float determinant = (float) s_xx*s_yy-s_xy*s_xy;
					ipDet.setf(x,y, determinant);
					
					// Ratio
					//ipRatio.setf(x,y, (float)(trace*trace) / determinant);																												
					// First eigenvalue: (a + d) / 2 + sqrt( ( 4*b^2 + (a - d)^2) ) / 2 )
					ipEig1.setf(x,y, (float) ( trace/2.0 + Math.sqrt((4*s_xy*s_xy + (s_xx - s_yy)*(s_xx - s_yy)) / 2.0 ) ) );
					// Second eigenvalue: (a + d) / 2 - sqrt( ( 4*b^2 + (a - d)^2) ) / 2 )
					ipEig2.setf(x,y, (float) ( trace/2.0 - Math.sqrt((4*s_xy*s_xy + (s_xx - s_yy)*(s_xx - s_yy)) / 2.0 ) ) );
					// Orientation
					if (s_xy < 0.0) // -0.5 * acos( (a-d) / sqrt( 4*b^2 + (a - d)^2)) )
					{
						float orientation =(float)( -0.5 * Math.acos((s_xx	- s_yy) 
								/ Math.sqrt(4.0 * s_xy * s_xy + (s_xx - s_yy) * (s_xx - s_yy)) ));							
						if (Float.isNaN(orientation))
							orientation = 0;
						ipOri.setf(x, y,  orientation);
					}
					else 	// 0.5 * acos( (a-d) / sqrt( 4*b^2 + (a - d)^2)) )
					{
						float orientation =(float)( 0.5 * Math.acos((s_xx	- s_yy) 
								/ Math.sqrt(4.0 * s_xy * s_xy + (s_xx - s_yy) * (s_xx - s_yy)) ));							
						if (Float.isNaN(orientation))
							orientation = 0;
						ipOri.setf(x, y,  orientation);
					}
					// Gamma-normalized square eigenvalue difference
					ipSed.setf(x, y, (float) ( Math.pow(t,4) * trace*trace * ( (s_xx - s_yy)*(s_xx - s_yy) + 4*s_xy*s_xy ) ) );
					// Square of Gamma-normalized eigenvalue difference
					ipNed.setf(x, y, (float) ( Math.pow(t,2) * ( (s_xx - s_yy)*(s_xx - s_yy) + 4*s_xy*s_xy ) ) );
				}
			}

			ImageStack hessianStack = new ImageStack(width, height);
			hessianStack.addSlice(availableFeatures[HESSIAN] + "_"  + sigma, ip);
			hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Trace_"+sigma, ipTr);
			hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Determinant_"+sigma, ipDet);
			//hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Eignevalue_Ratio_"+sigma, ipRatio);
			hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Eigenvalue_1_"+sigma, ipEig1);
			hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Eigenvalue_2_"+sigma, ipEig2);
			hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Orientation_"+sigma, ipOri);
			hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Square_Eigenvalue_Difference_"+sigma, ipSed);
			hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Normalized_Eigenvalue_Difference_"+sigma, ipNed);
		
			results[ ch ] = new ImagePlus("hessian stack", hessianStack);
		}
		
		ImagePlus merged = mergeResultChannels(results);
		
		for(int i=1; i<=merged.getImageStackSize(); i++)
			wholeStack.addSlice(merged.getImageStack().getSliceLabel(i), merged.getImageStack().getPixels(i));
	}
	
	/**
	 * Get Hessian features from original image (to be submitted in an ExecutorService).
	 * The features include a scalar representing the Hessian, the trace, determinant, 
	 * 1st eigenvalue, 2nd eigenvalue, orientation, gamma-normalized square eigenvalue difference
	 * and the square of Gamma-normalized eigenvalue difference
	 * 
	 * @param originalImage input image
	 * @param sigma radius of the Gaussian filter to use
	 * @return filtered image (stack: hessian, trace, determinant, 1st eigenvalue and 2nd eigenvalue) 
	 */
	public Callable<ImagePlus> getHessian(
			final ImagePlus originalImage,
			final float sigma)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){

				final int width = originalImage.getWidth();
				final int height = originalImage.getHeight();

				float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
				float[] sobelFilter_y = {1f,0f,-1f,2f,0f,-2f,1f,0f,-1f};
				Convolver c = new Convolver();				
				GaussianBlur gs = new GaussianBlur();

				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);

				ImagePlus[] results = new ImagePlus[ channels.length ];

				for(int ch=0; ch < channels.length; ch++)
				{

					ImageProcessor ip_x = channels[ch].getProcessor().duplicate().convertToFloat();
					gs.blurGaussian(ip_x, 0.4 * sigma, 0.4 * sigma,  0.0002);		
					c.convolveFloat(ip_x, sobelFilter_x, 3, 3);		

					ImageProcessor ip_y = channels[ch].getProcessor().duplicate().convertToFloat();
					gs.blurGaussian(ip_y, 0.4 * sigma, 0.4 * sigma,  0.0002);
					c = new Convolver();
					c.convolveFloat(ip_y, sobelFilter_y, 3, 3);

					ImageProcessor ip_xx = ip_x.duplicate();
					c.convolveFloat(ip_xx, sobelFilter_x, 3, 3);		

					ImageProcessor ip_xy = ip_x.duplicate();
					c.convolveFloat(ip_xy, sobelFilter_y, 3, 3);		

					ImageProcessor ip_yy = ip_y.duplicate();
					c.convolveFloat(ip_yy, sobelFilter_y, 3, 3);		

					ImageProcessor ip = new FloatProcessor(width, height);
					ImageProcessor ipTr = new FloatProcessor(width, height);
					ImageProcessor ipDet = new FloatProcessor(width, height);
					//ImageProcessor ipRatio = new FloatProcessor(width, height);
					ImageProcessor ipEig1 = new FloatProcessor(width, height);
					ImageProcessor ipEig2 = new FloatProcessor(width, height);
					ImageProcessor ipOri = new FloatProcessor(width, height);
					ImageProcessor ipSed = new FloatProcessor(width, height);
					ImageProcessor ipNed = new FloatProcessor(width, height);

					final double t = Math.pow(1, 0.75);

					for (int x=0; x<width; x++){
						for (int y=0; y<height; y++)
						{
							// a
							float s_xx = ip_xx.getf(x,y);
							// b, c
							float s_xy = ip_xy.getf(x,y);
							// d
							float s_yy = ip_yy.getf(x,y);
							// Hessian module: sqrt (a^2 + b*c + d^2)
							ip.setf(x,y, (float) Math.sqrt(s_xx*s_xx + s_xy*s_xy+ s_yy*s_yy));
							// Trace: a + d
							final float trace = (float) s_xx + s_yy;
							ipTr.setf(x,y,  trace);
							// Determinant: a*d - c*b
							final float determinant = (float) s_xx*s_yy-s_xy*s_xy;
							ipDet.setf(x,y, determinant);
							
							// Ratio
							//ipRatio.setf(x,y, (float)(trace*trace) / determinant);																												
							// First eigenvalue: (a + d) / 2 + sqrt( ( 4*b^2 + (a - d)^2) ) / 2 )
							ipEig1.setf(x,y, (float) ( trace/2.0 + Math.sqrt((4*s_xy*s_xy + (s_xx - s_yy)*(s_xx - s_yy)) / 2.0 ) ) );
							// Second eigenvalue: (a + d) / 2 - sqrt( ( 4*b^2 + (a - d)^2) ) / 2 )
							ipEig2.setf(x,y, (float) ( trace/2.0 - Math.sqrt((4*s_xy*s_xy + (s_xx - s_yy)*(s_xx - s_yy)) / 2.0 ) ) );
							// Orientation
							if (s_xy < 0.0) // -0.5 * acos( (a-d) / sqrt( 4*b^2 + (a - d)^2)) )
							{
								float orientation =(float)( -0.5 * Math.acos((s_xx	- s_yy) 
										/ Math.sqrt(4.0 * s_xy * s_xy + (s_xx - s_yy) * (s_xx - s_yy)) ));							
								if (Float.isNaN(orientation))
									orientation = 0;
								ipOri.setf(x, y,  orientation);
							}
							else 	// 0.5 * acos( (a-d) / sqrt( 4*b^2 + (a - d)^2)) )
							{
								float orientation =(float)( 0.5 * Math.acos((s_xx	- s_yy) 
										/ Math.sqrt(4.0 * s_xy * s_xy + (s_xx - s_yy) * (s_xx - s_yy)) ));							
								if (Float.isNaN(orientation))
									orientation = 0;
								ipOri.setf(x, y,  orientation);
							}
							// Gamma-normalized square eigenvalue difference
							ipSed.setf(x, y, (float) ( Math.pow(t,4) * trace*trace * ( (s_xx - s_yy)*(s_xx - s_yy) + 4*s_xy*s_xy ) ) );
							// Square of Gamma-normalized eigenvalue difference
							ipNed.setf(x, y, (float) ( Math.pow(t,2) * ( (s_xx - s_yy)*(s_xx - s_yy) + 4*s_xy*s_xy ) ) );
						}
					}

					ImageStack hessianStack = new ImageStack(width, height);
					hessianStack.addSlice(availableFeatures[HESSIAN] + "_"  + sigma, ip);
					hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Trace_"+sigma, ipTr);
					hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Determinant_"+sigma, ipDet);
					//hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Eignevalue_Ratio_"+sigma, ipRatio);
					hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Eigenvalue_1_"+sigma, ipEig1);
					hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Eigenvalue_2_"+sigma, ipEig2);
					hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Orientation_"+sigma, ipOri);
					hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Square_Eigenvalue_Difference_"+sigma, ipSed);
					hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Normalized_Eigenvalue_Difference_"+sigma, ipNed);

					results[ ch ] = new ImagePlus("hessian stack", hessianStack);
				}

				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Add difference of Gaussians to feature stack (single thread version)
	 * @param sigma1 first Gaussian sigma
	 * @param sigma2 second Gaussian sigma
	 */
	public void addDoG(float sigma1, float sigma2)
	{
		GaussianBlur gs = new GaussianBlur();
		
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);

		ImagePlus[] results = new ImagePlus[ channels.length ];

		for(int ch=0; ch < channels.length; ch++)
		{
			ImageProcessor ip_1 = channels[ch].getProcessor().duplicate();
			//gs.blur(ip_1, sigma1);
			gs.blurGaussian(ip_1, 0.4 * sigma1, 0.4 * sigma1,  0.0002);
			ImageProcessor ip_2 = channels[ch].getProcessor().duplicate();			
			//gs.blur(ip_2, sigma2);
			gs.blurGaussian(ip_2, 0.4 * sigma2, 0.4 * sigma2,  0.0002);

			ImageProcessor ip = new FloatProcessor(width, height);

			for (int x=0; x<width; x++){
				for (int y=0; y<height; y++){
					float v1 = ip_1.getf(x,y);
					float v2 = ip_2.getf(x,y);
					ip.setf(x,y, v2-v1);
				}
			}
		
			results[ ch ] = new ImagePlus(availableFeatures[DOG]+ "_"+sigma1+"_"+sigma2, ip);
		}
		
		ImagePlus merged = mergeResultChannels(results);		
		wholeStack.addSlice(merged.getTitle(), merged.getImageStack().getProcessor(1));
	}
	
	/**
	 * Get difference of Gaussians (to be submitted to an ExecutorService)
	 * @param originalImage input image
	 * @param sigma1 first Gaussian sigma
	 * @param sigma2 second Gaussian sigma
	 * @return difference of Gaussians image
	 */
	public Callable<ImagePlus> getDoG(
			final ImagePlus originalImage,
			final float sigma1,
			final float sigma2)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final int width = originalImage.getWidth();
				final int height = originalImage.getHeight();
				
				GaussianBlur gs = new GaussianBlur();
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);

				ImagePlus[] results = new ImagePlus[ channels.length ];

				for(int ch=0; ch < channels.length; ch++)
				{
					ImageProcessor ip_1 = channels[ch].getProcessor().duplicate();
					//gs.blur(ip_1, sigma1);
					gs.blurGaussian(ip_1, 0.4 * sigma1, 0.4 * sigma1,  0.0002);
					ImageProcessor ip_2 = channels[ch].getProcessor().duplicate();
					//gs.blur(ip_2, sigma2);
					gs.blurGaussian(ip_2, 0.4 * sigma2, 0.4 * sigma2,  0.0002);

					ImageProcessor ip = new FloatProcessor(width, height);

					for (int x=0; x<width; x++){
						for (int y=0; y<height; y++){
							float v1 = ip_1.getf(x,y);
							float v2 = ip_2.getf(x,y);
							ip.setf(x,y, v2-v1);
						}
					}
				
					results[ ch ] = new ImagePlus(availableFeatures[DOG]+ "_"+sigma1+"_"+sigma2, ip);
				}
				
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Add membrane features to the stack (single thread version)
	 * @param patchSize size of the filter to be used
	 * @param membraneSize expected membrane thickness
	 */
	public void addMembraneFeatures(int patchSize, int membraneSize)
	{
		//create membrane patch
		ImageProcessor membranePatch = new FloatProcessor(patchSize, patchSize);
		int middle = Math.round(patchSize / 2);
		int startX = middle - (int) Math.floor(membraneSize/2.0);
		int endX = middle + (int) Math.ceil(membraneSize/2.0);
		
		for (int x=startX; x<=endX; x++)
			for (int y=0; y<patchSize; y++)
				membranePatch.setf(x, y, 1f);
											
		ImageProcessor rotatedPatch;

        final double rotationAngle = 180/nAngles;
        
        // Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);

		ImagePlus[] results = new ImagePlus[ channels.length ];

		for(int ch=0; ch < channels.length; ch++)
		{
        
			ImageStack is = new ImageStack(width, height);
			
			// Rotate kernel "nAngles" degrees up to 180
			for (int i=0; i<nAngles; i++)
			{
				rotatedPatch = membranePatch.duplicate();
				rotatedPatch.rotate(i*rotationAngle);

				Convolver c = new Convolver();

				float[] kernel = (float[]) rotatedPatch.getPixels();
				ImageProcessor ip = channels[ ch ].getProcessor().duplicate();
				c.convolveFloat(ip, kernel, patchSize, patchSize);

				is.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip);
				//    wholeStack.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip.convertToByte(true));
			}

			ImagePlus projectStack = new ImagePlus("membraneStack",is);
			//projectStack.show();

			ImageStack membraneStack = new ImageStack(width, height);

			ZProjector zp = new ZProjector(projectStack);
			zp.setStopSlice(is.getSize());
			for (int i=0;i<6; i++){
				zp.setMethod(i);
				zp.doProjection();
				membraneStack.addSlice(availableFeatures[MEMBRANE] + "_" +i+"_"+patchSize+"_"+membraneSize, zp.getProjection().getChannelProcessor());
			}
			results[ ch ] =  new ImagePlus ("membrane stack", membraneStack);
		}
		
		ImagePlus merged = mergeResultChannels( results );
		for(int i=1; i<=merged.getImageStackSize(); i++)
			wholeStack.addSlice(merged.getImageStack().getSliceLabel(i), merged.getImageStack().getPixels(i));
	}
	
	/**
	 * Get membrane features (to be submitted in an ExecutorService)
	 * @param originalImage input image
	 * @param patchSize orientation kernel size
	 * @param membraneSize expected membrane thickness
	 * @return image stack with elongated membrane projections using all methods ("Average Intensity", "Max Intensity", "Min Intensity", "Sum Slices", "Standard Deviation", "Median")
	 */
	public Callable<ImagePlus> getMembraneFeatures(
			final ImagePlus originalImage,
			final int patchSize, 
			final int membraneSize)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				 final int width = originalImage.getWidth();
                 final int height = originalImage.getHeight();

                 //create membrane patch
                 final ImageProcessor membranePatch = new FloatProcessor(patchSize, patchSize);
                 int middle = Math.round(patchSize / 2);
                 int startX = middle - (int) Math.floor(membraneSize/2.0);
                 int endX = middle + (int) Math.ceil(membraneSize/2.0);

                 for (int x=startX; x<=endX; x++){
                         for (int y=0; y<patchSize; y++){
                                 membranePatch.setf(x, y, 1f);
                         }
                 }

                 
                 ImageProcessor rotatedPatch;

                 final double rotationAngle = 180/nAngles;
                 
                 // Get channel(s) to process
 				ImagePlus[] channels = extractChannels(originalImage);

 				ImagePlus[] results = new ImagePlus[ channels.length ];

 				for(int ch=0; ch < channels.length; ch++)
 				{
                 
 					ImageStack is = new ImageStack(width, height);
 					
 					// Rotate kernel "nAngles" degrees up to 180
 					for (int i=0; i<nAngles; i++)
 					{
 						rotatedPatch = membranePatch.duplicate();
 						rotatedPatch.rotate(i*rotationAngle);

 						Convolver c = new Convolver();

 						float[] kernel = (float[]) rotatedPatch.getPixels();
 						ImageProcessor ip = channels[ ch ].getProcessor().duplicate();
 						c.convolveFloat(ip, kernel, patchSize, patchSize);

 						is.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip);
 						//    wholeStack.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip.convertToByte(true));
 					}

 					ImagePlus projectStack = new ImagePlus("membraneStack",is);
 					//projectStack.show();

 					ImageStack membraneStack = new ImageStack(width, height);

 					ZProjector zp = new ZProjector(projectStack);
 					zp.setStopSlice(is.getSize());
 					for (int i=0;i<6; i++){
 						zp.setMethod(i);
 						zp.doProjection();
 						membraneStack.addSlice(availableFeatures[MEMBRANE] + "_" +i+"_"+patchSize+"_"+membraneSize, zp.getProjection().getChannelProcessor());
 					}
 					results[ ch ] =  new ImagePlus ("membrane stack", membraneStack);
 				}
 				
 				return mergeResultChannels( results );
			}
			
		};
	}
	

	/**
	 * Extract channels from input image if it is RGB
	 * @param originalImage input image
	 * @return array of channels
	 */
	ImagePlus[] extractChannels(final ImagePlus originalImage) 
	{
		final int width = originalImage.getWidth();
		final int height = originalImage.getHeight();
		ImagePlus[] channels;
		if( originalImage.getType() == ImagePlus.COLOR_RGB )
		{
			final ByteProcessor redBp = new ByteProcessor(width, height);
			final ByteProcessor greenBp = new ByteProcessor(width, height);
			final ByteProcessor blueBp = new ByteProcessor(width, height);

			final byte[] redPixels = (byte[]) redBp.getPixels();
			final byte[] greenPixels = (byte[]) greenBp.getPixels();
			final byte[] bluePixels = (byte[]) blueBp.getPixels();

			((ColorProcessor)(originalImage.getProcessor().duplicate())).getRGB(redPixels, greenPixels, bluePixels);

			channels = new ImagePlus[]{new ImagePlus("red", redBp.convertToFloat()), 
					new ImagePlus("green", greenBp.convertToFloat()), 
					new ImagePlus("blue", blueBp.convertToFloat() )};
		}
		else
		{
			channels = new ImagePlus[1];
			channels[0] = new ImagePlus(originalImage.getTitle(), originalImage.getProcessor().duplicate().convertToFloat() );
		}
		return channels;
	}
	
	/**
	 * Merge input channels if they are more than 1
	 * @param channels results channels
	 * @return result image 
	 */
	ImagePlus mergeResultChannels(final ImagePlus[] channels) 
	{
		if(channels.length > 1)
		{						
			ImageStack mergedColorStack = mergeStacks(channels[0].getImageStack(), channels[1].getImageStack(), channels[2].getImageStack());
			
			ImagePlus merged = new ImagePlus(channels[0].getTitle(), mergedColorStack); 
			
			for(int n = 1; n <= merged.getImageStackSize(); n++)
				merged.getImageStack().setSliceLabel(channels[0].getImageStack().getSliceLabel(n), n);
			
			return merged;
		}
		else
			return channels[0];
	}
	
	/**
	 * Merge three image stack into a color stack (no scaling)
	 * 
	 * @param redChannel image stack representing the red channel 
	 * @param greenChannel image stack representing the green channel
	 * @param blueChannel image stack representing the blue channel
	 * @return RGB merged stack
	 */
	ImageStack mergeStacks(ImageStack redChannel, ImageStack greenChannel, ImageStack blueChannel)
	{
		final ImageStack colorStack = new ImageStack( redChannel.getWidth(), redChannel.getHeight());
		
		for(int n=1; n<=redChannel.getSize(); n++)
		{
			final ByteProcessor red = (ByteProcessor) redChannel.getProcessor(n).convertToByte(false); 
			final ByteProcessor green = (ByteProcessor) greenChannel.getProcessor(n).convertToByte(false); 
			final ByteProcessor blue = (ByteProcessor) blueChannel.getProcessor(n).convertToByte(false); 
			
			final ColorProcessor cp = new ColorProcessor(redChannel.getWidth(), redChannel.getHeight());
			cp.setRGB((byte[]) red.getPixels(), (byte[]) green.getPixels(), (byte[]) blue.getPixels() );
			
			colorStack.addSlice(redChannel.getSliceLabel(n), cp);
		}
		
		return colorStack;
	}
	
	/**
	 * Apply a filter to the original image (to be submitted to an ExecutorService)
	 * @param originalImage original image
	 * @param filter filter kernel
	 * @param title filter name
	 * @return filtered image
	 */
	public Callable<ImagePlus> getFilter(
			final ImagePlus originalImage,
			final ImageProcessor filter,
			final String title)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){

				final int patchSize = filter.getWidth();


				Convolver c = new Convolver();				

				float[] kernel = (float[]) filter.getPixels();
				ImageProcessor ip = originalImage.getProcessor().duplicate();		
				c.convolveFloat(ip, kernel, patchSize, patchSize);		

					
				return new ImagePlus (title, ip);
			}
		};
	}
	
	
	/**
	 * Get derivatives features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma smoothing scale
	 * @param xOrder x-order of differentiation
	 * @param yOrder y-order of differentiation
	 * @return filter image after specific order derivatives
	 */
	public Callable<ImagePlus> getDerivatives(
			final ImagePlus originalImage,
			final double sigma,
			final int xOrder,
			final int yOrder)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>()
		{
			public ImagePlus call()
			{
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ImagePlus[] results = new ImagePlus[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] );
					Aspects aspects = img.aspects();


					imagescience.image.Image newimg = new FloatImage(img);
					Differentiator diff = new Differentiator();

					diff.run(newimg, sigma , xOrder, yOrder, 0);
					newimg.aspects(aspects);

					results[ch] =  newimg.imageplus();				
				}
						
				ImagePlus newimp = mergeResultChannels(results);
				return new ImagePlus (availableFeatures[DERIVATIVES] +"_" + xOrder + "_" +yOrder+"_"+sigma, newimp.getProcessor());
			}
		};
	}	
	
	
	/**
	 * Add derivatives features to current stack
	 *
	 * @param sigma smoothing scale
	 * @param xOrder x-order of differentiation
	 * @param yOrder y-order of differentiation
	 * @return filter image after specific order derivatives
	 */
	public void addDerivatives(
			final double sigma,
			final int xOrder,
			final int yOrder)
	{
		if (Thread.currentThread().isInterrupted()) 
			return;
			
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{
			final imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] ) ;

			final Aspects aspects = img.aspects();				

			final imagescience.image.FloatImage newimg = new FloatImage( img );

			final Differentiator diff = new Differentiator();

			diff.run(newimg, sigma , xOrder, yOrder, 0);

			newimg.aspects( aspects );

			results[ch] =  newimg.imageplus();
		
		}
		ImagePlus newimp = mergeResultChannels(results);
		wholeStack.addSlice(availableFeatures[DERIVATIVES] +"_" + xOrder + "_" +yOrder+"_"+sigma, newimp.getProcessor());		
	}	
	
	
	/**
	 * Get Laplacian features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma smoothing scale	
	 * @return filter Laplacian filter image
	 */
	public Callable<ImagePlus> getLaplacian(
			final ImagePlus originalImage,
			final double sigma)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>()
		{
			public ImagePlus call()
			{
				
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ImagePlus[] results = new ImagePlus[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					final imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] ) ;
				
					final Aspects aspects = img.aspects();				

					imagescience.image.Image newimg = new FloatImage( img );

					final Laplacian laplace = new Laplacian();

					newimg = laplace.run(newimg, sigma);
					newimg.aspects(aspects);						

					results[ch] =  newimg.imageplus();
					
				}
				
				ImagePlus newimp = mergeResultChannels(results);
							
				return new ImagePlus (availableFeatures[LAPLACIAN] +"_" + sigma, newimp.getProcessor());
			}
		};
	}
	
	/**
	 * Add Laplacian features to current stack
	 *
	 * @param sigma smoothing scale	
	 * @return filter Laplacian filter image
	 */
	public void addLaplacian(
			final double sigma)
	{
		if (Thread.currentThread().isInterrupted()) 
			return;
			
		
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{
			final imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] ) ;
		
			final Aspects aspects = img.aspects();				

			imagescience.image.Image newimg = new FloatImage( img );

			final Laplacian laplace = new Laplacian();

			newimg = laplace.run(newimg, sigma);
			newimg.aspects(aspects);						

			results[ch] =  newimg.imageplus();
			
		}
		
		ImagePlus newimp = mergeResultChannels(results);
				
		wholeStack.addSlice(availableFeatures[LAPLACIAN] +"_" + sigma, newimp.getProcessor());
		
	}
	
	/**
	 * Get structure tensor features (to be submitted in an ExecutorService).
	 * It computes, for all pixels in the input image, the eigenvalues of the so-called structure tensor.
	 *
	 * @param originalImage input image
	 * @param sigma smoothing scale	
	 * @param integrationScale integration scale (standard deviation of the Gaussian 
	 * 		kernel used for smoothing the elements of the structure tensor, must be larger than zero)
	 * @return filter structure tensor filter image
	 */
	public Callable<ImagePlus> getStructure(
			final ImagePlus originalImage,
			final double sigma,
			final double integrationScale)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>()
		{
			public ImagePlus call()
			{
				
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ImagePlus[] results = new ImagePlus[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					final imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] ) ;
				
					final Aspects aspects = img.aspects();				

					final Structure structure = new Structure();
					final Vector<imagescience.image.Image> eigenimages = structure.run(new FloatImage(img), sigma, integrationScale);

					final int nrimgs = eigenimages.size();
					for (int i=0; i<nrimgs; ++i)
						eigenimages.get(i).aspects(aspects);

					final ImageStack is = new ImageStack(width, height);

					is.addSlice(availableFeatures[STRUCTURE] +"_largest_" + sigma + "_" + integrationScale, eigenimages.get(0).imageplus().getProcessor() );
					is.addSlice(availableFeatures[STRUCTURE] +"_smallest_" + sigma + "_" + integrationScale, eigenimages.get(1).imageplus().getProcessor() );

					results[ ch ] = new ImagePlus ("Structure stack", is);
				}
				
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Add structure tensor features to current stack
	 * It computes, for all pixels in the input image, the eigenvalues of the so-called structure tensor.
	 * 
	 * @param sigma smoothing scale	
	 * @param integrationScale integration scale (standard deviation of the Gaussian 
	 * 			kernel used for smoothing the elements of the structure tensor, must be larger than zero)
	 * @return filter structure tensor filter image
	 */
	public void addStructure(
			final double sigma,
			final double integrationScale)
	{
		if (Thread.currentThread().isInterrupted()) 
			return;
			
		
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{
			final imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] ) ;
		
			final Aspects aspects = img.aspects();				

			final Structure structure = new Structure();
			final Vector<imagescience.image.Image> eigenimages = structure.run(new FloatImage(img), sigma, integrationScale);

			final int nrimgs = eigenimages.size();
			for (int i=0; i<nrimgs; ++i)
				eigenimages.get(i).aspects(aspects);

			final ImageStack is = new ImageStack(width, height);

			is.addSlice(availableFeatures[STRUCTURE] +"_largest_" + sigma + "_" + integrationScale, eigenimages.get(0).imageplus().getProcessor() );
			is.addSlice(availableFeatures[STRUCTURE] +"_smallest_" + sigma + "_" + integrationScale, eigenimages.get(1).imageplus().getProcessor() );

			results[ ch ] = new ImagePlus ("Structure stack", is);
		}									
		
		ImagePlus merged = mergeResultChannels(results);
		
		wholeStack.addSlice(merged.getImageStack().getSliceLabel( 1 ), merged.getImageStack().getProcessor( 1 ) );
		wholeStack.addSlice(merged.getImageStack().getSliceLabel( 2 ), merged.getImageStack().getProcessor( 2 ) );				
	}	
	
	/**
	 * Get Gabor features (to be submitted in an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param sigma size of the Gaussian envelope
	 * @param gamma spatial aspect ratio, it specifies the ellipticity of the support of the Gabor function
	 * @param psi phase offset
	 * @param frequency frequency of the sinusoidal component
	 * @param nAngles number of filter orientations
	 * @return image stack with Gabor filter projections using "Max Intensity" and "Min Intensity"
	 */
	public Callable<ImagePlus> getGabor(
			final ImagePlus originalImage,
			final double sigma,
			final double gamma,
			final double psi,
			final double frequency,
			final int nAngles)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>()
		{
			public ImagePlus call()
			{	
				final int width = originalImage.getWidth();
				final int height = originalImage.getHeight();
				
				// Apply aspect ratio to the Gaussian curves
				final double sigma_x = sigma;
				final double sigma_y = sigma / gamma;
				
				// Decide size of the filters based on the sigma
				int largerSigma = (sigma_x > sigma_y) ? (int) sigma_x : (int) sigma_y;
				if(largerSigma < 1)
					largerSigma = 1;
				
				// Create set of filters			
				final int filterSizeX = 6 * largerSigma + 1;
				final int filterSizeY = 6 * largerSigma + 1;
				
				final int middleX = (int) Math.round(filterSizeX / 2);
				final int middleY = (int) Math.round(filterSizeY / 2);
							
				final ImageStack kernels = new ImageStack(filterSizeX, filterSizeY);

				final double rotationAngle = Math.PI/nAngles;
				final double sigma_x2 = sigma_x * sigma_x;
				final double sigma_y2 = sigma_y * sigma_y;

				
				// Rotate kernel from 0 to 180 degrees
				for (int i=0; i<nAngles; i++)
				{	
					final double theta = rotationAngle * i;
					final ImageProcessor filter = new FloatProcessor(filterSizeX, filterSizeY);	
					for (int x=-middleX; x<=middleX; x++)
					{
						for (int y=-middleY; y<=middleY; y++)
						{			
							final double xPrime = x * Math.cos(theta) + y * Math.sin(theta);
						    final double yPrime = y * Math.cos(theta) - x * Math.sin(theta);
						        
							final double a = 1.0 / ( 2* Math.PI * sigma_x * sigma_y ) * Math.exp(-0.5 * (xPrime*xPrime / sigma_x2 + yPrime*yPrime / sigma_y2) );
							final double c = Math.cos( 2 * Math.PI * (frequency * xPrime) / filterSizeX + psi); 
							
							filter.setf(x+middleX, y+middleY, (float)(a*c) );
						}
					}
					kernels.addSlice("kernel angle = " + i, filter);
				}

				// Show kernels
				//ImagePlus ip_kernels = new ImagePlus("kernels", kernels);
				//ip_kernels.show();
				
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ImagePlus[] results = new ImagePlus[ channels.length ];
			
				for(int ch=0; ch < channels.length; ch++)
				{

					final ImageStack is = new ImageStack(width, height);
					// Apply kernels
					//FourierConvolution<FloatType, FloatType> fourierConvolution = null;
					//Image<FloatType> image2 = ImagePlusAdapter.wrap(originalImage);
					for (int i=0; i<nAngles; i++)
					{
						
						Image<FloatType> kernel = ImagePlusAdapter.wrap( new ImagePlus("", kernels.getProcessor(i+1)) );
						Image<FloatType> image2 = ImagePlusAdapter.wrap( channels[ ch ] );

						// compute Fourier convolution
						FourierConvolution<FloatType, FloatType> fourierConvolution = new FourierConvolution<FloatType, FloatType>( image2, kernel );
						//if (fourierConvolution == null || !fourierConvolution.replaceKernel( kernel ) )
						//	fourierConvolution = new FourierConvolution<FloatType, FloatType>( image2, kernel );

						if ( !fourierConvolution.checkInput() || !fourierConvolution.process() )
						{
							IJ.log( "Cannot compute fourier convolution: " + fourierConvolution.getErrorMessage() );
							return null;
						}

						Image<FloatType>  convolved = fourierConvolution.getResult();

						is.addSlice("gabor angle = " + i, ImageJFunctions.copyToImagePlus( convolved ).getProcessor() );

					}
					
					// Normalize filtered stack (it seems necessary to have proper results)					
					final ImagePlus projectStack = new ImagePlus("filtered stack", Utils.normalize( is ));
					
					//final ContrastEnhancer c = new ContrastEnhancer();
					//c.stretchHistogram(projectStack, 0.4);
					//projectStack.updateAndDraw();

					final ImageStack resultStack = new ImageStack(width, height);

					final ZProjector zp = new ZProjector(projectStack);
					zp.setStopSlice(is.getSize());
					for (int i=1;i<=2; i++)
					{
						zp.setMethod(i);
						zp.doProjection();
						resultStack.addSlice(availableFeatures[GABOR] + "_" + i 
								+"_"+sigma+"_" + gamma + "_"+ (int) (psi / (Math.PI/4) ) +"_"+frequency, 
								zp.getProjection().getChannelProcessor());
					}					
					
					results[ ch ] = new ImagePlus ("Gabor stack", resultStack);									
					
				}
				
				return mergeResultChannels(results);
			}
		};
	}	
	
	/**
	 * Add Gabor features to current stack
	 * @param originalImage input image
	 * @param sigma size of the Gaussian envelope
	 * @param gamma spatial aspect ratio, it specifies the ellipticity of the support of the Gabor function
	 * @param psi phase offset
	 * @param frequency frequency of the sinusoidal component
	 * @param nAngles number of filter orientations
	 */
	public void addGabor(
			final ImagePlus originalImage,
			final double sigma,
			final double gamma,
			final double psi,
			final double frequency,
			final int nAngles)
	{
		if (Thread.currentThread().isInterrupted()) 
			return;


		final int width = originalImage.getWidth();
		final int height = originalImage.getHeight();

		// Apply aspect ratio to the Gaussian curves
		final double sigma_x = sigma;
		final double sigma_y = sigma / gamma;

		// Decide size of the filters based on the sigma
		int largerSigma = (sigma_x > sigma_y) ? (int) sigma_x : (int) sigma_y;
		if(largerSigma < 1)
			largerSigma = 1;

		// Create set of filters			
		final int filterSizeX = 6 * largerSigma + 1;
		final int filterSizeY = 6 * largerSigma + 1;

		final int middleX = (int) Math.round(filterSizeX / 2);
		final int middleY = (int) Math.round(filterSizeY / 2);

		final ImageStack kernels = new ImageStack(filterSizeX, filterSizeY);

		final double rotationAngle = Math.PI/nAngles;
		final double sigma_x2 = sigma_x * sigma_x;
		final double sigma_y2 = sigma_y * sigma_y;

		// Rotate kernel from 0 to 180 degrees
		for (int i=0; i<nAngles; i++)
		{	
			final double theta = rotationAngle * i;
			final ImageProcessor filter = new FloatProcessor(filterSizeX, filterSizeY);	
			for (int x=-middleX; x<=middleX; x++)
			{
				for (int y=-middleY; y<=middleY; y++)
				{			
					final double xPrime = x * Math.cos(theta) + y * Math.sin(theta);
					final double yPrime = y * Math.cos(theta) - x * Math.sin(theta);

					final double a = 1.0 / ( 2* Math.PI * sigma_x * sigma_y ) * Math.exp(-0.5 * (xPrime*xPrime / sigma_x2 + yPrime*yPrime / sigma_y2) );
					final double c = Math.cos( 2 * Math.PI * (frequency * xPrime) / filterSizeX + psi); 

					filter.setf(x+middleX, y+middleY, (float)(a*c) );
				}
			}
			kernels.addSlice("kernel angle = " + i, filter);
		}

		// Show kernels
		//ImagePlus ip_kernels = new ImagePlus("kernels", kernels);
		//ip_kernels.show();

		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{

			final ImageStack is = new ImageStack(width, height);
			// Apply kernels
			//FourierConvolution<FloatType, FloatType> fourierConvolution = null;
			//Image<FloatType> image2 = ImagePlusAdapter.wrap(originalImage);
			for (int i=0; i<nAngles; i++)
			{
				Image<FloatType> kernel = ImagePlusAdapter.wrap( new ImagePlus("", kernels.getProcessor(i+1)) );
				Image<FloatType> image2 = ImagePlusAdapter.wrap( channels[ ch ] );

				// compute Fourier convolution
				FourierConvolution<FloatType, FloatType> fourierConvolution = new FourierConvolution<FloatType, FloatType>( image2, kernel );
				//if (fourierConvolution == null || !fourierConvolution.replaceKernel( kernel ) )
				//	fourierConvolution = new FourierConvolution<FloatType, FloatType>( image2, kernel );

				if ( !fourierConvolution.checkInput() || !fourierConvolution.process() )
				{
					IJ.log( "Cannot compute fourier convolution: " + fourierConvolution.getErrorMessage() );
					return;
				}

				Image<FloatType>  convolved = fourierConvolution.getResult();

				is.addSlice("gabor angle = " + i, ImageJFunctions.copyToImagePlus( convolved ).getProcessor() );					
			}			

			// Normalize filtered stack (it seems necessary to have proper results)
			final ImagePlus projectStack = new ImagePlus("filtered stack", Utils.normalize( is ));
			
			//final ContrastEnhancer c = new ContrastEnhancer();
			//c.stretchHistogram(projectStack, 0.4);
			//projectStack.updateAndDraw();

			final ImageStack resultStack = new ImageStack(width, height);

			final ZProjector zp = new ZProjector(projectStack);
			zp.setStopSlice(is.getSize());
			for (int i=1;i<=2; i++)
			{
				zp.setMethod(i);
				zp.doProjection();
				resultStack.addSlice(availableFeatures[GABOR] + "_" + i 
						+"_"+sigma+"_" + gamma + "_"+ (int) (psi / (Math.PI/4) ) +"_"+frequency, 
						zp.getProjection().getChannelProcessor());
			}
			results[ ch ] = new ImagePlus ("Gabor stack", resultStack);
		}
		
		ImagePlus merged = mergeResultChannels(results);
		
		for(int i=1; i<=merged.getImageStackSize(); i++)
			wholeStack.addSlice(merged.getImageStack().getSliceLabel(i), merged.getImageStack().getPixels(i));
	}	
	
	/**
	 * Get Kuwahara filter features (to be submitted in an ExecutorService)
	 * @param originalImage input image
	 * @param kernelSize orientation kernel size
	 * @param nAngles number of angles
	 * @param criterion 
	 * @return image stack with Kuwahara filter results using all the available criteria
	 */
	public Callable<ImagePlus> getKuwaharaFeatures(
			final ImagePlus originalImage,
			final int kernelSize, 
			final int nAngles,
			final int criterion)
	{
		return new Callable<ImagePlus>()
		{
			public ImagePlus call()
			{
				
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ImagePlus[] results = new ImagePlus[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					final ImageProcessor ip = channels[ ch ].getProcessor().duplicate();
					final Kuwahara filter = new Kuwahara();
					filter.applyFilter(ip, kernelSize, nAngles, criterion);
					results[ ch ] = new ImagePlus(availableFeatures[KUWAHARA] + "_" + kernelSize + "_ " + nAngles + "_" + criterion, ip);
				}
				
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Add Kuwahara filter features to the current stack
	 * @param originalImage input image
	 * @param kernelSize orientation kernel size
	 * @param nAngles number of angles
	 * @param criterion 
	 */
	public void addKuwaharaFeatures(
			final ImagePlus originalImage,
			final int kernelSize, 
			final int nAngles,
			final int criterion)
	{
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{
			final ImageProcessor ip = channels[ ch ].getProcessor().duplicate();
			final Kuwahara filter = new Kuwahara();
			filter.applyFilter(ip, kernelSize, nAngles, criterion);
			results[ ch ] = new ImagePlus(availableFeatures[KUWAHARA] + "_" + kernelSize + "_ " + nAngles + "_" + criterion, ip);
		}
		
		ImagePlus merged = mergeResultChannels(results);
		
		wholeStack.addSlice(merged.getTitle(), merged.getProcessor());
	}
	
	/**
	 * Apply anisotropic diffusion in a concurrent way (to be submitted in an ExecutorService)
	 * @param originalImage input image
	 * @param nb_iter number of iterations
	 * @param saveSteps number of steps after which we save the intermediate results
	 * @param nb_smoothings number of smoothings per iteration
	 * @param a1 diffusion limiter along minimal variations
	 * @param a2 diffusion limiter along maximal variations
	 * @param edgeThreshold edge threshold
	 * @return result image
	 */
	public Callable<ImagePlus> getAnisotropicDiffusion(
			final ImagePlus originalImage,
			final int nb_iter, 
			final int saveSteps,
			final int nb_smoothings,
			final float a1,
			final float a2,
			final float edgeThreshold)
	{
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
				
				if (Thread.currentThread().isInterrupted()) 
					return null;
				
				Anisotropic_Diffusion_2D ad = new Anisotropic_Diffusion_2D();
				
				ad.setup("", originalImage);
				
				ad.setSaveSteps(saveSteps);
				ad.setNumOfIterations(nb_iter);
				ad.setLimiterMinimalVariations(a1);
				ad.setLimiterMaximalVariations(a2);
				ad.setSmoothings(nb_smoothings);
				ad.setEdgeThreshold(edgeThreshold);
				
				final ImagePlus result = ad.runTD(originalImage.getProcessor());
				
				if (Thread.currentThread().isInterrupted()) 
					return null;
				
				if(result.getImageStackSize() == 1)
				{
					return new ImagePlus (availableFeatures[ANISOTROPIC_DIFFUSION] + "_" + nb_iter + "_" + nb_smoothings + "_" + a1 + "_" + a2 + "_" + edgeThreshold, result.getProcessor());
				}
				else
				{
					final ImageStack slices = result.getImageStack();
					slices.deleteSlice(1); // delete original image
					for(int i = 1; i <= slices.getSize() ; i++)
						slices.setSliceLabel(availableFeatures[ANISOTROPIC_DIFFUSION] + "_" + (saveSteps * i) + "_" + nb_smoothings + "_" + a1 + "_" + a2 +"_" + edgeThreshold, i);
					
					return new ImagePlus("Anisotropic diffusion", slices);
				}
				
			}
		};
	}

	/**
	 * Add anisotropic diffusion filtering images to current stack
	 * 
	 * @param originalImage input image
	 * @param nb_iter number of iterations
	 * @param saveSteps number of steps after which we save the intermediate results
	 * @param nb_smoothings number of smoothings per iteration
	 * @param a1 diffusion limiter along minimal variations
	 * @param a2 diffusion limiter along maximal variations
	 * @param edgeThreshold edge threshold 
	 */
	public void addAnisotropicDiffusion(
			final ImagePlus originalImage,
			final int nb_iter, 
			final int saveSteps,
			final int nb_smoothings,
			final float a1,
			final float a2,
			final float edgeThreshold)
	{
		Anisotropic_Diffusion_2D ad = new Anisotropic_Diffusion_2D();

		ad.setup("", originalImage);

		ad.setSaveSteps(saveSteps);
		ad.setNumOfIterations(nb_iter);
		ad.setLimiterMinimalVariations(a1);
		ad.setLimiterMaximalVariations(a2);
		ad.setSmoothings(nb_smoothings);
		ad.setEdgeThreshold(edgeThreshold);

		final ImagePlus result = ad.runTD(originalImage.getProcessor());


		if(result.getImageStackSize() == 1)
		{
			wholeStack.addSlice(availableFeatures[ANISOTROPIC_DIFFUSION] + "_" + nb_iter + "_" + nb_smoothings + "_" + a1 + "_" + a2 + "_" + edgeThreshold, result.getProcessor());
		}
		else
		{
			final ImageStack slices = result.getImageStack();
			slices.deleteSlice(1); // delete original image
			for(int i = 1; i <= slices.getSize() ; i++)
				wholeStack.addSlice(availableFeatures[ANISOTROPIC_DIFFUSION] + "_" + (saveSteps * i) + "_" + nb_smoothings + "_" + a1 + "_" + a2 +"_" + edgeThreshold, slices.getProcessor(i));										
		}					
	}
	
	/**
	 * Apply bilateral filter in a concurrent way (to be submitted in an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param spatialRadius spatial radius
	 * @param rangeRadius range radius	  
	 * @return result image
	 */
	public Callable<ImagePlus> getBilateralFilter(
			final ImagePlus originalImage,					
			final double spatialRadius,
			final double rangeRadius)
	{
		return new Callable<ImagePlus>(){
			public ImagePlus call()
			{							
				//IJ.log("calling bilateral filter with spatiaRadius =" + spatialRadius + " and rangeRadius = " + rangeRadius);
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ImagePlus[] results = new ImagePlus[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					final ImagePlus result = BilateralFilter.filter(
							new ImagePlus("", channels[ch].getProcessor().convertToByte(true)), spatialRadius, rangeRadius);								

					results[ ch ] = new ImagePlus (availableFeatures[BILATERAL] + "_" + spatialRadius + "_" + rangeRadius, result.getProcessor().convertToFloat());
				}
				
				return mergeResultChannels(results);
			}
		};
	}	
	
	/**
	 * Add bilateral filter image to current stack
	 * 
	 * @param originalImage input image
	 * @param spatialRadius spatial radius
	 * @param rangeRadius range radius	  
	 * @return result image
	 */
	public void addBilateralFilter(
			final ImagePlus originalImage,					
			final double spatialRadius,
			final double rangeRadius)
	{			
		//IJ.log("calling bilateral filter with spatiaRadius =" + spatialRadius + " and rangeRadius = " + rangeRadius);
		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{
			final ImagePlus result = BilateralFilter.filter(
					new ImagePlus("", channels[ch].getProcessor().convertToByte(true)), spatialRadius, rangeRadius);								

			results[ ch ] = new ImagePlus (availableFeatures[BILATERAL] + "_" + spatialRadius + "_" + rangeRadius, result.getProcessor().convertToFloat());
		}					

		ImagePlus merged = mergeResultChannels(results);
		wholeStack.addSlice(merged.getTitle(), merged.getImageStack().getProcessor(1));								
	}		
	
	/**
	 * Apply Lipschitz filter in a concurrent way (to be submitted in an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @return result image
	 */
	public Callable<ImagePlus> getLipschitzFilter(
			final ImagePlus originalImage,					
			final boolean downHat,
			final boolean topHat,
			final double slope)
	{
		return new Callable<ImagePlus>(){
			public ImagePlus call()
			{							
				final Lipschitz_ filter = new Lipschitz_();
				filter.setDownHat(downHat);
				filter.setTopHat(topHat);
				filter.m_Slope = slope;
				
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ImagePlus[] results = new ImagePlus[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					ImageProcessor result = channels[ ch ].getProcessor().duplicate().convertToByte(true);
					filter.Lipschitz2D(result);
				
					results[ ch ] = new ImagePlus (availableFeatures[LIPSCHITZ] + "_" + downHat + "_" + topHat + "_" + slope, result.convertToFloat());
				}
				
				return mergeResultChannels(results);
			}
		};
	}	

	/**
	 * Add Lipschitz filter image to current stack
	 * 
	 * @param originalImage input image
 	 */
	public void addLipschitzFilter(
			final ImagePlus originalImage,					
			final boolean downHat,
			final boolean topHat,
			final double slope)
	{
		final Lipschitz_ filter = new Lipschitz_();
		filter.setDownHat(downHat);
		filter.setTopHat(topHat);
		filter.m_Slope = slope;

		// Get channel(s) to process
		ImagePlus[] channels = extractChannels(originalImage);
		
		ImagePlus[] results = new ImagePlus[ channels.length ];
		
		for(int ch=0; ch < channels.length; ch++)
		{
			ImageProcessor result = channels[ ch ].getProcessor().duplicate().convertToByte(true);
			filter.Lipschitz2D(result);
		
			results[ ch ] = new ImagePlus (availableFeatures[LIPSCHITZ] + "_" + downHat + "_" + topHat + "_" + slope, result.convertToFloat());
		}
		
		ImagePlus merged = mergeResultChannels(results);
		wholeStack.addSlice(merged.getTitle(), merged.getImageStack().getProcessor(1));		
	}
	
	public void addTest()
	{
		FloatArray2D fftImage = new FloatArray2D((float[]) originalImage.getProcessor().getPixels(),
													originalImage.getWidth(), originalImage.getHeight());
		//int fftSize = FftReal.nfftFast(Math.max(width, height));
		
		//FloatArray2D fftImagePadded = CommonFunctions.zeroPad(fftImage, fftSize, fftSize);
		
		//fftImage = CommonFunctions.computeFFT(fftImage);
		//float[] xcorr = CommonFunctions.multiply(fftImage.data, fftImage.data, false);
		
		//FloatArray2D xcorrImage = new FloatArray2D(xcorr, fftImage.width, fftImage.height);
		//xcorrImage = CommonFunctions.com
		
		//float[] pcm = CommonFunctions.computePhaseCorrelationMatrix(fftImagePadded.data, fftImagePadded.data, false);

		FloatProcessor blah = new FloatProcessor(width, height);
		blah.setPixels(fftImage.data);
		
		ImagePlus foo = new ImagePlus("test", blah);
		foo.show();
		IJ.log("min Value " + blah.getMin() + " max: " + blah.getMax());
		foo.setProcessor("test", blah.convertToByte(true));
		foo.show();
		IJ.log("min Value " + blah.convertToByte(true).getMin() + " max: " + blah.convertToByte(true).getMax());
		
	}

	/**
	 * Get slice image processor
	 * @param index selected slice
	 * @return slice image processor
	 */
	public ImageProcessor getProcessor(int index) {
		return wholeStack.getProcessor(index);
	}
	
	/**
	 * Create the instances for the whole stack
	 * 
	 * @param classes list of classes names
	 * 
	 * @return whole stack set of instances
	 */
	public Instances createInstances(ArrayList<String> classes)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=wholeStack.getSize(); i++){
			String attString = wholeStack.getSliceLabel(i);
			attributes.add(new Attribute(attString));
		}
		
		if(useNeighborhood())
			for (int i=0; i<8; i++)
			{	
				IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
				attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
			}
		
		attributes.add(new Attribute("class", classes));
		
		Instances data =  new Instances("segment", attributes, width*height);
				
		for (int y=0; y<wholeStack.getHeight(); y++)
		{
			if (Thread.currentThread().isInterrupted()) 
				return null;
			IJ.showProgress(y, wholeStack.getHeight());
			for (int x=0; x<wholeStack.getWidth(); x++)
			{
				data.add(createInstance(x, y, 0));
			}
		}
		// Set the index of the class attribute
		data.setClassIndex( attributes.size() - 1 );
		IJ.showProgress(1.0);
		return data;
	}
	
	/**
	 * Add the default features to the feature stack
	 */
	public void addDefaultFeatures()
	{
		int counter = 1;
		for (float i=1.0f; i<maximumSigma; i*=2){
			IJ.showStatus("Creating feature stack...   " + counter);
			this.addGaussianBlur(i); counter++;
			IJ.showStatus("Creating feature stack...   " + counter);			
			this.addGradient(i); counter++;
			IJ.showStatus("Creating feature stack...   " + counter);			
			this.addHessian(i); counter++;
			for (float j=1.0f; j<i; j*=2){
				IJ.showStatus("Creating feature stack...   " + counter);				
				this.addDoG(i, j); counter++;
			}
		}
		this.addMembraneFeatures(19, 1);
		
		IJ.showProgress(1.0);
	}
	
	/**
	 * Update features with current list
	 * @deprecated
	 */
	public void updateFeatures()
	{
		wholeStack = new ImageStack(width, height);
		if( originalImage.getType() == ImagePlus.COLOR_RGB)
		{		
			wholeStack.addSlice("original", originalImage.getProcessor().duplicate());
			addHSB();
		}
		else
			wholeStack.addSlice("original", originalImage.getProcessor().duplicate().convertToFloat());
		
		// Anisotropic Diffusion
		if(enableFeatures[ANISOTROPIC_DIFFUSION])
		{
			//for(int i = 1; i < 8; i += 3)
			for (float i=minimumSigma; i<= maximumSigma; i *=2)
				for(float j = 0.10f; j < 0.5f; j+= 0.25f)
				{
					if (Thread.currentThread().isInterrupted()) 
						return;

					addAnisotropicDiffusion(originalImage, 20, 20,(int) i, j, 0.9f, (float) membraneSize)  ;
				}
		}

		// Bilateral filter
		if(enableFeatures[BILATERAL])			
		{
			for(double i = 5; i < 20; i *= 2)
				for(double j = 50; j <= 100; j*= 2)
				{
					if (Thread.currentThread().isInterrupted()) 
						return;
					//IJ.log( n++ +": Calculating bilateral filter (" + i + ", " + j + ")");
					addBilateralFilter(originalImage, i, j);
				}
		}

		// Lipschitz filter
		if(enableFeatures[LIPSCHITZ])			
		{
			for(double i = 5; i < 30; i += 5)					
			{
				if (Thread.currentThread().isInterrupted()) 
					return;
				//IJ.log( n++ +": Calculating Lipschitz filter (true, true, " + i + ")");
				addLipschitzFilter(originalImage, true, true, i);
			}
		}

		// Kuwahara filter
		if(enableFeatures[KUWAHARA])			
		{			
			for(int i = 0; i < 3; i++)
			{
				if (Thread.currentThread().isInterrupted()) 
					return;
				//IJ.log( n++ +": Calculating Kuwahara filter (" + membranePatchSize + ", " + nAngles + ", " + i + ")");
				addKuwaharaFeatures(originalImage, membranePatchSize, nAngles, i);
			}
		}

		// Gabor filters
		if ( enableFeatures[ GABOR ] )
		{				
			// elongated filters in y- axis (sigma = 1.0, gamma = [1.0 - 0.25])
			for(int i=0; i < 2; i++)
				for(double gamma = 1; gamma >= 0.25; gamma /= 2)						
					for(int frequency = 2; frequency<3; frequency ++)
					{
						if (Thread.currentThread().isInterrupted()) 
							return;
						final double psi = Math.PI / 2 * i;
						//IJ.log( n++ +": Calculating Gabor filter (1.0, " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
						addGabor( originalImage, 1.0, gamma, psi, frequency, nAngles ) ;
					}
			// elongated filters in x- axis (sigma = [2.0 - 4.0], gamma = [1.0 - 2.0])
			for(int i=0; i < 2; i++)
				for(double sigma = 2.0; sigma <= 4.0; sigma *= 2)					
					for(double gamma = 1.0; gamma <= 2.0; gamma *= 2)
						for(int frequency = 2; frequency<=3; frequency ++)
						{
							if (Thread.currentThread().isInterrupted()) 
								return;
							final double psi = Math.PI / 2 * i;
							//IJ.log( n++ +": Calculating Gabor filter (" + sigma + " , " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
							addGabor( originalImage, sigma, gamma, psi, frequency, nAngles ) ;
						}								
		}

		// Sobel (no blur)
		if(enableFeatures[SOBEL])
		{
			if ( Thread.currentThread().isInterrupted() ) 
				return;
			//IJ.log(n++ + ": Calculating Sobel filter (0.0)");
			addGradient(0);
		}
		// Hessian (no blur)
		if(enableFeatures[HESSIAN])
		{
			if (Thread.currentThread().isInterrupted()) 
				return;
			//IJ.log( n++ +": Calculating Hessian filter (0.0)");
			addHessian(0);
		}


		for (float i=minimumSigma; i<= maximumSigma; i *=2)
		{		
			if (Thread.currentThread().isInterrupted()) 
				return;
			// Gaussian blur
			if(enableFeatures[GAUSSIAN])
			{
				//IJ.log( n++ +": Calculating Gaussian filter ("+ i + ")");
				addGaussianBlur(i);
			}
			// Sobel
			if(enableFeatures[SOBEL])
			{
				//IJ.log( n++ +": Calculating Sobel filter ("+ i + ")");
				addGradient(i);
			}
			// Hessian
			if(enableFeatures[HESSIAN])
			{
				//IJ.log("Calculating Hessian filter ("+ i + ")");
				addHessian(i);
			}
			// Difference of gaussians
			if(enableFeatures[DOG])
			{
				for (float j=minimumSigma; j<i; j*=2)
				{
					//IJ.log( n++ +": Calculating DoG filter ("+ i + ", " + j + ")");
					addDoG(i, j);
				}
			}
			// Variance
			if(enableFeatures[VARIANCE])
			{
				//IJ.log( n++ +": Calculating Variance filter ("+ i + ")");
				addVariance(i);
			}
			// Mean
			if(enableFeatures[MEAN])
			{
				//IJ.log( n++ +": Calculating Mean filter ("+ i + ")");
				addMean(i);
			}

			// Min
			if(enableFeatures[MINIMUM])
			{
				//IJ.log( n++ +": Calculating Minimum filter ("+ i + ")");
				addMin(i);
			}
			// Max
			if(enableFeatures[MAXIMUM])
			{
				//IJ.log( n++ +": Calculating Maximum filter ("+ i + ")");
				addMax(i);
			}
			
			// Median
			if(enableFeatures[MEDIAN])
			{
				//IJ.log( n++ +": Calculating Median filter ("+ i + ")");
				addMedian(i);
			}
			
			// Derivatives
			if(enableFeatures[DERIVATIVES])
			{					
				for(int order = minDerivativeOrder; order<=maxDerivativeOrder; order++)
					addDerivatives( i, order, order );
			}
			
			// Laplacian
			if(enableFeatures[LAPLACIAN])
			{
				addLaplacian(i);
			}
			
			// Structure tensor
			if(enableFeatures[ STRUCTURE ])
			{					
				for(int integrationScale = 1; integrationScale <= 3; integrationScale+=2)
					addStructure(i, integrationScale );
			}

		}
		// Membrane projections
		if(enableFeatures[MEMBRANE])
		{
			if (Thread.currentThread().isInterrupted()) 
				return;
			//IJ.log( n++ +": Calculating Membranes projections ("+ membranePatchSize + ", " + membraneSize + ")");
			addMembraneFeatures(membranePatchSize, membraneSize);
		}
		
		// Neighbors
		if( enableFeatures[ NEIGHBORS ])
			addNeighbors( (int)minimumSigma, (int)maximumSigma );
		
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
	}
	
	
	/**
	 * Add features based on a list of filters in a multi-thread fashion
	 */
	public void addFeaturesMT(final ImagePlus filterList)
	{
		exe = Executors.newFixedThreadPool( Prefs.getThreads() );
		wholeStack = new ImageStack(width, height);
		//wholeStack.addSlice("original", originalImage.getProcessor().duplicate());

		
		final ArrayList< Future<ImagePlus> > futures = new ArrayList< Future<ImagePlus> >();
		
		try
		{
			for(int i=1; i<=filterList.getStackSize(); i++)
			{
				if (Thread.currentThread().isInterrupted()) 
					return;
				
				// check if the filter slice is labeled (if not, assign an arbitrary label)
				String filterLabel = filterList.getImageStack().getSliceLabel(i);
				if(null == filterLabel || filterLabel.equals(""))
				{
					filterLabel = new String("filter-"+i);
				}
				futures.add(exe.submit( getFilter(originalImage, filterList.getImageStack().getProcessor(i), filterLabel) ) );
			}
			
			// Wait for the jobs to be done
			for(Future<ImagePlus> f : futures)
			{
				final ImagePlus res = f.get();
				if(res.getImageStackSize() == 1)
				{
					this.wholeStack.addSlice(res.getTitle(), res.getProcessor());
				}
				else
				{
					final ImageStack slices = res.getImageStack();
					for(int i = 1; i <= slices.getSize() ; i++)
						this.wholeStack.addSlice(slices.getSliceLabel(i), slices.getProcessor(i));
				}
			}
			
		}
		catch(Exception ex)
		{
			IJ.log("Error when updating feature stack.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}	
		
	}
	
	/**
	 * Update features with current list in a single-thread fashion
	 * 
	 * @return true if the features are correctly updated 
	 */
	public boolean updateFeaturesST()
	{
		wholeStack = new ImageStack(width, height);
		if( originalImage.getType() == ImagePlus.COLOR_RGB)
		{		
			wholeStack.addSlice("original", originalImage.getProcessor().duplicate());
			addHSB();
		}
		else
			wholeStack.addSlice("original", originalImage.getProcessor().duplicate().convertToFloat());
		
		// Anisotropic Diffusion
		if(enableFeatures[ANISOTROPIC_DIFFUSION])
		{
			//for(int i = 1; i < 8; i += 3)
			for (float i=minimumSigma; i<= maximumSigma; i *=2)
				for(float j = 0.10f; j < 0.5f; j+= 0.25f)
				{
					if (Thread.currentThread().isInterrupted()) 
						return false;

					addAnisotropicDiffusion(originalImage, 20, 20,(int) i, j, 0.9f, (float) membraneSize)  ;
				}
		}				

		// Bilateral filter
		if(enableFeatures[BILATERAL])			
		{
			for(double i = 5; i < 20; i *= 2)
				for(double j = 50; j <= 100; j*= 2)
				{
					if (Thread.currentThread().isInterrupted()) 
						return false;
					//IJ.log( n++ +": Calculating bilateral filter (" + i + ", " + j + ")");
					addBilateralFilter(originalImage, i, j);
				}
		}

		// Lipschitz filter
		if(enableFeatures[LIPSCHITZ])			
		{
			for(double i = 5; i < 30; i += 5)					
			{
				if (Thread.currentThread().isInterrupted()) 
					return false;
				//IJ.log( n++ +": Calculating Lipschitz filter (true, true, " + i + ")");
				addLipschitzFilter(originalImage, true, true, i);
			}
		}

		// Kuwahara filter
		if(enableFeatures[KUWAHARA])			
		{			
			for(int i = 0; i < 3; i++)
			{
				if (Thread.currentThread().isInterrupted()) 
					return false;
				//IJ.log( n++ +": Calculating Kuwahara filter (" + membranePatchSize + ", " + nAngles + ", " + i + ")");
				addKuwaharaFeatures(originalImage, membranePatchSize, nAngles, i);
			}
		}

		// Gabor filters
		if ( enableFeatures[ GABOR ] )
		{				
			// elongated filters in y- axis (sigma = 1.0, gamma = [1.0 - 0.25])
			for(int i=0; i < 2; i++)
				for(double gamma = 1; gamma >= 0.25; gamma /= 2)						
					for(int frequency = 2; frequency<3; frequency ++)
					{
						if (Thread.currentThread().isInterrupted()) 
							return false;
						final double psi = Math.PI / 2 * i;
						//IJ.log( n++ +": Calculating Gabor filter (1.0, " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
						addGabor( originalImage, 1.0, gamma, psi, frequency, nAngles ) ;
					}
			// elongated filters in x- axis (sigma = [2.0 - 4.0], gamma = [1.0 - 2.0])
			for(int i=0; i < 2; i++)
				for(double sigma = 2.0; sigma <= 4.0; sigma *= 2)					
					for(double gamma = 1.0; gamma <= 2.0; gamma *= 2)
						for(int frequency = 2; frequency<=3; frequency ++)
						{
							if (Thread.currentThread().isInterrupted()) 
								return false;
							final double psi = Math.PI / 2 * i;
							//IJ.log( n++ +": Calculating Gabor filter (" + sigma + " , " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
							addGabor( originalImage, sigma, gamma, psi, frequency, nAngles ) ;
						}								
		}

		// Sobel (no blur)
		if(enableFeatures[SOBEL] && minimumSigma < 2)
		{
			if ( Thread.currentThread().isInterrupted() ) 
				return false;
			//IJ.log(n++ + ": Calculating Sobel filter (0.0)");
			addGradient(0);
		}
		// Hessian (no blur)
		if(enableFeatures[HESSIAN] && minimumSigma < 2)
		{
			if (Thread.currentThread().isInterrupted()) 
				return false;
			//IJ.log( n++ +": Calculating Hessian filter (0.0)");
			addHessian(0);
		}


		for (float i=minimumSigma; i<= maximumSigma; i *=2)
		{		
			if (Thread.currentThread().isInterrupted()) 
				return false;
			// Gaussian blur
			if(enableFeatures[GAUSSIAN])
			{
				//IJ.log( n++ +": Calculating Gaussian filter ("+ i + ")");
				addGaussianBlur(i);
			}
			// Sobel
			if(enableFeatures[SOBEL])
			{
				//IJ.log( n++ +": Calculating Sobel filter ("+ i + ")");
				addGradient(i);
			}
			// Hessian
			if(enableFeatures[HESSIAN])
			{
				//IJ.log("Calculating Hessian filter ("+ i + ")");
				addHessian(i);
			}
			// Difference of gaussians
			if(enableFeatures[DOG])
			{
				for (float j=minimumSigma; j<i; j*=2)
				{
					//IJ.log( n++ +": Calculating DoG filter ("+ i + ", " + j + ")");
					addDoG(i, j);
				}
			}
			// Variance
			if(enableFeatures[VARIANCE])
			{
				//IJ.log( n++ +": Calculating Variance filter ("+ i + ")");
				addVariance(i);
			}
			// Mean
			if(enableFeatures[MEAN])
			{
				//IJ.log( n++ +": Calculating Mean filter ("+ i + ")");
				addMean(i);
			}

			// Min
			if(enableFeatures[MINIMUM])
			{
				//IJ.log( n++ +": Calculating Minimum filter ("+ i + ")");
				addMin(i);
			}
			// Max
			if(enableFeatures[MAXIMUM])
			{
				//IJ.log( n++ +": Calculating Maximum filter ("+ i + ")");
				addMax(i);
			}
			
			// Median
			if(enableFeatures[MEDIAN])
			{
				//IJ.log( n++ +": Calculating Median filter ("+ i + ")");
				addMedian(i);
			}
			
			// Derivatives
			if(enableFeatures[DERIVATIVES])
			{					
				for(int order = minDerivativeOrder; order<=maxDerivativeOrder; order++)
					addDerivatives( i, order, order );
			}
			
			// Laplacian
			if(enableFeatures[LAPLACIAN])
			{
				addLaplacian(i);
			}
			
			// Structure tensor
			if(enableFeatures[ STRUCTURE ])
			{					
				for(int integrationScale = 1; integrationScale <= 3; integrationScale+=2)
					addStructure(i, integrationScale );
			}
			
			// Entropy
			if(enableFeatures[ENTROPY])
			{
				for(int nBins = 32; nBins <= 256; nBins *=2)
					addEntropy((int)i, nBins);
			}

		}
		// Membrane projections
		if(enableFeatures[MEMBRANE])
		{
			if (Thread.currentThread().isInterrupted()) 
				return false;
			//IJ.log( n++ +": Calculating Membranes projections ("+ membranePatchSize + ", " + membraneSize + ")");
			addMembraneFeatures(membranePatchSize, membraneSize);
		}
		
		// Neighbors
		if( enableFeatures[ NEIGHBORS ])
			addNeighbors( (int)minimumSigma, (int)maximumSigma );
		
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
		return true;
	}

	/**
	 * Add HSB features
	 */
	public void addHSB() 
	{
		final ImagePlus hsb = originalImage.duplicate();
		ImageConverter ic = new ImageConverter( hsb );
		ic.convertToHSB();
		for(int n=1; n<=hsb.getImageStackSize(); n++)
			wholeStack.addSlice(hsb.getImageStack().getSliceLabel(n), hsb.getImageStack().getProcessor(n).convertToRGB());
	}
	
	/**
	 * Calculate HSB out of the RGB channels (to be submitted to an ExecutorService)
	 * @param originalImage original input image
	 * @return HSB image
	 */
	public Callable<ImagePlus> getHSB(
			final ImagePlus originalImage)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final ImagePlus hsb = originalImage.duplicate();
				ImageConverter ic = new ImageConverter( hsb );
				ic.convertToHSB();
				ImageStack is = new ImageStack(originalImage.getWidth(), originalImage.getHeight()); 
				for(int n=1; n<=hsb.getImageStackSize(); n++)
					is.addSlice(hsb.getImageStack().getSliceLabel(n), hsb.getImageStack().getProcessor(n).convertToRGB());
				return new ImagePlus ("HSB", is);
			}
		};
	}
	
	
	/**
	 * Update features with current list in a multi-thread fashion
	 * 
	 * @return true if the features are correctly updated 
	 */
	public boolean updateFeaturesMT()
	{
		if (Thread.currentThread().isInterrupted() )
			return false;
		
		exe = Executors.newFixedThreadPool( Prefs.getThreads() );
				
		wholeStack = new ImageStack(width, height);
		if( originalImage.getType() == ImagePlus.COLOR_RGB)
			wholeStack.addSlice("original", originalImage.getProcessor().duplicate());
		else
			wholeStack.addSlice("original", originalImage.getProcessor().duplicate().convertToFloat());
			
		// Count the number of enabled features
		int finalIndex = 0;
		for(int i=0; i<enableFeatures.length; i++)
			if(enableFeatures[i])
				finalIndex ++;

		final ArrayList< Future<ImagePlus> > futures = new ArrayList< Future<ImagePlus> >();
		//int n=0;
		
		int currentIndex = 0;
		IJ.showStatus("Updating features...");
		try{
			
			// Anisotropic Diffusion
			if(enableFeatures[ANISOTROPIC_DIFFUSION])
			{
				//for(int i = 1; i < 8; i += 3)
				for (float i=minimumSigma; i<= maximumSigma; i *=2)
					for(float j = 0.10f; j < 0.5f; j+= 0.25f)
					{
						if (Thread.currentThread().isInterrupted()) 
							return false;
						//IJ.log( n++ +": Calculating anisotropic diffusion (20, 20, " + i + ", " + j + ", 0.9f" + ", " + membraneSize + ")");
					//for(float j = 0.10f; j <= 0.5f; j+= 0.2f)
						//for(float k = 0.5f; k < 6f; k+= 1f)
							futures.add(exe.submit( getAnisotropicDiffusion(originalImage, 20, 20,(int) i, j, 0.9f, (float) membraneSize) ) );
							//futures.add(exe.submit( getAnisotropicDiffusion(originalImage, 20, 20, (int) i, j, 0.9f, k) ) );
					}				
			}
			
			// HSB
			if( originalImage.getType() == ImagePlus.COLOR_RGB)
				futures.add(exe.submit( getHSB(originalImage) ) );
			
			// Bilateral filter
			if(enableFeatures[BILATERAL])			
			{
				for(double i = 5; i < 20; i *= 2)
					for(double j = 50; j <= 100; j*= 2)
					{
						if (Thread.currentThread().isInterrupted()) 
							return false;
						//IJ.log( n++ +": Calculating bilateral filter (" + i + ", " + j + ")");
						futures.add(exe.submit( getBilateralFilter(originalImage, i, j) ) );
					}
			}
			
			// Lipschitz filter
			if(enableFeatures[LIPSCHITZ])			
			{
				for(double i = 5; i < 30; i += 5)					
				{
					if (Thread.currentThread().isInterrupted()) 
						return false;
					//IJ.log( n++ +": Calculating Lipschitz filter (true, true, " + i + ")");
					futures.add(exe.submit( getLipschitzFilter(originalImage, true, true, i) ) );
				}
			}
			
			// Kuwahara filter
			if(enableFeatures[KUWAHARA])			
			{			
				for(int i = 0; i < 3; i++)
				{
					if (Thread.currentThread().isInterrupted()) 
						return false;
					//IJ.log( n++ +": Calculating Kuwahara filter (" + membranePatchSize + ", " + nAngles + ", " + i + ")");
					futures.add(exe.submit( getKuwaharaFeatures(originalImage, membranePatchSize, nAngles, i) ) );
				}
			}
			
			// Gabor filters
			if ( enableFeatures[ GABOR ] )
			{				
				// elongated filters in y- axis (sigma = 1.0, gamma = [1.0 - 0.25])
				for(int i=0; i < 2; i++)
					for(double gamma = 1; gamma >= 0.25; gamma /= 2)						
						for(int frequency = 2; frequency<3; frequency ++)
						{
							if (Thread.currentThread().isInterrupted()) 
								return false;
							final double psi = Math.PI / 2 * i;
							//System.out.println( " Calculating Gabor filter (1.0, " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
							futures.add(exe.submit( getGabor(originalImage, 1.0, gamma, psi, frequency, nAngles) ) );
						}
				// elongated filters in x- axis (sigma = [2.0 - 4.0], gamma = [1.0 - 2.0])
				for(int i=0; i < 2; i++)
					for(double sigma = 2.0; sigma <= 4.0; sigma *= 2)					
						for(double gamma = 1.0; gamma <= 2.0; gamma *= 2)
							for(int frequency = 2; frequency<=3; frequency ++)
							{
								if (Thread.currentThread().isInterrupted()) 
									return false;
								final double psi = Math.PI / 2 * i;
								//System.out.println( " Calculating Gabor filter (" + sigma + " , " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
								futures.add(exe.submit( getGabor(originalImage, sigma, gamma, psi, frequency, nAngles) ) );
							}								
			}
			
			// Sobel (no blur)
			if(enableFeatures[SOBEL] && minimumSigma < 2)
			{
				if ( Thread.currentThread().isInterrupted() ) 
					return false;
				//IJ.log(n++ + ": Calculating Sobel filter (0.0)");
				futures.add(exe.submit( getGradient(originalImage, 0)) );
			}
			// Hessian (no blur)
			if(enableFeatures[HESSIAN] && minimumSigma < 2)
			{
				if (Thread.currentThread().isInterrupted()) 
					return false;
				//IJ.log( n++ +": Calculating Hessian filter (0.0)");
				futures.add(exe.submit( getHessian(originalImage, 0)) );
			}
			
			
			for (float i=minimumSigma; i<= maximumSigma; i *=2)
			{		
				if (Thread.currentThread().isInterrupted()) 
					return false;
				// Gaussian blur
				if(enableFeatures[GAUSSIAN])
				{
					//IJ.log( n++ +": Calculating Gaussian filter ("+ i + ")");
					futures.add(exe.submit( getGaussianBlur(originalImage, i)) );
				}
				// Sobel
				if(enableFeatures[SOBEL])
				{
					//IJ.log( n++ +": Calculating Sobel filter ("+ i + ")");
					futures.add(exe.submit( getGradient(originalImage, i)) );
				}
				// Hessian
				if(enableFeatures[HESSIAN])
				{
					//IJ.log("Calculating Hessian filter ("+ i + ")");
					futures.add(exe.submit( getHessian(originalImage, i)) );
				}
				// Difference of gaussians
				if(enableFeatures[DOG])
				{
					for (float j=minimumSigma; j<i; j*=2)
					{
						//IJ.log( n++ +": Calculating DoG filter ("+ i + ", " + j + ")");
						futures.add(exe.submit( getDoG(originalImage, i, j)) );
					}
				}
				// Variance
				if(enableFeatures[VARIANCE])
				{
					//IJ.log( n++ +": Calculating Variance filter ("+ i + ")");
					futures.add(exe.submit( getVariance(originalImage, i)) );
				}
				// Mean
				if(enableFeatures[MEAN])
				{
					//IJ.log( n++ +": Calculating Mean filter ("+ i + ")");
					futures.add(exe.submit( getMean(originalImage, i)) );
				}

				// Min
				if(enableFeatures[MINIMUM])
				{
					//IJ.log( n++ +": Calculating Minimum filter ("+ i + ")");
					futures.add(exe.submit( getMin(originalImage, i)) );
				}
				// Max
				if(enableFeatures[MAXIMUM])
				{
					//IJ.log( n++ +": Calculating Maximum filter ("+ i + ")");
					futures.add(exe.submit( getMax(originalImage, i)) );
				}
			
				// Median
				if(enableFeatures[MEDIAN])
				{
					//IJ.log( n++ +": Calculating Median filter ("+ i + ")");
					futures.add(exe.submit( getMedian(originalImage, i)) );
				}
				
				// Derivatives
				if(enableFeatures[DERIVATIVES])
				{					
					for(int order = minDerivativeOrder; order<=maxDerivativeOrder; order++)
						futures.add(exe.submit( getDerivatives(originalImage, i, order, order)) );
				}
				
				// Laplacian
				if(enableFeatures[LAPLACIAN])
				{
					futures.add(exe.submit( getLaplacian(originalImage, i)) );
				}
				
				// Structure tensor
				if(enableFeatures[ STRUCTURE ])
				{					
					for(int integrationScale = 1; integrationScale <= 3; integrationScale+=2)
						futures.add(exe.submit( getStructure(originalImage, i, integrationScale )) );
				}
				
				// Entropy
				if(enableFeatures[ENTROPY])
				{
					for(int nBins = 32; nBins <= 256; nBins *=2)
						futures.add(exe.submit( getEntropy(originalImage, (int) i, nBins)) );
				}

			}
			// Membrane projections
			if(enableFeatures[MEMBRANE])
			{
				if (Thread.currentThread().isInterrupted()) 
					return false;
				//IJ.log( n++ +": Calculating Membranes projections ("+ membranePatchSize + ", " + membraneSize + ")");
				futures.add(exe.submit( getMembraneFeatures(originalImage, membranePatchSize, membraneSize) ));
			}

			// Neighbors
			if( enableFeatures[ NEIGHBORS ])
				futures.add(exe.submit( getNeighbors( originalImage, (int)minimumSigma, (int)maximumSigma ) ) );
			
			// Wait for the jobs to be done
			for(Future<ImagePlus> f : futures)
			{
				final ImagePlus res = f.get();
				currentIndex ++;
				IJ.showStatus("Updating features...");
				IJ.showProgress(currentIndex, finalIndex);
				if(res.getImageStackSize() == 1)
				{
					this.wholeStack.addSlice(res.getTitle(), res.getProcessor());
				}
				else
				{
					final ImageStack slices = res.getImageStack();
					for(int i = 1; i <= slices.getSize() ; i++)
						this.wholeStack.addSlice(slices.getSliceLabel(i), slices.getProcessor(i));
				}
			}
		
		}
		catch(InterruptedException ie)
		{
			IJ.log("The features udpate was interrupted by the user.");
			return false;
		}
		catch(Exception ex)
		{
			IJ.log("Error when updating feature stack.");
			ex.printStackTrace();
			return false;
		}
		finally{
			exe.shutdownNow();
		}	
		
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
		return true;
	}
	
	/**
	 * Set list of boolean flags for enabled features
	 * @param enableFeatures list of boolean flags to enable features
	 */
	public void setEnabledFeatures(boolean[] enableFeatures) {
		this.enableFeatures = enableFeatures;
	}

	/**
	 * Get the list of enabled features flags
	 * @return list of boolean flags (true means the feature is selected)
	 */
	public boolean[] getEnabledFeatures() {
		return enableFeatures;
	}
	
	/**
	 * Get the current membrane thickness
	 * @return expected membrane thickness
	 */
	public int getMembraneSize()
	{
		return this.membraneSize;
	}
	
	/**
	 * Set the expected membrane thickness
	 * @param membraneSize membrane thickness
	 */
	public void setMembraneSize(int membraneSize)
	{
		this.membraneSize = membraneSize;
	}
	
	/**
	 * Check if the stack has been initialized or not
	 * @return true if the features have not been calculated yet
	 */
	public boolean isEmpty()
	{
		return (null == this.wholeStack || this.wholeStack.getSize() < 2);
	}
	
	/**
	 * Save current whole stack as TIFF
	 * @param filename destination path and file name
	 * @return false if fails
	 */
	public boolean saveStackAsTiff(final String filename)
	{
		final ImagePlus ip = new ImagePlus("feature-stack", this.wholeStack);
		//ip.show();
		//IJ.log("path = " + filename);
		final FileSaver fs = new FileSaver(ip);
		return fs.saveAsTiffStack(filename);
	}

	/**
	 * Remove feature from stack
	 * 
	 * @param featureName complete name of the feature to remove
	 */
	public void removeFeature(String featureName) 
	{
		for(int n=1; n<=wholeStack.getSize(); n++)
			if(featureName.equalsIgnoreCase(wholeStack.getSliceLabel(n)))
			{
				this.wholeStack.deleteSlice(n);
				return;
			}		
	}

	/**
	 * Set the minimum sigma to use in the filters
	 * @param minSigma sigma value
	 */
	public void setMinimumSigma(float minSigma) 
	{
		this.minimumSigma = minSigma;		
	}
	
	/**
	 * Set the maximum sigma to use in the filters
	 * @param maxSigma sigma value
	 */
	public void setMaximumSigma(float maxSigma) 
	{
		this.maximumSigma = maxSigma;		
	}
	
	/**
	 * Create instance (feature vector) of a specific coordinate
	 * 
	 * @param x x- axis coordinate
	 * @param y y- axis coordinate
	 * @param classValue class value to be assigned
	 * @return corresponding instance
	 */
	public DenseInstance createInstance(int x, int y, int classValue)
	{
		final int extra = useNeighbors ? 8 : 0;
		
		double[] values = new double[ getSize() + 1 + extra ];
		int n = 0;
		for (int z=1; z<=getSize(); z++, n++)		
			values[z-1] = getProcessor(z).getPixelValue(x, y);
		
		
		// Test: add neighbors of original image
		if(useNeighbors)
		{
			for(int i=-1;  i < 2; i++)
				for(int j = -1; j < 2; j++)
				{
					if(i==0 && j==0)
						continue;				
					values[n] = getPixelMirrorConditions(getProcessor(1), x+i, y+j);
					n++;
				}
		}
		// Assign class
		values[values.length-1] = (double) classValue;
		
		return new DenseInstance(1.0, values);
	}
	
	/**
	 * Get pixel value from an ImageProcessor with mirror boundary conditions
	 * @param ip input image
	 * @param x x- pixel coordinate
	 * @param y y- pixel coordinate
	 * @return pixel vale
	 */
	double getPixelMirrorConditions(ImageProcessor ip, int x, int y)
	{
		int x2 = x < 0 ? -x : x;
		int y2 = y < 0 ? -y : y;
		
		if(x2 >= ip.getWidth())
			x2 = 2 * (ip.getWidth() - 1) - x2;
		
		if(y2 >= ip.getHeight())
			y2 = 2 * (ip.getHeight() - 1) - y2;
		
		return ip.getPixelValue(x2, y2);
	}

	/**
	 * Set an arbitrary stack as feature stack. Note: this method is not 
	 * compatible with the plugin GUI use since the feature names will not match.
	 * @param stack new stack of image features
	 */
	public void setStack(ImageStack stack)
	{
		this.wholeStack = stack;
	}
	
	public ImageStack getStack()
	{
		return wholeStack;
	}
	
}
