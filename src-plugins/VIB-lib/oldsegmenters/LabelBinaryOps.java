package oldsegmenters;

import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.ImagePlus;
import ij.IJ;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.ArrayList;

/**
 * If an underscore is added to this class name this will run as a plugin that operates the close operation.
 * Don't want the clutter in the menu though
 * User: Tom Larkworthy
 * Date: 06-Jul-2006
 * Time: 13:45:34
 */
public class LabelBinaryOps implements PlugInFilter{
	public int setup(String arg, ImagePlus imp) {
        return DOES_8G;
    }

    public void run(ImageProcessor ip) {
        SegmentatorModel model = new SegmentatorModel(IJ.getImage());


        int currentSlice = model.data.getCurrentSlice();

		ImageProcessor labelData = model.getLabelImagePlus().getStack().getProcessor(currentSlice);

        if(model.getCurrentMaterial() == null){
            IJ.showMessage("please select a label first");
            return;
        }


        close(labelData, IJ.getImage().getRoi(), (byte)model.getCurrentMaterial().id);

		model.updateSlice(currentSlice);
    }

	public static void dilate(ImageProcessor labelData, Roi roi, byte id){


		int width = labelData.getWidth();
    	byte[] pixelData = (byte[]) labelData.getPixels();

		dilate(roi, pixelData, width, id);

	}

	private static void dilate(Roi roi,  byte[] pixelData,int width, byte id) {
		ArrayList<Integer> dilateOffset = new ArrayList<Integer>();
        Rectangle bounds;
		if(roi != null){
			bounds = roi.getBoundingRect();
		}else{
			bounds = new Rectangle(0,0,width, pixelData.length/width);
		}
		for (int x = bounds.x; x <= bounds.x + bounds.width; x++) {
			for (int y = bounds.y; y <= bounds.y + bounds.height; y++) {
				if (roi.contains(x,y)) {
					//found a pixel
					//now superimpose 3x3 kernal
					kernalLoop:
					 for(int i=x-1; i<=x+1; i++){
						for(int j=y-1; j<=y+1; j++){
							//if any pixels are labelled in the kernal then dilation adds the current label
							int kernalOffset = i+ j*width;

							if(kernalOffset < 0 || kernalOffset >= pixelData.length) continue; //out of bounds

							if(pixelData[kernalOffset] == id){
								dilateOffset.add(x+y*width);
								break kernalLoop;
							}
						}
					}
				}
			}
		}

		for (Integer offset : dilateOffset) {
			pixelData[offset] = id;
		}
	}

	public static void erode(ImageProcessor labelData,Roi roi,  byte id){

     	byte[] pixelData = (byte[]) labelData.getPixels();
		int width = labelData.getWidth();

		erode(roi, pixelData, width, id);

	}

	private static void erode(Roi roi, byte[] pixelData,int width, byte id) {
		ArrayList<Integer> erodeOffsets = new ArrayList<Integer>();

		Rectangle bounds;
		if(roi != null){
			bounds = roi.getBoundingRect();
		}else{
			bounds = new Rectangle(0,0,width, pixelData.length/width);
		}
		for (int x = bounds.x; x <= bounds.x + bounds.width; x++) {
			for (int y = bounds.y; y <= bounds.y + bounds.height; y++) {
				if (roi.contains(x,y)) {
					//found a pixel
					//now superimpose 3x3 kernal
					kernalLoop:
					 for(int i=x-1; i<=x+1; i++){
						for(int j=y-1; j<=y+1; j++){
							//if any pixels are unlabelled in the kernal then removes the current label
							int kernalOffset = i+ j*width;

							if(kernalOffset < 0 || kernalOffset >= pixelData.length) continue; //out of bounds

							if(pixelData[kernalOffset] != id && pixelData[x+y*width] == id){
								erodeOffsets.add(x+y*width);
								break kernalLoop;
							}
						}
					}
				}
			}
		}
		for (Integer errodeOffset : erodeOffsets) {
			pixelData[errodeOffset] = 0;
		}
	}

	//performs a binary close operation on the label data for the specified material
	public static void close(ImageProcessor labelData, Roi roi,  byte id) {
   		dilate(labelData, roi, id);
		erode(labelData, roi, id);
	}

	//performs a binary open operation on the label data for the specified material
	public static void open(ImageProcessor labelData, Roi roi,  byte id) {
   		erode(labelData, roi, id);
		dilate(labelData, roi, id);
	}

	public static void clean(Roi roi, byte[] pixelData,int width, byte id) {
   		dilate(roi, pixelData, width, id);
		erode(roi, pixelData, width, id);
		erode(roi, pixelData, width, id);
		dilate(roi, pixelData, width, id);
	}
}
