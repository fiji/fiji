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

import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.planar.PlanarContainer;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursorFactory;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * Positionable for a {@link PlanarContainer PlanarContainers}
 * @param <T>
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class PlanarLocalizableByDimCursor< T extends Type< T > >
		extends PlanarLocalizableCursor< T >
		implements LocalizableByDimCursor< T >
{
	final protected int[] step, tmp, sliceSteps;
	
	int numNeighborhoodCursors = 0;

	public PlanarLocalizableByDimCursor( final PlanarContainer< T, ? > container, final Image< T > image, final T type )
	{
		super( container, image, type );
		
		final int[] dims = container.getDimensions();
		step = Array.createAllocationSteps( dims );
		sliceSteps = new int[ dims.length ];
		if ( dims.length > 2 )
		{
			sliceSteps[ 2 ] = 1;
			for ( int i = 3; i < dims.length; ++i )
			{
				final int j = i - 1;
				sliceSteps[ i ] = dims[ j ] * sliceSteps[ j ];
			}
		}
			
		
		tmp = new int[ numDimensions ];
	}	

	@Override
	public synchronized LocalNeighborhoodCursor< T > createLocalNeighborhoodCursor()
	{
		if ( numNeighborhoodCursors == 0 )
		{
			++numNeighborhoodCursors;
			return LocalNeighborhoodCursorFactory.createLocalNeighborhoodCursor( this );
		}
		else
		{
			System.err.println( getClass().getCanonicalName() + ".createLocalNeighborhoodCursor(): There is only one special cursor per cursor allowed." );
			return null;
		}
	}

	@Override
	public synchronized RegionOfInterestCursor< T > createRegionOfInterestCursor( final int[] offset, final int[] size )
	{
		if ( numNeighborhoodCursors == 0 )
		{
			++numNeighborhoodCursors;
			return new RegionOfInterestCursor< T >( this, offset, size );
		}
		else
		{
			System.err.println( getClass().getCanonicalName() + ".createRegionOfInterestCursor(): There is only one special cursor per cursor allowed." );
			return null;
		}
	}

	@Override
	public void fwd( final int dim )
	{
		position[ dim ]++;

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

	@Override
	public void move( final int steps, final int dim )
	{
		position[ dim ] += steps;	

		if ( dim > 1 )
		{
			sliceIndex += sliceSteps[ dim ] * steps;
			type.updateContainer( this );
		}
		else
		{
			type.incIndex( step[ dim ] * steps );
		}		
	}
	
	@Override
	public void moveRel( final int[] vector )
	{
		for ( int d = 0; d < numDimensions; ++d )
			move( vector[ d ], d );
	}

	@Override
	public void moveTo( final int[] pos )
	{		
		for ( int d = 0; d < numDimensions; ++d )
		{
			final int dist = pos[ d ] - getPosition( d );
			
			if ( dist != 0 )				
				move( dist, d );
		}
	}
	
	@Override
	public void moveTo( final Localizable localizable )
	{
		localizable.getPosition( tmp );
		moveTo( tmp );
	}

	@Override
	public void setPosition( final Localizable localizable )
	{
		localizable.getPosition( tmp );
		setPosition( tmp );
	}
	
	@Override
	public void bck( final int dim )
	{		
		position[ dim ]--;
		
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

	@Override
	public void setPosition( final int[] position )
	{
		type.updateIndex( container.getIndex( position ) );
		
		for ( int d = 0; d < numDimensions; d++ )
			this.position[ d ] = position[ d ];
		
		sliceIndex = 0;
		for ( int d = 2; d < numDimensions; ++d )
			sliceIndex += position[ d ] * sliceSteps[ d ];
		
		type.updateContainer( this );		
	}

	@Override
	public void setPosition( final int position, final int dim )
	{
		this.position[ dim ] = position;

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
