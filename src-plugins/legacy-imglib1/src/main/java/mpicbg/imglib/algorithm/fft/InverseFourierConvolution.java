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

package mpicbg.imglib.algorithm.fft;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.complex.ComplexFloatType;

/**
 * Convolve an image with the inverse of a kernel which is division in the Fourier domain.
 *
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class InverseFourierConvolution< T extends RealType< T >, S extends RealType< S > > extends FourierConvolution< T, S >
{
	public InverseFourierConvolution( final Image< T > image, final Image< S > kernel )
	{
		super( image, kernel );
	}
	
	/**
	 * Divide in Fourier Space
	 * 
	 * @param a
	 * @param b
	 */
	protected void multiply( final Image< ComplexFloatType > a, final Image< ComplexFloatType > b )
	{
		final Cursor<ComplexFloatType> cursorA = a.createCursor();
		final Cursor<ComplexFloatType> cursorB = b.createCursor();
		
		while ( cursorA.hasNext() )
		{
			cursorA.fwd();
			cursorB.fwd();
			
			cursorA.getType().div( cursorB.getType() );
		}
		
		cursorA.close();
		cursorB.close();
	}
}
