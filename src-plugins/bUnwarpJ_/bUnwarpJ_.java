
/**
 * bUnwarpJ plugin for ImageJ(C).
 * Copyright (C) 2005,2006,2007,2008 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

/**
 * ====================================================================
 *  Version: November 10th, 2008
 *  http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 * \===================================================================
 */

/**====================================================================
 * Ignacio Arganda-Carreras
 * Escuela Politecnica Superior
 * Laboratorio B-408     
 * Universidad Autonoma de Madrid
 * Ctra. de Colmenar Viejo, Km. 15
 * Madrid 28049,  Spain
 *
 * Phone: (+34) 91 497 2260
 * E-mail: Ignacio.Arganda@uam.es
 * Web: http://www.ii.uam.es/~iarganda
 *\===================================================================*/


/**
 * Old version (UnwarpJ) information: 
 * http://bigwww.epfl.ch/thevenaz/UnwarpJ/
 */

import bunwarpj.*;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.plugin.PlugIn;

import java.awt.Point;
import java.util.Stack;

/*====================================================================
|   bUnwarpJ_
\===================================================================*/

/**
 * Main class.
 * This class is a plugin for the ImageJ interface. It allows pairwise image
 * registration combining the ideas of elastic registration based on B-spline 
 * models and consistent registration.
 *
 * <p>
 * This work is an extension by Ignacio Arganda-Carreras and Jan Kybic 
 * of the previous UnwarpJ project by Carlos Oscar Sanchez Sorzano.
 * <p>
 * For more information visit the main site 
 * <a href="http://biocomp.cnb.csic.es/~iarganda/bUnwarpJ/">
 * http://biocomp.cnb.csic.es/~iarganda/bUnwarpJ/</a>
 *
 * @version 2.0 11/10/2008
 * @author Ignacio Arganda-Carreras <ignacio.arganda@uam.es>
 * @author Jan Kybic
 */
public class bUnwarpJ_ implements PlugIn
{ /* begin class bUnwarpJ_ */

	/*....................................................................
    	Private variables
 	....................................................................*/
    /** Image representation for source image */
    private ImagePlus sourceImp;
    /** Image representation for target image */
    private ImagePlus targetImp;
    
    /** minimum scale deformation */
    private static int min_scale_deformation = 0;
    /** maximum scale deformation */
    private static int max_scale_deformation = 2;
    /** algorithm mode (fast or accurate) */
    private static int mode = 1;
    
    // Transformation parameters
    /** divergence weight */
    private static double  divWeight                  = 0;
    /** curl weight */
    private static double  curlWeight                 = 0;
    /** landmarks weight */
    private static double  landmarkWeight             = 0;
    /** image similarity weight */
    private static double  imageWeight                = 1;
    /** consistency weight */
    private static double  consistencyWeight          = 10;
    /** flag for rich output (verbose option) */
    private static boolean richOutput                 = false;
    /** flag for save transformation option */
    private static boolean saveTransformation         = false;
    
    /** minimum image scale */
    private int     min_scale_image            = 0;
    /** stopping threshold */
    private static double  stopThreshold      = 1e-2;

	
    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Method to lunch the plugin.
     *
     * @param commandLine command to determine the action
     */
    public void run (final String commandLine) 
    {
    	Runtime.getRuntime().gc();
    	final ImagePlus[] imageList = createImageList();
    	if (imageList.length < 2) 
    	{
    		IJ.error("At least two (8, 16, 32-bit or RGB Color) images are required");
    		return;
    	}
	
    	final bUnwarpJDialog dialog = new bUnwarpJDialog(IJ.getInstance(), imageList, bUnwarpJ_.mode,
    			bUnwarpJ_.min_scale_deformation, bUnwarpJ_.max_scale_deformation, bUnwarpJ_.divWeight, 
    			bUnwarpJ_.curlWeight, bUnwarpJ_.landmarkWeight, bUnwarpJ_.imageWeight, bUnwarpJ_.consistencyWeight, 
    			bUnwarpJ_.stopThreshold, bUnwarpJ_.richOutput, bUnwarpJ_.saveTransformation);
	 	dialog.showDialog();
	 	
	 	// If canceled
	 	if (dialog.wasCanceled())
	 	{
	 		dialog.dispose();
    		dialog.restoreAll();
    		return;
    	}
    	
	 	// If OK
     	dialog.dispose();
    	dialog.joinThreads();        
        
        // Collect input values
		// Source and target image plus
		this.sourceImp = imageList[dialog.getNextChoiceIndex()];
		this.targetImp = imageList[dialog.getNextChoiceIndex()];
		  
		// Fast or accurate mode
		bUnwarpJ_.mode = dialog.getNextChoiceIndex();
		  
		// Min and max scale deformation level
		bUnwarpJ_.min_scale_deformation = dialog.getNextChoiceIndex();
		bUnwarpJ_.max_scale_deformation = dialog.getNextChoiceIndex();
				  
		// Weights
		bUnwarpJ_.divWeight  			= dialog.getNextNumber();
		bUnwarpJ_.curlWeight 			= dialog.getNextNumber();
		bUnwarpJ_.landmarkWeight 		= dialog.getNextNumber();
		bUnwarpJ_.imageWeight			= dialog.getNextNumber();
		bUnwarpJ_.consistencyWeight		= dialog.getNextNumber();
		bUnwarpJ_.stopThreshold			= dialog.getNextNumber();
		  
		// Verbose and save transformation options
		bUnwarpJ_.richOutput 		   	= dialog.getNextBoolean();
		bUnwarpJ_.saveTransformation 	= dialog.getNextBoolean();
        dialog.setSaveTransformation(bUnwarpJ_.saveTransformation);

        int outputLevel = 1;

        boolean showMarquardtOptim = false;

        if (richOutput)
        {
           outputLevel++;
           showMarquardtOptim = true;
        }                                 
        
        bUnwarpJFinalAction finalAction =
           new bUnwarpJFinalAction(dialog);

        finalAction.setup(sourceImp, targetImp,
           dialog.getSource(), dialog.getTarget(), dialog.getSourcePh(), dialog.getTargetPh(),
           dialog.getSourceMsk(), dialog.getTargetMsk(), 
           dialog.getSourceAffineMatrix(), dialog.getTargetAffineMatrix(),
           min_scale_deformation, max_scale_deformation,
           min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
           consistencyWeight, stopThreshold, outputLevel, showMarquardtOptim, mode);

        dialog.setFinalActionLaunched(true);
        dialog.setToolbarAllUp();
        dialog.repaintToolbar();
        Thread fa = finalAction.getThread();
	fa.start();
	try {
		fa.join();
	} catch (InterruptedException e) {
		e.printStackTrace();
	}

    } /* end run */

