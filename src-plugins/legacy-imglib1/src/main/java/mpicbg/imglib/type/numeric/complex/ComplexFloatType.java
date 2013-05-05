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

package mpicbg.imglib.type.numeric.complex;

import mpicbg.imglib.algorithm.Precision.PrecisionReal;
import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.type.numeric.ComplexType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ComplexFloatType extends ComplexTypeImpl<ComplexFloatType> implements ComplexType<ComplexFloatType>
{
	// the DirectAccessContainer
	final DirectAccessContainer<ComplexFloatType, ? extends FloatAccess> storage;
	
	// the (sub)DirectAccessContainer that holds the information 
	FloatAccess b;
	
	// this is the constructor if you want it to read from an array
	public ComplexFloatType( DirectAccessContainer<ComplexFloatType, ? extends FloatAccess> complexfloatStorage )
	{
		storage = complexfloatStorage;
	}
	
	// this is the constructor if you want it to be a variable
	public ComplexFloatType( final float real, final float complex )
	{
		storage = null;
		b = new FloatArray( 2 );
		set( real, complex );
	}

	// this is the constructor if you want it to be a variable
	public ComplexFloatType() { this( 0, 0 ); }

	@Override
	public DirectAccessContainer<ComplexFloatType, ? extends FloatAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )
	{
		// create the container
		final DirectAccessContainer<ComplexFloatType, ? extends FloatAccess> container = storageFactory.createFloatInstance( dim, 2 );
		
		// create a Type that is linked to the container
		final ComplexFloatType linkedType = new ComplexFloatType( container );
		
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
	public ComplexFloatType duplicateTypeOnSameDirectAccessContainer() { return new ComplexFloatType( storage ); }
	
	@Override
	public float getRealFloat() { return b.getValue( realI ); }
	@Override
	public double getRealDouble() { return b.getValue( realI ); }
	@Override
	public float getComplexFloat() { return b.getValue( complexI ); }
	@Override
	public double getComplexDouble() { return b.getValue( complexI ); }
	
	@Override
	public void setReal( final float real ){ b.setValue( realI, real ); }
	@Override
	public void setReal( final double real ){ b.setValue( realI, (float)real ); }
	@Override
	public void setComplex( final float complex ){ b.setValue( complexI, complex ); }
	@Override
	public void setComplex( final double complex ){ b.setValue( complexI, (float)complex ); }
	
	@Override
	public PrecisionReal getPreferredRealPrecision() { return PrecisionReal.Float; }

	public void set( final float real, final float complex ) 
	{ 
		b.setValue( realI, real );
		b.setValue( complexI, complex );
	}

	@Override
	public void add( final ComplexFloatType c ) 
	{
		setReal( getRealFloat() + c.getRealFloat() );
		setComplex( getComplexFloat() + c.getComplexFloat() );
	}

	@Override
	public void div( final ComplexFloatType c ) 
	{ 
		final float a1 = getRealFloat(); 
		final float b1 = getComplexFloat();
		final float c1 = c.getRealFloat();
		final float d1 = c.getComplexFloat();
		
		setReal( ( a1*c1 + b1*d1 ) / ( c1*c1 + d1*d1 ) );
		setComplex( ( b1*c1 - a1*d1 ) / ( c1*c1 + d1*d1 ) );
	}

	@Override
	public void mul( final ComplexFloatType t ) 
	{
		// a + bi
		final float a = getRealFloat(); 
		final float b = getComplexFloat();
		
		// c + di
		final float c = t.getRealFloat();
		final float d = t.getComplexFloat();
		
		setReal( a*c - b*d ); 
		setComplex( a*d + b*c ); 
	}

	@Override
	public void sub( final ComplexFloatType c ) 
	{ 
		setReal( getRealFloat() - c.getRealFloat() ); 
		setComplex( getComplexFloat() - c.getComplexFloat() ); 
	}
	
	public void complexConjugate(){ setComplex( -getComplexFloat() ); }
	
	public void switchRealComplex()
	{
		final float a = getRealFloat();
		setReal( getComplexFloat() );
		setComplex( a );
	}

	@Override
	public void set( final ComplexFloatType c ) 
	{ 
		setReal( c.getRealFloat() );
		setComplex( c.getComplexFloat() );
	}

	@Override
	public ComplexFloatType[] createArray1D(int size1){ return new ComplexFloatType[ size1 ]; }

	@Override
	public ComplexFloatType[][] createArray2D(int size1, int size2){ return new ComplexFloatType[ size1 ][ size2 ]; }

	@Override
	public ComplexFloatType[][][] createArray3D(int size1, int size2, int size3) { return new ComplexFloatType[ size1 ][ size2 ][ size3 ]; }
	
	@Override
	public ComplexFloatType createVariable(){ return new ComplexFloatType( 0, 0 ); }
	
	@Override
	public ComplexFloatType copy(){ return new ComplexFloatType( getRealFloat(), getComplexFloat() ); }
}
