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

package mpicbg.imglib.cursor.link;

import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 */
final public class GenericCursorLink< T extends Type<T> > implements CursorLink
{
	final LocalizableByDimCursor<T> linkedCursor;
	
	public GenericCursorLink( final LocalizableByDimCursor<T> linkedCursor ) { this.linkedCursor = linkedCursor; }
	
	public GenericCursorLink( final Image<T> img ) { this.linkedCursor = img.createLocalizableByDimCursor(); }
	
	public LocalizableCursor<T> getLinkedCursor() { return linkedCursor; }
	
	@Override
	final public void bck( final int dim ) { linkedCursor.bck( dim ); }

	@Override
	final public void fwd( final int dim ) { linkedCursor.fwd( dim ); }

	@Override
	final public void move( final int steps, final int dim ) { linkedCursor.move( steps, dim); }

	@Override
	final public void moveRel( final int[] position ) { linkedCursor.moveRel( position ); }

	@Override
	final public void moveTo( final Localizable localizable ) { linkedCursor.moveTo( localizable ); }

	@Override
	final public void moveTo( final int[] position ) { linkedCursor.moveTo( position ); }

	@Override
	final public void setPosition( final Localizable localizable ) { linkedCursor.setPosition( localizable ); }
	
	@Override
	final public void setPosition( final int[] position ) { linkedCursor.setPosition( position ); }

	@Override
	final public void setPosition( final int position, final int dim ) { linkedCursor.setPosition( position, dim ); }

	@Override
	final public void getPosition( final int[] position ) { linkedCursor.getPosition( position ); }

	@Override
	final public int[] getPosition() { return linkedCursor.getPosition(); }

	@Override
	final public int getPosition( final int dim ) { return linkedCursor.getPosition( dim ); }

	@Override
	final public String getPositionAsString() { return linkedCursor.getPositionAsString(); }

	@Override
	final public void fwd( final long steps ) { linkedCursor.fwd( steps ); }

	@Override
	final public void fwd() { linkedCursor.fwd(); }
}