    /*------------------------------------------------------------------*/
    /**
     * Main method for bUnwarpJ (command line).
     *
     * @param args arguments to decide the action
     */
    public static void main(String args[]) 
    {
       if (args.length<1) 
       {
          dumpSyntax();
          System.exit(1);
       } 
       else 
       {
          if      (args[0].equals("-help"))                 dumpSyntax();
          else if (args[0].equals("-align"))                alignImagesCommandLine(args);
          else if (args[0].equals("-elastic_transform"))    elasticTransformImageCommandLine(args);
          else if (args[0].equals("-raw_transform"))        rawTransformImageCommandLine(args);
          else if (args[0].equals("-compare_elastic"))      compareElasticTransformationsCommandLine(args);
          else if (args[0].equals("-compare_elastic_raw"))  compareElasticRawTransformationsCommandLine(args);
          else if (args[0].equals("-compare_raw"))          compareRawTransformationsCommandLine(args);
          else if (args[0].equals("-convert_to_raw"))       convertToRawTransformationCommandLine(args);
          else if (args[0].equals("-compose_elastic"))      composeElasticTransformationsCommandLine(args);
          else if (args[0].equals("-compose_raw"))          composeRawTransformationsCommandLine(args);
          else if (args[0].equals("-compose_raw_elastic"))  composeRawElasticTransformationsCommandLine(args);
          else if (args[0].equals("-adapt_transform"))      adaptCoefficientsCommandLine(args);
          else 
        	  dumpSyntax();
       }
       System.exit(0);
    }

    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Method for images alignment with no graphical interface.
     *
     * @param args arguments for the program
     */
    private static void alignImagesCommandLine(String args[]) 
    {
       if (args.length < 13)
       {
           dumpSyntax();
           System.exit(0);
       }
       // Read input parameters
       String fn_target = args[1];
       String fn_target_mask = args[2];
       String fn_source = args[3];
       String fn_source_mask = args[4];
       int min_scale_deformation = ((Integer) new Integer(args[5])).intValue();
       int max_scale_deformation = ((Integer) new Integer(args[6])).intValue();
       double  divWeight = ((Double) new Double(args[7])).doubleValue();
       double  curlWeight = ((Double) new Double(args[8])).doubleValue();
       double  imageWeight = ((Double) new Double(args[9])).doubleValue();

       double  consistencyWeight = ((Double) new Double(args[10])).doubleValue();

       String fn_out_1 = args[11];
       String fn_out_2 = args[12];
       double  landmarkWeight = 0;
       String fn_landmark = "";
       String fn_affine_1 = "";
       String fn_affine_2 = "";
       
       if (args.length==16) 
       {
          if(args[13].equals("-landmark"))
          {
              landmarkWeight = ((Double) new Double(args[14])).doubleValue();
              fn_landmark = args[15];
          }
          else if(args[13].equals("-affine"))
          {
              fn_affine_1 = args[14];
              fn_affine_2 = args[15];
          }
       }

       // Show parameters
       IJ.write("Target image           : " + fn_target);
       IJ.write("Target mask            : " + fn_target_mask);
       IJ.write("Source image           : " + fn_source);
       IJ.write("Source mask            : " + fn_source_mask);
       IJ.write("Min. Scale Deformation : " + min_scale_deformation);
       IJ.write("Max. Scale Deformation : " + max_scale_deformation);
       IJ.write("Div. Weight            : " + divWeight);
       IJ.write("Curl Weight            : " + curlWeight);
       IJ.write("Image Weight           : " + imageWeight);
       IJ.write("Consistency Weight     : " + consistencyWeight);
       IJ.write("Output 1               : " + fn_out_1);
       IJ.write("Output 2               : " + fn_out_2);
       IJ.write("Landmark Weight        : " + landmarkWeight);
       IJ.write("Landmark file          : " + fn_landmark);
       IJ.write("Affine matrix file 1   : " + fn_affine_1);
       IJ.write("Affine matrix file 2   : " + fn_affine_2);

       // Produce side information
       int     imagePyramidDepth=max_scale_deformation-min_scale_deformation+1;
       int     min_scale_image = 0;
       double  stopThreshold = 1e-2;  // originally -2
       int     outputLevel = -1;
       boolean showMarquardtOptim = false;
       int     accurate_mode = 1;
       boolean saveTransf = true;

       // First transformation file name.
       String fn_tnf_1 = "";
       int dot = fn_out_1.lastIndexOf('.');
       if (dot == -1) 
           fn_tnf_1 = fn_out_1 + "_transf.txt";
       else           
           fn_tnf_1 = fn_out_1.substring(0, dot) + "_transf.txt";
       
       // Second transformation file name.
       String fn_tnf_2 = "";
       dot = fn_out_2.lastIndexOf('.');
       if (dot == -1) 
           fn_tnf_2 = fn_out_2 + "_transf.txt";
       else           
           fn_tnf_2 = fn_out_2.substring(0, dot) + "_transf.txt";

       // Open target
       Opener opener = new Opener();
       ImagePlus targetImp;
       targetImp = opener.openImage(fn_target);
       
       bUnwarpJImageModel target =
          new bUnwarpJImageModel(targetImp.getProcessor(), true);
       
       target.setPyramidDepth(imagePyramidDepth+min_scale_image);
       target.getThread().start();
       
       bUnwarpJMask targetMsk = new bUnwarpJMask(targetImp.getProcessor(),false);
       
       if (fn_target_mask.equalsIgnoreCase(new String("NULL")) == false)
           targetMsk.readFile(fn_target_mask);
       
       bUnwarpJPointHandler targetPh = null;

       // Open source
       boolean bIsReverse = true;

       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);   

