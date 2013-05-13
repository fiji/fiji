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
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class Array3DLocalizableByDimOutOfBoundsCursor<T extends Type<T>> extends ArrayLocalizableByDimOutOfBoundsCursor<T> implements LocalizableByDimCursor3D<T>
{
	protected int x = -1, y = 0, z = 0;
	final int widthMinus1, heightMinus1, depthMinus1;
	final int width, height, depth;
	final int stepY, stepZ;
	final Array3D<T,?> container;

	public Array3DLocalizableByDimOutOfBoundsCursor( final Array3D<T,?> container, final Image<T> image, final T type, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory ) 
	{
		super( container, image, type, outOfBoundsStrategyFactory );
		
		this.container = container;
		
		this.widthMinus1 = container.getWidth() - 1;
		this.heightMinus1 = container.getHeight() - 1;
		this.depthMinus1 = container.getDepth() - 1;

		this.width = container.getWidth();
		this.height = container.getHeight();
		this.depth = container.getDepth();
		
		this.stepY = container.getWidth();
		this.stepZ = container.getWidth() * container.getHeight();
		
		reset();
	}
	
	@Override
	public void fwd()
	{ 
		if ( !isOutOfBounds )
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
			else if ( z < depthMinus1 )
			{
				x = 0;
				y = 0;
				++z;
			}
			else
			{
				// if it did not return we moved out of image bounds
				isOutOfBounds = true;
				++x;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
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
		if ( outOfBoundsStrategy == null )
			return;
		
		isOutOfBounds = false;
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
		
		System.err.println("Array3DLocalizableByDimOutOfBoundsCursor.getPosition( int dim ): There is no dimension " + dim );
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
			System.err.println("Array3DLocalizableByDimOutOfBoundsCursor.fwd( int dim ): There is no dimension " + dim );
		
		/*
		position[ dim ]++;

		if ( isOutOfBounds )
		{
			// reenter the image?
			if ( position[ dim ] == 0 )
				setPosition( position );
			else // moved out of image bounds
				outOfBoundsStrategy.notifyOutOfBounds( type );
		}
		else
		{			
			if ( position[ dim ] < dimensions[ dim ] )
			{
				// moved within the image
				type.i += step[ dim ];
			}
			else
			{
				// left the image
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBounds( type );
			}
		}
		 */
	}

	@Override
	public void fwdX()
	{
		if ( isOutOfBounds )
		{
			if ( x == -1 )
			{
				// possibly moved back into the image, depending on the other dimensions
				setPositionX( 0 );
			}
			else // moved out of image bounds
			{
				++x;
				outOfBoundsStrategy.notifyOutOfBOundsFwd( 0 );
			}
		}
		else
		{
			if ( x < widthMinus1 )
			{
				// moved within the image
				type.incIndex();
				++x;
			}
			else
			{
				// left the image
				++x;
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
	}

	@Override
	public void fwdY()
	{
		if ( isOutOfBounds )
		{
			if ( y == -1 )
			{
				// possibly moved back into the image, depending on the other dimensions
				setPositionY( 0 );
			}
			else // moved moved out of image bounds
			{
				++y;
				outOfBoundsStrategy.notifyOutOfBOundsFwd( 1 );
			}
		}
		else
		{
			if ( y < heightMinus1 )
			{
				// moved within the image
				type.incIndex( stepY );
				++y;
			}
			else
			{
				// left the image
				++y;
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
	}
	
	@Override
	public void fwdZ()
	{
		if ( isOutOfBounds )
		{
			if ( z == -1 )
			{
				// possibly moved back into the image, depending on the other dimensions
				setPositionZ( 0 );
			}
			else // moved out of image bounds
			{
				++z;
				outOfBoundsStrategy.notifyOutOfBOundsFwd( 2 );
			}
		}
		else
		{
			if ( z < depthMinus1 )
			{
				// moved within the image
				type.incIndex( stepZ );
				++z;
			}
			else
			{
				// left the image
				++z;
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
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
			System.err.println("Array3DLocalizableByDimOutOfBoundsCursor.move( int dim ): There is no dimension " + dim );
		
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
		x += steps;
		
		if ( isOutOfBounds )
		{
			if ( x > -1 && x < width )
			{
				// possibly moved back into the image, depending on the other dimensions
				if ( y < 0 || y >= height || z < 0 || z >= depth )
				{
					outOfBoundsStrategy.notifyOutOfBOunds( steps, 0 );
				}
				else
				{
					type.updateIndex( container.getPos( x, y, z ) );
					
					// new location is inside the image			
					type.updateContainer( this );
					
					isOutOfBounds = false;					
				}
			}
			else // moved out of image bounds
			{
				outOfBoundsStrategy.notifyOutOfBOunds( steps, 0 );
			}
		}
		else
		{
			if ( x > -1 && x < width )
			{
				// moved within the image
				type.incIndex( steps );
			}
			else
			{
				// left the image
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
	}

	@Override
	public void moveY( final int steps )
	{
		y += steps;
		
		if ( isOutOfBounds )
		{
			if ( y > -1 && y < height)
			{
				// possibly moved back into the image, depending on the other dimensions
				if ( x < 0 || x >= width || z < 0 || z >= depth )
				{
					outOfBoundsStrategy.notifyOutOfBOunds( steps, 1 );
				}
				else
				{
					type.updateIndex( container.getPos( x, y, z ) );
					
					// new location is inside the image			
					type.updateContainer( this );
					
					isOutOfBounds = false;					
				}
			}
			else
			{
				outOfBoundsStrategy.notifyOutOfBOunds( steps, 1 );
			}
		}
		else
		{
			if (  y > -1 && y < height )
			{
				// moved within the image
				type.incIndex( steps * stepY );
			}
			else
			{
				// left the image
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
	}
	
	@Override
	public void moveZ( final int steps )
	{
		z += steps;
		
		if ( isOutOfBounds )
		{
			if ( z > -1 && z < depth )
			{
				// possibly moved back into the image, depending on the other dimensions
				if ( y < 0 || y >= height || x < 0 || x >= width )
				{
					outOfBoundsStrategy.notifyOutOfBOunds( steps, 2 );
				}
				else
				{
					type.updateIndex( container.getPos( x, y, z ) );
					
					// new location is inside the image			
					type.updateContainer( this );
					
					isOutOfBounds = false;					
				}
			}
			else
			{
				outOfBoundsStrategy.notifyOutOfBOunds( steps, 2 );
			}
		}
		else
		{
			if (  z > -1 && z < depth )
			{
				// moved within the image
				type.incIndex( steps * stepZ );
			}
			else
			{
				// left the image
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
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
		if ( isOutOfBounds )
		{
			if ( x == width )
			{
				// possibly moved back into the image, depending on the other dimensions
				setPositionX( widthMinus1 );
			}
			else // moved out of image bounds
			{
				--x;
				outOfBoundsStrategy.notifyOutOfBOundsBck( 0 );
			}
		}
		else
		{
			--x;
			
			if ( x > -1 )
			{
				// moved within the image
				type.decIndex();
			}
			else
			{
				// left the image
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
	}

	@Override
	public void bckY()
	{
		if ( isOutOfBounds )
		{
			if ( y == height )
			{
				// possibly moved back into the image, depending on the other dimensions
				setPositionY( heightMinus1 );
			}
			else // moved out of image bounds
			{
				--y;
				outOfBoundsStrategy.notifyOutOfBOundsBck( 1 );
			}
		}
		else
		{
			--y;
			
			if ( y > -1 )
			{
				// moved within the image
				type.decIndex( stepY );
			}
			else
			{
				// left the image
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
	}
	
	@Override
	public void bckZ()
	{
		if ( isOutOfBounds )
		{
			if ( z == depth )
			{
				// possibly moved back into the image, depending on the other dimensions
				setPositionZ( depthMinus1 );
			}
			else // moved out of image bounds
			{
				--z;
				outOfBoundsStrategy.notifyOutOfBOundsBck( 2 );
			}
		}
		else
		{
			--z;
			
			if ( z > -1 )
			{
				// moved within the image
				type.decIndex( stepZ );
			}
			else
			{
				// left the image
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );				
			}
		}
	}
	
	@Override
	public void setPosition( final int[] position ) { setPosition( position[0], position[1], position[2] );	}

	@Override
	public void setPosition( final int posX, final int posY, final int posZ )
	{
		this.x = posX;
		this.y = posY;
		this.z = posZ;

		if ( posX > -1 && posX < width &&
			 posY > -1 && posY < height &&
			 posZ > -1 && posZ < depth)
		{			
			type.updateIndex( container.getPos( x, y, z ) );
			
			// new location is inside the image			
			if ( isOutOfBounds ) // we reenter the image with this setPosition() call
				type.updateContainer( this );
			
			isOutOfBounds = false;
		}
		else
		{
			// new location is out of image bounds
			if ( isOutOfBounds ) // just moved out of image bounds
			{
				outOfBoundsStrategy.notifyOutOfBOunds(  );
			}
			else // we left the image with this setPosition() call
			{
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds(  );
			}
		}
	}
	
	@Override
	public void setPositionX( final int pos )
	{		
		// we are out of image bounds or in the initial starting position
		if ( isOutOfBounds || type.getIndex() == -1 )
		{
			// if just this dimensions moves inside does not necessarily mean that
			// the other ones do as well, so we have to do a full check here
			setPosition( pos, y, z );
		}
		else if ( pos > -1 && pos < width )
		{
			type.incIndex( pos - x );
			x = pos;
		}
		else
		{
			x = pos;

			// moved out of image bounds
			isOutOfBounds = true;
			outOfBoundsStrategy.initOutOfBOunds(  );
		}
	}

	@Override
	public void setPositionY( final int pos )
	{
		// we are out of image bounds or in the initial starting position
		if ( isOutOfBounds || type.getIndex() == -1 )
		{
			// if just this dimensions moves inside does not necessarily mean that
			// the other ones do as well, so we have to do a full check here
			setPosition( x, pos, z );
		}
		else if ( pos > -1 && pos < height )
		{
			type.incIndex( (pos - y)*stepY );
			y = pos;
		}
		else
		{
			y = pos;

			// moved out of image bounds
			isOutOfBounds = true;
			outOfBoundsStrategy.initOutOfBOunds(  );
		}
	}

	@Override
	public void setPositionZ( final int pos )
	{
		// we are out of image bounds or in the initial starting position
		if ( isOutOfBounds || type.getIndex() == -1 )
		{
			// if just this dimensions moves inside does not necessarily mean that
			// the other ones do as well, so we have to do a full check here
			setPosition( x, y, pos );
		}
		else if ( pos > -1 && pos < depth )
		{
			type.incIndex( (pos - z)*stepZ );
			z = pos;
		}
		else
		{
			z = pos;

			// moved out of image bounds
			isOutOfBounds = true;
			outOfBoundsStrategy.initOutOfBOunds(  );
		}
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
			System.err.println("Array3DLocalizableByDimOutOfBoundsCursor.setPosition( int dim ): There is no dimension " + dim );
	}
	
	@Override
	public String getPositionAsString()
	{
		return "(" + x + ", " + y + ", " + z + ")";
	}
	
}
