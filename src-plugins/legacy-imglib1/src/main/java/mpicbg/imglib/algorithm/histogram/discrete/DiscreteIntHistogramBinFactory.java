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

package mpicbg.imglib.algorithm.histogram.discrete;

import mpicbg.imglib.algorithm.histogram.HistogramBin;
import mpicbg.imglib.algorithm.histogram.HistogramBinFactory;
import mpicbg.imglib.algorithm.histogram.HistogramKey;
import mpicbg.imglib.type.numeric.IntegerType;

/**
 * A HistogramBinFactory to be used to create a discrete Histogram over
 * integer-valued Type's.
 * @param <T> the type of {@link Type} corresponding to this factory, implementing IntegerType.
 *
 * @author Larry Lindsey
 */
public class DiscreteIntHistogramBinFactory<T extends IntegerType<T>> implements HistogramBinFactory<T>
{
	public class DiscreteIntHistogramBin extends HistogramBin<T>
	{

		public DiscreteIntHistogramBin(T t, HistogramKey<T> k) {
			super(t, k);
		}

		@Override
		public T getLowerBound() {
			return getCenter();
		}

		@Override
		public T getUpperBound() {
			return getCenter();
		}
		
	}
	
	@Override
	public HistogramBin<T> createBin(T type) {		
		return new DiscreteIntHistogramBin(type, createKey(type));
	}

	@Override
	public HistogramKey<T> createKey(T type) {
		return new HistogramKey<T>((new Double(type.getIntegerLong())).hashCode(),
				type.copy(), this);
	}

	@Override
	public boolean equivalent(T type1, T type2) {
		return type1.getIntegerLong() == type2.getIntegerLong();
	}
	
}
