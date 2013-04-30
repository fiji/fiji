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

package mpicbg.imglib.container.cell;

import java.util.ArrayList;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.DirectAccessContainerImpl;
import mpicbg.imglib.container.basictypecontainer.array.ArrayDataAccess;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableByDimCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.cursor.cell.CellCursor;
import mpicbg.imglib.cursor.cell.CellLocalizableByDimCursor;
import mpicbg.imglib.cursor.cell.CellLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.cell.CellLocalizableCursor;
import mpicbg.imglib.cursor.cell.CellLocalizablePlaneCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.label.FakeType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class CellContainer<T extends Type<T>, A extends ArrayDataAccess<A>> extends DirectAccessContainerImpl<T, A>
{
	final protected ArrayList<Cell<T,A>> data;
	final protected int[] numCellsDim, cellSize;
	final protected int numCells;
	
	public CellContainer( final ContainerFactory factory, final A creator, final int[] dim, final int[] cellSize, final int entitiesPerPixel )
	{
		super(factory, dim, entitiesPerPixel);
		
		// check that cellsize is not bigger than the image
		for ( int d = 0; d < getNumDimensions(); d++ )
			if ( cellSize[ d ] > dim[ d ] )
				cellSize[ d ] = dim[ d ];
			
		this.cellSize = cellSize;
		numCellsDim = new int[ getNumDimensions() ];				
		
		int tmp = 1;		
		for ( int d = 0; d < getNumDimensions(); d++ )
		{
			numCellsDim[ d ] = ( dim[ d ] - 1) / cellSize[ d ] + 1;
			tmp *= numCellsDim[ d ];
		}
		numCells = tmp;
		
		data = createCellArray( numCells );
		
		// Here we "misuse" an ArrayLocalizableCursor to iterate over cells,
		// it always gives us the location of the current cell we are instantiating.
		final ArrayLocalizableCursor<FakeType> cursor = ArrayLocalizableCursor.createLinearCursor( numCellsDim ); 
		
		for ( int c = 0; c < numCells; c++ )			
		{
			cursor.fwd();
			final int[] finalSize = new int[ getNumDimensions() ];
			final int[] finalOffset = new int[ getNumDimensions() ];
			
			for ( int d = 0; d < getNumDimensions(); d++ )
			{
				finalSize[ d ] = cellSize[ d ];
				
				// the last cell in each dimension might have another size
				if ( cursor.getPosition( d ) == numCellsDim[ d ] - 1 )
					if ( dim[ d ] % cellSize[ d ] != 0 )
						finalSize[ d ] = dim[ d ] % cellSize[ d ];
				
				finalOffset[ d ] = cursor.getPosition( d ) * cellSize[ d ];
			}			

			data.add( createCellInstance( creator, c, finalSize, finalOffset, entitiesPerPixel ) );			
		}
		
		cursor.close();
	}
	
	@Override
	public A update( final Cursor<?> c ) { return data.get( c.getStorageIndex() ).getData(); }
	
	public ArrayList<Cell<T, A>> createCellArray( final int numCells ) { return new ArrayList<Cell<T, A>>( numCells ); }	
	
	public Cell<T, A> createCellInstance( final A creator, final int cellId, final int[] dim, final int offset[], final int entitiesPerPixel )
	{
		return new Cell<T,A>( creator, cellId, dim, offset, entitiesPerPixel );
	}

	public Cell<T, A> getCell( final int cellId ) { return data.get( cellId ); }
	public int getCellIndex( final ArrayLocalizableByDimCursor<FakeType> cursor, final int[] cellPos )
	{
		cursor.setPosition( cellPos );
		return cursor.getArrayIndex();
	}

	// many cursors using the same cursor for getting their position
	public int getCellIndex( final ArrayLocalizableByDimCursor<FakeType> cursor, final int cellPos, final int dim )
	{
		cursor.setPosition( cellPos, dim );		
		return cursor.getArrayIndex();
	}
	
	public int[] getCellPosition( final int[] position )
	{
		final int[] cellPos = new int[ position.length ];
		
		for ( int d = 0; d < numDimensions; d++ )
			cellPos[ d ] = position[ d ] / cellSize[ d ];
		
		return cellPos;
	}

	public void getCellPosition( final int[] position, final int[] cellPos )
	{
		for ( int d = 0; d < numDimensions; d++ )
			cellPos[ d ] = position[ d ] / cellSize[ d ];
	}

	public int getCellPosition( final int position, final int dim ) { return position / cellSize[ dim ]; }
	
	public int getCellIndexFromImageCoordinates( final ArrayLocalizableByDimCursor<FakeType> cursor, final int[] position )
	{		
		return getCellIndex( cursor, getCellPosition( position ) );
	}
	
	public int getNumCells( final int dim ) 
	{
		if ( dim < numDimensions )
			return numCellsDim[ dim ];
		else
			return 1;
	}
	public int getNumCells() { return numCells; }
	public int[] getNumCellsDim() { return numCellsDim.clone(); }

	public int getCellSize( final int dim ) { return cellSize[ dim ]; }
	public int[] getCellSize() { return cellSize.clone(); }

	@Override
	public void close()
	{
		for ( final Cell<T, A> e : data )
			e.close();
	}

	@Override
	public CellCursor<T> createCursor( final Image<T> image ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		CellCursor<T> c = new CellCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}
	
	@Override
	public CellLocalizableCursor<T> createLocalizableCursor( final Image<T> image ) 
	{
		// create a Cursor using a Type that is linked to the container
		CellLocalizableCursor<T> c = new CellLocalizableCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}	

	@Override
	public CellLocalizablePlaneCursor<T> createLocalizablePlaneCursor( final Image<T> image ) 
	{
		// create a Cursor using a Type that is linked to the container
		CellLocalizablePlaneCursor<T> c = new CellLocalizablePlaneCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}	
	
	@Override
	public CellLocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image ) 
	{
		// create a Cursor using a Type that is linked to the container
		CellLocalizableByDimCursor<T> c = new CellLocalizableByDimCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}	
	
	@Override
	public CellLocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		CellLocalizableByDimOutOfBoundsCursor<T> c = new CellLocalizableByDimOutOfBoundsCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer(), outOfBoundsFactory );
		return c;
	}

	@Override
	public boolean compareStorageContainerCompatibility( final Container<?> container )
	{
		if ( compareStorageContainerDimensions( container ))
		{			
			if ( getFactory().getClass().isInstance( container.getFactory() ))
			{
				final CellContainer<?,?> otherCellContainer = (CellContainer<?,?>)container;
				
				for ( int d = 0; d < numDimensions; ++d )
					if ( this.getCellSize( d ) != otherCellContainer.getCellSize( d ) )
						return false;
				
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}	
}
