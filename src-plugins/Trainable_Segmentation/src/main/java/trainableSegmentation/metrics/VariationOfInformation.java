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
 * to compare clusters or segmentations.
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
	 * Calculate variation of information with N^2 normalization
	 *  
	 * @param label
	 * @param proposal
	 * @param binaryThreshold
	 * @return
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
		
		return variationOfInformationForegroundRestricted( components1, components2 );
		
	}

	public double variationOfInformationForegroundRestricted(
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
	

}
