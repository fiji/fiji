/*====================================================================
| Version: July 8, 2006 by Steve Bryson and Carlos îscar
| modified to replace Tiff image output with high-quality Jpeg output
\===================================================================*/

/*====================================================================
| Carlos Oscar Sanchez Sorzano
| Unidad de Bioinformatica
| Centro Nacional de Biotecnologia (CSIC)
| Campus Universidad Autonoma (Cantoblanco)
| E-28049 Madrid
| Spain
|
| phone (CET): +34(91)585.45.10
| fax: +34(91)585.45.06
| RFC-822: coss@cnb.uam.es
| URL: http://biocomp.cnb.uam.es/
\===================================================================*/

/*====================================================================
| This work is based on the following paper:
|
| C.O.S. Sorzano, P. Thevenaz, M. Unser
| Elastic Registration of Biological Images Using Vector-Spline Regularization
| IEEE Transactions on Biomedical Imaging, 52: 652-663 (2005)
|
| This paper is available on-line at
| http://bigwww.epfl.ch/publications/sorzano0501.html
|
| Other relevant on-line publications are available at
| http://bigwww.epfl.ch/publications/
\===================================================================*/

/*====================================================================
| Additional help available at http://bigwww.epfl.ch/thevenaz/UnwarpJ/
|
| You'll be free to use this software for research purposes, but you
| should not redistribute it without our consent. In addition, we expect
| you to include a citation or acknowledgment whenever you present or
| publish results that are based on it.
\===================================================================*/

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.io.FileSaver;
import ij.plugin.JpegWriter;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

/*====================================================================
|   UnwarpJ_
\===================================================================*/
/* Main class.
   This is the one called by ImageJ */

/*------------------------------------------------------------------*/
public class UnwarpJ_
   implements
      PlugIn

{ /* begin class UnwarpJ_ */

/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void run (
   final String commandLine
) {
   String options = Macro.getOptions();
   if (!commandLine.equals("")) options = commandLine;
   if (options == null) {
      Runtime.getRuntime().gc();
      final ImagePlus[] imageList = createImageList();
      if (imageList.length < 2) {
         IJ.error("At least two images are required (stack of color images disallowed)");
         return;
      }

      final unwarpJDialog dialog = new unwarpJDialog(IJ.getInstance(), imageList);
      GUI.center(dialog);
      dialog.setVisible(true);
   } else {
      final String[] args = getTokens(options);
      if (args.length<1) {
         dumpSyntax();
         return;
      } else {
         if      (args[0].equals("-help"))      dumpSyntax();
         else if (args[0].equals("-align"))     alignImagesMacro(args);
         else if (args[0].equals("-transform")) transformImageMacro(args);
      }
      return;
   }
} /* end run */

/*------------------------------------------------------------------*/
public static void main(String args[]) {
   if (args.length<1) {
      dumpSyntax();
      System.exit(1);
   } else {      
      if      (args[0].equals("-help"))      dumpSyntax();
      else if (args[0].equals("-align"))     alignImagesMacro(args);
      else if (args[0].equals("-transform")) transformImageMacro(args);
   }
   System.exit(0);
}

/*....................................................................
   Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private static void alignImagesMacro(String args[]) {

   if(args.length < 11)
   {
       dumpSyntax();
       System.exit(0);
   }

   // Check if -output_jpg at the end
   int args_len=args.length;
   String last_argument=args[args_len-1];
   boolean jpeg_output=last_argument.equals("-jpg_output");
   if (jpeg_output) args_len--;

   // Read input parameters
   String fn_target=args[1];
   String fn_target_mask=args[2];
   String fn_source=args[3];
   String fn_source_mask=args[4];
   int min_scale_deformation=((Integer) new Integer(args[5])).intValue();
   int max_scale_deformation=((Integer) new Integer(args[6])).intValue();
   double  divWeight=((Double) new Double(args[7])).doubleValue();
   double  curlWeight=((Double) new Double(args[8])).doubleValue();
   double  imageWeight=((Double) new Double(args[9])).doubleValue();
   String fn_out=args[10];
   double  landmarkWeight=0;
   String fn_landmark="";
   if (args_len>=13) {
      landmarkWeight=((Double) new Double(args[11])).doubleValue();
      fn_landmark=args[12];
   }
   double  stopThreshold=1e-2;
   if (args_len>=14)
      stopThreshold=((Double) new Double(args[13])).doubleValue();

   // Show parameters
   IJ.write("Target image           : "+fn_target);
   IJ.write("Target mask            : "+fn_target_mask);
   IJ.write("Source image           : "+fn_source);
   IJ.write("Source mask            : "+fn_source_mask);
   IJ.write("Min. Scale Deformation : "+min_scale_deformation);
   IJ.write("Max. Scale Deformation : "+max_scale_deformation);
   IJ.write("Div. Weight            : "+divWeight);
   IJ.write("Curl Weight            : "+curlWeight);
   IJ.write("Image Weight           : "+imageWeight);
   IJ.write("Output:                : "+fn_out);
   IJ.write("Landmark Weight        : "+landmarkWeight);
   IJ.write("Landmark file          : "+fn_landmark);
   IJ.write("JPEG Output            : "+jpeg_output);

   // Produce side information
   int     imagePyramidDepth=max_scale_deformation-min_scale_deformation+1;
   int     min_scale_image=0;
   int     outputLevel=-1;
   boolean showMarquardtOptim=false;
   int     accurate_mode=1;
   boolean saveTransf=true;

   String fn_tnf="";
   int dot = fn_out.lastIndexOf('.');
   if (dot == -1) fn_tnf=fn_out + "_transf.txt";
   else           fn_tnf=fn_out.substring(0, dot)+"_transf.txt";

   // Open target
   Opener opener=new Opener();
   ImagePlus targetImp;
   targetImp=opener.openImage(fn_target);
   unwarpJImageModel target =
      new unwarpJImageModel(targetImp.getProcessor(), true);
   target.setPyramidDepth(imagePyramidDepth+min_scale_image);
   target.getThread().start();
   unwarpJMask targetMsk =
      new unwarpJMask(targetImp.getProcessor(),false);
   if (fn_target_mask.equalsIgnoreCase(new String("NULL")) == false) 
       targetMsk.readFile(fn_target_mask);
   unwarpJPointHandler targetPh=null;

   // Open source
   ImagePlus sourceImp;
   sourceImp=opener.openImage(fn_source);
   unwarpJImageModel source =
      new unwarpJImageModel(sourceImp.getProcessor(), false);
   source.setPyramidDepth(imagePyramidDepth+min_scale_image);
   source.getThread().start();
   unwarpJMask sourceMsk =
       new unwarpJMask(sourceImp.getProcessor(),false);   
   if (fn_source_mask.equalsIgnoreCase(new String("NULL")) == false)
       sourceMsk.readFile(fn_source_mask);   
   unwarpJPointHandler sourcePh=null;

   // Load landmarks
   if (fn_landmark!="") {
      Stack sourceStack = new Stack();
      Stack targetStack = new Stack();
      unwarpJMiscTools.loadPoints(fn_landmark,sourceStack,targetStack);

      sourcePh  = new unwarpJPointHandler(sourceImp);
      targetPh  = new unwarpJPointHandler(targetImp);
      while ((!sourceStack.empty()) && (!targetStack.empty())) {
         Point sourcePoint = (Point)sourceStack.pop();
         Point targetPoint = (Point)targetStack.pop();
         sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
         targetPh.addPoint(targetPoint.x, targetPoint.y);
      }
   }

   // Join threads
   try {
    source.getThread().join();
    target.getThread().join();
  } catch (InterruptedException e) {
    IJ.error("Unexpected interruption exception " + e);
  }

   // Perform registration
   ImagePlus output_ip=new ImagePlus();
   unwarpJDialog dialog=null;
   final unwarpJTransformation warp = new unwarpJTransformation(
     sourceImp, targetImp, source, target, sourcePh, targetPh,
     sourceMsk, targetMsk, min_scale_deformation, max_scale_deformation,
     min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
     stopThreshold, outputLevel, showMarquardtOptim, accurate_mode,
     saveTransf, fn_tnf, output_ip, dialog);
   warp.doRegistration();

   // Save results
   ImageConverter converter=new ImageConverter(output_ip);
   converter.convertToGray16();
   FileSaver fs=new FileSaver(output_ip);
   if (jpeg_output) {
      JpegWriter js = new JpegWriter();
      js.setQuality(100);
      WindowManager.setTempCurrentImage(output_ip);	
      js.run(fn_out);
   } else
      fs.saveAsTiff(fn_out);
}

/*------------------------------------------------------------------*/
private ImagePlus[] createImageList (
) {
   final int[] windowList = WindowManager.getIDList();
   final Stack stack = new Stack();
   for (int k = 0; ((windowList != null) && (k < windowList.length)); k++) {
      final ImagePlus imp = WindowManager.getImage(windowList[k]);
      final int inputType = imp.getType();
      if ((imp.getStackSize() == 1) || (inputType == imp.GRAY8) || (inputType == imp.GRAY16)
         || (inputType == imp.GRAY32)) {
         stack.push(imp);
      }
   }
   final ImagePlus[] imageList = new ImagePlus[stack.size()];
   int k = 0;
   while (!stack.isEmpty()) {
      imageList[k++] = (ImagePlus)stack.pop();
   }
   return(imageList);
} /* end createImageList */

/*------------------------------------------------------------------*/
private static void dumpSyntax (
) {
   IJ.write("Purpose: Elastic registration of two images.");
   IJ.write(" ");
   IJ.write("Usage: unwarpj ");
   IJ.write("  -help                       : SHOWS THIS MESSAGE");
   IJ.write("");
   IJ.write("  -align                      : ALIGN TWO IMAGES");
   IJ.write("          target_image        : In any image format");
   IJ.write("          target_mask         : In any image format");
   IJ.write("          source_image        : In any image format");
   IJ.write("          source_mask         : In any image format");
   IJ.write("          min_scale_def       : Scale of the coarsest deformation");
   IJ.write("                                0 is the coarsest possible");
   IJ.write("          max_scale_def       : Scale of the finest deformation");
   IJ.write("          Div_weight          : Weight of the divergence term");
   IJ.write("          Curl_weight         : Weight of the curl term");
   IJ.write("          Image_weight        : Weight of the image term");
   IJ.write("          Output image        : Output result in JPG (100%)");
   IJ.write("          Optional parameters :");
   IJ.write("             Landmark_weight  : Weight of the landmarks");
   IJ.write("             Landmark_file    : Landmark file");
   IJ.write("             stopThreshold    : By default 1e-2");
   IJ.write("");
   IJ.write("  -transform                  : TRANSFORM AN IMAGE WITH A GIVEN DEFORMATION");
   IJ.write("          target_image        : In any image format");
   IJ.write("          source_image        : In any image format");
   IJ.write("          transformation_file : As saved by UnwarpJ");
   IJ.write("          Output image        : Output result in JPG (100%)");
   IJ.write("");
   IJ.write("Examples:");
   IJ.write("Align two images without landmarks and without mask");
   IJ.write("   unwarpj -align target.jpg NULL source.jpg NULL 0 2 1 1 1 output.tif");
   IJ.write("Align two images with landmarks and mask");
   IJ.write("   unwarpj -align target.tif target_mask.tif source.tif source_mask.tif 0 2 1 1 1 1 landmarks.txt output.tif");
   IJ.write("Align two images using only landmarks");
   IJ.write("   unwarpj -align target.jpg NULL source.jpg NULL 0 2 1 1 0 output.tif 1 landmarks.txt");
   IJ.write("Transform the source image with a previously computed transformation");
   IJ.write("   unwarpj -transform target.jpg source.jpg transformation.txt output.tif");
   IJ.write("");
   IJ.write("JPEG Output:");
   IJ.write("   If you want to produce JPEG output simply add -jpg_output as the last argument");
   IJ.write("   of the alignment or transformation command. For instance:");
   IJ.write("   unwarpj -align target.jpg NULL source.jpg NULL 0 2 1 1 1 output.jpg -jpg_output");
   IJ.write("   unwarpj -align target.tif target_mask.tif source.tif source_mask.tif 0 2 1 1 1 1 landmarks.txt output.jpg -jpg_output");
   IJ.write("   unwarpj -align target.jpg NULL source.jpg NULL 0 2 1 1 0 output.jpg 1 landmarks.txt -jpg_output");
   IJ.write("   unwarpj -transform target.jpg source.jpg transformation.txt output.jpg -jpg_output");
} /* end dumpSyntax */

/*------------------------------------------------------------------*/
private String[] getTokens (
	final String options
) {
	StringTokenizer t = new StringTokenizer(options);
	String[] token = new String[t.countTokens()];
	for (int k = 0; (k < token.length); k++) {
		token[k] = t.nextToken();
	}
	return(token);
} /* end getTokens */

/*------------------------------------------------------------------*/
private static void transformImageMacro(String args[]) {
   // Read input parameters
   String fn_target=args[1];
   String fn_source=args[2];
   String fn_tnf   =args[3];
   String fn_out   =args[4];

   // Jpeg output
   String last_argument=args[args.length-1];
   boolean jpeg_output=last_argument.equals("-jpg_output");

   // Show parameters
   IJ.write("Target image           : "+fn_target);
   IJ.write("Source image           : "+fn_source);
   IJ.write("Transformation file    : "+fn_tnf);
   IJ.write("Output                 : "+fn_out);
   IJ.write("JPEG output            : "+jpeg_output);

   // Open target
   Opener opener=new Opener();
   ImagePlus targetImp;
   targetImp=opener.openImage(fn_target);

   // Open source
   ImagePlus sourceImp;
   sourceImp=opener.openImage(fn_source);
   unwarpJImageModel source =
      new unwarpJImageModel(sourceImp.getProcessor(), false);
   source.setPyramidDepth(0);
   source.getThread().start();

   // Load transformation
   int intervals=unwarpJMiscTools.
      numberOfIntervalsOfTransformation(fn_tnf);
   double [][]cx=new double[intervals+3][intervals+3];
   double [][]cy=new double[intervals+3][intervals+3];
   unwarpJMiscTools.loadTransformation(fn_tnf, cx, cy);

   // Join threads
   try {
      source.getThread().join();
   } catch (InterruptedException e) {
      IJ.error("Unexpected interruption exception " + e);
   }

   // Apply transformation to source
   unwarpJMiscTools.applyTransformationToSource(
      sourceImp,targetImp,source,intervals,cx,cy);

   // Save results
   FileSaver fs=new FileSaver(sourceImp);
   if (jpeg_output) {
      JpegWriter js = new JpegWriter();
      js.setQuality(100);
      WindowManager.setTempCurrentImage(sourceImp);	
      js.run(fn_out);
   } else
      fs.saveAsTiff(fn_out);
}

} /* end class UnwarpJ_ */

/*====================================================================
|   unwarpJClearAll
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJClearAll
   extends
      Dialog
   implements
      ActionListener

{ /* begin class unwarpJClearAll */

/*....................................................................
   Private variables
....................................................................*/

private ImagePlus sourceImp;
private ImagePlus targetImp;
private unwarpJPointHandler sourcePh;
private unwarpJPointHandler targetPh;

/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void actionPerformed (
   final ActionEvent ae
) {
   if (ae.getActionCommand().equals("Clear All")) {
      sourcePh.removePoints();
      targetPh.removePoints();
      setVisible(false);
   }
   else if (ae.getActionCommand().equals("Cancel")) {
      setVisible(false);
   }
} /* end actionPerformed */

/*------------------------------------------------------------------*/
public Insets getInsets (
) {
   return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*------------------------------------------------------------------*/
unwarpJClearAll (
   final Frame parentWindow,
   final ImagePlus sourceImp,
   final ImagePlus targetImp,
   final unwarpJPointHandler sourcePh,
   final unwarpJPointHandler targetPh

) {
   super(parentWindow, "Removing Points", true);
   this.sourceImp = sourceImp;
   this.targetImp = targetImp;
   this.sourcePh = sourcePh;
   this.targetPh = targetPh;
   setLayout(new GridLayout(0, 1));
   final Button removeButton = new Button("Clear All");
   removeButton.addActionListener(this);
   final Button cancelButton = new Button("Cancel");
   cancelButton.addActionListener(this);
   final Label separation1 = new Label("");
   final Label separation2 = new Label("");
   add(separation1);
   add(removeButton);
   add(separation2);
   add(cancelButton);
   pack();
} /* end unwarpJClearAll */

} /* end class unwarpJClearAll */

/*====================================================================
|   unwarpJCredits
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJCredits
   extends
      Dialog

{ /* begin class unwarpJCredits */

/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public Insets getInsets (
) {
   return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*------------------------------------------------------------------*/
public unwarpJCredits (
   final Frame parentWindow
) {
   super(parentWindow, "UnwarpJ", true);
   setLayout(new BorderLayout(0, 20));
   final Label separation = new Label("");
   final Panel buttonPanel = new Panel();
   buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Button doneButton = new Button("Done");
   doneButton.addActionListener(
      new ActionListener (
      ) {
         public void actionPerformed (
            final ActionEvent ae
         ) {
            if (ae.getActionCommand().equals("Done")) {
               dispose();
            }
         }
      }
   );
   buttonPanel.add(doneButton);
   final TextArea text = new TextArea(22, 72);
   text.setEditable(false);
   text.append("\n");
   text.append(" This work is based on the following paper:\n");
   text.append("\n");
   text.append(" C.O.S. Sorzano, P. Th" + (char)233 + "venaz, M. Unser\n");
   text.append(" Elastic Registration of Biological Images Using Vector-Spline Regularization\n");
   text.append(" IEEE Transactions on Biomedical Engineering\n");
   text.append(" vol. ??, no. ??, pp. ??-??, July 2005.\n");
   text.append("\n");
   text.append(" This paper is available on-line at\n");
   text.append(" http://bigwww.epfl.ch/publications/sorzano0501.html\n");
   text.append("\n");
   text.append(" Other relevant on-line publications are available at\n");
   text.append(" http://bigwww.epfl.ch/publications/\n");
   text.append("\n");
   text.append(" Additional help available at\n");
   text.append(" http://bigwww.epfl.ch/thevenaz/UnwarpJ/\n");
   text.append("\n");
   text.append(" You'll be free to use this software for research purposes, but\n");
   text.append(" you should not redistribute it without our consent. In addition,\n");
   text.append(" we expect you to include a citation or acknowledgment whenever\n");
   text.append(" you present or publish results that are based on it.\n");
   add("North", separation);
   add("Center", text);
   add("South", buttonPanel);
   pack();
} /* end unwarpJCredits */

} /* end class unwarpJCredits */

/*====================================================================
|   unwarpJCumulativeQueue
\===================================================================*/
class unwarpJCumulativeQueue extends Vector {
private int ridx;
private int widx;
private int currentLength;
private double sum;

/*------------------------------------------------------------------*/
public unwarpJCumulativeQueue(int length)
   {currentLength=ridx=widx=0; setSize(length);}

/*------------------------------------------------------------------*/
public int currentSize(){return currentLength;}

/*------------------------------------------------------------------*/
public double getSum(){return sum;}

/*------------------------------------------------------------------*/
public double pop_front() {
   if (currentLength==0) return 0.0;
   double x=((Double)elementAt(ridx)).doubleValue();
   currentLength--;
   sum-=x;
   ridx++; if (ridx==size()) ridx=0;
   return x;
}

/*------------------------------------------------------------------*/
public void push_back(double x) {
   if (currentLength==size()) pop_front();
   setElementAt(new Double(x),widx);
   currentLength++;
   sum+=x;
   widx++; if (widx==size()) widx=0;
}

} /* end class unwarpJCumulativeQueue */

/*====================================================================
|   unwarpJDialog
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJDialog
   extends
      Dialog
   implements
      ActionListener

{ /* begin class unwarpJDialog */

/*....................................................................
   Private variables
....................................................................*/

// Advanced dialog
private Dialog advanced_dlg = null;


// List of available images in ImageJ
private ImagePlus[] imageList;

// Image representations (canvas and ImagePlus)
private ImageCanvas sourceIc;
private ImageCanvas targetIc;
private ImagePlus sourceImp;
private ImagePlus targetImp;

// Image models
private unwarpJImageModel source;
private unwarpJImageModel target;

// Image Masks
private unwarpJMask       sourceMsk;
private unwarpJMask       targetMsk;

// Point handlers for the landmarks
private unwarpJPointHandler sourcePh;
private unwarpJPointHandler targetPh;

// Toolbar handler
private boolean clearMask=false;
private unwarpJPointToolbar tb
   = new unwarpJPointToolbar(Toolbar.getInstance(),this);

// Final action
private boolean finalActionLaunched=false;
private boolean stopRegistration=false;

// Dialog related
private final Button DoneButton = new Button("Done");
private TextField min_scaleDeformationTextField;
private TextField max_scaleDeformationTextField;
private TextField divWeightTextField;
private TextField curlWeightTextField;
private TextField landmarkWeightTextField;
private TextField imageWeightTextField;
private TextField stopThresholdTextField;
private int sourceChoiceIndex = 0;
private int targetChoiceIndex = 1;
private static int min_scale_deformation = 0;
private static int max_scale_deformation = 2;
private static int mode = 1;
private Checkbox ckRichOutput;
private Checkbox ckSaveTransformation;

// Transformation parameters
private static int     MIN_SIZE                   = 8;
private static double  divWeight                  = 0;
private static double  curlWeight                 = 0;
private static double  landmarkWeight             = 0;
private static double  imageWeight                = 1;
private static boolean richOutput                 = false;
private static boolean saveTransformation         = false;
private static int     min_scale_image            = 0;
private static int     imagePyramidDepth          = 3;
private static double  stopThreshold              = 1e-2;

/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void actionPerformed (
   final ActionEvent ae
) {
   if (ae.getActionCommand().equals("Cancel")) {
      dispose();
      restoreAll();
   }
   else if (ae.getActionCommand().equals("Done")) {
      dispose();
      joinThreads();
      imagePyramidDepth = max_scale_deformation-min_scale_deformation+1;
      divWeight = Double.valueOf(divWeightTextField.getText()).doubleValue();
      curlWeight = Double.valueOf(curlWeightTextField.getText()).doubleValue();
      landmarkWeight = Double.valueOf(landmarkWeightTextField.getText()).doubleValue();
      imageWeight = Double.valueOf(imageWeightTextField.getText()).doubleValue();
      richOutput = ckRichOutput.getState();
      saveTransformation = ckSaveTransformation.getState();
      int outputLevel=1;
      boolean showMarquardtOptim=false;
      if (richOutput) {
         outputLevel++;
	 showMarquardtOptim=true;
      }

      unwarpJFinalAction finalAction =
         new unwarpJFinalAction(this);
      finalAction.setup(sourceImp,targetImp,
         source, target, sourcePh, targetPh,
         sourceMsk, targetMsk, min_scale_deformation, max_scale_deformation,
         min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
         stopThreshold, outputLevel, showMarquardtOptim, mode);
      finalActionLaunched=true;
      tb.setAllUp(); tb.repaint();
      finalAction.getThread().start();
   }
   else if (ae.getActionCommand().equals("Credits...")) {
      final unwarpJCredits dialog = new unwarpJCredits(IJ.getInstance());
      GUI.center(dialog);
      dialog.setVisible(true);
   } else if (ae.getActionCommand().equals("Advanced Options")) {
       advanced_dlg.setVisible(true);
   } else if (ae.getActionCommand().equals("Done")) {
       advanced_dlg.setVisible(false);
   }
} /* end actionPerformed */

/*------------------------------------------------------------------*/
public void applyTransformationToSource(
   int intervals,
   double [][]cx,
   double [][]cy) {
   // Apply transformation
   unwarpJMiscTools.applyTransformationToSource(
      sourceImp,targetImp,source,intervals,cx,cy);

   // Restart the computation of the model
   cancelSource();
   targetPh.removePoints();
   createSourceImage();
   setSecondaryPointHandlers();
}

/*------------------------------------------------------------------*/
public void createAdvancedOptions() {
   advanced_dlg = new Dialog(new Frame(), "Advanced Options", true);

   // Create min_scale_deformation, max_scale_deformation panel
   advanced_dlg.setLayout(new GridLayout(0, 1));
   final Choice min_scale_deformationChoice = new Choice();
   final Choice max_scale_deformationChoice = new Choice();
   final Panel min_scale_deformationPanel = new Panel();
   final Panel max_scale_deformationPanel = new Panel();
   min_scale_deformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   max_scale_deformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label min_scale_deformationLabel = new Label("Initial Deformation: ");
   final Label max_scale_deformationLabel = new Label("Final Deformation: ");
   min_scale_deformationChoice.add("Very Coarse");
   min_scale_deformationChoice.add("Coarse");
   min_scale_deformationChoice.add("Fine");
   min_scale_deformationChoice.add("Very Fine");
   max_scale_deformationChoice.add("Very Coarse");
   max_scale_deformationChoice.add("Coarse");
   max_scale_deformationChoice.add("Fine");
   max_scale_deformationChoice.add("Very Fine");
   min_scale_deformationChoice.select(min_scale_deformation);
   max_scale_deformationChoice.select(max_scale_deformation);
   min_scale_deformationChoice.addItemListener(
      new ItemListener (
      ) {
         public void itemStateChanged (
            final ItemEvent ie
         ) {
            final int new_min_scale_deformation = 
               min_scale_deformationChoice.getSelectedIndex();
            int new_max_scale_deformation=max_scale_deformation;
            if (max_scale_deformation<new_min_scale_deformation)
                new_max_scale_deformation=new_min_scale_deformation;
            if (new_min_scale_deformation!=min_scale_deformation ||
                new_max_scale_deformation!=max_scale_deformation) {
               min_scale_deformation=new_min_scale_deformation;
               max_scale_deformation=new_max_scale_deformation;
               computeImagePyramidDepth();
               restartModelThreads();
            }
            min_scale_deformationChoice.select(min_scale_deformation);
            max_scale_deformationChoice.select(max_scale_deformation);
         }
      }
   );
   max_scale_deformationChoice.addItemListener(
      new ItemListener (
      ) {
         public void itemStateChanged (
            final ItemEvent ie
         ) {
            final int new_max_scale_deformation = 
               max_scale_deformationChoice.getSelectedIndex();
            int new_min_scale_deformation=min_scale_deformation;
            if (new_max_scale_deformation<min_scale_deformation)
                new_min_scale_deformation=new_max_scale_deformation;
            if (new_max_scale_deformation!=max_scale_deformation ||
                new_min_scale_deformation!=min_scale_deformation) {
               min_scale_deformation=new_min_scale_deformation;
               max_scale_deformation=new_max_scale_deformation;
               computeImagePyramidDepth();
               restartModelThreads();
            }
            max_scale_deformationChoice.select(max_scale_deformation);
            min_scale_deformationChoice.select(min_scale_deformation);
         }
      }
   );
   min_scale_deformationPanel.add(min_scale_deformationLabel);
   max_scale_deformationPanel.add(max_scale_deformationLabel);
   min_scale_deformationPanel.add(min_scale_deformationChoice);
   max_scale_deformationPanel.add(max_scale_deformationChoice);

        // Create div and curl weight panels
   final Panel divWeightPanel= new Panel();
   divWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label label_divWeight = new Label();
   label_divWeight.setText("Divergence Weight:");
   divWeightTextField = new TextField("", 5);
   divWeightTextField.setText(""+divWeight);
   divWeightTextField.addTextListener(
      new TextListener (
      ) {
         public void textValueChanged (
            final TextEvent e
         ) {
            boolean validNumber =true;
            try {
               divWeight = Double.valueOf(divWeightTextField.getText()).doubleValue();
            } catch (NumberFormatException n) {
               validNumber = false;
            }
            DoneButton.setEnabled(validNumber);
         }
      }
   );
   divWeightPanel.add(label_divWeight);
   divWeightPanel.add(divWeightTextField);
        divWeightPanel.setVisible(true);

   final Panel curlWeightPanel= new Panel();
   curlWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label label_curlWeight = new Label();
   label_curlWeight.setText("Curl Weight:");
   curlWeightTextField = new TextField("", 5);
   curlWeightTextField.setText(""+curlWeight);
   curlWeightTextField.addTextListener(
      new TextListener (
      ) {
         public void textValueChanged (
            final TextEvent e
         ) {
            boolean validNumber =true;
            try {
               curlWeight = Double.valueOf(curlWeightTextField.getText()).doubleValue();
            } catch (NumberFormatException n) {
               validNumber = false;
            }
            DoneButton.setEnabled(validNumber);
         }
      }
   );
   curlWeightPanel.add(label_curlWeight);
   curlWeightPanel.add(curlWeightTextField);
        curlWeightPanel.setVisible(true);

        // Create landmark and image weight panels
   final Panel landmarkWeightPanel= new Panel();
   landmarkWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label label_landmarkWeight = new Label();
   label_landmarkWeight.setText("Landmark Weight:");
   landmarkWeightTextField = new TextField("", 5);
   landmarkWeightTextField.setText(""+landmarkWeight);
   landmarkWeightTextField.addTextListener(
      new TextListener (
      ) {
         public void textValueChanged (
            final TextEvent e
         ) {
            boolean validNumber =true;
            try {
               landmarkWeight = Double.valueOf(landmarkWeightTextField.getText()).doubleValue();
            } catch (NumberFormatException n) {
               validNumber = false;
            }
            DoneButton.setEnabled(validNumber);
         }
      }
   );
   landmarkWeightPanel.add(label_landmarkWeight);
   landmarkWeightPanel.add(landmarkWeightTextField);
        landmarkWeightPanel.setVisible(true);

   final Panel imageWeightPanel= new Panel();
   imageWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label label_imageWeight = new Label();
   label_imageWeight.setText("Image Weight:");
   imageWeightTextField = new TextField("", 5);
   imageWeightTextField.setText(""+imageWeight);
   imageWeightTextField.addTextListener(
      new TextListener (
      ) {
         public void textValueChanged (
            final TextEvent e
         ) {
            boolean validNumber =true;
            try {
               imageWeight = Double.valueOf(imageWeightTextField.getText()).doubleValue();
            } catch (NumberFormatException n) {
               validNumber = false;
            }
            DoneButton.setEnabled(validNumber);
         }
      }
   );
   imageWeightPanel.add(label_imageWeight);
   imageWeightPanel.add(imageWeightTextField);
   imageWeightPanel.setVisible(true);

   // Create stopThreshold panel
   final Panel stopThresholdPanel= new Panel();
   stopThresholdPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label label_stopThreshold = new Label();
   label_stopThreshold.setText("Stop Threshold:");
   stopThresholdTextField = new TextField("", 5);
   stopThresholdTextField.setText(""+stopThreshold);
   stopThresholdTextField.addTextListener(
      new TextListener (
      ) {
         public void textValueChanged (
            final TextEvent e
         ) {
            boolean validNumber =true;
            try {
               stopThreshold = Double.valueOf(stopThresholdTextField.getText()).doubleValue();
            } catch (NumberFormatException n) {
               validNumber = false;
            }
            DoneButton.setEnabled(validNumber);
         }
      }
   );
   stopThresholdPanel.add(label_stopThreshold);
   stopThresholdPanel.add(stopThresholdTextField);
   stopThresholdPanel.setVisible(true);

   // Create checkbox for creating rich output
   final Panel richOutputPanel=new Panel();
   richOutputPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   ckRichOutput=new Checkbox("Verbose",richOutput);
   richOutputPanel.add(ckRichOutput);

   // Create checkbox for saving the transformation
   final Panel saveTransformationPanel=new Panel();
   saveTransformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   ckSaveTransformation=new Checkbox("Save Transformation",saveTransformation);
   saveTransformationPanel.add(ckSaveTransformation);

   // Create close button
   final Panel buttonPanel = new Panel();
   buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Button CloseButton = new Button("Close");
   CloseButton.addActionListener(
      new ActionListener (
      ) {
         public void actionPerformed (
            final ActionEvent ae
         ) {
            if (ae.getActionCommand().equals("Close")) {
               advanced_dlg.dispose();
            }
         }
      }
   );
   buttonPanel.add(CloseButton);

   // Build separations
   final Label separation1 = new Label("");

   // Create the dialog
   advanced_dlg.add(min_scale_deformationPanel);
   advanced_dlg.add(max_scale_deformationPanel);
   advanced_dlg.add(divWeightPanel);
   advanced_dlg.add(curlWeightPanel);
   advanced_dlg.add(landmarkWeightPanel);
   advanced_dlg.add(imageWeightPanel);
   advanced_dlg.add(stopThresholdPanel);
   advanced_dlg.add(richOutputPanel);
   advanced_dlg.add(saveTransformationPanel);
   advanced_dlg.add(separation1);
   advanced_dlg.add(buttonPanel);
   advanced_dlg.pack();

   advanced_dlg.setVisible(false);
}

/*------------------------------------------------------------------*/
public void freeMemory() {
   advanced_dlg = null;
   imageList    = null;
   sourceIc     = null;
   targetIc     = null;
   sourceImp    = null;
   targetImp    = null;
   source       = null;
   target       = null;
   sourcePh     = null;
   targetPh     = null;
   tb           = null;
   Runtime.getRuntime().gc();
}

