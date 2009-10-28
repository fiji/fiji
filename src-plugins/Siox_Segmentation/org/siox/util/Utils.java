/*
   Copyright 2005, 2006 by Gerald Friedland and Kristian Jantz

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.siox.util;

/**
 * Collection of auxiliary image processing methods used by the
 * SioxSegmentator mainly for postprocessing.
 *
 * @author G. Friedland, K. Jantz, L. Knipping
 * @version 1.05
 */
public class Utils
{

	// CHANGELOG
	// 2005-11-09 1.04 further clean up
	// 2005-11-09 1.03 fixed some Javadoc comments
	// 2005-11-03 1.02 further clean up
	// 2005-11-02 1.01 cleaned up a bit
	// 2005-10-25 1.00 initial release

	/** Caches color conversion values to spped up RGB->CIELAB conversion.*/
	private final static IntHashMap RGB_TO_LAB=new IntHashMap(100000);

	/** Prevent outside instantiation. */
	private Utils() {}

	/**
	 * Applies the morphological dilate operator.
	 *
	 * Can be used to close small holes in the given confidence matrix.
	 *
	 * @param cm Confidence matrix to be processed.
	 * @param xres Horizontal resolution of the matrix.
	 * @param yres Vertical resolution of the matrix.
	 */
	public static void dilate(float[] cm, int xres, int yres)
	{
		for (int y=0; y<yres; y++) {
		for (int x=0; x<xres-1; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=Math.max(cm[idx], cm[idx+1]);
		}
	}
	for (int y=0; y<yres; y++) {
		for (int x=xres-1; x>=1; x--) {
			final int idx=(y*xres)+x;
		cm[idx]=Math.max(cm[idx-1], cm[idx]);
		}
	}
	for (int y=0; y<yres-1; y++) {
		for (int x=0; x<xres; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=Math.max(cm[idx], cm[((y+1)*xres)+x]);
		}
	}
	for (int y=yres-1; y>=1; y--) {
		for (int x=0; x<xres; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=Math.max(cm[((y-1)*xres)+x], cm[idx]);
		}
	}
	}

	/**
	 * Applies the morphological erode operator.
	 *
	 * @param cm Confidence matrix to be processed.
	 * @param xres Horizontal resolution of the matrix.
	 * @param yres Vertical resolution of the matrix.
	 */
	public static void erode(float[] cm, int xres, int yres)
	{
		for (int y=0; y<yres; y++) {
		for (int x=0; x<xres-1; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=Math.min(cm[idx], cm[idx+1]);
		}
	}
	for (int y=0; y<yres; y++) {
		for (int x=xres-1; x>=1; x--) {
			final int idx=(y*xres)+x;
		cm[idx]=Math.min(cm[idx-1], cm[idx]);
		}
	}
	for (int y=0; y<yres-1; y++) {
		for (int x=0; x<xres; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=Math.min(cm[idx], cm[((y+1)*xres)+x]);
		}
	}
	for (int y=yres-1; y>=1; y--) {
		for (int x=0; x<xres; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=Math.min(cm[((y-1)*xres)+x], cm[idx]);
		}
	}
	}

	/**
	 * Normalizes the matrix to values to [0..1].
	 *
	 * @param cm The matrix to be normalized.
	 */
	public static void normalizeMatrix(float[] cm)
	{
		float max=0.0f;
	for (int i=0; i<cm.length; i++) {
		if (max<cm[i]) {
			max=cm[i];
		}
	}
	if (max<=0.0) {
		return;
	} else if (max==1.00) {
		return;
	}
	final float alpha=1.00f/max;
	premultiplyMatrix(alpha, cm);
	}

	/**
	 * Multiplies matrix with the given scalar.
	 *
	 * @param alpha The scalar value.
	 * @param cm The matrix of values be multiplied with alpha.
	 */
	public static void premultiplyMatrix(float alpha, float[] cm)
	{
		for (int i=0; i<cm.length; i++) {
		cm[i]=alpha*cm[i];
	}
	}

