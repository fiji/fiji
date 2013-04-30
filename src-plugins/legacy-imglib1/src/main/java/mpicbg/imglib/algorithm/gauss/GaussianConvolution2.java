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

package mpicbg.imglib.algorithm.gauss;

import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.NumericType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class GaussianConvolution2< A extends Type<A>, B extends NumericType<B> > extends GaussianConvolution3<A, B, B>
{
	public GaussianConvolution2( final Image<A> image, final ImageFactory<B> factoryProcess, final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, final Converter<A, B> converterIn, final double[] sigma )
	{
		super( image, factoryProcess, null, outOfBoundsFactory, converterIn, null, sigma );
	}
	
	public GaussianConvolution2( final Image<A> image, final ImageFactory<B> factoryProcess, final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, final Converter<A, B> converterIn, final double sigma )
	{
		this( image, factoryProcess, outOfBoundsFactory, converterIn, createArray( image, sigma ) );
	}
	
	protected Image<B> getConvolvedImage()
	{
        final Image<B> output;
        
        if ( numDimensions % 2 == 0 )
        {
        	output = temp1;
            
        	// close other temporary datastructure
            temp2.close();
        }
        else
        {
        	output = temp2;

        	// close other temporary datastructure
            temp1.close();
        }
		
		return output;		
	}
	
}
