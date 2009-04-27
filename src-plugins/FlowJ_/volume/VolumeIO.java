package volume;
import java.io.*;
import ij.*;
import ij.gui.*;

/**
 * This is a class that implements reading and writing on float volumes.
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
public class VolumeIO extends VolumeFloat
{
      /* Creates a volume with width, height, depth */
      public VolumeIO(int width, int height, int depth)
      {
            super(width, height, depth);
      }
      // creates a volume from a volume file.
      public VolumeIO(String fileName)
      {
            getDimensions(fileName);
            v = new float[depth][height][width];
            read(fileName);
            edge = 0;
      }
      public void write(String fileName)
      {
		    OutputStream os = null;
		    try {os = new FileOutputStream(fileName);}
		    catch (IOException e) {IJ.write("" + e); return;}
        // write dimensions.
        {
              byte[] buffer = new byte[2*3];
				      int i = 0;
				      buffer[i*2] = (byte)((width>>>8)&0xff); buffer[i*2+1] = (byte)(width&0xff);
              i++;
				      buffer[i*2] = (byte)((height>>>8)&0xff); buffer[i*2+1] = (byte)(height&0xff);
              i++;
				      buffer[i*2] = (byte)((depth>>>8)&0xff); buffer[i*2+1] = (byte)(depth&0xff);
              try {os.write(buffer, 0, buffer.length); }
              catch (IOException e) {IJ.write("" + e); return;}
        }
        // now write the data.
        int size = width*height*depth;
        for (int t = 0; t < depth; t++)
        {
            byte[] buffer = new byte[width*height*4];
 		        for (int y = 0; y < height; y++)
            {
 		  	        for (int x = 0; x < width; x++)
		            {
				              int tmp = Float.floatToIntBits(v[t][y][x]);
				              buffer[y*width*4+x*4+0] = (byte)((tmp>>24)&0xff);
				              buffer[y*width*4+x*4+1] = (byte)((tmp>>16)&0xff);
				              buffer[y*width*4+x*4+2] = (byte)((tmp>>8)&0xff);
				              buffer[y*width*4+x*4+3] = (byte)(tmp&0xff);
                }
                IJ.showStatus("Writing... "+(100*(y*height+t*height*width))/size+"%");
            }
            try {os.write(buffer, 0, buffer.length); }
            catch (IOException e) {IJ.write("" + e); return;}
        }
        try {os.close(); }
        catch (IOException e) {IJ.write("" + e); return;}
    }
    // Get volume dimensions from a file.
    private void getDimensions(String fileName)
    {
		    InputStream os = null;
		    try {os = new FileInputStream(fileName);}
		    catch (IOException e) {IJ.write("" + e); return;}
        byte[] buffer = new byte[2*3];
        try {os.read(buffer, 0, buffer.length); }
        catch (IOException e) {IJ.write("" + e); return;}
				int j = 0;
        width = (int) (((buffer[j*2]&0xff)<<8) | (buffer[j*2+1]&0xff));
        j++;
        height = (int) (((buffer[j*2]&0xff)<<8) | (buffer[j*2+1]&0xff));
        j++;
        depth = (int) (((buffer[j*2]&0xff)<<8) | (buffer[j*2+1]&0xff));
        try {os.close(); }
        catch (IOException e) {IJ.write("" + e); return;}
    }
    private void read(String fileName)
    {
		    InputStream is = null;
		    try {is = new FileInputStream(fileName);}
		    catch (IOException e) {IJ.write("" + e); return;}
        int size = depth*height*width;
        {
              // skip the dimensions field.
              byte[] buffer = new byte[2*3];
              try {is.read(buffer, 0, buffer.length); }
              catch (IOException e) {IJ.write("" + e); return;}
        }
        for (int t = 0; t < depth; t++)
        {
            byte[] buffer = new byte[width*height*4];
            try {is.read(buffer, 0, buffer.length); }
            catch (IOException e) {IJ.write("" + e); return;}
 		        for (int y = 0; y < height; y++)
            {
 		  	        for (int x = 0; x < width; x++)
		            {
					            int tmp = (int)(((buffer[y*width*4+x*4+0]&0xff)<<24)
                                  | ((buffer[y*width*4+x*4+1]&0xff)<<16)
                                  | ((buffer[y*width*4+x*4+2]&0xff)<<8)
                                  | (buffer[y*width*4+x*4+3]&0xff));
						          v[t][y][x] = Float.intBitsToFloat(tmp);
                }
                IJ.showStatus("Reading... "+(100*(y*height+t*height*width))/size+"%");
            }
        }
        try {is.close(); }
        catch (IOException e) {IJ.write("" + e); return;}
    }
    public boolean delete(String filename)
    // delete the volume file.
    {
          File file = new File(filename);
          boolean result = file.delete();
          if (! result)
                IJ.error("unable to delete "+filename);
          return result;
    }
}
