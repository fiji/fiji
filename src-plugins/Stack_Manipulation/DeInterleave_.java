

import ij.*;           /*  Russell Kincaid     */
import ij.process.*;   /*  rekincai@syr.edu    */
import ij.gui.*;       /*  November 15, 2002    */
import java.awt.*;     /*    hacked from       */
import ij.plugin.*;    /* Slice_Multiplier.java etc.  */
			/*further modified			by tony collins*/
import ij.measure.Calibration;

/* This plugin splits slices from time bins in a stack. */
/* it DOES NOT assume that the number of timebins exactly divides the number of slices */
/* it requires explicit input about where to divide the stack */
/* it can deal with odd ball slice arrangements */

public class DeInterleave_ implements PlugIn {

    int[] startSlice;
    int  start1Slice = 1;
    int  start2Slice = 0;
    int  start3Slice = 0;
    int  start4Slice = 0;
    int  start5Slice = 0;
    int  start6Slice = 0;
    int  start7Slice = 0;
    int  start8Slice = 0;
    int  start9Slice = 0;
    int  start10Slice = 0;
    int  start11Slice = 0;
    int  start12Slice = 0;
    int  start13Slice = 0;
    int  start14Slice = 0;
    int  start15Slice = 0;
    int  start16Slice = 0;
    int  start17Slice = 0;
    int  start18Slice = 0;
    int  start19Slice = 0;
    int  start20Slice = 0;
  //   	private static boolean displayCounts =Prefs.get("ICP_counts.boolean",true);

     int channels = (int)Prefs.get("Deint_ch.int", 2);
    boolean keep = Prefs.get("Deint_keep.boolean", true);
    String endLabel;

    public void run(String arg) {

        ImagePlus imp2 = WindowManager.getCurrentImage();


        if (imp2==null)
            {IJ.noImage(); return;}
        ImageStack stack2 = imp2.getStack();
        if (stack2.getSize()==1)
            {IJ.error("Stack Required"); return;}

        if (!showDialog(stack2))
            return;


    String fileName2 = imp2.getTitle();
	Calibration oc = imp2.getCalibration().copy();
  new ImagePlus(fileName2, makeShuffled(stack2)).show();


  ImagePlus imp = WindowManager.getCurrentImage();


        String fileName = imp.getTitle();
   ImageStack stack = imp.getStack();

        startSlice = new int[22];   /* pretend I'm only using cells 1 - 12, i.e. not cell 0 */

        startSlice [1] = start1Slice;
        startSlice [2] = start2Slice;
        startSlice [3] = start3Slice;
        startSlice [4] = start4Slice;
        startSlice [5] = start5Slice;
        startSlice [6] = start6Slice;
        startSlice [7] = start7Slice;
        startSlice [8] = start8Slice;
        startSlice [9] = start9Slice;
        startSlice [10] = start10Slice;
        startSlice [11] = start11Slice;
        startSlice [12] = start12Slice;
        startSlice [13] = start13Slice;
        startSlice [14] = start14Slice;
        startSlice [15] = start15Slice;
        startSlice [16] = start16Slice;
        startSlice [17] = start17Slice;
        startSlice [18] = start18Slice;
        startSlice [19] = start19Slice;
      startSlice [20] = start20Slice;

            int stackSize = stack.getSize();
            int subStackEndSlice =2;
    /*  do some crude error handling for insane input */
            for (int SUBSTACK=1; SUBSTACK<=21; SUBSTACK++)  {
            if (startSlice[SUBSTACK] < 1)   /*  if they're already zero they get zero, no problem */
               startSlice[SUBSTACK] = 0;
            if (startSlice[SUBSTACK] > stackSize)
               startSlice[SUBSTACK] = 0;
            if (startSlice[SUBSTACK] < startSlice[SUBSTACK - 1])
               startSlice[SUBSTACK] = 0;
            }
    /*  now the array values will work  */

            for (int SUBSTACK=1; SUBSTACK<=21; SUBSTACK++)  {
               String startLabel = Integer.toString(startSlice[SUBSTACK]);
               if (SUBSTACK ==21)  { /* this if-else sets the endpoint of this substack */
                  subStackEndSlice = stackSize;
               }
               else   {  /* this if-else sets the endpoint of this substack */
                  if (startSlice[SUBSTACK + 1] > 0)  {  /* we're doing more after this one */
                     subStackEndSlice = startSlice[SUBSTACK + 1]-1;  /* end at the slice before the next start number */
                  }
                  else  {  /* we're NOT doing more after this one */
                     subStackEndSlice = stackSize;
                  }
               }
               String endLabel = Integer.toString(subStackEndSlice);
               String subStackName = Integer.toString(SUBSTACK);
               if (startSlice[SUBSTACK] > 0)
                  new ImagePlus(fileName+ " #" + subStackName, makeSubStack(stack, startSlice[SUBSTACK], subStackEndSlice)).show();
		ImagePlus impTmp = WindowManager.getCurrentImage();
		impTmp.setCalibration(oc);
		 impTmp.getWindow().repaint();
            }
imp.changes = false;
imp.getWindow().close();
if (!keep) {
			imp2.changes = false;
			imp2.getWindow().close();
			}
        IJ.register(DeInterleave_.class);

    }

