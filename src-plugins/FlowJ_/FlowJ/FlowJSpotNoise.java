package FlowJ;
import ij.*;
import ij.process.*;
import volume.*;
import VolumeJ.*;
import bijnum.*;

/**
	 This class implements spot noise displaying of flow fields.
	 On occasion you may wonder about why I coded it the way I did: the only reason is
	 that the JDK 1.1.8 javac compiler contains a bug that has to do with variables
	 local to a method that are put on the stack.
	 All weird codes are workarounds around this problem.

	 Copyright (c) 2000, Michael Abramoff. All rights reserved.

	 Reference: J.J. van Wijk, 1991

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
public class FlowJSpotNoise
{
	  // spotnoise parameters.
	  // larger sigma gives larger spots.
	  public final static double              SIGMA = 0.4f;
	  // larger sigmam gives more linearly oriented spotnoise
	  public final static double              SIGMAM = 2;

	  private Object 			pixels;
	  private boolean			isInt = false;
	  protected int 			maxp;
	  protected int 			maxq;
	  protected double  			sigmas;
	  public int				width;
	  // Java doesnt tolerate many variables on the stack.
	  protected static double               magnitude, correction, e1, e2, ge;
	  protected static int                  i, j, p, q, index, value;
	  protected static double               e1d, e2d, e12, e22, sigmas2, sigmal2;
	  protected static double 	        v0i, v0j, v1i, v1j;
	  protected static int                  rcomposite, gcomposite, bcomposite;
	  protected static int                  rp, gp, bp;

	  public FlowJSpotNoise()  {}
	  public FlowJSpotNoise(byte [] pixels, int maxp, int maxq, double sigmas)
	  {
			this.pixels = (Object) pixels;
			this.maxp = maxp;
			this.maxq = maxq;
			this.sigmas = sigmas;
			isInt = false;
	  }
	  public FlowJSpotNoise(int [] pixels, int maxp, int maxq, double sigmas)
	  {
			this.pixels = (Object) pixels;
			isInt = true;
			//ingerited from SpotNoise.
			this.maxp = maxp;
			this.maxq = maxq;
			this.sigmas = sigmas;
	  }
	  public void spot(int ip, int iq, double sigmal, int gray, float [] v)
	  /*
			Put a spotnoise in pixels.
			The pixel at ip, iq in pixels is the center of a spotnoise.
			The spotnoise is clipped from 0->maxp and 0->maxq.
			sigmal determines the length of the spotnoise Gaussian (in pixels).
			gray ([0-255]) is the color the spotnoise is derived from.
			v is the velocity vector that determines, together with sigma and sigma2,
			the width and length of the spotnoise.
	  */
	  {
			magnitude = BIJmatrix.norm(v);
			width = widthEllipse(sigmal);
			for (j = -width/2; j < width/2; j++)
			for (i = -width/2; i < width/2; i++)
			{
					p = ip+i; q = iq+j;
					if (p >= 0 && p < maxp && q >= 0 && q < maxq)
					{
						// rotate the velocity vector.
						v0i = v[0] * (float) i;
						v1j = v[1] * (float) j;
						v1i = v[1] * (float) i;
						v0j = v[0] * (float) j;
						e1d = (v1i + v0j);
						e2d = (v0i - v1j);
						e1 = e1d / magnitude;
						e2 = e2d / magnitude;
						// determine spot value.
						e12 = (float) Math.pow(e1,2);
						sigmas2 = (float) Math.pow(sigmas,2);
						e22 = (float) Math.pow(e2,2);
						sigmal2 = (float) Math.pow(sigmal,2);
						ge = (float) Math.exp(- (e12/sigmas2 + e22/sigmal2) / 2.0);
						// Bug in stack processing.
						index = q*maxp+p;
						value = (int) (((byte []) pixels)[index])&0xff;
						// Put the spot part into the image.
						((byte []) pixels)[index] = (byte) composePixel(value, gray, ge);
					}
			}
	  }
	  public void spot(int ip, int iq, double  sigmal, int r, int g, int b, float [] v)
	  /*
			Show a colored (rgb) spotnoise.
			The pixel at ip, iq in pixels is made part of a colored spotnoise.
			The spotnoise is clipped from 0->maxp and 0->maxq.
			sigma2 determine the length of the spotnoise.
			r,g,b [0-255] is the color the spotnoise is derived from.
			v is the velocity vector that determines, together with sigma and sigma2,
			the width and length of the spotnoise.
	  */
	  {
			magnitude = BIJmatrix.norm(v);
			width = widthEllipse(sigmal);
			if (width == 0)
				return;
			for (j = -width/2; j < width/2; j++)
			for (i = -width/2; i < width/2; i++)
			{
				p = ip+i; q = iq+j;
				if (p >= 0 && p < maxp && q >= 0 && q < maxq)
				{
						// rotate the velocity vector.
						v0i = v[0] * (float) i;
						v1j = v[1] * (float) j;
						v1i = v[1] * (float) i;
						v0j = v[0] * (float) j;
						e1d = (v1i + v0j);
						e2d = (v0i - v1j);
						e1 = e1d / magnitude;
						e2 = e2d / magnitude;
						// determine spot value.
						e12 = (float) Math.pow(e1,2);
						sigmas2 = (float) Math.pow(sigmas,2);
						e22 = (float) Math.pow(e2,2);
						sigmal2 = (float) Math.pow(sigmal,2);
						ge = (float) Math.exp(- (e12/sigmas2 + e22/sigmal2) / 2.0);
						// Bug in stack processing.
						index = q*maxp+p;
						value = ((int []) pixels)[index];
						((int []) pixels)[index] = compositePixel(value, r, g, b, ge);
				}
			}
	  }
	  protected float gaussianEllipse(double  e1, double  e2, double sigmas, double sigmal)
	  // Two dimensional Gaussian value at a,b [0-1]. e1 is short axis, e2 is long axis.
	  {
			  return (float) Math.exp(- (Math.pow(e1,2)/Math.pow(sigmas,2) + Math.pow(e2,2)/Math.pow(sigmal,2)) / 2);
	  }
	  protected int widthEllipse(double sigma)
	  {
				double  sigma6i;

				sigma6i = 6 * sigma + 1;
				if ((sigma6i % 2) == 0)
					return (int) sigma6i+1;
				else
					return (int) sigma6i;
	  }
	  protected float sigmaEllipse(int width)
	  { return ((float) width - 1) / 6; }
	  private int composePixel(int pxl, int gray, double ge)
	  {
			  pxl = Math.min((int) (ge * (float) gray)+pxl, 255);
			  return pxl;
	  }
	  private int compositePixel(int pxl, int r, int g, int b, double ge)
	  {
			  rp = (int)((pxl>>16)&0xff);
			  gp = (int)((pxl>>8)&0xff);
			  bp = (int)((pxl)&0xff);
			  correction = ge;
			  rcomposite = Math.min((int) (correction * (float) r) + rp, 255);
			  gcomposite = Math.min((int) (correction * (float) g) + gp, 255);
			  bcomposite = Math.min((int) (correction * (float) b) + bp, 255);
			  pxl = (rcomposite << 16) | (gcomposite << 8) | bcomposite;
			  return pxl;
	  }
}

