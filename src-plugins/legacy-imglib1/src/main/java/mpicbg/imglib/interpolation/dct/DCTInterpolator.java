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
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package mpicbg.imglib.interpolation.dct;

import java.util.ArrayList;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.InterpolatorImpl;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.label.FakeType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class DCTInterpolator<T extends RealType<T>> extends InterpolatorImpl<T>
{
	final Image<FloatType> coefficients;
	final ArrayList<Image<FloatType>> inverseDCT;
	final int numDimensions;
	final T interpolatedValue;
	
	protected DCTInterpolator( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		super( img, interpolatorFactory, outOfBoundsStrategyFactory );

		interpolatedValue = img.createType();
		numDimensions =  img.getNumDimensions();
		
		// create coefficient image
		final ImageFactory<FloatType> imgFactory = new ImageFactory<FloatType>( new FloatType(), img.getContainerFactory() );
		coefficients = imgFactory.createImage( img.getDimensions() );
		
		// create the images necessary for the inverse DCT
		if ( numDimensions <= 2 )
		{
			inverseDCT = null;
		}
		else
		{
			// we need it for dimension y,z,....
			inverseDCT = new ArrayList<Image<FloatType>>();
			
			for ( int d = 1; d < numDimensions-1; ++d )
			{
				final int[] dimensions = new int[ d ];
				for ( int e = 0; e < d; ++e )
					dimensions[ e ] = img.getDimension( e );				
				
				inverseDCT.add( imgFactory.createImage( dimensions ) );
			}
			inverseDCT.add( coefficients );
		}
		
		// compute coefficients
		computeCoefficients();
		
		moveTo( position );		
	}
	
	/**
	 * A method that provides the DCT coefficients
	 * 
	 * @return - the {@link Image} of {@link FloatType} containing all coefficients
	 */
	public Image<FloatType> getCoefficients() { return coefficients; }
	
	protected void computeCoefficients()
	{
		final LocalizableByDimCursor<T> cursor = img.createLocalizableByDimCursor();
		final LocalizableByDimCursor<FloatType> cursorCoeff = coefficients.createLocalizableByDimCursor();
		
		if ( numDimensions > 1 )
		{
			/**
			 * Here we "misuse" a ArrayLocalizableCursor to iterate through all dimensions except the one we are computing the dct in 
			 */	
			final int[] fakeSize = new int[ numDimensions - 1 ];
			final int[] tmp = new int[ numDimensions ];
			
			for ( int d = 1; d < numDimensions; ++d )
				fakeSize[ d - 1 ] = img.getDimension( d );
			
			final ArrayLocalizableCursor<FakeType> cursorDim0 = ArrayLocalizableCursor.createLinearCursor( fakeSize );
			
			final int length0 = img.getDimension( 0 );
			final float[] tempIn0 = new float[ length0 ];
			final float[] tempOut0 = new float[ length0 ];
			
			// iterate over all dimensions except the one we are computing the dct in, which is dim=0 here
			while( cursorDim0.hasNext() )
			{
				cursorDim0.fwd();

				// get all dimensions except the one we are currently doing the dct on
				cursorDim0.getPosition( fakeSize );

				tmp[ 0 ] = 0;
				for ( int d = 1; d < numDimensions; ++d )
					tmp[ d ] = fakeSize[ d - 1 ];
				
				// set both cursors to the beginning of the correct line
				cursor.setPosition( tmp );
				cursorCoeff.setPosition( tmp );
				
				// fill the input array with image data
				for ( int x = 0; x < length0 - 1; ++x )
				{
					tempIn0[ x ] = cursor.getType().getRealFloat();									
					cursor.fwd( 0 );
				}
				tempIn0[ (length0-1) ] = cursor.getType().getRealFloat();

				// compute
				computeDCTCoefficients( tempIn0, tempOut0 );
				
				// write back
				for ( int x = 0; x < length0 - 1; ++x )
				{
					cursorCoeff.getType().setReal( tempOut0[ x ] );
					cursorCoeff.fwd( 0 );
				}
				cursorCoeff.getType().setReal( tempOut0[ (length0-1) ] );
			}
			
			/**
			 * now all the other dimensions
			 */	

			for ( int dim = 1; dim < numDimensions; ++dim )
			{
				// get all dimensions except the one we are currently doing the fft on
				int countDim = 0;						
				for ( int d = 0; d < numDimensions; ++d )
					if ( d != dim )
						fakeSize[ countDim++ ] = img.getDimension( d );
				
				final ArrayLocalizableCursor<FakeType> cursorDim = ArrayLocalizableCursor.createLinearCursor( fakeSize );

				final int length = img.getDimension( dim );
				final float[] tempIn = new float[ length ];
				final float[] tempOut = new float[ length ];
				
				// iterate over all dimensions except the one we are computing the dct in, which is dim=d here
				while( cursorDim.hasNext() )
				{
					cursorDim.fwd();
					
					// update all positions except for the one we are currrently doing the fft on
					cursorDim.getPosition( fakeSize );

					tmp[ dim ] = 0;								
					countDim = 0;						
					for ( int d = 0; d < numDimensions; ++d )
						if ( d != dim )
							tmp[ d ] = fakeSize[ countDim++ ];
					
					// update the cursor in the input image to the current dimension position
					cursorCoeff.setPosition( tmp );

					// fill the input array with image data
					for ( int i = 0; i < length - 1; ++i )
					{
						tempIn[ i ] = cursorCoeff.getType().getRealFloat();									
						cursorCoeff.fwd( dim );
					}
					tempIn[ (length-1) ] = cursorCoeff.getType().getRealFloat();

					// compute
					computeDCTCoefficients( tempIn, tempOut );

					// update the cursor in the input image to the current dimension position
					cursorCoeff.setPosition( tmp );

					// write back
					for ( int i = 0; i < length - 1; ++i )
					{
						cursorCoeff.getType().setReal( tempOut[ i ] );
						cursorCoeff.fwd( dim );
					}
					cursorCoeff.getType().setReal( tempOut[ (length-1) ] );					
				}				
			}			
		}
		else
		{
			final int length0 = img.getDimension( 0 );
			final float[] tempIn0 = new float[ length0 ];
			final float[] tempOut0 = new float[ length0 ];

			// set both cursors to the beginning
			cursor.setPosition( 0, 0 );
			cursorCoeff.setPosition( 0, 0 );
			
			// fill the input array with image data
			for ( int x = 0; x < length0 - 1; ++x )
			{
				tempIn0[ x ] = cursor.getType().getRealFloat();									
				cursor.fwd( 0 );
			}
			tempIn0[ (length0-1) ] = cursor.getType().getRealFloat();

			// compute
			computeDCTCoefficients( tempIn0, tempOut0 );
			
			// write back
			for ( int x = 0; x < length0 - 1; ++x )
			{
				cursorCoeff.getType().setReal( tempOut0[ x ] );
				cursor.fwd( 0 );
			}
			cursorCoeff.getType().setReal( tempOut0[ (length0-1) ] );			
		}
	}

	final private static void computeDCTCoefficients( final float[] in, final float[] out )
	{
		final int maxN = in.length;

		for (int k = 0; k < maxN; k++)
		{
			out[k] = 0;
			
			for (int x = 0; x < maxN; x++)
				out[k] += in[x] * Math.cos((Math.PI/maxN) * k * (x + 0.5));
			
			if (k == 0)
				out[k] *= (2.0 / Math.sqrt(2)) / maxN;
			else
				out[k] *= 2.0 / maxN;
		}
	}    

	final private static float inverseDCT( final float[] ck, final float x )
	{
		final int maxN = ck.length;
		float f = 0;
		
		f += (1.0/Math.sqrt(2)) * ck[0];
		
		for ( int k = 1; k < maxN; ++k )
			f += ck[k] * Math.cos((Math.PI/maxN) * k * (x + 0.5));
		
		return f;
	}
	
	@Override
	public T getType() 
	{
		// if is outside do something
		// else
		
		if ( numDimensions == 1 )
		{
			final LocalizableByDimCursor<FloatType> cursor = coefficients.createLocalizableByDimCursor();

			final int length = img.getDimension( 0 );
			final float[] temp = new float[ length ];

			// set both cursors to the beginning
			cursor.setPosition( 0, 0 );
			
			// fill the input array with image data
			for ( int x = 0; x < length - 1; ++x )
			{
				temp[ x ] = cursor.getType().getRealFloat();									
				cursor.fwd( 0 );
			}
			temp[ (length-1) ] = cursor.getType().getRealFloat();

			// compute iDCT
			interpolatedValue.setReal( inverseDCT( temp, position[ 0 ] ) );
		}
		else
		{
			final Image<FloatType> iDCT2d;
			
			if ( numDimensions > 2 )
			{				
				for ( int dim = numDimensions-1; dim >= 1; --dim )
				{
					final Image<FloatType> iDCTtarget = inverseDCT.get( dim - 2 );
					final LocalizableCursor<FloatType> cursorOut = iDCTtarget.createLocalizableCursor();
					
					final Image<FloatType> iDCTsource = inverseDCT.get( dim - 1 );
					final LocalizableByDimCursor<FloatType> cursorIn = iDCTsource.createLocalizableByDimCursor();
					
					final int numDimensionsTarget = iDCTtarget.getNumDimensions();
					final int[] tmp = new int[ iDCTsource.getNumDimensions() ];
					final int length = iDCTsource.getDimension( numDimensionsTarget );
					final float[] temp = new float[ length ];
					final float pos = position[ dim ]; 
					
					while ( cursorOut.hasNext() )
					{
						cursorOut.fwd();
						
						for ( int d = 0; d < numDimensionsTarget; ++d )
							tmp[ d ] = cursorOut.getPosition( d );
						tmp[ numDimensionsTarget ] = 0;
						
						// set cursor to the beginning
						cursorIn.setPosition( tmp );
						
						// fill the input array with image data
						for ( int i = 0; i < length - 1; ++i )
						{
							temp[ i ] = cursorIn.getType().getRealFloat();									
							cursorIn.fwd( dim );
						}
						temp[ (length-1) ] = cursorIn.getType().getRealFloat();
						
						cursorOut.getType().setReal( inverseDCT( temp, pos ) );						
					}
				}
				
				// the input for the final computation is the 2d image in the pyramid
				iDCT2d = inverseDCT.get( 0 );
			}
			else
			{
				iDCT2d = coefficients;
			}

			// compute the inverse DCT from the 2d image with the remaining coefficients
			final int length0 = iDCT2d.getDimension( 0 );
			final int length1 = iDCT2d.getDimension( 1 );
			final float[] temp1 = new float[ length1 ];
			final float[] idct1d = new float[ length0 ];
			final float positionY = position[ 1 ];
			
			final LocalizableByDimCursor<FloatType> cursor1 = iDCT2d.createLocalizableByDimCursor();
			
			for ( int x = 0; x < length0; ++x )
			{
				// set cursor to the beginning
				cursor1.setPosition( x, 0 );
				cursor1.setPosition( 0, 1 );
				
				// fill the input array with image data
				for ( int y = 0; y < length1 - 1; ++y )
				{
					temp1[ y ] = cursor1.getType().getRealFloat();									
					cursor1.fwd( 1 );
				}
				temp1[ (length1-1) ] = cursor1.getType().getRealFloat();
				
				idct1d[ x ] = inverseDCT( temp1, positionY );
			}
			
			// compute iDCT
			interpolatedValue.setReal( inverseDCT( idct1d, position[ 0 ] ) );		
		}
		
		return interpolatedValue; 
	}
	
	@Override
	public void close() {}

	@Override
	public void moveTo( final float[] pos )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = pos[ d ];
	}

	@Override
	public void moveRel( final float[] vector )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] += vector[ d ];
	}
	
	@Override
	public void setPosition( final float[] pos )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = pos[ d ];
	}
}
