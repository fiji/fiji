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

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ByteType extends GenericByteType<ByteType>
{
	// this is the constructor if you want it to read from an array
	public ByteType( final DirectAccessContainer<ByteType, ? extends ByteAccess> byteStorage ) { super( byteStorage ); }
	
	// this is the constructor if you want it to be a variable
	public ByteType( final byte value ) { super( value ); }

	// this is the constructor if you want it to be a variable
	public ByteType() { super( (byte)0 ); }

	@Override
	public DirectAccessContainer<ByteType, ? extends ByteAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )
	{
		// create the container
		final DirectAccessContainer<ByteType, ? extends ByteAccess> container = storageFactory.createByteInstance( dim, 1 );
		
		// create a Type that is linked to the container
		final ByteType linkedType = new ByteType( container );
		
		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );
		
		return container;
	}
	
	@Override
	public ByteType duplicateTypeOnSameDirectAccessContainer() { return new ByteType( storage ); }

	public byte get() { return getValue(); }
	public void set( final byte b ) { setValue( b ); }
	
	@Override
	public int getInteger(){ return (int)get(); }
	@Override
	public long getIntegerLong() { return get(); }
	@Override
	public void setInteger( final int f ){ set( (byte)f ); }
	@Override
	public void setInteger( final long f ){ set( (byte)f ); }
	
	@Override
	public double getMaxValue() { return Byte.MAX_VALUE; }
	@Override
	public double getMinValue()  { return Byte.MIN_VALUE; }

	@Override
	public ByteType[] createArray1D( final int size1 ){ return new ByteType[ size1 ]; }

	@Override
	public ByteType[][] createArray2D( final int size1, final int size2 ){ return new ByteType[ size1 ][ size2 ]; }

	@Override
	public ByteType[][][] createArray3D( final int size1, final int size2, final int size3 ) { return new ByteType[ size1 ][ size2 ][ size3 ]; }

	@Override
	public ByteType createVariable(){ return new ByteType( (byte)0 ); }

	@Override
	public ByteType copy(){ return new ByteType( getValue() ); }
}
