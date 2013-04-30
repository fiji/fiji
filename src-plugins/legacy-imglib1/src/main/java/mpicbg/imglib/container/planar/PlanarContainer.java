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

package mpicbg.imglib.container.planar;

import java.util.ArrayList;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.DirectAccessContainerImpl;
import mpicbg.imglib.container.basictypecontainer.PlanarAccess;
import mpicbg.imglib.container.basictypecontainer.array.ArrayDataAccess;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.planar.PlanarCursor;
import mpicbg.imglib.cursor.planar.PlanarCursor2D;
import mpicbg.imglib.cursor.planar.PlanarLocalizableByDimCursor;
import mpicbg.imglib.cursor.planar.PlanarLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.planar.PlanarLocalizableCursor;
import mpicbg.imglib.cursor.planar.PlanarLocalizablePlaneCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * A {@link Container} that stores data in an array of 2d-slices each as a
 * linear array of basic types.  For types that are supported by ImageJ (byte,
 * short, int, float), an actual Planar is created or used to store the
 * data.  Alternatively, an {@link PlanarContainer} can be created using
 * an already existing {@link Planar} instance.
 *
 * {@link PlanarContainer PlanarContainers} provides a legacy layer to
 * apply imglib-based algorithm implementations directly on the data stored in
 * an ImageJ {@link Planar}.  For all types that are supported by ImageJ, the
 * {@link PlanarContainer} provides access to the pixels of an
 * {@link Planar} instance that can be accessed ({@see #getPlanar()}.
 *
 * @author Funke
 * @author Preibisch
 * @author Saalfeld
 * @author Schindelin
 * @author Jan Funke
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 * @author Johannes Schindelin
 */
public class PlanarContainer<T extends Type<T>, A extends ArrayDataAccess<A>> extends DirectAccessContainerImpl<T,A> implements PlanarAccess<A>
{
	final protected PlanarContainerFactory factory;
	final protected int slices;

	final protected ArrayList< A > mirror;

	public PlanarContainer( final int[] dim, final int entitiesPerPixel )
	{
		this( new PlanarContainerFactory(), dim, entitiesPerPixel );
	}

	protected PlanarContainer( final PlanarContainerFactory factory, final int[] dim, final int entitiesPerPixel )
	{
		this( factory, null, dim, entitiesPerPixel );
	}

	PlanarContainer( final PlanarContainerFactory factory, final A creator, final int[] dim, final int entitiesPerPixel )
	{
		super( factory, dim, entitiesPerPixel );

		// compute number of slices
		int s = 1;

		for ( int d = 2; d < numDimensions; ++d )
			s *= dim[ d ];

		slices = s;

		this.factory = factory;

		mirror = new ArrayList< A >( slices );

		for ( int i = 0; i < slices; ++i )
			mirror.add( creator == null ? null : creator.createArray( getDimension( 0 ) * getDimension( 1 ) * entitiesPerPixel ) );
	}

	@Override
	public A update( final Cursor< ? > c )
	{
		return mirror.get( c.getStorageIndex() );
	}

	/**
   * @return total number of image planes
	 */
	public int getSlices() { return slices; }

	/**
	 * For a given >=2d location, estimate the pixel index in the stack slice.
	 *
	 * @param l
	 * @return
	 */
	public final int getIndex( final int[] l )
	{
		if ( numDimensions > 1 )
			return l[ 1 ] * dim[ 0 ] + l[ 0 ];
		return l[ 0 ];
	}

	@Override
	public PlanarCursor<T> createCursor( final Image<T> image )
	{
		if ( numDimensions == 2 )
			return new PlanarCursor2D< T >( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		else
			return new PlanarCursor< T >( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public PlanarLocalizableCursor<T> createLocalizableCursor( final Image<T> image )
	{
		return new PlanarLocalizableCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public PlanarLocalizablePlaneCursor<T> createLocalizablePlaneCursor( final Image<T> image )
	{
		return new PlanarLocalizablePlaneCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public PlanarLocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image )
	{
		return new PlanarLocalizableByDimCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public PlanarLocalizableByDimOutOfBoundsCursor<T> createLocalizableByDimCursor( final Image<T> image, OutOfBoundsStrategyFactory<T> outOfBoundsFactory )
	{
		return new PlanarLocalizableByDimOutOfBoundsCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer(), outOfBoundsFactory );
	}

	@Override
	public PlanarContainerFactory getFactory() { return factory; }

	@Override
	public void close()
	{
		for ( final A array : mirror )
			array.close();
	}

	@Override
	public boolean compareStorageContainerCompatibility( final Container<?> container )
	{
		return compareStorageContainerDimensions( container ) &&
		  getFactory().getClass().isInstance( container.getFactory() );
	}

	@Override
	public A getPlane( final int no ) { return mirror.get( no ); }

	@Override
	public void setPlane( final int no, final A plane ) { mirror.set( no, plane ); }
}
