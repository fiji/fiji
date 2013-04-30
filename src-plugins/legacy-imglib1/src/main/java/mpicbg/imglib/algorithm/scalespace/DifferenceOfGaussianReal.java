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

package mpicbg.imglib.algorithm.scalespace;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.function.SubtractNormReal;
import mpicbg.imglib.algorithm.gauss.GaussianConvolution2;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.function.Function;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class DifferenceOfGaussianReal< A extends RealType<A>, B extends RealType<B> > extends DifferenceOfGaussian<A, B>
{
	double normalizationFactor, minPeakValue;
	
	public DifferenceOfGaussianReal( final Image<A> img, final ImageFactory<B> factory,  
									 final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, 
									 final double sigma1, final double sigma2, final double minPeakValue, final double normalizationFactor )
	{
		super( img, factory, new RealTypeConverter<A, B>(), outOfBoundsFactory, sigma1, sigma2, createVariable( factory, minPeakValue ), createVariable( factory, normalizationFactor ) );
		
		this.normalizationFactor = normalizationFactor;
		this.minPeakValue = minPeakValue;
	}

	public DifferenceOfGaussianReal( final Image<A> img, final ImageFactory<B> factory,  
			 						 final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, 
			 						 final double[] sigma1, final double[] sigma2, final double minPeakValue, final double normalizationFactor )
	{
		super( img, factory, new RealTypeConverter<A, B>(), outOfBoundsFactory, sigma1, sigma2, createVariable( factory, minPeakValue ), createVariable( factory, normalizationFactor ) );
		
		this.normalizationFactor = normalizationFactor;
		this.minPeakValue = minPeakValue;
	}
	
	protected static <T extends RealType<T> > T createVariable( final ImageFactory<T> img, final double value )
	{
		final T type = img.createType();
		type.setReal( value );
		return type;
	}
	
	@Override
	public void setMinPeakValue( final B value ) { minPeakValue = value.getRealDouble(); }
	@Override
	public B getMinPeakValue() { return createVariable( factory, minPeakValue ); }

	/**
	 * This method returns the {@link OutputAlgorithm} that will compute the Gaussian Convolutions, more efficient versions can override this method
	 * 
	 * @param sigma - the sigma of the convolution
	 * @param numThreads - the number of threads for this convolution
	 * @return
	 */
	@Override
	protected OutputAlgorithm<B> getGaussianConvolution( final double[] sigma, final int numThreads )
	{
		final GaussianConvolution2<A,B> gauss = new GaussianConvolution2<A,B>( image, factory, outOfBoundsFactory, new RealTypeConverter<A, B>(), sigma );
		
		return gauss;
	}
	
	/**
	 * Returns the function that does the normalized subtraction of the gauss images, more efficient versions can override this method
	 * @return - the Subtraction Function
	 */
	protected Function<B, B, B> getNormalizedSubtraction()
	{
		return new SubtractNormReal<B, B, B>( normalizationFactor );
	}

	/**
	 * Checks if the absolute value of the current peak is high enough, more efficient versions can override this method
	 * @param value - the current value
	 * @return true if the absoluted value is high enough, otherwise false 
	 */
	protected boolean isPeakHighEnough( final B value )
	{
		return Math.abs( value.getRealDouble() ) >= minPeakValue;
	}
	
	/**
	 * Checks if the current position is a minima or maxima in a 3^n neighborhood, more efficient versions can override this method
	 *  
	 * @param neighborhoodCursor - the {@link LocalNeighborhoodCursor}
	 * @param centerValue - the value in the center which is tested
	 * @return - if is a minimum, maximum or nothig
	 */
	protected SpecialPoint isSpecialPoint( final LocalNeighborhoodCursor<B> neighborhoodCursor, final B centerValue )
	{
		boolean isMin = true;
		boolean isMax = true;
		
		final double centerValueReal = centerValue.getRealDouble();
		
		while ( (isMax || isMin) && neighborhoodCursor.hasNext() )
		{			
			neighborhoodCursor.fwd();
			
			final double value = neighborhoodCursor.getType().getRealDouble(); 
			
			// it can still be a minima if the current value is bigger/equal to the center value
			isMin &= (value >= centerValueReal);
			
			// it can still be a maxima if the current value is smaller/equal to the center value
			isMax &= (value <= centerValueReal);
		}		
		
		// this mixup is intended, a minimum in the 2nd derivation is a maxima in image space and vice versa
		if ( isMin )
			return SpecialPoint.MAX;
		else if ( isMax )
			return SpecialPoint.MIN;
		else
			return SpecialPoint.INVALID;
	}
		
	@Override
	public boolean checkInput()
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image == null )
		{
			errorMessage = "DifferenceOfGaussian: [Image<A> img] is null.";
			return false;
		}
		else if ( factory == null )
		{
			errorMessage = "DifferenceOfGaussian: [ImageFactory<B> img] is null.";
			return false;
		}
		else if ( outOfBoundsFactory == null )
		{
			errorMessage = "DifferenceOfGaussian: [OutOfBoundsStrategyFactory<B>] is null.";
			return false;
		}
		else
			return true;
	}
}
