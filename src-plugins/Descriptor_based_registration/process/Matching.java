package process;

import fiji.util.KDTree;
import fiji.util.NNearestNeighborSearch;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PointRoi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian.SpecialPoint;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.container.imageplus.ImagePlusContainer;
import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.exception.ImgLibException;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AffineModel2D;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
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
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.BeadRegistration;
import mpicbg.spim.registration.detection.DetectionSegmentation;
import mpicbg.spim.segmentation.InteractiveDoG;
import plugin.DescriptorParameters;

public class Matching 
{
	public Matching( final ImagePlus imp1, final ImagePlus imp2, final DescriptorParameters params )
	{
		// get the input images for registration
		final Image<FloatType> img1, img2;
		
		if ( params.img1 != null )
			img1 = params.img1;
		else
			img1 = InteractiveDoG.convertToFloat( imp1, params.channel1 );
		
		img2 = InteractiveDoG.convertToFloat( imp2, params.channel2 );
		
		// extract Calibrations
		final Calibration cal1 = imp1.getCalibration();
		final Calibration cal2 = imp2.getCalibration();
		
		if ( params.dimensionality == 2 )
		{
			img1.setCalibration( new float[]{ (float)cal1.pixelWidth, (float)cal1.pixelHeight } );
			img2.setCalibration( new float[]{ (float)cal2.pixelWidth, (float)cal2.pixelHeight } );
		}
		else
		{
			img1.setCalibration( new float[]{ (float)cal1.pixelWidth, (float)cal1.pixelHeight, (float)cal1.pixelDepth } );
			img2.setCalibration( new float[]{ (float)cal2.pixelWidth, (float)cal2.pixelHeight, (float)cal2.pixelDepth } );			
		}
		
		// extract candidates
		final ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks1 = computeDoG( img1, (float)params.sigma1, (float)params.sigma2, params.lookForMaxima, params.lookForMinima, (float)params.threshold );
		final ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks2 = computeDoG( img2, (float)params.sigma1, (float)params.sigma2, params.lookForMaxima, params.lookForMinima, (float)params.threshold );

		// remove invalid peaks
		final int[] stats1 = removeInvalidAndCollectStatistics( peaks1 );
		final int[] stats2 = removeInvalidAndCollectStatistics( peaks2 );
		
		IJ.log( "Found " + peaks1.size() + " candidates for " + imp1.getTitle() + " (" + stats1[ 1 ] + " maxima, " + stats1[ 0 ] + " minima)" );
		IJ.log( "Found " + peaks2.size() + " candidates for " + imp2.getTitle() + " (" + stats2[ 1 ] + " maxima, " + stats2[ 0 ] + " minima)" );

		// find correspondence candidates
		final Matcher matcher = new SubsetMatcher( params.numNeighbors, params.numNeighbors + params.redundancy );
		ArrayList<PointMatch> candidates = getCorrespondenceCandidates( params.significance, matcher, peaks1, peaks2, null, params.dimensionality, img1, img2 );
		
		// compute ransac
		ArrayList<PointMatch> finalInliers = new ArrayList<PointMatch>();
		Model<?> finalModel = computeRANSAC( candidates, finalInliers, params.model.copy(), (float)params.ransacThreshold );

		// apply rotation-variant matching after applying the model until it converges
		if ( finalInliers.size() > finalModel.getMinNumMatches() )
		{
			int previousNumInliers = 0;
			int numInliers = 0;
			do
			{
				// get the correspondence candidates with the knowledge of the previous model
				candidates = getCorrespondenceCandidates( params.significance, matcher, peaks1, peaks2, finalModel, params.dimensionality, img1, img2 );
				
				// before we compute the RANSAC we will reset the coordinates of all points so that we directly get the correct model
				for ( final PointMatch pm : candidates )
				{
					((Particle)pm.getP1()).restoreCoordinates();
					((Particle)pm.getP2()).restoreCoordinates();
				}			
				
				// compute ransac
				previousNumInliers = finalInliers.size();				
				final ArrayList<PointMatch> inliers = new ArrayList<PointMatch>();
				Model<?> model2 = computeRANSAC( candidates, inliers, params.model.copy(), (float)params.ransacThreshold );
				numInliers = inliers.size();
				
				// update model if this one was better
				if ( numInliers > previousNumInliers )
				{
					finalModel = model2;
					finalInliers = inliers;
				}
			} 
			while ( numInliers > previousNumInliers );
		}
		else
		{
			IJ.log( "No inliers found, stopping. Tipp: You could increase the number of neighbors, redundancy or use a model that has more degrees of freedom." );
			return;
		}
		
		IJ.log( "" + finalModel );
		
		// set point rois if 2d and wanted
		if ( params.setPointsRois )
			setPointRois( imp1, imp2, finalInliers );
		
		// fuse if wanted
		if ( params.fuse )
		{
			if ( imp1.getType() == ImagePlus.GRAY32 || imp2.getType() == ImagePlus.GRAY32 )
				createOverlay( new FloatType(), imp1, imp2, finalModel, params.model.copy(), params.dimensionality );
			else if ( imp1.getType() == ImagePlus.GRAY16 || imp2.getType() == ImagePlus.GRAY16 )
				createOverlay( new UnsignedShortType(), imp1, imp2, finalModel, params.model.copy(), params.dimensionality );
			else
				createOverlay( new UnsignedByteType(), imp1, imp2, finalModel, params.model.copy(), params.dimensionality );
		}
	}
	
