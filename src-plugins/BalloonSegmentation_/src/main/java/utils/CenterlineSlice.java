package utils;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.lang.Math.*;
import java.awt.*;

import java.io.*;
import java.util.*;


/*********************************************************************
 * Version: May, 2008
 ********************************************************************/

/*********************************************************************
 * Lionel Dupuy
 * SCRI
 * EPI program
 * Invergowrie
 * DUNDEE DD2 5DA
 * Scotland
 * UK
 *
 * Lionel.Dupuy@scri.ac.uk
 * http://www.scri.ac.uk/staff/lioneldupuy
 ********************************************************************/


public // Obtain a slice perpendicular to the confocal plane along a collumn of cells.
class CenterlineSlice {
	public ImagePlus i1;				// reference image
	private ImageStack stack1;			// related stack
    public Vector Points;				// points defining the path along which to slice the image
    private int nx,ny,nz;
	byte[] r,g,b;						// red green blue

    public CenterlineSlice (ImagePlus imp)
    {
	i1 = imp;
		stack1 = i1.getStack();
		ImageProcessor ip1 = stack1.getProcessor(1);
		nx = ip1.getWidth();
		ny = ip1.getHeight();
		nz = i1.getStackSize();

		r = new byte[nx*ny];
	    g = new byte[nx*ny];
	    b = new byte[nx*ny];

	    Points = new Vector();
    }

    public void GenerateSlice()
    {
		ColorProcessor cp = new ColorProcessor(Points.size(), nz);

		for (int j=0;j<nz;j++)
		{
			ColorProcessor ip1 = (ColorProcessor)(stack1.getProcessor(j+1));
	        ip1.getRGB(r,g,b);
		for (int i=0;i<Points.size();i++)
		{
			double x = ((double[])(Points.get(i)))[0];
			double y = ((double[])(Points.get(i)))[1];
			int[] pix = AveragePixelValue(x, y);
				cp.putPixel(i,j, (((int)pix[0] & 0xff) <<16)+ (((int)pix[1] & 0xff) << 8) + ((int)pix[2] & 0xff));
		}

	}
		ImagePlus imp_slice =  new ImagePlus("Projection",cp);
		imp_slice.show();
    }

    public void getPoints(int[][] LIST)
    {
	int N = LIST.length;
	IJ.log("  " + N);
	Points.clear();
	for (int i=1;i<N;i++)
	{
		int x1 = LIST[i-1][0];
		int y1 = LIST[i-1][1];
		int x2 = LIST[i][0];
		int y2 = LIST[i][1];
		double L = Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
		double vx = (x2-x1)/L;
		double vy = (y2-y1)/L;
		IJ.log("    " + i + "  /  " + L);
		for (int j=0;j<(int)L+1;j++)
		{
			IJ.log("          " + j);
			double[] xy = {x1+j*vx,x2+j*vy};
			Points.add(xy);
		}
	}
    }
    private int[] AveragePixelValue(double x, double y)
    {
	int i = (int)x;
	int j = (int)y;
	int l = i+j*nx;
	int[] pix = {r[l], g[l],b[l]};
	return pix;
    }
}
