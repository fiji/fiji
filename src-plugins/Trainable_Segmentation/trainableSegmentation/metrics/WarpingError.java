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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.vecmath.Point3f;

import trainableSegmentation.utils.Utils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


/**
 * This class implements the warping error metric \cite{Jain10} 
 * 
 * BibTeX:
 * <pre>
 * &#64;article{Jain10,
 *   author    = {V. Jain, B. Bollmann, M. Richardson, D.R. Berger, M.N. Helmstaedter, 
 *   				K.L. Briggman, W. Denk, J.B. Bowden, J.M. Mendenhall, W.C. Abraham, 
 *   				K.M. Harris, N. Kasthuri, K.J. Hayworth, R. Schalek, J.C. Tapia, 
 *   				J.W. Lichtman, S.H. Seung},
 *   title     = {Boundary Learning by Optimization with Topological Constraints},
 *   booktitle = {2010 IEEE CONFERENCE ON COMPUTER VISION AND PATTERN RECOGNITION (CVPR)},
 *   year      = {2010},
 *   series    = {IEEE Conference on Computer Vision and Pattern Recognition},
 *   pages     = {2488-2495},
 *   doi       = {10.1109/CVPR.2010.5539950)
 * }
 * </pre>   
 */
public class WarpingError extends Metrics {

	/** simple point threshold value */
	public static final double SIMPLE_POINT_THRESHOLD = 0;
	/** merger flag */
	public static final int MERGE 			= 0x1;
	/** split flag */
	public static final int SPLIT 			= 0x2;
	/** hole addition error flag */
	public static final int HOLE_ADDITION	= 0x4;
	/** object deletion error flag */
	public static final int OBJECT_DELETION = 0x8;
	/** object addition error flag */
	public static final int OBJECT_ADDITION = 0x10;
	/** hole deletion error flag */
	public static final int HOLE_DELETION 	= 0x20;
	/** default flags */
	public static final int DEFAULT_FLAGS = MERGE + SPLIT + HOLE_ADDITION + OBJECT_DELETION + OBJECT_ADDITION + HOLE_DELETION;
	
	/** image mask containing in white the areas where warping is allowed (null for not geometric constraints) */
	ImagePlus mask = null;
	/** flags to select which error should be taken into account and which not */
	int flags = DEFAULT_FLAGS;
	
	/** boolean flag to set the level of detail on the standard output messages */
	private boolean verbose = true;
	
	/**
	 * Initialize warping error metric
	 * @param originalLabels original labels (single 2D image or stack)
	 * @param proposedLabels proposed new labels (single 2D image or stack of the same as as the original labels)
	 */
	public WarpingError(
			ImagePlus originalLabels, 
			ImagePlus proposedLabels) 
	{
		super(originalLabels, proposedLabels);
	}

	/**
	 * Initialize warping error metric
	 * @param originalLabels original labels (single 2D image or stack)
	 * @param proposedLabels proposed new labels (single 2D image or stack of the same as as the original labels)
	 * @param mask image mask containing in white the areas where warping is allowed (null for not geometric constraints)
	 */
	public WarpingError(
			ImagePlus originalLabels, 
			ImagePlus proposedLabels,
			ImagePlus mask) 
	{
		super(originalLabels, proposedLabels);
		this.mask = mask;
	}
	
