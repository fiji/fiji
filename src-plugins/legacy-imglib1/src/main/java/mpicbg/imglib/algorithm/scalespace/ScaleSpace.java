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

package mpicbg.imglib.algorithm.scalespace;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.function.SubtractNormReal;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal;
import mpicbg.imglib.algorithm.math.ImageCalculator;
import mpicbg.imglib.algorithm.math.ImageConverter;
import mpicbg.imglib.algorithm.math.NormalizeImageMinMax;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.Function;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ScaleSpace< A extends Type<A>, B extends RealType<B> > implements OutputAlgorithm<B>, MultiThreaded, Benchmark
{
	final Image<A> image;
	final ImageFactory<B> processFactory;
	final Converter<A, B> converter;
	
	ContainerFactory scaleSpaceContainerFactory;
	ArrayList<DifferenceOfGaussianPeak<B>> peaks;
	Image<B> scaleSpace;
	
	double initialSigma, scale, imageSigma;
	int minImageSize, stepsPerOctave;
	
	long processingTime;
	int numThreads;
	String errorMessage = "";
	
	public ScaleSpace( final Image<A> image, final ImageFactory<B> processFactory, final Converter<A, B> converter, final double initialSigma )
	{
		setNumThreads();
		
		this.image = image;
		this.processFactory = processFactory;
		this.converter = converter;
		
		this.scaleSpaceContainerFactory = processFactory.getContainerFactory(); //new ArrayContainerFactory(); //new CellContainerFactory( 64 );
		
		this.initialSigma = initialSigma;
		this.scale = 1.0;
		this.imageSigma = 0.5;
		this.minImageSize = 16;
		this.stepsPerOctave = 7;
	}
	
	@Override
	public Image<B> getResult() { return scaleSpace; }
	public ArrayList<DifferenceOfGaussianPeak<B>> getPeaks() { return peaks; }
	
	public void setMinImageSize( final int minImageSize ) { this.minImageSize = minImageSize; }
	public int getMinImageSize() { return minImageSize; }
	
	@Override
	public boolean process()
	{
		final long startTime = System.currentTimeMillis();
		
		//
		// compute the input image by upsampling or converting
		//
		final Image<B> input;
		
		if ( initialSigma < 1.0 )
		{
			input = upSample( image, processFactory, converter );
			
			imageSigma *= 2.0;
			initialSigma *= 2.0;
			scale = 2.0;
		}
		else
		{
			input = convert( image, processFactory, converter );
		}
	
		if ( input == null )
		{
			errorMessage = "Error creating input image: " + errorMessage;
			return false;
		}
		
		//
		// normalize the image to [0 ... 1]
		//
		if ( !normImageMinMax( input ) )
		{
			input.close();
			return false;
		}

		//
		// compute the necessary sigmas and normalization
		//
		double[] sigma = getSigmas( input, initialSigma, minImageSize, stepsPerOctave );
		double[] sigmaInc = getIncrementalSigmas( sigma, imageSigma );
		double norm = getNormalizationFactor( stepsPerOctave );
				
		//
		// build scale space
		//
		scaleSpace = computeScaleSpace( input, sigmaInc, norm, scaleSpaceContainerFactory );
		
		if ( scaleSpace == null )
		{
			errorMessage = "Cannot compute scale space: " + errorMessage;
			input.close();
			return false;			
		}
		
		//
		// find extrema
		//
		DifferenceOfGaussianReal<B, B> dog = new DifferenceOfGaussianReal<B, B>( scaleSpace, scaleSpace.getImageFactory(), null, 0, 0, 0.03f, 0 );
		peaks = dog.findPeaks( scaleSpace );
		
		//
		// subpixel localize them
		//
		SubpixelLocalization<B> spl = new SubpixelLocalization<B>( scaleSpace, peaks );
		spl.setNumThreads( getNumThreads() );
		
		if ( !spl.checkInput() || !spl.process() )
		{
			errorMessage = "Cannot compute subpixel localization: " + spl.getErrorMessage();
			scaleSpace.close();
			return false;
		}
		
		//
		// adjust the correct sigma and correct the locations if the image was originally upscaled
		//
		for ( final DifferenceOfGaussianPeak<B> peak : peaks )
		{
			// +0.5 to get it relative to the sigmas and not the difference of the sigmas 
			// e.g. dog 1 corresponds to between sigmas 1 and 2
			double size = peak.getSubPixelPosition( scaleSpace.getNumDimensions() - 1 ) + 0.5f; 			
			size = initialSigma * Math.pow( 2.0f, size / ( double )stepsPerOctave );
			
			peak.setPixelLocation( (int)Math.round(size), scaleSpace.getNumDimensions() - 1 );
			peak.setSubPixelLocationOffset( (float)size - (int)Math.round(size), scaleSpace.getNumDimensions() - 1 );
			
			if ( scale != 1.0 )
				for ( int d = 0; d < scaleSpace.getNumDimensions(); ++d )
				{				
					final float sizeHalf = peak.getSubPixelPosition( d ) / 2.0f;
					final int pixelLocation = Util.round( sizeHalf );
					
					peak.setPixelLocation( pixelLocation, d );
					peak.setSubPixelLocationOffset( sizeHalf - pixelLocation, d );					
				}
		}

		//
		// 
		//
		
		processingTime = System.currentTimeMillis() - startTime;
				
		return true;
	}
	
	protected Image<B> computeScaleSpace( final Image<B> image, final double[] sigma, double norm, final ContainerFactory factory )
	{ 
		// compute the dimensions for the scale space
		final int[] dimensions = new int[ image.getNumDimensions() + 1 ];		
		image.getDimensions( dimensions );
		dimensions[ image.getNumDimensions() ] = sigma.length - 1;
		
		// create scale space
		final Image<B> scaleSpace = new ImageFactory<B>( image.createType(), factory ).createImage( dimensions, "Scalespace of " + image.getName() );
		
		// compute scale space
		// compute inital gaussian convolution		
		Image<B> gauss1 = null, gauss2 = null;
		
		final GaussianConvolutionReal<B> gauss = new GaussianConvolutionReal<B>( image, new OutOfBoundsStrategyMirrorFactory<B>(), sigma[ 0 ] );
		gauss.setNumThreads( getNumThreads() );
		
		if ( !gauss.checkInput() || !gauss.process() )
		{
			errorMessage = "Cannot compute inital gaussian convolution: " + gauss.getErrorMessage();
			scaleSpace.close();
			return null;
		}
		
		gauss1 = gauss.getResult();
		
		// compute all scales
		for ( int s = 1; s < sigma.length; ++s )
		{
			// compute gaussian convolution
			gauss.setImage( gauss1 );
			gauss.setSigma( sigma[ s ] );
			
			if ( !gauss.checkInput() || !gauss.process() )
			{
				errorMessage = "Cannot compute gaussian convolution with sigma=" + sigma[ s ] + " : " + gauss.getErrorMessage();
				scaleSpace.close();
				return null;
			}

			gauss2 = gauss.getResult();
			
			// compute difference of gaussian, overwrite gauss1
	        final Function<B, B, B> function = new SubtractNormReal<B, B, B>( norm );        
	        final ImageCalculator<B, B, B> imageCalc = new ImageCalculator<B, B, B>( gauss2, gauss1, gauss1, function );
	        imageCalc.setNumThreads( getNumThreads() );
	        
	        if ( !imageCalc.checkInput() || !imageCalc.process() )
	        {
	        	errorMessage = "Cannot subtract images: " + imageCalc.getErrorMessage();
	        	
	        	scaleSpace.close();
	        	gauss1.close();
	        	gauss2.close();
	        	
	        	return null;
	        }

	        // copy DoG image into the scalespace
	        final LocalizableCursor<B> cursorIn = gauss1.createLocalizableCursor();
	        final LocalizableByDimCursor<B> cursorOut = scaleSpace.createLocalizableByDimCursor();
	        
	        final int[] position = cursorOut.getPosition();
	        position[ scaleSpace.getNumDimensions() - 1 ] = s - 1;	        
	        
	        while ( cursorIn.hasNext() )
	        {
	        	cursorIn.fwd();
	        	
	        	// it will only overwrite the lower dimensions
	        	cursorIn.getPosition( position );	        	
	        	cursorOut.moveTo( position );
	        	
	        	cursorOut.getType().set( cursorIn.getType() );
	        }
	        
	        cursorIn.close();
	        cursorOut.close();
	        
	        gauss1.close();
	        
	        // update the lower sigma image
	        gauss1 = gauss2;
	        gauss2 = null;
		}
		
		gauss1.close();
		
		return scaleSpace;
	}
	
	protected double getNormalizationFactor( final int stepsPerOctave )
	{
		double K = Math.pow( 2.0, 1.0 / stepsPerOctave );
		double K_MIN1_INV = 1.0f / ( K - 1.0f );
		
		return K_MIN1_INV;
	}
	
	protected double[] getIncrementalSigmas( final double[] sigma, final double imageSigma )
	{
		final double[] sigmaInc = new double[ sigma.length ];
		
		// first convolution is to the inital sigma
		sigmaInc[ 0 ] = Math.sqrt( sigma[ 0 ] * sigma[ 0 ] - imageSigma * imageSigma );	

		// the others are always to the previous convolution
		for ( int i = 1; i < sigma.length; ++i )
			sigmaInc[ i ] = Math.sqrt( sigma[ i ] * sigma[ i ] - sigma[ i - 1 ] * sigma[ i - 1 ] );				
		
		return sigmaInc;
	}
	
	protected double[] getSigmas( final Image<?> img, final double initialSigma, final int minImageSize, final int stepsPerOctave )
	{
		int minDim = img.getDimension( 0 );
		
		for ( int d = 1; d < img.getNumDimensions(); ++d )
			minDim = Math.min( minDim, img.getDimension( d ) );
		
		final int numOctaves = (int)Math.round( Util.log2( minDim ) - Util.log2( minImageSize ) + 0.25 );
		
		final double[] sigma = new double[ numOctaves * stepsPerOctave + 3 ];
		
		for ( int i = 0; i < sigma.length; ++i )
			sigma[ i ] = initialSigma * Math.pow( 2.0f, ( double )i / ( double )stepsPerOctave );
		
		return sigma;
	}
	
	protected boolean normImageMinMax( final Image<B> image )
	{
		NormalizeImageMinMax<B> norm = new NormalizeImageMinMax<B>( image );
		norm.setNumThreads( getNumThreads() );
		
		if ( !norm.checkInput() || !norm.process() )
		{
			errorMessage = "Cannot normalize image: " + norm.getErrorMessage();
			return false;
		}
		
		return true;
	}
	
	protected Image<B> convert( final Image<A> input, final ImageFactory<B> processFactory, final Converter<A, B> converter )
	{
		final ImageConverter<A, B> imgConv = new ImageConverter<A, B>( image, processFactory, converter );
		imgConv.setNumThreads( getNumThreads() );
		
		if ( !imgConv.checkInput() || !imgConv.process() )
		{
			errorMessage = "Cannot convert image: " + imgConv.getErrorMessage();			
			return null;
		}
		else
		{
			return imgConv.getResult();
		}
	}
	
	/**
	 * Upsamples the image by a factor of 2. It is not very efficient yet!!
	 * 
	 * @param input - The input image
	 * @param processFactory - The upsampled image of size 2*size-1
	 * @param converter - How to convert between A and B
	 * 
	 * @return the upsampled image, linearly interpolated
	 */
	protected Image<B> upSample( final Image<A> input, final ImageFactory<B> processFactory, final Converter<A, B> converter )
	{
		final int numDimensions = input.getNumDimensions();
		final int dim[] = input.getDimensions();
		
		// we do a centered upsampling
		for ( int d = 0; d < numDimensions; ++d )
			dim[ d ] = dim[ d ] * 2 - 1;

		// create output image
		final Image<B> upSampled = processFactory.createImage( dim );
		
		// create cursors and temp arrays
		final LocalizableCursor<A> inCursor = input.createLocalizableCursor();
		final LocalizableByDimCursor<B> outCursor = upSampled.createLocalizableByDimCursor();
		final int[] tmp = new int[ numDimensions ];
		
		while ( inCursor.hasNext() )
		{
			inCursor.fwd();
			inCursor.getPosition( tmp );

			for ( int d = 0; d < numDimensions; ++d )
				tmp[ d ] *= 2;
			
			outCursor.setPosition( tmp );
			converter.convert( inCursor.getType(), outCursor.getType() );
		}
		
		inCursor.close();
		
		// now interpolate the missing pixels, dimension by dimension
		final LocalizableCursor<B> outCursor2 = upSampled.createLocalizableCursor();
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			outCursor2.reset();
			
			while ( outCursor2.hasNext() )
			{
				outCursor2.fwd();
				
				final int pos = outCursor2.getPosition( d );
				
				// is it an empty spot?
				if ( pos % 2 == 1 )
				{
					outCursor.setPosition( outCursor2 );
					outCursor.bck( d );
					
					final double left = outCursor.getType().getRealDouble();

					outCursor.fwd( d );
					outCursor.fwd( d );

					final double right = outCursor.getType().getRealDouble();

					outCursor.bck( d );
					outCursor.getType().setReal( (right + left) / 2.0 );
				}
			}
		}
		
		outCursor.close();
		outCursor2.close();
		
		return upSampled;
	}
	
	@Override
	public boolean checkInput()
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image == null )
		{
			errorMessage = "ScaleSpace: [Image<A> img] is null.";
			return false;
		}
		else if ( processFactory == null )
		{
			errorMessage = "ScaleSpace: [ImageFactory<B> processFactory] is null.";
			return false;
		}
		else
			return true;
	}
	
	@Override
	public String getErrorMessage() { return errorMessage; }

	@Override
	public long getProcessingTime() { return processingTime; }
	
	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }	

}
