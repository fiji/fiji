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
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/* An abstract class to facilitate implementing a function that takes two arguments.
 * Subclasses must call one of the constructors, which take as arguments two out an {@link Image},
 * an {@link IFunction}, or a {@link Number}.
 * 
 * Suppose you want a function that returns the average value of two values. You could write:
<code>
Image<> extends RealType<?>> img1 = ...,
                                img2 = ...;
IFunction avg = new Divide(new Add(img1, img2), 2);
</code>
 * 
 *   Or, instead, define your own Average function:
<code>
public class Average extends BinaryOperation
{
    public Average(final Image<? extends RealType<?>> left, final Image<? extends RealType<?>> right) {
		super(left, right);
	}

	public Average(final IFunction fn, final Image<? extends RealType<?>> right) {
		super(fn, right);
	}

	public Average(final Image<? extends RealType<?>> left, final IFunction fn) {
		super(left, fn);
	}

	public Average(final IFunction fn1, final IFunction fn2) {
		super(fn1, fn2);
	}
	
	public Average(final Image<? extends RealType<?>> left, final Number val) {
		super(left, val);
	}

	public Average(final Number val,final Image<? extends RealType<?>> right) {
		super(val, right);
	}

	public Average(final IFunction left, final Number val) {
		super(left, val);
	}

	public Average(final Number val,final IFunction right) {
		super(val, right);
	}
	
	public Average(final Number val1, final Number val2) {
		super(val1, val2);
	}

	public Average(final Object... elems) throws Exception {
		super(elems);
	}
	
	@Override
	public final double eval() {
		return (a() + b()) / 2;
	}
}
</code>
 */
public abstract class BinaryOperation extends FloatImageOperation
{
	private final IFunction a, b;

	public BinaryOperation(final Image<? extends RealType<?>> left, final Image<? extends RealType<?>> right) {
		this.a = new ImageFunction(left);
		this.b = new ImageFunction(right);
	}

	public BinaryOperation(final IFunction fn, final Image<? extends RealType<?>> right) {
		this.a = fn;
		this.b = new ImageFunction(right);
	}

	public BinaryOperation(final Image<? extends RealType<?>> left, final IFunction fn) {
		this.a = new ImageFunction(left);
		this.b = fn;
	}

	public BinaryOperation(final IFunction fn1, final IFunction fn2) {
		this.a = fn1;
		this.b = fn2;
	}

	public BinaryOperation(final Image<? extends RealType<?>> left, final Number val) {
		this.a = new ImageFunction(left);
		this.b = new NumberFunction(val);
	}

	public BinaryOperation(final Number val,final Image<? extends RealType<?>> right) {
		this.a = new NumberFunction(val);
		this.b = new ImageFunction(right);
	}

	public BinaryOperation(final IFunction fn, final Number val) {
		this.a = fn;
		this.b = new NumberFunction(val);
	}

	public BinaryOperation(final Number val, final IFunction fn) {
		this.a = new NumberFunction(val);
		this.b = fn;
	}

	public BinaryOperation(final Number val1, final Number val2) {
		this.a = new NumberFunction(val1);
		this.b = new NumberFunction(val2);
	}

	/** Compose: "fn(a1, fn(a2, fn(a3, fn(a4, a5))))".
	 *  Will fail with either {@link IllegalArgumentException} or {@link ClassCastException}
	 *  when the @param elem contains instances of classes other than {@link Image},
	 *  {@link Number}, or {@link IFunction}. */
	public BinaryOperation(final Object... elem) throws Exception {
		this.a = Util.wrap(elem[0]);
		IFunction right = Util.wrap(elem[elem.length-1]);
		for (int i=elem.length-2; i>0; i--) {
			IFunction fn = getClass().getConstructor(new Class<?>[]{IFunction.class, IFunction.class})
					.newInstance(Util.wrap(elem[i]), right);
			right = fn;
		}
		this.b = right;
	}

	@Override
	public final void findCursors(final Collection<Cursor<?>> cursors) {
		a.findCursors(cursors);
		b.findCursors(cursors);
	}

	public final IFunction a() { return a; }
	public final IFunction b() { return b; }

	public IFunction duplicate() throws Exception
	{
		return getClass().getConstructor(IFunction.class, IFunction.class).newInstance(a.duplicate(), b.duplicate());
	}
}
