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

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.BitAccess;
import mpicbg.imglib.container.basictypecontainer.array.BitArray;
import mpicbg.imglib.cursor.Cursor;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class Unsigned12BitType extends IntegerTypeImpl<Unsigned12BitType>
{
	// the DirectAccessContainer
	final DirectAccessContainer<Unsigned12BitType, ? extends BitAccess> storage;

	// the adresses of the bits that we store
	int j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12;

	// the (sub)DirectAccessContainer that holds the information 
	BitAccess b;
	
	// this is the constructor if you want it to read from an array
	public Unsigned12BitType( DirectAccessContainer<Unsigned12BitType, ? extends BitAccess> bitStorage )
	{
		storage = bitStorage;
		updateIndex( 0 );
	}
	
	// this is the constructor if you want it to be a variable
	public Unsigned12BitType( final short value )
	{
		storage = null;
		updateIndex( 0 );
		b = new BitArray( 12 );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public Unsigned12BitType() { this( (short)0 ); }
	
	@Override
	public DirectAccessContainer<Unsigned12BitType, ? extends BitAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )
	{
		// create the container
		final DirectAccessContainer<Unsigned12BitType, ? extends BitAccess> container = storageFactory.createBitInstance( dim, 12 );
		
		// create a Type that is linked to the container
		final Unsigned12BitType linkedType = new Unsigned12BitType( container );
		
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
	public Unsigned12BitType duplicateTypeOnSameDirectAccessContainer() { return new Unsigned12BitType( storage ); }

	public short get() 
	{
		short value = 0;
		
		if ( b.getValue( j1 ) ) ++value;
		if ( b.getValue( j2 ) ) value += 2;
		if ( b.getValue( j3 ) ) value += 4;
		if ( b.getValue( j4 ) ) value += 8;
		if ( b.getValue( j5 ) ) value += 16;
		if ( b.getValue( j6 ) ) value += 32;
		if ( b.getValue( j7 ) ) value += 64;
		if ( b.getValue( j8 ) ) value += 128;
		if ( b.getValue( j9 ) ) value += 256;
		if ( b.getValue( j10 ) ) value += 512;
		if ( b.getValue( j11 ) ) value += 1024;
		if ( b.getValue( j12 ) ) value += 2048;
		
		return value; 
	}
	public void set( final short value ) 
	{
		b.setValue( j1, (value & 1) == 1 );
		b.setValue( j2, (value & 2) == 2 );
		b.setValue( j3, (value & 4) == 4 );
		b.setValue( j4, (value & 8) == 8 );
		b.setValue( j5, (value & 16) == 16 );
		b.setValue( j6, (value & 32) == 32 );
		b.setValue( j7, (value & 64) == 64 );
		b.setValue( j8, (value & 128) == 128 );
		b.setValue( j9, (value & 256) == 256 );
		b.setValue( j10, (value & 512) == 512 );
		b.setValue( j11, (value & 1024) == 1024 );
		b.setValue( j12, (value & 2048) == 2048 );		
	}

	@Override
	public int getInteger(){ return get(); }
	@Override
	public long getIntegerLong() { return get(); }
	@Override
	public void setInteger( final int f ) { set( (short)f ); }
	@Override
	public void setInteger( final long f ) { set( (short)f ); }

	@Override
	public double getMaxValue() { return 4095; }
	@Override
	public double getMinValue()  { return 0; }

	@Override
	public void updateIndex( final int i ) 
	{ 
		this.i = i;
		j1 = i * 12;
		j2 = j1 + 1;
		j3 = j1 + 2;
		j4 = j1 + 3;
		j5 = j1 + 4;
		j6 = j1 + 5;
		j7 = j1 + 6;
		j8 = j1 + 7;
		j9 = j1 + 8;
		j10 = j1 + 9;
		j11 = j1 + 10;
		j12 = j1 + 11;
	}
	
	@Override
	public void incIndex() 
	{ 
		++i;
		j1 += 12;
		j2 += 12;
		j3 += 12;
		j4 += 12;
		j5 += 12;
		j6 += 12;
		j7 += 12;
		j8 += 12;
		j9 += 12;
		j10 += 12;
		j11 += 12;
		j12 += 12;
	}
	@Override
	public void incIndex( final int increment ) 
	{ 
		i += increment; 
		
		final int inc12 = 12 * increment;		
		j1 += inc12;
		j2 += inc12;
		j3 += inc12;
		j4 += inc12;
		j5 += inc12;
		j6 += inc12;
		j7 += inc12;
		j8 += inc12;
		j9 += inc12;
		j10 += inc12;
		j11 += inc12;
		j12 += inc12;
	}
	@Override
	public void decIndex() 
	{ 
		--i;
		j1 -= 12;
		j2 -= 12;
		j3 -= 12;
		j4 -= 12;
		j5 -= 12;
		j6 -= 12;
		j7 -= 12;
		j8 -= 12;
		j9 -= 12;
		j10 -= 12;
		j11 -= 12;
		j12 -= 12;
	}
	@Override
	public void decIndex( final int decrement ) 
	{ 
		i -= decrement; 

		final int dec12 = 12 * decrement;		
		j1 -= dec12;
		j2 -= dec12;
		j3 -= dec12;
		j4 -= dec12;
		j5 -= dec12;
		j6 -= dec12;
		j7 -= dec12;
		j8 -= dec12;
		j9 -= dec12;
		j10 -= dec12;
		j11 -= dec12;
		j12 -= dec12;
	}

	
	@Override
	public Unsigned12BitType[] createArray1D(int size1){ return new Unsigned12BitType[ size1 ]; }

	@Override
	public Unsigned12BitType[][] createArray2D(int size1, int size2){ return new Unsigned12BitType[ size1 ][ size2 ]; }

	@Override
	public Unsigned12BitType[][][] createArray3D(int size1, int size2, int size3) { return new Unsigned12BitType[ size1 ][ size2 ][ size3 ]; }
	
	@Override
	public Unsigned12BitType createVariable(){ return new Unsigned12BitType(); }

	@Override
	public Unsigned12BitType copy(){ return new Unsigned12BitType( get() ); }
}
