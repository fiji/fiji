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

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public abstract class Display<T extends Type<T>>
{
	final protected Image<T> img;
	
	protected double min, max;
	
	public Display( final Image<T> img )
	{
		this.img = img;
		this.min = 0;
		this.max = 1;
	}	
	
	public Image<T> getImage() { return img; }
	
	public abstract void setMinMax();
	
	public double getMin() { return min; }
	public double getMax() { return max; }
	public void setMin( final double min ) { this.min = min; }
	public void setMax( final double max ) { this.max = max; }
	public void setMinMax( final double min, final double max ) 
	{
		this.min = min;
		this.max = max; 
	}

	public double normDouble( final double c )
	{
		double value = ( c - min ) / ( max - min );
		
		if ( value < 0 )
			value = 0;
		else if ( value > 1 )
			value = 1;
		
		return value;
	}

	public float normFloat( final float c )
	{
		double value = ( c - min ) / ( max - min );
		
		if ( value < 0 )
			value = 0;
		else if ( value > 1 )
			value = 1;
		
		return (float)value;
	}
	
	public abstract float get32Bit( T c );	
	public abstract float get32BitNormed( T c );

	public abstract byte get8BitSigned( T c );
	public abstract short get8BitUnsigned( T c );
	
	public int get8BitARGB( final T c )
	{
		final int col = get8BitUnsigned( c );		
				
		return (col<<16)+(col<<8)+col;
	}	
	
}
