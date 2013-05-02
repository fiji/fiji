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

import java.util.Collection;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.DoubleType;
import script.imglib.math.Compute;
import script.imglib.math.fn.IFunction;

/** A convenience wrapper for just about any ImgLib {@link Algorithm}.
 *  When combined with other {@link IFunction} instances, the {@code eval()}
 *  method returns the resulting, processed pixel values--either from the result
 *  image (from an {@link OutputAlgorithm} instance) or from the origina image
 *  (in the case of a {@link Algorithm} instance that is not an {@link OutputAlgorithm}).
 *  
 *  A few classes extend Process for simplified usage. See for example
 *  {@link Gauss}, {@link Dither}, {@link BandpassFilter}, {@Normalize}.
 *  */
/**
 * TODO
 *
 */
public class Process implements IFunction {

	private final Cursor<? extends RealType<?>> c;

	/** Execute the given {@link OutputAlgorithm} and prepare a cursor to deliver
	 *  its pixel values one by one in successive calls to {@code eval()}. */
	public Process(final OutputAlgorithm<? extends RealType<?>> algorithm) throws Exception {
		execute(algorithm);
		this.c = algorithm.getResult().createCursor();
	}

	/** Same as {@code this(algorithmClass, Process.asImage(fn), parameters);} */
	public Process(final Class<Algorithm> algorithmClass, final IFunction fn, final Object... parameters) throws Exception {
		this(algorithmClass, Compute.inDoubles(fn), parameters);
	}

	/** Initialize and execute the given {@link Algorithm} and prepare a cursor
	 *  to deliver its pixel values one by one in successive calls to {@code eval()}.
	 *  If the @param algorithmClass implements {@link OutputAlgorithm}, then
	 *  this IFunction will iterate over the image obtained from {@code getResult()}.
	 *  Otherwise, this IFunction iterates over the modified input image.
	 *  
	 *  The {@link Algorithm} is constructed by reflection and assumes that the
	 *  constructor accepts one {@link Image} as first argument and then the rest of
	 *  the argument in the same order as given. Otherwise, an {@link Exception} will
	 *  be thrown. */
	@SuppressWarnings("unchecked")
	public Process(final Class<Algorithm> algorithmClass, final Image<? extends RealType<?>> img, final Object... parameters) throws Exception {
		final Class<?>[] cargs = new Class[1 + parameters.length];
		final Object[] args = new Object[cargs.length];
		cargs[0] = Image.class;
		args[0] = img;
		for (int i=1; i<cargs.length; i++) {
			cargs[i] = parameters[i-1].getClass();
			args[i] = parameters[i-1];
		}
		final Algorithm a = algorithmClass.getConstructor(cargs).newInstance(args);
		execute(a);
		
		this.c = (a instanceof OutputAlgorithm<?> ?
				((OutputAlgorithm<? extends RealType<?>>)a).getResult() : img).createCursor();
	}

	/** Evaluate the @param fn for every pixel and return a new {@link Image} with the result.
	 *  This method enables {@link Algorithm} instances to interact with {@link IFunction},
	 *  by creating an intermediate (and temporary) {@link DoubleType} {@link Image}. */

	private final void execute(final Algorithm algorithm) throws Exception {
		if (!algorithm.checkInput() || !algorithm.process()) {
			throw new Exception("Algorithm " + algorithm.getClass().getSimpleName() + " failed: " + algorithm.getErrorMessage());
		}
	}

	private Process(final Cursor<? extends RealType<?>> c) {
		this.c = c;
	}

	public Image<? extends RealType<?>> getResult() {
		return c.getImage();
	}

	@Override
	public final IFunction duplicate() throws Exception {
		return new Process(c.getImage().createCursor());
	}

	@Override
	public final double eval() {
		c.fwd();
		return c.getType().getRealDouble();
	}

	@Override
	public final void findCursors(final Collection<Cursor<?>> cursors) {
		cursors.add(c);
	}
}
