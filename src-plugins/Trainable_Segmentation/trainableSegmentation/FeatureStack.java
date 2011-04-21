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
 * 
 * filters to come:
 * - make use of color channels
 * - membraneFilters faster
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import anisotropic_diffusion.Anisotropic_Diffusion_2D;

import stitching.FloatArray2D;

import vib.BilateralFilter;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.plugin.ZProjector;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.RankFilters;


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
	/* Gabor filter flag index */					
	public static final int GABOR					= 14;
	/** Minimum filter flag index */
//	public static final int BLUR_MINIMUM			= 15;
	/** Maximum filter flag index */
//	public static final int BLUR_MAXIMUM			= 16;
	
	
	/** names of available filters */
	public static final String[] availableFeatures 
		= new String[]{	"Gaussian_blur", "Sobel_filter", "Hessian", "Difference_of_gaussians", 
					   	"Membrane_projections","Variance","Mean", "Minimum", "Maximum", "Median", 
					   	"Anisotropic_diffusion", "Bilateral", "Lipschitz", "Kuwahara", "Gabor" /*, "Blur_minimum", " Blur_maximum" */};
	/** flags of filters to be used */
	private boolean[] enableFeatures = new boolean[]{true, true, true, true, true, false, false, 
													 false, false, false, false, false, false, false, false /*, false, false */};
	/** use neighborhood flag */
	private boolean useNeighbors = false;
	/** expected membrane thickness (in pixels) */
	private int membraneSize = 1;	
	/** size of the patch to use to enhance membranes (in pixels, NxN) */
	private int membranePatchSize = 19;
	/** number of rotating angles for membrane, Kuwahara and Gabor features */
	private int nAngles = 30;
	
	/** executor service to produce concurrent threads */
	ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	/**
	 * Construct object to store stack of image features
	 * @param image original image
	 */
	public FeatureStack(ImagePlus image)
	{
		originalImage = new ImagePlus("original image", image.getProcessor().convertToFloat());
		width = image.getWidth();
		height = image.getHeight();
		wholeStack = new ImageStack(width, height);
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());
	}
	
	/**
	 * Construct object to store stack of image features
	 * @param ip original image
	 */
	public FeatureStack(ImageProcessor ip)
	{
		originalImage = new ImagePlus("original image", ip.convertToFloat());
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
	 * 
	 * @return
	 */
	public boolean useNeighborhood()
	{
		return this.useNeighbors;
	}
	
	/**
	 * 
	 * @param useNeighbors
	 */
	public void setUseNeighbors( boolean useNeighbors)
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
		gs.blur(ip, sigma);
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
				gs.blur(ip, sigma);
				return new ImagePlus (availableFeatures[GAUSSIAN] + "_" + sigma, ip);
			}
		};
	}
	
	/**
	 * Add variance-filtered image to the stack (single thread version)
	 * @param radius variance filter radius
	 */
	public void addVariance(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.VARIANCE);
		wholeStack.addSlice(availableFeatures[VARIANCE]+ "_"  + radius, ip);
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
		
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				final RankFilters filter = new RankFilters();
				filter.rank(ip, radius, RankFilters.VARIANCE);
				return new ImagePlus (availableFeatures[VARIANCE]+ "_"  + radius, ip);
			}
		};
	}
	
	
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
	
	public void addGradient(float sigma){
		GaussianBlur gs = new GaussianBlur();
		ImageProcessor ip_x = originalImage.getProcessor().duplicate();
		gs.blur(ip_x, sigma);
		Convolver c = new Convolver();
		float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
		c.convolveFloat(ip_x, sobelFilter_x, 3, 3);
		
		ImageProcessor ip_y = originalImage.getProcessor().duplicate();
		gs.blur(ip_y, sigma);
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
		
		//ip.add(-ip.getMin());
		wholeStack.addSlice(availableFeatures[SOBEL]+ "_"  +sigma, ip);
	}
	
	/**
	 * Get sobel filter version of the original image (to be called from an ExecutorService)
	 * 
	 * @param originalImage input image
	 * @param sigma radius of the Gaussian blur applied previous to the sobel filtering
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
		
				GaussianBlur gs = new GaussianBlur();
				ImageProcessor ip_x = originalImage.getProcessor().duplicate();
				gs.blur(ip_x, sigma);
				Convolver c = new Convolver();
				float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
				c.convolveFloat(ip_x, sobelFilter_x, 3, 3);
				
				ImageProcessor ip_y = originalImage.getProcessor().duplicate();
				gs.blur(ip_y, sigma);
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
				return new ImagePlus (availableFeatures[SOBEL] + "_" + sigma, ip);
			}
		};
	}
	
	
	public void addHessian(float sigma)
	{
		float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
		float[] sobelFilter_y = {1f,0f,-1f,2f,0f,-2f,1f,0f,-1f};
		Convolver c = new Convolver();				
		GaussianBlur gs = new GaussianBlur();
		
		ImageProcessor ip_x = originalImage.getProcessor().duplicate();
		gs.blur(ip_x, sigma);		
		c.convolveFloat(ip_x, sobelFilter_x, 3, 3);		
		
		ImageProcessor ip_y = originalImage.getProcessor().duplicate();
		gs.blur(ip_y, sigma);
		c = new Convolver();
		c.convolveFloat(ip_y, sobelFilter_y, 3, 3);
		
		ImageProcessor ip_xx = ip_x.duplicate();
		//gs.blur(ip_xx, sigma);		
		c.convolveFloat(ip_xx, sobelFilter_x, 3, 3);		
		
		ImageProcessor ip_xy = ip_x.duplicate();
		//gs.blur(ip_xy, sigma);		
		c.convolveFloat(ip_xy, sobelFilter_y, 3, 3);		
		
		ImageProcessor ip_yy = ip_y.duplicate();
		//gs.blur(ip_yy, sigma);		
		c.convolveFloat(ip_yy, sobelFilter_y, 3, 3);		
		
		ImageProcessor ip = new FloatProcessor(width, height);
		ImageProcessor ipTr = new FloatProcessor(width, height);
		ImageProcessor ipDet = new FloatProcessor(width, height);
		ImageProcessor ipEig1 = new FloatProcessor(width, height);
		ImageProcessor ipEig2 = new FloatProcessor(width, height);
				
		for (int x=0; x<width; x++){
			for (int y=0; y<height; y++){
				float s_xx = ip_xx.getf(x,y);
				float s_xy = ip_xy.getf(x,y);
				float s_yy = ip_yy.getf(x,y);
				ip.setf(x,y, (float) Math.sqrt(s_xx*s_xx + s_xy*s_xy+ s_yy*s_yy));
				ipTr.setf(x,y, (float) s_xx + s_yy);
				ipDet.setf(x,y, (float) s_xx*s_yy-s_xy*s_xy);
				// First eigenvalue
				ipEig1.setf(x,y, (float) ( (s_xx+s_yy)/2.0 + Math.sqrt((4*s_xy*s_xy + (s_xx - s_yy)*(s_xx - s_yy)) / 2.0 ) ) );
				// Second eigenvalue
				ipEig2.setf(x,y, (float) ( (s_xx+s_yy)/2.0 - Math.sqrt((4*s_xy*s_xy + (s_xx - s_yy)*(s_xx - s_yy)) / 2.0 ) ) );
			}
		}
		
		wholeStack.addSlice(availableFeatures[HESSIAN] + "_"  + sigma, ip);
		wholeStack.addSlice(availableFeatures[HESSIAN]+ "_Trace_"+sigma, ipTr);
		wholeStack.addSlice(availableFeatures[HESSIAN]+ "_Determinant_"+sigma, ipDet);
		wholeStack.addSlice(availableFeatures[HESSIAN]+ "_Eigenvalue_1_"+sigma, ipEig1);
		wholeStack.addSlice(availableFeatures[HESSIAN]+ "_Eigenvalue_2_"+sigma, ipEig2);
	}
	
	/**
	 * Get Hessian features from original image (to be submitted in an ExecutorService).
	 * The features include a scalar representing the Hessian, the trace, determinant, 
	 * 1st eigenvalue and 2nd eigenvalue.
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
				
				ImageProcessor ip_x = originalImage.getProcessor().duplicate();
				gs.blur(ip_x, sigma);		
				c.convolveFloat(ip_x, sobelFilter_x, 3, 3);		
				
				ImageProcessor ip_y = originalImage.getProcessor().duplicate();
				gs.blur(ip_y, sigma);
				c = new Convolver();
				c.convolveFloat(ip_y, sobelFilter_y, 3, 3);
				
				ImageProcessor ip_xx = ip_x.duplicate();
				//gs.blur(ip_xx, sigma);		
				c.convolveFloat(ip_xx, sobelFilter_x, 3, 3);		
				
				ImageProcessor ip_xy = ip_x.duplicate();
				//gs.blur(ip_xy, sigma);		
				c.convolveFloat(ip_xy, sobelFilter_y, 3, 3);		
				
				ImageProcessor ip_yy = ip_y.duplicate();
				//gs.blur(ip_yy, sigma);		
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
				
				for (int x=0; x<width; x++)
				{
					for (int y=0; y<height; y++)
					{
						float s_xx = ip_xx.getf(x,y);
						float s_xy = ip_xy.getf(x,y);
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
				return new ImagePlus ("hessian stack", hessianStack);
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
		ImageProcessor ip_1 = originalImage.getProcessor().duplicate();
		gs.blur(ip_1, sigma1);
		ImageProcessor ip_2 = originalImage.getProcessor().duplicate();
		gs.blur(ip_2, sigma2);
		
		ImageProcessor ip = new FloatProcessor(width, height);
		
		for (int x=0; x<width; x++){
			for (int y=0; y<height; y++){
				float v1 = ip_1.getf(x,y);
				float v2 = ip_2.getf(x,y);
				ip.setf(x,y, v2-v1);
			}
		}
		
		wholeStack.addSlice(availableFeatures[DOG]+ "_"+sigma1+"_"+sigma2, ip);
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
				ImageProcessor ip_1 = originalImage.getProcessor().duplicate();
				gs.blur(ip_1, sigma1);
				ImageProcessor ip_2 = originalImage.getProcessor().duplicate();
				gs.blur(ip_2, sigma2);
				
				ImageProcessor ip = new FloatProcessor(width, height);
				
				for (int x=0; x<width; x++){
					for (int y=0; y<height; y++){
						float v1 = ip_1.getf(x,y);
						float v2 = ip_2.getf(x,y);
						ip.setf(x,y, v2-v1);
					}
				}
				return new ImagePlus (availableFeatures[DOG]+ "_"+sigma1+"_"+sigma2, ip);
			}
		};
	}
	
	/**
	 * Add membrane features to the stack (single thread version)
	 * @param patchSize size of the filter to be used
	 * @param membraneSize expected membrane thickness
	 */
	public void addMembraneFeatures(int patchSize, int membraneSize){
		//create membrane patch
		ImageProcessor membranePatch = new FloatProcessor(patchSize, patchSize);
		int middle = Math.round(patchSize / 2);
		int startX = middle - (int) Math.floor(membraneSize/2.0);
		int endX = middle + (int) Math.ceil(membraneSize/2.0);
		
		for (int x=startX; x<=endX; x++){
			for (int y=0; y<patchSize; y++){
				membranePatch.setf(x, y, 1f);
			}
		}

		
		ImageStack is = new ImageStack(width, height);
		ImageProcessor rotatedPatch;
		
		// Rotate kernel 15 degrees up to 180
		for (int i=0; i<12; i++)
		{
			rotatedPatch = membranePatch.duplicate();
			//rotatedPatch.invert();
			rotatedPatch.rotate(15*i);
			//rotatedPatch.invert();
			Convolver c = new Convolver();				
	
			float[] kernel = (float[]) rotatedPatch.getPixels();
			ImageProcessor ip = originalImage.getProcessor().duplicate();		
			c.convolveFloat(ip, kernel, patchSize, patchSize);		

			is.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip);
		//	wholeStack.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip.convertToByte(true));
		}
		
		ImagePlus projectStack = new ImagePlus("membraneStack",is);
		//projectStack.show();
		
		ZProjector zp = new ZProjector(projectStack);
		zp.setStopSlice(is.getSize());
		for (int i=0;i<6; i++){
			zp.setMethod(i);
			zp.doProjection();
			wholeStack.addSlice(availableFeatures[MEMBRANE] + "_" +i+"_"+patchSize+"_"+membraneSize, zp.getProjection().getChannelProcessor());
		}
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
				ImageProcessor membranePatch = new FloatProcessor(patchSize, patchSize);
				int middle = Math.round(patchSize / 2);
				int startX = middle - (int) Math.floor(membraneSize/2.0);
				int endX = middle + (int) Math.ceil(membraneSize/2.0);
				
				for (int x=startX; x<=endX; x++){
					for (int y=0; y<patchSize; y++){
						membranePatch.setf(x, y, 1f);
					}
				}
			
				ImageStack is = new ImageStack(width, height);
				ImageProcessor rotatedPatch;
				
				final double rotationAngle = 180/nAngles;
				// Rotate kernel 15 degrees up to 180
				for (int i=0; i<nAngles; i++)
				{					
					rotatedPatch = membranePatch.duplicate();
					rotatedPatch.rotate(i*rotationAngle);
					
					Convolver c = new Convolver();				
			
					float[] kernel = (float[]) rotatedPatch.getPixels();
					ImageProcessor ip = originalImage.getProcessor().duplicate();		
					c.convolveFloat(ip, kernel, patchSize, patchSize);		

					is.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip);
				//	wholeStack.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip.convertToByte(true));
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
				return new ImagePlus ("membrane stack", membraneStack);
			}
		};
	}
	
	


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
	 * Get Gabor features (to be submitted in an ExecutorService)
	 * @param originalImage input image
	 * @param sigma
	 * @param gamma
	 * @param psi
	 * @param frequency
	 * @param nAngles
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
				
				final ImageStack is = new ImageStack(width, height);
				// Apply kernels
				for (int i=0; i<nAngles; i++)
				{
					//final double theta = rotationAngle * i;		
					final Convolver c = new Convolver();				
					
					final float[] kernel = (float[]) kernels.getProcessor(i+1).getPixels();
					final ImageProcessor ip = originalImage.getProcessor().duplicate();		
					c.convolveFloat(ip, kernel, filterSizeX, filterSizeY);		

					is.addSlice("gabor angle = " + i, ip);
				}
				
				
				final ImagePlus projectStack = new ImagePlus("filtered stack",is);
				//projectStack.show();
				
				// Normalize filtered stack (it seems necessary to have proper results)
				IJ.run(projectStack, "Enhance Contrast", "saturated=0.4 normalize normalize_all");
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
				return new ImagePlus ("Gabor stack", resultStack);
			}
		};
	}	
	
	/**
	 * Add Gabor features to current stack
	 * @param originalImage input image
	 * @param sigma
	 * @param gamma
	 * @param psi
	 * @param frequency
	 * @param nAngles
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

		final ImageStack is = new ImageStack(width, height);
		// Apply kernels
		for (int i=0; i<nAngles; i++)
		{
			//final double theta = rotationAngle * i;		
			final Convolver c = new Convolver();				

			final float[] kernel = (float[]) kernels.getProcessor(i+1).getPixels();
			final ImageProcessor ip = originalImage.getProcessor().duplicate();		
			c.convolveFloat(ip, kernel, filterSizeX, filterSizeY);		

			is.addSlice("gabor angle = " + i, ip);
		}


		final ImagePlus projectStack = new ImagePlus("filtered stack",is);
		//projectStack.show();

		// Normalize filtered stack (it seems necessary to have proper results)
		IJ.run(projectStack, "Enhance Contrast", "saturated=0.4 normalize normalize_all");
		//final ContrastEnhancer c = new ContrastEnhancer();
		//c.stretchHistogram(projectStack, 0.4);
		//projectStack.updateAndDraw();


		final ZProjector zp = new ZProjector(projectStack);
		zp.setStopSlice(is.getSize());
		for (int i=1;i<=2; i++)
		{
			zp.setMethod(i);
			zp.doProjection();
			wholeStack.addSlice(availableFeatures[GABOR] + "_" + i 
					+"_"+sigma+"_" + gamma + "_"+ (int) (psi / (Math.PI/4) ) +"_"+frequency, 
					zp.getProjection().getChannelProcessor());
		}
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
				
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				final Kuwahara filter = new Kuwahara();
				filter.applyFilter(ip, kernelSize, nAngles, criterion);
				return new ImagePlus (availableFeatures[KUWAHARA] + "_" + kernelSize + "_ " + nAngles + "_" + criterion, ip);
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
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final Kuwahara filter = new Kuwahara();
		filter.applyFilter(ip, kernelSize, nAngles, criterion);
		wholeStack.addSlice(availableFeatures[KUWAHARA] + "_" + kernelSize + "_ " + nAngles + "_" + criterion, ip);
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
				final ImagePlus result = BilateralFilter.filter(
						new ImagePlus("", originalImage.getProcessor().convertToByte(true)), spatialRadius, rangeRadius);								
				
				return new ImagePlus (availableFeatures[BILATERAL] + "_" + spatialRadius + "_" + rangeRadius, result.getProcessor().convertToFloat());								
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
		final ImagePlus result = BilateralFilter.filter(
				new ImagePlus("", originalImage.getProcessor().convertToByte(true)), spatialRadius, rangeRadius);								

		wholeStack.addSlice(availableFeatures[BILATERAL] + "_" + spatialRadius + "_" + rangeRadius, result.getProcessor().convertToFloat());								
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
				
				ImageProcessor result = originalImage.getProcessor().duplicate().convertToByte(true);
				filter.Lipschitz2D(result);				
				return new ImagePlus (availableFeatures[LIPSCHITZ] + "_" + downHat + "_" + topHat + "_" + slope, result.convertToFloat());								
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

		ImageProcessor result = originalImage.getProcessor().duplicate().convertToByte(true);
		filter.Lipschitz2D(result);				
		wholeStack.addSlice(availableFeatures[LIPSCHITZ] + "_" + downHat + "_" + topHat + "_" + slope, result.convertToFloat());								
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
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());

		int counter = 1;
		for (float i=1.0f; i<= maximumSigma; i*=2)
		{
			// Gaussian blur
			if(enableFeatures[GAUSSIAN])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addGaussianBlur(i); 
				counter++;
			}
			// Sobel
			if(enableFeatures[SOBEL])
			{
				IJ.showStatus("Creating feature stack...   " + counter);			
				this.addGradient(i); 
				counter++;
			}
			// Hessian
			if(enableFeatures[HESSIAN])
			{
				IJ.showStatus("Creating feature stack...   " + counter);			
				this.addHessian(i); 
				counter++;
			}
			// Difference of gaussians
			if(enableFeatures[DOG])
			{
				for (float j=1.0f; j<i; j*=2)
				{
					IJ.showStatus("Creating feature stack...   " + counter);				
					this.addDoG(i, j); 
					counter++;
				}
			}
			// Variance
			if(enableFeatures[VARIANCE])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addVariance(i); 
				counter++;
			}
			// Mean
			if(enableFeatures[MEAN])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addMean(i); 
				counter++;
			}
			
			// Min
			if(enableFeatures[MINIMUM])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addMin(i); 
				counter++;
			}
			// Max
			if(enableFeatures[MAXIMUM])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addMax(i); 
				counter++;
			}
			
			// Median
			if(enableFeatures[MEDIAN])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addMedian(i); 
				counter++;
			}
			
		}
		// Membrane projections
		if(enableFeatures[MEMBRANE])
			this.addMembraneFeatures(19, 1);
			
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
	}
	
	
	/**
	 * Add features based on a list of filters in a multi-thread fashion
	 */
	public void addFeaturesMT(final ImagePlus filterList)
	{
		exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		wholeStack = new ImageStack(width, height);
		//wholeStack.addSlice("original", originalImage.getProcessor().duplicate());

		
		final ArrayList< Future<ImagePlus> > futures = new ArrayList< Future<ImagePlus> >();
		
		try
		{
			for(int i=1; i<=filterList.getStackSize(); i++)
			{
				if (Thread.currentThread().isInterrupted()) 
					return;
				futures.add(exe.submit( getFilter(originalImage, filterList.getImageStack().getProcessor(i), filterList.getImageStack().getSliceLabel(i)) ) );
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
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());
		
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

		// Bilateral filter
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
			for(int i=0; i < 3; i++)
				for(double gamma = 1; gamma >= 0.25; gamma /= 2)						
					for(int frequency = 2; frequency<=3; frequency ++)
					{
						if (Thread.currentThread().isInterrupted()) 
							return false;
						final double psi = Math.PI / 4 * i;
						//IJ.log( n++ +": Calculating Gabor filter (1.0, " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
						addGabor(originalImage, 1.0, gamma, psi, frequency, nAngles);
					}
			// elongated filters in x- axis (sigma = [2.0 - 4.0], gamma = [1.0 - 2.0])
			for(int i=0; i < 3; i++)
				for(double sigma = 2.0; sigma <= 4.0; sigma *= 2)					
					for(double gamma = 1.0; gamma <= 2.0; gamma *= 2)
						for(int frequency = 2; frequency<=3; frequency ++)
						{
							if (Thread.currentThread().isInterrupted()) 
								return false;
							final double psi = Math.PI / 4 * i;
							//IJ.log( n++ +": Calculating Gabor filter (" + sigma + " , " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
							addGabor(originalImage, sigma, gamma, psi, frequency, nAngles);
						}								
		}

		// Sobel (no blur)
		if(enableFeatures[SOBEL])
		{
			if ( Thread.currentThread().isInterrupted() ) 
				return false;
			//IJ.log(n++ + ": Calculating Sobel filter (0.0)");
			addGradient(0);
		}
		// Hessian (no blur)
		if(enableFeatures[HESSIAN])
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

		}
		// Membrane projections
		if(enableFeatures[MEMBRANE])
		{
			if (Thread.currentThread().isInterrupted()) 
				return false;
			//IJ.log( n++ +": Calculating Membranes projections ("+ membranePatchSize + ", " + membraneSize + ")");
			addMembraneFeatures(membranePatchSize, membraneSize);
		}
		
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
		return true;
	}
	
	
	/**
	 * Update features with current list in a multi-thread fashion
	 * 
	 * @return true if the features are correctly updated 
	 */
	public boolean updateFeaturesMT()
	{
		exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		wholeStack = new ImageStack(width, height);
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());

		final ArrayList< Future<ImagePlus> > futures = new ArrayList< Future<ImagePlus> >();
		//int n=0;
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
			
			// Bilateral filter
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
				for(int i=0; i < 3; i++)
					for(double gamma = 1; gamma >= 0.25; gamma /= 2)						
						for(int frequency = 2; frequency<=3; frequency ++)
						{
							if (Thread.currentThread().isInterrupted()) 
								return false;
							final double psi = Math.PI / 4 * i;
							//IJ.log( n++ +": Calculating Gabor filter (1.0, " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
							futures.add(exe.submit( getGabor(originalImage, 1.0, gamma, psi, frequency, nAngles) ) );
						}
				// elongated filters in x- axis (sigma = [2.0 - 4.0], gamma = [1.0 - 2.0])
				for(int i=0; i < 3; i++)
					for(double sigma = 2.0; sigma <= 4.0; sigma *= 2)					
						for(double gamma = 1.0; gamma <= 2.0; gamma *= 2)
							for(int frequency = 2; frequency<=3; frequency ++)
							{
								if (Thread.currentThread().isInterrupted()) 
									return false;
								final double psi = Math.PI / 4 * i;
								//IJ.log( n++ +": Calculating Gabor filter (" + sigma + " , " + gamma + ", " + psi + ", " + frequency + ", " + nAngles + ")");
								futures.add(exe.submit( getGabor(originalImage, sigma, gamma, psi, frequency, nAngles) ) );
							}								
			}
			
			// Sobel (no blur)
			if(enableFeatures[SOBEL])
			{
				if ( Thread.currentThread().isInterrupted() ) 
					return false;
				//IJ.log(n++ + ": Calculating Sobel filter (0.0)");
				futures.add(exe.submit( getGradient(originalImage, 0)) );
			}
			// Hessian (no blur)
			if(enableFeatures[HESSIAN])
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
/*
				// Blur Min
				if(enableFeatures[BLUR_MINIMUM])
				{
					for(float j = i/2; j<= i; j*=2)
						futures.add(exe.submit( getBlurMin(originalImage, i, j)) );
				}
				// Blur Max
				if(enableFeatures[BLUR_MAXIMUM])
				{
					for(float j = i/2; j<= i; j*=2)
						futures.add(exe.submit( getBlurMax(originalImage, i, j)) );
				}
*/				
				// Median
				if(enableFeatures[MEDIAN])
				{
					//IJ.log( n++ +": Calculating Median filter ("+ i + ")");
					futures.add(exe.submit( getMedian(originalImage, i)) );
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
			exe.shutdown();
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
		{
			values[z-1] = getProcessor(z).getPixelValue(x, y);
		}
		
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
	 * 
	 * @param ip
	 * @param x
	 * @param y
	 * @return
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
	
}
