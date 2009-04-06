import ij.*;
import ij.macro.Interpreter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;
import ij.plugin.filter.*;

/** Concatenates two or more images.
 *  Gives the option of viewing the concatenated stack as a 4D image (requires 'Image5D' plugin)
 *  @author Jon Jackson j.jackson # ucl.ac.uk
 *  last modified June 29 2006
 */

public class Concatenate_ implements PlugIn, ItemListener{
    public String pluginName =  "Concatenate_";
    static boolean all_option = false;
    boolean keep = false;
    static boolean keep_option = false;
    boolean batch = false;
    boolean macro = false;
    boolean im5D = false; // determines whether or not to include the Image5D option on the dialog box
    static boolean im5D_option = false;
    public int maxEntries = 18;  // limit number of entries to fit on screen
    String[] imageTitles;
    ImagePlus[] images;
    Vector choices;
    Checkbox allWindows;
    final String none = "-- None --";
    String newtitle = "Concatenated Stacks";
    ImagePlus newImp;
    int stackSize;
    double min = 0, max = Float.MAX_VALUE;
    
    /** Optional string argument sets the name dialog boxes if called from another plugin
     */
    public void run(String arg) {
        macro = ! arg.equals("");
        if (! setupDialog()) return;
        IJ.register(Concatenate_.class);
        newImp = createHypervol();
        if (newImp != null) {
            newImp.show();
            if (im5D) { // Image5D creates new Image from concat_Imp
                WindowManager.setTempCurrentImage(newImp);
                IJ.run("Stack to Image5D", "3rd=z 4th=ch 3rd_dimension_size=" + stackSize + " 4th_dimension_size=1");
                newImp = WindowManager.getImage(newtitle);
                newImp.getProcessor().setMinAndMax(min, max);
                newImp.updateAndDraw();
                WindowManager.setCurrentWindow(newImp.getWindow());
            }
        }
    }
    
    // Launch a dialog requiring user to choose images
    // returns ImagePlus of concatenated images
    public ImagePlus run() {
        if (! setupDialog()) return null;
        newImp = createHypervol();
        return newImp;
    }
    
    // concatenate two images
    public ImagePlus concatenate(ImagePlus imp1, ImagePlus imp2, boolean keep) {
        images = new ImagePlus[2];
        images[0] = imp1;
        images[1] = imp2;
        return concatenate(images, keep);
    }
    
    // concatenate more than two images
    public ImagePlus concatenate(ImagePlus[] ims, boolean keepIms) {
        images = ims;
        imageTitles = new String[ims.length];
        for (int i = 0; i < ims.length; i++) {
            if (ims[i] != null) {
                imageTitles[i] = ims[i].getTitle();
            } else {
                IJ.error(pluginName, "Null ImagePlus passed to concatenate(...) method");
                return null;
            }
        }
        keep = keepIms;
        batch = true;
        im5D = false;
        newImp = createHypervol();
        return newImp;
    }
    
    ImagePlus createHypervol() {
        boolean firstImage = true;
        boolean duplicated;
        Properties[] propertyArr = new Properties[images.length];
        ImagePlus currentImp = null;
        ImageStack concat_Stack = null;
        stackSize = 0;
        int dataType = 0, width= 0, height = 0;
        Calibration cal = null;
        int count = 0;
        for (int i = 0; i < images.length; i++) {
            if (images[i] != null) { // Should only find null imp if user has closed an image after starting plugin (unlikely...)
                currentImp = images[i];
                
                if (firstImage) { // Initialise based on first image
                    //concat_Imp = images[i];
                    cal = currentImp.getCalibration();
                    width = currentImp.getWidth();
                    height = currentImp.getHeight();
                    stackSize = currentImp.getNSlices();
                    dataType = currentImp.getType();
                    concat_Stack = currentImp.createEmptyStack();
                    min = currentImp.getProcessor().getMin();
                    max = currentImp.getProcessor().getMax();
                    firstImage = false;
                }
                
                // Safety Checks
                if (currentImp.getNSlices() != stackSize && im5D) {
                    IJ.error(pluginName, "Cannot create Image5D, stack sizes not equal");
                    return null;
                }
                if (currentImp.getType() != dataType) {
                    IJ.log("Omitting " + imageTitles[i] + " - image type not matched");
                    continue;
                }
                if (currentImp.getWidth() != width || currentImp.getHeight() != height) {
                    IJ.log("Omitting " + imageTitles[i] + " - dimensions not matched");
                    continue;
                }
                
                // concatenate
                duplicated = isDuplicated(currentImp, i);
                concat(concat_Stack, currentImp.getStack(), (keep || duplicated));
                propertyArr[count] = currentImp.getProperties();
                imageTitles[count] = currentImp.getTitle();
                if (! (keep || duplicated)) {
                    currentImp.changes = false;
                    currentImp.hide();
                }
                count++;
            }
        }
        
        // Copy across info fields
        ImagePlus imp = new ImagePlus(newtitle, concat_Stack);
        imp.setCalibration(cal);
        imp.setProperty("Number of Stacks", new Integer(count));
        imp.setProperty("Stacks Properties", propertyArr);
        imp.setProperty("Image Titles", imageTitles);
        imp.getProcessor().setMinAndMax(min, max);
        return imp;
    }
    
    // taken from WSR's Concatenator_.java
    void concat(ImageStack stack3, ImageStack stack1, boolean dup) {
        int slice = 1;
        int size = stack1.getSize();
        for (int i = 1; i <= size; i++) {
            ImageProcessor ip = stack1.getProcessor(slice);
            String label = stack1.getSliceLabel(slice);
            if (dup) {
                ip = ip.duplicate();
                slice++;
            } else
                stack1.deleteSlice(slice);
            stack3.addSlice(label, ip);
        }
    }
    
