package trainableSegmentation.utils;

/**
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
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu)
 */

import java.awt.Color;
import java.util.ArrayList;

import javax.vecmath.Point3f;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.measure.Measurements;
import ij.plugin.filter.GaussianBlur;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import trainableSegmentation.metrics.ClassificationStatistics;
import util.FindConnectedRegions;
import util.FindConnectedRegions.Results;
import weka.core.Instances;

/**
 * This class implements useful methods for the Weka Segmentation library.
 */
public class Utils {
	
	/**
	 * Connected components based on Find Connected Regions (from Mark Longair)
	 * @param im input image
	 * @param adjacency number of neighbors to check (4, 8...)
	 * @return list of images per region, all-regions image and regions info
	 */
	public static Results connectedComponents(final ImagePlus im, final int adjacency)
	{
		if( adjacency != 4 && adjacency != 8 )
			return null;

		final boolean diagonal = adjacency == 8 ? true : false;

		FindConnectedRegions fcr = new FindConnectedRegions();
		try {
			final Results r = fcr.run( im,
				 diagonal,
				 false,
				 true,
				 false,
				 false,
				 false,
				 false,
				 0,
				 1,
				 -1,
				 true /* noUI */ );
			return r;

		} catch( IllegalArgumentException iae ) {
			IJ.error(""+iae);
			return null;
		}

	}
	
	
	/**
	 * Connected components based on Find Connected Regions (from Mark Longair)
	 * @param im input image
	 * @param adjacency number of neighbors to check (4, 8...)
	 * @param minSize minimum size (in pixels) of the components
	 * @return list of images per region, all-regions image and regions info
	 */
	public static Results connectedComponents(
			final ImagePlus im, 
			final int adjacency,
			final int minSize)
	{
		if( adjacency != 4 && adjacency != 8 )
			return null;

		final boolean diagonal = adjacency == 8 ? true : false;

		FindConnectedRegions fcr = new FindConnectedRegions();
		try {
			final Results r = fcr.run( im,
				 diagonal,
				 false,
				 true,
				 false,
				 false,
				 false,
				 false,
				 0,
				 minSize,
				 -1,
				 true /* noUI */ );
			return r;

		} catch( IllegalArgumentException iae ) {
			IJ.error(""+iae);
			return null;
		}

	}
	
	/**
	 * Plot the precision-recall curve
	 * @param stats classification statistics
	 */
	public static void plotPrecisionRecall(
			ArrayList< ClassificationStatistics > stats)
	{
		// Extract precision and recall values
		float[] precision = new float[ stats.size() ];
		float[] recall = new float[ stats.size() ];
		
		for(int i = 0; i < precision.length; i++)
		{
			precision[i] = (float) stats.get(i).precision;
			recall[i] = (float) stats.get(i).recall;
		}

		Plot pl = new Plot("Precision-Recall curve", "Recall [tp / (tp + fn)]", "Precision [tp / (tp+fp)]", recall, precision);		
		pl.setLimits(0, 1, 0, 1);
		pl.setSize(540, 512);
		pl.setColor(Color.GREEN);
		pl.show();
	}
	
	/**
	 * Plot the Receiver operating characteristic curve
	 * @param stats classification statistics
	 */
	public static void plotROC(
			ArrayList< ClassificationStatistics > stats)
	{
		// Extract true positive and true negative rates
		float[] tpr = new float[ stats.size() ];
		float[] fpr = new float[ stats.size() ];
		
		for(int i = 0; i < tpr.length; i++)
		{
			tpr[i] = (float) stats.get(i).recall;
			fpr[i] = (float) (1f - stats.get(i).specificity);
		}

		Plot pl = new Plot("Receiver Operating Characteristic curve", "False Positive Rate (1 - specificity)", "True Positive Rate or sensitivity", fpr, tpr );		
		pl.setLimits(0, 1, 0, 1);
		pl.setSize(540, 512);
		pl.setColor(Color.RED);
		pl.show();
	}
	
