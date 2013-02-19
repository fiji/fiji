package mpicbg.spim.postprocessing.deconvolution2;

import ij.ImageJ;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor3D;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.array.Array3DLocalizableByDimCursor;
import mpicbg.imglib.cursor.array.Array3DLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
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
		this.blockSize = blockSize.clone();
		this.offset = offset.clone();
		this.effectiveSize = effectiveSize.clone();
		this.effectiveOffset = effectiveOffset.clone();
		this.effectiveLocalOffset = effectiveLocalOffset.clone();
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
                	final int threadIdx = ai.getAndIncrement();
                	
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( threadIdx );
                	
                	if ( source.getNumDimensions() == 3 && Array.class.isInstance( block.getContainer() ) )
                		copy3d( threadIdx, numThreads, source, block, offset, inside, factory );
                	else
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
			cursor.getType().set( randomAccess.getType() );
		}
	}

	private static final void copy3d( final int threadIdx, final int numThreads, final Image< FloatType > source, final Image< FloatType > block, final int[] offset, 
			final boolean inside, final OutOfBoundsStrategyFactory< FloatType > strategyFactory )
	{
		final int w = block.getDimension( 0 );
		final int h = block.getDimension( 1 );
		final int d = block.getDimension( 2 );
		
		final int offsetX = offset[ 0 ];
		final int offsetY = offset[ 1 ];
		final int offsetZ = offset[ 2 ];
		final float[] blockArray = ((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray();
		
		final LocalizableByDimCursor3D<FloatType> randomAccess;
		
		if ( inside )
			randomAccess = (LocalizableByDimCursor3D<FloatType>)source.createLocalizableByDimCursor();
		else
			randomAccess = (LocalizableByDimCursor3D<FloatType>)source.createLocalizableByDimCursor( strategyFactory );		
		
		for ( int z = threadIdx; z < d; z += numThreads )
		{
			randomAccess.setPosition( offsetX, offsetY, z + offsetZ );
			
			int i = z * h * w;
			
			for ( int y = 0; y < h; ++y )
			{
				randomAccess.setPosition( offsetX, 0 );
				
				for ( int x = 0; x < w; ++x )
				{
					blockArray[ i++ ] = randomAccess.getType().get();
					randomAccess.fwdX();
				}
				
				randomAccess.move( -w, 0 );
				randomAccess.fwdY();
			}
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
                	final int threadIdx = ai.getAndIncrement();
                	
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( threadIdx );
                	
                	if ( target.getNumDimensions() == 3 && Array.class.isInstance( target.getContainer() ) )
                		paste3d( threadIdx, numThreads, target, block, effectiveOffset, effectiveSize, effectiveLocalOffset );
                	else
                		paste( myChunk.getStartPosition(), myChunk.getLoopSize(), target, block, effectiveOffset, effectiveSize, effectiveLocalOffset );
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );

	}

	private static final void paste( final long start, final long loopSize, final Image< FloatType > target, final Image< FloatType > block, 
			final int[] effectiveOffset, final int[] effectiveSize, final int[] effectiveLocalOffset )
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

	private static final void paste3d( final int threadIdx, final int numThreads, final Image< FloatType > target, final Image< FloatType > block, 
			final int[] effectiveOffset, final int[] effectiveSize, final int[] effectiveLocalOffset )
	{
		// min position in the output
		final int minX = effectiveOffset[ 0 ];
		final int minY = effectiveOffset[ 1 ];
		final int minZ = effectiveOffset[ 2 ];

		// max+1 of the output area
		final int maxX = effectiveSize[ 0 ] + minX;
		final int maxY = effectiveSize[ 1 ] + minY;
		final int maxZ = effectiveSize[ 2 ] + minZ;

		// size of the output area
		final int sX = effectiveSize[ 0 ];

		// min position in the output
		final int minXb = effectiveLocalOffset[ 0 ];
		final int minYb = effectiveLocalOffset[ 1 ];
		final int minZb = effectiveLocalOffset[ 2 ];

		// size of the target image
		final int w = target.getDimension( 0 );
		final int h = target.getDimension( 1 );

		// size of the block image
		final int wb = block.getDimension( 0 );
		final int hb = block.getDimension( 1 );

		final float[] blockArray = ((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray();
		final float[] targetArray = ((FloatArray)((Array)target.getContainer()).update( null )).getCurrentStorageArray();
				
		for ( int z = minZ + threadIdx; z < maxZ; z += numThreads )
		{
			final int zBlock = z - minZ + minZb;
			
			int iTarget = z * h * w + minY * w + minX;
			int iBlock = zBlock * hb * wb + minYb * wb + minXb;
			
			for ( int y = minY; y < maxY; ++y )
			{			
				for ( int x = minX; x < maxX; ++x )
					targetArray[ iTarget++ ] = blockArray[ iBlock++ ];

				iTarget -= sX;
				iTarget += w;
				
				iBlock -= sX;
				iBlock += wb;
			}
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
	public static Block[] divideIntoBlocks( final int[] imgSize, final int[] blockSize, final int[] kernelSize )
	{
		final int numDimensions = imgSize.length;
		
		// compute the effective size & local offset of each block
		// this is the same for all blocks
		final int[] effectiveSizeGeneral = new int[ numDimensions ];
		final int[] effectiveLocalOffset = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			effectiveSizeGeneral[ d ] = blockSize[ d ] - kernelSize[ d ] + 1;
			
			if ( effectiveSizeGeneral[ d ] <= 0 )
			{
				System.out.println( "Blocksize in dimension " + d + " (" + blockSize[ d ] + ") is smaller than the kernel (" + kernelSize[ d ] + ") which results in an negative effective size: " + effectiveSizeGeneral[ d ] + ", retrying with double the blocksize" );
				blockSize[ d ] *= 2;
				
				return divideIntoBlocks( imgSize, blockSize, kernelSize );
			}
			
			effectiveLocalOffset[ d ] = kernelSize[ d ] / 2;
		}
		
		// compute the amount of blocks needed
		final int[] numBlocks = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			numBlocks[ d ] = imgSize[ d ] / effectiveSizeGeneral[ d ];
			
			// if the modulo is not 0 we need one more that is only partially useful
			if ( imgSize[ d ] % effectiveSizeGeneral[ d ] != 0 )
				++numBlocks[ d ];
		}
		
		 System.out.println( "imgSize " + Util.printCoordinates( imgSize ) );
		 System.out.println( "kernelSize " + Util.printCoordinates( kernelSize ) );
		 System.out.println( "blockSize " + Util.printCoordinates( blockSize ) );
		 System.out.println( "numBlocks " + Util.printCoordinates( numBlocks ) );
		 System.out.println( "effectiveSize " + Util.printCoordinates( effectiveSizeGeneral ) );
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
			final int[] effectiveSize = effectiveSizeGeneral.clone();
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
			//System.out.println( "block " + Util.printCoordinates( currentBlock ) + " effectiveOffset: " + Util.printCoordinates( effectiveOffset ) + " effectiveSize: " + Util.printCoordinates( effectiveSize )  + " offset: " + Util.printCoordinates( offset ) + " inside: " + inside );
		}
		
		final Block[] blocks = new Block[ blockList.size() ];
		for ( int i = 0; i < blockList.size(); ++i )
			blocks[ i ] = blockList.get( i );
			
		return blocks;
	}
	
	public static void main( String[] args )
	{
		new ImageJ();
		//final Image< FloatType > img = LOCI.openLOCIFloatType( "/Users/preibischs/Desktop/Geburtstagsfaust.jpg", new ArrayContainerFactory() );
		final Image< FloatType > img = LOCI.openLOCIFloatType( "/Users/preibischs/Desktop/spim.tif", new ArrayContainerFactory() );
				
		final Image< FloatType > kernel = FourierConvolution.createGaussianKernel( img.getContainerFactory(), new double[]{ 10, 10, 10 } );
		//ImageJFunctions.show( img );
		//final FourierConvolution< FloatType, FloatType > t = new FourierConvolution<FloatType, FloatType>( img, kernel );
		//t.process();
		//ImageJFunctions.show( t.getResult() );		
		
		final int[] imgSize = img.getDimensions();//new int[]{ 256, 384 };
		final int[] blockSize = new int[]{ 256, 256, 256 };
		
		//for ( int d = 0; d < blockSize.length; ++d )
		//	blockSize[ d ] = img.getDimension( d ) + kernel.getDimension( d ) - 1;
		
		final int[] kernelSize = kernel.getDimensions();//new int[]{ 51, 25 };
		
		final Block[] blocks = Block.divideIntoBlocks( imgSize, blockSize, kernelSize );
		
		final ArrayList< Image< FloatType > > blockImgs = new ArrayList< Image< FloatType > >();
		final ImageFactory< FloatType > factory = new ImageFactory< FloatType >( new FloatType(), new ArrayContainerFactory() );
		
		ImageJFunctions.show( img );	
		
		for ( int i = 0; i < blocks.length; ++i )
			blockImgs.add( factory.createImage( blockSize ) );
		
		long time = 0;
		
		//while ( time >= 0 )
		{
			time = System.currentTimeMillis();
			for ( int i = 0; i < blocks.length; ++i )
			{
				blocks[ i ].copyBlock( img, blockImgs.get( i ) );
				//ImageJFunctions.show( block );
			}

			System.out.println( System.currentTimeMillis() - time );
			//System.exit( 0 );
		}
		
		
		/*
		final FourierConvolution< FloatType, FloatType > t2 = new FourierConvolution<FloatType, FloatType>( blockImgs.get( 0 ), kernel );
		//t2.setExtendImageByKernelSize( false );

			//for ( final FloatType t : block )
			//	t.set( t.get() + 20*(i+1) );

		*/

		final Image< FloatType > img2 = img.createNewImage();
		
		//while ( time > 0 )
		{
			time = System.currentTimeMillis();
	
			for ( int i = 0; i < blocks.length; ++i )
			{
				//t2.replaceImage( blockImgs.get( i ) );
				//t2.process();
				
				blocks[ i ].pasteBlock( img2,  blockImgs.get( i ) );
				//ImageJFunctions.show( t.getResult() );
			}
			
			System.out.println( System.currentTimeMillis() - time );
		}
		ImageJFunctions.show( img2 );
		
	}
}
