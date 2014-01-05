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
 * Authors: Ignacio Arganda-Carreras (iargandacarreras@gmail.com)
 */

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import trainableSegmentation.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * This class implements the variation of information metric, used
 * to compare clusters or segmentations. Two implementations are given, 
 * the classic one, and a foreground-restricted version that prunes
 * out the background pixels of the ground truth during the metric
 * calculation.
 */
public class VariationOfInformation extends Metrics
{	
	/**
	 * Initialize variation of information metric
	 * @param originalLabels original labels (single 2D image or stack)
	 * @param proposedLabels proposed new labels (single 2D image or stack of the same as as the original labels) 
	 */
	public VariationOfInformation(
			ImagePlus originalLabels,
			ImagePlus proposedLabels) 
	{
		super(originalLabels, proposedLabels);
	}

	/**
	 * Get variation of information between original
	 * and proposed labels for a given threshold
	 * 
	 * @param binaryThreshold threshold value to binarize labels
	 * @return metric value
	 */
	public double getMetricValue(double binaryThreshold) 
	{
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double vi = 0;

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<Double> > futures = new ArrayList< Future<Double> >();

		try{
			for(int i = 1; i <= labelSlices.getSize(); i++)
			{
				futures.add(exe.submit( getVIConcurrent(labelSlices.getProcessor(i).convertToFloat(),
											proposalSlices.getProcessor(i).convertToFloat(),										
											binaryThreshold ) ) );
			}

			// Wait for the jobs to be done
			for(Future<Double> f : futures)
			{
				vi += f.get();				
			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when calculating variation of information in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return vi / labelSlices.getSize();
	}
	
	/**
	 * Get foreground-restricted variation of information between original
	 * and proposed labels for a given threshold
	 * 
	 * @param binaryThreshold threshold value to binarize labels
	 * @return foreground-restricted metric value
	 */
	public double getForegroundRestrictedMetricValue(double binaryThreshold) 
	{
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double vi = 0;

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<Double> > futures = new ArrayList< Future<Double> >();

		try{
			for(int i = 1; i <= labelSlices.getSize(); i++)
			{
				futures.add(exe.submit( getForegroundRestrictedVIConcurrent(labelSlices.getProcessor(i).convertToFloat(),
											proposalSlices.getProcessor(i).convertToFloat(),										
											binaryThreshold ) ) );
			}

			// Wait for the jobs to be done
			for(Future<Double> f : futures)
			{
				vi += f.get();				
			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when calculating foreground-restricted variation of information in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return vi / labelSlices.getSize();
	}
	
	/**
	 * Get the minimum foreground-restricted metric value over a set of thresholds 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return minimum foreground-restricted value of the metric
	 */
	public double getMinimumForegroundRestrictedMetricValue(
			double minThreshold,
			double maxThreshold,
			double stepThreshold )
	{
		double min = 1.0;
		double bestTh = minThreshold;
		for(double th =  minThreshold; th <= maxThreshold; th += stepThreshold)
		{
			if( verbose ) 
				IJ.log("  Calculating foreground-restricted VI value for threshold " + String.format("%.3f", th) + "...");
			
			double error = getForegroundRestrictedMetricValue( th );
			if( min > error )
			{
				min = error;;
				bestTh = th;
			}
			if( verbose )
				IJ.log("    Error = " + error);
		}
		
		if( verbose )
			IJ.log(" ** Minimum foreground-restricted VI value = " + min + ", with threshold = " + bestTh + " **\n");
	    return min;
	}
	
	/**
	 * Calculate variation of information between two images in 
	 * a concurrent way (to be submitted to an Executor Service). 
	 * Both images are binarized.
	 *  
	 * @param image1 first image
	 * @param image2 second image
	 * @param binaryThreshold threshold to apply to both images
	 * @return variation of information
	 */
	public Callable<Double> getVIConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<Double>()
		{
			public Double call()
			{				
				return variationOfInformationN2 ( image1, image2, binaryThreshold );
			}
		};
	}
	
	/**
	 * Calculate foreground-restricted variation of information  
	 * between two images in a concurrent way (to be submitted  
	 * to an Executor Service). Both images are binarized.
	 *  
	 * @param image1 first image
	 * @param image2 second image
	 * @param binaryThreshold threshold to apply to both images
	 * @return foreground-restricted variation of information
	 */
	public Callable<Double> getForegroundRestrictedVIConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<Double>()
		{
			public Double call()
			{				
				return foregroundRestrictedVIN2( image1, image2, binaryThreshold );
			}
		};
	}
	

	/**
	 * Calculate variation of information with N^2 normalization
	 *  
	 * @param label original labels
	 * @param proposal proposed labels (usually a probability image to be thresholded)
	 * @param binaryThreshold threshold value to binarize proposal
	 * @return variation of information between original labels and the binarized proposal
	 */
	public double variationOfInformationN2(
			ImageProcessor label,
			ImageProcessor proposal,
			double binaryThreshold)
	{
		// Binarize inputs
		ByteProcessor binaryLabel = new ByteProcessor( label.getWidth(), label.getHeight() );
		ByteProcessor binaryProposal = new ByteProcessor( label.getWidth(), label.getHeight() );
		
		for(int x=0; x<label.getWidth(); x++)
			for(int y=0; y<label.getHeight(); y++)
			{
				binaryLabel.set(   x, y,    label.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
				binaryProposal.set(x, y, proposal.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
			}
		
		// Find components
		ShortProcessor components1 = ( ShortProcessor ) Utils.connectedComponents(
				new ImagePlus("binary labels", binaryLabel), 4).allRegions.getProcessor();
		
		ShortProcessor components2 = ( ShortProcessor ) Utils.connectedComponents(
				new ImagePlus("proposal labels", binaryProposal), 4).allRegions.getProcessor();
		
		return getVI( components1, components2 );
		
	}
	
	/**
	 * Calculate foreground-restricted variation of information 
	 * with N^2 normalization
	 *  
	 * @param label original labels
	 * @param proposal proposed labels (usually a probability image to be thresholded)
	 * @param binaryThreshold threshold value to binarize proposal
	 * @return foreground-restricted variation of information between original labels and the binarized proposal
	 */
	public double foregroundRestrictedVIN2(
			ImageProcessor label,
			ImageProcessor proposal,
			double binaryThreshold)
	{
		// Binarize inputs
		ByteProcessor binaryLabel = new ByteProcessor( label.getWidth(), label.getHeight() );
		ByteProcessor binaryProposal = new ByteProcessor( label.getWidth(), label.getHeight() );
		
		for(int x=0; x<label.getWidth(); x++)
			for(int y=0; y<label.getHeight(); y++)
			{
				binaryLabel.set(   x, y,    label.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
				binaryProposal.set(x, y, proposal.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
			}
		
		// Find components
		ShortProcessor components1 = ( ShortProcessor ) Utils.connectedComponents(
				new ImagePlus("binary labels", binaryLabel), 4).allRegions.getProcessor();
		
		ShortProcessor components2 = ( ShortProcessor ) Utils.connectedComponents(
				new ImagePlus("proposal labels", binaryProposal), 4).allRegions.getProcessor();
		
		return foregroundRestrictedVI( components1, components2 );
		
	}

	/**
	 * Calculate the variation of information between two clusters
	 * (N^2 normalization)
	 * 
	 * @param cluster1 labels of cluster 1 (ground truth)
	 * @param cluster2 labels of cluster 2 (proposal)
	 * @return variation of information value
	 */
	public double getVI(
			ShortProcessor cluster1,
			ShortProcessor cluster2)
	{
		final short[] pixels1 = (short[]) cluster1.getPixels();
		final short[] pixels2 = (short[]) cluster2.getPixels();
		
		//(new ImagePlus("cluster 1", cluster1)).show();
		//(new ImagePlus("cluster 2", cluster2)).show();
		
		double n = pixels1.length;
		
		
		// reset min and max of the cluster processors 
		// (needed in order to have correct min-max values)
		cluster1.resetMinAndMax();
		cluster2.resetMinAndMax();
		
		int nLabelsA = (int) cluster1.getMax();
		int nLabelsB = (int) cluster2.getMax();
		
		// compute overlap matrix
		double[][]pij = new double[ nLabelsA + 1] [ nLabelsB + 1];
		for(int i=0; i<n; i++)									
			pij[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
		
		for( int i=0; i < (nLabelsA + 1); i++ )
			for( int j=0; j < (nLabelsB + 1); j++ )
				pij[ i ][ j ] /= n;
		
		// sum of squares of sums of rows
		double[] ai = new double[ pij.length ];
		for(int i=0; i<pij.length; i++)
			for(int j=0; j<pij[0].length; j++)
			{
				ai[ i ] += pij[ i ][ j ];				
			}

		// sum of squares of sums of columns
		double[] bj = new double[ pij[0].length ];
		for(int j=0; j<pij[0].length; j++)
			for(int i=0; i<pij.length; i++)
			{
				bj[ j ] += pij[ i ][ j ];
			}

		// ai, bj and pij are the same in the Rand index calculation,
		// here they are used in a different way to express the variation
		// of information
		
		// In matlab:
		// aux = a_i .* log(a_i);
		// sumA = sum(aux(~isnan(aux)));
		double sumA = 0;
		for(int i=0; i<ai.length; i++)
		{
			if( ai[ i ] != 0 )
				sumA += ai[ i ] * Math.log( ai[ i ] );
		}

		// In matlab:
		// aux = b_j .* log(b_j);
		// sumB = sum(aux(~isnan(aux)));
		double sumB = 0;
		for(int j=0; j<bj.length; j++)
			if( bj[ j ] != 0 )
				sumB += bj[ j ] * Math.log( bj[ j ] );

		// In matlab:
		// aux = p_ij .* log(p_ij);
		// s = sum(aux(~isnan(aux)));
		// if isempty( s )
		//    s = 0;
		// end
		// sumAB = s
		double sumAB = 0;
		for(int i=1; i<pij.length; i++)
			for(int j=1; j<pij[0].length; j++)
			{
				if( pij[ i ][ j ] != 0 )
					sumAB += pij[ i ][ j ] * Math.log( pij[ i ][ j ] );
			}
		
		// vi = full(sumA + sumB - 2*sumAB);
		return sumA + sumB - 2.0 * sumAB;
	}
	
	/**
	 * Calculate the variation of information between two clusters using 
	 * the foreground restriction, i.e. pruning out the zero component in 
	 * the labeling (un-assigned "out" space)
	 * 
	 * @param cluster1 labels of cluster 1 (ground truth)
	 * @param cluster2 labels of cluster 2 (proposal)
	 * @return foreground-restricted variation of information value
	 */
	public double foregroundRestrictedVI(
			ShortProcessor cluster1,
			ShortProcessor cluster2)
	{
		final short[] pixels1 = (short[]) cluster1.getPixels();
		final short[] pixels2 = (short[]) cluster2.getPixels();
		
		//(new ImagePlus("cluster 1", cluster1)).show();
		//(new ImagePlus("cluster 2", cluster2)).show();
		
		double n = pixels1.length;
		
		
		// reset min and max of the cluster processors 
		// (needed in order to have correct min-max values)
		cluster1.resetMinAndMax();
		cluster2.resetMinAndMax();
		
		int nLabelsA = (int) cluster1.getMax();
		int nLabelsB = (int) cluster2.getMax();
		
		// compute overlap matrix
		double[][]pij = new double[ nLabelsA + 1] [ nLabelsB + 1];
		for(int i=0; i<n; i++)									
			pij[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
		
		for( int i=0; i < (nLabelsA + 1); i++ )
			for( int j=0; j < (nLabelsB + 1); j++ )
				pij[ i ][ j ] /= n;
		
		// sum of squares of sums of rows
		// (skip background objects in the first cluster)
		double[] ai = new double[ pij.length ];
		for(int i=1; i<pij.length; i++)
			for(int j=0; j<pij[0].length; j++)
			{
				ai[ i ] += pij[ i ][ j ];				
			}

		// sum of squares of sums of columns
		// (prune out the zero component in the labeling (un-assigned "out" space))
		double[] bj = new double[ pij[0].length ];
		for(int j=1; j<pij[0].length; j++)
			for(int i=1; i<pij.length; i++)
			{
				bj[ j ] += pij[ i ][ j ];
			}

		double[] pi0 = new double[ pij.length ];
		double aux = 0;
		for(int i=1; i<pij.length; i++)
		{
			pi0[ i ] = pij[ i ][ 0 ];
			aux += pi0[ i ];
		}

		// ai, bj and pij are the same in the Rand index calculation,
		// here they are used in a different way to express the variation
		// of information
		
		// In matlab:
		// aux = a_i .* log(a_i);
		// sumA = sum(aux(~isnan(aux)));
		double sumA = 0;
		for(int i=0; i<ai.length; i++)
		{
			if( ai[ i ] != 0 )
				sumA += ai[ i ] * Math.log( ai[ i ] );
		}

		// In matlab:
		// aux = b_j .* log(b_j);
		// sumB = sum(aux(~isnan(aux))) - sum(p_i0)*log(n);
		double sumB = 0;
		for(int j=0; j<bj.length; j++)
			if( bj[ j ] != 0 )
				sumB += bj[ j ] * Math.log( bj[ j ] );
		sumB -= aux * Math.log( n );


		// In matlab:
		// aux = p_ij .* log(p_ij);
		// s = sum(aux(~isnan(aux)));
		// if isempty( s )
		//    s = 0;
		// end
		// sumAB = s - sum(p_i0)*log(n);
		double sumAB = 0;
		for(int i=1; i<pij.length; i++)
			for(int j=1; j<pij[0].length; j++)
			{
				if( pij[ i ][ j ] != 0 )
					sumAB += pij[ i ][ j ] * Math.log( pij[ i ][ j ] );
			}

		sumAB -= aux * Math.log( n );
		
		// vi = full(sumA + sumB - 2*sumAB);
		return sumA + sumB - 2.0 * sumAB;
	}
	
	/**
	 * Get the best F-score of the foreground-restricted variation of information 
	 * over a set of thresholds 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return maximal F-score of the foreground-restricted variation of information
	 */
	public double getForegroundRestrictedMaximalFScore(
			double minThreshold,
			double maxThreshold,
			double stepThreshold)
	{
		ArrayList< Double > fscores = getForegroundRestrictedFscores( minThreshold, maxThreshold, stepThreshold );
	    // trainableSegmentation.utils.Utils.plotPrecisionRecall( stats );    
	    double maxFScore = 0;

	    for(double f : fscores)
	    {
	    	if ( f > maxFScore)
	    		maxFScore = f;
	    }	    
	    return maxFScore;
	}
	
	/**
	 * Get the best F-score of the variation of information 
	 * over a set of thresholds 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return maximal F-score of the variation of information
	 */
	public double getMaximalFScore(
			double minThreshold,
			double maxThreshold,
			double stepThreshold)
	{
		ArrayList< Double > fscores = getFscores( minThreshold, maxThreshold, stepThreshold );
	    // trainableSegmentation.utils.Utils.plotPrecisionRecall( stats );    
	    double maxFScore = 0;

	    for(double f : fscores)
	    {
	    	if ( f > maxFScore)
	    		maxFScore = f;
	    }	    
	    return maxFScore;
	}


	/**
	 * Get foreground-restricted F-score of the variation of
	 * information over a set of thresholds
	 * 
	 * @param minThreshold minimum threshold value to check (included)
	 * @param maxThreshold maximum threshold value to check (included)
	 * @param stepThreshold step threshold value
	 * @return list with the F-score value of each threshold
	 */
	public ArrayList< Double > getForegroundRestrictedFscores(
			double minThreshold,
			double maxThreshold, 
			double stepThreshold) 
	{
		if( minThreshold < 0 || minThreshold > maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return null;
		}
		
		double bestFscore = 0;
		double bestTh = minThreshold;
		
		final ArrayList< Double > fscores = new ArrayList< Double >();
		
		for(double th = minThreshold; th <= maxThreshold; th += stepThreshold)
		{
			if( verbose ) 
				IJ.log("  Calculating foreground-restricted variation of information F-score for threshold value " + String.format("%.3f", th) + "...");
			final double fScore = getForegroundRestrictedFscore( th );
			fscores.add( fScore );
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
		
		return fscores;
		
	}

	/**
	 * Get F-score of the variation of
	 * information over a set of thresholds
	 * 
	 * @param minThreshold minimum threshold value to check (included)
	 * @param maxThreshold maximum threshold value to check (included)
	 * @param stepThreshold step threshold value
	 * @return list with the F-score value of each threshold
	 */
	public ArrayList< Double > getFscores(
			double minThreshold,
			double maxThreshold, 
			double stepThreshold) 
	{
		if( minThreshold < 0 || minThreshold > maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return null;
		}
		
		double bestFscore = 0;
		double bestTh = minThreshold;
		
		final ArrayList< Double > fscores = new ArrayList< Double >();
		
		for(double th = minThreshold; th <= maxThreshold; th += stepThreshold)
		{
			if( verbose ) 
				IJ.log("  Calculating variation of information F-score for threshold value " + String.format("%.3f", th) + "...");
			final double fScore = getFscore( th );
			fscores.add( fScore );
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
		
		return fscores;
		
	}
	
	/**
	 * Get F-score of the foreground-restricted variation of information for a
	 * given threshold of the proposal labels. Done in 2d, for stacks, the average
	 * F-score is calculated.
	 *  
	 * @param th threshold value to binarize proposal
	 * @return F-score of the foreground-restricted variation of information
	 */
	public double getForegroundRestrictedFscore( double th ) 
	{
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double fScore = 0;

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<Double> > futures = new ArrayList< Future<Double> >();

		try{
			for(int i = 1; i <= labelSlices.getSize(); i++)
			{
				futures.add(exe.submit( getforegroundRestrictedFscoreConcurrent(labelSlices.getProcessor(i).convertToFloat(),
											proposalSlices.getProcessor(i).convertToFloat(),										
											th ) ) );
			}

			// Wait for the jobs to be done
			for(Future<Double> f : futures)
			{
				fScore += f.get();				
			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when calculating the F-score of variation of information in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return fScore / labelSlices.getSize();
	}
	
	/**
	 * Get F-score of the variation of information for a
	 * given threshold of the proposal labels. Done in 2d, 
	 * for stacks, the average F-score is calculated.
	 *  
	 * @param th threshold value to binarize proposal
	 * @return F-score of the variation of information
	 */
	public double getFscore( double th ) 
	{
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double fScore = 0;

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<Double> > futures = new ArrayList< Future<Double> >();

		try{
			for(int i = 1; i <= labelSlices.getSize(); i++)
			{
				futures.add(exe.submit( getFscoreConcurrent(labelSlices.getProcessor(i).convertToFloat(),
											proposalSlices.getProcessor(i).convertToFloat(),										
											th ) ) );
			}

			// Wait for the jobs to be done
			for(Future<Double> f : futures)
			{
				fScore += f.get();				
			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when calculating the F-score of variation of information in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return fScore / labelSlices.getSize();
	}

	/**
	 * Get F-score of the variation of information in a concurrent way.
	 * To be submitted to an ExecutorService.
	 * 
	 * @param image1 ground truth (usually binary labels)
	 * @param image2 proposed labels (usually a probability map to binarize) 
	 * @param binaryThreshold threshold value to binarize proposal
	 * @return F-score of the variation of information (N^2 normalization)
	 */
	public Callable<Double> getFscoreConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<Double>()
		{
			public Double call()
			{				
				return fScoreN2 ( image1, image2, binaryThreshold );
			}
		};
	}
	
	/**
	 * Get F-score of the foreground-restricted variation of information 
	 * in a concurrent way. To be submitted to an ExecutorService.
	 * 
	 * @param image1 ground truth (usually binary labels)
	 * @param image2 proposed labels (usually a probability map to binarize) 
	 * @param binaryThreshold threshold value to binarize proposal
	 * @return F-score of the variation of information (foreground-restricted and N^2 normalization)
	 */
	public Callable<Double> getforegroundRestrictedFscoreConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<Double>()
		{
			public Double call()
			{				
				return foregroundRestrictedFScoreN2 ( image1, image2, binaryThreshold );
			}
		};
	}
	

	/**
	 * Calculate F-score of variation of information with N^2 normalization
	 *  
	 * @param label original labels
	 * @param proposal proposed labels (usually a probability image to be thresholded)
	 * @param binaryThreshold threshold value to binarize proposal
	 * @return F-score of the variation of information between original labels and the binarized proposal
	 */
	public double fScoreN2(
			ImageProcessor label,
			ImageProcessor proposal,
			double binaryThreshold)
	{
		// Binarize inputs
		ByteProcessor binaryLabel = new ByteProcessor( label.getWidth(), label.getHeight() );
		ByteProcessor binaryProposal = new ByteProcessor( label.getWidth(), label.getHeight() );
		
		for(int x=0; x<label.getWidth(); x++)
			for(int y=0; y<label.getHeight(); y++)
			{
				binaryLabel.set(   x, y,    label.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
				binaryProposal.set(x, y, proposal.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
			}
		
		// Find components
		ShortProcessor components1 = ( ShortProcessor ) Utils.connectedComponents(
				new ImagePlus("binary labels", binaryLabel), 4).allRegions.getProcessor();
		
		ShortProcessor components2 = ( ShortProcessor ) Utils.connectedComponents(
				new ImagePlus("proposal labels", binaryProposal), 4).allRegions.getProcessor();
		
		return fscore( components1, components2 );
		
	}

	/**
	 * Calculate F-score of foreground-restricted variation of 
	 * information with N^2 normalization
	 *  
	 * @param label original labels
	 * @param proposal proposed labels (usually a probability image to be thresholded)
	 * @param binaryThreshold threshold value to binarize proposal
	 * @return F-score of the variation of information between original labels and the binarized proposal
	 */
	public double foregroundRestrictedFScoreN2(
			ImageProcessor label,
			ImageProcessor proposal,
			double binaryThreshold)
	{
		// Binarize inputs
		ByteProcessor binaryLabel = new ByteProcessor( label.getWidth(), label.getHeight() );
		ByteProcessor binaryProposal = new ByteProcessor( label.getWidth(), label.getHeight() );
		
		for(int x=0; x<label.getWidth(); x++)
			for(int y=0; y<label.getHeight(); y++)
			{
				binaryLabel.set(   x, y,    label.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
				binaryProposal.set(x, y, proposal.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
			}
		
		// Find components
		ShortProcessor components1 = ( ShortProcessor ) Utils.connectedComponents(
				new ImagePlus("binary labels", binaryLabel), 4).allRegions.getProcessor();
		
		ShortProcessor components2 = ( ShortProcessor ) Utils.connectedComponents(
				new ImagePlus("proposal labels", binaryProposal), 4).allRegions.getProcessor();
		
		return foregroundRestrictedFscore( components1, components2 );
		
	}
	
	/**
	 * Calculate the F-score of the variation of information between two clusters
	 * using the foreground restriction, i.e. pruning out the zero component in 
	 * the labeling (un-assigned "out" space)
	 * 
	 * @param cluster1 labels of cluster 1 (ground truth)
	 * @param cluster2 labels of cluster 2 (proposal)
	 * @return F-score of the variation of information
	 */
	public double foregroundRestrictedFscore(
			ShortProcessor cluster1,
			ShortProcessor cluster2)
	{
		final short[] pixels1 = (short[]) cluster1.getPixels();
		final short[] pixels2 = (short[]) cluster2.getPixels();
		
		//(new ImagePlus("cluster 1", cluster1)).show();
		//(new ImagePlus("cluster 2", cluster2)).show();
		
		double n = pixels1.length;
		
		
		// reset min and max of the cluster processors 
		// (needed in order to have correct min-max values)
		cluster1.resetMinAndMax();
		cluster2.resetMinAndMax();
		
		int nLabelsA = (int) cluster1.getMax();
		int nLabelsB = (int) cluster2.getMax();
		
		// compute overlap matrix
		double[][]pij = new double[ nLabelsA + 1] [ nLabelsB + 1];
		for(int i=0; i<n; i++)									
			pij[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
		
		for( int i=0; i < (nLabelsA + 1); i++ )
			for( int j=0; j < (nLabelsB + 1); j++ )
				pij[ i ][ j ] /= n;
		
		// sum of squares of sums of rows
		// (skip background objects in the first cluster)
		double[] ai = new double[ pij.length ];
		for(int i=1; i<pij.length; i++)
			for(int j=0; j<pij[0].length; j++)
			{
				ai[ i ] += pij[ i ][ j ];				
			}

		// sum of squares of sums of columns
		// (prune out the zero component in the labeling (un-assigned "out" space))
		double[] bj = new double[ pij[0].length ];
		for(int j=1; j<pij[0].length; j++)
			for(int i=1; i<pij.length; i++)
			{
				bj[ j ] += pij[ i ][ j ];
			}

		double[] pi0 = new double[ pij.length ];
		double aux = 0;
		for(int i=1; i<pij.length; i++)
		{
			pi0[ i ] = pij[ i ][ 0 ];
			aux += pi0[ i ];
		}

		// ai, bj and pij are the same in the Rand index calculation,
		// here they are used in a different way to express the variation
		// of information
		
		// In matlab:
		// aux = a_i .* log(a_i);
		// sumA = sum(aux(~isnan(aux)));
		double sumA = 0;
		for(int i=0; i<ai.length; i++)
		{
			if( ai[ i ] != 0 )
				sumA += ai[ i ] * Math.log( ai[ i ] );
		}

		// In matlab:
		// aux = b_j .* log(b_j);
		// sumB = sum(aux(~isnan(aux))) - sum(p_i0)*log(n);
		double sumB = 0;
		for(int j=0; j<bj.length; j++)
			if( bj[ j ] != 0 )
				sumB += bj[ j ] * Math.log( bj[ j ] );
		sumB -= aux * Math.log( n );


		// In matlab:
		// aux = p_ij .* log(p_ij);
		// s = sum(aux(~isnan(aux)));
		// if isempty( s )
		//    s = 0;
		// end
		// sumAB = s - sum(p_i0)*log(n);
		double sumAB = 0;
		for(int i=1; i<pij.length; i++)
			for(int j=1; j<pij[0].length; j++)
			{
				if( pij[ i ][ j ] != 0 )
					sumAB += pij[ i ][ j ] * Math.log( pij[ i ][ j ] );
			}

		sumAB -= aux * Math.log( n );
		
		// H(A|B)
		double hab = sumB - sumAB;
		// H(B|A)
		double hba = sumA - sumAB;
		// H(A)
		double ha = -sumA;
		// H(B)
		double hb = -sumB;
		
		// An information theoretic analog of Rand recall 
		// is the asymmetrically normalized mutual information
		// C(A|B)
		double rec = (ha - hab) / ha;
		
		// An information theoretic analog of Rand precision 
		// is defined similarly
		// C(B|A)
		double prec = (hb - hba) / hb;

		if( rec == 0 )
		    prec = 1.0;
				
		// F-score
		return 2.0 * prec * rec / (prec + rec);
	}

	/**
	 * Calculate the F-score of the variation of information between two clusters
	 * A and B ( harmonic mean of C(A|B) and C(B|A) )
	 * 
	 * @param cluster1 labels of cluster 1 (ground truth)
	 * @param cluster2 labels of cluster 2 (proposal)
	 * @return F-score of the variation of information
	 */
	public double fscore(
			ShortProcessor cluster1,
			ShortProcessor cluster2)
	{
		final short[] pixels1 = (short[]) cluster1.getPixels();
		final short[] pixels2 = (short[]) cluster2.getPixels();
		
		//(new ImagePlus("cluster 1", cluster1)).show();
		//(new ImagePlus("cluster 2", cluster2)).show();
		
		double n = pixels1.length;
		
		
		// reset min and max of the cluster processors 
		// (needed in order to have correct min-max values)
		cluster1.resetMinAndMax();
		cluster2.resetMinAndMax();
		
		int nLabelsA = (int) cluster1.getMax();
		int nLabelsB = (int) cluster2.getMax();
		
		// compute overlap matrix
		double[][]pij = new double[ nLabelsA + 1] [ nLabelsB + 1];
		for(int i=0; i<n; i++)									
			pij[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
		
		for( int i=0; i < (nLabelsA + 1); i++ )
			for( int j=0; j < (nLabelsB + 1); j++ )
				pij[ i ][ j ] /= n;
		
		// sum of squares of sums of rows
		double[] ai = new double[ pij.length ];
		for(int i=0; i<pij.length; i++)
			for(int j=0; j<pij[0].length; j++)
			{
				ai[ i ] += pij[ i ][ j ];				
			}

		// sum of squares of sums of columns
		double[] bj = new double[ pij[0].length ];
		for(int j=0; j<pij[0].length; j++)
			for(int i=0; i<pij.length; i++)
			{
				bj[ j ] += pij[ i ][ j ];
			}

		// ai, bj and pij are the same in the Rand index calculation,
		// here they are used in a different way to express the variation
		// of information
		
		// In matlab:
		// aux = a_i .* log(a_i);
		// sumA = sum(aux(~isnan(aux)));
		double sumA = 0;
		for(int i=0; i<ai.length; i++)
		{
			if( ai[ i ] != 0 )
				sumA += ai[ i ] * Math.log( ai[ i ] );
		}

		// In matlab:
		// aux = b_j .* log(b_j);
		// sumB = sum(aux(~isnan(aux)));
		double sumB = 0;
		for(int j=0; j<bj.length; j++)
			if( bj[ j ] != 0 )
				sumB += bj[ j ] * Math.log( bj[ j ] );

		// In matlab:
		// aux = p_ij .* log(p_ij);
		// s = sum(aux(~isnan(aux)));
		// if isempty( s )
		//    s = 0;
		// end
		// sumAB = s;
		double sumAB = 0;
		for(int i=1; i<pij.length; i++)
			for(int j=1; j<pij[0].length; j++)
			{
				if( pij[ i ][ j ] != 0 )
					sumAB += pij[ i ][ j ] * Math.log( pij[ i ][ j ] );
			}
		
		// H(A|B)
		double hab = sumB - sumAB;
		// H(B|A)
		double hba = sumA - sumAB;
		// H(A)
		double ha = -sumA;
		// H(B)
		double hb = -sumB;
		
		// An information theoretic analog of Rand recall 
		// is the asymmetrically normalized mutual information
		// C(A|B)
		double rec = (ha - hab) / ha;
		
		// An information theoretic analog of Rand precision 
		// is defined similarly
		// C(B|A)
		double prec = (hb - hba) / hb;

		if( rec == 0 )
		    prec = 1.0;
				
		// F-score
		return 2.0 * prec * rec / (prec + rec);
	}
	
}
