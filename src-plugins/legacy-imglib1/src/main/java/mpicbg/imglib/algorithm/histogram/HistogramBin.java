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

package mpicbg.imglib.algorithm.histogram;

import mpicbg.imglib.type.Type;

/**
 * HistogramBin is used by {@Histogram} to represent the bins of a histogram.
 * This sentence is very helpful.
 * 
 * @param <T> the type of {@link Type} corresponding to this HistogramBin. 
 * @author Larry Lindsey
 */
public abstract class HistogramBin<T extends Type<T>> {

	/**
	 * The {@link Type} corresponding to the center of this HistogramBin.
	 */
	private final T center;
	
	/**
	 * A HistogramKey that may be used to key this HistogramBin in a 
	 * hash table.
	 */
	private final HistogramKey<T> key;
	
	/**
	 * The count of how many things are binned into this HistogramBin.
	 */
	private long count;
	
	/**
	 * Create a HistogramBin centered at {@Type} t, and keyed by 
	 * HistogramKey k.
	 * @param t the new HistogramBin's center.
	 * @param k a HistogramKey that may be used to key this HistogramBin.
	 */
	public HistogramBin(T t, HistogramKey<T> k)
	{
		center = t;
		key = k;
		count = 0;
	}
	
	/**
	 * Increment the count.
	 */
	public void inc()
	{
		++count;
	}
		
	public long getCount()
	{
		return count;	
	}

	/**
	 * Gets a HistogramKey that may be used to key this HistogramBin into a 
	 * hash table. 
	 * @return a HistogramKey that may be used to key this HistogramBin into a 
	 * hash table.
	 */
	public HistogramKey<T> getKey()
	{
		return key;
	}

	/**
	 * Returns the norm of this bin.  For most discrete purposes, this should 
	 * just be equal to 1.  When a bin may have varying-width, this should 
	 * return the difference between the upper and lower bound.
	 * @return the norm, or effective width of this HistogramBin.
	 */
	public double getNorm()
	{
		return 1;
	}
		
	public T getCenter()
	{
		return center;
	}
	
	/**
	 * Returns the lower bound of this bin.  In the case that this bin
	 * represents a discrete value, the lower bound should be equal to 
	 * the bin center.
	 * @return this bin's lower bound.
	 */
	public abstract T getLowerBound();

	/**
	 * Returns the upper bound of this bin.  In the case that this bin
	 * represents a discrete value, the upper bound should be equal to 
	 * the bin center.
	 * @return this bin's upper bound.
	 */
	public abstract T getUpperBound();
}