/*------------------------------------------------------------------*/
public void grayImage(final unwarpJPointHandler ph) {
   if (ph==sourcePh) {
      int Xdim=source.getWidth();
      int Ydim=source.getHeight();
      FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
      int ij=0;
      double []source_data=source.getImage();
      for (int i=0; i<Ydim; i++)
         for (int j=0; j<Xdim; j++,ij++)
            if (sourceMsk.getValue(j,i))
                 fp.putPixelValue(j,i,    source_data[ij]);
            else fp.putPixelValue(j,i,0.5*source_data[ij]);
      fp.resetMinAndMax();
      sourceImp.setProcessor(sourceImp.getTitle(),fp);
      sourceImp.updateImage();
   } else {
      int Xdim=target.getWidth();
      int Ydim=target.getHeight();
      FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
      double []target_data=target.getImage();
      int ij=0;
      for (int i=0; i<Ydim; i++)
         for (int j=0; j<Xdim; j++,ij++)
            if (targetMsk.getValue(j,i))
                 fp.putPixelValue(j,i,    target_data[ij]);
            else fp.putPixelValue(j,i,0.5*target_data[ij]);
      fp.resetMinAndMax();
      targetImp.setProcessor(targetImp.getTitle(),fp);
      targetImp.updateImage();
   }
}

/*------------------------------------------------------------------*/
public boolean isFinalActionLaunched () {return finalActionLaunched;}

/*------------------------------------------------------------------*/
public boolean isClearMaskSet () {return clearMask;}

/*------------------------------------------------------------------*/
public boolean isSaveTransformationSet () {return saveTransformation;}

/*------------------------------------------------------------------*/
public boolean isStopRegistrationSet () {return stopRegistration;}

/*------------------------------------------------------------------*/
public Insets getInsets (
) {
   return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*------------------------------------------------------------------*/
public void joinThreads (
) {
   try {
      source.getThread().join();
      target.getThread().join();
   } catch (InterruptedException e) {
      IJ.error("Unexpected interruption exception" + e);
   }
} /* end joinSourceThread */

/*------------------------------------------------------------------*/
public void restoreAll (
) {
   cancelSource();
   cancelTarget();
   tb.restorePreviousToolbar();
   Toolbar.getInstance().repaint();
   unwarpJProgressBar.resetProgressBar();
   Runtime.getRuntime().gc();
} /* end restoreAll */

/*------------------------------------------------------------------*/
public void setClearMask (boolean val) {clearMask=val;}

/*------------------------------------------------------------------*/
public void setStopRegistration () {stopRegistration=true;}

/*------------------------------------------------------------------*/
public unwarpJDialog (
   final Frame parentWindow,
   final ImagePlus[] imageList
) {
   super(parentWindow, "UnwarpJ", false);
   this.imageList = imageList;

   // Start concurrent image processing threads
   createSourceImage();
   createTargetImage();
   setSecondaryPointHandlers();

    // Create Source panel
   setLayout(new GridLayout(0, 1));
   final Choice sourceChoice = new Choice();
   final Choice targetChoice = new Choice();
   final Panel sourcePanel = new Panel();
   sourcePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label sourceLabel = new Label("Source: ");
   addImageList(sourceChoice);
   sourceChoice.select(sourceChoiceIndex);
   sourceChoice.addItemListener(
      new ItemListener (
      ) {
         public void itemStateChanged (
            final ItemEvent ie
         ) {
            final int newChoiceIndex = sourceChoice.getSelectedIndex();
            if (sourceChoiceIndex != newChoiceIndex) {
               stopSourceThread();
               if (targetChoiceIndex != newChoiceIndex) {
                  sourceChoiceIndex = newChoiceIndex;
                  cancelSource();
                  targetPh.removePoints();
                  createSourceImage();
                  setSecondaryPointHandlers();
               }
               else {
                   stopTargetThread();
                  targetChoiceIndex = sourceChoiceIndex;
                  sourceChoiceIndex = newChoiceIndex;
                  targetChoice.select(targetChoiceIndex);
                  permuteImages();
               }
            }
            repaint();
         }
      }
   );
   sourcePanel.add(sourceLabel);
   sourcePanel.add(sourceChoice);

   // Create target panel
   final Panel targetPanel = new Panel();
   targetPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label targetLabel = new Label("Target: ");
   addImageList(targetChoice);
   targetChoice.select(targetChoiceIndex);
   targetChoice.addItemListener(
      new ItemListener (
      ) {
         public void itemStateChanged (
            final ItemEvent ie
         ) {
            final int newChoiceIndex = targetChoice.getSelectedIndex();
            if (targetChoiceIndex != newChoiceIndex) {
                stopTargetThread();
               if (sourceChoiceIndex != newChoiceIndex) {
                  targetChoiceIndex = newChoiceIndex;
                  cancelTarget();
                  sourcePh.removePoints();
                  createTargetImage();
                  setSecondaryPointHandlers();
               }
               else {
                  stopSourceThread();
                  sourceChoiceIndex = targetChoiceIndex;
                  targetChoiceIndex = newChoiceIndex;
                  sourceChoice.select(sourceChoiceIndex);
                  permuteImages();
               }
            }
            repaint();
         }
      }
   );
   targetPanel.add(targetLabel);
   targetPanel.add(targetChoice);

    // Create mode panel
   setLayout(new GridLayout(0, 1));
   final Choice modeChoice = new Choice();
   final Panel modePanel = new Panel();
   modePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Label modeLabel = new Label("Registration Mode: ");
   modeChoice.add("Fast");
   modeChoice.add("Accurate");
   modeChoice.select(mode);
   modeChoice.addItemListener(
      new ItemListener (
      ) {
         public void itemStateChanged (
            final ItemEvent ie
         ) {
            final int mode = modeChoice.getSelectedIndex();
            if (mode==0) {
               // Fast
               min_scale_image=1;
            } else if (mode==1) {
               // Accurate
               min_scale_image=0;
            }
                repaint();
         }
      }
   );
   modePanel.add(modeLabel);
   modePanel.add(modeChoice);

   // Build Advanced Options panel
   final Panel advancedOptionsPanel = new Panel();
   advancedOptionsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   final Button advancedOptionsButton = new Button("Advanced Options");
   advancedOptionsButton.addActionListener(this);
   advancedOptionsPanel.add(advancedOptionsButton);

   // Build Done Cancel Credits panel
   final Panel buttonPanel = new Panel();
   buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
   DoneButton.addActionListener(this);
   final Button cancelButton = new Button("Cancel");
   cancelButton.addActionListener(this);
   final Button creditsButton = new Button("Credits...");
   creditsButton.addActionListener(this);
   buttonPanel.add(cancelButton);
   buttonPanel.add(DoneButton);
   buttonPanel.add(creditsButton);

   // Build separations
   final Label separation1 = new Label("");
   final Label separation2 = new Label("");

   // Finally build dialog
   add(separation1);
   add(sourcePanel);
   add(targetPanel);
   add(modePanel);
   add(advancedOptionsPanel);
   add(separation2);
   add(buttonPanel);
   pack();

   createAdvancedOptions();
} /* end unwarpJDialog */

/*------------------------------------------------------------------*/
public void ungrayImage(final unwarpJPointAction pa) {
   if (pa==sourcePh.getPointAction()) {
      int Xdim=source.getWidth();
      int Ydim=source.getHeight();
      FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
      int ij=0;
      double []source_data=source.getImage();
      for (int i=0; i<Ydim; i++)
         for (int j=0; j<Xdim; j++,ij++)
             fp.putPixelValue(j,i,source_data[ij]);
      fp.resetMinAndMax();
      sourceImp.setProcessor(sourceImp.getTitle(),fp);
      sourceImp.updateImage();
   } else {
      int Xdim=target.getWidth();
      int Ydim=target.getHeight();
      FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
      double []target_data=target.getImage();
      int ij=0;
      for (int i=0; i<Ydim; i++)
         for (int j=0; j<Xdim; j++,ij++)
             fp.putPixelValue(j,i,target_data[ij]);
      fp.resetMinAndMax();
      targetImp.setProcessor(targetImp.getTitle(),fp);
      targetImp.updateImage();
   }
}

/*....................................................................
   Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void addImageList (
   final Choice choice
) {
   for (int k = 0; (k < imageList.length); k++)
      choice.add(imageList[k].getTitle());
} /* end addImageList */

/*------------------------------------------------------------------*/
private void cancelSource (
) {
   sourcePh.killListeners();
   sourcePh  = null;
   sourceIc  = null;
   sourceImp.killRoi();
   sourceImp = null;
   source    = null;
   sourceMsk = null;
   Runtime.getRuntime().gc();
} /* end cancelSource */

/*------------------------------------------------------------------*/
private void cancelTarget (
) {
   targetPh.killListeners();
   targetPh  = null;
   targetIc  = null;
   targetImp.killRoi();
   targetImp = null;
   target    = null;
   targetMsk = null;
   Runtime.getRuntime().gc();
} /* end cancelTarget */

/*------------------------------------------------------------------*/
private void computeImagePyramidDepth (
) {
   imagePyramidDepth=max_scale_deformation-min_scale_deformation+1;
}

/*------------------------------------------------------------------*/
private void createSourceImage (
) {
   sourceImp = imageList[sourceChoiceIndex];
   sourceImp.setSlice(1);
   source    =
      new unwarpJImageModel(sourceImp.getProcessor(), false);
   source.setPyramidDepth(imagePyramidDepth+min_scale_image);
   source.getThread().start();
   sourceIc  = sourceImp.getWindow().getCanvas();
   if (sourceImp.getStackSize()==1) { 
      // Create an empy mask
      sourceMsk = new unwarpJMask(sourceImp.getProcessor(),false);
   } else {
      // Take the mask from the second slice
      sourceImp.setSlice(2);
      sourceMsk = new unwarpJMask(sourceImp.getProcessor(),true);
      sourceImp.setSlice(1);
   }
   sourcePh  = new unwarpJPointHandler(sourceImp, tb, sourceMsk, this);
   tb.setSource(sourceImp, sourcePh);
} /* end createSourceImage */

/*------------------------------------------------------------------*/
private void createTargetImage (
) {
   targetImp = imageList[targetChoiceIndex];
   targetImp.setSlice(1);
   target    =
      new unwarpJImageModel(targetImp.getProcessor(), true);
   target.setPyramidDepth(imagePyramidDepth+min_scale_image);
   target.getThread().start();
   targetIc  = targetImp.getWindow().getCanvas();
   if (targetImp.getStackSize()==1) { 
      // Create an empy mask
      targetMsk = new unwarpJMask(targetImp.getProcessor(),false);
   } else {
      // Take the mask from the second slice
      targetImp.setSlice(2);
      targetMsk = new unwarpJMask(targetImp.getProcessor(),true);
      targetImp.setSlice(1);
   }
   targetPh  = new unwarpJPointHandler(targetImp, tb, targetMsk, this);
   tb.setTarget(targetImp, targetPh);
} /* end createTargetImage */

/*------------------------------------------------------------------*/
private void permuteImages (
) {
   // Swap image canvas
   final ImageCanvas swapIc = sourceIc;
   sourceIc = targetIc;
   targetIc = swapIc;

   // Swap ImagePlus
   final ImagePlus swapImp = sourceImp;
   sourceImp = targetImp;
   targetImp = swapImp;

   // Swap Mask
   final unwarpJMask swapMsk = sourceMsk;
   sourceMsk=targetMsk;
   targetMsk=swapMsk;

   // Swap Point Handlers
   final unwarpJPointHandler swapPh = sourcePh;
   sourcePh = targetPh;
   targetPh = swapPh;
   setSecondaryPointHandlers();

   // Inform the Toolbar about the change
   tb.setSource(sourceImp, sourcePh);
   tb.setTarget(targetImp, targetPh);

   // Restart the computation with each image
   source    =
      new unwarpJImageModel(sourceImp.getProcessor(), false);
   source.setPyramidDepth(imagePyramidDepth+min_scale_image);
   source.getThread().start();

   target =
      new unwarpJImageModel(targetImp.getProcessor(), true);
   target.setPyramidDepth(imagePyramidDepth+min_scale_image);
   target.getThread().start();
} /* end permuteImages */

/*------------------------------------------------------------------*/
private void removePoints (
) {
   sourcePh.removePoints();
   targetPh.removePoints();
}

/*------------------------------------------------------------------*/
private void restartModelThreads (
) {
   // Stop threads
   stopSourceThread();
   stopTargetThread();

   // Remove the current image models
   source = null;
   target = null;
   Runtime.getRuntime().gc();

   // Now restart the threads
   source    =
      new unwarpJImageModel(sourceImp.getProcessor(), false);
   source.setPyramidDepth(imagePyramidDepth+min_scale_image);
   source.getThread().start();

   target =
      new unwarpJImageModel(targetImp.getProcessor(), true);
   target.setPyramidDepth(imagePyramidDepth+min_scale_image);
   target.getThread().start();
}

/*------------------------------------------------------------------*/
private void setSecondaryPointHandlers (
) {
   sourcePh.setSecondaryPointHandler(targetImp, targetPh);
   targetPh.setSecondaryPointHandler(sourceImp, sourcePh);
} /* end setSecondaryPointHandler */

/*------------------------------------------------------------------*/
private void stopSourceThread (
) {
   // Stop the source image model
   while (source.getThread().isAlive()) {
      source.getThread().interrupt();
   }
   source.getThread().interrupted();
} /* end stopSourceThread */

/*------------------------------------------------------------------*/
private void stopTargetThread (
) {
   // Stop the target image model
   while (target.getThread().isAlive()) {
      target.getThread().interrupt();
   }
   target.getThread().interrupted();
} /* end stopTargetThread */

} /* end class unwarpJDialog */

/*====================================================================
|   unwarpJFile
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJFile
   extends
      Dialog
   implements
      ActionListener

{ /* begin class unwarpJFile */

/*....................................................................
   Private variables
....................................................................*/

private final CheckboxGroup choice = new CheckboxGroup();
private ImagePlus sourceImp;
private ImagePlus targetImp;
private unwarpJPointHandler sourcePh;
private unwarpJPointHandler targetPh;
private unwarpJDialog       dialog;

/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void actionPerformed (
   final ActionEvent ae
) {
   this.setVisible(false);
   if (ae.getActionCommand().equals("Save Landmarks As...")) {
      savePoints();
   }
   else if (ae.getActionCommand().equals("Load Landmarks...")) {
      loadPoints();
   }
   else if (ae.getActionCommand().equals("Show Landmarks")) {
      showPoints();
   }
   else if (ae.getActionCommand().equals("Load Transformation")) {
       loadTransformation();
   }
   else if (ae.getActionCommand().equals("Cancel")) {
   }
} /* end actionPerformed */

/*------------------------------------------------------------------*/
public Insets getInsets (
) {
   return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*------------------------------------------------------------------*/
unwarpJFile (
   final Frame parentWindow,
   final ImagePlus sourceImp,
   final ImagePlus targetImp,
   final unwarpJPointHandler sourcePh,
   final unwarpJPointHandler targetPh,
   final unwarpJDialog       dialog
) {
   super(parentWindow, "I/O Menu", true);
   this.sourceImp = sourceImp;
   this.targetImp = targetImp;
   this.sourcePh = sourcePh;
   this.targetPh = targetPh;
   this.dialog   = dialog;
   setLayout(new GridLayout(0, 1));
   final Button saveAsButton = new Button("Save Landmarks As...");
   final Button loadButton = new Button("Load Landmarks...");
   final Button show_PointsButton = new Button("Show Landmarks");
   final Button loadTransfButton = new Button("Load Transformation");
   final Button cancelButton = new Button("Cancel");
   saveAsButton.addActionListener(this);
   loadButton.addActionListener(this);
   show_PointsButton.addActionListener(this);
   loadTransfButton.addActionListener(this);
   cancelButton.addActionListener(this);
   final Label separation1 = new Label("");
   final Label separation2 = new Label("");
   add(separation1);
   add(loadButton);
   add(saveAsButton);
   add(show_PointsButton);
   add(loadTransfButton);
   add(separation2);
   add(cancelButton);
   pack();
} /* end unwarpJFile */

/*....................................................................
   Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void loadPoints (
) {
   final Frame f = new Frame();
   final FileDialog fd = new FileDialog(f, "Load Points", FileDialog.LOAD);
   fd.setVisible(true);
   final String path = fd.getDirectory();
   final String filename = fd.getFile();
   if ((path == null) || (filename == null)) return;

   Stack sourceStack = new Stack();
   Stack targetStack = new Stack();
   unwarpJMiscTools.loadPoints(path+filename,sourceStack,targetStack);

   sourcePh.removePoints();
   targetPh.removePoints();
   while ((!sourceStack.empty()) && (!targetStack.empty())) {
      Point sourcePoint = (Point)sourceStack.pop();
      Point targetPoint = (Point)targetStack.pop();
      sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
      targetPh.addPoint(targetPoint.x, targetPoint.y);
   }
} /* end loadPoints */

/*------------------------------------------------------------------*/
private void loadTransformation (
) {
   final Frame f = new Frame();
   final FileDialog fd = new FileDialog(f, "Load Transformation", FileDialog.LOAD);
   fd.setVisible(true);
   final String path = fd.getDirectory();
   final String filename = fd.getFile();
   if ((path == null) || (filename == null)) {
      return;
   }
   String fn_tnf=path+filename;

   int intervals=unwarpJMiscTools.
      numberOfIntervalsOfTransformation(fn_tnf);
   double [][]cx=new double[intervals+3][intervals+3];
   double [][]cy=new double[intervals+3][intervals+3];
   unwarpJMiscTools.loadTransformation(fn_tnf, cx, cy);

   // Apply transformation
   dialog.applyTransformationToSource(intervals,cx,cy);
}

/*------------------------------------------------------------------*/
private void savePoints (
) {
   final Frame f = new Frame();
   final FileDialog fd = new FileDialog(f, "Save Points", FileDialog.SAVE);
   String filename = targetImp.getTitle();
   int dot = filename.lastIndexOf('.');
   if (dot == -1) {
      fd.setFile(filename + ".txt");
   }
   else {
      filename = filename.substring(0, dot);
      fd.setFile(filename + ".txt");
   }
   fd.setVisible(true);
   final String path = fd.getDirectory();
   filename = fd.getFile();
   if ((path == null) || (filename == null)) {
      return;
   }
   try {
      final FileWriter fw = new FileWriter(path + filename);
      final Vector sourceList = sourcePh.getPoints();
      final Vector targetList = targetPh.getPoints();
      Point sourcePoint;
      Point targetPoint;
      String n;
      String xSource;
      String ySource;
      String xTarget;
      String yTarget;
      fw.write("Index\txSource\tySource\txTarget\tyTarget\n");
      for (int k = 0; (k < sourceList.size()); k++) {
         n = "" + k;
         while (n.length() < 5) {
            n = " " + n;
         }
         sourcePoint = (Point)sourceList.elementAt(k);
         xSource = "" + sourcePoint.x;
         while (xSource.length() < 7) {
            xSource = " " + xSource;
         }
         ySource = "" + sourcePoint.y;
         while (ySource.length() < 7) {
            ySource = " " + ySource;
         }
         targetPoint = (Point)targetList.elementAt(k);
         xTarget = "" + targetPoint.x;
         while (xTarget.length() < 7) {
            xTarget = " " + xTarget;
         }
         yTarget = "" + targetPoint.y;
         while (yTarget.length() < 7) {
            yTarget = " " + yTarget;
         }
         fw.write(n + "\t" + xSource + "\t" + ySource + "\t" + xTarget + "\t" + yTarget + "\n");
      }
      fw.close();
   } catch (IOException e) {
      IJ.error("IOException exception" + e);
   } catch (SecurityException e) {
      IJ.error("Security exception" + e);
   }
} /* end savePoints */

/*------------------------------------------------------------------*/
private void showPoints (
) {
   final Vector sourceList = sourcePh.getPoints();
   final Vector targetList = targetPh.getPoints();
   Point sourcePoint;
   Point targetPoint;
   String n;
   String xTarget;
   String yTarget;
   String xSource;
   String ySource;
   IJ.getTextPanel().setFont(new Font("Monospaced", Font.PLAIN, 12));
   IJ.setColumnHeadings("Index\txSource\tySource\txTarget\tyTarget");
   for (int k = 0; (k < sourceList.size()); k++) {
      n = "" + k;
      while (n.length() < 5) {
         n = " " + n;
      }
      sourcePoint = (Point)sourceList.elementAt(k);
      xTarget = "" + sourcePoint.x;
      while (xTarget.length() < 7) {
         xTarget = " " + xTarget;
      }
      yTarget = "" + sourcePoint.y;
      while (yTarget.length() < 7) {
         yTarget = " " + yTarget;
      }
      targetPoint = (Point)targetList.elementAt(k);
      xSource = "" + targetPoint.x;
      while (xSource.length() < 7) {
         xSource = " " + xSource;
      }
      ySource = "" + targetPoint.y;
      while (ySource.length() < 7) {
         ySource = " " + ySource;
      }
      IJ.write(n + "\t" + xSource + "\t" + ySource + "\t" + xTarget + "\t" + yTarget);
   }
} /* end showPoints */

} /* end class unwarpJFile */


/*====================================================================
|   unwarpJFinalAction
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJFinalAction
   implements
      Runnable
{
/*....................................................................
   Private variables
....................................................................*/
private Thread t;
private unwarpJDialog dialog;

// Images
private ImagePlus                      sourceImp;
private ImagePlus                      targetImp;
private unwarpJImageModel   source;
private unwarpJImageModel   target;

// Landmarks
private unwarpJPointHandler sourcePh;
private unwarpJPointHandler targetPh;

// Masks for the images
private unwarpJMask sourceMsk;
private unwarpJMask targetMsk;

// Transformation parameters
private int     min_scale_deformation;
private int     max_scale_deformation;
private int     min_scale_image;
private int     outputLevel;
private boolean showMarquardtOptim;
private double  divWeight;
private double  curlWeight;
private double  landmarkWeight;
private double  imageWeight;
private double  stopThreshold;
private int     accurate_mode;

/*....................................................................
   Public methods
....................................................................*/
/*********************************************************************
 * Return the thread associated with this <code>unwarpJFinalAction</code>
 * object.
 ********************************************************************/
public Thread getThread (
) {
   return(t);
} /* end getThread */

/*********************************************************************
 * Perform the registration
 ********************************************************************/
public void run (
) {
    // Create output image
    int Ydimt=target.getHeight();
    int Xdimt=target.getWidth();
    int Xdims=source.getWidth();
    final FloatProcessor fp=new FloatProcessor(Xdimt,Ydimt);
    for (int i=0; i<Ydimt; i++)
       for (int j=0; j<Xdimt; j++)
           if (sourceMsk.getValue(j,i) && targetMsk.getValue(j,i))
              fp.putPixelValue(j,i,(target.getImage())[i*Xdimt+j]-
                                   (source.getImage())[i*Xdims+j]);
           else fp.putPixelValue(j,i,0);
    fp.resetMinAndMax();
    final ImagePlus      ip=new ImagePlus("Output", fp);
    final ImageWindow    iw=new ImageWindow(ip);
    ip.updateImage();

    // Perform the registration
    final unwarpJTransformation warp = new unwarpJTransformation(
      sourceImp, targetImp, source, target, sourcePh, targetPh,
      sourceMsk, targetMsk, min_scale_deformation, max_scale_deformation,
      min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
      stopThreshold, outputLevel, showMarquardtOptim, accurate_mode,
      dialog.isSaveTransformationSet(), "", ip, dialog);
    warp.doRegistration();
    dialog.ungrayImage(sourcePh.getPointAction());
    dialog.ungrayImage(targetPh.getPointAction());
    dialog.restoreAll();
    dialog.freeMemory();
}

/*********************************************************************
 * Pass parameter from <code>unwarpJDialog</code> to
 * <code>unwarpJFinalAction</code>.
 ********************************************************************/
public void setup (
   final ImagePlus sourceImp,
   final ImagePlus targetImp,
   final unwarpJImageModel source,
   final unwarpJImageModel target,
   final unwarpJPointHandler sourcePh,
   final unwarpJPointHandler targetPh,
   final unwarpJMask sourceMsk,
   final unwarpJMask targetMsk,
   final int min_scale_deformation,
   final int max_scale_deformation,
   final int min_scale_image,
   final double divWeight,
   final double curlWeight,
   final double landmarkWeight,
   final double imageWeight,
   final double stopThreshold,
   final int outputLevel,
   final boolean showMarquardtOptim,
   final int accurate_mode
) {
   this.sourceImp             = sourceImp;
   this.targetImp             = targetImp;
   this.source                = source;
   this.target                = target;
   this.sourcePh              = sourcePh;
   this.targetPh              = targetPh;
   this.sourceMsk             = sourceMsk;
   this.targetMsk             = targetMsk;
   this.min_scale_deformation = min_scale_deformation;
   this.max_scale_deformation = max_scale_deformation;
   this.min_scale_image       = min_scale_image;
   this.divWeight             = divWeight;
   this.curlWeight            = curlWeight;
   this.landmarkWeight        = landmarkWeight;
   this.imageWeight           = imageWeight;
   this.stopThreshold         = stopThreshold;
   this.outputLevel           = outputLevel;
   this.showMarquardtOptim    = showMarquardtOptim;
   this.accurate_mode         = accurate_mode;
} /* end setup */

/*********************************************************************
 * Start a thread under the control of the main event loop. This thread
 * has access to the progress bar, while methods called directly from
 * within <code>unwarpJDialog</code> do not because they are
 * under the control of its own event loop.
 ********************************************************************/
public unwarpJFinalAction (
   final unwarpJDialog dialog
) {
   this.dialog = dialog;
   t = new Thread(this);
   t.setDaemon(true);
}

}

/*====================================================================
|   unwarpJImageModel
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJImageModel
   implements
      Runnable

{ /* begin class unwarpJImageModel */
// Some constants
private static int min_image_size=4;


/*....................................................................
   Private variables
....................................................................*/
// Thread
private Thread t;

// Stack for the pyramid of images/coefficients
private final Stack cpyramid   = new Stack();
private final Stack imgpyramid = new Stack();

// Original image, image spline coefficients, and gradient
private double[] image;
private double[] coefficient;

// Current image (the size might be different from the original)
private double[] currentImage;
private double[] currentCoefficient;
private int      currentWidth;
private int      currentHeight;
private int      twiceCurrentWidth;
private int      twiceCurrentHeight;

// Size and other information
private int     width;
private int     height;
private int     twiceWidth;
private int     twiceHeight;
private int     pyramidDepth;
private int     currentDepth;
private int     smallestWidth;
private int     smallestHeight;
private boolean isTarget;
private boolean coefficientsAreMirrored;

// Some variables to speedup interpolation
// All these information is set through prepareForInterpolation()
private double   x;           // Point to interpolate
private double   y;   
public  int      xIndex[];    // Indexes related
public  int      yIndex[];
private double   xWeight[];   // Weights of the splines related
private double   yWeight[];
private double   dxWeight[];  // Weights of the derivatives splines related
private double   dyWeight[];
private double   d2xWeight[]; // Weights of the second derivatives splines related
private double   d2yWeight[];
private boolean  fromCurrent; // Interpolation source (current or original)
private int      widthToUse;  // Size of the image used for the interpolation
private int      heightToUse;

// Some variables to speedup interpolation (precomputed)
// All these information is set through prepareForInterpolation()
public  int      prec_xIndex[][];    // Indexes related
public  int      prec_yIndex[][];
private double   prec_xWeight[][];   // Weights of the splines related
private double   prec_yWeight[][];
private double   prec_dxWeight[][];  // Weights of the derivatives splines related
private double   prec_dyWeight[][];
private double   prec_d2xWeight[][]; // Weights of the second derivatives splines related
private double   prec_d2yWeight[][];

/*....................................................................
   Public methods
....................................................................*/

/* Clear the pyramid. */
public void clearPyramids (
) {
   cpyramid.removeAllElements();
   imgpyramid.removeAllElements();
} /* end clearPyramid */

/*------------------------------------------------------------------*/
/* Return the full-size B-spline coefficients. */
public double[] getCoefficient () {return coefficient;}

/*------------------------------------------------------------------*/
/* Return the current height of the image/coefficients. */
public int getCurrentHeight() {return currentHeight;}

/*------------------------------------------------------------------*/
/* Return the current image of the image/coefficients. */
public double[] getCurrentImage() {return currentImage;}

/*------------------------------------------------------------------*/
/* Return the current width of the image/coefficients. */
public int getCurrentWidth () {return currentWidth;}

/*------------------------------------------------------------------*/
/* Return the relationship between the current size of the image
   and the original size. */
public double getFactorHeight () {return (double)currentHeight/height;}

/*------------------------------------------------------------------*/
/* Return the relationship between the current size of the image
   and the original size. */
public double getFactorWidth () {return (double)currentWidth/width;}

/*------------------------------------------------------------------*/
/* Return the current depth of the image/coefficients. */
public int getCurrentDepth() {return currentDepth;}

/*------------------------------------------------------------------*/
/* Return the full-size image height. */
public int getHeight () {return(height);}

/*------------------------------------------------------------------*/
/* Return the full-size image. */
public double[] getImage () {return image;}

/*------------------------------------------------------------------*/
public double getPixelValFromPyramid(
   int x,   // Pixel location
   int y) {
   return currentImage[y*currentWidth+x];
}

/*------------------------------------------------------------------*/
/* Return the depth of the image pyramid. A depth 1 means
   that one coarse resolution level is present in the stack. The
   full-size level is not placed on the stack. */
public int getPyramidDepth () {return(pyramidDepth);}

/*------------------------------------------------------------------*/
/* Return the height of the smallest image in the pyramid. */
public int getSmallestHeight () {return(smallestHeight);}

/*------------------------------------------------------------------*/
/* Return the width of the smallest image in the pyramid. */
public int getSmallestWidth () {return(smallestWidth);}

/*------------------------------------------------------------------*/
/* Return the thread associated. */
public Thread getThread () {return(t);}

