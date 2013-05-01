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

package script.imglib.math.fn;

import java.util.Collection;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.IntType;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.integer.ShortType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.real.DoubleType;
import mpicbg.imglib.type.numeric.real.FloatType;

/** A function that, when evaluated, always returns the same number,
 *  expressed as a {@code double}.
 *  When given a {@code byte} or a @Byte, it reads it as unsigned. */
/**
 * TODO
 *
 */
public final class NumberFunction implements IFunction {

	private final double val;

	public NumberFunction(final Number num) {
		this.val = NumberFunction.asType(num).getRealDouble();
	}
	
	public NumberFunction(final double val) { this.val = val; }

	@Override
	public final double eval() {
		return val;
	}

	/** Defaults to DoubleType, and treats Byte as unsigned. */
	private static final RealType<?> asType(final Number val) {
		final Class<? extends Number> c = val.getClass();
		if (c == Double.class) return new DoubleType(val.doubleValue());
		else if (c == Long.class) return new LongType(val.longValue());
		else if (c == Float.class) return new FloatType(val.floatValue());
		else if (c == Byte.class) return new UnsignedByteType(val.byteValue());
		else if (c == Integer.class) return new IntType(val.intValue());
		else if (c == Short.class) return new ShortType(val.shortValue());
		return new DoubleType(val.doubleValue());
	}

	@Override
	public final void findCursors(final Collection<Cursor<?>> cursors) {}

	@Override
	public IFunction duplicate()
	{
		return new NumberFunction(val);
	}
}