	/**
	 * Blurs confidence matrix with a given symmetrically weighted kernel.
	 * <P>
	 * In the standard case confidence matrix entries are between 0...1 and
	 * the weight factors sum up to 1.
	 *
	 * @param cm The matrix to be smoothed.
	 * @param xres Horizontal resolution of the matrix.
	 * @param yres Vertical resolution of the matrix.
	 * @param f1 Weight factor for the first pixel.
	 * @param f2 Weight factor for the mid-pixel.
	 * @param f3 Weight factor for the last pixel.
	 */
	public static void smoothcm(float[] cm, int xres, int yres, float f1, float f2, float f3)
	{
		for (int y=0; y<yres; y++) {
		for (int x=0; x<xres-2; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=f1*cm[idx]+f2*cm[idx+1]+f3*cm[idx+2];
		}
	}
	for (int y=0; y<yres; y++) {
		for (int x=xres-1; x>=2; x--) {
			final int idx=(y*xres)+x;
		cm[idx]=f3*cm[idx-2]+f2*cm[idx-1]+f1*cm[idx];
		}
	}
	for (int y=0; y<yres-2; y++) {
		for (int x=0; x<xres; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=f1*cm[idx]+f2*cm[((y+1)*xres)+x]+f3*cm[((y+2)*xres)+x];
		}
	}
	for (int y=yres-1; y>=2; y--) {
		for (int x=0; x<xres; x++) {
			final int idx=(y*xres)+x;
		cm[idx]=f3*cm[((y-2)*xres)+x]+f2*cm[((y-1)*xres)+x]+f1*cm[idx];
		}
	}
	}

	/**
	 * Squared Euclidian distance of p and q.
	 * <P>
	 * Usage hint: When only comparisons between Euclidian distances without
	 * actual values are needed, the squared distance can be preferred
	 * for being faster to compute.
	 *
	 * @param p First euclidian point coordinates.
	 * @param q Second euclidian point coordinates.
	 *        Dimension must not be smaller than that of p.
	 *        Any extra dimensions will be ignored.
	 * @return Squared euclidian distance of p and q.
	 * @see #euclid
	 */
	public static float sqrEuclidianDist(float[] p, float[] q)
	{
		float sum=0;
	for (int i=0; i<p.length; i++) {
		sum+=(p[i]-q[i])*(p[i]-q[i]);
	}
	return sum;
	}

	/**
	 * Euclidian distance of p and q.
	 *
	 * @param p First euclidian point coordinates.
	 * @param q Second euclidian point coordinates.
	 *        Dimension must not be smaller than that of p.
	 *        Any extra dimensions will be ignored.
	 * @return Squared euclidian distance of p and q.
	 * @see #sqrEuclidianDist
	 */
	public static float euclid(float[] p, float[] q)
	{
		return (float)Math.sqrt(sqrEuclidianDist(p, q));
	}

	/**
	 * Computes Euclidian distance of two RGB color values.
	 *
	 * @param rgb0 First color value.
	 * @param rgb1 Second color value.
	 * @return Euclidian distance between the two color values.
	 */
	public static float colordiff(int rgb0, int rgb1)
	{
		return (float)Math.sqrt(colordiffsq(rgb0, rgb1));
	}

	/**
	 * Computes squared euclidian distance of two RGB color values.
	 * <P>
	 * Note: Faster to compute than colordiff
	 *
	 * @param rgb0 First color value.
	 * @param rgb1 Second color value.
	 * @return Squared Euclidian distance between the two color values.
	 */
	public static float colordiffsq(int rgb0, int rgb1)
	{
		final int rDist=getRed(rgb0)-getRed(rgb1);
	final int gDist=getGreen(rgb0)-getGreen(rgb1);
	final int bDist=getBlue(rgb0)-getBlue(rgb1);
	return rDist*rDist+gDist*gDist+bDist*bDist;
	}

	/**
	 * Averages two ARGB colors.
	 *
	 * @param argb0 First color value.
	 * @param argb1 Second color value.
	 * @return The averaged ARGB color.
	 */
	public static int average(int argb0, int argb1)
	{
		return packPixel((getAlpha(argb0)+getAlpha(argb1))/2,
			 (getRed(argb0)+getRed(argb1))/2,
			 (getGreen(argb0)+getGreen(argb1))/2,
			 (getBlue(argb0)+getBlue(argb1))/2);
	}

	/**
	 * Computes squared euclidian distance in CLAB space for two colors
	 * given as RGB values.
	 *
	 * @param rgb0 First color value.
	 * @param rgb1 Second color value.
	 * @return Squared Euclidian distance in CLAB space.
	 */
	public static float labcolordiffsq(int rgb0, int rgb1)
	{
		final float[] c1=rgbToClab(rgb0);
	final float[] c2=rgbToClab(rgb1);
	float euclid=0;
	for (int k=0; k<c1.length; k++) {
		euclid+=(c1[k]-c2[k])*(c1[k]-c2[k]);
	}
	return euclid;
	}

	/**
	 * Computes squared euclidian distance in CLAB space for two colors
	 * given as RGB values.
	 *
	 * @param rgb0 First color value.
	 * @param rgb1 Second color value.
	 * @return Euclidian distance in CLAB space.
	 */
	public static float labcolordiff(int rgb0, int rgb1)
	{
		return (float)Math.sqrt(labcolordiffsq(rgb0, rgb1));
	}

