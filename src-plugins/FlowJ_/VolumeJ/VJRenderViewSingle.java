package VolumeJ;
import java.awt.*;
import ij.*;
import ij.process.*;
import ij.io.*;
import volume.*;

/**
 * VJRenderView class implements a shell for viewing the one or more rendering of a volume
 * with a given renderer. It keeps the state of the current rendering view.
 * It also serves as an interface to VJUserInterface and ImageJ.
 * It manages the transformation matrix and provides entrypoints for it.
 * This class can be subclassed to generate single, stereo or cine rendering views.
 * These subclasses of VJRenderView have to implement the method run(), which does nothin g in the base class.
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
public class VJRenderViewSingle extends VJRenderView
{
        /**
         * Instantiates a new rendering shell.
         * The renderer is set.
         * The transformation matrix is created to the correct rotation and scaling, and saved in the renderer.
         * A default viewport is created (just width and height and offsets parameter)
         * The volume is centered at the center of the viewport.
         * @param renderer a VJRenderer
         * @param scale the amount by which to scale the volume
         * @param rotx, roty, rotz the amount by which to rotate the volume in that order.
         * @param message a useful message to identify the characteristics of this rendering.
         */
        public VJRenderViewSingle(VJRenderer renderer,
                double scale, double rotx, double roty, double rotz, String message)
        {
                super(renderer, scale, rotx, roty, rotz, message);
        }
        /**
         * The top level rendering thread handler. This method manages the
         * VJRenderer instance.
	*/
        public synchronized void run()
        {
		if (IJ.debugMode) VJUserInterface.write("starting VJRenderViewSingle");
		computeTransformationMatrix();
                renderer.setTransformation(m, mLight);
                long start = System.currentTimeMillis();
                // Set a default viewport.
                renderer.defaultViewport();
                // Render a single view.
                ImageProcessor ip = renderToImageProcessor();
                // Show the rendering in a window.
                if (ip instanceof ImageProcessor)
                {
                        // Make an ImagePlus out of it.
                        ImagePlus imp = new ImagePlus(message, ip);
                        imp.show();
                }
                long elapsedTime = System.currentTimeMillis() - start;
                report(elapsedTime);

                // Try  garbage collection.
                IJ.showStatus("gc...");
                System.gc();
                IJ.showStatus("");
        }
}

