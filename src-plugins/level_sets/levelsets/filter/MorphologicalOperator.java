package levelsets.filter;

import java.awt.image.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Base class for all morphological operations.
 */
public abstract class MorphologicalOperator implements Filter
{
   /**
    * The structuring element
    */
   protected boolean[][] mask = null;
   /**
    * A preallocated array for pixel storage
    */
   protected int[] pixel = new int[3];
   /**
    * Mask center index - held to avoid repeated costly calculations
    */
   protected int center = 0;
      
   /** Creates a new instance of MorphologicalOperator */
   public MorphologicalOperator(boolean[][] mask)
   {
      this.mask = mask;
      center = mask.length / 2 + 1;
   }
   
   // see javadoch in interface Filter
   public BufferedImage filter(BufferedImage input)
   {
      BufferedImage image =
              new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
      
      Raster in = input.getRaster();
      WritableRaster raster = image.getRaster();

      int width = image.getWidth();
      int height = image.getHeight();
      for (int i = 0; i < width; i ++)
      {
         for (int j = 0; j < height; j ++)
         {
            if (i < center || i > (width - center - 1)) continue;
            if (j < center || j > (height - center - 1)) continue;
            processPosition(i, j, raster, in);
         }
      }
      
      return image;
   }
   
   /**
    * Returns a completely set mask
    * @param width Mask width
    * @param height Mask height
    * @return The mask
    */
   public static boolean[][] getTrueMask(int width, int height)
   {
       boolean[][] newMask = new boolean[width][height];
       for (int i = 0; i< newMask.length; i++)
       {
           for (int j = 0; j < newMask[0].length; j++)
           {
               {
                   newMask[i][j] = true;
               }
           }
       }
       
       return newMask;
   }
   
   /**
    * Processes a postion in the image.
    * @param x The X coordinate
    * @param y The Y coordinate
    * @param raster The output raster
    * @param in The input raster
    */
   protected abstract void processPosition(int x, int y, WritableRaster raster, Raster in);
}
