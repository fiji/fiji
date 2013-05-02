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

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.array.BitArray;
import mpicbg.imglib.container.basictypecontainer.array.ByteArray;
import mpicbg.imglib.container.basictypecontainer.array.CharArray;
import mpicbg.imglib.container.basictypecontainer.array.DoubleArray;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.basictypecontainer.array.IntArray;
import mpicbg.imglib.container.basictypecontainer.array.LongArray;
import mpicbg.imglib.container.basictypecontainer.array.ShortArray;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class CellContainerFactory extends DirectAccessContainerFactory
{
	protected int[] cellSize;
	protected int standardCellSize = 10;

	public CellContainerFactory()
	{
	}
	
	public CellContainerFactory( final int cellSize )
	{
		this.standardCellSize = cellSize;
	}
	
	public CellContainerFactory( final int[] cellSize )
	{
		if ( cellSize == null || cellSize.length == 0 )
		{
			System.err.println("CellContainerFactory(): cellSize is null. Using equal cell size of 10.");
			this.cellSize = null;
			return;
		}
		
		for ( int i = 0; i < cellSize.length; i++ )
		{
			if ( cellSize[ i ] <= 0 )
			{
				System.err.println("CellContainerFactory(): cell size in dimension " + i + " is <= 0, using a size of " + standardCellSize + ".");
				cellSize[ i ] = standardCellSize;
			}
		}
		
		this.cellSize = cellSize;
	}
	
	protected int[] checkDimensions( int dimensions[] )
	{
		if ( dimensions == null || dimensions.length == 0 )
		{
			System.err.println("CellContainerFactory(): dimensionality is null. Creating a 1D cell with size 1.");
			dimensions = new int[]{1};
		}

		for ( int i = 0; i < dimensions.length; i++ )
		{
			if ( dimensions[ i ] <= 0 )
			{
				System.err.println("CellContainerFactory(): size of dimension " + i + " is <= 0, using a size of 1.");
				dimensions[ i ] = 1;
			}
		}

		return dimensions;
	}
	
	protected int[] checkCellSize( int[] cellSize, int[] dimensions )
	{		
		if ( cellSize == null )
		{
			cellSize = new int[ dimensions.length ];
			for ( int i = 0; i < cellSize.length; i++ )
				cellSize[ i ] = standardCellSize;
			
			return cellSize;
		}
		
		if ( cellSize.length != dimensions.length )
		{
			System.err.println("CellContainerFactory(): dimensionality of image is unequal to dimensionality of cells, adjusting cell dimensionality.");
			int[] cellSizeNew = new int[ dimensions.length ];
			
			for ( int i = 0; i < dimensions.length; i++ )
			{
				if ( i < cellSize.length )
					cellSizeNew[ i ] = cellSize[ i ];
				else
					cellSizeNew[ i ] = standardCellSize;
			}
					
			return cellSizeNew;
		}
		
		return cellSize;
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, BitArray> createBitInstance( int[] dimensions, int entitiesPerPixel )
	{
		dimensions = checkDimensions( dimensions );
		int[] cellSize = checkCellSize( this.cellSize, dimensions );
		
		return new CellContainer<T, BitArray>( this, new BitArray( 1 ), dimensions, cellSize, entitiesPerPixel );
	}
	
	@Override
	public <T extends Type<T>> DirectAccessContainer<T, ByteArray> createByteInstance( int[] dimensions, int entitiesPerPixel )
	{
		dimensions = checkDimensions( dimensions );
		int[] cellSize = checkCellSize( this.cellSize, dimensions );
		
		return new CellContainer<T, ByteArray>( this, new ByteArray( 1 ), dimensions, cellSize, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, CharArray> createCharInstance(int[] dimensions, int entitiesPerPixel)
	{
		dimensions = checkDimensions( dimensions );
		int[] cellSize = checkCellSize( this.cellSize, dimensions );
		
		return new CellContainer<T, CharArray>( this, new CharArray( 1 ), dimensions, cellSize, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, DoubleArray> createDoubleInstance(int[] dimensions, int entitiesPerPixel)
	{
		dimensions = checkDimensions( dimensions );
		int[] cellSize = checkCellSize( this.cellSize, dimensions );
		
		return new CellContainer<T, DoubleArray>( this, new DoubleArray( 1 ), dimensions, cellSize, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, FloatArray> createFloatInstance(int[] dimensions, int entitiesPerPixel)
	{
		dimensions = checkDimensions( dimensions );
		int[] cellSize = checkCellSize( this.cellSize, dimensions );
		
		return new CellContainer<T, FloatArray>( this, new FloatArray( 1 ), dimensions, cellSize, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, IntArray> createIntInstance(int[] dimensions, int entitiesPerPixel)
	{
		dimensions = checkDimensions( dimensions );
		int[] cellSize = checkCellSize( this.cellSize, dimensions );
		
		return new CellContainer<T, IntArray>( this, new IntArray( 1 ), dimensions, cellSize, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, LongArray> createLongInstance(int[] dimensions, int entitiesPerPixel)
	{
		dimensions = checkDimensions( dimensions );
		int[] cellSize = checkCellSize( this.cellSize, dimensions );
		
		return new CellContainer<T, LongArray>( this, new LongArray( 1 ), dimensions, cellSize, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, ShortArray> createShortInstance(int[] dimensions, int entitiesPerPixel)
	{
		dimensions = checkDimensions( dimensions );
		int[] cellSize = checkCellSize( this.cellSize, dimensions );
		
		return new CellContainer<T, ShortArray>( this, new ShortArray( 1 ), dimensions, cellSize, entitiesPerPixel );
	}

	@Override
	public String getErrorMessage()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void printProperties()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParameters(String configuration)
	{
		// TODO Auto-generated method stub
		
	}

}
