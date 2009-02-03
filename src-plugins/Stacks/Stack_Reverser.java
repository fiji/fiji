import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/** This plugin reverses the order of the slices in a stack. */

public class Stack_Reverser implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL+NO_UNDO;
    }

    public void run(ImageProcessor ip) {
        if (imp.getStackSize()==1)
            IJ.error("Stack required");
        else
            imp.setStack(null, reverseStack(imp.getStack(), imp));
            
    }

    /** Returns a stack with that has the order of the slices
        in 'stack' reversed. */
    public ImageStack reverseStack(ImageStack stack, ImagePlus imp) {
         int n;
         ImageStack stack2 = imp.createEmptyStack();
         while ((n=stack.getSize())>0) { 
             stack2.addSlice(stack.getSliceLabel(n), stack.getProcessor(n));
             stack.deleteLastSlice();
         }
         return stack2;
    }
}

