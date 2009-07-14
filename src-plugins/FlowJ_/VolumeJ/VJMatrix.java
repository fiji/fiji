package VolumeJ;
import bijnum.*;
/**
 * This class implements a transformation matrix and 3D transformation methods on it.
 * It also implements the tranformations necessary for the shear-warp rendering algorithm
 * as defined in Lacroute's thesis.
 * Interface leans heavily on
 * <code>Foley, van Dam. Computer Graphics. Second ed.</code>
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
public class VJMatrix
{
	/**
         * The (symmetric) transformation matrix.
         */
	private float [][] m;

        /**
         * Create a new transformation matrix initialized to the unit matrix.
         */
        public VJMatrix()
        {
                m = BIJtransform.newMatrix(3);
        }
        /**
         * Create a new transformation matrix from a float[][] matrix.
         */
        public VJMatrix(float [][] m)
        {
                this.m = m;
        }
        /**
         * Create a new transformation matrix initialized to another matrix.
         * @param m1 the matrix that is copied to this matrix.
         */
        public VJMatrix(VJMatrix vjm)
        {
                m = BIJtransform.newMatrix(3);
		for (int i = 0; i < m.length; i++)
		for (int j = 0; j < m[0].length; j++)
		        this.m[j][i] = vjm.m[j][i];
        }
        /**
         * Create a new 3-D homogenuous vector from 3 coordinates.
         */
        public static float [] newVector(float x, float y, float z)
        {
                float [] v = new float[4];
                v[0] = x; v[1] = y; v[2] = z; v[3] = 1;
                return v;
        }
        /**
         * Translate the coordinate system to tx, ty, tz.
         * @param tx,ty,tz the amounts to translate on each of the three axes.
         */
        public void translate(double tx, double ty, double tz)
        {
                m = BIJtransform.translate(m, (float) tx, (float) ty, (float) tz);
        }
        /**
         * Scale the coordinate system by sx, sy, sz.
         * @param sx,sy,sz the amounts to scale by on each of the three axes.
         */
        public void scale(double sx, double sy, double sz)
        {
                m = BIJtransform.scale(m, (float) sx, (float) sy, (float) sz);
        }
        /**
         *  Rotate the coordinate system around the x axis.
         *  @param theta the amount to rotate (in degrees)
         */
        public void rotatex(double theta)
        {
                m = BIJtransform.rotatex(m, (float) theta);
        }
        /**
         *  Rotate the coordinate system around the y axis.
         *  @param theta the amount to rotate (in degrees)
         */
        public void rotatey(double theta)
        {
                m = BIJtransform.rotatey(m, (float) theta);
        }
        /**
         *  Rotate the coordinate system around the z axis.
         *  @param theta the amount to rotate (in degrees)
         */
        public void rotatez(double theta)
        {
                m = BIJtransform.rotatez(m, (float) theta);
        }
        /**
         *  Multiply a vector v with this transformation matrix and return the result as a vector.
         *  @param v a 4-D homogeneous vector (x,y,z,w).
         *  @return a 4-D vector
         */
	public float [] mul(float [] v)
        {
                float [] r = null;
		try
		{
			r = BIJmatrix.mul(m, v);
		} catch (Exception e) { VJUserInterface.error("grave error in VJMatrix"+e); }
		return r;
        }
        /**
         * Find the homogenuous coordinate with the lowest value on a dimension in an array of coordinates.
         * @param vertex an array of double[4] coordinates.
         * @param dimension the axis on which you want the maximum value.
         * @return the coordinate with the lowest value on the chose dimension.
         */
         public static float [] getMin(float [][] vertex, int dimension)
         {
                float minv = Float.MAX_VALUE;
                int i = 0;
                for (int v = 0; v < vertex.length; v++)
                {
                        if (vertex[v][dimension] < minv)
                        {
                                minv = vertex[v][dimension];
                                i = v;
                        }
                }
                float [] min = new float[4];
                min[0] = vertex[i][0];
                min[1] = vertex[i][1];
                min[2] = vertex[i][2];
                min[3] = vertex[i][3];
                return min;
        }
        /**
         * Find the homogenuous coordinate with the highest value in an array of coordinates.
         * @param vertex an array of double[4] coordinates.
         * @param dimension the axis on which you want the maximum value.
         * @return the coordinate with the highest value on the chosen dimension.
         */
         public static float [] getMax(float [][] vertex, int dimension)
         {
                float maxv = -Float.MAX_VALUE;
                int i = 0;
                for (int v = 0; v < vertex.length; v++)
                {
                        if (vertex[v][dimension] > maxv)
                        {
                                maxv = vertex[v][dimension];
                                i = v;
                        }
                }
                float [] max = new float[4];
                max[0] = vertex[i][0];
                max[1] = vertex[i][1];
                max[2] = vertex[i][2];
                max[3] = vertex[i][3];
                return max;
        }
        /**
         *  Multiply this transformation matrix with another transformation matrix in place.
         *  this = m1 this
         *  @param ml a VJMatrix.
         */
        public void mul(VJMatrix m0)
        {
                m = BIJmatrix.mul(m0.m, m);
        }
        /**
         *  Return the third column of the 3D transform matrix as a vector.
         *  This can be used to step through the coordinate system incrementally along the z-axis.
         *  @return a 4-D vector
         */
         public float [] getStepperColumn()
	 {
                return BIJmatrix.col(m, 2);
	 }
        /**
         *  Return the n-th column of the 3D transform matrix as a vector.
         *  This can be used to step through the coordinate system incrementally along an arbitrary axis.
         *  @param the axis for which to get the column. 0 = i, 1 = j, 2 = k axis.
         *  @return a 4-D vector
         */
         public float [] getColumn(int n)
	 {
                return BIJmatrix.col(m, n);
	 }
        /**
         *  Return the oversampling ratio for this (usually inverse) transformation matrix.
         *  The oversampling ration N is defined by the inverse of the distance you travel
         *  in objectspace for a unitary step in k-space.
         *  @return the ratio as a double.
         */
	public float getOversamplingRatio()
	{
                // make the two vectors in object space.
                float [] vs = new float[4];
                vs[0] = 0; vs[1] = 0; vs[2] = 0; vs[3] = 1;
                float [] os0 = mul(vs);
                vs[0] = 0; vs[1] = 0; vs[2] = 1; vs[3] = 1;
                float [] os1 = mul(vs);
                // distance is the step size in objectspace for one k step.
                float distance = (float) Math.sqrt(Math.pow(os0[0] - os1[0], 2)+
                        Math.pow(os0[1] - os1[1], 2)+
                        Math.pow(os0[2] - os1[2], 2));
                // The oversampling ratio is the inverse of the distance.
                return 1 / distance;
	}
        /**
         * Shear the coordinate system by sx, sy.
         * @param sx,sy the amounts of shear by on each of the two axes.
         */
	public void shear(double sx, double sy)
        {
                m[0][2] = (float) sx;
                m[1][2] = (float) sy;
        }
        /**
         *  Make a permutation for one of the major axes (x,y,z)
         *  Used for shear-warp rendering.
         *  @param axis the axis around which to permute.
         */
	public void permutation(int axis)
        {
                if (axis == 0) // x
                {
                        m[0][0] = 0; m[0][1] = 1; m[0][2] = 0; m[0][3] = 0;
                        m[1][0] = 0; m[1][1] = 0; m[1][2] = 1; m[1][3] = 0;
                        m[2][0] = 1; m[2][1] = 0; m[2][2] = 0; m[2][3] = 0;
                        m[3][0] = 0; m[3][1] = 0; m[3][2] = 0; m[3][3] = 1;
                }
                else if (axis == 1) // y
                {
                        m[0][0] = 0; m[0][1] = 0; m[0][2] = 1; m[0][3] = 0;
                        m[1][0] = 1; m[1][1] = 0; m[1][2] = 0; m[1][3] = 0;
                        m[2][0] = 0; m[2][1] = 1; m[2][2] = 0; m[2][3] = 0;
                        m[3][0] = 0; m[3][1] = 0; m[3][2] = 0; m[3][3] = 1;
                }
                else                // z
                {
                        m[0][0] = 1; m[0][1] = 0; m[0][2] = 0; m[0][3] = 0;
                        m[1][0] = 0; m[1][1] = 1; m[1][2] = 0; m[1][3] = 0;
                        m[2][0] = 0; m[2][1] = 0; m[2][2] = 1; m[2][3] = 0;
                        m[3][0] = 0; m[3][1] = 0; m[3][2] = 0; m[3][3] = 1;
                }

        }
        /**
         * Return the viewing vector as defined in Lacroute's thesis.
         */
        public double [] getViewingVector()
        {
                double [] v = new double[4];
                v[0] = m[0][1] * m[1][2] - m[1][1] * m[0][2]; /* m[1][2]m[2][3] - m[2][2]m[1][3] */
                v[1] = m[1][0] * m[0][2] - m[0][0] * m[1][2]; /* m[2][1]m[1][3] - m[1][1]m[2][3] */
                v[2] = m[0][0] * m[1][1] - m[1][0] * m[0][1]; /* m[1][1]m[2][2] - m[2][1]m[1][2] */
                v[3] = 1;
                return v;
        }
        /**
         * Return the slice order as defined in Lacroutes thesis.
         * Returns true in inverse slice order, false if straight slice order.
        */
        public boolean getSliceOrder(double sci, double scj)
        {
                return (m[2][0] * sci + m[2][1] * scj - m[2][2]) > 0;
        }
        /**
         * Return the shear coefficients as defined in Lacroute's thesis.
         */
        public double [] getShearCoefficients()
        {
                double [] sc = new double[2];
                // i
                sc[0] = (m[1][1] * m[0][2] - m[0][1] * m[1][2]) / (m[0][0] * m[1][1] - m[1][0] * m[0][1]);
                // j
                sc[1] = (m[0][0] * m[1][2] - m[1][0] * m[0][2]) / (m[0][0] * m[1][1] - m[1][0] * m[0][1]);
                return sc;
        }
        /**
         * Invert this matrix. Slow, since it also checks whether the inverse is correct.
         * Interfaces to BIJMatrix.
         * @return a VJMatrix with the inverse of this matrix.
         */
        public VJMatrix inverse()
        {
                float [][] mi = null;
                try
                {
                        mi = BIJmatrix.inverse(m);
                } catch (Exception e) { VJUserInterface.error("VJMatrix: "+e); }
                return new VJMatrix(mi);
        }
        public String toString()
        {
                String s = "";

                s += m[0][0] + ", " + m[0][1] + ", " + m[0][2] + ", " + m[0][3] + "\n";
                s += m[1][0] + ", " + m[1][1] + ", " + m[1][2] + ", " + m[1][3] + "\n";
                s += m[2][0] + ", " + m[2][1] + ", " + m[2][2] + ", " + m[2][3] + "\n";
                s += m[3][0] + ", " + m[3][1] + ", " + m[3][2] + ", " + m[3][3] + "\n";
                return s;
        }
}
