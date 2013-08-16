package vib.app.module;

import amira.AmiraParameters;

import ij.IJ;
import ij.ImagePlus;

import java.awt.*;
import java.awt.image.*;

import java.io.File;

import java.util.*;

import vib.FastMatrix;
import vib.InterpolatedImage;
import vib.TransformedImage;
import vib.VIB;

public class AverageBrain extends Module {
	int[][] cumul;
	int w, h, d, count;

	public String getName() { return "AverageBrain"; }
	protected String getMessage() { return "Averaging brain"; }
	protected boolean runsOnce() { return true; }

	protected void run(State state, int index) {
		if (index != 0)
			return;
		new TransformImages().runOnAllImages(state);
		new Resample().runOnAllImages(state);

		prereqsDone(state, index);

		new AverageBrain().doit(state);
	}

	private void doit(State state) {
		matrices = null;
		scratch = null;
		for (int i = -1; i < state.options.numChannels; i++) {
			/* calculate AverageBrain */
			String outputPath = state.getOutputPath(i);
			doit(state, getCompleteChannel(state, i), outputPath);
		}
	}

	private FastMatrix[] matrices;
	private ImagePlus scratch;

	public void doit(State state, String[] images, String outputPath) {
		if (state.upToDate(images, outputPath))
			return;
		if (matrices == null)
			matrices = getMatrices(state);
		if (scratch == null)
			// try to reuse labels if they were already loaded
			scratch = state.options.needsLabels() ?
				state.getTemplateLabels() :
				state.getTemplate();
		doit(scratch, images, matrices);
		if(!state.save(scratch, outputPath))
			throw new RuntimeException(
				"Could not save " + outputPath);
	}

	private FastMatrix[] getMatrices(State state) {
		FastMatrix[] result = new FastMatrix[state.getImageCount()];
		for (int i = 0; i < result.length; i++) {
			result[i] = new FastMatrix();
			/*
			 * AverageBrain needs a FastMatrix, but
			 * state.getTransformMatrix() returns a FloatMatrix...
			 */
			result[i].copyFrom(state.getTransformMatrix(i));
		}
		return result;
	}

	private String[] getCompleteChannel(State state, int channel) {
		String[] result = new String[state.getImageCount()];
		for (int i = 0; i < result.length; i++)
			result[i] = state.getWarpedPath(channel, i);
		return result;
	}

	public void doit(ImagePlus image, String[] fileNames, FastMatrix[] matrices) {
		count = ("".equals(fileNames[0]) ? 0 : fileNames.length);
		if (count != matrices.length) {
			IJ.error("Count mismatch: " + count
				+ " files, but " + matrices.length
				+ " matrices!");
			return;
		}
		int realCount = count;

		InterpolatedImage ii = new InterpolatedImage(image);
		w = ii.w;
		h = ii.h;
		d = ii.d;
		cumul = new int[ii.d][ii.w * ii.h];
		boolean isGray = !image.getProcessor().isColorLut();
		Method method = isGray ?
			(Method)new AverageGray() : (Method)new AverageLabels();
		method.cumul = cumul;
		if (!isGray) {
			AmiraParameters p = new AmiraParameters(image);
			p.changeLabelfieldToGray();
			p.setParameters(image);
		}
		for (int m = 0; m < count; m++) {
			VIB.showStatus("Brain (" + (m + 1) + "/" + count + ")");
			ImagePlus img = IJ.openImage(fileNames[m]);
			if (img == null) {
				realCount--;
				continue;
			}
			method.t = new TransformedImage(image, img);
			method.t.setTransformation(matrices[m]);
			method.isIdentity = method.t.matrix.isIdentity();
			TransformedImage.Iterator iter = method.t.iterator();
			while (iter.next() != null) {
				method.accumulate(iter.i, iter.j, iter.k,
						iter.x, iter.y, iter.z);
			}
			method.t = null;
			img.close();
		}
		method.count = (realCount < 1 ? 1 : realCount);
		InterpolatedImage.Iterator iter = ii.iterator();
		while (iter.next() != null) {
			ii.set(iter.i, iter.j, iter.k,
					method.get(iter.i, iter.j, iter.k));
		}
	}

	abstract class Method {
		boolean isIdentity = false;
		TransformedImage t;
		int[][] cumul;
		int count;

		public abstract void accumulate(int i, int j, int k,
				double x, double y, double z);

		public abstract int get(int i, int j, int k);
	}

	class AverageGray extends Method {
		public void accumulate(int i, int j, int k,
				double x, double y, double z) {
			if (isIdentity) {
				cumul[k][i + j * w] += t.transform.getNoInterpol(i, j, k);
				return;
			}
			double v = t.transform.interpol.get(x, y, z);
			cumul[k][i + j * w] += (int)v;
		}

		public int get(int i, int j, int k) {
			return cumul[k][i + j * w] / count;
		}
	}

	class AverageLabels extends Method {
		byte[][] labels;
		Vector[] tuples;
		final static int maxProb = 100;

		public AverageLabels() {
			super();
			labels = new byte[d][w * h];
			tuples = new Vector[256];
			for (int i = 0; i < 256; i++)
				tuples[i] = new Vector();
		}

