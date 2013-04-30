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

package mpicbg.imglib.cursor.cell;

import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.cell.CellContainer;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableByDimCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursorFactory;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.label.FakeType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class CellLocalizableByDimCursor<T extends Type<T>> extends CellLocalizableCursor<T> implements LocalizableByDimCursor<T>
{
	/**
	 * Here we "misuse" a ArrayLocalizableCursor to iterate over cells,
	 * it always gives us the location of the current cell we are instantiating 
	 */
	final ArrayLocalizableByDimCursor<FakeType> cursor;
	
	/*
	protected final CellContainer<?,?> img;
	
	protected final int numCells;
	protected int cell, lastCell, cellMaxI;
	protected int[] cellSize;
	protected final int[] dim;
	protected CellElement<?,?> cellInstance;
	*/

	/* Inherited from CellLocalizableCursor<T>
	final protected int numDimensions;
	final protected int[] position;
	final protected int[] dimensions;
	final protected int[] cellDimensions;
	final protected int[] cellOffset;
	*/
	
	/*
	 * The number of cells in the image
	 */
	final protected int numCells;
	
	/*
	 * The number of cells in each dimension
	 */
	final protected int[] numCellsDim;
	
	/*
	 * The location of the current cell in the "cell space"
	 */
	final protected int[] cellPosition;
	
	/*
	 * Coordinates where the current cell ends
	 */
	final protected int[] cellEnd;
	
	/*
	 * Increments for each dimension when iterating through pixels
	 */
	final protected int[] step;
	
	/*
	 * Increments for each dimension when changing cells
	 */
	final protected int[] cellStep;
	
	int numNeighborhoodCursors = 0;
	
	final int[] tmp;
	
	public CellLocalizableByDimCursor( final CellContainer<T,?> container, final Image<T> image, final T type )
	{
		super( container, image, type);
		
		this.numCells = container.getNumCells();
		this.numCellsDim = container.getNumCellsDim();
		
		this.cellPosition = new int[ numDimensions ];
		this.cellEnd = new int[ numDimensions ];
		this.step = new int[ numDimensions ];
		this.cellStep = new int[ numDimensions ];
		this.tmp = new int[ numDimensions ];
		
		this.cursor = ArrayLocalizableByDimCursor.createLinearByDimCursor( numCellsDim );
		cursor.setPosition( new int[ container.getNumDimensions() ] );
		
		// the steps when moving from cell to cell
		Array.createAllocationSteps( numCellsDim, cellStep );
		
		reset();
	}
	
	protected void getCellData( final int cell )
	{
		if ( cell == lastCell )
			return;
		
		lastCell = cell;		
		cellInstance = container.getCell( cell );		

		cellMaxI = cellInstance.getNumPixels();	
		cellInstance.getDimensions( cellDimensions );
		cellInstance.getOffset( cellOffset );
		
		for ( int d = 0; d < numDimensions; d++ )
			cellEnd[ d ] = cellOffset[ d ] + cellDimensions[ d ];
		
		// the steps when moving inside a cell
		cellInstance.getSteps( step );
		//Array.createAllocationSteps( cellDimensions, step );
		
		// the steps when moving from cell to cell
		// Array.createAllocationSteps( numCellsDim, cellStep );
		
		type.updateContainer( this );
	}
	
	@Override
	public synchronized LocalNeighborhoodCursor<T> createLocalNeighborhoodCursor()
	{
		if ( numNeighborhoodCursors == 0)
		{
			++numNeighborhoodCursors;
			return LocalNeighborhoodCursorFactory.createLocalNeighborhoodCursor( this );
		}
		else
		{
			System.out.println("CellLocalizableByDimCursor.createLocalNeighborhoodCursor(): There is only one special cursor per cursor allowed.");
			return null;
		}
	}

	@Override
	public synchronized RegionOfInterestCursor<T> createRegionOfInterestCursor( final int[] offset, final int[] size )
	{
		if ( numNeighborhoodCursors == 0)
		{
			++numNeighborhoodCursors;
			return new RegionOfInterestCursor<T>( this, offset, size );
		}
		else
		{
			System.out.println("CellLocalizableByDimCursor.createRegionOfInterestCursor(): There is only one special cursor per cursor allowed.");
			return null;
		}
	}

	@Override
	public void fwd()
	{
		if ( type.getIndex() < cellMaxI - 1 )
		{
			type.incIndex();
			
			for ( int d = 0; d < numDimensions; d++ )
			{
				if ( position[ d ] < cellDimensions[ d ] + cellOffset[ d ] - 1 )
				{
					position[ d ]++;
					
					for ( int e = 0; e < d; e++ )
						position[ e ] = cellOffset[ e ];
					
					break;
				}
			}
			
		}
		else if (cell < numCells - 1)
		{
			cell++;
			type.updateIndex( 0 );			
			getCellData(cell);
			for ( int d = 0; d < numDimensions; d++ )
				position[ d ] = cellOffset[ d ];
			
			// the cell position in "cell space" from the image coordinates 
			container.getCellPosition( position, cellPosition );			
			cursor.setPosition( cellPosition );
		}
		else
		{			
			// we have to run out of the image so that the next hasNext() fails
			lastCell = -1;						
			type.updateIndex( cellMaxI );
			cell = numCells;
		}
	}	
	
	@Override
	public void reset()
	{
		if ( cellEnd == null )
			return;
		
		type.updateIndex( -1 );
		cell = 0;
		getCellData( cell );
		isClosed = false;
		
		position[ 0 ] = -1;
		cellPosition[ 0 ] = 0;
	
		for ( int d = 1; d < numDimensions; d++ )
		{
			position[ d ] = 0;
			cellPosition[ d ] = 0;
		}

		cursor.setPosition( cellPosition );
		
		type.updateContainer( this );
	}
	
	@Override
	public void fwd( final int dim )
	{
		if ( position[ dim ] + 1 < cellEnd[ dim ])
		{
			// still inside the cell
			type.incIndex( step[ dim ] );
			position[ dim ]++;	
		}
		else
		{	
			cursor.fwd( dim );
			if ( cellPosition[ dim ] < numCellsDim[ dim ] - 2 )
			{
				// next cell in dim direction is not the last one
				cellPosition[ dim ]++;
				cell += cellStep[ dim ];
				
				// we can directly compute the array index i in the next cell
				type.decIndex( ( position[ dim ] - cellOffset[ dim ] ) * step[ dim ] );
				getCellData(cell);
				
				position[ dim ]++;	
			} 
			else // if ( cellPosition[ dim ] == numCellsDim[ dim ] - 2) 
			{
				// next cell in dim direction is the last one, we cannot propagate array index i					
				cellPosition[ dim ]++;
				cell += cellStep[ dim ];

				getCellData(cell);					
				position[ dim ]++;	
				type.updateIndex( cellInstance.getPosGlobal( position ) );
			}
			// else moving out of image...			
		}
	}

	@Override
	public void move( final int steps, final int dim )
	{
		position[ dim ] += steps;	

		if ( position[ dim ] < cellEnd[ dim ] && position[ dim ] >= cellOffset[ dim ] )
		{
			// still inside the cell
			type.incIndex( step[ dim ] * steps );
		}
		else
		{
			setPosition( position[ dim ], dim );
		}
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
		if ( position[ dim ] - 1 >= cellOffset[ dim ])
		{
			// still inside the cell
			type.decIndex( step[ dim ] );
			position[ dim ]--;	
		}
		else
		{
			cursor.bck( dim );
			if ( cellPosition[ dim ] == numCellsDim[ dim ] - 1 && numCells != 1)
			{
				// current cell is the last one, so we cannot propagate the i
				cellPosition[ dim ]--;
				cell -= cellStep[ dim ];

				getCellData(cell);					
				
				position[ dim ]--;
				type.updateIndex( cellInstance.getPosGlobal( position ) );
			}
			else //if ( cellPosition[ dim ] > 0 )
			{
				// current cell in dim direction is not the last one
				cellPosition[ dim ]--;
				cell -= cellStep[ dim ];
				
				type.decIndex( ( position[ dim ] - cellOffset[ dim ]) * step[ dim ] );
				getCellData(cell);
				type.incIndex( ( cellDimensions[ dim ] - 1 ) * step[ dim ] );
				
				position[ dim ]--;	
			} 
			//else we are moving out of the image
		}				
	}
	

	@Override
	public void setPosition( final int[] position )
	{
		for ( int d = 0; d < numDimensions; d++ )
			this.position[ d ] = position[ d ];

		// the cell position in "cell space" from the image coordinates 
		container.getCellPosition( position, cellPosition );
		
		// get the cell index
		cell = container.getCellIndex( cursor, cellPosition );

		getCellData(cell);
		type.updateIndex( cellInstance.getPosGlobal( position ) );
	}

	@Override
	public void setPosition( final int position, final int dim )
	{
		this.position[ dim ] = position;

		// the cell position in "cell space" from the image coordinates 
		cellPosition[ dim ] = container.getCellPosition( position, dim );

		// get the cell index
		cell = container.getCellIndex( cursor, cellPosition[ dim ], dim );
		
		getCellData(cell);
		type.updateIndex( cellInstance.getPosGlobal( this.position ) );
	}	
	
	@Override
	public void close()
	{
		cursor.close();
		if (!isClosed)
		{
			lastCell = -1;
			isClosed = true;
		}		
	}
}
