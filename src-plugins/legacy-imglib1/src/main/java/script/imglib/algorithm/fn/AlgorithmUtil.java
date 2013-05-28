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

package script.imglib.algorithm.fn;

import java.util.Collection;

import mpicbg.imglib.image.Image;
import script.imglib.color.fn.ColorFunction;
import script.imglib.math.Compute;
import script.imglib.math.fn.IFunction;

/**
 * TODO
 *
 */
public class AlgorithmUtil
{
	/** Wraps Image, ColorFunction and IFunction, but not numbers. */
	@SuppressWarnings("unchecked")
	static public final Image wrap(final Object ob) throws Exception {
		if (ob instanceof Image<?>) return (Image)ob;
		if (ob instanceof ColorFunction) return Compute.inRGBA((ColorFunction)ob);
		if (ob instanceof IFunction) return Compute.inDoubles((IFunction)ob);
		throw new Exception("Cannot create an image from " + ob.getClass());
	}
	
	/** Wraps Image and IFunction, but not numbers, and not a ColorFunction:
	 * considers the image as single-channel. */
	@SuppressWarnings("unchecked")
	static public final Image wrapS(final Object ob) throws Exception {
		if (ob instanceof Image<?>) return (Image)ob;
		if (ob instanceof IFunction) return Compute.inDoubles((IFunction)ob);
		throw new Exception("Cannot create an image from " + ob.getClass());
	}

	/** Copy the given double value into each index of a double[] array of length {@param nDim}.*/
	static public final double[] asArray(final int nDim, final double sigma) {
		final double[] s = new double[nDim];
		for (int i=0; i<nDim; ++i)
			s[ i ] = sigma;
		return s;
	}

	public static double[] asDoubleArray(final Collection<Number> ls) {
		final double[] d = new double[ls.size()];
		int i = 0;
		for (final Number num : ls) d[i++] = num.doubleValue();
		return d;
	}

	public static double[] asDoubleArray(final float[] f) {
		final double[] d = new double[f.length];
		for (int i=0; i<f.length; i++) d[i] = f[i];
		return d;
	}
}
