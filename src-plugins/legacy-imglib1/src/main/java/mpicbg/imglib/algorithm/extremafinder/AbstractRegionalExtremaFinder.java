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

import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 */
public abstract class AbstractRegionalExtremaFinder<T extends RealType<T>> implements RegionalExtremaFinder<T> {

	protected OutOfBoundsStrategyFactory<T> outOfBoundsFactory = new OutOfBoundsStrategyValueFactory<T>();		// holds the outOfBoundsStrategy used by the cursors in this algorithm
	protected boolean allowEdgeMax = false;		// if true, maxima found on the edge of the images will be included in the results; if false, edge maxima are excluded
	protected boolean doInterpolate = false;
	protected Image<T> image;					// holds the image the algorithm is to be applied to
	final protected ArrayList< ArrayList< int[] > > maxima = new ArrayList< ArrayList< int[] > >();	// an array list which holds the coordinates of the maxima found in the image.
	protected T threshold;
	
	@Override
	public void allowEdgeExtrema(boolean flag) {
		this.allowEdgeMax = flag;
	}

	@Override
	public ArrayList< ArrayList<int[]>> getRegionalExtrema() {
		return maxima;
	}
	
	@Override
	public ArrayList< float[] > getRegionalExtremaCenters() {
		ArrayList<float[]> centeredRegionalMaxima = new ArrayList<float[]>();
		ArrayList<ArrayList<int[]>> regionalMaxima = new ArrayList<ArrayList<int[]>>(maxima); // make a copy
		ArrayList<int[]> curr = null;
		while (!regionalMaxima.isEmpty()) {
			curr = regionalMaxima.remove(0);
			float averagedCoord[] = findAveragePosition(curr);
			centeredRegionalMaxima.add(averagedCoord);
		}
		return centeredRegionalMaxima;
	}

	public void setThreshold(T threshold) {
		this.threshold = threshold;
	};
	

	/**
	 * Given an ArrayList of int[] (coordinates), computes the averaged coordinates and returns them.
	 * This will always return a 3-elements arrays, even for 2D.
	 * 
	 * @param searched
	 * @return
	 */
	protected float[] findAveragePosition(final ArrayList<int[]> coords) {
		int nDims = coords.get(0).length;
		final float[] array = new float[3];
		int[] curr;
		for (int j = 0; j < coords.size(); j++) {
			curr = coords.get(j);
			for (int i = 0; i<nDims; i++) 
				array[i] += curr[i];
		}
		for (int i = 0; i < array.length; i++)
			array[i] /= coords.size();
		return array;
	}
	
	@Override
	public void setOutOfBoundsStrategyFactory(OutOfBoundsStrategyFactory<T> strategy) {
		this.outOfBoundsFactory = strategy;		
	}
}
