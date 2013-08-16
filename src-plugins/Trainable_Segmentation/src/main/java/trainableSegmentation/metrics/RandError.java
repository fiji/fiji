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
 * This class implements the Rand error metric.
 * The Rand error is defined as the 1 - Rand index. We follow the
 * definition of Rand index as described by William M. Rand \cite{Rand71}.
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
 */
public class RandError extends Metrics
{
	/** boolean flag to set the level of detail on the standard output messages */
	private boolean verbose = true;

	/**
	 * Initialize Rand error metric
	 * @param originalLabels original labels (single 2D image or stack)
	 * @param proposedLabels proposed new labels (single 2D image or stack of the same as as the original labels) 
	 */
	public RandError(ImagePlus originalLabels, ImagePlus proposedLabels) 
	{
		super(originalLabels, proposedLabels);
	}

	
	/**
	 * Calculate the Rand error in 2D between some original labels 
	 * and the corresponding proposed labels. Both image are binarized.
	 * The Rand error is defined as the 1 - Rand index, as described by
	 * William M. Rand \cite{Rand71}.
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
	 * @return Rand error
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
				futures.add(exe.submit( getRandErrorConcurrent(labelSlices.getProcessor(i).convertToFloat(),
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
	 * Calculate the Rand index and its derived statistics in 2D between 
	 * some original labels and the corresponding proposed labels. Both images 
	 * are binarized. We follow the definition of Rand index described by
	 * William M. Rand \cite{Rand71}.
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
	 * @return Rand index value and derived satatistics
	 */
	public ClassificationStatistics getRandIndexStats( double binaryThreshold )
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
				futures.add(exe.submit( getRandIndexStatsConcurrent(labelSlices.getProcessor(i).convertToFloat(),
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
			IJ.log("Error when calculating rand error in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return new ClassificationStatistics( tp, tn, fp, fn, randIndex / labelSlices.getSize() );
	}
	
	/**
	 * Calculate the precision-recall values based on Rand index between 
	 * some 2D original labels and the corresponding proposed labels. 
	 * We follow the definition of Rand index as described by
	 * William M. Rand \cite{Rand71}.
	 *
	 * BibTeX:
	 * <pre>
	 * &#64;article{Rand71,label
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
		
		if( minThreshold < 0 || minThreshold > maxThreshold || maxThreshold > 1)
		{
			IJ.log("Error: unvalid threshold values.");
			return null;
		}
		
		ArrayList< ClassificationStatistics > cs = new ArrayList<ClassificationStatistics>();
		
		double bestFscore = 0;
		double bestTh = minThreshold;
		
		for(double th = minThreshold; th <= maxThreshold; th += stepThreshold)
		{
			if( verbose ) 
				IJ.log("  Calculating Rand index statistics for threshold value " + String.format("%.3f", th) + "...");
			cs.add( getRandIndexStats( th ));
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
	 * Get Rand error between two images in a concurrent way 
	 * (to be submitted to an Executor Service). Both images
	 * are binarized.
	 * The Rand error is defined as the 1 - Rand index, as described by
	 * William M. Rand \cite{Rand71}.
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
	 * @param image1 first image
	 * @param image2 second image
	 * @param binaryThreshold threshold to apply to both images
	 * @return Rand error
	 */
	public Callable<Double> getRandErrorConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<Double>()
		{
			public Double call()
			{				
				return randError ( image1, image2, binaryThreshold );
			}
		};
	}

	/**
	 * Get Rand index value and derived statistics between two images 
	 * in a concurrent way (to be submitted to an Executor Service). 
	 * Both images are binarized.
	 * We follow the Rand index definition described by William M. Rand \cite{Rand71}.
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
	 * @param image1 first image
	 * @param image2 second image
	 * @param binaryThreshold threshold to apply to both images
	 * @return Rand index value and derived statistics
	 */
	public  Callable<ClassificationStatistics> getRandIndexStatsConcurrent(
			final ImageProcessor image1, 
			final ImageProcessor image2,
			final double binaryThreshold) 
	{
		return new Callable<ClassificationStatistics>()
		{
			public ClassificationStatistics call()
			{				
				return randIndexStats( image1, image2, binaryThreshold );
			}
		};
	}
	
	/**
	 * Calculate the Rand error between some 2D original labels 
	 * and the corresponding proposed labels. Both image are binarized.
	 * The Rand error is defined as the 1 - Rand index, as described by
	 * William M. Rand \cite{Rand71}.
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
	 * @param label 2D image with the original labels
	 * @param proposal 2D image with the proposed labels
	 * @param binaryThreshold threshold value to binarize the input images
	 * @return Rand error
	 */
	public double randError(
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
		
		return 1 - randIndex( components1, components2 );
		
	}
	
	/**
	 * Calculate the Rand index between some 2D original labels 
	 * and the corresponding proposed labels. Both image are binarized.
	 * We follow the definition of Rand index as described by
	 * William M. Rand \cite{Rand71}.
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
	 * @param label 2D image with the original labels
	 * @param proposal 2D image with the proposed labels
	 * @param binaryThreshold threshold value to binarize the input images
	 * @return rand index value and derived statistics
	 */
	public  ClassificationStatistics randIndexStats(
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
		
		return getRandIndexStats( components1, components2 );		
	}
	
	
	/**
	 * Calculate the Rand index between to clusters, as described by
	 * William M. Rand \cite{Rand71}. Note that this version of the
	 * Rand index treats the zero component (background) as another 
	 * object.
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
	 * @param cluster1 2D segmented image (objects are labeled with different numbers) 
	 * @param cluster2 2D segmented image (objects are labeled with different numbers)
	 * @return Rand index
	 */
	public double classicRandIndex(
			ShortProcessor cluster1,
			ShortProcessor cluster2)
	{
		final short[] pixels1 = (short[]) cluster1.getPixels();
		final short[] pixels2 = (short[]) cluster2.getPixels();
		
		double n = pixels1.length;
		
		// Form contingency matrix
		int[][]cont = new int[(int) cluster1.getMax() ] [ (int) cluster2.getMax() ];
		
		for(int i=0; i<n; i++)
			cont[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
		
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
		
		double agreements=t1+t2-t3;		// number of agreements
		
		return agreements/t1;
	}
	
	/**
	 * Calculate the Rand index between to clusters, as described by
	 * William M. Rand \cite{Rand71}, but pruning out the zero component in 
	 * the ground truth (cluster 1).
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
	 * @param cluster1 ground truth, 2D segmented image (objects are labeled with different numbers) 
	 * @param cluster2 prediction, 2D segmented image (objects are labeled with different numbers)
	 * @return Rand index
	 */
	public double randIndex(
			ShortProcessor cluster1,
			ShortProcessor cluster2)
	{
		final short[] pixels1 = (short[]) cluster1.getPixels();
		final short[] pixels2 = (short[]) cluster2.getPixels();
		
		//(new ImagePlus("cluster 1", cluster1)).show();
		//(new ImagePlus("cluster 2", cluster2)).show();
		
		double nPixels = pixels1.length;
		
		// number of pixels that are "in" (not background) in
		// cluster number 1 (ground truth)
		double n = 0;
		
		// reset min and max of the cluster processors 
		// (needed in order to have correct min-max values)
		cluster1.resetMinAndMax();
		cluster2.resetMinAndMax();
		
		// Form the contingency matrix
		int[][]cont = new int[(int) cluster1.getMax() + 1] [ (int) cluster2.getMax() + 1];		

		for(int i=0; i<nPixels; i++)
		{						
			cont[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
			if( pixels1[ i ] > 0)
				n++;
		}

		// sum of squares of sums of rows
		// (skip background objects in the first cluster)
		double[] ni = new double[ cont.length ];
		for(int i=1; i<cont.length; i++)
			for(int j=0; j<cont[0].length; j++)
			{
				ni[ i ] += cont[ i ][ j ];				
			}

		// sum of squares of sums of columns
		// (prune out the zero component in the labeling (un-assigned "out" space))
		double[] nj = new double[ cont[0].length ];
		for(int j=1; j<cont[0].length; j++)
			for(int i=1; i<cont.length; i++)
			{
				nj[ j ] += cont[ i ][ j ];
			}
		
		// true positives - type (i): objects in the pair are placed in the 
		// same class in cluster1 and in the same class in claster2
		// (prune out the zero component in the labeling (un-assigned "out" space))
		double truePositives = 0;
		for(int j=1; j<cont[0].length; j++)
			for(int i=1; i<cont.length; i++)			
				truePositives += cont[ i ][ j ] * ( cont[ i ][ j ] - 1.0 ) / 2.0;			
			
		// total number of pairs (after pruning background pixels
		// of the ground truth)
		double nPairsTotal = n * (n-1.0) / 2.0 ;

		// total number of positive samples in ground truth
		double nPosTrue = 0;
		for(int k=0; k<ni.length; k++)
			nPosTrue += ni[ k ] * (ni[ k ]-1.0) /2.0;

		// number of pairs actually classified as positive (in the prediction)
		double nPosActual = 0;
		for(int k=0; k<nj.length; k++)
			nPosActual += nj[ k ] * (nj[ k ]-1.0)/2.0;					
				
		// true negatives - type (ii): objects in the pair are placed in different 
		// classes in cluster1 and in different classes in claster2
		// trueNegatives = 	nNegTrue - falsePositives = (nPairsTotal - nPosTrue) - (nPosActual - truePositives)	
		double trueNegatives = nPairsTotal + truePositives - nPosTrue - nPosActual;
		
		double agreements = truePositives + trueNegatives;	// number of agreements
		
		double randIndex = agreements / nPairsTotal;
		
		return randIndex;
	}
	
	
	/**
	 * Calculate the Rand index between to 3D clusters, as described by
	 * William M. Rand \cite{Rand71}, but pruning out the zero component of
	 * the ground truth, which leads to an asymmetric index. The input images 
	 * must be 16-bit. Note: this method is based on the N_choose_2 normalization.
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
	 * @param originalLabels ground truth, 3D segmented image (objects are labeled with different numbers) 
	 * @param proposedLabels prediction, 3D segmented image (objects are labeled with different numbers)
	 * @return adapted Rand index value
	 */
	public static double adaptedRandIndex3D(
		ImagePlus originalLabels,
		ImagePlus proposedLabels)
	{
		if( originalLabels.getImageStack().getProcessor( 1 ) instanceof ShortProcessor == false 
				|| proposedLabels.getImageStack().getProcessor( 1 ) instanceof ShortProcessor == false)
			return -1;
		
		int nSlices = originalLabels.getImageStackSize();
		
		int maxIDGroundTruth = 0;
		int maxIDProposal = 0;
						
		// Calculate larger IDs of both clusters
		for( int slice = 1; slice <= nSlices; slice ++ )
		{
			ImageProcessor gt = originalLabels.getImageStack().getProcessor( slice );
			gt.resetMinAndMax();
			if ( maxIDGroundTruth < gt.getMax() )
				maxIDGroundTruth = (int) gt.getMax();
			ImageProcessor proposal = proposedLabels.getImageStack().getProcessor( slice );
			proposal.resetMinAndMax();
			if ( maxIDProposal < proposal.getMax() )
				maxIDProposal = (int) proposal.getMax();
		}
		
		double nPairsStack = 0;
		double agreements = 0;
		
		for( int slice = 1; slice <= nSlices; slice ++ )
		{
			ShortProcessor cluster1 = (ShortProcessor) originalLabels.getImageStack().getProcessor( slice );
			ShortProcessor cluster2 = (ShortProcessor) proposedLabels.getImageStack().getProcessor( slice );
			
			final short[] pixels1 = (short[]) cluster1.getPixels();
			final short[] pixels2 = (short[]) cluster2.getPixels();
			
			double nPixels = pixels1.length;
			
			// number of pixels that are "in" (not background) in
			// cluster number 1 (ground truth)
			double n = 0;
			
			// Form the contingency matrix
			int[][]cont = new int[ maxIDGroundTruth + 1] [ maxIDProposal + 1];		

			for(int i=0; i<nPixels; i++)
			{						
				cont[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
				if( pixels1[ i ] > 0)
					n++;
			}

			// sum of squares of sums of rows
			// (skip background objects in the first cluster)
			double[] ni = new double[ cont.length ];
			for(int i=1; i<cont.length; i++)
				for(int j=0; j<cont[0].length; j++)
				{
					ni[ i ] += cont[ i ][ j ];				
				}

			// sum of squares of sums of columns
			// (prune out the zero component in the labeling (un-assigned "out" space))
			double[] nj = new double[ cont[0].length ];
			for(int j=1; j<cont[0].length; j++)
				for(int i=1; i<cont.length; i++)
				{
					nj[ j ] += cont[ i ][ j ];
				}
			
			// true positives - type (i): objects in the pair are placed in the 
			// same class in cluster1 and in the same class in claster2
			// (prune out the zero component in the labeling (un-assigned "out" space))
			double truePositives = 0;
			for(int j=1; j<cont[0].length; j++)
				for(int i=1; i<cont.length; i++)			
					truePositives += cont[ i ][ j ] * ( cont[ i ][ j ] - 1.0 ) / 2.0;			
				
				// total number of pairs (after pruning background pixels
				// of the ground truth)
				double nPairsTotal = n * (n-1.0) / 2.0 ;
				
				// total number of positive samples in ground truth
				double nPosTrue = 0;
				for(int k=0; k<ni.length; k++)
					nPosTrue += ni[ k ] * (ni[ k ]-1.0) /2.0;
				
				// number of pairs actually classified as positive (in the prediction)
				double nPosActual = 0;
				for(int k=0; k<nj.length; k++)
					nPosActual += nj[ k ] * (nj[ k ]-1.0)/2.0;							
					
			// true negatives - type (ii): objects in the pair are placed in different 
			// classes in cluster1 and in different classes in claster2
			// trueNegatives = 	nNegTrue - falsePositives = (nPairsTotal - nPosTrue) - (nPosActual - truePositives)	
			double trueNegatives = nPairsTotal + truePositives - nPosTrue - nPosActual;
			
			agreements += truePositives + trueNegatives;	// number of agreements
			
			nPairsStack += nPairsTotal;			
		}
		
		return agreements / nPairsStack;
	}
	
	/**
	 * Calculate the Rand index between to 3D clusters, as described by
	 * William M. Rand \cite{Rand71}, but pruning out the zero component of
	 * the ground truth, which leads to an asymmetric index. The input images 
	 * must be 16-bit. Note: this method is based on the N_choose_2 normalization.
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
	 * @param originalLabels ground truth, 3D segmented image (objects are labeled with different numbers) 
	 * @param proposedLabels prediction, 3D segmented image (objects are labeled with different numbers)
	 * @return adapted Rand index value and prediction statistics
	 */
	public static ClassificationStatistics adaptedRandIndexStats3D(
			ImagePlus originalLabels,
			ImagePlus proposedLabels)
		{
			if( originalLabels.getImageStack().getProcessor( 1 ) instanceof ShortProcessor == false 
					|| proposedLabels.getImageStack().getProcessor( 1 ) instanceof ShortProcessor == false)
				return null;
		
			//IJ.log( "Calculating adapted Rand index stats...");
						
			int nSlices = originalLabels.getImageStackSize();
			
			int maxIDGroundTruth = 0;
			int maxIDProposal = 0;
							
			// Calculate larger IDs of both clusters
			for( int slice = 1; slice <= nSlices; slice ++ )
			{
				ImageProcessor gt = originalLabels.getImageStack().getProcessor( slice );
				gt.resetMinAndMax();
				if ( maxIDGroundTruth < gt.getMax() )
					maxIDGroundTruth = (int) gt.getMax();
				ImageProcessor proposal = proposedLabels.getImageStack().getProcessor( slice );
				proposal.resetMinAndMax();
				if ( maxIDProposal < proposal.getMax() )
					maxIDProposal = (int) proposal.getMax();
			}
			
			double agreements = 0;

			
			// Form the contingency matrix
			long[][]cont = new long[ maxIDGroundTruth + 1] [ maxIDProposal + 1];
			double[] ni = new double[ cont.length    ];
			double[] nj = new double[ cont[0].length ];
			
			// number of pixels that are "in" (not background) in
			// cluster number 1 (ground truth)
			double n = 0;						
			
			for( int slice = 1; slice <= nSlices; slice ++ )
			{
				//IJ.log(" Processing slice " + slice +"...");
				
				ShortProcessor cluster1 = (ShortProcessor) originalLabels.getImageStack().getProcessor( slice );
				ShortProcessor cluster2 = (ShortProcessor) proposedLabels.getImageStack().getProcessor( slice );
				
				final short[] pixels1 = (short[]) cluster1.getPixels();
				final short[] pixels2 = (short[]) cluster2.getPixels();
				
				double nPixels = pixels1.length;
				
														
				for(int i=0; i<nPixels; i++)
				{						
					cont[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
					if( pixels1[ i ] > 0)
						n++;
				}
			}
			// sum of squares of sums of rows
			// (skip background objects in the first cluster)

			for(int i=1; i<cont.length; i++)
				for(int j=0; j<cont[0].length; j++)
				{
					ni[ i ] += cont[ i ][ j ];				
				}

			// sum of squares of sums of columns
			// (prune out the zero component in the labeling (un-assigned "out" space))				
			for(int j=1; j<cont[0].length; j++)
				for(int i=1; i<cont.length; i++)
				{
					nj[ j ] += cont[ i ][ j ];
				}
			
			
			// true positives - type (i): objects in the pair are placed in the 
			// same class in cluster1 and in the same class in claster2
			// (prune out the zero component in the labeling (un-assigned "out" space))
			double truePositives = 0;
			for(int j=1; j<cont[0].length; j++)
				for(int i=1; i<cont.length; i++)			
					truePositives += cont[ i ][ j ] * ( cont[ i ][ j ] - 1.0 ); // / 2.0;			

			// total number of pairs (after pruning background pixels
			// of the ground truth)
			double nPairsTotal = n * (n-1.0);// / 2.0 ;

			// total number of positive samples in ground truth
			double nPosTrue = 0;
			for(int k=0; k<ni.length; k++)
				nPosTrue += ni[ k ] * (ni[ k ]-1.0);// /2.0;

			// number of pairs actually classified as positive (in the prediction)
			double nPosActual = 0;
			for(int k=0; k<nj.length; k++)
				nPosActual += nj[ k ] * (nj[ k ]-1.0); ///2.0;				

			// true negatives - type (ii): objects in the pair are placed in different 
			// classes in cluster1 and in different classes in claster2
			// trueNegatives = 	nNegTrue - falsePositives = (nPairsTotal - nPosTrue) - (nPosActual - truePositives)	
			double trueNegatives = nPairsTotal + truePositives - nPosTrue - nPosActual;

			//IJ.log("  agreements = " + (truePositives + trueNegatives) );

			agreements += truePositives + trueNegatives;	// number of agreements


			double falsePositives = nPosActual - truePositives;
			double nNegActual = nPairsTotal - nPosActual;
			double falseNegatives = nNegActual - trueNegatives;

			truePositives /= 2.0;
			trueNegatives /= 2.0;
			falsePositives /= 2.0;
			falseNegatives /= 2.0;
						

			// agreements /= 2.0;
			// nPairsTotal /= 2.0;
			
			
			double randIndex = agreements / nPairsTotal;
			
			return new ClassificationStatistics( truePositives, trueNegatives, falsePositives,  falseNegatives, randIndex);
		}
	
	
	/**
	 * Calculate the Rand index stats between to 3D clusters, as described by
	 * William M. Rand \cite{Rand71}, but pruning out the zero component of
	 * the ground truth, which leads to an asymmetric index. The input images 
	 * must be 16-bit. Note: this method is based on the N^2 normalization.
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
	 * @param segA ground truth, 3D segmented image (objects are labeled with different numbers) 
	 * @param segB prediction, 3D segmented image (objects are labeled with different numbers)
	 * @return [ precision, recall, Rand index with n^2 normalization ]
	 */
	public static double[] adaptedRandIndexStats3DN2(
			ImagePlus segA,
			ImagePlus segB)
	{
		if( segA.getImageStack().getProcessor( 1 ) instanceof ShortProcessor == false 
				|| segB.getImageStack().getProcessor( 1 ) instanceof ShortProcessor == false)
			return null;

		//IJ.log( "Calculating adapted Rand index stats...");

		int nSlices = segA.getImageStackSize();

		int nLabelsA = 0;
		int nLabelsB = 0;

		// Calculate larger IDs of both clusters
		for( int slice = 1; slice <= nSlices; slice ++ )
		{
			ImageProcessor gt = segA.getImageStack().getProcessor( slice );
			gt.resetMinAndMax();
			if ( nLabelsA < gt.getMax() )
				nLabelsA = (int) gt.getMax();
			ImageProcessor proposal = segB.getImageStack().getProcessor( slice );
			proposal.resetMinAndMax();
			if ( nLabelsB < proposal.getMax() )
				nLabelsB = (int) proposal.getMax();
		}

		// Form the contingency matrix
		long[][]pij = new long[ nLabelsA + 1] [ nLabelsB + 1];		

		double n = segA.getImageStackSize() * segA.getWidth() * segA.getHeight();

		for( int slice = 1; slice <= nSlices; slice ++ )
		{
			//IJ.log(" Processing slice " + slice +"...");

			ShortProcessor cluster1 = (ShortProcessor) segA.getImageStack().getProcessor( slice );
			ShortProcessor cluster2 = (ShortProcessor) segB.getImageStack().getProcessor( slice );

			final short[] pixels1 = (short[]) cluster1.getPixels();
			final short[] pixels2 = (short[]) cluster2.getPixels();

			double nPixels = pixels1.length;				

			for(int i=0; i<nPixels; i++)									
				pij[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
		}



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

		double sumA = 0;
		for(int i=0; i<ai.length; i++)
			sumA += ai[ i ] * ai[ i ];

		double sumB = 0;
		for(int j=0; j<bj.length; j++)
			sumB += bj[ j ] * bj[ j ];

		sumB += aux/n;


		double sumAB = 0;
		for(int i=1; i<pij.length; i++)
			for(int j=1; j<pij[0].length; j++)
				sumAB += pij[ i ][ j ] * pij[ i ][ j ]; 

		sumAB += aux/n;

		// return precision, recall and Rand index value
		return new double[]{ sumAB/sumB, sumAB/sumA, 1.0 - (sumA+sumB-2.0*sumAB)/(n*n) };
	}
	
	/**
	 * Calculate the F-score of the Rand index between to 3D clusters, as described by
	 * William M. Rand \cite{Rand71}, but pruning out the zero component of
	 * the ground truth, which leads to an asymmetric index. The input images 
	 * must be 16-bit. Note: this method is based on the N^2 normalization.
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
	 * @param originalLabels ground truth, 3D segmented image (objects are labeled with different numbers) 
	 * @param proposedLabels prediction, 3D segmented image (objects are labeled with different numbers)
	 * @return F-score of Rand index in 3D (-1 if error)
	 */
	public static double adaptedRandIndexFScore3D(
			ImagePlus originalLabels,
			ImagePlus proposedLabels)
	{
		if( originalLabels.getImageStack().getProcessor( 1 ) instanceof ShortProcessor == false 
				|| proposedLabels.getImageStack().getProcessor( 1 ) instanceof ShortProcessor == false)
			return -1;

		double[] stats = adaptedRandIndexStats3DN2( originalLabels, proposedLabels );
		
		return 2.0 * stats[ 0 ] * stats[ 1 ] / (stats[ 0 ] + stats[ 1 ]); 
	}
	
	
	/**
	 * Calculate the Rand index between to clusters, as described by
	 * William M. Rand \cite{Rand71}, but pruning out the zero component of
	 * the ground truth, which leads to an asymmetric index.
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
	 * @param cluster1 ground truth, 2D segmented image (objects are labeled with different numbers) 
	 * @param cluster2 prediction, 2D segmented image (objects are labeled with different numbers)
	 * @return Rand index value and prediction statistics
	 */
	public ClassificationStatistics getRandIndexStats(
			ShortProcessor cluster1,
			ShortProcessor cluster2)
	{
		final short[] pixels1 = (short[]) cluster1.getPixels();
		final short[] pixels2 = (short[]) cluster2.getPixels();
		
		//(new ImagePlus("cluster 1", cluster1)).show();
		//(new ImagePlus("cluster 2", cluster2)).show();
		
		double nPixels = pixels1.length;
		
		// number of pixels that are "in" (not background) in 
		// the first cluster (ground truth)
		double n = 0;
		
		// reset min and max of the cluster processors 
		// (needed in order to have correct min-max values)
		cluster1.resetMinAndMax();
		cluster2.resetMinAndMax();
		
		// Form the contingency matrix
		int[][]cont = new int[(int) cluster1.getMax() + 1] [ (int) cluster2.getMax() + 1];		
		
		//IJ.log(" cont.length = " +cont.length );
		//IJ.log(" cont[0].length = " +cont[0].length );

		for(int i=0; i<nPixels; i++)
		{						
			cont[ pixels1[i] & 0xffff ] [ pixels2[i] & 0xffff ] ++;
			if( pixels1[ i ] > 0)
				n++;
		}
		// sum over rows & columnns of nij^2
		//double t2 = 0;				
		
		// sums of rows
		// (skip background objects in the first cluster)
		double[] ni = new double[ cont.length ];
		for(int i=1; i<cont.length; i++)
			for(int j=0; j<cont[0].length; j++)
			{
				ni[ i ] += cont[ i ][ j ];				
			}
		/*
		// sum of squares of sums of rows
		double nis = 0;
		for(int k=0; k<ni.length; k++)
			nis += ni[ k ] * ni[ k ];
		*/
		
		// sums of columns
		// (prune out the zero component in the labeling (un-assigned "out" space))
		double[] nj = new double[ cont[0].length ];
		for(int j=1; j<cont[0].length; j++)
			for(int i=1; i<cont.length; i++)
			{
				nj[ j ] += cont[ i ][ j ];
				//t2 += cont[ i ][ j ] * cont[ i ][ j ];
			}
		/*
		// sum of squares of sums of columns
		double njs = 0;
		for(int k=0; k<nj.length; k++)
			njs += nj[ k ] * nj[ k ];
		*/
		
		// true positives - type (i): objects in the pair are placed in the 
		// same class in cluster1 and in the same class in claster2
		// (prune out the zero component in the labeling (un-assigned "out" space))
		double truePositives = 0;
		for(int j=1; j<cont[0].length; j++)
			for(int i=1; i<cont.length; i++)
			{
				truePositives += cont[ i ][ j ] * ( cont[ i ][ j ] - 1.0 ) / 2.0;
			}
													
		// total number of pairs after pruning the brackground
		// pixels of the ground truth
		double nPairsTotal = n * (n-1) / 2 ;
		
		// number of true samples withing the ground truth
		double nPosTrue = 0;
		for(int k=0; k<ni.length; k++)
			nPosTrue += ni[ k ] * (ni[ k ]-1) /2;
		
		// number of pairs that were actually classified as positive
		// by the prediction
		double nPosActual = 0;
		for(int k=0; k<nj.length; k++)
			nPosActual += nj[ k ] * (nj[ k ]-1)/2;				
		
		// true negatives - type (ii): objects in the pair are placed in different 
		// classes in cluster1 and in different classes in claster2
		//double trueNegatives = (n*n + t2 - nis - njs) / 2;		
		// Pairs correctly classified as negative (i.e. true negatives)
		// Explanation: 
		//     trueNegatives = total negatives - false positives =
		//                   = (nPairsTotal - nPostTrue) - (nPosActual - truePositives)
		double trueNegatives = nPairsTotal + truePositives - nPosTrue - nPosActual;
						
		// false positives - type (iii): objects in the pair are placed in different 
		// classes in cluster1 and in the same class in claster2
		double falsePositives = nPosActual - truePositives; //(njs - t2) / 2;
		
		// number of pairs actually classified as negative
		double nNegActual = nPairsTotal - nPosActual;
		
		// false negatives - type (iv): objects in the pair are placed in the same 
		// class in cluster1 and in different classes in claster2		
		double falseNegatives = nNegActual - trueNegatives; //(nis - t2) / 2;
		
							
		// number of pairs classified as negative
		//double nNegTrue = nPairsTotal - nPosTrue;
		
		// the number of incorrectly classified pairs
		//double nPosIncorrect = nPosTrue-truePositives;
		//double nNegIncorrect = nNegTrue-trueNegatives;
		//double nPairsIncorrect = nPosIncorrect + nNegIncorrect;

		// clustering error
		//double clusteringError = nPairsIncorrect/nPairsTotal;
		
		double agreements = truePositives + trueNegatives;		// number of agreements
		
		double randIndex = agreements / nPairsTotal;
		
		/*
		IJ.log(" In getRandIndexStats:");
		IJ.log("  tp = " + truePositives);
	    IJ.log("  tn = " + trueNegatives);
	    IJ.log("  fp = " + falsePositives);
	    IJ.log("  fn = " + falseNegatives);
	    IJ.log(" nPairsTotal = " + nPairsTotal);
	    IJ.log(" nPosTrue = " + nPosTrue);
	    IJ.log(" nPosActual = " + nPosActual);
	    IJ.log(" nNegActual = " + nNegActual);
	    IJ.log("  clusteringError = " + clusteringError);
	    IJ.log("  agreements / nPairsTotal = " + (agreements / nPairsTotal));
		*/
		return new ClassificationStatistics( truePositives, trueNegatives, 
									falsePositives,  falseNegatives, randIndex);
	}
	
	/**
	 * Get the best F-score of the Rand index over a set of thresholds 
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

	    for(ClassificationStatistics stat : stats)
	    {
	    	if (stat.fScore > maxFScore)
	    		maxFScore = stat.fScore;
	    }	    
	    return maxFScore;
	}
	
    /**
     * Main method for calcualte the Rand error metrics 
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
          else if (args[0].equals("-maxFScoreRandIndex"))
        	  System.out.println( maxFScoreRandIndex(args) );
          else 
        	  dumpSyntax();
       }
       System.exit(0);
    }
    
    /**
     * Calculate the maximum F-score of the Rand index based on the
     * parameters introduced by command line
     * 
     * @param args command line arguments
     * @return maximal F-score of the Rand index
     */
    static double maxFScoreRandIndex(String[] args) 
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
    	
		RandError re = new RandError(label, proposal);
		re.setVerboseMode( false );
		return re.getRandIndexMaximalFScore(minThreshold, maxThreshold, stepThreshold);
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
       System.out.println("Purpose: calculate Rand error between proposed and original labels.\n");     
       System.out.println("Usage: RandError ");
       System.out.println("  -help                      : show this message");
       System.out.println("");
       System.out.println("  -maxFScoreRandIndex        : calculate maximum F-score of the Rand index over a set of thresholds");
       System.out.println("          labels             : image with the original labels");
       System.out.println("          proposal           : image with the proposed labels");
       System.out.println("          minThreshold       : minimum threshold value to binarize the proposal");
       System.out.println("          maxThreshold       : maximum threshold value to binarize the proposal");
       System.out.println("          stepThreshold      : threshold step value to use during binarization");
       System.out.println("Examples:");
       System.out.println("Calculate the maximum F-score of the Rand index between proposed and original labels over a set of");
       System.out.println("thresholds (from 0.0 to 1.0 in steps of 0.1)");
       System.out.println("   WarpingError -maxFScoreRandIndex original-labels.tif proposed-labels.tif 0.0 1.0 0.1");
    }
	
	
} // end class RandError
