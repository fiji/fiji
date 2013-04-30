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
 * The HistogramBinFactory interface is used by {@link Histogram} to generate
 * {@link HistogramBin}s and {@link HistogramKey}s.  In addition, a
 * HistogramBinFactory implements a method used to determine if two Type's are
 * equivalent to each other, much as in .equals(). 
 * @param <T> The type of {@link Type} that this HistogramBinFactory pertains
 * to.
 *
 * @author Larry Lindsey
 */
public interface HistogramBinFactory <T extends Type<T>>{

	/**
	 * Create a {@link HistogramKey} to be used for a {@link HashTable}, to key
	 * a {@link HistogramBin}.
	 * @param type the {@link Type} to which the generated HistogramKey will
	 * correspond.
	 * @return A HistogramKey corresponding to the given Type.
	 */
	 public HistogramKey<T> createKey(T type);
	
	 /**
	  * Determines whether two {@link Type}s are equivalent in the sense that
	  * they would be binned into the same HistogramBin.
	  * @param type1 the first Type for comparison
	  * @param type2 the second Type for comparison
	  * @return true, if the the types in question are equivalent, generally
	  * meaning that the values that they contain would be placed into the same
	  * histogram bin, and false otherwise.
	  */
	 public boolean equivalent(T type1, T type2);
	 
	 /**
	  * Create a {@link HistogramBin} corresponding to the give {@link Type},
	  * in the sense that for the purposes of the {@link Histogram} class,
	  * the given Type would be binned into the returned HistogramBin.
	  * @param type the prototype Type used to create the HistogramBin
	  * @return a HistogramBin, into which the given type would be binned.
	  */
	 public HistogramBin<T> createBin(T type);
}
