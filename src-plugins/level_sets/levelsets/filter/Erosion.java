/*
 * Erosion.java
 *
 * Created on 19. März 2005, 16:28
 */

package levelsets.filter;

import java.awt.image.*;
import java.awt.*;

/**
 *
 */
public class Erosion extends MorphologicalOperator
{
   
   /** Creates a new instance of Erosion */
   public Erosion(boolean[][] mask)
   {
      super(mask);
   }
   
   protected void processPosition(int x, int y, WritableRaster raster, Raster in)
   {
      boolean setPixel = true;
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
                  setPixel = false;
                  break;
               }
            }
         }
      }
      
      if (setPixel == true)
      {
         pixel[0] = pixel[1] = pixel[2] = 255;
         raster.setPixel(x, y, pixel);
      }
   }
}
