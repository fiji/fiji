package mpicbg.spim.postprocessing.deconvolution2;

import ij.IJ;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.mirror.MirrorImage;
import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.constant.ConstantContainer;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.real.FloatType;

public class LRFFT 
{
	public static enum PSFTYPE { EXPONENT, CONDITIONAL, INDEPENDENT };
	
	public static CUDAConvolution cuda = null;
	
	private Image<FloatType> image, weight, kernel1, kernel2;
	Image<FloatType> viewContribution = null;
	FourierConvolution<FloatType, FloatType> fftConvolution1, fftConvolution2;
	protected int numViews = 0;
	
	PSFTYPE iterationType;
	ArrayList< LRFFT > views;
	
	final boolean useBlocks, useCUDA, useCPU;
	final int[] blockSize, deviceList;
	final int device0, numDevices;
	final Block[] blocks;
	final ImageFactory< FloatType > factory;
	/**
	 * Used to determine if the Convolutions already have been computed for the current iteration
	 */
	int i = -1;
	
	public LRFFT( final Image<FloatType> image, final Image<FloatType> weight, final Image<FloatType> kernel, final int[] deviceList, final boolean useBlocks, final int[] blockSize )
	{
		this.image = image;
		this.kernel1 = kernel;
		this.weight = weight;
		
		this.deviceList = deviceList;
		this.device0 = deviceList[ 0 ];
		this.numDevices = deviceList.length;

		// figure out if we need GPU and/or CPU
		boolean anyGPU = false;
		boolean anyCPU = false;
		
		for ( final int i : deviceList )
		{
			if ( i >= 0 )
				anyGPU = true;
			else if ( i == -1 )
				anyCPU = true;
		}
		
		this.useCUDA = anyGPU;
		this.useCPU = anyCPU;
				
		if ( useBlocks )
		{
			this.useBlocks = true;
			
			// define the blocksize so that it is one single block
			this.blockSize = new int[ image.getNumDimensions() ];
						
			for ( int d = 0; d < this.blockSize.length; ++d )
				this.blockSize[ d ] = blockSize[ d ];
						
			this.blocks = Block.divideIntoBlocks( image.getDimensions(), this.blockSize, kernel.getDimensions() );
			
			// blocksize might change during division if they were too small
			 //this.blockSize = blockSize.clone();
			
			this.factory = new ImageFactory< FloatType >( new FloatType(), new ArrayContainerFactory() );
		}
		else if ( this.useCUDA ) // and no blocks, i.e. one big block
		{
			this.useBlocks = true;
			
			// define the blocksize so that it is one single block
			this.blockSize = new int[ image.getNumDimensions() ];
			
			for ( int d = 0; d < this.blockSize.length; ++d )
				this.blockSize[ d ] = image.getDimension( d ) + kernel.getDimension( d ) - 1;
			
			this.blocks = Block.divideIntoBlocks( image.getDimensions(), this.blockSize, kernel.getDimensions() );
			this.factory = new ImageFactory< FloatType >( new FloatType(), new ArrayContainerFactory() );			
		}
		else
		{
			this.blocks = null;
			this.blockSize = null;
			this.factory = null;
			this.useBlocks = false;
		}		
	}

	public LRFFT( final Image<FloatType> image, final Image<FloatType> kernel, final int[] deviceList, final boolean useBlocks, final int[] blockSize )
	{
		this( image, new Image< FloatType > ( new ConstantContainer< FloatType >( image.getDimensions(), new FloatType( 1 ) ), new FloatType() ), kernel, deviceList, useBlocks, blockSize );
	}

	/**
	 * @param numViews - the number of views in the acquisition, determines the exponential of the kernel
	 */
	protected void setNumViews( final int numViews ) { this.numViews = numViews; }
	
