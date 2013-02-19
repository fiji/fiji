package FlowJ;
import java.awt.*;
import ij.*;
import ij.process.*;
/**
 * This class maps a quiver map of a flow field.
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
public class FlowJQuiverMapper extends FlowJMapper
{
	  public FlowJQuiverMapper(ImageProcessor impr, float [][][] flow, int axes,
			int maxp, int maxq, double pScaling, double qScaling, double rho)
	  {
			super(impr, flow, axes, maxp, maxq, pScaling, qScaling, rho);
	  }
	  public void pixel(int ip, int iq, int ix, int iy, double dx, double dy)
	  /*
			Map the 2D flow in flow at ip, iq into pixels.
			Rho determines the maximal mapped magnitude.
	  */
	  {
			if ((ip % ((int) pScaling) == 0) &&
				 (iq % ((int) qScaling) == 0))
			{
				double r =  Math.random();
				float [] v;
				if (pScaling == 1 && qScaling == 1)
				{
						v = new float[2];
						v[0] = flow[iy][ix][0]; v[1] = flow[iy][ix][1];
				}
				else
						v = bl(flow, ix, iy, dx, dy);
				float [] pv = FlowJFlow.polar(v);
				Color c = new Color(0,0,0);
				impr.setColor(c);
				// There is a very bad stack related bug in ByteProcessor.
				double shiftp = v[0] / rho * pScaling;
				double shiftq = v[1] / rho * qScaling;
				if (shiftp >= pScaling) shiftp--;
				if (shiftp <= -pScaling) shiftp++;
				if (shiftq >= qScaling) shiftq--;
				if (shiftq <= -qScaling) shiftq++;
				// These have been declared static since there is some bug here.
				int shiftip = (int) (shiftp + 0.5);
				int shiftiq = (int) (shiftq + 0.5);
				int itp, itq;
				if (shiftip < 0)
					itp = ip + shiftip;
				else
				{
					itp = ip;
					ip = ip - shiftip;
				}
				if (shiftiq > 0)
					itq = iq - shiftiq;
				else
				{
					itq = iq;
					iq = iq + shiftiq;
				}
				// Weird bug. Took me weeks.
				impr.moveTo(ip, iq);
				impr.lineTo(itp, itq);
			}
	  }
}

