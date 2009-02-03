/*
 * Border.java
 *
 * Created on 19. März 2005, 16:28
 */

package levelsets.filter;

import java.awt.image.*;
import java.awt.*;

/**
 *
 */
public class UnsetPixel extends MorphologicalOperator
{
   
   /** Creates a new instance of Border */
   public UnsetPixel(boolean[][] mask)
   {
      super(mask);
   }
   
   protected void processPosition(int x, int y, WritableRaster raster, Raster in)
   {
      pixel = in.getPixel(x, y, pixel);
      if (pixel[0] == 0) return;
      
      boolean unsetPixel = true;
      for (int i = 0; i < mask.length; i++)
      {
         for (int j = 0; j < mask[0].length; j++)
         {
            //if (i == center && j == center) continue;
            if (mask[i][j] == true)
            {
               pixel = in.getPixel(i + x + 1 - center, j + y + 1 - center, pixel);
               if (pixel[0] == 0)
               {
                  unsetPixel = false;
                  break;
               }
            }
         }
      }
      
      if (unsetPixel == true)
      {
         pixel[0] = pixel[1] = pixel[2] = 0;
      }
      else
      {
         pixel[0] = pixel[1] = pixel[2] = 255;
      }
      raster.setPixel(x, y, pixel);
   }
}
