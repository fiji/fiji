/*
 * GreyValueErosion.java
 *
 * Created on 25. Juli 2005, 12:55
 */

package levelsets.filter;

import java.awt.image.*;

/**
 * Performs a grey value erosion operation on the input using a defineable mask.
 */
public class GreyValueErosion extends MorphologicalOperator {
    
    /** Creates a new instance of GreyValueErosion */
    public GreyValueErosion(final boolean[][] mask) {
        super(mask);
    }
    
    final protected void processPosition(final int x, final int y, final WritableRaster raster, final Raster in) {
        int minValue = Integer.MAX_VALUE;
        for (int i = 0; i < mask.length; i++) {
            for (int j = 0; j < mask[0].length; j++) {
                //if (i == center && j == center) continue;
                if (mask[i][j] == true) {
                    in.getPixel(i + x + 1 - center, j + y + 1 - center, pixel);
                    if (pixel[0] < minValue) {
                        minValue = pixel[0];
                    }
                }
            }
        }
        
        pixel[0] = pixel[1] = pixel[2] = minValue;
        raster.setPixel(x, y, pixel);
    }

   protected final void processPosition(final int x, final int y, final int width, final short[] source, final short[] target) { // inverse order than raster, in
        short minValue = Short.MAX_VALUE;
        for (int i = 0; i < mask.length; i++) {
            for (int j = 0; j < mask[0].length; j++) {
                //if (i == center && j == center) continue;
                if (mask[i][j] == true) {
                    //in.getPixel(i + x + 1 - center, j + y + 1 - center, pixel);
		    final int pos = (i + x + 1 - center) + (j + y + 1 - center) * width;
                    if (source[pos] < minValue) {
                        minValue = source[pos];
                    }
                }
            }
        }
        
        //raster.setPixel(x, y, pixel);
	target[x + y * width] = minValue;
   }
    
}
