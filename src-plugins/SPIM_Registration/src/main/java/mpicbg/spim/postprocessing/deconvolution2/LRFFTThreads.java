package mpicbg.spim.postprocessing.deconvolution2;

import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.real.FloatType;

public class LRFFTThreads 
{
	final protected static void convolve1BlockCPU( final Block blockStruct, final int i, final Image<FloatType> image, final Image<FloatType> result, final Image<FloatType> block, final FourierConvolution<FloatType, FloatType> fftConvolution1 )
	{
		long time = System.currentTimeMillis();
		blockStruct.copyBlock( image, block );
		System.out.println( " block " + i + "(CPU): copy " + (System.currentTimeMillis() - time) );

		time = System.currentTimeMillis();				
		fftConvolution1.replaceImage( block );
		fftConvolution1.process();
		System.out.println( " block " + i + "(CPU): compute " + (System.currentTimeMillis() - time) );
		
		time = System.currentTimeMillis();				
		blockStruct.pasteBlock( result, fftConvolution1.getResult() );					
		System.out.println( " block " + i + "(CPU): paste " + (System.currentTimeMillis() - time) );		
	}
	
	final protected static void convolve2BlockCPU( final Block blockStruct, final Image<FloatType> image, final Image<FloatType> result, final Image<FloatType> block, final FourierConvolution<FloatType, FloatType> fftConvolution2 )
	{
		blockStruct.copyBlock( image, block );

		fftConvolution2.replaceImage( block );
		fftConvolution2.process();
		
		blockStruct.pasteBlock( result, fftConvolution2.getResult() );
	}
	
	final protected static void convolve1BlockCUDA( final Block blockStruct, final int i, final int deviceId, final Image<FloatType> image, final Image<FloatType> result, final Image<FloatType> block, 
			final Image<FloatType> kernel1, final int[] blockSize )
	{
		long time = System.currentTimeMillis();
		blockStruct.copyBlock( image, block );
		System.out.println( " block " + i + "(CPU  " + deviceId + "): copy " + (System.currentTimeMillis() - time) );

		// convolve block with kernel1 using CUDA
		time = System.currentTimeMillis();				
		LRFFT.cuda.convolution3DfftCUDAInPlace( ((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( blockSize ), 
				((FloatArray)((Array)kernel1.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( kernel1.getDimensions() ), deviceId );
		System.out.println( " block " + i + "(CUDA " + deviceId + "): compute " + (System.currentTimeMillis() - time) );

		time = System.currentTimeMillis();
		blockStruct.pasteBlock( result, block );
		System.out.println( " block " + i + "(CPU  " + deviceId + "): paste " + (System.currentTimeMillis() - time) );
	}

	final protected static void convolve2BlockCUDA( final Block blockStruct, final int deviceId, final Image<FloatType> image, final Image<FloatType> result, final Image<FloatType> block, 
			final Image<FloatType> kernel2, final int[] blockSize )
	{
		blockStruct.copyBlock( image, block );

		// convolve block with kernel2 using CUDA
		LRFFT.cuda.convolution3DfftCUDAInPlace( ((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( blockSize ), 
				((FloatArray)((Array)kernel2.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( kernel2.getDimensions() ), deviceId );

		blockStruct.pasteBlock( result, block );		
	}
	
	final protected static Thread getCPUThread1( final AtomicInteger ai, final Block[] blocks, final int[] blockSize, final ImageFactory< FloatType > factory,
			final Image<FloatType> image, final Image<FloatType> result, final FourierConvolution<FloatType, FloatType> fftConvolution1 )
	{
		final Thread cpuThread1 = new Thread(new Runnable()
		{
			public void run()
			{
				final Image< FloatType > block = factory.createImage( blockSize );

				int i;

				while ( ( i = ai.getAndIncrement() ) < blocks.length )
				{
					convolve1BlockCPU( blocks[ i ], i, image, result, block, fftConvolution1 );					
				}
				
				block.close();
			}
		});
		
		return cpuThread1;
	}

	final protected static Thread getCPUThread2( final AtomicInteger ai, final Block[] blocks, final int[] blockSize, final ImageFactory< FloatType > factory,
			final Image<FloatType> image, final Image<FloatType> result, final FourierConvolution<FloatType, FloatType> fftConvolution2 )
	{
		final Thread cpuThread2 = new Thread(new Runnable()
		{
			public void run()
			{
				final Image< FloatType > block = factory.createImage( blockSize );

				int i;

				while ( ( i = ai.getAndIncrement() ) < blocks.length )
				{
					convolve2BlockCPU( blocks[ i ], image, result, block, fftConvolution2 );					
				}
				
				block.close();
			}
		});
		
		return cpuThread2;
	}

	final protected static Thread getCUDAThread1( final AtomicInteger ai, final Block[] blocks, final int[] blockSize, final ImageFactory< FloatType > factory,
			final Image<FloatType> image, final Image<FloatType> result, final int deviceId, final Image<FloatType> kernel1 )
	{
		final Thread cudaThread1 = new Thread(new Runnable()
		{
			public void run()
			{
				final Image< FloatType > block = factory.createImage( blockSize );

				int i;

				while ( ( i = ai.getAndIncrement() ) < blocks.length )
				{
					convolve1BlockCUDA( blocks[ i ], i, deviceId, image, result, block, kernel1, blockSize );					
				}
				
				block.close();
			}
		});
		
		return cudaThread1;
	}

	final protected static Thread getCUDAThread2( final AtomicInteger ai, final Block[] blocks, final int[] blockSize, final ImageFactory< FloatType > factory,
			final Image<FloatType> image, final Image<FloatType> result, final int deviceId, final Image<FloatType> kernel2 )
	{
		final Thread cudaThread2 = new Thread(new Runnable()
		{
			public void run()
			{
				final Image< FloatType > block = factory.createImage( blockSize );

				int i;

				while ( ( i = ai.getAndIncrement() ) < blocks.length )
				{
					convolve2BlockCUDA( blocks[ i ], deviceId, image, result, block, kernel2, blockSize );					
				}
				
				block.close();
			}
		});
		
		return cudaThread2;
	}

	private final static int[] getCUDACoordinates( final int[] c )
	{
		final int[] cuda = new int[ c.length ];
		
		for ( int d = 0; d < c.length; ++d )
			cuda[ c.length - d - 1 ] = c[ d ];
		
		return cuda;
	}

}
