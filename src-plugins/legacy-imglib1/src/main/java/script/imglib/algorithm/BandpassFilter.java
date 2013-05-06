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

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import script.imglib.math.Compute;
import script.imglib.math.fn.IFunction;

/** A bandpass filter. */
/**
 * TODO
 *
 */
public class BandpassFilter<T extends RealType<T>> extends Image<T>
{
	public BandpassFilter(final Image<T> img, final int beginRadius, final int endRadius) throws Exception {
		super(process(img, beginRadius, endRadius).getContainer(), img.createType(), "Bandpass");
	}

	@SuppressWarnings("unchecked")
	public BandpassFilter(final IFunction fn, final int beginRadius, final int endRadius) throws Exception {
		this((Image)Compute.inDoubles(fn), beginRadius, endRadius);
	}

	static private final <T extends RealType<T>> Image<T> process(final Image<T> img, final int beginRadius, final int endRadius) throws Exception {
		mpicbg.imglib.algorithm.fft.Bandpass<T> bp = new mpicbg.imglib.algorithm.fft.Bandpass<T>(img, beginRadius, endRadius);
		if (!bp.checkInput() || !bp.process()) {
			throw new Exception(bp.getClass().getSimpleName() + " failed: " + bp.getErrorMessage());
		}
		return bp.getResult();
	}
}
