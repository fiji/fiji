/*
 * SimpleEdgeDetect.java
 *
 * Created on 14. März 2005, 15:01
 */

package levelsets.filter;

import java.awt.*;
import java.awt.image.*;
import ij.process.ShortProcessor;

/**
 *
 * @author Arne
 */
public class SimpleEdgeDetect implements Filter
{
   final float[] edgeKernel;
   
   /** Creates a new instance of SimpleEdgeDetect */
   public SimpleEdgeDetect()
   {
      edgeKernel = new float[]{
         2.0f, -4.0f, 2.0f,
         -4.0f, 8.0f, -4.0f,
         2.0f, -4.0f, 2.0f
      };
   }

   public final BufferedImage filter(final BufferedImage input)
   {
      final BufferedImageOp edge = new ConvolveOp(new Kernel(3, 3, edgeKernel));
      
      final ColorModel cm = input.getColorModel();
      
      final BufferedImage srccpy = edge.createCompatibleDestImage(input, cm);
      input.copyData(srccpy.getRaster());      
      final BufferedImage result = edge.createCompatibleDestImage(input, cm);

      return edge.filter(srccpy, result);
   }

   public void filter(final int width, final int height, final short[] source, final short[] target) {
	final ShortProcessor sp = new ShortProcessor(width, height, source, null);
	final BufferedImage bi = filter(sp.get16BitBufferedImage());
        final DataBufferUShort db = (DataBufferUShort)bi.getData().getDataBuffer();
        System.arraycopy(target, 0, db.getData(), 0, target.length);
   }
   
}
