package process;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian.SpecialPoint;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.models.TileConfiguration;
import mpicbg.pointdescriptor.AbstractPointDescriptor;
import mpicbg.pointdescriptor.ModelPointDescriptor;
import mpicbg.pointdescriptor.SimplePointDescriptor;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.pointdescriptor.matcher.Matcher;
import mpicbg.pointdescriptor.matcher.SubsetMatcher;
import mpicbg.pointdescriptor.model.TranslationInvariantModel;
import mpicbg.pointdescriptor.model.TranslationInvariantRigidModel2D;
import mpicbg.pointdescriptor.model.TranslationInvariantRigidModel3D;
import mpicbg.pointdescriptor.similarity.SimilarityMeasure;
import mpicbg.pointdescriptor.similarity.SquareDistance;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import mpicbg.spim.mpicbg.TileConfigurationSPIM;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.BeadRegistration;
import mpicbg.spim.registration.bead.error.GlobalErrorStatistics;
import mpicbg.spim.registration.detection.AbstractDetection;
import mpicbg.spim.registration.detection.DetectionSegmentation;
import mpicbg.spim.segmentation.InteractiveDoG;
import plugin.DescriptorParameters;
import fiji.util.KDTree;
import fiji.util.NNearestNeighborSearch;

public class Matching 
{
	public static void descriptorBasedRegistration( final ImagePlus imp1, final ImagePlus imp2, final DescriptorParameters params )
	{
		// zStretching if applicable
		float zStretching1 = params.dimensionality == 3 ? (float)imp1.getCalibration().pixelDepth / (float)imp1.getCalibration().pixelWidth : 1;
		float zStretching2 = params.dimensionality == 3 ? (float)imp2.getCalibration().pixelDepth / (float)imp2.getCalibration().pixelWidth : 1;

		// get the peaks
		final ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks1 = extractCandidates( imp1, params.channel1, 0, params );
		final ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks2 = extractCandidates( imp2, params.channel2, 0, params );

		// compute ransac
		ArrayList<PointMatch> finalInliers = new ArrayList<PointMatch>();
		Model<?> finalModel = pairwiseMatching( finalInliers, peaks1, peaks2, zStretching1, zStretching2, params, "" );				
		IJ.log( "" + finalModel );
		
		// set point rois if 2d and wanted
		if ( params.setPointsRois )
			setPointRois( imp1, imp2, finalInliers );
		
		// fuse if wanted
		if ( params.fuse )
		{
			final CompositeImage composite;

			final Model<?> model2 = params.model.copy();
			
			if ( params.dimensionality == 3 )
			{
				BeadRegistration.concatenateAxialScaling( (AbstractAffineModel3D<?>)finalModel, imp1.getCalibration().pixelDepth / imp1.getCalibration().pixelWidth );				
				BeadRegistration.concatenateAxialScaling( (AbstractAffineModel3D<?>)model2, imp2.getCalibration().pixelDepth / imp2.getCalibration().pixelWidth );
			}
			
			if ( imp1.getType() == ImagePlus.GRAY32 || imp2.getType() == ImagePlus.GRAY32 )
				composite = OverlayFusion.createOverlay( new FloatType(), imp1, imp2, (InvertibleBoundable)finalModel, (InvertibleBoundable)model2, params.dimensionality );
			else if ( imp1.getType() == ImagePlus.GRAY16 || imp2.getType() == ImagePlus.GRAY16 )
				composite = OverlayFusion.createOverlay( new UnsignedShortType(), imp1, imp2, (InvertibleBoundable)finalModel, (InvertibleBoundable)params.model.copy(), params.dimensionality );
			else
				composite = OverlayFusion.createOverlay( new UnsignedByteType(), imp1, imp2, (InvertibleBoundable)finalModel, (InvertibleBoundable)params.model.copy(), params.dimensionality );
			
			composite.show();
		}
	}
	
