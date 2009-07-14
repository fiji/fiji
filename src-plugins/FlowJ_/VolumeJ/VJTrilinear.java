package VolumeJ;
import volume.*;

/**
 * This class implements trilinear voxel interpolation and voxel gradient interpolation.
 * It operates on volumes formatted as datatype[][][], datatype[][] and datatype[].
 * This class depends on the volume definitions in VolumeShort, VolumeRGB and VolumeFloat.
 *
 * @see VolumeShort
 * @see VolumeFloat
 * @see VolumeRGB
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
public class VJTrilinear extends VJInterpolator
{
        /**
         *  Does vl fall within the bounds of a volume for interpolation?
         *  @param vl a VJVoxelLoc for which you want to know whether it falls inside the bounds,
         *  taking account of support.
         *  @param startx ... endz the bounds of the volume.
         *  @param boolean whether or not vl falls within the bounds.
        */
        public boolean isValid(VJVoxelLoc vl, Volume v)
        {
                return (vl.ix >= 0 && vl.ix+1 < v.getWidth() &&
                        vl.iy >= 0 && vl.iy+1 < v.getHeight() && vl.iz >= 0 && vl.iz+1 < v.getDepth());
        }
        /**
         *  Does vl fall within the bounds of a volume for trilinear gradient interpolation?
         *  @param vl a VJVoxelLoc for which you want to know whether it falls inside the bounds,
         *  taking account of support for the gradient kernel.
         *  @param v the volume.
         *  @param boolean whether or not vl falls within the bounds.
        */
        public boolean isValidGradient(VJVoxelLoc vl, Volume v)
        {
                return (vl.ix-1 >= 0 && vl.ix+2 < v.getWidth() &&
                        vl.iy-1 >= 0 && vl.iy+2 < v.getHeight() && vl.iz-1 >= 0 && vl.iz+2 < v.getDepth());
        }
        /**
         *  Does cell c fall within the bounds of a volume for nearest neighbor gradient interpolation?
         *  Bounds are -2...+3 of the cell location (translates to -2...+2 for the voxel location).
         *  If you can use bilinear interpolation of the anterior and posterior faces of a cell,
         *  use -1...+2.
         *  @param c a VJCell for which you want to know whether it falls inside the bounds,
         *  taking account of support for the gradient kernel.
         *  @param v the volume.
         *  @param boolean whether or not c falls within the bounds.
        */
        public boolean isValidGradient(VJCell c, Volume v)
        {
                // Some compilers give problems with combining the below.
                boolean p = (c.ix >= 2) && (c.ix < v.getWidth()-3);
                boolean q = (c.iy >= 2) && (c.iy < v.getHeight()-3);
                boolean r = (c.iz >= 2) && (c.iz < v.getDepth()-3);
                return (p && q && r);
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
                                voxel.floatvalue = value(((VolumeShort) v).v, 0x000000ff, vl);
                                voxel.index = ((((VolumeShort) v).v[vl.iz][vl.iy][vl.ix]&0x0000ff00)>>8);
                        }
                        else
                                voxel.floatvalue = value(((VolumeShort) v).v, vl);
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
                {
                        if (((VolumeShort) v).getIndexed())
                                return gradient(((VolumeShort) v).v, 0x000000ff, vl);
                        else
                                return gradient(((VolumeShort) v).v, vl);
                }
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
         * Interpolate in a float volume.
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the interpolated value
         */
        protected static float value(float [][][] v, VJVoxelLoc vl)
        {
                // compute the interpolation deltas of the object coordinates
                vl.getWeights();
                return
                        v[vl.iz][vl.iy][vl.ix] * vl.tlf +       // TLF
                        v[vl.iz][vl.iy+1][vl.ix] * vl.blf +     // BLF
                        v[vl.iz][vl.iy][vl.ix+1] * vl.trf +     // TRF
                        v[vl.iz][vl.iy+1][vl.ix+1] * vl.brf +  // BRF
                        v[vl.iz+1][vl.iy][vl.ix] * vl.tlb +    // TLB
                        v[vl.iz+1][vl.iy+1][vl.ix] * vl.blb +  // BLB
                        v[vl.iz+1][vl.iy][vl.ix+1] * vl.trb +  // TRB
                        v[vl.iz+1][vl.iy+1][vl.ix+1] * vl.brb; // BRB
        }
        /**
         * Interpolate in a short volume.
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the interpolated value
         */
        protected static float value(short [][][] v, VJVoxelLoc vl)
        {
                // compute the interpolation deltas of the object coordinates
                vl.getWeights();
                float f =
                        (((v[vl.iz][vl.iy][vl.ix]&0xffff) * vl.tlf +            // TLF
                        (v[vl.iz][vl.iy+1][vl.ix]&0xffff) * vl.blf +             // BLF
                        (v[vl.iz][vl.iy][vl.ix+1]&0xffff) * vl.trf +             // TRF
                        (v[vl.iz][vl.iy+1][vl.ix+1]&0xffff) * vl.brf +           // BRF
                        (v[vl.iz+1][vl.iy][vl.ix]&0xffff) * vl.tlb +             // TLB
                        (v[vl.iz+1][vl.iy+1][vl.ix]&0xffff) * vl.blb +           // BLB
                        (v[vl.iz+1][vl.iy][vl.ix+1]&0xffff) * vl.trb +           // TRB
                        (v[vl.iz+1][vl.iy+1][vl.ix+1]&0xffff) * vl.brb));// BRB
                //VJUserInterface.write("interpolating at "+vl+" v[iz][iy][ix]="+v[vl.iz][vl.iy][vl.ix]+" = "+f);
                return f;
        }
        /**
         * Interpolate in a short volume using a mask, which is ANDed with the voxel value.
         * @param v the volume in which to interpolate
         * @param mask the mask bits
         * @param vl the location where to interpolate.
         * @return the interpolated value.
         */
        private static int value(short [][][] v, int mask, VJVoxelLoc vl)
        {
                // compute the interpolation deltas of the object coordinates
                vl.getWeights();
                return
                        ((int) ((v[vl.iz][vl.iy][vl.ix]&mask) * vl.tlf +        // TLF
                        (v[vl.iz][vl.iy+1][vl.ix]&mask) * vl.blf +              // BLF
                        (v[vl.iz][vl.iy][vl.ix+1]&mask) * vl.trf +              // TRF
                        (v[vl.iz][vl.iy+1][vl.ix+1]&mask) * vl.brf +            // BRF
                        (v[vl.iz+1][vl.iy][vl.ix]&mask) * vl.tlb +              // TLB
                        (v[vl.iz+1][vl.iy+1][vl.ix]&mask) * vl.blb +            // BLB
                        (v[vl.iz+1][vl.iy][vl.ix+1]&mask) * vl.trb +            // TRB
                        (v[vl.iz+1][vl.iy+1][vl.ix+1]&mask) * vl.brb));         // BRB
        }
        /**
         * Interpolate in a float 4D hypervolume in a single dimension.
         * @param v the volume in which to interpolate
         * @param dimension the dimension in which to interpolate.
         * @param vl the location where to interpolate.
         * @return the interpolated value.
         */
        protected static float value(float [][][][] v, int dimension, VJVoxelLoc vl)
        {
                // compute the interpolation deltas of the object coordinates
                vl.getWeights();
                return
                        v[vl.iz][vl.iy][vl.ix][dimension] * vl.tlf +       // TLF
                        v[vl.iz][vl.iy+1][vl.ix][dimension] * vl.blf +     // BLF
                        v[vl.iz][vl.iy][vl.ix+1][dimension] * vl.trf +     // TRF
                        v[vl.iz][vl.iy+1][vl.ix+1][dimension] * vl.brf +  // BRF
                        v[vl.iz+1][vl.iy][vl.ix][dimension] * vl.tlb +    // TLB
                        v[vl.iz+1][vl.iy+1][vl.ix][dimension] * vl.blb +  // BLB
                        v[vl.iz+1][vl.iy][vl.ix+1][dimension] * vl.trb +  // TRB
                        v[vl.iz+1][vl.iy+1][vl.ix+1][dimension] * vl.brb; // BRB
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
                vl.getWeights();
                return (int)
                   ((v[vl.iz*height*width+vl.iy*width + vl.ix]&0xff) * vl.tlf +       // TLF
                        (v[vl.iz*height*width+(vl.iy+1)*width + vl.ix]&0xff) * vl.blf +     // BLF
                        (v[vl.iz*height*width+(vl.iy)*width + (vl.ix+1)]&0xff) * vl.trf +     // TRF
                        (v[vl.iz*height*width+(vl.iy+1)*width + (vl.ix+1)]&0xff) * vl.brf +  // BRF
                        (v[(vl.iz+1)*height*width+(vl.iy)*width + (vl.ix)]&0xff) * vl.tlb +    // TLB
                        (v[(vl.iz+1)*height*width+(vl.iy+1)*width + (vl.ix)]&0xff) * vl.blb +  // BLB
                        (v[(vl.iz+1)*height*width+(vl.iy)*width + (vl.ix+1)]&0xff) * vl.trb +  // TRB
                        (v[(vl.iz+1)*height*width+(vl.iy+1)*width + (vl.ix+1)]&0xff) * vl.brb); // BRB
        }
        /**
         * Interpolate hue and saturation in an RGB int volume organized as an array of slices.
         * Interpolation of hue and saturation are not very succesful, probably because
         * hue is an angle, and empty voxels default to hue=0.
         * @param hs a float[2] that will contain the interpolated hue and saturation on return.
         * @param sliceArray the volume slice array in which to interpolate
         * @param width the width of the volume.
         * @param vl the location where to interpolate.
         * @return the interpolated value
         */
        protected static float [] valueHS(float [] hs, Object [] sliceArray, int width, VJVoxelLoc vl)
        {
                vl.getWeights();
                int [] slice0 = (int []) sliceArray[vl.iz];
                int [] slice1 = (int []) sliceArray[vl.iz+1];
                int vtlf = slice0[(vl.iy)*width + (vl.ix)];
                int vblf = slice0[(vl.iy+1)*width + (vl.ix)];
                int vtrf = slice0[(vl.iy)*width + (vl.ix+1)];
                int vbrf = slice0[(vl.iy+1)*width + (vl.ix+1)];
                int vtlb = slice1[(vl.iy)*width + (vl.ix)];
                int vblb = slice1[(vl.iy+1)*width + (vl.ix)];
                int vtrb = slice1[(vl.iy)*width + (vl.ix+1)];
                int vbrb = slice1[(vl.iy+1)*width + (vl.ix+1)];
                // Optimized to avoid allocating too many objects.
                float [] hsb = new float[3];
                hs[0] = 0; hs[1] = 0;
                VolumeRGB.intToHSB(hsb, vtlf);
                hs[0] += hsb[0] * vl.tlf;
                hs[1] += hsb[1] * vl.tlf;
                VolumeRGB.intToHSB(hsb, vblf);
                hs[0] += hsb[0] * vl.blf;
                hs[1] += hsb[1] * vl.blf;
                VolumeRGB.intToHSB(hsb, vtrf);
                hs[0] += hsb[0] * vl.trf;
                hs[1] += hsb[1] * vl.trf;
                VolumeRGB.intToHSB(hsb, vbrf);
                hs[0] += hsb[0] * vl.brf;
                hs[1] += hsb[1] * vl.brf;
                VolumeRGB.intToHSB(hsb, vtlb);
                hs[0] += hsb[0] * vl.tlb;
                hs[1] += hsb[1] * vl.tlb;
                VolumeRGB.intToHSB(hsb, vblb);
                hs[0] += hsb[0] * vl.blb;
                hs[1] += hsb[1] * vl.blb;
                VolumeRGB.intToHSB(hsb, vtrb);
                hs[0] += hsb[0] * vl.trb;
                hs[1] += hsb[1] * vl.trb;
                VolumeRGB.intToHSB(hsb, vbrb);
                hs[0] += hsb[0] * vl.brb;
                hs[1] += hsb[1] * vl.brb;

                //VolumeRGB.intToHSB(hsb, vtlf);
                //hs[0] = hsb[0];
                //hs[0] = 1;

                return hs;
        }
        /**
         * Interpolate the gradient *xyz* in a float volume.
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the VJGradient containing the 3 dimensional vector of the gradient.
         */
        protected static VJGradient gradient(float [][][] v, VJVoxelLoc vl)
        {
                // compute the interpolation deltas of the object coordinates
                vl.getWeights();
                double gx =
                        (v[vl.iz][vl.iy][vl.ix-1]-v[vl.iz][vl.iy][vl.ix+1]) * vl.tlf +      // TLF
                        (v[vl.iz][vl.iy+1][vl.ix-1]-v[vl.iz][vl.iy+1][vl.ix+1]) * vl.blf +  // BLF
                        (v[vl.iz][vl.iy][vl.ix]-v[vl.iz][vl.iy][vl.ix+2]) * vl.trf +        // TRF
                        (v[vl.iz][vl.iy+1][vl.ix]-v[vl.iz][vl.iy+1][vl.ix+2]) * vl.brf +   // BRF
                        (v[vl.iz+1][vl.iy][vl.ix-1]-v[vl.iz+1][vl.iy][vl.ix+1]) * vl.tlb +     // TLB
                        (v[vl.iz+1][vl.iy+1][vl.ix-1]-v[vl.iz+1][vl.iy+1][vl.ix+1]) * vl.blb + // BLB
                        (v[vl.iz+1][vl.iy][vl.ix]-v[vl.iz+1][vl.iy][vl.ix+2]) * vl.trb +       // TRB
                        (v[vl.iz+1][vl.iy+1][vl.ix]-v[vl.iz+1][vl.iy+1][vl.ix+2]) * vl.brb;    // BRB
                double gy =
                        (v[vl.iz][vl.iy-1][vl.ix]-v[vl.iz][vl.iy+1][vl.ix]) * vl.tlf +      // TLF
                        (v[vl.iz][vl.iy][vl.ix]-v[vl.iz][vl.iy+2][vl.ix]) * vl.blf +        // BLF
                        (v[vl.iz][vl.iy-1][vl.ix+1]-v[vl.iz][vl.iy+1][vl.ix+1]) * vl.trf +  // TRF
                        (v[vl.iz][vl.iy][vl.ix+1]-v[vl.iz][vl.iy+2][vl.ix+1]) * vl.brf +    // BRF
                        (v[vl.iz+1][vl.iy-1][vl.ix]-v[vl.iz+1][vl.iy+1][vl.ix]) * vl.tlb +  // TLB
                        (v[vl.iz+1][vl.iy][vl.ix]-v[vl.iz+1][vl.iy+2][vl.ix]) * vl.blb +    // BLB
                        (v[vl.iz+1][vl.iy-1][vl.ix+1]-v[vl.iz+1][vl.iy+1][vl.ix+1]) * vl.trb + // TRB
                        (v[vl.iz+1][vl.iy][vl.ix+1]-v[vl.iz+1][vl.iy+2][vl.ix+1]) * vl.brb;  // BRB
                double gz =
                        (v[vl.iz-1][vl.iy][vl.ix]-v[vl.iz+1][vl.iy][vl.ix]) * vl.tlf +       // TLF
                        (v[vl.iz-1][vl.iy+1][vl.ix]-v[vl.iz+1][vl.iy+1][vl.ix]) * vl.blf +   // BLF
                        (v[vl.iz-1][vl.iy][vl.ix+1]-v[vl.iz+1][vl.iy][vl.ix+1]) * vl.trf +   // TRF
                        (v[vl.iz-1][vl.iy+1][vl.ix+1]-v[vl.iz+1][vl.iy+1][vl.ix+1]) * vl.brf +  // BRF
                        (v[vl.iz][vl.iy][vl.ix]-v[vl.iz+2][vl.iy][vl.ix]) * vl.tlb +         // TLB
                        (v[vl.iz][vl.iy+1][vl.ix]-v[vl.iz+2][vl.iy+1][vl.ix]) * vl.blb +     // BLB
                        (v[vl.iz][vl.iy][vl.ix+1]-v[vl.iz+2][vl.iy][vl.ix+1]) * vl.trb +     // TRB
                        (v[vl.iz][vl.iy+1][vl.ix+1]-v[vl.iz+2][vl.iy+1][vl.ix+1]) * vl.brb;  // BRB
                VJGradient g = new VJGradient(gx, gy, gz);
                return g;
        }
        /**
         * Interpolate the gradient *xyz* in a short volume.
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the VJGradient containing the 3 dimensional vector of the gradient.
         */
        protected static VJGradient gradient(short [][][] v, VJVoxelLoc vl)
        {
                // compute the interpolation deltas of the object coordinates
                vl.getWeights();
                VJGradient g = null;
                try
                {
                double gx =
                        (((int)v[vl.iz][vl.iy][vl.ix-1]&0xffff)-((int)v[vl.iz][vl.iy][vl.ix+1]&0xffff)) * vl.tlf +      // TLF
                        (((int)v[vl.iz][vl.iy+1][vl.ix-1]&0xffff)-((int)v[vl.iz][vl.iy+1][vl.ix+1]&0xffff)) * vl.blf +  // BLF
                        (((int)v[vl.iz][vl.iy][vl.ix]&0xffff)-((int)v[vl.iz][vl.iy][vl.ix+2]&0xffff)) * vl.trf +        // TRF
                        (((int)v[vl.iz][vl.iy+1][vl.ix]&0xffff)-((int)v[vl.iz][vl.iy+1][vl.ix+2]&0xffff)) * vl.brf +   // BRF
                        (((int)v[vl.iz+1][vl.iy][vl.ix-1]&0xffff)-((int)v[vl.iz+1][vl.iy][vl.ix+1]&0xffff)) * vl.tlb +     // TLB
                        (((int)v[vl.iz+1][vl.iy+1][vl.ix-1]&0xffff)-((int)v[vl.iz+1][vl.iy+1][vl.ix+1]&0xffff)) * vl.blb + // BLB
                        (((int)v[vl.iz+1][vl.iy][vl.ix]&0xffff)-((int)v[vl.iz+1][vl.iy][vl.ix+2]&0xffff)) * vl.trb +       // TRB
                        (((int)v[vl.iz+1][vl.iy+1][vl.ix]&0xffff)-((int)v[vl.iz+1][vl.iy+1][vl.ix+2]&0xffff)) * vl.brb;    // BRB
                double gy =
                        (((int)v[vl.iz][vl.iy-1][vl.ix]&0xffff)-((int)v[vl.iz][vl.iy+1][vl.ix]&0xffff)) * vl.tlf +      // TLF
                        (((int)v[vl.iz][vl.iy][vl.ix]&0xffff)-((int)v[vl.iz][vl.iy+2][vl.ix]&0xffff)) * vl.blf +        // BLF
                        (((int)v[vl.iz][vl.iy-1][vl.ix+1]&0xffff)-((int)v[vl.iz][vl.iy+1][vl.ix+1]&0xffff)) * vl.trf +  // TRF
                        (((int)v[vl.iz][vl.iy][vl.ix+1]&0xffff)-((int)v[vl.iz][vl.iy+2][vl.ix+1]&0xffff)) * vl.brf +    // BRF
                        (((int)v[vl.iz+1][vl.iy-1][vl.ix]&0xffff)-((int)v[vl.iz+1][vl.iy+1][vl.ix]&0xffff)) * vl.tlb +  // TLB
                        (((int)v[vl.iz+1][vl.iy][vl.ix]&0xffff)-((int)v[vl.iz+1][vl.iy+2][vl.ix]&0xffff)) * vl.blb +    // BLB
                        (((int)v[vl.iz+1][vl.iy-1][vl.ix+1]&0xffff)-((int)v[vl.iz+1][vl.iy+1][vl.ix+1]&0xffff)) * vl.trb + // TRB
                        (((int)v[vl.iz+1][vl.iy][vl.ix+1]&0xffff)-((int)v[vl.iz+1][vl.iy+2][vl.ix+1]&0xffff)) * vl.brb;  // BRB
                double gz =
                        (((int)v[vl.iz-1][vl.iy][vl.ix]&0xffff)-((int)v[vl.iz+1][vl.iy][vl.ix]&0xffff)) * vl.tlf +       // TLF
                        (((int)v[vl.iz-1][vl.iy+1][vl.ix]&0xffff)-((int)v[vl.iz+1][vl.iy+1][vl.ix]&0xffff)) * vl.blf +   // BLF
                        (((int)v[vl.iz-1][vl.iy][vl.ix+1]&0xffff)-((int)v[vl.iz+1][vl.iy][vl.ix+1]&0xffff)) * vl.trf +   // TRF
                        (((int)v[vl.iz-1][vl.iy+1][vl.ix+1]&0xffff)-((int)v[vl.iz+1][vl.iy+1][vl.ix+1]&0xffff)) * vl.brf +  // BRF
                        (((int)v[vl.iz][vl.iy][vl.ix]&0xffff)-((int)v[vl.iz+2][vl.iy][vl.ix]&0xffff)) * vl.tlb +         // TLB
                        (((int)v[vl.iz][vl.iy+1][vl.ix]&0xffff)-((int)v[vl.iz+2][vl.iy+1][vl.ix]&0xffff)) * vl.blb +     // BLB
                        (((int)v[vl.iz][vl.iy][vl.ix+1]&0xffff)-((int)v[vl.iz+2][vl.iy][vl.ix+1]&0xffff)) * vl.trb +     // TRB
                        (((int)v[vl.iz][vl.iy+1][vl.ix+1]&0xffff)-((int)v[vl.iz+2][vl.iy+1][vl.ix+1]&0xffff)) * vl.brb;  // BRB
                g = new VJGradient(gx, gy, gz);
                }
                catch (ArrayIndexOutOfBoundsException e) { System.out.println("gradient error: VoxelLoc="+vl+" error="+e); }
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
                // compute the interpolation deltas of the object coordinates
                vl.getWeights();
                double gx =
                        (((int)v[(vl.iz)*height*width+(vl.iy)*width+(vl.ix-1)]&0xff)-((int)v[(vl.iz)*height*width+(vl.iy)*width+(vl.ix+1)]&0xff)) * vl.tlf +           // TLF
                        (((int)v[(vl.iz)*height*width+(vl.iy+1)*width+(vl.ix-1)]&0xff)-((int)v[(vl.iz)*height*width+(vl.iy+1)*width+(vl.ix+1)]&0xff)) * vl.blf +       // BLF
                        (((int)v[(vl.iz)*height*width+(vl.iy)*width+(vl.ix)]&0xff)-((int)v[(vl.iz)*height*width+(vl.iy)*width+(vl.ix+2)]&0xff)) * vl.trf +             // TRF
                        (((int)v[(vl.iz)*height*width+(vl.iy+1)*width+(vl.ix)]&0xff)-((int)v[(vl.iz)*height*width+(vl.iy+1)*width+(vl.ix+2)]&0xff)) * vl.brf +         // BRF
                        (((int)v[(vl.iz+1)*height*width+(vl.iy)*width+(vl.ix-1)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy)*width+(vl.ix+1)]&0xff)) * vl.tlb +   // TLB
                        (((int)v[(vl.iz+1)*height*width+(vl.iy+1)*width+(vl.ix-1)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy+1)*width+(vl.ix+1)]&0xff)) * vl.blb +// BLB
                        (((int)v[(vl.iz+1)*height*width+(vl.iy)*width+(vl.ix)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy)*width+(vl.ix+2)]&0xff)) * vl.trb +     // TRB
                        (((int)v[(vl.iz+1)*height*width+(vl.iy+1)*width+(vl.ix)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy+1)*width+(vl.ix+2)]&0xff)) * vl.brb; // BRB
                // y
                double gy =
                        (((int)v[(vl.iz)*height*width+(vl.iy-1)*width+(vl.ix)]&0xff)-((int)v[(vl.iz)*height*width+(vl.iy+1)*width+(vl.ix)]&0xff)) * vl.tlf +           // TLF
                        (((int)v[(vl.iz)*height*width+(vl.iy)*width+(vl.ix)]&0xff)-((int)v[(vl.iz)*height*width+(vl.iy+2)*width+(vl.ix)]&0xff)) * vl.blf +       // BLF
                        (((int)v[(vl.iz)*height*width+(vl.iy-1)*width+(vl.ix+1)]&0xff)-((int)v[(vl.iz)*height*width+(vl.iy+1)*width+(vl.ix+1)]&0xff)) * vl.trf +             // TRF
                        (((int)v[(vl.iz)*height*width+(vl.iy)*width+(vl.ix+1)]&0xff)-((int)v[(vl.iz)*height*width+(vl.iy+2)*width+(vl.ix+1)]&0xff)) * vl.brf +         // BRF
                        (((int)v[(vl.iz+1)*height*width+(vl.iy-1)*width+(vl.ix)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy+1)*width+(vl.ix)]&0xff)) * vl.tlb +   // TLB
                        (((int)v[(vl.iz+1)*height*width+(vl.iy)*width+(vl.ix)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy+2)*width+(vl.ix)]&0xff)) * vl.blb +// BLB
                        (((int)v[(vl.iz+1)*height*width+(vl.iy-1)*width+(vl.ix+1)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy+1)*width+(vl.ix+1)]&0xff)) * vl.trb +     // TRB
                        (((int)v[(vl.iz+1)*height*width+(vl.iy)*width+(vl.ix+1)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy+2)*width+(vl.ix+1)]&0xff)) * vl.brb; // BRB
                // z
                double gz =
                        (((int)v[(vl.iz-1)*height*width+(vl.iy)*width+(vl.ix)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy)*width+(vl.ix)]&0xff)) * vl.tlf +           // TLF
                        (((int)v[(vl.iz-1)*height*width+(vl.iy+1)*width+(vl.ix)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy+1)*width+(vl.ix)]&0xff)) * vl.blf +       // BLF
                        (((int)v[(vl.iz-1)*height*width+(vl.iy)*width+(vl.ix+1)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy)*width+(vl.ix+1)]&0xff)) * vl.trf +             // TRF
                        (((int)v[(vl.iz-1)*height*width+(vl.iy+1)*width+(vl.ix+1)]&0xff)-((int)v[(vl.iz+1)*height*width+(vl.iy+1)*width+(vl.ix+1)]&0xff)) * vl.brf +         // BRF
                        (((int)v[(vl.iz)*height*width+(vl.iy)*width+(vl.ix)]&0xff)-((int)v[(vl.iz+2)*height*width+(vl.iy)*width+(vl.ix)]&0xff)) * vl.tlb +   // TLB
                        (((int)v[(vl.iz)*height*width+(vl.iy+1)*width+(vl.ix)]&0xff)-((int)v[(vl.iz+2)*height*width+(vl.iy+1)*width+(vl.ix)]&0xff)) * vl.blb +// BLB
                        (((int)v[(vl.iz)*height*width+(vl.iy)*width+(vl.ix+1)]&0xff)-((int)v[(vl.iz+2)*height*width+(vl.iy)*width+(vl.ix+1)]&0xff)) * vl.trb +     // TRB
                        (((int)v[(vl.iz)*height*width+(vl.iy+1)*width+(vl.ix+1)]&0xff)-((int)v[(vl.iz+2)*height*width+(vl.iy+1)*width+(vl.ix+1)]&0xff)) * vl.brb; // BRB
                VJGradient g = new VJGradient(gx, gy, gz);
                return g;
        }
         /**
         * Interpolate the gradient *xyz* in a short volume with a mask.
         * @param v the volume in which to interpolate
         * @param vl the location where to interpolate.
         * @return the VJGradient containing the 3 dimensional vector of the gradient.
         */
        protected static VJGradient gradient(short [][][] v, int mask, VJVoxelLoc vl)
        {
                // compute the interpolation deltas of the object coordinates
                vl.getWeights();
                double gx, gy, gz;
                // x
                gx =
                        (((int)v[vl.iz][vl.iy][vl.ix-1]&mask)-((int)v[vl.iz][vl.iy][vl.ix+1]&mask)) * vl.tlf +      // TLF
                        (((int)v[vl.iz][vl.iy+1][vl.ix-1]&mask)-((int)v[vl.iz][vl.iy+1][vl.ix+1]&mask)) * vl.blf +  // BLF
                        (((int)v[vl.iz][vl.iy][vl.ix]&mask)-((int)v[vl.iz][vl.iy][vl.ix+2]&mask)) * vl.trf +        // TRF
                        (((int)v[vl.iz][vl.iy+1][vl.ix]&mask)-((int)v[vl.iz][vl.iy+1][vl.ix+2]&mask)) * vl.brf +   // BRF
                        (((int)v[vl.iz+1][vl.iy][vl.ix-1]&mask)-((int)v[vl.iz+1][vl.iy][vl.ix+1]&mask)) * vl.tlb +     // TLB
                        (((int)v[vl.iz+1][vl.iy+1][vl.ix-1]&mask)-((int)v[vl.iz+1][vl.iy+1][vl.ix+1]&mask)) * vl.blb + // BLB
                        (((int)v[vl.iz+1][vl.iy][vl.ix]&mask)-((int)v[vl.iz+1][vl.iy][vl.ix+2]&mask)) * vl.trb +       // TRB
                        (((int)v[vl.iz+1][vl.iy+1][vl.ix]&mask)-((int)v[vl.iz+1][vl.iy+1][vl.ix+2]&mask)) * vl.brb;    // BRB
                // y
                gy =
                        (((int)v[vl.iz][vl.iy-1][vl.ix]&mask)-((int)v[vl.iz][vl.iy+1][vl.ix]&mask)) * vl.tlf +      // TLF
                        (((int)v[vl.iz][vl.iy][vl.ix]&mask)-((int)v[vl.iz][vl.iy+2][vl.ix]&mask)) * vl.blf +        // BLF
                        (((int)v[vl.iz][vl.iy-1][vl.ix+1]&mask)-((int)v[vl.iz][vl.iy+1][vl.ix+1]&mask)) * vl.trf +  // TRF
                        (((int)v[vl.iz][vl.iy][vl.ix+1]&mask)-((int)v[vl.iz][vl.iy+2][vl.ix+1]&mask)) * vl.brf +    // BRF
                        (((int)v[vl.iz+1][vl.iy-1][vl.ix]&mask)-((int)v[vl.iz+1][vl.iy+1][vl.ix]&mask)) * vl.tlb +  // TLB
                        (((int)v[vl.iz+1][vl.iy][vl.ix]&mask)-((int)v[vl.iz+1][vl.iy+2][vl.ix]&mask)) * vl.blb +    // BLB
                        (((int)v[vl.iz+1][vl.iy-1][vl.ix+1]&mask)-((int)v[vl.iz+1][vl.iy+1][vl.ix+1]&mask)) * vl.trb + // TRB
                        (((int)v[vl.iz+1][vl.iy][vl.ix+1]&mask)-((int)v[vl.iz+1][vl.iy+2][vl.ix+1]&mask)) * vl.brb;  // BRB
                // z
                gz =
                        (((int)v[vl.iz-1][vl.iy][vl.ix]&mask)-((int)v[vl.iz+1][vl.iy][vl.ix]&mask)) * vl.tlf +       // TLF
                        (((int)v[vl.iz-1][vl.iy+1][vl.ix]&mask)-((int)v[vl.iz+1][vl.iy+1][vl.ix]&mask)) * vl.blf +   // BLF
                        (((int)v[vl.iz-1][vl.iy][vl.ix+1]&mask)-((int)v[vl.iz+1][vl.iy][vl.ix+1]&mask)) * vl.trf +   // TRF
                        (((int)v[vl.iz-1][vl.iy+1][vl.ix+1]&mask)-((int)v[vl.iz+1][vl.iy+1][vl.ix+1]&mask)) * vl.brf +  // BRF
                        (((int)v[vl.iz][vl.iy][vl.ix]&mask)-((int)v[vl.iz+2][vl.iy][vl.ix]&mask)) * vl.tlb +         // TLB
                        (((int)v[vl.iz][vl.iy+1][vl.ix]&mask)-((int)v[vl.iz+2][vl.iy+1][vl.ix]&mask)) * vl.blb +     // BLB
                        (((int)v[vl.iz][vl.iy][vl.ix+1]&mask)-((int)v[vl.iz+2][vl.iy][vl.ix+1]&mask)) * vl.trb +     // TRB
                        (((int)v[vl.iz][vl.iy+1][vl.ix+1]&mask)-((int)v[vl.iz+2][vl.iy+1][vl.ix+1]&mask)) * vl.brb;  // BRB
                VJGradient g = new VJGradient(gx, gy, gz);
                return g;
        }
        public String toString() { return " trilinear "; }
        /**
         * Bilinear interpolation at (ix+dx, iy+dy) of 2D image organised as float[].
         * @param i the 2D image.
         * @param width the width of the image.
         * @param ix the integer x position at which to interpolate.
         * @param iy the integer y position at which to interpolate.
         * @param dx the x fractional position
         * @param dy the y fractional position
	  */
        public static float value(float [] i, int width, int ix, int iy, float dx, float dy)
        {
                float p11, p12, p1, p21, p22, p2, r1;

                p11 = i[iy*width+ix];
                p12 = i[(iy)*width+(ix+1)];
                p21 = i[(iy+1)*width+(ix)];
                p22 = i[(iy+1)*width+(ix+1)];
                p1 = ((p12 - p11) * dx) + p11;
                p2 = ((p22 - p21) * dx) + p21;
                r1 = ((p2 - p1) * dy) + p1;
                return r1;
        }
        /**
         * Bilinear interpolation at (ix+dx, iy+dy) of 2D image organised as byte[].
         * @param i the 2D image.
         * @param width the width of the image.
         * @param ix the integer x position at which to interpolate.
         * @param iy the integer y position at which to interpolate.
         * @param dx the x fractional position
         * @param dy the y fractional position
	     */
        public static float value(byte [] i, int width, int ix, int iy, float dx, float dy)
        {
                float p11, p12, p1, p21, p22, p2, r1;

                p11 = i[iy*width+ix]&0xff;
                p12 = i[(iy)*width+(ix+1)]&0xff;
                p21 = i[(iy+1)*width+(ix)]&0xff;
                p22 = i[(iy+1)*width+(ix+1)]&0xff;
                p1 = ((p12 - p11) * dx) + p11;
                p2 = ((p22 - p21) * dx) + p21;
                r1 = ((p2 - p1) * dy) + p1;
                return r1;
        }
        /**
         * Bilinear interpolation at (ix+dx, iy+dy) of 2D image organised as short[].
         * @param i the 2D image.
         * @param width the width of the image.
         * @param ix the integer x position at which to interpolate.
         * @param iy the integer y position at which to interpolate.
         * @param dx the x fractional position
         * @param dy the y fractional position
	  */
        public static float value(short [] i, int width, int ix, int iy, float dx, float dy)
        {
                float p11, p12, p1, p21, p22, p2, r1;

                p11 = i[iy*width+ix]&0xffff;
                p12 = i[(iy)*width+(ix+1)]&0xffff;
                p21 = i[(iy+1)*width+(ix)]&0xffff;
                p22 = i[(iy+1)*width+(ix+1)]&0xffff;
                p1 = ((p12 - p11) * dx) + p11;
                p2 = ((p22 - p21) * dx) + p21;
                r1 = ((p2 - p1) * dy) + p1;
                return r1;
        }
        /**
         * Bilinear interpolation of brightness at (ix+dx, iy+dy) of 2D image organised as int[].
         * @param i the 2D image.
         * @param width the width of the image.
         * @param ix the integer x position at which to interpolate.
         * @param iy the integer y position at which to interpolate.
         * @param dx the x fractional position
         * @param dy the y fractional position
	  */
        public static float value(int [] i, int width, int ix, int iy, float dx, float dy)
        {
                VJUserInterface.write("RGB not implemented!!!!");
                return 0;
        }
		/**
		 * Determine whether x,y are valid in an image width*height
		 * for bilinear interpolation.
         * @param width the width of the image.
         * @param height the height of the image.
         * @param ix the integer x-position
         * @param iy the integer y-position.
		 */
		public static boolean valid(int width, int height, int ix, int iy)
		{
                return (ix >= 0 && ix+1 < width && iy >= 0 && iy+1 < height);
		}
}
