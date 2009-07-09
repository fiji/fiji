package FlowJ;
import java.awt.*;
import ij.*;
import ij.process.*;

/**
 * This is a class that implements 2D and 3D Dynamic Color Mapping.
 * Dynamic Color Mapping maps flow fields to a 6 basis-color-space with 3 axes.
 * one axis is blue-yellow, another red-green and the third white-black.
 * 3D vectors (speed-orientationxy-orientationz) are mapped to a point on these three axes.
 * with hue and value (intensity) coding for orientation in x,y,z
 * and saturation coding for speed.
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
public class FlowJDynamicColor
{
	  // This value is the exponent of the relationship between speed and saturation.
	  private final static double factorization = 1.2f;    // nonlinear correction for length -> saturation
	  // This value determines the oversaturation for yellowish hues (because these tend to be washed out.
	  private final static double epsilon = 1.6f;

	  /**
           * This method maps a polar 2d vector with orientation and magnitude (0-1)
           * to a dynamic color in rgb.
	  */
	  public static void map2D(byte [] rgb, double magnitude, double orientationxy)
	  {
			float [] rgbd = new float[3];

			map2D(rgbd, magnitude, orientationxy);
			// Make RGB values from normalized values of dynamic color values.
			// White = 255,255,255
			rgb[0] = (byte) ((int)((rgbd[0] * 255.0) + 0.5)&0xff);
			rgb[1] = (byte) ((int)((rgbd[1] * 255.0) + 0.5)&0xff);
			rgb[2] = (byte) ((int)((rgbd[2] * 255.0) + 0.5)&0xff);
	  }
	  /**
           * This method maps a polar 3d vector with orientations and speed (0-1)
           * to a dynamic color in rgb.
	  */
	  public static void map(byte [] rgb, double magnitude, double orientationxy, double orientationz)
	  {
			Color color = map(magnitude, orientationxy, orientationz);
			rgb[0] = (byte) color.getRed();
			rgb[1] = (byte) color.getGreen();
			rgb[2] = (byte) color.getBlue();
	  }
	  public static Color map(double speed, double orientationxy, double orientationz)
	  {
			byte [] rgb = new byte[3];

			map3D(rgb, speed, orientationxy, orientationz);
			return (new  Color((int) (rgb[0]&0xff), (int) (rgb[1]&0xff), (int) (rgb[2]&0xff)));
	  }
	  /**
           * Transform a polar 3D vector (orientationxy in the xy-plane,
           * orientationz along the z axis and normalized magnitude (0-1)),
           * to a dynamic color in rgb (0-255), with hue standing for orientationxy,
           * intensity standing for orientationz and saturation standing for magnitude.
           * Limits magnitude.
	  */
	  public static void map3D(byte [] rgb, double magnitude, double orientationxy, double orientationz)
	  {
                float [] hue = new float[3];
                float [] drgb = new float[3];

                // first the hue (x,y). green is right, red left, blue anterior and yellow posterior
                hue(hue, orientationxy);
                // rgb is now normalized to maximum saturation.
                // now the intensity (z). 1 is up (90 deg), 0 is down (270 deg).
                double intensity = ((double) Math.sin(orientationz)+1) / 2;
                // now the saturation (0-1). saturated is fast, unsaturated is slow.
                double saturation = Math.min(magnitude,1);
                // factor in the saturation.
                drgb[0] = (float) (hue[0] * saturation);
                drgb[1] = (float) (hue[1] * saturation);
                drgb[2] = (float) (hue[2] * saturation);
                // now consider intensity. if intensity = 0.5 nothing changes (no motion in z).
                if (intensity >= 0.5)
                {
                          // pull to 255,255,255.
                          drgb[0] = drgb[0] + (1 - drgb[0]) * ((float) intensity - 0.5f) * 2;
                          drgb[1] = drgb[1] + (1 - drgb[1]) * ((float) intensity - 0.5f) * 2;
                          drgb[2] = drgb[2] + (1 - drgb[2]) * ((float) intensity - 0.5f) * 2;
                }
                else
                {
                          // push to 0,0,0
                          drgb[0] = drgb[0] + (drgb[0]) * ((float) intensity - 0.5f) * 2;
                          drgb[1] = drgb[1] + (drgb[1]) * ((float) intensity - 0.5f) * 2;
                          drgb[2] = drgb[2] + (drgb[2]) * ((float) intensity - 0.5f) * 2;
                }
                // convert to byte value.
                rgb[0] = (byte) ((drgb[0] * 255)+0.5);
                rgb[1] = (byte) ((drgb[1] * 255)+0.5);
                rgb[2] = (byte) ((drgb[2] * 255)+0.5);
        }
        /**
         * Map a polar 2D vector (orientationxy in the xy-plane and normalized magnitude (0-1)),
         * to a dynamic color in rgb, with hue standing for orientationxy,
         * and saturation standing for speed.
         * Limits the magnitude.
         */
        private static void map2D(float [] rgb, double magnitude, double orientationxy)
        {
			float [] hue = new float[3];

			// first the hue (x,y). green is right, red left, blue anterior and yellow posterior
			hue(hue, orientationxy);
			// rgb is now normalized to maximum saturation.
			// now the saturation. saturated is fast, unsaturated is slow.
			// if constant factorization is not 1.0, you get a nonlinear relationship.
			magnitude = Math.min(magnitude, 1);
			double saturation = (double) Math.pow(magnitude, factorization);
			rgb[0] = (float) (1 -((1 - hue[0]) * saturation));
			rgb[1] = (float) (1 -((1 - hue[1]) * saturation));
			rgb[2] = (float) (1 -((1 - hue[2]) * saturation));
        }
        /**
         * Transform the 2D orientation of a vector into a hue (in float rgb  format [0-1.0]).
         * This is the central routine of dynamic color mapping.
	  */
        public static void hue(float [] rgb, double orientation)
        {
                double yc;

                // yellow correction.
                yc = epsilon+(1 - Math.cos(orientation*2)) / 3;
                // red channel
                double t = (orientation < Math.PI / 2) ?
                                0 : ((orientation < Math.PI) ?
                                ((1- Math.cos(orientation * 2- Math.PI)) / 2) : ((orientation < Math.PI * 3 / 2) ?
                                        (((3+ Math.cos(orientation*2)) / 4) * yc) :
                                        ((((1- Math.cos(orientation*2)) / 4) * yc))));
		 rgb[0] = (float) t;
                 // green channel
		 t = (orientation < Math.PI / 2) ?
                                ((1- Math.cos(orientation*2.0-Math.PI)) / 2) : ((orientation < Math.PI) ?
                                        0 : ((orientation < Math.PI * 3 / 2) ?
                                                (((1- Math.cos(orientation*2)) / 4) * yc) :
                                                ((((3+ Math.cos(orientation*2)) / 4) * yc))));
                  rgb[1] = (float) t;
		  // blue channel
		  t = (orientation <= Math.PI) ? ((1-Math.cos(orientation*2)) / 2) : 0;
                  rgb[2] = (float) t;
		  // To be sure clamp rgb values.
		  rgb[0] = Math.min(rgb[0], 1);
		  rgb[1] = Math.min(rgb[1], 1);
		  rgb[2] = Math.min(rgb[2], 1);
        } // hue
}

