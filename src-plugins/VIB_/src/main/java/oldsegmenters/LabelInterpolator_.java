package oldsegmenters;

import ij.ImagePlus;
import ij.IJ;
import ij.gui.Roi;
import ij.plugin.PlugIn;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.awt.geom.GeneralPath;
import java.awt.geom.AffineTransform;
import java.awt.*;

import math3d.*;

/**
 * Generates label data for unlabelled slices based on the data in the nearest slices
 * <p/>
 * Algorithm:
 * First unlabelled slices are differentiated from labelled ones via
 * almost exhastive search of all pixels
 * (implemented in StackData)
 * <p/>
 * Then a list interpolations are constructed. These are a list og unlabelled slices with labelled slices either end
 * <p/>
 * These interpolations are then interpolated.
 * Pixels that appear in a position in one labelled slice but not the other slice are the tricky part
 * a line is drawn from the pixel to its nearest neighbour in the other slice. All voxels that are touched by this line are colored
 * <p/>
 * User: Tom Larkworthy
 * Date: 23-Jun-2006
 * Time: 19:11:01
 */
public class LabelInterpolator_ implements PlugIn {

	public void run(String arg) {
		interpolate(new SegmentatorModel(IJ.getImage()));
	}


	public static void interpolate(SegmentatorModel model) {
		if (model.getCurrentMaterial() == null) {
			IJ.showMessage("please select a label first");
			return;
		}

		StackData data = new StackData(model, model.getCurrentMaterial().id);

		System.out.println("stack data = " + data);
		IJ.showProgress(.5);
		for (Interpolation interpolation : data.getInterpolations()) {
			interpolation.interpolate();
			for (int i = interpolation.firstIndex; i <= interpolation.secondIndex; i++) {
				model.updateSliceNoRedraw(i);
			}
		}

		IJ.showProgress(1);

		model.data.updateAndDraw();
	}


	private static class StackData {
		ImagePlus labelsData;
		ArrayList<Integer> labelledSlices = new ArrayList<Integer>();

		SegmentatorModel model;
		int label;

		public StackData(SegmentatorModel model, int label) {
			this.model = model;
			this.labelsData = model.getLabelImagePlus();
			this.label = label;

			findLabelledSlices();
		}

		private void findLabelledSlices() {
			//we go through every slice looking for a pixel with the same label
			//this will fill labelledSlices with the indexes of all labelled slices
			for (int i = 1; i <= labelsData.getStackSize(); i++) {
				byte[] pixels = (byte[]) labelsData.getStack().getProcessor(i).getPixels();

				for (int j = 0; j < pixels.length; j++) {
					byte pixel = pixels[j];
					if (pixel == label) {
						labelledSlices.add(i);
						break;
					}
				}
			}
		}

		public Iterable<Interpolation> getInterpolations() {
			LinkedList<Interpolation> results = new LinkedList<Interpolation>();

			for (int i = 0; i < labelledSlices.size() - 1; i++) {
				Interpolation inter = new Interpolation();
				int firstIndex = labelledSlices.get(i);
				int secondIndex = labelledSlices.get(i + 1);

				inter.width = model.getLabelImagePlus().getWidth();
				inter.color = label;

				inter.firstIndex = firstIndex;
				inter.secondIndex = secondIndex;

				inter.labelledPixels1 = (byte[]) labelsData.getStack().getProcessor(firstIndex).getPixels();
				inter.labelledPixels2 = (byte[]) labelsData.getStack().getProcessor(secondIndex).getPixels();

				inter.bounds = model.getLabelCanvas().getOutline(firstIndex, label).getBounds();
				inter.bounds.add(model.getLabelCanvas().getOutline(secondIndex, label).getBounds());


				for (int j = firstIndex + 1; j < secondIndex; j++) {
					inter.interpolatedPixels.add((byte[]) labelsData.getStack().getProcessor(j).getPixels());
				}
				results.add(inter);
			}

			return results;
		}

		public String toString() {
			return "labelledSlices: " + labelledSlices;
		}
	}


	private static class Interpolation {
		byte[] labelledPixels1;
		byte[] labelledPixels2;
		int firstIndex;
		int secondIndex;

		int width;
		int color;

		Rectangle bounds;

