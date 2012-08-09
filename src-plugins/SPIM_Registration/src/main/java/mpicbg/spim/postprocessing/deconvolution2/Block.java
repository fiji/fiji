package mpicbg.spim.postprocessing.deconvolution2;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.complex.ComplexFloatType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

public class Block 
{
	/**
	 * the number of dimensions of this block
	 */
	final int numDimensions;
	
	/**
	 * The dimensions of the block
	 */
	final int[] blockSize;
	
	/**
	 * The offset in coordinates (coordinate system of the original image) 
	 */
	final int[] offset;
	
	/**
	 * The effective size that can be convolved (depends on the kernelsize)
	 */
	final int[] effectiveSize;
	
	/**
	 * The effective offset, i.e. where the useful convolved data starts (coordinate system of the original image)
	 */
	final int[] effectiveOffset;
	
	/**
	 * The effective offset, i.e. where the useful convoved data starts (local coordinate system)
	 */
	final int[] effectiveLocalOffset;
	
	/**
	 * Stores wheather the block is grepping data from outside of the original image,
	 * getting the data is cheaper when no outofbounds is required to fill up a block
	 */
	final boolean inside;
	
	final Vector< Chunk > threadChunks;
	final int numThreads;
	final static OutOfBoundsStrategyFactory< FloatType > factory = new OutOfBoundsStrategyMirrorFactory< FloatType >();
	
	public Block( final int[] blockSize, final int[] offset, final int[] effectiveSize, final int[] effectiveOffset, final int[] effectiveLocalOffset, final boolean inside )
	{
		this.numDimensions = blockSize.length;
		this.blockSize = blockSize;
		this.offset = offset;
		this.effectiveSize = effectiveSize;
		this.effectiveOffset = effectiveOffset;
		this.effectiveLocalOffset = effectiveLocalOffset;
		this.inside = inside;
		
		this.numThreads = Runtime.getRuntime().availableProcessors();
		
		long n = blockSize[ 0 ];
		for ( int d = 1; d < numDimensions; ++d )
			n *= blockSize[ d ];
		
		this.threadChunks = SimpleMultiThreading.divideIntoChunks( n, numThreads );
	}
	