	public static void descriptorBasedStackRegistration( final ImagePlus imp, final DescriptorParameters params )
	{
		final int numImages = imp.getNFrames();
		
		// zStretching if applicable
		final float zStretching = params.dimensionality == 3 ? (float)imp.getCalibration().pixelDepth / (float)imp.getCalibration().pixelWidth : 1;
		
		// get the peaks
		final ArrayList<ArrayList<DifferenceOfGaussianPeak<FloatType>>> peaks = new ArrayList<ArrayList<DifferenceOfGaussianPeak<FloatType>>>();
		
		for ( int t = 0; t < numImages; ++t )
			peaks.add( extractCandidates( imp, params.channel1, t, params ) );
		
		// get all compare pairs
		final Vector<ComparePair> pairs = getComparePairs( params, numImages );

		// compute all matchings
		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads();
        final int numThreads = threads.length;
    	
        for ( int ithread = 0; ithread < threads.length; ++ithread )
            threads[ ithread ] = new Thread(new Runnable()
            {
                public void run()
                {		
                   	final int myNumber = ai.getAndIncrement();
                    
                    for ( int i = 0; i < pairs.size(); i++ )
                    	if ( i%numThreads == myNumber )
                    	{
                    		final ComparePair pair = pairs.get( i );
                    		pair.model = pairwiseMatching( pair.inliers, peaks.get( pair.indexA ), peaks.get( pair.indexB ), zStretching, zStretching, params, pair.indexA + "<->" + pair.indexB );
                    	}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
        // perform global optimization
    	final ArrayList<Tile<?>> tiles = new ArrayList<Tile<?>>();
		for ( int t = 1; t <= numImages; ++t )
			tiles.add( new Tile( params.model.copy() ) );
		
		// reset the coordinates of all points so that we directly get the correct model
		for ( final ComparePair pair : pairs )
		{
			if ( pair.inliers.size() > 0 )
			{
    			for ( final PointMatch pm : pair.inliers )
				{
					((Particle)pm.getP1()).restoreCoordinates();
					((Particle)pm.getP2()).restoreCoordinates();
				}    			
			}
			IJ.log( pair.indexA + "<->" + pair.indexB + ": " + pair.model );
		}
		
		for ( final ComparePair pair : pairs )
			addPointMatches( pair.inliers, tiles.get( pair.indexA ), tiles.get( pair.indexB ) );

		final TileConfiguration tc = new TileConfiguration();

		boolean fixed = false;
		for ( int t = 0; t < numImages; ++t )
		{
			final Tile<?> tile = tiles.get( t );
			
			if ( tile.getConnectedTiles().size() > 0 )
			{
				tc.addTile( tile );
				if ( !fixed )
				{
					tc.fixTile( tile );
					fixed = true;
				}
			}
			else 
			{
				IJ.log( "Tile " + t + " is not connected to any other tile, cannot compute a model" );
			}
		}
		
		try
		{
			tc.optimize( 10, 10000, 200 );
		}
		catch ( Exception e )
		{
			IJ.log( "Global optimization failed: " + e );
			return;
		}
		
		// assemble final list of models
		final ArrayList<InvertibleBoundable> models = new ArrayList<InvertibleBoundable>();
		
		for ( int t = 0; t < numImages; ++t )
		{
			final Tile<?> tile = tiles.get( t );
			
			if ( tile.getConnectedTiles().size() > 0 )
			{
				IJ.log( "Tile " + t + " (connected): " + tile.getModel()  );
				models.add( (InvertibleBoundable)tile.getModel() );
			}
			else
			{
				IJ.log( "Tile " + t + " (NOT connected): " + tile.getModel()  );
				models.add( (InvertibleBoundable)params.model.copy() );
			}
		}
		
		// fuse
		if ( params.dimensionality == 3 )
			for ( final InvertibleBoundable model : models )
				BeadRegistration.concatenateAxialScaling( (AbstractAffineModel3D<?>)model, imp.getCalibration().pixelDepth / imp.getCalibration().pixelWidth );				
		
		final ImagePlus result;
		
		if ( imp.getType() == ImagePlus.GRAY32 )
			result = OverlayFusion.createReRegisteredSeries( new FloatType(), imp, models, params.dimensionality );
		else if ( imp.getType() == ImagePlus.GRAY16 )
			result = OverlayFusion.createReRegisteredSeries( new UnsignedShortType(), imp, models, params.dimensionality );
		else
			result = OverlayFusion.createReRegisteredSeries( new UnsignedByteType(), imp, models, params.dimensionality );
		
		result.show();
	}
	
	public synchronized static void addPointMatches( final ArrayList<PointMatch> correspondences, final Tile<?> tileA, final Tile<?> tileB )
	{
		if ( correspondences.size() > 0 )
		{
			tileA.addMatches( correspondences );							
			tileB.addMatches( PointMatch.flip( correspondences ) );
			tileA.addConnectedTile( tileB );
			tileB.addConnectedTile( tileA );
		}
	}  

	protected static Vector<ComparePair> getComparePairs( final DescriptorParameters params, final int numImages )
	{
		final Vector<ComparePair> pairs = new Vector<ComparePair>();
		
		if ( params.globalOpt == 0 ) //all-to-all
		{
			for ( int indexA = 0; indexA < numImages - 1; indexA++ )
	    		for ( int indexB = indexA + 1; indexB < numImages; indexB++ )
	    			pairs.add( new ComparePair( indexA, indexB, params.model ) );
		}
		else if ( params.globalOpt == 1 ) //all-to-all-withrange
		{
			for ( int indexA = 0; indexA < numImages - 1; indexA++ )
	    		for ( int indexB = indexA + 1; indexB < numImages; indexB++ )
	    			if ( Math.abs( indexB - indexA ) <= params.range )
	    				pairs.add( new ComparePair( indexA, indexB, params.model ) );			
		}
		else if ( params.globalOpt == 2 ) //all-to-1
		{
			for ( int indexA = 1; indexA < numImages; ++indexA )
				pairs.add( new ComparePair( indexA, 0, params.model ) );
		}
		else // Consecutive
		{
			for ( int indexA = 1; indexA < numImages; ++indexA )
				pairs.add( new ComparePair( indexA, indexA - 1, params.model ) );			
		}
		
		return pairs;
	}
	
	protected static Model<?> pairwiseMatching( final ArrayList<PointMatch> finalInliers, final ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks1, final ArrayList<DifferenceOfGaussianPeak<FloatType>>peaks2, 
			final float zStretching1, final float zStretching2, final DescriptorParameters params, String explanation )
	{
		final Matcher matcher = new SubsetMatcher( params.numNeighbors, params.numNeighbors + params.redundancy );
		ArrayList<PointMatch> candidates = getCorrespondenceCandidates( params.significance, matcher, peaks1, peaks2, null, params.dimensionality, zStretching1, zStretching2 );
		
		// compute ransac
		//ArrayList<PointMatch> finalInliers = new ArrayList<PointMatch>();
		Model<?> finalModel = params.model.copy();
		String statement = computeRANSAC( candidates, finalInliers, finalModel, (float)params.ransacThreshold );

		// apply rotation-variant matching after applying the model until it converges
		if ( finalInliers.size() > finalModel.getMinNumMatches() )
		{
			int previousNumInliers = 0;
			int numInliers = 0;
			do
			{
				// get the correspondence candidates with the knowledge of the previous model
				candidates = getCorrespondenceCandidates( params.significance, matcher, peaks1, peaks2, finalModel, params.dimensionality, zStretching1, zStretching2 );
				
				// before we compute the RANSAC we will reset the coordinates of all points so that we directly get the correct model
				for ( final PointMatch pm : candidates )
				{
					((Particle)pm.getP1()).restoreCoordinates();
					((Particle)pm.getP2()).restoreCoordinates();
				}			
				
				// compute ransac
				previousNumInliers = finalInliers.size();				
				final ArrayList<PointMatch> inliers = new ArrayList<PointMatch>();
				Model<?> model2 = params.model.copy();
				statement = computeRANSAC( candidates, inliers, model2, (float)params.ransacThreshold );
				numInliers = inliers.size();
				
				// update model if this one was better
				if ( numInliers > previousNumInliers )
				{
					finalModel = model2;
					finalInliers.clear();
					finalInliers.addAll( inliers );
					//finalInliers = inliers;
				}
			} 
			while ( numInliers > previousNumInliers );
		}
		else
		{
			IJ.log( explanation + ": No inliers found, stopping. Tipp: You could increase the number of neighbors, redundancy or use a model that has more degrees of freedom." );
			finalInliers.clear();
			return null;
		}
		
		IJ.log( explanation + ": " + statement );
		
		return finalModel;
	}

	protected static ArrayList<DifferenceOfGaussianPeak<FloatType>> extractCandidates( final ImagePlus imp, final int channel, final int timepoint, final DescriptorParameters params )
	{
		// get the input images for registration
		final Image<FloatType> img = InteractiveDoG.convertToFloat( imp, channel, timepoint );

		// extract Calibrations
		final Calibration cal = imp.getCalibration();
		
		if ( params.dimensionality == 2 )
			img.setCalibration( new float[]{ (float)cal.pixelWidth, (float)cal.pixelHeight } );
		else
			img.setCalibration( new float[]{ (float)cal.pixelWidth, (float)cal.pixelHeight, (float)cal.pixelDepth } );
		
		// extract candidates
		final ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks = computeDoG( img, (float)params.sigma1, (float)params.sigma2, params.lookForMaxima, params.lookForMinima, (float)params.threshold );
		
		// remove invalid peaks
		final int[] stats1 = removeInvalidAndCollectStatistics( peaks );
		
		IJ.log( "Found " + peaks.size() + " candidates for " + imp.getTitle() + " [" + timepoint + "] (" + stats1[ 1 ] + " maxima, " + stats1[ 0 ] + " minima)" );
		
		return peaks;
	}
	
	
	protected static void setPointRois( final ImagePlus imp1, final ImagePlus imp2, final ArrayList<PointMatch> inliers )
	{
		final ArrayList<Point> list1 = new ArrayList<Point>();
		final ArrayList<Point> list2 = new ArrayList<Point>();

		PointMatch.sourcePoints( inliers, list1 );
		PointMatch.targetPoints( inliers, list2 );
		
		PointRoi sourcePoints = mpicbg.ij.util.Util.pointsToPointRoi(list1);
		PointRoi targetPoints = mpicbg.ij.util.Util.pointsToPointRoi(list2);
		
		imp1.setRoi( sourcePoints );
		imp2.setRoi( targetPoints );
		
	}
	
	protected static String computeRANSAC( final ArrayList<PointMatch> candidates, final ArrayList<PointMatch> inliers, final Model<?> model, final float maxEpsilon )
	{		
		boolean modelFound = false;
		float minInlierRatio = 0.05f;
		int numIterations = DescriptorParameters.ransacIterations;
		
		try
		{
			/*modelFound = m.ransac(
  					candidates,
					inliers,
					numIterations,
					maxEpsilon, minInlierRatio );*/
		
			modelFound = model.filterRansac(
					candidates,
					inliers,
					numIterations,
					maxEpsilon, minInlierRatio );
			
			if ( modelFound )
			{
				model.fit( inliers );
				return "Remaining inliers after RANSAC (" + model.getClass().getSimpleName() + "): " + inliers.size() + " of " + candidates.size() + " with average error " + model.getCost();
			}
			else
			{
				return "NO Model found after RANSAC (" + model.getClass().getSimpleName() + ") of " + candidates.size();
			}
		}
		catch ( Exception e )
		{
			return "Exception - NO Model found after RANSAC (" + model.getClass().getSimpleName() + ") of " + candidates.size();

		}
	}

	protected static ArrayList<PointMatch> getCorrespondenceCandidates( final double nTimesBetter, final Matcher matcher, 
			ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks1, ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks2, 
			final Model<?> model, final int dimensionality, final float zStretching1, final float zStretching2 )
	{
		// two new lists
		ArrayList<Particle> listA = new ArrayList<Particle>();
		ArrayList<Particle> listB = new ArrayList<Particle>();
		
		int id = 0;
		
		if ( model == null )
		{
			// no prior model known, do a locally rigid matching
			for ( DifferenceOfGaussianPeak<FloatType> peak : peaks1 )
				listA.add( new Particle( id++, peak, zStretching1 ) );
			for ( DifferenceOfGaussianPeak<FloatType> peak : peaks2 )
				listB.add( new Particle( id++, peak, zStretching2 ) );
		}
		else
		{
			// prior model known, apply to the points before matching and then do a simple descriptor matching
			for ( DifferenceOfGaussianPeak<FloatType> peak : peaks1 )
			{
				final Particle particle = new Particle( id++, peak, zStretching1 );			
				particle.apply( model );
				for ( int d = 0; d < particle.getL().length; ++d )
					particle.getL()[ d ] = particle.getW()[ d ];
				listA.add( particle );
			}
			
			for ( DifferenceOfGaussianPeak<FloatType> peak : peaks2 )
			{
				final Particle particle = new Particle( id++, peak, zStretching2 );
				listB.add( particle );
			}
		}
		
		/* create KDTrees */	
		final KDTree< Particle > treeA = new KDTree< Particle >( listA );
		final KDTree< Particle > treeB = new KDTree< Particle >( listB );
		
		/* extract point descriptors */						
		final int numNeighbors = matcher.getRequiredNumNeighbors();
		
		final SimilarityMeasure similarityMeasure = new SquareDistance();
		
		final ArrayList< AbstractPointDescriptor > descriptorsA, descriptorsB;
		
		if ( model == null )
		{
			descriptorsA = createModelPointDescriptors( treeA, listA, numNeighbors, matcher, similarityMeasure, dimensionality );
			descriptorsB = createModelPointDescriptors( treeB, listB, numNeighbors, matcher, similarityMeasure, dimensionality );
		}
		else
		{
			descriptorsA = createSimplePointDescriptors( treeA, listA, numNeighbors, matcher, similarityMeasure );
			descriptorsB = createSimplePointDescriptors( treeB, listB, numNeighbors, matcher, similarityMeasure );
		}
		
		/* compute matching */
		/* the list of correspondence candidates */
		final ArrayList<PointMatch> correspondenceCandidates = findCorrespondingDescriptors( descriptorsA, descriptorsB, (float)nTimesBetter );
		
		return correspondenceCandidates;
	}
	
	protected static final ArrayList<PointMatch> findCorrespondingDescriptors( final ArrayList<AbstractPointDescriptor> descriptorsA, final ArrayList<AbstractPointDescriptor> descriptorsB, final float nTimesBetter )
	{
		final ArrayList<PointMatch> correspondenceCandidates = new ArrayList<PointMatch>();
		
		for ( final AbstractPointDescriptor descriptorA : descriptorsA )
		{
			double bestDifference = Double.MAX_VALUE;			
			double secondBestDifference = Double.MAX_VALUE;
			
			AbstractPointDescriptor bestMatch = null;
			AbstractPointDescriptor secondBestMatch = null;

			for ( final AbstractPointDescriptor descriptorB : descriptorsB )
			{
				final double difference = descriptorA.descriptorDistance( descriptorB );

				if ( difference < secondBestDifference )
				{					
					secondBestDifference = difference;
					secondBestMatch = descriptorB;
					
					if ( secondBestDifference < bestDifference )
					{
						double tmpDiff = secondBestDifference;
						AbstractPointDescriptor tmpMatch = secondBestMatch;
						
						secondBestDifference = bestDifference;
						secondBestMatch = bestMatch;
						
						bestDifference = tmpDiff;
						bestMatch = tmpMatch;
					}
				}				
			}
			
			if ( bestDifference < 100 && bestDifference * nTimesBetter < secondBestDifference )
			{	
				// add correspondence for the two basis points of the descriptor
				Particle particleA = (Particle)descriptorA.getBasisPoint();
				Particle particleB = (Particle)bestMatch.getBasisPoint();
				
				// for RANSAC
				correspondenceCandidates.add( new PointMatch( particleA, particleB ) );
			}
		}
		
		return correspondenceCandidates;
	}

	protected static ArrayList< AbstractPointDescriptor > createSimplePointDescriptors( final KDTree< Particle > tree, final ArrayList< Particle > basisPoints, 
			final int numNeighbors, final Matcher matcher, final SimilarityMeasure similarityMeasure )
	{
		final NNearestNeighborSearch< Particle > nnsearch = new NNearestNeighborSearch< Particle >( tree );
		final ArrayList< AbstractPointDescriptor > descriptors = new ArrayList< AbstractPointDescriptor > ( );
		
		for ( final Particle p : basisPoints )
		{
			final ArrayList< Particle > neighbors = new ArrayList< Particle >();
			final Particle neighborList[] = nnsearch.findNNearestNeighbors( p, numNeighbors + 1 );
			
			// the first hit is always the point itself
			for ( int n = 1; n < neighborList.length; ++n )
				neighbors.add( neighborList[ n ] );
			
			try
			{
				descriptors.add( new SimplePointDescriptor<Particle>( p, neighbors, similarityMeasure, matcher ) );
			}
			catch ( NoSuitablePointsException e )
			{
				e.printStackTrace();
			}
		}
		
		return descriptors;
	}

	protected static ArrayList< AbstractPointDescriptor > createModelPointDescriptors( final KDTree< Particle > tree, final ArrayList< Particle > basisPoints, 
			final int numNeighbors, final Matcher matcher, final SimilarityMeasure similarityMeasure, final int dimensionality )
	{
		final NNearestNeighborSearch< Particle > nnsearch = new NNearestNeighborSearch< Particle >( tree );
		final ArrayList< AbstractPointDescriptor > descriptors = new ArrayList< AbstractPointDescriptor > ( );
		
		for ( final Particle p : basisPoints )
		{
			final ArrayList< Particle > neighbors = new ArrayList< Particle >();
			final Particle neighborList[] = nnsearch.findNNearestNeighbors( p, numNeighbors + 1 );
			
			// the first hit is always the point itself
			for ( int n = 1; n < neighborList.length; ++n )
				neighbors.add( neighborList[ n ] );
			
			final TranslationInvariantModel<?> model;
			
			if ( dimensionality == 2 )
				model = new TranslationInvariantRigidModel2D();
			else if ( dimensionality == 3 )
				model = new TranslationInvariantRigidModel3D();
			else
			{
				IJ.log( "dimensionality " + dimensionality + " not supported." );
				return descriptors;
			}
				
			try
			{
				descriptors.add( new ModelPointDescriptor<Particle>( p, neighbors, model, similarityMeasure, matcher ) );
			}
			catch ( NoSuitablePointsException e )
			{
				e.printStackTrace();
			}
		}
		
		return descriptors;
	}
	
	protected static ArrayList<DifferenceOfGaussianPeak<FloatType>> computeDoG( final Image<FloatType> image, final float sigma1, final float sigma2, 
			final boolean lookForMaxima, final boolean lookForMinima, final float threshold )
	{
		return DetectionSegmentation.extractBeadsLaPlaceImgLib( image, new OutOfBoundsStrategyMirrorFactory<FloatType>(), 0.5f, sigma1, sigma2, threshold, threshold/4, lookForMaxima, lookForMinima, ViewStructure.DEBUG_MAIN );
	}
	
	protected static int[] removeInvalidAndCollectStatistics( ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks )
	{
		int min = 0;
		int max = 0;

		// remove entries that are too low
        for ( int i = peaks.size() - 1; i >= 0; --i )
        {
        	final DifferenceOfGaussianPeak<FloatType> peak = peaks.get( i );
        	
        	if ( !peak.isValid() )
        		peaks.remove( i );
        	else if ( peak.getPeakType() == SpecialPoint.MIN )
				min++;
			else if ( peak.getPeakType() == SpecialPoint.MAX )
				max++;
        }
        
        return new int[]{ min, max };
	}

}
