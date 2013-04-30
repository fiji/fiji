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

package mpicbg.imglib.algorithm.histogram.arbitrary;

import mpicbg.imglib.algorithm.histogram.HistogramBin;
import mpicbg.imglib.algorithm.histogram.HistogramBinFactory;
import mpicbg.imglib.algorithm.histogram.HistogramKey;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Larry Lindsey
 */
public class RealHistogramBinFactory<T extends RealType<T>> implements HistogramBinFactory<T>
{
	
	public class RealHistogramBin extends HistogramBin<T>
	{

		public RealHistogramBin(T t, HistogramKey<T> k) {
			super(t, k);
		}

		@Override
		public T getLowerBound() {
			T lower = getCenter().createVariable();
			lower.setReal(getCenter().getRealDouble() - width / 2);
			return lower;
		}

		@Override
		public T getUpperBound() {
			T upper = getCenter().createVariable();
			upper.setReal(getCenter().getRealDouble() - width / 2);
			return upper;
		}
		
		@Override
		public double getNorm()
		{
			return width;
		}
		
	}
	
	
	private final double width;
	private final double offset;
	
	public RealHistogramBinFactory(double w, double d)
	{
		width = w;
		offset = d;
	}
	
	private double mapToCenter(double val)
	{
		/*
		 * This way, bins are closed at the lower bound and open at the upper.
		 * To make them closed at the upper bound and open at the lower, do
		 * r = Math.ceil((val - offset) / width) - .5;
		 */
		double r = Math.floor((val - offset) / width) + .5;
		return r * width + offset;
	}
	
	private T centerType(T type)
	{
		double center = mapToCenter(type.getRealDouble());
		T out = type.createVariable();
		out.setReal(center);
		return out;
	}
	
	@Override
	public HistogramBin<T> createBin(T type) {
		T center = centerType(type);
		return new RealHistogramBin(center, createKey(center));		
	}

	@Override
	public HistogramKey<T> createKey(T type) {
		HistogramKey<T> key;
		T center = centerType(type);
		int hc = (new Double(center.getRealDouble())).hashCode();
		
		key = new HistogramKey<T>(hc, center, this);
		
		return key;
	}

	@Override
	public boolean equivalent(T type1, T type2) {		
		return mapToCenter(type1.getRealDouble()) ==
			mapToCenter(type2.getRealDouble());
	}

}
