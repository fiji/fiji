/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.transforms;

public interface Transform {
	
	static int BOOKSTEIN = 0;
	static int FASTMATRIX = 1;
	
	public Transform inverse();
	
	public int getTransformType();
	
	public void apply(double x,double y,double z,double[] result);
	
	public String toStringIndented(String indent);
	
	public boolean isIdentity();
	
	public Transform composeWith(Transform next);
	
}