	/**
	 * This method is called once all views are added to the {@link LRInput}
	 */
	protected void init( final PSFTYPE iterationType, final ArrayList< LRFFT > views )
	{		
		// normalize kernel so that sum of all pixels == 1
		AdjustInput.normImage( kernel1 );

		this.iterationType = iterationType;
		this.views = views;

		if ( numViews == 0 )
		{
			System.out.println( "Warning, numViews was not set." );
			numViews = 1;
		}
		
		if ( numViews == 1 || iterationType == PSFTYPE.INDEPENDENT )
		{
			// compute the inverted kernel (switch dimensions)
			this.kernel2 = computeInvertedKernel( this.kernel1 );
		}
		else if ( iterationType == PSFTYPE.CONDITIONAL )
		{
			// compute the kernel using conditional probabilities
			// if this is x1, then compute
			// P(x1|psi) * P(x2|x1) * P(x3|x1) * ... * P(xn|x1)
			// where
			// P(xi|x1) = P(x|psi) convolved with P(xi|psi)
			
			// we first get P(x1|psi)
			final Image< FloatType > tmp = computeInvertedKernel( this.kernel1.clone() );

			//ImageJFunctions.copyToImagePlus( tmp ).show();
			
			// now convolve P(x1|psi) with all other kernels 
			for ( final LRFFT view : views )
			{
				if ( view != this )
				{
					final FourierConvolution<FloatType, FloatType> conv = new FourierConvolution<FloatType, FloatType>( this.kernel1, view.kernel1 );
					conv.setNumThreads();
					conv.setKeepImgFFT( false );
					conv.setImageOutOfBoundsStrategy( new OutOfBoundsStrategyValueFactory<FloatType>() );
					conv.process();
					
					// multiply with the kernel
					final Cursor<FloatType> cursor = tmp.createCursor();
					for ( final FloatType t : ( conv.getResult() ) )
					{
						cursor.fwd();
						cursor.getType().set( t.get() * cursor.getType().get() );
					}
				}
			}
			
			// norm the compound kernel
			AdjustInput.normImage( tmp );
						
			// compute the inverted kernel
			this.kernel2 = tmp;
			
			// close the temp image
			//tmp.close();			
		}
		else //if ( iterationType == PSFTYPE.EXPONENT )
		{			
			// compute the squared kernel and its inverse
			final Image< FloatType > exponentialKernel = computeExponentialKernel( this.kernel1, numViews );
			
			// norm the squared kernel
			AdjustInput.normImage( exponentialKernel );
			
			// compute the inverted squared kernel
			this.kernel2 = computeInvertedKernel( exponentialKernel );	
		}
		
		//ImageJFunctions.show( this.kernel2 );
		//SimpleMultiThreading.threadHaltUnClean();
		
		if ( useCPU )
		{
			if ( useBlocks )
			{
				final Image< FloatType > block = factory.createImage( blockSize );
				
				this.fftConvolution1 = new FourierConvolution<FloatType, FloatType>( block, this.kernel1 );				
				this.fftConvolution1.setNumThreads();
				//this.fftConvolution1.setExtendImageByKernelSize( false );
				this.fftConvolution1.setKeepImgFFT( false );
				
				this.fftConvolution2 = new FourierConvolution<FloatType, FloatType>( block, this.kernel2 );	
				this.fftConvolution2.setNumThreads();
				//this.fftConvolution2.setExtendImageByKernelSize( false );
				this.fftConvolution2.setKeepImgFFT( false );
			}
			else
			{
				this.fftConvolution1 = new FourierConvolution<FloatType, FloatType>( this.image, this.kernel1 );	
				this.fftConvolution1.setNumThreads();
				this.fftConvolution1.setKeepImgFFT( false );
				
				this.fftConvolution2 = new FourierConvolution<FloatType, FloatType>( this.image, this.kernel2 );	
				this.fftConvolution2.setNumThreads();
				this.fftConvolution2.setKeepImgFFT( false );
			}
		}
		else
		{
			this.fftConvolution1 = null;
			this.fftConvolution2 = null;			
		}
	}
	
	public static Image<FloatType> computeExponentialKernel( final Image<FloatType> kernel, final int numViews )
	{
		final Image<FloatType> exponentialKernel = kernel.clone();
		
		for ( final FloatType f : exponentialKernel )
			f.set( pow( f.get(), numViews ) );
		
		//IJ.log("Jusrt using numViews/2 as exponent" );
		
		return exponentialKernel;
	}

	public static Image< FloatType > computeInvertedKernel( final Image< FloatType > kernel )
	{
		final Image< FloatType > invKernel = kernel.clone();
		
		for ( int d = 0; d < invKernel.getNumDimensions(); ++d )
			new MirrorImage< FloatType >( invKernel, d ).process();
		
		return invKernel;
	}

	final private static float pow( final float value, final int power )
	{
		float result = value;
		
		for ( int i = 1; i < power; ++i )
			result *= value;
		
		return result;
	}


