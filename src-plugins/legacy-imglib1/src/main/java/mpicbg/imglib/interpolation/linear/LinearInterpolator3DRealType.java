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
public class LinearInterpolator3DRealType<T extends RealType<T>> extends LinearInterpolator<T> 
{
	final int tmpLocation[];

	protected LinearInterpolator3DRealType( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		super( img, interpolatorFactory, outOfBoundsStrategyFactory, false );
		
		tmpLocation = new int[ 3 ];				
		moveTo( position );		
	}
	
	@Override
	public T getType() { return tmp2; }
	
	@Override
	public void moveTo( final float[] position )
	{
		final float x = position[ 0 ];
		final float y = position[ 1 ];
		final float z = position[ 2 ];
		
		this.position[ 0 ] = x;
		this.position[ 1 ] = y;
		this.position[ 2 ] = z;
		
		//       y7     y6
		//        *-------*
		//       /       /|
		//   y3 /    y2 / |
		//     *-------*  * y5
		//     |    x  | /
		//     |       |/
		//     *-------*
		//     y0    y1

		// base offset (y0)
		final int baseX1 = x > 0 ? (int)x: (int)x-1;
		final int baseX2 = y > 0 ? (int)y: (int)y-1;
		final int baseX3 = z > 0 ? (int)z: (int)z-1;

		// update iterator position
		tmpLocation[ 0 ] = baseX1;
		tmpLocation[ 1 ] = baseX2;
		tmpLocation[ 2 ] = baseX3;
		
		cursor.moveTo( tmpLocation );

		// How to iterate the cube
		//
		//       y7     y6
		//        *------>*
		//       ^       /|
		//   y3 /    y2 / v
		//     *<------*  * y5
		//     |    x  ^ /
		//     |       |/
		//     *------>*
		//     y0    y1

        //final float y0 = strategy.get(baseX1    , baseX2,     baseX3);
        final float y0 = cursor.getType().getRealFloat(); 
        
        //final float y1 = strategy.get(baseX1 + 1, baseX2,     baseX3);
        cursor.fwd( 0 );
        final float y1 = cursor.getType().getRealFloat(); 
        
        //final float y2 = strategy.get(baseX1 + 1, baseX2 + 1, baseX3);
        cursor.fwd( 1 );
        final float y2 = cursor.getType().getRealFloat(); 
                
        //final float y3 = strategy.get(baseX1    , baseX2 + 1, baseX3);
        cursor.bck( 0 );
        final float y3 = cursor.getType().getRealFloat(); 
        
        //final float y7 = strategy.get(baseX1    , baseX2 + 1, baseX3 + 1);
        cursor.fwd( 2 );
        final float y7 = cursor.getType().getRealFloat();

        //final float y6 = strategy.get(baseX1 + 1, baseX2 + 1, baseX3 + 1);
        cursor.fwd( 0 );
        final float y6 = cursor.getType().getRealFloat();

        //final float y5 = strategy.get(baseX1 + 1, baseX2,     baseX3 + 1);
        cursor.bck( 1 );
        final float y5 = cursor.getType().getRealFloat();
        
        //final float y4 = strategy.get(baseX1    , baseX2,	  baseX3 + 1);
        cursor.bck( 0 );
        final float y4 = cursor.getType().getRealFloat();        

        // weights
        final float t = x - baseX1; 
        final float u = y - baseX2; 
        final float v = z - baseX3;

        final float t1 = 1 - t;
        final float u1 = 1 - u;
        final float v1 = 1 - v;

        final float value = t1*u1*v1*y0 + t*u1*v1*y1 + t*u*v1*y2 + t1*u*v1*y3 + 
                            t1*u1*v*y4  + t*u1*v*y5  + t*u*v*y6  + t1*u*v*y7;
        
        tmp2.setReal( value );
	}
	
	@Override
	public void setPosition( final float[] position )
	{
		final float x = position[ 0 ];
		final float y = position[ 1 ];
		final float z = position[ 2 ];
		
		this.position[ 0 ] = x;
		this.position[ 1 ] = y;
		this.position[ 2 ] = z;
		
		//       y7     y6
		//        *-------*
		//       /       /|
		//   y3 /    y2 / |
		//     *-------*  * y5
		//     |    x  | /
		//     |       |/
		//     *-------*
		//     y0    y1

		// base offset (y0)
		final int baseX1 = x > 0 ? (int)x: (int)x-1;
		final int baseX2 = y > 0 ? (int)y: (int)y-1;
		final int baseX3 = z > 0 ? (int)z: (int)z-1;

		// update iterator position
		tmpLocation[ 0 ] = baseX1;
		tmpLocation[ 1 ] = baseX2;
		tmpLocation[ 2 ] = baseX3;
		
		cursor.setPosition( tmpLocation );

		// How to iterate the cube
		//
		//       y7     y6
		//        *------>*
		//       ^       /|
		//   y3 /    y2 / v
		//     *<------*  * y5
		//     |    x  ^ /
		//     |       |/
		//     *------>*
		//     y0    y1

        //final float y0 = strategy.get(baseX1    , baseX2,     baseX3);
        final float y0 = cursor.getType().getRealFloat(); 
        
        //final float y1 = strategy.get(baseX1 + 1, baseX2,     baseX3);
        cursor.fwd( 0 );
        final float y1 = cursor.getType().getRealFloat(); 
        
        //final float y2 = strategy.get(baseX1 + 1, baseX2 + 1, baseX3);
        cursor.fwd( 1 );
        final float y2 = cursor.getType().getRealFloat(); 
                
        //final float y3 = strategy.get(baseX1    , baseX2 + 1, baseX3);
        cursor.bck( 0 );
        final float y3 = cursor.getType().getRealFloat(); 
        
        //final float y7 = strategy.get(baseX1    , baseX2 + 1, baseX3 + 1);
        cursor.fwd( 2 );
        final float y7 = cursor.getType().getRealFloat();

        //final float y6 = strategy.get(baseX1 + 1, baseX2 + 1, baseX3 + 1);
        cursor.fwd( 0 );
        final float y6 = cursor.getType().getRealFloat();

        //final float y5 = strategy.get(baseX1 + 1, baseX2,     baseX3 + 1);
        cursor.bck( 1 );
        final float y5 = cursor.getType().getRealFloat();
        
        //final float y4 = strategy.get(baseX1    , baseX2,	  baseX3 + 1);
        cursor.bck( 0 );
        final float y4 = cursor.getType().getRealFloat();        

        // weights
        final float t = x - baseX1; 
        final float u = y - baseX2; 
        final float v = z - baseX3;

        final float t1 = 1 - t;
        final float u1 = 1 - u;
        final float v1 = 1 - v;

        final float value = t1*u1*v1*y0 + t*u1*v1*y1 + t*u*v1*y2 + t1*u*v1*y3 + 
                            t1*u1*v*y4  + t*u1*v*y5  + t*u*v*y6  + t1*u*v*y7;
        
        tmp2.setReal( value );
	}	
	
}