/*------------------------------------------------------------------*/
/* Return the full-size image width. */
public int getWidth () {return(width);}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double getWeightDx(int l, int m) {return yWeight[l]*dxWeight[m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double getWeightDxDx(int l, int m) {return yWeight[l]*d2xWeight[m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double getWeightDxDy(int l, int m) {return dyWeight[l]*dxWeight[m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double getWeightDy(int l, int m) {return dyWeight[l]*xWeight[m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double getWeightDyDy(int l, int m) {return d2yWeight[l]*xWeight[m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double getWeightI(int l, int m) {return yWeight[l]*xWeight[m];}

/*------------------------------------------------------------------*/
/*
   There are two types of interpolation routines. Those that use
   precomputed weights and those that don't.

   An example of use of the ones without precomputation is the
   following:
       // Set of B-spline coefficients
       double [][]c;

      // Set these coefficients to an interpolator
      unwarpJImageModel sw = new unwarpJImageModel(c);

       // Compute the transformation mapping
      for (int v=0; v<ImageHeight; v++) {
         final double tv = (double)(v * intervals) / (double)(ImageHeight - 1) + 1.0F;
         for (int u = 0; u<ImageeWidth; u++) {
            final double tu = (double)(u * intervals) / (double)(ImageWidth - 1) + 1.0F;
            sw.prepareForInterpolation(tu, tv, ORIGINAL);
            interpolated_val[v][u] = sw.interpolateI();

*/
/*------------------------------------------------------------------*/
/*------------------------------------------------------------------*/
/* Interpolate the X and Y derivatives of the image at a
   given point. */
public void interpolateD(double []D) {
   // Only SplineDegree=3 is implemented
   D[0]=D[1]=0.0F;
   for (int j = 0; j<4; j++) {
       double sx=0.0F, sy=0.0F;
       int iy=yIndex[j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=xIndex[i];
             if (ix!=-1) {
                double c;
                if (fromCurrent) c=currentCoefficient[p + ix];
                else             c=coefficient[p + ix];
                sx += dxWeight[i]*c;
                sy +=  xWeight[i]*c;
             }
          }
          D[0]+= yWeight[j] * sx;
          D[1]+=dyWeight[j] * sy;
       }
   }
} /* end Interpolate D */

/*------------------------------------------------------------------*/
/* Interpolate the XY, XX and YY derivatives of the image at a
   given point. */
public void interpolateD2 (double []D2) {
   // Only SplineDegree=3 is implemented
   D2[0]=D2[1]=D2[2]=0.0F;
   for (int j = 0; j<4; j++) {
       double sxy=0.0F, sxx=0.0F, syy=0.0F;
       int iy=yIndex[j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=xIndex[i];
             if (ix!=-1) {
                double c;
                if (fromCurrent) c=currentCoefficient[p + ix];
                else             c=coefficient[p + ix];
                 sxy +=  dxWeight[i]*c;
                 sxx += d2xWeight[i]*c;
                 syy +=   xWeight[i]*c;
             }
          }
          D2[0]+= dyWeight[j] * sxy;
          D2[1]+=  yWeight[j] * sxx;
          D2[2]+=d2yWeight[j] * syy;
       }
   }
} /* end Interpolate dxdy, dxdx and dydy */

/*------------------------------------------------------------------*/
/* Interpolate the X derivative of the image at a given point. */
public double interpolateDx () {
   // Only SplineDegree=3 is implemented
   double ival=0.0F;
   for (int j = 0; j<4; j++) {
       double s=0.0F;
       int iy=yIndex[j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=xIndex[i];
             if (ix!=-1)
                if (fromCurrent) s += dxWeight[i]*currentCoefficient[p + ix];
                else             s += dxWeight[i]*coefficient[p + ix];
          }
          ival+=yWeight[j] * s;
       }
   }
   return ival;
} /* end Interpolate Dx */

/*------------------------------------------------------------------*/
/* Interpolate the X derivative of the image at a given point. */
public double interpolateDxDx () {
   // Only SplineDegree=3 is implemented
   double ival=0.0F;
   for (int j = 0; j<4; j++) {
       double s=0.0F;
       int iy=yIndex[j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=xIndex[i];
             if (ix!=-1)
                if (fromCurrent) s += d2xWeight[i]*currentCoefficient[p + ix];
                else             s += d2xWeight[i]*coefficient[p + ix];
          }
          ival+=yWeight[j] * s;
       }
   }
   return ival;
} /* end Interpolate DxDx */

/*------------------------------------------------------------------*/
/* Interpolate the X derivative of the image at a given point. */
public double interpolateDxDy () {
   // Only SplineDegree=3 is implemented
   double ival=0.0F;
   for (int j = 0; j<4; j++) {
       double s=0.0F;
       int iy=yIndex[j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=xIndex[i];
             if (ix!=-1)
                if (fromCurrent) s += dxWeight[i]*currentCoefficient[p + ix];
                else             s += dxWeight[i]*coefficient[p + ix];
          }
          ival+=dyWeight[j] * s;
       }
   }
   return ival;
} /* end Interpolate DxDy */

/*------------------------------------------------------------------*/
/* Interpolate the Y derivative of the image at a given point. */
public double interpolateDy () {
   // Only SplineDegree=3 is implemented
   double ival=0.0F;
   for (int j = 0; j<4; j++) {
       double s=0.0F;
       int iy=yIndex[j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=xIndex[i];
             if (ix!=-1)
                if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
                else             s += xWeight[i]*coefficient[p + ix];
          }
          ival+=dyWeight[j] * s;
       }
   }
   return ival;
} /* end Interpolate Dy */

/*------------------------------------------------------------------*/
/* Interpolate the X derivative of the image at a given point. */
public double interpolateDyDy () {
   // Only SplineDegree=3 is implemented
   double ival=0.0F;
   for (int j = 0; j<4; j++) {
       double s=0.0F;
       int iy=yIndex[j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=xIndex[i];
             if (ix!=-1)
                if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
                else             s += xWeight[i]*coefficient[p + ix];
          }
          ival+=d2yWeight[j] * s;
       }
   }
   return ival;
} /* end Interpolate DyDy */

/*------------------------------------------------------------------*/
/* Interpolate the image at a given point. */
public double interpolateI () {
   // Only SplineDegree=3 is implemented
   double ival=0.0F;
   for (int j = 0; j<4; j++) {
       double s=0.0F;
       int iy=yIndex[j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=xIndex[i];
             if (ix!=-1) 
                if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
                else             s += xWeight[i]*coefficient[p + ix];
          }
          ival+=yWeight[j] * s;
       }
   }
   return ival;
} /* end Interpolate Image */

/*------------------------------------------------------------------*/
public boolean isFinest() {return cpyramid.isEmpty();}

/*------------------------------------------------------------------*/
public void popFromPyramid(
){
   // Pop coefficients
   if (cpyramid.isEmpty()) {
      currentWidth       = width;
      currentHeight      = height;
      currentCoefficient = coefficient;
   } else {
      currentWidth       = ((Integer)cpyramid.pop()).intValue();
      currentHeight      = ((Integer)cpyramid.pop()).intValue();
      currentCoefficient = (double [])cpyramid.pop();
   }
   twiceCurrentWidth     = 2*currentWidth;
   twiceCurrentHeight    = 2*currentHeight;
   if (currentDepth>0) currentDepth--;

   // Pop image
   if (isTarget && !imgpyramid.isEmpty()) {
      if (currentWidth != ((Integer)imgpyramid.pop()).intValue())
         System.out.println("I cannot understand");
      if (currentHeight != ((Integer)imgpyramid.pop()).intValue())
         System.out.println("I cannot understand");
      currentImage = (double [])imgpyramid.pop();
   } else currentImage = image;
}

/*------------------------------------------------------------------*/
/* fromCurrent=true  --> The interpolation is prepared to be done
                         from the current image in the pyramid.
   fromCurrent=false --> The interpolation is prepared to be done
                         from the original image. */
public void prepareForInterpolation(
   double x, 
   double y,
   boolean fromCurrent
) {
    // Remind this point for interpolation
    this.x=x;
    this.y=y;
    this.fromCurrent=fromCurrent;

    if (fromCurrent) {widthToUse=currentWidth; heightToUse=currentHeight;}
    else             {widthToUse=width;        heightToUse=height;}

    int ix=(int)x;
    int iy=(int)y;

    int twiceWidthToUse =2*widthToUse;
    int twiceHeightToUse=2*heightToUse;

    // Set X indexes
    // p is the index of the rightmost influencing spline
    int p = (0.0 <= x) ? (ix + 2) : (ix + 1);
    for (int k = 0; k<4; p--, k++) {
       if (coefficientsAreMirrored) {
         int q = (p < 0) ? (-1 - p) : (p);
         if (twiceWidthToUse <= q) q -= twiceWidthToUse * (q / twiceWidthToUse);
         xIndex[k] = (widthToUse <= q) ? (twiceWidthToUse - 1 - q) : (q);
      } else
          xIndex[k] = (p<0 || p>=widthToUse) ? (-1):(p);
    }

    // Set Y indexes
    p = (0.0 <= y) ? (iy + 2) : (iy + 1);
    for (int k = 0; k<4; p--, k++) {
       if (coefficientsAreMirrored) {
         int q = (p < 0) ? (-1 - p) : (p);
         if (twiceHeightToUse <= q) q -= twiceHeightToUse * (q / twiceHeightToUse);
         yIndex[k] = (heightToUse <= q) ? (twiceHeightToUse - 1 - q) : (q);
      } else
          yIndex[k] = (p<0 || p>=heightToUse) ? (-1):(p);
    }

    // Compute how much the sample depart from an integer position
    double ex = x - ((0.0 <= x) ? (ix) : (ix - 1));
    double ey = y - ((0.0 <= y) ? (iy) : (iy - 1));

    // Set X weights for the image and derivative interpolation
    double s = 1.0F - ex;
    dxWeight[0] = 0.5F * ex * ex;
    xWeight[0]  = ex * dxWeight[0] / 3.0F; // Bspline03(x-ix-2)
    dxWeight[3] = -0.5F * s * s;
    xWeight[3]  = s * dxWeight[3] / -3.0F; // Bspline03(x-ix+1)
    dxWeight[1] = 1.0F - 2.0F * dxWeight[0] + dxWeight[3];
    //xWeight[1]  = 2.0F / 3.0F + (1.0F + ex) * dxWeight[3]; // Bspline03(x-ix-1);
    xWeight[1]  = unwarpJMathTools.Bspline03(x-ix-1);
    dxWeight[2] = 1.5F * ex * (ex - 4.0F/ 3.0F);
    xWeight[2]  = 2.0F / 3.0F - (2.0F - ex) * dxWeight[0]; // Bspline03(x-ix)

    d2xWeight[0] = ex;
    d2xWeight[1] = s-2*ex;
    d2xWeight[2] = ex-2*s;
    d2xWeight[3] = s;

   // Set Y weights for the image and derivative interpolation
   double t = 1.0F - ey;
   dyWeight[0] = 0.5F * ey * ey;
   yWeight[0]  = ey * dyWeight[0] / 3.0F;
   dyWeight[3] = -0.5F * t * t;
   yWeight[3]  = t * dyWeight[3] / -3.0F;
   dyWeight[1] = 1.0F - 2.0F * dyWeight[0] + dyWeight[3];
   yWeight[1]  = 2.0F / 3.0F + (1.0F + ey) * dyWeight[3];
   dyWeight[2] = 1.5F * ey * (ey - 4.0F/ 3.0F);
   yWeight[2]  = 2.0F / 3.0F - (2.0F - ey) * dyWeight[0];

   d2yWeight[0] = ey;
   d2yWeight[1] = t-2*ey;
   d2yWeight[2] = ey-2*t;
   d2yWeight[3] = t;
} /* prepareForInterpolation */

/*------------------------------------------------------------------*/
/* Return the width of the precomputed vectors */
public int precomputed_getWidth() {return prec_yWeight.length;}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double precomputed_getWeightDx(int l, int m, int u, int v)
   {return prec_yWeight[v][l]*prec_dxWeight[u][m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double precomputed_getWeightDxDx(int l, int m, int u, int v)
   {return prec_yWeight[v][l]*prec_d2xWeight[u][m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double precomputed_getWeightDxDy(int l, int m, int u, int v)
   {return prec_dyWeight[v][l]*prec_dxWeight[u][m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double precomputed_getWeightDy(int l, int m, int u, int v)
   {return prec_dyWeight[v][l]*prec_xWeight[u][m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double precomputed_getWeightDyDy(int l, int m, int u, int v)
   {return prec_d2yWeight[v][l]*prec_xWeight[u][m];}

/*------------------------------------------------------------------*/
/* Return the weight of the coefficient l,m (yIndex, xIndex) in the
   image interpolation */
public double precomputed_getWeightI(int l, int m, int u, int v)
   {return prec_yWeight[v][l]*prec_xWeight[u][m];}

/*------------------------------------------------------------------*/
/* Interpolate the X and Y derivatives of the image at a
   given point. */
public void precomputed_interpolateD(double []D, int u, int v) {
   // Only SplineDegree=3 is implemented
   D[0]=D[1]=0.0F;
   for (int j = 0; j<4; j++) {
       double sx=0.0F, sy=0.0F;
       int iy=prec_yIndex[v][j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=prec_xIndex[u][i];
             if (ix!=-1) {
                double c;
                if (fromCurrent) c=currentCoefficient[p + ix];
                else             c=coefficient[p + ix];
                sx += prec_dxWeight[u][i]*c;
                sy +=  prec_xWeight[u][i]*c;
             }
          }
          D[0]+= prec_yWeight[v][j] * sx;
          D[1]+=prec_dyWeight[v][j] * sy;
       }
   }
} /* end Interpolate D */

/*------------------------------------------------------------------*/
/* Interpolate the XY, XX and YY derivatives of the image at a
   given point. */
public void precomputed_interpolateD2 (double []D2, int u, int v) {
   // Only SplineDegree=3 is implemented
   D2[0]=D2[1]=D2[2]=0.0F;
   for (int j = 0; j<4; j++) {
       double sxy=0.0F, sxx=0.0F, syy=0.0F;
       int iy=prec_yIndex[v][j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=prec_xIndex[u][i];
             if (ix!=-1) {
                double c;
                if (fromCurrent) c=currentCoefficient[p + ix];
                else             c=coefficient[p + ix];
                 sxy +=  prec_dxWeight[u][i]*c;
                 sxx += prec_d2xWeight[u][i]*c;
                 syy +=   prec_xWeight[u][i]*c;
             }
          }
          D2[0]+= prec_dyWeight[v][j] * sxy;
          D2[1]+=  prec_yWeight[v][j] * sxx;
          D2[2]+=prec_d2yWeight[v][j] * syy;
       }
   }
} /* end Interpolate dxdy, dxdx and dydy */

/*------------------------------------------------------------------*/
/* Interpolate the image at a given point. */
public double precomputed_interpolateI (int u, int v) {
   // Only SplineDegree=3 is implemented
   double ival=0.0F;
   for (int j = 0; j<4; j++) {
       double s=0.0F;
       int iy=prec_yIndex[v][j];
       if (iy!=-1) {
          int p=iy*widthToUse;
          for (int i=0; i<4; i++) {
             int ix=prec_xIndex[u][i];
             if (ix!=-1) 
                if (fromCurrent) s += prec_xWeight[u][i]*currentCoefficient[p + ix];
                else             s += prec_xWeight[u][i]*coefficient[p + ix];
          }
          ival+=prec_yWeight[v][j] * s;
       }
   }
   return ival;
} /* end Interpolate Image */

/*------------------------------------------------------------------*/
/* Prepare precomputations for a given image size. */
public void precomputed_prepareForInterpolation(
   int Ydim, int Xdim, int intervals) {
   // Ask for memory
   prec_xIndex   =new int   [Xdim][4];
   prec_yIndex   =new int   [Ydim][4];
   prec_xWeight  =new double[Xdim][4];
   prec_yWeight  =new double[Ydim][4];
   prec_dxWeight =new double[Xdim][4];
   prec_dyWeight =new double[Ydim][4];
   prec_d2xWeight=new double[Xdim][4];
   prec_d2yWeight=new double[Ydim][4];

    boolean ORIGINAL = false;
    // Fill the precomputed weights and indexes for the Y axis
    for (int v=0; v<Ydim; v++) {
        // Express the current point in Spline units
       final double tv = (double)(v * intervals) / (double)(Ydim - 1) + 1.0F;
       final double tu = 1.0F;

        // Compute all weights and indexes
        prepareForInterpolation(tu,tv,ORIGINAL);

        // Copy all values
        for (int k=0; k<4; k++) {
         prec_yIndex   [v][k]=  yIndex [k];
         prec_yWeight  [v][k]=  yWeight[k];
         prec_dyWeight [v][k]= dyWeight[k];
         prec_d2yWeight[v][k]=d2yWeight[k];
       }
    }

    // Fill the precomputed weights and indexes for the X axis
    for (int u=0; u<Xdim; u++) {
        // Express the current point in Spline units
       final double tv = 1.0F;
      final double tu = (double)(u * intervals) / (double)(Xdim - 1) + 1.0F;

        // Compute all weights and indexes
        prepareForInterpolation(tu,tv,ORIGINAL);

        // Copy all values
        for (int k=0; k<4; k++) {
         prec_xIndex   [u][k]=  xIndex [k];
         prec_xWeight  [u][k]=  xWeight[k];
         prec_dxWeight [u][k]= dxWeight[k];
         prec_d2xWeight[u][k]=d2xWeight[k];
       }
    }
}

/*------------------------------------------------------------------*/
/* Start the image precomputations. The computation of the B-spline
   coefficients of the full-size image is not interruptible; all other
   methods are. */
public void run (
) {
    coefficient = getBasicFromCardinal2D();
    buildCoefficientPyramid();
    if (isTarget) buildImagePyramid();
} /* end run */

/*------------------------------------------------------------------*/
/* Set spline coefficients */
public void setCoefficients (
   final double []c,     // Set of B-spline coefficients
   final int Ydim,       // Dimensions of the set of coefficients
   final int Xdim,
   final int offset      // Offset of the beginning of the array
                         // with respect to the origin of c
) {
    // Copy the array of coefficients
    System.arraycopy(c, offset, coefficient, 0, Ydim*Xdim);
}

/*------------------------------------------------------------------*/
/* Sets the depth up to which the pyramids should be computed. */
public void setPyramidDepth (
   final int pyramidDepth
) {
   int proposedPyramidDepth=pyramidDepth;

   // Check what is the maximum depth allowed by the image
   int currentWidth=width;
   int currentHeight=height;
   int scale=0;
   while (currentWidth>=min_image_size && currentHeight>=min_image_size) {
      currentWidth/=2;
      currentHeight/=2;
      scale++;
   }
   scale--;

   if (proposedPyramidDepth>scale) proposedPyramidDepth=scale;

   this.pyramidDepth = proposedPyramidDepth;
} /* end setPyramidDepth */

/*------------------------------------------------------------------*/
/* Converts the pixel array of the incoming ImageProcessor
   object into a local double array. The flag is target enables the
   computation of the derivative or not. */
public unwarpJImageModel (
   final ImageProcessor ip,
   final boolean isTarget
) {
   // Initialize thread
   t = new Thread(this);
   t.setDaemon(true);

   // Get image information
   this.isTarget = isTarget;
   width         = ip.getWidth();
   height        = ip.getHeight();
   twiceWidth    = 2*width;
   twiceHeight   = 2*height;
   coefficientsAreMirrored = true;

   // Copy the pixel array
   int k = 0;
   image = new double[width * height];
   unwarpJMiscTools.extractImage(ip,image);

   // Resize the speedup arrays
   xIndex    = new int[4];
   yIndex    = new int[4];
   xWeight   = new double[4];
   yWeight   = new double[4];
   dxWeight  = new double[4];
   dyWeight  = new double[4];
   d2xWeight = new double[4];
   d2yWeight = new double[4];
} /* end unwarpJImage */

/* The same as before, but take the image from an array */
public unwarpJImageModel (
   final double [][]img,
   final boolean isTarget
) {
    // Initialize thread
   t = new Thread(this);
   t.setDaemon(true);

    // Get image information
   this.isTarget = isTarget;
   width         = img[0].length;
   height        = img.length;
   twiceWidth    = 2*width;
   twiceHeight   = 2*height;
   coefficientsAreMirrored = true;

   // Copy the pixel array
   int k = 0;
   image = new double[width * height];
   for (int y = 0; (y < height); y++)
      for (int x = 0; (x < width); x++, k++)
         image[k] = img[y][x];

   // Resize the speedup arrays
   xIndex    = new int[4];
   yIndex    = new int[4];
   xWeight   = new double[4];
   yWeight   = new double[4];
   dxWeight  = new double[4];
   dyWeight  = new double[4];
   d2xWeight = new double[4];
   d2yWeight = new double[4];
} /* end unwarpJImage */

/*------------------------------------------------------------------*/
/* Initialize the model from a set of coefficients */
public unwarpJImageModel (
   final double [][]c      // Set of B-spline coefficients
) {
    // Get the size of the input array
    currentHeight      = height      = c.length;
    currentWidth       = width       = c[0].length;
    twiceCurrentHeight = twiceHeight = 2*height;
    twiceCurrentWidth  = twiceWidth  = 2*width;
    coefficientsAreMirrored = false;

    // Copy the array of coefficients
    coefficient=new double[height*width];
    int k=0;
   for (int y=0; y<height; y++, k+= width)
      System.arraycopy(c[y], 0, coefficient, k, width);

   // Resize the speedup arrays
   xIndex    = new int[4];
   yIndex    = new int[4];
   xWeight   = new double[4];
   yWeight   = new double[4];
   dxWeight  = new double[4];
   dyWeight  = new double[4];
   d2xWeight = new double[4];
   d2yWeight = new double[4];
}

/*------------------------------------------------------------------*/
/* Initialize the model from a set of coefficients.
   The same as the previous function but now the coefficients
   are in a single row. */
public unwarpJImageModel (
   final double []c,     // Set of B-spline coefficients
   final int Ydim,       // Dimensions of the set of coefficients
   final int Xdim,
   final int offset      // Offset of the beginning of the array
                         // with respect to the origin of c
) {
    // Get the size of the input array
    currentHeight      = height      = Ydim;
    currentWidth       = width       = Xdim;
    twiceCurrentHeight = twiceHeight = 2*height;
    twiceCurrentWidth  = twiceWidth  = 2*width;
    coefficientsAreMirrored = false;

    // Copy the array of coefficients
    coefficient=new double[height*width];
    System.arraycopy(c, offset, coefficient, 0, height*width);

   // Resize the speedup arrays
   xIndex    = new int[4];
   yIndex    = new int[4];
   xWeight   = new double[4];
   yWeight   = new double[4];
   dxWeight  = new double[4];
   dyWeight  = new double[4];
   d2xWeight = new double[4];
   d2yWeight = new double[4];
}
/*....................................................................
   Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void antiSymmetricFirMirrorOffBounds1D (
   final double[] h,
   final double[] c,
   final double[] s
) {
   if (2 <= c.length) {
      s[0] = h[1] * (c[1] - c[0]);
      for (int i = 1; (i < (s.length - 1)); i++) {
         s[i] = h[1] * (c[i + 1] - c[i - 1]);
      }
      s[s.length - 1] = h[1] * (c[c.length - 1] - c[c.length - 2]);
   } else s[0] = 0.0;
} /* end antiSymmetricFirMirrorOffBounds1D */

/*------------------------------------------------------------------*/
private void basicToCardinal2D (
   final double[] basic,
   final double[] cardinal,
   final int width,
   final int height,
   final int degree
) {
   final double[] hLine = new double[width];
   final double[] vLine = new double[height];
   final double[] hData = new double[width];
   final double[] vData = new double[height];
   double[] h = null;
   switch (degree) {
      case 3:
         h = new double[2];
         h[0] = 2.0 / 3.0;
         h[1] = 1.0 / 6.0;
         break;
      case 7:
         h = new double[4];
         h[0] = 151.0 / 315.0;
         h[1] = 397.0 / 1680.0;
         h[2] = 1.0 / 42.0;
         h[3] = 1.0 / 5040.0;
         break;
      default:
         h = new double[1];
         h[0] = 1.0;
   }
   for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
      extractRow(basic, y, hLine);
      symmetricFirMirrorOffBounds1D(h, hLine, hData);
      putRow(cardinal, y, hData);
   }
   for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
      extractColumn(cardinal, width, x, vLine);
      symmetricFirMirrorOffBounds1D(h, vLine, vData);
      putColumn(cardinal, width, x, vData);
   }
} /* end basicToCardinal2D */

/*------------------------------------------------------------------*/
private void buildCoefficientPyramid (
) {
   int fullWidth;
   int fullHeight;
   double[] fullDual = new double[width * height];
   int halfWidth = width;
   int halfHeight = height;
   basicToCardinal2D(coefficient, fullDual, width, height, 7);
   for (int depth = 1; ((depth <= pyramidDepth) && (!t.isInterrupted())); depth++) {
      fullWidth = halfWidth;
      fullHeight = halfHeight;
      halfWidth /= 2;
      halfHeight /= 2;
      final double[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
      final double[] halfCoefficient = getBasicFromCardinal2D(halfDual, halfWidth, halfHeight, 7);
      cpyramid.push(halfCoefficient);
      cpyramid.push(new Integer(halfHeight));
      cpyramid.push(new Integer(halfWidth));
      fullDual = halfDual;
   }
    smallestWidth  = halfWidth;
    smallestHeight = halfHeight;
    currentDepth=pyramidDepth+1;
} /* end buildCoefficientPyramid */

/*------------------------------------------------------------------*/
private void buildImagePyramid (
) {
   int fullWidth;
   int fullHeight;
   double[] fullDual = new double[width * height];
   int halfWidth = width;
   int halfHeight = height;
   cardinalToDual2D(image, fullDual, width, height, 3);
   for (int depth = 1; ((depth <= pyramidDepth) && (!t.isInterrupted())); depth++) {
      fullWidth = halfWidth;
      fullHeight = halfHeight;
      halfWidth /= 2;
      halfHeight /= 2;
      final double[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
      final double[] halfImage = new double[halfWidth * halfHeight];
      dualToCardinal2D(halfDual, halfImage, halfWidth, halfHeight, 3);
      imgpyramid.push(halfImage);
      imgpyramid.push(new Integer(halfHeight));
      imgpyramid.push(new Integer(halfWidth));
      fullDual = halfDual;
   }
} /* end buildImagePyramid */

/*------------------------------------------------------------------*/
private void cardinalToDual2D (
   final double[] cardinal,
   final double[] dual,
   final int width,
   final int height,
   final int degree
) {
   basicToCardinal2D(getBasicFromCardinal2D(cardinal, width, height, degree),
      dual, width, height, 2 * degree + 1);
} /* end cardinalToDual2D */

/*------------------------------------------------------------------*/
private void coefficientToGradient1D (
   final double[] c
) {
   final double[] h = {0.0, 1.0 / 2.0};
   final double[] s = new double[c.length];
   antiSymmetricFirMirrorOffBounds1D(h, c, s);
   System.arraycopy(s, 0, c, 0, s.length);
} /* end coefficientToGradient1D */

/*------------------------------------------------------------------*/
private void coefficientToSamples1D (
   final double[] c
) {
   final double[] h = {2.0 / 3.0, 1.0 / 6.0};
   final double[] s = new double[c.length];
   symmetricFirMirrorOffBounds1D(h, c, s);
   System.arraycopy(s, 0, c, 0, s.length);
} /* end coefficientToSamples1D */

/*------------------------------------------------------------------*/
private void coefficientToXYGradient2D (
   final double[] basic,
   final double[] xGradient,
   final double[] yGradient,
   final int width,
   final int height
) {
   final double[] hLine = new double[width];
   final double[] hData = new double[width];
   final double[] vLine = new double[height];
   for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
      extractRow(basic, y, hLine);
      System.arraycopy(hLine, 0, hData, 0, width);
      coefficientToGradient1D(hLine);
      coefficientToSamples1D(hData);
      putRow(xGradient, y, hLine);
      putRow(yGradient, y, hData);
   }
   for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
      extractColumn(xGradient, width, x, vLine);
      coefficientToSamples1D(vLine);
      putColumn(xGradient, width, x, vLine);
      extractColumn(yGradient, width, x, vLine);
      coefficientToGradient1D(vLine);
      putColumn(yGradient, width, x, vLine);
   }
} /* end coefficientToXYGradient2D */

/*------------------------------------------------------------------*/
private void dualToCardinal2D (
   final double[] dual,
   final double[] cardinal,
   final int width,
   final int height,
   final int degree
) {
   basicToCardinal2D(getBasicFromCardinal2D(dual, width, height, 2 * degree + 1),
      cardinal, width, height, degree);
} /* end dualToCardinal2D */

/*------------------------------------------------------------------*/
private void extractColumn (
   final double[] array,
   final int width,
   int x,
   final double[] column
) {
   for (int i = 0; (i < column.length); i++, x+=width)
      column[i] = (double)array[x];
} /* end extractColumn */

/*------------------------------------------------------------------*/
private void extractRow (
   final double[] array,
   int y,
   final double[] row
) {
   y *= row.length;
   for (int i = 0; (i < row.length); i++)
      row[i] = (double)array[y++];
} /* end extractRow */

/*------------------------------------------------------------------*/
private double[] getBasicFromCardinal2D (
) {
   final double[] basic = new double[width * height];
   final double[] hLine = new double[width];
   final double[] vLine = new double[height];
   for (int y = 0; (y < height); y++) {
      extractRow(image, y, hLine);
      samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
      putRow(basic, y, hLine);
   }
   for (int x = 0; (x < width); x++) {
      extractColumn(basic, width, x, vLine);
      samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
      putColumn(basic, width, x, vLine);
   }
   return(basic);
} /* end getBasicFromCardinal2D */

/*------------------------------------------------------------------*/
private double[] getBasicFromCardinal2D (
   final double[] cardinal,
   final int width,
   final int height,
   final int degree
) {
   final double[] basic = new double[width * height];
   final double[] hLine = new double[width];
   final double[] vLine = new double[height];
   for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
      extractRow(cardinal, y, hLine);
      samplesToInterpolationCoefficient1D(hLine, degree, 0.0);
      putRow(basic, y, hLine);
   }
   for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
      extractColumn(basic, width, x, vLine);
      samplesToInterpolationCoefficient1D(vLine, degree, 0.0);
      putColumn(basic, width, x, vLine);
   }
   return(basic);
} /* end getBasicFromCardinal2D */

/*------------------------------------------------------------------*/
private double[] getHalfDual2D (
   final double[] fullDual,
   final int fullWidth,
   final int fullHeight
) {
   final int halfWidth = fullWidth / 2;
   final int halfHeight = fullHeight / 2;
   final double[] hLine = new double[fullWidth];
   final double[] hData = new double[halfWidth];
   final double[] vLine = new double[fullHeight];
   final double[] vData = new double[halfHeight];
   final double[] demiDual = new double[halfWidth * fullHeight];
   final double[] halfDual = new double[halfWidth * halfHeight];
   for (int y = 0; ((y < fullHeight) && (!t.isInterrupted())); y++) {
      extractRow(fullDual, y, hLine);
      reduceDual1D(hLine, hData);
      putRow(demiDual, y, hData);
   }
   for (int x = 0; ((x < halfWidth) && (!t.isInterrupted())); x++) {
      extractColumn(demiDual, halfWidth, x, vLine);
      reduceDual1D(vLine, vData);
      putColumn(halfDual, halfWidth, x, vData);
   }
   return(halfDual);
} /* end getHalfDual2D */

/*------------------------------------------------------------------*/
private double getInitialAntiCausalCoefficientMirrorOffBounds (
   final double[] c,
   final double z,
   final double tolerance
) {
   return(z * c[c.length - 1] / (z - 1.0));
} /* end getInitialAntiCausalCoefficientMirrorOffBounds */

/*------------------------------------------------------------------*/
private double getInitialCausalCoefficientMirrorOffBounds (
   final double[] c,
   final double z,
   final double tolerance
) {
   double z1 = z;
   double zn = Math.pow(z, c.length);
   double sum = (1.0 + z) * (c[0] + zn * c[c.length - 1]);
   int horizon = c.length;
   if (0.0 < tolerance) {
      horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
      horizon = (horizon < c.length) ? (horizon) : (c.length);
   }
   zn = zn * zn;
   for (int n = 1; (n < (horizon - 1)); n++) {
      z1 = z1 * z;
      zn = zn / z;
      sum = sum + (z1 + zn) * c[n];
   }
   return(sum / (1.0 - Math.pow(z, 2 * c.length)));
} /* end getInitialCausalCoefficientMirrorOffBounds */

/*------------------------------------------------------------------*/
private void putColumn (
   final double[] array,
   final int width,
   int x,
   final double[] column
) {
   for (int i = 0; (i < column.length); i++, x+=width)
      array[x] = (double)column[i];
} /* end putColumn */

/*------------------------------------------------------------------*/
private void putRow (
   final double[] array,
   int y,
   final double[] row
) {
   y *= row.length;
   for (int i = 0; (i < row.length); i++)
      array[y++] = (double)row[i];
} /* end putRow */

/*------------------------------------------------------------------*/
private void reduceDual1D (
   final double[] c,
   final double[] s
) {
   final double h[] = {6.0 / 16.0, 4.0 / 16.0, 1.0 / 16.0};
   if (2 <= s.length) {
      s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
      for (int i = 2, j = 1; (j < (s.length - 1)); i += 2, j++) {
         s[j] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
            + h[2] * (c[i - 2] + c[i + 2]);
      }
      if (c.length == (2 * s.length)) {
         s[s.length - 1] = h[0] * c[c.length - 2] + h[1] * (c[c.length - 3] + c[c.length - 1])
            + h[2] * (c[c.length - 4] + c[c.length - 1]);
      }
      else {
         s[s.length - 1] = h[0] * c[c.length - 3] + h[1] * (c[c.length - 4] + c[c.length - 2])
            + h[2] * (c[c.length - 5] + c[c.length - 1]);
      }
   }
   else {
      switch (c.length) {
         case 3:
            s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
            break;
         case 2:
            s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + 2.0 * h[2] * c[1];
            break;
         default:
      }
   }
} /* end reduceDual1D */

/*------------------------------------------------------------------*/
private void samplesToInterpolationCoefficient1D (
   final double[] c,
   final int degree,
   final double tolerance
) {
   double[] z = new double[0];
   double lambda = 1.0;
   switch (degree) {
      case 3:
         z = new double[1];
         z[0] = Math.sqrt(3.0) - 2.0;
         break;
      case 7:
         z = new double[3];
         z[0] = -0.5352804307964381655424037816816460718339231523426924148812;
         z[1] = -0.122554615192326690515272264359357343605486549427295558490763;
         z[2] = -0.0091486948096082769285930216516478534156925639545994482648003;
         break;
      default:
   }
   if (c.length == 1) {
      return;
   }
   for (int k = 0; (k < z.length); k++) {
      lambda *= (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
   }
   for (int n = 0; (n < c.length); n++) {
      c[n] = c[n] * lambda;
   }
   for (int k = 0; (k < z.length); k++) {
      c[0] = getInitialCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
      for (int n = 1; (n < c.length); n++) {
         c[n] = c[n] + z[k] * c[n - 1];
      }
      c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
      for (int n = c.length - 2; (0 <= n); n--) {
         c[n] = z[k] * (c[n+1] - c[n]);
      }
   }
} /* end samplesToInterpolationCoefficient1D */

/*------------------------------------------------------------------*/
private void symmetricFirMirrorOffBounds1D (
   final double[] h,
   final double[] c,
   final double[] s
) {
   switch (h.length) {
      case 2:
         if (2 <= c.length) {
            s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]);
            for (int i = 1; (i < (s.length - 1)); i++) {
               s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1]);
            }
            s[s.length - 1] = h[0] * c[c.length - 1]
               + h[1] * (c[c.length - 2] + c[c.length - 1]);
         }
         else {
            s[0] = (h[0] + 2.0 * h[1]) * c[0];
         }
         break;
      case 4:
         if (6 <= c.length) {
            s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
               + h[3] * (c[2] + c[3]);
            s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
               + h[3] * (c[1] + c[4]);
            s[2] = h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[4])
               + h[3] * (c[0] + c[5]);
            for (int i = 3; (i < (s.length - 3)); i++) {
               s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
                  + h[2] * (c[i - 2] + c[i + 2]) + h[3] * (c[i - 3] + c[i + 3]);
            }
            s[s.length - 3] = h[0] * c[c.length - 3]
               + h[1] * (c[c.length - 4] + c[c.length - 2])
               + h[2] * (c[c.length - 5] + c[c.length - 1])
               + h[3] * (c[c.length - 6] + c[c.length - 1]);
            s[s.length - 2] = h[0] * c[c.length - 2]
               + h[1] * (c[c.length - 3] + c[c.length - 1])
               + h[2] * (c[c.length - 4] + c[c.length - 1])
               + h[3] * (c[c.length - 5] + c[c.length - 2]);
            s[s.length - 1] = h[0] * c[c.length - 1]
               + h[1] * (c[c.length - 2] + c[c.length - 1])
               + h[2] * (c[c.length - 3] + c[c.length - 2])
               + h[3] * (c[c.length - 4] + c[c.length - 3]);
         }
         else {
            switch (c.length) {
               case 5:
                  s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
                     + h[3] * (c[2] + c[3]);
                  s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
                     + h[3] * (c[1] + c[4]);
                  s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
                     + (h[2] + h[3]) * (c[0] + c[4]);
                  s[3] = h[0] * c[3] + h[1] * (c[2] + c[4]) + h[2] * (c[1] + c[4])
                     + h[3] * (c[0] + c[3]);
                  s[4] = h[0] * c[4] + h[1] * (c[3] + c[4]) + h[2] * (c[2] + c[3])
                     + h[3] * (c[1] + c[2]);
                  break;
               case 4:
                  s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
                     + h[3] * (c[2] + c[3]);
                  s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
                     + h[3] * (c[1] + c[3]);
                  s[2] = h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[3])
                     + h[3] * (c[0] + c[2]);
                  s[3] = h[0] * c[3] + h[1] * (c[2] + c[3]) + h[2] * (c[1] + c[2])
                     + h[3] * (c[0] + c[1]);
                  break;
               case 3:
                  s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
                     + 2.0 * h[3] * c[2];
                  s[1] = h[0] * c[1] + (h[1] + h[2]) * (c[0] + c[2])
                     + 2.0 * h[3] * c[1];
                  s[2] = h[0] * c[2] + h[1] * (c[1] + c[2]) + h[2] * (c[0] + c[1])
                     + 2.0 * h[3] * c[0];
                  break;
               case 2:
                  s[0] = (h[0] + h[1] + h[3]) * c[0] + (h[1] + 2.0 * h[2] + h[3]) * c[1];
                  s[1] = (h[0] + h[1] + h[3]) * c[1] + (h[1] + 2.0 * h[2] + h[3]) * c[0];
                  break;
               case 1:
                  s[0] = (h[0] + 2.0 * (h[1] + h[2] + h[3])) * c[0];
                  break;
               default:
            }
         }
         break;
      default:
   }
} /* end symmetricFirMirrorOffBounds1D */

} /* end class unwarpJImageModel */

