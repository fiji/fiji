package VolumeJ;
import java.awt.*;
import ij.*;
import ij.gui.*;

/**
 * This class implements the Levoy tent classification function without indexing.
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
public class VJClassifierLNoIndex extends VJClassifierLevoy
{
        public VJClassifierLNoIndex()
        {
                // 8 bits for voxel value.
                // 8 bits for magnitude.
                super(8, 8, 0);
                // Name of this classifier.
                description = "Gradient no index";
        }
        // Tell calling program that this one will return grayscale values.
        public int does() { return GRAYSCALE; }
        // Tells the calling program it cannot process indexfiles.
      public boolean doesIndex() { return false; }
        public String toLongString()
        {
                return "Levoy ("+((does()==RGB)?"RGB":"grays")+") classifier. Makes voxels more opaque "+
                        " the closer their intensity is to threshold ("+threshold+") and the higher their surface gradient "+
                        " (relative contribution set by deviation). Voxel colors only from LUT, no indexing.";
        }
        public VJAlphaColor alphacolor(VJValue v, VJGradient g)
        /**
         * Classify the (interpolated) voxel value and gradient magnitude
         * into a grayvalue.
         * The gradient magnitude is converted into bits, the intensity into bits.
         * Return the opacity (0-1) and color (0-255) for this voxel intensity and gradient.
        */
        {
                int intensity = v.intvalue;
                int index = v.index;
                // Code magnitude into bits.
                int igradient = (int) (g.getmag() * fractionMagnitude) & maskMagnitude;
                // make all of it fit into 16 bits.
                  int entry = (igradient << nrIntensityBits) | intensity;
                  return new VJAlphaColor(opacityTable[entry], 255);
	  }
}

