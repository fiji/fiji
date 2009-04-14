package VolumeJ;
import java.awt.*;
import ij.*;
import ij.gui.*;

/*
     This class implements VJ shapes. Shapes are non-volumetric, parametric objects
     that are rendered together with volumes.
     They can be used for wireframes, indices, axes etc.

     Copyright (c) 1999, Michael Abramoff. All rights reserved.

     Author: Michael Abramoff,
              c/o Image Sciences Institute
              University Medical Center Utrecht
              Netherlands

     Small print:
     Permission to use, copy, modify and distribute this version of this software or any parts
     of it and its documentation or any parts of it ("the software"), for any purpose is
     hereby granted, provided that the above copyright notice and this permission notice
     appear intact in all copies of the software and that you do not sell the software,
     or include the software in a commercial package.
     The release of this software into the public domain does not imply any obligation
     on the part of the author to release future versions into the public domain.
     The author is free to make upgraded or improved versions of the software available
     for a fee or commercially only.
     Commercial licensing of the software is available by contacting the author.
     THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
     EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
     WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
*/
public class VJShape
{
      private int axisLength = 12;

      public VJShape()
      {
      }
      public boolean at(int xi, int yi, int zi)
      /* Is there a shape at xi, yi, zi? */
      {
            int axisx = 2; int axisy = 2; int axisz = 2;

            boolean isShape = false;

            if (xi >= axisx && xi <= axisx + axisLength &&  yi == axisy && zi == axisz)
                  isShape = true; // x-axis
            else if (xi == axisx && yi >= axisy && yi <= axisy + axisLength &&  zi == axisz)
                  isShape = true; // y-axis
            else if (xi == axisx && yi == axisy && zi >= axisz && zi <= axisz + axisLength)
                  isShape = true; // z-axis
            return isShape;
      }
      public double colorAlpha(byte [] rgb)
      /*
              Determine color and opacity of the shape.
      */
      {
              // white axis grid.
              double alpha = 1.0;
	            rgb[0] = (byte) (255&0xff);
	            rgb[1] = (byte) (255&0xff);
	            rgb[2] = (byte) (0&0xff);
              return alpha;
      }
      public double gradient(double [] gradient)
      /* determine the gradient for the shape. */
      {
              gradient[0] = gradient[1] = gradient[2] = 1;
              return Math.sqrt(3);
      }
}