/*====================================================================
|   unwarpJMask
\===================================================================*/

/* This class is responsible for the mask preprocessing that takes
   place concurrently with user-interface events. It contains methods
   to compute the mask pyramids. */
class unwarpJMask
{ /* begin class unwarpJMask */

/*....................................................................
   Private variables
....................................................................*/
// Mask related
private boolean[]     mask;
private int           width;
private int           height;
private Polygon       polygon=null;
private boolean       mask_from_the_stack;

/*....................................................................
   Public methods
....................................................................*/

/* Bounding box for the mask.
   An array is returned with the convention [x0,y0,xF,yF]. This array
   is returned in corners. This vector should be already resized. */
public void BoundingBox(int [] corners) {
   if (polygon.npoints!=0) {
      Rectangle boundingbox=polygon.getBounds();
      corners[0]=(int)boundingbox.x;
      corners[1]=(int)boundingbox.y;
      corners[2]=corners[0]+(int)boundingbox.width;
      corners[3]=corners[1]+(int)boundingbox.height;
   } else {
      corners[0]=0;
      corners[1]=0;
      corners[2]=width;
      corners[3]=height;
   }
}

/*------------------------------------------------------------------*/
/* Set to true every pixel of the full-size mask. */
public void clearMask (
) {
   int k = 0;
   for (int y = 0; (y < height); y++)
      for (int x = 0; (x < width); x++)
         mask[k++] = true;
   polygon=new Polygon();
} /* end clearMask */

/*------------------------------------------------------------------*/
/* Fill the mask associated to the mask points. */
public void fillMask (int tool) {
   int k=0;
   for (int y = 0; (y < height); y++)
      for (int x = 0; (x < width); x++) {
         mask[k] = polygon.contains(x,y);
         if (tool==unwarpJPointAction.INVERTMASK) mask[k]=!mask[k];
         k++;
      }
}

/*------------------------------------------------------------------*/
/* Returns the value of the mask at a certain pixel.
   If the sample is not integer then the closest point is returned. */
public boolean getValue(double x, double y) {
   int u=(int)Math.round(x);
   int v=(int)Math.round(y);
   if (u<0 || u>=width || v<0 || v>=height) return false;
   else                                     return mask[v*width+u];
}

/*------------------------------------------------------------------*/
/* Get a point from the mask. */
public Point getPoint(int i) {
   return new Point(polygon.xpoints[i],polygon.ypoints[i]);
}

/*------------------------------------------------------------------*/
/* True if the mask was taken from the stack. */
public boolean isFromStack() {
   return mask_from_the_stack;
}

/*------------------------------------------------------------------*/
/* Get the number of points in the mask. */
public int numberOfMaskPoints() {return polygon.npoints;}

/*------------------------------------------------------------------*/
/* Read mask from file.
   An error is shown if the file read is not of the same size as the
   previous mask. */
public void readFile(String filename) {
   ImagePlus aux = new ImagePlus(filename);
   if (aux.getWidth()!=width || aux.getHeight()!=height)
      IJ.error("Mask in file is not of the expected size");
   ImageProcessor ip=aux.getProcessor();
   int k=0;
   for (int y = 0; (y < height); y++)
      for (int x = 0; (x < width); x++, k++)
         if (ip.getPixelValue(x,y)!=0) mask[k]=true;
         else                          mask[k]=false;
}

/*------------------------------------------------------------------*/
/* Show mask. */
public void showMask () {
    double [][]img=new double[height][width];
   int k = 0;
   for (int y = 0; (y < height); y++)
      for (int x = 0; (x < width); x++)
         if (mask[k++]) img[y][x]=1; else img[y][x]=0;
   unwarpJMiscTools.showImage("Mask",img);
}

/*------------------------------------------------------------------*/
/* Set the mask points. */
public void setMaskPoints (final Vector listMaskPoints) {
   int imax=listMaskPoints.size();
   for (int i=0; i<imax; i++) {
      Point p=(Point)listMaskPoints.elementAt(i);
      polygon.addPoint(p.x,p.y);
   }
}

/*------------------------------------------------------------------*/
/* Sets the value of the mask at a certain pixel. */
public void setValue(int u, int v, boolean value) {
   if (u>=0 && u<width && v>=0 && v<height) mask[v*width+u]=value;
}

/*------------------------------------------------------------------*/
/* Empty constructor, the input image is used only to take the
   image size. */
public unwarpJMask (
   final ImageProcessor ip, boolean take_mask
) {
   width  = ip.getWidth();
   height = ip.getHeight();
   mask = new boolean[width * height];
   if (!take_mask) {
       mask_from_the_stack=false;
       clearMask();
   } else {
       mask_from_the_stack=true;
       int k=0;
      if (ip instanceof ByteProcessor) {
         final byte[] pixels = (byte[])ip.getPixels();
         for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++, k++) {
               mask[k] = (pixels[k] != 0);
            }
         }
      }
      else if (ip instanceof ShortProcessor) {
         final short[] pixels = (short[])ip.getPixels();
         for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++, k++) {
               mask[k] = (pixels[k] != 0);
            }
         }
      }
      else if (ip instanceof FloatProcessor) {
         final float[] pixels = (float[])ip.getPixels();
         for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++, k++) {
               mask[k] = (pixels[k] != 0.0F);
            }
         }
      }
   }
} /* end unwarpJMask */

} /* end class unwarpJMask */

/*====================================================================
|   unwarpJMathTools
\===================================================================*/
class unwarpJMathTools {

private static final double FLT_EPSILON = (double)Float.intBitsToFloat((int)0x33FFFFFF);
private static final int MAX_SVD_ITERATIONS = 1000;

/*------------------------------------------------------------------*/
public static double Bspline01 (
   double x
) {
   x = Math.abs(x);
   if (x < 1.0F) {
      return(1.0F - x);
   }
   else {
      return(0.0F);
   }
} /* Bspline01 */

/*------------------------------------------------------------------*/
public static double Bspline02 (
   double x
) {
   x = Math.abs(x);
   if (x < 0.5F) {
      return(3.0F / 4.0F - x * x);
   }
   else if (x < 1.5F) {
      x -= 1.5F;
      return(0.5F * x * x);
   }
   else {
      return(0.0F);
   }
} /* Bspline02 */

/*------------------------------------------------------------------*/
public static double Bspline03 (
   double x
) {
   x = Math.abs(x);
   if (x < 1.0F) {
      return(0.5F * x * x * (x - 2.0F) + (2.0F / 3.0F));
   }
   else if (x < 2.0F) {
      x -= 2.0F;
      return(x * x * x / (-6.0F));
   }
   else {
      return(0.0F);
   }
} /* Bspline03 */

/*------------------------------------------------------------------*/
public static double EuclideanNorm (
   final double a,
   final double b
) {
   final double absa = Math.abs(a);
   final double absb = Math.abs(b);
   if (absb < absa) {
      return(absa * Math.sqrt(1.0 + (absb * absb / (absa * absa))));
   }
   else {
      return((absb == 0.0F) ? (0.0F)
         : (absb * Math.sqrt(1.0 + (absa * absa / (absb * absb)))));
   }
} /* end EuclideanNorm */

/*------------------------------------------------------------------*/
public static boolean invertMatrixSVD(
         int   Ydim,       // Input, 
         int   Xdim,       // Input,   
   final double [][]B,      // Input, matrix to invert
   final double [][]iB      // Output, inverted matrix
) {
   boolean underconstrained=false;

   final double[] W = new double[Xdim];
   final double[][] V = new double[Xdim][Xdim];
   // B=UWV^t (U is stored in B)
   singularValueDecomposition(B, W, V);

   // B^-1=VW^-1U^t

   // Compute W^-1
   int Nzeros=0;
   for (int k = 0; k<Xdim; k++) {
      if (Math.abs(W[k]) < FLT_EPSILON) {
         W[k] = 0.0F;
         Nzeros++;
      } else 
         W[k] = 1.0F / W[k];
   }
   if (Ydim-Nzeros<Xdim) underconstrained=true;

   // Compute VW^-1
   for (int i = 0; i<Xdim; i++)
      for (int j = 0; j<Xdim; j++)
         V[i][j] *= W[j];

   // Compute B^-1
   // iB should have been already resized
   for (int i = 0; i<Xdim; i++) {
      for (int j = 0; j<Ydim; j++) {
         iB[i][j] = 0.0F;
         for (int k = 0; k<Xdim; k++)
            iB[i][j] += V[i][k] * B[j][k];
      }
   }
   return underconstrained;
} /* invertMatrixSVD */

/*------------------------------------------------------------------*/
/*********************************************************************
 * Gives the least-squares solution to (A * x = b) such that
 * (A^T * A)^-1 * A^T * b = x is a vector of size (column), where A is
 * a (line x column) matrix, and where b is a vector of size (line).
 * The result may differ from that obtained by a singular-value
 * decomposition in the cases where the least-squares solution is not
 * uniquely defined (SVD returns the solution of least norm, not QR).
 *
 * @param A An input matrix A[line][column] of size (line x column)
 * @param b An input vector b[line] of size (line)
 * @return An output vector x[column] of size (column)
 ********************************************************************/
public static double[] linearLeastSquares (
   final double[][] A,
   final double[] b
) {
   final int lines = A.length;
   final int columns = A[0].length;
   final double[][] Q = new double[lines][columns];
   final double[][] R = new double[columns][columns];
   final double[] x = new double[columns];
   double s;
   for (int i = 0; (i < lines); i++) {
      for (int j = 0; (j < columns); j++) {
         Q[i][j] = A[i][j];
      }
   }
   QRdecomposition(Q, R);
   for (int i = 0; (i < columns); i++) {
      s = 0.0F;
      for (int j = 0; (j < lines); j++) {
         s += Q[j][i] * b[j];
      }
      x[i] = s;
   }
   for (int i = columns - 1; (0 <= i); i--) {
      s = R[i][i];
      if ((s * s) == 0.0F) {
         x[i] = 0.0F;
      }
      else {
         x[i] /= s;
      }
      for (int j = i - 1; (0 <= j); j--) {
         x[j] -= R[j][i] * x[i];
      }
   }
   return(x);
} /* end linearLeastSquares */

/*------------------------------------------------------------------*/
public static double nchoosek(int n, int k) {
   if (k>n)  return 0;
   if (k==0) return 1;
   if (k==1) return n;
   if (k>n/2) k=n-k;
   double prod=1;
   for (int i=1; i<=k; i++) prod*=(n-k+i)/i; 
   return prod;
}

/*------------------------------------------------------------------*/
/*********************************************************************
 * Decomposes the (line x column) input matrix Q into an orthonormal
 * output matrix Q of same size (line x column) and an upper-diagonal
 * square matrix R of size (column x column), such that the matrix
 * product (Q * R) gives the input matrix, and such that the matrix
 * product (Q^T * Q) gives the identity.
 *
 * @param Q An in-place (line x column) matrix Q[line][column], which
 * expects as input the matrix to decompose, and which returns as
 * output an orthonormal matrix
 * @param R An output (column x column) square matrix R[column][column]
 ********************************************************************/
public static void QRdecomposition (
   final double[][] Q,
   final double[][] R
) {
   final int lines = Q.length;
   final int columns = Q[0].length;
   final double[][] A = new double[lines][columns];
   double s;
   for (int j = 0; (j < columns); j++) {
      for (int i = 0; (i < lines); i++) {
         A[i][j] = Q[i][j];
      }
      for (int k = 0; (k < j); k++) {
         s = 0.0F;
         for (int i = 0; (i < lines); i++) {
            s += A[i][j] * Q[i][k];
         }
         for (int i = 0; (i < lines); i++) {
            Q[i][j] -= s * Q[i][k];
         }
      }
      s = 0.0F;
      for (int i = 0; (i < lines); i++) {
         s += Q[i][j] * Q[i][j];
      }
      if ((s * s) == 0.0F) {
         s = 0.0F;
      }
      else {
         s = 1.0F / Math.sqrt(s);
      }
      for (int i = 0; (i < lines); i++) {
         Q[i][j] *= s;
      }
   }
   for (int i = 0; (i < columns); i++) {
      for (int j = 0; (j < i); j++) {
         R[i][j] = 0.0F;
      }
      for (int j = i; (j < columns); j++) {
         R[i][j] = 0.0F;
         for (int k = 0; (k < lines); k++) {
            R[i][j] += Q[k][i] * A[k][j];
         }
      }
   }
} /* end QRdecomposition */

/* -----------------------------------------------------------------*/
public static void showMatrix(
   int Ydim,
   int Xdim,
   final double [][]A
) {
   for (int i=0; i<Ydim; i++) {
      for (int j=0; j<Xdim; j++)
         System.out.print(A[i][j]+" ");
      System.out.println();
   }
}

/*------------------------------------------------------------------*/
public static void singularValueDecomposition (
   final double[][] U,
   final double[] W,
   final double[][] V
) {
   final int lines = U.length;
   final int columns = U[0].length;
   final double[] rv1 = new double[columns];
   double norm, scale;
   double c, f, g, h, s;
   double x, y, z;
   int l = 0;
   int nm = 0;
   boolean   flag;
   g = scale = norm = 0.0F;
   for (int i = 0; (i < columns); i++) {
      l = i + 1;
      rv1[i] = scale * g;
      g = s = scale = 0.0F;
      if (i < lines) {
         for (int k = i; (k < lines); k++) {
            scale += Math.abs(U[k][i]);
         }
         if (scale != 0.0) {
            for (int k = i; (k < lines); k++) {
               U[k][i] /= scale;
               s += U[k][i] * U[k][i];
            }
            f = U[i][i];
            g = (0.0 <= f) ? (-Math.sqrt((double)s))
               : (Math.sqrt((double)s));
            h = f * g - s;
            U[i][i] = f - g;
            for (int j = l; (j < columns); j++) {
               s = 0.0F;
               for (int k = i; (k < lines); k++) {
                  s += U[k][i] * U[k][j];
               }
               f = s / h;
               for (int k = i; (k < lines); k++) {
                  U[k][j] += f * U[k][i];
               }
            }
            for (int k = i; (k < lines); k++) {
               U[k][i] *= scale;
            }
         }
      }
      W[i] = scale * g;
      g = s = scale = 0.0F;
      if ((i < lines) && (i != (columns - 1))) {
         for (int k = l; (k < columns); k++) {
            scale += Math.abs(U[i][k]);
         }
         if (scale != 0.0) {
            for (int k = l; (k < columns); k++) {
               U[i][k] /= scale;
               s += U[i][k] * U[i][k];
            }
            f = U[i][l];
            g = (0.0 <= f) ? (-Math.sqrt(s))
               : (Math.sqrt(s));
            h = f * g - s;
            U[i][l] = f - g;
            for (int k = l; (k < columns); k++) {
               rv1[k] = U[i][k] / h;
            }
            for (int j = l; (j < lines); j++) {
               s = 0.0F;
               for (int k = l; (k < columns); k++) {
                  s += U[j][k] * U[i][k];
               }
               for (int k = l; (k < columns); k++) {
                  U[j][k] += s * rv1[k];
               }
            }
            for (int k = l; (k < columns); k++) {
               U[i][k] *= scale;
            }
         }
      }
      norm = ((Math.abs(W[i]) + Math.abs(rv1[i])) < norm) ? (norm)
         : (Math.abs(W[i]) + Math.abs(rv1[i]));
   }
   for (int i = columns - 1; (0 <= i); i--) {
      if (i < (columns - 1)) {
         if (g != 0.0) {
            for (int j = l; (j < columns); j++) {
               V[j][i] = U[i][j] / (U[i][l] * g);
            }
            for (int j = l; (j < columns); j++) {
               s = 0.0F;
               for (int k = l; (k < columns); k++) {
                  s += U[i][k] * V[k][j];
               }
               for (int k = l; (k < columns); k++) {
                  if (s != 0.0) {
                     V[k][j] += s * V[k][i];
                  }
               }
            }
         }
         for (int j = l; (j < columns); j++) {
            V[i][j] = V[j][i] = 0.0F;
         }
      }
      V[i][i] = 1.0F;
      g = rv1[i];
      l = i;
   }
   for (int i = (lines < columns) ? (lines - 1) : (columns - 1); (0 <= i); i--) {
      l = i + 1;
      g = W[i];
      for (int j = l; (j < columns); j++) {
         U[i][j] = 0.0F;
      }
      if (g != 0.0) {
         g = 1.0F / g;
         for (int j = l; (j < columns); j++) {
            s = 0.0F;
            for (int k = l; (k < lines); k++) {
               s += U[k][i] * U[k][j];
            }
            f = s * g / U[i][i];
            for (int k = i; (k < lines); k++) {
               if (f != 0.0) {
                  U[k][j] += f * U[k][i];
               }
            }
         }
         for (int j = i; (j < lines); j++) {
            U[j][i] *= g;
         }
      }
      else {
         for (int j = i; (j < lines); j++) {
            U[j][i] = 0.0F;
         }
      }
      U[i][i] += 1.0F;
   }
   for (int k = columns - 1; (0 <= k); k--) {
      for (int its = 1; (its <= MAX_SVD_ITERATIONS); its++) {
         flag = true;
         for (l = k; (0 <= l); l--) {
            nm = l - 1;
            if ((Math.abs(rv1[l]) + norm) == norm) {
               flag = false;
               break;
            }
            if ((Math.abs(W[nm]) + norm) == norm) {
               break;
            }
         }
         if (flag) {
            c = 0.0F;
            s = 1.0F;
            for (int i = l; (i <= k); i++) {
               f = s * rv1[i];
               rv1[i] *= c;
               if ((Math.abs(f) + norm) == norm) {
                  break;
               }
               g = W[i];
               h = EuclideanNorm(f, g);
               W[i] = h;
               h = 1.0F / h;
               c = g * h;
               s = -f * h;
               for (int j = 0; (j < lines); j++) {
                  y = U[j][nm];
                  z = U[j][i];
                  U[j][nm] = y * c + z * s;
                  U[j][i] = z * c - y * s;
               }
            }
         }
         z = W[k];
         if (l == k) {
            if (z < 0.0) {
               W[k] = -z;
               for (int j = 0; (j < columns); j++) {
                  V[j][k] = -V[j][k];
               }
            }
            break;
         }
         if (its == MAX_SVD_ITERATIONS) {
            return;
         }
         x = W[l];
         nm = k - 1;
         y = W[nm];
         g = rv1[nm];
         h = rv1[k];
         f = ((y - z) * (y + z) + (g - h) * (g + h)) / (2.0F * h * y);
         g = EuclideanNorm(f, 1.0F);
         f = ((x - z) * (x + z) + h * ((y / (f + ((0.0 <= f) ? (Math.abs(g))
            : (-Math.abs(g))))) - h)) / x;
         c = s = 1.0F;
         for (int j = l; (j <= nm); j++) {
            int i = j + 1;
            g = rv1[i];
            y = W[i];
            h = s * g;
            g = c * g;
            z = EuclideanNorm(f, h);
            rv1[j] = z;
            c = f / z;
            s = h / z;
            f = x * c + g * s;
            g = g * c - x * s;
            h = y * s;
            y *= c;
            for (int jj = 0; (jj < columns); jj++) {
               x = V[jj][j];
               z = V[jj][i];
               V[jj][j] = x * c + z * s;
               V[jj][i] = z * c - x * s;
            }
            z = EuclideanNorm(f, h);
            W[j] = z;
            if (z != 0.0F) {
               z = 1.0F / z;
               c = f * z;
               s = h * z;
            }
            f = c * g + s * y;
            x = c * y - s * g;
            for (int jj = 0; (jj < lines); jj++) {
               y = U[jj][j];
               z = U[jj][i];
               U[jj][j] = y * c + z * s;
               U[jj][i] = z * c - y * s;
            }
         }
         rv1[l] = 0.0F;
         rv1[k] = f;
         W[k] = x;
      }
   }
} /* end singularValueDecomposition */

public static void singularValueBackSubstitution (
   final double [][]U,            /* input matrix */
   final double   []W,            /* vector of singular values */
   final double [][]V,            /* untransposed orthogonal matrix */
   final double     []B,            /* input vector */
   final double   []X             /* returned solution */
){

/* solve (U.W.Transpose(V)).X == B in terms of X */
/* {U, W, V} are given by SingularValueDecomposition */
/* by convention, set w[i,j]=0 to get (1/w[i,j])=0 */
/* the size of the input matrix U is (Lines x Columns) */
/* the size of the vector (1/W) of singular values is (Columns) */
/* the size of the untransposed orthogonal matrix V is (Columns x Columns) */
/* the size of the input vector B is (Lines) */
/* the size of the output vector X is (Columns) */

   final int Lines   = U.length;
   final int Columns = U[0].length;
   double []  aux     = new double [Columns];

   // A=UWV^t
   // A^-1=VW^-1U^t
   // X=A^-1*B

   // Perform aux=W^-1 U^t B
   for (int i=0; i<Columns; i++) {
       aux[i]=0.0F;
      if (Math.abs(W[i]) > FLT_EPSILON) {
          for (int j=0; j<Lines; j++) aux[i]+=U[j][i]*B[j]; // U^t B
          aux[i]/=W[i];                                     // W^-1 U^t B
       }
   }

   // Perform X=V aux
   for (int i=0; i<Columns; i++) {
       X[i]=0.0F;
       for (int j=0; j<Columns; j++) X[i]+=V[i][j]*aux[j];
   }
}

} /* End MathTools */

