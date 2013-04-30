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

import mpicbg.imglib.container.ContainerImpl;
import mpicbg.imglib.container.PixelGridContainerImpl;
import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.basictypecontainer.array.ArrayDataAccess;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class Cell< T extends Type<T>, A extends ArrayDataAccess<A>> //extends Array<T,A>
{
	final protected int[] offset, step, dim;	
	final protected int cellId, numDimensions, numPixels, numEntities;
	
	// the ArrayDataAccess containing the data
	final protected A data;
	
	public Cell( final A creator, final int cellId, final int[] dim, final int offset[], final int entitiesPerPixel)
	{
		this.offset = offset;		
		this.cellId = cellId;
		this.numDimensions = dim.length;
		this.dim = dim;
		this.numPixels = ContainerImpl.getNumPixels( dim );
		this.numEntities = PixelGridContainerImpl.getNumEntities( dim, entitiesPerPixel );
		
		step = new int[ numDimensions ];
		
		this.data = creator.createArray( numEntities );
		
		// the steps when moving inside a cell
		Array.createAllocationSteps( dim, step );		
	}
	
	protected A getData() { return data; }
	protected void close() { data.close(); }
	
	public int getNumPixels() { return numPixels; }
	public int getNumEntities() { return numEntities; }
	public void getDimensions( final int[] dim )
	{
		for ( int d = 0; d < numDimensions; ++d )
			dim[ d ] = this.dim[ d ];
	}
	
	public void getSteps( final int[] step )
	{
		for ( int d = 0; d < numDimensions; d++ )
			step[ d ] = this.step[ d ];
	}
	
	public int getCellId() { return cellId; }
	
	public void getOffset( final int[] offset )
	{
		for ( int i = 0; i < numDimensions; i++ )
			offset[ i ] = this.offset[ i ];
	}
	
	public final int getPosGlobal( final int[] l ) 
	{ 
		int i = l[ 0 ] - offset[ 0 ];
		for ( int d = 1; d < dim.length; ++d )
			i += (l[ d ] - offset[ d ]) * step[ d ];
		
		return i;
	}
	
}
