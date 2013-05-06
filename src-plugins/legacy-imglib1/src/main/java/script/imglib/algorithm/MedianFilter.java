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

package script.imglib.algorithm;

import mpicbg.imglib.algorithm.roi.StructuringElement;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import script.imglib.math.Compute;
import script.imglib.math.fn.IFunction;

/**
 * TODO
 *
 */
public class MedianFilter<T extends RealType<T>> extends Image<T>
{
	/** A median filter with an {@link OutOfBoundsStrategyMirrorFactory}. */
	public MedianFilter(final Image<T> img, final float radius) throws Exception {
		this(img, radius, new OutOfBoundsStrategyMirrorFactory<T>());
	}

	public MedianFilter(final Image<T> img, final float radius, final OutOfBoundsStrategyFactory<T> oobs) throws Exception {
		super(process(img, radius, oobs).getContainer(), img.createType());
	}

	/** A median filter with an {@link OutOfBoundsStrategyMirrorFactory}. */
	@SuppressWarnings("unchecked")
	public MedianFilter(final IFunction fn, final float radius) throws Exception {
		this((Image)Compute.inDoubles(fn), radius, new OutOfBoundsStrategyMirrorFactory<T>());
	}

	@SuppressWarnings("unchecked")
	public MedianFilter(final IFunction fn, final float radius, final OutOfBoundsStrategyFactory<T> oobs) throws Exception {
		this((Image)Compute.inDoubles(fn), radius, oobs);
	}

	static private final <S extends RealType<S>> Image<S> process(final Image<S> img, final float radius, final OutOfBoundsStrategyFactory<S> oobs) throws Exception {
		final mpicbg.imglib.algorithm.roi.MedianFilter<S> mf =
			new mpicbg.imglib.algorithm.roi.MedianFilter<S>(img, StructuringElement.createBall(img.getNumDimensions(), radius), oobs);
		// TODO: mf.checkInput() returns false even if the image is processed fine.
		if (!mf.process()) {
			throw new Exception("MedianFilter: " + mf.getErrorMessage());
		}
		return mf.getResult();
	}
}
