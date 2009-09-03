// $Revision: 1.2 $, $Date: 2003/11/19 22:35:09 $, $Author: Administrator $

package levelsets.filter;

import java.awt.*;
import java.awt.image.*;

/**
 *
 */
public class GreyScaleFilter implements Filter {
      
   public BufferedImage filter(BufferedImage image) {
      BufferedImage result = new BufferedImage(
         image.getWidth(null), image.getHeight(null), image.getType());
      if (!(image instanceof WritableRenderedImage)) return null;
      
      if (image.getNumXTiles() > 1 || image.getNumYTiles() > 1) return null;
      if (result.getNumXTiles() > 1 || result.getNumYTiles() > 1) return null;
      
      WritableRaster in = image.getWritableTile(1, 1);
      WritableRaster out = result.getWritableTile(1, 1);
      
      int[] pixel = new int[4];
      for (int columns = 0; columns < in.getWidth(); columns++) {
         for (int lines = 0; lines < in.getHeight(); lines++) {
            pixel = in.getPixel(columns, lines, pixel);
            int sum = (pixel[0] + pixel[1] + pixel[2]) / 3;
            pixel[0] = pixel[1] = pixel[2] = sum;
            out.setPixel(columns, lines, pixel);
         }
      }
      
      image.releaseWritableTile(1, 1);
      result.releaseWritableTile(1, 1);
      
      return result;
   }

   public void filter(final int width, final int height, final short[] source, final short[] target) {
      System.arraycopy(source, 0, target, 0, target.length);
   }
}
