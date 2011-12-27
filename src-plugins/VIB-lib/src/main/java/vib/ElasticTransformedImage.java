package vib;

import ij.IJ;
import ij.ImagePlus;
import math3d.*;

public class ElasticTransformedImage {
	InterpolatedImage orig, trans;
	FastMatrix fromOrig, toTrans;
	Point3d[] origPoints, transPoints;
	distance.PixelPairs measure;

	public ElasticTransformedImage(InterpolatedImage orig,
			InterpolatedImage trans, Point3d[] origPoints,
			FastMatrix initialTransform) {
		this(orig, trans, origPoints,
				transformPoints(origPoints, initialTransform));
	}

	public ElasticTransformedImage(InterpolatedImage orig,
			InterpolatedImage trans, Point3d[] origPoints,
			Point3d[] transPoints) {
		this.orig = orig;
		this.trans = trans;
		fromOrig = FastMatrix.fromCalibration(orig.image);
		toTrans = FastMatrix.fromCalibration(trans.image).inverse();

		this.origPoints = origPoints;
		this.transPoints = transPoints;

		b = new Bookstein(origPoints, transPoints);
	}

	public static Point3d[] transformPoints(Point3d[] origPoints,
			FastMatrix matrix) {
		Point3d[] transPoints = new Point3d[origPoints.length];
		for (int i = 0; i < origPoints.length; i++) {
			matrix.apply(origPoints[i]);
			transPoints[i] = matrix.getResult();
		}
		return transPoints;
	}

	Bookstein b;

	double x, y, z;
	void apply(Point3d p) {
		b.apply(p);
		x = b.x;
		y = b.y;
		z = b.z;
	}

	void apply(int i, int j, int k) {
//System.err.print("pixel coords " + i + ", " + j + ", " + k);
		fromOrig.apply(i, j, k);
//System.err.print(" are real coordinates " + fromOrig.getResult());
		apply(fromOrig.getResult());
//System.err.print(", become after transformation " + x + ", " + y + ", " + z);
		toTrans.apply(x, y, z);
		this.x = toTrans.x;
		this.y = toTrans.y;
		this.z = toTrans.z;
//System.err.println(" and then " + this.x + ", " + this.y + ", " + this.z);
	}

	InterpolatedImage getTransformed() {
		InterpolatedImage res = orig.cloneDimensionsOnly();
		for (int k = 0; k < res.d; k++) {
			for (int j = 0; j < res.h; j++)
				for (int i = 0; i < res.w; i++) {
					apply(i, j, k);
					double v = trans.interpol.get(x, y, z);
					//double v = ((((int)x + (int)y + (int)z) & 8) > 0 ? 255 : 0);
					res.set(i, j, k, (byte)v);
				}
			IJ.showProgress(k + 1, res.d);
		}
		return res;
	}

	double getDistance() {
		measure.reset();
		for (int k = 0; k < orig.d; k++)
			for (int j = 0; j < orig.h; j++)
				for (int i = 0; i < orig.w; i++) {
					double v0 = orig.getNoInterpol(i, j, k);
					apply(i, j, k);
					double v1 = trans.interpol.get(x, y, z);
					measure.add((float)v0, (float)v1);
				}
		return measure.distance();
	}
}

