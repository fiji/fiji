/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import ij.ImagePlus;
import util.BatchOpener;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import vib.FastMatrix;
import math3d.Point3d;

/* These are some very basic (and not very helpful) unit tests for
   FastMatrix - it would be great if people wanted to add to these. */

public class TestFastMatrix {

	Point3d [] fromPoints;
	Point3d [] toPointsRotation;
	Point3d [] toPointsScale;
	FastMatrix realScale;
	FastMatrix silly;

	Point3d [] fromPoints4;
	Point3d [] toPointsRotationScaling;

	FastMatrix realRotation;
	FastMatrix realRotationScaling;

	@Before
	public void setUp() {

		fromPoints = new Point3d[3];
		fromPoints[0] = new Point3d( 1, 0, 2 );
		fromPoints[1] = new Point3d( 0, 0, 5 );
		fromPoints[2] = new Point3d( 0, 3, 5 );

		fromPoints4 = new Point3d[4];
		fromPoints4[0] = new Point3d( 1, 0, 2 );
		fromPoints4[1] = new Point3d( 0, 0, 5 );
		fromPoints4[2] = new Point3d( 0, 3, 5 );
		fromPoints4[3] = new Point3d( 4, 1, 2 );

		toPointsRotation = new Point3d[3];
		toPointsRotation[0] = new Point3d( 2, 1, 0 );
		toPointsRotation[1] = new Point3d( 5, 0, 0 );
		toPointsRotation[2] = new Point3d( 5, 0, 3 );

		toPointsRotationScaling = new Point3d[4];
		toPointsRotationScaling[0] = new Point3d( 4, 2, 0 );
		toPointsRotationScaling[1] = new Point3d( 10, 0, 0 );
		toPointsRotationScaling[2] = new Point3d( 10, 0, 6 );
		toPointsRotationScaling[3] = new Point3d( 4, 8, 2 );

		toPointsScale = new Point3d[3];
		toPointsScale[0] = new Point3d( 3, 0, 6 );
		toPointsScale[1] = new Point3d( 0, 0, 15 );
		toPointsScale[2] = new Point3d( 0, 9, 15 );

		realScale = new FastMatrix( 3 );

		double [][] sillyMatrix = { { 0, 1, 2, 3 },
					    { 4, 5, 6, 7 },
					    { 8, 9, 10, 11 } };
		silly = new FastMatrix( sillyMatrix );

		double [][] realRotationMatrix = { { 0, 0, 1, 0 },
						   { 1, 0, 0, 0 },
						   { 0, 1, 0, 0 } };
		realRotation = new FastMatrix( realRotationMatrix );

		double [][] realRotationScalingMatrix = { { 0, 0, 2, 0 },
							  { 2, 0, 0, 0 },
							  { 0, 2, 0, 0 } };
		realRotationScaling = new FastMatrix( realRotationScalingMatrix );
	}

	@Test
	public void testBestRigid() {

		FastMatrix shouldBeRotation = FastMatrix.bestRigid(
			fromPoints,
			toPointsRotation );

		assertTrue( shouldBeRotation.equals( realRotation ) );

		FastMatrix shouldBeScale = FastMatrix.bestRigid(
			fromPoints,
			toPointsScale );

		assertTrue( shouldBeScale.equals( realScale ) );

		FastMatrix shouldBeRotationScalingBestLinear = FastMatrix.bestLinear(
			fromPoints4,
			toPointsRotationScaling );
		
		assertTrue( shouldBeRotationScalingBestLinear.equals( realRotationScaling ) );

		FastMatrix shouldBeRotationScalingBestRigid = FastMatrix.bestRigid(
			fromPoints4,
			toPointsRotationScaling );

		assertTrue( shouldBeRotationScalingBestRigid.equals( realRotationScaling ) );

	}

	@Test
	public void testEqual() {
		assertTrue( silly.equals( silly ) );
	}
}
