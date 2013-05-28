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

package mpicbg.imglib.type.label;

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.BitAccess;
import mpicbg.imglib.container.basictypecontainer.array.BitArray;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.BasePairTypeDisplay;
import mpicbg.imglib.image.display.Display;
import mpicbg.imglib.type.BasePairType;
import mpicbg.imglib.type.TypeImpl;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class BasePairBitType extends TypeImpl<BasePairBitType> implements BasePairType<BasePairBitType>
{
	public static enum Base { gap, N, A, T, G, C; }

	@Override
	public int getEntitiesPerPixel() { return 1; } 

	// the DirectAccessContainer
	final DirectAccessContainer<BasePairBitType, ? extends BitAccess> storage;
	
	// the (sub)DirectAccessContainer that holds the information 
	BitAccess b;
	
	// the adresses of the bits that we store
	int j1, j2, j3;
	
	// this is the constructor if you want it to read from an array
	public BasePairBitType( DirectAccessContainer<BasePairBitType, ? extends BitAccess> bitStorage )
	{
		storage = bitStorage;
		updateIndex( 0 );
	}
	
	// this is the constructor if you want it to be a variable
	public BasePairBitType( final Base value )
	{
		storage = null;
		updateIndex( 0 );
		b = new BitArray( 3 );
		set( value );
	}	

	// this is the constructor if you want it to be a variable
	public BasePairBitType() { this( Base.N ); }
	
	@Override
	public DirectAccessContainer<BasePairBitType, ? extends BitAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )	
	{
		// create the container
		final DirectAccessContainer<BasePairBitType, ? extends BitAccess> container = storageFactory.createBitInstance( dim, 3 );
		
		// create a Type that is linked to the container
		final BasePairBitType linkedType = new BasePairBitType( container );
		
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
	public BasePairBitType duplicateTypeOnSameDirectAccessContainer() { return new BasePairBitType( storage ); }
	
	@Override
	public void updateIndex( final int i ) 
	{ 
		this.i = i;
		j1 = i * 3;
		j2 = j1 + 1;
		j3 = j1 + 2;
	}
	
	@Override
	public void incIndex() 
	{ 
		++i;
		j1 += 3;
		j2 += 3;
		j3 += 3;
	}
	@Override
	public void incIndex( final int increment ) 
	{ 
		i += increment; 
		
		final int inc3 = 3 * increment;		
		j1 += inc3;
		j2 += inc3;
		j3 += inc3;
	}
	@Override
	public void decIndex() 
	{ 
		--i;
		j1 -= 3;
		j2 -= 3;
		j3 -= 3;
	}
	@Override
	public void decIndex( final int decrement ) 
	{ 
		i -= decrement; 

		final int dec3 = 3 * decrement;		
		j1 -= dec3;
		j2 -= dec3;
		j3 -= dec3;
	}
	
	@Override
	public Display<BasePairBitType> getDefaultDisplay( final Image<BasePairBitType> image ) 
	{ 
		return new BasePairTypeDisplay<BasePairBitType>( image );
	}

	public void set( final Base base ) 
	{
		// the bits to set
		final boolean b1, b2, b3;
		
		switch ( base )
		{
			case A: b1 = b2 = b3 = false;        	   break;
			case T: b1 = b2 = false; b3 = true;  	   break;
			case G: b1 = b2 = true;  b3 = false; 	   break;
			case C: b1 = false; b2 = b3 = true;        break;
			case gap: b1 = true; b2 = b3 = false;      break;
			default: b1 = true; b2 = false; b3 = true; break;
		}
		
		b.setValue( j1, b1 );
		b.setValue( j2, b2 );
		b.setValue( j3, b3 );
	}
	
	public Base get() 
	{
		final boolean b1 = b.getValue( j1 );
		final boolean b2 = b.getValue( j2 );
		final boolean b3 = b.getValue( j3 );
		
		final Base base;
		
		if ( !b1 )
		{
			if ( !b2 )
			{
				if ( !b3 )
					base = Base.A;
				else
					base = Base.T;
			}
			else
			{
				if ( !b3 )
					base = Base.G;
				else
					base = Base.C;				
			}
		}
		else
		{
			if ( !b3 )
				base = Base.gap;
			else
				base = Base.N;
		}
		
		return base;
	}
	
	@Override
	public int compareTo( final BasePairBitType c ) 
	{ 
		final Base input = get();
		final Base compare = c.get();
		
		if ( input == compare )
		{
			return 0;
		}
		else
		{
			switch ( input )
			{
				case gap: return -1; 
				case N: if ( compare == Base.gap ) return 1; else return -1;
				case A: if ( compare == Base.gap || compare == Base.N ) return 1; else return -1;
				case T: if ( compare == Base.G || compare == Base.C ) return -1; else return 1;
				case G: if ( compare == Base.C ) return -1; else return 1;
				default: return 1;
			}
		}
	}
	
	@Override
	public void complement() 
	{
		final Base base = get();
		switch ( base )
		{
			case A: set( Base.T ); break;
			case T: set( Base.A ); break;
			case G: set( Base.C ); break;
			case C: set( Base.G ); break;
		}
	}

	@Override
	public byte baseToValue()
	{
		final Base base = get();
		
		switch ( base )
		{
			case N: return 1;
			case A: return 2;
			case T: return 3;
			case G: return 4;
			case C: return 5;
			default: return 0;
		}		
	}
	
	@Override
	public void set( final BasePairBitType c ) { set( c.get() ); }
	
	@Override
	public BasePairBitType[] createArray1D(int size1){ return new BasePairBitType[ size1 ]; }

	@Override
	public BasePairBitType[][] createArray2D(int size1, int size2){ return new BasePairBitType[ size1 ][ size2 ]; }

	@Override
	public BasePairBitType[][][] createArray3D(int size1, int size2, int size3) { return new BasePairBitType[ size1 ][ size2 ][ size3 ]; }

	@Override
	public BasePairBitType createVariable(){ return new BasePairBitType(); }

	@Override
	public BasePairBitType copy(){ return new BasePairBitType( this.get() ); }

	@Override
	public String toString() { return this.get().toString(); }
}
