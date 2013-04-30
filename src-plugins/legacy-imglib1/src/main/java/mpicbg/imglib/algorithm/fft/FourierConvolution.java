/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.fft;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.fft.FourierTransform.PreProcessing;
import mpicbg.imglib.algorithm.fft.FourierTransform.Rearrangement;
import mpicbg.imglib.algorithm.gauss.GaussianConvolution;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategy;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.complex.ComplexFloatType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class FourierConvolution<T extends RealType<T>, S extends RealType<S>> implements MultiThreaded, OutputAlgorithm<T>, Benchmark
{
	final int numDimensions;
	Image<T> image, convolved;
	Image<S> kernel;
	Image<ComplexFloatType> kernelFFT, imgFFT; 
	FourierTransform<T, ComplexFloatType> fftImage;
	boolean keepImgFFT = true;
	boolean extendImgByKernelSize = true;
	
	OutOfBoundsStrategyFactory<T> strategy = new OutOfBoundsStrategyMirrorFactory<T>();
	
	final int[] kernelDim;

	String errorMessage = "";
	int numThreads;
	long processingTime;

	public FourierConvolution( final Image<T> image, final Image<S> kernel )
	{
		this.numDimensions = image.getNumDimensions();
				
		this.image = image;
		this.kernel = kernel;
		this.kernelDim = kernel.getDimensions();
		this.kernelFFT = null;
		this.imgFFT = null;
		
		setNumThreads();
	}
	
	public Image< T > getImage() { return image; }
	public Image< S > getKernel() { return kernel; }

	public void setImageOutOfBoundsStrategy( final OutOfBoundsStrategyFactory<T> strategy ) { this.strategy = strategy; }
	public OutOfBoundsStrategyFactory<T> getImageOutOfBoundsStrategy() { return this.strategy; }
	
	public boolean replaceImage( final Image<T> img )
	{
		if ( !img.getContainer().compareStorageContainerCompatibility( this.image.getContainer() ))
		{
			errorMessage = "Image containers are not comparable, cannot exchange image";
			return false;
		}
		else
		{
			this.image = img;
			// the fft has to be recomputed
			this.imgFFT = null;
			return true;
		}
	}
	
	/**
	 * Defines if the image is extended by half the kernelsize all around its edges before computation.
	 * This way, the outoufbounds will be correct.
	 * 
	 * @param extend
	 */
	public void setExtendImageByKernelSize( final boolean extend ) { this.extendImgByKernelSize = extend; }
	public boolean getExtendImageByKernelSize() { return extendImgByKernelSize; }
	
	/**
	 * By default, he will not do the computation in-place
	 * @param keepImgFFT
	 */
	public void setKeepImgFFT( final boolean keepImgFFT ) { this.keepImgFFT = keepImgFFT; }
	public boolean getKeepImgFFT() { return this.keepImgFFT; } 

	public boolean replaceKernel( final Image<S> knl )
	{
		if ( !knl.getContainer().compareStorageContainerCompatibility( this.kernel.getContainer() ))
		{
			errorMessage = "Kernel containers are not comparable, cannot exchange image";
			return false;
		}
		else
		{
			this.kernel = knl;
			// the fft has to be recomputed
			this.kernelFFT = null;
			return true;
		}
	}

	
	final public static Image<FloatType> createGaussianKernel( final ContainerFactory factory, final double sigma, final int numDimensions )
	{
		final double[ ] sigmas = new double[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			sigmas[ d ] = sigma;
		
		return createGaussianKernel( factory, sigmas );
	}

	final public static Image<FloatType> createGaussianKernel( final ContainerFactory factory, final double[] sigmas )
	{
		final int numDimensions = sigmas.length;
		
		final int[] imageSize = new int[ numDimensions ];
		final double[][] kernel = new double[ numDimensions ][];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			kernel[ d ] = Util.createGaussianKernel1DDouble( sigmas[ d ], true );
			imageSize[ d ] = kernel[ d ].length;
		}
		
		final Image<FloatType> kernelImg = new ImageFactory<FloatType>( new FloatType(), factory ).createImage( imageSize );
		
		final LocalizableCursor<FloatType> cursor = kernelImg.createLocalizableByDimCursor();
		final int[] position = new int[ numDimensions ];
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.getPosition( position );
			
			double value = 1;
			
			for ( int d = 0; d < numDimensions; ++d )
				value *= kernel[ d ][ position[ d ] ];
			
			cursor.getType().set( (float)value );
		}
		
		cursor.close();
		
		return kernelImg;
	}

	final public static Image<FloatType> createGaussianKernel( final ContainerFactory factory, final double[] sigmas, final int precision )
	{
		final int numDimensions = sigmas.length;
		
		final int[] imageSize = new int[ numDimensions ];
		final double[][] kernel = new double[ numDimensions ][];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			kernel[ d ] = Util.createGaussianKernel1DDouble( sigmas[ d ], true, precision );
			imageSize[ d ] = kernel[ d ].length;
		}
		
		final Image<FloatType> kernelImg = new ImageFactory<FloatType>( new FloatType(), factory ).createImage( imageSize );
		
		final LocalizableCursor<FloatType> cursor = kernelImg.createLocalizableByDimCursor();
		final int[] position = new int[ numDimensions ];
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.getPosition( position );
			
			double value = 1;
			
			for ( int d = 0; d < numDimensions; ++d )
				value *= kernel[ d ][ position[ d ] ];
			
			cursor.getType().set( (float)value );
		}
		
		cursor.close();
		
		return kernelImg;
	}

	final public static <T extends RealType<T>> Image<T> getGaussianKernel( final ImageFactory<T> imgFactory, final double sigma, final int numDimensions )
	{
		final double[ ] sigmas = new double[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			sigmas[ d ] = sigma;
		
		return getGaussianKernel( imgFactory, sigmas );
	}
	
	final public static <T extends RealType<T>> Image<T> getGaussianKernel( final ImageFactory<T> imgFactory, final double[] sigma )
	{
		final int numDimensions = sigma.length;
		final int imgSize[] = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			imgSize[ d ] = Util.getSuggestedKernelDiameter( sigma[ d ] );
		
		final Image<T> kernel = imgFactory.createImage( imgSize );			
		final int[] center = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			center[ d ] = kernel.getDimension( d ) / 2;
				
		final LocalizableByDimCursor<T> c = kernel.createLocalizableByDimCursor();
		c.setPosition( center );
		c.getType().setOne();
		c.close();
		
		final GaussianConvolution<T> gauss = new GaussianConvolution<T>( kernel, new OutOfBoundsStrategyValueFactory<T>(), sigma );
		
		if ( !gauss.checkInput() || !gauss.process() )
		{
			System.out.println( "Gaussian Convolution failed: " + gauss.getErrorMessage() );
			return null;
		}
		
		kernel.close();
		
		return gauss.getResult();		
	}
	
	@Override
	public boolean process() 
	{		
		final long startTime = System.currentTimeMillis();

		//
		// compute fft of the input image
		//
		if ( imgFFT == null ) //not computed in a previous step
		{
			fftImage = new FourierTransform<T, ComplexFloatType>( image, new ComplexFloatType() );
			fftImage.setNumThreads( this.getNumThreads() );
			
			// we do not rearrange the fft quadrants
			fftImage.setRearrangement( Rearrangement.UNCHANGED );
						
			if ( extendImgByKernelSize )
			{
				// how to extend the input image out of its boundaries for computing the FFT,
				// we simply mirror the content at the borders
				//fftImage.setPreProcessing( PreProcessing.EXTEND_MIRROR );
				fftImage.setPreProcessing( PreProcessing.USE_GIVEN_OUTOFBOUNDSSTRATEGY );
				fftImage.setCustomOutOfBoundsStrategy( strategy );
			
				// the image has to be extended by the size of the kernel-1
				// as the kernel is always odd, e.g. if kernel size is 3, we need to add
				// one pixel out of bounds in each dimension (3-1=2 pixel all together) so that the
				// convolution works
				final int[] imageExtension = kernelDim.clone();		
				for ( int d = 0; d < numDimensions; ++d )
					--imageExtension[ d ];		
				fftImage.setImageExtension( imageExtension );
			}
			
			if ( !fftImage.checkInput() || !fftImage.process() )
			{
				errorMessage = "FFT of image failed: " + fftImage.getErrorMessage();
				return false;			
			}
			
			imgFFT = fftImage.getResult();
		}
		
		//
		// create the kernel for fourier transform
		//
		if ( kernelFFT == null )
		{
			// get the size of the kernel image that will be fourier transformed,
			// it has the same size as the image
			final int kernelTemplateDim[] = imgFFT.getDimensions();
			kernelTemplateDim[ 0 ] = ( imgFFT.getDimension( 0 ) - 1 ) * 2;
			
			// instaniate real valued kernel template
			// which is of the same container type as the image
			// so that the computation is easy
			final ImageFactory<S> kernelTemplateFactory = new ImageFactory<S>( kernel.createType(), image.getContainer().getFactory() );
			final Image<S> kernelTemplate = kernelTemplateFactory.createImage( kernelTemplateDim );
			
			// copy the kernel into the kernelTemplate,
			// the key here is that the center pixel of the kernel (e.g. 13,13,13)
			// is located at (0,0,0)
			final LocalizableCursor<S> kernelCursor = kernel.createLocalizableCursor();
			final LocalizableByDimCursor<S> kernelTemplateCursor = kernelTemplate.createLocalizableByDimCursor();
			
			final int[] position = new int[ numDimensions ];
			while ( kernelCursor.hasNext() )
			{
				kernelCursor.next();
				kernelCursor.getPosition( position );
				
				for ( int d = 0; d < numDimensions; ++d )
				{
					position[ d ] = ( position[ d ] - kernelDim[ d ]/2 + kernelTemplateDim[ d ] ) % kernelTemplateDim[ d ];
					/*final int tmp = ( position[ d ] - kernelDim[ d ]/2 );
					
					if ( tmp < 0 )
						position[ d ] = kernelTemplateDim[ d ] + tmp;
					else
						position[ d ] = tmp;*/
				}			
				
				kernelTemplateCursor.setPosition( position );
				kernelTemplateCursor.getType().set( kernelCursor.getType() );
			}
			
			// 
			// compute FFT of kernel
			//
			final FourierTransform<S, ComplexFloatType> fftKernel = new FourierTransform<S, ComplexFloatType>( kernelTemplate, new ComplexFloatType() );
			fftKernel.setNumThreads( this.getNumThreads() );
			
			fftKernel.setPreProcessing( PreProcessing.NONE );		
			fftKernel.setRearrangement( fftImage.getRearrangement() );
			
			if ( !fftKernel.checkInput() || !fftKernel.process() )
			{
				errorMessage = "FFT of kernel failed: " + fftKernel.getErrorMessage();
				return false;			
			}		
			kernelTemplate.close();		
			kernelFFT = fftKernel.getResult();
		}
		
		//
		// Multiply in Fourier Space
		//
		final Image< ComplexFloatType > copy;
		
		if ( keepImgFFT )
			copy = imgFFT.clone();
		else
			copy = imgFFT;
		
		long numPixels = copy.getDimension( 0 );
		for ( int d = 1; d < copy.getNumDimensions(); ++d )
			numPixels *= copy.getDimension( d );
		
		final Vector< Chunk > threadChunks = SimpleMultiThreading.divideIntoChunks( numPixels, getNumThreads() );
		
		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
        for ( int ithread = 0; ithread < threads.length; ++ithread )
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( ai.getAndIncrement() );
                	
            		multiply( myChunk.getStartPosition(), myChunk.getLoopSize(), copy, kernelFFT );
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );

		//multiply( copy, kernelFFT );
		
		//
		// Compute inverse Fourier Transform
		//		
		final InverseFourierTransform<T, ComplexFloatType> invFFT = new InverseFourierTransform<T, ComplexFloatType>( copy, fftImage );
		invFFT.setInPlaceTransform( true );
		invFFT.setNumThreads( this.getNumThreads() );

		if ( !invFFT.checkInput() || !invFFT.process() )
		{
			errorMessage = "InverseFFT of image failed: " + invFFT.getErrorMessage();
			return false;			
		}
		
		if ( !keepImgFFT )
		{
			// the imgFFT was changed during the multiplication
			// it cannot be re-used
			imgFFT.close();
			imgFFT = null;			
		}
		
		convolved = invFFT.getResult();	
		
		processingTime = System.currentTimeMillis() - startTime;
        return true;
	}
	
	/**
	 * Multiply in Fourier Space
	 * 
	 * @param a
	 * @param b
	 */
	protected void multiply( final Image< ComplexFloatType > a, final Image< ComplexFloatType > b )
	{
		final Cursor<ComplexFloatType> cursorA = a.createCursor();
		final Cursor<ComplexFloatType> cursorB = b.createCursor();
		
		while ( cursorA.hasNext() )
		{
			cursorA.fwd();
			cursorB.fwd();
			
			cursorA.getType().mul( cursorB.getType() );
		}
		
		cursorA.close();
		cursorB.close();
	}

	private final static void multiply( final long start, final long loopSize, final Image< ComplexFloatType > a, final Image< ComplexFloatType > b )
	{
		final Cursor<ComplexFloatType> cursorA = a.createCursor();
		final Cursor<ComplexFloatType> cursorB = b.createCursor();
		
		cursorA.fwd( start );
		cursorB.fwd( start );
		
		for ( long l = 0; l < loopSize; ++l )
		{
			cursorA.fwd();
			cursorB.fwd();
			
			cursorA.getType().mul( cursorB.getType() );
		}
		
		cursorA.close();
		cursorB.close();
	}
	
	@Override
	public long getProcessingTime() { return processingTime; }
	
	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }	

	@Override
	public Image<T> getResult() { return convolved; }

	@Override
	public boolean checkInput() 
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		
		if ( image == null )
		{
			errorMessage = "Input image is null";
			return false;
		}
		
		if ( kernel == null )
		{
			errorMessage = "Kernel image is null";
			return false;
		}
		
		for ( int d = 0; d < numDimensions; ++d )
			if ( kernel.getDimension( d ) % 2 != 1)
			{
				errorMessage = "Kernel image has NO odd dimensionality in dim " + d + " (" + kernel.getDimension( d ) + ")";
				return false;
			}
		
		return true;
	}
	
	public void close()
	{
		kernelFFT.close(); 
		
		image = null;
		convolved = null;
		kernel = null;
		kernelFFT = null;
		
		if ( imgFFT != null )
			imgFFT.close();
	}

	@Override
	public String getErrorMessage()  { return errorMessage; }
}
