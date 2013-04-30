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

package mpicbg.imglib.type.numeric.integer;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.Display;
import mpicbg.imglib.image.display.IntegerTypeDisplay;
import mpicbg.imglib.type.numeric.IntegerType;
import mpicbg.imglib.type.numeric.real.RealTypeImpl;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 */
public abstract class IntegerTypeImpl<T extends IntegerTypeImpl<T>> extends RealTypeImpl<T> implements IntegerType<T>
{
	@Override
	public Display<T> getDefaultDisplay( final Image<T> image )
	{
		return new IntegerTypeDisplay<T>( image );
	}

	@Override
	public double getMinIncrement() { return 1; }

	@Override
	public float getRealFloat() { return (float) getIntegerLong(); }
	@Override
	public double getRealDouble() { return getIntegerLong(); }
	
	@Override
	public void setReal( final float real ){ setInteger( Util.round( real ) ); }
	@Override
	public void setReal( final double real ){ setInteger( Util.round( real ) ); }	

	@Override
	public void inc() { setInteger( getIntegerLong() + 1 ); }
	@Override
	public void dec() { setInteger( getIntegerLong() - 1 ); }

	@Override
	public void setZero() { setInteger( 0 ); }
	@Override
	public void setOne() { setInteger( 1 ); };	

	@Override
	public int compareTo( final T c ) 
	{ 
		final long a = getIntegerLong();
		final long b = c.getIntegerLong();
		if ( a > b )
			return 1;
		else if ( a < b )
			return -1;
		else 
			return 0;
	}
	
	@Override
	public String toString() { return "" + getIntegerLong(); }	
}
