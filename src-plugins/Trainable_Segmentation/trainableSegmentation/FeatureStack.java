package trainableSegmentation;
/** 
 * This class is intended for the Trainable_Segmentation plugin. It creates and holds
 * different feature images for the classification. Possible filters include:
 * - Gaussianblur
 * - Gradientmagnitude
 * - Hessian
 * - Difference of Gaussian
 * - Orientation filter to detect membranes and then its projection
 * - Mean
 * - Variance
 * - Minimum
 * - Maximum
 * - Anisotropic diffusion
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
	/** maximum sigma/radius used in the filters */
	private static final int MAX_SIGMA = 16;
	
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
	/** names of available filters */
	public static final String[] availableFeatures 
		= new String[]{	"Gaussian_blur", "Sobel_filter", "Hessian", "Difference_of_gaussians", 
					   	"Membrane_projections","Variance","Mean", "Minimum", "Maximum", "Median", "Anisotropic_diffusion"};
	/** flags of filters to be used */
	private boolean[] enableFeatures = new boolean[]{true, true, true, true, true, false, false, false, false, false, false};
	/** normalization flag */
	private boolean normalize = false;
	/** expected membrane thickness (in pixels) */
	private int membraneSize = 1;
	
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
	 * Add Gaussian blur slice to current stack
	 * @param sigma Gaussian radius
	 */
	public void addGaussianBlur(float sigma){
		ImageProcessor ip = originalImage.getProcessor().duplicate();
		GaussianBlur gs = new GaussianBlur();
		gs.blur(ip, sigma);
		wholeStack.addSlice(availableFeatures[GAUSSIAN] + "_" + sigma, ip);
	}
	/**
	 * Calculate gaussian filter concurrently
	 * @param originalImage original input image
	 * @param sigma gaussian sigma
	 * @return result image
	 */
	public Callable<ImagePlus> getGaussianBlur(
			final ImagePlus originalImage,
			final float sigma)
	{
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
	
	public Callable<ImagePlus> getMean(
			final ImagePlus originalImage,
			final float radius)
	{
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
	
	public Callable<ImagePlus> getMin(
			final ImagePlus originalImage,
			final float radius)
	{
		return new Callable<ImagePlus>(){
			public ImagePlus call(){
		
				final ImageProcessor ip = originalImage.getProcessor().duplicate();
				final RankFilters filter = new RankFilters();
				filter.rank(ip, radius, RankFilters.MIN);
				return new ImagePlus (availableFeatures[MINIMUM]+ "_"  + radius, ip);
			}
		};
	}
	
	public void addMax(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MAX);
		wholeStack.addSlice(availableFeatures[MAXIMUM]+ "_"  + radius, ip);
	}
	
	public Callable<ImagePlus> getMax(
			final ImagePlus originalImage,
			final float radius)
	{
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
	
	public Callable<ImagePlus> getMedian(
			final ImagePlus originalImage,
			final float radius)
	{
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
	
	public Callable<ImagePlus> getGradient(
			final ImagePlus originalImage,
			final float sigma)
	{
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
				
		for (int x=0; x<width; x++){
			for (int y=0; y<height; y++){
				float s_xx = ip_xx.getf(x,y);
				float s_xy = ip_xy.getf(x,y);
				float s_yy = ip_yy.getf(x,y);
				ip.setf(x,y, (float) Math.sqrt(s_xx*s_xx + s_xy*s_xy+ s_yy*s_yy));
				ipTr.setf(x,y, (float) s_xx + s_yy);
				ipDet.setf(x,y, (float) s_xx*s_yy-s_xy*s_xy);
			}
		}
		
		wholeStack.addSlice(availableFeatures[HESSIAN] + "_"  + sigma, ip);
		wholeStack.addSlice(availableFeatures[HESSIAN]+ "_Trace_"+sigma, ipTr);
		wholeStack.addSlice(availableFeatures[HESSIAN]+ "_Determinant_"+sigma, ipDet);
	}
	
	public Callable<ImagePlus> getHessian(
			final ImagePlus originalImage,
			final float sigma)
	{
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
						
				for (int x=0; x<width; x++){
					for (int y=0; y<height; y++){
						float s_xx = ip_xx.getf(x,y);
						float s_xy = ip_xy.getf(x,y);
						float s_yy = ip_yy.getf(x,y);
						ip.setf(x,y, (float) Math.sqrt(s_xx*s_xx + s_xy*s_xy+ s_yy*s_yy));
						ipTr.setf(x,y, (float) s_xx + s_yy);
						ipDet.setf(x,y, (float) s_xx*s_yy-s_xy*s_xy);
					}
				}
				
				ImageStack hessianStack = new ImageStack(width, height);
				hessianStack.addSlice(availableFeatures[HESSIAN] + "_"  + sigma, ip);
				hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Trace_"+sigma, ipTr);
				hessianStack.addSlice(availableFeatures[HESSIAN]+ "_Determinant_"+sigma, ipDet);
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
			rotatedPatch.invert();
			rotatedPatch.rotate(15*i);
			rotatedPatch.invert();
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
				
				// Rotate kernel 15 degrees up to 180
				for (int i=0; i<12; i++)
				{					
					rotatedPatch = membranePatch.duplicate();
					rotatedPatch.rotate(15*i);
					
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
	
	/**
	 * Apply anisotropic diffusion in a concurrent way (to be submitted in an ExecutorService)
	 * @param originalImage input image
	 * @param nb_iter number of iterations
	 * @param saveSteps number of steps after which we save the intermediate results
	 * @param nb_smoothings number of smoothings per iteration
	 * @param a1 diffusion limiter along minimal variations
	 * @param a2 diffusion limiter along maximal variations
	 * @return result image
	 */
	public Callable<ImagePlus> getAnisotropicDiffusion(
			final ImagePlus originalImage,
			final int nb_iter, 
			final int saveSteps,
			final int nb_smoothings,
			final float a1,
			final float a2)
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
				
				final ImagePlus result = ad.runTD(originalImage.getProcessor());
				
				
				if(result.getImageStackSize() == 1)
				{
					return new ImagePlus (availableFeatures[ANISOTROPIC_DIFFUSION] + "_" + nb_iter + "_" + nb_smoothings + "_" + a1 + "_" + a2, result.getProcessor());
				}
				else
				{
					final ImageStack slices = result.getImageStack();
					slices.deleteSlice(1); // delete original image
					for(int i = 1; i <= slices.getSize() ; i++)
						slices.setSliceLabel(availableFeatures[ANISOTROPIC_DIFFUSION] + "_" + (saveSteps * i) + "_" + nb_smoothings + "_" + a1 + "_" + a2, i);
					
					return new ImagePlus("Anisotropic diffusion", slices);
				}
				
 
			}
		};
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
		//xcorrImage = CommonFunctions.comanyway, you talked about the time to change you
		
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
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=wholeStack.getSize(); i++){
			String attString = wholeStack.getSliceLabel(i);
			attributes.add(new Attribute(attString));
		}
		
		attributes.add(new Attribute("class", classes));
		
		Instances data =  new Instances("segment", attributes, width*height);
		
		Object[] pixelData = wholeStack.getImageArray();
		
		for (int y=0; y<wholeStack.getHeight(); y++)
		{
			IJ.showProgress(y, wholeStack.getHeight());
			for (int x=0; x<wholeStack.getWidth(); x++)
			{
				double[] values = new double[wholeStack.getSize()+1];
				for (int z=1; z<=wholeStack.getSize(); z++)
				{
					values[z-1] = ((float[]) pixelData[z-1])[y*width+x];
					//System.out.println("" + wholeStack.getProcessor(z).getPixelValue(x, y) + " * " + values[z-1]);
				}
				values[wholeStack.getSize()] = 0.0;
				data.add(new DenseInstance(1.0, values));
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
		for (float i=1.0f; i<FeatureStack.MAX_SIGMA; i*=2){
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
	 */
	public void updateFeatures()
	{
		wholeStack = new ImageStack(width, height);
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());

		int counter = 1;
		for (float i=1.0f; i<= FeatureStack.MAX_SIGMA; i*=2)
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
		
		if(normalize)
		{
			IJ.showStatus("Normalizing stack...");
			final ImagePlus imp = new ImagePlus("", this.wholeStack);
			IJ.run(imp, "Enhance Contrast", "saturated=0.1 normalize_all");
		}
		
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
	}
	
	/**
	 * Update features with current list in a multi-thread fashion
	 */
	public void updateFeaturesMT()
	{
		wholeStack = new ImageStack(width, height);
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<ImagePlus> > futures = new ArrayList< Future<ImagePlus> >();
		
		try{
			
			// Anisotropic Diffusion
			if(enableFeatures[ANISOTROPIC_DIFFUSION])
			{
				for(int i = 1; i < 8; i += 3)
					for(float j = 0.10f; j < 0.5f; j+= 0.25f)
						futures.add(exe.submit( getAnisotropicDiffusion(originalImage, 20, 20, i, j, 0.9f) ) );
			}
			for (float i=1.0f; i<= FeatureStack.MAX_SIGMA; i*=2)
			{
				// Gaussian blur
				if(enableFeatures[GAUSSIAN])
				{
					futures.add(exe.submit( getGaussianBlur(originalImage, i)) );
				}
				// Sobel
				if(enableFeatures[SOBEL])
				{
					futures.add(exe.submit( getGradient(originalImage, i)) );
				}
				// Hessian
				if(enableFeatures[HESSIAN])
				{
					futures.add(exe.submit( getHessian(originalImage, i)) );
				}
				// Difference of gaussians
				if(enableFeatures[DOG])
				{
					for (float j=1.0f; j<i; j*=2)
					{
						futures.add(exe.submit( getDoG(originalImage, i, j)) );
					}
				}
				// Variance
				if(enableFeatures[VARIANCE])
				{
					futures.add(exe.submit( getVariance(originalImage, i)) );
				}
				// Mean
				if(enableFeatures[MEAN])
				{
					futures.add(exe.submit( getMean(originalImage, i)) );
				}

				// Min
				if(enableFeatures[MINIMUM])
				{
					futures.add(exe.submit( getMin(originalImage, i)) );
				}
				// Max
				if(enableFeatures[MAXIMUM])
				{
					futures.add(exe.submit( getMax(originalImage, i)) );
				}

				// Median
				if(enableFeatures[MEDIAN])
				{
					futures.add(exe.submit( getMedian(originalImage, i)) );
				}

			}
			// Membrane projections
			if(enableFeatures[MEMBRANE])
				futures.add(exe.submit( getMembraneFeatures(originalImage, 19, membraneSize) ));						

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
		}
		finally{
			exe.shutdown();
		}
		
		if(normalize)
		{
			IJ.showStatus("Normalizing stack...");
			final ImagePlus imp = new ImagePlus("", this.wholeStack);
			IJ.run(imp, "Enhance Contrast", "saturated=0.1 normalize_all");
		}
		
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
	}
	
	/**
	 * Set list of boolean flags for enabled features
	 * @param enableFeatures list of boolean flags to enable features
	 */
	public void setEnableFeatures(boolean[] enableFeatures) {
		this.enableFeatures = enableFeatures;
	}

	/**
	 * Get the list of enabled features flags
	 * @return list of boolean flags (true means the feature is selected)
	 */
	public boolean[] getEnableFeatures() {
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

	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}

	public boolean isNormalized() {
		return normalize;
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
	
}