	/**
	 * Get area under the Precision/Recall curve
	 * @param stats classification statistics with the ROC curve information
	 * @return area under the input curve
	 */
	public static double getPrecRecArea(
			ArrayList< ClassificationStatistics > stats)
	{
		
		final int n = stats.size();
		double area = 0;
	    double xlast = stats.get( n - 1 ).recall;
	    
	    // start from the first real precision/recall pair (not the artificial zero point)
	    for (int i = n - 2; i >= 0; i--) 
	    {
	      double recallDelta = stats.get( i ).recall - xlast;
	      area += (stats.get( i ).precision * recallDelta);
	      
	      xlast = stats.get( i ).recall;
	    }
	    

	    return area;
	}
	
	  /**
	   * Calculates the area under the ROC curve as the Wilcoxon-Mann-Whitney statistic.
	   *
	   * @param tcurve a previously extracted threshold curve Instances.
	   * @return the ROC area, or Double.NaN if you don't pass in 
	   * a ThresholdCurve generated Instances. 
	   */
	  public static double getROCArea(ArrayList< ClassificationStatistics > stats) 
	  {
		  final int n = stats.size();
		  double area = 0;

		  double cumNeg = 0.0;
		  
		  // Get total number of positives and negatives assuming the first
		  // element of the list corresponds to threshold 0 (so all samples are
		  // considered positive)
		  final double totalPos = stats.get( 0 ).truePositives;
		  final double totalNeg = stats.get( 0 ).falsePositives;
		  
		  for (int i = 0; i < n; i++) 
		  {
			  double cip, cin;
			  if (i < n - 1) 
			  {
				  cip = stats.get( i ).truePositives - stats.get( i + 1 ).truePositives;
				  cin = stats.get( i ).falsePositives - stats.get( i + 1 ).falsePositives;
			  } else {
				  cip = stats.get( n - 1 ).truePositives;
				  cin = stats.get( n - 1 ).falsePositives;
			  }
			  area += cip * (cumNeg + (0.5 * cin));
			  cumNeg += cin;
		  }
		  area /= (totalNeg * totalPos);

		  return area;
	  }
	

	/**
	 * Get Kappa statistic
	 * @param stats classification statistics
	 * @return Kappa statistic
	 */
	public static double getKappa(
			ClassificationStatistics stats)
	{
		
		double correct = stats.truePositives + stats.trueNegatives;
		double numSamples = stats.truePositives + stats.falsePositives + stats.falseNegatives + stats.trueNegatives;

		double chanceAgreement = (stats.truePositives + stats.falsePositives) * (stats.truePositives + stats.falseNegatives)
					+ (stats.falseNegatives + stats.trueNegatives) * (stats.falsePositives + stats.trueNegatives);

		chanceAgreement /= (numSamples * numSamples);
		correct /= numSamples;

		double kappa = 1.0;
		if (chanceAgreement < 1) 
		     	kappa = (correct - chanceAgreement) / (1 - chanceAgreement);
	    

	    return kappa;
	}
	
	
	/**
	 * Create plot with the precision-recall curve
	 * @param stats classification statistics
	 * @return precision-recall plot
	 */
	public static Plot createPrecisionRecallPlot(
			ArrayList< ClassificationStatistics > stats)
	{
		// Extract precision and recall values		
		float[] precision = new float[ stats.size() ];
		float[] recall = new float[ stats.size() ];
		
		for(int i = 0; i < precision.length; i++)
		{
			precision[i] = (float) stats.get(i).precision;
			recall[i] = (float) stats.get(i).recall;
		}

		Plot pl = new Plot("Precision-Recall curve", "Recall [tp / (tp + fn)]", "Precision [tp / (tp+fp)]", recall, precision);		
		pl.setLimits(0, 1, 0, 1);
		pl.setSize(540, 512);
		pl.setColor(Color.GREEN);
		return pl;
	}
	
	/**
	 * Normalize an image stack so it has 0 mean and unit variance
	 * @param inputStack input stack
	 * @return normalize stack
	 */
	public static ImageStack normalize( ImageStack inputStack )
	{
		// new stack
		ImageStack is = new ImageStack( inputStack.getWidth(), inputStack.getHeight() );

		for(int slice = 1; slice<=inputStack.getSize(); slice++)
		{					
			is.addSlice(inputStack.getSliceLabel( slice ),  normalize( inputStack.getProcessor( slice ) ));
		}

		return is;
	}

