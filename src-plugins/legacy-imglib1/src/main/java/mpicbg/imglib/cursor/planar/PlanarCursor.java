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

import mpicbg.imglib.container.planar.PlanarContainer;
import mpicbg.imglib.cursor.CursorImpl;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * Basic Iterator for {@link PlanarContainer PlanarContainers}
 * @param <T>
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class PlanarCursor< T extends Type< T >> extends CursorImpl< T >
{
	protected final T type;

	protected final PlanarContainer< T, ? > container;

	protected final int lastIndex, lastSliceIndex;

	protected int sliceIndex;
	
	protected boolean hasNext;

	public PlanarCursor( final PlanarContainer< T, ? > container, final Image< T > image, final T type )
	{
		super( container, image );

		this.type = type;
		this.container = container;
		lastIndex = container.getDimension( 0 ) * container.getDimension( 1 ) - 1;
		lastSliceIndex = container.getSlices() - 1;

		reset();
	}

	@Override
	public T getType() { return type; }

	/**
	 * Note: This test is fragile in a sense that it returns true for elements
	 * after the last element as well.
	 * 
	 * @return false for the last element 
	 */
	@Override
	public boolean hasNext()
	{
		return hasNext;
	}

	@Override
	public void fwd()
	{
		type.incIndex();

		final int i = type.getIndex();
		
		if ( i < lastIndex )
			return;
		else if ( i == lastIndex )
			hasNext = sliceIndex < lastSliceIndex;
		else
		{
			++sliceIndex;
			type.updateIndex( 0 );
			type.updateContainer( this );
		}
	}

	@Override
	public void close()
	{
		isClosed = true;
		type.updateIndex( lastIndex + 1 );
		sliceIndex = lastSliceIndex + 1;
	}

	@Override
	public void reset()
	{
		sliceIndex = 0;
		type.updateIndex( -1 );
		type.updateContainer( this );
		isClosed = false;
		hasNext = true;
	}

	@Override
	public PlanarContainer< T, ? > getStorageContainer() { return container;	}

	@Override
	public int getStorageIndex() { return sliceIndex; }

	@Override
	public String toString() { return type.toString(); }
}
