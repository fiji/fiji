package VolumeJ;

/**
 * VJShade. This is a shade, but abstracted for polymorphic volume rendering.
 * Shade can be single channel (normal volume rendering)
 * or
 * multichannel: then each (RGB) color has its own shade.
 * For other shades: subclass this class.
 *
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
public class VJShade
{
		private int 	channels;

		// inlined for speed.
		private float attenuation0;
		private float attenuation1;
		private float attenuation2;

		public VJShade(float attenuation)
		{
			  channels = 1;
			  attenuation0 = attenuation;
		}
		public VJShade(float attenuation0, float attenuation1, float attenuation2)
		{
			  channels = 3;
			  this.attenuation0 = attenuation0;
			  this.attenuation1 = attenuation1;
			  this.attenuation2 = attenuation2;
		}
		public float get() { return attenuation0; }
		/**
		 * Compute the value of contribution after shading with this shade.
		 * @param contribution the contribution [0-1]
		 * @return a float with contribution * attenuation [0-1].
		 */
		public float compute(float contribution) { return attenuation0*contribution; }
		public float get(int i)
		{
			switch (i)
			{
				case 0: 	return attenuation0;
				case 1: 	return attenuation1;
				default: 	return attenuation2;
			}
		}
		public int channels() { return channels; }
		public boolean singlechannel() { return (channels == 1); }
		public String toString()
		{
			String s;

			if (channels == 1)
				s = "shade: "+attenuation0;
			else
				s = "shade "+attenuation0+"(r), "+attenuation1+"(g), "+attenuation2+"(b)";
			return s;
		}

}

