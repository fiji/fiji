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
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.container.imageplus;

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.basictypecontainer.array.BitArray;
import mpicbg.imglib.container.basictypecontainer.array.ByteArray;
import mpicbg.imglib.container.basictypecontainer.array.CharArray;
import mpicbg.imglib.container.basictypecontainer.array.DoubleArray;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.basictypecontainer.array.IntArray;
import mpicbg.imglib.container.basictypecontainer.array.LongArray;
import mpicbg.imglib.container.basictypecontainer.array.ShortArray;
import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.type.Type;

/**
 * Factory that creates an appropriate {@link ImagePlusContainer}.
 * 
 *   Johannes Schindelin
 *
 * @author Funke
 * @author Preibisch
 * @author Rueden
 * @author Saalfeld
 * @author Schindelin
 * @author Jan Funke
 * @author Stephan Preibisch
 * @author Curtis Rueden
 * @author Stephan Saalfeld
 */
public class ImagePlusContainerFactory extends PlanarContainerFactory
{
	@Override
	public < T extends Type< T > > DirectAccessContainer< T, BitArray > createBitInstance( int[] dimensions, final int entitiesPerPixel )
	{
		if ( dimensions.length > 5 )
			throw new RuntimeException( "Unsupported dimensionality: " + dimensions.length );

		return new ImagePlusContainer< T, BitArray >( this, new BitArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, ByteArray > createByteInstance( final int[] dimensions, final int entitiesPerPixel )
	{
		if ( dimensions.length > 5 )
			throw new RuntimeException( "Unsupported dimensionality: " + dimensions.length );

		return new ByteImagePlus< T >( this, dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, CharArray > createCharInstance( int[] dimensions, final int entitiesPerPixel )
	{
		if ( dimensions.length > 5 )
			throw new RuntimeException( "Unsupported dimensionality: " + dimensions.length );

		return new ImagePlusContainer< T, CharArray >( this, new CharArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, DoubleArray > createDoubleInstance( int[] dimensions, final int entitiesPerPixel )
	{
		if ( dimensions.length > 5 )
			throw new RuntimeException( "Unsupported dimensionality: " + dimensions.length );

		return new ImagePlusContainer< T, DoubleArray >( this, new DoubleArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, FloatArray > createFloatInstance( int[] dimensions, final int entitiesPerPixel )
	{
		if ( dimensions.length > 5 )
			throw new RuntimeException( "Unsupported dimensionality: " + dimensions.length );

		return new FloatImagePlus< T >( this, dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, IntArray > createIntInstance( int[] dimensions, final int entitiesPerPixel )
	{
		if ( dimensions.length > 5 )
			throw new RuntimeException( "Unsupported dimensionality: " + dimensions.length );

		return new IntImagePlus< T >( this, dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, LongArray > createLongInstance( int[] dimensions, final int entitiesPerPixel )
	{
		if ( dimensions.length > 5 )
			throw new RuntimeException( "Unsupported dimensionality: " + dimensions.length );

		return new ImagePlusContainer< T, LongArray >( this, new LongArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, ShortArray > createShortInstance( int[] dimensions, final int entitiesPerPixel )
	{
		if ( dimensions.length > 5 )
			throw new RuntimeException( "Unsupported dimensionality: " + dimensions.length );

		return new ShortImagePlus< T >( this, dimensions, entitiesPerPixel );
	}
}
