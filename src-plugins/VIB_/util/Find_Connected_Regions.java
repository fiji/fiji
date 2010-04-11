/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugin "Find Connected Regions".

    The ImageJ plugin "Find Connected Regions" is free software; you
    can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software
    Foundation; either version 3 of the License, or (at your option)
    any later version.

    The ImageJ plugin "Find Connected Regions" is distributed in the
    hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A
    PARTICULAR PURPOSE.  See the GNU General Public License for more
    details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* This plugin looks for connected regions with the same value in 8
 * bit images, and optionally displays images with just each of those
 * connected regions.  (Otherwise the useful information is just
 * printed out.)
 */

package util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.PointRoi;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;
import amira.AmiraParameters;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.plugin.ImageCalculator;
import java.awt.image.ColorModel;
import ij.measure.ResultsTable;
import java.awt.Dialog;
import java.awt.Button;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class CancelDialog extends Dialog implements ActionListener {
	Button cancel;
	Find_Connected_Regions plugin;
	public CancelDialog(Find_Connected_Regions plugin) {
		super( IJ.getInstance(), "Find Connected Regions", false );
		this.plugin = plugin;
		cancel = new Button("Cancel 'Find Connected Regions'");
		add(cancel);
		cancel.addActionListener(this);
		pack();
	}
        public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();
                if( source == cancel ) {
                        plugin.cancel();
		}
	}
}

public class Find_Connected_Regions implements PlugIn {

	public static final String PLUGIN_VERSION = "1.2";

	boolean pleaseStop = false;

	public void cancel() {
		pleaseStop = true;
	}

	/* An inner class to make the results list sortable. */
	private class Region implements Comparable {

		Region(int value, String materialName, int points, boolean sameValue) {
			byteImage = true;
			this.value = value;
			this.materialName = materialName;
			this.points = points;
			this.sameValue = sameValue;
		}

		Region(int points, boolean sameValue) {
			byteImage = false;
			this.points = points;
			this.sameValue = sameValue;
		}

		boolean byteImage;
		int points;
		String materialName;
		int value;
		boolean sameValue;

		public int compareTo(Object otherRegion) {
			Region o = (Region) otherRegion;
			return (points < o.points) ? -1 : ((points > o.points) ? 1 : 0);
		}

		@Override
		public String toString() {
			if (byteImage) {
				String materialBit = "";
				if (materialName != null) {
					materialBit = " (" + materialName + ")";
				}
				return "Region of value " + value + materialBit + " containing " + points + " points";
			} else {
				return "Region containing " + points + " points";
			}
		}

		public void addRow( ResultsTable rt ) {
			rt.incrementCounter();
			if(byteImage) {
				if(sameValue)
					rt.addValue("Value in Region",value);
				rt.addValue("Points In Region",points);
				if(materialName!=null)
					rt.addLabel("Material Name",materialName);
			} else {
				rt.addValue("Points in Region",points);
			}
		}

	}
	private static final byte IN_QUEUE = 1;
	private static final byte ADDED = 2;