		ArrayList<byte[]> interpolatedPixels = new ArrayList<byte[]>();


		/**
		 * gets the distance between the two labelled pixel slices
		 *
		 * @return
		 */
		int getDistance() {
			return interpolatedPixels.size();
		}

		int getPixel(int x, int y, byte[] data) {
			return data[x + y * width];
		}

		void setPixel(int x, int y, int color, byte[] data) {
			data[x + y * width] = (byte) color;
		}

		/**
		 * runs the interpolation algorithm
		 */
		void interpolate() {

			for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
				for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
					//if both end don't have a pixel at the location
					//then we know that the interpolated image won't weith
					if (getPixel(x, y, labelledPixels1) != color && getPixel(x, y, labelledPixels2) != color) {

					} else if (getPixel(x, y, labelledPixels1) == color && getPixel(x, y, labelledPixels2) == color) {

						//if both ends do have the pixel then we know the interpolated will also
						for (byte[] pixels : interpolatedPixels) {
							setPixel(x, y, color, pixels);
						}

					} else {
						//one slice at x,y is labelled and the other is not
						//we need to do some math to work out what is labelled and what is not

						//we draw a line from the labelled pixel we know to the closest
						//labelled pixel in the other slice
						Line fillLine;
						//a unmatch pixel in one img,
						if (getPixel(x, y, labelledPixels1) == color) {
							//unmatched in lowest slice
							Point oppPoint = getNearest(new Point(x, y), labelledPixels2, width, color);

							if (oppPoint == null) continue;

							//draw a line between the nearest neighbours
							fillLine = new Line(new Point3d(x, y, firstIndex), new Point3d(oppPoint.x, oppPoint.y, secondIndex));
						} else {
							//unmatched in highest slice
							Point oppPoint = getNearest(new Point(x, y), labelledPixels1, width, color);
							if (oppPoint == null) continue;
							//draw a line between the nearest neighbours
							fillLine = new Line(new Point3d(x, y, secondIndex), new Point3d(oppPoint.x, oppPoint.y, firstIndex));
						}

						//System.out.println("fillLine = " + fillLine);

						//now we need to see where this line intesects out unlabelled slices
						//need to check each slice
						int index = firstIndex;
						for (byte[] pixels : interpolatedPixels) {
							int sliceDepth = ++index;

							//create a plane that replesents the slice
							Plane slice = new Plane(0, 0, 1, -sliceDepth);

							Point3d intersect = slice.intersection(fillLine);
							//System.out.println("intersect = " + intersect);

							setPixel((int) Math.floor(intersect.x), (int) Math.ceil(intersect.y), color, pixels);
							setPixel((int) Math.floor(intersect.x), (int) Math.floor(intersect.y), color, pixels);
							setPixel((int) Math.ceil(intersect.x), (int) Math.floor(intersect.y), color, pixels);
							setPixel((int) Math.ceil(intersect.x), (int) Math.ceil(intersect.y), color, pixels);

						}
						//System.out.println("filled");
					}
				}
			}

			//clean up the interpolations
			for (byte[] pixels : interpolatedPixels) {
				 LabelBinaryOps.clean(new Roi(bounds), pixels, width, (byte)color );
			}

			System.out.println("one interpolation done");
		}

		/**
		 * finds the nearest point to the one supplied with the specified colour in the supplied pixel data
		 * performs an outward spiral search starting with the start point
		 *
		 * @param point
		 * @param pixels
		 * @param color
		 * @return
		 */
		private Point getNearest(Point point, byte[] pixels, int width, int color) {
			//should search in an ever increasing spiral circles to find the neairbour
			Utils.Spiral spiral = new Utils.Spiral(point);

			int count = 0;
			while (true) {
				Point tstPoint = spiral.next();
				/*
				if(count++ > max) {
					System.out.println("spiral search failed " + point);
					return null;
				} */

				if (tstPoint.x < 0 || tstPoint.x >= width) continue;
				if (tstPoint.y < 0 || tstPoint.y >= pixels.length / width) continue;

				//System.out.println("tstPoint = " + tstPoint);
				if (pixels[tstPoint.x + tstPoint.y * width] == color) return tstPoint;


			}


		}

	}


}
