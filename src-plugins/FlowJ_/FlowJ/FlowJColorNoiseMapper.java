package FlowJ;
import ij.*;
import ij.process.*;
/**
 * This class maps a flow field in colored spotnoise format.
 *
 * Copyright (c) 2001-2003, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff, Image Sciences Institute, University Medical Center Utrecht, Netherlands
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
public class FlowJColorNoiseMapper extends FlowJMapper
{
	  // spotnoise parameters.
	  public final static double		chance = 0.90f;
	  public final static double		minMagnitude = 0.05f;
	  private FlowJSpotNoise 		colornoise;

	  public FlowJColorNoiseMapper(ImageProcessor impr, float [][][] flow, int axes,
			int maxp, int maxq, double pScaling, double qScaling, double rho)
	  {
			super(impr, flow, axes, maxp, maxq, pScaling, qScaling, rho);
			colornoise = new FlowJSpotNoise((int []) impr.getPixels(), maxp, maxq, FlowJSpotNoise.SIGMA);
	  }
	  public void pixel(int ip, int iq, int ix, int iy, double dx, double dy)
	  /*
			Map the 2D flow in flow at ip, iq into pixels.
			Rho determines the maximal mapped magnitude.
	  */
	  {
			  double r = (double) Math.random();
			  if (r > chance)
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
					if (pv[0]/rho > minMagnitude)
					{
						  byte [] rgb = new byte[3];
						  FlowJDynamicColor.map2D(rgb, pv[0] / rho, pv[1]);
						  double rnd = 1;
						  int rrnd = (int) (rnd * (float) (rgb[0]&0xff));
						  int grnd = (int) (rnd * (float) (rgb[1]&0xff));
						  int brnd = (int) (rnd * (float) (rgb[2]&0xff));
						  colornoise.spot(ip, iq, FlowJSpotNoise.SIGMA
								+ FlowJSpotNoise.SIGMAM*pv[0]/rho, rrnd, grnd, brnd, v);
					}
			  }
	  }
}

