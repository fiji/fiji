package FlowJ;
import ij.*;
import ij.process.*;
/*
	 This class maps dynamic color of a flow field.

	 Copyright (c) 2001, Michael Abramoff. All rights reserved.

	 Author: Michael Abramoff,
			  Image Sciences Institute
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
public class FlowJDynamicColorMapper extends FlowJMapper
{
	  public FlowJDynamicColorMapper(ImageProcessor impr, float [][][] flow, int axes,
			int maxp, int maxq, double pScaling, double qScaling, double rho)
	  {
			super(impr, flow, axes,
					maxp, maxq, pScaling, qScaling, rho);
	  }
        public void pixel(int ip, int iq, int ix, int iy, double dx, double dy)
        /*
                Map the 3D flow in flow at ip, iq into pixels.
                Rho determines the maximal mapped magnitude.
        */
        {
                float [] v;
                if (pScaling == 1 && qScaling == 1)
                {
                        v = new float[2];
                        v[0] = flow[iy][ix][0]; v[1] = flow[iy][ix][1];
                }
                else
                        v = bl(flow, ix, iy, dx, dy);
                float [] pv = FlowJFlow.polar(v);
                byte [] rgb = new byte[3];
                FlowJDynamicColor.map2D(rgb, pv[0] / rho, pv[1]);
                if (pv[0]/rho > 0.05)
                        ((int []) pixels)[iq*maxp+ip] = ((rgb[0]&0xff) << 16) | ((rgb[1]&0xff) << 8) | (rgb[2]&0xff);
        }
}