	public void setImage( final Image<FloatType> image ) 
	{
		this.image = image;
		setCurrentIteration( -1 );
	}
	public void setWeight( final Image<FloatType> weight ) { this.weight = weight; }
	public void setKernel( final Image<FloatType> kernel ) 
	{
		this.kernel1 = kernel;
		
		init( iterationType, views );

		setCurrentIteration( -1 );
	}

	public Image<FloatType> getImage() { return image; }
	public Image<FloatType> getWeight() { return weight; }
	public Image<FloatType> getKernel1() { return kernel1; }
	public Image<FloatType> getKernel2() { return kernel2; }
	
	public void setCurrentIteration( final int i ) { this.i = i; }
	public int getCurrentIteration() { return i; }

	/**
	 * convolves the image with kernel1
	 * 
	 * @param image - the image to convolve with
	 * @return
	 */
	public Image< FloatType > convolve1( final Image< FloatType > image )
	{
		if ( useCPU && !useCUDA )
		{
			if ( useBlocks )
			{
				IJ.log( "Using CPU only on blocks ... " );
				
				final Image< FloatType > result = image.createNewImage();
				final Image< FloatType > block = factory.createImage( blockSize );
				
				for ( int i = 0; i < blocks.length; ++i )
				{
					/*
					long time = System.currentTimeMillis();
					blocks[ i ].copyBlock( image, block );
					System.out.println( " block " + i + ": copy " + (System.currentTimeMillis() - time) );

					time = System.currentTimeMillis();				
					fftConvolution1.replaceImage( block );
					fftConvolution1.process();
					System.out.println( " block " + i + ": compute " + (System.currentTimeMillis() - time) );
					
					time = System.currentTimeMillis();				
					blocks[ i ].pasteBlock( result, fftConvolution1.getResult() );					
					System.out.println( " block " + i + ": paste " + (System.currentTimeMillis() - time) );
					*/
					LRFFTThreads.convolve1BlockCPU( blocks[ i ], i, image, result, block, fftConvolution1 );
				}
				
				block.close();
				
				return result;
			}
			else
			{
				IJ.log( "Using CPU only to compute as one block ... " );

				long time = System.currentTimeMillis();
				final FourierConvolution<FloatType, FloatType> fftConv = fftConvolution1;
				fftConv.replaceImage( image );
				fftConv.process();
				System.out.println( " block " + i + ": compute " + (System.currentTimeMillis() - time) );
				
				return fftConv.getResult();				
			}
		}
		else if ( useCUDA && !useCPU && numDevices == 1 )
		{
			//if ( blocks.length > 1 )
			//	IJ.log( "Using CUDA only on blocks ... " );
			//else
			//	IJ.log( "Using CUDA only to compute as one block ... " );
			
			final Image< FloatType > result = image.createNewImage();
			final Image< FloatType > block = factory.createImage( blockSize );
			
			for ( int i = 0; i < blocks.length; ++i )
			{
				/*
				long time = System.currentTimeMillis();
				blocks[ i ].copyBlock( image, block );
				System.out.println( " block " + i + ": copy " + (System.currentTimeMillis() - time) );

				// convolve block with kernel1 using CUDA
				time = System.currentTimeMillis();				
				cuda.convolution3DfftCUDAInPlace( ((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( blockSize ), 
						((FloatArray)((Array)kernel1.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( kernel1.getDimensions() ), device0 );
				System.out.println( " block " + i + ": compute " + (System.currentTimeMillis() - time) );

				time = System.currentTimeMillis();
				blocks[ i ].pasteBlock( result, block );
				System.out.println( " block " + i + ": paste " + (System.currentTimeMillis() - time) );
				*/
				LRFFTThreads.convolve1BlockCUDA( blocks[ i ], i, device0, image, result, block, kernel1, blockSize );
			}
			
			block.close();
			
			return result;
		}
		else
		{
			// this implies useBlocks, otherwise we cannot combine several devices
			//IJ.log( "Using CUDA & CPU on blocks ... " );
			
			final Image< FloatType > result = image.createNewImage();
			
			final AtomicInteger ai = new AtomicInteger();
			final Thread[] threads = SimpleMultiThreading.newThreads( deviceList.length );
			
			for ( int i = 0; i < deviceList.length; ++i )
			{
				if ( deviceList[ i ] == -1 )
					threads[ i ] = LRFFTThreads.getCPUThread1( ai, blocks, blockSize, factory, image, result, fftConvolution1 );
				else
					threads[ i ] = LRFFTThreads.getCUDAThread1( ai, blocks, blockSize, factory, image, result, deviceList[ i ], kernel1 );
			}
			
			SimpleMultiThreading.startAndJoin( threads );
			
			return result;
		}
	}
	

