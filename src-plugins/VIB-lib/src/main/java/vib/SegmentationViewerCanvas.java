/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import amira.AmiraParameters;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.ImageCanvas;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Vector;
import java.util.HashMap;

public class SegmentationViewerCanvas extends ImageCanvas {
	final static int OUTLINE=1, FILL=2;
	int mode=FILL;
	int alpha=128; // if mode==FILL, use this transparency to fill

	protected ImagePlus labels;
	int w,h,d;
	Color[] label_colors; // these are the up to 256 material colors

	Vector[] contours; // each element is a vector of polygons
	Vector[] colors; // these are the corresponding colors
	Vector[] indices; // these are the corresponding material IDs

	private final boolean debug = false;

	public SegmentationViewerCanvas(ImagePlus imp) {
		super(imp);
		label_colors = new Color[256];
		w=imp.getWidth();
		h=imp.getHeight();
		d=imp.getStack().getSize();
		contours=new Vector[d];
		colors=new Vector[d];
		indices=new Vector[d];

		ImagePlus.addImageListener(new ImageListener() {
			public void imageOpened(ImagePlus imp) { }
			public void imageClosed(ImagePlus imp) { }
			public void imageUpdated(ImagePlus imp) {
				if (imp == labels)
					setLabels(labels);
			}
		});
	}

	public SegmentationViewerCanvas(ImagePlus imp,ImagePlus labels) {
		this(imp);
		setLabels(labels);
	}

	public ImagePlus getLabels() {
		return labels;
	}

	public void setLabels(ImagePlus labels) {
		this.labels=labels;
		contours=new Vector[d];
		colors=new Vector[d];
		indices=new Vector[d];
		if (labels == null)
			return;
		AmiraParameters parameters=new AmiraParameters(labels);
		int count = parameters.getMaterialCount();
		for(int i=0;i<label_colors.length;i++) {
			if (i >= count) {
				label_colors[i] = Color.RED;
				continue;
			}
			double[] c=parameters.getMaterialColor(i);
			int red=(int)(255*c[0]);
			int green=(int)(255*c[1]);
			int blue=(int)(255*c[2]);
			label_colors[i]=new Color(red,green,blue);
		}
		if (backBufferGraphics != null)
			repaint();
	}

	public void updateSlice(int slice){
		synchronized(this) {
			colors[slice-1] = null;
			contours[slice-1] = null;
			indices[slice-1] = null;
			createContoursIfNotExist(slice);
		}
	}

	public GeneralPath getOutline(int slice, int materialId){
		synchronized(this) {
			createContoursIfNotExist(slice);
			
			for (int i = 0; i < indices[slice-1].size(); i++)
				if (((Integer)indices[slice-1].get(i)).intValue()
				    == materialId)
					return (GeneralPath)contours[slice-1].get(i);
			return null;
		}
	}

	/*
	 * This class implements a Cartesian polygon in progress.
	 * The edges are supposed to be of unit length, and parallel to
	 * one axis.
	 * It is implemented as a deque to be able to add points to both
	 * sides.
	 * The points should be added such that for each pair of consecutive
	 * points, the inner part is on the left.
	 */
	static class Outline {
		int[] x, y;
		int first, last, reserved;
		final int GROW = 10;

		public Outline() {
			reserved = GROW;
			x = new int[reserved];
			y = new int[reserved];
			first = last = GROW / 2;
		}

		private void needs(int newCount, int offset) {
			if (newCount > reserved || (offset > first)) {
				if (newCount < reserved + GROW + 1)
					newCount = reserved + GROW + 1;
				int[] newX = new int[newCount];
				int[] newY = new int[newCount];
				System.arraycopy(x, 0, newX, offset, last);
				System.arraycopy(y, 0, newY, offset, last);
				x = newX;
				y = newY;
				first += offset;
				last += offset;
				reserved = newCount;
			}
		}

		public Outline push(int x, int y) {
			needs(last + 1, 0);
			this.x[last] = x;
			this.y[last] = y;
			last++;
			return this;
		}

		public Outline shift(int x, int y) {
			needs(last + 1, GROW);
			first--;
			this.x[first] = x;
			this.y[first] = y;
			return this;
		}

		public Outline push(Outline o) {
			int count = o.last - o.first;
			needs(last + count, 0);
			System.arraycopy(o.x, o.first, x, last, count);
			System.arraycopy(o.y, o.first, y, last, count);
			last += count;
			return this;
		}

