package VolumeJ;
import ij.*;
import volume.*;

/**
 * This class is the abstract class for all <code>volume</code> interpolators and defines the methods
 * for each of these classes.
 *
 * @see volume.Volume
 * @see VJGradient
 * @see VJValue
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
public abstract class VJInterpolator
{
	/**
	 *  Does vl fall within the bounds of volume for a specific interpolation kernel?
	 *  @param vl a VJVoxelLoc for which you want to know whether it falls inside the bounds,
	 *  taking account of support.
	 *  @param v the volume to be interpolated.
	 *  @return boolean whether or not vl falls within the bounds.
	*/
	public abstract boolean isValid(VJVoxelLoc vl, Volume v);
	/**
	 *  Does vl fall within the bounds of volume for nearest neighbor gradient interpolation?
	 *  @param vl a VJVoxelLoc for which you want to know whether it falls inside the bounds,
	 *  taking account of support for the gradient kernel.
	 *  @param v the volume to be interpolated.
	 *  @return boolean whether or not vl falls within the bounds.
	*/
	public abstract boolean isValidGradient(VJVoxelLoc vl, Volume v);
	/**
	 *  Does cell c fall within the bounds of a volume for nearest neighbor gradient interpolation?
	 *  @param c a VJCell for which you want to know whether it falls inside the bounds,
	 *  taking account of support for the gradient kernel.
	 *  @param v the volume to be interpolated.
	 *  @param boolean whether or not c falls within the bounds.
	*/
	public abstract boolean isValidGradient(VJCell c, Volume v);
	/**
	 * Interpolate the value of v at location vl.
	 * voxel must be instantiated as a (sub)class of VJValue.
	 * @param voxel a VJValue which will contain the interpolated voxel value on exit.
	 * @param v a volume
	 * @param vl a location in the volume.
	 * @return voxel, which contains the value in v at vl.
	 */
	public abstract VJValue value(VJValue voxel, Volume v, VJVoxelLoc vl);
	/**
	 * Interpolate the gradient of v at location vl.
	 * @param v a volume
	 * @param vl a location in the volume.
	 * @return a VJGradient which contains the gradient in v at vl.
	 */
	public abstract VJGradient gradient(Volume v, VJVoxelLoc vl);
	public abstract String toString();
	/**
	 * This method should not be here, but where:
	 * it is a special HSB vector interpolation for RGB volumes.
	 * It is not really interpolation type dependent, but it is a type of interpolation.
	 * Get the HSB voxel value (hue and saturation) with a brightness closest to threshold near vl.
	 * @param hsb a VJValueHSB to which will be added the hue and saturation.
	 * @param v the volume in which to interpolate.
	 * @param threshold the desired surface brightness value.
	 * @param vl the location around which to look for the surface voxel.
	 */
	public VJValueHSB valueHS(VJValueHSB hsb, VolumeRGB v, double  threshold, VJVoxelLoc vl)
	{
		// Look for the voxel with brightness closest to threshold.
		// Do this in a 2x2x2 region.
		VJVoxelLoc smallestVl = new VJVoxelLoc();
		int smallestDelta = 256;
		int REGION = 2;
		for (int z = vl.iz; z < vl.iz+REGION; z++)
		for (int y = vl.iy; y < vl.iy+REGION; y++)
		for (int x = vl.ix; x < vl.ix+REGION; x++)
		{
			int delta = (int) threshold - (int) (v.b[z*v.getHeight()*v.getWidth()+y*v.getWidth()+x]&0xff);
			if (Math.abs(delta) < Math.abs(smallestDelta))
			{
				smallestVl.setx(x); smallestVl.sety(y); smallestVl.setz(z);
				smallestDelta = delta;
			}
		}
		// Get the real RGB voxel (RGB int format).
		int [] slice = (int []) v.sliceArray[smallestVl.iz];
		int voxel = slice[(smallestVl.iy)*v.getWidth() + (smallestVl.ix)];
		VolumeRGB.intToHSB(hsb.hsb, voxel);
		return hsb;
	}
}
