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

package mpicbg.imglib.type;

import mpicbg.imglib.type.numeric.integer.ByteType;
import mpicbg.imglib.type.numeric.integer.IntType;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.integer.ShortType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Johannes Schindelin
 */
public abstract class TypeConverter
{
	public abstract void convert();
	
	public static <A extends Type< A >, B extends Type< B > > TypeConverter getTypeConverter( final A input, final B output ) 
	{		
		/* inconvertible types due to javac bug 6548436: if ( input instanceof ByteType )
			return getTypeConverter((ByteType)input, output); */
		if ( (Object)input instanceof ByteType )
			return getTypeConverter((ByteType)(Object)input, output);

		if ( (Object)input instanceof UnsignedByteType )
			return getTypeConverter((UnsignedByteType)(Object)input, output);

		/* inconvertible types due to javac bug 6548436: if ( input instanceof ShortType )
			return getTypeConverter((ShortType)input, output); */
		if ( (Object)input instanceof ShortType )
			return getTypeConverter((ShortType)(Object)input, output);
		
		if ( (Object)input instanceof UnsignedShortType )
			return getTypeConverter((UnsignedShortType)(Object)input, output);
		
		System.out.println("mpi.imglib.type.TypeConverter(): Do not know how to convert Type " + input.getClass() );		
		return null;		
	}
	
	public static <A extends Type< A > > TypeConverter getTypeConverter( final ByteType in, final A output ) 
	{
		
		if ( ByteType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final ByteType out = (ByteType)output; */
			final ByteType out = (ByteType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in );
				}
			};
		}

		if ( ShortType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final ShortType out = (ShortType)output; */
			final ShortType out = (ShortType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( (short)( in.get() & 0xff ) );
				}
			};
		}

		if ( IntType.class.isInstance( output ) )
		{
			/* inconvertible types due to javac bug 6548436: final IntType out = (IntType)output; */
			final IntType out = (IntType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in.get() & 0xff );
				}
			};
		}

		if ( LongType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final IntType out = (IntType)output; */
			final IntType out = (IntType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in.get() & 0xff );
				}
			};
		}
		
		if ( FloatType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final FloatType out = (FloatType)output; */
			final FloatType out = (FloatType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in.get() & 0xff );
				}
			};
		}
		
		System.out.println("mpi.imglib.type.TypeConverter(): Do not know how to convert Type ByteType to Type " + output.getClass() );		
		return null;		
	}

	public static <A extends Type< A > > TypeConverter getTypeConverter( final UnsignedByteType in, final A output ) 
	{
		
		if ( UnsignedByteType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final ByteType out = (ByteType)output; */
			final UnsignedByteType out = (UnsignedByteType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in );
				}
			};
		}

		if ( ShortType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final ShortType out = (ShortType)output; */
			final ShortType out = (ShortType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( (short)( in.get() ) );
				}
			};
		}

		if ( IntType.class.isInstance( output ) )
		{
			/* inconvertible types due to javac bug 6548436: final IntType out = (IntType)output; */
			final IntType out = (IntType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in.get() );
				}
			};
		}

		if ( LongType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final IntType out = (IntType)output; */
			final IntType out = (IntType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in.get() );
				}
			};
		}
		
		if ( FloatType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final FloatType out = (FloatType)output; */
			final FloatType out = (FloatType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in.get() );
				}
			};
		}
		
		System.out.println("mpi.imglib.type.TypeConverter(): Do not know how to convert Type UnsignedByteType to Type " + output.getClass() );		
		return null;		
	}

	public static <A extends Type< A > > TypeConverter getTypeConverter( final ShortType in, final A output ) 
	{
		
		if ( ShortType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final ShortType out = (ShortType)output; */
			final ShortType out = (ShortType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in );
				}
			};
		}

		if ( FloatType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final FloatType out = (FloatType)output; */
			final FloatType out = (FloatType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in.get() & 0xffff );
				}
			};
		}
		
		System.out.println("mpi.imglib.type.TypeConverter(): Do not know how to convert Type ShortType to Type " + output.getClass() );		
		return null;		
	}
	
	public static <A extends Type< A > > TypeConverter getTypeConverter( final UnsignedShortType in, final A output ) 
	{
		
		if ( UnsignedShortType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final ShortType out = (ShortType)output; */
			final UnsignedShortType out = (UnsignedShortType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in );
				}
			};
		}

		if ( FloatType.class.isInstance( output ) ) 
		{
			/* inconvertible types due to javac bug 6548436: final FloatType out = (FloatType)output; */
			final FloatType out = (FloatType)(Object)output;
			
			return new TypeConverter() 
			{
				final public void convert() 
				{
					out.set( in.get() );
				}
			};
		}
		
		System.out.println("mpi.imglib.type.TypeConverter(): Do not know how to convert Type UnsignedShortType to Type " + output.getClass() );		
		return null;		
	}
	
}
