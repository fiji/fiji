package bijnum;
/**
 * This class implements static transformation matrix operations for 2-D and 3-D transformation matrices.
 * Interface leans heavily on
 * <code>Foley, van Dam. Computer Graphics. Second ed.</code>
 * Homogenuous coordinate systems.
 *
 * Copyright (c) 1999-2003, Michael Abramoff. All rights reserved.
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
public class BIJtransform
{
        /**
	 * Create a new (diagonal) transformation matrix for the number of dimensions.
	 * Matrix will be one larger because we need a homogenuous coordinate system.
	 */
	public static float [][] newMatrix(int dimensions)
	{
		float [][] m = new float[dimensions+1][dimensions+1];
		for (int j = 0; j < m.length; j++)
	        for (int i = 0; i < m[0].length; i++)
	               m[j][i] = (i == j) ? 1 : 0;
                return m;
	}
	/**
         * Translate a 3-D transformation matrix by these values.
	 * @param m the transformation matrix
         * @param tx,ty,tz the amounts to translate on each of the axes.
	 * @return a new, transformed matrix.
         */
        public static float [][] translate(float [][] m, double tx, double ty, double tz)
        {
                float [][] nm = new float[m.length][m[0].length];
		nm[0][0] = 1;
		nm[1][1] = 1;
		nm[2][2] = 1;
		nm[3][3] = 1;
		nm[0][3] = (float) tx;
                nm[1][3] = (float) ty;
                nm[2][3] = (float) tz;
		return BIJmatrix.mul(m, nm);
        }
        /**
         * Translate a 2-D transformation matrix by these values.
	 * @param m the transformation matrix
         * @param tx,ty the amounts to translate on each of the axes.
	 * @return a new, transformed matrix.
         */
        public static float [][] translate(float [][] m, double tx, double ty)
        {
                float [][] nm = new float[m.length][m[0].length];
		nm[0][0] = 1;
		nm[1][1] = 1;
		nm[2][2] = 1;
		nm[0][2] = (float) tx;
                nm[1][2] = (float) ty;
		return BIJmatrix.mul(m, nm);
        }
        /**
         * Scale a 3-D transformation matrix by sx, sy, sz.
	 * @param m the transformation matrix
         * @param sx,sy,sz the amounts to scale by on each of the axes.
	 * @return a new, transformed matrix.
         */
        public static float [][] scale(float [][] m, double sx, double sy, double sz)
        {
                float [][] nm = new float[m.length][m[0].length];
		nm[3][3] = 1;
                nm[0][0] = (float) sx;
                nm[1][1] = (float) sy;
                nm[2][2] = (float) sz;
		return BIJmatrix.mul(m, nm);
        }
        /**
         * Scale a 2-D transformation matrix by sx, sy.
	 * @param m the transformation matrix
         * @param sx,sy the amounts to scale by on each of the axes.
	 * @return a new, transformed matrix.
         */
        public static float [][] scale(float [][] m, double sx, double sy)
        {
                float [][] nm = new float[m.length][m[0].length];
		nm[2][2] = 1;
                nm[0][0] = (float) sx;
                nm[1][1] = (float) sy;
		return BIJmatrix.mul(m, nm);
        }
        /**
         * Rotate a transformation matrix around the x-axis.
	 * I dont think you can rotate a 2-D image about the x-axis.
	 * @param m the transformation matrix
         * @param theta the amount to rotate (in degrees)
	 * @return a new, transformed matrix.
         */
        public static float [][] rotatex(float [][] m, double theta)
        {
                double d_theta = theta * Math.PI / 180;
                double sin_theta = clean(Math.sin(d_theta));
                double cos_theta = clean(Math.cos(d_theta));
                float [][] nm = new float[m.length][m[0].length];
		nm[0][0] = 1;
		nm[3][3] = 1;
		nm[1][1] = (float) cos_theta;
		nm[2][1] = (float) sin_theta;
		nm[1][2] = (float) -sin_theta;
		nm[2][2] = (float) cos_theta;
		return BIJmatrix.mul(m, nm);
        }
        /**
         * Rotate a transformation matrix around the y axis.
	 * I dont think you can rotate a 2-D image about the y-axis.
	 * @param m the transformation matrix
         * @param theta the amount to rotate (in degrees)
	 * @return a new, transformed matrix.
         */
        public static float [][] rotatey(float [][] m, double theta)
        {
                double d_theta = theta * Math.PI / 180;
                double sin_theta = clean(Math.sin(d_theta));
                double cos_theta = clean(Math.cos(d_theta));
                float [][] nm = new float[m.length][m[0].length];
		nm[1][1] = 1;
		nm[3][3] = 1;
                nm[0][0] = (float) cos_theta;
                nm[2][0] = (float) -sin_theta;
                nm[0][2] = (float) sin_theta;
                nm[2][2] = (float) cos_theta;
		return BIJmatrix.mul(m, nm);
	}
        /**
         * Rotate a transformation matrix around the z axis.
         * I dont think you can rotate a 2-D image about the z-axis.
	 * @param m the transformation matrix
         * @param theta the amount to rotate (in degrees)
	 * @return a new, transformed matrix.
         */
        public static float [][] rotatez(float [][] m, double theta)
        {
                double d_theta = theta * Math.PI / 180;
                double sin_theta = clean(Math.sin(d_theta));
                double cos_theta = clean(Math.cos(d_theta));
                float [][] nm = new float[m.length][m[0].length];
		if (nm.length > 3)
	              nm[3][3] = 1;
		nm[2][2] = 1;
		nm[0][0] = (float) cos_theta;
		nm[1][0] = (float) sin_theta;
		nm[0][1] = (float) -sin_theta;
		nm[1][1] = (float) cos_theta;
		return BIJmatrix.mul(m, nm);
        }
        protected static double clean(double d)
        {
                return Math.abs(d) < 0.00000001 ? 0 : d;
        }
}
