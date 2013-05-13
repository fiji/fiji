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
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class LinearInterpolator2DRealType<T extends RealType<T>> extends LinearInterpolator<T> 
{
	final int tmpLocation[];

	protected LinearInterpolator2DRealType( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		super( img, interpolatorFactory, outOfBoundsStrategyFactory, false );
		tmpLocation = new int[ 2 ];				
		moveTo( position );		
	}
	
	@Override
	public T getType() { return tmp2; }
	
	@Override
	public void moveTo( final float[] position )
	{
		final float x = position[ 0 ];
		final float y = position[ 1 ];
		
		this.position[ 0 ] = x;
		this.position[ 1 ] = y;
		
		//   y3         y2  
		//     *-------*
		//     |    x  |
		//     |       |
		//     *-------*
		//   y0         y1

		// base offset (y0)
		final int baseX1 = x > 0 ? (int)x: (int)x-1;
		final int baseX2 = y > 0 ? (int)y: (int)y-1;

		// update iterator position
		tmpLocation[ 0 ] = baseX1;
		tmpLocation[ 1 ] = baseX2;
		
		cursor.moveTo( tmpLocation );

		// How to iterate the area
		//
		//   y3         y2 
		//     *<------*
		//          x  ^
		//             |
		//     *------>*
		//   y0         y1

		// weights
		final float t = x - baseX1;
		final float u = y - baseX2;

		final float t1 = 1 - t;
		final float u1 = 1 - u;

		final float y1 = cursor.getType().getRealFloat();

		cursor.fwd( 0 );
		final float y2 = cursor.getType().getRealFloat();

		cursor.fwd( 1 );
		final float y3 = cursor.getType().getRealFloat();

		cursor.bck( 0 );
		final float y4 = cursor.getType().getRealFloat();
		
		tmp2.setReal( y1*t1*u1 + y2*t*u1 + y3*t*u + y4*t1*u );
	}
	
	@Override
	public void setPosition( final float[] position )
	{
		final float x = position[ 0 ];
		final float y = position[ 1 ];
		
		this.position[ 0 ] = x;
		this.position[ 1 ] = y;
		
		//   y3         y2  
		//     *-------*
		//     |    x  |
		//     |       |
		//     *-------*
		//   y0         y1

		// base offset (y0)
		final int baseX1 = x > 0 ? (int)x: (int)x-1;
		final int baseX2 = y > 0 ? (int)y: (int)y-1;

		// update iterator position
		tmpLocation[ 0 ] = baseX1;
		tmpLocation[ 1 ] = baseX2;
		
		cursor.setPosition( tmpLocation );

		// How to iterate the area
		//
		//   y3         y2 
		//     *<------*
		//          x  ^
		//             |
		//     *------>*
		//   y0         y1

		// weights
		final float t = x - baseX1;
		final float u = y - baseX2;

		final float t1 = 1 - t;
		final float u1 = 1 - u;

		final float y1 = cursor.getType().getRealFloat();

		cursor.fwd( 0 );
		final float y2 = cursor.getType().getRealFloat();

		cursor.fwd( 1 );
		final float y3 = cursor.getType().getRealFloat();

		cursor.bck( 0 );
		final float y4 = cursor.getType().getRealFloat();
		
		tmp2.setReal( y1*t1*u1 + y2*t*u1 + y3*t*u + y4*t1*u );
	}	
	
}