	/**
	 * Normalize an image so it have 0 mean and unit variance
	 * @param ip input image
	 * @return normalized image (32-bit)
	 */
	private static FloatProcessor normalize(ImageProcessor ip) 
	{
		// get mean and standard deviation of input image
		ImageStatistics stats = ImageStatistics.getStatistics( ip, Measurements.MEAN + Measurements.STD_DEV, null);

		FloatProcessor fp = (FloatProcessor) ip.convertToFloat();

		// subtract mean
		fp.subtract(stats.mean);
		
		// divide by std dev
		fp.multiply(1.0 / stats.stdDev);
		return fp;
	}

	
	/**
	 * Blurs probability image with a given symmetrically weighted kernel.
	 * Reused from the SIOX package. 
	 *
	 * @param fp probability image to be smoothed
	 * @param f1 Weight factor for the first pixel.
	 * @param f2 Weight factor for the mid-pixel.
	 * @param f3 Weight factor for the last pixel.
	 */
	public static void smooth(
			FloatProcessor fp, 
			float f1, 
			float f2, 
			float f3)
	{
		
		final float[] cm = (float[]) fp.getPixels();
		final int xres = fp.getWidth();
		final int yres = fp.getHeight();
		
		for (int y=0; y<yres; y++) {
			for (int x=0; x<xres-2; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=f1*cm[idx]+f2*cm[idx+1]+f3*cm[idx+2];
			}
		}
		for (int y=0; y<yres; y++) {
			for (int x=xres-1; x>=2; x--) {
				final int idx=(y*xres)+x;
				cm[idx]=f3*cm[idx-2]+f2*cm[idx-1]+f1*cm[idx];
			}
		}
		for (int y=0; y<yres-2; y++) {
			for (int x=0; x<xres; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=f1*cm[idx]+f2*cm[((y+1)*xres)+x]+f3*cm[((y+2)*xres)+x];
			}
		}
		for (int y=yres-1; y>=2; y--) {
			for (int x=0; x<xres; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=f3*cm[((y-2)*xres)+x]+f2*cm[((y-1)*xres)+x]+f1*cm[idx];
			}
		}
	}
	
	
	/**
	 * Applies the morphological erode operator.
	 * Reused from the SIOX package. 
	 *
	 * @param fp probability image to be eroded.
	 */
	public static void erode(FloatProcessor fp)
	{
		final float[] cm = (float[]) fp.getPixels();
		final int xres = fp.getWidth();
		final int yres = fp.getHeight();
		
		
		for (int y=0; y<yres; y++) {
			for (int x=0; x<xres-1; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=Math.min(cm[idx], cm[idx+1]);
			}
		}
		for (int y=0; y<yres; y++) {
			for (int x=xres-1; x>=1; x--) {
				final int idx=(y*xres)+x;
				cm[idx]=Math.min(cm[idx-1], cm[idx]);
			}
		}
		for (int y=0; y<yres-1; y++) {
			for (int x=0; x<xres; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=Math.min(cm[idx], cm[((y+1)*xres)+x]);
			}
		}
		for (int y=yres-1; y>=1; y--) {
			for (int x=0; x<xres; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=Math.min(cm[((y-1)*xres)+x], cm[idx]);
			}
		}
	}
	
	/**
	 * Applies the morphological dilate operator.
	 * Reused from the SIOX package. 
	 * Can be used to close small holes in the probability image.
	 *
	 * @param fp probability image to be dilated
	 */
	public static void dilate(FloatProcessor fp)	
	{
		final float[] cm = (float[]) fp.getPixels();
		final int xres = fp.getWidth();
		final int yres = fp.getHeight();

		for (int y=0; y<yres; y++) {
			for (int x=0; x<xres-1; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=Math.max(cm[idx], cm[idx+1]);
			}
		}
		for (int y=0; y<yres; y++) {
			for (int x=xres-1; x>=1; x--) {
				final int idx=(y*xres)+x;
				cm[idx]=Math.max(cm[idx-1], cm[idx]);
			}
		}
		for (int y=0; y<yres-1; y++) {
			for (int x=0; x<xres; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=Math.max(cm[idx], cm[((y+1)*xres)+x]);
			}
		}
		for (int y=yres-1; y>=1; y--) {
			for (int x=0; x<xres; x++) {
				final int idx=(y*xres)+x;
				cm[idx]=Math.max(cm[((y-1)*xres)+x], cm[idx]);
			}
		}
	}

