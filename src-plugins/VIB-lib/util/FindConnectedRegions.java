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

    In addition, as a special exception, the copyright holders give
    you permission to combine this program with free software programs or
    libraries that are released under the Apache Public License.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* This plugin looks for connected regions with the same value in 8
   bit images, and optionally displays images with just each of those
   connected regions or one image where each value represents the
   region number.  The sizes of the regions are output to the results table.

   This is rather inefficient at the moment:

      - We maintain an extra "pointState" array of bytes the size of
      the image, which we could do without - the state of the search
      could be stored in the output image.

      - We should exclude sub-threshold points discovered in the
      search as we go along, and at the moment this isn't done.

 */

package util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.PointRoi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.IndexColorModel;

class CancelDialog extends Dialog implements ActionListener {
	Button cancel;
	FindConnectedRegions fcr;
	public CancelDialog(FindConnectedRegions fcr) {
		super( IJ.getInstance(), "Find Connected Regions", false );
		this.fcr = fcr;
		cancel = new Button("Cancel 'Find Connected Regions'");
		add(cancel);
		cancel.addActionListener(this);
		pack();
	}
        public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();
                if( source == cancel ) {
                        fcr.cancel();
		}
	}
}

public class FindConnectedRegions {

	volatile boolean pleaseStop = false;

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
	private static final byte ADDED_TO_CURRENT_REGION = 2;
	private static final byte IN_PREVIOUS_REGION = 3;

	public static IndexColorModel backgroundAndSpectrum() {
		return backgroundAndSpectrum(255);
	}

	/* This method returns an IndexColorModel where 0 is black
	   (background) and 1 to min(maximum,255) inclusive are spread
	   through the spectrum.  Any higher values are set to white. */
	public static IndexColorModel backgroundAndSpectrum(int maximum) {
		if( maximum > 255 )
			maximum = 255;
		byte [] reds = new byte[256];
		byte [] greens = new byte[256];
		byte [] blues = new byte[256];
		// Set all to white:
		for( int i = 0; i < 256; ++i ) {
			reds[i] = greens[i] = blues[i] = (byte)255;
		}
		// Set 0 to black:
		reds[0] = greens[0] = blues[0] = 0;
		float divisions = maximum;
		Color c;
		for( int i = 1; i <= maximum; ++i ) {
			float h = (i - 1) / divisions;
			c = Color.getHSBColor(h,1f,1f);
			reds[i] = (byte)c.getRed();
			greens[i] = (byte)c.getGreen();
			blues[i] = (byte)c.getBlue();
		}
		return new IndexColorModel( 8, 256, reds, greens, blues );
	}

	public class Results {
		public List<ImagePlus> perRegion;
		public ImagePlus allRegions;
		public List<Region> regionInfo;
	}

	static final boolean verbose = false;

