import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.image.*;
import ij.plugin.filter.*;
import ij.measure.*;

public class Binner_  implements PlugInFilter {
    ImagePlus imp;
    static int xshrink=2, yshrink=2;
	private static String[] methods=new String[4];
	private static int methodindex;
    int product;
    int[] pixel = new int[3];
    int[] sum = new int[3];
    int samples;


    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        if (IJ.versionLessThan("1.28e"))
            return DONE;
        else
            return DOES_ALL+NO_UNDO;
    }

    public void run(ImageProcessor ip) {
        if (showDialog())
            shrink(ip);
    }

    public void shrink(ImageProcessor ip) {
	boolean isstack=(imp.getStackSize()!=1);

	Calibration cal1 = imp.getCalibration();	
        

        samples = ip instanceof ColorProcessor?3:1;
        int w = ip.getWidth()/xshrink;
        int h = ip.getHeight()/yshrink;
	if (!isstack) {
	        if (ip.isInvertedLut()) 
			ip.invert();
		ImageProcessor ip2 = ip.createProcessor(w, h);
	        for (int y=0; y<h; y++)
			for (int x=0; x<w; x++) 
				if (methods[methodindex]=="average")
       		         		ip2.putPixel(x, y, getAverage(ip, x, y));
				else if (methods[methodindex]=="median")
       		         		ip2.putPixel(x, y, getMedian(ip, x, y));
				else if (methods[methodindex]=="max")
       		         		ip2.putPixel(x, y, getMax(ip, x, y));
				else if (methods[methodindex]=="min")
       		         		ip2.putPixel(x, y, getMin(ip, x, y));
	        if (ip.isInvertedLut()) ip.invert();
       		ip2.resetMinAndMax();
	        new ImagePlus("Reduced "+imp.getShortTitle(), ip2).show();

	ImagePlus imp2 = WindowManager.getCurrentImage();
	Calibration cal2 = imp2.getCalibration();
	cal2.pixelWidth = cal1.pixelWidth*xshrink;
	cal2.pixelHeight = cal1.pixelHeight*yshrink;
	cal2.setUnit (cal1.getUnit());
	imp2.updateAndRepaintWindow();
	
    	}

	else {	// we have a stack, so loop through all ImageProcessors
		ColorModel cm=imp.createLut().getColorModel();
		ImageStack stack=imp.getStack();
		ImageStack newStack = new ImageStack (w,h,cm);
		for (int z=1; z<=imp.getStackSize(); z++) {
			IJ.showProgress(z/imp.getStackSize());
			ImageProcessor theSlice=stack.getProcessor(z);
			ImageProcessor theNewSlice=theSlice.createProcessor(w,h);
	        	if (theSlice.isInvertedLut()) 
				theSlice.invert();
	                for (int y=0; y<h; y++)
				for (int x=0; x<w; x++) {
					if (methods[methodindex]=="average")
						theNewSlice.putPixel(x, y, getAverage(theSlice, x, y));
					else if (methods[methodindex]=="median")
						theNewSlice.putPixel(x, y, getMedian(theSlice, x, y));
					else if (methods[methodindex]=="max")
						theNewSlice.putPixel(x, y, getMax(theSlice, x, y));
					else if (methods[methodindex]=="min")
						theNewSlice.putPixel(x, y, getMin(theSlice, x, y));
				}
	                if (ip.isInvertedLut()) 
				theSlice.invert();
			newStack.addSlice("",theNewSlice);
			}
	       		new ImagePlus("Reduced "+imp.getShortTitle(), newStack).show();
ImagePlus imp2 = WindowManager.getCurrentImage();
	Calibration cal2 = imp2.getCalibration();

	cal2.pixelWidth = cal1.pixelWidth*xshrink;
cal2.pixelHeight = cal1.pixelHeight*yshrink;
String unitz = cal1.getUnit();
cal2.setUnit (unitz);
imp2.updateAndRepaintWindow();
		}
	}

    int[] getAverage(ImageProcessor ip, int x, int y) {
         for (int i=0; i<samples; i++)
            sum[i] = 0;       
         for (int y2=0; y2<yshrink; y2++) {
            for (int x2=0;  x2<xshrink; x2++) {
                pixel = ip.getPixel(x*xshrink+x2, y*yshrink+y2, pixel); 
                for (int i=0; i<samples; i++)
                     sum[i] += pixel[i];
             }
        }
        for (int i=0; i<samples; i++)
            sum[i] = (int)((sum[i]/product)+0.5);
       return sum;
    }

    int[] getMedian(ImageProcessor ip, int x, int y) {
	int shrinksize=xshrink*yshrink;
	int[][] pixels = new int[shrinksize][];
	int p=0;
	// fill pixels within local neighborhood
         for (int y2=0; y2<yshrink; y2++) {
            for (int x2=0;  x2<xshrink; x2++) {
                pixels[p]= ip.getPixel(x*xshrink+x2, y*yshrink+y2, null); 
		p++;
             }
        }

	// find median value
	int k=0;
	int halfsize=shrinksize/2;
	for (int i=0; i<=halfsize; i++) {
		int max=0;
		int mj=0;
		for (int j=0; j<shrinksize; j++) {
			if (pixels[j][k] > max) {
				max = pixels[j][k];
				mj = j;
			}
		}
		pixels[mj][k] = 0;
	}
	int[] max = new int[3];
	max[0]=0;max[1]=0;max[2]=0;
	for (int j=0; j<shrinksize; j++){
		if (pixels[j][k] > max[k])
			max[k] = pixels[j][k];
	}
 
	return max;
    }

    int[] getMax(ImageProcessor ip, int x, int y) {
	int[] max = new int[3];
	max[0]=0;max[1]=0;max[2]=0;
	// fill pixels within local neighborhood
         for (int y2=0; y2<yshrink; y2++) {
            for (int x2=0;  x2<xshrink; x2++) {
                pixel = ip.getPixel(x*xshrink+x2, y*yshrink+y2, pixel); 
                for (int i=0; i<samples; i++)
                     if (pixel[i] > max[i])
			max[i]=pixel[i];
             }
        }
	return max;
	}

    int[] getMin(ImageProcessor ip, int x, int y) {
	int[] max = new int[3];
	max[0]=255;max[1]=255;max[2]=255;
	// fill pixels within local neighborhood
         for (int y2=0; y2<yshrink; y2++) {
            for (int x2=0;  x2<xshrink; x2++) {
                pixel = ip.getPixel(x*xshrink+x2, y*yshrink+y2, pixel); 
                for (int i=0; i<samples; i++)
                     if (pixel[i] < max[i])
			max[i]=pixel[i];
             }
        }
	return max;
	}

    boolean showDialog() {
        GenericDialog gd = new GenericDialog("Image Shrink");
        gd.addNumericField("X Shrink Factor:", xshrink, 0);
        gd.addNumericField("Y Shrink Factor:", yshrink, 0);
	methods[0]="average";
	methods[1]="median";
	methods[2]="max";
	methods[3]="min";
	if (methodindex > methods.length)
		methodindex=0;
	gd.addChoice ("Bin Method: ", methods, methods[methodindex]);
        gd.showDialog();
        if (gd.wasCanceled()) 
            return false;
        xshrink = (int) gd.getNextNumber();
        yshrink = (int) gd.getNextNumber();
	methodindex=gd.getNextChoiceIndex();
        product = xshrink*yshrink;
        return true;
    }


}
