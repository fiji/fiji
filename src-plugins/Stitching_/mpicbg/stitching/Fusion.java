package mpicbg.stitching;

import fiji.stacks.Hyperstack_rearranger;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import process.OverlayFusion;

import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.InvertibleCoordinateTransform;
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
	public static < T extends RealType< T > > void fuse( final T targetType, final ArrayList< ImagePlus > images, final ArrayList< InvertibleBoundable > models, final int dimensionality, final boolean subpixelResolution )
	{
		// first we need to estimate the boundaries of the new image
		final float[] offset = new float[ dimensionality ];
		final int[] size = new int[ dimensionality ];
		final int numTimePoints = images.get( 0 ).getNFrames();
		final int numChannels = images.get( 0 ).getNChannels();
		
		OverlayFusion.estimateBounds( offset, size, images, models, dimensionality );
		
		// for output
		final ImageFactory<T> f = new ImageFactory<T>( targetType, new ImagePlusContainerFactory() );
		
		// the composite
		final ImageStack stack = new ImageStack( size[ 0 ], size[ 1 ] );

		//"Overlay into composite image"
		for ( int t = 1; t <= numTimePoints; ++t )
		{
			for ( int c = 1; c <= numChannels; ++c )
			{
				// create the 2d/3d target image for the current channel and timepoint 
				final Image< T > out = f.createImage( size );

				// 
				
				// extract the complete blockdata
				if ( subpixelResolution )
				{
					final ArrayList< Image< FloatType > > blockData = new ArrayList< Image< FloatType > >();
					for ( final ImagePlus imp : images )
						blockData.add( ImageJFunctions.convertFloat( Hyperstack_rearranger.getImageChunk( imp, c, t ) ) );
					
					fuseBlock( out, blockData, offset, models, new LinearInterpolatorFactory<FloatType>( new OutOfBoundsStrategyValueFactory<FloatType>() ) );
				}
				else
				{
					// can be a mixture of different RealTypes
					final ArrayList< Image< ? extends RealType< ? > > > blockData = new ArrayList< Image< ? extends RealType< ? > > >();
					
					for ( final ImagePlus imp : images )
					{
						if ( imp.getType() == ImagePlus.GRAY32 )
							blockData.add( ImageJFunctions.wrapFloat( Hyperstack_rearranger.getImageChunk( imp, c, t ) ) );
						else if ( imp.getType() == ImagePlus.GRAY16 )
							blockData.add( ImageJFunctions.wrapShort( Hyperstack_rearranger.getImageChunk( imp, c, t ) ) );
						else
							blockData.add( ImageJFunctions.wrapByte( Hyperstack_rearranger.getImageChunk( imp, c, t ) ) );
					}
				}
			}
		}
		
		
	}
	
	/**
	 * Fuse one slice/volume (one channel)
	 * 
	 * @param output - same the type of the ImagePlus input
	 * @param input - FloatType, because of Interpolation that needs to be done
	 * @param transform - the transformation
	 */
	protected static <T extends RealType<T>> void fuseBlock( final Image<T> output, final ArrayList<Image<FloatType>> input, final float[] offset, final ArrayList< InvertibleBoundable > transform, final InterpolatorFactory< FloatType > factory )
	{
		final int dims = output.getNumDimensions();
		long imageSize = output.getDimension( 0 );
		
		for ( int d = 1; d < output.getNumDimensions(); ++d )
			imageSize *= output.getDimension( d );

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
            		final Interpolator<FloatType> in = null;//input.createInterpolator( factory );
            		
            		final float[] tmp = new float[ output.getNumDimensions() ];
            		
            		try 
            		{
                		// move to the starting position of the current thread
            			out.fwd( startPos );
            			
                		// do as many pixels as wanted by this thread
                        for ( long j = 0; j < loopSize; ++j )
                        {
            				out.fwd();
            				
            				for ( int d = 0; d < dims; ++d )
            					tmp[ d ] = out.getPosition( d ) + offset[ d ];
            				
            				transform.get( 0 ).applyInverseInPlace( tmp );
            	
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
            });
        
        SimpleMultiThreading.startAndJoin( threads );
	}

}