    boolean setupDialog() {
        boolean all_windows = false;
        batch = Interpreter.isBatchMode();
        macro = macro | Macro.getOptions() != null;
        im5D = Menus.commandInUse("Stack to Image5D") && ! batch;
        if (macro) maxEntries = Integer.MAX_VALUE; // screen size is not limitation in macro mode
        
        // Checks
        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No windows are open.");
            return false;
        } else if (wList.length < 2) {
            IJ.error("Two or more windows must be open");
            return false;
        }
        int nImages = wList.length;
        
        String[] titles = new String[nImages];
        String[] titles_none = new String[nImages + 1];
        for (int i=0; i<nImages; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null) {
                titles[i] = imp.getTitle();
                titles_none[i] = imp.getTitle();
            } else {
                titles[i] = "";
                titles_none[i] = "";
            }
        }
        titles_none[nImages] = none;
        
        GenericDialog gd = new GenericDialog(pluginName, IJ.getInstance());
        gd.addCheckbox("All_Open Windows", all_option);
        if (macro) {
            for (int i = 0; i < ((nImages+1)<maxEntries?(nImages+1):maxEntries); i++) {
                // the none string is used in macro mode so that images will not be selected by default
                gd.addChoice("Image_" + (i+1), titles_none, none);
            }
        } else {
            gd.addChoice("Image_1:", titles, titles[0]);
            gd.addChoice("Image_2:", titles, titles[1]);
            for (int i = 2; i < ((nImages+1)<maxEntries?(nImages+1):maxEntries); i++) {
                gd.addChoice("Image_" + (i+1), titles_none, titles_none[i]);
            }
        }
        gd.addStringField("Title:", newtitle, 16);
        gd.addCheckbox("Keep Original Images", keep_option);
        if (im5D) gd.addCheckbox("4D_image", im5D_option);
        if (! macro) { // Monitor user selections
            choices = gd.getChoices();
            for (Enumeration e = choices.elements() ; e.hasMoreElements() ;) {
                ((Choice)e.nextElement()).addItemListener(this);
            }
            Vector v = gd.getCheckboxes();
            allWindows = (Checkbox)v.firstElement();
            allWindows.addItemListener(this);
            if (all_option) itemStateChanged(new ItemEvent(allWindows, ItemEvent.ITEM_STATE_CHANGED, null, ItemEvent.SELECTED));
        }
        gd.showDialog();
        
        if (gd.wasCanceled())
            return false;
        all_windows = gd.getNextBoolean();
        all_option = all_windows;
        newtitle = gd.getNextString();
        keep = gd.getNextBoolean();
        keep_option = keep;
        if (im5D) {
            im5D = gd.getNextBoolean();
            im5D_option = im5D;
        }
        
        ImagePlus[] tmpImpArr = new ImagePlus[nImages+1];
        String[] tmpStrArr = new String[nImages+1];
        int index, count = 0;
        for (int i = 0; i < (nImages+1); i++) { // compile a list of images to concatenate from user selection
            if (all_windows) { // Useful to not have to specify images in batch mode
                index = i;
            } else {
                if (i == ((nImages+1)<maxEntries?(nImages+1):maxEntries) ) break;
                index = gd.getNextChoiceIndex();
            }
            if (index >= nImages) break; // reached the 'none' string or handled all images (in case of all_windows)
            if (! titles[index].equals("")) {
                tmpStrArr[count] = titles[index];
                tmpImpArr[count] = WindowManager.getImage(wList[index]);
                count++;
            }
        }
        if (count < 2) {
            IJ.error(pluginName, "Please select at least 2 images");
            return false;
        }
        
        imageTitles = new String[count];
        images = new ImagePlus[count];
        System.arraycopy(tmpStrArr, 0, imageTitles, 0, count);
        System.arraycopy(tmpImpArr, 0, images, 0, count);
        return true;
    }
    
    // test if this imageplus appears again in the list
    boolean isDuplicated(ImagePlus imp, int index) {
        int length = images.length;
        if (index >= length - 1) return false;
        for (int i = index + 1; i < length; i++) {
            if (imp == images[i]) return true;
        }
        return false;
    }
    
    public void	itemStateChanged(ItemEvent ie) {
        Choice c;
        if (ie.getSource() == allWindows) { // User selected / unselected 'all windows' button
            int count = 0;
            if (allWindows.getState()) {
                for (Enumeration e = choices.elements() ; e.hasMoreElements() ;) {
                    c = (Choice)e.nextElement();
                    c.select(count++);
                    c.setEnabled(false);
                }
            } else {
                for (Enumeration e = choices.elements() ; e.hasMoreElements() ;) {
                    c = (Choice)e.nextElement();
                    c.setEnabled(true);
                }
            }
        } else { // User image selection triggered event
            boolean foundNone = false;
            // All image choices after an occurance of 'none' are reset to 'none'
            for (Enumeration e = choices.elements() ; e.hasMoreElements() ;) {
                c = (Choice)e.nextElement();
                if (! foundNone) {
                    c.setEnabled(true);
                    if (c.getSelectedItem().equals(none)) foundNone = true;
                } else { // a previous choice was 'none'
                    c.select(none);
                    c.setEnabled(false);
                }
            }
        }
    }
    
    public void setIm5D(boolean bool) {
        im5D = bool;
    }
//     Concatenate_(String arg) { // not IJ compatible
//     super();
//        pluginName=arg;
//        im5D = false;
//    }
    
}
