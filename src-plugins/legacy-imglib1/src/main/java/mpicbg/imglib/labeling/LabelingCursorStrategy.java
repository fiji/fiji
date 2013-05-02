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
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package mpicbg.imglib.labeling;

import java.util.Collection;

import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.type.label.FakeType;

/**
 * A labeling cursor strategy provides label cursors, bounds and other
 * potentially cacheable parameters on the labeling. It may be advantageous
 * to find all pixels for each label only once for sparse labelings,
 * small spaces and labelings that do not change during cursor use.
 * 
 * @param <T>
 * @param <L>
 */
/**
 * @param <T>
 * @param <L>
 *
 * @author Lee Kamentsky
 * @author leek
 */
public interface LabelingCursorStrategy<T extends Comparable<T>, L extends Labeling<T>> {
	/**
	 * A localizable cursor that iterates over every pixel with the given
	 * label.
	 * 
	 * @param label - the label of the pixels to be traversed
	 * @return a localizable cursor that can be used to find the position
	 * of every pixel with the given label.
	 */
	public LocalizableCursor<FakeType> createLocalizableLabelCursor(T label);

	/**
	 * A localizable cursor that iterates over every pixel in the perimeter
	 * of the labeled object. Given a space S(x[1],...x[D]) of dimension D, 
	 * a pixel is a perimeter pixel of a label if the pixel has that label and
	 * there is some pixel S(x[1]+y[1],..x[D]+y[D]) that is not labeled
	 * for sum(abs(y[i in 1..D])) == 1 - in other words, the pixel is
	 * adjacent to a non-labeled pixel where diagonals don't count.
	 *  
	 * @param label - the label to be traversed
	 * @return a cursor over the perimeter of the labeled object
	 */
	public LocalizableCursor<FakeType> createLocalizablePerimeterCursor(T label);
	
	/**
	 * Get the extents of the bounding box around the labeled object
	 * @param label - the label of the object in question
	 * @param minExtents - on input, must be an array of size D or larger
	 * where D is the dimension of the space. On output, the minimum value 
	 * of the pixel coordinates for the labeled object is stored in the array.
	 * @param maxExtents - the maximum value of the pixel coordinates for
	 * the labeled object.
	 * @return true if some pixel has the label
	 */
	public boolean getExtents(T label, int [] minExtents, int [] maxExtents);
	
	/**
	 * The coordinates of the first pixel with the given label when raster
	 * scanning through the space.
	 * 
	 * There is a class of algorithms on labeled objects that trace around
	 * the perimeter or surface of the object, starting with the first
	 * pixel encountered in a raster scan (given a space S(x[1],..x[D])
	 * of dimension D, sort the coordinates of the labeled pixels by increasing
	 * x[1], then increasing x[2], etc and pick the first in the list). This
	 * function can be used to find the pixel.
	 * 
	 * Note: the algorithms typically assume that all similarly labeled pixels are
	 * connected and that either there are no holes or holes are not
	 * significant.
	 * 
	 * @param label - search on pixels of this label
	 * @param start - on input, an array of at least size D, on output
	 * the coordinates of the raster start.
	 * @return true if some pixel has the label
	 */
	public boolean getRasterStart(T label, int []start);
	/**
	 * Return the area of the labeled object in units of pixels or the volume
	 * of the labeled object in voxels (or the hyper-volume of the 4/5-d
	 * object in ?toxels? or ?spectracels? or ?spectratoxels?)
	 * @return the area of the labeled object in pixels.
	 */
	public long getArea(T label);
	/**
	 * Find all of the labels used to label pixels.
	 * @return array of labels used.
	 */
	public Collection<T> getLabels();
}