/*====================================================================
|   unwarpJMiscTools
\===================================================================*/
class unwarpJMiscTools {

/* Apply a given splines transformation to the source image.
   The source image is modified. The target image is used to know
   the output size. */
static public void applyTransformationToSource(
   ImagePlus sourceImp, ImagePlus targetImp,
   unwarpJImageModel source,
   int intervals,
   double [][]cx,
   double [][]cy) {
   int targetHeight=targetImp.getProcessor().getHeight();
   int targetWidth =targetImp.getProcessor().getWidth ();
   int sourceHeight=sourceImp.getProcessor().getHeight();
   int sourceWidth =sourceImp.getProcessor().getWidth ();

   // Ask for memory for the transformation
   double [][] transformation_x=new double [targetHeight][targetWidth];
   double [][] transformation_y=new double [targetHeight][targetWidth];

   // Compute the deformation
   // Set these coefficients to an interpolator
   unwarpJImageModel swx = new unwarpJImageModel(cx);
   unwarpJImageModel swy = new unwarpJImageModel(cy);

    // Compute the transformation mapping
    boolean ORIGINAL=false;
    for (int v=0; v<targetHeight; v++) {
      final double tv = (double)(v * intervals) / (double)(targetHeight - 1) + 1.0F;
      for (int u = 0; u<targetWidth; u++) {
         final double tu = (double)(u * intervals) / (double)(targetWidth - 1) + 1.0F;
         swx.prepareForInterpolation(tu, tv, ORIGINAL); transformation_x[v][u] = swx.interpolateI();
         swy.prepareForInterpolation(tu, tv, ORIGINAL); transformation_y[v][u] = swy.interpolateI();
      }
    }

    // Compute the warped image
    FloatProcessor fp=new FloatProcessor(targetWidth,targetHeight);
    for (int v=0; v<targetHeight; v++)
      for (int u=0; u<targetWidth; u++) {
         final double x = transformation_x[v][u];
         final double y = transformation_y[v][u];
         if (x>=0 && x<sourceWidth && y>=0 && y<sourceHeight) {
            source.prepareForInterpolation(x,y,ORIGINAL);
            fp.putPixelValue(u,v,source.interpolateI());
            } else
               fp.putPixelValue(u,v,0);
        }
    fp.resetMinAndMax();
    sourceImp.setProcessor(sourceImp.getTitle(),fp);
    sourceImp.updateImage();
}

/*------------------------------------------------------------------*/
/* Draw an arrow between two points.
   The arrow head is in (x2,y2) */
static public void drawArrow(double [][]canvas, int x1, int y1,
   int x2, int y2, double color, int arrow_size) {
   drawLine(canvas,x1,y1,x2,y2,color);
   int arrow_size2=2*arrow_size;

   // Do not draw the arrow_head if the arrow is very small
   if ((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)<arrow_size*arrow_size) return;

   // Vertical arrow
   if (x2 == x1) {
      if (y2 > y1) {
        drawLine(canvas,x2,y2,x2-arrow_size,y2-arrow_size2,color);
        drawLine(canvas,x2,y2,x2+arrow_size,y2-arrow_size2,color);
      } else {
        drawLine(canvas,x2,y2,x2-arrow_size,y2+arrow_size2,color);
        drawLine(canvas,x2,y2,x2+arrow_size,y2+arrow_size2,color);
      }
    }

    // Horizontal arrow
    else if (y2 == y1) {
      if (x2 > x1) {
        drawLine(canvas,x2,y2,x2-arrow_size2,y2-arrow_size,color);
        drawLine(canvas,x2,y2,x2-arrow_size2,y2+arrow_size,color);
      } else {
        drawLine(canvas,x2,y2,x2+arrow_size2,y2-arrow_size,color);
        drawLine(canvas,x2,y2,x2+arrow_size2,y2+arrow_size,color);
      }
    }

    // Now we need to rotate the arrow head about the origin
    else {
      // Calculate the angle of rotation and adjust for the quadrant
      double t1 = Math.abs(new Integer(y2 - y1).doubleValue());
      double t2 = Math.abs(new Integer(x2 - x1).doubleValue());
      double theta = Math.atan(t1 / t2);
      if (x2 < x1) {
        if (y2 < y1) theta = Math.PI + theta;
        else         theta = - (Math.PI + theta);
      } else if (x2 > x1 && y2 < y1)
        theta =  2*Math.PI - theta;
      double cosTheta = Math.cos(theta);
      double sinTheta = Math.sin(theta);

      // Create the other points and translate the arrow to the origin
      Point p2 = new Point(-arrow_size2,-arrow_size);
      Point p3 = new Point(-arrow_size2,+arrow_size);

      // Rotate the points (without using matrices!)
      int x = new Long(Math.round((cosTheta * p2.x) - (sinTheta * p2.y))).intValue();
      p2.y = new Long(Math.round((sinTheta * p2.x) + (cosTheta * p2.y))).intValue();
      p2.x = x;
      x = new Long(Math.round((cosTheta * p3.x) - (sinTheta * p3.y))).intValue();
      p3.y = new Long(Math.round((sinTheta * p3.x) + (cosTheta * p3.y))).intValue();
      p3.x = x;

      // Translate back to desired location and add to polygon
      p2.translate(x2,y2);
      p3.translate(x2,y2);
      drawLine(canvas,x2,y2,p2.x,p2.y,color);
      drawLine(canvas,x2,y2,p3.x,p3.y,color);
    }   
}

/*------------------------------------------------------------------*/
/* Draw a line between two points.
   Bresenham's algorithm */
static public void drawLine(double [][]canvas, int x1, int y1,
   int x2, int y2, double color) {
   int temp;
   int dy_neg = 1;
   int dx_neg = 1;
   int switch_x_y = 0;
   int neg_slope = 0;
   int tempx, tempy;
   int dx = x2 - x1;
   if(dx == 0)
      if(y1 > y2) {
         for(int n = y2; n <= y1; n++) Point(canvas,n,x1,color);
         return;
      } else {
         for(int n = y1; n <= y2; n++) Point(canvas,n,x1,color);
         return;
      }

   int dy = y2 - y1;
   if(dy == 0)
      if(x1 > x2) {
         for(int n = x2; n <= x1; n++) Point(canvas,y1,n,color);
         return;
      } else {
         for(int n = x1; n <= x2; n++) Point(canvas,y1,n,color);
         return;
      }

   float m = (float) dy/dx;

   if(m > 1 || m < -1) {
      temp = x1;
      x1 = y1;
      y1 = temp;
      temp = x2;
      x2 = y2;
      y2 = temp;
      dx = x2 - x1;
      dy = y2 - y1;
      m = (float) dy/dx;
      switch_x_y = 1;
   }

   if(x1 > x2) {
      temp = x1;
      x1 = x2;
      x2 = temp;
      temp = y1;
      y1 = y2;
      y2 = temp;
      dx = x2 - x1;
      dy = y2 - y1;
      m = (float) dy/dx;
   }

   if(m < 0) {
      if(dy < 0) {
         dy_neg = -1;
         dx_neg = 1;
      } else {
         dy_neg = 1;
         dx_neg = -1;
      }
      neg_slope = 1;
   }

   int d = 2 * (dy * dy_neg) - (dx * dx_neg);
   int incrH = 2 * dy * dy_neg;
   int incrHV = 2 * ( (dy * dy_neg)  - (dx * dx_neg) );
   int x = x1;
   int y = y1;
   tempx = x;
   tempy = y;

   if(switch_x_y == 1) {
      temp = x;
      x = y;
      y = temp;
   }
   Point(canvas,y,x,color);
   x = tempx;
   y = tempy;

   while(x < x2) {
      if(d <= 0) {
         x++;
         d += incrH;
      } else {
         d += incrHV;
         x++;
         if(neg_slope == 0) y++;
         else               y--;
      }
      tempx = x;
      tempy = y;

      if (switch_x_y == 1) {
         temp = x;
         x = y;
         y = temp;
      }
      Point(canvas,y,x,color);
      x = tempx;
      y = tempy;
   }
}

/*------------------------------------------------------------------*/
static public void extractImage(final ImageProcessor ip, double image[]) {
   int k=0;
   int height=ip.getHeight();
   int width =ip.getWidth ();
   if (ip instanceof ByteProcessor) {
      final byte[] pixels = (byte[])ip.getPixels();
      for (int y = 0; (y < height); y++)
         for (int x = 0; (x < width); x++, k++)
            image[k] = (double)(pixels[k] & 0xFF);
   } else if (ip instanceof ShortProcessor) {
      final short[] pixels = (short[])ip.getPixels();
      for (int y = 0; (y < height); y++)
         for (int x = 0; (x < width); x++, k++)
            if (pixels[k] < (short)0) image[k] = (double)pixels[k] + 65536.0F;
            else                      image[k] = (double)pixels[k];
   } else if (ip instanceof FloatProcessor) {
      final float[] pixels = (float[])ip.getPixels();
      for (int p = 0; p<height*width; p++)
         image[p]=pixels[p];
   }
}

static public void extractImage(final ImageProcessor ip, double image[][]) {
    int k=0;
    int height=ip.getHeight();
    int width =ip.getWidth ();
    if (ip instanceof ByteProcessor) {
      final byte[] pixels = (byte[])ip.getPixels();
      for (int y = 0; (y < height); y++)
         for (int x = 0; (x < width); x++, k++)
            image[y][x] = (double)(pixels[k] & 0xFF);
    } else if (ip instanceof ShortProcessor) {
      final short[] pixels = (short[])ip.getPixels();
      for (int y = 0; (y < height); y++)
         for (int x = 0; (x < width); x++, k++)
            if (pixels[k] < (short)0) image[y][x] = (double)pixels[k] + 65536.0F;
            else                      image[y][x] = (double)pixels[k];
    } else if (ip instanceof FloatProcessor) {
      final float[] pixels = (float[])ip.getPixels();
      for (int y = 0; (y < height); y++)
         for (int x = 0; (x < width); x++, k++)
             image[y][x]=pixels[k];
    }
}

/*------------------------------------------------------------------*/
/* Load landmarks from file. */
static public void loadPoints(String filename,
   Stack sourceStack, Stack targetStack) {
   Point sourcePoint;
   Point targetPoint;
   try {
      final FileReader fr = new FileReader(filename);
      final BufferedReader br = new BufferedReader(fr);
      String line;
      String index;
      String xSource;
      String ySource;
      String xTarget;
      String yTarget;
      int separatorIndex;
      int k = 1;
      if (!(line = br.readLine()).equals("Index\txSource\tySource\txTarget\tyTarget")) {
         fr.close();
         IJ.write("Line " + k + ": 'Index\txSource\tySource\txTarget\tyTarget'");
         return;
      }
      ++k;
      while ((line = br.readLine()) != null) {
         line = line.trim();
         separatorIndex = line.indexOf('\t');
         if (separatorIndex == -1) {
            fr.close();
            IJ.write("Line " + k
               + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
            return;
         }
         index = line.substring(0, separatorIndex);
         index = index.trim();
         line = line.substring(separatorIndex);
         line = line.trim();
         separatorIndex = line.indexOf('\t');
         if (separatorIndex == -1) {
            fr.close();
            IJ.write("Line " + k
               + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
            return;
         }
         xSource = line.substring(0, separatorIndex);
         xSource = xSource.trim();
         line = line.substring(separatorIndex);
         line = line.trim();
         separatorIndex = line.indexOf('\t');
         if (separatorIndex == -1) {
            fr.close();
            IJ.write("Line " + k
               + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
            return;
         }
         ySource = line.substring(0, separatorIndex);
         ySource = ySource.trim();
         line = line.substring(separatorIndex);
         line = line.trim();
         separatorIndex = line.indexOf('\t');
         if (separatorIndex == -1) {
            fr.close();
            IJ.write("Line " + k
               + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
            return;
         }
         xTarget = line.substring(0, separatorIndex);
         xTarget = xTarget.trim();
         yTarget = line.substring(separatorIndex);
         yTarget = yTarget.trim();
         sourcePoint = new Point(Integer.valueOf(xSource).intValue(),
            Integer.valueOf(ySource).intValue());
         sourceStack.push(sourcePoint);
         targetPoint = new Point(Integer.valueOf(xTarget).intValue(),
            Integer.valueOf(yTarget).intValue());
         targetStack.push(targetPoint);
      }
      fr.close();
   } catch (FileNotFoundException e) {
      IJ.error("File not found exception" + e);
      return;
   } catch (IOException e) {
      IJ.error("IOException exception" + e);
      return;
   } catch (NumberFormatException e) {
      IJ.error("Number format exception" + e);
      return;
   }
}

static public void loadTransformation(String filename,
   final double [][]cx, final double [][]cy) {
   try {
      final FileReader fr = new FileReader(filename);
      final BufferedReader br = new BufferedReader(fr);
      String line;

      // Read number of intervals
      line = br.readLine();
      int lineN=1;
      StringTokenizer st=new StringTokenizer(line,"=");
      if (st.countTokens()!=2) {
         fr.close();
         IJ.write("Line "+lineN+"+: Cannot read number of intervals");
         return; 
      }
      st.nextToken();
      int intervals=Integer.valueOf(st.nextToken()).intValue();

      // Skip next 2 lines
      line = br.readLine();
      line = br.readLine();
      lineN+=2;

      // Read the cx coefficients
      for (int i= 0; i<intervals+3; i++) {
          line = br.readLine(); lineN++;
          st=new StringTokenizer(line);
          if (st.countTokens()!=intervals+3) {
               fr.close();
               IJ.write("Line "+lineN+": Cannot read enough coefficients");
               return; 
          }
         for (int j=0; j<intervals+3; j++)
            cx[i][j]=Double.valueOf(st.nextToken()).doubleValue();
      }

      // Skip next 2 lines
      line = br.readLine();
      line = br.readLine();
      lineN+=2;

      // Read the cy coefficients
      for (int i= 0; i<intervals+3; i++) {
          line = br.readLine(); lineN++;
          st=new StringTokenizer(line);
          if (st.countTokens()!=intervals+3) {
               fr.close();
               IJ.write("Line "+lineN+": Cannot read enough coefficients");
               return; 
          }
          for (int j=0; j<intervals+3; j++)
             cy[i][j]=Double.valueOf(st.nextToken()).doubleValue();
      }
      fr.close();
   } catch (FileNotFoundException e) {
      IJ.error("File not found exception" + e);
      return;
   } catch (IOException e) {
      IJ.error("IOException exception" + e);
      return;
   } catch (NumberFormatException e) {
      IJ.error("Number format exception" + e);
      return;
   }
}

/*------------------------------------------------------------------*/
static public int numberOfIntervalsOfTransformation(String filename) {
   try {
      final FileReader fr = new FileReader(filename);
      final BufferedReader br = new BufferedReader(fr);
      String line;

      // Read number of intervals
      line = br.readLine();
      int lineN=1;
      StringTokenizer st=new StringTokenizer(line,"=");
      if (st.countTokens()!=2) {
         fr.close();
         IJ.write("Line "+lineN+"+: Cannot read number of intervals");
         return -1; 
      }
      st.nextToken();
      int intervals=Integer.valueOf(st.nextToken()).intValue();

      fr.close();
      return intervals;
   } catch (FileNotFoundException e) {
      IJ.error("File not found exception" + e);
      return -1;
   } catch (IOException e) {
      IJ.error("IOException exception" + e);
      return -1;
   } catch (NumberFormatException e) {
      IJ.error("Number format exception" + e);
      return -1;
   }
}

/*------------------------------------------------------------------*/
/* Plot a point in a canvas. */
static public void Point(double [][]canvas, int y, int x, double color) {
   if (y<0 || y>=canvas.length)    return;
   if (x<0 || x>=canvas[0].length) return;
   canvas[y][x]=color;
}

/*------------------------------------------------------------------*/
public static void printMatrix(
   final String    title,
   final double [][]array
) {
    int Ydim=array.length;
    int Xdim=array[0].length;

    System.out.println(title);
    for (int i=0; i<Ydim; i++) {
        for (int j=0; j<Xdim; j++)
           System.out.print(array[i][j]+" ");
        System.out.println();
    }
}

/*------------------------------------------------------------------*/
public static void showImage(
   final String    title,
   final double []  array,
   final int       Ydim,
   final int       Xdim
) {
   final FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
   int ij=0;
   for (int i=0; i<Ydim; i++)
       for (int j=0; j<Xdim; j++, ij++)
   	  fp.putPixelValue(j,i,array[ij]); 
   fp.resetMinAndMax();
   final ImagePlus      ip=new ImagePlus(title, fp);
   final ImageWindow    iw=new ImageWindow(ip);
   ip.updateImage();
}

/*------------------------------------------------------------------*/
public static void showImage(
   final String    title,
   final double [][]array
) {
   int Ydim=array.length;
   int Xdim=array[0].length;

   final FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
      for (int i=0; i<Ydim; i++)
          for (int j=0; j<Xdim; j++)
             fp.putPixelValue(j,i,array[i][j]);
   fp.resetMinAndMax();
   final ImagePlus      ip=new ImagePlus(title, fp);
   final ImageWindow    iw=new ImageWindow(ip);
   ip.updateImage();
}

} /* End of MiscTools class */

/*====================================================================
|   unwarpJPointAction
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJPointAction
   extends
      ImageCanvas
   implements
      KeyListener,
      MouseListener,
      MouseMotionListener

{ /* begin class unwarpJPointAction */

/*....................................................................
   Public variables
....................................................................*/

public static final int ADD_CROSS    = 0;
public static final int MOVE_CROSS   = 1;
public static final int REMOVE_CROSS = 2;
public static final int MASK         = 3;
public static final int INVERTMASK   = 4;
public static final int FILE         = 5;
public static final int STOP         = 7;
public static final int MAGNIFIER    = 11;

/*....................................................................
   Private variables
....................................................................*/

private ImagePlus                      mainImp;
private ImagePlus                      secondaryImp;
private unwarpJPointHandler mainPh;
private unwarpJPointHandler secondaryPh;
private unwarpJPointToolbar tb;
private unwarpJDialog       dialog;
private long                           mouseDownTime;

/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void keyPressed (
   final KeyEvent e
) {
    if (tb.getCurrentTool()==MASK || tb.getCurrentTool()==INVERTMASK) return;
   final Point p = mainPh.getPoint();
   if (p == null) return;
   final int x = p.x;
   final int y = p.y;
   switch (e.getKeyCode()) {
      case KeyEvent.VK_DELETE:
      case KeyEvent.VK_BACK_SPACE:
         mainPh.removePoint();
         secondaryPh.removePoint();
         updateAndDraw();
         break;
      case KeyEvent.VK_DOWN:
         mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
            mainImp.getWindow().getCanvas().screenY(y
            + (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())));
         mainImp.setRoi(mainPh);
         break;
      case KeyEvent.VK_LEFT:
         mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x
            - (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())),
            mainImp.getWindow().getCanvas().screenY(y));
         mainImp.setRoi(mainPh);
         break;
      case KeyEvent.VK_RIGHT:
         mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x
            + (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())),
            mainImp.getWindow().getCanvas().screenY(y));
         mainImp.setRoi(mainPh);
         break;
      case KeyEvent.VK_TAB:
         mainPh.nextPoint();
         secondaryPh.nextPoint();
         updateAndDraw();
         break;
      case KeyEvent.VK_UP:
         mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
            mainImp.getWindow().getCanvas().screenY(y
            - (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())));
         mainImp.setRoi(mainPh);
         break;
   }
} /* end keyPressed */

/*------------------------------------------------------------------*/
public void keyReleased (
   final KeyEvent e
) {
} /* end keyReleased */

/*------------------------------------------------------------------*/
public void keyTyped (
   final KeyEvent e
) {
} /* end keyTyped */

/*------------------------------------------------------------------*/
public void mouseClicked (
   final MouseEvent e
) {
} /* end mouseClicked */

/*------------------------------------------------------------------*/
public void mouseDragged (
   final MouseEvent e
) {
   final int x = e.getX();
   final int y = e.getY();
   if (tb.getCurrentTool() == MOVE_CROSS) {
      mainPh.movePoint(x, y);
      updateAndDraw();
   }
   mouseMoved(e);
} /* end mouseDragged */

/*------------------------------------------------------------------*/
public void mouseEntered (
   final MouseEvent e
) {
   WindowManager.setCurrentWindow(mainImp.getWindow());
   mainImp.getWindow().toFront();
   updateAndDraw();
} /* end mouseEntered */

/*------------------------------------------------------------------*/
public void mouseExited (
   final MouseEvent e
) {
   IJ.showStatus("");
} /* end mouseExited */

/*------------------------------------------------------------------*/
public void mouseMoved (
   final MouseEvent e
) {
   setControl();
   final int x = mainImp.getWindow().getCanvas().offScreenX(e.getX());
   final int y = mainImp.getWindow().getCanvas().offScreenY(e.getY());
   IJ.showStatus(mainImp.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end mouseMoved */

/*------------------------------------------------------------------*/
public void mousePressed (
   final MouseEvent e
) {
   if (dialog.isFinalActionLaunched()) return;
   int x = e.getX(),xp;
   int y = e.getY(),yp;
   int currentPoint;
   boolean doubleClick = (System.currentTimeMillis() - mouseDownTime) <= 250L;
   mouseDownTime = System.currentTimeMillis();
   switch (tb.getCurrentTool()) {
      case ADD_CROSS:
         xp=mainImp.getWindow().getCanvas().offScreenX(x);
         yp=mainImp.getWindow().getCanvas().offScreenY(y);
         mainPh.addPoint(xp,yp);
         xp = positionX(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenX(x));
         yp = positionY(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenY(y));
         secondaryPh.addPoint(xp, yp);
         updateAndDraw();
         break;
      case MOVE_CROSS:
         currentPoint = mainPh.findClosest(x, y);
         secondaryPh.setCurrentPoint(currentPoint);
         updateAndDraw();
         break;
      case REMOVE_CROSS:
         currentPoint = mainPh.findClosest(x, y);
         mainPh.removePoint(currentPoint);
         secondaryPh.removePoint(currentPoint);
         updateAndDraw();
         break;
      case MASK:
      case INVERTMASK:
          if (mainPh.canAddMaskPoints()) {
             if (!doubleClick) {
                if (dialog.isClearMaskSet()) {
                   mainPh.clearMask();
                   dialog.setClearMask(false);
                   dialog.ungrayImage(this);
                }
                x = positionX(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenX(x));
                               y = positionY(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenY(y));
                mainPh.addMaskPoint(x,y);
             } else 
                mainPh.closeMask(tb.getCurrentTool());
             updateAndDraw();
         } else {
             IJ.error("A mask cannot be manually assigned since the mask was already in the stack");
         }
          break;
      case MAGNIFIER:
         final int flags = e.getModifiers();
         if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) {
            mainImp.getWindow().getCanvas().zoomOut(x, y);
         }
         else {
            mainImp.getWindow().getCanvas().zoomIn(x, y);
         }
         break;
   }
} /* end mousePressed */

/*------------------------------------------------------------------*/
public void mouseReleased (
   final MouseEvent e
) {
} /* end mouseReleased */

/*------------------------------------------------------------------*/
public void setSecondaryPointHandler (
   final ImagePlus secondaryImp,
   final unwarpJPointHandler secondaryPh
) {
   this.secondaryImp = secondaryImp;
   this.secondaryPh = secondaryPh;
} /* end setSecondaryPointHandler */

/*------------------------------------------------------------------*/
public unwarpJPointAction (
   final ImagePlus imp,
   final unwarpJPointHandler ph,
   final unwarpJPointToolbar tb,
   final unwarpJDialog       dialog
) {
   super(imp);
   this.mainImp = imp;
   this.mainPh = ph;
   this.tb = tb;
   this.dialog = dialog;
} /* end unwarpJPointAction */

/*....................................................................
   Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private String getValueAsString (
   final int x,
   final int y
) {
   final Calibration cal = mainImp.getCalibration();
   final int[] v = mainImp.getPixel(x, y);
   final int mainImptype=mainImp.getType();
   if (mainImptype==mainImp.GRAY8 || mainImptype==mainImp.GRAY16) {
       final double cValue = cal.getCValue(v[0]);
       if (cValue==v[0]) {
          return(", value=" + v[0]);
       }
       else {
          return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
       }
   } else if (mainImptype==mainImp.GRAY32) {
            return(", value=" + Float.intBitsToFloat(v[0]));
   } else if (mainImptype==mainImp.COLOR_256) {
       return(", index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);
   } else if (mainImptype==mainImp.COLOR_RGB) {
       return(", value=" + v[0] + "," + v[1] + "," + v[2]);
   } else {
       return("");
   }
} /* end getValueAsString */

/*------------------------------------------------------------------*/
private int positionX (
   final ImagePlus imp1,
   final ImagePlus imp2,
   final int x
) {
   return((x * imp2.getWidth()) / imp1.getWidth());
} /* end PositionX */

/*------------------------------------------------------------------*/
private int positionY (
   final ImagePlus imp1,
   final ImagePlus imp2,
   final int y
) {
   return((y * imp2.getHeight()) / imp1.getHeight());
} /* end PositionY */

/*------------------------------------------------------------------*/
private void setControl (
) {
   switch (tb.getCurrentTool()) {
      case ADD_CROSS:
         mainImp.getWindow().getCanvas().setCursor(crosshairCursor);
         break;
      case FILE:
      case MAGNIFIER:
      case MOVE_CROSS:
      case REMOVE_CROSS:
      case MASK:
      case INVERTMASK:
      case STOP:
         mainImp.getWindow().getCanvas().setCursor(defaultCursor);
         break;
   }
} /* end setControl */

/*------------------------------------------------------------------*/
private void updateAndDraw (
) {
   mainImp.setRoi(mainPh);
   secondaryImp.setRoi(secondaryPh);
} /* end updateAndDraw */

} /* end class unwarpJPointAction */

/*====================================================================
|   unwarpJPointHandler
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJPointHandler
   extends
      Roi

{ /* begin class unwarpJPointHandler */

/*....................................................................
   Private variables
....................................................................*/

private static final int CROSS_HALFSIZE = 5;

// Colors
private static final int GAMUT       = 1024;
private final Color   spectrum[]     = new Color[GAMUT];
private final boolean usedColor[]    = new boolean[GAMUT];
private final Vector  listColors     = new Vector(0, 16);
private int           currentColor   = 0;

// List of crosses
private final Vector  listPoints     = new Vector(0, 16);
private int           currentPoint   = -1;
private int           numPoints      = 0;
private boolean       started        = false;

// List of points for the mask
private final Vector  listMaskPoints = new Vector(0,16);
private boolean       maskClosed     = false;

// Some useful references
private ImagePlus                      imp;
private unwarpJPointAction  pa;
private unwarpJPointToolbar tb;
private unwarpJMask         mask;
private unwarpJDialog       dialog;

/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void addMaskPoint (
   final int x,
   final int y
) {
   if (maskClosed) return;
   final Point p = new Point(x, y);
   listMaskPoints.addElement(p);
}

/*------------------------------------------------------------------*/
public void addPoint (
   final int x,
   final int y
) {
   if (numPoints < GAMUT) {
      final Point p = new Point(x, y);
      listPoints.addElement(p);
      if (!usedColor[currentColor]) {
         usedColor[currentColor] = true;
      }
      else {
         int k;
         for (k = 0; (k < GAMUT); k++) {
            currentColor++;
            currentColor &= GAMUT - 1;
            if (!usedColor[currentColor]) {
               break;
            }
         }
         if (GAMUT <= k) {
            throw new IllegalStateException("Unexpected lack of available colors");
         }
      }
      int stirredColor = 0;
      int c = currentColor;
      for (int k = 0; (k < (int)Math.round(Math.log((double)GAMUT) / Math.log(2.0))); k++) {
         stirredColor <<= 1;
         stirredColor |= (c & 1);
         c >>= 1;
      }
      listColors.addElement(new Integer(stirredColor));
      currentColor++;
      currentColor &= GAMUT - 1;
      currentPoint = numPoints;
      numPoints++;
   }
   else {
      IJ.error("Maximum number of points reached");
   }
} /* end addPoint */

/*------------------------------------------------------------------*/
/* False if the image is coming from a stack */
public boolean canAddMaskPoints() {
   return !mask.isFromStack();
}

/*------------------------------------------------------------------*/
public void clearMask () {
   // Clear mask information in this object
   listMaskPoints.removeAllElements();
   maskClosed=false;
   mask.clearMask();
}

/*------------------------------------------------------------------*/
public void closeMask (int tool) {
   listMaskPoints.addElement(listMaskPoints.elementAt(0));
   maskClosed=true;
   mask.setMaskPoints(listMaskPoints);
   mask.fillMask(tool);
   dialog.grayImage(this);
}

/*------------------------------------------------------------------*/
public void draw (
   final Graphics g
) {
    // Draw landmarks
   if (started) {
      final double mag = (double)ic.getMagnification();
      final int dx = (int)(mag / 2.0);
      final int dy = (int)(mag / 2.0);
      for (int k = 0; (k < numPoints); k++) {
         final Point p = (Point)listPoints.elementAt(k);
         g.setColor(spectrum[((Integer)listColors.elementAt(k)).intValue()]);
         if (k == currentPoint) {
            if (WindowManager.getCurrentImage() == imp) {
               g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                  ic.screenY(p.y - 1) + dy,
                  ic.screenX(p.x - 1) + dx,
                  ic.screenY(p.y - 1) + dy);
               g.drawLine(ic.screenX(p.x - 1) + dx,
                  ic.screenY(p.y - 1) + dy,
                  ic.screenX(p.x - 1) + dx,
                  ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
               g.drawLine(ic.screenX(p.x - 1) + dx,
                  ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
                  ic.screenX(p.x + 1) + dx,
                  ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
               g.drawLine(ic.screenX(p.x + 1) + dx,
                  ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
                  ic.screenX(p.x + 1) + dx,
                  ic.screenY(p.y - 1) + dy);
               g.drawLine(ic.screenX(p.x + 1) + dx,
                  ic.screenY(p.y - 1) + dy,
                  ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                  ic.screenY(p.y - 1) + dy);
               g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                  ic.screenY(p.y - 1) + dy,
                  ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                  ic.screenY(p.y + 1) + dy);
               g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                  ic.screenY(p.y + 1) + dy,
                  ic.screenX(p.x + 1) + dx,
                  ic.screenY(p.y + 1) + dy);
               g.drawLine(ic.screenX(p.x + 1) + dx,
                  ic.screenY(p.y + 1) + dy,
                  ic.screenX(p.x + 1) + dx,
                  ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
               g.drawLine(ic.screenX(p.x + 1) + dx,
                  ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
                  ic.screenX(p.x - 1) + dx,
                  ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
               g.drawLine(ic.screenX(p.x - 1) + dx,
                  ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
                  ic.screenX(p.x - 1) + dx,
                  ic.screenY(p.y + 1) + dy);
               g.drawLine(ic.screenX(p.x - 1) + dx,
                  ic.screenY(p.y + 1) + dy,
                  ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                  ic.screenY(p.y + 1) + dy);
               g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                  ic.screenY(p.y + 1) + dy,
                  ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                  ic.screenY(p.y - 1) + dy);
               if (1.0 < ic.getMagnification()) {
                  g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
                     ic.screenY(p.y) + dy,
                     ic.screenX(p.x + CROSS_HALFSIZE) + dx,
                     ic.screenY(p.y) + dy);
                  g.drawLine(ic.screenX(p.x) + dx,
                     ic.screenY(p.y - CROSS_HALFSIZE) + dy,
                     ic.screenX(p.x) + dx,
                     ic.screenY(p.y + CROSS_HALFSIZE) + dy);
               }
            }
            else {
               g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
                  ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
                  ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
                  ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
               g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
                  ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
                  ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
                  ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
            }
         }
         else {
            g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
               ic.screenY(p.y) + dy,
               ic.screenX(p.x + CROSS_HALFSIZE) + dx,
               ic.screenY(p.y) + dy);
            g.drawLine(ic.screenX(p.x) + dx,
               ic.screenY(p.y - CROSS_HALFSIZE) + dy,
               ic.screenX(p.x) + dx,
               ic.screenY(p.y + CROSS_HALFSIZE) + dy);
         }
      }
      if (updateFullWindow) {
         updateFullWindow = false;
         imp.draw();
      }
   }

   // Draw mask
   int numberMaskPoints=listMaskPoints.size();
   if (numberMaskPoints!=0) {
       final double mag = (double)ic.getMagnification();
       final int dx = (int)(mag / 2.0);
       final int dy = (int)(mag / 2.0);

       int CIRCLE_RADIUS=CROSS_HALFSIZE/2;
       int CIRCLE_DIAMETER=2*CIRCLE_RADIUS;
      for (int i=0; i<numberMaskPoints; i++) {
              final Point p = (Point)listMaskPoints.elementAt(i);
              g.setColor(Color.yellow);
              g.drawOval(ic.screenX(p.x)-CIRCLE_RADIUS+dx, ic.screenY(p.y)-CIRCLE_RADIUS+dy,
            CIRCLE_DIAMETER, CIRCLE_DIAMETER);
              if (i!=0) {
            Point previous_p=(Point)listMaskPoints.elementAt(i-1);
            g.drawLine(ic.screenX(p.x)+dx,ic.screenY(p.y)+dy,
                   ic.screenX(previous_p.x)+dx,ic.screenY(previous_p.y)+dy);
              }
      }
   }
} /* end draw */

/*------------------------------------------------------------------*/
public int findClosest (
   int x,
   int y
) {
   if (numPoints == 0) {
      return(currentPoint);
   }
   x = ic.offScreenX(x);
   y = ic.offScreenY(y);
   Point p = new Point((Point)listPoints.elementAt(currentPoint));
   double distance = (double)(x - p.x) * (double)(x - p.x)
      + (double)(y - p.y) * (double)(y - p.y);
   for (int k = 0; (k < numPoints); k++) {
      p = (Point)listPoints.elementAt(k);
      final double candidate = (double)(x - p.x) * (double)(x - p.x)
         + (double)(y - p.y) * (double)(y - p.y);
      if (candidate < distance) {
         distance = candidate;
         currentPoint = k;
      }
   }
   return(currentPoint);
} /* end findClosest */

/*------------------------------------------------------------------*/
public Point getPoint (
) {
   return((0 <= currentPoint) ? (Point)listPoints.elementAt(currentPoint) : (null));
} /* end getPoint */

/*------------------------------------------------------------------*/
public unwarpJPointAction getPointAction () {return pa;}

/*------------------------------------------------------------------*/
public int getCurrentPoint (
) {
   return(currentPoint);
} /* end getCurrentPoint */

/*------------------------------------------------------------------*/
public Vector getPoints (
) {
   return(listPoints);
} /* end getPoints */

/*------------------------------------------------------------------*/
public void killListeners (
) {
   final ImageWindow iw = imp.getWindow();
   final ImageCanvas ic = iw.getCanvas();
   ic.removeKeyListener(pa);
   ic.removeMouseListener(pa);
   ic.removeMouseMotionListener(pa);
   ic.addMouseMotionListener(ic);
   ic.addMouseListener(ic);
   ic.addKeyListener(IJ.getInstance());
} /* end killListeners */

/*------------------------------------------------------------------*/
public void movePoint (
   int x,
   int y
) {
   if (0 <= currentPoint) {
      x = ic.offScreenX(x);
      y = ic.offScreenY(y);
      x = (x < 0) ? (0) : (x);
      x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
      y = (y < 0) ? (0) : (y);
      y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
      listPoints.removeElementAt(currentPoint);
      final Point p = new Point(x, y);
      listPoints.insertElementAt(p, currentPoint);
   }
} /* end movePoint */

/*------------------------------------------------------------------*/
public void nextPoint (
) {
   currentPoint = (currentPoint == (numPoints - 1)) ? (0) : (currentPoint + 1);
} /* end nextPoint */

/*------------------------------------------------------------------*/
public void removePoint (
) {
   if (0 < numPoints) {
      listPoints.removeElementAt(currentPoint);
      usedColor[((Integer)listColors.elementAt(currentPoint)).intValue()] = false;
      listColors.removeElementAt(currentPoint);
      numPoints--;
   }
   currentPoint = numPoints - 1;
   if (currentPoint < 0) {
      tb.setTool(pa.ADD_CROSS);
   }
} /* end removePoint */

/*------------------------------------------------------------------*/
public void removePoint (
   final int k
) {
   if (0 < numPoints) {
      listPoints.removeElementAt(k);
      usedColor[((Integer)listColors.elementAt(k)).intValue()] = false;
      listColors.removeElementAt(k);
      numPoints--;
   }
   currentPoint = numPoints - 1;
   if (currentPoint < 0) {
      tb.setTool(pa.ADD_CROSS);
   }
} /* end removePoint */

/*------------------------------------------------------------------*/
public void removePoints (
) {
   listPoints.removeAllElements();
   listColors.removeAllElements();
   for (int k = 0; (k < GAMUT); k++) {
      usedColor[k] = false;
   }
   currentColor = 0;
   numPoints = 0;
   currentPoint = -1;
   tb.setTool(pa.ADD_CROSS);
   imp.setRoi(this);
} /* end removePoints */

/*------------------------------------------------------------------*/
public void setCurrentPoint (
   final int currentPoint
) {
   this.currentPoint = currentPoint;
} /* end setCurrentPoint */

/*------------------------------------------------------------------*/
public void setTestSourceSet (
   final int set
) {
   removePoints();
   switch(set) {
      case 1: // Deformed_Lena 1
         addPoint(11,11);
         addPoint(200,6);
         addPoint(197,204);
         addPoint(121,111);
         break;
      case 2: // Deformed_Lena 1
         addPoint(6,6);
         addPoint(202,7);
         addPoint(196,210);
         addPoint(10,214);
         addPoint(120,112);
         addPoint(68,20);
         addPoint(63,163);
         addPoint(186,68);
         break;
   }
} /* end setTestset */

/*------------------------------------------------------------------*/
public void setTestTargetSet (
   final int set
) {
   removePoints();
   switch(set) {
      case 1:
         addPoint(11,11);
         addPoint(185,15);
         addPoint(154,200);
         addPoint(123,92);
         break;
      case 2: // Deformed_Lena 1
         addPoint(6,6);
         addPoint(185,14);
         addPoint(154,200);
         addPoint(3,178);
         addPoint(121,93);
         addPoint(67,14);
         addPoint(52,141);
         addPoint(178,68);
         break;
   }
} /* end setTestset */

/*------------------------------------------------------------------*/
public void setSecondaryPointHandler (
   final ImagePlus secondaryImp,
   final unwarpJPointHandler secondaryPh
) {
   pa.setSecondaryPointHandler(secondaryImp, secondaryPh);
} /* end setSecondaryPointHandler */

/*------------------------------------------------------------------*/
/* Constructor with graphical capabilities */
public unwarpJPointHandler (
   final ImagePlus           imp,
   final unwarpJPointToolbar tb,
   final unwarpJMask         mask,
   final unwarpJDialog       dialog
) {
   super(0, 0, imp.getWidth(), imp.getHeight(), imp);
   this.imp = imp;
   this.tb = tb;
   this.dialog=dialog;
   pa = new unwarpJPointAction(imp, this, tb, dialog);
   final ImageWindow iw = imp.getWindow();
   final ImageCanvas ic = iw.getCanvas();
   iw.requestFocus();
   iw.removeKeyListener(IJ.getInstance());
   iw.addKeyListener(pa);
   ic.removeMouseMotionListener(ic);
   ic.removeMouseListener(ic);
   ic.removeKeyListener(IJ.getInstance());
   ic.addKeyListener(pa);
   ic.addMouseListener(pa);
   ic.addMouseMotionListener(pa);
   setSpectrum();
   started = true;

   this.mask=mask;
   clearMask();
} /* end unwarpJPointHandler */

/* Constructor without graphical capabilities */
public unwarpJPointHandler (
   final ImagePlus           imp
) {
   super(0, 0, imp.getWidth(), imp.getHeight(), imp);
   this.imp = imp;
   tb = null;
   dialog=null;
   pa = null;
   started = true;
   mask=null;
} /* end unwarpJPointHandler */

/*....................................................................
   Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void setSpectrum (
) {
   final int bound1 = GAMUT / 6;
   final int bound2 = GAMUT / 3;
   final int bound3 = GAMUT / 2;
   final int bound4 = (2 * GAMUT) / 3;
   final int bound5 = (5 * GAMUT) / 6;
   final int bound6 = GAMUT;
   final float gamutChunk1 = (float)bound1;
   final float gamutChunk2 = (float)(bound2 - bound1);
   final float gamutChunk3 = (float)(bound3 - bound2);
   final float gamutChunk4 = (float)(bound4 - bound3);
   final float gamutChunk5 = (float)(bound5 - bound4);
   final float gamutChunk6 = (float)(bound6 - bound5);
   int k = 0;
   do {
      spectrum[k] = new Color(1.0F, (float)k / gamutChunk1, 0.0F);
      usedColor[k] = false;
   } while (++k < bound1);
   do {
      spectrum[k] = new Color(1.0F - (float)(k - bound1) / gamutChunk2, 1.0F, 0.0F);
      usedColor[k] = false;
   } while (++k < bound2);
   do {
      spectrum[k] = new Color(0.0F, 1.0F, (float)(k - bound2) / gamutChunk3);
      usedColor[k] = false;
   } while (++k < bound3);
   do {
      spectrum[k] = new Color(0.0F, 1.0F - (float)(k - bound3) / gamutChunk4, 1.0F);
      usedColor[k] = false;
   } while (++k < bound4);
   do {
      spectrum[k] = new Color((float)(k - bound4) / gamutChunk5, 0.0F, 1.0F);
      usedColor[k] = false;
   } while (++k < bound5);
   do {
      spectrum[k] = new Color(1.0F, 0.0F, 1.0F - (float)(k - bound5) / gamutChunk6);
      usedColor[k] = false;
   } while (++k < bound6);
} /* end setSpectrum */

} /* end class unwarpJPointHandler */

/*====================================================================
|   unwarpJPointToolbar
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJPointToolbar
   extends
      Canvas
   implements
      MouseListener

{ /* begin class unwarpJPointToolbar */

/*....................................................................
   Private variables
....................................................................*/

private static final int NUM_TOOLS = 19;
private static final int SIZE      = 22;
private static final int OFFSET    = 3;

private static final Color gray       = Color.lightGray;
private static final Color brighter   = gray.brighter();
private static final Color darker     = gray.darker();
private static final Color evenDarker = darker.darker();

