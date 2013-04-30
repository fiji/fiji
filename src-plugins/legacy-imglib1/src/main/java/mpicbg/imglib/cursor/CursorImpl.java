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

package mpicbg.imglib.cursor;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * We use the class {@link CursorImpl} instead of implementing methods here so that other classes can
 * only implement {@link Cursor} and extend other classes instead. As each {@link CursorImpl} is also
 * a {@link Cursor} there are no disadvantages for the {@link Cursor} implementations.
 * 
 * @param <T>
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 * @author Stephan
 */
public abstract class CursorImpl<T extends Type<T>> implements Cursor<T>
{
	final protected Image<T> image;
	final protected Container<T> container;
	protected boolean isClosed = false, debug = false;
	
	public CursorImpl( final Container<T> container, final Image<T> image )
	{
		this.image = image;
		this.container = container;
	}

	@Override
	public Iterator<T> iterator() 
	{
		reset();
		return this;
	}
	
	@Override
	public int getArrayIndex() { return getType().getIndex(); }
	@Override
	public Image<T> getImage() { return image; }
	@Override
	public Container<T> getStorageContainer() { return container; }
	@Override
	public boolean isActive() { return !isClosed; }
	@Override
	public void setDebug( final boolean debug ) { this.debug = debug; }
	
	@Override
	public void remove() {}
	
	@Override
	public T next(){ fwd(); return getType(); }

	@Override
	public void fwd( final long steps )
	{ 
		for ( long j = 0; j < steps; ++j )
			fwd();
	}

	@Override
	public int[] createPositionArray() { return new int[ image.getNumDimensions() ]; }	
	
	@Override
	public int getNumDimensions() { return image.getNumDimensions(); }
	
	@Override
	public int[] getDimensions() { return image.getDimensions(); }
	
	@Override
	public void getDimensions( int[] position ) { image.getDimensions( position ); }
	
}
