package volume;

/**
 * This class implements a 4D Sobel differentiation kernel.
 * Abramoff, Viergever, IEEE TMI, April 2002
 *
 * (c) 1999-2002 Michael Abramoff. All rights reserved.
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
public class Sobel4D extends Kernel4D
{
    public String toString() { return "Sobel4D"; }
    /** Initialize Sobel 1D kernel. */
    public Sobel4D()
    {
            int width = 3;
	          if (width % 2 == 0) width++;
            halfwidth = width / 2;
            k = new double[3][3][3][3];
            /*
                  -2  0   2     -3  0   3    -2   0   2
                  -3  0   3     -6  0   6    -3   0   3
                  -2  0   2     -3  0   3    -2   0   2
            */
            k[0][0][0][0] = -2; k[0][0][0][1] = 0; k[0][0][0][2] = 2;
            k[0][0][1][0] = -3; k[0][0][1][1] = 0; k[0][0][1][2] = 3;
            k[0][0][2][0] = -2; k[0][0][2][1] = 0; k[0][0][2][2] = 2;
            k[0][1][0][0] = -3; k[0][1][0][1] = 0; k[0][1][0][2] = 3;
            k[0][1][1][0] = -6; k[0][1][1][1] = 0; k[0][1][1][2] = 6;
            k[0][1][2][0] = -3; k[0][1][2][1] = 0; k[0][1][2][2] = 3;
            k[0][2][0][0] = -2; k[0][2][0][1] = 0; k[0][2][0][2] = 2;
            k[0][2][1][0] = -3; k[0][2][1][1] = 0; k[0][2][1][2] = 3;
            k[0][2][2][0] = -2; k[0][2][2][1] = 0; k[0][2][2][2] = 2;

            /*
                  -6  0   6     -9  0   9    -6   0   6
                  -9  0   9     -18  0  18   -9   0   9
                  -6  0   6     -9  0   9    -6   0   6
            */
            k[1][0][0][0] = -6; k[1][0][0][1] = 0; k[1][0][0][2] = 6;
            k[1][0][1][0] = -9; k[1][0][1][1] = 0; k[1][0][1][2] = 9;
            k[1][0][2][0] = -6; k[1][0][2][1] = 0; k[1][0][2][2] = 6;
            k[1][1][0][0] = -9; k[1][1][0][1] = 0; k[1][1][0][2] = 9;
            k[1][1][1][0] = -18; k[1][1][1][1] = 0; k[1][1][1][2] = 18;
            k[1][1][2][0] = -9; k[1][1][2][1] = 0; k[1][1][2][2] = 9;
            k[1][2][0][0] = -6; k[1][2][0][1] = 0; k[1][2][0][2] = 6;
            k[1][2][1][0] = -9; k[1][2][1][1] = 0; k[1][2][1][2] = 9;
            k[1][2][2][0] = -6; k[1][2][2][1] = 0; k[1][2][2][2] = 6;

            /*
                  -2  0   2     -3  0   3    -2   0   2
                  -3  0   3     -6  0   6    -3   0   3
                  -2  0   2     -3  0   3    -2   0   2
            */
            k[2][0][0][0] = -2; k[2][0][0][1] = 0; k[2][0][0][2] = 2;
            k[2][0][1][0] = -3; k[2][0][1][1] = 0; k[2][0][1][2] = 3;
            k[2][0][2][0] = -2; k[2][0][2][1] = 0; k[2][0][2][2] = 2;
            k[2][1][0][0] = -3; k[2][1][0][1] = 0; k[2][1][0][2] = 3;
            k[2][1][1][0] = -6; k[2][1][1][1] = 0; k[2][1][1][2] = 6;
            k[2][1][2][0] = -3; k[2][1][2][1] = 0; k[2][1][2][2] = 3;
            k[2][2][0][0] = -2; k[2][2][0][1] = 0; k[2][2][0][2] = 2;
            k[2][2][1][0] = -3; k[2][2][1][1] = 0; k[2][2][1][2] = 3;
            k[2][2][2][0] = -2; k[2][2][2][1] = 0; k[2][2][2][2] = 2;
    }
}
