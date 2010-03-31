package VolumeJ;
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.*;
import java.io.*;

/**
 * This class implements the Levoy tent classification function with indexing.
 * It implements a table for the rgb and alpha lookup.
 * It can be subclassed for variations on the indexing and lookup methods.
 * A spectrum type color table is used for indexing color lookup.<br>
 * Reference:  Levoy, 1988, IEEE CGA, 5(3), 29-37<br>
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
public class VJClassifierLevoy extends VJClassifier
{
	protected float []	opacityTable;           // contains opacity values.
	protected byte []	lut;             // rgb values.
	protected double         threshold;
	protected double         width;
	protected int           maxIntensity; // max voxel intensity.
	protected int           maskMagnitude;
	protected int           maskIndex;
	protected float        fractionMagnitude;
	protected int           nrIndexBits;
	protected int           nrMagnitudeBits;
	protected int           nrIntensityBits;
	protected int		maskIntensity;

	/**
	 * Default instantiation.
	 */
	public VJClassifierLevoy()
	{
		this(8, 8, 8);
		description = "Gradient + index(spectrum)";
	}
	/**
	 * Instantiation of new classifier.
	 * @param nrIntensityBits: the number of intensity bits available for the opacity table index.
	 * @param nrMagnitudeBits the number of gradient magnitude bits available for the opacity table index.
	 * @param nrIndexBits the number of bits available indexing for the opacity table index.
	 */
	public VJClassifierLevoy(int nrIntensityBits, int nrMagnitudeBits, int nrIndexBits)
	{
		this.nrMagnitudeBits = nrMagnitudeBits;
		this.nrIntensityBits = nrIntensityBits;
		this.nrIndexBits = nrIndexBits;
		maxIntensity = (int) Math.pow(2, nrIntensityBits);
		maskIntensity = (int) Math.pow(2, nrIntensityBits) - 1;
		// Compute maximum value for the gradient magnitude.
		int maxMagnitude = (int) Math.sqrt(3 * Math.pow(maxIntensity, 2));
		// Compute the fraction of the gradient magnitude that fits into the bits.
		fractionMagnitude = (float) Math.pow(2, nrMagnitudeBits) / maxMagnitude;
		maskMagnitude = (int) Math.pow(2, nrMagnitudeBits) - 1;
		/*
		ij.VJUserInterface.write("bits: int "+nrIntensityBits+", mag "+nrMagnitudeBits+" ix "
		+nrIndexBits
		+" "+maxIntensity+" "+maskIntensity+"
		"+maxMagnitude+" "
		+fractionMagnitude+" "+maskMagnitude);
		*/
		// Set up LUT and opacity tables.
		// Opacity table always 64K bytes.
		opacityTable  = new float[(int) Math.pow(2, 16)];
		if (nrIndexBits > 0)
			defaultLUT();
	}
	/**
	 * Set width of gradient tent function. Determines tolerance to voxel values != threshold.
	 */
	public void setWidth(double  width) { this.width = width; }
	public double  getThreshold() { return threshold; }
	/**
	 * Set center of gradient tent function.
	 * Determines voxel value that will always be rendered, no matter what the gradient value.
	 * @param threshold the center value.
	*/
	public void setThreshold(double  threshold) { this.threshold = threshold; }
	/**
	 * Tells renderer that all voxels are potentially visible. Overload as needed.
	*/
	public boolean visible(VJValue v) { return true; }
	/**
	 * Tell calling program that this classifier will return RGB values.
	*/
	public int does() { return RGB; }
	/**
	 * Tell renderer that this classifier has user settable LUT.
	*/
	public boolean hasLUT() { return true; }
	/**
	 * Tells renderer this classifier can process indices in the voxel values.
	*/
	public boolean doesIndex() { return true; }
	/**
	 * Tells the calling program it can process cutouts (slice faces).
	*/
	public boolean doesCutouts() { return true; }
	/**
	 * Tell calling program name of this classifier.
	*/
	public String toString() { return description; }
	public String toLongString()
	{
		return "Levoy ("+((does()==RGB)?"RGB":"grays")+") classifier. Makes voxels more opaque "+
			" the closer their intensity is to threshold ("+threshold+") and the higher their surface gradient "+
			" (relative contribution set by deviation). Voxel colors determined from LUT and index volume if present.";
	}
	/**
	 * Classify the (interpolated) voxel value and gradient magnitude into a alpha and rgb-value.
	 * If the voxel is RGB, use the hue and saturation of the voxel, and set the opacity from
	 * the opacity table.
	 * @param v the VJValue, the interpolated value at this location.
	 * @param g the gradient at this location
	*/
	public VJAlphaColor alphacolor(VJValue v, VJGradient g)
	{
		// Code magnitude into bits.
		int igradient = (int) (g.getmag() * fractionMagnitude) & maskMagnitude;
		// Fit into 16 bits.
		int entry = (igradient << nrIntensityBits) | v.intvalue;
		if (v instanceof VJValueHSB)
		{
			// Is an RGB (HSB format) voxel! Use true colors.
			Color color = java.awt.Color.getHSBColor(((VJValueHSB)v).getHue(),
			      ((VJValueHSB)v).getSaturation(), 1);
			return new VJAlphaColor((double) opacityTable[entry],
						color.getRed(), color.getGreen(), color.getBlue());
		}
		else return new VJAlphaColor((double) opacityTable[entry],
			(lut[v.index*3+0]&0xff),
			(lut[v.index*3+1]&0xff),
			(lut[v.index*3+2]&0xff));
	}
	/**
		Slice view classification.
		Classify the (interpolated) voxel value for a slice view.
		If the index is 0, the value is rendered straight away with opacity 1.
		If the index > 0, the index is used to determine the color of the rendering, also
		with opacity 1.
	*/
	public VJAlphaColor alphacolor(VJValue v)
	{
		int intensity = v.intvalue;
		int index = v.index;
		if (index == 0)
			return new VJAlphaColor((double) 1, intensity, intensity, intensity);
		else
			return new VJAlphaColor(1, lut[index*3+0]&0xff, lut[index*3+1]&0xff,
				lut[index*3+2]&0xff);
	}
	/**
	 * Compute the opacities for a range of voxel values and gradient values.
	 * nrMagnitudeBits defines the resolution for the gradientmagnitude,
	 * and maxIntensity defines the resolution for the voxel value.
	 * Correct for oversampling at oblique rays.
	 * @param oversampling the oversampling ratio.
	 */
	 public void setupOpacities(double  oversampling)
	{
		for (int magnitude = 0; magnitude < (int) Math.pow(2, nrMagnitudeBits); magnitude++)
		for (int intensity = 0; intensity < maxIntensity; intensity++)
		{
			double  dfxi = magnitude << Math.min(0, (8 - nrMagnitudeBits));
			double  uncorrectedOpacity = opacityCompute(dfxi,
				intensity, threshold, width);
			int ind = (magnitude << nrIntensityBits) | intensity;
			opacityTable[ind] =
				1f - (float) Math.pow(1 - uncorrectedOpacity, oversampling);
		}
	}
	/**
	 * Make a LUT obtained from a spectrum.
	 * First entry is white by default.
	 * Rest is filled with a spectrum.
	 */
	protected void defaultLUT()
	{
		lut = new byte[(int) Math.pow(2, nrIndexBits)*3];
		for (int index = 0; index < (int) Math.pow(2, nrIndexBits); index++)
		{
			if (index == 0)
			{
				// white
				lut[index*3+0] = (byte) 255;
				lut[index*3+1] = (byte) 255;
				lut[index*3+2] = (byte) 255;
			}
			else
			{
				Color c = Color.getHSBColor(index/255f, 1f, 1f);
				lut[index*3+0] = (byte) c.getRed();
				lut[index*3+1] = (byte) c.getGreen();
				lut[index*3+2] = (byte) c.getBlue();
			}
		}
	}
	/**
	 * Set the LUT to a user-defined LUT. Size of user-defined LUT must be same as
	 * classifier LUT.
	 * @param reds, greens, blues byte[] containing the RGB values for the LUT.
	 * @return true if LUT was set, false if size of user-defined LUT did not correspond.
	*/
	public boolean setLUT(byte [] reds, byte [] greens, byte [] blues)
	{
		lut = new byte[(int) Math.pow(2, nrIndexBits)*3];
		if (lut.length != (reds.length + greens.length + blues.length))
			return false;
		for (int index = 0; index < reds.length; index++)
		{
			lut[index*3+0] = reds[index];
			lut[index*3+1] = greens[index];
			lut[index*3+2] = blues[index];
		}
		return true;
	}
	/**
	 * Implements Levoy's gradient tent function for precomputation.
	 * @param dfxi[0-255] the magnitude of the gradient.
	 * @param intensity of the voxel.
	 * @param threshold the center of the tent function.
	 * @param width, the width of the tent function.
	*/
	protected double  opacityCompute(double  dfxi, double  intensity, double  threshold, double  width)
	{
		double  opacity;
		if (dfxi == 0 && intensity == threshold)
			opacity = 1;
		else if (dfxi > 0 &&
			intensity <= (threshold +  width * dfxi) &&
			intensity >= (threshold - width * dfxi))
			opacity = 1 - (1 / width) * Math.abs((threshold - intensity) / dfxi);
		      else
			opacity = 0;
		return opacity;
	}
}