		public void accumulate(int i, int j, int k,
				double x, double y, double z) {
			byte v = t.transform.getNearestByte(x, y, z);
			if (v == 0)
				return;
			int l = (v < 0 ? 256 + v : v);
			int l1 = labels[k][i + j * w];
			if (l1 < 0)
				l1 += 256;
			int c1 = cumul[k][i + j * w];
			if (l1 == 0) {
				labels[k][i + j * w] = v;
				cumul[k][i + j * w] = 1;
			} else if (l1 == l && c1 > 0)
				cumul[k][i + j * w]++;
			else {
				Tuple newTuple;
				if (c1 > 0)
					newTuple = new Tuple(l1, c1, l);
				else {
					Tuple old = (Tuple)tuples[l1].get(-c1);
					newTuple = new Tuple(old, l);
				}
				if (newTuple.labels[0] != (byte)l1) {
					l1 = newTuple.labels[0];
					labels[k][i + j * w] = (byte)l1;
					if (l1 < 0)
						l1 += 256;
				}
				cumul[k][i + j * w] = 
					-newTuple.getIndex(tuples[l1]);
			}
		}

		public int get(int i, int j, int k) {
			int label = labels[k][i + j * w];
			if (label == 0)
				return 0;
			else if (label < 0)
				label += 256;
			int c = cumul[k][i + j * w];
			if (c <= 0) {
				Tuple t = (Tuple)tuples[label].get(-c);
				c = t.counts[0];
			}
			return c * maxProb / count;
		}
	}

	/*
	 * The tuple class holds a tuple of labels along with the
	 * respective counts.
	 *
	 * The idea: at a given voxel, usually there is only one
	 * label, or at most one label and Exterior. In this case,
	 * store the label in the labels array, and the count of this
	 * label in the counts array.
	 *
	 * The less common case is handled by tuples. A tuple contains
	 * a list of the labels together with their respective counts.
	 *
	 * To make this space efficient, tuples are stored in arrays,
	 * and just the array index is stored as a negative count.
	 *
	 * In a tuple, the labels are sorted by count (higher counts
	 * come first) and label number (if the count is equal, the
	 * lower label comes first).
	 *
	 * As a consequence, ((Tuple)tuples[label].get(i)).labels[0]
	 * is always label.
	 */
	static class Tuple {
		/*
		 * the labels are sorted by count: higher counts
		 * come first. If the counts are equal, the order of the
		 * label numbers are undefined.
		 */
		byte[] labels = new byte[256];
		int[] counts = new int[256];

		public Tuple(int label1, int count1, int label2) {
			if (count1 == 1 && label2 < label1) {
				int l = label2;
				label2 = label1;
				label1 = l;
			}
			labels[0] = (byte)label1;
			counts[0] = count1;
			labels[1] = (byte)label2;
			counts[1] = 1;
		}

		public Tuple(Tuple tuple, int label) {
			int i;
			System.arraycopy(tuple.labels, 0, labels, 0,
					labels.length);
			System.arraycopy(tuple.counts, 0, counts, 0,
					counts.length);
			for (i = 0; i < labels.length &&
					labels[i] != label &&
					labels[i] != 0; i++);
			if (labels[i] == 0)
				labels[i] = (byte)label;

			int j;
			for (j = i - 1; j >= 0 &&
					counts[j] == counts[i]; j--);
			j++;
			if (j < i) {
				int c = counts[i];
				System.arraycopy(labels, j,
						labels, j + 1, i - j);
				System.arraycopy(counts, j,
						counts, j + 1, i - j);
				labels[j] = (byte)label;
				counts[j] = c;
				i = j;
			}
			counts[i]++;
		}

		public boolean equals(Tuple tuple) {
			for (int i = 0; i == 0 || labels[i - 1] != 0;
					i++)
				if (labels[i] != tuple.labels[i] ||
						counts[i] !=
						tuple.counts[i])
					return false;
			return true;
		}

		public int getIndex(Vector tuples) {
			int count = tuples.size();
			for (int i = 0; i < count; i++)
				if (equals((Tuple)tuples.get(i)))
					return i;
			tuples.add(this);
			return count;
		}

		public String toString() {
			String result = "";
			for (int i = 0; i < 256 && labels[i] != 0; i++)
				result += "" + labels[i] + "(" + counts[i]
					+ ") ";
			return result;
		}
	}

	private static byte physicsLUTHelper(double rad) {
		double s = Math.sin(rad);
		if (s < 0)
			return 0;
		return (byte)(int)Math.round(255 * s);
	}

	private static void setPhysicsLUT(ImagePlus ip) {
		byte[] rLUT = new byte[256];
		byte[] gLUT = new byte[256];
		byte[] bLUT = new byte[256];
		int max = 100;
		for(int i = 0; i <= max; i++) {
			double rad = i * Math.PI / (max / 2);
			rLUT[i]=physicsLUTHelper(rad + Math.PI);
			gLUT[i]=physicsLUTHelper(rad - Math.PI / 2);
			bLUT[i]=physicsLUTHelper(rad);
		}
		ColorModel c = new IndexColorModel(8, 256, rLUT, gLUT, bLUT);
                ip.getProcessor().setColorModel(c);
                if (ip.getStackSize() > 1)
                        ip.getStack().setColorModel(c);

	}

	public static void main(String[] args) {
		Tuple t = new Tuple(5, 1, 3);
		Tuple t1 = new Tuple(t, 5);
		Tuple t2 = new Tuple(t, 3);
		Tuple t3 = new Tuple(t, 2);
		Tuple t4 = new Tuple(t1, 5);
		Tuple t5 = new Tuple(t1, 3);
		System.err.println("3(1) 5(1) : " + t);
		System.err.println("5(2) 3(1) : " + t1);
		System.err.println("3(2) 5(1) : " + t2);
		System.err.println("3(1) 5(1) 2(1) : " + t3);
		System.err.println("5(3) 3(1) : " + t4);
		System.err.println("5(2) 3(2) : " + t5);
	}
}
