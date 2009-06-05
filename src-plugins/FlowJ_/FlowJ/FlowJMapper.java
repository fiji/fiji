package FlowJ;
import ij.process.*;
import ij.*;

/**
* This class is the superclass for the OF display mappers.
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
public class FlowJMapper
{
	  protected ImageProcessor 	        impr;
	  protected Object 			pixels;
	  protected float [][][] 	        flow;
	  protected int 			axes;
	  protected int 			maxp;
	  protected int 			maxq;
	  protected double 			pScaling;
	  protected double 			qScaling;
	  protected double 			rho;

	  public FlowJMapper() {}
	  public FlowJMapper(ImageProcessor impr, float [][][] flow, int axes,
			int maxp, int maxq, double pScaling, double qScaling, double rho)
	  {
			this.impr = impr;
			this.pixels = impr.getPixels();
			this.flow = flow;
			this.axes = axes;
			this.maxp = maxp;
			this.maxq = maxq;
			this.pScaling = pScaling;
			this.qScaling = qScaling;
			this.rho = rho;
	  }
	  public void pixel(int ip, int iq, int ix, int iy, double dx, double dy)
	  {}
        /**
         * This is a HACK and should not be here.
         * Bilinear interpolation of 2D or 3D-vector organised as float[][][].
         * @param v the 2-D matrix or 3-D volume to be interpolated.
         * @param x the integer x position at which to interpolate.
         * @param y the integer x position at which to interpolate.
         * @param dx the x-weight to use for interpolation.
         * @param dy the y-weight to use for interpolation.
         * @return a vector with the interpolated x and y values.
	  */
        public static float [] bl(float [][][] v, int x, int y, double dx, double dy)
        {
                float [] vv = new float[3];
                double p11, p12, p1, p21, p22, p2, r1;

                p11 = v[y][x][0];
                p12 = v[y][x+1][0];
                p1 = ((p12 - p11) * dx) + p11;
                p21 = v[y+1][x][0];
                p22 = v[y+1][x+1][0];
                p2 = ((p22 - p21) * dx) + p21;
                r1 = ((p2 - p1) * dy) + p1;
                vv[0] = (float) r1;
                p11 = v[y][x][1];
                p12 = v[y][x+1][1];
                p1 = ((p12 - p11) * dx) + p11;
                p21 = v[y+1][x][1];
                p22 = v[y+1][x+1][1];
                p2 = ((p22 - p21) * dx) + p21;
                r1 = ((p2 - p1) * dy) + p1;
                vv[1] = (float) r1;
                if (v[y][x].length > 2)
                {
                        // 3D vector.
                        p11 = v[y][x][2];
                        p12 = v[y][x+1][2];
                        p1 = ((p12 - p11) * dx) + p11;
                        p21 = v[y+1][x][2];
                        p22 = v[y+1][x+1][2];
                        p2 = ((p22 - p21) * dx) + p21;
                        r1 = ((p2 - p1) * dy) + p1;
                        vv[2] = (float) r1;
                }
                return vv;
        }
}

