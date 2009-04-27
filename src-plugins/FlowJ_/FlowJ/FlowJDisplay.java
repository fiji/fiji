package FlowJ;
import java.awt.*;
import ij.*;
import ij.process.*;
import volume.*;
import VolumeJ.*;

/**
 * OFDisplay class implements the 2D display of 2D flow fields in different formats.
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
public class FlowJDisplay extends Object
{
	  // 2D and 3D
	  public static final int             DCM2D = 0;
	  public static final int             QUIVER = 1;
	  public static final int             DCNOISE = 2;
	  public static final int             SPOTNOISE = 3;
	  // 3D only
	  public static final int             DCM3D = 4;
	  public static final int             DCM3DNOISE = 5;
	  public static String []             description = {"dcm 2D", "quiver", "dc 2D noise", "spotnoise" };

	  /*
				Java doesnt tolerate many variables on the stack or locally.
				The compiler doesnt warn for this.
	  */
	  private static double p, q, x, y, dx, dy;
	  private static int ix, iy, ip, iq, maxp, maxq;

	  public static ImageProcessor quiver(FlowJFlow f, double scale, double rho)
	  {
			return mapImage(f.v.v, f.full, null, QUIVER, 0, scale, scale, rho);
	  }
	  public static ImageProcessor dcmImage(FlowJFlow f, ImageProcessor image, double scale, double rho)
	  {
			return mapImage(f.v.v, f.full, image, DCM2D, 0, scale, scale, rho);
	  }
        /**
        * Convert FlowJDisplay parameters to a String.
        */
        public static String toString(int mapping, double scale, double rho)
        {
			String s;

			s = "Flow "+description[mapping];
			s+=" "+rho;
			return s;
        }
        /**
         * Map the 2D flow field f into an imageprocessor ([vx, vy][y][x]).
         * @param f is a 2D flow field.
         * @param full tells whether or not the f at x,y is a full flow field.
         * @param image is an optional 2D background image.
         * @param mapping determines one of the mappingtypes.
         * @param mappingaxes determines the axes along which the mapping will be rotated.
         * @param pFactor and qFactor are 2D scaling factors.
         * @param rho determines the maximal mapped magnitude.
        */
        public synchronized static ImageProcessor mapImage(float [][][] f, boolean [][] full,
               ImageProcessor image, int mapping, int mappingaxes,
               double pFactor, double qFactor, double rho)
	  {
			maxp = (int) (f[0].length * pFactor);
			maxq = (int) (f.length * qFactor);
			ImageProcessor processor;
			if (mapping == QUIVER || mapping == SPOTNOISE)
					processor = new ByteProcessor(maxp, maxq);
			else
					processor = new ColorProcessor(maxp, maxq);
			Object pixels = processor.getPixels();
			FlowJMapper mapper = null;
			switch (mapping)
			{
					case DCNOISE:
						mapper = new FlowJColorNoiseMapper(processor, f, mappingaxes,
							maxp, maxq, pFactor, qFactor, rho);
						break;
					case SPOTNOISE:
						mapper = new FlowJSpotNoiseMapper(processor, f, mappingaxes,
							maxp, maxq, pFactor, qFactor, rho);
						break;
					case DCM3DNOISE:
						mapper = new Flow3JColorNoiseMapper(processor, f, mappingaxes,
							maxp, maxq, pFactor, qFactor, rho);
						break;
					case QUIVER:
						mapper = new FlowJQuiverMapper(processor, f, mappingaxes,
							maxp, maxq, pFactor, qFactor, rho);
						break;
					case DCM2D:
						mapper = new FlowJDynamicColorMapper(processor, f, mappingaxes,
							maxp, maxq, pFactor, qFactor, rho);
						break;
					case DCM3D:
						mapper = new Flow3JDynamicColorMapper(processor, f, mappingaxes,
							maxp, maxq, pFactor, qFactor, rho);
						break;
			}
			IJ.showStatus("Rendering...");
			for (iq = 0; iq < maxq; iq++)
			{
				  IJ.showProgress((float) iq/(float) maxq);
				  for (ip = 0; ip < maxp; ip++)
				  {
						//p = (float) ip; q = (float) iq;
						x = (float) ip / pFactor; y = (float) iq / qFactor;
						ix = (int) x; iy = (int) y;
						dx = x - (float) ix;
						dy = y - (float) iy;
						if (ix < f[0].length - 1 && iy < f.length - 1)
						{
							  int index = iq*maxp+ip;
							  // Map the background if applicable.
							  if (image != null)
							  {
									if (image instanceof ByteProcessor)
									{
											if (pixels instanceof byte [])
													((byte [])pixels)[index] = (byte) bilinearV((byte [])image.getPixels(), image.getWidth(), ix, iy, dx, dy);
											else if (pixels instanceof int [])
											{
													int b = (int) bilinearV((byte [])image.getPixels(), image.getWidth(), ix, iy, dx, dy);
													((int []) pixels)[index] = (b<<16)+(b<<8)+(b);
											}
									}
							  }
							  else if (mapping == QUIVER)
									// quiver likes a white background with black quivers.
									((byte [])pixels)[index] = (byte) 255;

							  boolean valid = (full == null) || (full instanceof boolean [][] && full[iy][ix]);
							  // Do the actual mapping.
							  if (valid)
									mapper.pixel(ip, iq, ix, iy, dx, dy);
						}
				  }
			}
			IJ.showStatus("");
			return processor;
	  }
	  private static double bilinearV(byte [] i, int width, int x, int y, double dx, double dy)
	  // Bilinear interpolation of 1D vector representing 2D map.
	  {
				double p11, p12, p1, p21, p22, p2, r1;

				if (dx == 0 && dy == 0)
					  return (float) i[y * width + x];
				p11 = (float) i[y * width + x];
				p12 = (float) i[y * width + x+1];
				p1 = ((p12 - p11) * dx) + p11;
				p21 = (float) i[(y+1) * width + x];
				p22 = (float) i[(y+1) * width + x +1];
				p2 = ((p22 - p21) * dx) + p21;
				r1 = ((p2 - p1) * dy) + p1;
				return r1;
	  }
}

