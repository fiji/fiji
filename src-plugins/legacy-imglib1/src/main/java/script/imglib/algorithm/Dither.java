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

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.floydsteinberg.FloydSteinbergDithering;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import script.imglib.math.Compute;
import script.imglib.math.fn.IFunction;

/** Perform {@link FloydSteinbergDithering} on an image. */
/**
 * TODO
 *
 */
public class Dither<T extends RealType<T>> extends Image<BitType>
{
	/** The dithering threshold is computed from the min and max values of the image;
	 *  see {@link FloydSteinbergDithering}. */
	public Dither(final Image<T> img) throws Exception {
		super(process(img).getContainer(), new BitType());
	}

	public Dither(final Image<T> img, final float ditheringThreshold) throws Exception {
		super(process(img, ditheringThreshold).getContainer(), new BitType());
	}

	@SuppressWarnings("unchecked")
	public Dither(final IFunction fn) throws Exception {
		this((Image)Compute.inDoubles(fn));
	}

	@SuppressWarnings("unchecked")
	public Dither(final IFunction fn, final float ditheringThreshold) throws Exception {
		this((Image)Compute.inDoubles(fn), ditheringThreshold);
	}

	static private final <R extends RealType<R>> Image<BitType> process(final Image<R> img, final float ditheringThreshold) throws Exception {
		return process(new FloydSteinbergDithering<R>(img, ditheringThreshold));
	}
	static private final <R extends RealType<R>> Image<BitType> process(final Image<R> img) throws Exception {
		return process(new FloydSteinbergDithering<R>(img));
	}
	static private final <R extends RealType<R>> Image<R> process(final OutputAlgorithm<R> oa) throws Exception {
		if (!oa.checkInput() || !oa.process()) {
			throw new Exception("Dither failed: " + oa.getErrorMessage());
		}
		return oa.getResult();
	}
}
