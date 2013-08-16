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

package mpicbg.imglib.cursor.shapelist;

import mpicbg.imglib.container.shapelist.ShapeList;
import mpicbg.imglib.cursor.CursorImpl;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursorFactory;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * 
 * @param <T>
 *
 * @version 0.1a
 *
 * @author Stephan Saalfeld
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class ShapeListLocalizableByDimCursor< T extends Type< T > > extends CursorImpl< T > implements LocalizableByDimCursor< T >
{
	private int numNeighborhoodCursors = 0;
	 
	final protected ShapeList< T > container;
	
	final protected int numDimensions;
	final protected int[] position, dimensions;
	
	public ShapeListLocalizableByDimCursor( final ShapeList< T > container, final Image< T > image ) 
	{
		super( container, image );
		this.container = container;
		numDimensions = container.getNumDimensions(); 
		
		position = new int[ numDimensions ];
		dimensions = container.getDimensions();
		
		position[ 0 ] = -1;
	}
	
	@Override
	public T getType()
	{
		return container.getShapeType( position );
	}
	
	@Override
	public synchronized LocalNeighborhoodCursor<T> createLocalNeighborhoodCursor()
	{
		if ( numNeighborhoodCursors == 0 )
		{
			++numNeighborhoodCursors;
			return LocalNeighborhoodCursorFactory.createLocalNeighborhoodCursor( this );
		}
		else
		{
			System.out.println( "ShapeListLocalizableByDimCursor.createLocalNeighborhoodCursor(): There is only one one special cursor per cursor allowed." );
			return null;
		}
	}

	@Override
	public synchronized RegionOfInterestCursor<T> createRegionOfInterestCursor( final int[] offset, final int[] size )
	{
		if ( numNeighborhoodCursors == 0 )
		{
			++numNeighborhoodCursors;
			return new RegionOfInterestCursor<T>( this, offset, size );
		}
		else
		{
			System.out.println( "ShapeListLocalizableByDimCursor.createRegionOfInterestCursor(): There is only one special cursor per cursor allowed." );
			return null;
		}
	}
	
	@Override
	public void fwd( final int dim )
	{
		++position[ dim ];	
		//link.fwd(dim);
	}

	@Override
	public void move( final int steps, final int dim )
	{
		position[ dim ] += steps;	
		//link.move(steps, dim);
	}
	
	@Override
	public void bck( final int dim )
	{
		--position[ dim ];
		//link.bck(dim);
	}
		
	@Override
	public void moveRel( final int[] vector )
	{
		for ( int d = 0; d < numDimensions; ++d )
			move( vector[ d ], d );
	}

	@Override
	public void moveTo( final int[] position )
	{		
		for ( int d = 0; d < numDimensions; ++d )
		{
			final int dist = position[ d ] - getPosition( d );
			
			if ( dist != 0 )				
				move( dist, d );
		}
	}

	@Override
	public void moveTo( final Localizable localizable )
	{
		localizable.getPosition( position );
	}

	@Override
	public void setPosition( final Localizable localizable )
	{
		localizable.getPosition( position );
	}
	
	@Override
	public void setPosition( final int[] position )
	{
		for ( int d = 0; d < numDimensions; ++d )
			this.position[ d ] = position[ d ];
		
		//link.setPosition( position );
	}

	@Override
	public void setPosition( final int position, final int dim )
	{
		this.position[ dim ] = position;
		//link.setPosition( position, dim );
	}

	@Override
	public void close(){}

	@Override
	public int getStorageIndex(){ return 0; }

	@Override
	public void reset()
	{
		position[ 0 ] = -1;
		
		for ( int d = 1; d < numDimensions; ++d )
			position[ d ] = 0; 
	}

	/**
	 * Assumes that position is not out of bounds.
	 * 
	 * TODO Not the most efficient way to calculate this on demand.  Better: count an index while moving...
	 */
	@Override
	public boolean hasNext()
	{
		for ( int d = numDimensions - 1; d >= 0; --d )
		{
			final int sizeD = dimensions[ d ] - 1;
			if ( position[ d ] < sizeD )
				return true;
			else if ( position[ d ] > sizeD )
				return false;
		}
		return false;
	}

	@Override
	public void fwd()
	{
		for ( int d = 0; d < numDimensions; ++d )
		{
			if ( ++position[ d ] >= dimensions[ d ] )
				position[ d ] = 0;
			else
				break;
		}
	}

	@Override
	public void getPosition( final int[] position )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = this.position[ d ];
	}

	@Override
	public int[] getPosition()
	{
		return position.clone();
	}

	@Override
	public int getPosition( final int dim )
	{
		return position[ dim ];
	}

	@Override
	public String getPositionAsString()
	{
		String pos = "(" + position[ 0 ];
		
		for ( int d = 1; d < numDimensions; d++ )
			pos += ", " + position[ d ];
		
		pos += ")";
		
		return pos;
	}
}
