package VolumeJ;

/**
 * VJrgbValue. Contains a rgb value plus an index.
 * This is a more object oriented way of passing interpolated voxel values around than &-ing
 * them into an int.
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
public class VJrgbValue extends VJValue
{
        public int r, g, b;

        public VJrgbValue() {}
        /**
         * Create a new VJrgbValue.
         * @param r,g,b int for R,G and B
         * @param index an int serving as the index value.
         */
        public VJrgbValue(int r, int g, int b, int index)
        {       this.r = r; this.g = g; this.b = b; this.index = index; }
}