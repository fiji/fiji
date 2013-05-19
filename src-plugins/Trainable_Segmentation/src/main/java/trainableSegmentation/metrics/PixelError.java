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
public class PixelError extends Metrics
{

	/** boolean flag to set the level of detail on the standard output messages */
	private boolean verbose = true;
		
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
			double stepThreshold )
	{
		
		if( minThreshold < 0 || minThreshold >= maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return null;
		}
		
		ArrayList< ClassificationStatistics > cs = new ArrayList<ClassificationStatistics>();
				
		double bestFscore = 0;
		double bestTh = minThreshold;
		
		for(double th =  minThreshold; th <= maxThreshold; th += stepThreshold)
		{
			if( verbose ) 
				IJ.log("  Calculating pixel error statistics for threshold value " + String.format("%.3f", th) + "...");
			cs.add( getPrecisionRecallStats( th ));
			
			final double fScore = cs.get( cs.size()-1 ).fScore;
			if( fScore > bestFscore )
			{
				bestFscore = fScore;
				bestTh = th;
			}
			if( verbose )
				IJ.log("    F-score = " + fScore);
		}
		
		if( verbose )
			IJ.log(" ** Best F-score = " + bestFscore + ", with threshold = " + bestTh + " **\n");
		
		return cs;
	}
	
	/**
	 * Calculate the pixel error and its derived statistics in 2D between 
	 * some original labels and the corresponding proposed labels. Both images 
	 * are binarized. 
	 *  
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @return pixel error value and derived statistics
	 */
	public ClassificationStatistics getPrecisionRecallStats( double binaryThreshold )
	{
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double pixelError = 0;
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
				pixelError += cs.metricValue;
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

		return new ClassificationStatistics( tp, tn, fp, fn, pixelError / labelSlices.getSize() );
	}

	/**
	 * Calculate the pixel error and its derived statistics in 2D between 
	 * some original labels and the corresponding proposed labels. Both images 
	 * are binarized. 
	 *  
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @param mask mask image
	 * @return pixel error value and derived statistics
	 */
	public ClassificationStatistics getPrecisionRecallStats( 
			double binaryThreshold, 
			ImagePlus mask )
	{
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double pixelError = 0;
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
											( null != mask ) ? mask.getImageStack().getProcessor(i).convertToFloat() : null,										
											binaryThreshold ) ) );
			}

			// Wait for the jobs to be done
			for(Future<ClassificationStatistics> f : futures)
			{
				ClassificationStatistics cs = f.get();
				pixelError += cs.metricValue;
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

		return new ClassificationStatistics( tp, tn, fp, fn, pixelError / labelSlices.getSize() );
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
	 * Get pixel error value and derived statistics between two images 
	 * in a concurrent way (to be submitted to an Executor Service). 
	 * Both images are binarized.
	 * 
	 * @param image1 first image
	 * @param image2 second image
	 * @param mask mask image
	 * @param binaryThreshold threshold to apply to both images
	 * @return pixel error value and derived statistics
	 */
	public  Callable<ClassificationStatistics> getPrecisionRecallStatsConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final ImageProcessor mask,
			final double binaryThreshold) 
	{
		return new Callable<ClassificationStatistics>()
		{
			public ClassificationStatistics call()
			{
				if(null == mask)
					return precisionRecallStats( image1, image2, binaryThreshold );
				else
					return precisionRecallStats( image1, image2, mask, binaryThreshold );
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
			// make sure labels are binary
			int pix1 = (labelPix[ i ] > 0) ? 1 : 0;
			// threshold proposal
			int pix2 = (proposalPix[ i ] > binaryThreshold) ? 1 : 0;
			
			if (pix2 == 1)
			{
				if(pix1 == 1)
					truePositives ++;
				else 
					falsePositives ++;
			}
			else
			{
				if(pix1 == 1)
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
	 * Calculate the pixel error and derived statistics between some 2D original labels 
	 * and the corresponding proposed labels. Both image are binarized.	 
	 * 
	 * @param label 2D image with the original labels
	 * @param proposal 2D image with the proposed labels
	 * @param mask 2D image representing the binary mask
	 * @param binaryThreshold threshold value to binarize the input images
	 * @return classification statistics
	 */
	public  ClassificationStatistics precisionRecallStats(
			ImageProcessor label,
			ImageProcessor proposal,
			ImageProcessor mask,
			double binaryThreshold)
	{
		// Binarize inputs
		float[] labelPix = (float[]) label.getPixels();
		float[] proposalPix = (float[]) proposal.getPixels();
		float[] maskPixels = (float[]) mask.getPixels();
		
		double truePositives = 0;
		double trueNegatives = 0;
		double falsePositives = 0;
		double falseNegatives = 0;
		double pixelError = 0;
				
		double n = 0;
		
		for(int i=0; i<labelPix.length; i++)
		{
			// make sure labels are binary
			int pix1 = (labelPix[ i ] > 0) ? 1 : 0;
			// threshold proposal
			int pix2 = (proposalPix[ i ] > binaryThreshold) ? 1 : 0;
			
			// check mask
			if ( maskPixels[ i ] > 0 )															
			{				
				if (pix2 == 1)
				{
					if(pix1 == 1)
						truePositives ++;
					else 
						falsePositives ++;
				}
				else
				{
					if(pix1 == 1)
						falseNegatives ++;
					else
						trueNegatives ++;						
				}

				pixelError +=  ( pix1 - pix2 ) * ( pix1 - pix2 ) ;

				n++;
			}
		}
		
		if ( n > 0 )
			pixelError /= n;
		
		return new ClassificationStatistics(truePositives, trueNegatives, falsePositives, falseNegatives, pixelError);		
	}
	
	
	/**
	 * Get the best F-score of the pixel error over a set of thresholds 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @param verbose flag to print or not output information
	 * @return maximal F-score of the pixel error
	 */
	public double getPixelErrorMaximalFScore(
			double minThreshold,
			double maxThreshold,
			double stepThreshold )
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

	
    /**
     * Main method for calculate the pixel error metrics from the command line
     *
     * @param args arguments to decide the action
     */
    public static void main(String args[]) 
    {
       if (args.length<1) 
       {
          dumpSyntax();
          System.exit(1);
       } 
       else 
       {
          if( args[0].equals("-help") )                 
        	  dumpSyntax();  
          else if (args[0].equals("-maxFScore"))
        	  System.out.println( maximalFScoreCommandLine(args) );
          else 
        	  dumpSyntax();
       }
       System.exit(0);
    }
    
    /**
     * Calculate the maximal F-score of pixel similarity based on the
     * parameters introduced by command line
     * 
     * @param args command line arguments
     * @return maximal F-score
     */
    static double maximalFScoreCommandLine(String[] args) 
    {
    	if (args.length != 6)
        {
            dumpSyntax();
            return -1;
        }
    	
    	final ImagePlus label = new ImagePlus( args[ 1 ] );
    	final ImagePlus proposal = new ImagePlus( args[ 2 ] );
    	final double minThreshold = Double.parseDouble( args[ 3 ] );
		final double maxThreshold = Double.parseDouble( args[ 4 ] );
		final double stepThreshold = Double.parseDouble( args[ 5 ] );
    	
		PixelError pe = new PixelError(label, proposal);
		pe.setVerboseMode( false );
		return pe.getPixelErrorMaximalFScore(minThreshold, maxThreshold, stepThreshold );
	}


    /**
     * Method to write the syntax of the program in the command line.
     */
    private static void dumpSyntax () 
    {
       System.out.println("Purpose: calculate pixel error between proposed and original labels.\n");     
       System.out.println("Usage: PixelError ");
       System.out.println("  -help                      : show this message");
       System.out.println("");
       System.out.println("  -maxFScore                 : calculate the best F-score of the pixel error over a set of thresholds");
       System.out.println("          labels             : image with the original labels");
       System.out.println("          proposal           : image with the proposed labels");
       System.out.println("          minThreshold       : minimum threshold value to binarize the proposal");
       System.out.println("          maxThreshold       : maximum threshold value to binarize the proposal");
       System.out.println("          stepThreshold      : threshold step value to use during binarization\n");
       System.out.println("Examples:");
       System.out.println("Calculate the maximal F-score of pixel similarity between proposed and original labels over a set of");
       System.out.println("thresholds (from 0.0 to 1.0 in steps of 0.1):");
       System.out.println("   PixelError -maxFScore original-labels.tif proposed-labels.tif 0.0 1.0 0.1");

    } 
    
    /**
     * Set verbose mode
     * @param verbose true to display more information in the standard output
     */
    public void setVerboseMode(boolean verbose) 
    {		
    	this.verbose = verbose;
	}
	
}
