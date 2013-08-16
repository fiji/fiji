package volume;
import java.awt.*;
import ij.*;
import ij.process.*;
/**
 * This class implements RGB vector volumes.
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
public class VolumeRGB extends Volume
{
        /**
         * The brightness of each voxel as a byte (0-225).
         * This brightness derives from the HSB color model.
         */
        public byte []  b;
        /**
         * The index value for each voxel, if any.
         */
        public byte [] index;
        /**
         * The slices for this volume with the actual RGB data.
         */
        public Object [] sliceArray;

        /**
        * Creates an RGB volume of defined size and aspect ratio.
        * @param width
        * @param height
        * @param depth the dimensions of the volume
        * @param aspectx
        * @param aspecty
        * @param aspectz the aspect ratios of the volume dimensions.
        */
        public VolumeRGB(int width, int height, int depth, double aspectx, double aspecty, double aspectz)
        {
                this.width = width; this.height = height; this.depth = depth;
                b = new byte[depth*height*width];
                setAspects(aspectx, aspecty, aspectz);
                edge = 0;
        }
        /**
        * Creates an RGB volume of defined aspect ratio from an array of slices.
        * @param sliceArray an array of Objects, which are int [] with each RGB voxel.
        * @param aspectx
        * @param aspecty
        * @param aspectz the aspect ratios of the volume dimensions.
        */
        public VolumeRGB(Object [] sliceArray, int width, int depth, double aspectx, double aspecty, double aspectz)
        {
                this(width, ((int []) sliceArray[0]).length/width, depth, aspectx, aspecty, aspectz);
                this.sliceArray = sliceArray;
                load(sliceArray, 0);
                //(new ImagePlus("RGB brightness", getImageStack())).show();
        }
        /**
         * Get the brightness of the voxel at x,y,z as an integer between 0-255.
         * The brightness is derived from the HSB color model.
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         * @return an Integer with the brightness converted to a value between 0 (0.0) and 255 (1.0)
         */
        public Object get(int x, int y, int z)
        {
                return new Byte(b[z*width*height+y*width+x]);
        }
        /**
         * Set the brightness of the voxel at x,y,z as an integer between 0-255.
         * The brightness is derived from the HSB color model.
         * @param value a Number with the brightness between 0 (0.0) and 255 (1.0)
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         */
        public void set(Object value, int x, int y, int z)
        {
                b[z*width*height+y*width+x] = ((Number) value).byteValue();
        }
        /**
         * Fill the brightness volume b with the int[] slices, starting at slice start.
         * @param sliceArray an array of Objects, which are byte [] with the index value for
         * each voxel.
         * @param start the first slice to be loaded (starting with slice 1).
        */
        protected void load(Object [] sliceArray, int start)
        {
                float [] hsb = new float[3];
                for (int z = start; z < start + depth; z++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                {
                        int [] slice = (int []) sliceArray[z];
                        int vv = slice[y*width+x];
                        intToHSB(hsb, vv);
                        b[(z-start)*height*width+y*width+x] = (byte) (((int) (hsb[2]*255.0))&0xff);
                }
        }
        /**
         * Make an ImageJ imagestack from the brightness info. For debugging purposes only.
         */
        public ImageStack getImageStack()
        {
                ImageStack stack = new ImageStack(width, height);
                for (int z = 0; z < depth; z++)
                {
                        ImageProcessor ip = new ByteProcessor(width, height);
                        byte [] plane = (byte []) ip.getPixels();
                        for (int y = 0; y < height; y++)
                        for (int x = 0; x < width; x++)
							plane[y*width+x] = b[z*height*width+y*width+x];
                        stack.addSlice(""+(z+1), ip);
                }
                return stack;
        }
        /**
         * Utility routine to convert an ARGB int format to float [] HSB format.
         * @param hsb a float[3] (at least) that will contain the HSB values on exit
         * @param i a color in int ARGB format.
         */
        public static float [] intToHSB(float [] hsb, int i)
        {
                int red = (i&0xff0000)>>16;
                int green = (i&0xff00)>>8;
                int blue = i&0xff;
                Color.RGBtoHSB(red, green, blue, hsb);
                return hsb;
        }
        /**
         * Utility routine to convert a java Color to an ARGB int.
         * @param color a Color.
         * @return an int with color in int ARGB format.
         */
        public static int ColorToInt(Color color)
        {
                int rvalue = color.getRed();
                int gvalue = color.getGreen();
                int bvalue = color.getBlue();
                return (int) (rvalue<<16 | gvalue<<8 | bvalue);
        }
        /**
         * Combine the values of s into the index array of this volume.
         * @param sliceArray an array of Objects, which are byte [] with the index value for
         * each voxel.
        */
        public void setIndex(Object [] sliceArray)
        {
                index = new byte[depth*height*width];
                for (int z = 0; z < depth; z++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                {
                        byte [] slice = (byte []) sliceArray[z];
                        index[z*height*width+y*width+x] = slice[y*width+x];
                }
        }
        public boolean isIndexed() { return index instanceof byte[]; }
}
