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

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Saalfeld
 * @author Stephan Preibisch
 * @author Albert Cardona
 */
public class ShapeListContainerFactory extends ContainerFactory
{
	boolean useCaching = false;
	int cacheSize = 32;
	
	public ShapeListContainerFactory() {}
	public ShapeListContainerFactory( final int cacheSize )
	{
		this.useCaching = true;
		this.cacheSize = cacheSize;
	}
	
	public void setCaching( final boolean useCaching ) { this.useCaching = useCaching; }
	public void setCacheSize( final int cacheSize ) { this.cacheSize = cacheSize; }

	public boolean getCaching() { return useCaching; }
	public int getCacheSize() { return cacheSize; }

	/**
	 * This method is called by {@link Image}. The {@link ContainerFactory} can decide how to create the {@link Container},
	 * if it is for example a {@link DirectAccessContainerFactory} it will ask the {@link Type} to create a 
	 * suitable {@link Container} for the {@link Type} and the dimensionality
	 * 
	 * @return {@link Container} - the instantiated Container
	 */
	public <T extends Type<T>> ShapeList<T> createContainer( final int[] dim, final T type )
	{
		if ( useCaching )
			return new ShapeListCached<T>( this, dim, type );
		else
			return new ShapeList<T>( this, dim, type );
	}

	@Override
	public String getErrorMessage()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see mpicbg.imglib.Factory#printProperties()
	 */
	@Override
	public void printProperties()
	{
	// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see mpicbg.imglib.Factory#setParameters(java.lang.String)
	 */
	@Override
	public void setParameters( String configuration )
	{
	// TODO Auto-generated method stub

	}

}
