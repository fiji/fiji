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

package mpicbg.imglib.outofbounds;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class OutOfBoundsStrategyPeriodic<T extends Type<T>> extends OutOfBoundsStrategy<T>
{
	final LocalizableCursor<T> parentCursor;
	final LocalizableByDimCursor<T> circleCursor;
	final T type, circleType;
	final int numDimensions;
	final int[] dimension, position, circledPosition;
	
	public OutOfBoundsStrategyPeriodic( final LocalizableCursor<T> parentCursor )
	{
		super( parentCursor );
		
		this.parentCursor = parentCursor;
		this.circleCursor = parentCursor.getImage().createLocalizableByDimCursor();
		this.circleType = circleCursor.getType();
		this.type = circleType.createVariable();
			
		this.numDimensions = parentCursor.getImage().getNumDimensions();
		this.dimension = parentCursor.getImage().getDimensions();
		this.position = new int[ numDimensions ];
		this.circledPosition = new int[ numDimensions ];
	}

	@Override
	public T getType(){ return type; }
	
	@Override
	final public void notifyOutOfBOunds()
	{
		parentCursor.getPosition( position );
		getCircleCoordinate( position, circledPosition, dimension, numDimensions );		
		circleCursor.setPosition( circledPosition );

		type.set( circleType );
	}

	@Override
	public void notifyOutOfBOunds( final int steps, final int dim ) 
	{
		final int oldPos = circleCursor.getPosition( dim ); 
		
		circleCursor.move( getCircleCoordinateDim( oldPos + steps, dimension[ dim ] )  - oldPos, dim );
		type.set( circleType );
	}
	
	@Override
	public void notifyOutOfBOundsFwd( final int dim ) 
	{		
		final int oldPos = circleCursor.getPosition( dim ); 
		
		circleCursor.move( getCircleCoordinateDim( oldPos + 1, dimension[ dim ] )  - oldPos, dim );
		type.set( circleType );
	}

	@Override
	public void notifyOutOfBOundsBck( final int dim ) 
	{
		final int oldPos = circleCursor.getPosition( dim ); 
		
		circleCursor.move( getCircleCoordinateDim( oldPos - 1, dimension[ dim ] )  - oldPos, dim );
		type.set( circleType );
	}
	
	@Override
	public void initOutOfBOunds() 
	{ 
		parentCursor.getPosition( position );
		getCircleCoordinate( position, circledPosition, dimension, numDimensions );		
		circleCursor.setPosition( circledPosition );

		type.set( circleType );
	}
	
	final private static int getCircleCoordinateDim( final int pos, final int dim )
	{		
		if ( pos > -1 )
			return pos % dim;
		else
			return (dim-1) + (pos+1) % dim;		
	}
	
	final private static void getCircleCoordinate( final int[] position, final int circledPosition[], final int[] dimensions, final int numDimensions )
	{
		for ( int d = 0; d < numDimensions; d++ )
			circledPosition[ d ] = getCircleCoordinateDim( position[ d ], dimensions[ d ] );
	}
	
	@Override
	public void close()
	{
		circleCursor.close();
	}
}
