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

package mpicbg.imglib.algorithm.labeling;

import java.util.List;
import java.util.PriorityQueue;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.labeling.LabelingType;
import mpicbg.imglib.type.numeric.ComplexType;

/**
 * Watershed algorithms. The watershed algorithm segments and labels an image
 * using an analogy to a landscape. The image intensities are turned into
 * the z-height of the landscape and the landscape is "filled with water"
 * and the bodies of water label the landscape's pixels. Here is the
 * reference for the original paper:
 * 
 * Lee Vincent, Pierre Soille, Watersheds in digital spaces: An efficient
 * algorithm based on immersion simulations, IEEE Trans. Pattern Anal.
 * Machine Intell., 13(6) 583-598 (1991)
 * 
 * Watersheds are often performed on the gradient of an intensity image
 * or one where the edges of the object boundaries have been enhanced.
 * The resulting image has a depressed object interior and a ridge which
 * constrains the watershed boundary.
 * 
 * @author Lee Kamentsky
 */
public class Watershed {
	protected static class PixelIntensity<T extends Comparable<T>> 
	implements Comparable<PixelIntensity<T>> {
		protected final long index;
		protected final long age;
		protected final double intensity;
		protected final List<T> labeling;
		public PixelIntensity(int [] position, 
				              int[] dimensions, 
				              double intensity,
				              long age,
				              List<T> labeling) {
			long index = position[0];
			long multiplier = dimensions[0];
			for (int i=1; i<dimensions.length; i++) {
				index += position[i] * multiplier;
				multiplier *= dimensions[i];
			}
			
			this.index = index;
			this.intensity = intensity;
			this.labeling = labeling;
			this.age = age;
		}
		@Override
		public int compareTo(PixelIntensity<T> other) {
			int result = Double.compare(intensity, other.intensity);
			if (result == 0)
				result = Double.compare(age, other.age);
			return result;
		}
		void getPosition(int [] position, int [] dimensions) {
			long idx = index;
			for (int i=0; i<dimensions.length; i++) {
				position[i] = (int)(idx % dimensions[i]);
				idx /= dimensions[i];
			}
		}
		List<T> getLabeling() {
			return labeling;
		}
	}
	/**
	 * The seeded watershed uses a pre-existing labeling of the space where
	 * the labels act as seeds for the output watershed. The analogy would
	 * be to use dyed liquids emanating from the seeded pixels, flowing to the
	 * local minima and then filling individual watersheds until the
	 * liquids meet at the boundaries.
	 * 
	 * This implementation breaks ties by assigning the pixel to the
	 * label that occupied an adjacent pixel first.
	 * 
	 * @param <T> - the image type, typically real or integer. Technically
	 * complex is supported but only the real part is used.
	 * 
	 * @param <L> - the labeling type, typically Integer for machine-coded
	 * seeds or possibly String for human labeled seeds.
	 * 
	 * @param image - the intensity image that defines the watershed
	 * landscape. Lower values will be labeled first.
	 * 
	 * @param seeds - a labeling of the space, defining the first pixels
	 * in the space to be labeled. The seeded pixels will be similarly labeled
	 * in the output as will be their watershed neighbors.
	 * 
	 * @param structuringElement - an array of offsets where each element
	 * of the array gives the offset of a connected pixel from a pixel of
	 * interest. You can use AllConnectedComponents.getStructuringElement
	 * to get an 8-connected (or N-dimensional equivalent) structuring
	 * element (all adjacent pixels + diagonals).
	 * 
	 * @param output - a similarly-sized, but initially unlabeled labeling
	 * space
	 */
	static public <T extends ComplexType<T>, L extends Comparable<L>>
	void seededWatershed(Image<T> image, 
			             Labeling<L> seeds,
			             int [][] structuringElement,
			             Labeling<L> output) {
		/*
		 * Preconditions
		 */
		assert(seeds.getNumDimensions() == image.getNumDimensions());
		assert(seeds.getNumDimensions() == output.getNumDimensions());
		for (int i=0; i< structuringElement.length; i++) {
			assert(structuringElement[i].length == seeds.getNumDimensions());
		}
		/*
		 * Start by loading up a priority queue with the seeded pixels
		 */
		PriorityQueue<PixelIntensity<L>> pq = new PriorityQueue<PixelIntensity<L>>();
		LocalizableCursor<LabelingType<L>> c = seeds.createLocalizableCursor();
		LocalizableByDimCursor<LabelingType<L>> outputCursor =
			output.createLocalizableByDimCursor();
		LocalizableByDimCursor<T> ic = image.createLocalizableByDimCursor();
		
		int [] dimensions = output.getDimensions();
		int [] imageDimensions = image.getDimensions();
		int [] position = seeds.createPositionArray();
		int [] destPosition = seeds.createPositionArray();
		long age = 0;
		
		for (LabelingType<L> t:c) {
			List<L> l = t.getLabeling();
			if (l.isEmpty()) continue;
			
			c.getPosition(position);
			boolean outofbounds = false;
			for (int i=0; i<position.length; i++)
				if ((position[i] >= dimensions[i]) || 
					(position[i] >= imageDimensions[i])) {
					outofbounds = true;
					break;
				}
			if (outofbounds) continue;
			outputCursor.setPosition(position);
			l = outputCursor.getType().intern(l);
			outputCursor.getType().setLabeling(l);
			ic.setPosition(position);
			double intensity = ic.getType().getRealDouble();
			pq.add(new PixelIntensity<L>(position, dimensions, intensity, age++, l));
		}
		/*
		 * Pop the head of the priority queue, label and push all unlabeled
		 * connected pixels.
		 */
		while (! pq.isEmpty()) {
			PixelIntensity<L> currentPI = pq.remove();
			List<L> l = currentPI.getLabeling(); 
			currentPI.getPosition(position, dimensions);
			for (int [] offset:structuringElement) {
				boolean outofbounds = false;
				for (int i=0; i<position.length; i++) {
					destPosition[i] = position[i] + offset[i];
					if ((destPosition[i] >= dimensions[i]) ||
						(destPosition[i] >= imageDimensions[i]) ||
						(destPosition[i] < 0)) {
						outofbounds = true;
					}
				}
				if (outofbounds) continue;
				outputCursor.setPosition(destPosition);
				if (! outputCursor.getType().getLabeling().isEmpty()) continue;
				outputCursor.getType().setLabeling(l);
				ic.setPosition(position);
				double intensity = ic.getType().getRealDouble();
				pq.add(new PixelIntensity<L>(destPosition, dimensions, intensity, age++, l));
			}
		}
		c.close();
		outputCursor.close();
		ic.close();
	}
}
