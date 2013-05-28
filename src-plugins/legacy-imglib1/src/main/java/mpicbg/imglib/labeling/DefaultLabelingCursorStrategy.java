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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.type.label.FakeType;

/**
 * A relatively conservative strategy suitable for blobby objects - 
 * retain the bounding boxes and raster starts and reconstruct the 
 * cursors by scanning.
 * 
 * @param <T>
 * @param <L>
 * @author Lee Kamentsky
 * @author leek
 */
public class DefaultLabelingCursorStrategy<T extends Comparable<T>, L extends Labeling<T>>
		implements LabelingCursorStrategy<T, L> {

	final protected L labeling;
	protected long generation;
	protected LabelingType<T> type = null;
	
	private class LabelStatistics extends BoundingBox {
		private int [] rasterStart;
		private long area = 0;
		public LabelStatistics(int dimensions) {
			super(dimensions);
			rasterStart = new int [dimensions];
			Arrays.fill(rasterStart, Integer.MAX_VALUE);
		}
		
		public void getRasterStart(int [] dest) {
			System.arraycopy(rasterStart, 0, dest, 0, rasterStart.length);
		}
		public long getArea() {
			return area;
		}
		public void update(int [] coordinates) {
			super.update(coordinates);
			area++;
			for (int i = 0; i<rasterStart.length; i++) {
				if (rasterStart[i] > coordinates[i]) {
					System.arraycopy(coordinates, 0, rasterStart, 0, rasterStart.length);
					return;
				} else if (rasterStart[i] < coordinates[i]) {
					return;
				}
			}
		}
	}
	protected Map<T, LabelStatistics> statistics;
	public DefaultLabelingCursorStrategy(L labeling) {
		this.labeling = labeling;
		generation = Long.MIN_VALUE;
	}

	/**
	 * Compute all statistics on the labels if cache is dirty.
	 */
	protected void computeStatistics() {
		if ((type == null) || (type.getGeneration() != generation)) {
			statistics = new HashMap<T, LabelStatistics>();
			LocalizableCursor<LabelingType<T>> c = labeling.createLocalizableCursor();
			if (type == null) {
				type = c.getType();
			}
			int [] position = c.createPositionArray();
			LabelStatistics last = null;
			T lastLabel = null;
			for (LabelingType<T> t:c) {
				c.getPosition(position);
				for (T label:t.getLabeling()) {
					if ((last == null) || (! label.equals(lastLabel))) {
						lastLabel = label;
						last = statistics.get(label);
						if (last == null) {
							last = new LabelStatistics(c.getNumDimensions());
							statistics.put(label, last);
						}
					}
					last.update(position);
				}
			}
			generation = type.getGeneration();
			c.close();
		}
	}
	@Override
	public LocalizableCursor<FakeType> createLocalizableLabelCursor(T label) {
		int [] offset = new int[labeling.getNumDimensions()];
		int [] size = new int[labeling.getNumDimensions()];
		getExtents(label, offset, size);
		for (int i=0; i<offset.length; i++) size[i] -= offset[i];
		
		LocalizableByDimCursor<LabelingType<T>> c = labeling.createLocalizableByDimCursor();
		RegionOfInterestCursor<LabelingType<T>> roiCursor = new RegionOfInterestCursor<LabelingType<T>>(c, offset, size);
		return new LocalizableLabelingCursor<T>(roiCursor, offset, label);
	}

	@Override
	public LocalizableCursor<FakeType> createLocalizablePerimeterCursor(T label) {
		int [] offset = new int[labeling.getNumDimensions()];
		int [] size = new int[labeling.getNumDimensions()];
		getExtents(label, offset, size);
		for (int i=0; i<offset.length; i++) size[i] -= offset[i];
		LocalizableByDimCursor<LabelingType<T>> c = labeling.createLocalizableByDimCursor();
		RegionOfInterestCursor<LabelingType<T>> roiCursor = new RegionOfInterestCursor<LabelingType<T>>(c, offset, size);
		LocalizableByDimCursor<LabelingType<T>> cc = labeling.createLocalizableByDimCursor();
		return new LocalizableLabelingPerimeterCursor<T>(roiCursor, offset, cc, label);
	}

	@Override
	public boolean getExtents(T label, int[] minExtents, int[] maxExtents) {
		computeStatistics();
		LabelStatistics stats = statistics.get(label);
		if (stats == null) {
			if (minExtents != null)
				Arrays.fill(minExtents, 0);
			if (maxExtents != null)
				Arrays.fill(maxExtents, 0);
			return false;
		} else {
			stats.getExtents(minExtents, maxExtents);
			return true;
		}
	}

	@Override
	public boolean getRasterStart(T label, int[] start) {
		computeStatistics();
		LabelStatistics stats = statistics.get(label);
		if (stats == null) {
			Arrays.fill(start, 0);
			return false;
		} else {
			stats.getRasterStart(start);
			return true;
		}
	}

	@Override
	public long getArea(T label) {
		computeStatistics();
		LabelStatistics stats = statistics.get(label);
		if (stats == null) {
			return 0;
		}
		return stats.getArea();
	}

	@Override
	public Collection<T> getLabels() {
		computeStatistics();
		return statistics.keySet();
	}

}