	public void run(String ignored) {

		GenericDialog gd = new GenericDialog("Find Connected Regions Options (version: "+PLUGIN_VERSION+")");
		gd.addCheckbox("Allow_diagonal connections?", false);
		gd.addCheckbox("Display_image_for_each region?", true);
		gd.addCheckbox("Display_results table?", true);
		gd.addCheckbox("Regions_must have the same value?", true);
		gd.addCheckbox("Start_from_point selection?", false);
		gd.addCheckbox("Autosubtract discovered regions from original image?", false);
		gd.addNumericField("Regions_for_values_over: ", 0, 0);
		gd.addNumericField("Minimum_number_of_points in a region", 1, 0);
		gd.addNumericField("Stop_after this number of regions are found: ", 1, 0);
		gd.addMessage("(If number of regions is -1, find all of them.)");

		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		boolean diagonal = gd.getNextBoolean();
		boolean display = gd.getNextBoolean();
		boolean showResults = gd.getNextBoolean();
		boolean mustHaveSameValue = gd.getNextBoolean();
		boolean startFromPointROI = gd.getNextBoolean();
		boolean autoSubtract = gd.getNextBoolean();
		double valuesOverDouble = gd.getNextNumber();
		double minimumPointsInRegionDouble = gd.getNextNumber();
		int stopAfterNumberOfRegions = (int) gd.getNextNumber();

		
		ImageCalculator iCalc = new ImageCalculator();
		
		ImagePlus imagePlus = IJ.getImage();
		if (imagePlus == null) {
			IJ.error("No image to operate on.");
			return;
		}

		int type = imagePlus.getType();

		if (!(ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type || ImagePlus.GRAY32 == type)) {
			IJ.error("The image must be either 8 bit or 32 bit for this plugin.");
			return;
		}

		boolean byteImage = false;
		if (ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type) {
			byteImage = true;
		}

		if (!byteImage && mustHaveSameValue) {
			IJ.error("You can only specify that each region must have the same value for 8 bit images.");
			return;
		}				

		boolean startAtMaxValue = !mustHaveSameValue;

		int point_roi_x = -1;
		int point_roi_y = -1;
		int point_roi_z = -1;

		if( startFromPointROI ) {
			
			Roi roi = imagePlus.getRoi();
			if (roi == null) {
				IJ.error("There's no point selected in the image.");
				return;
			}
			if (roi.getType() != Roi.POINT) {
				IJ.error("There's a selection in the image, but it's not a point selection.");
				return;
			}			
			Polygon p = roi.getPolygon();
			if(p.npoints > 1) {
				IJ.error("You can only have one point selected.");
				return;
			}
			
			point_roi_x = p.xpoints[0];
			point_roi_y = p.ypoints[0];
			point_roi_z = imagePlus.getCurrentSlice()-1;
			
			System.out.println("Fetched ROI with co-ordinates: "+p.xpoints[0]+", "+p.ypoints[0]);			
		}

		int width = imagePlus.getWidth();
		int height = imagePlus.getHeight();
		int depth = imagePlus.getStackSize();

		if (width * height * depth > Integer.MAX_VALUE) {
			IJ.error("This stack is too large for this plugin (must have less than " + Integer.MAX_VALUE + " points.");
			return;
		}

		String[] materialList = null;

		AmiraParameters parameters = null;
		if (AmiraParameters.isAmiraLabelfield(imagePlus)) {
			parameters = new AmiraParameters(imagePlus);
			materialList = parameters.getMaterialList();
		}

		ArrayList<Region> results = new ArrayList<Region>();

		ImageStack stack = imagePlus.getStack();

		byte[][] sliceDataBytes = null;
		float[][] sliceDataFloats = null;

		if (byteImage) {
			sliceDataBytes = new byte[depth][];
			for (int z = 0; z < depth; ++z) {
				ByteProcessor bp = (ByteProcessor) stack.getProcessor(z+1);
				sliceDataBytes[z] = (byte[]) bp.getPixelsCopy();
			}
		} else {
			sliceDataFloats = new float[depth][];
			for (int z = 0; z < depth; ++z) {
				FloatProcessor bp = (FloatProcessor) stack.getProcessor(z+1);
				sliceDataFloats[z] = (float[]) bp.getPixelsCopy();
			}
		}

		// Preserve the calibration and colour lookup tables
		// for generating new images of each individual
		// region.
		Calibration calibration = imagePlus.getCalibration();

		ColorModel cm = null;
		if (ImagePlus.COLOR_256 == type) {
			cm = stack.getColorModel();
		}

		ResultsTable rt=ResultsTable.getResultsTable();
		rt.reset();

		CancelDialog cancelDialog=new CancelDialog(this);
		cancelDialog.show();

		boolean firstTime = true;

		while (true) {

			if( pleaseStop )
				break;

			/* Find one pixel that's above the minimum, or
			   find the maximum in the case where we're
			   not insisting that all regions are made up
			   of the same color.  These are set in all
			   cases... */

			int initial_x = -1;
			int initial_y = -1;
			int initial_z = -1;

			int foundValueInt = -1;
			float foundValueFloat = Float.MIN_VALUE;
			int maxValueInt = -1;
			float maxValueFloat = Float.MIN_VALUE;

			if (firstTime && startFromPointROI ) {
				
				initial_x = point_roi_x;
				initial_y = point_roi_y;
				initial_z = point_roi_z;

				if(byteImage)
					foundValueInt = sliceDataBytes[initial_z][initial_y * width + initial_x] & 0xFF;
				else
					foundValueFloat = sliceDataFloats[initial_z][initial_y * width + initial_x];

			} else if (byteImage && startAtMaxValue) {

				for (int z = 0; z < depth; ++z) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							int value = sliceDataBytes[z][y * width + x] & 0xFF;
							if (value > maxValueInt) {
								initial_x = x;
								initial_y = y;
								initial_z = z;
								maxValueInt = value;
							}
						}
					}
				}

