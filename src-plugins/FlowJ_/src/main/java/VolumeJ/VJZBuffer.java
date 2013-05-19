package VolumeJ;
import volume.*;

/**
 * VJZBuffer.
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
public class VJZBuffer
{
        private float []        depths;
        private int             width;
        private int             height;

        /**
         * Create a new VJZBuffer filled with 0.
         * @param width
         * @param height the dimensions of the buffer.
         */
        public VJZBuffer(int width, int height)
        {
                this.width = width;
                this.height = height;
                depths = new float[height*width];
        }
        /**
         * Create a new VJZBuffer filled with 0.
         * @param width
         * @param height the dimensions of the buffer.
         */
        public VJZBuffer(int width, int height, double f)
        {
                this.width = width;
                this.height = height;
                depths = new float[height*width];
                for (int i = 0; i < depths.length; i++)
                        depths[i] = (float) f;
        }
        /**
         * Insert a depth at i,j into the z-buffer.
         * @param depth the depth at i,j
         * @param i,j the positions in viewspace coordinates.
         */
        protected void insert(double depth, int i, int j)
        {
                depths[j*width+i] = (float) depth;
        }
        /**
         * Get a depth at i,j.
         * @param i,j the positions in viewspace coordinates.
         * @return the depth at i,j
         */
        protected double get(int i, int j)
        {
                return (double) depths[j*width+i];
        }
        public float [] getBuffer()
        {
                return depths;
        }
}