	protected static <T extends RealType<T>> void createOverlay( final T targetType, final ImagePlus imp1, final ImagePlus imp2, final Model<?> finalModel1, final Model<?> finalModel2, final int dimensionality ) 
	{
		// estimate the bounaries of the output image
		final float[] max1, max2;
		final float[] min1 = new float[ dimensionality ];
		final float[] min2= new float[ dimensionality ];
		
		if ( dimensionality == 2 )
		{
			max1 = new float[] { imp1.getWidth(), imp1.getHeight() };
			max2 = new float[] { imp2.getWidth(), imp2.getHeight() };
		}
		else
		{
			max1 = new float[] { imp1.getWidth(), imp1.getHeight(), imp1.getNSlices() };
			max2 = new float[] { imp2.getWidth(), imp2.getHeight(), imp2.getNSlices() };
			BeadRegistration.concatenateAxialScaling( (AbstractAffineModel3D)finalModel1, imp1.getCalibration().pixelDepth / imp1.getCalibration().pixelWidth );
			BeadRegistration.concatenateAxialScaling( (AbstractAffineModel3D)finalModel2, imp2.getCalibration().pixelDepth / imp2.getCalibration().pixelWidth );
		}
		
		final InvertibleBoundable boundable1 = (InvertibleBoundable)finalModel1;
		final InvertibleBoundable boundable2 = (InvertibleBoundable)finalModel2;
		
		IJ.log( imp1.getTitle() + ": " + Util.printCoordinates( min1 ) + " -> " + Util.printCoordinates( max1 ) );
		IJ.log( imp2.getTitle() + ": " + Util.printCoordinates( min2 ) + " -> " + Util.printCoordinates( max2 ) );
		
		boundable1.estimateBounds( min1, max1 );
		boundable2.estimateBounds( min2, max2 );

		final float[] minImg = new float[ dimensionality ];
		final float[] maxImg = new float[ dimensionality ];

		for ( int d = 0; d < dimensionality; ++d )
		{
			// the image might be rotated so that min is actually max
			maxImg[ d ] = Math.max( Math.max( max1[ d ], max2[ d ] ), Math.max( min1[ d ], min2[ d ]) );
			minImg[ d ] = Math.min( Math.min( max1[ d ], max2[ d ] ), Math.min( min1[ d ], min2[ d ]) );
		}
		
		IJ.log( imp1.getTitle() + ": " + Util.printCoordinates( min1 ) + " -> " + Util.printCoordinates( max1 ) );
		IJ.log( imp2.getTitle() + ": " + Util.printCoordinates( min2 ) + " -> " + Util.printCoordinates( max2 ) );
		IJ.log( "output: " + Util.printCoordinates( minImg ) + " -> " + Util.printCoordinates( maxImg ) );

		// the size of the new image
		final int[] size = new int[ dimensionality ];
		// the offset relative to the output image which starts with its local coordinates (0,0,0)
		final float[] offset = new float[ dimensionality ];
		
		for ( int d = 0; d < dimensionality; ++d )
		{
			size[ d ] = Math.round( maxImg[ d ] - minImg[ d ] ) + 1;
			offset[ d ] = minImg[ d ];			
		}
		
		IJ.log( "size: " + Util.printCoordinates( size ) );
		IJ.log( "offset: " + Util.printCoordinates( offset ) );
		
		// for output
		final ImageFactory<T> f = new ImageFactory<T>( targetType, new ImagePlusContainerFactory() );
		// the composite
		final ImageStack stack = new ImageStack( size[ 0 ], size[ 1 ] );

		// 2d
		if ( dimensionality == 2 )
		{			
			// transform all channels of imp1
			for ( int c = 1; c <= imp1.getNChannels(); ++ c )
			{
				final Image<T> out = f.createImage( size );
				fuseChannel( out, ImageJFunctions.convertFloat( new ImagePlus( "", imp1.getStack().getProcessor( c ) ) ), offset, boundable1 );
				try 
				{
					stack.addSlice( imp1.getTitle(), ((ImagePlusContainer)out.getContainer()).getImagePlus().getProcessor() );
				} 
				catch (ImgLibException e) 
				{
					IJ.log( "Output image has no ImageJ type: " + e );
				}
				
			}

			// transform all channels of imp2
			for ( int c = 1; c <= imp2.getNChannels(); ++ c )
			{
				final Image<T> out = f.createImage( size );
				fuseChannel( out, ImageJFunctions.convertFloat( new ImagePlus( "", imp2.getStack().getProcessor( c ) ) ), offset, boundable2 );
				try 
				{
					stack.addSlice( imp1.getTitle(), ((ImagePlusContainer)out.getContainer()).getImagePlus().getProcessor() );
				} 
				catch (ImgLibException e) 
				{
					IJ.log( "Output image has no ImageJ type: " + e );
				}
				
			}

			final ImagePlus result = new ImagePlus( "overlay " + imp1.getTitle() + "<->" + imp2.getTitle(), stack );
			// numchannels, z-slices, timepoints
			result.setDimensions( imp1.getNChannels() + imp2.getNChannels(), 1, 1 );
			final CompositeImage composite = new CompositeImage( result );
			composite.show();
		}
		else //3d
		{
			for ( int c = 1; c <= imp1.getNChannels(); ++c )
			{
				final Image<T> out = f.createImage( size );
				fuseChannel( out, ImageJFunctions.convertFloat( getImageChunk( imp1, c, 1 ) ), offset, boundable1 );
				try 
				{
					final ImagePlus outImp = ((ImagePlusContainer)out.getContainer()).getImagePlus();
					for ( int z = 1; z <= out.getDimension( 2 ); ++z )
						stack.addSlice( imp1.getTitle(), outImp.getStack().getProcessor( z ) );
				} 
				catch (ImgLibException e) 
				{
					IJ.log( "Output image has no ImageJ type: " + e );
				}				
			}
			
			//channel 2 ...
			for ( int c = 1; c <= imp2.getNChannels(); ++c )
			{
				final Image<T> out = f.createImage( size );
				fuseChannel( out, ImageJFunctions.convertFloat( getImageChunk( imp2, c, 1 ) ), offset, boundable2 );
				try 
				{
					final ImagePlus outImp = ((ImagePlusContainer)out.getContainer()).getImagePlus();
					for ( int z = 1; z <= out.getDimension( 2 ); ++z )
						stack.addSlice( imp2.getTitle(), outImp.getStack().getProcessor( z ) );
				} 
				catch (ImgLibException e) 
				{
					IJ.log( "Output image has no ImageJ type: " + e );
				}				
			}
			
			//convertXYZCT ...
			ImagePlus result = new ImagePlus( "overlay " + imp1.getTitle() + "<->" + imp2.getTitle(), stack );
			// numchannels, z-slices, timepoints (but right now the order is still XYZCT)
			result.setDimensions( size[ 2 ], imp1.getNChannels() + imp2.getNChannels(), 1 );
			
			result = rearrangeIntoXYZCT( result );
			
			final CompositeImage composite = new CompositeImage( result );
			composite.show();			
		}
	}
	
