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

package mpicbg.imglib.cursor.array;

import mpicbg.imglib.container.array.Array;
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
public class ArrayCursor<T extends Type<T>> extends CursorImpl<T> implements Cursor<T>
{
	protected final T type;
	protected final Array<T,?> container;
	protected final int maxIndex;
	
	public ArrayCursor( final Array<T,?> container, final Image<T> image, final T type ) 
	{
		super( container, image );

		this.type = type;
		this.container = container;
		this.maxIndex = container.getNumPixels() - 1;
		
		reset();
	}
	
	@Override
	public T getType() { return type; }
	
	@Override
	public boolean hasNext() { return type.getIndex() < maxIndex; }

	@Override
	public void fwd( final long steps ) { type.incIndex( (int)steps ); }

	@Override
	public void fwd() { type.incIndex(); }

	@Override
	public void close() 
	{ 
		isClosed = true;
		type.updateIndex( maxIndex + 1 );
	}

	@Override
	public void reset()
	{ 
		type.updateIndex( -1 ); 
		type.updateContainer( this );
		isClosed = false;
	}

	@Override
	public Array<T,?> getStorageContainer(){ return container; }

	@Override
	public int getStorageIndex() { return 0; }
	
	@Override
	public String toString() { return type.toString(); }		
}