	/**
	 * Initialize warping error metric
	 * @param originalLabels original labels (single 2D image or stack)
	 * @param proposedLabels proposed new labels (single 2D image or stack of the same as as the original labels)
	 * @param mask image mask containing in white the areas where warping is allowed (null for not geometric constraints)
	 * @param flags flags to select which error should be taken into account and which not
	 */
	public WarpingError(
			ImagePlus originalLabels, 
			ImagePlus proposedLabels,
			ImagePlus mask,
			int flags) 
	{
		super(originalLabels, proposedLabels);
		this.mask = mask;
		this.flags = flags;
	}
	
	
	/**
	 * Calculate the classic topology-preserving warping error \cite{Jain10} 
	 * in 2D between some original labels and the corresponding proposed labels. 
	 * Both, original and proposed labels are expected to have float values 
	 * between 0 and 1. Otherwise, they will be converted.
	 * 
	 * BibTeX:
	 * <pre>
	 * &#64;article{Jain10,
	 *   author    = {V. Jain, B. Bollmann, M. Richardson, D.R. Berger, M.N. Helmstaedter, 
	 *   				K.L. Briggman, W. Denk, J.B. Bowden, J.M. Mendenhall, W.C. Abraham, 
	 *   				K.M. Harris, N. Kasthuri, K.J. Hayworth, R. Schalek, J.C. Tapia, 
	 *   				J.W. Lichtman, S.H. Seung},
	 *   title     = {Boundary Learning by Optimization with Topological Constraints},
	 *   booktitle = {2010 IEEE CONFERENCE ON COMPUTER VISION AND PATTERN RECOGNITION (CVPR)},
	 *   year      = {2010},
	 *   series    = {IEEE Conference on Computer Vision and Pattern Recognition},
	 *   pages     = {2488-2495},
	 *   doi       = {10.1109/CVPR.2010.5539950)
	 * }
	 * </pre>
	 *
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @return total warping error (it counts all type of mismatches as errors)
	 */	
	@Override
	public double getMetricValue(double binaryThreshold) 	
	{		
		if( verbose )
			IJ.log("  Warping ground truth...");
		
		// Warp ground truth, relax original labels to proposal. Only simple
		// points warping is allowed.
		WarpingResults[] wrs = simplePointWarp2dMT(super.originalLabels, super.proposedLabels, mask, binaryThreshold);		

		if(null == wrs)
			return -1;

		double error = 0;

		for(int j=0; j<wrs.length; j++)			
			error += wrs[ j ].warpingError;
		
		if(wrs.length != 0)
			return error / wrs.length;
		else
			return -1;
	}
	
	
	/**
	 * Calculate the topology-preserving warping error in 2D between some
	 * original labels and the corresponding proposed labels. Pixels belonging 
	 * to the same mistake will be only counted once. For example, if we have 
	 * a line of 15 pixels that prevent from a merger, it will count as 1 instead
	 * of 15 as in the classic warping error method. 
	 * Both, original and proposed labels are expected to have float values between 
	 * 0 and 1. Otherwise, they will be converted.
	 *	
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @param clusterByError if false, cluster mismatches by type, otherwise cluster them by error and type
	 * @return clustered warping error (it clusters the mismatches that belong to the same type and/or error together)
	 */
	public double getMetricValue(			
			double binaryThreshold,
			boolean clusterByError)
	{
		
		if( verbose )
			IJ.log("  Warping ground truth...");
		
		// Get clustered mismatches after warping ground truth, i.e. relaxing original labels to proposal. 
		// Only simple points warping is allowed.
		ClusteredWarpingMismatches[] cwm = getClusteredWarpingMismatches(originalLabels, proposedLabels, 
																		mask, binaryThreshold, clusterByError, -1);		
		if(null == cwm)
			return -1;

		double error = 0;
		double count = originalLabels.getWidth() * originalLabels.getHeight() * originalLabels.getImageStackSize();
		
		if( (flags & HOLE_ADDITION) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfHoleAdditions;
		if( (flags & HOLE_DELETION) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfHoleDeletions;
		if( (flags & MERGE) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfMergers; 
		if( (flags & OBJECT_ADDITION) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfObjectAdditions;
		if( (flags & OBJECT_DELETION) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfObjectDeletions; 
		if( (flags & SPLIT) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfSplits;

		if(count != 0)
			return error / count;
		else
			return -1;
	}

	
	/**
	 * Calculate the topology-preserving warping error in 2D between some
	 * original labels and the corresponding proposed labels. Pixels belonging 
	 * to the same mistake will be only counted once. For example, if we have 
	 * a line of 15 pixels that prevent from a merger, it will count as 1 instead
	 * of 15 as in the classic warping error method. 
	 * Both, original and proposed labels are expected to have float values between 
	 * 0 and 1. Otherwise, they will be converted.
	 *	
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @param clusterByError if false, cluster mismatches by type, otherwise cluster them by error and type
	 * @param radius radius in pixels to use when classifying mismatches
	 * @return clustered warping error (it clusters the mismatches that belong to the same type and/or error together)
	 */
	public double getMetricValue(			
			double binaryThreshold,
			boolean clusterByError,
			int radius)
	{
		
		if( verbose )
			IJ.log("  Warping ground truth...");
		
		// Get clustered mismatches after warping ground truth, i.e. relaxing original labels to proposal. 
		// Only simple points warping is allowed.
		ClusteredWarpingMismatches[] cwm = getClusteredWarpingMismatches(originalLabels, proposedLabels, 
																		mask, binaryThreshold, clusterByError, radius);		
		if(null == cwm)
			return -1;

		double error = 0;
		double count = originalLabels.getWidth() * originalLabels.getHeight() * originalLabels.getImageStackSize();
		
		if( (flags & HOLE_ADDITION) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfHoleAdditions;
		if( (flags & HOLE_DELETION) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfHoleDeletions;
		if( (flags & MERGE) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfMergers; 
		if( (flags & OBJECT_ADDITION) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfObjectAdditions;
		if( (flags & OBJECT_DELETION) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfObjectDeletions; 
		if( (flags & SPLIT) != 0)
			for(int j=0; j<cwm.length; j++)					
				error += cwm[ j ].numOfSplits;

		if(count != 0)
			return error / count;
		else
			return -1;
	}

	/**
	 * Calculate the number of splits and mergers for different thresholds
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @param clusterByError if false, cluster mismatches by type, otherwise cluster them by error and type
	 * @return list with arrays with the number of splits and mergers
	 */
	public ArrayList<int[]> getSplitsAndMergers(
			double minThreshold,
			double maxThreshold,
			double stepThreshold,
			boolean clusterByError)
	{
		
		if( minThreshold < 0 || minThreshold >= maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return null;
		}
						
		ArrayList < int[] > listOfSplitsAndMergers = new ArrayList<int[]>();
				
		for(double th = minThreshold; th<=maxThreshold; th += stepThreshold)
		{						
			if( verbose )
				IJ.log("  Calculating splits and mergers for threshold value " + String.format("%.3f", th) + "...");
			ClusteredWarpingMismatches[] cwm = 
						getClusteredWarpingMismatches(originalLabels, proposedLabels, 
														mask, th, clusterByError, -1);		
			if(null == cwm)
				return null;
			
			int[] splitsAndMergers = new int[2];
			
			for(int j=0; j<cwm.length; j++)
			{
				splitsAndMergers[ 0 ] += cwm[ j ].numOfSplits;
				splitsAndMergers[ 1 ] += cwm[ j ].numOfMergers;
			}
			
			listOfSplitsAndMergers.add( splitsAndMergers );
			
			if( verbose )
				IJ.log( "  # splits = " + splitsAndMergers[ 0 ] + ", # mergers = " + splitsAndMergers[ 1 ]);
		}
						
		return listOfSplitsAndMergers;
	}
	
	/**
	 * Calculate the number of splits and mergers for different thresholds
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @param clusterByError if false, cluster mismatches by type, otherwise cluster them by error and type
	 * @param radius radius in pixel to use when classifying mismatches
	 * @return list with arrays with the number of splits and mergers
	 */
	public ArrayList<int[]> getSplitsAndMergers(
			double minThreshold,
			double maxThreshold,
			double stepThreshold,
			boolean clusterByError,
			int radius )
	{
		
		if( minThreshold < 0 || minThreshold >= maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return null;
		}
						
		ArrayList < int[] > listOfSplitsAndMergers = new ArrayList<int[]>();
				
		for(double th = minThreshold; th<=maxThreshold; th += stepThreshold)
		{						
			if( verbose )
				IJ.log("  Calculating splits and mergers for threshold value " + String.format("%.3f", th) + "...");
			ClusteredWarpingMismatches[] cwm = 
						getClusteredWarpingMismatches(originalLabels, proposedLabels, 
														mask, th, clusterByError, radius );		
			if(null == cwm)
				return null;
			
			int[] splitsAndMergers = new int[2];
			
			for(int j=0; j<cwm.length; j++)
			{
				splitsAndMergers[ 0 ] += cwm[ j ].numOfSplits;
				splitsAndMergers[ 1 ] += cwm[ j ].numOfMergers;
			}
			
			listOfSplitsAndMergers.add( splitsAndMergers );
			
			if( verbose )
				IJ.log( "  # splits = " + splitsAndMergers[ 0 ] + ", # mergers = " + splitsAndMergers[ 1 ]);
		}
						
		return listOfSplitsAndMergers;
	}
	
	/**
	 * Calculate error with the minimum number of splits and mergers for different thresholds
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @param clusterByError if false, cluster mismatches by type, otherwise cluster them by error and type
	 * @return list with arrays with the number of splits and mergers
	 */
	public double getMinimumSplitsAndMergersErrorValue(
			double minThreshold,
			double maxThreshold,
			double stepThreshold,
			boolean clusterByError)
	{
		
		if( minThreshold < 0 || minThreshold >= maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return -1;
		}
						
		flags = MERGE + SPLIT;
		
		double minError = Double.MAX_VALUE;
		double bestTh = minThreshold;
		
		for(double th = minThreshold; th<=maxThreshold; th += stepThreshold)
		{						
			if( verbose )
				IJ.log("  Calculating splits and mergers for threshold value " + String.format("%.3f", th) + "...");
			double error = getMetricValue( th, clusterByError );
			if ( verbose )
				IJ.log("    error = " + error);
			if ( error < minError)
			{
				minError = error;
				bestTh = th;
			}
		}
		
		if (verbose)
			IJ.log(" **  Minimum error = " + minError + ", with threshold = " + bestTh + " **\n");
						
		return minError;
	}
		
	/**
	 * Calculate error with the minimum number of splits and mergers for different thresholds
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @param clusterByError if false, cluster mismatches by type, otherwise cluster them by error and type
	 * @param radius radius in pixel to use when classifying mismatches
	 * @return list with arrays with the number of splits and mergers
	 */
	public double getMinimumSplitsAndMergersErrorValue(
			double minThreshold,
			double maxThreshold,
			double stepThreshold,
			boolean clusterByError,
			int radius )
	{
		
		if( minThreshold < 0 || minThreshold >= maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return -1;
		}
						
		flags = MERGE + SPLIT;
		
		double minError = Double.MAX_VALUE;
		double bestTh = minThreshold;
				
		for(double th = minThreshold; th<=maxThreshold; th += stepThreshold)
		{						
			if ( verbose )
				IJ.log("  Calculating splits and mergers for threshold value " + String.format("%.3f", th) + "...");
			double error = getMetricValue( th, clusterByError, radius );
			if ( verbose )
				IJ.log("    error = " + error);
			if ( error < minError)
			{
				minError = error;
				bestTh = th;
			}
		}
		
		if (verbose)
			IJ.log(" **  Minimum error = " + minError + ", with threshold = " + bestTh + " **\n");
						
		return minError;
	}
	
	/**
	 * Get the best F-score of the pixel error between proposed and original labels
	 * over a set of thresholds 
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
	    double th = 0;
	    double bestTh = 0;
	    
	    for(ClassificationStatistics stat : stats)
	    {
	    	if (stat.fScore > maxFScore)
	    	{
	    		maxFScore = stat.fScore;
	    		bestTh = th;  
	    	}
	    	th += stepThreshold;
	    }	 
	    
	    if( verbose )
			IJ.log(" ** Best F-score = " + maxFScore + ", with threshold = " + bestTh + " **\n");
	    
	    return maxFScore;
	}
	
	/**
	 * Calculate the precision-recall values based on pixel error between 
	 * some warped 2D original labels and the corresponding proposed labels. 
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
			if( verbose )
				IJ.log("  Calculating warping error statistics for threshold value " + String.format("%.3f", th) + "...");
			
			WarpingResults[] wrs = simplePointWarp2dMT(originalLabels, proposedLabels, mask, th);
			
			ImageStack is = new ImageStack( originalLabels.getWidth(), originalLabels.getHeight() );
			for(int i = 0; i < wrs.length; i ++)
				is.addSlice("warped source slice " + (i+1), wrs[i].warpedSource.getProcessor() );
			
			ImagePlus warpedSource = new ImagePlus ("warped source", is);
									
			// We calculate the precision-recall value between the warped original labels and the 
			// proposed labels 
			PixelError pixelError = new PixelError( warpedSource, proposedLabels);			
			ClassificationStatistics stats = pixelError.getPrecisionRecallStats( th );
			if( verbose )
				IJ.log("   F-score = " + stats.fScore );
			cs.add( stats );
		}		
		return cs;
	}
	
	/**
	 * Get the best F-score of the pixel error between proposed and original labels
	 * (and all the way around) over a set of thresholds 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @param verbose flag to print or not output information
	 * @return maximal F-score of the pixel error
	 */
	public double getDualPixelErrorMaximalFScore(
			double minThreshold,
			double maxThreshold,
			double stepThreshold )
	{
		ArrayList<ClassificationStatistics> stats = getDualPrecisionRecallStats( minThreshold, maxThreshold, stepThreshold );
	    // trainableSegmentation.utils.Utils.plotPrecisionRecall( stats );    
	    double maxFScore = 0;
	    double th = 0;
	    double bestTh = 0;
	    
	    for(ClassificationStatistics stat : stats)
	    {
	    	if (stat.fScore > maxFScore)
	    	{
	    		maxFScore = stat.fScore;
	    		bestTh = th;  
	    	}
	    	th += stepThreshold;
	    }	 
	    
	    if( verbose )
			IJ.log(" ** Best F-score = " + maxFScore + ", with threshold = " + bestTh + " **\n");
	    
	    return maxFScore;
	}
	
	/**
	 * Calculate the precision-recall values based on pixel error between 
	 * some warped 2D original labels and the corresponding proposed labels
	 * in both directions (from original labels to proposal and reversely). 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return pixel error value and derived statistics for each threshold
	 */
	public ArrayList< ClassificationStatistics > getDualPrecisionRecallStats(
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
			if( verbose )
				IJ.log("  Calculating warping error statistics for threshold value " + String.format("%.3f", th) + "...");
			
			WarpingResults[] wrs = simplePointWarp2dMT(originalLabels, proposedLabels, mask, th);
			
			ImageStack is = new ImageStack( originalLabels.getWidth(), originalLabels.getHeight() );
			for(int i = 0; i < wrs.length; i ++)
				is.addSlice("warped source slice " + (i+1), wrs[i].warpedSource.getProcessor() );
			
			ImagePlus warpedSource = new ImagePlus ("warped source", is);
									
			// We calculate first the precision-recall value between the warped 
			// original labels and the proposed labels 
			PixelError pixelError = new PixelError( warpedSource, proposedLabels );			
			ClassificationStatistics stats = pixelError.getPrecisionRecallStats( th );
			
			// ... and then from warped proposed labels to original labels 
			
			// apply threshold to proposed labels so they are binary
			double max = proposedLabels.getImageStack().getProcessor( 1 ) instanceof ByteProcessor ? 255 : 1.0;
			ImagePlus proposal8bit = proposedLabels.duplicate();	
			IJ.setThreshold( proposal8bit, th + 0.00001, max);
			IJ.run( proposal8bit, "Convert to Mask", "  black");
			
			// warp proposal into original labels
			wrs = simplePointWarp2dMT( proposal8bit, originalLabels, mask, th);
			
			is = new ImageStack( originalLabels.getWidth(), originalLabels.getHeight() );
			for(int i = 0; i < wrs.length; i ++)
				is.addSlice("warped source slice " + (i+1), wrs[i].warpedSource.getProcessor() );
			
			warpedSource = new ImagePlus ("warped source", is);
									
			// then calculate pixel error
			pixelError = new PixelError( warpedSource, originalLabels );			
			
			
			ClassificationStatistics statsInverse = pixelError.getPrecisionRecallStats( th );
			
			// Join statistics and average errors		
			stats.metricValue = (stats.metricValue + statsInverse.metricValue) / 2.0;
			
			ClassificationStatistics finalStats = new ClassificationStatistics(
					stats.truePositives + statsInverse.truePositives,
					stats.trueNegatives + statsInverse.trueNegatives, 
					stats.falsePositives + statsInverse.falsePositives,
					stats.falseNegatives + statsInverse.falseNegatives,
					(stats.metricValue + statsInverse.metricValue) / 2.0); 
			
			if( verbose )
				IJ.log("   F-score = " + finalStats.fScore );						
			cs.add( finalStats );
		}		
		return cs;
	}
	
	
	/**
	 * Calculate the precision-recall values based on Rand index between 
	 * some warped 2D original labels and the corresponding proposed labels. 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return Rand index value and derived statistics for each threshold
	 */
	public ArrayList< ClassificationStatistics > getRandIndexStats(
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
			if( verbose )
				IJ.log("  Calculating warping error statistics for threshold value " + String.format("%.3f", th) + "...");
			
			WarpingResults[] wrs = simplePointWarp2dMT(originalLabels, proposedLabels, mask, th);
			
			ImageStack is = new ImageStack( originalLabels.getWidth(), originalLabels.getHeight() );
			for(int i = 0; i < wrs.length; i ++)
				is.addSlice("warped source slice " + (i+1), wrs[i].warpedSource.getProcessor() );
			
			ImagePlus warpedSource = new ImagePlus ("warped source", is);						
			
			// We calculate the precision-recall value between the warped original labels and the 
			// proposed labels 
			RandError randError = new RandError( warpedSource, proposedLabels );			
			ClassificationStatistics stats = randError.getRandIndexStats( th );
			if( verbose )
				IJ.log("   F-score = " + stats.fScore );			
			cs.add( stats );
		}		
		return cs;
	}
	
	/**
	 * Get the best F-score of the Rand index based on Rand index between 
	 * some warped 2D original labels and the corresponding proposed labels. 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return maximal F-score of the Rand index
	 */
	public double getRandIndexMaximalFScore(
			double minThreshold,
			double maxThreshold,
			double stepThreshold)
	{
		ArrayList<ClassificationStatistics> stats = getRandIndexStats( minThreshold, maxThreshold, stepThreshold );
	    // trainableSegmentation.utils.Utils.plotPrecisionRecall( stats );    
	    
	    double maxFScore = 0;
	    double th = 0;
	    double bestTh = 0;
	    
	    for(ClassificationStatistics stat : stats)
	    {
	    	if (stat.fScore > maxFScore)
	    	{
	    		maxFScore = stat.fScore;
	    		bestTh = th;  
	    	}
	    	th += stepThreshold;
	    }	 
	    
	    if( verbose )
			IJ.log(" ** Best F-score = " + maxFScore + ", with threshold = " + bestTh + " **\n");
	    
	    return maxFScore;
	}

	/**
	 * Check if a point is simple (in 2D) based on 3D code from Mark Richardson
	 * inspired in the work of Bertrand et al. \cite{Bertrand94} 
	 * 
	 * BibTeX:
	 * <pre>
	 * &#64;article{Bertrand94,
	 *   author    = {Bertrand, Gilles and Malandain, Gr\'{e}goire},
	 *   title     = {A new characterization of three-dimensional simple points},
	 *   journal   = {Pattern Recogn. Lett.},
	 *   volume    = {15},
	 *   issue     = {2},
	 *   month     = {February},
	 *   year      = {1994},
	 *   issn      = {0167-8655},
	 *   pages     = {169--175},
	 *   numpages  = {7},
	 *   url       = {http://dl.acm.org/citation.cfm?id=179348.179356},
	 *   doi       = {10.1016/0167-8655(94)90046-9},
	 *   acmid     = {179356},
	 *   publisher = {Elsevier Science Inc.},
	 *   address   = {New York, NY, USA},
	 *   keywords  = {digital topology, simple points, thinning algorithms, three dimensions},
	 * }
	 * </pre>  
	 * @param im input patch
	 * @param n neighbors
	 * @return true if the center pixel of the patch is a simple point
	 */
	public boolean simple2DBertrand(ImagePlus im, int n)
	{
		
		float[] input = new float[27];
		
		float[] center = (float[])im.getProcessor().getPixels();
		
		for(int i=0; i<9; i++)
			input[i+9] = center[i];
		
		switch (n)
		{
			case 4:
				return simple3d( input, 6);
			case 8:
				return simple3d(input, 26);
			default:
				IJ.error("Non valid adjacency value");
				return false;
		}
	}
	
	
	/**
	 * Check if a point is simple (in 2D)
	 * @param im input patch
	 * @param n neighbors
	 * @return true if the center pixel of the patch is a simple point
	 */
	public boolean simple2D(ImagePlus im, int n)
	{
		final ImagePlus invertedIm = new ImagePlus("inverted", im.getProcessor().duplicate());
		//IJ.run(invertedIm, "Invert","");
		final float[] pix = (float[])invertedIm.getProcessor().getPixels();
		for(int i=0; i<pix.length; i++)
			pix[i] = pix[i] == 0f ? 1f : 0f;

		switch (n)
		{
			case 4:
				if ( topo(im,4)==1 && topo(invertedIm, 8)==1 )
	            	return true;
				else
					return false;
			case 8:
				if ( topo(im,8)==1 && topo(invertedIm, 4)==1 )
					return true;
				else
					return false;
			default:
				IJ.error("Non valid adjacency value");
				return false;
		}
	}
	
	

	/**
	 * Computes topological numbers for the central point of an image patch.
	 * These numbers can be used as the basis of a topological classification.
	 * T_4 and T_8 are used when IM is a 2d image patch of size 3x3
	 * defined on p. 172 of Bertrand & Malandain, Patt. Recog. Lett. 15, 169-75 (1994).
	 *
	 * @param im input image
	 * @param adjacency number of neighbors
	 * @return number of components in the patch excluding the center pixel
	 */
	public int topo(final ImagePlus im, final int adjacency)
	{
		ImageProcessor components = null;
		final ImagePlus im2 = new ImagePlus("copy of im", im.getProcessor().duplicate());
		
		switch (adjacency)
		{
			case 4:
				if( im.getStack().getSize() > 1 )
				{
					IJ.error("n=4 is valid for a 2d image");
					return -1;
				}
				if( im.getProcessor().getWidth() > 3 || im.getProcessor().getHeight() > 3)
				{
					IJ.error("must be 3x3 image patch");
					return -1;
				}
				// ignore the central point
				im2.getProcessor().set(1, 1, 0);
				components = Utils.connectedComponents(im2, adjacency).allRegions.getProcessor();
				
				// zero out locations that are not in the four-neighborhood
				components.set(0,0,0);
				components.set(0,2,0);
				components.set(1,1,0);
				components.set(2,0,0);
				components.set(2,2,0);
				break;
			case 8:
				if( im.getStack().getSize() > 1 )
				{
					IJ.error("n=8 is valid for a 2d image");
					return -1;
				}
				if( im.getProcessor().getWidth() > 3 || im.getProcessor().getHeight() > 3)
				{
					IJ.error("must be 3x3 image patch");
					return -1;
				}
				// ignore the central point
				im2.getProcessor().set(1, 1, 0);
				components = Utils.connectedComponents(im2, adjacency).allRegions.getProcessor();
				break;
			default:
				IJ.error("Non valid adjacency value");
				return -1;
		}

		if(null == components)
			return -1;

		
		int t = 0;
		ArrayList<Integer> uniqueId = new ArrayList<Integer>();
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 3; j++)
			{
				if(( t = components.get(i, j) ) != 0)
					if(!uniqueId.contains(t))
						uniqueId.add(t);
			}

		return uniqueId.size();				
	}

	
	/**
	 * Use simple point relaxation to warp 2D source into 2D target.
	 * Source is only modified at nonzero locations in the mask
	 *
	 * @param source input 2D image to be relaxed
	 * @param target target 2D image
	 * @param mask 2D image mask
	 * @param binaryThreshold binarization threshold
	 * @return warped source image and warping error
	 */
	public WarpingResults simplePointWarp2d(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			double binaryThreshold)
	{
		if(binaryThreshold < 0 || binaryThreshold > 1.01)
			binaryThreshold = 0.5;

		// Grayscale target
		final ImagePlus targetReal;// = new ImagePlus("target_real", target.duplicate());
		// Binarized target
		final ImagePlus targetBin; // = new ImagePlus("target_aux", target.duplicate());

		final ImagePlus sourceReal; // = new ImagePlus("source_real", source.duplicate());

		final ImagePlus maskReal; // = (null != mask) ? new ImagePlus("mask_real", mask.duplicate().convertToFloat()) : null;

		final int width = target.getWidth();
		final int height = target.getHeight();

		// Resize canvas to avoid checking the borders
		ImageProcessor ip = target.createProcessor(width+2, height+2);
		ip.insert(target, 1, 1);
		targetReal = new ImagePlus("target_real", ip.duplicate());

		targetBin = new ImagePlus("target_aux", ip.duplicate());

		ip = target.createProcessor(width+2, height+2);
		ip.insert(source, 1, 1);
		sourceReal = new ImagePlus("source_real", ip.duplicate());

		if(null != mask)
		{
			ip = target.createProcessor(width+2, height+2);
			ip.insert(mask, 1, 1);
			maskReal = new ImagePlus("mask_real", ip.duplicate());
		}
		else{
			maskReal = null;
		}

		// make sure source and target are binary images
		final float[] sourceRealPix = (float[])sourceReal.getProcessor().getPixels();
		for(int i=0; i < sourceRealPix.length; i++)
			if(sourceRealPix[i] > 0)
				sourceRealPix[i] = 1.0f;

		final float[] targetBinPix = (float[])targetBin.getProcessor().getPixels();
		for(int i=0; i < targetBinPix.length; i++)
			targetBinPix[i] = (targetBinPix[i] <= binaryThreshold) ? 0.0f : 1.0f;
		
		double diff = Double.MIN_VALUE;
		double diff_before = 0;

		final WarpingResults result = new WarpingResults();

		while(true)
		{
			ImageProcessor missclass_points_image = sourceReal.getProcessor().duplicate();
			missclass_points_image.copyBits(targetBin.getProcessor(), 0, 0, Blitter.DIFFERENCE);

			diff_before = diff;

			// Count mismatches
			float pixels[] = (float[]) missclass_points_image.getPixels();
			float mask_pixels[] = (null != maskReal) ? (float[]) maskReal.getProcessor().getPixels() : new float[pixels.length];
			if(null == maskReal)
				Arrays.fill(mask_pixels, 1f);

			diff = 0;
			for(int k = 0; k < pixels.length; k++)
				if(pixels[k] != 0 && mask_pixels[k] != 0)
					diff ++;

			//IJ.log("Difference = " + diff);
			
			if( diff == 0 )
			{
				result.mismatches = new ArrayList<Point3f>();
				break;
			}
			if(diff == diff_before)
				break;

			final ArrayList<Point3f> mismatches = new ArrayList<Point3f>();

			final float[] realTargetPix = (float[])targetReal.getProcessor().getPixels();

			// Sort mismatches by the absolute value of the target pixel value - threshold
			for(int x = 1; x < width+1; x++)
				for(int y = 1; y < height+1; y++)
				{
					if(pixels[x+y*(width+2)] != 0 && mask_pixels[x+y*(width+2)] != 0)
						mismatches.add(new Point3f(x , y , (float) Math.abs( realTargetPix[x+y*(width+2)] - binaryThreshold) ));
				}

			// Sort mismatches in descending order
			Collections.sort(mismatches,  new Comparator<Point3f>() {
			    public int compare(Point3f o1, Point3f o2) {
			        return (int)((o2.z - o1.z) *10000);
			    }});

			// Process mismatches
			for(final Point3f p : mismatches)
			{
				final int x = (int) p.x;
				final int y = (int) p.y;

				if(p.z < SIMPLE_POINT_THRESHOLD)
					continue;

				double[] val = new double[]{
						sourceRealPix[ (x-1) + (y-1) * (width+2) ],
						sourceRealPix[ (x  ) + (y-1) * (width+2) ],
						sourceRealPix[ (x+1) + (y-1) * (width+2) ],
						sourceRealPix[ (x-1) + (y  ) * (width+2) ],
						sourceRealPix[ (x  ) + (y  ) * (width+2) ],
						sourceRealPix[ (x+1) + (y  ) * (width+2) ],
						sourceRealPix[ (x-1) + (y+1) * (width+2) ],
						sourceRealPix[ (x  ) + (y+1) * (width+2) ],
						sourceRealPix[ (x+1) + (y+1) * (width+2) ]
				};

				final double pix = val[4];

				final ImagePlus patch = new ImagePlus("patch", new FloatProcessor(3,3,val));
				if( simple2DBertrand(patch, 4) )
				{
					sourceRealPix[ x + y * (width+2)] =  pix > 0.0 ? 0.0f : 1.0f ;
					//IJ.log("flipping pixel x: " + x + " y: " + y + " to " + (pix > 0  ? 0.0 : 1.0));
				}
			}
			result.mismatches = mismatches;
		}

		//IJ.run(sourceReal, "Canvas Size...", "width="+ width + " height=" + height + " position=Center zero");
		ip = source.createProcessor(width, height);
		ip.insert(sourceReal.getProcessor(), -1, -1);
		sourceReal.setProcessor(ip.duplicate());

		// Adjust mismatches coordinates 
		final ArrayList<Point3f> mismatches = new ArrayList<Point3f>();
		for(Point3f p : result.mismatches)
		{
			mismatches.add(new Point3f( p.x - 1, p.y - 1, p.z));
		}
		
		sourceReal.setTitle("Warped source");
		
		result.mismatches = mismatches;
		result.warpedSource = sourceReal;
		result.warpingError = diff / (width * height);
		return result;
	}

	/**
	 * Calculate the simple point warping in a concurrent way
	 * (to be submitted to an Executor Service)
	 * @param source moving image
	 * @param target fixed image
	 * @param mask mask image
	 * @param binaryThreshold binary threshold to use
	 * @return warping results (warped labels, warping error value and mismatching points)
	 */
	public Callable<WarpingResults> simplePointWarp2DConcurrent(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			final double binaryThreshold)
	{
		return new Callable<WarpingResults>(){
			public WarpingResults call(){

				return simplePointWarp2d(source, target, mask, binaryThreshold);
			}
		};
	}
	
	/**
	 * Calculate the simple point warping in a concurrent way
	 * (to be submitted to an Executor Service)
	 * @param source moving image
	 * @param target fixed image
	 * @param mask mask image
	 * @param binaryThreshold binary threshold to use
	 * @param radius radius in pixels to use while classifying pixels
	 * @return warping results (warped labels, warping error value and mismatching points)
	 */
	public Callable<WarpingResults> simplePointWarp2DConcurrent(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			final double binaryThreshold,
			final boolean calculateMismatchImage,
			final int radius )
	{
		return new Callable<WarpingResults>(){
			public WarpingResults call(){

				WarpingResults wr = simplePointWarp2d(source, target, mask, binaryThreshold);				
				
				if( calculateMismatchImage )
					wr.classifiedMismatches = getMismatchImage( wr, radius );
				
				return wr;
			}
		};
	}
	
	/**
	 * Get the image with the classified mismatches
	 * 
	 * @param wr warping results
	 * @param radius radius in pixels to use while classifying pixels
	 * @return image with classified mismatches
	 */
	public ImagePlus getMismatchImage(WarpingResults wr, int radius) 
	{
		int[] mismatchesLabels = classifyMismatches2d( wr.warpedSource, wr.mismatches, radius );
		ByteProcessor bp = new ByteProcessor( wr.warpedSource.getWidth(), wr.warpedSource.getHeight() );
		for(int i=0; i < wr.mismatches.size(); i++)
		{
			Point3f p = wr.mismatches.get( i );
			bp.set( (int)p.x, (int)p.y, mismatchesLabels[ i ] );
		}
		return new ImagePlus( "Mismatches", bp );
	}

	/**
	 * Get the image with the classified mismatches
	 * 
	 * @param wr warping results
	 * @param radius radius in pixels to use while classifying pixels
	 * @return image with classified mismatches
	 */
	public ImagePlus getMismatchImage(WarpingResults wr, int radius, int flags) 
	{
		int[] mismatchesLabels = classifyMismatches2d( wr.warpedSource, wr.mismatches, radius );
		ByteProcessor bp = new ByteProcessor( wr.warpedSource.getWidth(), wr.warpedSource.getHeight() );
		for(int i=0; i < wr.mismatches.size(); i++)
		{
			Point3f p = wr.mismatches.get( i );
			bp.set( (int)p.x, (int)p.y, mismatchesLabels[ i ] & flags );
		}
		return new ImagePlus( "Mismatches", bp );
	}
	
	/**
	 * Get the image with the classified mismatches
	 * 
	 * @param wr warping results
	 * @param mismatchesLabels labels of the warping mismatches
	 * @return image with classified mismatches
	 */
	public ImagePlus getMismatchImage(WarpingResults wr,int[] mismatchesLabels, int flags) 
	{
		ByteProcessor bp = new ByteProcessor( wr.warpedSource.getWidth(), wr.warpedSource.getHeight() );
		for(int i=0; i < wr.mismatches.size(); i++)
		{
			Point3f p = wr.mismatches.get( i );
			bp.set( (int)p.x, (int)p.y, mismatchesLabels[ i ] & flags );
		}
		return new ImagePlus( "Mismatches", bp );
	}
	
	/**
	 * Use simple point relaxation to warp 2D source into 2D target.
	 * Source is only modified at nonzero locations in the mask
	 * (multi-thread version)
	 *
	 * @param source input image to be relaxed
	 * @param target target image
	 * @param mask image mask
	 * @param binaryThreshold binarization threshold
	 * @param mismatches list of points that could not be flipped 
	 * @return warped source image
	 */
	public ImagePlus simplePointWarp2dMT(
			ImagePlus source,
			ImagePlus target,
			ImagePlus mask,
			double binaryThreshold,
			ArrayList<Point3f>[] mismatches)
	{
		if(source.getWidth() != target.getWidth()
				|| source.getHeight() != target.getHeight()
				|| source.getImageStackSize() != target.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return null;
		}


		final ImageStack sourceSlices = source.getImageStack();
		final ImageStack targetSlices = target.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;

		final ImageStack warpedSource = new ImageStack(source.getWidth(), source.getHeight());

		if(null == mismatches)
			mismatches = new ArrayList[sourceSlices.getSize()];

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<WarpingResults> > futures = new ArrayList< Future<WarpingResults> >();

		try{
			for(int i = 1; i <= sourceSlices.getSize(); i++)
			{
				futures.add(exe.submit( simplePointWarp2DConcurrent(sourceSlices.getProcessor(i),
										targetSlices.getProcessor(i),
										null != maskSlices ? maskSlices.getProcessor(i) : null,
										binaryThreshold ) ) );
			}

			double warpingError = 0;
			int i = 0;
			// Wait for the jobs to be done
			for(Future<WarpingResults> f : futures)
			{
				final WarpingResults wr = f.get();
				if(null != wr.warpedSource)
					warpedSource.addSlice("warped source " + i, wr.warpedSource.getProcessor());
				if(wr.warpingError != -1)
					warpingError += wr.warpingError;
				if(null != wr.mismatches)
					mismatches[i] = wr.mismatches;
				i++;
			}
			if( verbose )
				IJ.log("Warping error = " + (warpingError / sourceSlices.getSize()));
		}
		catch(Exception ex)
		{
			IJ.log("Error when warping ground truth in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return new ImagePlus("warped source", warpedSource);
	}		

	/**
	 * Use simple point relaxation to warp 2D source into 2D target.
	 * Source is only modified at nonzero locations in the mask
	 *
	 * @param source input image to be relaxed
	 * @param target target image
	 * @param mask image mask
	 * @param binaryThreshold binarization threshold
	 * @return warped source image
	 */
	public ImagePlus simplePointWarp2d(
			ImagePlus source,
			ImagePlus target,
			ImagePlus mask,
			double binaryThreshold)
	{
		if(source.getWidth() != target.getWidth()
				|| source.getHeight() != target.getHeight()
				|| source.getImageStackSize() != target.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return null;
		}

		final ImageStack sourceSlices = source.getImageStack();
		final ImageStack targetSlices = target.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;

		final ImageStack warpedSource = new ImageStack(source.getWidth(), source.getHeight());

		double warpingError = 0;
		for(int i = 1; i <= sourceSlices.getSize(); i++)
		{
			WarpingResults wr = simplePointWarp2d(sourceSlices.getProcessor(i),
					targetSlices.getProcessor(i), null != mask ? maskSlices.getProcessor(i) : null,
					binaryThreshold);
			if(null != wr.warpedSource)
				warpedSource.addSlice("warped source " + i, wr.warpedSource.getProcessor());
			if(wr.warpingError != -1)
				warpingError += wr.warpingError;
		}

		//IJ.log("Warping error = " + (warpingError / sourceSlices.getSize()));

		return new ImagePlus("warped source", warpedSource);
	}
	
	/**
	 * Use simple point relaxation to warp 2D source into 2D target.
	 * Source is only modified at nonzero locations in the mask
	 * (multi-thread version)
	 *
	 * @param source input image to be relaxed (2D image or stack)
	 * @param target target image (2D image or stack)
	 * @param mask image mask (2D image or stack)
	 * @param binaryThreshold binarization threshold
	 * @return warping results for each slice of the source
	 */
	public WarpingResults[] simplePointWarp2dMT(
			ImagePlus source,
			ImagePlus target,
			ImagePlus mask,
			double binaryThreshold)
	{
		if(source.getWidth() != target.getWidth()
				|| source.getHeight() != target.getHeight()
				|| source.getImageStackSize() != target.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return null;
		}

		final ImageStack sourceSlices = source.getImageStack();
		final ImageStack targetSlices = target.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;

		final WarpingResults[] wrs = new WarpingResults[ source.getImageStackSize() ];

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<WarpingResults> > futures = new ArrayList< Future<WarpingResults> >();

		try{
			for(int i = 1; i <= sourceSlices.getSize(); i++)
			{
				futures.add(exe.submit( simplePointWarp2DConcurrent(sourceSlices.getProcessor(i).convertToFloat(),
										targetSlices.getProcessor(i).convertToFloat(),
										null != maskSlices ? maskSlices.getProcessor(i) : null,
										binaryThreshold ) ) );
			}

			int i = 0;
			// Wait for the jobs to be done
			for(Future<WarpingResults> f : futures)
			{
				wrs[ i ] = f.get();				
				i++;
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

		return wrs;
	}

	/**
	 * Use simple point relaxation to warp 2D labels into the 2D proposal.
	 * Source is only modified at nonzero locations in the mask
	 * (multi-thread version)
	 *
	 * @param binaryThreshold binarization threshold
	 * @param clusterByError if false, cluster mismatches by type, otherwise cluster them by error and type
	 * @param calculateMismatchImage boolean flag to calculate mismatch image
	 * @param radius radius in pixels to use while classifying mismatches
	 * @return warping results for each slice of the source
	 */
	public WarpingResults[] simplePointWarp2dMT(
			double binaryThreshold,
			boolean clusterByError,
			boolean calculateMismatchImage,
			int radius )
	{
		final ImageStack sourceSlices = originalLabels.getImageStack();
		final ImageStack targetSlices = proposedLabels.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;

		final WarpingResults[] wrs = new WarpingResults[ originalLabels.getImageStackSize() ];

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<WarpingResults> > futures = new ArrayList< Future<WarpingResults > >();

		try{
			for(int i = 1; i <= sourceSlices.getSize(); i++)
			{
				futures.add(exe.submit( getWarpingResultsConcurrent(sourceSlices.getProcessor(i).convertToFloat(),
													targetSlices.getProcessor(i).convertToFloat(),
													null != maskSlices ? maskSlices.getProcessor(i) : null,
													binaryThreshold, clusterByError, radius, 
													flags, calculateMismatchImage ) ) );
			}

			int i = 0;
			// Wait for the jobs to be done
			for(Future<WarpingResults> f : futures)
			{
				wrs[ i ] = f.get();				
				i++;
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

		return wrs;
	}
	
	
	/**
	 * Calculate warping error (single thread version)
	 *
	 * @param label original labels (single image or stack)
	 * @param proposal proposed new labels
	 * @param mask image mask
	 * @param binaryThreshold binary threshold to binarize proposal
	 * @return total warping error
	 */
	public double warpingErrorSingleThread(
			ImagePlus label,
			ImagePlus proposal,
			ImagePlus mask,
			double binaryThreshold)
	{
		final ImagePlus warpedLabels = simplePointWarp2d(label, proposal, mask, binaryThreshold);

		if(null == warpedLabels)
			return -1;

		double error = 0;
		double count = 0;


		for(int j=1; j<=proposal.getImageStackSize(); j++)
		{
			final float[] proposalPixels = (float[])proposal.getImageStack().getProcessor(j).getPixels();
			final float[] warpedPixels = (float[])warpedLabels.getImageStack().getProcessor(j).getPixels();
			for(int i=0; i<proposalPixels.length; i++)
			{
				count ++;
				final float thresholdedProposal = (proposalPixels[i] <= binaryThreshold) ? 0.0f : 1.0f;
				if (warpedPixels[i] != thresholdedProposal)
					error++;
			}

		}

		if(count != 0)
			return error / count;
		else
			return -1;
	}

		
	/**
	 * Get all the mismatches of warping a source image into a target image  
	 * and clustering them when they belong to the same error. Simple point 
	 * relaxation is used for the warping. The source is only modified at 
	 * nonzero locations in the mask (multi-thread static version)
	 *
	 * @param source input image to be relaxed (2D image or stack)
	 * @param target target image (2D image or stack)
	 * @param mask image mask (2D image or stack)
	 * @param binaryThreshold binarization threshold
	 * @param clusterByError if false, cluster mismatches by type, otherwise cluster them by error and type
	 * @param radius radius in pixels of the local area to look when deciding some cases (small radius speed up the method a lot, -1 to use whole image) 
	 * @return clustered warping mismatches for each slice of the source
	 */
	public ClusteredWarpingMismatches[] getClusteredWarpingMismatches(
			ImagePlus source,
			ImagePlus target,
			ImagePlus mask,
			double binaryThreshold,
			boolean clusterByError,
			int radius)
	{
		if(source.getWidth() != target.getWidth()
				|| source.getHeight() != target.getHeight()
				|| source.getImageStackSize() != target.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return null;
		}

		final ImageStack sourceSlices = source.getImageStack();
		final ImageStack targetSlices = target.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;

		final ClusteredWarpingMismatches[] cwm = new ClusteredWarpingMismatches[ source.getImageStackSize() ];

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<ClusteredWarpingMismatches> > futures = new ArrayList< Future<ClusteredWarpingMismatches> >();

		try{
			for(int i = 1; i <= sourceSlices.getSize(); i++)
			{
				futures.add(exe.submit( getClusteredWarpingMismatchesConcurrent(sourceSlices.getProcessor(i).convertToFloat(),
										targetSlices.getProcessor(i).convertToFloat(),
										null != maskSlices ? maskSlices.getProcessor(i) : null,
										binaryThreshold, clusterByError, radius ) ) );
			}

			int i = 0;
			// Wait for the jobs to be done
			for(Future<ClusteredWarpingMismatches> f : futures)
			{
				cwm[ i ] = f.get();				
				i++;
			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when getting the clustered warping mismatches in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return cwm;
	}

	/**
	 * Calculate the simple point warping in a concurrent way
	 * (to be submitted to an Executor Service)
	 * 
	 * @param source moving image
	 * @param target fixed image
	 * @param mask mask image
	 * @param binaryThreshold binary threshold to use
	 * @param clusterByError boolean flag to use clustering by error or only by type
	 * @param radius radius in pixels of the local area to look when deciding some cases (small radius speed up the method a lot, -1 to use whole image) 
	 * @return clustered mismatching points after warping
	 */
	public Callable<ClusteredWarpingMismatches> getClusteredWarpingMismatchesConcurrent(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			final double binaryThreshold,
			final boolean clusterByError,
			final int radius)
	{
		return new Callable<ClusteredWarpingMismatches>()
		{
			public ClusteredWarpingMismatches call()
			{
				WarpingResults wr = simplePointWarp2d(source, target, mask, binaryThreshold);				
				//wr.warpedSource.show();
				int[] mismatchesLabels = classifyMismatches2d( wr.warpedSource, wr.mismatches, radius );

				if( clusterByError )
					return clusterMismatchesByError( wr.warpedSource, wr.mismatches, mismatchesLabels );
				else
					return clusterMismatchesByType( mismatchesLabels );
			}
		};
	}

	/**
	 * Calculate the simple point warping in a concurrent way
	 * (to be submitted to an Executor Service)
	 * 
	 * @param source moving image
	 * @param target fixed image
	 * @param mask mask image
	 * @param binaryThreshold binary threshold to use
	 * @param clusterByError boolean flag to use clustering by error or only by type
	 * @param radius radius in pixels of the local area to look when deciding some cases (small radius speed up the method a lot, -1 to use whole image) 
	 * @param flags flags indicating the type of errors to take into account
	 * @param calculateMismatchImage boolean flag to determine if the mismatches image should be calculated
	 * @return clustered mismatching points after warping
	 */
	public Callable<WarpingResults> getWarpingResultsConcurrent(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			final double binaryThreshold,
			final boolean clusterByError,
			final int radius,
			final int flags,
			final boolean calculateMismatchImage)
	{
		return new Callable<WarpingResults>()
		{
			public WarpingResults call()
			{
				WarpingResults wr = simplePointWarp2d(source, target, mask, binaryThreshold);				
				//wr.warpedSource.show();
				int[] mismatchesLabels = classifyMismatches2d( wr.warpedSource, wr.mismatches, radius );

				if( calculateMismatchImage )
					wr.classifiedMismatches = getMismatchImage( wr, mismatchesLabels, flags );
				
				ClusteredWarpingMismatches cwm = null;
				if( clusterByError )
					cwm = clusterMismatchesByError( wr.warpedSource, wr.mismatches, mismatchesLabels );
				else
					cwm = clusterMismatchesByType( mismatchesLabels );
				
				double error = 0;
				double count = source.getWidth() * source.getHeight();
				
				if( (flags & HOLE_ADDITION) != 0)				
					error += cwm.numOfHoleAdditions;
				if( (flags & HOLE_DELETION) != 0)
					error += cwm.numOfHoleDeletions;
				if( (flags & MERGE) != 0)
					error += cwm.numOfMergers; 
				if( (flags & OBJECT_ADDITION) != 0)
					error += cwm.numOfObjectAdditions;
				if( (flags & OBJECT_DELETION) != 0)
					error += cwm.numOfObjectDeletions; 
				if( (flags & SPLIT) != 0)
					error += cwm.numOfSplits;

				wr.warpingError = error / count;
				
				return wr;
			}
		};
	}	
	
		
	/**
	 * Classify warping mismatches as MERGE, SPLIT, HOLE_ADDITION, HOLE_DELETION, OBJECT_ADDITION, OBJECT_DELETION
	 *  
	 * @param warpedLabels labels after warping (binary image)
	 * @param mismatches list of mismatch points after warping
	 * @param radius radius in pixels of the local area to look when deciding some cases (small radius speed up the method a lot, -1 to use whole image) 
	 * @return array of mismatch classifications
	 */
	public int[] classifyMismatches2d( 
			ImagePlus warpedLabels, 
			ArrayList<Point3f> mismatches,
			int radius)
	{
		final int[] pointClassification = new int[ mismatches.size() ];
		
		int radiusToUse = radius;
		if(radius <1 || radius > warpedLabels.getWidth() || radius > warpedLabels.getHeight())
			radiusToUse = -1;			
		
		// Calculate components in warped labels
		ImageProcessor components = Utils.connectedComponents(
				new ImagePlus("8-bit warped labels", warpedLabels.getProcessor().convertToByte(true)
						), 4).allRegions.getProcessor();
		
		int n = 0;
		for(Point3f p : mismatches)
		{
			final int x = (int) p.x;
			final int y = (int) p.y;
			final ArrayList<Integer> neighborhood = getNeighborhood(components, new Point(x, y), 1, 1);
			
			//IJ.log(" mismatch ("+ p.x + ", " + p.y + ")");
								
			// Count number of unique IDs in the neighborhood
			ArrayList<Integer> uniqueId = new ArrayList<Integer>();
			for( Integer neighbor : neighborhood)
			{
				if(!uniqueId.contains( neighbor ))
					uniqueId.add( neighbor );				
			}
					
			// If all surrounding pixels are background
			if( uniqueId.size() == 1 && uniqueId.get(0) == 0)
			{
				if(components.getPixel(x, y) != 0)
				{
					pointClassification[ n ] = OBJECT_DELETION;
					//IJ.log(" all surrounding pixels are black and the point is white -> object deletion");
				}
				else
				{
					pointClassification[ n ] = OBJECT_ADDITION;
					//IJ.log(" all surrounding pixels are black and the point is black -> object addition");
				}
			}
			// If all surrounding pixels belong to one object 
			else if ( uniqueId.size() == 1 && uniqueId.get(0) != 0)
			{
				if(components.getPixel(x, y) != 0)
				{
					pointClassification[ n ] = HOLE_ADDITION;
					//IJ.log(" all surrounding pixels are white and the point is white -> hole addition");
				}
				else
				{
					pointClassification[ n ] = HOLE_DELETION;
					//IJ.log(" all surrounding pixels are white and the point is black -> hole deletion");
				}
			}
			// If there are background and one single object ID in the surrounding pixels
			else if ( uniqueId.size() == 2 )
			{
				// if the point is black, that's a hole addition error (flipping it to white would create a hole)
				if (components.getPixel(x, y) == 0)
				{
					pointClassification[ n ] = HOLE_ADDITION;
					//IJ.log(" surrounding pixels are white and black and the point is black -> hole addition");
				}
				else // if the point is white
				{
					// flip pixel and apply connected components again					
					ByteProcessor warpedPixels2;
					  
					warpedPixels2 = (ByteProcessor) warpedLabels.getProcessor().duplicate().convertToByte(true);
										
					Point pixelOfInterest = new Point( x, y );
					
					if (radiusToUse != -1)
					{	
						warpedPixels2 = new ByteProcessor( 2*radiusToUse+1, 2*radiusToUse+1 );
						for(int i = x-radiusToUse, l=0; i<=x+radiusToUse; i++, l++)
							for(int j = y-radiusToUse, k=0; j<=y+radiusToUse; j++, k++)
								warpedPixels2.set(l, k, warpedLabels.getProcessor().getPixel(i, j) == 0 ? 0 : 255);
						pixelOfInterest = new Point( radiusToUse , radiusToUse );
					}
					
					// flip pixel
					warpedPixels2.set( pixelOfInterest.x, pixelOfInterest.y, 0 );
					
					// Calculate components in the new warped labels
					ImageProcessor components2 = Utils.connectedComponents(new ImagePlus("8-bit warped labels", warpedPixels2), 4).allRegions.getProcessor();

					//(new ImagePlus( "components", components2)).show();

					final ArrayList<Integer> neighborhood2 = getNeighborhood(components2, pixelOfInterest, 1, 1);								

					// Count number of unique IDs in the neighborhood of the new components
					ArrayList<Integer> uniqueId2 = new ArrayList<Integer>();
					for( Integer neighbor : neighborhood2)
					{			
						if(!uniqueId2.contains( neighbor ))
							uniqueId2.add( neighbor );				
					}

					// If there are more than 2 new components then it's a split
					if ( uniqueId2.size() > 2 )
					{
						pointClassification[ n ] = SPLIT;
						//IJ.log(" all surrounding pixels are white, the point is white and second CC has more than 2 objects -> split");
					}
					// otherwise it deletes a hole
					else
					{
						pointClassification[ n ] = HOLE_DELETION;
						//IJ.log(" all surrounding pixels are white, the point is white and second CC has 2 objects -> hole deletion");
					}
				}
			}			
			else // If there are more than 1 object ID in the surrounding pixels 
			{
				if(components.getPixel(x, y) == 0)
				{
					pointClassification[ n ] = MERGE;
					//IJ.log(" surrounding pixels have at least 2 objects and the point is black -> merge");					
				}
				else
				{
					pointClassification[ n ] = SPLIT;
					//IJ.log(" surrounding pixels have at least 2 objects and the point is white -> split");					
				}
			}	
			n++;
		}
		
		return pointClassification;
	}
	
	/**
	 * Classify warping mismatches as MERGE, SPLIT, HOLE_ADDITION, 
	 * HOLE_DELETION, OBJECT_ADDITION, OBJECT_DELETION and count 
	 * the number of false positives and false negatives
	 *  
	 * @param warpedLabels labels after warping (binary image)
	 * @param mismatches list of mismatch points after warping
	 * @param falsePositives (output) number of false positives
	 * @param falseNegatives (output) number of false negatives
	 * @param flags
	 * @return array of mismatch classifications
	 */
	public int[] classifyMismatches2d( 
			ImagePlus warpedLabels, 
			ArrayList<Point3f> mismatches,
			double falsePositives,
			double falseNegatives,
			int flags)
	{
		final int[] pointClassification = new int[ mismatches.size() ];
		
		// Calculate components in warped labels
		ImageProcessor components = Utils.connectedComponents(
				new ImagePlus("8-bit warped labels", warpedLabels.getProcessor().convertToByte(true)
						), 4).allRegions.getProcessor();
		
		int n = 0;
		for(Point3f p : mismatches)
		{
			final int x = (int) p.x;
			final int y = (int) p.y;
			final ArrayList<Integer> neighborhood = getNeighborhood(components, new Point(x, y), 1, 1);
								
			// Count number of unique IDs in the neighborhood
			ArrayList<Integer> uniqueId = new ArrayList<Integer>();
			for( Integer neighbor : neighborhood)
			{
				if(!uniqueId.contains( neighbor ))
					uniqueId.add( neighbor );				
			}
					
			// If all surrounding pixels are background
			if( uniqueId.size() == 1 && uniqueId.get(0) == 0)
			{
				if(components.getPixel(x, y) != 0)
				{
					pointClassification[ n ] = OBJECT_DELETION;
					if( (flags & OBJECT_DELETION) != 0 )
						falseNegatives ++;
				}
				else
				{
					pointClassification[ n ] = OBJECT_ADDITION;
					if( (flags & OBJECT_ADDITION) != 0 )
						falsePositives ++;
				}
			}
			// If all surrounding pixels belong to one object 
			else if ( uniqueId.size() == 1 && uniqueId.get(0) != 0)
			{
				if(components.getPixel(x, y) != 0)
				{
					pointClassification[ n ] = HOLE_ADDITION;
					if( (flags & HOLE_ADDITION) != 0 )
						falseNegatives ++;
				}
				else
				{
					pointClassification[ n ] = HOLE_DELETION;
					if( (flags & HOLE_DELETION) != 0 )
						falsePositives ++;
				}
			}
			
			// If there are background and one single object ID in the surrounding pixels
			else if ( uniqueId.size() == 2 )
			{
				if (components.getPixel(x, y) == 0)
				{
					pointClassification[ n ] = HOLE_ADDITION;
					if( (flags & HOLE_ADDITION) != 0 )
						falsePositives ++;
				}
				else
				{
					// flip pixel and apply connected components again
					final ByteProcessor warpedPixels2 = (ByteProcessor) warpedLabels.getProcessor().duplicate().convertToByte(true);
					warpedPixels2.set( x, y, warpedPixels2.get(x, y) != 0 ? 0 : 255);
					// Calculate components in the new warped labels
					ImageProcessor components2 = Utils.connectedComponents(new ImagePlus("8-bit warped labesl", warpedPixels2), 4).allRegions.getProcessor();


					final ArrayList<Integer> neighborhood2 = getNeighborhood(components2, new Point(x, y), 1, 1);								

					// Count number of unique IDs in the neighborhood of the new components
					ArrayList<Integer> uniqueId2 = new ArrayList<Integer>();
					for( Integer neighbor : neighborhood2)
					{			
						if(!uniqueId2.contains( neighbor ))
							uniqueId2.add( neighbor );				
					}

					// If there are more than 2 new components then it's a split
					if ( uniqueId2.size() > 2 )
					{
						pointClassification[ n ] = SPLIT;
						if( (flags & SPLIT) != 0 )
							falseNegatives ++;
					}
					// otherwise it deletes a hole
					else
					{
						pointClassification[ n ] = HOLE_DELETION;
						if( (flags & HOLE_DELETION) != 0 )
							falseNegatives ++;
					}
					
					
				}
			}			
			else // If there are more than 1 object ID in the surrounding pixels 
			{
				if(components.getPixel(x, y) == 0)
				{
					pointClassification[ n ] = MERGE;
					if( (flags & MERGE) != 0 )
						falsePositives ++;
				}
				else
				{
					pointClassification[ n ] = SPLIT;
					if( (flags & SPLIT) != 0 )
						falseNegatives ++;
				}
			}	
			n++;
		}
		
		return pointClassification;
	}
	

	/**
	 * Cluster the result mismatches from the warping so pixels
	 * belonging to the same error are only counted once.
	 * 
	 * @param warpedLabels result warped labels
	 * @param mismatches list of non simple points 
	 * @param mismatchClassification array of classified mismatches
	 * @return number of warping mismatches after clustering by error
	 */
	public ClusteredWarpingMismatches clusterMismatchesByError(
			ImagePlus warpedLabels, 
			ArrayList<Point3f> mismatches, 
			int [] mismatchClassification)
	{
		
		// Create the 8 possible cases out of the mismatches
		// 0: object addition, 1: hole deletion with an isolated background pixel
		// 2: merger, 3: hole creation by removing a background pixel 
		// 4: delete object, 5: hole creation by adding a background pixel
		// 6: split ,7: hole deletion by removing a foreground pixel

		ByteProcessor[] binaryMismatches = new ByteProcessor[ 8 ];
		
		final int width = warpedLabels.getWidth();
		final int height = warpedLabels.getHeight();
		
		for(int i=0; i<8; i++)
			binaryMismatches[ i ] = new ByteProcessor(width, height);
		
		// corresponding connectivity for each case (to run connected components)
		final int[] connectivity = new int[]{4, 4, 8, 4, 4, 8, 4, 4};
		
		for(int i=0 ; i < mismatchClassification.length; i++)
		{
			final int x = (int) mismatches.get( i ).x;
			final int y = (int) mismatches.get( i ).y;
			
			switch( mismatchClassification[ i ])
			{				
				case OBJECT_ADDITION:
					binaryMismatches[ 0 ].set(x, y, 255);
					break;
				case HOLE_DELETION:
					if( warpedLabels.getProcessor().getPixel(x, y) == 0)
						binaryMismatches[ 1 ].set(x, y, 255);
					else
						binaryMismatches[ 7 ].set(x, y, 255);
					break;
				case MERGE:
					binaryMismatches[ 2 ].set(x, y, 255);
					break;
				case HOLE_ADDITION:
					if( warpedLabels.getProcessor().getPixel(x, y) == 0)
						binaryMismatches[ 3 ].set(x, y, 255);
					else
						binaryMismatches[ 5 ].set(x, y, 255);
					break;
				case OBJECT_DELETION:
					binaryMismatches[ 4 ].set(x, y, 255);
					break;
				case SPLIT:
					binaryMismatches[ 6 ].set(x, y, 255);
					break;
				default:					
			}
		}
		
		// run connected components on each case
		int[] componentsPerCase = new int[8];
		for(int i=0; i<8; i++)
		{
			ImagePlus im = new ImagePlus("components case " + i, binaryMismatches[ i ]);
			//im.show();
			componentsPerCase[i] = Utils.connectedComponents(	im, connectivity[ i ]).regionInfo.size();
		}
						
		return new ClusteredWarpingMismatches(componentsPerCase[ 0 ], 
							componentsPerCase[ 1 ] + componentsPerCase[ 7 ], 
							componentsPerCase[ 2 ], 
							componentsPerCase[ 3 ] + componentsPerCase[ 5 ], 
							componentsPerCase[ 4 ], 
							componentsPerCase[ 6 ]);
	}	
	
	
	/**
	 * Cluster the result mismatches from the warping
	 * by types of errors.
	 * 
	 * @param mismatchClassification array of classified mismatches
	 * @return number of warping mismatches after clustering by type
	 */
	public ClusteredWarpingMismatches clusterMismatchesByType(
			int [] mismatchClassification)
	{
		
		// Create the 8 possible cases out of the mismatches
		// 0: object addition, 1: hole deletion with an isolated background pixel
		// 2: merger, 3: hole creation by removing a background pixel 
		// 4: delete object, 5: hole creation by adding a background pixel
		// 6: split, 7: hole deletion by removing a foreground pixel

		int numOfObjectAdditions = 0;
		int numOfHoleDeletions = 0;
		int numOfMergers = 0;
		int numOfHoleAdditions = 0;
		int numOfObjectDeletions = 0;
		int numOfSplits = 0;
		
		for(int i=0 ; i < mismatchClassification.length; i++)
		{			
			switch( mismatchClassification[ i ])
			{				
				case OBJECT_ADDITION:
					numOfObjectAdditions ++;
					break;
				case HOLE_DELETION:
					numOfHoleDeletions ++;
					break;
				case MERGE:
					numOfMergers ++;
					break;
				case HOLE_ADDITION:
					numOfHoleAdditions ++;
					break;
				case OBJECT_DELETION:
					numOfObjectDeletions ++;
					break;
				case SPLIT:
					numOfSplits ++;
					break;
				default:	
					IJ.log("Unrecognized mismatch classification!");
			}
		}				 
		
		return new ClusteredWarpingMismatches(numOfObjectAdditions,
				numOfHoleDeletions, numOfMergers,
				numOfHoleAdditions, numOfObjectDeletions,
				numOfSplits);
	}	
	
	/**
	 * Get neighborhood of a pixel in a 2D image
	 * 
	 * @param image 2D image
	 * @param p point coordinates
	 * @param x_offset x- neighborhood offset
	 * @param y_offset y- neighborhood offset
	 * @return corresponding neighborhood
	 */
	public ArrayList<Integer> getNeighborhood(
			final ImageProcessor image, 
			final Point p, 
			final int x_offset, 
			final int y_offset)
	{
		final ArrayList<Integer> neighborhood = new ArrayList<Integer>();
		

		for(int j = p.y - y_offset; j <= p.y + y_offset; j++)
			for(int i = p.x - x_offset; i <= p.x + x_offset; i++)							
			{
				if(i!=p.x || j!= p.y)
					if(j>=0 && j<image.getHeight() && i>=0 && i<image.getWidth())
						neighborhood.add( image.get(i, j));
			}
		
		return neighborhood;
	} // end getNeighborhood 		

	
	
	/**
	 * Calculate the number of cavities of a 3D neighborhood
	 * 
	 * @param input 3D neighborhood
	 * @param con connectivity (6, 18 or 26)
	 * @param space
	 * @return number of cavities of the 3D neighborhood
	 */
	int nca(float[] input, int con, int space)
	{
		int tsum;
		switch (con)
		{
		case 6:
			tsum=((int)input[4] + (int)input[10]+(int)input[12]+(int)input[14]+(int)input[16] + (int)input[22]);
			return (space==1)?(6*space - tsum):(tsum);
		case 18:
			tsum=((int)input[1]+(int)input[3]+(int)input[4]+(int)input[5]+(int)input[7] + (int)input[9]+(int)input[10]+(int)input[11]+(int)input[12]+(int)input[14]+(int)input[15]+(int)input[16]+(int)input[17] + (int)input[19]+(int)input[21]+(int)input[22]+(int)input[23]+(int)input[25]);
			return (space==1)?(18*space - tsum):(tsum);
		case 26:
			tsum=((int)input[0]+(int)input[1]+(int)input[2]+(int)input[3]+(int)input[4]+(int)input[5]+(int)input[6]+(int)input[7]+(int)input[8] + (int)input[9]+(int)input[10]+(int)input[11]+(int)input[12]+(int)input[14]+(int)input[15]+(int)input[16]+(int)input[17] + (int)input[18]+(int)input[19]+(int)input[20]+(int)input[21]+(int)input[22]+(int)input[23]+(int)input[24]+(int)input[25]+(int)input[26]);
			return (space==1)?(26*space - tsum):(tsum);
		default:
			return 0;
		}
	}

	/**
	 * 
	 * @param input 3D neighborhood
	 * @param ctyp
	 * @param con connectivity (6, 18 or 26)
	 * @param space
	 * @return
	 */
	int ncb(float[] input, char ctyp, int con, int space)
	{
		int tsum;
		final int[][][] a6m = new int[][][]{{{0,1,0}, {1,5,1}, {0,1,0}},
											{{0,0,0}, {0,0,0}, {0,0,0}},
											{{0,0,0}, {0,0,0}, {0,0,0}}};

		final int[][][] a18m = new int[][][]{{{0,0,0}, {0,1,1}, {0,0,0}},
											 {{0,0,0}, {0,0,1}, {0,0,0}},
											 {{0,0,0}, {0,0,0}, {0,0,0}}};

		final int[][][] a26m = new int[][][]{{{0,0,0}, {0,1,1}, {0,1,1}},
											 {{0,0,0}, {0,0,1}, {0,1,1}},
											 {{0,0,0}, {0,0,0}, {0,0,0}}};

		final int[][][] b18m = new int[][][]{{{0,1,0}, {0,1,9}, {0,1,0}},
											 {{0,1,1}, {0,0,1}, {0,1,1}},
											 {{0,0,0}, {0,0,0}, {0,0,0}}};

		final int[][][] b26m = new int[][][]{{{0,0,0}, {0,1,1}, {0,1,7}},
											 {{0,0,0}, {0,0,1}, {0,1,1}},										
											 {{0,0,0}, {0,0,0}, {0,0,0}}};

		int[] tsuma = new int[12];
		int x, y, z, i;

		if(ctyp=='a'){
			switch (con)
			{
			case 6:
				for(x=0;x<3;x++){
					for(y=0;y<3;y++){
						for(z=0;z<3;z++){
							tsuma[0]+=a6m[x][y][z]*input[x+y*3+z*3*3];
							tsuma[1]+=a6m[2-x][y][z]*input[x+y*3+z*3*3];
							tsuma[2]+=a6m[y][x][z]*input[x+y*3+z*3*3];
							tsuma[3]+=a6m[2-y][x][z]*input[x+y*3+z*3*3];
							tsuma[4]+=a6m[z][y][x]*input[x+y*3+z*3*3];
							tsuma[5]+=a6m[2-z][y][x]*input[x+y*3+z*3*3];
						}
					}
				}
				tsum=0;
				for(i=0;i<6;i++)
					tsum += (tsuma[i]==(5-space))?1:0;

				return tsum;
			case 18:
				for(x=0;x<3;x++){
					for(y=0;y<3;y++){
						for(z=0;z<3;z++){
							tsuma[0]+=a18m[x][y][z]*input[x+y*3+z*3*3];
							tsuma[1]+=a18m[x][y][2-z]*input[x+y*3+z*3*3];
							tsuma[2]+=a18m[2-x][y][z]*input[x+y*3+z*3*3];
							tsuma[3]+=a18m[2-x][y][2-z]*input[x+y*3+z*3*3];
							tsuma[4]+=a18m[y][x][z]*input[x+y*3+z*3*3];
							tsuma[5]+=a18m[y][x][2-z]*input[x+y*3+z*3*3];
							tsuma[6]+=a18m[2-y][x][z]*input[x+y*3+z*3*3];
							tsuma[7]+=a18m[2-y][x][2-z]*input[x+y*3+z*3*3];
							tsuma[8]+=a18m[x][z][y]*input[x+y*3+z*3*3];
							tsuma[9]+=a18m[x][z][2-y]*input[x+y*3+z*3*3];
							tsuma[10]+=a18m[2-x][z][y]*input[x+y*3+z*3*3];
							tsuma[11]+=a18m[2-x][z][2-y]*input[x+y*3+z*3*3];
						}
					}
				}
				tsum=0;
				for(i=0;i<12;i++){
					tsum += (tsuma[i]==(3-3*space))?1:0;
				}
				return tsum;
			case 26:
				for(x=0;x<3;x++){
					for(y=0;y<3;y++){
						for(z=0;z<3;z++){
							tsuma[0]+=a26m[x][y][z]*input[x+y*3+z*3*3];
							tsuma[1]+=a26m[2-x][y][z]*input[x+y*3+z*3*3];
							tsuma[2]+=a26m[x][2-y][z]*input[x+y*3+z*3*3];
							tsuma[3]+=a26m[x][y][2-z]*input[x+y*3+z*3*3];
							tsuma[4]+=a26m[2-x][2-y][z]*input[x+y*3+z*3*3];
							tsuma[5]+=a26m[x][2-y][2-z]*input[x+y*3+z*3*3];
							tsuma[6]+=a26m[2-x][y][2-z]*input[x+y*3+z*3*3];
							tsuma[7]+=a26m[2-x][2-y][2-z]*input[x+y*3+z*3*3];
						}
					}
				}
				tsum=0;
				for(i=0;i<8;i++){
					tsum += (tsuma[i]==(7-7*space))?1:0;
				}
				return tsum;
			default:
				return 0;
			}
		}else if(ctyp=='b'){
			switch (con)
			{
			case 18:
				for(x=0;x<3;x++){
					for(y=0;y<3;y++){
						for(z=0;z<3;z++){
							tsuma[0]+=b18m[x][y][z]*input[x+y*3+z*3*3];
							tsuma[1]+=b18m[x][y][2-z]*input[x+y*3+z*3*3];
							tsuma[2]+=b18m[2-x][y][z]*input[x+y*3+z*3*3];
							tsuma[3]+=b18m[2-x][y][2-z]*input[x+y*3+z*3*3];
							tsuma[4]+=b18m[y][x][z]*input[x+y*3+z*3*3];
							tsuma[5]+=b18m[y][x][2-z]*input[x+y*3+z*3*3];
							tsuma[6]+=b18m[2-y][x][z]*input[x+y*3+z*3*3];
							tsuma[7]+=b18m[2-y][x][2-z]*input[x+y*3+z*3*3];
							tsuma[8]+=b18m[x][z][y]*input[x+y*3+z*3*3];
							tsuma[9]+=b18m[x][z][2-y]*input[x+y*3+z*3*3];
							tsuma[10]+=b18m[2-x][z][y]*input[x+y*3+z*3*3];
							tsuma[11]+=b18m[2-x][z][2-y]*input[x+y*3+z*3*3];
						}
					}
				}
				tsum=0;
				for(i=0;i<12;i++){
					tsum += (tsuma[i]==(9-space))?1:0;
				}
				return tsum;
			case 26:
				for(x=0;x<3;x++){
					for(y=0;y<3;y++){
						for(z=0;z<3;z++){
							tsuma[0]+=b26m[x][y][z]*input[x+y*3+z*3*3];
							tsuma[1]+=b26m[2-x][y][z]*input[x+y*3+z*3*3];
							tsuma[2]+=b26m[x][2-y][z]*input[x+y*3+z*3*3];
							tsuma[3]+=b26m[x][y][2-z]*input[x+y*3+z*3*3];
							tsuma[4]+=b26m[2-x][2-y][z]*input[x+y*3+z*3*3];
							tsuma[5]+=b26m[x][2-y][2-z]*input[x+y*3+z*3*3];
							tsuma[6]+=b26m[2-x][y][2-z]*input[x+y*3+z*3*3];
							tsuma[7]+=b26m[2-x][2-y][2-z]*input[x+y*3+z*3*3];
						}
					}
				}
				tsum=0;
				for(i=0;i<8;i++){
					tsum += (tsuma[i]==7-space)?1:0;
				}
				return tsum;
			default:
				return 0;
			}
		}
		else
			return 0;

	}

	/**
	 * Calculate if a point is simple in 3D
	 * 
	 * @param input 3D neighborhood (27 pixels) in a single array
	 * @param region adjacency (26 or 6)
	 * @return true if the point is simple
	 */
	boolean simple3d(float[] input, int region)
	{
		boolean simple = false;

		if(region==26)
		{
			simple=false;
			if( nca(input, 6, 1)==1 ){
				simple=true;
			}else if(nca(input, 26, 0)==1 ){
				simple=true;
			}else if( ncb(input, 'b', 26, 0)==0 ){
				if( nca(input, 18, 0)==1 ){
					simple=true;
				}else if( (ncb(input, 'a', 6, 1)==0) && (ncb(input, 'b', 18, 0)==0) && ((nca(input, 6, 1)-ncb(input, 'a', 18, 1)+ncb(input, 'a', 26, 1))==1) ){
					simple=true;
				}
			}
		}
		else if(region==6)
		{
			int i;
			float[] input2 = new float[27];
			
			for(i=0;i<27;i++){
				input2[i] = input[i] == 1.0f ? 0.0f : 1.0f;
			}
			simple=false;
			if( nca(input2, 6, 1)==1 ){
				simple=true;
			}else if(nca(input2, 26, 0)==1 ){
				simple=true;
			}else if( ncb(input2, 'b', 26, 0)==0 ){
				if( nca(input2, 18, 0)==1 ){
					simple=true;
				}else if( (ncb(input2, 'a', 6, 1)==0) && (ncb(input2, 'b', 18, 0)==0) && ((nca(input2, 6, 1)-ncb(input2, 'a', 18, 1)+ncb(input2, 'a', 26, 1))==1) ){
					simple=true;
				}
			}
		}
		return simple;
	}

	
    /**
     * Main method for calcualte the warping error metrics 
     * from the command line
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
          else if (args[0].equals("-splitsAndMergers"))
        	  System.out.println( splitsAndMergersCommandLine(args) );
          else 
        	  dumpSyntax();
       }
       System.exit(0);
    }
    
    /**
     * Calculate the best splits and mergers ratio based on the
     * parameters introduced by command line
     * 
     * @param args command line arguments
     * @return warping error with minimum splits and mergers ratio
     */
    static double splitsAndMergersCommandLine(String[] args) 
    {
    	if (args.length != 8)
        {
            dumpSyntax();
            return -1;
        }
    	
    	final ImagePlus label = new ImagePlus( args[ 1 ] );
    	final ImagePlus proposal = new ImagePlus( args[ 2 ] );
    	final double minThreshold = Double.parseDouble( args[ 3 ] );
		final double maxThreshold = Double.parseDouble( args[ 4 ] );
		final double stepThreshold = Double.parseDouble( args[ 5 ] );
		final boolean clusterByError = Boolean.parseBoolean( args[ 6 ]);
		final int radius = Integer.parseInt( args[ 7 ]);
    	
		WarpingError we = new WarpingError(label, proposal);
		we.setVerboseMode( false );
		return we.getMinimumSplitsAndMergersErrorValue(minThreshold, maxThreshold, stepThreshold, clusterByError, radius );
	}

    /**
     * Set verbose mode
     * @param verbose true to display more information in the standard output
     */
    public void setVerboseMode(boolean verbose) 
    {		
    	this.verbose = verbose;
	}

	/**
     * Method to write the syntax of the program in the command line.
     */
    private static void dumpSyntax () 
    {
       System.out.println("Purpose: calculate warping error between proposed and original labels.\n");     
       System.out.println("Usage: WarpingError ");
       System.out.println("  -help                      : show this message");
       System.out.println("");
       System.out.println("  -splitsAndMergers          : calculate the splits and mergers ratio over a set of thresholds");
       System.out.println("          labels             : image with the original labels");
       System.out.println("          proposal           : image with the proposed labels");
       System.out.println("          minThreshold       : minimum threshold value to binarize the proposal");
       System.out.println("          maxThreshold       : maximum threshold value to binarize the proposal");
       System.out.println("          stepThreshold      : threshold step value to use during binarization");
       System.out.println("          clusterMistakes    : boolean flag to cluster or not the mistakes by type of error");
       System.out.println("          radius             : radius of the search neighborhood to decide simple points classification\n");
       System.out.println("Examples:");
       System.out.println("Calculate the splits and mergers ratio between proposed and original labels over a set of");
       System.out.println("thresholds (from 0.0 to 1.0 in steps of 0.1) without clustering the mistakes and using a \n" +
       					  "radius of 20 pixels:");
       System.out.println("   WarpingError -splitsAndMergers original-labels.tif proposed-labels.tif 0.0 1.0 0.1 false 20");
    } 

    
	/**
	 * Calculate warping error and return the related result images and values.
	 *
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @param clusterByError if false, cluster topology errors by type, otherwise cluster by type and mistake
	 * @param calculateMismatchImage flag to calculate mismatch image
	 * @param radius radius in pixels to use when classifiying mismatches
	 * @return total warping error (it counts all type of mismatches as errors)
	 */	
	public WarpingResults getWarpingResults(
			double binaryThreshold,
			boolean clusterByError, 
			boolean calculateMismatchImage,
			int radius ) 	
	{		
		if( verbose )
			IJ.log("  Warping ground truth...");
		
		// Warp ground truth, relax original labels to proposal. Only simple
		// points warping is allowed.
		WarpingResults[] wrs = simplePointWarp2dMT( binaryThreshold, clusterByError, calculateMismatchImage, radius );		

		
		if(null == wrs)
			return null;
		
		WarpingResults result = new WarpingResults();
		result.warpingError = 0;
		
		ImageStack is = new ImageStack( originalLabels.getWidth(), originalLabels.getHeight() );
		ImageStack is2 = calculateMismatchImage ? new ImageStack( originalLabels.getWidth(), originalLabels.getHeight()) : null;
		for(int i = 0; i < wrs.length; i ++)
		{
			result.warpingError += wrs[ i ].warpingError;
			is.addSlice("warped source slice " + (i+1), wrs[i].warpedSource.getProcessor() );
			if( calculateMismatchImage )
				is2.addSlice("Mismatches slice " + (i+1), wrs[i].classifiedMismatches.getProcessor() );
		}
		
		result.warpedSource = new ImagePlus ("warped source", is);
		if( calculateMismatchImage )
			result.classifiedMismatches = new ImagePlus( "Classified mismatches", is2);
		
		if(wrs.length != 0)		
			result.warpingError /= wrs.length;			
			
		return result;
	}
    	
} // end class WarpingError





