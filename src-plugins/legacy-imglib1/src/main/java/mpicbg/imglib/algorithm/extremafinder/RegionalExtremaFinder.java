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

package mpicbg.imglib.algorithm.extremafinder;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * Provides a quick algorithm for finding regional maxima in 2- or 3-dimensional images.
 * 
 * @param <T>
 * @author Nick Perry
 */
public interface RegionalExtremaFinder<T extends RealType<T>> extends Algorithm, Benchmark {
	
	
	/**
	 * Set the threshold value under which (or over which, in the case of a minima finder) pixels are ignored.
	 * If <code>null</code>, then all pixels are taken into account.
	 */
	public void setThreshold(T threshold);
	
	/**
	 * Returns the ArrayList containing the coordinates of the local extrema found. Each 
	 * element of the ArrayList is a int array, representing the coordinate of the found
	 * extrema, in the same order that of the source {@link Image}.
	 * 
	 * @return  the ArrayList containing the extrema coordinates
	 */
	public ArrayList< ArrayList< int[] > > getRegionalExtrema();
	
	/**
	 * Computes the average coordinate of the extrema locations.
	 * This average coordinates are returned, and represents the "center" of the regional maximum.
	 * Note that this is not guaranteed to be in the regional maximum itself (imagine a regional maximum
	 * that is a ring shape; the direct center is not part of the regional maximum itself, but is what
	 * would be computed by this function). Return <code>null</code> if the {@link #process()} method
	 * has not bee called.
	 * 
	 * @return The coordinates of the "center pixel" of the regional maximum. It will always be 
	 * a 3-elements float arrays, even for 2D case.
	 */
	public ArrayList<float[]> getRegionalExtremaCenters();
		
	/**
	 * If set to true before the {@link #process()} method is called, then extrema found 
	 * at the edges of the image bounds (including time edges) will not be pruned, and will
	 * be included in the result array.
	 * @param flag
	 */
	public void allowEdgeExtrema(boolean flag);
	
	/**
	 * Set the strategy used by this extrema finder to deal with edge pixels.
	 * By default, it is an {@link OutOfBoundsStrategyValueFactory} set with the value
	 * 0 to avoid nasty edge effects.
	 * @param strategy  the strategy to set
	 */
	void setOutOfBoundsStrategyFactory(OutOfBoundsStrategyFactory<T> strategy);
}
