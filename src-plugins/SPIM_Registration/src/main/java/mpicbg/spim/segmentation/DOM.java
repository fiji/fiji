package mpicbg.spim.segmentation;

import ij.ImageJ;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.integral.IntegralImageLong;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

public class DOM 
{
	final public static void computeDifferencOfMean3d( final Image< LongType> integralImg, final Image< FloatType > domImg, final int sx1, final int sy1, final int sz1, final int sx2, final int sy2, final int sz2, final float min, final float max  )
	{
		final float diff = max - min;
		
		final float sumPixels1 = sx1 * sy1 * sz1;
		final float sumPixels2 = sx2 * sy2 * sz2;
		
		final float d1 = sumPixels1 * diff;
		final float d2 = sumPixels2 * diff;
		
		final int sx1Half = sx1 / 2;
		final int sy1Half = sy1 / 2;
		final int sz1Half = sz1 / 2;

		final int sx2Half = sx2 / 2;
		final int sy2Half = sy2 / 2;
		final int sz2Half = sz2 / 2;
		
		final int sxHalfMax = Math.max( sx1Half, sx2Half );
		final int syHalfMax = Math.max( sy1Half, sy2Half );
		final int szHalfMax = Math.max( sz1Half, sz2Half );

		final int w = domImg.getDimension( 0 ) - ( Math.max( sx1, sx2 ) / 2 ) * 2;
		final int h = domImg.getDimension( 1 ) - ( Math.max( sy1, sy2 ) / 2 ) * 2;
		final int d = domImg.getDimension( 2 ) - ( Math.max( sz1, sz2 ) / 2 ) * 2;

		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads();
		final int numThreads = threads.length;
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

            		// for each computation we need 8 randomaccesses, so 16 all together
            		final LocalizableByDimCursor< LongType > r11 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r12 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r13 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r14 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r15 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r16 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r17 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r18 = integralImg.createLocalizableByDimCursor();

            		final LocalizableByDimCursor< LongType > r21 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r22 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r23 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r24 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r25 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r26 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r27 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r28 = integralImg.createLocalizableByDimCursor();
            		
            		final LocalizableByDimCursor< FloatType > result = domImg.createLocalizableByDimCursor();
            		
            		final int[] p = new int[ 3 ];

            		for ( int z = 0; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
	            			for ( int y = 0; y < h; ++y )
	            			{
	            				// set the result randomaccess
	            				p[ 0 ] = sxHalfMax; p[ 1 ] = y + syHalfMax; p[ 2 ] = z + szHalfMax;
	            				result.setPosition( p );
	            				
	            				// set all randomaccess for the first box accordingly
	            				p[ 0 ] = sxHalfMax - sx1Half; p[ 1 ] = y + syHalfMax - sy1Half; p[ 2 ] = z + szHalfMax - sz1Half;
	            				r11.setPosition( p ); // negative
	
	            				p[ 0 ] += sx1;
	            				r12.setPosition( p ); // positive
	            				
	            				p[ 1 ] += sy1;
	            				r13.setPosition( p ); // negative
	            				
	            				p[ 0 ] -= sx1;
	            				r14.setPosition( p ); // positive
	
	            				p[ 2 ] += sz1;
	            				r15.setPosition( p ); // negative
	
	            				p[ 0 ] += sx1;
	            				r16.setPosition( p ); // positive
	
	            				p[ 1 ] -= sy1;
	            				r17.setPosition( p ); // negative
	
	            				p[ 0 ] -= sx1;
	            				r18.setPosition( p ); // positive
	
	            				// set all randomaccess for the second box accordingly
	            				p[ 0 ] = sxHalfMax - sx2Half; p[ 1 ] = y + syHalfMax - sy2Half; p[ 2 ] = z + szHalfMax - sz2Half;
	            				r21.setPosition( p );
	
	            				p[ 0 ] += sx2;
	            				r22.setPosition( p ); // positive
	            				
	            				p[ 1 ] += sy2;
	            				r23.setPosition( p ); // negative
	            				
	            				p[ 0 ] -= sx2;
	            				r24.setPosition( p ); // positive
	
	            				p[ 2 ] += sz2;
	            				r25.setPosition( p ); // negative
	
	            				p[ 0 ] += sx2;
	            				r26.setPosition( p ); // positive
	
	            				p[ 1 ] -= sy2;
	            				r27.setPosition( p ); // negative
	
	            				p[ 0 ] -= sx2;
	            				r28.setPosition( p ); // positive
	
	            				for ( int x = 0; x < w; ++x )
	            				{
	            					final long s1 = -r11.getType().get() + r12.getType().get() - r13.getType().get() + r14.getType().get() - r15.getType().get() + r16.getType().get() - r17.getType().get() + r18.getType().get();
	            					final long s2 = -r21.getType().get() + r22.getType().get() - r23.getType().get() + r24.getType().get() - r25.getType().get() + r26.getType().get() - r27.getType().get() + r28.getType().get();
	
	            					result.getType().set( (float)s2/d2 - (float)s1/d1 );
	            					
	            					r11.fwd( 0 ); r12.fwd( 0 ); r13.fwd( 0 ); r14.fwd( 0 ); r15.fwd( 0 ); r16.fwd( 0 ); r17.fwd( 0 ); r18.fwd( 0 );
	            					r21.fwd( 0 ); r22.fwd( 0 ); r23.fwd( 0 ); r24.fwd( 0 ); r25.fwd( 0 ); r26.fwd( 0 ); r27.fwd( 0 ); r28.fwd( 0 );
	            					result.fwd( 0 );
	            				}
	            			}
            			}
            		}            		
                }
            });

        SimpleMultiThreading.startAndJoin( threads );
 	}

	final public static void mean( final Image< LongType> integralImg, final Image< FloatType > domImg, final int sx, final int sy, final int sz, final float min, final float max  )
	{
		final float diff = max - min;
		
		final float sumPixels = sx * sy * sz;
		
		final float div = sumPixels * diff;
		
		final int sxHalf = sx / 2;
		final int syHalf = sy / 2;
		final int szHalf = sz / 2;

		final int w = domImg.getDimension( 0 ) - ( sx / 2 ) * 2; // this makes sense as sx is odd
		final int h = domImg.getDimension( 1 ) - ( sy / 2 ) * 2;
		final int d = domImg.getDimension( 2 ) - ( sz / 2 ) * 2;

		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads();
		final int numThreads = threads.length;
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

            		// for each computation we need 8 randomaccesses, so 16 all together
            		final LocalizableByDimCursor< LongType > r1 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r2 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r3 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r4 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r5 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r6 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r7 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r8 = integralImg.createLocalizableByDimCursor();
            		
            		final LocalizableByDimCursor< FloatType > result = domImg.createLocalizableByDimCursor();
            		
            		final int[] p = new int[ 3 ];

            		for ( int z = 0; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
	            			for ( int y = 0; y < h; ++y )
	            			{
	            				// set the result randomaccess
	            				p[ 0 ] = sxHalf; p[ 1 ] = y + syHalf; p[ 2 ] = z + szHalf;
	            				result.setPosition( p );
	            				
	            				// set all randomaccess for the first box accordingly
	            				p[ 0 ] = 0; p[ 1 ] = y; p[ 2 ] = z;
	            				r1.setPosition( p ); // negative
	
	            				p[ 0 ] += sx;
	            				r2.setPosition( p ); // positive
	            				
	            				p[ 1 ] += sy;
	            				r3.setPosition( p ); // negative
	            				
	            				p[ 0 ] -= sx;
	            				r4.setPosition( p ); // positive
	
	            				p[ 2 ] += sz;
	            				r5.setPosition( p ); // negative
	
	            				p[ 0 ] += sx;
	            				r6.setPosition( p ); // positive
	
	            				p[ 1 ] -= sy;
	            				r7.setPosition( p ); // negative
	
	            				p[ 0 ] -= sx;
	            				r8.setPosition( p ); // positive
	
	            				for ( int x = 0; x < w; ++x )
	            				{
	            					final long s = -r1.getType().get() + r2.getType().get() - r3.getType().get() + r4.getType().get() - r5.getType().get() + r6.getType().get() - r7.getType().get() + r8.getType().get();
	
	            					result.getType().set( (float)s/div );
	            					
	            					r1.fwd( 0 ); r2.fwd( 0 ); r3.fwd( 0 ); r4.fwd( 0 ); r5.fwd( 0 ); r6.fwd( 0 ); r7.fwd( 0 ); r8.fwd( 0 );
	            					result.fwd( 0 );
	            				}
	            			}
            			}
            		}            		
                }
            });

        SimpleMultiThreading.startAndJoin( threads );
 	}

	final public static void meanReflect( final Image< LongType> integralImg, final Image< FloatType > domImg, final int sx, final int sy, final int sz, final float min, final float max  )
	{
		final float diff = max - min;	
		final float sumPixels = sx * sy * sz;
		final float div = sumPixels * diff;
		
		final int sxHalf = sx / 2;
		final int syHalf = sy / 2;
		final int szHalf = sz / 2;

		final int w = domImg.getDimension( 0 ) - ( sx / 2 ) * 2; // this makes sense as sx is odd
		final int h = domImg.getDimension( 1 ) - ( sy / 2 ) * 2;
		final int d = domImg.getDimension( 2 ) - ( sz / 2 ) * 2;

		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads();
		final int numThreads = threads.length;
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

            		// for each computation we need 8 randomaccesses, so 16 all together
            		final LocalizableByDimCursor< LongType > r1 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r2 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r3 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r4 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r5 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r6 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r7 = integralImg.createLocalizableByDimCursor();
            		final LocalizableByDimCursor< LongType > r8 = integralImg.createLocalizableByDimCursor();
            		
            		final LocalizableByDimCursor< FloatType > result = domImg.createLocalizableByDimCursor();
            		
            		final int[] p = new int[ 3 ];

            		for ( int z = 0; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
	            			for ( int y = 0; y < h; ++y )
	            			{
	            				// set the result randomaccess
	            				p[ 0 ] = sxHalf; p[ 1 ] = y + syHalf; p[ 2 ] = z + szHalf;
	            				result.setPosition( p );
	            				
	            				// set all randomaccess for the first box accordingly
	            				p[ 0 ] = 0; p[ 1 ] = y; p[ 2 ] = z;
	            				r1.setPosition( p ); // negative
	
	            				p[ 0 ] += sx;
	            				r2.setPosition( p ); // positive
	            				
	            				p[ 1 ] += sy;
	            				r3.setPosition( p ); // negative
	            				
	            				p[ 0 ] -= sx;
	            				r4.setPosition( p ); // positive
	
	            				p[ 2 ] += sz;
	            				r5.setPosition( p ); // negative
	
	            				p[ 0 ] += sx;
	            				r6.setPosition( p ); // positive
	
	            				p[ 1 ] -= sy;
	            				r7.setPosition( p ); // negative
	
	            				p[ 0 ] -= sx;
	            				r8.setPosition( p ); // positive
	
	            				for ( int x = 0; x < w; ++x )
	            				{
	            					final long s = -r1.getType().get() + r2.getType().get() - r3.getType().get() + r4.getType().get() - r5.getType().get() + r6.getType().get() - r7.getType().get() + r8.getType().get();
	
	            					result.getType().set( (float)s/div );
	            					
	            					r1.fwd( 0 ); r2.fwd( 0 ); r3.fwd( 0 ); r4.fwd( 0 ); r5.fwd( 0 ); r6.fwd( 0 ); r7.fwd( 0 ); r8.fwd( 0 );
	            					result.fwd( 0 );
	            				}
	            			}
            			}
            		}            		
                }
            });

        SimpleMultiThreading.startAndJoin( threads );
 	}

	final public static void computeDifferencOfMean( final Image< LongType> integralImg, final Image< FloatType > domImg, final int sx1, final int sy1, final int sz1, final int sx2, final int sy2, final int sz2, final float min, final float max  )
	{
		final float diff = max - min;
		
		final float sumPixels1 = sx1 * sy1 * sz1;
		final float sumPixels2 = sx2 * sy2 * sz2;
		
		final float d1 = sumPixels1 * diff;
		final float d2 = sumPixels2 * diff;
		
		final int sx1Half = sx1 / 2;
		final int sy1Half = sy1 / 2;
		final int sz1Half = sz1 / 2;

		final int sx2Half = sx2 / 2;
		final int sy2Half = sy2 / 2;
		final int sz2Half = sz2 / 2;
		
		final int sxHalfMax = Math.max( sx1Half, sx2Half );
		final int syHalfMax = Math.max( sy1Half, sy2Half );
		final int szHalfMax = Math.max( sz1Half, sz2Half );

		final int w = domImg.getDimension( 0 ) - ( Math.max( sx1, sx2 ) / 2 ) * 2;
		final int h = domImg.getDimension( 1 ) - ( Math.max( sy1, sy2 ) / 2 ) * 2;
		final int d = domImg.getDimension( 2 ) - ( Math.max( sz1, sz2 ) / 2 ) * 2;
				
		final long imageSize = domImg.getNumPixels();

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
        
                	final int[] position = new int[ 3 ];
                	
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( myNumber );
                	final long loopSize = myChunk.getLoopSize();
                	
            		final LocalizableCursor< FloatType > cursor = domImg.createLocalizableCursor();
            		final LocalizableByDimCursor< LongType > randomAccess = integralImg.createLocalizableByDimCursor();

            		cursor.fwd( myChunk.getStartPosition() );
            		
            		// do as many pixels as wanted by this thread
                    for ( long j = 0; j < loopSize; ++j )
                    {                    	
            			final FloatType result = cursor.next();
            			
            			final int x = cursor.getPosition( 0 );
            			final int y = cursor.getPosition( 1 );
            			final int z = cursor.getPosition( 2 );
            			
            			final int xt = x - sxHalfMax;
            			final int yt = y - syHalfMax;
            			final int zt = z - szHalfMax;
            			
            			if ( xt >= 0 && yt >= 0 && zt >= 0 && xt < w && yt < h && zt < d )
            			{
            				position[ 0 ] = x - sx1Half;
            				position[ 1 ] = y - sy1Half;
            				position[ 2 ] = z - sz1Half;
            				randomAccess.setPosition( position );
            				final float s1 = computeSum2( sx1, sy1, sz1, randomAccess ) / d1;
            				
            				position[ 0 ] = x - sx2Half;
            				position[ 1 ] = y - sy2Half;
            				position[ 2 ] = z - sz2Half;
            				randomAccess.setPosition( position );
            				final float s2 = computeSum2( sx2, sy2, sz2, randomAccess ) / d2;

            				result.set( ( s2 - s1 ) );
              			}
            			else
            			{
            				result.set( 0 );
            			}
                    }
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
        //ImageJFunctions.show( tmp1  ).setTitle( "s2" );
        //ImageJFunctions.show( tmp2  ).setTitle( "s1" );
        //ImageJFunctions.show( tmp3  ).setTitle( "1" );
 	}

	/**
	 * Compute the average in the area, the RandomAccess needs to be positioned at the top left front corner:
	 * 
	 * fromX - start coordinate in x (exclusive in integral image coordinates, inclusive in image coordinates)
	 * fromY - start coordinate in y (exclusive in integral image coordinates, inclusive in image coordinates)
	 * fromZ - start coordinate in z (exclusive in integral image coordinates, inclusive in image coordinates)
	 * 
	 * @param sX - number of pixels in x
	 * @param sY - number of pixels in y
	 * @param sZ - number of pixels in z
	 * @param randomAccess - randomAccess on the integral image
	 * @return
	 */
	final public static long computeSum2( final int vX, final int vY, final int vZ, final LocalizableByDimCursor< LongType > randomAccess )
	{
		long sum = -randomAccess.getType().get();
		
		randomAccess.move( vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( vY, 1 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( -vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( vZ, 2 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( -vY, 1 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( -vX, 0 );
		sum += randomAccess.getType().get();
		
		return sum;
	}

	/**
	 * Compute the average in the area
	 * 
	 * @param fromX - start coordinate in x (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param fromY - start coordinate in y (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param fromZ - start coordinate in z (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param sX - number of pixels in x
	 * @param sY - number of pixels in y
	 * @param sZ - number of pixels in z
	 * @param randomAccess - randomAccess on the integral image
	 * @return
	 */
	final public static long computeSum( final int fromX, final int fromY, final int fromZ, final int vX, final int vY, final int vZ, final LocalizableByDimCursor< LongType > randomAccess )
	{
		randomAccess.setPosition( fromX, 0 );
		randomAccess.setPosition( fromY, 1 );
		randomAccess.setPosition( fromZ, 2 );
		
		long sum = -randomAccess.getType().get();
		
		randomAccess.move( vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( vY, 1 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( -vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( vZ, 2 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( -vY, 1 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( -vX, 0 );
		sum += randomAccess.getType().get();
		
		return sum;
	}

	final static public void computeMinMax( final Image< FloatType > img, final FloatType min, final FloatType max )
	{
		min.set( Float.MAX_VALUE );
		max.set( Float.MIN_VALUE );
		
		for ( final FloatType t : img )
		{
			final float value = t.get();
			
			if ( value > max.get() )
				max.set( value );
			
			if ( value < min.get() )
				min.set( value );
		}
	}
	
	public static void main( String args[] )
	{
		new ImageJ();
		
		Image< FloatType > img = LOCI.openLOCIFloatType( "/Users/preibischs/Documents/Microscopy/SPIM/HisYFP-SPIM/spim_TL18_Angle0.tif", new ArrayContainerFactory() );
		final IntegralImageLong< FloatType > intImg = new IntegralImageLong<FloatType>( img, new Converter< FloatType, LongType >()
		{
			@Override
			public void convert( final FloatType input, final LongType output ) { output.set( Util.round( input.get() ) ); } 
		} );
		
		intImg.process();
		
		final Image< LongType > integralImg = intImg.getResult();
		
		final FloatType min = new FloatType();
		final FloatType max = new FloatType();
		
		DOM.computeMinMax( img, min, max );
	
		final Image< FloatType > domImg = img.createNewImage();
		
		long t = 0;
		int num = 10;
		
		for ( int i = 0; i < num; ++i )
		{
			long t1 = System.currentTimeMillis();
			
			DOM.computeDifferencOfMean3d( integralImg, domImg, 3, 3, 3, 5, 5, 5, min.get(), max.get() );
			
			long t2 = System.currentTimeMillis();
			
			t += (t2 - t1);
			System.out.println( "run " + i + ": " + (t2-t1) + " ms." );
		}
		
		System.out.println( "avg: " + (t/num) + " ms." );
		
		//DOM.computeDifferencOfMean( integralImg, img, 3, 3, 3, 5, 5, 5, min.get(), max.get() );
		
		
	}

}
