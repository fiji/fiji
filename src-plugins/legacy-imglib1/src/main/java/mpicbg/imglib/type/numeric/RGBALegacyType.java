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
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.type.numeric;

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.IntAccess;
import mpicbg.imglib.container.basictypecontainer.array.IntArray;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.RGBALegacyTypeDisplay;
import mpicbg.imglib.type.TypeImpl;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
final public class RGBALegacyType extends TypeImpl<RGBALegacyType> implements NumericType<RGBALegacyType>
{
	// the DirectAccessContainer
	final DirectAccessContainer<RGBALegacyType, ? extends IntAccess> storage;
	
	// the (sub)DirectAccessContainer that holds the information 
	IntAccess b;
	
	// this is the constructor if you want it to read from an array
	public RGBALegacyType( DirectAccessContainer<RGBALegacyType, ? extends IntAccess> byteStorage )
	{
		storage = byteStorage;
	}

	// this is the constructor if you want it to be a variable
	public RGBALegacyType( final int value )
	{
		storage = null;
		b = new IntArray( 1 );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public RGBALegacyType() { this( 0 ); }
	
	@Override
	public DirectAccessContainer<RGBALegacyType, ? extends IntAccess> createSuitableDirectAccessContainer( final DirectAccessContainerFactory storageFactory, final int dim[] )
	{
		// create the container
		final DirectAccessContainer<RGBALegacyType, ? extends IntAccess> container = storageFactory.createIntInstance( dim, 1 );
		
		// create a Type that is linked to the container
		final RGBALegacyType linkedType = new RGBALegacyType( container );
		
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
	public RGBALegacyType duplicateTypeOnSameDirectAccessContainer() { return new RGBALegacyType( storage ); }
	
	@Override
	public RGBALegacyTypeDisplay getDefaultDisplay( Image<RGBALegacyType> image )
	{
		return new RGBALegacyTypeDisplay( image );
	}

	final public static int rgba( final int r, final int g, final int b, final int a)
	{
		return ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff) | ((a & 0xff) << 24);
	}
	
	final public static int rgba( final float r, final float g, final float b, final float a)
	{
		return rgba( Util.round(r), Util.round(g), Util.round(b), Util.round(a) );
	}

	final public static int rgba( final double r, final double g, final double b, final double a)
	{
		return rgba( (int)Util.round(r), (int)Util.round(g), (int)Util.round(b), (int)Util.round(a) );
	}
	
	final public static int red( final int value )
	{
		return (value >> 16) & 0xff;
	}
	
	final public static int green( final int value )
	{
		return (value >> 8) & 0xff;
	}
	
	final public static int blue( final int value )
	{
		return value & 0xff;
	}
	
	final public static int alpha( final int value )
	{
		return (value >> 24) & 0xff;
	}
	
	public int get(){ return b.getValue( i ); }
	public void set( final int f ){ b.setValue( i, f ); }
		
	@Override
	public void mul( final float c )
	{
		final int value = get();
		set( rgba( red(value) * c, green(value) * c, blue(value) * c, alpha(value) * c ) );
	}

	@Override
	public void mul( final double c ) 
	{ 
		final int value = get();		
		set( rgba( red(value) * c, green(value) * c, blue(value) * c, alpha(value) * c ) );
	}

	@Override
	public void add( final RGBALegacyType c ) 
	{ 
		final int value1 = get();		
		final int value2 = c.get();		
		
		set( rgba( red(value1) + red(value2), green(value1) + green(value2), blue(value1) + blue(value2), alpha(value1) + alpha(value2) ) );		 
	}

	@Override
	public void div( final RGBALegacyType c ) 
	{ 
		final int value1 = get();		
		final int value2 = c.get();		
		
		set( rgba( red(value1) / red(value2), green(value1) / green(value2), blue(value1) / blue(value2), alpha(value1) / alpha(value2) ) );		 
	}

	@Override
	public void mul( final RGBALegacyType c ) 
	{
		final int value1 = get();		
		final int value2 = c.get();		
		
		set( rgba( red(value1) * red(value2), green(value1) * green(value2), blue(value1) * blue(value2), alpha(value1) * alpha(value2) ) );		 
	}

	@Override
	public void sub( final RGBALegacyType c ) 
	{
		final int value1 = get();		
		final int value2 = c.get();		
		
		set( rgba( red(value1) - red(value2), green(value1) - green(value2), blue(value1) - blue(value2), alpha(value1) - alpha(value2) ) );		 
	}
	
	@Override
	public void set( final RGBALegacyType c ) { set( c.get() ); }

	@Override
	public void setOne() { set( rgba( 1, 1, 1, 1 ) ); }

	@Override
	public void setZero() { set( 0 ); }
	
	@Override
	public RGBALegacyType[] createArray1D(int size1){ return new RGBALegacyType[ size1 ]; }

	@Override
	public RGBALegacyType[][] createArray2D(int size1, int size2){ return new RGBALegacyType[ size1 ][ size2 ]; }

	@Override
	public RGBALegacyType[][][] createArray3D(int size1, int size2, int size3) { return new RGBALegacyType[ size1 ][ size2 ][ size3 ]; }

	@Override
	public RGBALegacyType createVariable() { return new RGBALegacyType( 0 ); }

	@Override
	public RGBALegacyType copy() { return new RGBALegacyType( get() ); }

	@Override
	public String toString() 
	{
		final int rgba = get();
		return "(r=" + red( rgba ) + ",g=" + green( rgba ) + ",b=" + blue( rgba ) + ",a=" + alpha( rgba ) + ")";
	}

	@Override
	public int getEntitiesPerPixel() { return 1; }
}