	public void copyBlock( final Image< FloatType > source, final Image< FloatType > block )
	{	
		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
        for ( int ithread = 0; ithread < threads.length; ++ithread )
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( ai.getAndIncrement() );
                	
            		copy( myChunk.getStartPosition(), myChunk.getLoopSize(), source, block, offset, inside, factory );
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );

	}

	private static final void copy( final long start, final long loopSize, final Image< FloatType > source, final Image< FloatType > block, final int[] offset, final boolean inside, final OutOfBoundsStrategyFactory< FloatType > strategyFactory )
	{
		final int numDimensions = source.getNumDimensions();
		final LocalizableCursor<FloatType> cursor = block.createLocalizableCursor();
		final LocalizableByDimCursor<FloatType> randomAccess;
		
		if ( inside )
			randomAccess = source.createLocalizableByDimCursor();
		else
			randomAccess = source.createLocalizableByDimCursor( strategyFactory );
		
		cursor.fwd( start );
		
		final int[] tmp = new int[ numDimensions ];
		
		for ( long l = 0; l < loopSize; ++l )
		{
			cursor.fwd();
			cursor.getPosition( tmp );
			
			for ( int d = 0; d < numDimensions; ++d )
				tmp[ d ] += offset[ d ];
			
			randomAccess.setPosition( tmp );
			randomAccess.getType().set( cursor.getType() );
		}
	}

	public void pasteBlock( final Image< FloatType > target, final Image< FloatType > block )
	{	
		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
        for ( int ithread = 0; ithread < threads.length; ++ithread )
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( ai.getAndIncrement() );
                	
            		paste( myChunk.getStartPosition(), myChunk.getLoopSize(), target, block, offset, effectiveOffset, effectiveSize, effectiveLocalOffset );
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );

	}

	private static final void paste( final long start, final long loopSize, final Image< FloatType > target, final Image< FloatType > block, 
			final int[] offset, final int[] effectiveOffset, final int[] effectiveSize, final int[] effectiveLocalOffset )
	{
		final int numDimensions = target.getNumDimensions();
		
		// iterate over effective size
		final LocalizableCursor<?> cursor = ArrayLocalizableCursor.createLinearCursor( effectiveSize );
		
		// read from block
		final LocalizableByDimCursor<FloatType> blockRandomAccess  = block.createLocalizableByDimCursor();
		
		// write to target		
		final LocalizableByDimCursor<FloatType> targetRandomAccess  = target.createLocalizableByDimCursor();
		
		cursor.fwd( start );
		
		final int[] tmp = new int[ numDimensions ];
		
		for ( long l = 0; l < loopSize; ++l )
		{
			cursor.fwd();
			cursor.getPosition( tmp );
			
			// move to the relative local offset where the real data starts
			for ( int d = 0; d < numDimensions; ++d )
				tmp[ d ] += effectiveLocalOffset[ d ];
			
			blockRandomAccess.setPosition( tmp );
			
			// move to the right position in the image
			for ( int d = 0; d < numDimensions; ++d )
				tmp[ d ] += effectiveOffset[ d ] - effectiveLocalOffset[ d ];

			targetRandomAccess.setPosition( tmp );

			// write the pixel
			targetRandomAccess.getType().set( blockRandomAccess.getType() );
		}
	}

	/**
	 * Divides an image into blocks
	 * 
	 * @param imgSize - the size of the image
	 * @param blockSize - the final size of each block covering the entire image
	 * @param kernelSize - the size of the kernel (has to be odd!)
	 * @return
	 */
	public static ArrayList< Block > divideIntoBlocks( final int[] imgSize, final int[] blockSize, final int[] kernelSize )
	{
		final int numDimensions = imgSize.length;
		
		// compute the effective size & local offset of each block
		// this is the same for all blocks
		final int[] effectiveSize = new int[ numDimensions ];
		final int[] effectiveLocalOffset = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			effectiveSize[ d ] = blockSize[ d ] - kernelSize[ d ] + 1;
			effectiveLocalOffset[ d ] = kernelSize[ d ] / 2;
		}
		
		// compute the amount of blocks needed
		final int[] numBlocks = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			numBlocks[ d ] = imgSize[ d ] / effectiveSize[ d ];
			
			// if the modulo is not 0 we need one more that is only partially useful
			if ( imgSize[ d ] % effectiveSize[ d ] != 0 )
				++numBlocks[ d ];
		}
		
		System.out.println( "numBlocks " + Util.printCoordinates( numBlocks ) );
		System.out.println( "effectiveSize " + Util.printCoordinates( effectiveSize ) );
		System.out.println( "effectiveLocalOffset " + Util.printCoordinates( effectiveLocalOffset ) );
				
		// now we instantiate the individual blocks iterating over all dimensions
		// we use the well-known ArrayLocalizableCursor for that
		final LocalizableCursor<?> cursor = ArrayLocalizableCursor.createLinearCursor( numBlocks );
		final ArrayList< Block > blockList = new ArrayList< Block >();
		
		final int[] currentBlock = new int[ numDimensions ];
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.getPosition( currentBlock );
			
			// compute the current offset
			final int[] offset = new int[ numDimensions ];
			final int[] effectiveOffset = new int[ numDimensions ];
			boolean inside = true;
			
			for ( int d = 0; d < numDimensions; ++d )
			{
				effectiveOffset[ d ] = currentBlock[ d ] * effectiveSize[ d ];
				offset[ d ] = effectiveOffset[ d ] - kernelSize[ d ]/2;
				
				if ( offset[ d ] < 0 || offset[ d ] + blockSize[ d ] > imgSize[ d ] )
					inside = false;
				
				if ( effectiveOffset[ d ] + effectiveSize[ d ] > imgSize[ d ] )
					effectiveSize[ d ] = imgSize[ d ] - effectiveOffset[ d ];
			}

			blockList.add( new Block( blockSize, offset, effectiveSize, effectiveOffset, effectiveLocalOffset, inside ) );
			System.out.println( "block " + Util.printCoordinates( currentBlock ) + " effectiveOffset: " + Util.printCoordinates( effectiveOffset ) + " effectiveSize: " + Util.printCoordinates( effectiveSize )  + " offset: " + Util.printCoordinates( offset ) + " inside: " + inside );
		}
			
		return blockList;
	}
	
	public static void main( String[] args )
	{
		final int[] imgSize = new int[]{ 256, 384 };
		final int[] blockSize = new int[]{ 64, 128 }; 
		final int[] kernelSize = new int[]{ 15, 25 };
		
		final ArrayList< Block > blocks = Block.divideIntoBlocks( imgSize, blockSize, kernelSize );
	}
}
