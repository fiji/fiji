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
import mpicbg.imglib.container.basictypecontainer.IntAccess;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class UnsignedIntType extends GenericIntType<UnsignedIntType>
{
	// this is the constructor if you want it to read from an array
	public UnsignedIntType( DirectAccessContainer<UnsignedIntType, ? extends IntAccess> intStorage ) { super( intStorage ); }

	// this is the constructor if you want it to be a variable
	public UnsignedIntType( final long value ) { super( getCodedSignedIntChecked(value) ); }

	// this is the constructor if you want it to be a variable
	public UnsignedIntType() { this( 0 ); }
	
	public static int getCodedSignedIntChecked( long unsignedInt )
	{
		if ( unsignedInt < 0 )
			unsignedInt = 0;
		else if ( unsignedInt > 4294967295l )
			unsignedInt = 4294967295l;
		
		return getCodedSignedInt( unsignedInt );
	}
	public static int getCodedSignedInt( final long unsignedInt ) { return (int)( unsignedInt & 0xffffffff ); }
	public static long getUnsignedInt( final int signedInt ) { return signedInt & 0xffffffffL; }

	@Override
	public DirectAccessContainer<UnsignedIntType, ? extends IntAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )
	{
		// create the container
		final DirectAccessContainer<UnsignedIntType, ? extends IntAccess> container = storageFactory.createIntInstance( dim, 1 );
		
		// create a Type that is linked to the container
		final UnsignedIntType linkedType = new UnsignedIntType( container );
		
		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );
		
		return container;
	}
	
	@Override
	public UnsignedIntType duplicateTypeOnSameDirectAccessContainer() { return new UnsignedIntType( storage ); }

	@Override
	public void mul( final float c )
	{
		final long a = getUnsignedInt( getValue() );
		setValue( getCodedSignedInt( Util.round( a * c ) ) );
	}

	@Override
	public void mul( final double c )
	{
		final long a = getUnsignedInt( getValue() );
		setValue( getCodedSignedInt( ( int )Util.round( a * c ) ) );
	}

	public long get(){ return getUnsignedInt( getValue() ); }
	public void set( final long f ){ setValue( getCodedSignedInt( f ) ); }

	@Override
	public int getInteger(){ return (int)get(); }
	@Override
	public long getIntegerLong() { return get(); }
	@Override
	public void setInteger( final int f ){ set( f ); }
	@Override
	public void setInteger( final long f ){ set( f ); }

	@Override
	public double getMaxValue() { return -((double)Integer.MIN_VALUE) + Integer.MAX_VALUE; }
	@Override
	public double getMinValue()  { return 0; }
	
	@Override
	public void div( final UnsignedIntType c )
	{
		set( get() / c.get() );
	}

	@Override
	public int compareTo( final UnsignedIntType c ) 
	{
		final long a = get();
		final long b = c.get();
		
		if ( a > b )
			return 1;
		else if ( a < b )
			return -1;
		else 
			return 0;
	}

	@Override
	public UnsignedIntType[] createArray1D( final int size1 ){ return new UnsignedIntType[ size1 ]; }

	@Override
	public UnsignedIntType[][] createArray2D( final int size1, final int size2 ){ return new UnsignedIntType[ size1 ][ size2 ]; }

	@Override
	public UnsignedIntType[][][] createArray3D( final int size1, final int size2, final int size3 ) { return new UnsignedIntType[ size1 ][ size2 ][ size3 ]; }

	@Override
	public UnsignedIntType createVariable(){ return new UnsignedIntType( 0 ); }

	@Override
	public UnsignedIntType copy(){ return new UnsignedIntType( get() ); }
}
