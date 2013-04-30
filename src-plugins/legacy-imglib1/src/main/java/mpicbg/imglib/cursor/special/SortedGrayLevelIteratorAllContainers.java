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

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.IntegerType;

/**
 * Iterate over all pixels ordered by their gray level
 * 
 * @param <T>
 *
 * @author Steffen Jaensch
 * @author Steffen Jaensch <jaensch@mpi-cbg.de>
 */
public class SortedGrayLevelIteratorAllContainers< T extends IntegerType< T > > extends AbstractSortedGrayLevelIterator<T>
{
	
	protected LocalizableByDimCursor<T> loccur;
		
	public SortedGrayLevelIteratorAllContainers( final Image< T > image )
	{
		super(image);
	}
	
	@Override
	protected void createInternalCursor()
	{
		this.loccur = image.createLocalizableByDimCursor();
	}
	
	@Override
	protected int getIntegerValueAtLinearIndex(final int p)
	{
		indexToPosition( p, dimensions, position );
		loccur.setPosition(position);
		return loccur.getType().getInteger();
	}

	@Override
	public void fwd()
	{
		curIdx++;
		indexToPosition( sortedLinIdx[curIdx], dimensions, position );
		loccur.setPosition(position);
	}

	@Override
	public void fwd( final long steps )
	{
		curIdx+=(int)steps;
		indexToPosition( sortedLinIdx[curIdx], dimensions, position );
		loccur.setPosition(position);
	}

	@Override
	public void getPosition( final int[] position ) 
	{ 
		indexToPosition( sortedLinIdx[curIdx], dimensions, position );
	}

	@Override
	public int getPosition( final int dim ) 
	{ 
		indexToPosition( sortedLinIdx[curIdx], dimensions, position );
		return this.position[dim];
	}

	@Override
	public int getStorageIndex() {return loccur.getStorageIndex(); }

	@Override
	public T getType() { return loccur.getType(); }
	
	public void close(){loccur.close();}

	
	

}
