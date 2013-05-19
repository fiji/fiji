package VolumeJ;
import java.awt.*;
import volume.*;

/**
 * VJViewspaceUtil implements viewspace utility methods, that can be used to
 * relate a volume in a transformation matrix to the coordinates of that volume in viewspace.
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
public class VJViewspaceUtil
{
	/**
	 * Determine the optimal size for the rendering in coordinate system m of the whole v.
	 * @param v the volume to be rendered.
	 * @param m the transformation matrix translating from object space to viewspace.
	 * @return an int[3] containing the width int[0], height int[1] and depth int[2] of the optimal viewport.
	 */
	public static int [] suggestViewport(Volume v, VJMatrix m)
	{
		int [] size = new int[3];
		// Calculate extents of volume in viewspace.
		int [][] minmax = minmax(v, m);
		//VJUserInterface.write("minmax="+minmax[1][0]+" "+minmax[0][0]+" "+minmax[1][1]+" "+minmax[0][1]+" "+minmax[1][2]+" "+minmax[0][2]);
		size[0] = (int) Math.round(minmax[1][0] - minmax[0][0]); // x
		size[1] = (int) Math.round(minmax[1][1] - minmax[0][1]); // y
		size[2] = (int) Math.round(minmax[1][2] - minmax[0][2]); // z
		return size;
	}
	/**
	 * Determine the optimal size for a cine rendering in coordinate system m of the whole v.
	 * The size should allow for rotated volumes, therefore test the full extent of the rotation and use the maxima.
	 * @param v the volume to be rendered.
	 * @param m the transformation matrix translating from object space to viewspace.
	 * @param stepx, stepy, stepz the steps along each of the axes.
	 * @param n the number of steps.
	 * @return an int[3] containing the width int[0], height int[1] and depth int[2] of the optimal viewport.
	 */
	public static int [] suggestCineViewport(Volume v, VJMatrix m, double stepx, double stepy, double stepz, int n)
	{
		int maxx = 0, maxy = 0, maxz = 0;
		VJMatrix mc = new VJMatrix(m);
		// Calculate extents of volume in viewspace.
		VJUserInterface.status("Calculating viewport extent");
		for (int j = 0; j < n; j++)
		{
			int [][] minmax = minmax(v, mc);
			int x = (int) Math.round(minmax[1][0] - minmax[0][0]); // x
			int y =(int) Math.round(minmax[1][1] - minmax[0][1]); // y
			int z =(int) Math.round(minmax[1][2] - minmax[0][2]); // z
			if (x > maxx) maxx = x;
			if (y > maxy) maxy = y;
			if (z > maxz) maxz = z;
			// Rotate next step.
			VJMatrix mm = new VJMatrix();
			mm.rotatex(stepx);
			mc.mul(mm);
			mm = new VJMatrix();
			mm.rotatey(stepy);
			mc.mul(mm);
			mm = new VJMatrix();
			mm.rotatez(stepz);
			mc.mul(mm);
		}
		int [] size = new int[3];
		size[0] = maxx;
		size[1] = maxy;
		size[2] = maxz;
		return size;
	}
	/**
	 * Determine the center of volume v in coordinate system m.
	 * @param v the volume to be rendered.
	 * @param m the transformation matrix translating from object space to viewspace.
	 * @return an int[3] containing the x,y,z position of the center of v.
	 */
	public int [] centerVolume(Volume v, VJMatrix m)
	{
		float [] center = new float[4];
		center[0] = v.getWidth()/2; center[1] = v.getHeight()/2;
		center[2] = v.getHeight()/2; center[3] = 1;
		float [] centervs = m.mul(center);
		int [] centerivs = new int[3];
		centerivs[0] = (int) Math.round(centervs[0]);
		centerivs[1] = (int) Math.round(centervs[1]);
		centerivs[2] = (int) Math.round(centervs[2]);
		return centerivs;
	}
	/**
	 * Calculate minimum and maximum extents in xyz viewspace coordinates of volume v.
	 * Transform the eight corners of the dataset from
	 * objectspace into view space using the transformation
	 * matrix m, which converts from objectspace into viewspace.
	 * These eight new points are the
	 * minimum and maximum i,j and k values, in view space, where voxels
	 * are present. It is an upper bound for where to cast rays from in viewspace.
	 * @param v the volume to be rendered.
	 * @param m the transformation matrix translating from object space to viewspace.
	 * @return an int[2][3] containing min [0][..] and max [1][..] for the i,j,k axes.
	*/
	public static int [][] minmax(Volume v, VJMatrix m)
	{
		// Boundaries of the volume in view space
		float [][] boundary = new float[8][4];

		// Compute the corner points of the volume in objectspace.
		for (int i = 0; i < boundary.length; i++)
		{
			boundary[i][0] = 0; // x
			boundary[i][1] = 0; // y
			boundary[i][2] = 0; // z
			boundary[i][3] = 1; // w
		}
		for (int i = 4; i < boundary.length; i++)
			boundary[i][2] = (float) (v.getDepth()-1);
		boundary[1][0] = (float) (v.getWidth()-1);
		boundary[2][0] = (float) (v.getWidth()-1);
		boundary[2][1] = (float) (v.getHeight()-1);
		boundary[3][1] = (float) (v.getHeight()-1);
		boundary[5][0] = (float) (v.getWidth()-1);
		boundary[6][0] = (float) (v.getWidth()-1);
		boundary[6][1] = (float) (v.getHeight()-1);
		boundary[7][1] = (float) (v.getHeight()-1);
		// Convert each of the corner points into viewspace.
		float [][] boundaryVs = new float[8][];
		for (int i = 0; i < boundaryVs.length; i++)
			boundaryVs[i] = m.mul(boundary[i]);
		// Find the maxima among each of the corner points in viewspace.
		int [][] minmax = new int[2][3];
		minmax[0][0] = minmax[1][0] = (int) boundaryVs[0][0];

		minmax[0][1] = minmax[1][1] = (int) boundaryVs[0][1];
		minmax[0][2] = minmax[1][2] = (int) boundaryVs[0][2];
		// sort.
		for (int i = 1; i < boundaryVs.length; i++)
		{
			for (int j = 0; j < 3; j++)
			{
				if (minmax[0][j] > (int) boundaryVs[i][j])
					minmax[0][j] = (int) boundaryVs[i][j];
				if (minmax[1][j] < (int) boundaryVs[i][j])
					minmax[1][j] = (int) boundaryVs[i][j];
			}
		}
		// Round to above.
		minmax[1][0]++; minmax[1][1]++; minmax[1][2]++;
		return minmax;
	}
}

