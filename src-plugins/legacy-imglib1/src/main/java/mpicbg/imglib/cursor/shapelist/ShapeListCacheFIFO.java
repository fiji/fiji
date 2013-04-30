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

package mpicbg.imglib.cursor.shapelist;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import mpicbg.imglib.container.shapelist.ShapeListCached;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 */
public class ShapeListCacheFIFO<T extends Type< T > > extends ShapeListCache<T>
{
	final Map<Integer, T> cache;
	final LinkedList< Integer > queue;

	public ShapeListCacheFIFO( final int cacheSize, final ShapeListCached<T> container )
	{		
		super( cacheSize, container );
		
		cache = new HashMap<Integer, T>( cacheSize );
		queue = new LinkedList<Integer>( );
		
		for ( int i = 0; i < cacheSize; ++i )
			queue.add( Integer.MIN_VALUE );		
	}

	@Override
	public T lookUp( final int[] position )
	{
		final int index = fakeArray.getPos( position );

		final T value = cache.get( index );
		
		if ( value == null )
		{
			final T t = container.getShapeType( position );
			queue.add( index );
			cache.put( index , t );
			
			cache.remove( queue.removeFirst() );				
			
			return t;
		}
		else
		{
			return value;
		}		
	}

	@Override
	public ShapeListCache<T> getCursorCacheInstance() { return new ShapeListCacheFIFO<T>( cacheSize, container );	}
}
