package VolumeJ;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import ij.gui.*;
import ij.*;
/**
 * This class is an interactive rendering viewer.
 * The GUI allows the rendering to be scaled and rotated using the mouse.
 *
 * Copyright (c) 2001-2003, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Inspired by SliceViewer, a Java program written in 1998 by Orion Lawlor (fsosl@uaf.edu),
 * available at http://charm.cs.uiuc.edu/users/olawlor/
 *
 * Note: this is not open source software!
 * These algorithms, source code, documentation or any derived programs ('the software')
 * are the intellectual property of Michael Abramoff.
 * Michael Abramoff asserts his right as the sole owner of the rights
 * to this software.
 * You and/or any person(s) acting with or for you may not:
 * - directly or indirectly copy, sell, lease, rent, license,
 * sublicense, redistribute, lend, give, transfer or otherwise distribute or
 * use the software
 * - modify, translate, or create derivative works from the software, assign or
 * otherwise transfer rights to the Software or use the Software for timesharing
 * or service bureau purposes
 * - reverse engineer, decompile, disassemble or otherwise attempt to discover the
 * source code or underlying ideas or algorithms of the Software or any subsequent
 * version thereof or any part thereof.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class VJViewerCanvas extends ImageCanvas
{
	// Maximum mouse motion in pixels that leads to fine adjustments. */
	protected static final int FINETHRESHOLD = 5;
	/** Mouse motion (in pixels) that stands for a single radian in rotation. */
	protected static final float PIXELSDEGREES = 1;
	/** The rendering viewer */
        protected VJRenderViewInteractive renderView;
	/** Some practical variables */
	protected long clickTime;
        protected boolean mouseIsDown = false;
        protected int startMouseX, startMouseY;

        /**
         * Create a new VJViewerCanvas for imp.
         * @param imp the ImagePlus for which this canvas will be created.
         */
        public VJViewerCanvas(ImagePlus imp)
        {
                super(imp);
        }
	/**
	 * Set the VJRenderViewInteractive instance to process actions.
	 * This Instance is not yet available at the creation of this canvas.
	 */
	public void setRenderView(VJRenderViewInteractive renderView)
	{ this.renderView = renderView; }
	/**
	 * User pressed mouse button, moved mouse, and released.
	 * Change the view and start rendering.
	 */
	public void effectDragging(int dx, int dy)
	{
		float roty = distanceToAngle(dy);
		float rotx = distanceToAngle(dx);
		if (! (renderView instanceof VJRenderViewInteractive))
		        IJ.write("VJViewerCanvas error: renderView not initialize");
		// X-axis in volume is parellel to y-axis on viewing plane and vv.
		renderView.rotateInPlane(roty, -rotx);
	}
	/**
	 * Compute the angle corresponding to a mouse motion (in pixels) dx.
	 * Correct so that small movements result in tiny rotation for fine control.
	 * @param dx the amount of pixels the mouse has moved.
	 * @return the angle in degrees corresponding to dx.
	 */
	protected float distanceToAngle(int dx)
	{
		if (Math.abs(dx) >= FINETHRESHOLD)
			return dx * PIXELSDEGREES;
		else
		{
			float corrected = dx * dx / (FINETHRESHOLD * PIXELSDEGREES);
			if (dx < 0)
				return -corrected;
			else return corrected;
		}
	}
        /**
         * Process a mouse button down event.
         * @param e the MouseEvent with the parameters.
         */
        public void mousePressed(MouseEvent e)
        {
                int x = e.getX();
                int y = e.getY();
		mouseIsDown = true;
		startMouseX = x;
		startMouseY = y;
		clickTime = System.currentTimeMillis();
        }
        /**
         * Process a mouse button up event.
         * This is where the rendering will be redrawn
         * @param e the MouseEvent with the parameters.
         */
        public void mouseReleased(MouseEvent e)
        {
                int x = e.getX();
                int y = e.getY();
                if (mouseIsDown)
                {
                        if ((x != startMouseX)||(y != startMouseY))
                        {
                                // Move the trackball position.
                                int deltax = x - startMouseX;
                                int deltay = y - startMouseY;
				effectDragging(deltax, deltay);
                        }
                }
		mouseIsDown=false;
        }
}
