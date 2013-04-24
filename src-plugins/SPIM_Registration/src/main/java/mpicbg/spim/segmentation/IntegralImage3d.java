package mpicbg.spim.segmentation;

import ij.ImageJ;

import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.integral.IntegralImageLong;
import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.LongAccess;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.basictypecontainer.array.LongArray;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

public class IntegralImage3d 
{
	final public static int getIndex( final int x, final int y, final int z, final int w, final int h )
	{
		return (x + w * (y + z * h));
	}
	
	final public static Image< LongType > computeArray( final Image< FloatType > img )
	{
		final ImageFactory< LongType > imgFactory = new ImageFactory< LongType >( new LongType(), new ArrayContainerFactory() );
		final Image< LongType > integralTmp = imgFactory.createImage( new int[]{ img.getDimension( 0 ) + 1, img.getDimension( 1 ) + 1, img.getDimension( 2 ) + 1 } );
		
		computeArray( integralTmp, img );
		
		return integralTmp;
	}
	
	final public static void computeArray( final Image< LongType > integralTmp, final Image< FloatType > img )
	{
		final Array<LongType, LongAccess> array1 = (Array<LongType, LongAccess>)integralTmp.getContainer();
		final LongArray longarray = (LongArray)array1.update( null );
		final long[] data = longarray.getCurrentStorageArray();

		final Array<FloatType, FloatAccess> array2 = (Array<FloatType, FloatAccess>)img.getContainer();
		final FloatArray floatarray = (FloatArray)array2.update( null );
		final float[] dataF = floatarray.getCurrentStorageArray();

		final int w = integralTmp.getDimension( 0 );
		final int h = integralTmp.getDimension( 1 );
		final int d = integralTmp.getDimension( 2 );

		final int wf = img.getDimension( 0 );
		final int hf = img.getDimension( 1 );

		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads(  );
        final int numThreads = threads.length;
        
        //
        // sum over x
		//
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

            		for ( int z = 1; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
	            			for ( int y = 1; y < h; ++y )
	            			{
	            				int indexIn = getIndex( 0, y - 1, z - 1, wf, hf );
	            				int indexOut = getIndex( 1, y, z, w, h );
	            				
	            				// compute the first pixel
	            				long sum = (int)( dataF[ indexIn ] );
	            				data[ indexOut ] = sum;
	            				
	            				for ( int x = 2; x < w; ++x )
	            				{
	            					++indexIn;
	            					++indexOut;
	
	            					sum += (int)( dataF[ indexIn ] );
		            				data[ indexOut ] = sum;
	            				}
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
        //
        // sum over y
		//
        
        ai.set( 0 );
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

                	//int index = 0;
            		
            		for ( int z = 1; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
	            			for ( int x = 1; x < w; ++x )
	            			{
	            				int index = getIndex( x, 1, z, w, h );
	            				
	            				// init sum on first pixel that is not zero
	            				long sum = data[ index ];
	
	            				for ( int y = 2; y < h; ++y )
	            				{
	            					index += w;
	            					
	            					sum += data[ index ];
	            					data[ index ] = sum;
	            				}
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
        //
        // sum over z
		//
        
        ai.set( 0 );
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

                	//int index = 0;
                	final int inc = w*h;
                	
            		for ( int y = 1; y < h; ++y )
            		{
            			if ( y % numThreads == myNumber )
            			{
	            			for ( int x = 1; x < w; ++x )
	            			{
	            				int index = getIndex( x, y, 1, w, h );
	
	            				//System.out.println( index + " " + data[ index ] );
	            				
	            				// init sum on first pixel that is not zero
	            				long sum = data[ index ];
	
	            				for ( int z = 2; z < d; ++z )
	            				{
	            					index += inc;

		            				//System.out.println( index + " " + data[ index ] );

	            					sum += data[ index ];
	            					data[ index ] = sum;
	            				}
	            				//System.out.println();
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
	}

	public static void main( String[] args )
	{
		new ImageJ();
		
		//Image< FloatType > img = new ImageFactory< FloatType >( new FloatType(), new ArrayContainerFactory() ).createImage( new int[]{ 2, 3, 4 } );
		Image< FloatType > img = LOCI.openLOCIFloatType( "/Users/preibischs/Documents/Microscopy/SPIM/HisYFP-SPIM/spim_TL18_Angle0.tif", new ArrayContainerFactory() );
		
		//int i = 1;
		//for ( final FloatType t : img )
		///	t.set( i++ );
		
		long t = 0;
		
		System.out.println( "new implementation" );
		
		for ( int i = 0; i < 10; ++i )
		{
			long t1 = System.currentTimeMillis();
			
			final Image< LongType > integralImg = computeArray( img );
			
			integralImg.close();
			
			long t2 = System.currentTimeMillis();
			
			System.out.println( (t2 - t1) + " ms" );
			t += t2-t1;
		}

		System.out.println( "avg: " + (t/10) + " ms" );
		System.out.println( "\nold implementation" );
		t = 0;
		
		for ( int i = 0; i < 10; ++i )
		{
			long t1 = System.currentTimeMillis();

			final IntegralImageLong< FloatType > intImg = new IntegralImageLong<FloatType>( img, new Converter< FloatType, LongType >()
			{
				@Override
				public void convert( final FloatType input, final LongType output ) { output.set( Util.round( input.get() ) ); } 
			} );
			
			intImg.process();
			
			final Image< LongType > integralImg = intImg.getResult();
								
			integralImg.close();
			
			long t2 = System.currentTimeMillis();
			
			System.out.println( (t2 - t1) + " ms" );
			t += t2-t1;
		}
		System.out.println( "avg: " + (t/10) + " ms" );
		/*

		final IntegralImageLong< FloatType > intImg = new IntegralImageLong<FloatType>( img, new Converter< FloatType, LongType >()
		{
			@Override
			public void convert( final FloatType input, final LongType output ) { output.set( Util.round( input.get() ) ); } 
		} );
		
		intImg.process();
		
		final Image< LongType > integralImg = intImg.getResult();
		
		final Image< LongType > integralImgNew = computeArray( img );		
		
		ImageJFunctions.show( img ).setTitle( "img" );
		ImageJFunctions.show( integralImg ).setTitle( "integral_correct");
		ImageJFunctions.show( integralImgNew ).setTitle( "integral_new");
		
		
		final Image< LongType> diff = integralImg.createNewImage();
		
		final Cursor< LongType > c1 = integralImg.createCursor();
		final Cursor< LongType > c2 = integralImgNew.createCursor();
		final Cursor< LongType > cd = diff.createCursor();
		
		while ( c1.hasNext() )
		{
			cd.next().set( c1.next().get() - c2.next().get() );
		}

		ImageJFunctions.show( diff ).setTitle( "integral_diff");
		*/
	}

}
