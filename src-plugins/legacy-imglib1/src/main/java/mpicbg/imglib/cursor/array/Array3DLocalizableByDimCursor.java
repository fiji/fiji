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
import mpicbg.imglib.cursor.LocalizableByDimCursor3D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class Array3DLocalizableByDimCursor<T extends Type<T>> extends ArrayLocalizableByDimCursor<T> implements LocalizableByDimCursor3D<T>
{
	protected int x = -1, y = 0, z = 0;
	final int widthMinus1, heightMinus1, depthMinus1, width, height, depth;
	final int stepY, stepZ;
	final Array3D<T,?> container;

	public Array3DLocalizableByDimCursor( final Array3D<T,?> container, final Image<T> image, final T type ) 
	{
		super( container, image, type );
		
		this.container = container;
		
		this.width = container.getWidth();
		this.height = container.getHeight();
		this.depth = container.getDepth();

		this.widthMinus1 = width - 1;
		this.heightMinus1 = height - 1;
		this.depthMinus1 = depth - 1;
		
		this.stepY = container.getWidth();
		this.stepZ = container.getWidth() * container.getHeight();
		
		reset();
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
	public void getPosition( int[] position )
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
		
		System.err.println("Array3DLocalizableByDimCursor.getPosition( int dim ): There is no dimension " + dim );
		return -1;
	}

	@Override
	public void fwd( final int dim )
	{
		if ( dim == 0 )
			fwdX();
		else if ( dim == 1 )
			fwdY();
		else if ( dim == 2 )
			fwdZ();
		else
			System.err.println("Array3DLocalizableByDimCursor.fwd( int dim ): There is no dimension " + dim );
	}

	@Override
	public void fwdX()
	{
		type.incIndex();
		++x;		
	}

	@Override
	public void fwdY()
	{
		type.incIndex( stepY );
		++y;
	}
	
	@Override
	public void fwdZ()
	{
		type.incIndex( stepZ );
		++z;		
	}

	@Override
	public void move( final int steps, final int dim )
	{
		if ( dim == 0 )
			moveX( steps );
		else if ( dim == 1 )
			moveY( steps );
		else if ( dim == 2 )
			moveZ( steps );
		else
			System.err.println("Array3DLocalizableByDimCursor.move( int dim ): There is no dimension " + dim );		
	}

	@Override
	public void moveRel( final int x, final int y, final int z )
	{
		moveX( x );
		moveY( y );
		moveZ( z );
	}

	@Override
	public void moveTo( final int x, final int y, final int z )
	{		
		moveX( x - this.x );
		moveY( y - this.y );
		moveZ( z - this.z );
	}
	
	@Override
	public void moveX( final int steps )
	{
		type.incIndex( steps );
		x += steps;		
	}

	@Override
	public void moveY( final int steps )
	{
		type.incIndex( steps * stepY );
		y += steps;		
	}
	
	@Override
	public void moveZ( final int steps )
	{
		type.incIndex( steps * stepZ );
		z += steps;		
	}

	@Override
	public void bck( final int dim )
	{
		if ( dim == 0 )
			bckX();
		else if ( dim == 1 )
			bckY();
		else if ( dim == 2 )
			bckZ();
		else
			System.err.println("Array3DLocalizableByDimCursor.bck( int dim ): There is no dimension " + dim );
	}

	@Override
	public void bckX()
	{
		type.decIndex();
		--x;		
	}

	@Override
	public void bckY()
	{
		type.decIndex( stepY );
		--y;
	}
	
	@Override
	public void bckZ()
	{
		type.decIndex( stepZ );
		--z;		
	}
	
	@Override
	public void setPosition( final int[] position ) { setPosition( position[0], position[1], position[2] );	}

	@Override
	public void setPosition( final int posX, final int posY, final int posZ )
	{
		this.x = posX;
		this.y = posY;
		this.z = posZ;
		
		type.updateIndex( container.getPos( x, y, z ) );				
	}
	
	@Override
	public void setPositionX( final int pos )
	{
		type.incIndex( pos - x );
		x = pos;
	}

	@Override
	public void setPositionY( final int pos )
	{
		type.incIndex( (pos - y)*stepY );
		y = pos;		
	}

	@Override
	public void setPositionZ( final int pos )
	{
		type.incIndex( (pos - z)*stepZ );
		z = pos;		
	}

	@Override
	public void setPosition( final int position, final int dim )
	{
		if ( dim == 0 )
			setPositionX( position );
		else if ( dim == 1 )
			setPositionY( position );
		else if ( dim == 2 )
			setPositionZ( position );
		else
			System.err.println("Array3DLocalizableByDimCursor.setPosition( int dim ): There is no dimension " + dim );
	}
	
	@Override
	public String getPositionAsString()
	{
		return "(" + x + ", " + y + ", " + z + ")";
	}	
}