private final boolean[] down = new boolean[NUM_TOOLS];
private Graphics g;
private ImagePlus sourceImp;
private ImagePlus targetImp;
private Toolbar previousInstance;
private unwarpJPointHandler sourcePh;
private unwarpJPointHandler targetPh;
private unwarpJPointToolbar instance;
private long mouseDownTime;
private int currentTool = unwarpJPointAction.ADD_CROSS;
private int x;
private int y;
private int xOffset;
private int yOffset;
private unwarpJDialog dialog;

/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public int getCurrentTool (
) {
   return(currentTool);
} /* getCurrentTool */

/*------------------------------------------------------------------*/
public void mouseClicked (
   final MouseEvent e
) {
} /* end mouseClicked */

/*------------------------------------------------------------------*/
public void mouseEntered (
   final MouseEvent e
) {
} /* end mouseEntered */

/*------------------------------------------------------------------*/
public void mouseExited (
   final MouseEvent e
) {
} /* end mouseExited */

/*------------------------------------------------------------------*/
public void mousePressed (
   final MouseEvent e
) {
   final int x = e.getX();
   final int y = e.getY();
   int newTool = 0;
   for (int i = 0; (i < NUM_TOOLS); i++) {
      if (((i * SIZE) < x) && (x < (i * SIZE + SIZE))) {
         newTool = i;
      }
   }
   boolean doubleClick = ((newTool == getCurrentTool())
      && ((System.currentTimeMillis() - mouseDownTime) <= 500L)
      && (newTool == unwarpJPointAction.REMOVE_CROSS));
   mouseDownTime = System.currentTimeMillis();
   if (newTool==unwarpJPointAction.STOP && !dialog.isFinalActionLaunched())
      return;
   if (newTool!=unwarpJPointAction.STOP &&  dialog.isFinalActionLaunched())
      return;
   setTool(newTool);
   if (doubleClick) {
      unwarpJClearAll clearAllDialog = new unwarpJClearAll(IJ.getInstance(),
         sourceImp, targetImp, sourcePh, targetPh);
      GUI.center(clearAllDialog);
      clearAllDialog.setVisible(true);
      setTool(unwarpJPointAction.ADD_CROSS);
      clearAllDialog.dispose();
   }
   switch (newTool) {
      case unwarpJPointAction.FILE:
         unwarpJFile fileDialog = new unwarpJFile(IJ.getInstance(),
            sourceImp, targetImp, sourcePh, targetPh,dialog);
         GUI.center(fileDialog);
         fileDialog.setVisible(true);
         setTool(unwarpJPointAction.ADD_CROSS);
         fileDialog.dispose();
         break;
      case unwarpJPointAction.MASK:
      case unwarpJPointAction.INVERTMASK:
          dialog.setClearMask(true);
          break;
      case unwarpJPointAction.STOP:
          dialog.setStopRegistration();
          break;
   }
} /* mousePressed */

/*------------------------------------------------------------------*/
public void mouseReleased (
   final MouseEvent e
) {
} /* end mouseReleased */

/*------------------------------------------------------------------*/
public void paint (
   final Graphics g
) {
   for (int i = 0; (i < NUM_TOOLS); i++) {
      drawButton(g, i);
   }
} /* paint */

/*------------------------------------------------------------------*/
public void restorePreviousToolbar (
) {
   final Container container = instance.getParent();
   final Component[] component = container.getComponents();
   for (int i = 0; (i < component.length); i++) {
      if (component[i] == instance) {
         container.remove(instance);
         container.add(previousInstance, i);
         container.validate();
         break;
      }
   }
} /* end restorePreviousToolbar */

/*------------------------------------------------------------------*/
public void setAllUp () {
   for (int i=0; i<NUM_TOOLS; i++) down[i]=false;
}

/*------------------------------------------------------------------*/
public void setSource (
   final ImagePlus sourceImp,
   final unwarpJPointHandler sourcePh
) {
   this.sourceImp = sourceImp;
   this.sourcePh = sourcePh;
} /* end setSource */

/*------------------------------------------------------------------*/
public void setTarget (
   final ImagePlus targetImp,
   final unwarpJPointHandler targetPh
) {
   this.targetImp = targetImp;
   this.targetPh = targetPh;
} /* end setTarget */

/*------------------------------------------------------------------*/
public void setTool (
   final int tool
) {
   if (tool == currentTool) {
      return;
   }
   down[tool] = true;
   down[currentTool] = false;
   Graphics g = this.getGraphics();
   drawButton(g, currentTool);
   drawButton(g, tool);
   g.dispose();
   showMessage(tool);
   currentTool = tool;
} /* end setTool */

/*------------------------------------------------------------------*/
public unwarpJPointToolbar (
   final Toolbar previousToolbar,
   final unwarpJDialog dialog
) {
   previousInstance = previousToolbar;
   this.dialog      = dialog;
   instance = this;
   final Container container = previousToolbar.getParent();
   final Component[] component = container.getComponents();
   for (int i = 0; (i < component.length); i++) {
      if (component[i] == previousToolbar) {
         container.remove(previousToolbar);
         container.add(this, i);
         break;
      }
   }
   resetButtons();
   down[currentTool] = true;
   setTool(currentTool);
   setForeground(evenDarker);
   setBackground(gray);
   addMouseListener(this);
   container.validate();
} /* end unwarpJPointToolbar */

/*....................................................................
   Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void d (
   int x,
   int y
) {
   x += xOffset;
   y += yOffset;
   g.drawLine(this.x, this.y, x, y);
   this.x = x;
   this.y = y;
} /* end d */

/*------------------------------------------------------------------*/
private void drawButton (
   final Graphics g,
   final int tool
) {
   fill3DRect(g, tool * SIZE + 1, 1, SIZE, SIZE - 1, !down[tool]);
   if (tool==unwarpJPointAction.STOP && !dialog.isFinalActionLaunched())
      return;
   if (tool!=unwarpJPointAction.STOP &&  dialog.isFinalActionLaunched())
      return;
   g.setColor(Color.black);
   int x = tool * SIZE + OFFSET;
   int y = OFFSET;
   if (down[tool]) {
      x++;
      y++;
   }
   this.g = g;

    // Plygon for the mask
   int px[]=new int[5]; px[0]=x+4;px[1]=x+ 4;px[2]=x+14;px[3]=x+ 9;px[4]=x+14;
   int py[]=new int[5]; py[0]=y+3;py[1]=y+13;py[2]=y+13;py[3]=y+ 8;py[4]=y+ 3;

   switch (tool) {
      case unwarpJPointAction.ADD_CROSS:
         xOffset = x;
         yOffset = y;
         m(7, 0);
         d(7, 1);
         m(6, 2);
         d(6, 3);
         m(8, 2);
         d(8, 3);
         m(5, 4);
         d(5, 5);
         m(9, 4);
         d(9, 5);
         m(4, 6);
         d(4, 8);
         m(10, 6);
         d(10, 8);
         m(5, 9);
         d(5, 14);
         m(9, 9);
         d(9, 14);
         m(7, 4);
         d(7, 6);
         m(7, 8);
         d(7, 8);
         m(4, 11);
         d(10, 11);
         g.fillRect(x + 6, y + 12, 3, 3);
         m(11, 13);
         d(15, 13);
         m(13, 11);
         d(13, 15);
         break;
      case unwarpJPointAction.FILE:
         xOffset = x;
         yOffset = y;
         m(3, 1);
         d(9, 1);
         d(9, 4);
         d(12, 4);
         d(12, 14);
         d(3, 14);
         d(3, 1);
         m(10, 2);
         d(11, 3);
         m(5, 4);
         d(7, 4);
         m(5, 6);
         d(10, 6);
         m(5, 8);
         d(10, 8);
         m(5, 10);
         d(10, 10);
         m(5, 12);
         d(10, 12);
         break;
      case unwarpJPointAction.MAGNIFIER:
         xOffset = x + 2;
         yOffset = y + 2;
         m(3, 0);
         d(3, 0);
         d(5, 0);
         d(8, 3);
         d(8, 5);
         d(7, 6);
         d(7, 7);
         d(6, 7);
         d(5, 8);
         d(3, 8);
         d(0, 5);
         d(0, 3);
         d(3, 0);
         m(8, 8);
         d(9, 8);
         d(13, 12);
         d(13, 13);
         d(12, 13);
         d(8, 9);
         d(8, 8);
         break;
      case unwarpJPointAction.MOVE_CROSS:
         xOffset = x;
         yOffset = y;
         m(1, 1);
         d(1, 10);
         m(2, 2);
         d(2, 9);
         m(3, 3);
         d(3, 8);
         m(4, 4);
         d(4, 7);
         m(5, 5);
         d(5, 7);
         m(6, 6);
         d(6, 7);
         m(7, 7);
         d(7, 7);
         m(11, 5);
         d(11, 6);
         m(10, 7);
         d(10, 8);
         m(12, 7);
         d(12, 8);
         m(9, 9);
         d(9, 11);
         m(13, 9);
         d(13, 11);
         m(10, 12);
         d(10, 15);
         m(12, 12);
         d(12, 15);
         m(11, 9);
         d(11, 10);
         m(11, 13);
         d(11, 15);
         m(9, 13);
         d(13, 13);
         break;
      case unwarpJPointAction.REMOVE_CROSS:
         xOffset = x;
         yOffset = y;
         m(7, 0);
         d(7, 1);
         m(6, 2);
         d(6, 3);
         m(8, 2);
         d(8, 3);
         m(5, 4);
         d(5, 5);
         m(9, 4);
         d(9, 5);
         m(4, 6);
         d(4, 8);
         m(10, 6);
         d(10, 8);
         m(5, 9);
         d(5, 14);
         m(9, 9);
         d(9, 14);
         m(7, 4);
         d(7, 6);
         m(7, 8);
         d(7, 8);
         m(4, 11);
         d(10, 11);
         g.fillRect(x + 6, y + 12, 3, 3);
         m(11, 13);
         d(15, 13);
         break;
      case unwarpJPointAction.MASK:
         xOffset = x;
         yOffset = y;
         g.fillPolygon(px,py,5);
         break;
      case unwarpJPointAction.INVERTMASK:
         xOffset = x;
         yOffset = y;
         g.fillRect(x + 1, y + 1, 15, 15);
            g.setColor(gray);
         g.fillPolygon(px,py,5);
         g.setColor(Color.black);
         break;
      case unwarpJPointAction.STOP:
         xOffset = x;
         yOffset = y;
            // Octogon
         m( 1,  5);
         d( 1, 11);
         d( 5, 15);
         d(11, 15);
         d(15, 11);
         d(15,  5);
         d(11,  1);
         d( 5,  1);
         d( 1,  5);
         // S
         m( 5,  6);
         d( 3,  6);
         d( 3,  8);
         d( 5,  8);
         d( 5, 10);
         d( 3, 10);
         // T
         m( 6,  6);
         d( 6,  8);
         m( 7,  6);
         d( 7, 10);
         // O
         m(11,  6);
         d( 9,  6);
         d( 9, 10);
         d(11, 10);
         d(11,  6);
         // P
         m(12, 10);
         d(12,  6);
         d(14,  6);
         d(14,  8);
         d(12,  8);
         break;
   }
} /* end drawButton */

/*------------------------------------------------------------------*/
private void fill3DRect (
   final Graphics g,
   final int x,
   final int y,
   final int width,
   final int height,
   final boolean raised
) {
   if (raised) {
      g.setColor(gray);
   }
   else {
      g.setColor(darker);
   }
   g.fillRect(x + 1, y + 1, width - 2, height - 2);
   g.setColor((raised) ? (brighter) : (evenDarker));
   g.drawLine(x, y, x, y + height - 1);
   g.drawLine(x + 1, y, x + width - 2, y);
   g.setColor((raised) ? (evenDarker) : (brighter));
   g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
   g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
} /* end fill3DRect */

/*------------------------------------------------------------------*/
private void m (
   final int x,
   final int y
) {
   this.x = xOffset + x;
   this.y = yOffset + y;
} /* end m */

/*------------------------------------------------------------------*/
private void resetButtons (
) {
   for (int i = 0; (i < NUM_TOOLS); i++) {
      down[i] = false;
   }
} /* end resetButtons */

/*------------------------------------------------------------------*/
private void showMessage (
   final int tool
) {
   switch (tool) {
      case unwarpJPointAction.ADD_CROSS:
         IJ.showStatus("Add crosses");
         return;
      case unwarpJPointAction.FILE:
         IJ.showStatus("Export/import list of points");
         return;
      case unwarpJPointAction.MAGNIFIER:
         IJ.showStatus("Magnifying glass");
         return;
      case unwarpJPointAction.MOVE_CROSS:
         IJ.showStatus("Move crosses");
         return;
      case unwarpJPointAction.REMOVE_CROSS:
         IJ.showStatus("Remove crosses");
         return;
      case unwarpJPointAction.MASK:
         IJ.showStatus("Draw a mask");
         return;
      case unwarpJPointAction.STOP:
         IJ.showStatus("Stop registration");
         return;
      default:
         IJ.showStatus("Undefined operation");
         return;
   }
} /* end showMessage */

} /* end class unwarpJPointToolbar */

/*====================================================================
|   unwarpJProgressBar
\===================================================================*/

/*********************************************************************
 * This class implements the interactions when dealing with ImageJ's
 * progress bar.
 ********************************************************************/
class unwarpJProgressBar

{ /* begin class unwarpJProgressBar */

/*....................................................................
   Private variables
....................................................................*/

/*********************************************************************
 * Same time constant than in ImageJ version 1.22
 ********************************************************************/
private static final long TIME_QUANTUM = 50L;

private static volatile long lastTime = System.currentTimeMillis();
private static volatile int completed = 0;
private static volatile int workload = 0;

/*....................................................................
   Public methods
....................................................................*/

/*********************************************************************
 * Extend the amount of work to perform by <code>batch</code>.
 *
 * @param batch Additional amount of work that need be performed.
 ********************************************************************/
public static synchronized void addWorkload (
   final int batch
) {
   workload += batch;
} /* end addWorkload */

/*********************************************************************
 * Erase the progress bar and cancel pending operations.
 ********************************************************************/
public static synchronized void resetProgressBar (
) {
   final long timeStamp = System.currentTimeMillis();
   if ((timeStamp - lastTime) < TIME_QUANTUM) {
      try {
         Thread.sleep(TIME_QUANTUM - timeStamp + lastTime);
      } catch (InterruptedException e) {
         IJ.error("Unexpected interruption exception" + e);
      }
   }
   lastTime = timeStamp;
   completed = 0;
   workload = 0;
   IJ.showProgress(1.0);
} /* end resetProgressBar */

/*********************************************************************
 * Perform <code>stride</code> operations at once.
 *
 * @param stride Amount of work that is skipped.
 ********************************************************************/
public static synchronized void skipProgressBar (
   final int stride
) {
   completed += stride - 1;
   stepProgressBar();
} /* end skipProgressBar */

/*********************************************************************
 * Perform <code>1</code> operation unit.
 ********************************************************************/
public static synchronized void stepProgressBar (
) {
   final long timeStamp = System.currentTimeMillis();
   completed = completed + 1;
   if ((TIME_QUANTUM <= (timeStamp - lastTime)) | (completed == workload)) {
      lastTime = timeStamp;
      IJ.showProgress((double)completed / (double)workload);
   }
} /* end stepProgressBar */

/*********************************************************************
 * Acknowledge that <code>batch</code> work has been performed.
 *
 * @param batch Completed amount of work.
 ********************************************************************/
public static synchronized void workloadDone (
   final int batch
) {
   workload -= batch;
   completed -= batch;
} /* end workloadDone */

} /* end class unwarpJProgressBar */

/*====================================================================
|   unwarpJTransformation
\===================================================================*/

/*------------------------------------------------------------------*/
class unwarpJTransformation

