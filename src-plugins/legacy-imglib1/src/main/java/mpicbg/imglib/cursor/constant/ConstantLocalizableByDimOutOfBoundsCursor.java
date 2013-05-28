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

package mpicbg.imglib.cursor.constant;

import mpicbg.imglib.container.constant.ConstantContainer;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategy;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * A simple {@link LocalizableByDimCursorOutOfBoundsCursor} that always returns the same value at each position. Here I just used Saalfeld's
 * implementation from the {@link ShapeListLocalizableByDimCursorOutOfBoundsCursor}.
 * 
 * @param <T>
 *
 * @author Tobias Pietzsch
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ConstantLocalizableByDimOutOfBoundsCursor< T extends Type< T > > extends ConstantLocalizableByDimCursor< T >
{
	final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory;
	final OutOfBoundsStrategy<T> outOfBoundsStrategy;
	
	boolean isOutOfBounds = false;

	public ConstantLocalizableByDimOutOfBoundsCursor( final ConstantContainer<T> container, final Image<T> image, final T type, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		super( container, image, type );
		
		this.outOfBoundsStrategyFactory = outOfBoundsStrategyFactory;
		this.outOfBoundsStrategy = outOfBoundsStrategyFactory.createStrategy( this );
		
		reset();
	}
	
	/**
	 * TODO Not the most efficient way to calculate this on demand.  Better: count an index while moving...
	 */
	@Override
	public boolean hasNext()
	{
		if ( isOutOfBounds ) return false;
		
		for ( int d = numDimensions - 1; d >= 0; --d )
		{
			final int sizeD = dimensions[ d ] - 1;
			if ( position[ d ] < sizeD )
				return true;
		}
		return false;
	}

	@Override
	public void reset()
	{
		if ( outOfBoundsStrategy == null )
			return;
		
		isClosed = false;
		isOutOfBounds = false;
		
		super.reset();
	}
	
	@Override
	public T getType() 
	{
		if ( isOutOfBounds )
			return outOfBoundsStrategy.getType();
		else
			return super.getType(); 
	}
		
	@Override
	public void fwd()
	{
		if ( !isOutOfBounds )
		{
			for ( int d = 0; d < numDimensions; ++d )
			{
				if ( ++position[ d ] >= dimensions[ d ] )
					position[ d ] = 0;
				else
					return;
			}
			
			isOutOfBounds = true;
			++position[ 0 ];
			outOfBoundsStrategy.initOutOfBOunds();
			//link.fwd();
		}
	}

	@Override
	public void fwd( final int dim )
	{
		++position[ dim ];

		if ( isOutOfBounds )
		{
			if ( position[ dim ] == 0 )
				setPosition( position );
			else
				outOfBoundsStrategy.notifyOutOfBOundsFwd( dim );
		}
		else
		{			
			if ( position[ dim ] >= dimensions[ dim ] )
			{
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds();
			}
		}
		//link.fwd( dim );
	}

	@Override
	public void move( final int steps, final int dim )
	{
		position[ dim ] += steps;

		if ( isOutOfBounds )
		{
			if ( position[ dim ] >= 0 && position[ dim ] < dimensions[ dim ] )
			{
				isOutOfBounds = false;
				for ( int d = 0; d < numDimensions && !isOutOfBounds; ++d )
					isOutOfBounds = position[ d ] < 0 || position[ d ] >= dimensions[ d ];
				
				if ( isOutOfBounds )
					outOfBoundsStrategy.notifyOutOfBOunds( steps, dim  );
			}
			else
				outOfBoundsStrategy.notifyOutOfBOunds( steps, dim  );
		}
		else
		{			
			if ( position[ dim ] < 0 || position[ dim ] >= dimensions[ dim ] )
			{
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds();
			}
		}
		//link.move( steps, dim );
	}
	
	@Override
	public void bck( final int dim )
	{
		--position[ dim ];

		if ( isOutOfBounds )
		{
			if ( position[ dim ] < dimensions[ dim ] )
				setPosition( position );
			else
				outOfBoundsStrategy.notifyOutOfBOundsFwd( dim );
		}
		else
		{			
			if ( position[ dim ] == -1 )
			{
				isOutOfBounds = true;
				outOfBoundsStrategy.initOutOfBOunds();
			}
		}
		//link.bck( dim );
	}

	@Override
	public void setPosition( final int[] position )
	{
		final boolean wasOutOfBounds = isOutOfBounds;
		isOutOfBounds = false;
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			this.position[ d ] = position[ d ];
			if ( position[ d ] < 0 || position[ d ] >= dimensions[ d ])
				isOutOfBounds = true;
		}
		
		if ( isOutOfBounds )
		{
			if ( wasOutOfBounds )
				outOfBoundsStrategy.notifyOutOfBOunds();
			else
				outOfBoundsStrategy.initOutOfBOunds();
		}
		//link.setPosition( position );
	}

	@Override
	public void setPosition( final int position, final int dim )
	{
		this.position[ dim ] = position;

		if ( isOutOfBounds )
		{
			setPosition( this.position );
		}
		else if ( position < 0 || position >= dimensions[ dim ] )
		{
			isOutOfBounds = true;
			outOfBoundsStrategy.initOutOfBOunds();
			return;
		}
		//link.setPosition(position, dim);
	}
	
	@Override
	public void moveTo( final Localizable localizable )
	{
		moveTo( localizable.getPosition() );
	}

	@Override
	public void setPosition( final Localizable localizable )
	{
		setPosition( localizable.getPosition() );
	}

}
