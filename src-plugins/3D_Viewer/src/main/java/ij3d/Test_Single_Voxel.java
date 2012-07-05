/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package ij3d;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;
import ij.measure.Calibration;
import ij.io.*;

import ij3d.Image3DUniverse;
import ij3d.Image3DMenubar;
import ij3d.Content;
import ij3d.Pipe;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import ij.gui.GUI;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

/* A test for the 3D viewer.  The results are odd at the moment - one
   end of the red line should always appear to be at the centre of the
   voxel, since A Pixel Is Not A Little Square. */

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
		Content c = univ.addContent(i,
					    new Color3f(Color.white),
					    "Volume Rendering of a Single Voxel at (1,1,1)",
					    10, // threshold
					    channels,
					    1, // resampling factor
					    Content.VOLUME);
		List<Point3f> linePoints = new ArrayList<Point3f>();
		linePoints.add(new Point3f(1,1,1));
		linePoints.add(new Point3f(2,2,2));
		univ.addLineMesh( linePoints, new Color3f(Color.red), "Line from (1,1,1) to (2,2,2)", false );
		univ.resetView();
	}
}
