import ij.plugin.filter.PlugInFilter;
import ij.ImagePlus;
import ij.IJ;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.process.ByteProcessor;

import java.awt.*;

import Jama.Matrix;

/**
 * Created by IntelliJ IDEA.
 * User: dhovis
 * Date: Jun 20, 2008
 * Time: 10:31:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class Remove_Slope implements PlugInFilter {

    protected ImagePlus imagePlus;
    public int setup(String string, ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
        return DOES_32 + DOES_STACKS;//+SUPPORTS_MASKING;
    }

    public void run(ImageProcessor ip) {
        Rectangle selection = ip.getRoi();

        int selectionX = selection.width;
        int selectionY = selection.height;

        ByteProcessor maskProcessor = (ByteProcessor)imagePlus.getMask();
        byte[]mask;
        if (maskProcessor != null)
            mask = (byte[])maskProcessor.getPixels();
        else
        {
            mask = new byte[selectionX*selectionY];
            for (int jj = 0; jj<mask.length; jj++)
                mask[jj]=-1;
        }

        int numMaskPixels = 0;
        for (int ii=0; ii<mask.length; ii++)
        {
            //IJ.write(""+mask[ii]);
            if (mask[ii]<0)
                numMaskPixels++;
        }

        //IJ.write("numMaskPixels: " +numMaskPixels);
        if (numMaskPixels < 4){
            IJ.write(""+mask.length);
            IJ.write("Selection is not big enough.  Please Try Again");
            return;
        }
        float[] pixels = (float[])ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();


        double sumX  = 0.;      // Sum of x values
        double sumX2 = 0.;      // Sum of x^2
        double sumXY = 0.;      // Sum of x*y
        double sumY  = 0.;      // Sum of y values
        double sumY2 = 0.;      //Sum of y^2
        double sumXZ = 0.;      //Sum of x*z
        double sumYZ = 0.;      //Sum of y*z
        double sumZ  = 0.;      //Sum of Z vlues
        double sumPoints = 0.;

        for (int yy=0;yy<selectionY; yy++){
            for (int xx=0; xx<selectionX; xx++){
                int actualY = selection.y+yy;
                int actualX = selection.x+xx;



                float currentZ = pixels[actualY*width+actualX];

                //IJ.write("actualX: " + actualX + ",  actualY: "+ actualY + ",  currentZ: " + currentZ);
                //IJ.write("currentZ: "+ currentZ);
                //IJ.write("yy: " + yy+"/"+selectionY + "  xx: " + xx+"/"+selectionX);
                //IJ.write("mask index: "+ (yy*selectionX+xx) + " mask.length: " + mask.length);
                if (mask[yy*selectionX+xx]<0){
                    sumX  += actualX;
                    sumX2 += actualX*actualX;
                    sumXY += actualX*actualY;
                    sumY  += actualY;
                    sumY2 += actualY*actualY;
                    sumXZ += actualX*currentZ;
                    sumYZ += actualY*currentZ;
                    sumZ  += currentZ;
                    sumPoints +=1.;
                }
            }
        }

        /*
        IJ.write("SumX: " + sumX);
        IJ.write("SumX2: " + sumX2);
        IJ.write("SumXY: " + sumXY);
        IJ.write("SumY: " + sumY);
        IJ.write("SumY2: " + sumY2);
        IJ.write("SumXZ: " + sumXZ);
        IJ.write("SumYZ: " + sumYZ);
        IJ.write("SumZ: " + sumZ);
*/

        Matrix A = new Matrix(3,3);
        A.set(0, 0, sumX2);
        A.set(0, 1, sumXY);
        A.set(0, 2, sumX);

        A.set(1, 0, sumXY);
        A.set(1, 1, sumY2);
        A.set(1, 2, sumY);

        A.set(2, 0, sumX);
        A.set(2, 1, sumY);
        A.set(2, 2, sumPoints);

        Matrix B = new Matrix(3, 1);
        B.set(0, 0, sumXZ);
        B.set(1, 0, sumYZ);
        B.set(2, 0, sumZ);

        double[] result = A.solve(B).getRowPackedCopy();

        //IJ.write(""+result.length);
        // zfit = X*result[0] + Y*result[1] + result[2]

        // Now we have a planar fit to the selection.  Subtract it from the entire image

        for (int yy = 0; yy<height; yy++)
            for (int xx = 0; xx<width; xx++){
                int offset = yy*width + xx;
                float zfit = (float)(xx*result[0] + yy*result[1] + result[2]);
                pixels[offset] = pixels[offset]-zfit;
            }


        //IJ.write("" + result[0]);
        //IJ.write("" + result[1]);
        //IJ.write("" + result[2]);
        FloatProcessor fip = (FloatProcessor) ip;
        fip.resetMinAndMax();
        fip.findMinAndMax();
        fip.setMinAndMax(fip.getMin(), fip.getMax());

    }
}
