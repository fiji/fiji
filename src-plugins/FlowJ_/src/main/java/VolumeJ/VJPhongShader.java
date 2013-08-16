package VolumeJ;
/**
 * VJShader implements shading in a Phong illumination model, for ambient and diffuse shading only.
 * Specular refelection has not been implemented.
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
public class VJPhongShader extends VJShader
{
        private float		ambient, diffuse;

        /**
         * Create a new shader for ambient light and a VJLight light.
         * @param ambient a float [0-1] that sets the amount of background lighting.
         * @param light a VJLight that is the position and intensity of the diffuse lighting.
         * @param doBackface a boolean that sets whether or not backfacing is on.
         */
        public VJPhongShader(float ambient, VJLight light, boolean doBackface)
        {
                this.ambient = ambient;
                this.doBackface = doBackface;
                this.light = light;
                diffuse = light.getDiffuse();
        }
        /**
         * Shading method for Phong illumination model that uses normalized gradient g.
         * Finds the shade that fits with light and g, with ambient and diffuse reflection only.
         * Viewing direction = 0,0,1.
         * @param g a VJGradient containing the normalized gradients in x, y, and z dimensions.
         * @return a VJShade containing a monocolor or trichannel shade.
        */
        public VJShade shade(VJGradient g)
        {
                if (g.getDimensions() == 1)
		{
		        // Phong illumination model. Dot-product: light*surface.normal
                        float dot = g.getx() * light.getx() + g.gety() * light.gety() + g.getz() * light.getz();
			// Shade backfaces (turned away from light) as front faces?
                        if (doBackface)
                                dot = Math.abs(dot);
			else if (dot < 0)
				dot = 0;	// no negative contributions
			return new VJShade(ambient + (diffuse * dot));
                }
		else
		{
			// multichannel shading.
			float [] dot = new float[3];
			dot[0] = g.getx(0) * light.getx() + g.gety(1) * light.gety() + g.getz(2) * light.getz();
			if (dot[0] < 0 && doBackface) dot[0] = Math.abs(dot[0]);
                	else if (dot[0] < 0) dot[0] = 0;	// no negative contributions
			dot[1] = g.getx(0) * light.getx() + g.gety(1) * light.gety() + g.getz(2) * light.getz();
			if (dot[1] < 0 && doBackface) dot[1] = Math.abs(dot[1]);
			else if (dot[1] < 0) dot[1] = 0;	// no negative contributions
			dot[2] = g.getx(0) * light.getx() + g.gety(1) * light.gety() + g.getz(2) * light.getz();
			if (dot[2] < 0 && doBackface) dot[2] = Math.abs(dot[2]);
			else if (dot[2] < 0) dot[2] = 0;	// no negative contributions
			return new VJShade(
				ambient + (diffuse * dot[0]),
				ambient + (diffuse * dot[1]),
				ambient + (diffuse * dot[2]));
		}
        }
        public String toString() { return "Phong shader: "+ambient+" "+diffuse+" "+light; }
}

