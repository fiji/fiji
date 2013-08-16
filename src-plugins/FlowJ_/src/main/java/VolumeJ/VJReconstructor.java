package VolumeJ;

import java.awt.*;
import ij.*;
import volume.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.PlugIn;

/**
 * VJReconstructor reconstructs volumes in
 * an ImageJ stack of any anisotropy along any axis. It can scale and rotate.
 * It can also create hypervolumes (multiple volumes in one stack)
 * by setting the length parameter.
 * Interpolation is trilinear except for the last slice, where trilinear is impossible and
 * nearest neighborhood is used (instead of losing a slice).
 *
 * @author: Michael Abramoff, copyright (c) 1999-2003. All rights reserved.
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
public class VJReconstructor
{
        // rotation per frame.
        private static float 		anglex=0;
        private static float 		angley=0;
        private static float 		anglez=0;
        private static int 		length = 1;
        private static float 		aspectx = 1;
        private static float 		aspecty = 1;
        private static float 		aspectz = 1;
        protected VolumeShort 		v;
        protected VJMatrix 		mi;
        // size of viewport.
        protected int                   width, height, depth;
        // start of rays in viewspace.
        protected int                   ioffset, joffset, koffset;

        public VJReconstructor()
        {
                ImagePlus imp = WindowManager.getCurrentImage();
                if (imp instanceof ImagePlus && imp.getStackSize() > 1)
                {
                        ImageStack isv = imp.getStack();
                        Calibration c = imp.getCalibration();
                        aspectx = 1;
                        aspecty = 1;
                        aspectz = (float) (c.pixelDepth / c.pixelWidth);
                        v = null;
                        try
                        {
                                v = new VolumeShort(isv);
                        } catch (Exception exc) {  }
                        width = v.getWidth();
                        height = v.getHeight();
                        depth = v.getDepth();
                        GenericDialog gd = new GenericDialog("Volume Reconstructor 1.1 ("
                                +width+"x"+height+"x"+depth+")", IJ.getInstance());
                        gd.addNumericField("aspect_x:", aspectx, 2);
                        gd.addNumericField("aspect_y:", aspecty, 2);
                        gd.addNumericField("aspect_z:", aspectz, 2);
                        gd.addNumericField("rotation_x (around x) angle (degrees/frame):", anglex, 1);
                        gd.addNumericField("rotation_y (around y) angle (degrees/frame):", angley, 1);
                        gd.addNumericField("rotation_z (around z) angle (degrees/frame):", anglez, 1);
                        gd.addNumericField("number of volumes:", length, 0);
                        gd.showDialog();
                        if (gd.wasCanceled())
                                return;
                        aspectx = (float) gd.getNextNumber();
                        aspecty = (float) gd.getNextNumber();
                        aspectz = (float) gd.getNextNumber();
                        anglex = (float) gd.getNextNumber();
                        angley = (float) gd.getNextNumber();
                        anglez = (float) gd.getNextNumber();
                        length = (int) gd.getNextNumber();

                        // create a new transformation matrix.
                        VJMatrix m = new VJMatrix();
                        // scale.
                        VJMatrix ms = new VJMatrix();
                        ms.scale(aspectx, aspecty, aspectz);
                        m.mul(ms);
                        // do the first rotation step.
                        rotate(m, v, anglex, angley, anglez);
                        // leave centered around 0,0,0

                        // Determine extents in viewspace.
                        setup(v, m);
                        // create the new stack of proper size.
                        ImageStack is = new ImageStack(width, height);

                        // for the whole volume.
                        for (int t = 0; t < length; t++)
                        {
                                for (int k = 0; k < depth; k++)
                                {
                                        IJ.showStatus("Reconstructing "+(t+1)+"/"+length+"("+k+"/"+depth+") "+(anglex)*(t+1)+
                                        ", "+(angley)*(t+1)+", "+(anglez)*(t+1));

                                        ImageProcessor ip;
                                        // Check the type of original stack.
                                        if (isv.getProcessor(1) instanceof ByteProcessor)
                                                ip = sliceByte(k);
                                        else
                                                ip = sliceShort(k);
                                        is.addSlice(""+k, ip);
                                }
                                rotate(m, v, anglex, angley, anglez);
                        }
                        ImagePlus impn = new ImagePlus("Hypervolume ", is);
                        float temp = (float) c.pixelDepth;
                        c.pixelDepth = c.pixelWidth;
                        impn.setCalibration(c);
                        c.pixelDepth = temp;
                        impn.show();
                }
        }
        /**
         * Process a stack slice in byte format.
         * @param k the position in k-space.
         */
        private ImageProcessor sliceByte(int k)
        {
                VJInterpolator interpolator = new VJTrilinear();
                VJInterpolator endinterpolator = new VJNearestNeighbor();
                ImageProcessor ip = new ByteProcessor(width, height);

                byte [] pixels = (byte[])ip.getPixels();
                VJValue value = new VJValue();

                // run through pixels in slice.
                for (int j = 0; j < height; j++)
                for (int i = 0; i < width; i++)
                {
                                // the viewspace location vector.
                                float [] vsv = new float[4];
                                vsv[0] = (float) i+ioffset;	// set destination x value
                                vsv[1] = (float) j+joffset;	// set destination y value
                                vsv[2] = (float) k+koffset; 	// this slice
                                vsv[3] = 1;
                                // the objectspace location, where you are in the rotated volume.
                                float [] osv = mi.mul(vsv);
                                VJVoxelLoc vl = new VJVoxelLoc(osv);
                                if (interpolator.isValid(vl, v))
                                        // Get the interpolated voxel value or values (vector).
                                        pixels[j*width+i] = (byte) interpolator.value(value, v, vl).intvalue;
                                else if ((float) vl.iz == vl.z &&
                                        endinterpolator.isValid(vl, v))
                                        // if dz == null, be sure to take the last slice in the volume.
                                        pixels[j*width+i] = (byte) endinterpolator.value(value, v, vl).intvalue;
                                else
                                        // no.
                                        pixels[j*width+i] = 0;
                }
                return ip;
        }
        /**
         * Process a stack slice in short format.
         * @param k the position in k-space.
         */
        private ImageProcessor sliceShort(int k)
        {
                VJInterpolator interpolator = new VJTrilinear();
                VJInterpolator endinterpolator = new VJNearestNeighbor();
                ImageProcessor ip = new ShortProcessor(width, height, false);

                short [] pixels = (short[])ip.getPixels();
                VJValue value = new VJValue();

                // run through pixels in slice.
                for (int j = 0; j < height; j++)
                for (int i = 0; i < width; i++)
                {
                                // the viewspace location vector.
                                float [] vsv = new float[4];
                                vsv[0] = (float) i+ioffset;	// set destination x value
                                vsv[1] = (float) j+joffset;	// set destination y value
                                vsv[2] = (float) k+koffset; 	// this slice
                                vsv[3] = 1;
                                // the objectspace location, where you are in the rotated volume.
                                float [] osv = mi.mul(vsv);
                                VJVoxelLoc vl = new VJVoxelLoc(osv);
                                if (interpolator.isValid(vl, v))
                                        // Get the interpolated voxel value or values (vector).
                                        pixels[j*width+i] = (short) interpolator.value(value, v, vl).intvalue;
                                else if ((float) vl.iz == vl.z &&
                                        endinterpolator.isValid(vl, v))
                                        // if dz == null, be sure to take the last slice in the volume.
                                        pixels[j*width+i] = (short) endinterpolator.value(value, v, vl).intvalue;
                                else
                                        // no.
                                        pixels[j*width+i] = 0;
                }
                return ip;
        }
        /**
         * Rotate the volume in viewspace
         * @param m the transformation matrix to convert from objectspace to viewspace coordinates.
         * @param anglex the rotation angle in degrees around the x-axis
         * @param angley the rotation angle in degrees around the y-axis
         * @param anglez the rotation angle in degrees around the z-axis
         */
        private void rotate(VJMatrix m, Volume v, float anglex, float angley, float anglez)
        {
                // Center the volume in viewspace.
                VJMatrix mt = new VJMatrix();
                mt.translate(-v.getWidth() / 2, -v.getHeight() / 2, -v.getDepth() / 2);
                m.mul(mt);
                // Rotate with angle around x,y,z.
                VJMatrix mrx = new VJMatrix();
                mrx.rotatex(anglex);
                m.mul(mrx);
                VJMatrix mry = new VJMatrix();
                mry.rotatey(angley);
                m.mul(mry);
                VJMatrix mrz = new VJMatrix();
                mrz.rotatez(anglez);
                m.mul(mrz);
        }
        /**
         * Setup the reconstructor for a specific viewport size width and height.
         * @param v the volume to be reconstructed.
         * @param m the transformation matrix.
        */
        public void setup(Volume v, VJMatrix m)
        {
                // determine optimal viewport size.
                int [] vp = VJViewspaceUtil.suggestViewport(v, m);
                width = vp[0];
                height = vp[1];

                // Compute the inverse of the transformation matrix for this view.
                mi = m.inverse();

                // Determine viewspace volume extents.
                int [][] minmax = VJViewspaceUtil.minmax(v, m);

                // Center the view in viewspace.
                // The i (viewspace x) and j (viewspace y) offsets relative to the center of the volume relative to the center of the viewspace.
                ioffset = (minmax[0][0]+minmax[1][0])/2 - width/2;
                joffset = (minmax[0][1]+minmax[1][1])/2 - height/2;

                // set k offset and depth.
                koffset = minmax[0][2];
                depth = minmax[1][2] - minmax[0][2];
        }
}

