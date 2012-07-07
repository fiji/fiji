package vib;

import amira.AmiraParameters;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import math3d.Point3d;

public class AugmentedLandmarkWarp_ implements PlugInFilter {
	ImagePlus image;
	InterpolatedImage ii;

	InterpolatedImage model;
	
	int labelCount;
	FastMatrix[] matrix;
	Point3d[] center;

	public void run(ImageProcessor ip) {
		try {
			
			GenericDialog gd = new GenericDialog("Transform Parameters");
			gd.addStringField("Center of materials or label image", "", 15);
			if (!AmiraParameters.addAmiraMeshList(gd, "Model"))
				return;
			gd.addStringField("LabelTransformationList","1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1");
			gd.showDialog();
			if (gd.wasCanceled())
				return;

			ii = new InterpolatedImage(image);
			model = new InterpolatedImage(
					WindowManager.getImage(gd.getNextChoice()));
			String labelString = gd.getNextString();
			matrix = FastMatrix.parseMatrices(gd.getNextString());
		
			ImagePlus labelImp = WindowManager.getImage(labelString);
			if(labelImp != null){
				InterpolatedImage labels = new InterpolatedImage(labelImp);
				initCentersFromLabelField(labels);
			} else {
				initCentersFromString(labelString);
			}
			run();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void setCenter(Point3d[] center){
		this.center = center;
		labelCount = center.length;
		adjustMatricesToPixelCoordinates();
	}
	
	void initCentersFromString(String s){
		center = Point3d.parsePoints(s);
		if(center.length != matrix.length)
			IJ.error("Number of center points and matrices must agree");
		labelCount = center.length;

		adjustMatricesToPixelCoordinates();
	}

	void initCentersFromLabelField(InterpolatedImage labels) {
		// find centers
		center = new Point3d[256];
		for(int i=0;i<center.length;i++)
			center[i] = new Point3d();
		long[] volumes = new long[256];
		InterpolatedImage.Iterator iter = labels.iterator(false);
		while (iter.next() != null) {
			int value = labels.getNoInterpol(iter.i,
					iter.j, iter.k);
			center[value].x += iter.i;
			center[value].y += iter.j;
			center[value].z += iter.k;
			volumes[value]++;
		}
		for (labelCount = 256; labelCount > 1 &&
				volumes[labelCount - 1] == 0; labelCount--);
		for (int i = 0; i < labelCount; i++)
			if (volumes[i] > 0){
				center[i].x /= volumes[i];
				center[i].y /= volumes[i];
				center[i].z /= volumes[i];
			} else
				matrix[i] = null;
		
		adjustMatricesToPixelCoordinates();
	}
	
	private void adjustMatricesToPixelCoordinates(){
		FastMatrix fromTemplate = FastMatrix.fromCalibration(ii.image);
		FastMatrix toModel = FastMatrix.fromCalibration(model.image).inverse();

		for (int i = 1; i < matrix.length; i++)
			if (matrix[i] != null)
				matrix[i] = toModel.times(matrix[i].inverse().times(fromTemplate));
				//matrix[i] = fromTemplate.inverse().times(matrix[i].inverse().times(toModel.inverse()));
	
	}

	private float x, y, z;

	void transCoord(int i, int j, int k) {
		float total = 0.0f;
		x = y = z = 0;
		for (int l = 0; l < labelCount; l++) {
			if (matrix[l] == null)
				continue;
			
			matrix[l].apply(i, j, k);
			double x = matrix[l].x;
			double y = matrix[l].y;
			double z = matrix[l].z;

			int xdiff = (int)Math.round(center[l].x - i);
			int ydiff = (int)Math.round(center[l].y - j);
			int zdiff = (int)Math.round(center[l].z - k);
			
			float factor = xdiff * xdiff + ydiff * ydiff + zdiff * zdiff;
			factor = 1.0f / (factor + 0.01f);
			
			this.x += x * factor;
			this.y += y * factor;
			this.z += z * factor;
			
			total += factor;
		}
		x /= total;
		y /= total;
		z /= total;
//		System.err.println("result: " + x + ", " + y + ", " + z);
	}

	void run() {
		InterpolatedImage.Iterator iter = ii.iterator(true);
		while (iter.next() != null) {
			transCoord(iter.i, iter.j, iter.k);
			ii.set(iter.i, iter.j, iter.k, (int)model.interpol.get(x, y, z));
		}
		new AmiraParameters(model.image).setParameters(ii.image, false);
		ii.image.updateAndDraw();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C;
	}

}
