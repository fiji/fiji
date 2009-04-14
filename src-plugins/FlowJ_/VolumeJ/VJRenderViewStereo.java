package VolumeJ;
import java.awt.*;
import ij.*;
import ij.process.*;
import ij.io.*;
import volume.*;

/**
 * VJRenderViewStereo is a subclass of VJRenderView that implements the shell
 * for rendering two stereo views for viewing with a stereo viewer.<br>
 *
 * Copyright (c) 2001-2003, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
*/
public class VJRenderViewStereo extends VJRenderView
{
	// difference between two stereo images in degrees.
	public static final double  stereoDifference = 5.0;

	public VJRenderViewStereo(VJRenderer renderer,
		double scale, double xrot, double yrot, double zrot, String message)
	{
		super(renderer, scale, xrot, yrot, zrot, message);
	}
	public synchronized void run()
      	{
		computeTransformationMatrix();
                renderer.setTransformation(m, mLight);
                long start = System.currentTimeMillis();

		// rotate a little for the left eye view.
		VJMatrix mry = new VJMatrix();
		mry.rotatey(-stereoDifference/2.0);
		m.mul(mry);
		// Setup the renderer thread for the left image.
		renderer.setTransformation(m);
                renderer.setMessage("stereo 1/2");
		// Get a viewport.
		int [] vp = renderer.defaultViewport();
		// Render the left eye view.
		ImageProcessor ip = renderToImageProcessor();
		if (ip instanceof ImageProcessor)
		{
			ImagePlus imp = new ImagePlus(message+"(Left Eye)", ip); imp.show();
			IJ.wait(250);  // give system time to redraw ImageJ window
		}
		else
			IJ.showStatus("");
		// Now prepare for the right eye view.
		mry = new VJMatrix();
		mry.rotatey(stereoDifference/2.0);
		m.mul(mry);
		// Setup the renderer thread for the right image.
		renderer.setTransformation(m);
                renderer.setMessage("stereo 2/2");
		renderer.setViewport(vp[0], vp[1]);

		// Render the right eye view.
		ip = renderToImageProcessor();
		if (ip instanceof ImageProcessor)
		{
			ImagePlus imp = new ImagePlus(message+"(Right Eye)", ip); imp.show();
		}
		else
			IJ.showStatus("");

		long elapsedTime = System.currentTimeMillis() - start;
		report(elapsedTime);

		// Try  garbage collection.
		IJ.showStatus("gc...");
		System.gc();
		IJ.showStatus("");
        }
}