    public boolean showDialog(ImageStack stack) {
        GenericDialog gd = new GenericDialog("De-Interleaver");
        gd.addNumericField("How many Channels?",channels,0);
gd.addCheckbox("Keep Source Stacks", keep);
          gd.showDialog();
        if (gd.wasCanceled())
            return false;
channels= (int) gd.getNextNumber();
keep = gd.getNextBoolean();

Prefs.set("Deint_ch.int", (int)channels);
Prefs.set("Deint_keep.boolean", keep);

start1Slice = 1;
start2Slice = (stack.getSize()/channels) +1;
start3Slice = (start2Slice) + (stack.getSize()/channels);
start4Slice = (start3Slice) + (stack.getSize()/channels);
start5Slice = (start4Slice) + (stack.getSize()/channels);
start6Slice = (start5Slice) + (stack.getSize()/channels);
start7Slice = (start6Slice) + (stack.getSize()/channels);
start8Slice = (start7Slice) + (stack.getSize()/channels);
start9Slice = (start8Slice) + (stack.getSize()/channels);
start10Slice = (start9Slice) + (stack.getSize()/channels);
start11Slice = (start10Slice) + (stack.getSize()/channels);
start12Slice = (start11Slice) + (stack.getSize()/channels);
start13Slice = (start12Slice) + (stack.getSize()/channels);
start14Slice = (start13Slice) + (stack.getSize()/channels);
start15Slice = (start14Slice) + (stack.getSize()/channels);
start16Slice = (start15Slice) + (stack.getSize()/channels);
start17Slice = (start16Slice) + (stack.getSize()/channels);
start18Slice = (start17Slice) + (stack.getSize()/channels);
start19Slice = (start18Slice) + (stack.getSize()/channels);
start20Slice = (start19Slice) + (stack.getSize()/channels);
        return true;
    }

    public ImageStack makeSubStack(ImageStack stack, int theStartSlice, int theEndSlice) {
              /* returns an ImageStack */
                int B_numDetRows = stack.getHeight();
                int C_numDetCols = stack.getWidth();

                ImageStack newStack = new ImageStack(C_numDetCols, B_numDetRows, stack.getColorModel());

        for (int SLICE=theStartSlice; SLICE<=theEndSlice; SLICE++) {
           ImageProcessor ip = stack.getProcessor(1);
           ImageProcessor ip1;
               ip1 = stack.getProcessor(SLICE);
               newStack.addSlice(null,ip1);
        }
        return newStack;
    }

    public ImageStack makeShuffled(ImageStack stack) {   /* returns an
ImageStack */
                int A_stackSize = stack.getSize();
                int B_numDetRows = stack.getHeight();
                int C_numDetCols = stack.getWidth();
                int OLDSLICE = 0;
                int SLICE=0;   /* SLICE is the NEW slice number */
                /*  just takes the slices in a new order 0,3,6,etc.,then
1,4,7,etc.,then 2,5,8,etc.  */
                ImageStack newStack = new ImageStack(C_numDetCols,
B_numDetRows, stack.getColorModel());

                ImageProcessor ip = stack.getProcessor(1);   /* just to
name the ip first ??? */
        ImageProcessor ip1;
                for (int HEAD=1; HEAD<=channels; HEAD++) {    /* puts them into
order */
                   OLDSLICE = HEAD;
                   while (OLDSLICE <= A_stackSize) {
              ip1 = stack.getProcessor(OLDSLICE);
                      newStack.addSlice(null,ip1,SLICE);
                      OLDSLICE = (OLDSLICE + channels);
                      SLICE = (SLICE + 1);
                   }
            }
        return newStack;
    }



}







