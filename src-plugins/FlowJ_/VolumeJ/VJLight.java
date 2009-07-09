package VolumeJ;
import java.awt.*;

/**
 * This class implements a volume rendering light, and adds to methods to manipulate the light.
 * Use objectify to transform the light position into
 * another coordinate system (for example, the same coordinate system as the gradient, usually object
 * space).
 * Currently only monochrome lightsare supported.
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
public class VJLight
{
        private float          x, y, z;          // position.
        public float           tx, ty, tz;          // transformed, normalized position.
        private final float    EPSILON = 0.000000001f;
        public float	       diffuse, specular;

        /**
         * Create a new light. The light comes from x, y, z.
         * @param x the x-position of the light (usually in viewspace).
         * @param y the y-position of the light (usually in viewspace).
         * @param z the z-position of the light (usually in viewspace).
         * @param diffuse the amount of light diffuse reflection cast by this light.
         * @param specular the amount of specular reflection cast by this light.
         */
        public VJLight(float x, float y, float z, float diffuse, float specular)
        {
                this.x = x;
                this.y = y;
                this.z = z;
                this.diffuse = diffuse;
                this.specular = specular;
                setNormalized(x, y, z);
        }
        public String toString()
        {
                return "light ("+x+","+y+","+z+"); ";
        }
        public String toLongString()
        {
                return "light ("+x+","+y+","+z+") "+tx+","+ty+","+tz;
        }
        /**
         * Transform the position of this light into another coordinate system.
         * Usually from viewspace to objectspace.
         * @param m the transformation matrix.
         */
        public void objectify(VJMatrix m)
        {
                float [] v = VJMatrix.newVector(x, y, z);
                float [] tv = m.mul(v);
                setNormalized(tv[0], tv[1], tv[2]);
        }
        private void setNormalized(float tx, float ty, float tz)
        {
                float mag = (float) Math.sqrt(Math.pow(tx,2) + Math.pow(ty, 2) + Math.pow(tz, 2));
                if (mag > EPSILON)
                {
                        this.tx = tx / mag;
                        this.ty = ty / mag;
                        this.tz = tz / mag;
                }
                else
                {
                        this.tx = tx;
                        this.ty = ty;
                        this.tz = tz;
                }
        }
        public float getSpecular() { return specular; }
        public float getDiffuse() { return diffuse; }
        public float getx() { return tx; }
        public float gety() { return ty; }
        public float getz() { return tz; }
}

