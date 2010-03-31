package VolumeJ;
import java.awt.*;
import ij.*;
import ij.process.*;
import ij.io.*;
import volume.*;

/**
 * VJRenderView class implements a shell for rendering one or more views of a volume
 * with a given renderer and volume. It keeps the state of the current rendering view.
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
public class VJRenderView extends Thread
{
        /** The renderer */
        protected VJRenderer renderer;
	/** State variables. Rotation around x-axis. */
	protected float rotx;
	/** State variables. Rotation around y-axis. */
	protected float roty;
	/** State variables. Rotation around z-axis. */
	protected float rotz;
	/** State variables. Scaling factor. */
	protected float scale;
	/** State variables. The transformation matrix to be set in the renderer. */
	protected VJMatrix m;
	/** State variables. The transformation matrix for the light. */
	protected VJMatrix mLight;

        // For toString() purposes.
        protected String     		message;
        protected String     		description;

        // User break control.
        protected boolean		running;

        /**
         * Instantiates a new rendering shell.
         * The renderer is set.
         * The volume is centered at the center of the viewport.
         * @param renderer a VJRenderer
         * @param scale the amount by which to scale the volume
         * @param message a useful message to identify the characteristics of this rendering.
         */
        public VJRenderView(VJRenderer renderer, String message)
        {
                super("Render shell");
                if (renderer == null)
                {
                        IJ.error("renderer not initialized!");
                        return;
                }
                this.renderer = renderer;
                this.message = message;
                // Set the description String.
                description = ""+message+" "+renderer.getClassifier().toString();
                description += " scale "+scale+renderer.getShader().toString()+renderer.getInterpolator();
		if (IJ.debugMode) VJUserInterface.write("VJRenderView created");
        }
        /**
         * Instantiates a new rendering shell.
         * The renderer is set.
         * @param renderer a VJRenderer
         * @param scale the amount by which to scale the volume
         * @param rotx, roty, rotz the amount by which to rotate the volume in that order.
         * @param message a useful message to identify the characteristics of this rendering.
         */
        public VJRenderView(VJRenderer renderer,
                double scale, double rotx, double roty, double rotz, String message)
        {
                super("Render shell");
                if (renderer == null)
                {
                        IJ.error("renderer not initialized!");
                        return;
                }
                this.renderer = renderer;
                this.message = message;
		this.scale = (float) scale;
		this.rotx = (float) rotx; this.roty = (float) roty; this.rotz = (float) rotz;
                // Set the description String.
                description = ""+message+" "+renderer.getClassifier().toString();
                description += " scale "+scale+" rot "+rotx+","+roty+","+rotz;
                description += " "+renderer.getShader().toString()+renderer.getInterpolator();
        }
        /**
         * Fully calculate the transformation matrix for a view.
	 * At instantiation of a VJRenderView, the renderer may not fully initialized,
	 * so this method can only be called when actual rendering starts.
         * Scales, centers and rotates the volume and corrects the position of the light.
	 * After calling this method,
	 * the transformation matrix has been scaled, corrected for aspect ratio,
	 * centered relative to the volume center and rotated.
	 * The light transformation matrix has been scaled,
	 * centered relative to the volume center and rotated.
         */
        protected void computeTransformationMatrix()
        {
                // Create a new transformation matrix.
                this.m = new VJMatrix();
                // First step is scaling.
                VJMatrix mm = new VJMatrix();
                mm.scale(scale, scale, scale);
                m.mul(mm);
                // This is the transformation matrix used to determine the light position.
                this.mLight = new VJMatrix(m);
                // Now modify the transformation matrix to account for the aspect ratios.
                VJMatrix mAspects = new VJMatrix();
                mAspects.scale(renderer.getVolume().getAspectx(), renderer.getVolume().getAspecty(),
                        renderer.getVolume().getAspectz());
                m.mul(mAspects);
                // The mLight matrix is not corrected for the aspect ratio.
                // Put the center of the volume at the center of the screen.
                center(m, renderer.getVolume());
                // The light matrix is also centered.
                center(mLight, renderer.getVolume());
                // Only after centering can you rotate around the axes.
                mm = new VJMatrix();
                mm.rotatex(rotx);
                m.mul(mm);
                mLight.mul(mm);
                mm = new VJMatrix();
                mm.rotatey(roty);
                m.mul(mm);
                mLight.mul(mm);
                mm = new VJMatrix();
                mm.rotatez(rotz);
                m.mul(mm);
		renderer.setTransformation(m, mLight);
		if (IJ.debugMode)
                {
                        VJUserInterface.write("VJRenderView initial rotated: x="+rotx+" y="+roty+" z="+rotz);
                }
                description += " aniso "+renderer.getVolume().getAspectx()+"x"+renderer.getVolume().getAspecty()+"x"+renderer.getVolume().getAspectz();
        }
        /**
         * Overload to make functional.
	*/
        public synchronized void run() {}
        /**
         * Render in a separate thread and make an image processor out of the resulting image.
	 * @return an ImageProcessor containing the view.
         */
        protected ImageProcessor renderToImageProcessor()
        {
                ImageProcessor ip = null;
                Object pixels = renderToPixelArray();
                if (pixels instanceof byte [])
                        ip = new ByteProcessor(renderer.getViewportWidth(), renderer.getViewportHeight(),
                                (byte []) pixels, null);
                else if (pixels instanceof int [])
                        ip = new ColorProcessor(renderer.getViewportWidth(), renderer.getViewportHeight(),
                                (int []) pixels);
                if (pixels instanceof float [])
                        ip = new FloatProcessor(renderer.getViewportWidth(), renderer.getViewportHeight(),
                                (float []) pixels, null);
                return ip;
        }
        /**
         * Render in a separate thread and return the pixels with the rendering.
	 * @return an Object with the pixels, null if problems.
         */
        protected Object renderToPixelArray()
        {
                try
                {
                        Thread thread = new Thread(renderer);
	                thread.setPriority(Thread.NORM_PRIORITY-1);
                        // start the rendering thread.
                        thread.start();
                        // wait for renderer to finish.
                        thread.join();
                        return renderer.getPixels();
                }
                catch (Exception e) { VJUserInterface.write("problems!" + e); return null; }
        }
        /**
         * Stops the shell.
         * Also tries to stop the renderer.
         */
        public void kill()
        {
                running = false;
                if (renderer instanceof VJRenderer) renderer.kill();
        }
        /**
         * Center m around 0,0,0 by translating to the center of the volume.
         * @param m an transformation matrix converting object to viewspace coordinates.
         * @param v the volume to be centered.
        */
        protected static void center(VJMatrix m, Volume v)
        {
                VJMatrix mt = new VJMatrix();
                mt.translate(-v.getWidth() / 2, -v.getHeight() / 2, -v.getDepth() / 2);
                m.mul(mt);
        }
        protected void report(long elapsedTime)
        {
                double seconds = elapsedTime / 1000.0;
                long vxs = renderer.getVolume().getWidth() * renderer.getVolume().getHeight() * renderer.getVolume().getDepth();
                VJUserInterface.write(""+IJ.d2s(((double)vxs/seconds), 2)+"voxels/second");
        }
	/**
         * Compute memory usage for rendering.
	 * @return the number of bytes used.
         */
        protected long memoryInUse()
        {
		long freeMem = Runtime.getRuntime().freeMemory();
		long totMem = Runtime.getRuntime().totalMemory();
		return  (totMem-freeMem);
	}
        public String toString()
        {
                return description;
        }
}

