package vib;

import amira.AmiraParameters;

import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;

public class Relabel_ implements PlugInFilter, ActionListener {
	ImagePlus image;

	GenericDialog gd;
	String[] materialList;

	public void run(ImageProcessor ip) {
		if (!AmiraParameters.isAmiraLabelfield(image)) {
			IJ.error("No labelfield!");
			return;
		}

		AmiraParameters params = new AmiraParameters(image);
		materialList = params.getMaterialList();

		gd = new GenericDialog("Relabel Parameters");
		for (int i = 0; i < materialList.length; i++)
			gd.addChoice(materialList[i] + ":", materialList,
					materialList[i]);
		Button switchSides = new Button("Switch sides");
		switchSides.addActionListener(this);
		Panel panel = new Panel();
		panel.add(switchSides);
		gd.addPanel(panel);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int[] mapping = new int[materialList.length];
		for (int i = 0; i < materialList.length; i++)
			mapping[i] = gd.getNextChoiceIndex();

		InterpolatedImage ii = new InterpolatedImage(image);
		for (int k = 0; k < ii.d; k++) {
			for (int j = 0; j < ii.h; j++)
				for (int i = 0; i < ii.w; i++) {
					int v = ii.getNoInterpol(i, j, k);
					ii.set(i, j, k, mapping[v]);
				}
			IJ.showProgress(k + 1, ii.d);
		}
		image.updateAndDraw();
	}

	public int getOppositeMaterial(int index) {
		String m = materialList[index];
		String to;
		if (m.endsWith("_r"))
			to = "l";
		else if (m.endsWith("_l"))
			to = "r";
		else
			return -1;

		to = m.substring(0, m.length() - 1) + to;

		for (int i = 0; i < materialList.length; i++)
			if (materialList[i].equals(to))
				return i;

		return -1;
	}

	public void actionPerformed(ActionEvent event) {
		// switch sides
		Vector v = gd.getChoices();
		for (int i = 0; i < materialList.length; i++) {
			Choice c = (Choice)v.get(i);
			int oppositeIndex = getOppositeMaterial(
					c.getSelectedIndex());
			if (oppositeIndex >= 0)
				c.select(oppositeIndex);
		}
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8C | DOES_8G;
	}
}

