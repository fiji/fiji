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

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.fft.FourierTransform.Rearrangement;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.ComplexType;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class InverseFourierTransform<T extends RealType<T>, S extends ComplexType<S>> implements MultiThreaded, OutputAlgorithm<T>, Benchmark
{
	final Image<S> fftImage;	
	final int numDimensions;
	Image<T> image;
	T type;
	
	Rearrangement rearrangement;

	String errorMessage = "";
	int numThreads;
	long processingTime;
	boolean scale, inPlace, cropBack;
	int[] originalSize, originalOffset; 
	float additionalNormalization;

	public InverseFourierTransform( final Image<S> fftImage, final T type, final Rearrangement rearrangement, 
									final boolean inPlace, final boolean scale, final boolean cropBack, 
									final int[] originalSize, final int[] originalOffset )
	{
		this.fftImage = fftImage;
		this.type = type;
		this.numDimensions = fftImage.getNumDimensions();
		
		this.rearrangement = rearrangement;
		this.scale = scale;
		this.inPlace = inPlace;
		this.cropBack = cropBack;
		
		this.additionalNormalization = 1;
		
		if ( originalSize != null )
			this.originalSize = originalSize.clone();
		
		if ( originalOffset != null )
			this.originalOffset = originalOffset.clone();
		
		setNumThreads();
	}
	
	public InverseFourierTransform( final Image<S> fftImage, final FourierTransform<T,?> forwardTransform )
	{
		this ( fftImage, forwardTransform.getImageType(), forwardTransform.getRearrangement(), false, true, true, forwardTransform.getFFTInputSize(), forwardTransform.getFFTInputOffset() );
	}

	public InverseFourierTransform( final Image<S> fftImage, final FourierTransform<?,?> forwardTransform, final T type )
	{
		this ( fftImage, type, forwardTransform.getRearrangement(), false, true, true, forwardTransform.getFFTInputSize(), forwardTransform.getFFTInputOffset() );
	}

	public InverseFourierTransform( final Image<S> fftImage, final T type )
	{
		this( fftImage, type, Rearrangement.REARRANGE_QUADRANTS, false, true, false, null, null );
	}
	
	public void setRearrangement( final Rearrangement rearrangement ) { this.rearrangement = rearrangement; }
	public void setInPlaceTransform( final boolean inPlace ) { this.inPlace = inPlace; }
	public void setDoScaling( final boolean scale ) { this.scale = scale; }
	public void setCropBackToOriginalSize( final boolean cropBack ) { this.cropBack = cropBack; }
	public void setOriginalSize( final int[] originalSize ) { this.originalSize = originalSize; }
	public void setOriginalOffset( final int[] originalOffset ) { this.originalOffset = originalOffset; }
	public void setAdditionalNormalization( final float additionalNormalization ) { this.additionalNormalization = additionalNormalization; }

	public Rearrangement getRearrangement() { return rearrangement; }
	public boolean getInPlaceTransform() { return inPlace; }
	public boolean getDoScaling() { return scale; }
	public boolean getCropBackToOriginalSize() { return cropBack; }
	public int[] getOriginalSize() { return originalSize.clone(); }
	public int[] getOriginalOffset() { return originalOffset.clone(); }
	public float getAdditionalNormalization() { return additionalNormalization; }

	@Override
	public boolean process() 
	{		
		final long startTime = System.currentTimeMillis();

		// in Place computation will destroy the image
		final Image<S> complex;		
		
		if ( inPlace )
			complex = fftImage;
		else
			complex = fftImage.clone();
			
		if ( rearrangement == Rearrangement.REARRANGE_QUADRANTS )
			FFTFunctions.rearrangeFFTQuadrants( complex, false, getNumThreads() );

		// perform inverse FFT 					
		image = FFTFunctions.computeInverseFFT( complex, type, getNumThreads(), scale, cropBack, originalSize, originalOffset, additionalNormalization );
		
		if ( !inPlace )
			complex.close();

		processingTime = System.currentTimeMillis() - startTime;

        return true;
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
	public Image<T> getResult() { return image; }

	@Override
	public boolean checkInput() 
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( fftImage == null )
		{
			errorMessage = "Input image is null";
			return false;
		}
		else
		{
			return true;
		}
	}

	@Override
	public String getErrorMessage()  { return errorMessage; }

}