		public Outline shift(Outline o) {
			int count = o.last - o.first;
			needs(last + count + GROW, count + GROW);
			first -= count;
			System.arraycopy(o.x, o.first, x, first, count);
			System.arraycopy(o.y, o.first, y, first, count);
			return this;
		}

		public Polygon getPolygon() {
			// TODO: optimize out long straight lines
			int count = last - first;
			int[] x1 = new int[count];
			int[] y1 = new int[count];
			System.arraycopy(x, first, x1, 0, count);
			System.arraycopy(y, first, y1, 0, count);
			return new Polygon(x1, y1, count);
		}

		public String toString() {
			String res = "(first:" + first
				+ ",last:" + last + ",reserved:" + reserved + ":";
			if (last > x.length) System.err.println("ERROR!");
			for (int i = first; i < last && i < x.length; i++)
				res += "(" + x[i] + "," + y[i] + ")";
			return res + ")";
		}
	}

	class ContourFinder {
		int slice;
		byte[] pixels;
		GeneralPath[] paths;
		Outline[] outline;

		public ContourFinder(int slice) {
			this.slice=slice;
			pixels=(byte[])labels.getStack().getProcessor(slice+1).getPixels();
			paths = new GeneralPath[255];
		}

		// no check!
		final byte get(int x,int y) { return pixels[y * w + x]; }

		/*
		 * Construct all outlines simultaneously by traversing the rows
		 * from top to bottom.
		 *
		 * The open ends of the polygons are stored in outline[]:
		 * if the polygon ends at the left of the pixel at x in the
		 * previous row, and the pixel is not contained in the polygin,
		 * outline[2 * x] contains the partial outline;
		 * if the polygon contains the pixel, outline[2 * x + 1] holds
		 * the partial outline.
		 */
		public void initContours() {
			contours[slice]=new Vector();
			colors[slice]=new Vector();
			indices[slice]=new Vector();

			// actually find the outlines
			ArrayList polygons = new ArrayList();
			outline = new Outline[2 * w + 2];

			for (int y = 0; y <= h; y++)
				for (int x = 0; x < w; x++)
					handle(x, y);

			for (int i = 1; i < paths.length; i++) {
				if (paths[i] != null) {
					contours[slice].add(paths[i]);
					colors[slice].add(label_colors[i]);
					indices[slice].add(new Integer(i));
				}
			}
		}

		final private Outline newOutline(int left, int right,
						 int x1, int x2, int y) {
			outline[left] = outline[right] = new Outline();
			outline[left].push(x1, y);
			outline[left].push(x2, y);
			return outline[left];
		}

		final private Outline mergeOutlines(Outline left, Outline right) {
			left.push(right);
			for (int k = 0; k < outline.length; k++)
				if (outline[k] == right) {
					outline[k] = left;
					return outline[k];
				}
			throw new RuntimeException("assertion failed!");
		}

		final private Outline moveOutline(int from, int to) {
			outline[to] = outline[from];
			outline[from] = null;
			return outline[to];
		}

		private void closeOutline(byte material, Outline outline) {
			int m = material & 0xff;

			if(material == -1) m = 0;//????? Tom
			if (paths[m] == null)
				paths[m] = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
			paths[m].append(outline.getPolygon(), false);
		}

