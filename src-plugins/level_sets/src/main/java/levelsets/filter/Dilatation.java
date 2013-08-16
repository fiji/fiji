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
   public Dilatation(final boolean[][] mask)
   {
      super(mask);
   }

   protected final void processPosition(final int x, final int y, final WritableRaster raster, final Raster in)
   {
      for (int i = 0; i < mask.length; i++)
      {
         for (int j = 0; j < mask[0].length; j++)
         {
            //if (i == center && j == center) continue;
            if (mask[i][j] == true)
            {
               in.getPixel(i + x + 1 - center, j + y + 1 - center, pixel);
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

   protected final void processPosition(final int x, final int y, final int width, final short[] source, final short[] target) { // inverse order than raster, in
      for (int i = 0; i < mask.length; i++)
      {
         for (int j = 0; j < mask[0].length; j++)
         {
            //if (i == center && j == center) continue;
            if (mask[i][j] == true)
            {
		final int pos = (i + x + 1 - center) + (j + y + 1 - center) * width;
                //in.getPixel(i + x + 1 - center, j + y + 1 - center, pixel);
               if (source[pos] > 0)
               {
                  //raster.setPixel(x, y, pixel);
		  target[pos] = source[pos];
                  break;
               }
            }
         }
      }
   }
   
}
