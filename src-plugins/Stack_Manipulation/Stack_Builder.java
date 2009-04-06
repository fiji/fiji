import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/**
This plugin creates a stack from the ROIs on all open images.
Author: Nicolas Roggli
Organisation: University of Geneva, Switzerland
Version 0.9
*/

public class Stack_Builder implements PlugIn {
 
    public void run(String arg) {
        
        ImagePlus imp;
        ImageProcessor ip;
        int nbrFenetre;
        int[] listFenetre;
        Rectangle[] listRoi;
        int maxWidth, maxHeight;
        boolean rgbStack = false;

        nbrFenetre = WindowManager.getWindowCount();
        listFenetre = new int[nbrFenetre];
        listRoi = new Rectangle[nbrFenetre];
        listFenetre = WindowManager.getIDList();
        maxWidth=0;
        maxHeight=0;
        if (listFenetre==null)
            {IJ.noImage(); return;}

        // Create an array with ROIs attributes, search the biggest width and height
        // and check image types
        boolean allSameType =true;
        boolean is16or32BitImage = false;
        int typeOfFirstImage = WindowManager.getImage(listFenetre[0]).getType();
        for(int i=0; i<nbrFenetre; i++){
            imp=WindowManager.getImage(listFenetre[i]);
              int type = imp.getType();
            if (type!=typeOfFirstImage)
                allSameType = false;
            switch (type) {
                case ImagePlus.GRAY16: case ImagePlus.GRAY32:
                   is16or32BitImage=true; break;
                case ImagePlus.COLOR_RGB:
                    rgbStack = true;
                default:
            }
            ip=imp.getProcessor();
            listRoi[i]= ip.getRoi();
            if (listRoi[i].width>maxWidth) {maxWidth=listRoi[i].width;};
            if (listRoi[i].height>maxHeight) {maxHeight=listRoi[i].height;};
            //if (maxWidth>ip.getWidth()) {maxWidth=ip.getWidth();};
            //if (maxHeight>ip.getHeight()) {maxHeight=ip.getHeight();};
        }

        if (!allSameType && is16or32BitImage) {
            if (!IJ.showMessageWithCancel("Stack Builder", "Images are not all the same type and some are\n"+
            "16 or 32 bits. All the images will be converted\n"+
            "to 8-bits or RGB to create the stack."))
               return;
        }
         
        // Resizes the ROIs according to the biggest size found 
        for(int i=0; i<nbrFenetre; i++){
            listRoi[i].x-=(int)Math.ceil((maxWidth-listRoi[i].width)/2);
            if (listRoi[i].x<0)
                listRoi[i].x =0;
            listRoi[i].y-=(int)Math.ceil((maxHeight-listRoi[i].height)/2);
            if (listRoi[i].y<0) {
                listRoi[i].y=0;
            };
            listRoi[i].width=maxWidth;
            listRoi[i].height=maxHeight;
        }
        
        // set the ROIs and make a stack from the selections
        ImageStack newStack= new ImageStack(maxWidth, maxHeight);
        for(int i=0; i<nbrFenetre; i++){
            imp=WindowManager.getImage(listFenetre[i]);
            ip=imp.getProcessor();
            if (listRoi[i].x+listRoi[i].width>=ip.getWidth())
                listRoi[i].x =ip.getWidth()-listRoi[i].width;
            if (listRoi[i].y+listRoi[i].height>=ip.getHeight())
                listRoi[i].y =ip.getHeight()-listRoi[i].height;
            ip.setRoi(listRoi[i]);
            ip=ip.crop();
            if (ip.getWidth()!=maxWidth || ip.getHeight()!=maxHeight) {
               ImageProcessor ip2 = ip.createProcessor(maxWidth, maxHeight);
               ip2.setColor(Toolbar.getForegroundColor());
               ip2.fill();
               ip2.insert(ip, (maxWidth-ip.getWidth())/2, (maxHeight-ip.getHeight())/2);
               ip = ip2;
            }
            if (rgbStack)
                ip = ip.convertToRGB();
            else if (!allSameType)
                ip = ip.convertToByte(true);
            //Rectangle r = ip.getRoi();
            //IJ.write(i+" "+r.width+" "+r.height);
            newStack.addSlice(imp.getTitle(),ip);
        }
        ImagePlus newImage = new ImagePlus("the stack", newStack);
        newImage.show();
    } // end of run

    
}
