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
    public GreyValueErosion(boolean[][] mask) {
        super(mask);
    }
    
    protected void processPosition(int x, int y, WritableRaster raster, Raster in) {
        int minValue = Integer.MAX_VALUE;
        for (int i = 0; i < mask.length; i++) {
            for (int j = 0; j < mask[0].length; j++) {
                //if (i == center && j == center) continue;
                if (mask[i][j] == true) {
                    pixel = in.getPixel(i + x + 1 - center, j + y + 1 - center, pixel);
                    if (pixel[0] < minValue) {
                        minValue = pixel[0];
                    }
                }
            }
        }
        
        pixel[0] = pixel[1] = pixel[2] = minValue;
        raster.setPixel(x, y, pixel);
    }
    
}