				foundValueInt = maxValueInt;

				/* If the maximum value is below the
				   level we care about, we're done. */

				if (foundValueInt < valuesOverDouble) {
					break;
				}

			} else if (byteImage && !startAtMaxValue) {

				// Just finding some point in the a region...
				for (int z = 0; z < depth && foundValueInt == -1; ++z) {
					for (int y = 0; y < height && foundValueInt == -1; ++y) {
						for (int x = 0; x < width; ++x) {
							int value = sliceDataBytes[z][y * width + x] & 0xFF;
							if (value > valuesOverDouble) {

								initial_x = x;
								initial_y = y;
								initial_z = z;
								foundValueInt = value;
								break;
							}
						}
					}
				}

				if (foundValueInt == -1) {
					break;
				}

			} else {

				// This must be a 32 bit image and we're starting at the maximum
				assert (!byteImage && startAtMaxValue);

				for (int z = 0; z < depth; ++z) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							float value = sliceDataFloats[z][y * width + x];
							if (value > valuesOverDouble) {
								initial_x = x;
								initial_y = y;
								initial_z = z;
								maxValueFloat = value;
							}
						}
					}
				}

				foundValueFloat = maxValueFloat;

				if (foundValueFloat == Float.MIN_VALUE) {
					break;

					// If the maximum value is below the level we
					// care about, we're done.
				}
				if (foundValueFloat < valuesOverDouble) {
					break;
				}

			}

			firstTime = false;

			int vint = foundValueInt;
			float vfloat = foundValueFloat;

			String materialName = null;
			if (materialList != null) {
				materialName = materialList[vint];
			}
			int pointsInQueue = 0;
			int queueArrayLength = 1024;
			int[] queue = new int[queueArrayLength];

			byte[] pointState = new byte[depth * width * height];
			int i = width * (initial_z * height + initial_y) + initial_x;
			pointState[i] = IN_QUEUE;
			queue[pointsInQueue++] = i;

			int pointsInThisRegion = 0;

			while (pointsInQueue > 0) {

				if(pleaseStop)
					break;

				int nextIndex = queue[--pointsInQueue];

				int currentPointStateIndex = nextIndex;
				int pz = nextIndex / (width * height);
				int currentSliceIndex = nextIndex % (width * height);
				int py = currentSliceIndex / width;
				int px = currentSliceIndex % width;

				pointState[currentPointStateIndex] = ADDED;

				if (byteImage) {
					sliceDataBytes[pz][currentSliceIndex] = 0;
				} else {
					sliceDataFloats[pz][currentSliceIndex] = Float.MIN_VALUE;
				}
				++pointsInThisRegion;

				int x_unchecked_min = px - 1;
				int y_unchecked_min = py - 1;
				int z_unchecked_min = pz - 1;

				int x_unchecked_max = px + 1;
				int y_unchecked_max = py + 1;
				int z_unchecked_max = pz + 1;

				int x_min = (x_unchecked_min < 0) ? 0 : x_unchecked_min;
				int y_min = (y_unchecked_min < 0) ? 0 : y_unchecked_min;
				int z_min = (z_unchecked_min < 0) ? 0 : z_unchecked_min;

				int x_max = (x_unchecked_max >= width) ? width - 1 : x_unchecked_max;
				int y_max = (y_unchecked_max >= height) ? height - 1 : y_unchecked_max;
				int z_max = (z_unchecked_max >= depth) ? depth - 1 : z_unchecked_max;

				for (int z = z_min; z <= z_max; ++z) {
					for (int y = y_min; y <= y_max; ++y) {
						for (int x = x_min; x <= x_max; ++x) {

							// If we're not including diagonals,
							// skip those points.
							if ((!diagonal) && (x == x_unchecked_min || x == x_unchecked_max) && (y == y_unchecked_min || y == y_unchecked_max) && (z == z_unchecked_min || z == z_unchecked_max)) {
								continue;
							}
							int newSliceIndex = y * width + x;
							int newPointStateIndex = width * (z * height + y) + x;

							if (byteImage) {

								int neighbourValue = sliceDataBytes[z][newSliceIndex] & 0xFF;

								if (mustHaveSameValue) {
									if (neighbourValue != vint) {
										continue;
									}
								} else {
									if (neighbourValue <= valuesOverDouble) {
										continue;
									}
								}
							} else {

								float neighbourValue = sliceDataFloats[z][newSliceIndex];

								if (neighbourValue <= valuesOverDouble) {
									continue;
								}
							}

							if (0 == pointState[newPointStateIndex]) {
								pointState[newPointStateIndex] = IN_QUEUE;
								if (pointsInQueue == queueArrayLength) {
									int newArrayLength = (int) (queueArrayLength * 1.2);
									int[] newArray = new int[newArrayLength];
									System.arraycopy(queue, 0, newArray, 0, pointsInQueue);
									queue = newArray;
									queueArrayLength = newArrayLength;
								}
								queue[pointsInQueue++] = newPointStateIndex;
							}
						}
					}
				}
			}

			if(pleaseStop)
				break;

			// So now pointState should have no IN_QUEUE
			// status points...
			Region region;
			if (byteImage) {
				region = new Region(vint, materialName, pointsInThisRegion, mustHaveSameValue );
			} else {
				region = new Region(pointsInThisRegion, mustHaveSameValue);
			}
			if (pointsInThisRegion < minimumPointsInRegionDouble) {
				// System.out.println("Too few points - only " + pointsInThisRegion);
				continue;
			}

			results.add(region);

			byte replacementValue;
			if (byteImage) {
				replacementValue = (byte) ( (cm == null) ? 255 : vint );
			} else {
				replacementValue = (byte) 255;
			}

	
			if (display || autoSubtract) {

				ImageStack newStack = new ImageStack(width, height);
				for (int z = 0; z < depth; ++z) {
					byte[] sliceBytes = new byte[width * height];
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {

							byte status = pointState[width * (z * height + y) + x];

							if (status == IN_QUEUE) {
								IJ.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE");
							}

							if (status == ADDED) {
								sliceBytes[y * width + x] = replacementValue;
							}
						}
					}
					ByteProcessor bp = new ByteProcessor(width, height);
					bp.setPixels(sliceBytes);
					newStack.addSlice("", bp);
				}

				if (ImagePlus.COLOR_256 == type) {
					if (cm != null) {
						newStack.setColorModel(cm);
					}
				}

				ImagePlus newImagePlus = new ImagePlus(region.toString(), newStack);

				if (calibration != null) {
					newImagePlus.setCalibration(calibration);
				}

				if (parameters != null) {
					parameters.setParameters(newImagePlus, true);
				}
				
				if (autoSubtract) {
					iCalc.calculate("Subtract stack", imagePlus, newImagePlus);
				}
				
				if (display) {
					newImagePlus.show();
				} else {
					newImagePlus.changes = false;
					newImagePlus.close();
				}
			}

			if ( (stopAfterNumberOfRegions > 0) && (results.size() >= stopAfterNumberOfRegions) ) {
				break;
			}
		}

		Collections.sort(results, Collections.reverseOrder());

		cancelDialog.dispose();

		for (Iterator<Region> it = results.iterator(); it.hasNext();) {
			Region r = it.next();
			System.out.println(r.toString());		       			
			if( showResults ) {
				r.addRow(rt);
			}		
		}

		if( showResults )
			rt.show("Results");
		
	}
}
