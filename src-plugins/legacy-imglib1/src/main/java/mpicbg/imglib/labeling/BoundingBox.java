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

import java.util.Arrays;

/**
 * The bounding box that contains a region from the minimum inclusive to
 * the maximum non-inclusive.
 * 
 * @author Lee Kamentsky
 * @author leek
 */
public class BoundingBox {
	protected int [] min;
	protected int [] max;
	public BoundingBox(int dimensions) {
		min = new int [dimensions];
		max = new int [dimensions];
		Arrays.fill(max, Integer.MIN_VALUE);
		Arrays.fill(min, Integer.MAX_VALUE);
	}
	
	/**
	 * @return the # of dimensions in the bounding box's space (e.g. 2, 3)
	 */
	public int getDimensions() {
		return min.length;
	}
	/**
	 * the minimum and maximum extents of the box
	 * @param destMin - on input, an array of at least size D, the dimension of
	 * the space. On output, the minimum extents of the bounding box.
	 * @param destMax - on output, the maximum extents of the bounding box.
	 */
	public void getExtents(int [] destMin, int [] destMax) {
		if (destMin != null)
			System.arraycopy(min, 0, destMin, 0, min.length);
		if (destMax != null)
			System.arraycopy(max, 0, destMax, 0, max.length);
	}

	/**
	 * update the minimum and maximum extents with the given coordinates.
	 * @param coordinates
	 */
	public void update(final int [] coordinates) {
		for (int i = 0; i<min.length; i++) {
			if (coordinates[i] < min[i]) min[i] = coordinates[i];
			if (coordinates[i] >= max[i]) max[i] = coordinates[i]+1;
		}
	}
}
