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

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.DoubleAccess;
import mpicbg.imglib.container.basictypecontainer.array.DoubleArray;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.type.numeric.ComplexType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ComplexDoubleType extends ComplexTypeImpl<ComplexDoubleType> implements ComplexType<ComplexDoubleType>
{
	// the DirectAccessContainer
	final DirectAccessContainer<ComplexDoubleType, ? extends DoubleAccess> storage;
	
	// the (sub)DirectAccessContainer that holds the information 
	DoubleAccess b;
	
	// this is the constructor if you want it to read from an array
	public ComplexDoubleType( DirectAccessContainer<ComplexDoubleType, ? extends DoubleAccess> complexfloatStorage )
	{
		storage = complexfloatStorage;
	}
	
	// this is the constructor if you want it to be a variable
	public ComplexDoubleType( final double real, final double complex )
	{
		storage = null;
		b = new DoubleArray( 2 );
		set( real, complex );
	}

	// this is the constructor if you want it to be a variable
	public ComplexDoubleType() { this( 0, 0 ); }

	@Override
	public DirectAccessContainer<ComplexDoubleType, ? extends DoubleAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )
	{
		// create the container
		final DirectAccessContainer<ComplexDoubleType, ? extends DoubleAccess> container = storageFactory.createDoubleInstance( dim, 2 );
		
		// create a Type that is linked to the container
		final ComplexDoubleType linkedType = new ComplexDoubleType( container );
		
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
	public ComplexDoubleType duplicateTypeOnSameDirectAccessContainer() { return new ComplexDoubleType( storage ); }

	@Override
	public float getRealFloat() { return (float)b.getValue( realI ); }
	@Override
	public double getRealDouble() { return b.getValue( realI ); }
	@Override
	public float getComplexFloat() { return (float)b.getValue( complexI ); }
	@Override
	public double getComplexDouble() { return b.getValue( complexI ); }
	
	@Override
	public void setReal( final float real ){ b.setValue( realI, real ); }
	@Override
	public void setReal( final double real ){ b.setValue( realI, real ); }
	@Override
	public void setComplex( final float complex ){ b.setValue( complexI, complex ); }
	@Override
	public void setComplex( final double complex ){ b.setValue( complexI, complex ); }
	
	public void set( final double real, final double complex ) 
	{ 
		b.setValue( realI, real );
		b.setValue( complexI, complex );
	}

	@Override
	public void set( final ComplexDoubleType c ) 
	{ 
		setReal( c.getRealDouble() );
		setComplex( c.getComplexDouble() );
	}

	@Override
	public ComplexDoubleType[] createArray1D(int size1){ return new ComplexDoubleType[ size1 ]; }

	@Override
	public ComplexDoubleType[][] createArray2D(int size1, int size2){ return new ComplexDoubleType[ size1 ][ size2 ]; }

	@Override
	public ComplexDoubleType[][][] createArray3D(int size1, int size2, int size3) { return new ComplexDoubleType[ size1 ][ size2 ][ size3 ]; }
	
	@Override
	public ComplexDoubleType createVariable(){ return new ComplexDoubleType( 0, 0 ); }
	
	@Override
	public ComplexDoubleType copy(){ return new ComplexDoubleType( getRealFloat(), getComplexFloat() ); }	
}
