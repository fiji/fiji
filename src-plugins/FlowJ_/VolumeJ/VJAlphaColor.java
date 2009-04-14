package VolumeJ;
/**
 * This class is the abstract type for an alphacolor, i.e. the combination of an
 * RGB color and associated alpha (transparency).
 * It has methods for composing colors.
 *
 * (c) 1999-2002 Michael Abramoff. All rights reserved.
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
public class VJAlphaColor
{
        public float alpha;
        public float r, g, b;
        private boolean grayscale;

        public VJAlphaColor()
        {
                alpha = 0; r = 0; g = 0; b = 0;
                grayscale = false;
        }
        /**
        * Use as an alpha-grayscale value container.
        * @param alpha: the transparency [0-1]
        * @param grayvalue: the grayscale value (integer).
        */
        public VJAlphaColor(double alpha, int grayvalue)
        {
                this.alpha = (float) alpha; r = grayvalue;
                grayscale = true;
        }
        /**
        * Use as a grayscale value container.
        * @param grayvalue: the grayscale value (integer).
        */
        public VJAlphaColor(int grayvalue)
        {
                alpha = 0;
                r = grayvalue;
                grayscale = true;
        }
        /**
        * Use as a grayscale value container.
        * @param grayvalue: the grayscale value (double).
        */
        public VJAlphaColor(double grayvalue)
        {
                alpha = 0;
                r = (float) grayvalue;
                grayscale = true;
        }
        /**
        * An alpha-grayscale value.
        * @param alpha: the transparency [0-1]
        * @param r, g, b: the RGB components [0-255] in int format.
        */
	public VJAlphaColor(double alpha, int r, int g, int b)
	{
		this.alpha = (float) alpha;
		this.r = r; this.g = g; this.b = b;
		grayscale = false;
	}
	/**
        * An alpha-grayscale value for float RGB values.
        * @param alpha: the transparency [0-1]
        * @param r, g, b: the RGB components [0-1] in float format.
        */
        public VJAlphaColor(double alpha, double r, double g, double b)
        {
                this.alpha = (float) alpha;
                this.r = (float)(r * 255); this.g = (float)(g * 255); this.b = (float) (b * 255);
                grayscale = false;
        }
        public float getAlpha() { return alpha; }
        public int getRed() { return (int) (r+0.5); }
        public int getGreen() { return (int) (g+0.5); }
        public int getBlue() { return (int) (b+0.5); }
        public int getValue() { return (int) (r+0.5); }
        public boolean isGrayscale() { return grayscale; }
        /**
        * Additive color mixing.
        * Compose this alphacolor with a fraction of color.
        * @param color: the color to be composed (added) this alphacolor.
        * @param contribution: the fraction of the components of color that are added.
        */
        public void compose(VJAlphaColor color, float contribution)
        {
                // May be grayscale component.
                r += ((color.r * contribution));
		if (! grayscale)
		{
                        g += ((color.g * contribution));
			b += ((color.b * contribution));
                }
        }
        /**
         * Composing. Compose the classified voxel color into pixel, combining with shade and gradient.
         * The color value is attenuated with the shade. Update the alpha value for pixel.
         * Front to back alpha blending compositing. White lights only!
         * @param g the interpolated gradient for shading
         * @param color contains the alpha value and the color (grayscale or RGB) of the classified values.
         * @param shade is the effect of shading.
        */
        public void blendComposeScalar(VJAlphaColor color, VJShade shade)
        {
                float composedAlpha = color.getAlpha() * (1 - alpha);
                // Update this alpha composite
                alpha += composedAlpha;
                // Modify all channels with color,  the composed alpha of this pixel and the shade.
                compose(color, shade, composedAlpha);
        }
	/**
         * Additive shading and color mixing.
         * Compose this alphacolor with a shaded fraction of color.
         * @param color: the color to be composed (added) this alphacolor.
         * @param shade: the shade with which the fraction of color is shaded before adding.
         * @param contribution: the fraction of the components of color that are added.
        */
        public void compose(VJAlphaColor color, VJShade shade, float contribution)
	{
		if (shade.singlechannel())
		{
			contribution = shade.compute(contribution);
			r += ((color.r * contribution));
			if (! grayscale)
			{
				g += ((color.g * contribution));
				b += ((color.b * contribution));
			}
		}
	        else
		{
                	r += (color.r * shade.get(0) * contribution);
			g += (color.g * shade.get(1) * contribution);
			b += (color.b * shade.get(2) * contribution);
		}
        }
	public boolean visible() { return alpha > 0; }
	public boolean notOpaque() { return alpha != 1; }
	public void setOpaque() { alpha = 1; }
        /**
         * Make a copy of this VJAlphaColor.
         */
	public void copy(VJAlphaColor ac) { alpha = ac.alpha; r = ac.r; g = ac.g; b = ac.b; }
	public void setAlpha(float alpha) { this.alpha = (float) alpha; }
	/**
         * Return whether this alphacolor is almost opaque.
         * @return: true if this alphacolor is opaque, false if not.
         */
        public boolean almostOpaque()	{ return alpha > 0.95; }
        public void attenuate(VJShade shade)
	{
		if (shade.singlechannel())
		{
			if (grayscale)
				r *= shade.get();
			else
			{
				r *= shade.get();
				g *= shade.get();
				b *= shade.get();
			}
		}
	}
	public String toString()
	{
		if (grayscale)
			return ""+ij.IJ.d2s(alpha, 2)+":"+ij.IJ.d2s(r,1);
        	else
			return ""+ij.IJ.d2s(alpha, 2)+":"+ij.IJ.d2s(r,1)+","+ij.IJ.d2s(g,1)+","+ij.IJ.d2s(b,1);
	}
}
