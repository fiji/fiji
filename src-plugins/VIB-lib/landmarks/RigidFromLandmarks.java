/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.*;
import ij.process.*;
import ij.gui.*;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;

import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Comparator;

import landmarks.NamedPointWorld;
import vib.FastMatrix;
import vib.TransformedImage;

import vib.oldregistration.RegistrationAlgorithm;

public class RigidFromLandmarks extends RegistrationAlgorithm {

	boolean allowScaling;

	public ImagePlus register() {

		NamedPointSet points0 = null;
		NamedPointSet points1 = null;

		try {
			points0 = NamedPointSet.forImage(sourceImages[0]);
		} catch( NamedPointSet.PointsFileException e ) {
			IJ.error("No corresponding .points file found "+
				 "for image: \""+sourceImages[0].getTitle()+"\"");
			System.out.println("for 0 in Rigid_From_Landmarks.register()");
			return null;
		}

		try {
			points1 = NamedPointSet.forImage(sourceImages[1]);
		} catch( NamedPointSet.PointsFileException e ) {
			IJ.error("No corresponding .points file found "+
				 "for image: \""+sourceImages[1].getTitle()+"\"");
			System.out.println("for 1 in Rigid_From_Landmarks.register()");
			return null;
		}

		return register( points0, points1 );
	}

	public ImagePlus register( NamedPointSet points0, NamedPointSet points1 ) {

		ArrayList< String > sharedNames = points0.namesSharedWith( points1, true );

		Point3d[] fromPoints = new Point3d[sharedNames.size()];
		Point3d[] toPoints = new Point3d[sharedNames.size()];

		int pointIndex = 0;
		for( String name : sharedNames ) {
			NamedPointWorld npw0 = points0.getPoint(name);
			NamedPointWorld npw1 = points1.getPoint(name);
			toPoints[pointIndex] = npw0.toPoint3d();
			fromPoints[pointIndex] = npw1.toPoint3d();
			++ pointIndex;
		}

		FastMatrix fm = FastMatrix.bestRigid( fromPoints, toPoints, allowScaling );

		TransformedImage ti = new TransformedImage( sourceImages[0], sourceImages[1] );
		ti.setTransformation( fm );

		ImagePlus transformed = ti.getTransformed();
		transformed.setTitle( "Transformed" );
		return transformed;
	}
}