		private void handle(int x, int y) {
			byte m = (y < h ? get(x, y) : 0);
			byte mPrev = (y > 0 ? get(x, y - 1) : 0);
			byte mLeft = (x > 0 && y < h ? get(x - 1, y) : 0);
			byte mRight = (x < w - 1 && y < h ? get(x + 1, y) : 0);
			byte mPrevLeft = (x > 0 && y > 0 ? get(x - 1, y - 1) : 0);
			byte mPrevRight = (x < w - 1 && y > 0 ? get(x + 1, y - 1) : 0);

			Outline left1 = outline[2 * x];
			Outline left2 = outline[2 * x + 1];
			Outline right2 = outline[2 * x + 2];
			Outline right1 = outline[2 * x + 3];
			outline[2 * x] = outline[2 * x + 3] = null;
			outline[2 * x + 1] = outline[2 * x + 2] = null;

			if (mPrev != 0 && mPrev != m) {
				// lower edge
				// - both null: new outline
				// - left == null: shift
				// - right == null: push
				// - right == left: close
				// - right != left: push
				int l = 2 * x, r = 2 * x + 3;
				if (left2 == null && right2 == null)
					newOutline(l, r, x, x + 1, y);
				else if (left2 == null)
					outline[l] = right2.shift(x, y);
				else if (right2 == null)
					outline[r] = left2.push(x + 1, y);
				else if (left2 == right2)
					closeOutline(mPrev, left2);
				else
					mergeOutlines(left2, right2);
				left2 = right2 = null;
			}
			if (m != 0 && mPrev != m) {
				// upper edge:
				// - left and right are null: new outline
				// - left null: push
				// - right null: shift
				// - left == right: close
				// - left != right: merge
				int l = 2 * x + 1, r = 2 * x + 2;
				if (left1 != null && mLeft != m) {
					outline[2 * x] = left1;
					left1 = null;
				}
				if (right1 != null && (mRight != m || mPrevRight != m)) {
					outline[2 * x + 3] = right1;
					right1 = null;
				}
				if (left1 == null && right1 == null)
					newOutline(l, r, x + 1, x, y);
				else if (left1 == null)
					outline[l] = right1.push(x, y);
				else if(right1 == null)
					outline[r] = left1.shift(x + 1, y);
				else if (left1 == right1)
					closeOutline(m, left1);
				else
					mergeOutlines(right1, left1);
				left1 = right1 = null;
			}
			if (left1 != null)
				outline[2 * x] = left1;
			if (left2 != null)
				outline[2 * x + 1] = left2;
			if (right1 != null)
				outline[2 * x + 3] = right1;
			if (right2 != null)
				outline[2 * x + 2] = right2;
			if (m != 0 && mLeft != m) {
				// left edge
				int l = 2 * x + 1;
				if (outline[l] == null)
					outline[l] = left2;
				outline[l].push(x, y + 1);
			}
			if (mLeft != 0 && mLeft != m) {
				// right edge
				int l = 2 * x + 0;
				if (outline[l] == null)
					outline[l] = left1;
				outline[l].shift(x, y + 1);
			}
		}
	}

	public void createContoursIfNotExist(int slice) {
		if (labels == null || contours[slice-1]!=null)
			return;
		ContourFinder finder=new ContourFinder(slice-1);
		finder.initContours();
	}

	private int backBufferWidth;
	private int backBufferHeight;

	private Graphics backBufferGraphics;
	private Image backBufferImage;

	private void resetBackBuffer() {

		if(backBufferGraphics!=null){
			backBufferGraphics.dispose();
			backBufferGraphics=null;
		}

		if(backBufferImage!=null){
			backBufferImage.flush();
			backBufferImage=null;
		}

		backBufferWidth=getSize().width;
		backBufferHeight=getSize().height;

		backBufferImage=createImage(backBufferWidth,backBufferHeight);
	        backBufferGraphics=backBufferImage.getGraphics();

	}

	public void paint(Graphics g) {
		
		if(backBufferWidth!=getSize().width ||
		   backBufferHeight!=getSize().height ||
		   backBufferImage==null ||
		   backBufferGraphics==null)
			resetBackBuffer();

		int slice = imp.getCurrentSlice();
		synchronized(this) {
			createContoursIfNotExist(slice);
			super.paint(backBufferGraphics);
			drawOverlay(backBufferGraphics,slice);
		}
		g.drawImage(backBufferImage,0,0,this);
	}

	void drawOverlay(Graphics g,int slice) {
		if (labels == null)
			return;
		double magnification=getMagnification();

		for(int i=0;i<contours[slice-1].size();i++) {
			g.setColor((Color)colors[slice-1].get(i));
			Shape poly = (Shape)contours[slice-1].get(i);

			// take offset into account (magnification very high)
			if(magnification!=1.0) {
				AffineTransform trans = (((Graphics2D)g).getDeviceConfiguration()).getDefaultTransform();
				trans.setTransform(magnification, 0,
						   0, magnification,
						   -srcRect.x * magnification,
						   -srcRect.y * magnification);
				poly = trans.createTransformedShape(poly);
			}

			((Graphics2D)g).draw(poly);
			if(mode==FILL) {
				Color c=(Color)colors[slice-1].get(i);
				Color c1=new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
				g.setColor(c1);
				((Graphics2D)g).fill(poly);
			}
		}
	}
}
