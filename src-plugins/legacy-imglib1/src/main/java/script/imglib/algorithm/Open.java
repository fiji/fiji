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

import mpicbg.imglib.algorithm.roi.MorphOpen;
import mpicbg.imglib.type.numeric.RealType;
import script.imglib.algorithm.fn.Morph;

/** Operates on an {@link Image} or an {@link IFunction}. */
/**
 * TODO
 *
 */
public class Open<T extends RealType<T>> extends Morph<T>
{
	public Open(final Object fn) throws Exception {
		super(fn, MorphOpen.class, Shape.CUBE, 3, 0, 0);
	}

	public Open(final Object fn, final Shape s, final Number shapeLength,
			final Number lengthDim, final Number outside) throws Exception {
		super(fn, MorphOpen.class, s, shapeLength.intValue(), lengthDim.intValue(), outside.floatValue());
	}
}
