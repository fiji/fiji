import ij.plugin.PlugIn;
import VolumeJ.*;
import volume.Volume;
import ij.*;

/**
 * This class interfaces the VolumeJ package to a ImageJ plugin.
 *
 * Copyright (c) 1999-2002, Michael Abramoff. All rights reserved.
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
public class VolumeJ_ implements PlugIn
{
	public void run(String arg)
	{
	  String macroOptions = Macro.getOptions();
	  if (macroOptions!=null)
	  {
	    int algorithm = Integer.parseInt(Macro.getValue(macroOptions, "algorithm", "0"));
	    int mode = Integer.parseInt(Macro.getValue(macroOptions, "mode", "0"));
	    int interpolation = Integer.parseInt(Macro.getValue(macroOptions, "interpolation", "1"));
	    int lightx = Integer.parseInt(Macro.getValue(macroOptions, "lightx", "0"));
	    int lighty = Integer.parseInt(Macro.getValue(macroOptions, "lighty", "0"));
	    int lightz = Integer.parseInt(Macro.getValue(macroOptions, "lightz", "0"));
	    double aspectx = Double.parseDouble(Macro.getValue(macroOptions, "aspectx", "1"));
	    double aspecty = Double.parseDouble(Macro.getValue(macroOptions, "aspecty", "1"));
	    double aspectz = Double.parseDouble(Macro.getValue(macroOptions, "aspectz", "1"));
	    double scale = Double.parseDouble(Macro.getValue(macroOptions, "scale", "0"));
	    double xrot = Double.parseDouble(Macro.getValue(macroOptions, "xrot", "0"));
	    double yrot = Double.parseDouble(Macro.getValue(macroOptions, "yrot", "0"));
	    double zrot = Double.parseDouble(Macro.getValue(macroOptions, "zrot", "0"));
	    int cine = Integer.parseInt(Macro.getValue(macroOptions, "cine", "20"));
	    int cineN = Integer.parseInt(Macro.getValue(macroOptions, "cineN", "1"));
	    int classification = Integer.parseInt(Macro.getValue(macroOptions, "classification", "0"));
	    double threshold = Double.parseDouble(Macro.getValue(macroOptions, "threshold", "100"));
	    double width = Double.parseDouble(Macro.getValue(macroOptions, "width", "5"));
	    String text = Macro.getValue(macroOptions, "text", "0");
	    int cineToDisk = Integer.parseInt(Macro.getValue(macroOptions, "cineToDisk", "0"));
	    int cineAxis = Integer.parseInt(Macro.getValue(macroOptions, "cineAxis", "0"));
	    run(algorithm, mode, interpolation, lightx, lighty, lightz,
		aspectx, aspecty, aspectz, scale,
		xrot, yrot, zrot, cine, cineN,
		classification, threshold, width,
		text, cineToDisk, cineAxis);

	  }
	  else
	      new VJUserInterface();
	}
	/**
	 * Interface for macro control of volume rendering. Does the same thing as VJUserInterface, but without
	 * a user interface. Straightly sets up a rendering.
	 * @param algorithm int 0 = RAYTRACE< 1 = ISOSURFACE
	 * @param mode int 0 = mono, 1 = stereo, 2 = cine
	 * @param interpolation int 0 = NN, 1 = trilinear
	 * @param lightx int
	 * @param lighty int
	 * @param lightz int
	 * @param aspectx double
	 * @param aspecty double
	 * @param aspectz double
	 * @param scale double
	 * @param xrot double
	 * @param yrot double
	 * @param zrot double
	 * @param cine int = degrees between renderings
	 * @param cineN int = number of cine renderings
	 * @param classification int see VJClassifiers for index. Default = 0.
	 * @param double threshold for Levoy surfaces
	 * @param double width for Levoy surfaces.
	 * @param text a text to show as title of the rendering window.
	 * @param cineToDisk whether or not to save the rendering to disk
	 * @param cineAxis which axis to rotate for cine mode 0 = x, 1=y, 2=z
	 */
	public void run(int algorithm, int mode, int interpolation, int lightx, int lighty, int lightz,
			double aspectx, double aspecty, double aspectz, double scale,
			double xrot, double yrot, double zrot, int cine, int cineN,
			int classification, double threshold, double width,
			String text, int cineToDisk, int cineAxis)
	{
	  try
	  {
	    VJInterpolator interpolator = null;
	    if (interpolation == 1)
	      interpolator = new VJTrilinear();
	    else
	      interpolator = new VJNearestNeighbor();
	    // Check the shader.
	    // Create a white light with 0.9 diffuse light and no specular light.
	    VJLight light = new VJLight( (float) lightx, (float) lighty,
					(float) lightz, (float) 0.9, 0);
	    // Create a shader, with 0.1 background light.
	    VJShader shader = new VJPhongShader( (float) 0.1, light, false);
	    // Check the classifier (and initialize the indexes if needed).
	    VJClassifier classifier = VJClassifiers.getClassifier(
		classification);
	    if (classifier instanceof VJClassifierIsosurface)
	      ( (VJClassifierIsosurface) classifier).setThreshold(threshold);
	    if (classifier instanceof VJClassifierLevoy) {
	      ( (VJClassifierLevoy) classifier).setThreshold(threshold);
	      ( (VJClassifierLevoy) classifier).setWidth(width);
	    }
	    VJRenderer renderer = null;
	    if (algorithm == 0)
	      renderer = new VJRender(interpolator, shader, classifier);
	    else if (algorithm == 1)
	      renderer = new VJIsosurfaceRender(interpolator, shader,
						classifier);
	    if (renderer == null)
	      return;
	    ImagePlus imp = WindowManager.getImage(0);
	    ImagePlus impindex = null;
	    Volume v = VJUserInterface.resetVolume(renderer, imp, impindex,
		aspectx, aspecty, aspectz);
	    renderer.setVolume(v);
	    VJRenderView rs = null;
	    if (mode == 0)
	      rs = new VJRenderViewSingle(renderer, scale, xrot, yrot, zrot,
					  text);
	    else if (mode == 1)
	      rs = new VJRenderViewStereo(renderer, scale, xrot, yrot, zrot,
					  text);
	    else if (mode == 2) {
	      rs = new VJRenderViewCine(renderer, scale, xrot, yrot, zrot,
					text,
					(int) (cineN / cine), cineToDisk == 1);
	      switch (cineAxis) {
		case 0: // x axis.
		  ( (VJRenderViewCine) rs).setRotationSteps(cine, 0, 0);
		  break;
		case 1: // y axis.
		  ( (VJRenderViewCine) rs).setRotationSteps(0, cine, 0);
		  break;
		case 2: // z axis.
		  ( (VJRenderViewCine) rs).setRotationSteps(0, 0, cine);
		  break;
	      }
	    }
	    //Start rendering.
	    rs.start();
	  }
	  catch (Exception e) { IJ.write(e.getMessage()); }
      }
}