	/**
	 * Returns an {@link ImagePlus} for a 2d or 3d stack where ImageProcessors are not copied but just added.
	 * 
	 * @param imp - the input image
	 * @param channel - which channel (first channel is 1, NOT 0)
	 * @param timepoint - which timepoint (first timepoint is 1, NOT 0)
	 */
	public static ImagePlus getImageChunk( final ImagePlus imp, final int channel, final int timepoint )
	{
		if ( imp.getNSlices() == 1 )
		{
			return new ImagePlus( "", imp.getStack().getProcessor( imp.getStackIndex( channel, 1, timepoint ) ) );
		}
		else
		{
			final ImageStack stack = new ImageStack( imp.getWidth(), imp.getHeight() );
			
			for ( int z = 1; z < imp.getNSlices(); ++z )
			{
				final int index = imp.getStackIndex( channel, z, timepoint );
				final ImageProcessor ip = imp.getStack().getProcessor( index );
				stack.addSlice( imp.getStack().getSliceLabel( index ), ip );
			}
			
			return new ImagePlus( "", stack );
		}
			
	}
	
	/**
	 * Rearranges an ImageJ XYCZT Hyperstack into XYZCT without wasting memory for processing 3d images as a chunk,
	 * if it is already XYZCT it will shuffle it back to XYCZT
	 * 
	 * @param imp - the input {@link ImagePlus}
	 * @return - an {@link ImagePlus} which can be the same instance if the image is XYZT, XYZ, XYT or XY - otherwise a new instance
	 * containing the same processors but in the new order XYZCT
	 */
	public static ImagePlus rearrangeIntoXYZCT( final ImagePlus imp )
	{
		final int numChannels = imp.getNChannels();
		final int numTimepoints = imp.getNFrames();
		final int numZStacks = imp.getNSlices();
		
		// there is only one channel
		if ( numChannels == 1 )
			return imp;
		
		// it is only XYC(T)
		if ( numZStacks == 1 )
			return imp;
		
		// now we have to rearrange
		final ImageStack stack = new ImageStack( imp.getWidth(), imp.getHeight() );
		
		for ( int t = 1; t <= numTimepoints; ++t )
		{
			for ( int c = 1; c <= numChannels; ++c )
			{
				for ( int z = 1; z <= numZStacks; ++z )
				{
					final int index = imp.getStackIndex( c, z, t );
					final ImageProcessor ip = imp.getStack().getProcessor( index );
					stack.addSlice( imp.getStack().getSliceLabel( index ), ip );
				}
			}
		}
		
		String newTitle;
		if ( imp.getTitle().startsWith( "[XYZCT]" ) )
			newTitle = imp.getTitle().substring( 8, imp.getTitle().length() );
		else
			newTitle = "[XYZCT] " + imp.getTitle();
		
		final ImagePlus result = new ImagePlus( newTitle, stack );
		// numchannels, z-slices, timepoints 
		// but of course now reversed...
		result.setDimensions( numZStacks, numChannels, numTimepoints );
		final CompositeImage composite = new CompositeImage( result );
		
		return composite;
	}

