package mpicbg.spim.postprocessing.deconvolution2;

import ij.IJ;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.mirror.MirrorImage;
import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.constant.ConstantContainer;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.real.FloatType;

import com.sun.jna.Library;

public class LRFFT 
{
	public static CUDAConvolution cuda = null;
	
	private Image<FloatType> image, weight, kernel1, kernel2;
	Image<FloatType> viewContribution = null;
	FourierConvolution<FloatType, FloatType> fftConvolution1, fftConvolution2;
	protected int numViews = 0;
	boolean useExponentialKernel = false;
	
	final boolean useBlocks, useCUDA;
	final int[] blockSize;
	final ArrayList< Block > blocks;
	final ImageFactory< FloatType > factory;
	/**
	 * Used to determine if the Convolutions already have been computed for the current iteration
	 */
	int i = -1;
	
	public LRFFT( final Image<FloatType> image, final Image<FloatType> weight, final Image<FloatType> kernel, final boolean useCUDA, final boolean useBlocks, final int[] blockSize )
	{
		this.image = image;
		this.kernel1 = kernel;
		this.weight = weight;
		this.useCUDA = useCUDA;
				
		if ( useBlocks )
		{
			this.useBlocks = true;
			
			this.blocks = Block.divideIntoBlocks( image.getDimensions(), blockSize, kernel.getDimensions() );
			
			// blocksize might change during division if they were too small
			this.blockSize = blockSize.clone();
			
			this.factory = new ImageFactory< FloatType >( new FloatType(), new ArrayContainerFactory() );
		}
		else if ( this.useCUDA ) // and no blocks, i.e. one big block
		{
			this.useBlocks = true;
			
			// define the blocksize so that it is one single block
			this.blockSize = new int[ image.getNumDimensions() ];
			
			for ( int d = 0; d < this.blockSize.length; ++d )
				this.blockSize[ d ] = image.getDimension( d ) + kernel.getDimension( d ) - 1;
			
			this.blocks = Block.divideIntoBlocks( image.getDimensions(), blockSize, kernel.getDimensions() );
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

	public LRFFT( final Image<FloatType> image, final Image<FloatType> kernel, final boolean useCUDA, final boolean useBlocks, final int[] blockSize )
	{
		this( image, new Image< FloatType > ( new ConstantContainer< FloatType >( image.getDimensions(), new FloatType( 1 ) ), new FloatType() ), kernel, useCUDA, useBlocks, blockSize );
	}

	/**
	 * @param numViews - the number of views in the acquisition, determines the exponential of the kernel
	 */
	protected void setNumViews( final int numViews ) { this.numViews = numViews; }
	
	/**
	 * This method is called once all views are added to the {@link LRInput}
	 */
	protected void init( final boolean useExponentialKernel )
	{
		this.useExponentialKernel = useExponentialKernel;
		
		// normalize kernel so that sum of all pixels == 1
		AdjustInput.normImage( kernel1 );

		if ( useExponentialKernel )
		{
			if ( numViews == 0 )
			{
				System.out.println( "Warning, numViews was not set." );
				numViews = 1;
			}
			
			// compute the squared kernel and its inverse
			final Image< FloatType > exponentialKernel = computeExponentialKernel( this.kernel1, numViews );
			
			// norm the squared kernel
			AdjustInput.normImage( exponentialKernel );
			
			// compute the inverted squared kernel
			this.kernel2 = computeInvertedKernel( exponentialKernel );	
		}
		else
		{
			// compute the inverted kernel (switch dimensions)
			this.kernel2 = computeInvertedKernel( this.kernel1 );
		}
		
		if ( useCUDA )
		{
			this.fftConvolution1 = null;
			this.fftConvolution2 = null;
			
			
		}
		else
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
	}
	
	public static Image<FloatType> computeExponentialKernel( final Image<FloatType> kernel, final int numViews )
	{
		final Image<FloatType> exponentialKernel = kernel.clone();
		
		for ( final FloatType f : exponentialKernel )
			f.set( pow( f.get(), numViews ) );
		
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
		
		init( this.useExponentialKernel );

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
		if ( useCUDA )
		{
			IJ.log( "Using CUDA ... " );
			
			final Image< FloatType > result = image.createNewImage();
			final Image< FloatType > block = factory.createImage( blockSize );
			
			for ( int i = 0; i < blocks.size(); ++i )
			{
				blocks.get( i ).copyBlock( image, block );

				// convolve block with kernel1 using CUDA
				
				//((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray();
				
				cuda.convolution3DfftCUDAInPlace( ((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( blockSize ), 
						((FloatArray)((Array)kernel1.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( kernel1.getDimensions() ), 0 );
				
				blocks.get( i ).pasteBlock( result, block );
			}
			
			block.close();
			
			return result;
		}
		else
		{
			if ( useBlocks )
			{
				IJ.log( "Using blocks ... " );
				
				final Image< FloatType > result = image.createNewImage();
				final Image< FloatType > block = factory.createImage( blockSize );
				
				for ( int i = 0; i < blocks.size(); ++i )
				{
					blocks.get( i ).copyBlock( image, block );

					fftConvolution1.replaceImage( block );
					fftConvolution1.process();
					
					blocks.get( i ).pasteBlock( result, fftConvolution1.getResult() );
				}
				
				block.close();
				
				return result;
			}
			else
			{
				IJ.log( "Using standard way ... " );
				
				final FourierConvolution<FloatType, FloatType> fftConv = fftConvolution1;
				fftConv.replaceImage( image );
				fftConv.process();			
				return fftConv.getResult();				
			}
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
	 
	private final static int[] getCUDACoordinates( final int[] c )
	{
		final int[] cuda = new int[ c.length ];
		
		for ( int d = 0; d < c.length; ++d )
			cuda[ c.length - d - 1 ] = c[ d ];
		
		return cuda;
	}
	
	/**
	 * convolves the image with kernel2 (inverted kernel1)
	 * 
	 * @param image - the image to convolve with
	 * @return
	 */
	public Image< FloatType > convolve2( final Image< FloatType > image )
	{
		if ( useCUDA )
		{
			final Image< FloatType > result = image.createNewImage();
			final Image< FloatType > block = factory.createImage( blockSize );
			
			for ( int i = 0; i < blocks.size(); ++i )
			{
				blocks.get( i ).copyBlock( image, block );

				// convolve block with kernel2 using CUDA
				cuda.convolution3DfftCUDAInPlace( ((FloatArray)((Array)block.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( blockSize ), 
						((FloatArray)((Array)kernel2.getContainer()).update( null )).getCurrentStorageArray(), getCUDACoordinates( kernel2.getDimensions() ), 0 );

				blocks.get( i ).pasteBlock( result, block );
			}
			
			block.close();
			
			return result;
		}
		else
		{
			if ( useBlocks )
			{
				final Image< FloatType > result = image.createNewImage();
				final Image< FloatType > block = factory.createImage( blockSize );
				
				for ( int i = 0; i < blocks.size(); ++i )
				{
					blocks.get( i ).copyBlock( image, block );

					fftConvolution2.replaceImage( block );
					fftConvolution2.process();
					
					blocks.get( i ).pasteBlock( result, fftConvolution2.getResult() );
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
	}

	@Override
	public LRFFT clone()
	{
		final LRFFT viewClone = new LRFFT( this.image.clone(), this.weight.clone(), this.kernel1.clone(), useCUDA, useBlocks, blockSize );
	
		viewClone.numViews = numViews;
		viewClone.useExponentialKernel = useExponentialKernel;
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
