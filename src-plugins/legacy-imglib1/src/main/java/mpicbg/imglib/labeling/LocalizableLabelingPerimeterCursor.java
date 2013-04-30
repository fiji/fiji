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

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;

/**
 * TODO
 *
 * @author Lee Kamentsky
 */
public class LocalizableLabelingPerimeterCursor<T extends Comparable<T>>
		extends LocalizableLabelingCursor<T> {

	LocalizableByDimCursor<LabelingType<T>> cc;
	int [] dimensions;
	int [] position;
	public LocalizableLabelingPerimeterCursor(
			LocalizableCursor<LabelingType<T>> c,
			LocalizableByDimCursor<LabelingType<T>> cc, T label) {
		super(c, label);
		this.cc = cc;
		dimensions = cc.getDimensions();
		position = c.createPositionArray();
	}

	public LocalizableLabelingPerimeterCursor(
			RegionOfInterestCursor<LabelingType<T>> c, int[] offset,
			LocalizableByDimCursor<LabelingType<T>> cc, T label) {
		super(c, offset, label);
		this.cc = cc;
		dimensions = cc.getDimensions();
		position = c.createPositionArray();
	}
	@Override
	protected boolean isLabeled() {
		if (! super.isLabeled()) {
			return false;
		}
		/*
		 * Check to see if the current location is 4-connected to an
		 * unlabeled pixel.
		 */
		getCursorPosition(position);
		for (int i = 0; i < position.length; i++) {
			/*
			 * At each coordinate, try the pixel +/- 1
			 */
			for (int j = -1; j <=1; j+= 2) {
				if ((j == -1) && (position[i] == 0)) continue;
				if ((j == 1) && (position[i] == dimensions[i]-1)) continue;
				position[i] += j;
				cc.setPosition(position);
				position[i] -= j;
				boolean match = false;
				for (T l:cc.getType().getLabeling() ) {
					if (l.equals(this.label)) {
						match = true;
						break;
					}
				}
				if (! match) return true;
			}
		}
		return false;
	}
}
