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

package mpicbg.imglib.algorithm.floydsteinberg;

import java.util.Random;

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.label.FakeType;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class FloydSteinbergDithering<T extends RealType<T>> implements OutputAlgorithm<BitType>, Benchmark
{
	Image<BitType> result;
	final Image<T> img;
	final Image<FloatType> errorDiffusionKernel;
	final int[] dim, tmp1, tmp2;
	final int numDimensions;
	final float ditheringThreshold, minValue, maxValue;
	long processingTime;
	
	String errorMessage = "";
	
	public FloydSteinbergDithering( final Image<T> img, final float ditheringThreshold )
	{
		this.img = img.clone();
		this.dim = img.getDimensions();
		this.tmp1 = img.createPositionArray();
		this.tmp2 = img.createPositionArray();

		this.errorDiffusionKernel = createErrorDiffusionKernel( img.getNumDimensions() );

		this.ditheringThreshold = ditheringThreshold;
		img.getDisplay().setMinMax();		
		this.minValue = (float)img.getDisplay().getMin();
		this.maxValue = (float)img.getDisplay().getMax();
		this.numDimensions = img.getNumDimensions();
	}

	public FloydSteinbergDithering( final Image<T> img )
	{
		this ( img, getThreshold( img ) );	
	}	

	@Override
	public boolean process()
	{		
		final long startTime = System.currentTimeMillis();

		// creates the output image of BitType using the same Storage Strategy as the input image 
		final ImageFactory<BitType> imgFactory = new ImageFactory<BitType>( new BitType(), img.getContainerFactory() );
		result = imgFactory.createImage( dim );
		
		// we create a Cursor that traverses (top -> bottom) and (left -> right) in n dimensions,
		// which is a Cursor on a normal Array, therefore we use a FakeArray which just gives us position
		// information without allocating memory
		final LocalizableCursor<FakeType> cursor = ArrayLocalizableCursor.createLinearCursor( dim );

		// we also need a Cursors for the input, the output and the kernel image
		final LocalizableByDimCursor<T> cursorInput = img.createLocalizableByDimCursor( new OutOfBoundsStrategyValueFactory<T>() );
		final LocalizableByDimCursor<BitType> cursorOutput = result.createLocalizableByDimCursor();
		final LocalizableCursor<FloatType> cursorKernel = errorDiffusionKernel.createLocalizableCursor();
		
		while( cursor.hasNext() )
		{
			cursor.fwd();
			
			// move input and output cursor to the current location
			cursorInput.moveTo( cursor );
			cursorOutput.moveTo( cursor );
			
			// set new value and compute error
			final float error;
			final float in = cursorInput.getType().getRealFloat(); 
			if ( in < ditheringThreshold )
			{
				cursorOutput.getType().setZero();
				error = in - minValue; 
			}
			else
			{
				cursorOutput.getType().setOne();
				error = in - maxValue; 
			}
			
			if ( error != 0.0f )
			{
				// distribute the error
				cursorKernel.reset();
				cursorKernel.fwd( errorDiffusionKernel.getNumPixels()/2 );
				cursor.getPosition( tmp1 );			
				
				while ( cursorKernel.hasNext() )
				{
					cursorKernel.fwd();				
					
					final float value = error * cursorKernel.getType().get();
					cursorKernel.getPosition( tmp2 );
					
					for ( int d = 0; d < numDimensions; ++d )
						tmp2[ d ] += tmp1[ d ] - 1;
					
					cursorInput.moveTo( tmp2 );
					cursorInput.getType().setReal( cursorInput.getType().getRealFloat() + value );
				}
			}		
		}
		
		// close all cursors
		cursor.close();
		cursorInput.close();
		cursorOutput.close();
		cursorKernel.close();

		// close image
		img.close();
		
		processingTime = System.currentTimeMillis() - startTime;
		
		// successfully computed the dithering
		return true;
	}
	
	@Override
	public long getProcessingTime() { return processingTime; }

	@Override
	public Image<BitType> getResult() { return result; }

	@Override
	public boolean checkInput() { return true; }
	
	public static <T extends RealType<T>> float getThreshold( final Image<T> img )
	{
		img.getDisplay().setMinMax();
		return (float) (img.getDisplay().getMax() - img.getDisplay().getMin()) / 2.0f ;
	}

	@Override
	public String getErrorMessage() { return errorMessage; }

	public Image<FloatType> createErrorDiffusionKernel( final int numDimensions )
	{
		ImageFactory<FloatType> factory = new ImageFactory<FloatType>( new FloatType(), new ArrayContainerFactory() );
		
		// for 2d we take the values from the literature
		if ( numDimensions == 2 )
		{
			final Image<FloatType> kernel = factory.createImage( new int[] { 3, 3 } );
			
			final LocalizableByDimCursor<FloatType> cursor = kernel.createLocalizableByDimCursor();
			
			// For the 2d-case as well:
			// |-  -  -|
			// |-  #  7|
			// |3  5  1|
			//( - means processed already, # means the one we are currently processing)			
			cursor.setPosition( 2, 0 );
			cursor.setPosition( 1, 1 );			
			cursor.getType().setReal( 7.0f / 16.0f );
			
			cursor.move( 1, 1 );
			cursor.getType().setReal( 1.0f / 16.0f );

			cursor.move( -1, 0 );
			cursor.getType().setReal( 5.0f / 16.0f );

			cursor.move( -1, 0 );
			cursor.getType().setReal( 3.0f / 16.0f );

			cursor.close();
			
			return kernel;			
		}
		else
		{
			final Image<FloatType> kernel = factory.createImage( Util.getArrayFromValue( 3, numDimensions) );				
			final LocalizableCursor<FloatType> cursor = kernel.createLocalizableCursor();
			
			final int numValues = kernel.getNumPixels() / 2;
			final float[] rndValues = new float[ numValues ];
			float sum = 0;
			Random rnd = new Random( 435345 );
			
			for ( int i = 0; i < numValues; ++i )
			{
				rndValues[ i ] = rnd.nextFloat();
				sum += rndValues[ i ];
			}

			for ( int i = 0; i < numValues; ++i )
				rndValues[ i ] /= sum;

			int count = 0;
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				
				if ( count > numValues )
					cursor.getType().setReal( rndValues[ count - numValues - 1 ] );				
				
				++count;
			}
			
			//
			// Optimize
			//
			for ( int i = 0; i < 100; ++i )
			for ( int d = 0; d < numDimensions; ++d )
			{
				cursor.reset();
				
				float sumD = 0;
				
				while ( cursor.hasNext() )
				{
					cursor.fwd();
					if ( cursor.getPosition( d ) != 1 )
						sumD += cursor.getType().get(); 				
				}
				
				cursor.reset();
				while ( cursor.hasNext() )
				{
					cursor.fwd();

					if ( cursor.getPosition( d ) != 1 )
						cursor.getType().set( cursor.getType().get() / sumD );
				}
			}
			cursor.close();

			sum = 0;
			
			cursor.reset();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				sum += cursor.getType().get();
			}

			cursor.reset();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.getType().set( cursor.getType().get() / sum );
			}
			return kernel;			
		}
	}
}
