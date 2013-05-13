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

package mpicbg.imglib.container;

import mpicbg.imglib.container.basictypecontainer.BitAccess;
import mpicbg.imglib.container.basictypecontainer.ByteAccess;
import mpicbg.imglib.container.basictypecontainer.CharAccess;
import mpicbg.imglib.container.basictypecontainer.DoubleAccess;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.IntAccess;
import mpicbg.imglib.container.basictypecontainer.LongAccess;
import mpicbg.imglib.container.basictypecontainer.ShortAccess;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 */
public abstract class DirectAccessContainerFactory extends PixelGridContainerFactory
{
	/**
	 * This method is called by {@link Image}. This class will ask the {@link Type} to create a 
	 * suitable {@link Container} for the {@link Type} and the dimensionality.
	 * 
	 * {@link Type} will then call one of the abstract methods defined below to create the 
	 * {@link DirectAccessContainer}
	 * 
	 * @return {@link Container} - the instantiated Container
	 */
	@Override
	public <T extends Type<T>> DirectAccessContainer<T,?> createContainer( final int[] dim, final T type )
	{
		return type.createSuitableDirectAccessContainer( this, dim );
	}

	// All basic Type containers
	public abstract <T extends Type<T>> DirectAccessContainer<T, ? extends BitAccess> createBitInstance( int[] dimensions, int entitiesPerPixel );
	public abstract <T extends Type<T>> DirectAccessContainer<T, ? extends ByteAccess> createByteInstance( int[] dimensions, int entitiesPerPixel );
	public abstract <T extends Type<T>> DirectAccessContainer<T, ? extends CharAccess> createCharInstance( int[] dimensions, int entitiesPerPixel );
	public abstract <T extends Type<T>> DirectAccessContainer<T, ? extends ShortAccess> createShortInstance( int[] dimensions, int entitiesPerPixel );
	public abstract <T extends Type<T>> DirectAccessContainer<T, ? extends IntAccess> createIntInstance( int[] dimensions, int entitiesPerPixel );
	public abstract <T extends Type<T>> DirectAccessContainer<T, ? extends LongAccess> createLongInstance( int[] dimensions, int entitiesPerPixel );
	public abstract <T extends Type<T>> DirectAccessContainer<T, ? extends FloatAccess> createFloatInstance( int[] dimensions, int entitiesPerPixel );
	public abstract <T extends Type<T>> DirectAccessContainer<T, ? extends DoubleAccess> createDoubleInstance( int[] dimensions, int entitiesPerPixel );
}
