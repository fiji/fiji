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

package mpicbg.imglib.interpolation.lanczos;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.InterpolatorImpl;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class LanczosInterpolator<T extends RealType<T>> extends InterpolatorImpl<T>
{
	final protected static float piSquare = (float) ( Math.PI * Math.PI );
	
	final LocalizableByDimCursor<T> cursor;
	final RegionOfInterestCursor<T> roiCursor;
	final float alphaF;
	final int alpha, numDimensions;	
	final T interpolatedValue;	
	final int[] offset, size;
	
	final float minValue, maxValue;
	final boolean clipping;
	
	protected LanczosInterpolator( final Image<T> img, 
								   final InterpolatorFactory<T> interpolatorFactory, 
								   final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory,
								   final int alpha,
								   final boolean clipping )
	{
		super(img, interpolatorFactory, outOfBoundsStrategyFactory);
		
		this.alphaF = alpha;
		this.alpha = alpha;
		this.numDimensions = img.getNumDimensions();		
		this.offset = new int[ numDimensions ];
		this.size = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			size[ d ] = alpha * 2;

		this.cursor = img.createLocalizableByDimCursor( outOfBoundsStrategyFactory );
		this.roiCursor = new RegionOfInterestCursor<T>( cursor, offset, size );
		this.interpolatedValue = img.createType();
		
		this.clipping = clipping;
		this.minValue = (float)interpolatedValue.getMinValue();
		this.maxValue = (float)interpolatedValue.getMaxValue();
		
		moveTo( position );		
	}

	@Override
	public T getType() 
	{
		roiCursor.reset( offset, size );
		
		float convolved = 0;
		
		while ( roiCursor.hasNext() )
		{
			roiCursor.fwd();
			
			float v = 1;
			
			for ( int d = 0; d < numDimensions; ++d )
				v *= sinc( position[ d ] - cursor.getPosition( d ), alphaF );
			
			convolved += roiCursor.getType().getRealFloat() * v;
		}
		
		if ( clipping )
		{
			if ( convolved < minValue )
				convolved = minValue;
			else if ( convolved > maxValue )
				convolved = maxValue;
			
			interpolatedValue.setReal( convolved );
		}
		else
		{
			interpolatedValue.setReal( convolved );
		}
		
		return interpolatedValue; 
	}
	
	protected static final float sinc( final float x, final float a )
	{
		if ( x == 0 )
			return 1;
		else
			return (float) (( a * Math.sin( Math.PI * x ) * Math.sin( Math.PI * x / a ) ) / ( piSquare * x * x ));
	}
	
	protected static final int floor( final float value )
	{
		return value > 0 ? (int)value:(int)value-1;
	}

	@Override
	public void close() {}

	@Override
	public void moveTo( final float[] pos )
	{
		for ( int d = 0; d < numDimensions; ++d )
		{
			final float p = pos[ d ]; 
			position[ d ] = p;
			offset[ d ] = floor( p ) - alpha + 1;
		}
	}

	@Override
	public void moveRel( final float[] vector )
	{
		for ( int d = 0; d < numDimensions; ++d )
		{
			final float p = position[ d ] + vector[ d ]; 
			position[ d ] = p;
			offset[ d ] = floor( p ) - alpha + 1;
		}
	}
	
	@Override
	public void setPosition( final float[] pos )
	{
		for ( int d = 0; d < numDimensions; ++d )
		{
			final float p = pos[ d ]; 
			position[ d ] = p;
			offset[ d ] = floor( p ) - alpha + 1;
		}
	}
}
