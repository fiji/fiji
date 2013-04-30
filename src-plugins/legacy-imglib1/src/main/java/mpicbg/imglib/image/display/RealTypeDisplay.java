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

package mpicbg.imglib.image.display;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class RealTypeDisplay<T extends RealType<T>> extends Display<T>
{
	public RealTypeDisplay( final Image<T> img )
	{
		super(img);

		final T type = img.createType(); 
		min = type.getMinValue();
		max = type.getMaxValue();
	}	
	
	@Override
	public void setMinMax()
	{
		final Cursor<T> c = img.createCursor();
		
		if ( !c.hasNext() )
		{
			final T type = img.createType(); 
			min = type.getMinValue();
			max = type.getMaxValue();
			return;
		}
		
		c.fwd();
		min = max = c.getType().getRealDouble();

		while ( c.hasNext() )
		{
			c.fwd();

			final double value = c.getType().getRealDouble();
			
			if ( value > max )
				max = value;			
			else if ( value < min )
				min = value;
		}
		
		c.close();
	}

	@Override
	public float get32Bit( T c ) { return c.getRealFloat(); }
	@Override
	public float get32BitNormed( T c ) { return normFloat( c.getRealFloat() ); }
	
	@Override
	public byte get8BitSigned( final T c) { return (byte) Math.round( normFloat( c.getRealFloat() ) * 255 ); }
	@Override
	public short get8BitUnsigned( final T c) { return (short)Math.round( normFloat( c.getRealFloat() ) * 255 ); }		
}
