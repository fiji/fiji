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

package mpicbg.imglib.interpolation.linear;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.NumericType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class LinearInterpolator1D<T extends NumericType<T>> extends LinearInterpolator<T> 
{
	final int[] tmpLocation;

	protected LinearInterpolator1D( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		super( img, interpolatorFactory, outOfBoundsStrategyFactory, false );

		tmpLocation = new int[ 1 ];
		setPosition( position );
	}
	
	@Override
	public T getType() { return tmp2; }
	
	@Override
	public void moveTo( final float[] position )
	{
		final float x = position[ 0 ];
		
		this.position[ 0 ] = x;
		
		//     *----x--*
		//   y0         y1

		// base offset (y0)
		final int baseX1 = x > 0 ? (int)x: (int)x-1;

		// update iterator position
		tmpLocation[ 0 ] = baseX1;
		
		cursor.moveTo( tmpLocation );

		// How to iterate the range
		//
		//     *----x->*
		//   y0         y1

		// weights
		final float t = x - baseX1;
		final float t1 = 1 - t;

		tmp1.set( cursor.getType() );
		tmp1.mul( t1 );
		tmp2.set( tmp1 );

		cursor.fwd( 0 );
		tmp1.set( cursor.getType() );
		tmp1.mul( t );
		tmp2.add( tmp1 );
	}
	
	@Override
	public void setPosition( final float[] position )
	{
		final float x = position[ 0 ];
		
		this.position[ 0 ] = x;
		
		//     *----x--*
		//   y0         y1

		// base offset (y0)
		final int baseX1 = x > 0 ? (int)x: (int)x-1;

		// update iterator position
		tmpLocation[ 0 ] = baseX1;
		
		cursor.setPosition( tmpLocation );

		// How to iterate the range
		//
		//     *----x->*
		//   y0         y1

		// weights
		final float t = x - baseX1;
		final float t1 = 1 - t;

		tmp1.set( cursor.getType() );
		tmp1.mul( t1 );
		tmp2.set( tmp1 );

		cursor.fwd( 0 );
		tmp1.set( cursor.getType() );
		tmp1.mul( t );
		tmp2.add( tmp1 );
	}	
	
}
