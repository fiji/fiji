/*
 * Sobel.java
 *
 * Created on 7. Juli 2005, 10:42
 */

package levelsets.filter;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.DataBufferUShort;

import levelsets.ij.ImageContainer;
import ij.process.ShortProcessor;


/**
 *
 * @author Arne
 */
public class Sobel implements Filter
{
   public static float[] HORIZONTAL_KERNEL = new float[]
   {
      1.0f, 2.0f, 1.0f,
              0.0f, 0.0f, 0.0f,
              -1.0f, -2.0f, -1.0f
   };
   
   public static float[] VERTICAL_KERNEL = new float[]
   {
      -1.0f, 0.0f, 1.0f,
              -2.0f, 0.0f, 2.0f,
              -1.0f, 0.0f, 1.0f
   };
   
   public BufferedImage filter(BufferedImage input)
   {
      System.out.println("Sobel filter");
        Filter laplace = new ConvolutionFilter(ConvolutionFilter.LAPLACE_EDGE_KERNEL); 
      
      BufferedImage laplace_image = laplace.filter(input);
        
      double [][][] data = new double[input.getWidth()][input.getHeight()][1];
      int [] pixel = new int[4];
      
      Raster laplace_raster = laplace_image.getRaster();      
      
      for (int x = 0; x < input.getWidth(); x++)
      {
         for (int y = 0; y < input.getHeight(); y++)
         {
            data[x][y][0] += laplace_raster.getPixel(x, y, pixel)[0];
            if (data[x][y][0] > 255) data[x][y][0] = 255;
         }
      }
      
//      ImageContainer cnt = new ImageContainer(new BufferedImage[] {input});
//      cnt.setData(data);
//      
//      return cnt.getImage(0);
      return null;
   }

   public void filter(final int width, final int height, final short[] source, final short[] target) {
	final ShortProcessor sp = new ShortProcessor(width, height, source, null);
	final BufferedImage bi = filter(sp.get16BitBufferedImage());
        final DataBufferUShort db = (DataBufferUShort)bi.getData().getDataBuffer();
        System.arraycopy(target, 0, db.getData(), 0, target.length);
   }
}
