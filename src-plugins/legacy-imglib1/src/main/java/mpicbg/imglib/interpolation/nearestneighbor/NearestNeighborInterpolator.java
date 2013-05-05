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

package mpicbg.imglib.interpolation.nearestneighbor;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.InterpolatorImpl;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class NearestNeighborInterpolator<T extends Type<T>> extends InterpolatorImpl<T>
{
	final LocalizableByDimCursor<T> cursor;
	
	protected NearestNeighborInterpolator( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		super(img, interpolatorFactory, outOfBoundsStrategyFactory);
		
		cursor = img.createLocalizableByDimCursor( outOfBoundsStrategyFactory );
		
		moveTo( position );		
	}

	@Override
	public void close() { cursor.close(); }

	@Override
	public T getType() { return cursor.getType(); }

	@Override
	public void moveTo( final float[] position )
	{
		for ( int d = 0; d < numDimensions; d++ )
		{
			this.position[ d ] = position[d];
			
			//final int pos = (int)( position[d] + (0.5f * Math.signum( position[d] ) ) );
			final int pos = Util.round( position[ d ] );
			cursor.move( pos - cursor.getPosition(d), d );
		}
	}

	@Override
	public void moveRel( final float[] vector )
	{
		for ( int d = 0; d < numDimensions; d++ )
		{
			this.position[ d ] += vector[ d ];
			
			//final int pos = (int)( position[d] + (0.5f * Math.signum( position[d] ) ) );
			final int pos = Util.round( position[ d ] );			
			cursor.move( pos - cursor.getPosition(d), d );
		}
	}
	
	@Override
	public void setPosition( final float[] position )
	{
		for ( int d = 0; d < numDimensions; d++ )
		{
			this.position[ d ] = position[d];

			//final int pos = (int)( position[d] + (0.5f * Math.signum( position[d] ) ) );
			final int pos = Util.round( position[ d ] );
			cursor.setPosition( pos, d );
		}
	}
}
