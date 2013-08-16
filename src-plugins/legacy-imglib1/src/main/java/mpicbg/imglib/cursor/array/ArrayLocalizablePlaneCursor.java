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

package mpicbg.imglib.cursor.array;

import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.cursor.LocalizablePlaneCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ArrayLocalizablePlaneCursor<T extends Type<T>> extends ArrayLocalizableCursor<T> implements LocalizablePlaneCursor<T>
{
	protected int planeDimA, planeDimB, planeSizeA, planeSizeB, incPlaneA, incPlaneB, maxI;
	
	public ArrayLocalizablePlaneCursor( final Array<T,?> container, final Image<T> image, final T type ) 
	{
		super( container, image, type );
	}	
	
	@Override 
	public boolean hasNext()
	{
		return type.getIndex() < maxI;
		
		/*if ( type.i < maxI )
			return true;
		else
			return false;*/
	}
	
	@Override
	public void fwd()
	{ 
		if ( position[ planeDimA ] < dimensions[ planeDimA ] - 1)
		{
			position[ planeDimA ]++;
			type.incIndex( incPlaneA );
		}
		else if ( position[ planeDimB ] < dimensions[ planeDimB ] - 1)
		{
			position[ planeDimA ] = 0;
			position[ planeDimB ]++;
			type.incIndex( incPlaneB );
			type.decIndex( (planeSizeA - 1) * incPlaneA );
		}
	}
	
	@Override
	public void reset( final int planeDimA, final int planeDimB, final int[] dimensionPositions )
	{
		this.planeDimA = planeDimA;
		this.planeDimB = planeDimB;
		
		this.planeSizeA = container.getDimension( planeDimA );
		this.planeSizeB = container.getDimension( planeDimB );
		
		final int[] steps = Array.createAllocationSteps( container.getDimensions() );

		// store the current position
    	final int[] dimPos = dimensionPositions.clone();
		
		incPlaneA = steps[ planeDimA ];
		dimPos[ planeDimA ] = 0;
		
		if ( planeDimB > -1 && planeDimB < steps.length )
		{
			incPlaneB = steps[ planeDimB ];
			dimPos[ planeDimB ] = 0;
		}
		else
		{
			incPlaneB = 0;
		}

		setPosition( dimPos );		
		isClosed = false;
		
		type.decIndex( incPlaneA );					
		position[ planeDimA ] = -1;
		
		dimPos[ planeDimA ] = dimensions[ planeDimA ] - 1;		
		if ( planeDimB > -1 && planeDimB < steps.length )
			dimPos[ planeDimB ] = dimensions[ planeDimB ] - 1;
		
		maxI = container.getPos( dimPos );
		
		type.updateContainer( this );				
	}

	@Override
	public void reset( final int planeDimA, final int planeDimB )
	{
		if ( dimensions == null )
			return;

		reset( planeDimA, planeDimB, new int[ numDimensions ] );
	}
	
	@Override
	public void reset()
	{
		if ( dimensions == null )
			return;
		
		reset( 0, 1, new int[ numDimensions ] );		
	}

	@Override
	public void getPosition( int[] position )
	{
		for ( int d = 0; d < numDimensions; d++ )
			position[ d ] = this.position[ d ];
	}
	
	@Override
	public int[] getPosition(){ return position.clone(); }
	
	@Override
	public int getPosition( final int dim ){ return position[ dim ]; }
	
	protected void setPosition( final int[] position )
	{
		type.updateIndex( container.getPos( position ) );
		
		for ( int d = 0; d < numDimensions; d++ )
			this.position[ d ] = position[ d ];
	}
	
}