	public Results run( ImagePlus imagePlus,
			    boolean diagonal,
			    boolean imagePerRegion,
			    boolean imageAllRegions,
			    boolean showResults,
			    boolean mustHaveSameValue,
			    boolean startFromPointROI,
			    boolean autoSubtract,
			    double valuesOverDouble,
			    double minimumPointsInRegionDouble,
			    int stopAfterNumberOfRegions,
			    boolean noUI ) {

		Results results = new Results();
		if( imagePerRegion )
			results.perRegion = new ArrayList();
		results.regionInfo = new ArrayList<Region>();

		ImageCalculator iCalc = new ImageCalculator();

		int type = imagePlus.getType();

		if (!(ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type || ImagePlus.GRAY32 == type))
			throw new IllegalArgumentException("The image must be either 8 bit or 32 bit for this plugin.");

		boolean byteImage = false;
		if (ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type) {
			byteImage = true;
		}

		if (!byteImage && mustHaveSameValue)
			throw new IllegalArgumentException("You can only specify that each region must have the same value for 8 bit images.");

		int point_roi_x = -1;
		int point_roi_y = -1;
		int point_roi_z = -1;

		if( startFromPointROI ) {

			Roi roi = imagePlus.getRoi();
			if (roi == null)
				throw new IllegalArgumentException("There's no point selected in the image.");

			if (roi.getType() != Roi.POINT)
				throw new IllegalArgumentException("There's a selection in the image, but it's not a point selection.");

			Polygon p = roi.getPolygon();
			if(p.npoints > 1)
				throw new IllegalArgumentException("You can only have one point selected.");

			point_roi_x = p.xpoints[0];
			point_roi_y = p.ypoints[0];
			point_roi_z = imagePlus.getCurrentSlice()-1;
		}

		int width = imagePlus.getWidth();
		int height = imagePlus.getHeight();
		int depth = imagePlus.getStackSize();

		if (width * height * depth > Integer.MAX_VALUE)
			throw new IllegalArgumentException("This stack is too large for this plugin (must have less than " + Integer.MAX_VALUE + " points.");

		String[] materialList = null;

		AmiraParameters parameters = null;
		if (AmiraParameters.isAmiraLabelfield(imagePlus)) {
			parameters = new AmiraParameters(imagePlus);
			materialList = parameters.getMaterialList();
		}

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

		/* Preserve the calibration and colour lookup tables
		   for generating new images of each individual
		   region. */
		Calibration calibration = imagePlus.getCalibration();

		ColorModel cm = null;
		if (ImagePlus.COLOR_256 == type) {
			cm = stack.getColorModel();
		}

		String defaultAllRegionsTitle = "All connected regions";

		ImageStack allRegionsStack = null;
		short [][] allRegionsPixels = null;
		if( imageAllRegions ) {
			allRegionsStack = new ImageStack(width,height);
			allRegionsPixels = new short[depth][width*height];
			for( int z = 0; z < depth; z++ ) {
				ShortProcessor sp = new ShortProcessor(width,height);
				sp.setPixels(allRegionsPixels[z]);
				allRegionsStack.addSlice("",sp);
			}
			allRegionsStack.setColorModel(backgroundAndSpectrum(0));
			results.allRegions = new ImagePlus(
				defaultAllRegionsTitle + " (still generating...)",
				allRegionsStack);
			if (calibration != null)
				results.allRegions.setCalibration(calibration);
			if (parameters != null)
				parameters.setParameters(results.allRegions, true);
			if( ! noUI )
				results.allRegions.show();
		}

		ResultsTable rt=ResultsTable.getResultsTable();
		rt.reset();

		CancelDialog cancelDialog = null;
		try {

			if( ! noUI ) {
				cancelDialog=new CancelDialog(this);
				cancelDialog.show();
			}

			boolean firstTime = true;

			int regionNumber = 0;

			int numberOfPointsInStack = width * height * depth;
			byte[] pointState = new byte[numberOfPointsInStack];

			int ignoreBeforeX = 0;
			int ignoreBeforeY = 0;
			int ignoreBeforeZ = 0;

			IJ.showProgress(0);

			int lastProgessZ = 0;

			while (true) {

				if( pleaseStop )
					break;

				/* Find the next pixel that's should be in a region: */

				int initial_x = -1;
				int initial_y = -1;
				int initial_z = -1;

				int foundValueInt = -1;
				float foundValueFloat = Float.MIN_VALUE;

				// ------------------------------------------------------------------------
				/* The next section tries to find the next starting point, depending on the
				   options the user chose: */

				if (firstTime && startFromPointROI ) {

					initial_x = point_roi_x;
					initial_y = point_roi_y;
					initial_z = point_roi_z;

					boolean tooSmall;
					if(byteImage) {
						foundValueInt = sliceDataBytes[initial_z][initial_y * width + initial_x] & 0xFF;
						tooSmall = foundValueInt <= valuesOverDouble;
					} else {
						foundValueFloat = sliceDataFloats[initial_z][initial_y * width + initial_x];
						tooSmall = foundValueFloat <= valuesOverDouble;
					}
					if(tooSmall)
						throw new IllegalArgumentException("The value at the point you selected is not over the threshold you specified ("+valuesOverDouble+")");

				} else if (byteImage) {

					boolean foundPoint = false;
					for (int z = ignoreBeforeZ; z < depth && ! foundPoint; ++z) {
						if( z != lastProgessZ ) {
							IJ.showProgress(z / (double)depth);
							lastProgessZ = z;
						}
						int startY = (z == ignoreBeforeZ) ? ignoreBeforeY : 0;
						for (int y = startY; y < height && ! foundPoint; ++y) {
							int startX = (z == ignoreBeforeZ && y == ignoreBeforeY) ? ignoreBeforeX : 0;
							for (int x = startX; x < width; ++x) {
								if( IN_PREVIOUS_REGION == pointState[ width * (z * height + y) + x ] )
									continue;
								int value = sliceDataBytes[z][y * width + x] & 0xFF;
								if (value > valuesOverDouble) {
									initial_x = x;
									initial_y = y;
									initial_z = z;
									foundValueInt = value;
									foundPoint = true;
									break;
								}
							}
						}
					}

					if( foundValueInt == -1 )
						break;

				} else {

					// This must be a 32 bit image:
					assert (!byteImage);

					boolean foundPoint = false;
					for (int z = ignoreBeforeZ; z < depth && ! foundPoint; ++z) {
						if( z != lastProgessZ ) {
							IJ.showProgress(z / (double)depth);
							lastProgessZ = z;
						}
						int startY = (z == ignoreBeforeZ) ? ignoreBeforeY : 0;
						for (int y = startY; y < height && ! foundPoint; ++y) {
							int startX = (z == ignoreBeforeZ && y == ignoreBeforeY) ? ignoreBeforeX : 0;
							for (int x = startX; x < width; ++x) {
								if( IN_PREVIOUS_REGION == pointState[ width * (z * height + y) + x ] )
									continue;
								float value = sliceDataFloats[z][y * width + x];
								if (value > valuesOverDouble) {
									initial_x = x;
									initial_y = y;
									initial_z = z;
									foundValueFloat = value;
									foundPoint = true;
									break;
								}
							}
						}
					}

					if (foundValueFloat == Float.MIN_VALUE)
						break;
				}

				// ------------------------------------------------------------------------
				/* Now we've got the starting point, we can record that we can start
				   at the next part when we start searching again */

				/* If x >= width it immediately moves on to
				   the next y as we'd like */
				ignoreBeforeX = initial_x + 1;
				ignoreBeforeZ = initial_z;
				ignoreBeforeY = initial_y;

				if( verbose )
					System.out.println("Starting from "+initial_x+", "+initial_y+", "+initial_z);

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

					if( verbose )
						System.out.println("  Considering point from queue at "+px+", "+py+", "+pz);

					pointState[currentPointStateIndex] = ADDED_TO_CURRENT_REGION;

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

					int x_min = Math.max(0,x_unchecked_min);
					int y_min = Math.max(0,y_unchecked_min);
					int z_min = Math.max(0,z_unchecked_min);

					int x_max = Math.min(x_unchecked_max,width-1);
					int y_max = Math.min(y_unchecked_max,height-1);
					int z_max = Math.min(z_unchecked_max,depth-1);

					for (int z = z_min; z <= z_max; ++z) {
						for (int y = y_min; y <= y_max; ++y) {
							for (int x = x_min; x <= x_max; ++x) {

								int x_off_centre = (x == x_unchecked_min || x == x_unchecked_max) ? 1 : 0;
								int y_off_centre = (y == y_unchecked_min || y == y_unchecked_max) ? 1 : 0;
								int z_off_centre = (z == z_unchecked_min || z == z_unchecked_max) ? 1 : 0;

								int off_centre_total = x_off_centre + y_off_centre + z_off_centre;

								// Ignore the start point:
								if( off_centre_total == 0 )
									continue;

								// If we're not including diagonals,
								// skip those points too:
								if (!diagonal) {
									if( x_off_centre + y_off_centre + z_off_centre > 1 )
										continue;
								}
								if( verbose ) {
									System.out.println("    Considering neighbour point at: "+x+", "+y+", "+z);
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

								if( verbose ) {
									System.out.println("    Not excluded by value");
									System.out.println("    pointState is: "+pointState[newPointStateIndex]);
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
									if( verbose )
										System.out.println("    ... so adding");
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
					/* But we don't want to keep searching
					   these, so set as IN_PREVIOUS_REGION: */
					for( int p = 0; p < numberOfPointsInStack; ++p )
						if( pointState[p] == ADDED_TO_CURRENT_REGION )
							pointState[p] = IN_PREVIOUS_REGION;
					continue;
				}

				++regionNumber;
				results.regionInfo.add(region);

				byte replacementValue;
				if (byteImage) {
					replacementValue = (byte) ( (cm == null) ? 255 : vint );
				} else {
					replacementValue = (byte) 255;
				}

				if (imageAllRegions) {
					if( regionNumber == Short.MAX_VALUE + 1 ) {
						IJ.showMessage("Found more regions than Short.MAX_VALUE, so the all regions image will have overflowed values...");
					}
					/* Look for all the ADDED_TO_CURRENT_REGION points just found, and
					   add them to the "all regions" image: */
					for (int z = 0; z < depth; ++z ) {
						for( int y = 0; y < height; ++y ) {
							for( int x = 0; x < width; ++x ) {
								if( pointState[width * (z * height + y) + x] == ADDED_TO_CURRENT_REGION ) {
									allRegionsPixels[z][y*width+x] = (short)regionNumber;
								}
							}
						}
					}
					allRegionsStack.setColorModel(backgroundAndSpectrum(Math.min(regionNumber,255)));
					ImageProcessor ip = results.allRegions.getProcessor();
					if( ip != null ) {
						ip.setColorModel(backgroundAndSpectrum(Math.min(regionNumber,255)));
						int min = 0;
						int max = Math.max( regionNumber, 255 );
						ip.setMinAndMax( min, max );
					}
					results.allRegions.updateAndDraw();
				}

				/* In either case we generate a new image for
				   that region, either display it or just use
				   it for subtracing from the original image */

				if (imagePerRegion || autoSubtract) {

					ImageStack newStack = new ImageStack(width, height);
					for (int z = 0; z < depth; ++z) {
						byte[] sliceBytes = new byte[width * height];
						for (int y = 0; y < height; ++y) {
							for (int x = 0; x < width; ++x) {

								byte status = pointState[width * (z * height + y) + x];

								if (status == IN_QUEUE) {
									IJ.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE");
								}

								if (status == ADDED_TO_CURRENT_REGION) {
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

					if (imagePerRegion) {
						if( ! noUI )
							newImagePlus.show();
					} else {
						newImagePlus.changes = false;
						newImagePlus.close();
					}
				}

				for( int p = 0; p < numberOfPointsInStack; ++p )
					if( pointState[p] == ADDED_TO_CURRENT_REGION )
						pointState[p] = IN_PREVIOUS_REGION;

				if ( (stopAfterNumberOfRegions > 0) && (results.regionInfo.size() >= stopAfterNumberOfRegions) ) {
					break;
				}
			}

			IJ.showProgress(1.0);

			if( imageAllRegions )
				results.allRegions.setTitle(defaultAllRegionsTitle);

		} finally {
			if( ! noUI && cancelDialog != null )
				cancelDialog.dispose();
		}

		for (Iterator<Region> it = results.regionInfo.iterator(); it.hasNext();) {
			Region r = it.next();
			if( verbose )
				System.out.println(r.toString());
			if( showResults ) {
				r.addRow(rt);
			}
		}

		if( showResults && ! noUI )
			rt.show("Results");

		return results;
	}
}
