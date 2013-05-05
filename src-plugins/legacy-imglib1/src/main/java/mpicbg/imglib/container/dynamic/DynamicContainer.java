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

package mpicbg.imglib.container.dynamic;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.DirectAccessContainerImpl;
import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizablePlaneCursor;
import mpicbg.imglib.cursor.dynamic.DynamicCursor;
import mpicbg.imglib.cursor.dynamic.DynamicLocalizableByDimCursor;
import mpicbg.imglib.cursor.dynamic.DynamicLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.dynamic.DynamicLocalizableCursor;
import mpicbg.imglib.cursor.dynamic.DynamicLocalizablePlaneCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public abstract class DynamicContainer<T extends Type<T>, A extends DynamicContainerAccessor> extends DirectAccessContainerImpl<T, A>
{
	final protected int[] step;
	
	// we have to overwrite those as this can change during the processing
	protected int numPixels, numEntities;
	
	public DynamicContainer( final DynamicContainerFactory factory, final int[] dim, final int entitiesPerPixel )
	{
		super( factory, dim, entitiesPerPixel );
		
		this.step = Array.createAllocationSteps( dim );
		this.numPixels = super.numPixels; 
		this.numEntities = super.numEntities; 
	}
	
	public int[] getSteps() { return step.clone(); }
	public int getStep( final int dim ) { return step[ dim ]; }
	public final int getPos( final int[] l ) 
	{ 
		int i = l[ 0 ];
		for ( int d = 1; d < numDimensions; ++d )
			i += l[ d ] * step[ d ];
		
		return i;
	}	
	
	/**
	 * Creates a Cursor-specific Accessor reading from the ArrayList,
	 * the Cursor creates it himself in his constructor
	 * @return
	 */
	public abstract A createAccessor();

	@Override
	public int getNumEntities() { return numEntities; }

	@Override
	public int getNumPixels() { return numPixels; }

	@Override
	public DynamicCursor<T> createCursor( final Image<T> image )
	{
		// create a Cursor using a Type that is linked to the container
		return new DynamicCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image )
	{
		// create a Cursor using a Type that is linked to the container
		return new DynamicLocalizableByDimCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory )
	{
		// create a Cursor using a Type that is linked to the container
		return new DynamicLocalizableByDimOutOfBoundsCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer(), outOfBoundsFactory );
	}

	@Override
	public DynamicLocalizableCursor<T> createLocalizableCursor( final Image<T> image )
	{
		// create a Cursor using a Type that is linked to the container
		return new DynamicLocalizableCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );	
	}

	@Override
	public LocalizablePlaneCursor<T> createLocalizablePlaneCursor( final Image<T> image )
	{
		// create a Cursor using a Type that is linked to the container
		return new DynamicLocalizablePlaneCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}
	
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
}
