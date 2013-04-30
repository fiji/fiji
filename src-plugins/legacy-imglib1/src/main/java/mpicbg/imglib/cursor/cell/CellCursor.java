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

import mpicbg.imglib.container.cell.Cell;
import mpicbg.imglib.container.cell.CellContainer;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.CursorImpl;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class CellCursor<T extends Type<T>> extends CursorImpl<T> implements Cursor<T>
{
	final protected T type;
	
	/*
	 * Pointer to the CellContainer we are iterating on
	 */
	protected final CellContainer<T,?> container;
	
	/*
	 * The number of cells inside the image
	 */
	protected final int numCells;
	
	/*
	 * The index of the current cell
	 */
	protected int cell;
	
	/*
	 * The index of the last cell
	 */
	protected int lastCell;
	
	/*
	 * The index+1 of the last pixel in the cell 
	 */
	protected int cellMaxI;
	
	/*
	 * The instance of the current cell
	 */
	protected Cell<T,?> cellInstance;
	
	public CellCursor( final CellContainer<T,?> container, final Image<T> image, final T type )
	{
		super( container, image );
		
		this.type = type;
		this.container = container;
		this.numCells = container.getNumCells();
		this.lastCell = -1;
		
		reset();
	}
	
	protected void getCellData( final int cell )
	{
		if ( cell == lastCell )
			return;
		
		lastCell = cell;		
		cellInstance = container.getCell( cell );				
		cellMaxI = cellInstance.getNumPixels();	
		
		type.updateContainer( this );
	}
	
	public Cell<T,?> getCurrentCell() { return cellInstance; }
	
	@Override
	public T getType() { return type; }
	
	@Override
	public void reset()
	{
		type.updateIndex( -1 );
		cell = 0;
		getCellData(cell);
		isClosed = false;
	}
	
	
	@Override
	public void close() 
	{ 
		if (!isClosed)
		{
			lastCell = -1;
			isClosed = true;
		}
	}

	@Override
	public boolean hasNext()
	{			
		if ( cell < numCells - 1 )
			return true;
		else if ( type.getIndex() < cellMaxI - 1 )
			return true;
		else
			return false;
	}	
	
	@Override
	public void fwd()
	{
		if ( type.getIndex() < cellMaxI - 1 )
		{
			type.incIndex();
		}
		else //if (cell < numCells - 1)
		{
			cell++;
			type.updateIndex( 0 );			
			getCellData(cell);
		}
		/*
		else
		{			
			// we have to run out of the image so that the next hasNext() fails
			lastCell= -1;						
			type.i = cellMaxI;
			cells = numCells;
		}
		*/
	}	

	@Override
	public CellContainer<T,?> getStorageContainer(){ return container; }

	@Override
	public int getStorageIndex() { return cellInstance.getCellId(); }	

	@Override
	public String toString() { return type.toString(); }	
}
