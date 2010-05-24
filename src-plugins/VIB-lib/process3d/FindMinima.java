package process3d;

import ij.ImageStack;
import ij.ImagePlus;
import ij.IJ;
import ij.process.ImageProcessor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Stack;
import java.util.ArrayList;
import java.util.HashSet;

public class FindMinima {

	private static final int NO_MINIMUM = 0;
	private static final int UNLABELLED = 100;
	private static final int MINIMUM    = 255;
	private static final int IN_QUEUE   = 25;

	protected ImagePlus image;
	private ImagePlus result;
	private int w, h, d;
	private byte[][] data;
	private byte[][] minima;

	public FindMinima() {}

	public FindMinima(ImagePlus imp) {
		this.init(imp);
	}

	public void init(ImagePlus imp) {
		this.image = imp;
		w = imp.getWidth();
		h = imp.getHeight();
		d = imp.getStackSize();
		data = new byte[d][];
		for(int z = 0; z < d; z++)
			data[z] = (byte[])imp.getStack().getPixels(z+1);

		minima = new byte[d][w * h];
		ImageStack stack = new ImageStack(w, h);

		for(int z = 0; z < d; z++) {
			Arrays.fill(minima[z], (byte)UNLABELLED);
			stack.addSlice("", minima[z]);
		}

		result = new ImagePlus("Minima", stack);
		result.setCalibration(image.getCalibration());
	}

	private final int get(Point p) {
		return (int)(data[p.z][p.y * w + p.x] & 0xff);
	}

	private final int getLabel(Point p) {
		return (int)(minima[p.z][p.y * w + p.x] & 0xff);
	}

	private final void label(Point p) {
		minima[p.z][p.y * w + p.x] = (byte)MINIMUM;
	}

	private final void unlabel(Point p) {
		minima[p.z][p.y * w + p.x] = (byte)NO_MINIMUM;
	}

	private final void inQueue(Point p) {
		minima[p.z][p.y * w + p.x] = (byte)IN_QUEUE;
	}

	private final boolean isLabelled(Point p) {
		return minima[p.z][p.y * w + p.x] != UNLABELLED;
	}

	public ImagePlus createOverlay() {
		ImageStack stack = new ImageStack(w, h);
		int RED = 0xff0000;
		for(int z = 0; z < d; z++) {
			ImageProcessor ip = image.getStack().
				getProcessor(z + 1).convertToRGB();
			for(int i = 0; i < w * h; i++) {
				if(minima[z][i] == (byte)MINIMUM)
					ip.set(i, RED);
			}
			stack.addSlice("", ip);
		}
		ImagePlus ret = new ImagePlus("overlay", stack);
		ret.setCalibration(image.getCalibration());
		return ret;
	}

	public ImagePlus classify() {
		for(int z = 0; z < d; z++) {
			for(int y = 0; y < h; y++)
				for(int x = 0; x < w; x++)
					classifyPixel(x, y, z);
			IJ.showProgress(z, d);
		}
		return result;
	}

	/*
	 * If x, y, z is a nonstrict min, put it into a stack.
	 * iteratively,
	 *     pop first point
	 *     add point to closed.
	 *     check its neighbors:
	 *         if there is a neighbor with a smaller value, label the whole
	 *         region as non-minimum.
	 *
	 *         else if there is a neighbor with same value, push it onto
	 *         the stack.
	 */
	public void classifyPixel(int x, int y, int z) {
		Point start = new Point(x, y, z);
		if(isLabelled(start))
			return;
// 		Stack<Point> s = new Stack<Point>();
		LinkedList<Point> s = new LinkedList<Point>();
// 		HashSet<Point> closed = new HashSet<Point>();
		ArrayList<Point> closed = new ArrayList<Point>();
// 		s.push(start);
		s.add(start);
		closed.add(start);
		minima[start.z][start.y * w + start.x] = (byte)IN_QUEUE;
		while (!s.isEmpty()){
// 			Point p = s.pop();
			Point p = s.removeLast();
			int v = get(p);
			for(int iz = -1; iz <= +1; iz++) {
				int zIdx = p.z + iz;
				if(zIdx < 0 || zIdx >= d)
					continue;
				for(int iy = -1; iy <= +1; iy++) {
					int yIdx = p.y + iy;
					if(yIdx < 0 || yIdx >= h)
						continue;
					for(int ix = -1; ix <= +1; ix++) {
						int xIdx = p.x + ix;
						if(xIdx < 0 || xIdx >= w)
							continue;
						Point nPoint = new Point(
							xIdx, yIdx, zIdx);
						// point is already in the stack
// 						if(closed.contains(nPoint))
						if(getLabel(nPoint) == IN_QUEUE)
							continue;

						int vt = get(nPoint);
						if(vt < v) {
							for(Point c : closed)
								unlabel(c);
							return;
						}
						if(vt > v)
							continue;
						// vt == v:
						int l = getLabel(nPoint);
						switch(l) {
						case MINIMUM:
							for(Point c : closed)
								label(c);
							return;
						case NO_MINIMUM:
							for(Point c : closed)
								unlabel(c);
							return;
						default:
// 							s.push(nPoint);
							s.add(nPoint);
							closed.add(nPoint);
							inQueue(nPoint);
						}
					}
				}
			}
		}
		// if we reached that point, the queue is empty,
		// which means that we indeed found a minimum region.
		for(Point c : closed)
			label(c);
	}

	private final static class Point {
		private int x, y, z;
		
		Point(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public boolean equals(Object o) {
			Point p = (Point)o;
			return x == p.x && y == p.y && z == p.z;
		}

		public int hashCode() {
			return z * y * x;
		}
	}
}

