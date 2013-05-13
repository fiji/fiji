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

package mpicbg.imglib.algorithm.integral;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.function.VoidConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 */
public class ScaleAreaAveraging2d< T extends RealType<T>, R extends RealType<R>> implements OutputAlgorithm<R>
{
	protected Image<R> scaled;
	protected Image<T> integralImg;
	protected String error;
	protected final int[] size;
	final R targetType;
	final Converter<T, R> converter;
	
	@SuppressWarnings("unchecked")
	public ScaleAreaAveraging2d(final Image<T> integralImg, final R targetType, final int[] size) {
		this.size = size;
		this.targetType = targetType;
		this.integralImg = integralImg;
		
		if ( targetType.getClass().isInstance( integralImg.createType() ) )
		{
			converter = (Converter<T, R>) (Converter<?,?>) new VoidConverter<T>(); // double cast to workaround javac error
		}
		else
		{
			converter = new RealTypeConverter<T, R>();
		}
	}

	public ScaleAreaAveraging2d(final Image<T> integralImg, final R targetType, final Converter<T, R> converter, final int[] size) {
		this.size = size;
		this.targetType = targetType;
		this.integralImg = integralImg;
		this.converter = converter;
	}
	
	public void setOutputDimensions(final int width, final int height) {
		size[0] = width;
		size[1] = height;
	}

	@Override
	public boolean checkInput() {return true;}

	@Override
	public boolean process() {		
		final ImageFactory<R> imgFactory = new ImageFactory<R>( targetType, integralImg.getContainerFactory() );
		scaled = imgFactory.createImage(size);
		
		final LocalizableCursor< R > cursor = scaled.createLocalizableCursor();
		final LocalizableByDimCursor< T > c2 = integralImg.createLocalizableByDimCursor();
		
		final T sum = integralImg.createType();
		final T area = integralImg.createType();
		
		if ( isIntegerDivision( integralImg, scaled ) )
		{
			final int stepSizeX = (integralImg.getDimension( 0 ) -1) / size[ 0 ];
			final int stepSizeY = (integralImg.getDimension( 1 ) -1) / size[ 1 ];
			area.setReal( stepSizeX * stepSizeY );
			
			//final int vX = stepSizeX;
			//final int vY = stepSizeY;
			
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				
				/*
				final int px = cursor.getPosition( 0 );
				final int py = cursor.getPosition( 1 );
				
				final int startX = px * stepSizeX;				
				final int startY = py * stepSizeY;
				
				computeAverage(startX, startY, vX, vY, c2, sum);
				*/
				
				// Same as above, without intermediary variables:
				computeSum(
						cursor.getPosition( 0 ) * stepSizeX,
						cursor.getPosition( 1 ) * stepSizeY,
						stepSizeX, stepSizeY, // vX, vY,
						c2, sum);
				
				sum.div( area );
				
				//System.out.println( sum );
				//System.exit( 0 );
				
				converter.convert( sum, cursor.getType() );
			}
		}
		else
		{
			final double stepSizeX = ((double)integralImg.getDimension( 0 ) -1) / (double)size[ 0 ];
			final double stepSizeY = ((double)integralImg.getDimension( 1 ) -1) / (double)size[ 1 ];
			
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				
				final int px = cursor.getPosition( 0 );
				final int py = cursor.getPosition( 1 );
				
				final double tmp1 = px * stepSizeX + 0.5;
				final int startX = (int)(tmp1);
				final int vX = (int)(tmp1 + stepSizeX) - startX;
				
				final double tmp2 = py * stepSizeY + 0.5;
				final int startY = (int)(tmp2);
				final int vY = (int)(tmp2 + stepSizeY) - startY;
				
				area.setReal( vX * vY );
				
				computeSum(startX, startY, vX, vY, c2, sum);
				
				sum.div( area );
				
				converter.convert( sum, cursor.getType() );
			}
		}	
		
		return true;
	}

	final private static <T extends RealType<T>> void computeSum( final int startX, final int startY, final int vX, final int vY, 
			final LocalizableByDimCursor< T > c2, final T sum )
	{
		c2.setPosition( startX, 0 );
		c2.setPosition( startY, 1 );
		sum.set( c2.getType() );
		
		c2.move( vX, 0 );
		sum.sub( c2.getType() );
		
		c2.move( vY, 1 );
		sum.add( c2.getType() );
		
		c2.move( -vX, 0 );
		sum.sub( c2.getType() );
	}
	
	/** The dimensions of the integral image are always +1 from the integrated image. */
	protected static final boolean isIntegerDivision(Image<?> integralImg, Image<?> scaled) {
		for ( int d = 0; d < scaled.getNumDimensions(); ++d )
			if ( 0 != (integralImg.getDimension( d ) -1) % scaled.getDimension( d ) )
				return false;
		
		return true;
	}

	@Override
	public String getErrorMessage() {
		return error;
	}

	@Override
	public Image<R> getResult() {
		return scaled;
	}

}
