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

import trainableSegmentation.utils.Utils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * This class implements the adjusted Rand error, defined as the 1 - adjusted Rand index. 
 * We follow the Rand index definition described by Lawrence Hubert and Phipps Arabie \cite{Hubert85}.
 *
 * BibTeX:
 * <pre>
 * &#64;article{Hubert85,
 *   author    = {Lawrence Hubert and Phipps Arabie},
 *   title     = {Comparing partitions},
 *   journal   = {Journal of Classification},
 *   year      = {1985},
 *   volume    = {2},
 *   issue	   = {1},
 *   pages     = {193-218},
 *   doi       = {10.1007/BF01908075)
 * }
 * </pre>
 *
 */
public class AdjustedRandError extends Metrics
{

	/**
	 * Initialize ajusted Rand error metric.
	 * 
	 * @param originalLabels original labels (single 2D image or stack)
	 * @param proposedLabels proposed new labels (single 2D image or stack of the same as as the original labels)
	 */
	public AdjustedRandError(ImagePlus originalLabels, ImagePlus proposedLabels) {
		super(originalLabels, proposedLabels);
	}
	
	/**
	 * Calculate the Rand error in 2D between some original labels 
	 * and the corresponding proposed labels. Both image are binarized.
	 * The adjusted Rand error is defined as the 1 - adjusted Rand index, 
	 * as described by William M. Rand \cite{Rand71}.
	 *
	 * BibTeX:
	 * <pre>
	 * &#64;article{Rand71,
	 *   author    = {William M. Rand},
	 *   title     = {Objective criteria for the evaluation of clustering methods},
	 *   journal   = {Journal of the American Statistical Association},
	 *   year      = {1971},
	 *   volume    = {66},
	 *   number    = {336},
	 *   pages     = {846--850},
	 *   doi       = {10.2307/2284239)
	 * }
	 * </pre>
	 * 
	 * @param binaryThreshold threshold value to binarize proposal (larger than 0 and smaller than 1)
	 * @return adjusted Rand error
	 */
	public double getMetricValue(double binaryThreshold) 
	{
		final ImageStack labelSlices = originalLabels.getImageStack();
		final ImageStack proposalSlices = proposedLabels.getImageStack();

		double randError = 0;

		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<Double> > futures = new ArrayList< Future<Double> >();

		try{
			for(int i = 1; i <= labelSlices.getSize(); i++)
			{
				futures.add(exe.submit( getAdjustedRandErrorConcurrent(labelSlices.getProcessor(i).convertToFloat(),
											proposalSlices.getProcessor(i).convertToFloat(),										
											binaryThreshold ) ) );
			}

			// Wait for the jobs to be done
			for(Future<Double> f : futures)
			{
				randError += f.get();				
			}			
		}
		catch(Exception ex)
		{
			IJ.log("Error when calculating rand error in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return randError / labelSlices.getSize();
	}
	
	
	/**
	 * Calculate the adjusted Rand error between some 2D original labels 
	 * and the corresponding proposed labels. Both image are binarized.
	 * The adjusted Rand error is defined as the 1 - adjusted Rand index, 
	 * as described by Lawrence Hubert and Phipps Arabie \cite{Hubert85}.
	 *
	 * BibTeX:
	 * <pre>
	 * &#64;article{Hubert85,
	 *   author    = {Lawrence Hubert and Phipps Arabie},
	 *   title     = {Comparing partitions},
	 *   journal   = {Journal of Classification},
	 *   year      = {1985},
	 *   volume    = {2},
	 *   issue	   = {1},
	 *   pages     = {193-218},
	 *   doi       = {10.1007/BF01908075)
	 * }
	 * </pre>
	 * 
	 * @param label 2D image with the original labels
	 * @param proposal 2D image with the proposed labels
	 * @param binaryThreshold threshold value to binarize the input images
	 * @return adjusted Rand error
	 */
	public static double adjustedRandError(
			ImageProcessor label,
			ImageProcessor proposal,
			double binaryThreshold)
	{
		// Binarize inputs
		ByteProcessor binaryLabel = new ByteProcessor( label.getWidth(), label.getHeight() );
		ByteProcessor binaryProposal = new ByteProcessor( proposal.getWidth(), proposal.getHeight() );
		
		for(int x=0; x<label.getWidth(); x++)
			for(int y=0; y<label.getHeight(); y++)
			{
				binaryLabel.set(   x, y,    label.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
				binaryProposal.set(x, y, proposal.getPixelValue( x, y ) > binaryThreshold ? 255 : 0);
			}
		
		// Find components
		final ImagePlus im1 = new ImagePlus("binary labels", binaryLabel);
		//im1.show();
		ShortProcessor components1 = ( ShortProcessor ) Utils.connectedComponents(
				im1, 4).allRegions.getProcessor();
		
		final ImagePlus im2 = new ImagePlus("proposal labels", binaryProposal);
		//im2.show();
		ShortProcessor components2 = ( ShortProcessor ) Utils.connectedComponents(
				im2, 4).allRegions.getProcessor();
		
		return 1 - adjustedRandIndex( components1, components2 );
		
	}

	
	/**
	 * Get adjusted Rand error between two images in a concurrent way 
	 * (to be submitted to an Executor Service). Both images
	 * are binarized.
	 * The adjusted Rand error is defined as the 1 - adjusted Rand index, 
	 * as described by Lawrence Hubert and Phipps Arabie \cite{Hubert85}.
	 *
	 * BibTeX:
	 * <pre>
	 * &#64;article{Hubert85,
	 *   author    = {Lawrence Hubert and Phipps Arabie},
	 *   title     = {Comparing partitions},
	 *   journal   = {Journal of Classification},
	 *   year      = {1985},
	 *   volume    = {2},
	 *   issue	   = {1},
	 *   pages     = {193-218},
	 *   doi       = {10.1007/BF01908075)
	 * }
	 * </pre>
	 * 
	 * @param image1 first image
	 * @param image2 second image
	 * @param binaryThreshold threshold to apply to both images
	 * @return adjusted Rand error
	 */
	public Callable<Double> getAdjustedRandErrorConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<Double>()
		{
			public Double call()
			{				
				return adjustedRandError ( image1, image2, binaryThreshold );
			}
		};
	}
	
	

	
	/**
	 * Calculate the adjusted Rand index between to clusters, as described by
	 * Lawrence Hubert and Phipps Arabie \cite{Rand71}.
	 *
	 * BibTeX:
	 * <pre>
	 * &#64;article{Hubert85,
	 *   author    = {Lawrence Hubert and Phipps Arabie},
	 *   title     = {Comparing partitions},
	 *   journal   = {Journal of Classification},
	 *   year      = {1985},
	 *   volume    = {2},
	 *   issue	   = {1},
	 *   pages     = {193-218},
	 *   doi       = {10.1007/BF01908075)
	 * }
	 * </pre>
	 * 
	 * @param cluster1 2D segmented image (objects are labeled with different numbers) 
	 * @param cluster2 2D segmented image (objects are labeled with different numbers)
	 * @return adjusted Rand index
	 */
	public static double adjustedRandIndex(
			ShortProcessor cluster1,
			ShortProcessor cluster2)
	{		
		final short[] pixels1 = (short[]) cluster1.getPixels();
		final short[] pixels2 = (short[]) cluster2.getPixels();
		
		double n = pixels1.length;
		
		// Form contingency matrix
		int[][]cont = new int[(int) cluster1.getMax() ] [ (int) cluster2.getMax() ];
		
		for(int i=0; i<n; i++)
			cont[ pixels1[i] ] [ pixels2[i] ] ++;
		
		// sum over rows & columnns of nij^2
		double t2 = 0;
		
		// sum of squares of sums of rows
		double[] ni = new double[ cont.length ];
		for(int i=0; i<cont.length; i++)
			for(int j=0; j<cont[i].length; j++)			
				ni[ i ] += cont[ i ][ j ];
		double nis = 0;
		for(int k=0; k<ni.length; k++)
			nis += ni[ k ] * ni[ k ];
		
		// sum of squares of sums of columns
		double[] nj = new double[ cont.length ];
		for(int j=0; j<cont[0].length; j++)
			for(int i=0; i<cont.length; i++)
			{
				nj[ j ] += cont[ i ][ j ];
				t2 += cont[ i ][ j ] * cont[ i ][ j ];
			}
		double njs = 0;
		for(int k=0; k<nj.length; k++)
			njs += nj[ k ] * nj[ k ];
		
		// total number of pairs of entities
		double t1 =  n * (n - 1) / 2 ;
		
		double t3 = 0.5 * (nis+njs);
		
		//Expected index (for adjustment)
		double nc = ( n*(n*n+1) - (n+1)*nis - (n+1)*njs+2*(nis*njs)/n) / (2*(n-1) );
		
		double agreements=t1+t2-t3;		// number of agreements
		/*
		double D=  -t2+t3;		// number of disagreements
		
		double RI=agreements/t1;			// Rand 1971		%Probability of agreement
		double MI=D/t1;			// Mirkin 1970	%p(disagreement)
		double HI=(agreements-D)/t1;		// Hubert 1977	%p(agree)-p(disagree)
		*/
		
		//IJ.log("n = " + n + ", nis = " + nis + ", njs = " + njs);
		//IJ.log("t1 = " + t1 + ", t2 = " + t2 + ", t3 = " + t3);
		//IJ.log("nc = " + nc);
								
		double adjustedRandIndex;
		
		if ( t1 == nc )
		   adjustedRandIndex=0;			// avoid division by zero; if k=1, define Rand = 0
		else
		   adjustedRandIndex=(agreements-nc)/(t1-nc);		// adjusted Rand - Hubert & Arabie 1985
		
		return adjustedRandIndex;
		
	}	
	
}
