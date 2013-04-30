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

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.PixelGridContainerImpl;
import mpicbg.imglib.container.basictypecontainer.BitAccess;
import mpicbg.imglib.container.basictypecontainer.ByteAccess;
import mpicbg.imglib.container.basictypecontainer.CharAccess;
import mpicbg.imglib.container.basictypecontainer.DoubleAccess;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.IntAccess;
import mpicbg.imglib.container.basictypecontainer.LongAccess;
import mpicbg.imglib.container.basictypecontainer.ShortAccess;
import mpicbg.imglib.container.basictypecontainer.array.BitArray;
import mpicbg.imglib.container.basictypecontainer.array.ByteArray;
import mpicbg.imglib.container.basictypecontainer.array.CharArray;
import mpicbg.imglib.container.basictypecontainer.array.DoubleArray;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.basictypecontainer.array.IntArray;
import mpicbg.imglib.container.basictypecontainer.array.LongArray;
import mpicbg.imglib.container.basictypecontainer.array.NIOByteArray;
import mpicbg.imglib.container.basictypecontainer.array.NIOCharArray;
import mpicbg.imglib.container.basictypecontainer.array.NIODoubleArray;
import mpicbg.imglib.container.basictypecontainer.array.NIOFloatArray;
import mpicbg.imglib.container.basictypecontainer.array.NIOIntArray;
import mpicbg.imglib.container.basictypecontainer.array.NIOLongArray;
import mpicbg.imglib.container.basictypecontainer.array.NIOShortArray;
import mpicbg.imglib.container.basictypecontainer.array.ShortArray;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ArrayContainerFactory extends DirectAccessContainerFactory
{
	protected boolean useNIO = false;

	public void setNIOUse ( final boolean useNIO ) {
		this.useNIO = useNIO;
	}
	public boolean useNIO() { return useNIO; }

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, BitAccess> createBitInstance( int[] dimensions, final int entitiesPerPixel)
	{
		if (useNIO) throw new IllegalStateException("Cannot create NIO bit arrays");

		final int numPixels = PixelGridContainerImpl.getNumEntities(dimensions, entitiesPerPixel);

		BitAccess access = new BitArray(numPixels);
		if ( dimensions.length == 3 && useOptimizedContainers )
			return new Array3D<T, BitAccess>( this, access, dimensions[0], dimensions[1], dimensions[2], entitiesPerPixel );
		else
			return new Array<T, BitAccess>( this, access, dimensions, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, ByteAccess> createByteInstance( final int[] dimensions, final int entitiesPerPixel)
	{
		final int numPixels = PixelGridContainerImpl.getNumEntities(dimensions, entitiesPerPixel);

		ByteAccess access;
		if (useNIO) access = new NIOByteArray(numPixels);
		else access = new ByteArray(numPixels);
		if ( dimensions.length == 3 && useOptimizedContainers )
			return new Array3D<T, ByteAccess>( this, access, dimensions[0], dimensions[1], dimensions[2], entitiesPerPixel );
		else
			return new Array<T, ByteAccess>( this, access, dimensions, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, CharAccess> createCharInstance(int[] dimensions, final int entitiesPerPixel)
	{
		final int numPixels = PixelGridContainerImpl.getNumEntities(dimensions, entitiesPerPixel);

		CharAccess access;
		if (useNIO) access = new NIOCharArray(numPixels);
		else access = new CharArray(numPixels);

		if ( dimensions.length == 3 && useOptimizedContainers )
			return new Array3D<T, CharAccess>( this, access, dimensions[0], dimensions[1], dimensions[2], entitiesPerPixel );
		else
			return new Array<T, CharAccess>( this, access, dimensions, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, DoubleAccess> createDoubleInstance(int[] dimensions, final int entitiesPerPixel)
	{
		final int numPixels = PixelGridContainerImpl.getNumEntities(dimensions, entitiesPerPixel);

		DoubleAccess access;
		if (useNIO) access = new NIODoubleArray(numPixels);
		else access = new DoubleArray(numPixels);
		if ( dimensions.length == 3 && useOptimizedContainers )
			return new Array3D<T, DoubleAccess>( this, access, dimensions[0], dimensions[1], dimensions[2], entitiesPerPixel );
		else
			return new Array<T, DoubleAccess>( this, access, dimensions, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, FloatAccess> createFloatInstance(int[] dimensions, final int entitiesPerPixel)
	{
		final int numPixels = PixelGridContainerImpl.getNumEntities(dimensions, entitiesPerPixel);

		FloatAccess access;
		if (useNIO) access = new NIOFloatArray(numPixels);
		else access = new FloatArray(numPixels);
		if ( dimensions.length == 3 && useOptimizedContainers )
			return new Array3D<T, FloatAccess>( this, access, dimensions[0], dimensions[1], dimensions[2], entitiesPerPixel );
		else
			return new Array<T, FloatAccess>( this, access, dimensions, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, IntAccess> createIntInstance(int[] dimensions, final int entitiesPerPixel)
	{
		final int numPixels = PixelGridContainerImpl.getNumEntities(dimensions, entitiesPerPixel);

		IntAccess access;
		if (useNIO) access = new NIOIntArray(numPixels);
		else access = new IntArray(numPixels);
		if ( dimensions.length == 3 && useOptimizedContainers )
			return new Array3D<T, IntAccess>( this, access, dimensions[0], dimensions[1], dimensions[2], entitiesPerPixel );
		else
			return new Array<T, IntAccess>( this, access, dimensions, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, LongAccess> createLongInstance(int[] dimensions, final int entitiesPerPixel)
	{
		final int numPixels = PixelGridContainerImpl.getNumEntities(dimensions, entitiesPerPixel);

		LongAccess access;
		if (useNIO) access = new NIOLongArray(numPixels);
		else access = new LongArray(numPixels);
		if ( dimensions.length == 3 && useOptimizedContainers )
			return new Array3D<T, LongAccess>( this, access, dimensions[0], dimensions[1], dimensions[2], entitiesPerPixel );
		else
			return new Array<T, LongAccess>( this, access, dimensions, entitiesPerPixel );
	}

	@Override
	public <T extends Type<T>> DirectAccessContainer<T, ShortAccess> createShortInstance(int[] dimensions, final int entitiesPerPixel)
	{
		final int numPixels = PixelGridContainerImpl.getNumEntities(dimensions, entitiesPerPixel);

		ShortAccess access;
		if (useNIO) access = new NIOShortArray(numPixels);
		else access = new ShortArray(numPixels);
		if ( dimensions.length == 3 && useOptimizedContainers )
			return new Array3D<T, ShortAccess>( this, access, dimensions[0], dimensions[1], dimensions[2], entitiesPerPixel );
		else
			return new Array<T, ShortAccess>( this, access, dimensions, entitiesPerPixel );
	}

	@Override
	public String getErrorMessage()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void printProperties()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setParameters(String configuration)
	{
		// TODO Auto-generated method stub

	}

}
