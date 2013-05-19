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
import mpicbg.imglib.cursor.Iterable;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * Iterate over all pixels in an n-dimensional sphere.
 * 
 * This implementation is {@link Container} agnostic and thus the general
 * purpose version of the {@link Iterator}.  For specific
 * {@link Container Containers}, a different implementation might be more
 * efficient.
 * 
 * @param <T>
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 * @author Stephan Preibisch <preibisch@mpi-cbg.de>
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class HyperSphereIterator< T extends Type< T > > implements Iterable, LocalizableCursor< T >, Localizable
{
	final protected Image< T > image;
	final protected Localizable center;
	final protected LocalizableByDimCursor< T > cursor;
	protected int activeCursorIndex;
	
	final protected int radius, numDimensions, maxDim;
	
	// the current radius in each dimension we are at
	final int[] r;
	
	// the remaining number of steps in each dimension we still have to go
	final int[] s;
	
	public HyperSphereIterator( final Image< T > image, final Localizable center, final int radius ) { this( image, center, radius, null ); }
	
	public HyperSphereIterator( final Image< T > image, final Localizable center, final int radius, final OutOfBoundsStrategyFactory< T > oobFactory )
	{
		this.image = image;
		this.center = center;
		this.radius = radius;
		this.numDimensions = image.getNumDimensions();
		this.maxDim = numDimensions - 1;
		this.r = new int[ numDimensions ];
		this.s = new int[ numDimensions ];
		
		if ( oobFactory == null )
			this.cursor = image.createLocalizableByDimCursor();
		else
			this.cursor = image.createLocalizableByDimCursor( oobFactory );	
		
		reset();
	}

	@Override
	public boolean hasNext()
	{
		return s[ maxDim ] > 0; 
	}

	@Override
	public void fwd()
	{
		int d;
		for ( d = 0; d < numDimensions; ++d )
		{
			if ( --s[ d ] >= 0 )
			{
				cursor.fwd( d );
				break;
			}
			else
			{
				s[ d ] = r[ d ] = 0;
				cursor.setPosition( center.getPosition( d ), d );
			}
		}

		if ( d > 0 )
		{
			final int e = d - 1;
			final int rd = r[ d ];
			final int pd = rd - s[ d ];
			
			final int rad = (int)( Math.sqrt( rd * rd - pd * pd ) );
			s[ e ] = 2 * rad;
			r[ e ] = rad;
			
			cursor.setPosition( center.getPosition( e ) - rad, e );
		}
	}

	@Override
	public void reset()
	{		
		final int maxDim = numDimensions - 1;
		
		for ( int d = 0; d < maxDim; ++d )
		{
			r[ d ] = s[ d ] = 0;
			cursor.setPosition( center.getPosition( d ), d ); 
		}
		
		cursor.setPosition( center.getPosition( maxDim ) - radius - 1, maxDim  );
		
		r[ maxDim ] = radius;
		s[ maxDim ] = 1 + 2 * radius;			
	}

	@Override
	public void fwd( final long steps )
	{
		for ( long j = 0; j < steps; ++j )
			fwd();
	}

	@Override
	public void getPosition( final int[] position ) { cursor.getPosition( position );	}

	@Override
	public int[] getPosition() { return cursor.getPosition(); }

	@Override
	public int getPosition( final int dim ) { return cursor.getPosition( dim ); }

	@Override
	public String getPositionAsString() { return cursor.getPositionAsString(); }

	@Override
	public void close() { cursor.close(); }	

	@Override
	public int[] createPositionArray() { return cursor.createPositionArray(); }

	@Override
	public int getArrayIndex() { return cursor.getArrayIndex(); }

	@Override
	public Image<T> getImage() { return image; }

	@Override
	public Container<T> getStorageContainer() { return cursor.getStorageContainer(); }

	@Override
	public int getStorageIndex() { return cursor.getStorageIndex(); }

	@Override
	public T getType() { return cursor.getType(); }

	@Override
	public boolean isActive() { return cursor.isActive(); }

	@Override
	public void setDebug(boolean debug) {}

	@Override
	public T next() { fwd(); return getType(); }

	@Override
	public void remove() {}

	@Override
	public Iterator<T> iterator()
	{
		reset();
		return this;
	}

	@Override
	public int[] getDimensions() { return image.getDimensions(); }

	@Override
	public void getDimensions(int[] position) { image.getDimensions( position ); }

	@Override
	public int getNumDimensions() { return image.getNumDimensions(); }
}
