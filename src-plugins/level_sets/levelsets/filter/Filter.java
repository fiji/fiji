// $Revision: 1.2 $, $Date: 2003/11/19 22:35:11 $, $Author: Administrator $

package levelsets.filter;

import java.awt.image.BufferedImage;

/**
 * This interface is implemented by all classes performing operations on input
 * images and returning output images
 */
public interface Filter {
    
   /**
    * Performs the filter operation on  the passed image
    * @param input The input image
    * @return The result image
    */
   public BufferedImage filter(BufferedImage input);

   public void filter(final int width, final int height, final short[] source, final short[] target);
}
