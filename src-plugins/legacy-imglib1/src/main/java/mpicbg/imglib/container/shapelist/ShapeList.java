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

package mpicbg.imglib.container.shapelist;

import java.awt.Shape;
import java.util.ArrayList;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.ContainerImpl;
import mpicbg.imglib.cursor.shapelist.ShapeListLocalizableByDimCursor;
import mpicbg.imglib.cursor.shapelist.ShapeListLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.shapelist.ShapeListLocalizablePlaneCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * 
 * @param <T>
 *
 * @version 0.1a
 */
//public class ShapeList< T extends Type< T > > extends ContainerImpl< T, DataAccess >
/**
 * TODO
 *
 * @author Stephan Saalfeld
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class ShapeList< T extends Type< T > > extends ContainerImpl< T >
{
	final public ShapeListContainerFactory factory;
	
	/* shapes need to be ordered for rendering with correct overlap */
	final protected ArrayList< ArrayList< Shape > > shapeLists;
	final protected ArrayList< ArrayList< T > > typeLists;
	final protected T background;
	
	public ShapeList( final ShapeListContainerFactory factory, final int[] dim, final T background )
	{
		super( factory, dim );
		this.factory = factory;
		
		int n = 1;
		for ( int d = 2; d < dim.length; ++d )
			n *= dim[ d ];
		
		shapeLists = new ArrayList< ArrayList< Shape > > ( n );
		typeLists = new ArrayList< ArrayList< T > > ( n );
		
		for ( int d = 0; d < n; ++d )
		{
			shapeLists.add( new ArrayList< Shape >() );
			typeLists.add( new ArrayList< T >() );
		}
		this.background = background;
	}

	public ShapeList( final int[] dim, final T background )
	{
		this( new ShapeListContainerFactory(), dim, background );
	}
	
	public T getBackground() { return background; }
	
	public synchronized void addShape( final Shape shape, final T type, final int[] position )
	{
		int p = 0;
		if ( position != null )
		{
			int f = 1;
			for ( int d = 2; d < numDimensions; ++d )
			{
				p += f * position[ d - 2 ];
				f *= dim[ d ];
			}
		}
		shapeLists.get( p ).add( shape ); 
		typeLists.get( p ).add( type );
	}

	/** @return a shallow copy of the lists of Shape instances.
	 *  That is, the Shape instances themselves are the originals. */
	public synchronized ArrayList< ArrayList< Shape > > getShapeLists() {
		final ArrayList< ArrayList< Shape > > sl = new ArrayList< ArrayList< Shape > >();
		for (final ArrayList< Shape > a : shapeLists)
		{
			sl.add( new ArrayList< Shape >( a ) );
		}

		return sl;
	}

	@Override
	public ShapeListContainerFactory getFactory() { return factory; }
	
	@Override
	public ShapeListLocalizableByDimCursor< T > createCursor( final Image< T > image ) 
	{ 
		return createLocalizableByDimCursor( image );
	}

	@Override
	public ShapeListLocalizableByDimCursor< T > createLocalizableCursor( final Image< T > image ) 
	{ 
		return createLocalizableByDimCursor( image );
	}

	@Override
	public ShapeListLocalizablePlaneCursor< T > createLocalizablePlaneCursor( final Image< T > image ) 
	{ 
		return new ShapeListLocalizablePlaneCursor< T >( this, image );
	}
	
	@Override
	public ShapeListLocalizableByDimCursor< T > createLocalizableByDimCursor( final Image< T > image ) 
	{
		return new ShapeListLocalizableByDimCursor< T >( this, image );
	}
	
	@Override
	public ShapeListLocalizableByDimOutOfBoundsCursor< T > createLocalizableByDimCursor( final Image< T > image, final OutOfBoundsStrategyFactory< T > outOfBoundsFactory ) 
	{
		return new ShapeListLocalizableByDimOutOfBoundsCursor< T >( this, image, outOfBoundsFactory );
	}
	
	@Override
	public void close(){}

	/**
	 * Find the upper most Shape visible at the given position and return its
	 * {@link Type}.
	 * 
	 * Does not perform bounds checking.  For shape planes, this doesn't
	 * matter, but for all other dimensions, the result is undefined or an
	 * {@link IndexOutOfBoundsException}.
	 * 
	 * @param x
	 * @param y
	 * @param p pre-multiplied index of all dimensions >1
	 * @return
	 */
	protected T getShapeType( final int x, final int y, final int p )
	{
		final ArrayList< Shape > shapeList = shapeLists.get( p );
		for ( int i = shapeList.size() - 1; i >= 0; --i )
		{
			if ( shapeList.get( i ).contains( x, y ) )
				return typeLists.get( p ).get( i );
		}
		return background;
	}
	
	
	/**
	 * Find the upper most Shape visible at the given position and return its
	 * {@link Type}.
	 * 
	 * This random access and, therefore, not efficient.  Use only if the
	 * dimensions >1 cannot be pre-calculated.
	 * 
	 * Does not perform bounds checking.  For shape planes, this doesn't
	 * matter, but for all other dimensions, the result is undefined or an
	 * {@link IndexOutOfBoundsException}.
	 * 
	 * @param position
	 * @return
	 */
	public T getShapeType( final int[] position )
	{
		int p = 0;
		int f = 1;
		for ( int d = 2; d < position.length; ++d )
		{
			p += f * position[ d ];
			f *= dim[ d ];
		}
		return getShapeType( position[ 0 ], position[ 1 ], p );
	}

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
