/*
 * Dilatation.java
 *
 * Created on 19. März 2005, 16:28
 */

package levelsets.filter;

import java.awt.image.*;
import java.awt.*;

/**
 *
 */
public class Dilatation extends MorphologicalOperator
{
   
   /** Creates a new instance of Dilatation */
   public Dilatation(boolean[][] mask)
   {
      super(mask);
   }
   
   protected void processPosition(int x, int y, WritableRaster raster, Raster in)
   {
      for (int i = 0; i < mask.length; i++)
      {
         for (int j = 0; j < mask[0].length; j++)
         {
            //if (i == center && j == center) continue;
            if (mask[i][j] == true)
            {
               pixel = in.getPixel(i + x + 1 - center, j + y + 1 - center, pixel);
               if (pixel[0] > 0)
               {
                  pixel[0] = pixel[1] = pixel[2] = 255;
                  raster.setPixel(x, y, pixel);
                  //               System.out.println("Setting pixel");
                  break;
               }
            }
         }
      }  
   }
}
