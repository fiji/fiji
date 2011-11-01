package trainableSegmentation.metrics;

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

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * This class implements the pixel error metric
 */
public class PixelError extends Metrics{

	
	/**
	 * Initialize pixel error metric
	 * @param originalLabels original labels (single 2D image or stack)
	 * @param proposedLabels proposed new labels (single 2D image or stack of the same as as the original labels)
	 */
	public PixelError(ImagePlus originalLabels, ImagePlus proposedLabels) 
	{
		super(originalLabels, proposedLabels);
	}
	

	/**
	 * Calculate the pixel error in 2D between some original labels 
	 * and the corresponding proposed labels. Both image are binarized.
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @return pixel error
	 */
	@Override
	public double getMetricValue(double binaryThreshold)
	{
		
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double pixelError = 0;

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<Double> > futures = new ArrayList< Future<Double> >();

		try{
			for(int i = 1; i <= labelSlices.getSize(); i++)
			{
				futures.add(exe.submit( getPixelErrorConcurrent(labelSlices.getProcessor(i).convertToFloat(),
											proposalSlices.getProcessor(i).convertToFloat(),										
											binaryThreshold ) ) );
			}

			// Wait for the jobs to be done
			for(Future<Double> f : futures)
			{
				pixelError += f.get();				

			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when warping ground truth in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return pixelError / labelSlices.getSize();
	}
		
	/**
	 * Get pixel error between two image in a concurrent way 
	 * (to be submitted to an Executor Service). Both images
	 * are binarized.
	 * 
	 * @param image1 first image
	 * @param image2 second image
	 * @param binaryThreshold threshold to apply to both images
	 * @return pixel error
	 */
	public Callable<Double> getPixelErrorConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<Double>()
		{
			public Double call()
			{
				double pixelError = 0;
				for(int x=0; x<image1.getWidth(); x++)
					for(int y=0; y<image1.getHeight(); y++)
					{
						double pix1 = image1.getPixelValue(x, y) > binaryThreshold ? 1 : 0;
						double pix2 = image2.getPixelValue(x, y) > binaryThreshold ? 1 : 0;
						pixelError +=  ( pix1 - pix2 ) * ( pix1 - pix2 ) ;
					}
				return pixelError / (image1.getWidth() * image1.getHeight());
			}
		};
	}

	
	/**
	 * Calculate the pixel error in 2D between some original labels 
	 * and the corresponding proposed labels (without thresholding).
	 *
	 * @return pixel error
	 */
	public double getMetricValue()
	{		
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double pixelError = 0;

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<Double> > futures = new ArrayList< Future<Double> >();

		try{
			for(int i = 1; i <= labelSlices.getSize(); i++)
			{
				futures.add(exe.submit( getPixelErrorConcurrent(labelSlices.getProcessor(i).convertToFloat(),
											proposalSlices.getProcessor(i).convertToFloat() ) ) );
			}

			// Wait for the jobs to be done
			for(Future<Double> f : futures)
			{
				pixelError += f.get();				

			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when warping ground truth in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}
		
		return pixelError / labelSlices.getSize();
	}
	
	/**
	 * Get pixel error between two image in a concurrent way 
	 * (to be submitted to an Executor Service). 
	 * 
	 * @param image1 first image
	 * @param image2 second image
	 * @return pixel error
	 */
	public Callable<Double> getPixelErrorConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2) 
	{
		return new Callable<Double>()
		{
			public Double call()
			{
				double pixelError = 0;			
				
				for(int x=0; x<image1.getWidth(); x++)
				{
					for(int y=0; y<image1.getHeight(); y++)
					{
						double pix1 = image1.getPixelValue(x, y);
						double pix2 = image2.getPixelValue(x, y);									
						pixelError +=  ( pix1 - pix2 ) * ( pix1 - pix2 ) ;
											}
				}
				return pixelError / (image1.getWidth() * image1.getHeight());
			}
		};
	}

	/**
	 * Calculate the precision-recall values based on pixel error between 
	 * some 2D original labels and the corresponding proposed labels. 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return pixel error value and derived statistics for each threshold
	 */
	public ArrayList< ClassificationStatistics > getPrecisionRecallStats(
			double minThreshold,
			double maxThreshold,
			double stepThreshold)
	{
		
		if( minThreshold < 0 || minThreshold >= maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return null;
		}
		
		ArrayList< ClassificationStatistics > cs = new ArrayList<ClassificationStatistics>();
				
		for(double th =  minThreshold; th <= maxThreshold; th += stepThreshold)
		{
			IJ.log("  Calculating pixel error statistics for threshold value " + String.format("%.2f", th) + "...");
			cs.add( getPrecisionRecallStats( th ));
		}		
		return cs;
	}
	
	/**
	 * Calculate the pixel error and its derived statistics in 2D between 
	 * some original labels and the corresponding proposed labels. Both images 
	 * are binarized. 
	 *  
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @return pixel error value and derived satatistics
	 */
	public ClassificationStatistics getPrecisionRecallStats( double binaryThreshold )
	{
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double randIndex = 0;
		double tp = 0;
		double tn = 0;
		double fp = 0;
		double fn = 0;

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<ClassificationStatistics> > futures = new ArrayList< Future<ClassificationStatistics> >();

		try{
			for(int i = 1; i <= labelSlices.getSize(); i++)
			{
				futures.add(exe.submit( getPrecisionRecallStatsConcurrent(labelSlices.getProcessor(i).convertToFloat(),
											proposalSlices.getProcessor(i).convertToFloat(),										
											binaryThreshold ) ) );
			}

			// Wait for the jobs to be done
			for(Future<ClassificationStatistics> f : futures)
			{
				ClassificationStatistics cs = f.get();
				randIndex += cs.metricValue;
				tp += cs.truePositives;
				tn += cs.trueNegatives;
				fp += cs.falsePositives;
				fn += cs.falseNegatives;
			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when calculating pixel error in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return new ClassificationStatistics( tp, tn, fp, fn, randIndex / labelSlices.getSize() );
	}
	
	/**
	 * Get pixel error value and derived statistics between two images 
	 * in a concurrent way (to be submitted to an Executor Service). 
	 * Both images are binarized.
	 * 
	 * @param image1 first image
	 * @param image2 second image
	 * @param binaryThreshold threshold to apply to both images
	 * @return pixel error value and derived statistics
	 */
	public  Callable<ClassificationStatistics> getPrecisionRecallStatsConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<ClassificationStatistics>()
		{
			public ClassificationStatistics call()
			{				
				return precisionRecallStats( image1, image2, binaryThreshold );
			}
		};
	}
	
	/**
	 * Calculate the pixel error and derived statistics between some 2D original labels 
	 * and the corresponding proposed labels. Both image are binarized.	 
	 * 
	 * @param label 2D image with the original labels
	 * @param proposal 2D image with the proposed labels
	 * @param binaryThreshold threshold value to binarize the input images
	 * @return rand index value and derived statistics
	 */
	public  ClassificationStatistics precisionRecallStats(
			ImageProcessor label,
			ImageProcessor proposal,
			double binaryThreshold)
	{
		// Binarize inputs
		float[] labelPix = (float[]) label.getPixels();
		float[] proposalPix = (float[]) proposal.getPixels();
		
		double truePositives = 0;
		double trueNegatives = 0;
		double falsePositives = 0;
		double falseNegatives = 0;
		double pixelError = 0;
		for(int i=0; i<labelPix.length; i++)
		{
			int pix1 = (labelPix[ i ] > binaryThreshold) ? 1 : 0;
			int pix2 = (proposalPix[ i ] > binaryThreshold) ? 1 : 0;
			
			if (pix1 == 1)
			{
				if(pix2 == 1)
					truePositives ++;
				else 
					falsePositives ++;
			}
			else
			{
				if(pix2 == 1)
					falseNegatives ++;
				else
					trueNegatives ++;						
			}
			
			pixelError +=  ( pix1 - pix2 ) * ( pix1 - pix2 ) ;
		}
		
		pixelError /= label.getWidth() * label.getHeight();
		
		return new ClassificationStatistics(truePositives, trueNegatives, falsePositives, falseNegatives, pixelError);		
	}
	
	/**
	 * Get the best F-score of the pixel error over a set of thresholds 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return maximal F-score of the pixel error
	 */
	public double getPixelErrorMaximalFScore(
			double minThreshold,
			double maxThreshold,
			double stepThreshold)
	{
		ArrayList<ClassificationStatistics> stats = getPrecisionRecallStats( minThreshold, maxThreshold, stepThreshold );
	    // trainableSegmentation.utils.Utils.plotPrecisionRecall( stats );    
	    double maxFScore = 0;

	    for(ClassificationStatistics stat : stats)
	    {
	    	if (stat.fScore > maxFScore)
	    		maxFScore = stat.fScore;
	    }	    
	    return maxFScore;
	}
	
}
