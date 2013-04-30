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

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.array.BitArray;
import mpicbg.imglib.container.basictypecontainer.array.ByteArray;
import mpicbg.imglib.container.basictypecontainer.array.CharArray;
import mpicbg.imglib.container.basictypecontainer.array.DoubleArray;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.basictypecontainer.array.IntArray;
import mpicbg.imglib.container.basictypecontainer.array.LongArray;
import mpicbg.imglib.container.basictypecontainer.array.ShortArray;
import mpicbg.imglib.type.Type;

/**
 * Factory that creates an appropriate {@link PlanarContainer}.
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
public class PlanarContainerFactory extends DirectAccessContainerFactory
{
	@Override
	public < T extends Type< T > > DirectAccessContainer< T, BitArray > createBitInstance( int[] dimensions, final int entitiesPerPixel )
	{
		return new PlanarContainer< T, BitArray >( this, new BitArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, ByteArray > createByteInstance( final int[] dimensions, final int entitiesPerPixel )
	{
		return new PlanarContainer< T , ByteArray >( this, new ByteArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, CharArray > createCharInstance( int[] dimensions, final int entitiesPerPixel )
	{
		return new PlanarContainer< T, CharArray >( this, new CharArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, DoubleArray > createDoubleInstance( int[] dimensions, final int entitiesPerPixel )
	{
		return new PlanarContainer< T, DoubleArray >( this, new DoubleArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, FloatArray > createFloatInstance( int[] dimensions, final int entitiesPerPixel )
	{
		return new PlanarContainer< T, FloatArray >( this, new FloatArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, IntArray > createIntInstance( int[] dimensions, final int entitiesPerPixel )
	{
		return new PlanarContainer< T, IntArray >( this, new IntArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, LongArray > createLongInstance( int[] dimensions, final int entitiesPerPixel )
	{
		return new PlanarContainer< T, LongArray >( this, new LongArray( 1 ), dimensions, entitiesPerPixel );
	}

	@Override
	public < T extends Type< T > > DirectAccessContainer< T, ShortArray > createShortInstance( int[] dimensions, final int entitiesPerPixel )
	{
		return new PlanarContainer< T, ShortArray >( this, new ShortArray( 1 ), dimensions, entitiesPerPixel );
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
	public void setParameters( String configuration )
	{
	// TODO Auto-generated method stub

	}

}
