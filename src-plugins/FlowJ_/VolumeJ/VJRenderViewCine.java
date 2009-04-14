package VolumeJ;
import java.awt.*;
import ij.*;
import ij.process.*;
import ij.io.*;
import volume.*;

/**
 * This subclass of VJRenderView implements cine mode rendering.
 * Subclasses of VJRenderer need to implement a new instantiation method, and
 * a new prepareNext().
 * If the total cine renderings are smaller than 32Mb, they are shown on screen as a stack,
 * and backed-up in the ImageJ directory under the name "VolumeJ_Cine.tif".
 * If the total cine renderings would be larger than 32 Mb, each rendering is
 * separately saved in the ImageJ directory under the name "VolumeJ_Cine_XXXX.tif".
 *
 * Copyright (c) 2001-2002, Michael Abramoff. All rights reserved.
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
public class VJRenderViewCine extends VJRenderView
{
	/** Total number of view to be rendered (in a stack) */
	protected int                   n;
	/** The rotation steps to go to the next view. */
	protected double                stepx, stepy, stepz;
	/** Whether the cine rendering will be written to disk only or also displayed. **/
	protected boolean               toDisk;

	/**
	 * Instantiates a new rendering shell: a wrapper for a VJRenderer to allow
	 * cine (multiple rotated views) renderings.
	 * @param renderer a VJRenderer
	 * @param scale the amount by which to scale the volume
	 * @param rotx, roty, rotz the amount by which to rotate the amount in that order.
	 * @param message a useful message to identify the characteristics of this rendering.
	 */
	public VJRenderViewCine(VJRenderer renderer,
		double scale, double rotx, double roty, double rotz, String message, int n, boolean toDisk)
	{
		super(renderer, scale, rotx, roty, rotz, message);
		this.n = n;
		this.toDisk = toDisk;
	}
	/**
	 * The top level rendering thread handler.
	 * Takes care of rendering the cine series and
	 * updating the transformation matrix after each cine step.
	 * It saves intermediate renderings to a file in the current directory.
	*/
	public synchronized void run()
	{
		computeTransformationMatrix();
		renderer.setTransformation(m, mLight);
		running = true;
		// Setup the renderer thread for the first image.
		renderer.setMessage("1/"+n);

		// Get a viewport. Cannot use default because will rotate.
		int [] vp = VJViewspaceUtil.suggestCineViewport(renderer.getVolume(), m, stepx, stepy, stepz, n);
		renderer.setViewport(vp[0], vp[1]);

		// Calculate how much memory the cine stack will need.
		long bytesNeeded = n * (long) vp[0] * (long) vp[1];
		// If too large, save each rendering to disk separately, otherwise make a stack.
		if (toDisk)
			IJ.showMessage("VolumeJ",
				"Renderings will not be shown but only written to the ImageJ directory as separate VolumeJ_Cine1xxxx.tif image files.");
		ImagePlus imp = null;
		ImageStack rs = null;
		float ms = 0;
		// Create the views in a stack rs.
		for (int j = 0; j < n && running; j++)
		{
			// Render the view.
			renderer.setDescription(""+j+"/"+n);
			renderer.setSequenceNumber(j);
			ImageProcessor ip = renderToImageProcessor();
			ms += renderer.getTimePerPixel();
			// Process the rendered image.
			if (ip instanceof ImageProcessor)
			{
				if (! toDisk)
				{
					// Update the stack and show in window.
					if (j == 0)
					{
						rs = new ImageStack(ip.getWidth(), ip.getHeight());
						rs.addSlice(""+j, ip);
						imp =  new ImagePlus(message, rs);
						imp.show();
					}
					else
					{
						// Add the view as the last slice.
						//VJUserInterface.write("adding rendering as last slice.");
						rs.addSlice(""+j, ip);
						imp.setStack(null, rs);
						// Show the last slice
						imp.setSlice(j+1);
					}
				}
				else
				{
					// Save the rendering to disk as separate images,
					// do not make stack, do not show.
					imp = new ImagePlus(message, ip);
					FileSaver fs = new FileSaver(imp);
					fs.saveAsTiff("VolumeJ_Cine_"+(10000+j)+".tif");
				}
			}
			// Go to the next rendering.
			nextView(j);
		}
		float averagems = ms / n;
		VJUserInterface.write(""+averagems+" ms/pixel.");

		// Try  garbage collection.
		System.gc();
		IJ.showStatus("memory use "+memoryInUse());
	}
	/**
	* Prepare for the next rendering in a cine rendering.
	* In this case, rotate the transformation matrix (i.e. the volume) by
	* the step rotation amounts in x,y,z.
	* Overload as wanted to implement other types of cine rendering.
	* @param k the number of the current (i.e. before the next) rendering.
	*/
	protected void nextView(int k)
	{
		// Rotate the volume (i.e. m) to the next position
		VJMatrix mm = new VJMatrix();
		mm.rotatex(stepx);
		m.mul(mm);
		mm = new VJMatrix();
		mm.rotatey(stepy);
		m.mul(mm);
		mm = new VJMatrix();
		mm.rotatez(stepz);
		m.mul(mm);
		// Setup the renderer transformation matrix with the same size and position viewport.
		// This also moves the light.
		renderer.setTransformation(m);
		renderer.setMessage(""+(k+1)+"/"+n+" used "+(memoryInUse()/1000)+"Kb ");
		if (IJ.debugMode) VJUserInterface.write("VJRenderViewCine "+k+" rotated: x="+stepx+" y="+stepy+" z="+stepz);
	}
	/**
	 * Set the rotation steps around the x,y and z axes (per cine step).
	 * @param stepx rotation in degrees around x-axis.
	 * @param stepy rotation in degrees around y-axis.
	 * @param stepz rotation in degrees around z-axis.
	 */
	public void setRotationSteps(float stepx, float stepy, float stepz)
	{
		// Angles in degrees per view.
		this.stepx = stepx; this.stepy = stepy; this.stepz = stepz;
	}
}

