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

import mpicbg.imglib.cursor.CursorImpl;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.label.FakeType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class LocalNeighborhoodCursor<T extends Type<T>> extends CursorImpl<T>
{
	/**
	 * Here we "misuse" a ArrayLocalizableCursor to iterate over cells,
	 * it always gives us the location of the current cell we are instantiating 
	 */
	final ArrayLocalizableCursor<FakeType> neigborhoodCursor;

	final LocalizableByDimCursor<T> cursor;
	final int[] position, tmp;
	final int numDimensions, centralPositionIndex;
	boolean isActive, debug = false;
	
	public LocalNeighborhoodCursor( final LocalizableByDimCursor<T> cursor )
	{
		super( cursor.getStorageContainer(), cursor.getImage() );
		
		this.cursor = cursor;
		this.position = cursor.getPosition();
		
		this.numDimensions = cursor.getImage().getNumDimensions();
		this.tmp = new int[ numDimensions ];
				
		int[] dim = new int[ numDimensions ];
		for ( int d = 0; d < numDimensions; ++d )
			dim[ d ] = 3;

		this.neigborhoodCursor = ArrayLocalizableCursor.createLinearCursor( dim );
		this.isActive = true;

		for ( int d = 0; d < numDimensions; ++d )
			dim[ d ] = 1;

		this.centralPositionIndex = neigborhoodCursor.getStorageContainer().getPos( dim );
	}
	
	@Override
	public boolean hasNext() { return neigborhoodCursor.hasNext(); }
	
	@Override
	public void close() 
	{
		neigborhoodCursor.close();
		isActive = false;
	}

	public void update()
	{
		cursor.getPosition( position );
		this.neigborhoodCursor.reset();		
	}

	@Override
	public T getType() { return cursor.getType(); }
	
	@Override
	public void reset()
	{
		cursor.setPosition( position );
		this.neigborhoodCursor.reset();		
	}

	@Override
	public void fwd()
	{
		neigborhoodCursor.fwd();
		
		if ( neigborhoodCursor.getType().getIndex() == centralPositionIndex )
			neigborhoodCursor.fwd();
		
		neigborhoodCursor.getPosition( tmp );

		for ( int d = 0; d < numDimensions; ++d )
			tmp[ d ] = position[ d ] + ( tmp[d] - 1 );
		
		cursor.moveTo( tmp );		
	}
	
	public LocalizableByDimCursor<T> getParentCursor() { return cursor; }
	
	public int getRelativePosition( final int d ) { return neigborhoodCursor.getPosition( d ); }
	
	@Override
	public int getArrayIndex() { return cursor.getArrayIndex(); }

	@Override
	public int getStorageIndex() { return cursor.getStorageIndex();	}

	@Override
	public boolean isActive() { return cursor.isActive() && isActive; }

	@Override
	public void setDebug( boolean debug ) { this.debug = debug; }
}
