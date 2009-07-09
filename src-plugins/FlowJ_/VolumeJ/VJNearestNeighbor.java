package VolumeJ;
import volume.*;

/**
 * This class is the standard interpolator and
 * implements nearest neighbor interpolation and interpolation of gradients.
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
public class VJNearestNeighbor extends VJInterpolator
{
        /**
         *  Does vl fall within the bounds of volume for a nearest neighbor interpolation?
         *  @param vl a VJVoxelLoc for which you want to know whether it falls inside the bounds,
         *  taking account of support.
         *  @param v the volume to be interpolated.
         *  @return boolean whether or not vl falls within the bounds.
        */
        public boolean isValid(VJVoxelLoc vl, Volume v)
        {
                return (vl.ix >= 0 && vl.ix < v.getWidth() &&
                        vl.iy >= 0 && vl.iy < v.getHeight() && vl.iz >= 0 && vl.iz < v.getDepth());
        }
        /**
         *  Does vl fall within the bounds of volume for nearest neighbor gradient interpolation?
         *  @param vl a VJVoxelLoc for which you want to know whether it falls inside the bounds,
         *  taking account of support for the gradient kernel.
         *  @param v the volume to be interpolated.
         *  @return boolean whether or not vl falls within the bounds.
        */
        public boolean isValidGradient(VJVoxelLoc vl, Volume v)
        {
                return (vl.ix-1 >= 0 && vl.ix+1 < v.getWidth() &&
                        vl.iy-1 >= 0 && vl.iy+1 < v.getHeight() && vl.iz-1 >= 0 && vl.iz+1 < v.getDepth());
        }
        /**
         *  Does cell c fall within the bounds of a volume for nearest neighbor gradient interpolation?
         *  @param c a VJCell for which you want to know whether it falls inside the bounds,
         *  taking account of support for the gradient kernel.
         *  @param v the volume to be interpolated.
         *  @param boolean whether or not c falls within the bounds.
        */
        public boolean isValidGradient(VJCell c, Volume v)
        {
                return (c.ix-1 >= 0 && c.ix+1 < v.getWidth() &&
                        c.iy-1 >= 0 && c.iy+1 < v.getHeight() && c.iz-1 >= 0 && c.iz+1 < v.getDepth());
        }
		/**
		 * Interpolate a volume. This method can be overloaded in subclasses
		 * for different interpolation algorithms.
         * Interpolate the value of v at location vl.
		 * voxel must be instantiated as a (sub)class of VJValue.
		 * @param voxel a VJValue which will contain the interpolated voxel value on exit.
		 * @param v a volume
         * @param vl a location in the volume.
         * @return voxel, which contains the value in v at vl.
		 */
		public VJValue value(VJValue voxel, Volume v, VJVoxelLoc vl)
        {
                if (v instanceof VolumeShort)
                {
                        if (((VolumeShort) v).getIndexed())
                        {
                                /*
                                * Interpolate in a short volume but only look at the lowest 8 bits.
                                * The 8 highest bits are the index.
                                * The index value is not interpolated, but determined nearest neighbor wise.
                                */
                                voxel.intvalue = value(((VolumeShort) v).v, 0x00ff, vl);
                                voxel.index = ((((VolumeShort) v).v[vl.iz][vl.iy][vl.ix]&0x0000ff00)>>8);
                        }
                        else
                                voxel.intvalue = value(((VolumeShort) v).v, vl);
                        voxel.floatvalue = (float) voxel.intvalue;
                }
				else if (v instanceof VolumeFloat)
						voxel.floatvalue = value(((VolumeFloat) v).v, vl);
                else if (v instanceof VolumeRGB)
                        voxel.floatvalue = value(((VolumeRGB) v).b, v.getHeight(), v.getWidth(), vl);
				else
				{
						VJUserInterface.error("unknown Volume type v");
						return null;
				}
				voxel.intvalue = (int) voxel.floatvalue;
                return voxel;
        }
        /**
         * Compute an interpolated gradient from a volume.
         * The called methods can be overloaded in subclasses for different interpolation algorithms.
         * @param v the volume.
         * @param vl the VJVoxelLoc where to interpolate the gradient
         * @return a VJGradient with the interpolated value(s).
         */
        public VJGradient gradient(Volume v, VJVoxelLoc vl)
        {
                if (v instanceof VolumeShort)
                        return gradient(((VolumeShort) v).v, vl);
                else if (v instanceof VolumeFloat)
                        return gradient(((VolumeFloat) v).v, vl);
				else if (v instanceof VolumeRGB)
						return gradient(((VolumeRGB) v).b, v.getHeight(), v.getWidth(), vl);
                else
                {
                        VJUserInterface.error("unknown Volume type v");
                        return null;
                }
        }
        /**
         * Interpolate an entry in a float vector volume.
         * @param v the vector volume in which to interpolate
         * @param vl the location where to interpolate.
         * @param i the entry in the vector to interpolate.
         * @return the interpolated value
         */
        protected static float value(float [][][][] v, VJVoxelLoc vl, int i)
        {
                return value(v[i], vl);
        }
        /**
         * Interpolate in a float volume.
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the interpolated value
         */
        protected static float value(float [][][] v, VJVoxelLoc vl)
        {
              return v[vl.getnnz()][vl.getnny()][vl.getnnx()];
        }
        /**
         * Interpolate in a short volume.
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the interpolated value
         */
        protected static int value(short [][][] v, VJVoxelLoc vl)
        {
              return (int) v[vl.getnnz()][vl.getnny()][vl.getnnx()];
        }
		/**
		 * Interpolate in a byte volume organized as a single array.
		 * @param v the volume array in which to interpolate
		 * @param height the height of the volume.
		 * @param width the width of the volume.
		 * @param vl the location where to interpolate.
		 * @return the interpolated value
		 */
		protected static float value(byte [] v, int height, int width, VJVoxelLoc vl)
		{
				return (int) (v[vl.getnnz()*height*width+vl.getnny()*width+vl.getnnx()]&0xff);
		}
        /**
         * Interpolate in a short volume using a mask.
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the interpolated value.
         */
        private static int value(short [][][] v, int mask, VJVoxelLoc vl)
        {
              return (int) (v[vl.getnnz()][vl.getnny()][vl.getnnx()]&mask);
        }
		/**
		 * Interpolate hue and saturation in an RGB int[][] volume organized as an array of slices.
		 * @param hs a float[2] that will contain the hue and saturation [0-1] on return.
		 * @param v the volume array in which to interpolate
		 * @param width the width of the volume.
		 * @param vl the location where to interpolate.
		 * @return hs, the interpolated hue and saturation at vl.
		 */
		protected static float [] valueHS(float [] hs, Object [] sliceArray, int width, VJVoxelLoc vl)
		{
				int [] slice0 = (int []) sliceArray[vl.getnnz()];
				int vv = slice0[vl.getnny()*width+vl.getnnx()];
				float [] hsb = new float[3];
				VolumeRGB.intToHSB(hsb, vv);
				hs[0] = hsb[0];
				hs[1] = hsb[1];
				return hs;
		}
        /**
         * Interpolate the gradient *xyz* in a short volume (central difference).
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the VJGradient containing the 3 dimensional vector of the gradient.
         */
        protected static VJGradient gradient(short [][][] v, VJVoxelLoc vl)
        {
                double gx =((int)v[vl.getnnz()][vl.getnny()][vl.getnnx()-1]&0xffff)
					   -((int)v[vl.getnnz()][vl.getnny()][vl.getnnx()+1]&0xffff);
                double gy =((int)v[vl.getnnz()][vl.getnny()-1][vl.getnnx()]&0xffff)
					   -((int)v[vl.getnnz()][vl.getnny()+1][vl.getnnx()]&0xffff);
                double gz =((int)v[vl.getnnz()-1][vl.getnny()][vl.getnnx()]&0xffff)
					   -((int)v[vl.getnnz()+1][vl.getnny()][vl.getnnx()]&0xffff);
                VJGradient g = new VJGradient(gx, gy, gz);
                return g;
        }
        /**
         * Interpolate the gradient *xyz* in a float volume (central difference).
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the VJGradient containing the 3 dimensional vector of the gradient.
         */
        protected static VJGradient gradient(float [][][] v, VJVoxelLoc vl)
        {
                double gx =v[vl.getnnz()][vl.getnny()][vl.getnnx()-1]
					   -v[vl.getnnz()][vl.getnny()][vl.getnnx()+1];
                double gy =v[vl.getnnz()][vl.getnny()-1][vl.getnnx()]
					   -v[vl.getnnz()][vl.getnny()+1][vl.getnnx()];
                double gz =v[vl.getnnz()-1][vl.getnny()][vl.getnnx()]
					   -v[vl.getnnz()+1][vl.getnny()][vl.getnnx()];
                VJGradient g = new VJGradient(gx, gy, gz);
                return g;
        }
		 /**
		 * Interpolate the gradient *xyz* in a byte volume.
		 * @param v the volume in which to interpolate
		 * @param vl the location where to interpolate.
		 * @return the VJGradient containing the 3 dimensional vector of the gradient.
		 */
		protected static VJGradient gradient(byte [] v, int height, int width, VJVoxelLoc vl)
		{
				double gx = (((int)v[vl.getnnz()*height*width+vl.getnny()*width+(vl.getnnx()-1)]&0xff)-((int)v[vl.getnnz()*height*width+vl.getnny()*width+(vl.getnnx()+1)]&0xff));
				double gy = (((int)v[vl.getnnz()*height*width+(vl.getnny()-1)*width+vl.getnnx()]&0xff)-((int)v[vl.getnnz()*height*width+(vl.getnny()+1)*width+vl.getnnx()]&0xff));
				double gz =	(((int)v[(vl.getnnz()-1)*height*width+vl.getnny()*width+vl.getnnx()]&0xff)-((int)v[(vl.getnnz()+1)*height*width+vl.getnny()*width+vl.getnnx()]&0xff));
				VJGradient g = new VJGradient(gx, gy, gz);
				return g;
		}
        /**
         * Interpolate in a float 4D hypervolume in a single dimension.
         * @param v the volume in which to interpolate
         * @param dimension the dimension in which to interpolate.
         * @param vl the location where to interpolate.
         * @return the interpolated value.
         */
        protected  static float value(float [][][][] v, int dimension, VJVoxelLoc vl)
        {
                return v[vl.getnnz()][vl.getnny()][vl.getnnx()][dimension];
        }
        public String toString() { return " nearest neighbor "; }
}
