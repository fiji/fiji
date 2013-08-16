/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;

import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Comparator;

import landmarks.NamedPointWorld;
import vib.FastMatrix;
import vib.TransformedImage;

import vib.oldregistration.RegistrationAlgorithm;

import util.Overlay_Registered;

public class Rigid_From_Landmarks extends RigidFromLandmarks implements PlugIn {
	public void run(String arg) {

		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("Rigid_From_Landmarks: No images are open.");
			return;
		}

		String[] titles = new String[wList.length+1];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}

		String none = "*None*";
		titles[wList.length] = none;

		GenericDialog gd = new GenericDialog("Rigid Registration from Landmarks");
		gd.addChoice("Template stack:", titles, titles[0]);
		gd.addChoice("Stack to transform:", titles, titles[1]);

		gd.addCheckbox("Allow scaling",true);
		gd.addCheckbox("Overlay result",true);

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();

		setImages( WindowManager.getImage(wList[index[0]]), WindowManager.getImage(wList[index[1]]) );

		allowScaling = gd.getNextBoolean();

		boolean overlayResult = gd.getNextBoolean();

		ImagePlus transformed = register();

		if( overlayResult ) {
			ImagePlus merged = Overlay_Registered.overlayToImagePlus( sourceImages[0], transformed );
			merged.setTitle( "Registered and Overlayed" );
			merged.show();
		} else
			transformed.show();
	}
}