	/**
	 * Converts 24-bit RGB values to {l,a,b} float values.
	 * <P>
	 * The conversion used is decribed at
	 * <a href="http://www.easyrgb.com/math.php?MATH=M7#text7">CLAB Conversion</a>
	 * for reference white D65. Note that that the conversion is computational
	 * expensive. Result are cached to speed up further conversion calls.
	 *
	 * @param rgb RGB color value,
	 * @return CLAB color value tripel.
	 */
	public static float[] rgbToClab(int rgb)
	{
		float lab[]=(float[])RGB_TO_LAB.get(rgb);
	if (lab!=null) {
		return lab;
	}
	lab=new float[3];
	final int R=getRed(rgb);
	final int G=getGreen(rgb);
	final int B=getBlue(rgb);

	float var_R=(R/255.0f); //R = From 0 to 255
	float var_G=(G/255.0f); //G = From 0 to 255
	float var_B=(B/255.0f); //B = From 0 to 255

	if (var_R>0.04045) {
		var_R=(float)Math.pow((var_R+0.055f)/1.055f, 2.4);
	} else {
		var_R=var_R/12.92f;
	}
	if (var_G>0.04045) {
		var_G=(float)Math.pow((var_G+0.055f)/1.055f, 2.4);
	} else {
		var_G=var_G/12.92f;
	}
	if (var_B>0.04045) {
		var_B=(float)Math.pow((var_B+0.055f)/1.055f, 2.4);
	} else {
		var_B=var_B/12.92f;
	}
	var_R=var_R*100f;
	var_G=var_G*100f;
	var_B=var_B*100f;

	// Observer. = 2�, Illuminant = D65
	final float X=var_R*0.4124f+var_G*0.3576f+var_B*0.1805f;
	final float Y=var_R*0.2126f+var_G*0.7152f+var_B*0.0722f;
	final float Z=var_R*0.0193f+var_G*0.1192f+var_B*0.9505f;

	float var_X=X/95.047f;
	float var_Y=Y/100f;
	float var_Z=Z/108.883f;

	if (var_X>0.008856f) {
		var_X=(float)Math.pow(var_X, (1f/3f));
	} else {
		var_X=(7.787f*var_X)+(16f/116f);
	}
	if (var_Y>0.008856f) {
		var_Y=(float)Math.pow(var_Y, (1f/3f));
	} else {
		var_Y=(7.787f*var_Y)+(16f/116f);
	}
	if (var_Z>0.008856f) {
		var_Z=(float)Math.pow(var_Z, (1f/3f));
	} else {
		var_Z=(7.787f*var_Z)+(16f/116f);
	}
	lab[0]=(116f*var_Y)-16f;
	lab[1]=500f*(var_X-var_Y);
	lab[2]=200f*(var_Y-var_Z);

	RGB_TO_LAB.put(rgb, lab);
	return lab;
	}

	/**
	 * Converts an CLAB value to a RGB color value.
	 * <P>
	 * This is the reverse operation to rgbToClab.
	 * @param clab CLAB value.
	 * @return RGB value.
	 * @see #rgbToClab
	 */
	public static int clabToRGB(float[] clab)
	{
	  final float L=clab[0];
	  final float a=clab[1];
	  final float b=clab[2];

	  float var_Y=(L+16f)/116f;
	  float var_X=a/500f+var_Y;
	  float var_Z=var_Y-b/200f;

	  final float var_yPow3=(float)Math.pow(var_Y, 3);
	  final float var_xPow3=(float)Math.pow(var_X, 3);
	  final float var_zPow3=(float)Math.pow(var_Z, 3);
	  if (var_yPow3>0.008856f) {
	  var_Y=var_yPow3;
	  } else {
	  var_Y=(var_Y-16f/116f)/7.787f;
	  }
	  if (var_xPow3>0.008856f) {
	  var_X=var_xPow3;
	  } else {
	  var_X=(var_X-16f/116f)/7.787f;
	  }
	  if (var_zPow3>0.008856f) {
	  var_Z=var_zPow3;
	  } else {
	  var_Z=(var_Z-16f/116f)/7.787f;
	  }
	  final float X=95.047f*var_X; //ref_X= 95.047  Observer=2�, Illuminant=D65
	  final float Y=100f*var_Y;    //ref_Y=100.000
	  final float Z=108.883f*var_Z;//ref_Z=108.883

	  var_X=X/100f; //X = From 0 to ref_X
	  var_Y=Y/100f; //Y = From 0 to ref_Y
	  var_Z=Z/100f; //Z = From 0 to ref_Y

	  float var_R=(float)(var_X*3.2406f+var_Y*-1.5372f+var_Z*-0.4986f);
	  float var_G=(float)(var_X*-0.9689f+var_Y*1.8758f+var_Z*0.0415f);
	  float var_B=(float)(var_X*0.0557f+var_Y*-0.2040f+var_Z*1.0570f);

	  if (var_R>0.0031308f) {
	  var_R=(float)(1.055f*Math.pow(var_R, (1f/2.4f))-0.055f);
	  } else {
	  var_R=12.92f*var_R;
	  }
	  if (var_G>0.0031308f) {
	  var_G=(float)(1.055f*Math.pow(var_G, (1f/2.4f))-0.055f);
	  } else {
	  var_G=12.92f*var_G;
	  }
	  if (var_B>0.0031308f) {
	  var_B=(float)(1.055f*Math.pow(var_B, (1f/2.4f))-0.055f);
	  } else {
	  var_B=12.92f*var_B;
	  }
	  final int R=Math.round(var_R*255f);
	  final int G=Math.round(var_G*255f);
	  final int B=Math.round(var_B*255f);

	  return packPixel(0xFF, R, G, B);
	}

