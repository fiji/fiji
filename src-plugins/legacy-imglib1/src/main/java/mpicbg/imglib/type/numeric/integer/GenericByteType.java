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
import mpicbg.imglib.container.basictypecontainer.ByteAccess;
import mpicbg.imglib.container.basictypecontainer.array.ByteArray;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public abstract class GenericByteType<T extends GenericByteType<T>> extends IntegerTypeImpl<T>
{
	// the DirectAccessContainer
	final DirectAccessContainer<T, ? extends ByteAccess> storage;
	
	// the (sub)DirectAccessContainer that holds the information 
	ByteAccess b;
	
	// this is the constructor if you want it to read from an array
	public GenericByteType( DirectAccessContainer<T, ? extends ByteAccess> byteStorage )
	{
		storage = byteStorage;
	}
	
	// this is the constructor if you want it to be a variable
	protected GenericByteType( final byte value )
	{
		storage = null;
		b = new ByteArray( 1 );
		setValue( value );
	}

	// this is the constructor if you want it to be a variable
	protected GenericByteType() { this( ( byte )0 ); }
			
	@Override
	public void updateContainer( final Cursor< ? > c ) 
	{ 
		b = storage.update( c ); 
	}
	
	protected byte getValue(){ return b.getValue( i ); }
	protected void setValue( final byte f ){ b.setValue( i, f ); }
	
	@Override
	public void mul( final float c )
	{
		final byte a = getValue();
		setValue( ( byte )Util.round( a * c ) );
	}

	@Override
	public void mul( final double c )
	{
		final byte a = getValue();
		setValue( ( byte )Util.round( a * c ) );
	}

	@Override
	public void add( final T c )
	{
		final byte a = getValue( );
		setValue( ( byte )( a + c.getValue() ) );
	}

	@Override
	public void div( final T c )
	{
		final byte a = getValue();
		setValue( ( byte )( a / c.getValue() ) );
	}

	@Override
	public void mul( final T c )
	{
		final byte a = getValue( );
		setValue( ( byte )( a * c.getValue() ) );
	}

	@Override
	public void sub( final T c )
	{
		final byte a = getValue( );
		setValue( ( byte )( a - c.getValue() ) );
	}

	@Override
	public int compareTo( final T c ) 
	{ 
		final byte a = getValue();
		final byte b = c.getValue();
		if ( a > b )
			return 1;
		else if ( a < b )
			return -1;
		else 
			return 0;
	}
	
	@Override
	public void set( final T c )
	{
		setValue( c.getValue() );
	}

	@Override
	public void setOne() { setValue( ( byte )1 ); }

	@Override
	public void setZero() { setValue( ( byte )0 ); }

	@Override
	public void inc()
	{
		byte a = getValue();
		setValue( ++a );
	}

	@Override
	public void dec()
	{
		byte a = getValue();
		setValue( --a );
	}
	
	@Override
	public String toString() { return "" + getValue(); }
}
