/* Copyright (C) 2003 Free Software Foundation, Inc.
 
   This file is part of GNU Classpath.
 
   GNU Classpath is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.
 
   GNU Classpath is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
 
   You should have received a copy of the GNU General Public License
   along with GNU Classpath; see the file COPYING.  If not, write to the
   Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
   02111-1307 USA.
 
   Linking this library statically or dynamically with other modules is
   making a combined work based on this library.  Thus, the terms and
   conditions of the GNU General Public License cover the whole
   combination.
 
   As a special exception, the copyright holders of this library give you
   permission to link this library with independent modules to produce an
   executable, regardless of the license terms of these independent
   modules, and to copy and distribute the resulting executable under
   terms of your choice, provided that you also meet, for each linked
   independent module, the terms and conditions of the license of that
   module.  An independent module is a module which is not derived from
   or based on this library.  If you modify this library, you may extend
   this exception to your version of the library, but you are not
   obligated to do so.  If you do not wish to do so, delete this
   exception statement from your version. */

package levelsets.filter;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class BufferedImageBuilder implements ImageConsumer
{
   BufferedImage bufferedImage;
   ColorModel defaultModel;
   
   public BufferedImage getBufferedImage()
   {
      return bufferedImage;
   }
   
   public void setDimensions(int width, int height)
   {
      bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
   }
   
   public void setProperties(java.util.Hashtable<?, ?> props)
   {}
   
   public void setColorModel(ColorModel model)
   {
      defaultModel = model;
   }
   
   public void setHints(int flags)
   {}
   
   public void setPixels(int x, int y, int w, int h,
           ColorModel model, byte[] pixels,
           int offset, int scansize)
   {
   }
   
   public void setPixels(int x, int y, int w, int h,
           ColorModel model, int[] pixels,
           int offset, int scansize)
   {
      if (bufferedImage != null)
      {
         
         if (model == null)
            model = defaultModel;
         
         int pixels2[];
         if (model != null)
         {
            pixels2 = new int[pixels.length];
            for (int yy = 0; yy < h; yy++)
               for (int xx = 0; xx < w; xx++)
               {
               int i = yy * scansize + xx;
               pixels2[i] = model.getRGB(pixels[i]);
               }
         }
         else
            pixels2 = pixels;
         
         bufferedImage.setRGB(x, y, w, h, pixels2, offset, scansize);
      }
   }
   
   public void imageComplete(int status)
   {}
}