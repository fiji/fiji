/*
 * SUSAN.java
 *
 * Created on 17. März 2005, 19:36
 */

package levelsets.filter;

import java.awt.image.*;
import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author Arne
 */
public class SUSAN implements Filter
{
   private int[][] usan = null;
   boolean[][] mask = null;
   private int radius = 0;
   private int uniValueThreshold = 0;
   private int[] pixel = null;
   private int maxUSAN = 0;
   
   public SUSAN(int radius, int uniValueThreshold)
   {
      this.radius = radius;
      this.uniValueThreshold = uniValueThreshold;
      mask = generateCircleMask();
   }
   
   public BufferedImage filter(BufferedImage image)
   {
      pixel = new int[image.getSampleModel().getNumBands()];
      usan = new int[image.getWidth()][image.getHeight()];
      calculateUSAN(image);
      
      return buildResponseImage();
   }
   
   private void calculateUSAN(BufferedImage img)
   {
      int width = img.getWidth();
      int height = img.getHeight();

      Raster raster = img.getRaster();
      
      for (int i = 0; i < width; i += 1)
      {
         for (int j = 0; j < height; j += 1)
         {
            usan[i][j] = getUSANForPixel(i, j, raster);
         }
      }
      
   }
   
   private int getUSANForPixel(int x, int y, Raster raster)
   {
      int blockradius = radius;
      int pixelUSAN = 0;
      if (x - blockradius < 0) blockradius = x;
      if (y - blockradius < 0) blockradius = y;
      if (x + blockradius > raster.getWidth() - 1)
      {
         blockradius = raster.getWidth() - 1 - x;
      }
      if (y + blockradius > raster.getHeight() - 1)
      {
         blockradius = raster.getHeight() - 1 - y;
      }
      
      pixel = raster.getPixel(x, y, pixel);
      int nucleusValue = pixel[0];
      
      for (int i = x - blockradius; i <= x + blockradius; i++)
      {
         for (int j = y - blockradius; j <= y + blockradius; j++)
         {
            if (i == x && j == y) continue; //do not consider the nucleus
            if (mask[i - x + blockradius][j - y + blockradius] == false) continue;
            
            pixel = raster.getPixel(i, j, pixel);
            if (Math.abs(pixel[0] - nucleusValue) < uniValueThreshold)
            {
               pixelUSAN++;
            }
         }
      }
      
      return pixelUSAN;
   }
   
   private boolean[][] generateCircleMask()
   {
      int size = radius * 2 + 1;
      boolean[][] mask = new boolean[size][size];
      maxUSAN = 0;
      Ellipse2D circle = new Ellipse2D.Float(0, 0, size, size);
      
      for (int i = 0; i < size; i++)
      {
         for (int j = 0; j < size; j++)
         {
            if (circle.contains(i, j))
            {
               mask[i][j] = true;
               maxUSAN++; 
               System.out.print(" * ");
            }
            else
            {
               mask[i][j] = false;
               System.out.print(" - ");
            }
         }
         System.out.println();
      }
      
//      boolean[][] mask = new boolean[][] 
//      {{false, false, true, true, true, false, false},
//       {false, true, true, true, true, true, false},
//       {true, true, true, true, true, true, true},
//       {true, true, true, true, true, true, true},
//       {true, true, true, true, true, true, true},
//       {false, true, true, true, true, true, false},
//       {false, false, true, true, true, false, false}};
//       maxUSAN = 37;
      
      return mask;
   }
   
   private BufferedImage buildResponseImage()
   {
      BufferedImage image =
              new BufferedImage(usan.length, usan[0].length, BufferedImage.TYPE_INT_RGB);
      
      //      System.out.println("Number of response image samples -> " + image.getSampleModel().getNumBands());
      
      WritableRaster raster = image.getRaster();
      int[] rgbpixel = new int[3];
      int max = 3 * maxUSAN / 4;
      
      for (int i = 0; i < image.getWidth(); i ++)
      {
         for (int j = 0; j < image.getHeight(); j ++)
         {
            //            int response = maxresponse - usan[i][j];
            //            if (usan[i][j] < )
            rgbpixel[0] = rgbpixel[1] = rgbpixel[2] =
//                    (usan[i][j] < (max)) ? 255 : 0;
                    255 - ((max - usan[i][j]));
            raster.setPixel(i, j, rgbpixel);
         }
      }
      
      return image;
   }
}