	/**
	 * Apply binary threshold to input image
	 * @param ip input image
	 * @param thresholdValue threshold value (all pixel above that value will be set to 255, the rest to 0)
	 * @return binary result
	 */
	public static ByteProcessor threshold (ImageProcessor ip, double thresholdValue)
	{
		final ByteProcessor result = new ByteProcessor(ip.getWidth(), ip.getHeight());
		
		for(int x=0; x<ip.getWidth(); x++)
			for(int y=0; y<ip.getHeight(); y++)
			{
				if( ip.getPixelValue(x, y) > thresholdValue)
					result.putPixelValue(x, y, 255);
				else
					result.putPixelValue(x, y, 0);
			}
						
		return result;
	}
	
	/**
	 * Post-process probability image to get more reasonable objects
	 * at a certain threshold
	 * 
	 * @param probabilityMap probability image
	 * @param smoothIterations number of smoothing iterations
	 * @param threshold threshold to use
	 * @param minSize minimum object size (in pixels)
	 * @param binarize flag to binarize results
	 */
	public static void postProcess( 
			FloatProcessor probabilityMap, 
			int smoothIterations,
			double threshold,
			int minSize,
			boolean binarize)
	{
		//smooth( probabilityMap, 0.33f, 0.33f, 0.33f );
		GaussianBlur gb = new GaussianBlur();
		gb.blur(probabilityMap, 2);
		
		normalize01( probabilityMap );
		erode( probabilityMap );
		
		filterSmallObjectsAndHoles(probabilityMap, threshold, minSize);
		
		for (int i=0; i<smoothIterations; i++) 
			//smooth( probabilityMap, 0.33f, 0.33f, 0.33f );
			gb.blur(probabilityMap, 2);
		normalize01( probabilityMap );
		
		filterSmallObjectsAndHoles(probabilityMap, threshold, minSize);
		
		if( binarize )
		{
			float[] pixels = (float[]) probabilityMap.getPixels();
			for(int i=0; i<pixels.length; i++)
				if( pixels[ i ] > threshold )
					pixels[ i ] = 1.0f;
				else
					pixels[ i ] = 0.0f;
			
		}	
		
		dilate( probabilityMap );
		normalize01(probabilityMap);
	}

	/**
	 * Normalize float image so the pixel are between 0 and 1
	 * @param fp input image
	 */
	public static void normalize01( FloatProcessor fp )
	{
		fp.resetMinAndMax();
		double max = fp.getMax();
		double min = fp.getMin();				
		
		double scale = max>min?1.0/(max-min):1.0;
		int size = fp.getWidth()*fp.getHeight();
		float[] pixels = (float[])fp.getPixels();
		double v;
		for (int i=0; i<size; i++) {
			v = pixels[i] - min;
			if (v<0.0) v = 0.0;
			v *= scale;
			if (v>1.0) v = 1.0;
			pixels[i] = (float)v;
		}
	}
	