	/**
	 * Sets the alpha byte of a pixel.
	 *
	 * Constructs alpha to values from 0 to 255.
	 * @param alpha Alpha value from 0 (transparent) to 255 (opaque).
	 * @param rgb The 24bit rgb color to be combined with the alpga value.
	 * @return An ARBG calor value.
	 */
	public static int setAlpha(int alpha, int rgb)
	{
		if (alpha>255) {
		alpha=0;
	} else if (alpha<0) {
		alpha=0;
	}
	return (alpha<<24)|(rgb&0xFFFFFF);
	}

	/**
	 * Sets the alpha byte of a pixel.
	 *
	 * Constricts alpha to values from 0 to 255.
	 * @param alpha Alpha value from 0.0f (transparent) to 1.0f (opaque).
	 * @param rgb The 24bit rgb color to be combined with the alpga value.
	 * @return An ARBG calor value.
	 */
	public static int setAlpha(float alpha, int rgb)
	{
		return setAlpha((int)(255f*alpha), rgb);
	}

	/**
	 * Limits the values of a,r,g,b to values from 0 to 255 and puts them
	 * together into an 32 bit integer.
	 *
	 * @param a Alpha part, the first byte.
	 * @param r Red part, the second byte.
	 * @param g Green part, the third byte.
	 * @param b Blue part, the fourth byte.
	 * @return A ARBG value packed to an int.
	 */
	public static int packPixel(int a, int r, int g, int b)
	{
		if (a<0) {
		a=0;
	} else if (a>255) {
		a=255;
	}
	if (r<0) {
		r=0;
	} else if (r>255) {
		r=255;
	}
	if (g<0) {
		g=0;
	} else if (g>255) {
		g=255;
	}
	if (b<0) {
		b=0;
	} else if (b>255) {
		b=255;
	}
	return (a<<24)|(r<<16)|(g<<8)|b;
	}

	/**
	 * Returns the alpha component of an ARGB value.
	 *
	 * @param argb An ARGB color value.
	 * @return The alpha component, ranging from 0 to 255.
	 */
	public static int getAlpha(int argb)
	{
		return (argb>>24)&0xFF;
	}

	/**
	 * Returns the red component of an (A)RGB value.
	 *
	 * @param rgb An (A)RGB color value.
	 * @return The red component, ranging from 0 to 255.
	 */
	public static int getRed(int rgb)
	{
		return (rgb>>16)&0xFF;
	}


	/**
	 * Returns the green component of an (A)RGB value.
	 *
	 * @param rgb An (A)RGB color value.
	 * @return The green component, ranging from 0 to 255.
	 */
	public static int getGreen(int rgb)
	{
		return (rgb>>8)&0xFF;
	}

	/**
	 * Returns the blue component of an (A)RGB value.
	 *
	 * @param rgb An (A)RGB color value.
	 * @return The blue component, ranging from 0 to 255.
	 */
	public static int getBlue(int rgb)
	{
		return (rgb)&0xFF;
	}

	/**
	 * Returns a string representation of a CLAB value.
	 *
	 * @param clab The CLAB value.
	 * @return A string representation of the CLAB value.
	 */
	public static String clabToString(float[] clab)
	{
		final StringBuffer buff=new StringBuffer();
	for (int i=0; i<clab.length; i++) {
		buff.append(clab[i]).append((i==clab.length-1)?"":", ");
	}
	return buff.toString();
	}
}
