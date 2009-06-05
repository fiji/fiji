package VolumeJ;

import java.awt.*;
import ij.*;
import volume.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.PlugIn;

/**
 * VJBackprojection. Performs inverse Radon transform on sequence of projection images at
 * different angles to obtain the original volume.
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
public class VJBackprojection
{
        // rotation per frame.
        private static float        deltaAngle = 0;
        private static float 		scale=0;
        private static int          n=0;
        private static int          width = 0;
        private static int          height = 0;
        private static int          depth = 0;

        private static double 		aspectx = 1;
        private static double 		aspecty = 1;
        private static double 		aspectz = 1;
        protected VolumeFloat 		v;

        public VJBackprojection()
        {
                ImagePlus imp = WindowManager.getCurrentImage();
                if (imp instanceof ImagePlus && imp.getStackSize() > 1)
                {
                        Calibration c = imp.getCalibration();
                        aspectx = 1;
                        aspecty = 1;
                        aspectz = c.pixelDepth / c.pixelWidth;
                        deltaAngle = 45;
                        width = imp.getStack().getWidth();
                        height = width;
                        depth = imp.getStack().getHeight();
                        n = imp.getStack().getSize();
                        GenericDialog gd = new GenericDialog("Backprojection ("+n+" projections)", IJ.getInstance());
                        gd.addMessage("Allows reconstruction of a volume from any number of projections of that volume, as done for CT or MR scanning.\n"+
                                "Implements the inverse Radon transform. The more projections the better the quality of the reconstruction");
                        gd.addNumericField("Delta angle (between projections)", deltaAngle, 2);
                        gd.addNumericField("width", width, 2);
                        gd.addNumericField("height", height, 2);
                        gd.addNumericField("depth", depth, 2);
                        gd.showDialog();
                        if (gd.wasCanceled())
                                        return;
                        deltaAngle = (float) gd.getNextNumber();
                        width = (int) gd.getNextNumber();
                        height = (int) gd.getNextNumber();
                        depth = (int) gd.getNextNumber();
                        v = new VolumeFloat(width, height, depth, 1, 1, 1);
                        VJProjection vjp = new VJProjection(deltaAngle,
                                imp.getStack().getImageArray(), n, width, height);
                        vjp.backproject(v);
                        (new ImagePlus("backprojection ", ((VolumeFloat) v).getImageStack())).show();
                }
        }
}

