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

package mpicbg.imglib.algorithm.roi;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * DirectCrossCorr performs direct cross-correlation of a kernel against an image.
 * @param <T> input image type
 * @param <R> kernel type
 * @param <S> output image type 
 *
 * @author Larry Lindsey
 */
public class DirectCrossCorr
	<T extends RealType<T>, R extends RealType<R>, S extends RealType<S>>
		extends DirectConvolution<T, R, S>
{
	public DirectCrossCorr(final S type, final Image<T> inputImage, final Image<R> kernel)
	{
		this(type, inputImage, kernel, null);
	}
	
	
	public DirectCrossCorr(final S type, final Image<T> inputImage, final Image<R> kernel,
			final OutOfBoundsStrategyFactory<T> outsideFactory) {
		super(type, inputImage, kernel, outsideFactory, false);
		setName(inputImage.getName() + " x " + kernel.getName());
	}

}
