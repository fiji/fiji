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

import mpicbg.imglib.algorithm.Precision.PrecisionReal;
import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class FloatType extends RealTypeImpl<FloatType> implements RealType<FloatType>
{
	// the DirectAccessContainer
	final DirectAccessContainer<FloatType, ? extends FloatAccess> storage;
	
	// the (sub)DirectAccessContainer that holds the information 
	FloatAccess b;
	
	// this is the constructor if you want it to read from an array
	public FloatType( DirectAccessContainer<FloatType, ? extends FloatAccess> floatStorage )
	{
		storage = floatStorage;
	}

	// this is the constructor if you want it to be a variable
	public FloatType( final float value )
	{
		storage = null;
		b = new FloatArray( 1 );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public FloatType() { this( 0 ); }

	@Override
	public DirectAccessContainer<FloatType, ? extends FloatAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )
	{
		// create the container
		final DirectAccessContainer<FloatType, ? extends FloatAccess> container = storageFactory.createFloatInstance( dim, 1 );
		
		// create a Type that is linked to the container
		final FloatType linkedType = new FloatType( container );
		
		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );
		
		return container;
	}

	@Override
	public void updateContainer( final Cursor<?> c ) 
	{ 
		b = storage.update( c ); 
	}
	
	@Override
	public FloatType duplicateTypeOnSameDirectAccessContainer() { return new FloatType( storage ); }

	public float get(){ return b.getValue( i ); }
	public void set( final float f ){ b.setValue( i, f ); }
	
	@Override
	public float getRealFloat() { return get(); }
	@Override
	public double getRealDouble() { return get(); }
	
	@Override
	public void setReal( final float real ){ set( real ); }
	@Override
	public void setReal( final double real ){ set( (float)real ); }
	
	@Override
	public PrecisionReal getPreferredRealPrecision() { return PrecisionReal.Float; }

	@Override
	public double getMaxValue() { return Float.MAX_VALUE; }
	@Override
	public double getMinValue()  { return -Float.MAX_VALUE; }
	@Override
	public double getMinIncrement()  { return Float.MIN_VALUE; }

	@Override
	public void mul( final float c )
	{
		set( get() * c );
	}

	@Override
	public void mul( final double c )
	{
		set( ( float )( get() * c ) );
	}
	
	@Override
	public void add( final FloatType c )
	{
		set( get() + c.get() );
	}

	@Override
	public void div( final FloatType c )
	{
		set( get() / c.get() );
	}

	@Override
	public void mul( final FloatType c )
	{
		set( get() * c.get() );
	}

	@Override
	public void sub( final FloatType c )
	{
		set( get() - c.get() );
	}

	@Override
	public int compareTo( final FloatType c ) 
	{ 
		final float a = get();
		final float b = c.get();
		if ( a > b )
			return 1;
		else if ( a < b )
			return -1;
		else 
			return 0;
	}
	
	@Override
	public void set( final FloatType c ){ set( c.get() ); }

	@Override
	public void setOne() { set( 1 ); }

	@Override
	public void setZero() { set( 0 ); }

	@Override
	public void inc()
	{
		float a = get();
		set( ++a );
	}

	@Override
	public void dec()
	{
		float a = get();
		set( --a );
	}

	@Override
	public FloatType[] createArray1D(int size1){ return new FloatType[ size1 ]; }

	@Override
	public FloatType[][] createArray2D(int size1, int size2){ return new FloatType[ size1 ][ size2 ]; }

	@Override
	public FloatType[][][] createArray3D(int size1, int size2, int size3) { return new FloatType[ size1 ][ size2 ][ size3 ]; }
	
	@Override
	public FloatType createVariable(){ return new FloatType( 0 ); }
	
	@Override
	public FloatType copy(){ return new FloatType( get() ); }
}