       bUnwarpJImageModel source =
          new bUnwarpJImageModel(sourceImp.getProcessor(), bIsReverse);

       source.setPyramidDepth(imagePyramidDepth + min_scale_image);
       source.getThread().start();

       bUnwarpJMask sourceMsk = new bUnwarpJMask(sourceImp.getProcessor(), false);

       if (fn_source_mask.equalsIgnoreCase(new String("NULL")) == false)
           sourceMsk.readFile(fn_source_mask);
       
       bUnwarpJPointHandler sourcePh=null;

       // Load landmarks
       if (fn_landmark.equals("") == false)
       {
          Stack<Point> sourceStack = new Stack<Point>();
          Stack<Point> targetStack = new Stack<Point>();
          bUnwarpJMiscTools.loadPoints(fn_landmark, sourceStack, targetStack);

          sourcePh  = new bUnwarpJPointHandler(sourceImp);
          targetPh  = new bUnwarpJPointHandler(targetImp);

          while ((!sourceStack.empty()) && (!targetStack.empty())) 
          {
             Point sourcePoint = (Point)sourceStack.pop();
             Point targetPoint = (Point)targetStack.pop();
             sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
             targetPh.addPoint(targetPoint.x, targetPoint.y);
          }
       }
       
       // Load initial affine matrices
       double[][] sourceAffineMatrix = null;
       if(fn_affine_1.equals("") == false && fn_affine_1.equalsIgnoreCase(new String("NULL")) == false)
       {
           sourceAffineMatrix = new double[2][3];
           bUnwarpJMiscTools.loadAffineMatrix(fn_affine_1, sourceAffineMatrix);
       }
       double[][] targetAffineMatrix = null;
       if(fn_affine_2.equals("") == false && fn_affine_2.equalsIgnoreCase(new String("NULL")) == false)
       {
           targetAffineMatrix = new double[2][3];
           bUnwarpJMiscTools.loadAffineMatrix(fn_affine_2, targetAffineMatrix);
       }

       // Join threads
       try 
       {
           source.getThread().join();
           target.getThread().join();
       } 
       catch (InterruptedException e) 
       {
           IJ.error("Unexpected interruption exception " + e);
       }

       // Perform registration
       ImagePlus output_ip_1 = new ImagePlus();
       ImagePlus output_ip_2 = new ImagePlus();
       bUnwarpJDialog dialog = null;


       final bUnwarpJTransformation warp = new bUnwarpJTransformation(
         sourceImp, targetImp, source, target, sourcePh, targetPh,
         sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
         min_scale_deformation, max_scale_deformation, min_scale_image, divWeight, 
         curlWeight, landmarkWeight, imageWeight, consistencyWeight, stopThreshold, 
         outputLevel, showMarquardtOptim, accurate_mode,
         saveTransf, fn_tnf_1, fn_tnf_2, output_ip_1, output_ip_2, dialog);

       IJ.write("\nRegistering...\n");
       
       warp.doRegistration();

