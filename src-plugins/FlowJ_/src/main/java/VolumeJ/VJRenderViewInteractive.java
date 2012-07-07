package VolumeJ;
import java.awt.*;
import ij.*;
import ij.process.*;
import ij.io.*;
import volume.*;

/**
 * VJRenderViewInteractive implements a shell for interactively modifying the view (by rotating etc).
 * It adds methods to change the current rotation and scaling, and can be controlled from another class.
 * To start the first rendering, call <code>start</code>
 * To change the view, call <code>rotateInPlane</code>
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
public class VJRenderViewInteractive extends VJRenderView
{
	// Where the output ImageProcessor goes.
	protected ImagePlus     imp;

        /**
         * Instantiates a new interactive render viewer.
         * The renderer is set.
         * The transformation matrix is created to the correct rotation and scaling, and saved in the renderer.
         * A default viewport is created (just width and height and offsets parameter)
         * The volume is centered at the center of the viewport.
         * @param renderer a VJRenderer
         * @param scale the amount by which to scale the volume
         * @param message a useful message to identify the characteristics of this rendering.
         */
        public VJRenderViewInteractive(ImagePlus imp, VJRenderer renderer, double scale, String message)
        {
                super(renderer, message);
		this.scale = (float) scale;
		this.imp = imp;
                // Set the viewport to the size of the imp.
                renderer.setViewport(imp.getWidth(), imp.getHeight());
		VJViewerCanvas canvas = (VJViewerCanvas) (imp.getWindow().getCanvas());
		// Be sure that canvas knows where to ask for new views.
		canvas.setRenderView(this);
		computeTransformationMatrix();
        }
	/**
	 * Rotate the transformation matrix by viewing plane rotation angles rotx, roty.
	 * @param anglex angle in degrees in x direction.
	 * @param angley angle in degrees in y direction.
	 */
	public void rotateInPlane(float anglex, float angley)
	{
                VJMatrix mm = new VJMatrix();
                mm.rotatex(anglex);
                m.mul(mm);
                mLight.mul(mm);
                mm = new VJMatrix();
                mm.rotatey(angley);
                m.mul(mm);
                IJ.write("rotated by: "+anglex+", "+angley+" starting rendering now...");
		newView();
	}
        /**
         * Renders a view.
	*/
        public void newView()
        {
                if (! running)
		        IJ.write("newView not initialized!");
		renderer.setTransformation(m, mLight);
                long start = System.currentTimeMillis();
                // Render a single view.
                ImageProcessor ip = renderToImageProcessor();
                // Show the rendering in the preset imp.
                imp.setProcessor(message, ip);
                long elapsedTime = System.currentTimeMillis() - start;
                report(elapsedTime);
        }
        /**
         * Generates the first view and stays on.
	*/
        public synchronized void run()
        {
                running = true;
		this.setPriority(this.MIN_PRIORITY);
		newView();
		while (running);
        }
}

