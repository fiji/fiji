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

import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import script.imglib.math.Compute;
import script.imglib.math.fn.IFunction;

/** Performs a {@link GaussianConvolutionReal} operation on an {@link Image} or an {@link IFunction},
 *  the latter computed first into an {@link Image} by using {@link Compute}.inDoubles. */
/**
 * TODO
 *
 */
public class Gauss<T extends RealType<T>> extends Image<T>
{
	/** A Gaussian convolution with an {@link OutOfBoundsStrategyMirrorFactory}. */
	public Gauss(final Image<T> img, final double sigma) throws Exception {
		this(img, new OutOfBoundsStrategyMirrorFactory<T>(), sigma);
	}

	/** A Gaussian convolution with an {@link OutOfBoundsStrategyMirrorFactory}. */
	public Gauss(final Image<T> img, final double[] sigma) throws Exception {
		this(img, new OutOfBoundsStrategyMirrorFactory<T>(), sigma);
	}

	/** A Gaussian convolution with an {@link OutOfBoundsStrategyMirrorFactory}. */
	@SuppressWarnings("unchecked")
	public Gauss(final IFunction fn, final double sigma) throws Exception {
		this((Image)Compute.inDoubles(fn), new OutOfBoundsStrategyMirrorFactory<T>(), sigma);
	}

	/** A Gaussian convolution with an {@link OutOfBoundsStrategyMirrorFactory}. */
	@SuppressWarnings("unchecked")
	public Gauss(final IFunction fn, final double[] sigma) throws Exception {
		this((Image)Compute.inDoubles(fn), new OutOfBoundsStrategyMirrorFactory<T>(), sigma);
	}

	public Gauss(final Image<T> img, final OutOfBoundsStrategyFactory<T> oobs, final double sigma) throws Exception {
		this(img, oobs, asArray(sigma, img.getNumDimensions()));
	}

	public Gauss(final Image<T> img, final OutOfBoundsStrategyFactory<T> oobs, final double[] sigma) throws Exception {
		super(process(img, oobs, sigma).getContainer(), img.createType());
	}

	@SuppressWarnings("unchecked")
	public Gauss(final IFunction fn, final OutOfBoundsStrategyFactory<T> oobs, final double sigma) throws Exception {
		this((Image)Compute.inDoubles(fn), oobs, sigma);
	}

	@SuppressWarnings("unchecked")
	public Gauss(final IFunction fn, final OutOfBoundsStrategyFactory<T> oobs, final double[] sigma) throws Exception {
		this((Image)Compute.inDoubles(fn), oobs, sigma);
	}

	static private final double[] asArray(final double sigma, final int nDimensions) {
		final double[] s = new double[nDimensions];
		for (int i=0; i<s.length; i++) s[i] = sigma;
		return s;
	}

	static private final <R extends RealType<R>> Image<R> process(final Image<R> img, final OutOfBoundsStrategyFactory<R> oobs, final double[] sigma) throws Exception {
		final GaussianConvolutionReal<R> gcr = new GaussianConvolutionReal<R>(img, oobs, sigma);
		if (!gcr.checkInput() || !gcr.process()) {
			throw new Exception("Gauss: " + gcr.getErrorMessage());
		}
		return gcr.getResult();
	}
}
