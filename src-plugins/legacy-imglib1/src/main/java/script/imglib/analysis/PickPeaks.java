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

package script.imglib.analysis;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.PickImagePeaks;
import mpicbg.imglib.type.numeric.RealType;
import script.imglib.algorithm.fn.AlgorithmUtil;

/** Picks peaks in an image. It is recommended to smooth the image a bit with a Gaussian first.
 * 
 * See underlying algorithm {@link PickImagePeaks}. */
/**
 * TODO
 *
 */
public class PickPeaks<T extends RealType<T>> extends ArrayList<float[]>
{
	private static final long serialVersionUID = 5392390381529251353L;

	/** @param fn Any of {@link IFunction, Image}. */
	public PickPeaks(final Object fn) throws Exception {
		this(fn, null);
	}

	/**
	 * @param fn Any of {@link IFunction, Image}.
	 * @param suppressionRegion A float array with as many dimensions as the image has, and which describes an spheroid for supression around a peak.
	 */
	@SuppressWarnings("unchecked")
	public PickPeaks(final Object fn, final double[] suppressionRegion) throws Exception {
		PickImagePeaks<T> pick = new PickImagePeaks<T>(AlgorithmUtil.wrapS(fn));
		if (null != suppressionRegion) pick.setSuppression(suppressionRegion);
		if (!pick.checkInput() || !pick.process()) {
			throw new Exception("PickPeaks error: " + pick.getErrorMessage());
		}
		for (int[] p : pick.getPeakList()) {
			float[] f = new float[p.length];
			System.arraycopy(p, 0, f, 0, p.length);
			this.add(f);
		}
	}
}
