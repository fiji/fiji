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
import mpicbg.imglib.type.numeric.integer.LongType;
import script.imglib.math.Compute;
import script.imglib.math.fn.IFunction;

/**
 * TODO
 *
 */
public class HoughLineTransform<T extends RealType<T>> extends Image<LongType>
{
	/** A {@link mpicbg.imglib.algorithm.transformation.HoughLineTransform} with a LongType vote space.*/
	public HoughLineTransform(final Image<T> img) throws Exception {
		super(process(img).getContainer(), new LongType());
	}

	@SuppressWarnings("unchecked")
	public HoughLineTransform(final IFunction fn) throws Exception {
		this((Image)Compute.inDoubles(fn));
	}

	static private final <S extends RealType<S>> Image<LongType> process(final Image<S> img) throws Exception {
		mpicbg.imglib.algorithm.transformation.HoughLineTransform<LongType, S> h = 
			new mpicbg.imglib.algorithm.transformation.HoughLineTransform<LongType, S>(img, new LongType());
		if (!h.checkInput() || !h.process()) {
			throw new Exception("HoughLineTransform: " + h.getErrorMessage());
		}
		return h.getResult();
	}
}
