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

/* An abstract class to facilitate implementing a function that takes one argument.
 * Subclasses must call one of the three constructors: for an {@link Image}, an {@link IFunction},
 * or for a {@link Number}.
 * 
 * Here is an example. Suppose you want a function that adds 42:
 * 
 <code>
 import mpicbg.imglib.scripting.math.fn.IFunction;
 import mpicbg.imglib.scripting.math.fn.UnaryOperation;
 import mpicbg.imglib.image.Image;
 
 public class Add42 extends UnaryOperation {
     public Add42(Image<? extends RealType<?>> img) {
         super(img);
     }
     public Add42(IFunction fn) {
         super(fn);
     }
     public Add42(Number val) {
         super(val);
     }
     
     public final double eval() {
         return a() + 42;
     }
 }
 </code>
 *
 * The new Add42 function created above will interact with any other of the math functions,
 * or with any other class implementing IFunction.
 *
 */
public abstract class UnaryOperation extends FloatImageOperation
{
	private final IFunction a;

	public UnaryOperation(final Image<? extends RealType<?>> img) {
		this.a = new ImageFunction(img);
	}

	public UnaryOperation(final IFunction fn) {
		this.a = fn;
	}

	public UnaryOperation(final Number val) {
		this.a = new NumberFunction(val);
	}

	@Override
	public final void findCursors(final Collection<Cursor<?>> cursors) {
		a.findCursors(cursors);
	}

	/** Call a().eval() to obtain the result as a double of the computation encapsulated by the @field a. 
	 *  @returns the IFunction @field a*/
	public final IFunction a() { return a; }
	
	public IFunction duplicate() throws Exception
	{
		return getClass().getConstructor(IFunction.class).newInstance(a.duplicate());
	}
}
