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

import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.Interpolator3D;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class NearestNeighborInterpolator3D<T extends Type<T>> extends NearestNeighborInterpolator<T> implements Interpolator3D<T>
{
	//final LocalizableByDimCursor<T> cursor;
	//final T type;
	
	float x, y, z;
	
	protected NearestNeighborInterpolator3D( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		super( img, interpolatorFactory, outOfBoundsStrategyFactory );
		
		//cursor = img.createLocalizableByDimCursor( outOfBoundsStrategyFactory );
		//type = cursor.getType();
		
		x = 0;
		y = 0;
		z = 0;		
	}

	/**
	 * Returns the typed image the interpolator is working on
	 * 
	 * @return - the image
	 */
	@Override
	public Image<T> getImage() { return img; }		
	
	@Override
	public void getPosition( final float[] position )
	{
		position[ 0 ] = x;
		position[ 1 ] = y;
		position[ 2 ] = z;
	}

	@Override
	public float[] getPosition() { return new float[]{ x, y, z }; }	
	
	@Override
	public void close() { cursor.close(); }
	
	@Override
	public void moveTo( final float x, final float y, final float z )
	{		
		this.x = x;
		this.y = y;
		this.z = z;
		
		final int ix = Util.round( x ); //(int)(x + (0.5f * Math.signum(x) )); 
		final int iy = Util.round( y ); //(int)(y + (0.5f * Math.signum(y) )); 
		final int iz = Util.round( z ); //(int)(z + (0.5f * Math.signum(z) )); 
		
		cursor.move( ix - cursor.getPosition( 0 ), 0 );
		cursor.move( iy - cursor.getPosition( 1 ), 1 );
		cursor.move( iz - cursor.getPosition( 2 ), 2 );
		
		/*
		
		for ( int d = 0; d < numDimensions; d++ )
		{
			this.position[ d ] = position[d];

			final int pos = Math.round( position[d] );
			cursor.move( pos - cursor.getPosition(d), d );
		}

		*/
	}

	@Override
	public void moveTo( final float[] position )
	{
		moveTo( position[0], position[1], position[2] );
	}

	@Override
	public void moveRel( final float x, final float y, final float z )
	{
		this.x += x;
		this.y += y;
		this.z += z;

		//cursor.move( (int)( this.x + (0.5f * Math.signum(this.x) ) ) - cursor.getPosition( 0 ), 0 );
		//cursor.move( (int)( this.y + (0.5f * Math.signum(this.y) ) ) - cursor.getPosition( 1 ), 1 );
		//cursor.move( (int)( this.z + (0.5f * Math.signum(this.z) ) ) - cursor.getPosition( 2 ), 2 );
		
		cursor.move( Util.round( this.x ) - cursor.getPosition( 0 ), 0 );
		cursor.move( Util.round( this.y ) - cursor.getPosition( 1 ), 1 );
		cursor.move( Util.round( this.z ) - cursor.getPosition( 2 ), 2 );
		
		/*
		for ( int d = 0; d < numDimensions; d++ )
		{
			this.position[ d ] += vector[ d ];
			
			final int pos = Math.round( position[d] );			
			cursor.move( pos - cursor.getPosition(d), d );
		}
		*/
	}
	
	@Override
	public void moveRel( final float[] vector )
	{
		moveRel( vector[0], vector[1], vector[2] );
	}
	
	@Override
	public void setPosition( final float x, final float y, final float z )
	{
		this.x = x;
		this.y = y;
		this.z = z;

		//cursor.setPosition( (int)(x + (0.5f * Math.signum(x) ) ), 0 );
		//cursor.setPosition( (int)(y + (0.5f * Math.signum(y) ) ), 1 );
		//cursor.setPosition( (int)(z + (0.5f * Math.signum(z) ) ), 2 );
		
		cursor.setPosition( Util.round( x ), 0 );
		cursor.setPosition( Util.round( y ), 1 );
		cursor.setPosition( Util.round( z ), 2 );
		
		/*
		for ( int d = 0; d < numDimensions; d++ )
		{
			this.position[ d ] = position[d];

			final int pos = Math.round( position[d] );
			cursor.setPosition( pos, d );
		}
		*/
	}
	
	@Override
	public void setPosition( final float[] position )
	{
		setPosition( position[0], position[1], position[2] );
	}

	@Override
	public float getX() { return x;	}

	@Override
	public float getY() { return y; }

	@Override
	public float getZ() { return z;	}
}
