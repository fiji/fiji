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

}
