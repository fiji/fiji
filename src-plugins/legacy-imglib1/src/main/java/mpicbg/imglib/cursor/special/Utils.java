/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package mpicbg.imglib.cursor.special;

/**
 * Util class made of static methods, meant to simplify the writing of special cursors.
 *
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 9
 * @author 2010
 */
public class Utils {
	
	/** 
	 * Store the half-widths of a X line to scan to fill an ellipse of given axis lengths.
	 * The parameter <code>a</code> is the axis half-length in the X direction, and <code>b</code>
	 * is the axis half-length in the Y direction. 
	 * <p>
	 * The half-widhts will be stored in the array <code>lineBounds</code>, which must be of size equal
	 * to at least <code>b+1</code>.
	 * <p>
	 * This is an implementation of the McIlroy's algorithm, adapted freely from 
	 * {@link http://enchantia.com/software/graphapp/doc/tech/ellipses.html}.
	 * 
	 * @param a  half-length of the ellipse in the X direction
	 * @param b  half-length of the ellipse in the Y direction
	 * @param lineBounds  will store the half-length of the ellipse lines in the X direction
	 */
	public static final void getXYEllipseBounds(int a, int b, int[] lineBounds) {
		/* e(x,y) = b^2*x^2 + a^2*y^2 - a^2*b^2 */
		int x = 0, y = b;
		int width = 0;
		long a2 = (long)a*a, b2 = (long)b*b;
		long crit1 = -(a2/4 + a%2 + b2);
		long crit2 = -(b2/4 + b%2 + a2);
		long crit3 = -(b2/4 + b%2);
		long t = -a2*y; /* e(x+1/2,y-1/2) - (a^2+b^2)/4 */
		long dxt = 2*b2*x, dyt = -2*a2*y;
		long d2xt = 2*b2, d2yt = 2*a2;

		while (y>=0 && x<=a) {
			if (t + b2*x <= crit1 ||     /* e(x+1,y-1/2) <= 0 */
					t + a2*y <= crit3) {     /* e(x+1/2,y) <= 0 */
				x++; dxt += d2xt; t += dxt;// incx();
				width += 1;
			}
			else if (t - a2*y > crit2) { /* e(x+1/2,y-1) > 0 */
				lineBounds[y] = width; //row(y, width);
				//					if (y!=0)
				//						row(xc-x, yc+y, width);
				y--; dyt += d2yt; t += dyt; // incy();
			}
			else {
				lineBounds[y] = width; // row(y, width);
				//					if (y!=0)
				//						row(xc-x, yc+y, width);
				x++; dxt += d2xt; t += dxt; //incx();
				y--; dyt += d2yt; t += dyt; //incy();
				width += 1;
			}
		}
		if (b == 0)
			lineBounds[0] = a; //row(0, 2*a+1);
	}
	
	/**
	 * Midpoint circle algorithm: store the bounds of a circle in the given array. From
	 * {@link http://en.wikipedia.org/wiki/Midpoint_circle_algorithm}
	 * @param radius  the radius of the circle
	 * @param lineBounds  the array to store bounds in
	 */
	public static final void getXYCircleBounds(int radius, int[] lineBounds)	{
		int f = 1 - radius;
		int ddF_x = 1;
		int ddF_y = -2 * radius;
		int x = 0;
		int y = radius;

		lineBounds[0] = radius;

		while(x < y) 		  {
			// ddF_x == 2 * x + 1;
			// ddF_y == -2 * y;
			// f == x*x + y*y - radius*radius + 2*x - y + 1;
			if(f >= 0)  {
				y--;
				ddF_y += 2;
				f += ddF_y;
			}
			x++;
			ddF_x += 2;
			f += ddF_x;  
			lineBounds[y] = x;
			lineBounds[x] = y;
		}
	}

	/**
	 * Return a copy of the double array where every value has been casted to float
	 */
	public static float[] copyDoubleArrayToFloat(final double[] array) {
		final float[] casted = new float[array.length];
		for (int i = 0; i < casted.length; i++) 
			casted[i] = (float) array[i];
		return casted;
	}

}
