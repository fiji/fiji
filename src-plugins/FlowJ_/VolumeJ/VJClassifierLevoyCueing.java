package VolumeJ;
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.*;
import java.io.*;

/**
 * This class implements the Levoy tent classification function with depth cueing and indexing.
 * class name VJClassifierLevoyDepthCueing is not supported on Mac VJM.
 * @see VJClassifierLevoy
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
public class VJClassifierLevoyCueing extends VJClassifierLevoy
{
        /**
         * Default instantiation.
         */
        public VJClassifierLevoyCueing()
        {
                super(8, 8, 8);
                description = "Gradient w/ depth cueing and index(spectrum)";
        }
        public String toString() { return description; }
        public String toLongString()
        {
                return "Levoy ("+((does()==RGB)?"RGB":"grays")+") depth cueing classifier. Makes voxels more opaque "+
                        " the closer their intensity is to threshold ("+threshold+") and the higher their surface gradient "+
                        " (relative contribution set by deviation). Brightness of voxel decreases with distance. Voxel colors determined from LUT and index volume if present.";
        }
        /**
         * Classify the (interpolated) voxel value and gradient magnitude into a alpha and rgb-value.
         * If the voxel is RGB, use the hue and saturation of the voxel, and set the opacity from
         * the opacity table.
         * @param v the VJValue, the interpolated value at this location.
         * @param g the gradient at this location
	 * @return a VJAlphaColor with the classified voxel.
        */
        public VJAlphaColor alphacolor(VJValue v, VJGradient g)
        {
                // Code gradient magnitude into int.
                int igradient = (int) (g.getmag() * fractionMagnitude) & maskMagnitude;
                // Fit gradient magnitude and intensity into 16 bits.
                int entry = (igradient << nrIntensityBits) | v.intvalue;
                if (v instanceof VJValueHSB)
                {
                        // Is an RGB (HSB format) voxel! Use true colors.
                        Color color = java.awt.Color.getHSBColor(((VJValueHSB)v).getHue(),
                              ((VJValueHSB)v).getSaturation(), 1);
                        return new VJAlphaColor(opacityTable[entry],
                        color.getRed(), color.getGreen(), color.getBlue());
                }
                else return new VJAlphaColor(opacityTable[entry],
                        (lut[v.index*3+0]&0xff),
                        (lut[v.index*3+1]&0xff),
                        (lut[v.index*3+2]&0xff));
        }
}

