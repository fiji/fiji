package VolumeJ;
import volume.*;

/**
 * This class creates thresholded (binary) volumes of any type of Volume.
 * It is used in iso-surface rendering.
 *
 * Copyright (c) 2001-2003, Michael Abramoff. All rights reserved.
 * Patent pending.
 * @author: Michael Abramoff
 *
 * Note: this is not open source software!
 * These algorithms, source code, documentation or any derived programs ('the software')
 * are the intellectual property of Michael Abramoff.
 * Michael Abramoff asserts his right as the sole owner of the rights
 * to this software.
 * You and/or any person(s) acting with or for you may not:
 * - directly or indirectly copy, sell, lease, rent, license,
 * sublicense, redistribute, lend, give, transfer or otherwise distribute or
 * use the software
 * - modify, translate, or create derivative works from the software, assign or
 * otherwise transfer rights to the Software or use the Software for timesharing
 * or service bureau purposes
 * - reverse engineer, decompile, disassemble or otherwise attempt to discover the
 * source code or underlying ideas or algorithms of the Software or any subsequent
 * version thereof or any part thereof.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class VJThresholdedVolume extends Volume
{
        public boolean []      	v;

        /** creates the null volume. */
        public VJThresholdedVolume() {  }
        /**
         * Create a new thresholded volume.
         * @param v the volume to be thresholded
         * @param classifier a VJClassifier that can threshold a voxel.
         */
        public VJThresholdedVolume(Volume v, VJClassifier classifier)
        {
                if (v instanceof VolumeShort)
                        threshold((VolumeShort) v, (VJClassifierIsosurface) classifier);
                else if (v instanceof VolumeRGB)
                        threshold((VolumeRGB) v, (VJClassifierIsosurface) classifier);
                else if (v instanceof VolumeFloat)
                        threshold((VolumeFloat) v, (VJClassifierIsosurface) classifier);
                //else
        }
        /**
         * Threshold a short volume.
         * @param v the volume to be thresholded
         * @param classifier a VJClassifier that can threshold a voxel.
         */
        private void threshold(VolumeShort v, VJClassifierIsosurface classifier)
        {
                depth = v.getDepth();
                height = v.getHeight();
                width = v.getWidth();
                this.v = new boolean[depth*height*width];
                for (int z = 0; z < depth; z++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                        this.v[z*height*width+y*width+x] = classifier.overThreshold(v.v[z][y][x]);
        }
        /**
         * Threshold an RGB volume. The threshold is compared to the HSB brightness level of the
		 * voxel [0-255].
         * @param v the volume to be thresholded
         * @param classifier a VJClassifier that can threshold a voxel.
         */
        private void threshold(VolumeRGB v, VJClassifierIsosurface classifier)
        {
                depth = v.getDepth();
                height = v.getHeight();
                width = v.getWidth();
                this.v = new boolean[depth*height*width];
                for (int z = 0; z < depth; z++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                        this.v[z*height*width+y*width+x] = classifier.overThreshold(v.b[z*height*width+y*width+x]&0xff);
        }
        /**
         * Threshold a float volume.
         * @param vs the volume to be thresholded
         * @param classifier a VJClassifier that can threshold a voxel.
         */
        private void threshold(VolumeFloat v, VJClassifierIsosurface classifier)
        {
                depth = v.getDepth();
                height = v.getHeight();
                width = v.getWidth();
                this.v = new boolean[depth*height*width];
                for (int z = 0; z < depth; z++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                        this.v[z*height*width+y*width+x] = classifier.overThreshold(v.v[z][y][x]);
        }
        /**
         * Get the threshold value as a boolean.
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         * @value a Number with 0 for false and 1 for true
         */
        public Object get(int x, int y, int z) { return new Boolean(v[z*height*width+y*width+x]); }
        /**
         * Set the threshold value to a boolean value.
         * @param value a boolean
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         */
        public void set(Object value, int x, int y, int z) { v[z*height*width+y*width+x] = ((Boolean) value).booleanValue(); }
}
