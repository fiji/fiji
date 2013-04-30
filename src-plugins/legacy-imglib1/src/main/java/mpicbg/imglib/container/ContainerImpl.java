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

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public abstract class ContainerImpl<T extends Type<T>> implements Container<T>
{
	final protected int numPixels, numDimensions;
	final protected long id;
	protected final int[] dim;
	
	final ContainerFactory factory;

	public ContainerImpl( final ContainerFactory factory, int[] dim )
	{
		this.numDimensions = dim.length;
		
		this.numPixels = getNumPixels(dim);
		
		this.dim = dim.clone();
		this.factory = factory;
		this.id = Image.createUniqueId();
	}
	
	public static int getNumPixels( final int[] dim )
	{
		int numPixels = 1;		
		
		for (int i = 0; i < dim.length; i++)
			numPixels *= dim[i];
		
		return numPixels;		
	}
		
	@Override
	public ContainerFactory getFactory() { return factory; }
	
	@Override
	public long getId(){ return id; }
	@Override
	public int getNumDimensions() { return dim.length; }
	@Override
	public int[] getDimensions() { return dim.clone(); }
	
	@Override
	public void getDimensions( final int[] dimensions )
	{
		for (int i = 0; i < numDimensions; i++)
			dimensions[i] = this.dim[i];
	}

	@Override
	public int getDimension( final int dim )
	{
		if ( dim < numDimensions && dim > -1 )
			return this.dim[ dim ];
		else
			return 1;		
	}
	
	@Override
	public int getNumPixels() { return numPixels; }

	@Override
	public String toString()
	{
		String className = this.getClass().getCanonicalName();
		className = className.substring( className.lastIndexOf(".") + 1, className.length());
		
		String description = className + ", id '" + getId() + "' [" + dim[ 0 ];
		
		for ( int i = 1; i < numDimensions; i++ )
			description += "x" + dim[ i ];
		
		description += "]";
		
		return description;
	}
	
	@Override
	public boolean compareStorageContainerDimensions( final Container<?> container )
	{
		if ( container.getNumDimensions() != this.getNumDimensions() )
			return false;
		
		for ( int i = 0; i < numDimensions; i++ )
			if ( this.dim[i] != container.getDimensions()[i])
				return false;
		
		return true;
	}		

	/*
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
	*/
}
