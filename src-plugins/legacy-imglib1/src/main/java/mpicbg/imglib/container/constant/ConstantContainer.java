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

package mpicbg.imglib.container.constant;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.ContainerImpl;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.LocalizablePlaneCursor;
import mpicbg.imglib.cursor.constant.ConstantCursor;
import mpicbg.imglib.cursor.constant.ConstantLocalizableByDimCursor;
import mpicbg.imglib.cursor.constant.ConstantLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.constant.ConstantLocalizableCursor;
import mpicbg.imglib.cursor.constant.ConstantLocalizablePlaneCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * A simple container that has only one value and returns it at each location.
 * 
 * @param <T>
 *
 * @author Tobias Pietzsch
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ConstantContainer< T extends Type< T > > extends ContainerImpl< T >
{
	final T type;
	ConstantContainerFactory factory;
	
	/**
	 * 
	 * @param factory
	 * @param dim
	 * @param type
	 */
	public ConstantContainer( final ConstantContainerFactory factory, final int[] dim, final T type ) 
	{
		super( factory, dim );
		this.factory = factory;
		this.type = type;
	}

	public ConstantContainer( final int[] dim, final T type ) 
	{
		super( null, dim );
		
		this.factory = null;
		this.type = type;
	}

	@Override
	public ContainerFactory getFactory() 
	{ 
		if ( factory == null )
			this.factory = new ConstantContainerFactory();

		return this.factory; 
	}
	
	@Override
	public ConstantCursor<T> createCursor( final Image<T> image ) { return new ConstantCursor< T >( this, image, type ); }

	@Override
	public LocalizableCursor<T> createLocalizableCursor( final Image<T> image ) { return new ConstantLocalizableCursor< T >( this, image, type ); }

	@Override
	public LocalizablePlaneCursor<T> createLocalizablePlaneCursor( final Image<T> image ) { return new ConstantLocalizablePlaneCursor< T >( this, image, type ); }

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor(Image<T> image) { return new ConstantLocalizableByDimCursor< T >( this, image, type); }

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory )
	{
		return new ConstantLocalizableByDimOutOfBoundsCursor< T >( this, image, type, outOfBoundsFactory );
	}

	@Override
	public void close() {}

	@Override
	public boolean compareStorageContainerCompatibility( Container<?> img ) { return false; }

}
