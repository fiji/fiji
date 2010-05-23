/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import amira.AmiraParameters;

import distance.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextWindow;
import java.awt.Choice;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import math3d.Point3d;
import pal.math.*;

public class RigidRegistration_ extends RigidRegistration
		implements PlugInFilter {
	ImagePlus image;

	GenericDialog gd;
        
	public void run(ImageProcessor ip) {
		verbose = true;
		gd = new GenericDialog("Registration Parameters");
                gd.addMessage("Transforming: "+image.getTitle());
		gd.addStringField("initialTransform", "", 30);
		gd.addNumericField("n initial positions to try", 1, 0);

		int level = 0;
		while ((image.getWidth() >> level) > 20)
			level++;
		gd.addNumericField("tolerance", 1.0, 3);
		gd.addNumericField("level", level, 0);
		gd.addNumericField("stopLevel", (level > 2 ? 2 : level), 0);
		gd.addStringField("materialCenterAndBBox", "", 30);
		gd.addCheckbox("noOptimization", false);
		gd.addCheckbox("showTransformed", false);
		gd.addCheckbox("showDifferenceImage", false);
		gd.addCheckbox("Fast but inaccurate", !true);

                
                int[] wIDs = WindowManager.getIDList();
                if(wIDs == null){
                        IJ.error("No images open");
                        return;
                }
                String[] titles = new String[wIDs.length];
                for(int i=0;i<wIDs.length;i++){
                        titles[i] = WindowManager.
                                        getImage(wIDs[i]).getTitle();
                }

                ArrayList<ImagePlus> otherImages = new ArrayList<ImagePlus>(); 
                                
                boolean isLabels = AmiraParameters.isAmiraLabelfield(image);

		if (isLabels) {
			AmiraParameters params = new AmiraParameters(image);
			materials1 = params.getMaterialList();
			gd.addChoice("material", materials1, materials1[0]);
			// cannot possibly fail!
			AmiraParameters.addAmiraLabelsList(gd, "Template");
			gd.addChoice("templateMaterial", 
					materials1, materials1[0]);
			getMaterials2();
		} else {			
			gd.addChoice("Template", titles,
				WindowManager.getCurrentImage().getTitle());
			String[] methods = {
				"Euclidean", "MutualInfo", "Threshold55",
				"Threshold155", "Correlation" };
			gd.addChoice("measure", methods, "Euclidean");

                        // Add a list of images of the same size to also
                        // transform....
                        gd.addMessage("Also transform these images:");
                        for (int i=0; i<wIDs.length; i++) {
                                ImagePlus imp = WindowManager.getImage(wIDs[i]);
                                if( imp.getWidth() == image.getWidth() &&
                                    imp.getHeight() == image.getHeight() &&
                                    imp.getStackSize() == image.getStackSize() ) {

                                    otherImages.add(imp);
                                    gd.addCheckbox(imp.getTitle(), false);
                                }
                        }                
                }
                
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String initial = gd.getNextString();
		int nInitialPositions = (int) gd.getNextNumber();
		double tolerance = gd.getNextNumber();
		level = (int)gd.getNextNumber();
		int stopLevel = (int)gd.getNextNumber();
		String materialBBox = gd.getNextString();
		boolean noOptimization = gd.getNextBoolean();
		boolean showTransformed = gd.getNextBoolean();
		boolean showDifferenceImage = gd.getNextBoolean();
		boolean fastButInaccurate = gd.getNextBoolean();
		int mat1 = (isLabels ? gd.getNextChoiceIndex() : -1);
		ImagePlus templ = WindowManager.getImage(gd.getNextChoice());
		int mat2 = (isLabels ? gd.getNextChoiceIndex() : -1);
		TransformedImage trans = new TransformedImage(templ, image);
                ArrayList<ImagePlus> alsoTransform = new ArrayList<ImagePlus>();

		int templType = templ.getType();
		int imageType = image.getType();

		int templBitDepth = templ.getBitDepth();
		int imageBitDepth = image.getBitDepth();

		ImageStack templStack = templ.getStack();
		ImageStack imageStack = image.getStack();

		if( templBitDepth != imageBitDepth ) {
			IJ.error("Images must both be of the same bit depth");
			return;			
		}

		float minValue = Float.MAX_VALUE;
		float maxValue = Float.MIN_VALUE;

		// If the type of the image is 8 bit, then we don't
		// need to look at minimum and maximum values:

		if( templBitDepth == 8 ) {
			// That's fine...
		} else if( templBitDepth == 16 ) {
			float [] valuesRange = trans.getValuesRange();
			// Find the range of values - this might well
			// just be a 12 bit image....
			minValue = valuesRange[0];
			maxValue = valuesRange[1];
		} else {
			IJ.error("Unsupported bit depth: "+templBitDepth);
			return;
		}

		if (isLabels) {
			trans.measure = new distance.TwoValues(mat1, mat2);
			if(verbose)
				VIB.println("working on materials " + mat1 + " "
					+ mat2);
		} else {
			int measureIndex = gd.getNextChoiceIndex();
			if (measureIndex == 1) {
				if( templBitDepth == 8 )
					trans.measure =
						new distance.MutualInformation();
				else if( templBitDepth == 16 )
					trans.measure =
						new distance.MutualInformation(minValue,maxValue,256);
			} else if (measureIndex == 2)
				trans.measure =
					new distance.Thresholded(55);
			else if (measureIndex == 3)
				trans.measure =
					new distance.Thresholded(155);
			else if (measureIndex == 4)
				trans.measure =
					new distance.Correlation();
			else
				trans.measure =
					new distance.Euclidean();
                        for( int i = 0; i < otherImages.size(); ++i )
                            if( gd.getNextBoolean() )
                                alsoTransform.add(otherImages.get(i));
		}                
 
		FastMatrix matrix = rigidRegistration(trans, materialBBox, 
					initial, mat1, mat2, noOptimization, 
					level, stopLevel, tolerance, 
					nInitialPositions, showTransformed, 
					showDifferenceImage, fastButInaccurate,
                                        alsoTransform);

		if (!Interpreter.isBatchMode() && verbose)
			WindowManager.setWindow(new TextWindow("Matrix",
						matrix.toStringForAmira(),
						550, 150));

		lastResult = matrix;
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C | DOES_16 | NO_CHANGES;
	}

	void getMaterials2() {
		Choice templateChoice = (Choice)gd.getChoices().get(1);
		int index = templateChoice.getSelectedIndex();
		if (index < 0)
			index = 0;
		String template = templateChoice.getItem(index);
		ImagePlus t = WindowManager.getImage(template);
		AmiraParameters params = new AmiraParameters(t);
		materials2 = params.getMaterialList();

		Choice mat2 = (Choice)gd.getChoices().get(2);
		String chosen = (mat2 == null ? null :
				mat2.getItem(mat2.getSelectedIndex()));

		mat2.removeAll();
		int selectedIndex = 0;
		for (int i = 0; i < materials2.length; i++) {
			mat2.addItem(materials2[i]);
			if (chosen != null && materials2[i].equals(chosen))
				selectedIndex = i;
		}

		if (chosen == null)
			adjustMaterial(false);
		else
			mat2.select(selectedIndex);
	}

	void adjustMaterial(boolean fromTemplate) {
		Choice c1 = (Choice)gd.getChoices().get(0);
		Choice c2 = (Choice)gd.getChoices().get(2);
		if (fromTemplate) {
			Choice c3 = c1; c1 = c2; c2 = c3;
		}

		int index = c1.getSelectedIndex();
		String chosen = c1.getItem(index);
		for (int i = 0; i < c2.getItemCount(); i++)
			if (chosen.equals(c2.getItem(i))) {
				c2.select(i);
				return;
			}

		if (c2.getSelectedIndex() < 0)
			c2.select(0);
	}
}
