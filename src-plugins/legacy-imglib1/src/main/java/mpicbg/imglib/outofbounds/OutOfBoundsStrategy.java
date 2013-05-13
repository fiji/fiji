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

package mpicbg.imglib.outofbounds;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public abstract class OutOfBoundsStrategy<T extends Type<T>>
{
	final Cursor<T> parentCursor;
	
	public OutOfBoundsStrategy( final Cursor<T> parentCursor )
	{
		this.parentCursor = parentCursor;
	}
	
	/**
	 * @returns a link to the parent Cursor of this Strategy
	 */
	public Cursor<T> getParentCursor() { return parentCursor; }

	/**
	 * Fired by the parent cursor in case that it moves while being out of image bounds
	 */
	public abstract void notifyOutOfBOunds();

	/**
	 * Fired by the parent cursor in case that it moves while being out of image bounds
	 */
	public abstract void notifyOutOfBOunds( int steps, int dim );

	/**
	 * Fired by the parent cursor in case that it moves while being out of image bounds
	 */
	public abstract void notifyOutOfBOundsFwd( int dim );

	/**
	 * Fired by the parent cursor in case that it moves while being out of image bounds
	 */
	public abstract void notifyOutOfBOundsBck( int dim );
	
	/**
	 * Fired by the parent cursor in case that it leaves image bounds
	 */
	public abstract void initOutOfBOunds();
	
	/**
	 * @returns the Type that stores the current value of the OutOfBoundsStrategy
	 */
	public abstract T getType();
	
	/**
	 * Closed possibly created cursors or images
	 */
	public abstract void close();
}
