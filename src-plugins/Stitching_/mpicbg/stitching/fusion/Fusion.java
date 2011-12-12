package mpicbg.stitching.fusion;

import fiji.stacks.Hyperstack_rearranger;
import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.imageplus.ImagePlusContainer;
import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.exception.ImgLibException;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.interpolation.nearestneighbor.NearestNeighborInterpolatorFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.NoninvertibleModelException;

/**
 * Manages the fusion for all types except the overlayfusion
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class Fusion 
{
	/**
	 * 
	 * @param targetType
	 * @param images
	 * @param models
	 * @param dimensionality
	 * @param subpixelResolution - if there is no subpixel resolution, we do not need to convert to float as no interpolation is necessary, we can compute everything with RealType
	 */
	public static < T extends RealType< T > > ImagePlus fuse( final T targetType, final ArrayList< ImagePlus > images, final ArrayList< InvertibleBoundable > models, 
			final int dimensionality, final boolean subpixelResolution, final int fusionType )
	{
		// first we need to estimate the boundaries of the new image
		final float[] offset = new float[ dimensionality ];
		final int[] size = new int[ dimensionality ];
		final int numTimePoints = images.get( 0 ).getNFrames();
		final int numChannels = images.get( 0 ).getNChannels();
		
		estimateBounds( offset, size, images, models, dimensionality );
		
		// for output
		final ImageFactory<T> f = new ImageFactory<T>( targetType, new ImagePlusContainerFactory() );
		
		// the final composite
		final ImageStack stack = new ImageStack( size[ 0 ], size[ 1 ] );

		//"Overlay into composite image"
		for ( int t = 1; t <= numTimePoints; ++t )
		{
			for ( int c = 1; c <= numChannels; ++c )
			{
				// create the 2d/3d target image for the current channel and timepoint 
				final Image< T > out = f.createImage( size );

				// init the fusion
				PixelFusion fusion = null;
				
				if ( fusionType == 1 ) 
					fusion = new AveragePixelFusion();
				else if ( fusionType == 2 )
					fusion = new MedianPixelFusion();
				else if ( fusionType == 3 )
					fusion = new MaxPixelFusion();
				else if ( fusionType == 4)
					fusion = new MinPixelFusion();	
				
				// extract the complete blockdata
				if ( subpixelResolution )
				{
					final ArrayList< ImageInterpolation< FloatType > > blockData = new ArrayList< ImageInterpolation< FloatType > >();

					// for linear interpolation we want to mirror, otherwise we get black areas at the first and last pixel of each image
					final InterpolatorFactory< FloatType > interpolatorFactory = new LinearInterpolatorFactory<FloatType>( new OutOfBoundsStrategyMirrorFactory<FloatType>() );
					
					for ( final ImagePlus imp : images )
						blockData.add( new ImageInterpolation<FloatType>( ImageJFunctions.convertFloat( Hyperstack_rearranger.getImageChunk( imp, c, t ) ), interpolatorFactory ) );
					
					// init blending with the images
					if ( fusionType == 0 )
						fusion = new BlendingPixelFusion( blockData );
					
					fuseBlock( out, blockData, offset, models, fusion );
				}
				else
				{
					// can be a mixture of different RealTypes
					final ArrayList< ImageInterpolation< ? extends RealType< ? > > > blockData = new ArrayList< ImageInterpolation< ? extends RealType< ? > > >();

					final InterpolatorFactory< FloatType > interpolatorFactoryFloat = new NearestNeighborInterpolatorFactory< FloatType >( new OutOfBoundsStrategyValueFactory<FloatType>() );
					final InterpolatorFactory< UnsignedShortType > interpolatorFactoryShort = new NearestNeighborInterpolatorFactory< UnsignedShortType >( new OutOfBoundsStrategyValueFactory<UnsignedShortType>() );
					final InterpolatorFactory< UnsignedByteType > interpolatorFactoryByte = new NearestNeighborInterpolatorFactory< UnsignedByteType >( new OutOfBoundsStrategyValueFactory<UnsignedByteType>() );

					for ( final ImagePlus imp : images )
					{
						if ( imp.getType() == ImagePlus.GRAY32 )
							blockData.add( new ImageInterpolation<FloatType>( ImageJFunctions.wrapFloat( Hyperstack_rearranger.getImageChunk( imp, c, t ) ), interpolatorFactoryFloat ) );
						else if ( imp.getType() == ImagePlus.GRAY16 )
							blockData.add( new ImageInterpolation<UnsignedShortType>( ImageJFunctions.wrapShort( Hyperstack_rearranger.getImageChunk( imp, c, t ) ), interpolatorFactoryShort ) );
						else
							blockData.add( new ImageInterpolation<UnsignedByteType>( ImageJFunctions.wrapByte( Hyperstack_rearranger.getImageChunk( imp, c, t ) ), interpolatorFactoryByte ) );
					}
					
					// init blending with the images
					if ( fusionType == 0 )
						fusion = new BlendingPixelFusion( blockData );					

					fuseBlock( out, blockData, offset, models, fusion );
				}
				
				// add to stack
				try 
				{
					final ImagePlus outImp = ((ImagePlusContainer<?,?>)out.getContainer()).getImagePlus();
					for ( int z = 1; z <= out.getDimension( 2 ); ++z )
						stack.addSlice( "", outImp.getStack().getProcessor( z ) );
				} 
				catch (ImgLibException e) 
				{
					IJ.log( "Output image has no ImageJ type: " + e );
				}				
			}
		}
		
		//convertXYZCT ...
		ImagePlus result = new ImagePlus( "", stack );
		
		// numchannels, z-slices, timepoints (but right now the order is still XYZCT)
		if ( dimensionality == 3 )
		{
			result.setDimensions( size[ 2 ], numChannels, numTimePoints );
			result = OverlayFusion.switchZCinXYCZT( result );
			return new CompositeImage( result, CompositeImage.COMPOSITE );
		}
		else
		{
			//IJ.log( "ch: " + imp.getNChannels() );
			//IJ.log( "slices: " + imp.getNSlices() );
			//IJ.log( "frames: " + imp.getNFrames() );
			result.setDimensions( numChannels, 1, numTimePoints );
			
			if ( numChannels > 1 || numTimePoints > 1 )
				return new CompositeImage( result, CompositeImage.COMPOSITE );
			else
				return result;
		}
	}
	
	/**
	 * Fuse one slice/volume (one channel)
	 * 
	 * @param output - same the type of the ImagePlus input
	 * @param input - FloatType, because of Interpolation that needs to be done
	 * @param transform - the transformation
	 */
	protected static <T extends RealType<T>> void fuseBlock( final Image<T> output, final ArrayList< ? extends ImageInterpolation< ? extends RealType< ? > > > input, final float[] offset, 
			final ArrayList< InvertibleBoundable > transform, final PixelFusion fusion )
	{
		final int numDimensions = output.getNumDimensions();
		final int numImages = input.size();
		long imageSize = output.getDimension( 0 );
		
		for ( int d = 1; d < output.getNumDimensions(); ++d )
			imageSize *= output.getDimension( d );

		final int[][] max = new int[ numImages ][ numDimensions ];
		for ( int i = 0; i < numImages; ++i )
			for ( int d = 0; d < numDimensions; ++d )
				max[ i ][ d ] = input.get( i ).getImage().getDimension( d ) - 1; 
		
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
                	
            		final LocalizableCursor<T> out = output.createLocalizableCursor();
            		final ArrayList<Interpolator<? extends RealType<?>>> in = new ArrayList<Interpolator<? extends RealType<?>>>();
            		
            		for ( int i = 0; i < numImages; ++i )
            			in.add( input.get( i ).createInterpolator() );
            		
            		final float[][] tmp = new float[ numImages ][ output.getNumDimensions() ];
            		final PixelFusion myFusion = fusion.copy();
            		
            		try 
            		{
                		// move to the starting position of the current thread
            			out.fwd( startPos );
            			
                		// do as many pixels as wanted by this thread
                        for ( long j = 0; j < loopSize; ++j )
                        {
            				out.fwd();
            				
            				// get the current position in the output image
            				for ( int d = 0; d < numDimensions; ++d )
            				{
            					final float value = out.getPosition( d ) + offset[ d ];
            					
            					for ( int i = 0; i < numImages; ++i )
            						tmp[ i ][ d ] = value;
            				}
            				
            				// transform and compute output value
            				myFusion.clear();
            				
            				// loop over all images for this output location
A:        					for ( int i = 0; i < numImages; ++i )
        					{
        						transform.get( i ).applyInverseInPlace( tmp[ i ] );
            	
        						// test if inside
        						for ( int d = 0; d < numDimensions; ++d )
        							if ( tmp[ i ][ d ] < 0 || tmp[ i ][ d ] > max[ i ][ d ] )
        								continue A;
        						
        						in.get( i ).setPosition( tmp[ i ] );			
        						myFusion.addValue( in.get( i ).getType().getRealFloat(), i, tmp[ i ] );
        					}
            				
            				// set value
    						out.getType().setReal( myFusion.getValue() );
                        }
            		} 
            		catch ( NoninvertibleModelException e ) 
            		{
            			IJ.log( "Cannot invert model, qutting." );
            			return;
            		}

                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
	}

	/**
	 * Estimate the bounds of the output image. If there are more models than images, we assume that this encodes for more timepoints.
	 * E.g. 2 Images and 10 models would mean 5 timepoints. The arrangement of the models should be as follows:
	 * 
	 * image1 timepoint1
	 * image2 timepoint1
	 * image1 timepoint2
	 * ...
	 * image2 timepoint5
	 * 
	 * @param offset - the offset, will be computed
	 * @param size - the size, will be computed
	 * @param images - all imageplus in a list
	 * @param models - all models
	 * @param dimensionality - which dimensionality (2 or 3)
	 */
	public static void estimateBounds( final float[] offset, final int[] size, final List<ImagePlus> images, final ArrayList<InvertibleBoundable> models, final int dimensionality )
	{
		final int[][] imgSizes = new int[ images.size() ][ dimensionality ];
		
		for ( int i = 0; i < images.size(); ++i )
		{
			imgSizes[ i ][ 0 ] = images.get( i ).getWidth();
			imgSizes[ i ][ 1 ] = images.get( i ).getHeight();
			if ( dimensionality == 3 )
				imgSizes[ i ][ 2 ] = images.get( i ).getNSlices();
		}
		
		estimateBounds( offset, size, imgSizes, models, dimensionality );
	}
	
	/**
	 * Estimate the bounds of the output image. If there are more models than images, we assume that this encodes for more timepoints.
	 * E.g. 2 Images and 10 models would mean 5 timepoints. The arrangement of the models should be as follows:
	 * 
	 * image1 timepoint1
	 * image2 timepoint1
	 * image1 timepoint2
	 * ...
	 * image2 timepoint5
	 * 
	 * @param offset - the offset, will be computed
	 * @param size - the size, will be computed
	 * @param imgSizes - the dimensions of all input images imgSizes[ image ][ x, y, (z) ]
	 * @param models - all models
	 * @param dimensionality - which dimensionality (2 or 3)
	 */
	public static void estimateBounds( final float[] offset, final int[] size, final int[][]imgSizes, final ArrayList<InvertibleBoundable> models, final int dimensionality )
	{
		final int numImages = imgSizes.length;
		final int numTimePoints = models.size() / numImages;
		
		// estimate the bounaries of the output image
		final float[][] max = new float[ numImages * numTimePoints ][];
		final float[][] min = new float[ numImages * numTimePoints ][ dimensionality ];
		
		if ( dimensionality == 2 )
		{
			for ( int i = 0; i < numImages * numTimePoints; ++i )
				max[ i ] = new float[] { imgSizes[ i % numImages ][ 0 ], imgSizes[ i % numImages ][ 1 ] };
		}
		else
		{
			for ( int i = 0; i < numImages * numTimePoints; ++i )
				max[ i ] = new float[] { imgSizes[ i % numImages ][ 0 ], imgSizes[ i % numImages ][ 1 ], imgSizes[ i % numImages ][ 2 ] };
		}
		
		//IJ.log( "1: " + Util.printCoordinates( min[ 0 ] ) + " -> " + Util.printCoordinates( max[ 0 ] ) );
		//IJ.log( "2: " + Util.printCoordinates( min[ 1 ] ) + " -> " + Util.printCoordinates( max[ 1 ] ) );

		// casts of the models
		final ArrayList<InvertibleBoundable> boundables = new ArrayList<InvertibleBoundable>();
		
		for ( int i = 0; i < numImages * numTimePoints; ++i )
		{
			final InvertibleBoundable boundable = (InvertibleBoundable)models.get( i ); 
			boundables.add( boundable );
			
			//IJ.log( "i: " + boundable );
			
			boundable.estimateBounds( min[ i ], max[ i ] );
		}
		//IJ.log( "1: " + Util.printCoordinates( min[ 0 ] ) + " -> " + Util.printCoordinates( max[ 0 ] ) );
		//IJ.log( "2: " + Util.printCoordinates( min[ 1 ] ) + " -> " + Util.printCoordinates( max[ 1 ] ) );
		
		// dimensions of the final image
		final float[] minImg = new float[ dimensionality ];
		final float[] maxImg = new float[ dimensionality ];

		for ( int d = 0; d < dimensionality; ++d )
		{
			// the image might be rotated so that min is actually max
			maxImg[ d ] = Math.max( Math.max( max[ 0 ][ d ], max[ 1 ][ d ] ), Math.max( min[ 0 ][ d ], min[ 1 ][ d ]) );
			minImg[ d ] = Math.min( Math.min( max[ 0 ][ d ], max[ 1 ][ d ] ), Math.min( min[ 0 ][ d ], min[ 1 ][ d ]) );
			
			for ( int i = 2; i < numImages * numTimePoints; ++i )
			{
				maxImg[ d ] = Math.max( maxImg[ d ], Math.max( min[ i ][ d ], max[ i ][ d ]) );
				minImg[ d ] = Math.min( minImg[ d ], Math.min( min[ i ][ d ], max[ i ][ d ]) );	
			}
		}
		
		//IJ.log( "output: " + Util.printCoordinates( minImg ) + " -> " + Util.printCoordinates( maxImg ) );

		// the size of the new image
		//final int[] size = new int[ dimensionality ];
		// the offset relative to the output image which starts with its local coordinates (0,0,0)
		//final float[] offset = new float[ dimensionality ];
		
		for ( int d = 0; d < dimensionality; ++d )
		{
			size[ d ] = Math.round( maxImg[ d ] - minImg[ d ] );
			offset[ d ] = minImg[ d ];			
		}
		
		//IJ.log( "size: " + Util.printCoordinates( size ) );
		//IJ.log( "offset: " + Util.printCoordinates( offset ) );		
	}
	
	public static void main( String[] args )
	{
		new ImageJ();
		
		// test blending
		ImageFactory< FloatType > f = new ImageFactory<FloatType>( new FloatType(), new ArrayContainerFactory() );
		Image< FloatType > img = f.createImage( new int[] { 1024, 100 } ); 
		
		LocalizableCursor< FloatType > c = img.createLocalizableCursor();
		final int numDimensions = img.getNumDimensions();
		final float[] tmp = new float[ numDimensions ];
		
		// for blending
		final int[] dimensions = img.getDimensions();
		final float percentScaling = 0.3f;
		final float[] dimensionScaling = new float[ numDimensions ];
		final float[] border = new float[ numDimensions ];
		for ( int d = 0; d < numDimensions; ++d )
			dimensionScaling[ d ] = 1;
		
		//dimensionScaling[ 1 ] = 0.5f;
			
		while ( c.hasNext() )
		{
			c.fwd();
			
			for ( int d = 0; d < numDimensions; ++d )
				tmp[ d ] = c.getPosition( d );
			
			c.getType().set( (float)BlendingPixelFusion.computeWeight( tmp, dimensions, border, dimensionScaling, percentScaling ) );
		}
		
		ImageJFunctions.show( img );
		System.out.println( "done" );
	}
}
