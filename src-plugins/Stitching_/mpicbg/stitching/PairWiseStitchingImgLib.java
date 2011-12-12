package mpicbg.stitching;

import fiji.stacks.Hyperstack_rearranger;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.fft.PhaseCorrelation;
import mpicbg.imglib.algorithm.fft.PhaseCorrelationPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

/**
 * Pairwise Stitching of two ImagePlus using ImgLib1 and PhaseCorrelation.
 * It deals with aligning two slices (2d) or stacks (3d) having an arbitrary
 * amount of channels. If the ImagePlus contains several time-points it will 
 * only consider the first time-point as this requires global optimization of 
 * many independent 2d/3d <-> 2d/3d alignments.
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class PairWiseStitchingImgLib 
{
	public static PairWiseStitchingResult stitchPairwise( final ImagePlus imp1, final ImagePlus imp2, Roi roi1, Roi roi2, final int timepoint1, final int timepoint2, final StitchingParameters params )
	{
		PairWiseStitchingResult result = null;
		roi1 = getOnlyRectangularRoi( roi1 );
		roi2 = getOnlyRectangularRoi( roi2 );
		
		// can both images be wrapped into imglib without copying
		final boolean canWrap = !StitchingParameters.alwaysCopy && canWrapIntoImgLib( imp1, roi1, params.channel1 ) && canWrapIntoImgLib( imp2, roi2, params.channel2 );
		
		
		//
		// the ugly but correct way into generic programming...
		//
		if ( canWrap )
		{
			if ( imp1.getType() == ImagePlus.GRAY32 )
			{
				final Image<FloatType> image1 = getWrappedImageFloat( imp1, params.channel1, timepoint1 );
				
				if ( imp2.getType() == ImagePlus.GRAY32 )
					result = performStitching( image1, getWrappedImageFloat( imp2, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY16 )
					result = performStitching( image1, getWrappedImageUnsignedShort( imp2, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY8 )
					result = performStitching( image1, getWrappedImageUnsignedByte( imp2, params.channel2, timepoint2 ), params );
				else
					IJ.log( "Unknown image type: " + imp2.getType() );
			}
			else if ( imp1.getType() == ImagePlus.GRAY16 )
			{
				final Image<UnsignedShortType> image1 = getWrappedImageUnsignedShort( imp1, params.channel1, timepoint1 );
				
				if ( imp2.getType() == ImagePlus.GRAY32 )
					result = performStitching( image1, getWrappedImageFloat( imp2, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY16 )
					result = performStitching( image1, getWrappedImageUnsignedShort( imp2, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY8 )
					result = performStitching( image1, getWrappedImageUnsignedByte( imp2, params.channel2, timepoint2 ), params );
				else
					IJ.log( "Unknown image type: " + imp2.getType() );
			} 
			else if ( imp1.getType() == ImagePlus.GRAY8 )
			{
				final Image<UnsignedByteType> image1 = getWrappedImageUnsignedByte( imp1, params.channel1, timepoint1 );
				
				if ( imp2.getType() == ImagePlus.GRAY32 )
					result = performStitching( image1, getWrappedImageFloat( imp2, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY16 )
					result = performStitching( image1, getWrappedImageUnsignedShort( imp2, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY8 )
					result = performStitching( image1, getWrappedImageUnsignedByte( imp2, params.channel2, timepoint2 ), params );
				else
					IJ.log( "Unknown image type: " + imp2.getType() );
			} 
			else
			{
				IJ.log( "Unknown image type: " + imp1.getType() );			
			}
		}
		else
		{
			final ImageFactory<UnsignedByteType> imgFactoryByte = new ImageFactory<UnsignedByteType>( new UnsignedByteType(), StitchingParameters.phaseCorrelationFactory );
			final ImageFactory<UnsignedShortType> imgFactoryShort = new ImageFactory<UnsignedShortType>( new UnsignedShortType(), StitchingParameters.phaseCorrelationFactory );
			final ImageFactory<FloatType> imgFactoryFloat = new ImageFactory<FloatType>( new FloatType(), StitchingParameters.phaseCorrelationFactory );
			
			if ( imp1.getType() == ImagePlus.GRAY32 )
			{
				final Image< FloatType > image1 = getImage( imp1, roi1, imgFactoryFloat, params.channel1, timepoint1 );
				
				if ( imp2.getType() == ImagePlus.GRAY32 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryFloat, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY16 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryShort, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY8 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryByte, params.channel2, timepoint2 ), params );
				else
					IJ.log( "Unknown image type: " + imp2.getType() );					
			}
			else if ( imp1.getType() == ImagePlus.GRAY16 )
			{
				final Image< UnsignedShortType > image1 = getImage( imp1, roi1, imgFactoryShort, params.channel1, timepoint1 );
				
				if ( imp2.getType() == ImagePlus.GRAY32 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryFloat, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY16 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryShort, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY8 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryByte, params.channel2, timepoint2 ), params );
				else
					IJ.log( "Unknown image type: " + imp2.getType() );					
			}
			else if ( imp1.getType() == ImagePlus.GRAY8 )
			{
				final Image< UnsignedByteType > image1 = getImage( imp1, roi1, imgFactoryByte, params.channel1, timepoint1 );
				
				if ( imp2.getType() == ImagePlus.GRAY32 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryFloat, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY16 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryShort, params.channel2, timepoint2 ), params );
				else if ( imp2.getType() == ImagePlus.GRAY8 )
					result = performStitching( image1, getImage( imp2, roi2, imgFactoryByte, params.channel2, timepoint2 ), params );
				else
					IJ.log( "Unknown image type: " + imp2.getType() );					
			}
			else
			{
				IJ.log( "Unknown image type: " + imp1.getType() );			
			}
		}
		
		// add the offset to the shift
		if ( roi2 != null )
		{
			result.offset[ 0 ] -= roi2.getBounds().x;
			result.offset[ 1 ] -= roi2.getBounds().y;
		}	
		
		if ( roi1 != null )
		{
			result.offset[ 0 ] += roi1.getBounds().x;
			result.offset[ 1 ] += roi1.getBounds().y;			
		}
		
		return result;
	}

	public static < T extends RealType<T>, S extends RealType<S> > PairWiseStitchingResult performStitching( final Image<T> img1, final Image<S> img2, final StitchingParameters params )
	{
		if ( img1 == null )
		{
			IJ.log( "Image 1 could not be wrapped." );
			return null;
		}
		else if ( img2 == null )
		{
			IJ.log( "Image 2 could not be wrapped." );
			return null;
		}
		else if ( params == null )
		{
			IJ.log( "Parameters are null." );
			return null;
		}
		
		final PairWiseStitchingResult result = computePhaseCorrelation( img1, img2, params.checkPeaks, params.subpixelAccuracy );
		
		return result;
	}
	
	public static < T extends RealType<T>, S extends RealType<S> > PairWiseStitchingResult computePhaseCorrelation( final Image<T> img1, final Image<S> img2, final int numPeaks, final boolean subpixelAccuracy )
	{
		final PhaseCorrelation< T, S > phaseCorr = new PhaseCorrelation<T, S>( img1, img2 );
		phaseCorr.setInvestigateNumPeaks( numPeaks );
		
		if ( subpixelAccuracy )
			phaseCorr.setKeepPhaseCorrelationMatrix( true );
		
		phaseCorr.setComputeFFTinParalell( true );
		phaseCorr.process();

		// result
		final PhaseCorrelationPeak pcp = phaseCorr.getShift();
		final float[] shift = new float[ img1.getNumDimensions() ];
		final PairWiseStitchingResult result;
		
		if ( subpixelAccuracy )
		{
			final Image<FloatType> pcm = phaseCorr.getPhaseCorrelationMatrix();		
		
			final ArrayList<DifferenceOfGaussianPeak<FloatType>> list = new ArrayList<DifferenceOfGaussianPeak<FloatType>>();		
			final Peak p = new Peak( pcp );
			list.add( p );
					
			final SubpixelLocalization<FloatType> spl = new SubpixelLocalization<FloatType>( pcm, list );
			final boolean move[] = new boolean[ pcm.getNumDimensions() ];
			for ( int i = 0; i < pcm.getNumDimensions(); ++i )
				move[ i ] = false;
			spl.setCanMoveOutside( true );
			spl.setAllowedToMoveInDim( move );
			spl.setMaxNumMoves( 0 );
			spl.setAllowMaximaTolerance( false );
			spl.process();
			
			final Peak peak = (Peak)list.get( 0 );
			
			for ( int d = 0; d < img1.getNumDimensions(); ++d )
				shift[ d ] = peak.getPCPeak().getPosition()[ d ] + peak.getSubPixelPositionOffset( d );
			
			pcm.close();
			
			result = new PairWiseStitchingResult( shift, pcp.getCrossCorrelationPeak(), p.getValue().get() );
		}
		else
		{
			for ( int d = 0; d < img1.getNumDimensions(); ++d )
				shift[ d ] = pcp.getPosition()[ d ];
			
			result = new PairWiseStitchingResult( shift, pcp.getCrossCorrelationPeak(), pcp.getPhaseCorrelationPeak() );
		}
		
		return result;
	}

	/**
	 * return an {@link Image}<T> as input for the PhaseCorrelation.
	 * 
	 * @param imp - the {@link ImagePlus}
	 * @param imgFactory - the {@link ImageFactory} defining wher to put it into
	 * @param channel - which channel (if channel=0 means average all channels)
	 * @param timepoint - which timepoint
	 * 
	 * @return - the {@link Image} or null if it was not an ImagePlus.GRAY8, ImagePlus.GRAY16 or ImagePlus.GRAY32
	 */
	public static < T extends RealType<T> > Image<T> getImage( final ImagePlus imp, Roi roi, final ImageFactory<T> imgFactory, final int channel, final int timepoint )
	{
		// first test the roi
		roi = getOnlyRectangularRoi( roi );
		
		// how many dimensions?
		final int numDimensions;		
		if ( imp.getNSlices() > 1 )
			numDimensions = 3;
		else
			numDimensions = 2;
		
		// the size of the image
		final int[] size = new int[ numDimensions ];
		final int[] offset = new int[ numDimensions ];
		
		if ( roi == null )
		{
			size[ 0 ] = imp.getWidth();
			size[ 1 ] = imp.getHeight();
			
			if ( numDimensions == 3 )
				size[ 2 ] = imp.getNSlices();
		}
		else
		{
			size[ 0 ] = roi.getBounds().width;
			size[ 1 ] = roi.getBounds().height;

			offset[ 0 ] = roi.getBounds().x;
			offset[ 1 ] = roi.getBounds().y;
			
			if ( numDimensions == 3 )
				size[ 2 ] = imp.getNSlices();
		}
		
		// create the Image
		final Image<T> img = imgFactory.createImage( size );
		final boolean success;
		
		// copy the content
		if ( channel == 0 )
		{
			// we need to average all channels
			success = averageAllChannels( img, offset, imp, timepoint );
		}
		else
		{
			// otherwise only copy one channel
			success = fillInChannel( img, offset, imp, channel, timepoint );
		}
		
		if ( success )
		{
			return img;
		}
		else
		{
			img.close();
			return null;
		}
	}
	
	/**
	 * Averages all channels into the target image. The size is given by the dimensions of the target image,
	 * the offset (if applicable) is given by an extra field
	 * 
	 * @param target - the target Image
	 * @param offset - the offset of the area (might be [0,0] or [0,0,0])
	 * @param imp - the input ImagePlus
	 * @param timepoint - for which timepoint
	 * 
	 * @return true if successful, false if the ImagePlus type was unknow
	 */
	public static < T extends RealType< T > > boolean averageAllChannels( final Image< T > target, final int[] offset, final ImagePlus imp, final int timepoint )
	{
		final int numChannels = imp.getNChannels();
		
		if ( imp.getType() == ImagePlus.GRAY8 )
		{
			final ArrayList< Image< UnsignedByteType > > images = new ArrayList<Image<UnsignedByteType>>();

			// first get wrapped instances of all channels
			for ( int c = 1; c <= numChannels; ++c )
				images.add( getWrappedImageUnsignedByte( imp, c, timepoint ) );			

			averageAllChannels( target, images, offset );			
			return true;
		}
		else if ( imp.getType() == ImagePlus.GRAY16 )
		{
			final ArrayList< Image< UnsignedShortType > > images = new ArrayList<Image<UnsignedShortType>>();

			// first get wrapped instances of all channels
			for ( int c = 1; c <= numChannels; ++c )
				images.add( getWrappedImageUnsignedShort( imp, c, timepoint ) );			

			averageAllChannels( target, images, offset );
			return true;
		}
		else if ( imp.getType() == ImagePlus.GRAY32 )
		{
			final ArrayList< Image< FloatType > > images = new ArrayList<Image<FloatType>>();

			// first get wrapped instances of all channels
			for ( int c = 1; c <= numChannels; ++c )
				images.add( getWrappedImageFloat( imp, c, timepoint ) );
			
			averageAllChannels( target, images, offset );
			return true;
		}
		else
		{
			IJ.log( "Unknow image type: " + imp.getType() );
			return false;
		}
	}

	/**
	 * Averages all channels into the target image. The size is given by the dimensions of the target image,
	 * the offset (if applicable) is given by an extra field
	 * 
	 * @param target - the target Image
	 * @param offset - the offset of the area (might be [0,0] or [0,0,0])
	 * @param imp - the input ImagePlus
	 * @param timepoint - for which timepoint
	 * 
	 * @return true if successful, false if the ImagePlus type was unknow
	 */
	public static < T extends RealType< T > > boolean fillInChannel( final Image< T > target, final int[] offset, final ImagePlus imp, final int channel, final int timepoint )
	{
		if ( imp.getType() == ImagePlus.GRAY8 )
		{
			final ArrayList< Image< UnsignedByteType > > images = new ArrayList<Image<UnsignedByteType>>();

			// first get wrapped instances of all channels
			images.add( getWrappedImageUnsignedByte( imp, channel, timepoint ) );			

			averageAllChannels( target, images, offset );			
			return true;
		}
		else if ( imp.getType() == ImagePlus.GRAY16 )
		{
			final ArrayList< Image< UnsignedShortType > > images = new ArrayList<Image<UnsignedShortType>>();

			// first get wrapped instances of all channels
			images.add( getWrappedImageUnsignedShort( imp, channel, timepoint ) );			

			averageAllChannels( target, images, offset );
			return true;
		}
		else if ( imp.getType() == ImagePlus.GRAY32 )
		{
			final ArrayList< Image< FloatType > > images = new ArrayList<Image<FloatType>>();

			// first get wrapped instances of all channels
			images.add( getWrappedImageFloat( imp, channel, timepoint ) );
			
			averageAllChannels( target, images, offset );
			return true;
		}
		else
		{
			IJ.log( "Unknow image type: " + imp.getType() );
			return false;
		}
	}

	/**
	 * Averages all channels into the target image. The size is given by the dimensions of the target image,
	 * the offset (if applicable) is given by an extra field
	 * 
	 * @param target - the target Image
	 * @param offset - the offset of the area (might be [0,0] or [0,0,0])
	 * @param sources - a list of input Images
	 */
	protected static < T extends RealType< T >, S extends RealType< S > > void averageAllChannels( final Image< T > target, final ArrayList< Image< S > > sources, final int[] offset )
	{
		// get the major numbers
		final int numDimensions = target.getNumDimensions();
		final float numImages = sources.size();
		long imageSize = target.getDimension( 0 );
		
		for ( int d = 1; d < target.getNumDimensions(); ++d )
			imageSize *= target.getDimension( d );

		// run multithreaded
		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads();

        final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, threads.length );
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();
        
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( myNumber );
                	final long startPos = myChunk.getStartPosition();
                	final long loopSize = myChunk.getLoopSize();
                	
            		// the cursor for the output
            		final LocalizableCursor< T > targetCursor =  target.createLocalizableCursor();
            		
            		// the input cursors
            		final ArrayList< LocalizableByDimCursor< S > > sourceCursors = new ArrayList< LocalizableByDimCursor< S > > ();
            		
            		for ( final Image< S > source : sources )
            			sourceCursors.add( source.createLocalizableByDimCursor() );
            		
            		// temporary array
            		final int[] location = new int[ numDimensions ]; 

            		// move to the starting position of the current thread
            		targetCursor.fwd( startPos );
                    
            		// do as many pixels as wanted by this thread
                    for ( long j = 0; j < loopSize; ++j )
            		{
            			targetCursor.fwd();
            			targetCursor.getPosition( location );
            			
            			for ( int d = 0; d < numDimensions; ++d )
            				location[ d ] += offset[ d ];
            			
            			float sum = 0;
            			
            			for ( final LocalizableByDimCursor< S > sourceCursor : sourceCursors )
            			{
            				sourceCursor.setPosition( location );
            				sum += sourceCursor.getType().getRealFloat();
            			}
            			
            			targetCursor.getType().setReal( sum / numImages );
            		}                	
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );		
	}

	/**
	 * return an {@link Image} of {@link UnsignedByteType} as input for the PhaseCorrelation. If no rectangular roi
	 * is selected, it will only wrap the existing ImagePlus!
	 * 
	 * @param targetType - which {@link RealType}
	 * @param channel - which channel (if channel=0 means average all channels)
	 * @param timepoint - which timepoint
	 * @param imp - the {@link ImagePlus}
	 * 
	 * @return - the {@link Image} or null if it was not an ImagePlus.GRAY8 or if channel = 0
	 */
	public static Image<UnsignedByteType> getWrappedImageUnsignedByte( final ImagePlus imp, final int channel, final int timepoint )
	{
		if ( channel == 0 || imp.getType() != ImagePlus.GRAY8 )
			return null;
		else
			return ImageJFunctions.wrapByte( Hyperstack_rearranger.getImageChunk( imp, channel, timepoint ) );
	}

	/**
	 * return an {@link Image} of {@link UnsignedShortType} as input for the PhaseCorrelation. If no rectangular roi
	 * is selected, it will only wrap the existing ImagePlus!
	 * 
	 * @param targetType - which {@link RealType}
	 * @param channel - which channel (if channel=0 means average all channels)
	 * @param timepoint - which timepoint
	 * @param imp - the {@link ImagePlus}
	 * 
	 * @return - the {@link Image} or null if it was not an ImagePlus.GRAY16 or if channel = 0
	 */
	public static Image<UnsignedShortType> getWrappedImageUnsignedShort( final ImagePlus imp, final int channel, final int timepoint )
	{
		if ( channel == 0 || imp.getType() != ImagePlus.GRAY16 )
			return null;
		else
			return ImageJFunctions.wrapShort( Hyperstack_rearranger.getImageChunk( imp, channel, timepoint ) );
	}

	/**
	 * return an {@link Image} of {@link FloatType} as input for the PhaseCorrelation. If no rectangular roi
	 * is selected, it will only wrap the existing ImagePlus!
	 * 
	 * @param targetType - which {@link RealType}
	 * @param channel - which channel (if channel=0 means average all channels)
	 * @param timepoint - which timepoint
	 * @param imp - the {@link ImagePlus}
	 * 
	 * @return - the {@link Image} or null if it was not an ImagePlus.GRAY32 or if channel = 0
	 */
	public static Image<FloatType> getWrappedImageFloat( final ImagePlus imp, final int channel, final int timepoint )
	{
		if ( channel == 0 || imp.getType() != ImagePlus.GRAY32 )
			return null;
		else
			return ImageJFunctions.wrapFloat( Hyperstack_rearranger.getImageChunk( imp, channel, timepoint ) );
	}

	/**
	 * Determines if this imageplus with these parameters can be wrapped directly into an Image<T>.
	 * This is important, because if we would wrap the first but not the second image, they would
	 * have different {@link ImageFactory}s
	 * 
	 * @param imp - the ImagePlus
	 * @param channel - which channel (if channel=0 means average all channels)
	 * 
	 * @return true if it can be wrapped, otherwise false
	 */
	public static boolean canWrapIntoImgLib( final ImagePlus imp, Roi roi, final int channel )
	{
		// first test the roi
		roi = getOnlyRectangularRoi( roi );
		
		if ( roi == null && channel > 0 )
			return true;
		else
			return false;
	}

	protected static Roi getOnlyRectangularRoi( Roi roi )
	{
		// we can only do rectangular rois
		if ( roi != null && roi.getType() != Roi.RECTANGLE )
			return null;
		else
			return roi;
	}

	/*
	protected static Roi getOnlyRectangularRoi( final ImagePlus imp )
	{
		Roi roi = imp.getRoi();
		
		// we can only do rectangular rois
		if ( roi != null && roi.getType() != Roi.RECTANGLE )
		{
			IJ.log( "WARNING: roi for " + imp.getTitle() + " is not a rectangle, we have to ignore it." );
			roi = null;
		}

		return roi;
	}
	*/
}