	/**
	 * Fuse one slice/volume (one channel)
	 * 
	 * @param output - same the type of the ImagePlus input
	 * @param input - FloatType, because of Interpolation that needs to be done
	 * @param transform - the transformation
	 */
	protected static <T extends RealType<T>> void fuseChannel( final Image<T> output, final Image<FloatType> input, final float[] offset, final InvertibleCoordinateTransform transform )
	{
		final int dims = output.getNumDimensions();
		final LocalizableCursor<T> out = output.createLocalizableCursor();
		final Interpolator<FloatType> in = input.createInterpolator( new LinearInterpolatorFactory<FloatType>( new OutOfBoundsStrategyValueFactory<FloatType>() ) );
		
		final float[] tmp = new float[ input.getNumDimensions() ];
		
		try 
		{
			while ( out.hasNext() )
			{
				out.fwd();
				
				for ( int d = 0; d < dims; ++d )
					tmp[ d ] = out.getPosition( d ) + offset[ d ];
				
				transform.applyInverseInPlace( tmp );
	
				in.setPosition( tmp );			
				out.getType().setReal( in.getType().get() );
			}
		} 
		catch (NoninvertibleModelException e) 
		{
			IJ.log( "Cannot invert model, qutting." );
			return;
		}
	}
	
	
	protected void setPointRois( final ImagePlus imp1, final ImagePlus imp2, final ArrayList<PointMatch> inliers )
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
	
