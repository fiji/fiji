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

package script.imglib.math;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import script.imglib.math.fn.BinaryOperation;
import script.imglib.math.fn.IFunction;

/**
 * TODO
 *
 */
public class Divide extends BinaryOperation
{
	public Divide(final Image<? extends RealType<?>> left, final Image<? extends RealType<?>> right) {
		super(left, right);
	}

	public Divide(final IFunction fn, final Image<? extends RealType<?>> right) {
		super(fn, right);
	}

	public Divide(final Image<? extends RealType<?>> left, final IFunction fn) {
		super(left, fn);
	}

	public Divide(final IFunction fn1, final IFunction fn2) {
		super(fn1, fn2);
	}
	
	public Divide(final Image<? extends RealType<?>> left, final Number val) {
		super(left, val);
	}

	public Divide(final Number val,final Image<? extends RealType<?>> right) {
		super(val, right);
	}

	public Divide(final IFunction left, final Number val) {
		super(left, val);
	}

	public Divide(final Number val,final IFunction right) {
		super(val, right);
	}
	
	public Divide(final Number val1, final Number val2) {
		super(val1, val2);
	}

	public Divide(final Object... elems) throws Exception {
		super(elems);
	}

	/** 1 / img */
	public Divide(final Image<? extends RealType<?>> right) {
		super(1, right);
	}

	/** 1 / val */
	public Divide(final Number val) {
		super(1, val);
	}

	/** 1 / fn.eval() */
	public Divide(final IFunction fn) {
		super(1, fn);
	}

	@Override
	public final double eval() {
		return a().eval() / b().eval();
	}
}