       // Save results
       FileSaver fs = new FileSaver(output_ip_1);
       fs.saveAsTiff(fn_out_1);
       fs = new FileSaver(output_ip_2);
       fs.saveAsTiff(fn_out_2);
    }

    /*------------------------------------------------------------------*/
    /**
     * Create a list with the open images in ImageJ that bUnwarpJ can
     * process.
     *
     * @return array of references to the open images in bUnwarpJ
     */
    private ImagePlus[] createImageList () 
    {
       final int[] windowList = WindowManager.getIDList();
       final Stack <ImagePlus> stack = new Stack <ImagePlus>();
       for (int k = 0; ((windowList != null) && (k < windowList.length)); k++) 
       {
          final ImagePlus imp = WindowManager.getImage(windowList[k]);
          final int inputType = imp.getType();

          // Since October 6th, 2008, bUnwarpJ can deal with 8, 16, 32-bit grayscale 
          // and RGB Color images.
          if ((imp.getStackSize() == 1) || (inputType == ImagePlus.GRAY8) || (inputType == ImagePlus.GRAY16)
             || (inputType == ImagePlus.GRAY32) || (inputType == ImagePlus.COLOR_RGB)) 
          {
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
    /**
     * Method to write the syntax of the program in the command line.
     */
    private static void dumpSyntax () 
    {
       IJ.write("Purpose: Consistent and elastic registration of two images.");
       IJ.write(" ");
       IJ.write("Usage: bUnwarpj_ ");
       IJ.write("  -help                       : SHOW THIS MESSAGE");
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
       IJ.write("          Consistency_weight  : Weight of the deformation consistency");
       IJ.write("          Output image 1      : Output result 1 in TIFF");
       IJ.write("          Output image 2      : Output result 2 in TIFF");
       IJ.write("          Optional parameters :");
       IJ.write("             -landmarks        ");
       IJ.write("                   Landmark_weight  : Weight of the landmarks");
       IJ.write("                   Landmark_file    : Landmark file");
       IJ.write("             OR -affine        ");
       IJ.write("                   Affine_file_1    : Initial source affine matrix transformation");
       IJ.write("                   Affine_file_2    : Initial target affine matrix transformation");    
       IJ.write("");
       IJ.write("  -elastic_transform          : TRANSFORM A SOURCE IMAGE WITH A GIVEN ELASTIC DEFORMATION");
       IJ.write("          target_image        : In any image format");
       IJ.write("          source_image        : In any image format");
       IJ.write("          transformation_file : As saved by bUnwarpJ in elastic format");
       IJ.write("          Output image        : Output result in TIFF");
       IJ.write("");
       IJ.write("  -raw_transform              : TRANSFORM A SOURCE IMAGE WITH A GIVEN RAW DEFORMATION");
       IJ.write("          target_image        : In any image format");
       IJ.write("          source_image        : In any image format");
       IJ.write("          transformation_file : As saved by bUnwarpJ in raw format");
       IJ.write("          Output image        : Output result in TIFF");
       IJ.write("");
       IJ.write("  -compare_elastic                   : COMPARE 2 OPPOSITE ELASTIC DEFORMATIONS (BY WARPING INDEX)");
       IJ.write("          target_image               : In any image format");
       IJ.write("          source_image               : In any image format");
       IJ.write("          target_transformation_file : As saved by bUnwarpJ");
       IJ.write("          source_transformation_file : As saved by bUnwarpJ");
       IJ.write("");
       IJ.write("  -compare_elastic_raw                : COMPARE AN ELASTIC DEFORMATION WITH A RAW DEFORMATION (BY WARPING INDEX)");
       IJ.write("          target_image                : In any image format");
       IJ.write("          source_image                : In any image format");
       IJ.write("          Elastic Transformation File : As saved by bUnwarpJ in elastic format");
       IJ.write("          Raw Transformation File     : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -compare_raw                       : COMPARE 2 ELASTIC DEFORMATIONS (BY WARPING INDEX)");
       IJ.write("          target_image               : In any image format");
       IJ.write("          source_image               : In any image format");
       IJ.write("          Raw Transformation File 1  : As saved by bUnwarpJ in raw format");
       IJ.write("          Raw Transformation File 2  : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -convert_to_raw                           : CONVERT AN ELASTIC DEFORMATION INTO RAW FORMAT");
       IJ.write("          target_image                      : In any image format");
       IJ.write("          source_image                      : In any image format");
       IJ.write("          Input Elastic Transformation File : As saved by bUnwarpJ in elastic format");
       IJ.write("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -compose_elastic                          : COMPOSE TWO ELASTIC DEFORMATIONS");
       IJ.write("          target_image                      : In any image format");
       IJ.write("          source_image                      : In any image format");
       IJ.write("          Elastic Transformation File 1     : As saved by bUnwarpJ in elastic format");
       IJ.write("          Elastic Transformation File 2     : As saved by bUnwarpJ in elastic format");
       IJ.write("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -compose_raw                              : COMPOSE TWO RAW DEFORMATIONS");
       IJ.write("          target_image                      : In any image format");
       IJ.write("          source_image                      : In any image format");
       IJ.write("          Raw Transformation File 1         : As saved by bUnwarpJ in raw format");
       IJ.write("          Raw Transformation File 2         : As saved by bUnwarpJ in raw format");
       IJ.write("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -compose_raw_elastic                      : COMPOSE A RAW DEFORMATION WITH AN ELASTIC DEFORMATION");
       IJ.write("          target_image                      : In any image format");
       IJ.write("          source_image                      : In any image format");
       IJ.write("          Raw Transformation File           : As saved by bUnwarpJ in raw format");
       IJ.write("          Elastic Transformation File       : As saved by bUnwarpJ in elastic format");       
       IJ.write("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -adapt_transform                           : ADAPT AN ELASTIC DEFORMATION GIVEN A NEW IMAGE SIZE");
       IJ.write("          target_image                       : In any image format");
       IJ.write("          source_image                       : In any image format");
       IJ.write("          Input Elastic Transformation File  : As saved by bUnwarpJ in elastic format");
       IJ.write("          Output Elastic Transformation File : As saved by bUnwarpJ in elastic format");
       IJ.write("          Image Size Factor                  : Integer (2, 4, 8...)");
       IJ.write("");
       IJ.write("Examples:");
       IJ.write("Align two images without landmarks and without mask");
       IJ.write("   bUnwarpj_ -align target.jpg NULL source.jpg NULL 0 2 0.1 0.1 1 10 output_1.tif output_2.tif");
       IJ.write("Align two images with landmarks and mask");
       IJ.write("   bUnwarpj_ -align target.tif target_mask.tif source.tif source_mask.tif 0 2 0.1 0.1 1 10 output_1.tif output_2.tif -landmarks 1 landmarks.txt");
       IJ.write("Align two images with landmarks and initial affine transformations");
       IJ.write("   bUnwarpj_ -align target.tif target_mask.tif source.tif source_mask.tif 0 2 0.1 0.1 1 10 output_1.tif output_2.tif -affine affine_mat1.txt affine_mat2.txt");       
       IJ.write("Align two images using only landmarks");
       IJ.write("   bUnwarpj_ -align target.jpg NULL source.jpg NULL 0 2 0.1 0.1 0 0 output.tif_1 output_2.tif -landmarks 1 landmarks.txt");
       IJ.write("Transform the source image with a previously computed elastic transformation");
       IJ.write("   bUnwarpj_ -elastic_transform target.jpg source.jpg elastic_transformation.txt output.tif");       
       IJ.write("Transform the source image with a previously computed raw transformation");
       IJ.write("   bUnwarpj_ -raw_transform target.jpg source.jpg raw_transformation.txt output.tif");
       IJ.write("Calculate the warping index of two opposite elastic transformations");
       IJ.write("   bUnwarpj_ -compare_elastic target.jpg source.jpg source_transformation.txt target_transformation.txt");
       IJ.write("Calculate the warping index between an elastic transformation and a raw transformation");
       IJ.write("   bUnwarpj_ -compare_elastic_raw target.jpg source.jpg elastic_transformation.txt raw_transformation.txt");
       IJ.write("Calculate the warping index between two raw transformations");
       IJ.write("   bUnwarpj_ -compare_raw target.jpg source.jpg raw_transformation_1.txt raw_transformation_2.txt");
       IJ.write("Convert an elastic transformation into raw format");
       IJ.write("   bUnwarpj_ -convert_to_raw target.jpg source.jpg elastic_transformation.txt output_raw_transformation.txt");
       IJ.write("Compose two elastic transformations ");
       IJ.write("   bUnwarpj_ -compose_elastic target.jpg source.jpg elastic_transformation_1.txt elastic_transformation_2.txt output_raw_transformation.txt");
       IJ.write("Compose two raw transformations ");
       IJ.write("   bUnwarpj_ -compose_raw target.jpg source.jpg raw_transformation_1.txt raw_transformation_2.txt output_raw_transformation.txt");
       IJ.write("Compose a raw transformation with an elastic transformation ");
       IJ.write("   bUnwarpj_ -compose_raw_elastic target.jpg source.jpg raw_transformation.txt elastic_transformation.txt output_raw_transformation.txt");
       IJ.write("Adapt an elastic transformation to a new image size ");
       IJ.write("   bUnwarpj_ -adapt_transform target.jpg source.jpg input_transformation.txt output_transformation.txt 2");
    } /* end dumpSyntax */

    /*------------------------------------------------------------------*/
    /**
     * Method to adapt coefficients to new image size. The factor between
     * the old and the new image size is expected to be a power of 2, positive
     * or negative (8, 4, 2, 0.5, 0.25, etc).
     *
     * @param args program arguments
     */
    private static void adaptCoefficientsCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target         = args[1];
       String fn_source         = args[2];
       String fn_tnf            = args[3];
       String fn_out            = args[4];
       String sImageSizeFactor  = args[5];

       // Show parameters
       IJ.write("Target image                 : " + fn_target);
       IJ.write("Source image                 : " + fn_source);
       IJ.write("Input Transformation file    : " + fn_tnf);
       IJ.write("Output Transformation file   : " + fn_out);
       IJ.write("Image Size Factor            : " + sImageSizeFactor);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       bUnwarpJImageModel source = new bUnwarpJImageModel(sourceImp.getProcessor(), false);
       source.setPyramidDepth(0);
       source.getThread().start();

       // Load transformation
       int intervals=bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);
       double [][]cx=new double[intervals+3][intervals+3];
       double [][]cy=new double[intervals+3][intervals+3];
       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx, cy);

       // Join threads
       try {
          source.getThread().join();
       } catch (InterruptedException e) {
          IJ.error("Unexpected interruption exception " + e);
       }

       // adapt coefficients.
       double dImageSizeFactor = Double.parseDouble(sImageSizeFactor);
       
       for(int i = 0; i < (intervals+3); i++)
           for(int j = 0; j < (intervals+3); j++)
           {
                cx[i][j] *= dImageSizeFactor;
                cy[i][j] *= dImageSizeFactor;
           }
       
       // Save transformation
       bUnwarpJMiscTools.saveElasticTransformation(intervals, cx, cy, fn_out);
       
    } /* end adaptCoefficientsCommandLine */    
    
    /*------------------------------------------------------------------*/
    /**
     * Method to transform an image given an elastic deformation.
     *
     * @param args program arguments
     */
    private static void elasticTransformImageCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf    = args[3];
       String fn_out    = args[4];

       // Show parameters
       IJ.write("Target image           : " + fn_target);
       IJ.write("Source image           : " + fn_source);
       IJ.write("Transformation file    : " + fn_tnf);
       IJ.write("Output:                : " + fn_out);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
   
       bUnwarpJImageModel source = null;

       source = new bUnwarpJImageModel(sourceImp.getProcessor(), false);
       source.setPyramidDepth(0);
       source.getThread().start();

       // Load transformation
       int intervals=bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);
       double [][]cx=new double[intervals+3][intervals+3];
       double [][]cy=new double[intervals+3][intervals+3];
       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx, cy);

       // Join threads

       try {
    	   source.getThread().join();
       } catch (InterruptedException e) {
    	   IJ.error("Unexpected interruption exception " + e);
       }


       // Apply transformation to source
       bUnwarpJMiscTools.applyTransformationToSource(
    		   sourceImp, targetImp, source, intervals, cx, cy);

       // Save results
       FileSaver fs = new FileSaver(sourceImp);
       boolean ret = fs.saveAsTiff(fn_out);
       if(ret == false)
    	   System.out.println("Error when saving file " + fn_out);
       else
    	   System.out.println("Saved file " + fn_out);
       
    } /* end elasticTransformIMageCommandLine */

    /*------------------------------------------------------------------*/
    /**
     * Method to transform an image given an raw deformation.
     *
     * @param args program arguments
     */
    private static void rawTransformImageCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf    = args[3];
       String fn_out    = args[4];

       // Show parameters
       IJ.write("Target image           : " + fn_target);
       IJ.write("Source image           : " + fn_source);
       IJ.write("Transformation file    : " + fn_tnf);
       IJ.write("Output:                : " + fn_out);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       bUnwarpJImageModel source = new bUnwarpJImageModel(sourceImp.getProcessor(), false);
       source.setPyramidDepth(0);
       source.getThread().start();

       double [][]transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][]transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];

       bUnwarpJMiscTools.loadRawTransformation(fn_tnf, transformation_x, transformation_y);

       // Apply transformation
       bUnwarpJMiscTools.applyRawTransformationToSource(sourceImp, targetImp, source, transformation_x, transformation_y);

       // Save results
       FileSaver fs = new FileSaver(sourceImp);
       boolean ret = fs.saveAsTiff(fn_out);
       if(ret == false)
    	   System.out.println("Error when saving file " + fn_out);
       else
    	   System.out.println("Saved file " + fn_out);
       
    } /* end rawTransformImageCommandLine */    
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare two opposite elastic deformations through the 
     * warping index.
     *
     * @param args program arguments
     */
    private static void compareElasticTransformationsCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_1   = args[3];
       String fn_tnf_2   = args[4];

       // Show parameters
       IJ.write("Target image                  : " + fn_target);
       IJ.write("Source image                  : " + fn_source);
       IJ.write("Target Transformation file    : " + fn_tnf_1);
       IJ.write("Source Transformation file    : " + fn_tnf_2);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       // First deformation.
       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_2);

       double [][]cx_direct = new double[intervals+3][intervals+3];
       double [][]cy_direct = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_2, cx_direct, cy_direct);      

       intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_1);

       double [][]cx_inverse = new double[intervals+3][intervals+3];
       double [][]cy_inverse = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_1, cx_inverse, cy_inverse);
       
       double warpingIndex = bUnwarpJMiscTools.warpingIndex(sourceImp, targetImp, intervals, cx_direct, cy_direct, cx_inverse, cy_inverse);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!");             
       
    } /* end method compareElasticTransformationsCommandLine */
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare an elastic deformation with a raw deformation
     * through the warping index (both transformations having same direction).
     *
     * @param args program arguments
     */
    private static void compareElasticRawTransformationsCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_elastic = args[3];
       String fn_tnf_raw     = args[4];

       // Show parameters
       IJ.write("Target image                  : " + fn_target);
       IJ.write("Source image                  : " + fn_source);
       IJ.write("Elastic Transformation file   : " + fn_tnf_elastic);
       IJ.write("Raw Transformation file       : " + fn_tnf_raw);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx_direct = new double[intervals+3][intervals+3];
       double [][]cy_direct = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic, cx_direct, cy_direct);
      
       // We load the transformation raw file.
       double[][] transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw, transformation_x, 
               transformation_y);
       
       double warpingIndex = bUnwarpJMiscTools.rawWarpingIndex(sourceImp, targetImp, 
               intervals, cx_direct, cy_direct, transformation_x, transformation_y);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!");             
       
    } /* end method compareElasticRawTransformationCommandLine */    
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare two raw deformations through the warping index
     * (both transformations having same direction).
     *
     * @param args program arguments
     */
    private static void compareRawTransformationsCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_1   = args[3];
       String fn_tnf_2   = args[4];

       // Show parameters
       IJ.write("Target image                  : " + fn_target);
       IJ.write("Source image                  : " + fn_source);
       IJ.write("Target Transformation file    : " + fn_tnf_1);
       IJ.write("Source Transformation file    : " + fn_tnf_2);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       // We load the transformation raw file.
       double[][] transformation_x_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_1, transformation_x_1, transformation_y_1);
       
       // We load the transformation raw file.
       double[][] transformation_x_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_2, transformation_x_2, transformation_y_2);
       
       double warpingIndex = bUnwarpJMiscTools.rawWarpingIndex(sourceImp, targetImp, 
               transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!");            
       
    } /* end method compareRawTransformationsCommandLine */

    /*------------------------------------------------------------------*/
    /**
     * Method to convert an elastic deformations into raw format.
     *
     * @param args program arguments
     */
    private static void convertToRawTransformationCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_elastic = args[3];
       String fn_tnf_raw     = args[4];
       

       // Show parameters
       IJ.write("Target image                      : " + fn_target);
       IJ.write("Source image                      : " + fn_source);
       IJ.write("Input Elastic Transformation file : " + fn_tnf_elastic);
       IJ.write("Ouput Raw Transformation file     : " + fn_tnf_raw);

       // Open target
       Opener opener = new Opener();
       ImagePlus targetImp;
       targetImp = opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx = new double[intervals+3][intervals+3];
       double [][]cy = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic, cx, cy);
       
       
       // We load the transformation raw file.
       double[][] transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
       
       bUnwarpJMiscTools.convertElasticTransformationToRaw(targetImp, intervals, cx, cy, transformation_x, transformation_y); 
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw, targetImp.getWidth(), targetImp.getHeight(), transformation_x, transformation_y);
       
    } /* end method convertToRawTransformationCommandLine */    

    /*------------------------------------------------------------------*/
    /**
     * Method to compose two raw deformations.
     *
     * @param args program arguments
     */
    private static void composeRawTransformationsCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_raw_1   = args[3];
       String fn_tnf_raw_2   = args[4];
       String fn_tnf_raw_out = args[5];
       

       // Show parameters
       IJ.write("Target image                      : " + fn_target);
       IJ.write("Source image                      : " + fn_source);
       IJ.write("Input Raw Transformation file 1   : " + fn_tnf_raw_1);
       IJ.write("Input Raw Transformation file 2   : " + fn_tnf_raw_2);
       IJ.write("Output Raw Transformation file    : " + fn_tnf_raw_out);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       // We load the first transformation raw file.
       double[][] transformation_x_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw_1, transformation_x_1, transformation_y_1);
              
       // We load the second transformation raw file.
       double[][] transformation_x_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw_2, transformation_x_2, transformation_y_2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeRawTransformations(targetImp.getWidth(), targetImp.getHeight(), 
               transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2, 
               outputTransformation_x, outputTransformation_y);
              
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw_out, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);
       
    } /* end method composeRawTransformationsCommandLine */     
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compose two elastic deformations.
     *
     * @param args program arguments
     */
    private static void composeElasticTransformationsCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_elastic_1   = args[3];
       String fn_tnf_elastic_2   = args[4];
       String fn_tnf_raw = args[5];
       

       // Show parameters
       IJ.write("Target image                        : " + fn_target);
       IJ.write("Source image                        : " + fn_source);
       IJ.write("Input Elastic Transformation file 1 : " + fn_tnf_elastic_1);
       IJ.write("Input Elastic Transformation file 2 : " + fn_tnf_elastic_2);
       IJ.write("Output Raw Transformation file      : " + fn_tnf_raw);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic_1);

       double [][]cx1 = new double[intervals+3][intervals+3];
       double [][]cy1 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic_1, cx1, cy1);

       intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic_2);

       double [][]cx2 = new double[intervals+3][intervals+3];
       double [][]cy2 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic_2, cx2, cy2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeElasticTransformations(targetImp, intervals, 
               cx1, cy1, cx2, cy2, outputTransformation_x, outputTransformation_y);
       
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);       
       
    } /* end method composeElasticTransformationsCommandLine */
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compose a raw deformation with an elastic deformation.
     *
     * @param args program arguments
     */
    private static void composeRawElasticTransformationsCommandLine(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_raw_in = args[3];
       String fn_tnf_elastic = args[4];
       String fn_tnf_raw_out = args[5];
       

       // Show parameters
       IJ.write("Target image                      : " + fn_target);
       IJ.write("Source image                      : " + fn_source);
       IJ.write("Input Raw Transformation file     : " + fn_tnf_raw_in);
       IJ.write("Input Elastic Transformation file : " + fn_tnf_elastic);
       IJ.write("Output Raw Transformation file    : " + fn_tnf_raw_out);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");

       // We load the transformation raw file.
       double[][] transformation_x_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw_in, transformation_x_1, transformation_y_1);              

       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx2 = new double[intervals+3][intervals+3];
       double [][]cy2 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic, cx2, cy2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeRawElasticTransformations(targetImp, intervals, 
               transformation_x_1, transformation_y_1, cx2, cy2, outputTransformation_x, outputTransformation_y);
       
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw_out, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);       
       
    } /* end method composeRawElasticTransformationsCommandLine */
    
    /*------------------------------------------------------------------*/
    /**
     * Method to transform the source image given an elastic deformation.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-elastic_transform        		: TRANSFORM A SOURCE IMAGE WITH A GIVEN ELASTIC DEFORMATION<br>
     * 		     target_image       	: In any image format<br>
     *           source_image       	: In any image format<br>
     *           transformation_file 	: As saved by bUnwarpJ in elastic format<br>
     *           Output image 		    : Output result in TIFF<br>
     *           
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param transformationFileName transformation file name (with path)
     * @param outputFileName output file name (with path)
     */
    public static void elasticTransformImageMacro(
    		String targetImageName,
    		String sourceImageName,
    		String transformationFileName,
    		String outputFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, transformationFileName, outputFileName};
    	elasticTransformImageCommandLine(args);
    }
    /* end elasticTransformImageMacro */

    /*------------------------------------------------------------------*/
    /**
     * Method to transform the source image given an raw deformation.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-raw_transform              : TRANSFORM A SOURCE IMAGE WITH A GIVEN RAW DEFORMATION<br>
     *          target_image        : In any image format<br>
     *          source_image        : In any image format<br>
     *          transformation_file : As saved by bUnwarpJ in raw format<br>
     *          Output image        : Output result in TIFF<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param transformationFileName transformation file name (with path)
     * @param outputFileName output file name (with path)
     */
    public static void rawTransformImageMacro(
    		String targetImageName,
    		String sourceImageName,
    		String transformationFileName,
    		String outputFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, transformationFileName, outputFileName};
    	rawTransformImageCommandLine(args);
    }
    /* end rawTransformImageMacro */    
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare two opposite elastic transformations by warping index.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-compare_elastic                   : COMPARE 2 OPPOSITE ELASTIC DEFORMATIONS (BY WARPING INDEX)<br>
     *          target_image               : In any image format<br>
     *          source_image               : In any image format<br>
     *          target_transformation_file : As saved by bUnwarpJ<br>
     *          source_transformation_file : As saved by bUnwarpJ<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param targetTransfFileName target transformation file name (with path)
     * @param sourceTransfFileName source transformation file name (with path)
     */
    public static void compareElasticTransformationsMacro(
    		String targetImageName,
    		String sourceImageName,
    		String targetTransfFileName,
    		String sourceTransfFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, targetTransfFileName, sourceTransfFileName};
    	compareElasticTransformationsCommandLine(args);
    }
    /* end compareElasticTransformationsMacro */ 
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare an elastic and a raw transformations by warping index.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-compare_elastic_raw                : COMPARE AN ELASTIC DEFORMATION WITH A RAW DEFORMATION (BY WARPING INDEX)<br>
     *          target_image               : In any image format<br>
     *          source_image               : In any image format<br>
     *          target_transformation_file : As saved by bUnwarpJ<br>
     *          source_transformation_file : As saved by bUnwarpJ<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param targetTransfFileName target transformation file name (with path)
     * @param sourceTransfFileName source transformation file name (with path)
     */
    public static void compareElasticRawTransformationsMacro(
    		String targetImageName,
    		String sourceImageName,
    		String targetTransfFileName,
    		String sourceTransfFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, targetTransfFileName, sourceTransfFileName};
    	compareElasticRawTransformationsCommandLine(args);
    }
    /* end compareElasticRawTransformationsMacro */ 
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare two raw transformations by warping index.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-compare_raw                       : COMPARE 2 ELASTIC DEFORMATIONS (BY WARPING INDEX)<br>
     *          target_image               : In any image format<br>
     *          source_image               : In any image format<br>
     *          Raw Transformation File 1  : As saved by bUnwarpJ in raw format<br>
     *          Raw Transformation File 2  : As saved by bUnwarpJ in raw format<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param targetTransfFileName target transformation file name (with path)
     * @param sourceTransfFileName source transformation file name (with path)
     */
    public static void compareRawTransformationsMacro(
    		String targetImageName,
    		String sourceImageName,
    		String targetTransfFileName,
    		String sourceTransfFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, targetTransfFileName, sourceTransfFileName};
    	compareRawTransformationsCommandLine(args);
    }
    /* end compareRawTransformationsMacro */     
    
    /*------------------------------------------------------------------*/
    /**
     * Method to convert an elastic deformation into raw format.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-convert_to_raw        					  : CONVERT AN ELASTIC DEFORMATION INTO RAW FORMAT<br>
     *          target_image   					  : In any image format<br>
     *          source_image   				 	  : In any image format<br>
     *          Input Elastic Transformation File : As saved by bUnwarpJ in elastic format<br>
     *          Output Raw Transformation File    : As saved by bUnwarpJ in raw format<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param inputElasticTransfFileName input transformation file name (with path)
     * @param outputRawTransfFileName output transformation file name (with path)
     */
    public static void convertToRawTransformationMacro(
    		String targetImageName,
    		String sourceImageName,
    		String inputElasticTransfFileName,
    		String outputRawTransfFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, inputElasticTransfFileName, outputRawTransfFileName};
    	convertToRawTransformationCommandLine(args);
    }
    /* end convertToRawTransformationsMacro */ 
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compose two elastic transformations.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-compose_elastic                          : COMPOSE TWO ELASTIC DEFORMATIONS<br>
     *          target_image                      : In any image format<br>
     *          source_image                      : In any image format<br>
     *          Elastic Transformation File 1     : As saved by bUnwarpJ in elastic format<br>
     *          Elastic Transformation File 2     : As saved by bUnwarpJ in elastic format<br>
     *          Output Raw Transformation File    : As saved by bUnwarpJ in raw format<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param inputElasticTransfFileName1 first input transformation file name (with path)
     * @param inputElasticTransfFileName2 second input transformation file name (with path)
     * @param outputRawTransfFileName output transformation file name (with path)
     */
    public static void composeElasticTransformationsMacro(
    		String targetImageName,
    		String sourceImageName,
    		String inputElasticTransfFileName1,
    		String inputElasticTransfFileName2,
    		String outputRawTransfFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, inputElasticTransfFileName1, inputElasticTransfFileName2, outputRawTransfFileName};
    	composeElasticTransformationsCommandLine(args);
    }
    /* end composeElasticTransformationsMacro */     
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compose two raw transformations.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-compose_raw                              : COMPOSE TWO RAW DEFORMATIONS<br>
     *          target_image                      : In any image format<br>
     *          source_image                      : In any image format<br>
     *          Raw Transformation File 1         : As saved by bUnwarpJ in raw format<br>
     *          Raw Transformation File 2         : As saved by bUnwarpJ in raw format<br>
     *          Output Raw Transformation File    : As saved by bUnwarpJ in raw format<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param inputRawTransfFileName1 first input transformation file name (with path)
     * @param inputRawTransfFileName2 second input transformation file name (with path)
     * @param outputRawTransfFileName output transformation file name (with path)
     */
    public static void composeRawTransformationsMacro(
    		String targetImageName,
    		String sourceImageName,
    		String inputRawTransfFileName1,
    		String inputRawTransfFileName2,
    		String outputRawTransfFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, inputRawTransfFileName1, inputRawTransfFileName2, outputRawTransfFileName};
    	composeRawTransformationsCommandLine(args);
    }
    /* end composeRawTransformationsMacro */    
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compose a raw transformation with an elastic transformation.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a> 
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-compose_raw_elastic                      : COMPOSE A RAW DEFORMATION WITH AN ELASTIC DEFORMATION<br>
     *          target_image                      : In any image format<br>
     *          source_image                      : In any image format<br>
     *          Raw Transformation File           : As saved by bUnwarpJ in raw format<br>
     *          Elastic Transformation File       : As saved by bUnwarpJ in elastic format<br>
     *          Output Raw Transformation File    : As saved by bUnwarpJ in raw format<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param inputRawTransfFileName input raw transformation file name (with path)
     * @param inputElasticTransfFileName input elastic transformation file name (with path)
     * @param outputRawTransfFileName output transformation file name (with path)
     */
    public static void composeRawElasticTransformationsMacro(
    		String targetImageName,
    		String sourceImageName,
    		String inputRawTransfFileName,
    		String inputElasticTransfFileName,
    		String outputRawTransfFileName) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, inputRawTransfFileName, inputElasticTransfFileName, outputRawTransfFileName};
    	composeRawElasticTransformationsCommandLine(args);
    }
    /* end composeRawElasticTransformationsMacro */     
    
    /*------------------------------------------------------------------*/
    /**
     * Method to adapt an elastic transformation given a new image size.
     * To be called by the macro language instruction "call":
     * <a href="http://rsb.info.nih.gov/ij/developer/macro/functions.html#call">
     * http://rsb.info.nih.gov/ij/developer/macro/functions.html#call</a>     
     * <p>
     * It calls the main command line method with the following option:<br>
     * 	-adapt_transform                           : ADAPT AN ELASTIC DEFORMATION GIVEN A NEW IMAGE SIZE<br>
     *          target_image                       : In any image format<br>
     *          source_image                       : In any image format<br>
     *          Input Elastic Transformation File  : As saved by bUnwarpJ in elastic format<br>
     *          Output Elastic Transformation File : As saved by bUnwarpJ in elastic format<br>
     *          Image Size Factor                  : Double (0.25, 0.5, 2, 4, 8...)<br>
     *          
     * @param targetImageName target image file name (with path)
     * @param sourceImageName source image file name (with path)
     * @param inputElasticTransfFileName input elastic transformation file name (with path)
     * @param outputElasticTransfFileName output elastic transformation file name (with path)
     * @param sizeFactor double size factor (between old and new image)
     */
    public static void adaptCoefficientsMacro(
    		String targetImageName,
    		String sourceImageName,
    		String inputElasticTransfFileName,
    		String outputElasticTransfFileName,
    		String sizeFactor) 
    {
    	String[] args = {"bUnwarpJ_", targetImageName, sourceImageName, inputElasticTransfFileName, outputElasticTransfFileName, sizeFactor};
    	adaptCoefficientsCommandLine(args);
    }
    /* end adaptCoefficientsMacro */     
    
} /* end class bUnwarpJ_ */
