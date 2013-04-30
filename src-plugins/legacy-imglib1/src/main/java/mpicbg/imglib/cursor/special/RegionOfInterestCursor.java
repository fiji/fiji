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
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class RegionOfInterestCursor<T extends Type<T>> extends CursorImpl<T> implements LocalizableCursor<T> 
{
	final LocalizableByDimCursor<T> cursor;
	final int[] offset, size, roiPosition;
	
	// true means go forward, false go backward
	final boolean[] currentDirectionDim;
	
	final int numDimensions, numPixels, numPixelsMinus1;
	
	boolean isActive, debug = false;
	int i;
	
	public RegionOfInterestCursor( final LocalizableByDimCursor<T> cursor, final int[] offset, final int size[] )
	{
		super( cursor.getStorageContainer(), cursor.getImage() );
		
		this.offset = offset.clone();
		this.size = size.clone();		
		this.cursor = cursor;
		
		this.numDimensions = cursor.getImage().getNumDimensions();
		this.roiPosition = new int[ numDimensions ];
		this.currentDirectionDim = new boolean[ numDimensions ]; 
		
		int count = 1;
		for ( int d = 0; d < numDimensions; ++d )
			count *= size[ d ];
		
		numPixels = count;
		numPixelsMinus1 = count - 1;
		
		reset();
	}
	
	@Override
	public boolean hasNext() { return i < numPixelsMinus1; }
	
	@Override
	public void close()  { isActive = false; }

	@Override
	public T getType() { return cursor.getType(); }
	
	
	public void reset( final int[] o )
	{
		for ( int d = 0; d < numDimensions; ++d )
			this.offset[ d ] = o[ d ];
		
		reset();
	}

	public void reset( final int[] o, final int[] s )
	{
		for ( int d = 0; d < numDimensions; ++d )
			this.offset[ d ] = o[ d ];

		if (size != null)
		{
			for ( int d = 0; d < numDimensions; ++d )
				this.size[ d ] = s[ d ];
		}
		reset();
	}
	
	@Override
	public void reset()
	{
		i = -1;
		cursor.setPosition( offset );
		cursor.bck( 0 );
			
		for ( int d = 0; d < numDimensions; ++d )
		{
			// true means go forward
			currentDirectionDim[ d ] = true;
			roiPosition[ d ] = 0;
		}
		
		roiPosition[ 0 ] = -1;
	}

	@Override
	public void fwd()
	{
		++i;
		
		for ( int dim = 0; dim < numDimensions; ++dim )
		{
			final int d = dim;
			
			if ( currentDirectionDim[ d ] )
			{
				if ( roiPosition[ d ] < size[ d ] - 1 )
				{
					cursor.fwd( d );
					++roiPosition[ d ];
					
					// revert the direction of all lower dimensions
					for ( int e = 0; e < d; ++e )
						currentDirectionDim[ e ] = !currentDirectionDim[ e ];

					return;
				}				
			}
			else
			{
				if ( roiPosition[ d ] > 0 )
				{
					cursor.bck( d );
					--roiPosition[ d ];

					// revert the direction of all lower dimensions
					for ( int e = 0; e < d; ++e )
						currentDirectionDim[ e ] = !currentDirectionDim[ e ];
		
					return;
				}
			}
		}
	}
	
	@Override
	public int getArrayIndex() { return cursor.getArrayIndex(); }

	@Override
	public int getStorageIndex() { return cursor.getStorageIndex();	}

	@Override
	public boolean isActive() { return cursor.isActive() && isActive; }

	@Override
	public void setDebug( boolean debug ) { this.debug = debug; }

	@Override
	public void getPosition( final int[] position )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = roiPosition[ d ];
	}

	@Override
	public int[] getPosition() { return roiPosition.clone(); }

	@Override
	public int getPosition( final int dim ) { return roiPosition[ dim ]; }

	@Override
	public String getPositionAsString()
	{
		String pos = "(" + roiPosition[ 0 ];
		
		for ( int d = 1; d < numDimensions; d++ )
			pos += ", " + roiPosition[ d ];
		
		pos += ")";
		
		return pos;
	}
	
	@Override
	public String toString() { return Util.printCoordinates( roiPosition ) + " [" + Util.printCoordinates( cursor.getPosition() ) + "] " + getType(); }
}
