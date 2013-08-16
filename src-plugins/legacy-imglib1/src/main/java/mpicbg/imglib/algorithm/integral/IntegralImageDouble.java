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

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.type.numeric.real.DoubleType;

/**
 * Special implementation for double using the basic type to sum up the individual lines. 
 * 
 * @param <R>
 * @author Stephan Preibisch
 */
public class IntegralImageDouble< R extends NumericType< R > > extends IntegralImage< R, DoubleType >
{

	public IntegralImageDouble( final Image<R> img, final Converter<R, DoubleType> converter) 
	{
		super( img, new DoubleType(), converter );
	}

	@Override
	protected void integrateLineDim0( final Converter< R, DoubleType > converter, final LocalizableByDimCursor< R > cursorIn, final LocalizableByDimCursor< DoubleType > cursorOut, final DoubleType sum, final DoubleType tmpVar, final int size )
	{
		// compute the first pixel
		converter.convert( cursorIn.getType(), sum );
		cursorOut.getType().set( sum );
		
		double sum2 = sum.get();

		for ( int i = 2; i < size; ++i )
		{
			cursorIn.fwd( 0 );
			cursorOut.fwd( 0 );

			converter.convert( cursorIn.getType(), tmpVar );
			sum2 += tmpVar.get();
			cursorOut.getType().set( sum2 );
		}		
	}

	@Override
	protected void integrateLine( final int d, final LocalizableByDimCursor< DoubleType > cursor, final DoubleType sum, final int size )
	{
		// init sum on first pixel that is not zero
		double sum2 = cursor.getType().get();

		for ( int i = 2; i < size; ++i )
		{
			cursor.fwd( d );
			final DoubleType type = cursor.getType();
			
			sum2 += type.get();
			type.set( sum2 );
		}
	}
	
}
