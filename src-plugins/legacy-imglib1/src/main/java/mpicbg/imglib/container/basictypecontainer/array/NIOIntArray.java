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

package mpicbg.imglib.container.basictypecontainer.array;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import mpicbg.imglib.container.basictypecontainer.IntAccess;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 * @author Rick Lentz
 */
public class NIOIntArray implements ArrayDataAccess<NIOIntArray>, IntAccess
{
	protected IntBuffer data;

	public NIOIntArray( final int numEntities )
	{
		this.data = ByteBuffer.allocateDirect(numEntities * 4).order( ByteOrder.nativeOrder() ).asIntBuffer();
	}
    		
	public NIOIntArray( final int[] data )
	{
		IntBuffer bufferIn = IntBuffer.wrap( data );
		IntBuffer copy = ByteBuffer.allocateDirect( bufferIn.capacity() ).order( ByteOrder.nativeOrder() ).asIntBuffer();
		this.data = copy.put( bufferIn );
	}

	@Override
	public void close() { data = null; }

	@Override
	public int getValue( final int index )
	{
		return data.get( index );
	}

	@Override
	public void setValue( final int index, final int value )
	{
		data.put(index, value);		
	}
	
	@Override
	public int[] getCurrentStorageArray()
	{ 		  
		int[] outData = new int[ data.capacity() ];
		data.get( outData );
		return outData;
	}
	
	@Override
	public NIOIntArray createArray( final int numEntities ) { return new NIOIntArray( numEntities ); }

}
