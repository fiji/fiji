package FlowJ;
import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import volume.*;
import ij.measure.*;
import bijnum.*;

/**
 * This class implements 2D flow fields and methods (including reading and
 * writing and utility methods).
 * Reading and writing is done in the famous Burkitt format to be compatible with other optical flow
 * packages.
 * The Burkitt format is defined as follows:
 * <width: float><height: float><computed width: float><computed height: float>
 * <x-offset from edge: float><y-offset from edge:float>
 * <x-component of flow vector at 0,0: float><y-component of flow vector at 0,0: float>
 * <x-component of flow vector at 0,1: float><y-component of flow vector at 0,1: float>
 * ...
 * ...
 *
 * where float = 4 byte little endian float.
 *
 * Originaly written in C++ by Michael Abramoff, 1997.
 * Translated to Java, Michael Abramoff, 1999
 *
 * @author: Michael Abramoff, (c) 1999-2003, Michael Abramoff. All rights reserved.
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
public class FlowJFlow
{
        public VolumeFloat v;
        public boolean     full[][];       // full or normal velocity?
        private float []  avg = new float[2];
        private Calibration c;

        /* Create a new flow field. */

        public FlowJFlow() {}

        public FlowJFlow(int width, int height)
        {
                        v = new VolumeFloat(2, width, height, 1, 1, 1);/* flowx, flowy */
                        full = new boolean[height][width];
        }
        public ImageStack toStack()
        /* Convert the flow field to two images with the x and y vector components as pixel intensities. */
        {
                ImageStack is = new ImageStack(getWidth(), getHeight());
                FloatProcessor fp = new FloatProcessor(getWidth(), getHeight());
                float [] im = (float [])fp.getPixels();
                for (int y = 0; y < getHeight(); y++)
                {
                        int offset = y * getWidth();
                        for (int x = 0; x < getWidth(); x++)
                                        im[offset + x] = v.v[y][x][0];
                }
                is.addSlice("x", fp);
                fp = new FloatProcessor(getWidth(), getHeight());
                im = (float [])fp.getPixels();
                for (int y = 0; y < getHeight(); y++)
                {
                        int offset = y * getWidth();
                        for (int x = 0; x < getWidth(); x++)
                                        im[offset + x] = v.v[y][x][1];
                }
                is.addSlice("y", fp);
                return is;
        }
        public void setCalibration(Calibration c) { this.c = c; }
        public boolean valid(int x, int y) { return v.valid(x, y); }
        public void set(int x, int y, float vx, float vy)
        {  set(x, y, vx, vy, true); }
        public void set(int x, int y, float vx, float vy, boolean fullFlow)
        {
                        v.v[y][x][0] = (float) vx; v.v[y][x][1] = (float) vy; full[y][x] = fullFlow;
        }
        public float [] get(int x, int y)
        {
                        float [] vv = new float[2];
                        vv[0] = v.v[y][x][0];
                        vv[1] = v.v[y][x][1];
                        return vv;
        }
        public int getWidth() { return v.getHeight(); }
        public int getHeight() { return v.getDepth(); }
        public float getX(int x, int y) { return v.v[y][x][0]; }
        public float getY(int x, int y) { return v.v[y][x][1]; }
        public boolean full(int x, int y) {  return full[y][x]; }
        public String getTitle(int mappingChoice, float scale, float rho)
        {
                        return FlowJDisplay.toString(mappingChoice, scale, rho);
        }
        public ImageProcessor mapImage(ImageProcessor background, int mappingChoice, int axis, float scale, float rho)
        {
                return FlowJDisplay.mapImage(v.v, full, background, mappingChoice, axis, scale, scale, rho);
        }
        public float getMagnitude(int x, int y)
        {
                        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight())
                        {
                                        float [] p = new float[2];
                                        polar(p, get(x, y));
                                        if (full[y][x])
                                                                return p[0];
                                        else
                                                                return 0;
                        }
                        else return -1;
        }
        public float getAlphaDeg(int x, int y)
        {
                        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight())
                        {
                                        float [] p = new float[2];
                                        polar(p, get(x, y));
                                        float a = p[1] * 360 / ((float) Math.PI * 2);
                                        if (a >= 360)
                                                        return a - 360;
                                        else
                                                        return a;
                        }
                        else return -1;
        }
        public String calibratedMagnitudeString(float magnitude, int significance)
        /*
         Returns the spatially calibrated flow magnitude as a string.
        */
        {
                return IJ.d2s(c.pixelWidth * magnitude, significance)+c.getUnits()+"/frame";
        }
        public int average(Roi roi)
        /*
                compute the average flow over an area defined in Roi.
                Return the surface area in pixels.
        */
        {
		  Rectangle r = roi.getBoundingRect();
		  float xSum = 0;
		  float ySum = 0;
		  int s = 0;
		  for (int y = 0; y < r.height; y++)
		  {
			for (int x = 0; x < r.width; x++)
			{
				if (full[r.y + y][r.x + x] && roi.contains(r.x + x, r.y + y))
				{
					xSum += v.v[r.y + y][r.x + x][0];
					ySum += v.v[r.y + y][r.x + x][1];
					s++;
				}
			}
		  }
		  avg[0] = xSum / (float) s;
		  avg[1] = ySum / (float) s;
		  return s;
	}
	/** Return the angle of the previously computed average flow. */
	public float averageAlpha()
	{
		  float [] p = new float[2];
		  polar(p, avg[0], avg[1]);
		  return p[1] * 360 / ((float) Math.PI * 2);
	}
	/** Return the length of the previously computed average flow. */
	public float averageMagnitude()
	{
		float [] p = new float[2];
		polar(p, avg[0], avg[1]);
		return p[0];
	}
	/** Read a flow field (Burkitt format) from  a file. */
	private float readfloat(InputStream s)
	{
		byte[] buffer = new byte[4];
		try {s.read(buffer, 0, buffer.length); }
		catch (IOException e) {IJ.write("error reading float" + e); return (0);}
		int tmp = (int)(((buffer[0]&0xff)<<24)
								  | ((buffer[1]&0xff)<<16)
								  | ((buffer[2]&0xff)<<8)
								  | (buffer[3]&0xff));
		return Float.intBitsToFloat(tmp);
	}
	private void writefloat(float d, OutputStream s)
	{
		byte[] buffer = new byte[4];
		int tmp = Float.floatToIntBits(d);
		buffer[0] = (byte)((tmp>>24)&0xff);
		buffer[1] = (byte)((tmp>>16)&0xff);
		buffer[2] = (byte)((tmp>>8)&0xff);
		buffer[3] = (byte)(tmp&0xff);
		try {s.write(buffer, 0, buffer.length); }
		catch (IOException e) {IJ.write("error writing float" + e); return;}
	}
	/**
         * Read a flow field defined in a file.
         * File contains a header with in (4 byte float) original width, original height,
         * computed width, computed height, offset from edge x, offset from edge y.
         * and then the v.v as 4 byte floats (x,y....).
         * @param fileName a String with the name of the file and path to read.
         * @return true if succesfully read from the file, false otherwise.
	*/
	public boolean read(String fileName)
	{
                InputStream is = null;
                try {is = new FileInputStream(fileName);}
                catch (IOException e) {IJ.write("" + e); return false;}
		int width = (int) readfloat(is);
		int height = (int) readfloat(is);
		// allocate the arrays.
		v = new VolumeFloat(2, width, height); /* flowx, flowy */
		int size = getWidth()* getHeight();
		full = new boolean[height][width];
		int endwidth = (int) readfloat(is);
		int endheight = (int) readfloat(is);
		int startwidth = (int) readfloat(is);
		int startheight = (int) readfloat(is);
		for (int y = 0; y < v.getHeight(); y++)
		{
			  if (y >= startheight && y < endheight)
			  {
					byte[] buffer = new byte[getWidth()*4*2];
					try {is.read(buffer, 0, buffer.length); }
					catch (IOException e) {IJ.write("" + e); return false;}
					int tmpx = 0; int tmpy = 0;
					for (int x = startwidth; x < endwidth; x++)
                                        {
                                                tmpx = (int)(((buffer[x*4*2+0]&0xff)<<24) | ((buffer[x*4*2+1]&0xff)<<16)
                                                  | ((buffer[x*4*2+2]&0xff)<<8) | (buffer[x*4*2+3]&0xff));
                                                tmpy = (int)(((buffer[x*4*2+4+0]&0xff)<<24) | ((buffer[x*4*2+4+1]&0xff)<<16)
                                                  | ((buffer[x*4*2+4+2]&0xff)<<8) | (buffer[x*4*2+4+3]&0xff));
                                                if ((Float.intBitsToFloat(tmpx) == 100) && (Float.intBitsToFloat(tmpy) == 100))
                                                        set(x, y, 0, 0, false);
                                                else
                                                        set(x, y, Float.intBitsToFloat(tmpx), Float.intBitsToFloat(tmpy), true);
					}
			  }
			  IJ.showStatus("Reading... "+(100*(y*getHeight()))/size+"%");
		}
		try {is.close(); }
		catch (IOException e) {IJ.write("" + e); return false;}
		return true;
	}
	/**
         * Write the full flow field in Burkitt format into a file.
         * @param fileName a String with the name of the file and path to write.
	*/
	public void write(String fileName)
	{
                OutputStream s = null;
                try {s = new FileOutputStream(fileName);}
                catch (IOException e) {IJ.write("" + e); return;}
		// Write the dimensions of the file. */
		writefloat(getWidth(), s);
		writefloat(getHeight(), s);
		writefloat(getWidth(), s);
		writefloat(getHeight(), s);
		writefloat(0, s);
		writefloat(0, s);
		for (int y = 0; y < getHeight(); y++)
		{
                        byte[] buffer = new byte[getWidth()*4*2];
                                int tmp=0;
                        for (int x = 0; x < getWidth(); x++)
                        {
                                        if (full[y][x])
                                                tmp = Float.floatToIntBits((float) getX(x, y));
                                  else
                                                tmp = Float.floatToIntBits(100);
                                  buffer[x*4*2+0] = (byte)((tmp>>24)&0xff);
                                  buffer[x*4*2+1] = (byte)((tmp>>16)&0xff);
                                  buffer[x*4*2+2] = (byte)((tmp>>8)&0xff);
                                  buffer[x*4*2+3] = (byte)(tmp&0xff);
                                  if (full[y][x])
                                                tmp = Float.floatToIntBits((float) getY(x, y));
                                  else
                                                tmp = Float.floatToIntBits(100);
                                  buffer[x*4*2+4+0] = (byte)((tmp>>24)&0xff);
                                  buffer[x*4*2+4+1] = (byte)((tmp>>16)&0xff);
                                  buffer[x*4*2+4+2] = (byte)((tmp>>8)&0xff);
                                  buffer[x*4*2+4+3] = (byte)(tmp&0xff);
                        }
                        try {s.write(buffer, 0, buffer.length); }
                        catch (IOException e) {IJ.write("cannot write flow field" + e); return;}
                        IJ.showStatus("Writing... "+(100*(y*v.getHeight()))/(v.getWidth()*v.getHeight())+"%");
		}
		try {s.close(); }
		catch (IOException e) {IJ.write("" + e); return;}
	}
	public void createRotation(int centerX, int centerY, float angle, Roi roi)
   /*
		  Put a rotation (over angle) flow field in flow, and
		  include the part within the current ROI.
		  Only the v.v within the roi will be valid.
   */
   {
		  Rectangle r=null;
		  if (roi instanceof Roi)
				r = roi.getBoundingRect();
		  int fulls = 0;
		  for (int y = 0; y < getHeight(); y++)
		  {
			  for (int x = 0; x < getWidth(); x++)
			  {
					// set to invalid.
					set(x, y, 0, 0, false);
					boolean doflag = ! (roi instanceof Roi);
					if (! doflag)
						doflag = x > r.x && x < r.x+r.width && y > r.y && y < r.y+r.height && roi.contains(x, y);
					if (doflag)
					{
						  // Compute expected velocity.
						  float [] fv = polar((x - centerX) + 0.5f, - (y - centerY) + 0.5f);
						  float [] expA = new float[2];
						  expA[0] = fv[0] * (float) Math.tan(-angle); // angular velocity in image units.
						  expA[1] = fv[1] + (float) Math.PI / 2;       // direction of angular velocity (+90 degrees)
						  float [] expG = new float[2];
						  FlowJFlow.toGrid(expG, expA);       // true displacement in pixels.
						  // put in flow.
						  set(x, y, expG[0], expG[1]);
						  fulls++;
					}
				}
			}
			IJ.write("Rotate "+centerX+", "+centerY+"; "+fulls+" pixels in flow field");
	}
        /**
         * Return the magnitude of a rectangular motion vector.
         */
        public static float magnitude3D(float [] r)
        {  return (float) Math.sqrt(r[0]*r[0] + r[1]*r[1] + r[2]*r[2]);  }
        public static float orientationxy(float [] r) { return orientationxy(r[0], r[1]); }
        /** return the orientation (in radians) in the xy plane of a rectangular motion vector r */
        public static float orientationxy(float vx, float vy)
        {
                float orient;

                if  (vx == 0 && vy == 0)
                                orient = 0;
                else if (vx > 0 && vy >= 0)
                                orient = (float) Math.atan(vy / vx);
                else if (vx <= 0 && vy > 0)
                                orient = (float) Math.PI / 2 + (float) Math.atan(-vx / vy);
                else if (vx < 0 && vy <= 0)
                                orient = (float) Math.PI + (float) Math.atan(-vy/ -vx);
                else
                                orient = (float) Math.PI * 3 / 2 + (float) Math.atan(vx / -vy);
                if (orient >= Math.PI * 2)  // primitive modulo 360 degrees.
                                orient -= Math.PI * 2;
                if (orient < 0)  // primitive modulo 360 degrees.
                                orient += Math.PI * 2;
                return orient;
        }
        public static float orientationz(float [] r)
        /* return the orientation (in radians) relative to the xy plane of a rectangular motion vector r */
        {
                float lxy = r[0]*r[0]+r[1]*r[1];
                if (lxy == 0)
                                return (float) Math.PI / 2 + ((r[2] > 0) ? 0 : (float) Math.PI);
                else
                                return (float) Math.atan(r[2] / (float) Math.sqrt(lxy));
        }
        public static void polar3D(float [] p, float [] r)
        /* Convert a rectangular vector r to a vector in polar coordinates p. */
        {
                p[0] = magnitude3D(r);
                p[1] = orientationxy(r);
                p[2] = orientationz(r);
        }
        public static void polar3D(float [] p, float vx, float vy, float vz)
        /* Convert a rectangular vector vx, vy, vz to a vector in polar coordinates p. */
        {
                float lxy = vx*vx+vy*vy;
                p[0] = (float) Math.sqrt(lxy+vz*vz);
                p[1] = orientationxy(vx, vy);
                if (lxy == 0)
                                p[2] = (float) Math.PI / 2 + ((vz > 0) ? 0 : (float) Math.PI);
                else
                                p[2] = (float) Math.atan(vz / Math.sqrt(lxy));
        }
        public static void polar(float [] p, float rx, float ry)
        /* Convert a rectangular vector rx, ry to a vector in polar coordinates p. */
        {
                p[0] = (float) Math.sqrt(rx*rx + ry*ry);
                p[1] = orientationxy(rx, ry);
        }
        public static float [] polar(float rx, float ry)
        /* Convert a rectangular vector rx, ry to a vector in polar coordinates p. */
        {
                float [] p = new float[2];
                p[0] = (float) Math.sqrt(rx*rx + ry*ry);
                p[1] = orientationxy(rx, ry);
                return p;
        }
        /** Convert a rectangular vector r to a vector in polar coordinates p. */
        public static float [] polar(float [] r)
        {
                float [] p = new float[2];
                p[0] = (float) Math.sqrt(r[0]*r[0] + r[1]*r[1]);
                p[1] = orientationxy(r);
                return p;
        }
        public static void polar(float [] p, float [] r)
        {
                polar(p, r[0], r[1]);
        }
        /** Translate to grid coordinates. grid[0] = x, grid[1] = y. */
        public static void toGrid(float [] grid, float [] polar)
        {
                grid[0] = (float) Math.cos(polar[1]) * polar[0];  // x = cos * length
                grid[1] = (float) Math.sin(polar[1]) * polar[0];  // y = sin * length
        }
}
