package VolumeJ;
import java.awt.*;
import ij.*;
import ij.gui.*;

/**
 * This class implements a classification function on the voxel value only.
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
public class VJClassifierValue extends VJClassifierLevoy
{
        public VJClassifierValue() { super(16, 0, 8); description = "Value"; }
        public boolean doesIndex() { return false; }
        /**
        * Classify the interpolated voxel value into a VJAlphaColor.
        * Only the value is used.
        */
        public VJAlphaColor alphacolor(VJValue v, VJGradient g)
        {
                int intensity = v.intvalue & maskIntensity;     // low bits.
                return new VJAlphaColor(opacityTable[intensity],
                        (lut[0]&0xff),
                        (lut[1]&0xff),
                        (lut[2]&0xff));
        }
        public String toLongString()
        {
                return "Value ("+((does()==RGB)?"RGB":"grays")+") classifier. Makes voxels more opaque "+
                        " the closer their intensity is to threshold ("+threshold+"). The gradient is not used. "+
                        "Voxel colors determined only from LUT.";
        }
        /**
         * Compute opacity from intensity only.
        */
        protected double opacityCompute(double dfxi, double intensity, double threshold, double width)
        {
                if (intensity == threshold)
                        return 1;
                else return Math.max(0, 1.0 - (1.0 / width * 20) * Math.abs(threshold - intensity));
        }
}
