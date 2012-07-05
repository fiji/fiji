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
   protected final boolean[][] mask;
   /**
    * A preallocated array for pixel storage
    */
   protected final int[] pixel = new int[3];
   /**
    * Mask center index - held to avoid repeated costly calculations
    */
   protected int center = 0;
      
   /** Creates a new instance of MorphologicalOperator */
   public MorphologicalOperator(final boolean[][] mask)
   {
      this.mask = mask;
      center = mask.length / 2 + 1;
   }
   
   // see javadoch in interface Filter
   public BufferedImage filter(final BufferedImage input)
   {
      final BufferedImage image =
              new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
      
      final Raster in = input.getRaster();
      final WritableRaster raster = image.getRaster();

      final int width = image.getWidth();
      final int height = image.getHeight();
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

   public final void filter(final int width, final int height, final short[] source, final short[] target) {
      for (int i = 0; i < width; i ++)
      {
         for (int j = 0; j < height; j ++)
         {
            if (i < center || i > (width - center - 1)) continue;
            if (j < center || j > (height - center - 1)) continue;
            processPosition(i, j, width, source, target);
         }
      }
   }
   
   /**
    * Returns a completely set mask
    * @param width Mask width
    * @param height Mask height
    * @return The mask
    */
   public final static boolean[][] getTrueMask(final int width, final int height)
   {
       final boolean[][] newMask = new boolean[width][height];
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

   protected abstract void processPosition(final int x, final int y, final int width, final short[] source, final short[] target); // inverse order than raster, in
}