{ /* begin class unwarpJTransformation */

/*....................................................................
   Private variables
....................................................................*/

private final double FLT_EPSILON = (double)Float.intBitsToFloat((int)0x33FFFFFF);
private final boolean PYRAMID  = true;
private final boolean ORIGINAL = false;
private final int transformationSplineDegree=3;

// Some useful references
private ImagePlus           output_ip;
private unwarpJDialog       dialog;

// Images
private ImagePlus           sourceImp;
private ImagePlus           targetImp;
private unwarpJImageModel   source;
private unwarpJImageModel   target;

// Landmarks
private unwarpJPointHandler sourcePh;
private unwarpJPointHandler targetPh;

// Masks for the images
private unwarpJMask sourceMsk;
private unwarpJMask targetMsk;

// Image size
private int     sourceHeight;
private int     sourceWidth;
private int     targetHeight;
private int     targetWidth;
private int     targetCurrentHeight;
private int     targetCurrentWidth;
private double  factorHeight;
private double  factorWidth;

// Transformation parameters
private int     min_scale_deformation;
private int     max_scale_deformation;
private int     min_scale_image;
private int     outputLevel;
private boolean showMarquardtOptim;
private double  divWeight;
private double  curlWeight;
private double  landmarkWeight;
private double  imageWeight;
private double  stopThreshold;
private int     accurate_mode;
private boolean saveTransf;
private String  fn_tnf;

// Transformation estimate
private int     intervals;
private double  [][]cx;
private double  [][]cy;
private double  [][]transformation_x;
private double  [][]transformation_y;
private unwarpJImageModel swx;
private unwarpJImageModel swy;

// Regularization temporary variables
private double  [][]P11;
private double  [][]P22;
private double  [][]P12;


/*....................................................................
   Public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void doRegistration (
) {
    // This function can only be applied with splines of an odd order

    // Bring into consideration the image/coefficients at the smallest scale
    source.popFromPyramid();
    target.popFromPyramid();
    targetCurrentHeight = target.getCurrentHeight();
    targetCurrentWidth  = target.getCurrentWidth();
    factorHeight        = target.getFactorHeight();
    factorWidth         = target.getFactorWidth();

    // Ask memory for the transformation coefficients
    intervals=(int)Math.pow(2,min_scale_deformation);
    cx = new double[intervals+3][intervals+3];
    cy = new double[intervals+3][intervals+3];

    // Build matrices for computing the regularization
    buildRegularizationTemporary(intervals);

   // Ask for memory for the residues
   final int K;
   if (targetPh!=null) K=targetPh.getPoints().size();
   else                K=0;
   double [] dx=new double[K];
   double [] dy=new double[K];
   computeInitialResidues(dx,dy);

   // Compute the affine transformation from the target to the source coordinates
   // Notice that this matrix is independent of the scale, but the residues are not
   double[][] affineMatrix = null;
   if (landmarkWeight==0) affineMatrix=computeAffineMatrix();
   else {
      affineMatrix=new double[2][3];
       affineMatrix[0][0]=affineMatrix[1][1]=1;
      affineMatrix[0][1]=affineMatrix[0][2]=0;
      affineMatrix[1][0]=affineMatrix[1][2]=0;
   }

   // Incorporate the affine transformation into the spline coefficient
   for (int i= 0; i<intervals + 3; i++) {
      final double v = (double)((i - 1) * (targetCurrentHeight - 1)) / (double)intervals;
      final double xv = affineMatrix[0][2] + affineMatrix[0][1] * v;
      final double yv = affineMatrix[1][2] + affineMatrix[1][1] * v;
      for (int j = 0; j < intervals + 3; j++) {
         final double u = (double)((j - 1) * (targetCurrentWidth - 1)) / (double)intervals;
         cx[i][j] = xv + affineMatrix[0][0] * u;
         cy[i][j] = yv + affineMatrix[1][0] * u;
      }
   }

    // Now refine with the different scales
    int state;   // state=-1 --> Finish
                 // state= 0 --> Increase deformation detail
                 // state= 1 --> Increase image detail
                 // state= 2 --> Do nothing until the finest image scale
    if (min_scale_deformation==max_scale_deformation) state=1;
    else                                              state=0;
    int s=min_scale_deformation;
    int step=0;
    computeTotalWorkload();
    while (state!=-1) {
        int currentDepth=target.getCurrentDepth();

        // Update the deformation coefficients only in states 0 and 1
        if (state==0 || state==1) {
           // Update the deformation coefficients with the error of the landmarks
           // The following conditional is now useless but it is there to allow
           // easy changes like applying the landmarks only in the coarsest deformation
           if (s>=min_scale_deformation) {
             // Number of intervals at this scale and ask for memory
             intervals=(int) Math.pow(2,s);
               final double[][] newcx = new double[intervals+3][intervals+3];
             final double[][] newcy = new double[intervals+3][intervals+3];

            // Compute the residues before correcting at this scale
            computeScaleResidues(intervals, cx, cy, dx, dy);

            // Compute the coefficients at this scale
            boolean underconstrained=true;
            if (divWeight==0 && curlWeight==0)
               underconstrained=
                  computeCoefficientsScale(intervals, dx, dy, newcx, newcy);
            else
               underconstrained=
                  computeCoefficientsScaleWithRegularization(
                     intervals, dx, dy, newcx, newcy);

            // Incorporate information from the previous scale
            if (!underconstrained || (step==0 && landmarkWeight!=0)) {
                 for (int i=0; i<intervals+3; i++)
                     for (int j=0; j<intervals+3; j++) {
                        cx[i][j]+=newcx[i][j];
                        cy[i][j]+=newcy[i][j];
                  }
             }
          }

           // Optimize deformation coefficients
           if (imageWeight!=0)
              optimizeCoeffs(intervals,stopThreshold,cx,cy);
       }

        // Prepare for next iteration
        step++;
        switch (state) {
           case 0:
              // Finer details in the deformation
              if (s<max_scale_deformation) {
                 cx=propagateCoeffsToNextLevel(intervals,cx,1);
                cy=propagateCoeffsToNextLevel(intervals,cy,1);
                s++;
                intervals*=2;

                 // Prepare matrices for the regularization term
                  buildRegularizationTemporary(intervals);

                if (currentDepth>min_scale_image) state=1;
                else                              state=0;
             } else
                if (currentDepth>min_scale_image) state=1;
                else                              state=2;
             break;
           case 1: // Finer details in the image, go on  optimizing
           case 2: // Finer details in the image, do not optimize
              // Compute next state
              if (state==1) {
                 if      (s==max_scale_deformation && currentDepth==min_scale_image) state=2;
                 else if (s==max_scale_deformation)                                  state=1;
                 else                                                                state=0;
              } else if (state==2) {
                 if (currentDepth==0) state=-1;
                 else                 state= 2;
              }

              // Pop another image and prepare the deformation
              if (currentDepth!=0) {
                 double oldTargetCurrentHeight=targetCurrentHeight;
                 double oldTargetCurrentWidth =targetCurrentWidth;
                 source.popFromPyramid();
                 target.popFromPyramid();
                targetCurrentHeight = target.getCurrentHeight();
                 targetCurrentWidth  = target.getCurrentWidth();
                 factorHeight        = target.getFactorHeight();
                 factorWidth         = target.getFactorWidth();

                 // Adapt the transformation to the new image size
                 double factorY=(targetCurrentHeight-1)/(oldTargetCurrentHeight-1);
                 double factorX=(targetCurrentWidth -1)/(oldTargetCurrentWidth -1);
                 for (int i=0; i<intervals+3; i++)
                   for (int j=0; j<intervals+3; j++) {
                       cx[i][j]*=factorX;
                       cy[i][j]*=factorY;
                     }

                 // Prepare matrices for the regularization term
                  buildRegularizationTemporary(intervals);
              }
              break;
        }

        // In accurate_mode reduce the stopping threshold for the last iteration
        if ((state==0 || state==1) && s==max_scale_deformation && 
            currentDepth==min_scale_image+1 && accurate_mode==1)
            stopThreshold/=10;
   }

   // Show results
   showTransformation(intervals,cx,cy);
   if (saveTransf) saveTransformation(intervals,cx,cy);
} /* end doMultiresolutionElasticTransformation */

/*--------------------------------------------------------------------------*/
public double evaluateImageSimilarity() {
   int int3=intervals+3;
   int halfM=int3*int3;
   int M=halfM*2;

   double   []x            = new double   [M];
   double   []grad         = new double   [M];

   for (int i= 0, p=0; i<intervals + 3; i++)
       for (int j = 0; j < intervals + 3; j++, p++) {
         x[      p]=cx[i][j];
         x[halfM+p]=cy[i][j];
      }

   if (swx==null) {
      swx=new unwarpJImageModel(cx);
      swy=new unwarpJImageModel(cy);
      swx.precomputed_prepareForInterpolation(
         target.getCurrentHeight(), target.getCurrentWidth(), intervals);
      swy.precomputed_prepareForInterpolation(
         target.getCurrentHeight(), target.getCurrentWidth(), intervals);
   }

   if (swx.precomputed_getWidth()!=target.getCurrentWidth()) {
      swx.precomputed_prepareForInterpolation(
         target.getCurrentHeight(), target.getCurrentWidth(), intervals);
      swy.precomputed_prepareForInterpolation(
         target.getCurrentHeight(), target.getCurrentWidth(), intervals);
   }
   return evaluateSimilarity(x,intervals,grad,true,false);
}

/*------------------------------------------------------------------*/
public void getDeformation(
    final double [][]transformation_x,
    final double [][]transformation_y) {
    computeDeformation(intervals,cx,cy,
       transformation_x,transformation_y);
}

/*------------------------------------------------------------------*/
public unwarpJTransformation (
   final ImagePlus                    sourceImp,
   final ImagePlus                    targetImp,
   final unwarpJImageModel source,
   final unwarpJImageModel target,
   final unwarpJPointHandler sourcePh,
   final unwarpJPointHandler targetPh,
   final unwarpJMask sourceMsk,
   final unwarpJMask targetMsk,
   final int min_scale_deformation,
   final int max_scale_deformation,
   final int min_scale_image,
   final double divWeight,
   final double curlWeight,
   final double landmarkWeight,
   final double imageWeight,
   final double stopThreshold,
   final int outputLevel,
   final boolean showMarquardtOptim,
   final int accurate_mode,
   final boolean saveTransf,
   final String fn_tnf,
   final ImagePlus output_ip,
   final unwarpJDialog dialog
) {
   this.sourceImp	      = sourceImp;
   this.targetImp	      = targetImp;
   this.source                = source;
   this.target                = target;
   this.sourcePh              = sourcePh;
   this.targetPh              = targetPh;
   this.sourceMsk             = sourceMsk;
   this.targetMsk             = targetMsk;
   this.min_scale_deformation = min_scale_deformation;
   this.max_scale_deformation = max_scale_deformation;
   this.min_scale_image       = min_scale_image;
   this.divWeight             = divWeight;
   this.curlWeight            = curlWeight;
   this.landmarkWeight        = landmarkWeight;
   this.imageWeight           = imageWeight;
   this.stopThreshold         = stopThreshold;
   this.outputLevel           = outputLevel;
   this.showMarquardtOptim    = showMarquardtOptim;
   this.accurate_mode         = accurate_mode;
   this.saveTransf            = saveTransf;
   this.fn_tnf                = fn_tnf;
   this.output_ip             = output_ip;
   this.dialog                = dialog;
   sourceWidth                = source.getWidth();
   sourceHeight               = source.getHeight();
   targetWidth                = target.getWidth();
   targetHeight               = target.getHeight();
} /* end unwarpJTransformation */

/*------------------------------------------------------------------*/
public void transform(double u, double v, double []xyF) {
   final double tu = (u * intervals) / (double)(target.getCurrentWidth () - 1) + 1.0F;
   final double tv = (v * intervals) / (double)(target.getCurrentHeight() - 1) + 1.0F;

   final boolean ORIGINAL=false;
   swx.prepareForInterpolation(tu,tv,ORIGINAL); xyF[0]=swx.interpolateI();
   swy.prepareForInterpolation(tu,tv,ORIGINAL); xyF[1]=swy.interpolateI();
}

/*....................................................................
   Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void build_Matrix_B(
    int intervals,    // Intervals in the deformation
    int K,            // Number of landmarks
    double [][]B      // System matrix of the landmark interpolation
) {
   Vector targetVector = null;
   if (targetPh!=null) targetVector=targetPh.getPoints();
   for (int k = 0; k<K; k++) {
      final Point targetPoint = (Point)targetVector.elementAt(k);
       double x=factorWidth *(double)targetPoint.x;
      double y=factorHeight*(double)targetPoint.y;
      final double[] bx = xWeight(x, intervals, true);
      final double[] by = yWeight(y, intervals, true);
      for (int i=0; i<intervals+3; i++)
         for (int j=0; j<intervals+3; j++)
            B[k][(intervals+3)*i+j] = by[i] * bx[j];
   }
}

/*------------------------------------------------------------------*/
private void build_Matrix_Rq1q2(
    int intervals,
    double weight,
    int q1, int q2,
    double [][]R
){build_Matrix_Rq1q2q3q4(intervals,weight,q1,q2,q1,q2,R);}

private void build_Matrix_Rq1q2q3q4(
    int intervals,
    double weight,
    int q1, int q2, int q3, int q4,
    double [][]R
){
   /* Let's define alpha_q as the q-th derivative of a B-Spline

                     q   n
                   d    B (x)
      alpha_q(x)= --------------
                         q
                       dx

      eta_q1q2(x,s1,s2)=integral_0^Xdim alpha_q1(x/h-s1) alpha_q2(x/h-s2)

   */
   double [][]etaq1q3=new double[16][16];
   int Ydim=target.getCurrentHeight();
   int Xdim=target.getCurrentWidth();

   build_Matrix_R_geteta(etaq1q3,q1,q3,Xdim,intervals);

   double [][]etaq2q4=null;
   if (q2!=q1 || q4!=q3 || Ydim!=Xdim) {
      etaq2q4=new double[16][16];
      build_Matrix_R_geteta(etaq2q4,q2,q4,Ydim,intervals);
   } else etaq2q4=etaq1q3;

   int M=intervals+1;
   int Mp=intervals+3;
   for (int l=-1; l<=M; l++)
      for (int k=-1; k<=M; k++)
         for (int n=-1; n<=M; n++)
            for (int m=-1; m<=M; m++) {
               int []ip=new int[2];
               int []jp=new int[2];
               boolean valid_i=build_Matrix_R_getetaindex(l,n,intervals,ip);
               boolean valid_j=build_Matrix_R_getetaindex(k,m,intervals,jp);
               if (valid_i && valid_j) {
                  int mn=(n+1)*Mp+(m+1);
                  int kl=(l+1)*Mp+(k+1);
                  R[kl][mn]+=weight*etaq1q3[jp[0]][jp[1]]*etaq2q4[ip[0]][ip[1]];
               }
            }
}

/*------------------------------------------------------------------*/
private double build_Matrix_R_computeIntegral_aa(
   double x0,
   double xF,
   double s1,
   double s2,
   double h,
   int    q1,
   int    q2
) {
// Computes the following integral
//
//           xF d^q1      3  x        d^q2    3  x
//  integral    -----   B  (--- - s1) ----- B  (--- - s2) dx
//           x0 dx^q1        h        dx^q2      h

   // Form the spline coefficients
   double [][]C=new double [3][3];
   int    [][]d=new int    [3][3];
   double [][]s=new double [3][3];
   C[0][0]= 1  ; C[0][1]= 0  ; C[0][2]= 0  ;
   C[1][0]= 1  ; C[1][1]=-1  ; C[1][2]= 0  ;
   C[2][0]= 1  ; C[2][1]=-2  ; C[2][2]= 1  ; 
   d[0][0]= 3  ; d[0][1]= 0  ; d[0][2]= 0  ;
   d[1][0]= 2  ; d[1][1]= 2  ; d[1][2]= 0  ;
   d[2][0]= 1  ; d[2][1]= 1  ; d[2][2]= 1  ; 
   s[0][0]= 0  ; s[0][1]= 0  ; s[0][2]= 0  ;
   s[1][0]=-0.5; s[1][1]= 0.5; s[1][2]= 0  ;
   s[2][0]= 1  ; s[2][1]= 0  ; s[2][2]=-1  ; 

   // Compute the integral
   double integral=0;
   for (int k=0; k<3; k++) {
       double ck=C[q1][k]; if (ck==0) continue;
       for (int l=0; l<3; l++) {
           double cl=C[q2][l]; if (cl==0) continue;
           integral+=ck*cl*build_matrix_R_computeIntegral_BB(
              x0,xF,s1+s[q1][k],s2+s[q2][l],h,d[q1][k],d[q2][l]);
       }
    }
    return integral;
}

/*------------------------------------------------------------------*/
private double build_matrix_R_computeIntegral_BB(
   double x0,
   double xF,
   double s1,
   double s2,
   double h,
   int    n1,
   int    n2
) {
// Computes the following integral
//
//           xF   n1  x          n2  x
//  integral     B  (--- - s1)  B  (--- - s2) dx
//           x0       h              h

   // Change the variable so that the h disappears
   // X=x/h
   double xFp=xF/h;
   double x0p=x0/h;

   // Form the spline coefficients
   double []c1=new double [n1+2];
   double fact_n1=1; for (int k=2; k<=n1; k++) fact_n1*=k;
   double sign=1; 
   for (int k=0; k<=n1+1; k++, sign*=-1)
       c1[k]=sign*unwarpJMathTools.nchoosek(n1+1,k)/fact_n1;

   double []c2=new double [n2+2];
   double fact_n2=1; for (int k=2; k<=n2; k++) fact_n2*=k;
   sign=1; 
   for (int k=0; k<=n2+1; k++, sign*=-1)
       c2[k]=sign*unwarpJMathTools.nchoosek(n2+1,k)/fact_n2;

   // Compute the integral
   double n1_2=(double)((n1+1))/2.0;
   double n2_2=(double)((n2+1))/2.0;
   double integral=0;
   for (int k=0; k<=n1+1; k++)
       for (int l=0; l<=n2+1; l++) {
           integral+=
              c1[k]*c2[l]*build_matrix_R_computeIntegral_xx(
                 x0p,xFp,s1+k-n1_2,s2+l-n2_2,n1,n2);
       }
   return integral*h;
}

/*------------------------------------------------------------------*/
private double build_matrix_R_computeIntegral_xx(
   double x0,
   double xF,
   double s1,
   double s2,
   int    q1,
   int    q2
){
// Computation of the integral
//             xF          q1       q2
//    integral       (x-s1)   (x-s2)     dx
//             x0          +        +

   // Change of variable so that s1 is 0
   // X=x-s1 => x-s2=X-(s2-s1)
   double s2p=s2-s1;
   double xFp=xF-s1;
   double x0p=x0-s1;

   // Now integrate
   if (xFp<0) return 0;

   // Move x0 to the first point where both integrands
   // are distinct from 0
   x0p=Math.max(x0p,Math.max(s2p,0));
   if (x0p>xFp) return 0;

   // There is something to integrate
   // Evaluate the primitive at xF and x0
   double IxFp=0;
   double Ix0p=0;
   for (int k=0; k<=q2; k++) {
       double aux=unwarpJMathTools.nchoosek(q2,k)/(q1+k+1)*
                  Math.pow(-s2p,q2-k);
       IxFp+=Math.pow(xFp,q1+k+1)*aux;
       Ix0p+=Math.pow(x0p,q1+k+1)*aux;
   }

   return IxFp-Ix0p;
}

/*------------------------------------------------------------------*/
private void build_Matrix_R_geteta(
   double [][]etaq1q2,
   int q1,
   int q2,
   int dim,
   int intervals
) {
   boolean [][]done=new boolean[16][16];
   // Clear
   for (int i=0; i<16; i++)
       for (int j=0; j<16; j++) {
           etaq1q2[i][j]=0;
           done[i][j]=false;
       }

   // Compute each integral needed
   int M=intervals+1;
   double h=(double)dim/intervals;
   for (int ki1=-1; ki1<=M; ki1++)
      for (int ki2=-1; ki2<=M; ki2++) {
          int []ip=new int[2];
          boolean valid_i=build_Matrix_R_getetaindex(ki1,ki2,intervals,ip);
          if (valid_i && !done[ip[0]][ip[1]]) {
             etaq1q2[ip[0]][ip[1]]=
                build_Matrix_R_computeIntegral_aa(0,dim,ki1,ki2,h,q1,q2);
             done[ip[0]][ip[1]]=true;
          }
      }
}

/*------------------------------------------------------------------*/
private boolean build_Matrix_R_getetaindex(
   int ki1,
   int ki2,
   int intervals,
   int []ip
) {
   ip[0]=0;
   ip[1]=0;

   // Determine the clipped inner limits of the intersection
   int kir=Math.min(intervals,Math.min(ki1,ki2)+2);
   int kil=Math.max(0        ,Math.max(ki1,ki2)-2);

   if (kil>=kir) return false;

   // Determine which are the pieces of the
   // function that lie in the intersection
   int two_i=1;
   double ki;
   for (int i=0; i<=3; i++, two_i*=2) {
       // First function
       ki=ki1+i-1.5; // Middle sample of the piece i
       if (kil<=ki && ki<=kir) ip[0]+=two_i;

       // Second function
       ki=ki2+i-1.5; // Middle sample of the piece i
       if (kil<=ki && ki<=kir) ip[1]+=two_i;
   }

   ip[0]--;
   ip[1]--;
   return true;   
}

/*------------------------------------------------------------------*/
private void buildRegularizationTemporary(int intervals) {
    // M is the number of spline coefficients per row
    int M=intervals+3;
    int M2=M*M;

   // P11
   P11=new double[M2][M2];
   for (int i=0; i<M2; i++)
      for (int j=0; j<M2; j++) P11[i][j]=0.0;
   build_Matrix_Rq1q2(intervals, divWeight           , 2, 0, P11);
   build_Matrix_Rq1q2(intervals, divWeight+curlWeight, 1, 1, P11);
   build_Matrix_Rq1q2(intervals,           curlWeight, 0, 2, P11);

   // P22
   P22=new double[M2][M2];
   for (int i=0; i<M2; i++)
      for (int j=0; j<M2; j++) P22[i][j]=0.0;
   build_Matrix_Rq1q2(intervals, divWeight           , 0, 2, P22);
   build_Matrix_Rq1q2(intervals, divWeight+curlWeight, 1, 1, P22);
   build_Matrix_Rq1q2(intervals,           curlWeight, 2, 0, P22);

   // P12
   P12=new double[M2][M2];
   for (int i=0; i<M2; i++)
      for (int j=0; j<M2; j++) P12[i][j]=0.0;
   build_Matrix_Rq1q2q3q4(intervals, 2*divWeight , 2, 0, 1, 1, P12);
   build_Matrix_Rq1q2q3q4(intervals, 2*divWeight , 1, 1, 0, 2, P12);
   build_Matrix_Rq1q2q3q4(intervals,-2*curlWeight, 0, 2, 1, 1, P12);
   build_Matrix_Rq1q2q3q4(intervals,-2*curlWeight, 1, 1, 2, 0, P12);
}

/*------------------------------------------------------------------*/
private double[][] computeAffineMatrix (
) {
    boolean adjust_size=false;

   final double[][] D = new double[3][3];
   final double[][] H = new double[3][3];
   final double[][] U = new double[3][3];
   final double[][] V = new double[3][3];
   final double[][] X = new double[2][3];
   final double[] W = new double[3];
   Vector sourceVector=null;
   if (sourcePh!=null) sourceVector=sourcePh.getPoints();
   else                sourceVector=new Vector();
   Vector targetVector = null;
   if (targetPh!=null) targetVector=targetPh.getPoints();
   else                targetVector=new Vector();
   int removeLastPoint=0;

    if (false) {
        removeLastPoint=sourceMsk.numberOfMaskPoints();
        for (int i=0; i<removeLastPoint; i++) {
           sourceVector.addElement(sourceMsk.getPoint(i));
           targetVector.addElement(targetMsk.getPoint(i));
        }
   }

   int n = targetVector.size();
   switch (n) {
      case 0:
         for (int i = 0; (i < 2); i++) 
            for (int j = 0; (j < 3); j++) X[i][j]=0.0;
          if (adjust_size) {
              // Make both images of the same size
             X[0][0]=(double)source.getCurrentWidth ()/target.getCurrentWidth ();
             X[1][1]=(double)source.getCurrentHeight()/target.getCurrentHeight();
           } else {
             // Make both images to be centered
             X[0][0]=X[1][1]=1;
             X[0][2]=((double)source.getCurrentWidth ()-target.getCurrentWidth ())/2;
             X[1][2]=((double)source.getCurrentHeight()-target.getCurrentHeight())/2;
         }
         break;
      case 1:
         for (int i = 0; (i < 2); i++) {
            for (int j = 0; (j < 2); j++) {
               X[i][j] = (i == j) ? (1.0F) : (0.0F);
            }
         }
         X[0][2] = factorWidth*(double)(((Point)sourceVector.firstElement()).x
            - ((Point)targetVector.firstElement()).x);
         X[1][2] = factorHeight*(double)(((Point)sourceVector.firstElement()).y
            - ((Point)targetVector.firstElement()).y);
         break;
      case 2:
         final double x0 = factorWidth *((Point)sourceVector.elementAt(0)).x;
         final double y0 = factorHeight*((Point)sourceVector.elementAt(0)).y;
         final double x1 = factorWidth *((Point)sourceVector.elementAt(1)).x;
         final double y1 = factorHeight*((Point)sourceVector.elementAt(1)).y;
         final double u0 = factorWidth *((Point)targetVector.elementAt(0)).x;
         final double v0 = factorHeight*((Point)targetVector.elementAt(0)).y;
         final double u1 = factorWidth *((Point)targetVector.elementAt(1)).x;
         final double v1 = factorHeight*((Point)targetVector.elementAt(1)).y;
         sourceVector.addElement(new Point((int)(x1 + y0 - y1), (int)(x1 + y1 - x0)));
         targetVector.addElement(new Point((int)(u1 + v0 - v1), (int)(u1 + v1 - u0)));
         removeLastPoint=1;
         n = 3;
      default:
         for (int i = 0; (i < 3); i++) {
            for (int j = 0; (j < 3); j++) {
               H[i][j] = 0.0F;
            }
         }
         for (int k = 0; (k < n); k++) {
            final Point sourcePoint = (Point)sourceVector.elementAt(k);
            final Point targetPoint = (Point)targetVector.elementAt(k);
            final double sx=factorWidth * (double)sourcePoint.x;
            final double sy=factorHeight* (double)sourcePoint.y;
            final double tx=factorWidth * (double)targetPoint.x;
            final double ty=factorHeight* (double)targetPoint.y;
            H[0][0] += tx * sx;
            H[0][1] += tx * sy;
            H[0][2] += tx;
            H[1][0] += ty * sx;
            H[1][1] += ty * sy;
            H[1][2] += ty;
            H[2][0] += sx;
            H[2][1] += sy;
            H[2][2] += 1.0F;
            D[0][0] += sx * sx;
            D[0][1] += sx * sy;
            D[0][2] += sx;
            D[1][0] += sy * sx;
            D[1][1] += sy * sy;
            D[1][2] += sy;
            D[2][0] += sx;
            D[2][1] += sy;
            D[2][2] += 1.0F;
         }
         unwarpJMathTools.singularValueDecomposition(H, W, V);
         if ((Math.abs(W[0]) < FLT_EPSILON) || (Math.abs(W[1]) < FLT_EPSILON)
            || (Math.abs(W[2]) < FLT_EPSILON)) {
            return(computeRotationMatrix());
         }
         for (int i = 0; (i < 3); i++) {
            for (int j = 0; (j < 3); j++) {
               V[i][j] /= W[j];
            }
         }
         for (int i = 0; (i < 3); i++) {
            for (int j = 0; (j < 3); j++) {
               U[i][j] = 0.0F;
               for (int k = 0; (k < 3); k++) {
                  U[i][j] += D[i][k] * V[k][j];
               }
            }
         }
         for (int i = 0; (i < 2); i++) {
            for (int j = 0; (j < 3); j++) {
               X[i][j] = 0.0F;
               for (int k = 0; (k < 3); k++) {
                  X[i][j] += U[i][k] * H[j][k];
               }
            }
         }
         break;
   }
   if (removeLastPoint!=0) {
      for (int i=1; i<=removeLastPoint; i++) {
         sourcePh.getPoints().removeElementAt(n-i);
         targetPh.getPoints().removeElementAt(n-i);
      }
   }

   return(X);
} /* end computeAffineMatrix */

/*------------------------------------------------------------------*/
private void computeAffineResidues(
      final double[][] affineMatrix,             // Input
   final double[] dx,                           // output, difference in x for each landmark
   final double[] dy                            // output, difference in y for each landmark
                                                // The output vectors should be already resized
) {
   Vector sourceVector=null;
   if (sourcePh!=null) sourceVector=sourcePh.getPoints();
   else                sourceVector=new Vector();
   Vector targetVector = null;
   if (targetPh!=null) targetVector=targetPh.getPoints();
   else                targetVector=new Vector();

   final int K = targetPh.getPoints().size();
   for (int k=0; k<K; k++) {
      final Point sourcePoint = (Point)sourceVector.elementAt(k);
      final Point targetPoint = (Point)targetVector.elementAt(k);
      double u = factorWidth *(double)targetPoint.x;
      double v = factorHeight*(double)targetPoint.y;
      final double x = affineMatrix[0][2]
         + affineMatrix[0][0] * u + affineMatrix[0][1] * v;
      final double y = affineMatrix[1][2]
         + affineMatrix[1][0] * u + affineMatrix[1][1] * v;
      dx[k] = factorWidth*(double)sourcePoint.x - x;
      dy[k] = factorHeight*(double)sourcePoint.y - y;
   }
}

/*------------------------------------------------------------------*/
private boolean computeCoefficientsScale(
   final int intervals,                      // input, number of intervals at this scale
   final double []dx,                         // input, x residue so far
   final double []dy,                         // input, y residue so far
   final double [][]cx,                       // output, x coefficients for splines
   final double [][]cy                        // output, y coefficients for splines
) {
   int K=0;
   if (targetPh!=null) K=targetPh.getPoints().size();
   boolean underconstrained=false;

   if (0<K) {
      // Form the equation system Bc=d
      final double[][] B = new double[K][(intervals + 3) * (intervals + 3)];
      build_Matrix_B(intervals, K, B);

      // "Invert" the matrix B
      int Nunk=(intervals+3)*(intervals+3);
      double [][] iB=new double[Nunk][K];
      underconstrained=unwarpJMathTools.invertMatrixSVD(K,Nunk,B,iB);

      // Now multiply iB times dx and dy respectively
      int ij=0;
      for (int i = 0; i<intervals+3; i++)
         for (int j = 0; j<intervals+3; j++) {
            cx[i][j] = cy[i][j] = 0.0F;
            for (int k = 0; k<K; k++) {
               cx[i][j] += iB[ij][k] * dx[k];
               cy[i][j] += iB[ij][k] * dy[k];
            }
            ij++;
         }
   }
   return underconstrained;
}

/*------------------------------------------------------------------*/
private boolean computeCoefficientsScaleWithRegularization(
   final int intervals,                       // input, number of intervals at this scale
   final double []dx,                         // input, x residue so far
   final double []dy,                         // input, y residue so far
   final double [][]cx,                       // output, x coefficients for splines
   final double [][]cy                        // output, y coefficients for splines
) {
   boolean underconstrained=true;
   int K = 0;
   if (targetPh!=null) K=targetPh.getPoints().size();

   if (0<K) {
      // M is the number of spline coefficients per row
      int M=intervals+3;
      int M2=M*M;

      // Create A and b for the system Ac=b
      final double[][] A=new double[2*M2][2*M2];
      final double[]   b=new double[2*M2];
      for (int i=0; i<2*M2; i++) {
         b[i]=0.0;
         for (int j=0; j<2*M2; j++) A[i][j]=0.0;
      }

      // Get the matrix related to the landmarks 
      final double[][] B = new double[K][M2];
      build_Matrix_B(intervals, K, B);

      // Fill the part of the equation system related to the landmarks
      // Compute 2 * B^t * B
      for (int i=0; i<M2; i++) {
          for (int j=i; j<M2; j++) {
             double bitbj=0; // bi^t * bj, i.e., column i x column j
             for (int l=0; l<K; l++)
                 bitbj+=B[l][i]*B[l][j];
             bitbj*=2;
             int ij=i*M2+j;
             A[M2+i][M2+j]=A[M2+j][M2+i]=A[i][j]=A[j][i]=bitbj;
          }
      }

      // Compute 2 * B^t * [dx dy]
      for (int i=0; i<M2; i++) {
         double bitdx=0;
         double bitdy=0;
         for (int l=0; l<K; l++) {
            bitdx+=B[l][i]*dx[l];
            bitdy+=B[l][i]*dy[l];
         }
         bitdx*=2;
         bitdy*=2;
         b[   i]=bitdx;
         b[M2+i]=bitdy;
      }

      // Get the matrices associated to the regularization
      // Copy P11 symmetrized to the equation system
      for (int i=0; i<M2; i++)
         for (int j=0; j<M2; j++) {
            double aux=P11[i][j];
            A[i][j]+=aux;
            A[j][i]+=aux;
         }

      // Copy P22 symmetrized to the equation system
      for (int i=0; i<M2; i++)
         for (int j=0; j<M2; j++) {
            double aux=P22[i][j];
            A[M2+i][M2+j]+=aux;
            A[M2+j][M2+i]+=aux;
         }

      // Copy P12 and P12^t to their respective places
      for (int i=0; i<M2; i++)
         for (int j=0; j<M2; j++) {
            A[   i][M2+j]=P12[i][j]; // P12
            A[M2+i][   j]=P12[j][i]; // P12^t
         }

      // Now solve the system
      // Invert the matrix A
      double [][] iA=new double[2*M2][2*M2];
      underconstrained=unwarpJMathTools.invertMatrixSVD(2*M2,2*M2,A,iA);

      // Now multiply iB times b and distribute in cx and cy
      int ij=0;
      for (int i = 0; i<intervals+3; i++)
         for (int j = 0; j<intervals+3; j++) {
            cx[i][j] = cy[i][j] = 0.0F;
            for (int l = 0; l<2*M2; l++) {
               cx[i][j] += iA[   ij][l] * b[l];
               cy[i][j] += iA[M2+ij][l] * b[l];
            }
            ij++;
         }
   }
   return underconstrained;
}

/*------------------------------------------------------------------*/
private void computeInitialResidues(
   final double[] dx,                           // output, difference in x for each landmark
   final double[] dy                            // output, difference in y for each landmark
                                               // The output vectors should be already resized
) {
   Vector sourceVector=null;
   if (sourcePh!=null) sourceVector=sourcePh.getPoints();
   else                sourceVector=new Vector();
   Vector targetVector = null;
   if (targetPh!=null) targetVector=targetPh.getPoints();
   else                targetVector=new Vector();
   int K = 0;
   if (targetPh!=null) targetPh.getPoints().size();
   for (int k=0; k<K; k++) {
      final Point sourcePoint = (Point)sourceVector.elementAt(k);
      final Point targetPoint = (Point)targetVector.elementAt(k);
      dx[k] = factorWidth *(sourcePoint.x - targetPoint.x);
      dy[k] = factorHeight*(sourcePoint.y - targetPoint.y);
   }
}

/*------------------------------------------------------------------*/
private void computeDeformation(
   final int intervals,
   final double [][]cx,
   final double [][]cy,
   final double [][]transformation_x,
   final double [][]transformation_y) {

   // Set these coefficients to an interpolator
   unwarpJImageModel swx = new unwarpJImageModel(cx);
   unwarpJImageModel swy = new unwarpJImageModel(cy);

    // Compute the transformation mapping
   for (int v=0; v<targetCurrentHeight; v++) {
      final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
      for (int u = 0; u<targetCurrentWidth; u++) {
         final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;
         swx.prepareForInterpolation(tu, tv, ORIGINAL); transformation_x[v][u] = swx.interpolateI();
         swy.prepareForInterpolation(tu, tv, ORIGINAL); transformation_y[v][u] = swy.interpolateI();
      }
   }
}

/*------------------------------------------------------------------*/
private double[][] computeRotationMatrix (
) {
   final double[][] X = new double[2][3];
   final double[][] H = new double[2][2];
   final double[][] V = new double[2][2];
   final double[] W = new double[2];
   Vector sourceVector=null;
   if (sourcePh!=null) sourceVector=sourcePh.getPoints();
   else                sourceVector=new Vector();
   Vector targetVector = null;
   if (targetPh!=null) targetVector=targetPh.getPoints();
   else                targetVector=new Vector();
   final int n = targetVector.size();
   switch (n) {
      case 0:
         for (int i = 0; (i < 2); i++) {
            for (int j = 0; (j < 3); j++) {
               X[i][j] = (i == j) ? (1.0F) : (0.0F);
            }
         }
         break;
      case 1:
         for (int i = 0; (i < 2); i++) {
            for (int j = 0; (j < 2); j++) {
               X[i][j] = (i == j) ? (1.0F) : (0.0F);
            }
         }
         X[0][2] = factorWidth *(double)(((Point)sourceVector.firstElement()).x
            - ((Point)targetVector.firstElement()).x);
         X[1][2] = factorHeight*(double)(((Point)sourceVector.firstElement()).y
            - ((Point)targetVector.firstElement()).y);
         break;
      default:
         double xTargetAverage = 0.0F;
         double yTargetAverage = 0.0F;
         for (int i = 0; (i < n); i++) {
            final Point p = (Point)targetVector.elementAt(i);
            xTargetAverage += factorWidth *(double)p.x;
            yTargetAverage += factorHeight*(double)p.y;
         }
         xTargetAverage /= (double)n;
         yTargetAverage /= (double)n;
         final double[] xCenteredTarget = new double[n];
         final double[] yCenteredTarget = new double[n];
         for (int i = 0; (i < n); i++) {
            final Point p = (Point)targetVector.elementAt(i);
            xCenteredTarget[i] = factorWidth *(double)p.x - xTargetAverage;
            yCenteredTarget[i] = factorHeight*(double)p.y - yTargetAverage;
         }
         double xSourceAverage = 0.0F;
         double ySourceAverage = 0.0F;
         for (int i = 0; (i < n); i++) {
            final Point p = (Point)sourceVector.elementAt(i);
            xSourceAverage += factorWidth *(double)p.x;
            ySourceAverage += factorHeight*(double)p.y;
         }
         xSourceAverage /= (double)n;
         ySourceAverage /= (double)n;
         final double[] xCenteredSource = new double[n];
         final double[] yCenteredSource = new double[n];
         for (int i = 0; (i < n); i++) {
            final Point p = (Point)sourceVector.elementAt(i);
            xCenteredSource[i] = factorWidth *(double)p.x - xSourceAverage;
            yCenteredSource[i] = factorHeight*(double)p.y - ySourceAverage;
         }
         for (int i = 0; (i < 2); i++) {
            for (int j = 0; (j < 2); j++) {
               H[i][j] = 0.0F;
            }
         }
         for (int k = 0; (k < n); k++) {
            H[0][0] += xCenteredTarget[k] * xCenteredSource[k];
            H[0][1] += xCenteredTarget[k] * yCenteredSource[k];
            H[1][0] += yCenteredTarget[k] * xCenteredSource[k];
            H[1][1] += yCenteredTarget[k] * yCenteredSource[k];
         }
         // COSS: Watch out that this H is the transpose of the one
         // defined in the text. That is why X=V*U^t is the inverse of
         // of the rotation matrix.
         unwarpJMathTools.singularValueDecomposition(H, W, V);
         if (((H[0][0] * H[1][1] - H[0][1] * H[1][0])
            * (V[0][0] * V[1][1] - V[0][1] * V[1][0])) < 0.0F) {
            if (W[0] < W[1]) {
               V[0][0] *= -1.0F;
               V[1][0] *= -1.0F;
            }
            else {
               V[0][1] *= -1.0F;
               V[1][1] *= -1.0F;
            }
         }
         for (int i = 0; (i < 2); i++) {
            for (int j = 0; (j < 2); j++) {
               X[i][j] = 0.0F;
               for (int k = 0; (k < 2); k++) {
                  X[i][j] += V[i][k] * H[j][k];
               }
            }
         }
         X[0][2] = xSourceAverage - X[0][0] * xTargetAverage - X[0][1] * yTargetAverage;
         X[1][2] = ySourceAverage - X[1][0] * xTargetAverage - X[1][1] * yTargetAverage;
         break;
   }
   return(X);
} /* end computeRotationMatrix */

/*------------------------------------------------------------------*/
private void computeScaleResidues(
         int intervals,                           // input, number of intevals
   final double [][]cx,                            // Input, spline coefficients
   final double [][]cy,
   final double []dx,                              // Input/Output. At the input it has the
   final double []dy                               // residue so far, at the output this
                                                  // residue is modified to account for
                                                  // the model at the new scale
) {
   // Set these coefficients to an interpolator
   unwarpJImageModel swx = new unwarpJImageModel(cx);
   unwarpJImageModel swy = new unwarpJImageModel(cy);

    // Get the list of landmarks
   Vector sourceVector=null;
   if (sourcePh!=null) sourceVector=sourcePh.getPoints();
   else                sourceVector=new Vector();
   Vector targetVector = null;
   if (targetPh!=null) targetVector=targetPh.getPoints();
   else                targetVector=new Vector();
   final int K = targetVector.size();

   for (int k=0; k<K; k++) {
       // Get the landmark coordinate in the target image
      final Point sourcePoint = (Point)sourceVector.elementAt(k);
      final Point targetPoint = (Point)targetVector.elementAt(k);
      double u = factorWidth *(double)targetPoint.x;
      double v = factorHeight*(double)targetPoint.y;

      // Express it in "spline" units
      double tu = (double)(u * intervals) / (double)(targetCurrentWidth  - 1) + 1.0F;
      double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;

      // Transform this coordinate to the source image
      swx.prepareForInterpolation(tu, tv, false); double x=swx.interpolateI();
      swy.prepareForInterpolation(tu, tv, false); double y=swy.interpolateI();

      // Substract the result from the residual
      dx[k]=factorWidth *(double)sourcePoint.x - x;
      dy[k]=factorHeight*(double)sourcePoint.y - y;
   }
}

/*--------------------------------------------------------------------------*/
private void computeTotalWorkload() {
    // This code is an excerpt from doRegistration() to compute the exact
    // number of steps

    // Now refine with the different scales
    int state;   // state=-1 --> Finish
                // state= 0 --> Increase deformation detail
                // state= 1 --> Increase image detail
                // state= 2 --> Do nothing until the finest image scale
    if (min_scale_deformation==max_scale_deformation) state=1;
    else                                              state=0;
    int s=min_scale_deformation;
    int currentDepth=target.getCurrentDepth();
    int workload=0;
    while (state!=-1) {
        // Update the deformation coefficients only in states 0 and 1
        if (state==0 || state==1) {
           // Optimize deformation coefficients
           if (imageWeight!=0)
              workload+=300*(currentDepth+1);
        }

        // Prepare for next iteration
        switch (state) {
           case 0:
              // Finer details in the deformation
              if (s<max_scale_deformation) {
               s++;
                if (currentDepth>min_scale_image) state=1;
                else                              state=0;
             } else
                if (currentDepth>min_scale_image) state=1;
                else                              state=2;
             break;
          case 1: // Finer details in the image, go on  optimizing
          case 2: // Finer details in the image, do not optimize
              // Compute next state
              if (state==1) {
                 if      (s==max_scale_deformation && currentDepth==min_scale_image) state=2;
                 else if (s==max_scale_deformation)                                  state=1;
                 else                                                                state=0;
              } else if (state==2) {
                 if (currentDepth==0) state=-1;
                 else                 state= 2;
              }

            // Pop another image and prepare the deformation
              if (currentDepth!=0) currentDepth--;
              break;
        }
   }
   unwarpJProgressBar.resetProgressBar();
   unwarpJProgressBar.addWorkload(workload);
}

/*--------------------------------------------------------------------------*/
private double evaluateSimilarity(
   final double []c,            // Input:  Deformation coefficients
   final int      intervals,    // Input:  Number of intervals for the deformation
         double []grad,         // Output: Gradient of the similarity
                                // Output: the similarity is returned
   final boolean  only_image,   // Input:  if true, only the image term is considered
                                //         and not the regularization
   final boolean  show_error    // Input:  if true, an image is shown with the error
) {
   int cYdim=intervals+3;
   int cXdim=cYdim;
   int Nk=cYdim*cXdim;
   int twiceNk=2*Nk;
   double []vgradreg =new double[grad.length];
   double []vgradland=new double[grad.length];

   // Set the transformation coefficients to the interpolator
   swx.setCoefficients(c,cYdim,cXdim,0);
   swy.setCoefficients(c,cYdim,cXdim,Nk);

   // Initialize gradient
   for (int k=0; k<twiceNk; k++) vgradreg[k]=vgradland[k]=grad[k]=0.0F;

   // Estimate the similarity and gradient between both images
   double imageSimilarity=0.0;
   int Ydim=target.getCurrentHeight();
   int Xdim=target.getCurrentWidth();

   // Prepare to show
   double [][]error_image=null;
   double [][]div_error_image=null;
   double [][]curl_error_image=null;
   double [][]laplacian_error_image=null;
   double [][]jacobian_error_image=null;
   if (show_error) {
      error_image=new double[Ydim][Xdim];
      div_error_image=new double[Ydim][Xdim];
      curl_error_image=new double[Ydim][Xdim];
      laplacian_error_image=new double[Ydim][Xdim];
      jacobian_error_image=new double[Ydim][Xdim];
      for (int v=0; v<Ydim; v++)
         for (int u=0; u<Xdim; u++)
            error_image[v][u]=div_error_image[v][u]=curl_error_image[v][u]=
               laplacian_error_image[v][u]=jacobian_error_image[v][u]=-1.0;
   }

   // Loop over all points in the source image
   int n=0;
   if (imageWeight!=0 || show_error) {
      double []xD2=new double[3]; // Some space for the second derivatives
      double []yD2=new double[3]; // of the transformation
      double []xD =new double[2]; // Some space for the second derivatives
      double []yD =new double[2]; // of the transformation
      double []I1D=new double[2]; // Space for the first derivatives of I1
      double hx=(Xdim-1)/intervals;   // Scale in the X axis
      double hy=(Ydim-1)/intervals;   // Scale in the Y axis
      double []targetCurrentImage=target.getCurrentImage();
      int uv=0;
      for (int v=0; v<Ydim; v++) {
          for (int u=0; u<Xdim; u++, uv++) {
               // Compute image term .....................................................
               // Check if this point is in the target mask
               if (targetMsk.getValue(u/factorWidth,v/factorHeight)) {
        	 // Compute value in the source image
        	 double I2=targetCurrentImage[uv];

        	 // Compute the position of this point in the target
        	 double x=swx.precomputed_interpolateI(u,v);
        	 double y=swy.precomputed_interpolateI(u,v);

        	 // Check if this point is in the source mask
        	 if (sourceMsk.getValue(x/factorWidth,y/factorHeight)) {
        	    // Compute the value of the target at that point
        	    source.prepareForInterpolation(x,y,PYRAMID); double I1=source.interpolateI();
        	    source.interpolateD(I1D); double I1dx=I1D[0], I1dy=I1D[1];

        	    double error=I2-I1;
        	    double error2=error*error;
         	    if (show_error) error_image[v][u]=error;
                    imageSimilarity+=error2;

        	    // Compute the derivative with respect to all the c coefficients
        	    // Cost of the derivatives = 16*(3 mults + 2 sums)
        	    // Current cost= 359 mults + 346 sums
        	    for (int l=0; l<4; l++)
                      for (int m=0; m<4; m++) {
                	 if (swx.prec_yIndex[v][l]==-1 || swx.prec_xIndex[u][m]==-1) continue;
                	 double weightI=swx.precomputed_getWeightI(l,m,u,v);
                	 int k=swx.prec_yIndex[v][l]*cYdim+swx.prec_xIndex[u][m];

                	 // Compute partial result
                	 // There's also a multiplication by 2 that I will
                	 // do later
                	 double aux=-error*weightI;

                	 // Derivative related to X deformation
                	 grad[k]   +=aux*I1dx;

                	 // Derivative related to Y deformation
                	 grad[k+Nk]+=aux*I1dy;
                      }
        	   n++; // Another point has been successfully evaluated
        	}
	     }

           // Show regularization images ...........................................
            if (show_error) {
              double gradcurlx=0.0, gradcurly=0.0;
              double graddivx =0.0, graddivy =0.0;
              double xdx  =0.0, xdy  =0.0,
                     ydx  =0.0, ydy  =0.0,
                     xdxdy=0.0, xdxdx=0.0, xdydy=0.0,
                     ydxdy=0.0, ydxdx=0.0, ydydy=0.0; 

              // Compute the first derivative terms
              swx.precomputed_interpolateD(xD,u,v); xdx=xD[0]/hx; xdy=xD[1]/hy; 
              swy.precomputed_interpolateD(yD,u,v); ydx=yD[0]/hx; ydy=yD[1]/hy; 

              // Compute the second derivative terms
              swx.precomputed_interpolateD2(xD2,u,v);
              xdxdy=xD2[0]; xdxdx=xD2[1]; xdydy=xD2[2]; 
              swy.precomputed_interpolateD2(yD2,u,v);
              ydxdy=yD2[0]; ydxdx=yD2[1]; ydydy=yD2[2]; 

              // Error in the divergence
              graddivx=xdxdx+ydxdy;
              graddivy=xdxdy+ydydy;
              double graddiv=graddivx*graddivx+graddivy*graddivy;
              double errorgraddiv=divWeight*graddiv;

              if (divWeight!=0) div_error_image [v][u]=errorgraddiv;
              else              div_error_image [v][u]=graddiv;

              // Compute error in the curl
              gradcurlx=-xdxdy+ydxdx;
              gradcurly=-xdydy+ydxdy;
              double gradcurl=gradcurlx*gradcurlx+gradcurly*gradcurly;
              double errorgradcurl=curlWeight*gradcurl;

              if (curlWeight!=0) curl_error_image[v][u]=errorgradcurl;
              else               curl_error_image[v][u]=gradcurl;

              // Compute Laplacian error
              laplacian_error_image[v][u] =xdxdx*xdxdx;
              laplacian_error_image[v][u]+=xdxdy*xdxdy;
              laplacian_error_image[v][u]+=xdydy*xdydy;
              laplacian_error_image[v][u]+=ydxdx*ydxdx;
              laplacian_error_image[v][u]+=ydxdy*ydxdy;
              laplacian_error_image[v][u]+=ydydy*ydydy;

              // Compute jacobian error
              jacobian_error_image[v][u] =xdx*ydy-xdy*ydx;
            }
        }
       }
   }

    // Average the image related terms
    if (n!=0) {
       imageSimilarity*=imageWeight/n;
       double aux=imageWeight*2.0/n; // This is the 2 coming from the
                                     // derivative that I would do later
       for (int k=0; k<twiceNk; k++) grad[k]*=aux;
    } else 
       if (imageWeight==0) imageSimilarity=0;
       else                imageSimilarity=1/FLT_EPSILON;

    // Compute regularization term .............................................. 
    double regularization=0.0;
    if (!only_image) {
       for (int i=0; i<Nk; i++)
           for (int j=0; j<Nk; j++) {
               regularization+=c[   i]*P11[i][j]*c[   j]+// c1^t P11 c1
                               c[Nk+i]*P22[i][j]*c[Nk+j]+// c2^t P22 c2
                               c[   i]*P12[i][j]*c[Nk+j];// c1^t P12 c2
               vgradreg[   i]+=2*P11[i][j]*c[j];         // 2 P11 c1
               vgradreg[Nk+i]+=2*P22[i][j]*c[Nk+j];      // 2 P22 c2
               vgradreg[   i]+=  P12[i][j]*c[Nk+j];      //   P12 c2
               vgradreg[Nk+i]+=  P12[j][i]*c[   j];      //   P12^t c1
           }
       regularization*=1.0/(Ydim*Xdim);
       for (int k=0; k<twiceNk; k++) vgradreg [k]*=1.0/(Ydim*Xdim);
    }

    // Compute landmark error and derivative ...............................
    // Get the list of landmarks
    double landmarkError=0.0;
    int K = 0;
    if (targetPh!=null) K=targetPh.getPoints().size();
    if (landmarkWeight!=0) {
      Vector sourceVector=null;
      if (sourcePh!=null) sourceVector=sourcePh.getPoints();
      else                sourceVector=new Vector();
      Vector targetVector = null;
      if (targetPh!=null) targetVector=targetPh.getPoints();
      else                targetVector=new Vector();

      for (int kp=0; kp<K; kp++) {
          // Get the landmark coordinate in the target image
         final Point sourcePoint = (Point)sourceVector.elementAt(kp);
         final Point targetPoint = (Point)targetVector.elementAt(kp);
         double u = factorWidth *(double)targetPoint.x;
         double v = factorHeight*(double)targetPoint.y;

         // Express it in "spline" units
         double tu = (double)(u * intervals) / (double)(targetCurrentWidth  - 1) + 1.0F;
         double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;

         // Transform this coordinate to the source image
         swx.prepareForInterpolation(tu, tv, false); double x=swx.interpolateI();
         swy.prepareForInterpolation(tu, tv, false); double y=swy.interpolateI();

         // Substract the result from the residual
         double dx=factorWidth *(double)sourcePoint.x - x;
         double dy=factorHeight*(double)sourcePoint.y - y;

         // Add to landmark error
         landmarkError+=dx*dx+dy*dy;

         // Compute the derivative with respect to all the c coefficients
         for (int l=0; l<4; l++)
            for (int m=0; m<4; m++) {
               if (swx.yIndex[l]==-1 || swx.xIndex[m]==-1) continue;
               int k=swx.yIndex[l]*cYdim+swx.xIndex[m];

               // There's also a multiplication by 2 that I will do later
               // Derivative related to X deformation
               vgradland[k]   -=dx*swx.getWeightI(l,m);

               // Derivative related to Y deformation
               vgradland[k+Nk]-=dy*swy.getWeightI(l,m);
            }
      }
   }

   if (K!=0) {
      landmarkError*=landmarkWeight/K;
      double aux=2.0*landmarkWeight/K;
                          // This is the 2 coming from the derivative
                          // computation that I would do at the end
      for (int k=0; k<twiceNk; k++) vgradland[k]*=aux;
   }
   if (only_image) landmarkError=0;

   // Finish computations .............................................................
   // Add all gradient terms
   for (int k=0; k<twiceNk; k++)
       grad[k]+=vgradreg[k]+vgradland[k];

   if (show_error) {
      unwarpJMiscTools.showImage("Error",error_image);
      unwarpJMiscTools.showImage("Divergence Error",div_error_image);
      unwarpJMiscTools.showImage("Curl Error",curl_error_image);
      unwarpJMiscTools.showImage("Laplacian Error",laplacian_error_image);
      unwarpJMiscTools.showImage("Jacobian Error",jacobian_error_image);
   }

   if (showMarquardtOptim) {
      if (imageWeight!=0)                IJ.write("    Image          error:"+imageSimilarity);
      if (landmarkWeight!=0)             IJ.write("    Landmark       error:"+landmarkError);
      if (divWeight!=0 || curlWeight!=0) IJ.write("    Regularization error:"+regularization);
   }
   return imageSimilarity+landmarkError+regularization;
}

/*--------------------------------------------------------------------------*/
private void Marquardt_it (
   double   []x,
   boolean  []optimize,
   double   []gradient,
   double   []Hessian,
   double     lambda
)

{
    /* In this function the system (H+lambda*Diag(H))*update=gradient
       is solved for update.
       H is the hessian of the function f,
       gradient is the gradient of the function f,
       Diag(H) is a matrix with the diagonal of H.
    */
    final double TINY  = FLT_EPSILON;
    final int   M      = x.length;
    final int   Mmax   = 35;
    final int   Mused  = Math.min(M,Mmax);

    double [][] u         = new double  [Mused][Mused];
    double [][] v         = null; //new double  [Mused][Mused];
    double   [] w         = null; //new double  [Mused];
    double   [] g         = new double  [Mused];
    double   [] update    = new double  [Mused];
    boolean  [] optimizep = new boolean [M];
       System.arraycopy(optimize,0,optimizep,0,M);

    lambda+=1.0F;

    if (M>Mmax) {
       /* Find the threshold for the most important components */
       double []sortedgradient= new double [M];
       for (int i=0; i<M; i++) sortedgradient[i]=Math.abs(gradient[i]);
       Arrays.sort(sortedgradient);

       double gradient_th=sortedgradient[M-Mmax];
       int m=0, i;
       // Take the first Mused components with big gradients
       for (i=0; i<M; i++)
           if  (optimizep[i] && Math.abs(gradient[i])>=gradient_th) {
               m++; if (m==Mused) break;
           } else optimizep[i]=false;
       // Set the rest to 0
       for (i=i+1; i<M; i++) optimizep[i]=false;
    }

    // Gradient descent
    //for (int i=0; i<M; i++) if (optimizep[i]) x[i]-=0.01*gradient[i];
    //if (true) return;

    /* u will be a copy of the Hessian where we take only those
       components corresponding to variables being optimized */
    int kr=0, iw=0;
    for (int ir = 0; ir<M; kr=kr+M,ir++) {
      if (optimizep[ir]) {
          int jw=0;
         for (int jr = 0; jr<M; jr++)
            if (optimizep[jr]) u[iw][jw++] = Hessian[kr + jr];
         g[iw]=gradient[ir];
         u[iw][iw] *= lambda;
         iw++;
      }
    }

    // Solve he equation system
    /* SVD u=u*w*v^t */
    update=unwarpJMathTools.linearLeastSquares(u,g);

    /* x = x - update */
    kr=0;
    for (int kw = 0; kw<M; kw++)
      if (optimizep[kw]) x[kw] -=  update[kr++];
} /* end Marquardt_it */

/*--------------------------------------------------------------------------*/
private double optimizeCoeffs(
   int intervals,
   double thChangef,
   double [][]cx,
   double [][]cy
){
   if (dialog!=null && dialog.isStopRegistrationSet()) return 0.0;
   final double TINY               = FLT_EPSILON;
   final double EPS                = 3.0e-8F;
   final double FIRSTLAMBDA        = 1;
   final int    MAXITER_OPTIMCOEFF = 300;
   final int    CUMULATIVE_SIZE    = 5;

   int int3=intervals+3;
   int halfM=int3*int3;
   int M=halfM*2;

   double   rescuedf, f;
   double   []x            = new double   [M];
   double   []rescuedx     = new double   [M];
   double   []diffx        = new double   [M];
   double   []rescuedgrad  = new double   [M];
   double   []grad         = new double   [M];
   double   []diffgrad     = new double   [M];
   double   []Hdx          = new double   [M];
   double   []rescuedhess  = new double   [M*M];
   double   []hess         = new double   [M*M];
   double   []safehess     = new double   [M*M];
   double   []proposedHess = new double   [M*M];
   boolean  []optimize     = new boolean  [M];
   int        i, j, p, iter=1;
   boolean    skip_update, ill_hessian;
   double     improvementx=(double)Math.sqrt(TINY),
              lambda=FIRSTLAMBDA, max_normx, distx, aux, gmax;
   double     fac, fae, dgdx, dxHdx, sumdiffg, sumdiffx;
   unwarpJCumulativeQueue lastBest=
      new unwarpJCumulativeQueue(CUMULATIVE_SIZE);

   for (i=0; i<M; i++) optimize[i]=true;

   /* Form the vector with the current guess for the optimization */
   for (i= 0, p=0; i<intervals + 3; i++)
       for (j = 0; j < intervals + 3; j++, p++) {
         x[      p]=cx[i][j];
         x[halfM+p]=cy[i][j];
       }

   /* Prepare the precomputed weights for interpolation */
   swx = new unwarpJImageModel(x,intervals+3,intervals+3,0);
   swy = new unwarpJImageModel(x,intervals+3,intervals+3,halfM);
   swx.precomputed_prepareForInterpolation(
      target.getCurrentHeight(), target.getCurrentWidth(), intervals);
   swy.precomputed_prepareForInterpolation(
      target.getCurrentHeight(), target.getCurrentWidth(), intervals);

   /* First computation of the similarity */
   f=evaluateSimilarity(x,intervals,grad,false,false);
   if (showMarquardtOptim) IJ.write("f(1)="+f);

   /* Initially the hessian is the identity matrix multiplied by
      the first function value */
   for (i=0,p=0; i<M; i++)
      for (j=0; j<M; j++,p++)
         if (i==j) hess[p]=1.0F; else hess[p]=0.0F;

   rescuedf    = f;
   for (i=0,p=0; i<M; i++) {
      rescuedx[i]=x[i];
      rescuedgrad[i]=grad[i];
      for (j=0; j<M; j++,p++) rescuedhess[p]=hess[p];
   }

   int maxiter=MAXITER_OPTIMCOEFF*(source.getCurrentDepth()+1);
   unwarpJProgressBar.stepProgressBar();
   int last_successful_iter=0;
   boolean stop=dialog!=null && dialog.isStopRegistrationSet();
   while (iter < maxiter && !stop) {
       /* Compute new x ------------------------------------------------- */
      Marquardt_it(x, optimize, grad, hess, lambda);

      /* Stopping criteria --------------------------------------------- */
      /* Compute difference with the previous iteration */
      max_normx=improvementx=0;
      for (i=0; i<M; i++) {
         diffx[i]=x[i]-rescuedx[i];
         distx=Math.abs(diffx[i]);
         improvementx+=distx*distx;
         aux=Math.abs(rescuedx[i]) < Math.abs(x[i]) ? x[i] : rescuedx[i];
         max_normx+=aux*aux;
      }
      if (TINY < max_normx) improvementx = improvementx/max_normx;
      improvementx = (double) Math.sqrt(Math.sqrt(improvementx));

      /* If there is no change with respect to the old geometry then
         finish the iterations */
      if (improvementx < Math.sqrt(TINY)) break;

      /* Estimate the new function value -------------------------------- */
      f=evaluateSimilarity(x,intervals,grad,false,false);
      iter++;
      if (showMarquardtOptim) IJ.write("f("+iter+")="+f+" lambda="+lambda);
      unwarpJProgressBar.stepProgressBar();

      /* Update lambda -------------------------------------------------- */
      if (rescuedf > f) {
          /* Check if the improvement is only residual */
          lastBest.push_back(rescuedf-f);
          if (lastBest.currentSize()==CUMULATIVE_SIZE && lastBest.getSum()/f<thChangef)
             break;

          /* If we have improved then estimate the hessian, 
             update the geometry, and decrease the lambda */
          /* Estimate the hessian ....................................... */
          if (showMarquardtOptim) IJ.write("  Accepted");
          if ((last_successful_iter++%10)==0 && outputLevel>-1)
             update_current_output(x,intervals);

          /* Estimate the difference between gradients */
          for (i=0; i<M; i++) diffgrad[i]=grad[i]-rescuedgrad[i];

          /* Multiply this difference by the current inverse of the hessian */
          for (i=0, p=0; i<M; i++) {
             Hdx[i]=0.0F;
             for (j=0; j<M; j++, p++) Hdx[i]+=hess[p]*diffx[j];
          }

          /* Calculate dot products for the denominators ................ */
          dgdx=dxHdx=sumdiffg=sumdiffx=0.0F;
          skip_update=true;
          for (i=0; i<M; i++) {
             dgdx     += diffgrad[i]*diffx[i];
             dxHdx    += diffx[i]*Hdx[i];
             sumdiffg += diffgrad[i]*diffgrad[i];
             sumdiffx += diffx[i]*diffx[i];
             if (Math.abs(grad[i])>=Math.abs(rescuedgrad[i])) gmax=Math.abs(grad[i]);
             else                                             gmax=Math.abs(rescuedgrad[i]);
             if (gmax!=0 && Math.abs(diffgrad[i]-Hdx[i])>Math.sqrt(EPS)*gmax)
                skip_update=false;
          }

          /* Update hessian ............................................. */
          /* Skip if fac not sufficiently positive */
          if (dgdx>Math.sqrt(EPS*sumdiffg*sumdiffx) && !skip_update) {
             fae=1.0F/dxHdx;
             fac=1.0F/dgdx;

             /* Update the hessian after BFGS formula */
            for (i=0, p=0; i<M; i++)
               for (j=0; j<M; j++, p++) {
                  if (i<=j) proposedHess[p]=hess[p]+
                     fac*diffgrad[i]*diffgrad[j]
                    -fae*(Hdx[i]*Hdx[j]);
                  else proposedHess[p]=proposedHess[j*M+i];
               }

            ill_hessian=false;
            if (!ill_hessian) {
               for (i=0, p=0; i<M; i++)
                   for (j=0; j<M; j++,p++)
                       hess[p]= proposedHess[p];
            } else
               if (showMarquardtOptim)
                      IJ.write("Hessian cannot be safely updated, ill-conditioned");

          } else
            if (showMarquardtOptim)
                IJ.write("Hessian cannot be safely updated");

          /* Update geometry and lambda ................................. */
         rescuedf    = f;
         for (i=0,p=0; i<M; i++) {
            rescuedx[i]=x[i];
            rescuedgrad[i]=grad[i];
            for (j=0; j<M; j++,p++) rescuedhess[p]=hess[p];
         }
         if (1e-4 < lambda) lambda = lambda/10;
      } else {
      /* else, if it is worse, then recover the last geometry
         and increase lambda, saturate lambda with FIRSTLAMBDA */
         for (i=0,p=0; i<M; i++) {
            x[i]=rescuedx[i];
            grad[i]=rescuedgrad[i];
            for (j=0; j<M; j++,p++) hess[p]=rescuedhess[p];
         }
         if (lambda < 1.0/TINY) lambda*=10;
         else break;
         if (lambda < FIRSTLAMBDA) lambda = FIRSTLAMBDA;
      }

        stop=dialog!=null && dialog.isStopRegistrationSet();
    }

    // Copy the values back to the input arrays
    for (i= 0, p=0; i<intervals + 3; i++)
        for (j = 0; j < intervals + 3; j++, p++) {
          cx[i][j]=x[      p];
          cy[i][j]=x[halfM+p];
      }

    unwarpJProgressBar.skipProgressBar(maxiter-iter);
   return f;
}

/*-----------------------------------------------------------------------------*/
private double [][] propagateCoeffsToNextLevel(
   int intervals,
   final double [][]c,
   double expansionFactor     // Due to the change of size in the represented image
){
   // Expand the coefficients for the next scale
   intervals*=2;
   double [][] cs_expand = new double[intervals+7][intervals+7];

   // Upsample
   for (int i=0; i<intervals+7; i++)
      for (int j=0; j<intervals+7; j++) {
          // If it is not in an even sample then set it to 0
          if (i%2 ==0 || j%2 ==0) cs_expand[i][j]=0.0F;
          else {
             // Now look for this sample in the coarser level
             int ipc=(i-1)/2;
             int jpc=(j-1)/2;
              cs_expand[i][j]=c[ipc][jpc];
          }
      }

   // Define the FIR filter
   double [][] u2n=new double [4][];
   u2n[0]=null;
   u2n[1]=new double[3]; u2n[1][0]=0.5F; u2n[1][1]=1.0F; u2n[1][2]=0.5F;
   u2n[2]=null;
   u2n[3]=new double[5]; u2n[3][0]=0.125F; u2n[3][1]=0.5F; u2n[3][2]=0.75F;
       u2n[3][3]=0.5F; u2n[3][4]=0.125F; 
   int [] half_length_u2n={0, 1, 0, 2};
   int kh=half_length_u2n[transformationSplineDegree];

   // Apply the u2n filter to rows
   double [][] cs_expand_aux = new double[intervals+7][intervals+7];

   for (int i=1; i<intervals+7; i+=2)
      for (int j=0; j<intervals+7; j++) {
          cs_expand_aux[i][j]=0.0F;
          for (int k=-kh; k<=kh; k++)
              if (j+k>=0 && j+k<=intervals+6)
                 cs_expand_aux[i][j]+=u2n[transformationSplineDegree][k+kh]*cs_expand[i][j+k];
      }

   // Apply the u2n filter to columns
   for (int i=0; i<intervals+7; i++)
      for (int j=0; j<intervals+7; j++) {
          cs_expand[i][j]=0.0F;
          for (int k=-kh; k<=kh; k++)
              if (i+k>=0 && i+k<=intervals+6)
                 cs_expand[i][j]+=u2n[transformationSplineDegree][k+kh]*cs_expand_aux[i+k][j];
      }

   // Copy the central coefficients to c
   double [][]newc=new double [intervals+3][intervals+3];
   for (int i= 0; i<intervals+3; i++)
      for (int j = 0; j <intervals + 3; j++)
          newc[i][j]=cs_expand[i+2][j+2]*expansionFactor;

   // Return the new set of coefficients
   return newc;
}

/*------------------------------------------------------------------*/
private void saveTransformation(
   int intervals,
   double [][]cx,
   double [][]cy
) {
   String filename=fn_tnf;
   if (filename.equals("")) {
      // Get the filename to save
      File dir=new File(".");
      String path="";
      try {
         path=dir.getCanonicalPath()+"/";
      } catch (Exception e) {
         e.printStackTrace();
      }
      filename=sourceImp.getTitle();
      String new_filename="";
      int dot = filename.lastIndexOf('.');
      if (dot == -1) new_filename=filename + "_transf.txt";
      else           new_filename=filename.substring(0, dot)+"_transf.txt";
      filename=path+filename;

      if (outputLevel>-1) {
         final Frame f = new Frame();
         final FileDialog fd = new FileDialog(f, "Save Transformation", FileDialog.SAVE);
         fd.setFile(new_filename);
         fd.setVisible(true);
         path = fd.getDirectory();
         filename = fd.getFile();
         if ((path == null) || (filename == null)) return;
         filename=path+filename;
       } else
         filename=new_filename;
   }

   // Save the file
   try {
      final FileWriter fw = new FileWriter(filename);
      String aux;
      fw.write("Intervals="+intervals+"\n\n");
      fw.write("X Coeffs -----------------------------------\n");
      for (int i= 0; i<intervals + 3; i++) {
         for (int j = 0; j < intervals + 3; j++) {
            aux=""+cx[i][j];
                while (aux.length()<21) aux=" "+aux;
                fw.write(aux+" ");
         }
         fw.write("\n");
      }
      fw.write("\n");
      fw.write("Y Coeffs -----------------------------------\n");
      for (int i= 0; i<intervals + 3; i++) {
         for (int j = 0; j < intervals + 3; j++) {
            aux=""+cy[i][j];
                while (aux.length()<21) aux=" "+aux;
                fw.write(aux+" ");
         }
         fw.write("\n");
      }
      fw.close();
   } catch (IOException e) {
      IJ.error("IOException exception" + e);
   } catch (SecurityException e) {
      IJ.error("Security exception" + e);
   }
}

/*------------------------------------------------------------------*/
private void showDeformationGrid(
   int intervals,
   double [][]cx,
   double [][]cy,
   ImageStack is
) {
    // Initialize output image
   int stepv=Math.min(Math.max(10,targetCurrentHeight/15),30);
   int stepu=Math.min(Math.max(10,targetCurrentWidth/15),30);
   final double transformedImage [][]=new double [targetCurrentHeight][targetCurrentWidth];
   for (int v=0; v<targetCurrentHeight; v++)
      for (int u=0; u<targetCurrentWidth; u++) transformedImage[v][u]=255;

   // Ask for memory for the transformation
   double [][] transformation_x=new double [targetCurrentHeight][targetCurrentWidth];
   double [][] transformation_y=new double [targetCurrentHeight][targetCurrentWidth];

   // Compute the deformation
   computeDeformation(intervals,cx,cy,transformation_x,transformation_y);

   // Show deformed grid ........................................
   // Show deformation vectors
   for (int v=0; v<targetCurrentHeight; v+=stepv)
      for (int u=0; u<targetCurrentWidth; u+=stepu) {
         final double x = transformation_x[v][u];
         final double y = transformation_y[v][u];
           // Draw horizontal line
           int uh=u+stepu;
           if (uh<targetCurrentWidth) {
                final double xh = transformation_x[v][uh];
              final double yh = transformation_y[v][uh];
              unwarpJMiscTools.drawLine(
                 transformedImage,
                 (int)Math.round(x) ,(int)Math.round(y),
                 (int)Math.round(xh),(int)Math.round(yh),0);
          }

           // Draw vertical line
           int vv=v+stepv;
           if (vv<targetCurrentHeight) {
                final double xv = transformation_x[vv][u];
              final double yv = transformation_y[vv][u];
              unwarpJMiscTools.drawLine(
                 transformedImage,
                 (int)Math.round(x) ,(int)Math.round(y),
                 (int)Math.round(xv),(int)Math.round(yv),0);
          }
       }

   // Set it to the image stack
   FloatProcessor fp=new FloatProcessor(targetCurrentWidth,targetCurrentHeight);
   for (int v=0; v<targetCurrentHeight; v++)
      for (int u=0; u<targetCurrentWidth; u++)
         fp.putPixelValue(u,v,transformedImage[v][u]);
   is.addSlice("Deformation Grid",fp);
}

/*------------------------------------------------------------------*/
private void showDeformationVectors(
   int intervals,
   double [][]cx,
   double [][]cy,
   ImageStack is
) {
    // Initialize output image
   int stepv=Math.min(Math.max(10,targetCurrentHeight/15),30);
   int stepu=Math.min(Math.max(10,targetCurrentWidth/15),30);
   final double transformedImage [][]=new double [targetCurrentHeight][targetCurrentWidth];
   for (int v=0; v<targetCurrentHeight; v++)
      for (int u=0; u<targetCurrentWidth; u++) transformedImage[v][u]=255;

   // Ask for memory for the transformation
   double [][] transformation_x=new double [targetCurrentHeight][targetCurrentWidth];
   double [][] transformation_y=new double [targetCurrentHeight][targetCurrentWidth];

   // Compute the deformation
   computeDeformation(intervals,cx,cy,transformation_x,transformation_y);

   // Show shift field ........................................
   // Show deformation vectors
   for (int v=0; v<targetCurrentHeight; v+=stepv)
      for (int u=0; u<targetCurrentWidth; u+=stepu)
          if (targetMsk.getValue(u,v)) {
            final double x = transformation_x[v][u];
            final double y = transformation_y[v][u];
             if (sourceMsk.getValue(x,y))
                unwarpJMiscTools.drawArrow(
                   transformedImage,
                   u,v,(int)Math.round(x),(int)Math.round(y),0,2);
         }

   // Set it to the image stack
   FloatProcessor fp=new FloatProcessor(targetCurrentWidth,targetCurrentHeight);
   for (int v=0; v<targetCurrentHeight; v++)
      for (int u=0; u<targetCurrentWidth; u++)
         fp.putPixelValue(u,v,transformedImage[v][u]);
   is.addSlice("Deformation Field",fp);
}

/*-------------------------------------------------------------------*/
private void showTransformation(
   final int   intervals,
   final double [][]cx,                          // Input, spline coefficients
   final double [][]cy
){
   boolean show_deformation=false;

   // Ask for memory for the transformation
   double [][] transformation_x=new double [targetHeight][targetWidth];
   double [][] transformation_y=new double [targetHeight][targetWidth];

   // Compute the deformation
   computeDeformation(intervals,cx,cy,transformation_x,transformation_y);

   if (show_deformation) {
       unwarpJMiscTools.showImage("Transf. X",transformation_x);
       unwarpJMiscTools.showImage("Transf. Y",transformation_y);
   }

    // Compute the warped image
    FloatProcessor fp=new FloatProcessor(targetWidth,targetHeight);
    FloatProcessor fp_mask=new FloatProcessor(targetWidth,targetHeight);
    FloatProcessor fp_target=new FloatProcessor(targetWidth,targetHeight);
    int uv=0;
    for (int v=0; v<targetHeight; v++)
      for (int u=0; u<targetWidth; u++,uv++) {
          fp_target.putPixelValue(u,v,target.getImage()[uv]);
          if (!targetMsk.getValue(u,v)) {
             fp.putPixelValue(u,v,0);
             fp_mask.putPixelValue(u,v,0);
          } else {
            final double x = transformation_x[v][u];
            final double y = transformation_y[v][u];
            if (sourceMsk.getValue(x,y)) {
               source.prepareForInterpolation(x,y,ORIGINAL);
               double sval=source.interpolateI();
               fp.putPixelValue(u,v,sval);
               fp_mask.putPixelValue(u,v,255);
               } else {
                  fp.putPixelValue(u,v,0);
                  fp_mask.putPixelValue(u,v,0);
               }
         }
     }
   fp.resetMinAndMax();
   final ImageStack is = new ImageStack(targetWidth,targetHeight);

   is.addSlice("Registered Image",fp);

   if (outputLevel>-1)
      is.addSlice("Target Image",fp_target);   

   if (outputLevel>-1)
      is.addSlice("Warped Source Mask",fp_mask);
   if (outputLevel==2) {
      showDeformationVectors(intervals,cx,cy,is);
      showDeformationGrid(intervals,cx,cy,is);
   }
   output_ip.setStack("Registered Image",is);
   output_ip.setSlice(1); output_ip.getProcessor().resetMinAndMax();
   if (outputLevel>-1) output_ip.updateAndRepaintWindow();
}

/*------------------------------------------------------------------*/
private void update_current_output(
   final double []c,
   int intervals
) {
   // Set the coefficients to an interpolator
   int cYdim=intervals+3;
   int cXdim=cYdim;
   int Nk=cYdim*cXdim;
   swx.setCoefficients(c,cYdim,cXdim,0);
   swy.setCoefficients(c,cYdim,cXdim,Nk);

   // Compute the deformed image
   FloatProcessor fp=new FloatProcessor(targetWidth,targetHeight);
   int uv=0;
   for (int v=0; v<targetHeight; v++)
       for (int u=0; u<targetWidth; u++, uv++) {
         if (targetMsk.getValue(u,v)) {
             double down_u=u*factorWidth;
             double down_v=v*factorHeight;
             final double tv = (double)(down_v * intervals)/(double)(targetCurrentHeight-1) + 1.0F;
             final double tu = (double)(down_u * intervals)/(double)(targetCurrentWidth -1) + 1.0F;
            swx.prepareForInterpolation(tu,tv,ORIGINAL); double x=swx.interpolateI();
            swy.prepareForInterpolation(tu,tv,ORIGINAL); double y=swy.interpolateI();
            double up_x=x/factorWidth;
            double up_y=y/factorHeight;
            if (sourceMsk.getValue(up_x,up_y)) {
               source.prepareForInterpolation(up_x,up_y,ORIGINAL);
                fp.putPixelValue(u,v,target.getImage()[uv]-
                   source.interpolateI());
             } else
                fp.putPixelValue(u,v,0);
          } else
             fp.putPixelValue(u,v,0);
       }

   double min_val=output_ip.getProcessor().getMin();
   double max_val=output_ip.getProcessor().getMax();
   fp.setMinAndMax(min_val,max_val);
   output_ip.setProcessor("Output",fp);
   output_ip.updateImage();

   // Draw the grid on the target image ...............................
   // Some initialization
   int stepv=Math.min(Math.max(10,targetHeight/15),30);
   int stepu=Math.min(Math.max(10,targetWidth/15),30);
   final double transformedImage [][]=new double [sourceHeight][sourceWidth];
   double grid_colour=-1e-10;
   uv=0;
   for (int v=0; v<sourceHeight; v++)
      for (int u=0; u<sourceWidth; u++,uv++) {
         transformedImage[v][u]=source.getImage()[uv];
         if (transformedImage[v][u]>grid_colour) grid_colour=transformedImage[v][u];
      }

    // Draw grid
   for (int v=0; v<targetHeight+stepv; v+=stepv)
      for (int u=0; u<targetWidth+stepu; u+=stepu) {
            double down_u=u*factorWidth;
            double down_v=v*factorHeight;
            final double tv = (double)(down_v * intervals)/(double)(targetCurrentHeight-1) + 1.0F;
            final double tu = (double)(down_u * intervals)/(double)(targetCurrentWidth -1) + 1.0F;
            swx.prepareForInterpolation(tu,tv,ORIGINAL); double x=swx.interpolateI();
            swy.prepareForInterpolation(tu,tv,ORIGINAL); double y=swy.interpolateI();
            double up_x=x/factorWidth;
            double up_y=y/factorHeight;

            // Draw horizontal line
            int uh=u+stepu;
            if (uh<targetWidth+stepu) {
                double down_uh=uh*factorWidth;
                final double tuh = (double)(down_uh * intervals)/(double)(targetCurrentWidth -1) + 1.0F;
                swx.prepareForInterpolation(tuh,tv,ORIGINAL); double xh=swx.interpolateI();
                swy.prepareForInterpolation(tuh,tv,ORIGINAL); double yh=swy.interpolateI();
                double up_xh=xh/factorWidth;
                double up_yh=yh/factorHeight;
                unwarpJMiscTools.drawLine(
                  transformedImage,
                  (int)Math.round(up_x) ,(int)Math.round(up_y),
                  (int)Math.round(up_xh),(int)Math.round(up_yh),grid_colour);
            }

            // Draw vertical line
            int vv=v+stepv;
            if (vv<targetHeight+stepv) {
                double down_vv=vv*factorHeight;
                final double tvv = (double)(down_vv * intervals)/(double)(targetCurrentHeight-1) + 1.0F;
                swx.prepareForInterpolation(tu,tvv,ORIGINAL); double xv=swx.interpolateI();
                swy.prepareForInterpolation(tu,tvv,ORIGINAL); double yv=swy.interpolateI();
                double up_xv=xv/factorWidth;
                double up_yv=yv/factorHeight;
                unwarpJMiscTools.drawLine(
                  transformedImage,
                  (int)Math.round(up_x) ,(int)Math.round(up_y),
                  (int)Math.round(up_xv),(int)Math.round(up_yv),grid_colour);
           }
       }

   // Update the target image plus
   FloatProcessor fpg=new FloatProcessor(sourceWidth,sourceHeight);
   for (int v=0; v<sourceHeight; v++)
       for (int u=0; u<sourceWidth; u++)
          fpg.putPixelValue(u,v,transformedImage[v][u]);
   min_val=sourceImp.getProcessor().getMin();
   max_val=sourceImp.getProcessor().getMax();
   fpg.setMinAndMax(min_val,max_val);
   sourceImp.setProcessor(sourceImp.getTitle(),fpg);
   sourceImp.updateImage();
}

/*------------------------------------------------------------------*/
private double[] xWeight(
   final double x,
   final int xIntervals,
   final boolean extended
) {
   int length=xIntervals+1;
   int j0=0, jF=xIntervals;
   if (extended) {length+=2; j0--; jF++;}
   final double[] b = new double[length];
   final double interX = (double)xIntervals / (double)(targetCurrentWidth - 1);
   for (int j=j0; j<=jF; j++) {
      b[j-j0] = unwarpJMathTools.Bspline03(x * interX - (double)j);
   }
   return(b);
} /* end xWeight */

/*------------------------------------------------------------------*/
private double[] yWeight(
   final double y,
   final int yIntervals,
   final boolean extended
) {
   int length=yIntervals+1;
   int i0=0, iF=yIntervals;
   if (extended) {length+=2; i0--; iF++;}
   final double[] b = new double[length];
   final double interY = (double)yIntervals / (double)(targetCurrentHeight - 1);
   for (int i = i0; i<=iF; i++) {
      b[i-i0] = unwarpJMathTools.Bspline03(y * interY - (double)i);
   }
   return(b);
} /* end yWeight */

} /* end class unwarpJTransformation */