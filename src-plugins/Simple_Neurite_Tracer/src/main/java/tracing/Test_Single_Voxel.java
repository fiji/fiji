/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.*;
import ij.process.*;
import ij.plugin.*;

import ij3d.Image3DUniverse;
import ij3d.Content;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import ij.gui.GUI;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

/* A test for the 3D viewer.  The results are odd at the moment - the
   crossing point of the lines should always appear to be at the
   centre of the voxel, since A Pixel Is Not A Little Square. */

public class Test_Single_Voxel implements PlugIn {
	public void run( String ignore ) {
		ImageStack stack = new ImageStack(3,3);
		for( int i = 0; i < 3; ++i ) {
			byte [] pixels = new byte[9];
			if( i == 1 )
				pixels[4] = (byte)255;
			ByteProcessor bp = new ByteProcessor(3,3);
			bp.setPixels(pixels);
			stack.addSlice("",bp);
		}
		ImagePlus i = new ImagePlus("test",stack);
		i.show();
		Image3DUniverse univ = new Image3DUniverse(512, 512);
		univ.show();
		GUI.center(univ.getWindow());
		boolean [] channels = { true, true, true };
		univ.addContent(i,
				new Color3f(Color.white),
				"Volume Rendering of a Single Voxel at (1,1,1)",
				10, // threshold
				channels, 1, // resampling
				// factor
				Content.VOLUME);
		List<Point3f> linePoints = new ArrayList<Point3f>();
		boolean fudgeCoordinates = false;
		if( fudgeCoordinates ) {
			// You shouldn't need to fudge the coordinates
			// like this to make the cross appear in the
			// centre of the voxel...
			linePoints.add(new Point3f(0.5f,0.5f,1.5f));
			linePoints.add(new Point3f(2.5f,2.5f,1.5f));
			linePoints.add(new Point3f(0.5f,2.5f,1.5f));
			linePoints.add(new Point3f(2.5f,0.5f,1.5f));
		} else {
			linePoints.add(new Point3f(0,0,1));
			linePoints.add(new Point3f(2,2,1));
			linePoints.add(new Point3f(0,2,1));
			linePoints.add(new Point3f(2,0,1));
		}
		univ.addLineMesh( linePoints, new Color3f(Color.red), "Line that cross at (1,1,1)", false );
		univ.resetView();
	}
}
