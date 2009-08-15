/*
* File: Grouped_ZProjector.java
* Written: 25 Jan. 2000
* Author: Charlie Holly <caholly@colby.edu>, Holly Mountain Software
*
* This code is in the public domain and may be freely used,
* modified, or redistributed without permission.
*/

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.*;
import ij.process.*;


/**
* <p>
* Grouped_ZProjector performs a z-projection on a stack of images.
* The stack is segregated into some number of groups (by specifying
* a group size) and a z-projection is performed on each group using
* ZProjector.  The result is a new stack of images, one slice for
* each group. The output stack has the same type as the input stack.
* The projection methods supported are the same as for ZProjector;
* currently "Average Intensity", "Max Intensity", and "Sum Slices".
* (It appears that ZProjector actually uses the "Average Intensity"
* method instead of the "Sum Slices" method.) </p>
* <p>
* Grouped_ZProjector is primarily designed to be run as a plugin
* from within the ImageJ environment, but may also be used by other
* plugins, etc...  When launched as a plugin, the default constructor
* is used and then the run method is called.  Other code wishing to
* use Grouped_ZProjector should use the setMethod and computeProjection
* methods directly; the buildDialog getMethod, and validGroupSize methods
* may also come in handy. </p>
*
* @author   Charlie Holly <caholly@colby.edu>, Holly Mountain Software
* @version  Last Modified 20 May 2000 (only documentation changes since 3
Feb. 2000).
*/
public class Grouped_ZProjector implements PlugIn {

    static private int defaultMethod = ZProjector.MAX_METHOD;

   /** Strings denoting the possible projection methods.  These should
    *  be the same as those supported by ZProjector. */
    // Note: This will need to be updated if the projection methods supported
    // by ZProjector changes. */
    static public final String[] methodStrings =
      {"Average Intensity", "Max Intensity", "Sum Slices"};


   /**
    * The default constructor.  This is the constructor used when the
    * plugin is loaded.
    */
    public Grouped_ZProjector() {;} //Nothing to do.


   /**
    * Builds a dialog to query users for the group size and
    * projection method.  The projection method defaultMethod will
    * be selected by default.
    *
    * @param gs      The default group size
   * @param parent  The dialog's parent Frame
    * @return  The resulting GenericDialog
    */
    private GenericDialog buildDialog(int gs, java.awt.Frame parent) {

       GenericDialog gd =
          new GenericDialog("Grouped ZProjector", parent);

        // Text field for the group size.
       gd.addNumericField("Group size:", gs, 0/*digits*/);

        // Selection for the projection type.
        gd.addChoice("Projection Type:",methodStrings,
          methodStrings[defaultMethod]);

        return gd;
    }


   /**
    * Performs the projection using the current projection method.
    *
    * @param imp  The ImagePlus object on which to perform the projection
    * @param gs   The group size
    * @return  The resulting ImagePlus object
    */
    public ImagePlus computeProjection(ImagePlus imp, int gs) {

        int imp_size = imp.getStackSize();

        // Validate the arguments.
        if ( imp_size == 0 )
            throw new IllegalArgumentException("Empty stack.");
        if ( !validGroupSize(imp_size, gs) )
            throw new IllegalArgumentException("Invalid group size.");

        // Create an empty stack to hold the projections.
        // It will have the same width, height, and color model as imp.
        ImageStack out_stack = imp.createEmptyStack();

        // Initialize the ZProjector object.
        ZProjector zproj = new ZProjector(imp);

        // Important note: Slices are numbered 1,2,...,n where n is the
        // number of slices in the stack.
        for (int t=0; t<imp_size/gs; t++) {
            zproj.setStartSlice(t*gs + 1);
            zproj.setStopSlice( (t+1) * gs );
            zproj.setMethod(defaultMethod);
            zproj.doProjection();
            ImagePlus projection =  zproj.getProjection();
            ImageProcessor improc = projection.getProcessor();
            out_stack.addSlice("Proj. " + (t+1), improc);
        }

        ImagePlus out_image = new ImagePlus("Projection of " + imp.getTitle(), out_stack);

        return out_image;
    }

   /**
    * Returns the current projection method.
    */
    public int getMethod() { return defaultMethod; }


   /**
    * This method will be called when the plugin is loaded.
    *
    * @param arg  Currently ignored
    */
    public void run(String arg) {

        // Set the input image.
        ImagePlus in_image = WindowManager.getCurrentImage();

        // Check that there is a current image.
        if( in_image==null ) {
            IJ.noImage(); //This posts a message.
            return;
        }

        //  Make sure the input image is a stack.
        int in_size = in_image.getStackSize();
        if( in_size == 1 ) {
            IJ.error("Grouped_ZProjector:" +
              " this plugin must be called on an image stack.");
            return;
        }

       // Build and display the dialog.
       GenericDialog gd =
          buildDialog(in_image.getStackSize(), IJ.getInstance());
       gd.showDialog();

       if( gd.wasCanceled() )
            return; //The user pushed the cancel button.

        // Set the group size and the method from the dialog.
        int gs = (int)gd.getNextNumber();
        defaultMethod = gd.getNextChoiceIndex();

        // If the entered group size is invalid, post a message and
        // then redisplay the dialog.
        while ( !validGroupSize(in_image.getStackSize(), gs ) ) {
            IJ.showMessage("The group size must evenly divide the " +
              "stack size (" + in_image.getStackSize() + ").");
            gd = buildDialog(in_image.getStackSize(), IJ.getInstance());
            gd.showDialog();

            if(gd.wasCanceled())
                return; //The user pushed the cancel button.
            else {
                // Set the group size and the method from the dialog.
                gs = (int)gd.getNextNumber();
                defaultMethod = gd.getNextChoiceIndex();
            }
        }

        // Get a lock on the image.
        if( !in_image.lock() )
            return; //The image is in use.

        // Perform the projection and display the resulting image stack.
        ImagePlus out_image =
          computeProjection(in_image, gs);
        out_image.show();

        // Free the image lock.
        in_image.unlock();

        // This ensures that the class variables (defaultMethod here)
        // is not reloaded the next time the plugin is launched.
        // This can be that of as an ImageJ bug workaround.
        IJ.register(Grouped_ZProjector.class);

        return;
    }


   /**
    * Sets the current projection method.
    *
    * @param  method  The new projection method.  This must be in the range
    *                 of the methodStrings array.
    */
    public void setMethod(int method) {
      if ( method < 0  ||  method >= methodStrings.length )
          throw new IllegalArgumentException("Unknown method");
      else {
          defaultMethod = method;
      }
    }


   /**
    * Validates the group size.
    *
    * @param  stack_size  The size of the input image stack
    * @param  gs          The group size to be validated
    * @return  true if gs is valid and false otherwise
    */
    public boolean validGroupSize(int stack_size, int gs) {
        boolean retval; // The return value.

        if ( gs > 0  &&  (gs <= stack_size)  &&  (stack_size % gs) == 0 )
            retval = true;
        else
            retval = false;

        return retval;
    }

}  // End Grouped_ZProjector.


