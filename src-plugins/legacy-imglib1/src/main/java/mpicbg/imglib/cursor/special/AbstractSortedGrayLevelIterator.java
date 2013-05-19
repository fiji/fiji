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

package mpicbg.imglib.cursor.special;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.Iterable;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.IntegerType;
import mpicbg.imglib.util.Util;

/**
 * Iterate over all pixels ordered by their gray level
 * 
 * @param <T>
 *
 * @author Steffen Jaensch
 * @author Steffen Jaensch <jaensch@mpi-cbg.de>
 */
public abstract class AbstractSortedGrayLevelIterator< T extends IntegerType< T > > implements Iterable, LocalizableCursor< T >, Localizable
{
	protected final Image< T > image;
	protected final int[] sortedLinIdx;

	protected int n, maxIdx, curIdx;
	protected final int[] position;
	protected final int[] dimensions;
	
	public AbstractSortedGrayLevelIterator( final Image< T > image )
	{
		this.image 			= image;
		this.position 		= image.createPositionArray();
		this.n 				= image.getNumPixels();
		this.maxIdx 		= this.n-1;
		this.dimensions 	= image.getDimensions();
		createInternalCursor();
		this.sortedLinIdx 	= getLinearIndexArraySortedByGrayLevel();
		reset();
	}

	@Override
	public boolean hasNext()
	{
		return curIdx < this.maxIdx;
	}

	@Override
	public abstract void fwd();

	@Override
	public void reset()
	{		
		curIdx = -1;
	}

	@Override
	public int[] getPosition() 
	{ 
		int[] position = image.createPositionArray();
		getPosition( position );
		return position;
	}
	
	@Override
	public String getPositionAsString() 
	{ 
		return Util.printCoordinates(getPosition());
	}

	@Override
	public int[] createPositionArray() { return image.createPositionArray(); }

	@Override
	public int getArrayIndex() { return sortedLinIdx[curIdx]; }

	@Override
	public Image<T> getImage() { return image; }

	@Override
	public Container<T> getStorageContainer() { return image.getContainer(); }

	@Override
	public abstract int getStorageIndex();

	@Override
	public boolean isActive() { return true; }

	@Override
	public void setDebug(boolean debug) {}

	@Override
	public T next() { fwd(); return getType(); }

	@Override
	public void remove() { throw new RuntimeException("remove is not supported.");}

	@Override
	public Iterator<T> iterator()
	{
		reset();
		return this;
	}

	@Override
	public int[] getDimensions() { return dimensions; }

	@Override
	public void getDimensions(int[] position) { image.getDimensions( position ); }

	@Override
	public int getNumDimensions() { return image.getNumDimensions(); }
	
	
	protected  <C extends Comparable<C> & Type<C>> C max( final Image<C> image)
	{
		
		C max = image.createType();

		// create a cursor for the image (the order does not matter)
		Cursor<C> cursor = image.createCursor();
		
		// initialize max with the first image value
		max.set( cursor.next() );

		for(C type : cursor)
		{
			if ( type.compareTo( max ) > 0 )
				max.set( type );
		}
		return max;
	}
	
	//create a cursor that can be set to a linear index position, see also getIntegerValueAtLinearIndex()
	protected abstract void createInternalCursor();
	
	//returns the gray value at the given linear index p, see also createInternalCursor()
	protected abstract int getIntegerValueAtLinearIndex(final int p);
	
	//counting sort for sorting the pixels by intensity
	//needs only one array of length n (compare to bucket sort)
	protected int[] getLinearIndexArraySortedByGrayLevel()
	{
		int k     = max(image).getInteger();
		int[] c   = new int[k+1];
		
		int[] idx = new int[this.n];
		int p=0;
		for(p=0;p<n;p++)
		{
			c[getIntegerValueAtLinearIndex(p)]++;
		}
		//c[i] = how many times gray level i is in image
		
		for(int i = 1; i<=k;i++)
			c[i]= c[i] + c[i-1];
		//c[i] = number of elements <= i in image
		//     = position with the largest index at which element i will be in the sorted array
		
		int level;
		for(p=this.n-1;p>=0;p--)
		{

			level = getIntegerValueAtLinearIndex(p);
			
			//idx[c[level]-1] = p;    //increasing order
			idx[this.n-c[level]] = p; //decreasing order
			c[level]--;
		}
		return idx;
	}
	
	final static public int positionToIndex( final int[] position, final int[] dimensions )

    {

                   final int maxDim = dimensions.length - 1;

                   int i = position[ maxDim ];

                   for ( int d = maxDim - 1; d >= 0; --d )

                                   i = i * dimensions[ d ] + position[ d ];

                   return i;

    }

    final static public void indexToPosition( int index, final int[] dimensions, final int[] position )

    {

                   final int maxDim = dimensions.length - 1;

                   for ( int d = 0; d < maxDim; ++d )

                   {

                                   final int j = index / dimensions[ d ];

                                   position[ d ] = index - j * dimensions[ d ];

                                   index = j;

                   }

                   position[ maxDim ] = index;

    }

}
