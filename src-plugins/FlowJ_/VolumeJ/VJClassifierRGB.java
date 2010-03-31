package VolumeJ;
import java.awt.*;
import ij.*;
import ij.gui.*;

/**
 * This class implements a classifier for RGB vector volumes.
 * It uses the familiar Levoy tent classification function with indexing.
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
public class VJClassifierRGB extends VJClassifierLevoy
{
        public VJClassifierRGB()
      	{
                super(8, 8, 0);
                description = "RGB Gradient";
        }
		// Tell calling program that this one will return grayscale values.
		public int does() { return GRAYSCALE; }
		// Tells the calling program it cannot process indexfiles.
		public boolean doesIndex() { return false; }
		public String toLongString() { return toString() + " ("+nrIntensityBits+":"+
	  			nrIndexBits+""+"RGB(avg gradient)"+"), thr: "+threshold+", dev: "+width; }
		public VJAlphaColor alphacolor(VJrgbValue v, VJGradient g)
		/**
			  RGB volume voxel classification.
			  Classify the interpolated r,g,b color in vvalue
			  for the future pixel into an rgb value, and
			  compute the opacity for the future pixel from the
			  R,G,B intensity and the total 3-D gradient g, i.e. the combined intensity of
			  the interpolated r,g,b gradients.
			  The difficult part is to decide whether to take separate opacities
			  for the r, g, b channels.
			  If no, what is a good combination of them: their average?
			  If yes, you have to carry separate opacities all the way through the composing
			  process, resulting in more complex, more difficult to understand code,
			  and the interface becomes more complex.
			  So, for the moment I took the average of the r,g,b, intensities
			  as the total intensity, and the average of the gradients as the
			  total gradient.
		*/
		{
                        int [] intensity = new int[3];
                        // First create the classified color (just take the (interpol) voxel color.
                        // Use the index if applicable.
                        int index = v.index;     // highest 8 bits.
                        // Code magnitude into bits.
                        int igradient = (int) (g.getAverageMag() * fractionMagnitude) & maskMagnitude;
                        // Compute average intensity
                        int avgIntensity = (v.r+v.g+v.b) / 3;
                        // make all of it fit into 16 bits.
                        int entry = (igradient << nrIntensityBits) | avgIntensity;
                        // Now create the color and opacities.
                        return new VJAlphaColor(opacityTable[entry],
					(intensity[0]&0xff),
					(intensity[1]&0xff),
					(intensity[2]&0xff));
		}
}

