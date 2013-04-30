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

import mpicbg.imglib.container.Container3D;
import mpicbg.imglib.container.basictypecontainer.DataAccess;
import mpicbg.imglib.cursor.array.Array3DLocalizableByDimCursor;
import mpicbg.imglib.cursor.array.Array3DLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.array.Array3DLocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class Array3D<T extends Type<T>, A extends DataAccess> extends Array<T,A> implements Container3D<T>
{
	final int width, height, depth;
	
	public Array3D( final ArrayContainerFactory factory, final A data, final int width, final int height, final int depth, final int entitiesPerPixel )
	{
		super( factory, data, new int[]{ width, height, depth }, entitiesPerPixel );

		this.width = dim[ 0 ];
		this.height = dim[ 1 ];
		this.depth = dim[ 2 ];
	}

	@Override
	public Array3DLocalizableCursor<T> createLocalizableCursor( final Image<T> image ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		Array3DLocalizableCursor<T> c = new Array3DLocalizableCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}

	@Override
	public Array3DLocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		Array3DLocalizableByDimCursor<T> c = new Array3DLocalizableByDimCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		return c;
	}

	@Override
	public Array3DLocalizableByDimOutOfBoundsCursor<T> createLocalizableByDimCursor( final Image<T> image, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory ) 
	{ 
		// create a Cursor using a Type that is linked to the container
		Array3DLocalizableByDimOutOfBoundsCursor<T> c = new Array3DLocalizableByDimOutOfBoundsCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer(), outOfBoundsFactory );
		return c;
	}
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getDepth() { return depth; }

	public int getPos( final int x, final int y, final int z )
	{
		return (x + width * (y + z * height));
	}
}
