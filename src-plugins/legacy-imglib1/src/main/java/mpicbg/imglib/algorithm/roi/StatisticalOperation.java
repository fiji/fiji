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

package mpicbg.imglib.algorithm.roi;

import java.util.LinkedList;

import mpicbg.imglib.algorithm.ROIAlgorithm;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;

/**
 * StatisticalOperation provides the framework to create Order Statistic operations.  It operates
 * by cursing over the input {@link Image}, and collecting a sorted list of the pixels "covered" by
 * a {@link StructuringElement}.  This list is made available to children classes, which are
 * responsible for setting the pixel value at the current position in the output Image.
 * 
 * @param <T> The input- and output-{@link Image} type.
 * @author Larry Lindsey
 */
public abstract class StatisticalOperation<T extends RealType<T>> extends ROIAlgorithm<T, T> {
	//Member classes
	
	/**
	 * Implements a strategy for populating the sorted list associated with this
	 * StatisticalOperation.
	 * 
	 * This could mean either re-populating and resorting this list on every iteration,
	 * which might be reasonably efficient for small structuring elements, or it could
	 * mean being smart about the boundaries, since we expect our cursor to move maybe
	 * only one position for every iteration.
	 * 
	 * @param <R> Image storage type.
	 */
	public interface StatisticsCollectionStrategy<R extends RealType<R>> 
	{
		public void collectStats(LinkedList<R> list, RegionOfInterestCursor<R> cursor, int[] pos);
	}
	
	/**
	 * Simple, dumb statistics collection implementation.  Resorts every time, hopefully in a
	 * O(n log(n)) manner, with respect to strel size.
	 * 
	 * @param <R> Image storage type.
	 */
	public class SimpleCollectionStrategy<R extends RealType<R>> 
		implements StatisticsCollectionStrategy<R>
	{
		private final LocalizableByDimCursor<BitType> strelCursor;
		
		public SimpleCollectionStrategy()
		{
			strelCursor = strel.createLocalizableByDimCursor();
		}
		
		public void collectStats(LinkedList<R> list, RegionOfInterestCursor<R> cursor, int[] pos)
		{
			list.clear();
			
			while(cursor.hasNext())
			{
				cursor.fwd();
				strelCursor.setPosition(cursor);
				
				if (strelCursor.getType().get())
				{
					R type = cursor.getType().copy();
					int i = 0;
					while(i < list.size() && type.compareTo(list.get(i)) > 0)
					{
						++i;
					}
					list.add(i, type.copy());							
				}
						
			}
		}
		
	}
	
	/*
	 * As of this writing, there are no other collection strategy classes written, as you can tell.
	 * 
	 * To be clear, here we're concerned with strel operations, ie, operations of a shaped element
	 * with respect to an input image.
	 * 
	 * Now, here's how I intend to implement a more efficient strel operation.  If you're reading
	 * this, maybe you'll get to it ahead of me ;).
	 * 
	 * First, the StructuringElement class will be updated so that it can produce cursors that
	 * iterate over its edges with respect to each dimension (two per dimension - one for positive
	 * motion, the other for negative).  This class will use those cursors in tandem with the ROI
	 * cursor to store image data from the region of interest, in the current state.  In the next
	 * state, the strel patch will have moved.  If it moves a distance of only one pixel, we can do
	 * the following:
	 * 
	 * One of the lists we stored in the previous state will contain exactly the values that need
	 * to be removed from the sorted list, so remove them.  This should be an O(n log(n)) operation
	 * in the number of edge values.  Next, grab the cursor from the strel that corresponds to the
	 * location of the pixels that must be added newly.  Iterate over those pixels, adding their
	 * values to the sorted list.  This should also be O(n log(n)) in the number of edge pixels.
	 * 
	 * Note that here, n is approximately the dth root of the n in the simple collection method,
	 * meaning we get a pretty nice speed-up when n is big enough.  In particular, n should be
	 * large with respect to d + 1, where d is the dimensionality.
	 */
	
	//Member variables
	
	private final StructuringElement strel;
	private final LinkedList<T> statList;
	private final int[] lastPosition;
	private final LocalizableByDimCursor<T> outputCursor;
	private boolean init = false;
	private StatisticsCollectionStrategy<T> statsStrategy;

	
	//Member functions
	
	public StatisticalOperation(final Image<T> imageIn, final StructuringElement strel) {
		this(imageIn, strel, null);
	}
	
	public StatisticalOperation(final Image<T> imageIn, final StructuringElement inStrel,
			final OutOfBoundsStrategyFactory<T> inOutFactory) {
		super(imageIn.createType(), imageIn, inStrel.getDimensions(), inOutFactory);
		strel = inStrel;
		statList = new LinkedList<T>();
		lastPosition = new int[strel.getNumDimensions()];
		outputCursor = getOutputImage().createLocalizableByDimCursor();
		statsStrategy = new SimpleCollectionStrategy<T>();		
	}

	public void reset()
	{
		init = false;
	}

	public boolean isInit()
	{
		return init;
	}
	
	public StructuringElement getStrel()
	{
		return strel;
	}
	
	protected LinkedList<T> getList()
	{
		return statList;
	}
	
	public int[] getLastPosition()
	{
		return lastPosition;
	}
	
	public void close()
	{
		super.close();
		outputCursor.close();
	}

	@Override
	public boolean checkInput()
	{
		return super.checkInput() && outputCursor.isActive();
	}
	
	@Override
	protected boolean patchOperation(final int[] position,
			final RegionOfInterestCursor<T> cursor) {
		statsStrategy.collectStats(statList, cursor, position);
		outputCursor.setPosition(position);
		
		statsOp(outputCursor);
		
		System.arraycopy(position, 0, lastPosition, 0, position.length);
		init = true;
		return true;
	}

	/*
	I can't remember the reason behind the decision to pass a cursor to statsOp rather than a type.
	It's possible that I'll change this later.  It doesn't make sense to me to give a child class the
	power to set the position of the output Image's cursor.
	
	-Larry
	*/
	
	/**
	 * Perform the order statistic operation, then set the value of the type of the given cursor.
	 * @param cursor
	 */
	protected abstract void statsOp(LocalizableByDimCursor<T> cursor);
	
}
