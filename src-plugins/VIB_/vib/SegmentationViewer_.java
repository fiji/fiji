package vib;

import amira.AmiraParameters;

import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** This plugin is an example showing how to add a non-destructive 
  graphic overlay to an image or stack. */
public class SegmentationViewer_ implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();

		if(AmiraParameters.isAmiraLabelfield(imp)) {
			IJ.error("The current image is a labelfield!");
			return;
		}

		// find labelfields
		Vector labelfields=new Vector();
		ImagePlus labels;
		for(int i=1; (labels=WindowManager.getImage(i))!=null; i++) {
			// TODO: check dimensions
			if(AmiraParameters.isAmiraLabelfield(labels))
				labelfields.add(labels);
		}
		// TODO: show dialog to create a labelfield
		if(labelfields.size()<1) {
			IJ.error("No labelfields loaded");
			return;
		}

		if(labelfields.size()>1) {
			String[] list=new String[labelfields.size()];
			for(int i=0;i<list.length;i++)
				list[i]=((ImagePlus)labelfields.get(i)).getTitle();

			GenericDialog gd=new GenericDialog("Parameters");
			gd.addChoice("Labelfield",list,list[0]);
			gd.showDialog();
			if(gd.wasCanceled())
				return;

			labels=(ImagePlus)labelfields.get(gd.getNextChoiceIndex());
		} else
			labels=(ImagePlus)labelfields.get(0);

		SegmentationViewerCanvas cc = new SegmentationViewerCanvas(imp, labels);
		if (imp.getStackSize()>1)
			new StackWindow(imp, cc);
		else
			new ImageWindow(imp, cc);
	}
}

