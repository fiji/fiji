package VolumeJ;

/**
 * VJPlane. Defines a 3D plane equation and methods to create and operate on it.
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
public class VJPlane
{
        private float 				A, B, C, D;

        /**
         * Create a new plane through 3 points p, q, r.
         * @param p, q, r  three double[3] points.
        */
        public VJPlane(float [] p, float [] q, float [] r)
        {
                solve(p, q, r);
        }
        /**
         * Create a new plane from another plane.
         * @param p a VJPlane.
        */
        public VJPlane(VJPlane p)
        {
                A = p.A; B = p.B; C = p.C; D = p.D;
        }
        /**
         * Create a new plane parallel to a plane p through a point q.
         * Only D in the plane equation is different for parallel planes.
         * @param p a VJPlane to which this plane is to be parallel.
         * @param q a point through which this new plane should also go.
        */
        public VJPlane(VJPlane p, float [] q)
        {
                A = p.A; B = p.B; C = p.C;
                D = - (A * q[0] + B * q[1] + C * q[2]);
        }
        /**
         * Create a new plane through 3 homogenuous coordinates p, q, r defined in a coordinate
         * system, but define the plane in a transformation of that coordinate system defined by m.
         * @param p, q, r  three double[4] points.
        */
        public VJPlane(float [] p, float [] q, float [] r, VJMatrix m)
        {
                float [] tsp = m.mul(p);
                float [] tsq = m.mul(q);
                float [] tsr = m.mul(r);
                solve(tsp, tsq, tsr);
        }
        /**
         * Create a new plane through 3 points p, q, r by solving
         * the plane equation:
         * Ax + By + Cz + D = 0;
         * for a plane through points p, q and r. See Foley et al, Computer Graphics.
        */
        private void solve(float [] p, float [] q, float [] r)
        {
                float [] v1 = new float[3];
                // pq
                v1[0]= q[0] - p[0]; v1[1] = q[1] - p[1]; v1[2] = q[2] - p[2];
                float [] v2 = new float[3];
                // pr
                v2[0]= r[0] - p[0]; v2[1] = r[1] - p[1]; v2[2] = r[2] - p[2];
                // v1 x v2
                A = v1[1] * v2[2] - v1[2]*v2[1];
                B = v1[2] * v2[0] - v1[0]*v2[2];
                C = v1[0] * v2[1] - v1[1]*v2[0];
                D = - (A * p[0] + B * p[1] + C * p[2]);
        }
        /**
         * Find the intersection of the straight line from x0,y0, z0 with derivative
         * dx, dy, dz with this plane. Return the intersection in homogoneous coordinates.
         * Foley, van Dam. Computer Graphics. Second ed.
         * Plane equation: Ax+ By + Cz + D = 0
         * Ray equation:
         *      x = x0 + t dx.
         *      y = y0 + t dy.
         *      z = z0 + t dz.
         *      t = - (Ax0 + By0 + Cz0 + D) / (Adx + Bdy + Cdz)
         * if t = 0, the planes do not intersect.
         * else	the crossing is at x0+tdx, y0+tdy, z0+tdz
         * @param x0, y0, z0 a point on the line.
         * @param dx, dy, dz the derivative of the line.
         * @return float [] with the homogenous coordinates of the intersection.
         * @return null if plane and line are parallel.
        */
        private float [] intersect(float x0, float y0, float z0, float dx, float dy, float dz)
        {
                // parallel!
                if ((A * dx + B * dy + C * dz) == 0)
                        return null;
                float t = - (A * x0 + B * y0 + C * z0 + D)
                        / (A * dx + B * dy + C * dz);
                return VJMatrix.newVector(x0 + t * dx, y0 + t * dy, z0 + t * dz);
        }
        /**
         * Optimization for finding intersection of this plane with a ray running through
         * x0, y0, 0 with derivative 0,0,1.
         * For this line, dx and dy = 0, z0 = 0, dz = 1.
         * Return the intersection in homogoneous coordinates.
         * @param x0, y0 defines the origin of the ray.
         * @return the z coordinate of the intersection (others are x0, y0).
         * @return Double.POSITIVE_INFINITY if plane and line are parallel.
        */
        public float intersectRay(float x0, float y0)
        {
                if (C == 0)
                        return Float.POSITIVE_INFINITY;
                float t = - (A * x0 + B * y0 + D) / C;
                return t;
        }
        /**
         * Return the result of a plane equation with a,b,c,d with a point x,y,z filled in.
         * Should be 0.0 if x,y,z, is within the plane.
         * @param x,y,z the point you want to check.
         * @param a,b,c,d the A,B,C,D of a plane equation.
         */
        public static float check(float x, float y, float z, float a, float b, float c, float d)
        {
                return a*x + b*y + c*z + d;
        }
        /**
         * Return the gradient of a plane (the normal vector) as a VJGradient.
         * @return a VJGradient that contains the normal vector of the plane.
         */
        public VJGradient getGradient()
        {
                VJGradient g = new VJGradient(A, B, C);
                g.normalize();
                return g;
        }
        /**
         * Get the A for the plane equation Ax+By+C+D=0 of this plane
         * @return double the value of A
         */
        public float getA()
        {
                return A;
        }
        /**
         * Get the B for the plane equation Ax+By+C+D=0 of this plane
         * @return double the value of B
         */
        public float getB()
        {
                return B;
        }
        /**
         * Get the D for the plane equation Ax+By+C+D=0 of this plane
         * @return double the value of D
         */
        public float getC()
        {
                return C;
        }
        /**
         * Get the D for the plane equation Ax+By+C+D=0 of this plane
         * @return double the value of D
         */
        public float getD()
        {
                return D;
        }
        /**
         * Set the D for the plane equation Ax+By+C+D=0 of this plane
         * @param d double the value of D
         */
        public void setD(float d)
        {
                D = d;
        }
        public String toString()
        {
                return "plane eq: "+A+"x + "+B+"y + "+C+"z + "+D+" = 0";
        }
}

