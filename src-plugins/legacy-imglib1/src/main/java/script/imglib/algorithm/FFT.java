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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.algorithm.fft.FourierTransform;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.complex.ComplexDoubleType;
import script.imglib.math.Compute;
import script.imglib.math.fn.IFunction;

/**
 * TODO
 *
 */
public class FFT<T extends RealType<T>> extends Image<ComplexDoubleType>
{
	static private Map<Thread,FourierTransform<?, ComplexDoubleType>> m =
		Collections.synchronizedMap(new HashMap<Thread,FourierTransform<?, ComplexDoubleType>>());

	final FourierTransform<T, ComplexDoubleType> fft;
	final T value;

	@SuppressWarnings("unchecked")
	public FFT(final Image<T> img) throws Exception {
		super(process(img).getContainer(), new ComplexDoubleType(), "FFT");
		fft = (FourierTransform<T, ComplexDoubleType>) m.remove(Thread.currentThread());
		value = img.createType();
	}

	@SuppressWarnings("unchecked")
	public FFT(final IFunction fn) throws Exception {
		this((Image)Compute.inDoubles(fn));
	}

	static synchronized private final <T extends RealType<T>> Image<ComplexDoubleType> process(final Image<T> img) throws Exception {
		final FourierTransform<T, ComplexDoubleType> fft = new FourierTransform<T, ComplexDoubleType>(img, new ComplexDoubleType());
		if (!fft.checkInput() || !fft.process()) {
			throw new Exception("FFT: failed to process for image " + img.getClass() + " -- " + fft.getErrorMessage());
		}
		m.put(Thread.currentThread(), fft);
		return fft.getResult();
	}
}
