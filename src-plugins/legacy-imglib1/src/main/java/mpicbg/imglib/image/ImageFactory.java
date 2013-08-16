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

package mpicbg.imglib.image;

import mpicbg.imglib.Factory;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ImageFactory<T extends Type<T>> implements Factory
{
	final ContainerFactory containerFactory;
	
	final T type;
	String errorMessage = "No errors.";
	
	public ImageFactory( final T type, final ContainerFactory containerFactory )
	{
		this.containerFactory = containerFactory;
		this.type = type;
	}

	public ContainerFactory getContainerFactory() { return containerFactory; }
	
	public T createType() { return type.createVariable();	}

	public Image<T> createImage( final int dim[], final String name )
	{
		return new Image<T>( this, dim, name );
	}

	public Image<T> createImage( final int dim[] ) { return createImage( dim, null ); }
	
	@Override
	public void printProperties()
	{
		System.out.println( this.getClass().getCanonicalName() + ": " );
		System.out.println( "Use optimized containers if possible: " + containerFactory.useOptimizedContainers() );
		containerFactory.printProperties();	
	}
	
	@Override
	public String getErrorMessage() { return errorMessage; }

	@Override
	public void setParameters(String configuration) {}	
	
	
	public void setOptimizedContainerUse ( final boolean useOptimizedContainers ) { containerFactory.setOptimizedContainerUse( useOptimizedContainers ); }
	public boolean useOptimizedContainers() { return containerFactory.useOptimizedContainers(); }
}
