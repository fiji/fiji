package vib;

import amira.AmiraParameters;

import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.util.*;
import math3d.Point3d;

public class ElasticTransform_ implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Transform Parameters");
		AmiraParameters.addAmiraMeshList(gd, "imageToTransform");
		gd.addStringField("origPoints", "");
		gd.addStringField("transPoints", "");
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus trans = WindowManager.getImage(gd.getNextChoice());
		Point3d[] origPoints = parsePoints(gd.getNextString());
		Point3d[] transPoints = parsePoints(gd.getNextString());

		ElasticTransformedImage t =
			new ElasticTransformedImage(new InterpolatedImage(image), new InterpolatedImage(trans), origPoints, transPoints);
		t.getTransformed().image.show();
	}

	Point3d[] parsePoints(String s) {
		ArrayList array = new ArrayList();
		StringTokenizer t = new StringTokenizer(s);
		while(t.hasMoreTokens())
			array.add(new Point3d(Double.parseDouble(t.nextToken()),
					Double.parseDouble(t.nextToken()),
					Double.parseDouble(t.nextToken())));
		Point3d[] res = new Point3d[array.size()];
		for (int i = 0; i < res.length; i++)
			res[i] = (Point3d)array.get(i);
		return res;
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C;
	}
}

