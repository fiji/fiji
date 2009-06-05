package VolumeJ;
import volume.*;

/**
 * This class defines a voxel location for interpolation and rendering.
 * It is useful because it keeps both the integer part of the voxel location (nearest neighbor voxel),
 * and the weights to each of the
 * eight surrounding voxels that can be used for higher order interpolation.
 * Voxel locations are always in object space.
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
public class VJVoxelLoc
{
        /** integer floor of voxel location. */
        public int ix;
        public int iy;
        public int iz;
        public float x;
        public float y;
        public float z;
        /** Difference between ix/iy/iz and x/y/z [0-1]. */
        public float dx;
        public float dy;
        public float dz;
        /** trilinear interpolation weights. */
        public float tlf;
        public float blf;
        public float trf;
        public float brf;
        public float tlb;
        public float blb;
        public float trb;
        public float brb;
        public boolean hasWeights = false;

        /**
         * Empty VJVoxelLoc creator.
         */
        public VJVoxelLoc() {}
        /**
         * Create a new VJVoxelLoc for an objectspace location x,y,z.
         * Voxel coordinates are always in object space.
         * @param x,y,z float the object space location.
         */
        public VJVoxelLoc(float x, float y, float z)
        {
                init(x, y, z);
        }
        /**
         * Create a new VJVoxelLoc from a transformed vector location.
         * Voxel coordinates are always in object space.
         * @param vs float[4] the location vector (homogenous).
         * @param m the the transformation matrix.
         */
        public VJVoxelLoc(float [] vs, VJMatrix m)
        {
                // Transform the input vector.
                float [] tv = m.mul(vs);
                init(tv[0], tv[1], tv[2]);
        }
        /**
         * Create a new VJVoxelLoc from a transformed vector location.
         * @param x, y, z the location vector in un-transformed coordinates.
         * @param m the transformation matrix.
         */
        public VJVoxelLoc(float x, float y, float z, VJMatrix m)
        {
                float [] v = new float[4];
                // Make a coordinates vector.
                v[0] = x; v[1] = y; v[2] = z; v[3] = 1;
                // Transform the input vector.
                float [] tv = m.mul(v);
                init((float) tv[0], (float) tv[1], (float) tv[2]);
        }
        /**
         * Create a new VJVoxelLoc from a vector.
         * @param v the location vector (homogenous).
         */
        public VJVoxelLoc(float [] v)
        {
                init((float) v[0], (float) v[1], (float) v[2]);
        }
        private void init(float x, float y, float z)
        {
                this.x = x;
                this.y = y;
                this.z = z;
                ix = (int) this.x;
                iy = (int) this.y;
                iz = (int) this.z;
        }
        /**
		 * Get the x-position.
		 * @return a float with the x position.
		*/
        public float getx() { return (float) this.x; }
        /**
		 * Get the y-position.
		 * @return a float with the x position.
		*/
        public float gety() { return (float) this.y; }
        /**
		 * Get the z-position.
		 * @return a float with the x position.
		*/
        public float getz() { return (float) this.z; }
        /**
		 * Get the x-position rounded to the nearest int for nearest neighborhood interpolation.
		 * @return an int with the closest integer x position.
		 */
        public int getnnx() { return (int) Math.round(this.x); }
        /**
		 * Get the y-position rounded to the nearest int for nearest neighborhood interpolation.
		 * @return an int with the closest integer y position.
		 */
        public int getnny() { return (int) Math.round(this.y); }
        /**
		 * Get the z-position rounded to the nearest int for nearest neighborhood interpolation.
		 * @return an int with the closest integer z position.
		 */
        public int getnnz() { return (int) Math.round(this.z); }
        /**
         * Set the value for z.
         * @param z the value to set.
         */
        public void setz(float z) { this.z = (float) z; iz = (int) z; }
        /**
         * Set the value for x.
         * @param x an int with the x value.
         */
        public void setx(int x) { this.x = (float) x; ix = x; }
        /**
         * Set the value for x.
         * @param y an int with the y value.
         */
        public void sety(int y) { this.y = (float) y; iy = y; }
        /**
         * Set the value for z.
         * @param z an int with the z value.
         */
        public void setz(int z) { this.z = (float) z; iz = z; }
        public void getWeights()
        {
                // Optimization.
                if (hasWeights)
                        return;
                hasWeights = true;
                dx = x - (float)ix;
                dy = y - (float)iy;
                dz = z - (float)iz;
                // Compute the interpolation coefficients (trilinear).
                tlf =  (1-dx) * (1-dy) * (1-dz);  // top left front
                blf =  (1-dx) * dy * (1-dz);  // bottom left front
                trf =  dx * (1-dy) * (1-dz);  // top right front
                brf = dx * dy * (1-dz);      // bottom right front
                brb = dx * dy * dz;          // bottom right back
                trb = dx * (1-dy) * dz;      // top right back
                blb = (1-dx) * dy * dz;      // bottom left back
                tlb = (1-dx) * (1-dy) * dz;  // top left back
        }
        /**
         * Move the location of this voxel with delta's in dos.
         * Useful for stepping through a number of voxels on a straight line.
         * @param dos float[>=3] a vector containing the motion.
         */
        public void move(float [] dos)
        {
                x += dos[0]; y += dos[1]; z += dos[2];
                ix = (int) x;
                iy = (int) y;
                iz = (int) z;
                hasWeights = false;
        }
        /**
         * Move the location of this voxel with delta's in dos.
         * Useful for stepping through a number of voxels on a straight line.
         * @param dos float[>=3] a vector containing the motion.
         */
        public void move(double dx, double dy, double dz)
        {
                x += dx; y += dy; z += dz;
                ix = (int) x;
                iy = (int) y;
                iz = (int) z;
                hasWeights = false;
        }
        public String toString()
        {
                String s = "VJVoxelLoc: "+x+","+y+","+z+": (voxel) "+ix+","+iy+","+iz+
					": (wghts) "+
					tlf+","+
					blf+","+
					trf+","+
					brf+","+
					tlb+","+
					blb+","+
					trb+","+
					brb+",";
                return s;
        }
}
