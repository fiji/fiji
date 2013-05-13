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

package mpicbg.imglib.cursor.array;

import mpicbg.imglib.container.array.Array3D;
import mpicbg.imglib.cursor.LocalizableCursor3D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class Array3DLocalizableCursor<T extends Type<T>> extends ArrayLocalizableCursor<T> implements LocalizableCursor3D<T>
{
	protected int x = -1, y = 0, z = 0;
	final int widthMinus1, heightMinus1, depthMinus1, width, height, depth;
	final Array3D<T,?> container;
	
	public Array3DLocalizableCursor( final Array3D<T,?> container, final Image<T> image, final T type ) 
	{
		super( container, image, type );

		this.container = container;
		
		this.width = container.getWidth();
		this.height = container.getHeight();
		this.depth = container.getDepth();

		this.widthMinus1 = width - 1;
		this.heightMinus1 = height - 1;
		this.depthMinus1 = depth - 1;
	}
	
	@Override
	public void fwd()
	{ 
		//++type.i;
		type.incIndex();
		
		if ( x < widthMinus1 )
		{
			++x;
		}
		else if ( y < heightMinus1 )
		{
			x = 0;
			++y;
		}
		else
		{
			x = 0;
			y = 0;
			++z;
		}
	}

	@Override
	public void fwd( final long steps )
	{
		if ( steps <= 0 )
			return;
		
		type.incIndex( (int)steps );
		
		int i = type.getIndex();
		
		z = i / (width*height);		
		i -= z * width * height;
		
		y = i / width;
		i -= y * width;
		
		x = i;
	}
	
	@Override
	public int getX() { return x; }
	@Override
	public int getY() { return y; }
	@Override
	public int getZ() { return z; }

	@Override
	public void reset()
	{ 
		isClosed = false;
		x = -1;
		y = z = 0;
		type.updateIndex( -1 );
		type.updateContainer( this );
	}

	@Override
	public void getPosition( final int[] position )
	{
		position[ 0 ] = x;
		position[ 1 ] = y;
		position[ 2 ] = z;
	}

	@Override
	public Array3D<T,?> getStorageContainer(){ return container; }

	@Override
	public int[] getPosition(){ return new int[]{x, y, z}; }
	
	@Override
	public int getPosition( final int dim )
	{
		if ( dim == 0 )
			return x;
		else if ( dim == 1 )
			return y;
		else if ( dim == 2 )
			return z;

		System.err.println("Array3DLocalizableCursor.getPosition( int dim ): There is no dimension " + dim );
		return -1;
	}
	
	@Override
	public String getPositionAsString()
	{
		return "(" + x + ", " + y + ", " + z + ")";
	}	
}