	/**
	 * Filter small objects and holes at a specific threshold value
	 * @param probabilityMap probability image
	 * @param thresholdValue threshold to use
	 * @param minSize minimum size of the objects (in pixels)
	 */
	public static void filterSmallObjectsAndHoles(
			FloatProcessor probabilityMap,
			double thresholdValue, 
			int minSize) 
	{
		// apply threshold 
		ByteProcessor thresholded = threshold(probabilityMap, thresholdValue);
		
		// Calculate connected components above the minimum size
		Results res = connectedComponents( new ImagePlus("thresholded", thresholded), 4, minSize);
		
		//res.allRegions.show();
		
		// Binarize components image (after removing small objects)
		ByteProcessor th = threshold( res.allRegions.getProcessor(), 0.5 );
		
		// Localize small objects by the difference with the original thresholded image
		ByteProcessor th2 = (ByteProcessor) th.duplicate();
		th2.copyBits(thresholded, 0, 0, Blitter.DIFFERENCE);				
		
		byte[] th2pixels = (byte[])th2.getPixels();
		final float[] probPixels = (float[])probabilityMap.getPixels();
				
		// Set those pixels to background in the probability image
		for(int i=0; i<th2pixels.length; i++)
		{
			if( th2pixels[ i ] != 0)
				probPixels[ i ] = 0;
		}
				
		// Localize holes by the removing them first from the image
		// without small objects and then looking at the difference
		th2 = (ByteProcessor) th.duplicate();
		
		// Fill holes in the thresholded components image
		fill( th2, 255, 0 );
						
		th2.copyBits(th, 0, 0, Blitter.DIFFERENCE);
		th2pixels = (byte[])th2.getPixels();
										
		// Set those pixels to foreground in the probability image
		for(int i=0; i<th2pixels.length; i++)
		{
			if( th2pixels[ i ] != 0)
				probPixels[ i ] = 1;
		}
		
	}
	
	
	// Binary fill by Gabriel Landini, G.Landini at bham.ac.uk
	// 21/May/2008
	public static void fill(ImageProcessor ip, int foreground, int background) 
	{
		int width = ip.getWidth();
		int height = ip.getHeight();
		FloodFiller ff = new FloodFiller(ip);
		ip.setColor(127);
		for (int y=0; y<height; y++) {
			if (ip.getPixel(0,y)==background) ff.fill(0, y);
			if (ip.getPixel(width-1,y)==background) ff.fill(width-1, y);
		}
		for (int x=0; x<width; x++){
			if (ip.getPixel(x,0)==background) ff.fill(x, 0);
			if (ip.getPixel(x,height-1)==background) ff.fill(x, height-1);
		}
		byte[] pixels = (byte[])ip.getPixels();
		int n = width*height;
		for (int i=0; i<n; i++) {
			if (pixels[i]==127)
				pixels[i] = (byte)background;
			else
				pixels[i] = (byte)foreground;
		}
	}
	
	/**
	 * Get the binary class coordinates from a label image (2D image or stack)
	 * 
	 * @param labelImage labels (they can be in any format, black = 0)
	 * @param mask binary mask to select the pixels to be extracted
	 * @return array with the two lists (black and white) of sample coordinates
	 */
	public static ArrayList< Point3f >[] getClassCoordinates( 
			ImagePlus labelImage,
			ImagePlus mask)
	{
		final ArrayList< Point3f >[] classPoints = new ArrayList[2];
		classPoints[ 0 ] = new ArrayList< Point3f >();
		classPoints[ 1 ] = new ArrayList< Point3f >();
		
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();
		final int size = labelImage.getImageStackSize();
		
		if( null != mask )
		{					
			for(int slice = 1; slice <= size; slice ++)
			{
				final float[] labelsPix = (float[]) labelImage.getImageStack().getProcessor( slice ).convertToFloat().getPixels();
				final float[] maskPix = (float[]) mask.getImageStack().getProcessor( slice ).convertToFloat().getPixels();
				
				for(int x = 0; x < width; x++)
					for( int y = 0; y < height; y++ )
						if( maskPix[ x + y * width] > 0 )
						{
							if( labelsPix[ x + y * width] != 0)				
								classPoints[ 1 ].add( new Point3f( new float[]{ x, y, slice-1}) );					
							else				
								classPoints[ 0 ].add( new Point3f( new float[]{ x, y, slice-1}) );
						}
			}
		}
		else
		{
			for(int slice = 1; slice <= size; slice ++)
			{
				final float[] labelsPix = (float[]) labelImage.getImageStack().getProcessor( slice ).convertToFloat().getPixels();
				
				for(int x = 0; x < width; x++)
					for( int y = 0; y < height; y++ )					
							if( labelsPix[ x + y * width] != 0)				
								classPoints[ 1 ].add( new Point3f( new float[]{ x, y, slice-1}) );					
							else				
								classPoints[ 0 ].add( new Point3f( new float[]{ x, y, slice-1}) );
					
			}
		}
		return classPoints;
	}

