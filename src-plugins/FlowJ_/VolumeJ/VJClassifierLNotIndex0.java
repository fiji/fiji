package VolumeJ;
import java.awt.*;
import ij.*;
import ij.gui.*;

/**
 * This class implements the Levoy tent classification function with indexing.
 * <pre>
 * index = 0 is not shown (opacity 0).
 * index == 1 is white
 * index > 1 direct to the spectrum LUT (index == 2 is red, index == 128 is green andsoforth)
 * </pre>
 *
 * It can be subclassed for variations on the lookup methods.
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
public class VJClassifierLNotIndex0 extends VJClassifierLevoy
{
        public VJClassifierLNotIndex0()
        {
                super();
                // Name of this classifier.
                description = "Gradient index<>0";
        }
        /**
         * Tell calling program whether this voxel has an interesting index
         * (worthy to do interpolation and gradient calcs on)
         * If the index == 0 (this classifier), the voxel will be skipped.
         * @param v the value of which has to be decided whether it is visiblw or not.
        */
        public boolean visible(VJValue v)
        {
                int index = v.index;
                return (index != 0);
        }
        public String toLongString()
        {
                return "Levoy ("+((does()==RGB)?"RGB":"grays")+") classifier with >=1 indexing. Voxels more opaque "+
                        " the closer to threshold ("+threshold+") and the higher surface gradient "+
                        " (relative contribution set by deviation). Indexing: index=0, voxel not shown; "+
                        " index=1, voxel shown as gray; else voxel color determined by color LUT.";
        }
        /**
         * Setup a default LUT.
         * <pre>
         * index = 0 is not shown (opacity 0).
         * index == 1 is white
         * index > 1 direct to the spectrum LUT (index == 2 is red, index == 128 is green andsoforth)
         * </pre>
        */
        protected void defaultLUT()
        {
				if (nrIndexBits > 0)
				{
						lut = new byte[(int) Math.pow(2, nrIndexBits)*3];// r,g,b
						for (int index = 0; index < (int) Math.pow(2, nrIndexBits); index++)
						{
								if (index == 1)
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
		}
}

