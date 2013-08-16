package fiji.process3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

/*
 * The idea of the Euclidean Distance Transform is to get the
 * distance of every outside pixel to the nearest outside pixel.
 *
 * We use the algorithm proposed in

	@TECHREPORT{Felzenszwalb04distancetransforms,
	    author = {Pedro F. Felzenszwalb and Daniel P. Huttenlocher},
	    title = {Distance transforms of sampled functions},
	    institution = {Cornell Computing and Information Science},
	    year = {2004}
	}

 * Felzenszwalb & Huttenlocher's idea is to extend the concept to
 * a broader one, namely to minimize not only the distance to an
 * outside pixel, but to minimize the distance plus a value that
 * depends on the outside pixel.
 *
 * In mathematical terms: we determine the minimum of the term
 *
 *	g(x) = min(d^2(x, y) + f(y) for all y)
 *
 * where y runs through all pixels and d^2 is the square of the
 * Euclidean distance. For the Euclidean distance transform, f(y)
 * is 0 for all outside pixels, and infinity for all inside
 * pixels, and the result is the square root of g(x).
 *
 * The trick is to calculate g in one dimension, store the result,
 * and use it as f(y) in the next dimension. Continue until you
 * covered all dimensions.
 *
 * In order to find the minimum in one dimension (i.e. row by
 * row), the following fact is exploited: for two different
 * y1 < y2, (x - y1)^2 + f(y1) < (x - y2)^2 + f(y2) for x < s,
 * where s is the intersection point of the two parabolae (there
 * is the corner case where one parabola is always lower than
 * the other one, in that case there is no intersection).
 *
 * Using this fact, for each row of n elements, a maximum number
 * of n parabolae are constructed, adding them one by one for each
 * y, adjusting the range of x for which this y yields the minimum,
 * possibly overriding a number of previously added parabolae.
 *
 * At most n parabolae can be added, so the complexity is still
 * linear.
 *
 * After this step, the list of parabolae is iterated to calculate
 * the values for g(x).
 *
 * To make the Euclidean Distance Transform a signed one, the
 * stored function values can be positive or negative, but not
 * zero. For every row, effectively two different calculations
 * are performed, one for the positive and one for the negative
 * distances. For both cases, the values with the "wrong" sign
 * are substituted by 0.5 of the same sign.
 */