	public static ImagePlus[] maxPool( 
			ImagePlus input,
			ImagePlus label,
			int sizeX,
			int sizeY)
	{
		final int maxPoolWidth = input.getWidth() / sizeX;
		final int maxPoolHeight = input.getHeight() / sizeY;
		
		final int inputWidth = input.getWidth();
		final int inputHeight = input.getHeight();
		
		ImageStack isMaxPoolInput = new ImageStack(maxPoolWidth, maxPoolHeight);
		ImageStack isMaxPoolLabel = new ImageStack(maxPoolWidth, maxPoolHeight);
		
		ImagePlus[] maxPool = new ImagePlus[ 2 ];
		
		for(int slice = 1; slice <= input.getImageStackSize(); slice ++)
		{
			IJ.log("Processing slice " + slice + "...");
			
			double[] inputPix = new double [ maxPoolWidth * maxPoolHeight ];
			byte[] labelPix = new byte [ maxPoolWidth * maxPoolHeight ];
				
			for(int y=0, pos2 = 0; y<inputHeight; y += sizeY)
				for(int x=0; x<inputWidth; x += sizeX)				
				{
					double max = 0;										
					
					for(int x2=0; x2<sizeX; x2++)
						for(int y2=0; y2<sizeY; y2++)
						{
							final int pos = (y2 + y) * inputWidth + x2 + x;
							
							double val = ((float[]) input.getImageStack().getProcessor( slice ).getPixels())[pos];
							
							if (val > max)
							{								
								inputPix[ pos2 ] = val;
								labelPix[ pos2 ] = ((byte[]) label.getImageStack().getProcessor( slice ).getPixels())[ pos ];
							}							
						}
					pos2++;
				}
			
			isMaxPoolInput.addSlice( new FloatProcessor( maxPoolWidth, maxPoolHeight, inputPix));
			isMaxPoolLabel.addSlice( new ByteProcessor( maxPoolWidth, maxPoolHeight, labelPix, null ));
			
		}
		
		maxPool[ 0 ] = new ImagePlus("Input", isMaxPoolInput );
		maxPool[ 1 ] = new ImagePlus("Labels", isMaxPoolLabel );
		
		return maxPool;
	}
	
	public static ImagePlus[] maxPoolNoReduction( 
			ImagePlus input,
			ImagePlus label,
			int sizeX,
			int sizeY)
	{		
		final int width = input.getWidth();
		final int height = input.getHeight();
		
		ImageStack isMaxPoolInput = new ImageStack(width, height);
		ImageStack isMaxPoolLabel = new ImageStack(width, height);
		
		ImagePlus[] maxPool = new ImagePlus[ 2 ];
		
		for(int slice = 1; slice <= input.getImageStackSize(); slice ++)
		{
			IJ.log("Processing slice " + slice + "...");
			
			double[] inputPix = new double [ width * height ];
			byte[] labelPix = new byte [ width * height ];
				
			for(int y=0; y<height; y += sizeY)
				for(int x=0; x<width; x += sizeX)				
				{
					double max = 0;										
					double maxVal = 0;
					byte maxLabel = 0;
					
					for(int x2=0; x2<sizeX; x2++)
						for(int y2=0; y2<sizeY; y2++)
						{
							final int pos = (y2 + y) * width + x2 + x;
							
							double val = ((float[]) input.getImageStack().getProcessor( slice ).getPixels())[pos];
							
							if (val > max)
							{								
								maxVal = val;
								maxLabel = ((byte[]) label.getImageStack().getProcessor( slice ).getPixels())[ pos ];
							}							
						}
					
					for(int x2=0; x2<sizeX; x2++)
						for(int y2=0; y2<sizeY; y2++)
						{
							final int pos = (y2 + y) * width + x2 + x;
							inputPix [ pos ] = maxVal;
							labelPix [ pos ] = maxLabel;
						}
			
				}
			
			isMaxPoolInput.addSlice( new FloatProcessor( width, height, inputPix));
			isMaxPoolLabel.addSlice( new ByteProcessor( width, height, labelPix, null ));
			
		}
		
		maxPool[ 0 ] = new ImagePlus("Input", isMaxPoolInput );
		maxPool[ 1 ] = new ImagePlus("Labels", isMaxPoolLabel );
		
		return maxPool;
	}
	
}
