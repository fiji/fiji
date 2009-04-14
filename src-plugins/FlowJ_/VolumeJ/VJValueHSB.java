package VolumeJ;

/**
 * Contains an RGB volume voxel value in HSB format.
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
public class VJValueHSB extends VJValue
{
        public float [] hsb;

        /**
         * Create a new VJValueHSB.
         */
        public VJValueHSB() { hsb = new float[3]; }
        /**
         * Create a new VJValueHSB with hue, saturation and brightness set.
		 * @param hue a float [0-1]
		 * @param saturation a float [0-1]
		 * @param brightness a float 0-255.
         */
        public VJValueHSB(float hue, float saturation, float brightness)
        {
			super(brightness);
			hsb = new float[3];
			hsb[0] = hue;
			hsb[1] = saturation;
			hsb[2] = floatvalue;
		}
		public float getHue() { return hsb[0]; }
		public float getSaturation() { return hsb[1]; }
}