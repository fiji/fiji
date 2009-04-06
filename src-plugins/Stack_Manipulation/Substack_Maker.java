import ij.plugin.frame.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.PlugInFilter;
import ij.text.*;
import ij.measure.*;
import ij.plugin.filter.*;

/**
 * This version requires v1.35f or later: Oct 31, 2005
 *
 * The purpose of this plugin is to extract selected images from a stack to make a new substack.
 * It would take one of two types of input: either a range of images (e.g. 2-14) or a list of
 * images (e.g. 7,9,25,27,34,132) and copy those images from the active stack to a new stack in
 * the order of listing or ranging.
 *
 * @author Anthony Padua
 * @author Neuroradiology
 * @author Duke University Medical Center
 * @author padua001@mc.duke.edu
 *
 * @author Daniel Barboriak, MD
 * @author Neuroradiology
 * @author Duke University Medical Center
 * @author barbo013@mc.duke.edu
 *
 */

public class Substack_Maker implements PlugInFilter {
    private ImagePlus       imp;
    private String          userInput, stackTitle, num, strA, strB;
    private int             rangeStart, rangeEnd, range, currSlice, count;
    private boolean         bAbort;
    private ImageStack      stack, stackNew;
    private ImageProcessor  ip;
    private int[]           numList;
    private Integer         obj;
    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        IJ.register(Substack_Maker.class);
        return DOES_ALL;
    }
    
    public void run(ImageProcessor ip) {
        if(IJ.versionLessThan("1.35f"))                                 // check for appropriate version
            return;
        
        bAbort = false;
        getInput();
        if (bAbort)
            return;
        
        stackTitle = "Substack ("+userInput+")";
        if(stackTitle.length()>25){
            int idxA = stackTitle.indexOf(",",18);
            int idxB = stackTitle.lastIndexOf(",");
            if(idxA>=1 && idxB>=1){
                strA = stackTitle.substring(0,idxA);
                strB = stackTitle.substring(idxB+1);
                stackTitle = strA + ", ... " + strB;
            }
        }
        
        try{
            int idx1 = userInput.indexOf("-");
            if(idx1>=1){                                                    // input displayed in range
                String rngStart = userInput.substring(0,idx1);
                String rngEnd = userInput.substring(idx1+1);
                obj = new Integer(rngStart);
                rangeStart = obj.intValue();
                obj = new Integer(rngEnd);
                rangeEnd = obj.intValue();
                range = rangeEnd-rangeStart+1;
                stackRange(rangeStart,stackTitle);
            }
            else{
                count = 1;                                                  // count # of slices to extract
                for (int j=0; j<userInput.length(); j++) {
                    char ch = Character.toLowerCase(userInput.charAt(j));
                    if(ch==','){count += 1;}
                }
                
                numList = new int[count];
                for(int i=0; i<count; i++){
                    int idx2 = userInput.indexOf(",");
                    if(idx2>0){
                        num = userInput.substring(0,idx2);
                        obj = new Integer(num);
                        numList[i] = obj.intValue();
                        userInput = userInput.substring(idx2+1);
                    }
                    else{
                        num = userInput;
                        obj = new Integer(num);
                        numList[i] = obj.intValue();
                    }
                }
                stackList(numList,stackTitle);
            }
        }
        catch(NumberFormatException e){
            IJ.error("Improper input:\n"+userInput);
            return;
        }
    }
    
    void stackList(int[] numList, String stackTitle){                   // extract specific slices
        try{
            int width = imp.getWidth();
            int height = imp.getHeight();
            int stackSize = imp.getStackSize();
            ImageStack stack = imp.getStack();
            ImageStack stack2 = new ImageStack(width, height, imp.getProcessor().getColorModel());
            
            for (int i=0; i<count; i++) {
                currSlice = numList[i];
                ImageProcessor ip2 = stack.getProcessor(currSlice);
                ip2 = ip2.crop();
                stack2.addSlice(stack.getSliceLabel(currSlice), ip2);
            }
            
            ImagePlus impSubstack = imp.createImagePlus();
            impSubstack.setStack(stackTitle, stack2);
            impSubstack.setCalibration(imp.getCalibration());
            impSubstack.show();
        }
        catch(IllegalArgumentException e){
            IJ.error("Argument out of range: " + userInput);
        }
    }
    
    void stackRange(int currSlice, String stackTitle){                  // extract range of slices
        try{
            int width = imp.getWidth();
            int height = imp.getHeight();
            ImageStack stack = imp.getStack();
            ImageStack stack2 = new ImageStack(width, height, imp.getProcessor().getColorModel());
            
            for (int i=1; i<=range; i++) {
                if(i>1)
                    currSlice += 1;
                ImageProcessor ip2 = stack.getProcessor(currSlice);
                ip2 = ip2.crop();
                stack2.addSlice(stack.getSliceLabel(currSlice), ip2);
            }
            
            ImagePlus impSubstack = imp.createImagePlus();
            impSubstack.setStack(stackTitle, stack2);
            impSubstack.setCalibration(imp.getCalibration());
            impSubstack.show();
        }
        catch(IllegalArgumentException e){
            IJ.error("Argument out of range: " + userInput);
        }
    }
    
    void getInput(){
        GenericDialog gd = new GenericDialog("Substack Maker", IJ.getInstance());
        gd.addMessage("Enter either range (e.g. 2-14) or a list (e.g. 7,9,25,27)");
        gd.addStringField("slices", "", 50);
        gd.showDialog();
        if (gd.wasCanceled()){
            bAbort = true;
            return;
        }
        userInput = gd.getNextString();
        if(userInput.length()==0){
            IJ.error("Input required.");
            bAbort = true;
            return;
        }
    }
    
}
