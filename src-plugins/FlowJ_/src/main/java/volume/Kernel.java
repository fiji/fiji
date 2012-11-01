package volume;

/**
 * This class is the generic type for all 1D, 2D and 3D kernels.
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
public abstract class Kernel
{
    public int halfwidth = 0;       // half width of the kernel.
    public int support()
    /*
        Calculate the width of the support (in pixels or slices) of this kernel.
    */
    {
   	      if (halfwidth  == 0) return 0;
          return halfwidth * 2 + 1;
    }
    public String toString() { return "Convolution Kernel"; }
    public String kernelToString() { return "kernel"; }
}
