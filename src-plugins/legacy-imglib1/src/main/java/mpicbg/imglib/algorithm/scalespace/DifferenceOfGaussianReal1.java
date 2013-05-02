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
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class DifferenceOfGaussianReal1< A extends RealType<A> > extends DifferenceOfGaussianReal<A, A>
{
	public DifferenceOfGaussianReal1( final Image<A> img, OutOfBoundsStrategyFactory<A> outOfBoundsFactory, 
								      final double sigma1, final double sigma2, double minPeakValue, double normalizationFactor)
	{
		super( img, img.getImageFactory(), outOfBoundsFactory, sigma1, sigma2, minPeakValue, normalizationFactor);
	}

	public DifferenceOfGaussianReal1( final Image<A> img, OutOfBoundsStrategyFactory<A> outOfBoundsFactory, 
			  						  final double[] sigma1, final double[] sigma2, double minPeakValue, double normalizationFactor)
	{
		super( img, img.getImageFactory(), outOfBoundsFactory, sigma1, sigma2, minPeakValue, normalizationFactor);
	}
	
	/**
	 * This method returns the {@link OutputAlgorithm} that will compute the Gaussian Convolutions, more efficient versions can override this method
	 * 
	 * @param sigma - the sigma of the convolution
	 * @param numThreads - the number of threads for this convolution
	 * @return
	 */
	@Override
	protected OutputAlgorithm<A> getGaussianConvolution( final double[] sigma, final int numThreads )
	{
		final GaussianConvolutionReal<A> gauss = new GaussianConvolutionReal<A>( image, outOfBoundsFactory, sigma );
		
		return gauss;
	}
	
}