	protected Model<?> computeRANSAC( final ArrayList<PointMatch> candidates, final ArrayList<PointMatch> inliers, final Model<?> model, final float maxEpsilon )
	{		
		boolean modelFound = false;
		float minInlierRatio = 0.05f;
		int numIterations = 10000;
		
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
				IJ.log( "Remaining inliers after RANSAC (" + model.getClass().getSimpleName() + "): " + inliers.size() + " of " + candidates.size() + " with average error " + model.getCost() );
				model.fit( inliers );
			}
			else
			{
				IJ.log( "NO Model found after RANSAC (" + model.getClass().getSimpleName() + ") of " + candidates.size() );
			}
		}
		catch ( Exception e )
		{
			IJ.log( "NO Model found after RANSAC (" + model.getClass().getSimpleName() + ") of " + candidates.size() );
			System.out.println( e.toString() );
			return null;
		}
		
		return model;
	}

	protected ArrayList<PointMatch> getCorrespondenceCandidates( final double nTimesBetter, final Matcher matcher, 
			ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks1, ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks2, 
			final Model<?> model, final int dimensionality, final Image<?> img1, final Image<?> img2 )
	{
		// two new lists
		ArrayList<Particle> listA = new ArrayList<Particle>();
		ArrayList<Particle> listB = new ArrayList<Particle>();
		
		int id = 0;
		
		if ( model == null )
		{
			// no prior model known, do a locally rigid matching
			float zStretching = img1.getNumDimensions() >= 3 ? img1.getCalibration( 2 ) / img1.getCalibration( 0 ) : 1;
			for ( DifferenceOfGaussianPeak<FloatType> peak : peaks1 )
				listA.add( new Particle( id++, peak, zStretching ) );
			zStretching = img2.getNumDimensions() >= 3 ? img2.getCalibration( 2 ) / img2.getCalibration( 0 ) : 1;
			for ( DifferenceOfGaussianPeak<FloatType> peak : peaks2 )
				listB.add( new Particle( id++, peak, zStretching ) );
		}
		else
		{
			// prior model known, apply to the points before matching and then do a simple descriptor matching
			float zStretching = img1.getNumDimensions() >= 3 ? img1.getCalibration( 2 ) / img1.getCalibration( 0 ) : 1;
			for ( DifferenceOfGaussianPeak<FloatType> peak : peaks1 )
			{
				final Particle particle = new Particle( id++, peak, zStretching );			
				particle.apply( model );
				for ( int d = 0; d < particle.getL().length; ++d )
					particle.getL()[ d ] = particle.getW()[ d ];
				listA.add( particle );
			}
			
			zStretching = img2.getNumDimensions() >= 3 ? img2.getCalibration( 2 ) / img2.getCalibration( 0 ) : 1;
			for ( DifferenceOfGaussianPeak<FloatType> peak : peaks2 )
			{
				final Particle particle = new Particle( id++, peak, zStretching );
				listB.add( particle );
			}
			//0 (192.01309, 394.9002, 56.54292) ((191.87242, 510.94934, 52.197083)) -> 561 (192.35054, 514.0911, 52.14846) ((192.35054, 514.0911, 52.14846))
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
	
	protected final ArrayList<PointMatch> findCorrespondingDescriptors( final ArrayList<AbstractPointDescriptor> descriptorsA, final ArrayList<AbstractPointDescriptor> descriptorsB, final float nTimesBetter )
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

	protected ArrayList< AbstractPointDescriptor > createSimplePointDescriptors( final KDTree< Particle > tree, final ArrayList< Particle > basisPoints, 
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

	protected ArrayList< AbstractPointDescriptor > createModelPointDescriptors( final KDTree< Particle > tree, final ArrayList< Particle > basisPoints, 
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
	
	protected ArrayList<DifferenceOfGaussianPeak<FloatType>> computeDoG( final Image<FloatType> image, final float sigma1, final float sigma2, 
			final boolean lookForMaxima, final boolean lookForMinima, final float threshold )
	{
		return DetectionSegmentation.extractBeadsLaPlaceImgLib( image, new OutOfBoundsStrategyMirrorFactory<FloatType>(), 0.5f, sigma1, sigma2, threshold, threshold/4, lookForMaxima, lookForMinima, ViewStructure.DEBUG_MAIN );
	}
	
	protected int[] removeInvalidAndCollectStatistics( ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks )
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
