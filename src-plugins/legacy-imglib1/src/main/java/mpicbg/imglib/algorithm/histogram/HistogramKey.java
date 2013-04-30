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
 * HistogramKeys are used by {@link Histogram} to key {@link HistogramBin}s 
 * into a {@link HashTable}.
 * @param <T> the type of {@link Type} that this HistogramKey pertains to.
 *
 * @author Larry Lindsey
 */
public class HistogramKey<T extends Type<T>>
{
	/**
	 * The hash code returned by hashCode().
	 */
	private final int code;
	
	/**
	 * The HistogramBinFactory that generated this HistogramKey.
	 */
	private final HistogramBinFactory<T> keyFactory;
	
	/**
	 * A representative {@link Type}.
	 */
	private final T type;
	
	/**
	 * Create a HistogramKey with hash code hc, representative {@Type} t, and
	 * HistogramBinFactory factory.
	 * @param hc the hash code to be returned by this HistogramKey's hashCode().
	 * @see Object#hashCode()
	 * @param t the representative {@link Type}.  Preferably, this should be
	 * the center Type of the HistogramBin to be keyed, but it only need be one
	 * that would be binned into that bin.
	 * @param factory the HistogramBinFactory corresponding to this key.
	 * Typically, this should be the HistogramBinFactory that generated this
	 * key.
	 */
	public HistogramKey(int hc, T t, HistogramBinFactory<T> factory)
	{
		code = hc;
		keyFactory = factory;
		type = t;
	}
	
	@Override
	public int hashCode()
	{
		return code;
	}

	
	/**
	 * Determines whether this HistogramKey is equal to the given Object.
	 * This HistogramKey is equal to {@code o} iff {@code o} is also a
	 * HistogramKey, and {@link HistogramBinFactory#equivalent(Type, Type)}
	 * returns true when called with this HistogramKey's representative type
	 * and the one corresponding to {@code o}.
	 * @param o the Object against which to check for equality.
	 * @return true if this HistogramKey is equal to o. 
	 */	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o)
	{
		if (o instanceof HistogramKey)
		{
			if (this.getClass().isInstance(o))
			{
				HistogramKey<T> key = (HistogramKey<T>) o;
				return keyFactory.equivalent(type, key.type);
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	
	public T getType()
	{
		return type;
	}
}