public class SEDT implements PlugInFilter {
	ImagePlus image;
	int w, h, d;
	int current, total;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8G | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		compute(image.getStack()).show();
	}

	public ImagePlus compute(ImageStack stack) {
		w = stack.getWidth();
		h = stack.getHeight();
		d = stack.getSize();
		ImageStack result = new ImageStack(w, h, d);
		for (int i = 1; i <= d; i++)
			result.setPixels(new float[w * h], i);

		current = 0;
		total = w * h * d * 3;

		new Z(stack, result).compute();
		new Y(result).compute();
		new X(result).compute();

		return new ImagePlus("EDT", result);
	}

	abstract class EDTBase {
		int width;
		/*
		 * parabola k is defined by y[k] (v in the paper)
		 * and f[k] (f(v[k]) in the paper): (y, f) is the
		 * coordinate of the minimum of the parabola.
		 * z[k] determines the left bound of the interval
		 * in which the k-th parabola determines the lower
		 * envelope.
		 */

		int kNeg, kPos;
		float[] fNeg, fPos, zNeg, zPos;
		int[] yNeg, yPos;

		EDTBase(int rowWidth) {
			width = rowWidth;
			fNeg = new float[width + 1];
			zNeg = new float[width + 1];
			yNeg = new int[width + 1];
			fPos = new float[width + 1];
			zPos = new float[width + 1];
			yPos = new int[width + 1];
		}

		final void computeRow() {
			// calculate the parabolae ("lower envelope")
			fNeg[0] = -Float.MAX_VALUE;
			yNeg[0] = -1;
			zNeg[0] = Float.MAX_VALUE;
			kNeg = 0;
			fPos[0] = Float.MAX_VALUE;
			yPos[0] = -1;
			zPos[0] = Float.MAX_VALUE;
			kPos = 0;
			float fx, fxNeg, fxPos, s;
			for (int x = 0; x < width; x++) {
				fx = get(x);
				fxNeg = fx < 0 ? fx : 0f;
				for (;;) {
					// calculate the intersection
					s = ((fxNeg - x * x) - (fNeg[kNeg] - yNeg[kNeg] * yNeg[kNeg])) / -2 / (x - yNeg[kNeg]);
					if (s > zNeg[kNeg])
						break;
					if (--kNeg < 0)
						break;
				}
				kNeg++;
				yNeg[kNeg] = x;
				fNeg[kNeg] = fxNeg;
				zNeg[kNeg] = s;
				fxPos = fx > 0 ? fx : 0f;
				for (;;) {
					// calculate the intersection
					s = ((fxPos + x * x) - (fPos[kPos] + yPos[kPos] * yPos[kPos])) / 2 / (x - yPos[kPos]);
					if (s > zPos[kPos])
						break;
					if (--kPos < 0)
						break;
				}
				kPos++;
				yPos[kPos] = x;
				fPos[kPos] = fxPos;
				zPos[kPos] = s;
			}
			zNeg[++kNeg] = Float.MAX_VALUE;
			zPos[++kPos] = Float.MAX_VALUE;
			// calculate g(x)
			int iNeg = 0, iPos = 0;
			for (int x = 0; x < width; x++) {
				while (zNeg[iNeg + 1] < x)
					iNeg++;
				while (zPos[iPos + 1] < x)
					iPos++;
				if (get(x) < 0)
					set(x, -(x - yNeg[iNeg]) * (x - yNeg[iNeg]) + fNeg[iNeg]);
				else
					set(x, (x - yPos[iPos]) * (x - yPos[iPos]) + fPos[iPos]);
			}
		}

		abstract float get(int column);

		abstract void set(int column, float value);

		final void compute() {
			while (nextRow()) {
				computeRow();
				if (total > 0) {
					current += width;
					IJ.showProgress(current, total);
				}
			}
		}

		abstract boolean nextRow();
	}

	class Z extends EDTBase {
		byte[][] inSlice;
		float[][] outSlice;
		int offset;

		Z(ImageStack in, ImageStack out) {
			super(d);
			inSlice = new byte[d][];
			outSlice = new float[d][];
			for (int i = 0; i < d; i++) {
				inSlice[i] = (byte[])in.getPixels(i + 1);
				outSlice[i] = (float[])out.getPixels(i + 1);
			}
			offset = -1;
		}

		final float get(int x) {
			return inSlice[x][offset] == 0 ? -Float.MAX_VALUE : Float.MAX_VALUE;
		}

		final void set(int x, float value) {
			outSlice[x][offset] = value;
		}

		final boolean nextRow() {
			return ++offset < w * h;
		}
	}

	abstract class OneDimension extends EDTBase {
		ImageStack stack;
		float[] slice;
		int offset, lastOffset, rowStride, columnStride, sliceIndex;

		OneDimension(ImageStack out, boolean iterateX) {
			super(iterateX ? w : h);
			stack = out;
			columnStride = iterateX ? 1 : w;
			rowStride = iterateX ? w : 1;
			offset = w * h;
			lastOffset = rowStride * (iterateX ? h : w);
			sliceIndex = -1;
		}

		final float get(int x) {
			return slice[x * columnStride + offset];
		}

		final boolean nextRow() {
			offset += rowStride;
			if (offset >= lastOffset) {
				if (++sliceIndex >= d)
					return false;
				offset = 0;
				slice = (float[])stack.getPixels(sliceIndex + 1);
			}
			return true;
		}
	}

	class Y extends OneDimension {
		Y(ImageStack out) {
			super(out, false);
		}

		final void set(int x, float value) {
			slice[x * columnStride + offset] = value;
		}
	}

	class X extends OneDimension {
		X(ImageStack out) {
			super(out, true);
		}

		final void set(int x, float value) {
			if (value < 0)
				value = 0.5f - (float)Math.sqrt(-value);
			else
				value = -0.5f + (float)Math.sqrt(value);
			slice[x * columnStride + offset] = value;
		}
	}
}
