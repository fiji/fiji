/*
 * SimpleEdgeDetect.java
 *
 * Created on 14. März 2005, 15:01
 */

package levelsets.filter;

import java.awt.*;
import java.awt.image.*;

/**
 *
 * @author Arne
 */
public class SimpleEdgeDetect implements Filter
{
   float[] edgeKernel = null;
   
   /** Creates a new instance of SimpleEdgeDetect */
   public SimpleEdgeDetect()
   {
      edgeKernel = new float[]{
         2.0f, -4.0f, 2.0f,
         -4.0f, 8.0f, -4.0f,
         2.0f, -4.0f, 2.0f
      };
   }

   public BufferedImage filter(BufferedImage input)
   {
      BufferedImageOp edge = new ConvolveOp(new Kernel(3, 3, edgeKernel));
      
      ColorModel cm = input.getColorModel();
      
      BufferedImage srccpy = edge.createCompatibleDestImage(input, cm);
      input.copyData(srccpy.getRaster());      
      BufferedImage result = edge.createCompatibleDestImage(input, cm);

      return edge.filter(srccpy, result);
   }
   
}
