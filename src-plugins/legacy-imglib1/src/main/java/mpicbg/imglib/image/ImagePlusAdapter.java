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

package mpicbg.imglib.image;

import ij.ImagePlus;
import ij.measure.Calibration;
import mpicbg.imglib.container.imageplus.ByteImagePlus;
import mpicbg.imglib.container.imageplus.FloatImagePlus;
import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.container.imageplus.IntImagePlus;
import mpicbg.imglib.container.imageplus.ShortImagePlus;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.TypeConverter;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ImagePlusAdapter
{
	@SuppressWarnings("unchecked")
	public static <T extends RealType<T>> Image< T > wrap( final ImagePlus imp )
	{
		return (Image<T>) wrapLocal(imp);
	}
	
	protected static Image<?> wrapLocal( final ImagePlus imp )
	{
		switch( imp.getType() )
		{		
			case ImagePlus.GRAY8 : 
			{
				return wrapByte( imp );
			}
			case ImagePlus.GRAY16 : 
			{
				return wrapShort( imp );
			}
			case ImagePlus.GRAY32 : 
			{
				return wrapFloat( imp );
			}
			case ImagePlus.COLOR_RGB : 
			{
				return wrapRGBA( imp );
			}
			default :
			{
				throw new RuntimeException("Only 8, 16, 32-bit and RGB supported!");
			}
		}
	}

	protected static void setCalibrationFromImagePlus( final Image<?> image, final ImagePlus imp ) 
	{
		final int d = image.getNumDimensions();
		final float [] spacing = new float[d];
		
		for( int i = 0; i < d; ++i )
			spacing[i] = 1f;
		
		final Calibration c = imp.getCalibration();
		
		if( c != null ) 
		{
			if( d >= 1 )
				spacing[0] = (float)c.pixelWidth;
			if( d >= 2 )
				spacing[1] = (float)c.pixelHeight;
			if( d >= 3 )
				spacing[2] = (float)c.pixelDepth;
			if( d >= 4 )
				spacing[3] = (float)c.frameInterval;
		}

		image.setCalibration( spacing );
	}
	
	public static Image<UnsignedByteType> wrapByte( final ImagePlus imp )
	{
		if ( imp.getType() != ImagePlus.GRAY8)
			return null;
		
		final ImagePlusContainerFactory containerFactory = new ImagePlusContainerFactory();
		final ByteImagePlus<UnsignedByteType> container = new ByteImagePlus<UnsignedByteType>( imp,  containerFactory );

		// create a Type that is linked to the container
		final UnsignedByteType linkedType = new UnsignedByteType( container );
		
		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );

		final Image<UnsignedByteType> image = new Image<UnsignedByteType>( container, new UnsignedByteType(), imp.getTitle() );

		setCalibrationFromImagePlus( image, imp );
		
		return image;		
	}
	
	public static Image<UnsignedShortType> wrapShort( final ImagePlus imp )
	{
		if ( imp.getType() != ImagePlus.GRAY16)
			return null;

		final ImagePlusContainerFactory containerFactory = new ImagePlusContainerFactory();
		final ShortImagePlus<UnsignedShortType> container = new ShortImagePlus<UnsignedShortType>( imp,  containerFactory );

		// create a Type that is linked to the container
		final UnsignedShortType linkedType = new UnsignedShortType( container );
		
		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );

		Image<UnsignedShortType> image = new Image<UnsignedShortType>( container, new UnsignedShortType(), imp.getTitle() );

		setCalibrationFromImagePlus( image, imp );
		
		return image;						
	}

	public static Image<RGBALegacyType> wrapRGBA( final ImagePlus imp )
	{
		if ( imp.getType() != ImagePlus.COLOR_RGB)
			return null;

		final ImagePlusContainerFactory containerFactory = new ImagePlusContainerFactory();
		final IntImagePlus<RGBALegacyType> container = new IntImagePlus<RGBALegacyType>( imp,  containerFactory );

		// create a Type that is linked to the container
		final RGBALegacyType linkedType = new RGBALegacyType( container );
		
		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );

		final Image<RGBALegacyType> image = new Image<RGBALegacyType>( container, new RGBALegacyType(), imp.getTitle() );

		setCalibrationFromImagePlus( image, imp );
		
		return image;				
	}	
	
	public static Image<FloatType> wrapFloat( final ImagePlus imp )
	{
		if ( imp.getType() != ImagePlus.GRAY32)
			return null;

		final ImagePlusContainerFactory containerFactory = new ImagePlusContainerFactory();
		final FloatImagePlus<FloatType> container = new FloatImagePlus<FloatType>( imp,  containerFactory );

		// create a Type that is linked to the container
		final FloatType linkedType = new FloatType( container );
		
		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );

		final Image<FloatType> image = new Image<FloatType>( container, new FloatType(), imp.getTitle() );

		setCalibrationFromImagePlus( image, imp );
		
		return image;				
	}	
	
	public static Image<FloatType> convertFloat( final ImagePlus imp )
	{
		if ( imp.getType() != ImagePlus.GRAY32)
		{
			Image<?> img = wrapLocal( imp );
			
			if ( img == null )
				return null;				
			
			return convertToFloat( img );
			
		}
		else
		{
			return wrapFloat( imp );
		}
	}
	
	protected static <T extends Type<T> > Image<FloatType> convertToFloat( Image<T> input )
	{		
		ImageFactory<FloatType> factory = new ImageFactory<FloatType>( new FloatType(), new ImagePlusContainerFactory() );
		Image<FloatType> output = factory.createImage( input.getDimensions(), input.getName() );
	
		Cursor<T> in = input.createCursor();
		Cursor<FloatType> out = output.createCursor();
		
		TypeConverter tc = TypeConverter.getTypeConverter( in.getType(), out.getType() );
		
		if ( tc == null )
		{
			System.out.println( "Cannot convert from " + in.getType().getClass() + " to " + out.getType().getClass() );
			output.close();
			return null;
		}
		
		while ( in.hasNext() )
		{
			in.fwd();
			out.fwd();
			
			tc.convert();			
		}
		
		return output;
	}
}
