/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
 * This is an adapter subclass to make the Bookstein class implement
 * Transform.
 */

package vib.transforms;

import math3d.Bookstein;
import math3d.Point3d;

public class BooksteinTransform extends Bookstein implements Transform {
	
	protected Point3d[] originalPoints;
	protected Point3d[] transformedPoints;
	
	public BooksteinTransform inverse() {
		return new BooksteinTransform(transformedPoints,originalPoints);
	}
	
	public Transform composeWith(Transform t) {
		return null;
	}
	
	public boolean isIdentity() {
		return false;
	}
	
	public int getTransformType() {
		return Transform.BOOKSTEIN;
	}
	
	public String toStringIndented(String indent) {
		return indent+"FIXME: implement this...\n";
	}
	
	public BooksteinTransform(Point3d[] orig, Point3d[] trans) {
		super(orig,trans);
		originalPoints=orig;
		transformedPoints=trans;
	}
	
	public void apply(double x,double y,double z,double[] result) {
		Point3d p=new Point3d(x,y,z);
		result[0] = bx.evalInit(p);
		result[1] = by.evalInit(p);
		result[2] = bz.evalInit(p);
		for (int i = 0; i < points.length; i++) {
			double u = U(p.distanceTo(points[i]));
			result[0] += bx.w[i] * u;
			result[1] += by.w[i] * u;
			result[2] += bz.w[i] * u;
		}
	}
	
	public void apply(double x,double y,double z) {
		apply(new Point3d(x,y,z));
	}
	
}
