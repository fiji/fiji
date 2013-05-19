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
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.IndexColorModel;

public class Find_Connected_Regions implements PlugIn {

	public static final String PLUGIN_VERSION = "1.3";

	public void run(String ignored) {

		GenericDialog gd = new GenericDialog("Find Connected Regions Options (version: "+PLUGIN_VERSION+")");
		gd.addCheckbox("Allow_diagonal connections?", true);
		gd.addCheckbox("Display_image_for_each region?", false);
		gd.addCheckbox("Display_one_image for all regions?", true);
		gd.addCheckbox("Display_results table?", true);
		gd.addCheckbox("Regions_must have the same value?", false);
		gd.addCheckbox("Start_from_point selection?", false);
		gd.addCheckbox("Autosubtract discovered regions from original image?", false);
		gd.addNumericField("Regions_for_values_over: ", 100, 0);
		gd.addNumericField("Minimum_number_of_points in a region", 1, 0);
		gd.addNumericField("Stop_after this number of regions are found: ", -1, 0);
		gd.addMessage("(If number of regions is -1, find all of them.)");

		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		boolean diagonal = gd.getNextBoolean();
		boolean imagePerRegion = gd.getNextBoolean();
		boolean imageAllRegions = gd.getNextBoolean();
		boolean showResults = gd.getNextBoolean();
		boolean mustHaveSameValue = gd.getNextBoolean();
		boolean startFromPointROI = gd.getNextBoolean();
		boolean autoSubtract = gd.getNextBoolean();
		double valuesOverDouble = gd.getNextNumber();
		double minimumPointsInRegionDouble = gd.getNextNumber();
		int stopAfterNumberOfRegions = (int) gd.getNextNumber();

		ImagePlus imagePlus = IJ.getImage();
		if (imagePlus == null) {
			IJ.error("No image to operate on.");
			return;
		}

		FindConnectedRegions fcr = new FindConnectedRegions();
		try {
			fcr.run( imagePlus,
				 diagonal,
				 imagePerRegion,
				 imageAllRegions,
				 showResults,
				 mustHaveSameValue,
				 startFromPointROI,
				 autoSubtract,
				 valuesOverDouble,
				 minimumPointsInRegionDouble,
				 stopAfterNumberOfRegions,
				 false /* noUI */ );
		} catch( IllegalArgumentException iae ) {
			IJ.error(""+iae);
			return;
		}
	}
}
