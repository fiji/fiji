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

package mpicbg.imglib.type.numeric.real;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.Display;
import mpicbg.imglib.image.display.RealTypeDisplay;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.complex.ComplexTypeImpl;

/**
 * TODO
 *
 */
public abstract class RealTypeImpl<T extends RealTypeImpl<T>> extends ComplexTypeImpl<T> implements RealType<T>
{
	@Override
	public int getEntitiesPerPixel() { return 1; } 

	@Override
	public Display<T> getDefaultDisplay( final Image<T> image )
	{
		return new RealTypeDisplay<T>( image );
	}

	@Override
	public float getComplexFloat() { return 0; }
	@Override
	public double getComplexDouble() { return 0; }
	
	@Override
	public void setComplex( final float complex ){}
	@Override
	public void setComplex( final double complex ){}
	
	@Override
	public void inc() { setReal( getRealDouble() + 1 ); }
	@Override
	public void dec() { setReal( getRealDouble() - 1 ); }
	
	@Override
	public void set( final T c ){ setReal( c.getRealDouble() ); }
	
	@Override
	public void mul( final float c ) { setReal( getRealDouble() * c ); }

	@Override
	public void mul( final double c ) { setReal( getRealDouble() * c ); }
	
	@Override
	public void add( final T c ) { setReal( getRealDouble() + c.getRealDouble() ); }

	@Override
	public void div( final T c ) { setReal( getRealDouble() / c.getRealDouble() ); }

	@Override
	public void mul( final T c ) { setReal( getRealDouble() * c.getRealDouble() ); }

	@Override
	public void sub( final T c ) { setReal( getRealDouble() - c.getRealDouble() ); }
	
	@Override
	public void setZero() { setReal( 0 ); }
	@Override
	public void setOne() { setReal( 1 ); };	
	
	@Override
	public int compareTo( final T c ) 
	{ 
		final double a = getRealDouble();
		final double b = c.getRealDouble();
		if ( a > b )
			return 1;
		else if ( a < b )
			return -1;
		else 
			return 0;
	}
	@Override
	public float getPowerFloat() { return getRealFloat(); }

	@Override
	public double getPowerDouble() { return getRealDouble(); }
	
	@Override
	public float getPhaseFloat() { return 0; }
	
	@Override
	public double getPhaseDouble() { return 0; }
	
	@Override
	public void updateIndex( final int index ) { this.i = index; }
	
	@Override
	public void incIndex() { ++i; }
	@Override
	public void incIndex( final int increment ) { i += increment; }
	@Override
	public void decIndex() { --i; }
	@Override
	public void decIndex( final int decrement ) { i -= decrement; }	
	
	@Override
	public String toString() { return "" + getRealDouble(); }	
}
