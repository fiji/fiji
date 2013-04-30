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

package mpicbg.imglib.cursor.planar;

import mpicbg.imglib.container.planar.PlanarContainer;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategy;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * Positionable with OutOfBoundsStrategy for a
 * {@link PlanarContainer PlanarContainers}
 * 
 * @param <T>
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class PlanarLocalizableByDimOutOfBoundsCursor<T extends Type<T>> extends PlanarLocalizableByDimCursor<T> implements LocalizableByDimCursor<T>
{
	final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory;
	final OutOfBoundsStrategy<T> outOfBoundsStrategy;
	
	boolean isOutOfBounds = false;
	
	public PlanarLocalizableByDimOutOfBoundsCursor( final PlanarContainer<T,?> container, final Image<T> image, final T type, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory ) 
	{
		super( container, image, type );
		
		this.outOfBoundsStrategyFactory = outOfBoundsStrategyFactory;
		this.outOfBoundsStrategy = outOfBoundsStrategyFactory.createStrategy( this );
		
		reset();
	}	
	
	@Override
	public boolean hasNext()
	{
		if ( !isOutOfBounds && ( type.getIndex() < lastIndex || sliceIndex < lastSliceIndex ) )
			return true;
		else
			return false;
	}

	@Override
	public T getType() 
	{ 
		if ( isOutOfBounds )
			return outOfBoundsStrategy.getType();
		else
			return type; 
	}
	
	@Override
	public void reset()
	{
		if ( outOfBoundsStrategy == null )
			return;
		
		isClosed = false;
		isOutOfBounds = false;
		type.updateIndex( -1 );
		
		position[ 0 ] = -1;
		sliceIndex = 0;
		
		for ( int d = 1; d < numDimensions; d++ )
			position[ d ] = 0;
		
		type.updateContainer( this );
	}
	
	@Override
	public void fwd()
	{
		if ( !isOutOfBounds )
		{
			type.incIndex();

			if ( type.getIndex() > lastIndex )
			{
				sliceIndex++;
				type.updateIndex( 0 );
				type.updateContainer( this );
			}
			
			int d;
			for ( d = 0; d < numDimensions; ++d )
			{
				if ( ++position[ d ] >= dimensions[ d ] )
					position[ d ] = 0;
				else
					break;
			}
			
			if ( d == numDimensions )
			{
				/* we moved out of image bounds */
				isOutOfBounds = true;
				position[0]++;
				outOfBoundsStrategy.initOutOfBOunds(  );
			}
		}
	}

	@Override
	public void fwd( final int dim )
	{
		++position[ dim ];

		if ( isOutOfBounds )
		{
			// reenter the image?
			if ( position[ dim ] == 0 )
				setPosition( position );
			else // moved out of image bounds
				outOfBoundsStrategy.notifyOutOfBOundsFwd( dim );
		}
		else
		{			
			if ( position[ dim ] < dimensions[ dim ] )
			{
				// moved within the image
				if ( dim > 1 )
				{
					sliceIndex += sliceSteps[ dim ];
					type.updateContainer( this );
				}
				else
				{
					type.incIndex( step[ dim ] );
				}
			}
			else
			{
				// left the image
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds();
			}
		}
	}

	@Override
	public void move( final int steps, final int dim )
	{
		position[ dim ] += steps;

		if ( isOutOfBounds )
		{
			// reenter the image?
			if ( position[ dim ] >= 0 && position[ dim ] < dimensions[ dim ] )
			{
				isOutOfBounds = false;
				
				for ( int d = 0; d < numDimensions && !isOutOfBounds; d++ )
					isOutOfBounds = position[ d ] < 0 || position[ d ] >= dimensions[ d ];
				
				if ( !isOutOfBounds )
				{
					// new location is inside the image
					
					// get the offset inside the image
					type.updateIndex( container.getIndex( position ) );
					sliceIndex = 0;
					for ( int d = 2; d < numDimensions; ++d )
						sliceIndex += position[ d ] * sliceSteps[ d ];
					
					type.updateContainer( this );			
				}
				else
				{
					outOfBoundsStrategy.notifyOutOfBOunds( steps, dim  );
				}
			}
			else // moved out of image bounds
			{
				outOfBoundsStrategy.notifyOutOfBOunds( steps, dim );
			}
		}
		else
		{			
			if ( position[ dim ] >= 0 && position[ dim ] < dimensions[ dim ] )
			{
				// moved within the image
				if ( dim > 1 )
				{
					sliceIndex += steps * sliceSteps[ dim ];
					type.updateContainer( this );
				}
				else
				{
					type.incIndex( step[ dim ] * steps );
				}
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
		position[ dim ]--;	

		if ( isOutOfBounds )
		{
			// reenter the image?
			if ( position[ dim ] == dimensions[ dim ] - 1 )
				setPosition( position );
			else // moved out of image bounds
				outOfBoundsStrategy.notifyOutOfBOundsBck( dim );
		}
		else
		{			
			if ( position[ dim ] > -1 )
			{
				// moved within the image
				if ( dim > 1 )
				{
					sliceIndex -= sliceSteps[ dim ];
					type.updateContainer( this );
				}
				else
				{
					type.decIndex( step[ dim ] );
				}
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
	public void setPosition( final int[] position )
	{
		// save current state
		final boolean wasOutOfBounds = isOutOfBounds;
		isOutOfBounds = false;
		
		// update positions and check if we are inside the image
		for ( int d = 0; d < numDimensions; d++ )
		{
			this.position[ d ] = position[ d ];
			
			if ( position[ d ] < 0 || position[ d ] >= dimensions[ d ])
				isOutOfBounds = true;
		}
		
		if ( isOutOfBounds )
		{
			// new location is out of image bounds
		
			if ( wasOutOfBounds ) // just moved out of image bounds
				outOfBoundsStrategy.notifyOutOfBOunds(  );
			else // we left the image with this setPosition() call
				outOfBoundsStrategy.initOutOfBOunds(  );
		}
		else
		{
			// new location is inside the image
						
			// get the offset inside the image
			type.updateIndex( container.getIndex( position ) );
			sliceIndex = 0;
			for ( int d = 2; d < numDimensions; ++d )
				sliceIndex += position[ d ] * sliceSteps[ d ];
			
			type.updateContainer( this );			
		}
	}

	@Override
	public void setPosition( final int position, final int dim )
	{
		this.position[ dim ] = position;

		// we areout of image bounds or in the initial starting position
		if ( isOutOfBounds || type.getIndex() == -1 )
		{
			// if just this dimensions moves inside does not necessarily mean that
			// the other ones do as well, so we have to do a full check here
			setPosition( this.position );
		}
		else if ( position < 0 || position >= dimensions[ dim ]) // we can just check in this dimension if it is still inside
		{
			// cursor has left the image
			isOutOfBounds = true;
			outOfBoundsStrategy.initOutOfBOunds(  );
			return;
		}
		else
		{
			// jumped around inside the image
			
			if ( dim > 1 )
			{
				sliceIndex = position * sliceSteps[ dim ];
				type.updateContainer( this );
			}
			else
			{
				type.updateIndex( container.getIndex( this.position ) );
			}
		}		
	}
}
