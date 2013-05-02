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

package mpicbg.imglib.container.array;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.DirectAccessContainerImpl;
import mpicbg.imglib.container.basictypecontainer.DataAccess;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.array.ArrayCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableByDimCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizablePlaneCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class Array<T extends Type<T>, A extends DataAccess> extends DirectAccessContainerImpl<T, A>
{
	final protected int[] step;
	final ArrayContainerFactory factory;
	
	// the DataAccess created by the ArrayContainerFactory
	final A data;

	public Array( final ArrayContainerFactory factory, final A data, final int[] dim, final int entitiesPerPixel )
	{
		super( factory, dim, entitiesPerPixel );
		
		step = Array.createAllocationSteps( dim );
		this.factory = factory;
		this.data = data;
	}
	
	@Override
	public A update( final Cursor<?> c ) { return data; }

	@Override
	public ArrayContainerFactory getFactory() { return factory; }
	
	@Override
	public ArrayCursor<T> createCursor( final Image<T> image ) 
	{
		// create a Cursor using a Type that is linked to the container
		ArrayCursor<T> c = new ArrayCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}

	@Override
	public ArrayLocalizableCursor<T> createLocalizableCursor( final Image<T> image ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		ArrayLocalizableCursor<T> c = new ArrayLocalizableCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}

	@Override
	public ArrayLocalizablePlaneCursor<T> createLocalizablePlaneCursor( final Image<T> image ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		ArrayLocalizablePlaneCursor<T> c = new ArrayLocalizablePlaneCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}
	
	@Override
	public ArrayLocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		ArrayLocalizableByDimCursor<T> c = new ArrayLocalizableByDimCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}
	
	@Override
	public ArrayLocalizableByDimOutOfBoundsCursor<T> createLocalizableByDimCursor( final Image<T> image, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		ArrayLocalizableByDimOutOfBoundsCursor<T> c = new ArrayLocalizableByDimOutOfBoundsCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer(), outOfBoundsFactory );
		return c;
	}
	
	public static int[] createAllocationSteps( final int[] dim )
	{
		int[] steps = new int[ dim.length ];
		createAllocationSteps( dim, steps );
		return steps;		
	}

	public static void createAllocationSteps( final int[] dim, final int[] steps )
	{
		steps[ 0 ] = 1;
		for ( int d = 1; d < dim.length; ++d )
			  steps[ d ] = steps[ d - 1 ] * dim[ d - 1 ];
	}
	
	public final int getPos( final int[] l ) 
	{ 
		int i = l[ 0 ];
		for ( int d = 1; d < numDimensions; ++d )
			i += l[ d ] * step[ d ];
		
		return i;
	}

	@Override
	public void close() { data.close();	}

	@Override
	public boolean compareStorageContainerCompatibility( final Container<?> container )
	{
		if ( compareStorageContainerDimensions( container ))
		{			
			if ( getFactory().getClass().isInstance( container.getFactory() ))
				return true;
			else
				return false;
		}
		else
		{
			return false;
		}
	}
}
