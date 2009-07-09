package VolumeJ;
import ij.*;
import volume.*;

/**
 * This class is a set of projections of an object.
 * Its properties are the projections (as images) and the projection angles of each image.
 * It belongs to the BackProjection group of algorithms.
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
public class VJProjection
{
        /**
         * Projection images are of type byte[].
         */
        private static final int BYTE = 1;
        /**
         * Projection images are of type short[].
         */
        private static final int SHORT = 2;
        /**
         * Projection images are of type float[].
         */
        private static final int FLOAT = 4;
        /**
         * Projection images are of type int (3 byte RGB)[].
         */
        private static final int INT = 3;
        protected int type;
        /**
         * Angle (in degrees) of each projection image.
         */
        protected float [] theta;
        /**
         * The projection images.
         */
        protected Object [] sliceArray;
        /**
         * The number of projection images.
         */
        protected int   n;
        /**
         * The width of each projection image.
         */
        protected int   sliceWidth;
        protected int   sliceHeight;

        /**
         * Create a new projection.
         * @param deltaAngle the distance in degrees between each projection image.
         * @param sliceArray an array of Object-s with in each object a byte, short or int image.
         * @param n the number of projection images in sliceArray.
         * @param width the width of each projection image in sliceArray.
         */
        public VJProjection(double deltaAngle, Object [] sliceArray, int n, int width, int height)
        {
                this.sliceArray = sliceArray;
                this.n = n;
                this.sliceWidth = width;
                this.sliceHeight = height;
                type = getType(sliceArray);
                buildAngles(deltaAngle, n);
        }
        /**
         * Return the slice array this back projection is based on.
         * Can be used for pre-filtering the projection images.
         * @return the sliceArray as an Object.
         */
        public Object [] getImageArray() { return sliceArray; }
        /**
         * Return the width of the images in sliceArray.
         * @return an int with the width.
         */
        public int getWidth() { return sliceWidth; }
        /**
         * Return the height of the images in sliceArray.
         * @return an int with the width.
         */
        public int getHeight() { return sliceHeight; }
        /**
         * Return the datatype of the array components.
         * @param array an array of objects.
         * @return BYTE if array is of type byte[], SHORT if array is of type short[], INT if array is of type int[]
         * and FLOAt if array is of type float[].
         */
        protected int getType(Object [] array)
        {
                if (array[0] instanceof byte[]) return BYTE;
                else if (array[0] instanceof short[]) return SHORT;
                else if (array[0] instanceof float[]) return FLOAT;
                else if (array[0] instanceof int[]) return INT;
                else return 0;
        }
        /**
         * Compute the angle of each projection image for a sequence of equidistant
         * projection images.
         * @param deltaAngle the distance between each projectioon image in degrees.
         * @param n the number of projection images.
         */
        protected void buildAngles(double deltaAngle, int n)
        {
                float angle = 0;
                theta = new float[n];
                for (int i = 0; i < n; i++)
                {
                        theta[i] = angle;
                        angle += deltaAngle;
                }
        }
        /**
         * Return the number of projection images in this projection.
         */
        public int n() { return n; }
        /**
         * Backprojection around a single axis y. This is the inverse of tha Radon transform.
         * Backproject each projection image into the volume.
         * @param v a VolumeFloat that has the right size to be filled with the backprojection.
         */
        public void backproject(Volume v)
        {
                int width = v.getWidth();
                // Compute center of volume vc.
                int cvx = v.getWidth() / 2;
                int cvy = v.getHeight() / 2;
                int cvz = v.getDepth() / 2;
                // Compute center of projection image ic.
                int cix = sliceWidth / 2;
                // Compute line (actually plane) equation of vc to ic.
                // Traverse all projections.
                VJInterpolator interpolator = new VJTrilinear();
                for (int i = 0; i < n; i++)
                {
                        float currentTheta = theta[i];
                        float costheta = (float) Math.cos(currentTheta);
                        float sintheta = (float) Math.sin(currentTheta);
                        // Traverse all voxels.
                        for (int z = 0; z < v.getDepth(); z++)
                        for (int y = 0; y < v.getHeight(); y++)
                        for (int x = 0; x < v.getWidth(); x++)
                        {
                                StringBuffer sb = new StringBuffer("Backproject "); sb.append(theta[i]);
                                sb.append("degrees "); sb.append((z*100)/v.getDepth()); sb.append("%");
                                VJUserInterface.status(sb.toString());
                                VJVoxelLoc vl = new VJVoxelLoc(x, y, z);
                                if (interpolator.isValid(vl, v))
                                {
                                        // Compute x and y position relative to center of volume.
                                        float vx = x - cvx;
                                        float vy = y - cvy;
                                        // d=distance of current voxel to line between vc and ic.
                                        float d = vx * costheta + vy * sintheta;
                                        // Compute position in projection image.
                                        float imagex = d + cix;
                                        int imageix = (int) imagex;
                                        float dx = imagex - (float) imageix;
                                        if (VJTrilinear.valid(v.getWidth(), v.getHeight(), imageix, y))
                                        {
                                                // Interpolate in projection image at x, z.
                                                float value;
                                                if (type == BYTE)
                                                                value = VJTrilinear.value((byte []) sliceArray[i], width, imageix, z, dx, 0f);
                                                else if (type == SHORT)
                                                                value = VJTrilinear.value((short []) sliceArray[i], width, imageix, z, dx, 0f);
                                                else if (type == FLOAT)
                                                                value = VJTrilinear.value((float []) sliceArray[i], width, imageix, z, dx, 0f);
                                                else
                                                                value = 0;
                                                // Summate for voxel at x,y,z over all projections i.
                                                float sum = (float) ((Number) v.get(x, y, z)).floatValue() + value;
                                                v.set(new Float(sum), x, y, z);
                                        }
                                }
                        }
                }
        }
}
