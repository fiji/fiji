package vib;

import process3d.*;
import amira.*;
import distance.Euclidean;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import java.util.Vector;
import java.awt.Choice;
import math3d.Point3d;

public class Raster_Rigid implements PlugIn {

	ImagePlus templ, model, warped;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Raster Rigid Registration");
		int[] wIDs = WindowManager.getIDList();
		if(wIDs == null){
			IJ.error("No images open");
			return;
		}
		String[] titles = new String[wIDs.length];
		for(int i=0;i<wIDs.length;i++)
			titles[i] = WindowManager.getImage(wIDs[i]).getTitle();

		gd.addChoice("Template", titles, titles[0]);
		gd.addChoice("Model", titles, titles[0]);

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		templ = WindowManager.getImage(gd.getNextChoice());
		model = WindowManager.getImage(gd.getNextChoice());
		register();
		warp(model);
		warped.show();
	}

	private static final int INIT_WIDTH = 512;
	private static final double TOLERANCE = 1.0;
	private static final float SMOOTH = 4;
	private static final int GRID_W = 3;
	private static final int GRID_H = 3;
	private static final int GRID_D = 2;

	private static final int STARTL = 3;
	private static final int STOPL = 1;

	private boolean verbose = false;
	private Vector transformations;
	private Vector centers;
	private FastMatrix globalRigid;

	public void register() {
		/*
		 * Preprocess images
		 */
		ImagePlus t = preprocess(templ);
		ImagePlus m = preprocess(model);

		/*
		 * Roughly align via rigid registration
		 */
		TransformedImage trans = new TransformedImage(t, m);
		trans.measure = new distance.Euclidean();

		globalRigid = new RigidRegistration()
			.rigidRegistration(trans, "", "", -1, -1,
				false, STARTL + 1, STOPL + 1, TOLERANCE,
				1, false, false, false, null);

		transformations = new Vector();
		centers = new Vector();
		int w = templ.getWidth(), h = templ.getHeight();
		int d = templ.getStackSize();

		int tw = w / GRID_W, th = h / GRID_H, td = d / GRID_D;

		for(int iz = 0; iz < GRID_D + GRID_D - 1; iz++) {
			int z = iz * (td / 2);
			if(z + GRID_D > d) z = d - GRID_D;
			for(int iy = 0; iy < GRID_H + GRID_H - 1; iy++) {
				int y = iy * (th / 2);
				if(y + GRID_H > h) y = h - GRID_H;
				for(int ix = 0; ix < GRID_W + GRID_W-1; ix++) {
					int x = ix * (tw / 2);
					if(x + GRID_W > w) x = w - GRID_W;
trans.narrowBBox(x, x+tw, y, y+th, z, z+td);
// stop condition
float meanV = getMeanValue(trans.orig.image, x, y, z, tw, th, td);
// only use transformation if the image is not black
if(meanV <= 10.0)
	continue;
FastMatrix fm = new RigidRegistration().rigidRegistration(
	trans, "", globalRigid.toString(), -1, -1, false, 
	STARTL, STARTL, TOLERANCE, 1, false, false, false, null);
fm = fm.times(globalRigid);
transformations.add(fm);
centers.add(new Point3d(x + tw/2, y + th/2, z + td/2));
if(verbose) {
	trans.setTransformation(fm);
	ImagePlus tmp = trans.getTransformed();
	tmp.setTitle("transformed_" + ix + "_" + iy + "_" + iz);
	tmp.show();
}
				}
			}
		}
	}

	private ImagePlus preprocess(ImagePlus img) {
		int rx = Math.round(img.getWidth() / INIT_WIDTH);
		int ry = Math.round(img.getHeight() / INIT_WIDTH);
		int rz = Math.round(2 * img.getStackSize() / INIT_WIDTH);
		if(rx == 0) rx = 1;
		if(ry == 0) ry = 1;
		if(rz == 0) rz = 1;

		if(rx != 1 || ry != 1 || rz != 1)
			img = NaiveResampler.resample(img, rx, ry, rz);

		img = Smooth_.smooth(img, true, SMOOTH, true);
		img = Gradient_.calculateGrad(img, true);
		img = Rebin_.rebin(img, 256);

		return img;
	}

	private void warp(ImagePlus model) {
		AugmentedLandmarkWarp_ aw = new AugmentedLandmarkWarp_();
		aw.ii = new InterpolatedImage(templ).cloneDimensionsOnly();
		aw.model = new InterpolatedImage(model);
		FastMatrix[] fm = new FastMatrix[transformations.size()];
		transformations.toArray(fm);
		aw.matrix = fm;
		Point3d[] cen = new Point3d[centers.size()];
		centers.toArray(cen);
		aw.setCenter(cen);
		aw.run();
		aw.ii.image.setTitle(model + "_warped");
		warped = aw.ii.image;
	}

	private float getMeanValue(ImagePlus image, int x, int y, int z,
							int w, int h, int d) {
		float sum = 0, count = 0;
		int width = image.getWidth();
		for(int zi = z; zi < z + d; zi++) {
			byte[] p = (byte[])image.getStack().getPixels(zi+1);
			for(int yi = y; yi < y + h; yi++) {
				for(int xi = x; xi < x + h; xi++) {
					sum += (int)(p[yi*width + xi] & 0xff);
					count++;
				}
			}
		}
		return sum / count;
	}
}