	final public static Image<FloatType> createImageFromArray( final float[] data, final int[] dim )
    {
        final FloatAccess access = new FloatArray( data );
        final Array<FloatType, FloatAccess> array = 
            new Array<FloatType, FloatAccess>(new ArrayContainerFactory(), access, dim, 1 );
            
        // create a Type that is linked to the container
        final FloatType linkedType = new FloatType( array );
        
        // pass it to the DirectAccessContainer
        array.setLinkedType( linkedType );
        
        return new Image<FloatType>(array, new FloatType());
    }
	
	/**
	 * convolves the image with kernel2 (inverted kernel1)
	 * 
	 * @param image - the image to convolve with
	 * @return
	 */
	public Image< FloatType > convolve2( final Image< FloatType > image )
	{
		if ( useCPU && !useCUDA )
		{
			if ( useBlocks )
			{
				final Image< FloatType > result = image.createNewImage();
				final Image< FloatType > block = factory.createImage( blockSize );
				
				for ( int i = 0; i < blocks.length; ++i )
				{
					/*
					blocks[ i ].copyBlock( image, block );

					fftConvolution2.replaceImage( block );
					fftConvolution2.process();
					
					blocks[ i ].pasteBlock( result, fftConvolution2.getResult() );
					*/
					LRFFTThreads.convolve2BlockCPU( blocks[ i ], image, result, block, fftConvolution2 );
				}
				
				block.close();
				
				return result;
			}
			else
			{
				final FourierConvolution<FloatType, FloatType> fftConv = fftConvolution2;
				fftConv.replaceImage( image );
				fftConv.process();			
				return fftConv.getResult();				
			}			
		}
		else if ( useCUDA && !useCPU && numDevices == 1 )
		{
			final Image< FloatType > result = image.createNewImage();
			final Image< FloatType > block = factory.createImage( blockSize );
			
			for ( int i = 0; i < blocks.length; ++i )
			{
				/*
				blocks[ i ].copyBlock( image, block );

				// convolve block with kernel2 using CUDA
				cuda.convolution3DfftCUDAInPlace( ((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( blockSize ), 
						((FloatArray)((Array)kernel2.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( kernel2.getDimensions() ), device0 );

				blocks[ i ].pasteBlock( result, block );
				*/
				LRFFTThreads.convolve2BlockCUDA( blocks[ i ], device0, image, result, block, block, blockSize );
			}
			
			block.close();
			
			return result;
		}
		else
		{
			final Image< FloatType > result = image.createNewImage();
			
			final AtomicInteger ai = new AtomicInteger();
			final Thread[] threads = SimpleMultiThreading.newThreads( deviceList.length );
			
			for ( int i = 0; i < deviceList.length; ++i )
			{
				if ( deviceList[ i ] == -1 )
					threads[ i ] = LRFFTThreads.getCPUThread2( ai, blocks, blockSize, factory, image, result, fftConvolution2 );
				else
					threads[ i ] = LRFFTThreads.getCUDAThread2( ai, blocks, blockSize, factory, image, result, deviceList[ i ], kernel2 );
			}
			
			SimpleMultiThreading.startAndJoin( threads );
			
			return result;
		}
	}

	@Override
	public LRFFT clone()
	{
		final LRFFT viewClone = new LRFFT( this.image.clone(), this.weight.clone(), this.kernel1.clone(), deviceList, useBlocks, blockSize );
	
		viewClone.numViews = numViews;
		viewClone.iterationType = iterationType;
		viewClone.views = views;
		viewClone.i = i;
		
		if ( this.kernel2 != null )
			viewClone.kernel2 = kernel2.clone();
		
		if ( this.viewContribution != null )
			viewClone.viewContribution = this.viewContribution.clone();
		
		if ( this.fftConvolution1 != null )
		{
			viewClone.fftConvolution1 = new FourierConvolution<FloatType, FloatType>( fftConvolution1.getImage(), fftConvolution1.getKernel() );
			viewClone.fftConvolution1.process();
		}

		if ( this.fftConvolution2 != null )
		{
			viewClone.fftConvolution2 = new FourierConvolution<FloatType, FloatType>( fftConvolution2.getImage(), fftConvolution2.getKernel() );
			viewClone.fftConvolution2.process();
		}

		return viewClone;
	}	
}
