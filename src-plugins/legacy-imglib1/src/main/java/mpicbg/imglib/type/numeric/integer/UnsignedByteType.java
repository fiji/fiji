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
import mpicbg.imglib.container.basictypecontainer.ByteAccess;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class UnsignedByteType extends GenericByteType<UnsignedByteType>
{
	// this is the constructor if you want it to read from an array
	public UnsignedByteType( final DirectAccessContainer<UnsignedByteType, ? extends ByteAccess> byteStorage ) { super( byteStorage ); }

	// this is the constructor if you want it to be a variable
	public UnsignedByteType( final int value ) { super( getCodedSignedByteChecked(value) ); }

	// this is the constructor if you want it to be a variable
	public UnsignedByteType() { this( 0 ); }
	
	public static byte getCodedSignedByteChecked( int unsignedByte )
	{
		if ( unsignedByte < 0 )
			unsignedByte = 0;
		else if ( unsignedByte > 255 )
			unsignedByte = 255;
		
		return getCodedSignedByte( unsignedByte );
	}
	public static byte getCodedSignedByte( final int unsignedByte ) { return (byte)( unsignedByte & 0xff );	}
	public static int getUnsignedByte( final byte signedByte ) { return signedByte & 0xff; }

	@Override
	public DirectAccessContainer<UnsignedByteType, ? extends ByteAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )
	{
		// create the container
		final DirectAccessContainer<UnsignedByteType, ? extends ByteAccess> container = storageFactory.createByteInstance( dim, 1 );
		
		// create a Type that is linked to the container
		final UnsignedByteType linkedType = new UnsignedByteType( container );
		
		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );
		
		return container;
	}
	
	@Override
	public UnsignedByteType duplicateTypeOnSameDirectAccessContainer() { return new UnsignedByteType( storage ); }
	
	@Override
	public void mul( final float c )
	{
		
		final int a = getUnsignedByte( getValue() );
		setValue( getCodedSignedByte( Util.round( a * c ) ) );
	}

	@Override
	public void mul( final double c )
	{
		final int a = getUnsignedByte( getValue() );
		setValue( getCodedSignedByte( ( int )Util.round( a * c ) ) );
	}

	public int get(){ return getUnsignedByte( getValue() ); }
	public void set( final int f ){ setValue( getCodedSignedByte( f ) ); }
	
	@Override
	public int getInteger(){ return get(); }
	@Override
	public long getIntegerLong() { return get(); }
	@Override
	public void setInteger( final int f ){ set( f ); }
	@Override
	public void setInteger( final long f ){ set( (int)f ); }

	@Override
	public double getMaxValue() { return -Byte.MIN_VALUE + Byte.MAX_VALUE; }
	@Override
	public double getMinValue()  { return 0; }

	@Override
	public void div( final UnsignedByteType c )
	{
		set( get() / c.get() );
	}

	@Override
	public int compareTo( final UnsignedByteType c ) 
	{
		final int a = get();
		final int b = c.get();
		
		if ( a > b )
			return 1;
		else if ( a < b )
			return -1;
		else 
			return 0;
	}

	@Override
	public UnsignedByteType[] createArray1D( final int size1 ){ return new UnsignedByteType[ size1 ]; }

	@Override
	public UnsignedByteType[][] createArray2D( final int size1, final int size2 ){ return new UnsignedByteType[ size1 ][ size2 ]; }

	@Override
	public UnsignedByteType[][][] createArray3D( final int size1, final int size2, final int size3 ) { return new UnsignedByteType[ size1 ][ size2 ][ size3 ]; }

	@Override
	public UnsignedByteType createVariable(){ return new UnsignedByteType( 0 ); }

	@Override
	public UnsignedByteType copy(){ return new UnsignedByteType( get() ); }
}
