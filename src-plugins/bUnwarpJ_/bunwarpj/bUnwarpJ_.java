package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ and Fiji.
 * Copyright (C) 2005-2009 Ignacio Arganda-Carreras and Jan Kybic 
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
 *  Version: November 2nd, 2009
 *  http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 * \===================================================================
 */

/**
 * Old version (UnwarpJ) information: 
 * http://bigwww.epfl.ch/thevenaz/UnwarpJ/
 */

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.util.Stack;

/*====================================================================
|   bUnwarpJ_
\===================================================================*/

/**
 * Main class for the image registration plugin for ImageJ/Fiji.
 * <p>
 * This class is a plugin for the ImageJ/Fiji interface. It allows pairwise image
 * registration combining the ideas of elastic registration based on B-spline 
 * models and consistent registration.
 *
 * <p>
 * This work is an extension by Ignacio Arganda-Carreras and Jan Kybic 
 * of the previous UnwarpJ project by Carlos Oscar Sanchez Sorzano.
 * <p>
 * For more information visit the main site 
 * <A target="_blank" href="http://biocomp.cnb.csic.es/~iarganda/bUnwarpJ/">
 * http://biocomp.cnb.csic.es/~iarganda/bUnwarpJ/</a>
 *
 * @author Ignacio Arganda-Carreras (ignacio.arganda at gmail.com)
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
    /** algorithm mode (fast, accurate or mono) */
    private static int mode = MainDialog.ACCURATE_MODE;
    /** image subsampling factor at the highest pyramid level*/
    private static int maxImageSubsamplingFactor = 0;
    
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
    /** debug flag */
	private static boolean debug = false;

	
    /*....................................................................
       Public methods
    ....................................................................*/

    //------------------------------------------------------------------
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
	
    	final MainDialog dialog = new MainDialog(IJ.getInstance(), imageList, bUnwarpJ_.mode,
    			bUnwarpJ_.maxImageSubsamplingFactor, bUnwarpJ_.min_scale_deformation, 
    			bUnwarpJ_.max_scale_deformation, bUnwarpJ_.divWeight, bUnwarpJ_.curlWeight, 
    			bUnwarpJ_.landmarkWeight, bUnwarpJ_.imageWeight, bUnwarpJ_.consistencyWeight, 
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
        
        // Collect input values
		// Source and target image plus
		this.sourceImp = imageList[dialog.getNextChoiceIndex()];
		this.targetImp = imageList[dialog.getNextChoiceIndex()];
		  
		// Fast or accurate mode
		bUnwarpJ_.mode = dialog.getNextChoiceIndex();
		// Image subsampling factor at highest resolution level		
		bUnwarpJ_.maxImageSubsamplingFactor = (int) dialog.getNextNumber();
		  
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
        
        FinalAction finalAction =
           new FinalAction(dialog);

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
        
        // Throw final action thread
        Thread fa = finalAction.getThread();
        fa.start();
        try {
        	// We join the thread to the main plugin thread
			fa.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		

    } /* end run */

    
    //------------------------------------------------------------------
    /**
     * Method for images alignment with no graphical interface. This 
     * method gives as result a Transformation object that 
     * contains all the registration information.
     *
     * @param targetImp input target image 
     * @param sourceImp input source image
     * @param targetMskIP target mask 
     * @param sourceMskIP source mask
     * @param mode accuracy mode (0 - Fast, 1 - Accurate, 2 - Mono)
     * @param img_subsamp_fact image subsampling factor (from 0 to 7, representing 2^0=1 to 2^7 = 128)
     * @param min_scale_deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine)
     * @param max_scale_deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine, 4 - Super Fine)
     * @param divWeight divergence weight
     * @param curlWeight curl weight
     * @param landmarkWeight landmark weight
     * @param imageWeight image similarity weight
     * @param consistencyWeight consistency weight
     * @param stopThreshold stopping threshold
     * 
     * @return results transformation object
     */
    public static Transformation computeTransformationBatch(ImagePlus targetImp,
    									 ImagePlus sourceImp,
    									 ImageProcessor targetMskIP,
    									 ImageProcessor sourceMskIP,
    									 int mode,
    									 int img_subsamp_fact,
    									 int min_scale_deformation,
    									 int max_scale_deformation,
    									 double divWeight,
    									 double curlWeight,
    									 double landmarkWeight,
    									 double imageWeight,
    									 double consistencyWeight,
    									 double stopThreshold) 
    {    	       
       // Produce side information
       final int imagePyramidDepth = max_scale_deformation - min_scale_deformation + 1;
       final int min_scale_image = 0;
       
       // output level to -1 so nothing is displayed 
       final int outputLevel = -1;
       
       final boolean showMarquardtOptim = false;       

       // Create target image model
       final BSplineModel target = new BSplineModel(targetImp.getProcessor(), true, 
    		   													 (int) Math.pow(2, img_subsamp_fact));
       
       target.setPyramidDepth(imagePyramidDepth+min_scale_image);
       target.startPyramids();
       
       // Create target mask
       final Mask targetMsk = (targetMskIP != null) ? new Mask(targetMskIP, true) 
       		  										         : new Mask(targetImp.getProcessor(), false);
                    
       PointHandler targetPh = null;

       // Create source image model
       boolean bIsReverse = true;         

       final BSplineModel source = new BSplineModel(sourceImp.getProcessor(), bIsReverse, 
    		   													(int) Math.pow(2, img_subsamp_fact));

       source.setPyramidDepth(imagePyramidDepth + min_scale_image);
       source.startPyramids();
       
       // Create source mask
       final Mask sourceMsk = (sourceMskIP != null) ? new Mask(sourceMskIP, true) 
       														 : new Mask(sourceImp.getProcessor(), false);
       
       PointHandler sourcePh = null;

       // Load points rois as landmarks if any.
       Stack<Point> sourceStack = new Stack<Point>();
       Stack<Point> targetStack = new Stack<Point>();
       MiscTools.loadPointRoiAsLandmarks(sourceImp, targetImp, sourceStack, targetStack);

       sourcePh  = new PointHandler(sourceImp);
       targetPh  = new PointHandler(targetImp);

       while ((!sourceStack.empty()) && (!targetStack.empty())) 
       {
    	   Point sourcePoint = (Point)sourceStack.pop();
    	   Point targetPoint = (Point)targetStack.pop();
    	   sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
    	   targetPh.addPoint(targetPoint.x, targetPoint.y);
       }
       
       
       // Set no initial affine matrices
       final double[][] sourceAffineMatrix = null;
       final double[][] targetAffineMatrix = null;
 
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
       ImagePlus[] output_ip = new ImagePlus[2];
       output_ip[0] = null; 
       output_ip[1] = null; 
       
       // The dialog is set to null to work in batch mode
       final MainDialog dialog = null;
       
       final ImageProcessor originalSourceIP = sourceImp.getProcessor();
       final ImageProcessor originalTargetIP = targetImp.getProcessor();

       // Setup registration parameters
       final Transformation warp = new Transformation(
         sourceImp, targetImp, source, target, sourcePh, targetPh,
         sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
         min_scale_deformation, max_scale_deformation, min_scale_image, divWeight, 
         curlWeight, landmarkWeight, imageWeight, consistencyWeight, stopThreshold, 
         outputLevel, showMarquardtOptim, mode, null, null, output_ip[0], output_ip[1], dialog,
         originalSourceIP, originalTargetIP);

       IJ.log("\nRegistering...\n");
       
       long start = System.currentTimeMillis(); // start timing

       // Perform registration
       if(mode == MainDialog.MONO_MODE)       
    	   warp.doUnidirectionalRegistration();    	       
       else
    	   warp.doBidirectionalRegistration();

       long stop = System.currentTimeMillis(); // stop timing
       IJ.log("bUnwarpJ is done! Registration time: " + (stop - start) + "ms"); // print execution time

       return warp;
       
    } // end computeTransformationBatch    
    
    
    //------------------------------------------------------------------
    /**
     * Method for images alignment with no graphical interface. This 
     * method gives as result a Transformation object that 
     * contains all the registration information.
     *
     * @param targetImp input target image 
     * @param sourceImp input source image
     * @param targetMskIP target mask 
     * @param sourceMskIP source mask
     * @param parameter registration parameters
     * 
     * @return results transformation object
     */
    public static Transformation computeTransformationBatch(ImagePlus targetImp,
    									 ImagePlus sourceImp,
    									 ImageProcessor targetMskIP,
    									 ImageProcessor sourceMskIP,
    									 Param parameter) 
    {    	
       if(targetImp == null || sourceImp == null || parameter == null)
       {
    	   IJ.error("Missing parameters to compute transformation!");
    	   return null;
       }
       
       if(debug)
    	   IJ.log("\n--- bUnwarpJ parameters ---\n" +
    			   "\nSource image: " + sourceImp.getTitle() + 
    			   "\nTarget image: " + targetImp.getTitle() + "\n" +
    			   parameter.toString() + "\n");
       
       // Produce side information
       final int imagePyramidDepth = parameter.max_scale_deformation - parameter.min_scale_deformation + 1;
       final int min_scale_image = 0;
       
       // output level to -1 so nothing is displayed 
       final int outputLevel = -1;
       
       final boolean showMarquardtOptim = false;       

       // Create target image model
       final BSplineModel target = new BSplineModel(targetImp.getProcessor(), true, 
    		   													 (int) Math.pow(2, parameter.img_subsamp_fact));
       
       target.setPyramidDepth(imagePyramidDepth+min_scale_image);
       target.startPyramids();
       
       // Create target mask
       final Mask targetMsk = (targetMskIP != null) ? new Mask(targetMskIP, true) 
       		  										         : new Mask(targetImp.getProcessor(), false);
                    
       PointHandler targetPh = null;

       // Create source image model
       boolean bIsReverse = true;         

       final BSplineModel source = new BSplineModel(sourceImp.getProcessor(), bIsReverse, 
    		   													(int) Math.pow(2, parameter.img_subsamp_fact));

       source.setPyramidDepth(imagePyramidDepth + min_scale_image);
       source.startPyramids();
       
       // Create source mask
       final Mask sourceMsk = (sourceMskIP != null) ? new Mask(sourceMskIP, true) 
       														 : new Mask(sourceImp.getProcessor(), false);
       
       PointHandler sourcePh = null;

       // Load landmarks
       //if (parameter.landmarkWeight != 0)
       //{
          Stack<Point> sourceStack = new Stack<Point>();
          Stack<Point> targetStack = new Stack<Point>();
          MiscTools.loadPointRoiAsLandmarks(sourceImp, targetImp, sourceStack, targetStack);

          sourcePh  = new PointHandler(sourceImp);
          targetPh  = new PointHandler(targetImp);

          while ((!sourceStack.empty()) && (!targetStack.empty())) 
          {
             Point sourcePoint = (Point)sourceStack.pop();
             Point targetPoint = (Point)targetStack.pop();
             sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
             targetPh.addPoint(targetPoint.x, targetPoint.y);
          }
       //}
       
       // Set no initial affine matrices
       final double[][] sourceAffineMatrix = null;
       final double[][] targetAffineMatrix = null;
 
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
       ImagePlus[] output_ip = new ImagePlus[2];
       output_ip[0] = null; 
       output_ip[1] = null; 
       
       // The dialog is set to null to work in batch mode
       final MainDialog dialog = null;
       
       final ImageProcessor originalSourceIP = sourceImp.getProcessor();
       final ImageProcessor originalTargetIP = targetImp.getProcessor();


       final Transformation warp = new Transformation(
         sourceImp, targetImp, source, target, sourcePh, targetPh,
         sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
         parameter.min_scale_deformation, parameter.max_scale_deformation, 
         min_scale_image, parameter.divWeight, 
         parameter.curlWeight, parameter.landmarkWeight, parameter.imageWeight, 
         parameter.consistencyWeight, parameter.stopThreshold, 
         outputLevel, showMarquardtOptim, parameter.mode,null, null, output_ip[0], output_ip[1], dialog,
         originalSourceIP, originalTargetIP);
       
       // Initial affine transform correction values
       warp.setAnisotropyCorrection( parameter.getAnisotropyCorrection() );
       warp.setScaleCorrection( parameter.getScaleCorrection() );
       warp.setShearCorrection( parameter.getShearCorrection() );

       IJ.log("\nRegistering...\n");
       
       long start = System.currentTimeMillis(); // start timing

       if(parameter.mode == MainDialog.MONO_MODE)       
    	   warp.doUnidirectionalRegistration();    	       
       else
    	   warp.doBidirectionalRegistration();

       long stop = System.currentTimeMillis(); // stop timing
       IJ.log("bUnwarpJ is done! Registration time: " + (stop - start) + "ms"); // print execution time

       return warp;
       
    } // end computeTransformationBatch    
    

    //------------------------------------------------------------------
    /**
     * Method for images alignment with no graphical interface. This 
     * method gives as result a Transformation object that 
     * contains all the registration information.
     *
     * @param targetImp input target image 
     * @param sourceImp input source image
     * @param targetMskIP target mask 
     * @param sourceMskIP source mask
     * @param targetAffineTransf initial target affine transform
     * @param sourceAffineTransf initial source affine transform
     * @param parameter registration parameters
     * 
     * @return results transformation object
     */
    public static Transformation computeTransformationBatch(ImagePlus targetImp,
    									 ImagePlus sourceImp,
    									 ImageProcessor targetMskIP,
    									 ImageProcessor sourceMskIP,
    									 AffineTransform targetAffineTransf,
    									 AffineTransform sourceAffineTransf,
    									 Param parameter) 
    {    	
       if(targetImp == null || sourceImp == null || parameter == null)
       {
    	   IJ.error("Missing parameters to compute transformation!");
    	   return null;
       }

       // Produce side information
       final int imagePyramidDepth = parameter.max_scale_deformation - parameter.min_scale_deformation + 1;
       final int min_scale_image = 0;
       
       // output level to -1 so nothing is displayed 
       final int outputLevel = -1;
       
       final boolean showMarquardtOptim = false;       

       // Create target image model
       final BSplineModel target = new BSplineModel(targetImp.getProcessor(), true, 
    		   													 (int) Math.pow(2, parameter.img_subsamp_fact));
       
       target.setPyramidDepth(imagePyramidDepth+min_scale_image);
       target.startPyramids();
       
       // Create target mask
       final Mask targetMsk = (targetMskIP != null) ? new Mask(targetMskIP, true) 
       		  										         : new Mask(targetImp.getProcessor(), false);
                    
       PointHandler targetPh = null;

       // Create source image model
       boolean bIsReverse = true;         

       final BSplineModel source = new BSplineModel(sourceImp.getProcessor(), bIsReverse, 
    		   													(int) Math.pow(2, parameter.img_subsamp_fact));

       source.setPyramidDepth(imagePyramidDepth + min_scale_image);
       source.startPyramids();
       
       // Create source mask
       final Mask sourceMsk = (sourceMskIP != null) ? new Mask(sourceMskIP, true) 
       														 : new Mask(sourceImp.getProcessor(), false);
       
       PointHandler sourcePh = null;

       Stack<Point> sourceStack = new Stack<Point>();
       Stack<Point> targetStack = new Stack<Point>();
       MiscTools.loadPointRoiAsLandmarks(sourceImp, targetImp, sourceStack, targetStack);

       sourcePh  = new PointHandler(sourceImp);
       targetPh  = new PointHandler(targetImp);

       while ((!sourceStack.empty()) && (!targetStack.empty())) 
       {
    	   Point sourcePoint = (Point)sourceStack.pop();
    	   Point targetPoint = (Point)targetStack.pop();
    	   sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
    	   targetPh.addPoint(targetPoint.x, targetPoint.y);
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
       ImagePlus[] output_ip = new ImagePlus[2];
       output_ip[0] = null; 
       output_ip[1] = null; 
       
       // The dialog is set to null to work in batch mode
       final MainDialog dialog = null;
       
       final ImageProcessor originalSourceIP = sourceImp.getProcessor();
       final ImageProcessor originalTargetIP = targetImp.getProcessor();

       
       final double[][] targetAffineMatrix;
       final double[][] sourceAffineMatrix;
       
       if(sourceAffineTransf != null && targetAffineTransf != null)
       {
    	   final double[] flatMat = new double [6];
    	   sourceAffineTransf.getMatrix(flatMat);
    	   sourceAffineMatrix = new double[][]{ {flatMat[0], flatMat[2], flatMat[4] },  {flatMat[1], flatMat[3], flatMat[5]} };
    	   //IJ.log("Source Matrix = " + flatMat[0] + " " +  flatMat[2] + " " +  flatMat[4] + " " +  flatMat[1] + " "  + flatMat[3] + " " +  flatMat[5]);
    	   targetAffineTransf.getMatrix(flatMat);
    	   //IJ.log("Target Matrix = " + flatMat[0] + " " +  flatMat[2] + " " +  flatMat[4] + " " +  flatMat[1] + " "  + flatMat[3] + " " +  flatMat[5]);
    	   targetAffineMatrix = new double[][]{ {flatMat[0], flatMat[2], flatMat[4] },  {flatMat[1], flatMat[3], flatMat[5]} };
       }
       else
       {
    	   sourceAffineMatrix = null;
    	   targetAffineMatrix = null;
       }
       
       final Transformation warp = new Transformation(
         sourceImp, targetImp, source, target, sourcePh, targetPh,
         sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
         parameter.min_scale_deformation, parameter.max_scale_deformation, 
         min_scale_image, parameter.divWeight, 
         parameter.curlWeight, parameter.landmarkWeight, parameter.imageWeight, 
         parameter.consistencyWeight, parameter.stopThreshold, 
         outputLevel, showMarquardtOptim, parameter.mode,null, null, output_ip[0], output_ip[1], dialog,
         originalSourceIP, originalTargetIP);

       IJ.log("\nRegistering...\n");
       
       long start = System.currentTimeMillis(); // start timing

       if(parameter.mode == MainDialog.MONO_MODE)       
    	   warp.doUnidirectionalRegistration();    	       
       else
    	   warp.doBidirectionalRegistration();

       long stop = System.currentTimeMillis(); // stop timing
       IJ.log("bUnwarpJ is done! Registration time: " + (stop - start) + "ms"); // print execution time

       return warp;
       
    } // end computeTransformationBatch   
    
    
    //------------------------------------------------------------------
    /**
     * Method for images alignment with no graphical interface. This 
     * method gives as result a Transformation object that 
     * contains all the registration information.
     *
     * @param sourceWidth
     * @param sourceHeight
     * @param targetWidth 
     * @param targetHeight
     * @param sourcePoints
     * @param targetPoints
     * @param parameter registration parameters
     * 
     * @return results transformation object
     */
    public static Transformation computeTransformationBatch(
    									int sourceWidth,
    									int sourceHeight,
    									int targetWidth,
    									int targetHeight,
    									Stack<Point> sourcePoints,
    									Stack<Point> targetPoints,
    									Param parameter) 
    {    	
       if(sourcePoints == null || targetPoints == null || parameter == null)
       {
    	   IJ.error("Missing parameters to compute transformation!");
    	   return null;
       }


       //IJ.log("Registration parameters:\n" + parameter.toString());
       
       // Produce side information
       final int imagePyramidDepth = parameter.max_scale_deformation - parameter.min_scale_deformation + 1;
       final int min_scale_image = 0;
       
       // output level to -1 so nothing is displayed 
       final int outputLevel = -1;
       
       final boolean showMarquardtOptim = false;       

       // Create target image model
       final BSplineModel target = new BSplineModel(targetWidth, targetHeight, (int) Math.pow(2, parameter.img_subsamp_fact));
       
       target.setPyramidDepth(imagePyramidDepth+min_scale_image);
       target.startPyramids();
       
       // Create target mask
       final Mask targetMsk = new Mask(targetWidth, targetHeight);                           

       // Create source image model
       final BSplineModel source = new BSplineModel(sourceWidth, sourceHeight, (int) Math.pow(2, parameter.img_subsamp_fact));

       source.setPyramidDepth(imagePyramidDepth + min_scale_image);
       source.startPyramids();
       
       // Create source mask
       final Mask sourceMsk = new Mask (sourceWidth, sourceHeight);
          

       // Set landmarks
       PointHandler sourcePh  = new PointHandler(sourceWidth, sourceHeight);
       PointHandler targetPh  = new PointHandler(targetWidth, targetHeight);

       while ((!sourcePoints.empty()) && (!targetPoints.empty())) 
       {
    	   Point sourcePoint = (Point)sourcePoints.pop();
    	   Point targetPoint = (Point)targetPoints.pop();
    	   sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
    	   targetPh.addPoint(targetPoint.x, targetPoint.y);
       }

       
       // Set no initial affine matrices
       final double[][] sourceAffineMatrix = null;
       final double[][] targetAffineMatrix = null;
 
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
       ImagePlus[] output_ip = new ImagePlus[2];
       output_ip[0] = null; 
       output_ip[1] = null; 
       
       // The dialog is set to null to work in batch mode
       final MainDialog dialog = null;
       
       final ImageProcessor originalSourceIP = null;
       final ImageProcessor originalTargetIP = null;

       // Set transformation parameters
       final Transformation warp = new Transformation(
         null, null, source, target, sourcePh, targetPh,
         sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
         parameter.min_scale_deformation, parameter.max_scale_deformation, 
         min_scale_image, parameter.divWeight, 
         parameter.curlWeight, parameter.landmarkWeight, parameter.imageWeight, 
         parameter.consistencyWeight, parameter.stopThreshold, 
         outputLevel, showMarquardtOptim, parameter.mode, null, null, output_ip[0], output_ip[1], dialog,
         originalSourceIP, originalTargetIP);

       IJ.log("\nRegistering...\n");
       
       long start = System.currentTimeMillis(); // start timing
       
       // Register
       if(parameter.mode == MainDialog.MONO_MODE)       
    	   warp.doUnidirectionalRegistration();    	       
       else
    	   warp.doBidirectionalRegistration();

       long stop = System.currentTimeMillis(); // stop timing
       IJ.log("bUnwarpJ is done! Registration time: " + (stop - start) + "ms"); // print execution time

       return warp;
       
    } // end computeTransformationBatch  
    
    
    
    
    //------------------------------------------------------------------
    /**
     * Method for images alignment with no graphical interface. This 
     * method gives as result an array of 2 ImagePlus containing the
     * source-target and target-source results respectively. Each of them
     * has three slices: the final deformed image, its target image and 
     * its mask.
     *
     * @param targetImp input target image 
     * @param sourceImp input source image
     * @param targetMskIP target mask 
     * @param sourceMskIP source mask
     * @param mode accuracy mode (0 - Fast, 1 - Accurate, 2 - Mono)
     * @param img_subsamp_fact image subsampling factor (from 0 to 7, representing 2^0=1 to 2^7 = 128)
     * @param min_scale_deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine)
     * @param max_scale_deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine, 4 - Super Fine)
     * @param divWeight divergence weight
     * @param curlWeight curl weight
     * @param landmarkWeight landmark weight
     * @param imageWeight image similarity weight
     * @param consistencyWeight consistency weight
     * @param stopThreshold stopping threshold
     * 
     * @return resulting ImagePlus array (with source-target and target-source results)
     */
    public static ImagePlus[] alignImagesBatch(ImagePlus targetImp,
    									 ImagePlus sourceImp,
    									 ImageProcessor targetMskIP,
    									 ImageProcessor sourceMskIP,
    									 int mode,
    									 int img_subsamp_fact,
    									 int min_scale_deformation,
    									 int max_scale_deformation,
    									 double  divWeight,
    									 double  curlWeight,
    									 double  landmarkWeight,
    									 double  imageWeight,
    									 double  consistencyWeight,
    									 double  stopThreshold) 
    {    	
       

       Transformation warp 
       				= computeTransformationBatch(targetImp, sourceImp,	targetMskIP, sourceMskIP,
       											 mode, img_subsamp_fact, min_scale_deformation,
       											 max_scale_deformation, divWeight, curlWeight,
       											 landmarkWeight, imageWeight, consistencyWeight,
       											 stopThreshold);

       // Return results as ImagePlus
       final ImagePlus[] output_ip = new ImagePlus[2];
       output_ip[0] = warp.getDirectResults();       
       output_ip[1] = warp.getInverseResults();
       
       return output_ip;       
       
    } // end alignImagesBatch
    
    //------------------------------------------------------------------
    /**
     * Method for images alignment with no graphical interface. This 
     * method gives as result an array of 2 ImagePlus containing the
     * source-target and target-source results respectively. Each of them
     * has three slices: the final deformed image, its target image and 
     * its mask.
     *
     * @param targetImp input target image 
     * @param sourceImp input source image
     * @param targetMskIP target mask 
     * @param sourceMskIP source mask
     * @param parameter registration parameters
     * 
     * @return resulting ImagePlus array (with source-target and target-source results)
     */
    public static ImagePlus[] alignImagesBatch(ImagePlus targetImp,
    									 ImagePlus sourceImp,
    									 ImageProcessor targetMskIP,
    									 ImageProcessor sourceMskIP,
    									 Param parameter) 
    {    	
       

    	Transformation warp 
    		= computeTransformationBatch(targetImp, sourceImp,	targetMskIP, 
    													sourceMskIP, parameter);

       // Return results as ImagePlus
       final ImagePlus[] output_ip = new ImagePlus[2];
       output_ip[0] = warp.getDirectResults();       
       output_ip[1] = warp.getInverseResults();
       
       return output_ip;       
       
    } // end alignImagesBatch
    
    //------------------------------------------------------------------
    /**
     * Method for images alignment with no graphical interface. This 
     * method gives as result an array of 2 ImagePlus containing the
     * source-target and target-source results (only final transformed image).
     *
     * @param targetImp input target image 
     * @param sourceImp input source image
     * @param targetMskIP target mask 
     * @param sourceMskIP source mask
     * @param mode accuracy mode (0 - Fast, 1 - Accurate, 2 - Mono)
     * @param img_subsamp_fact image subsampling factor (from 0 to 7, representing 2^0=1 to 2^7 = 128)
     * @param min_scale_deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine)
     * @param max_scale_deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine, 4 - Super Fine)
     * @param divWeight divergence weight
     * @param curlWeight curl weight
     * @param landmarkWeight landmark weight
     * @param imageWeight image similarity weight
     * @param consistencyWeight consistency weight
     * @param stopThreshold stopping threshold
     * @param xScale scale factor for x axis
     * @param yScale scale factor for y axis 
     * 
     * @return resulting ImagePlus array (with source-target and target-source results)
     */
    public static ImagePlus[] alignImagesBatch(ImagePlus targetImp,
    									 ImagePlus sourceImp,
    									 ImageProcessor targetMskIP,
    									 ImageProcessor sourceMskIP,
    									 int mode,
    									 int img_subsamp_fact,
    									 int min_scale_deformation,
    									 int max_scale_deformation,
    									 double  divWeight,
    									 double  curlWeight,
    									 double  landmarkWeight,
    									 double  imageWeight,
    									 double  consistencyWeight,
    									 double  stopThreshold,
    									 double xScale,
    									 double yScale) 
    {    	

    	// Scale input images
    	final ImageProcessor fullTargetIP = targetImp.getProcessor();
    	fullTargetIP.setInterpolate(true);
    	final ImageProcessor scaledTargetIP = fullTargetIP.resize((int) (xScale * targetImp.getWidth()),
    														 (int) (yScale * targetImp.getHeight()));
    	
    	final ImageProcessor fullSourceIP = sourceImp.getProcessor();
    	fullSourceIP.setInterpolate(true);
    	final ImageProcessor scaledSourceIP = fullSourceIP.resize((int) (xScale * sourceImp.getWidth()),
    														 (int) (yScale * sourceImp.getHeight()));

    	// Produce side information
    	final int imagePyramidDepth = max_scale_deformation - min_scale_deformation + 1;
    	final int min_scale_image = 0;

    	// output level to -1 so nothing is displayed 
    	final int outputLevel = -1;

    	final boolean showMarquardtOptim = false;       

    	// Create target image model
    	final BSplineModel target = new BSplineModel(scaledTargetIP, true, 
    			(int) Math.pow(2, img_subsamp_fact));

    	target.setPyramidDepth(imagePyramidDepth+min_scale_image);
    	target.startPyramids();

    	// Create target mask
    	final Mask targetMsk = (targetMskIP != null) ? new Mask(targetMskIP, true) 
    														 : new Mask(targetImp.getProcessor(), false);

    	PointHandler targetPh = null;

    	// Create source image model
    	boolean bIsReverse = true;         

    	final BSplineModel source = new BSplineModel(scaledSourceIP, bIsReverse, 
    			(int) Math.pow(2, img_subsamp_fact));

    	source.setPyramidDepth(imagePyramidDepth + min_scale_image);
    	source.startPyramids();

    	// Create source mask
    	final Mask sourceMsk = (sourceMskIP != null) ? new Mask(sourceMskIP, true) 
    														 : new Mask(sourceImp.getProcessor(), false);

    	PointHandler sourcePh = null;

    	// Load landmarks
    	if (landmarkWeight != 0)
    	{
    		Stack<Point> sourceStack = new Stack<Point>();
    		Stack<Point> targetStack = new Stack<Point>();
    		MiscTools.loadPointRoiAsLandmarks(sourceImp, targetImp, sourceStack, targetStack);

    		sourcePh  = new PointHandler(sourceImp);
    		targetPh  = new PointHandler(targetImp);

    		while ((!sourceStack.empty()) && (!targetStack.empty())) 
    		{
    			Point sourcePoint = (Point)sourceStack.pop();
    			Point targetPoint = (Point)targetStack.pop();
    			sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
    			targetPh.addPoint(targetPoint.x, targetPoint.y);
    		}
    	}

    	// Set no initial affine matrices
    	final double[][] sourceAffineMatrix = null;
    	final double[][] targetAffineMatrix = null;

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
    	ImagePlus[] output_ip = new ImagePlus[2];
    	output_ip[0] = new ImagePlus();
    	output_ip[1] = new ImagePlus();

    	// The dialog is set to null to work in batch mode
    	final MainDialog dialog = null;

    	final ImageProcessor originalSourceIP = sourceImp.getProcessor();
    	final ImageProcessor originalTargetIP = targetImp.getProcessor();


    	final Transformation warp = new Transformation(
    			sourceImp, targetImp, source, target, sourcePh, targetPh,
    			sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
    			min_scale_deformation, max_scale_deformation, min_scale_image, divWeight, 
    			curlWeight, landmarkWeight, imageWeight, consistencyWeight, stopThreshold, 
    			outputLevel, showMarquardtOptim, mode, null, null, output_ip[0], output_ip[1], dialog,
    			originalSourceIP, originalTargetIP);

    	IJ.log("\nRegistering...\n");

    	long start = System.currentTimeMillis(); // start timing

    	if(mode == MainDialog.MONO_MODE)       
    		warp.doUnidirectionalRegistration();    	       
    	else
    		warp.doBidirectionalRegistration();

    	long stop = System.currentTimeMillis(); // stop timing
    	IJ.log("bUnwarpJ is done! Registration time: " + (stop - start) + "ms"); // print execution time

    	// Adapt transformation to scale
    	int intervals = warp.getIntervals();
    	double[][] cx_direct = warp.getDirectDeformationCoefficientsX();
    	double[][] cy_direct = warp.getDirectDeformationCoefficientsY();
    	
    	MiscTools.adaptCoefficients(1.0/xScale, 1.0/yScale, intervals, cx_direct, cy_direct);
    	
    	BSplineModel fullSource = null;

    	fullSource = new BSplineModel(fullSourceIP, false, 1);
    	fullSource.setPyramidDepth(0);
    	fullSource.startPyramids();
    	
    	MiscTools.applyTransformationToSourceMT(sourceImp, targetImp, fullSource, intervals, cx_direct, cy_direct);
    	
    	output_ip[0] = sourceImp;
    	
    	if(mode != MainDialog.MONO_MODE)
    	{
    		double[][] cx_inverse = warp.getInverseDeformationCoefficientsX();
    		double[][] cy_inverse = warp.getInverseDeformationCoefficientsY();
    		
    		MiscTools.adaptCoefficients(1.0/xScale, 1.0/yScale, intervals, cx_inverse, cy_inverse);
        	
        	BSplineModel fullTarget = null;

        	fullTarget = new BSplineModel(fullTargetIP, false, 1);
        	fullTarget.setPyramidDepth(0);
        	fullTarget.startPyramids();
        	
        	MiscTools.applyTransformationToSourceMT(targetImp, sourceImp, fullTarget, intervals, cx_inverse, cy_inverse);
        	
        	output_ip[1] = targetImp;
    	
    	}
    	// Return results as ImagePlus
    	return output_ip;       

    } // end alignImagesBatch
    
    //------------------------------------------------------------------
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

    //------------------------------------------------------------------
    /**
     * Method for images alignment with no graphical interface.
     *
     * @param args arguments for the program
     */
    private static void alignImagesCommandLine(String args[]) 
    {    	
       if (args.length < 14)
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
       int max_subsamp_fact = ((Integer) new Integer(args[7])).intValue();
       double  divWeight = ((Double) new Double(args[8])).doubleValue();
       double  curlWeight = ((Double) new Double(args[9])).doubleValue();
       double  imageWeight = ((Double) new Double(args[10])).doubleValue();
       
       int     accurate_mode = MainDialog.ACCURATE_MODE;

       double  consistencyWeight = ((Double) new Double(args[11])).doubleValue();

       String fn_out_1 = args[12];
       String fn_out_2 = args[13];
       double  landmarkWeight = 0;
       String fn_landmark = "";
       String fn_affine_1 = "";
       String fn_affine_2 = "";
       
       if (args.length == 17) 
       {
          if(args[14].equals("-landmark"))
          {
              landmarkWeight = ((Double) new Double(args[15])).doubleValue();
              fn_landmark = args[16];
          }
          else if(args[14].equals("-affine"))
          {
              fn_affine_1 = args[15];
              fn_affine_2 = args[16];
          }
       }
       else if(args.length == 15)
       {
    	   if(args[14].equalsIgnoreCase("-mono"))
    	   {
    		   accurate_mode = MainDialog.MONO_MODE;
    		   fn_out_2 = "NULL (Mono mode)";
    	   }
    	   else
    	   {
    		   dumpSyntax();
               System.exit(0);
    	   }
    		   
       }

       // Show parameters
       IJ.log("Target image           : " + fn_target);
       IJ.log("Target mask            : " + fn_target_mask);
       IJ.log("Source image           : " + fn_source);
       IJ.log("Source mask            : " + fn_source_mask);
       IJ.log("Min. Scale Deformation : " + min_scale_deformation);
       IJ.log("Max. Scale Deformation : " + max_scale_deformation);
       IJ.log("Max. Subsampling factor: " + max_subsamp_fact);
       IJ.log("Div. Weight            : " + divWeight);
       IJ.log("Curl Weight            : " + curlWeight);
       IJ.log("Image Weight           : " + imageWeight);
       IJ.log("Consistency Weight     : " + consistencyWeight);
       IJ.log("Output 1               : " + fn_out_1);
       IJ.log("Output 2               : " + fn_out_2);
       IJ.log("Landmark Weight        : " + landmarkWeight);
       IJ.log("Landmark file          : " + fn_landmark);
       IJ.log("Affine matrix file 1   : " + fn_affine_1);
       IJ.log("Affine matrix file 2   : " + fn_affine_2);
       String sMode = (accurate_mode == MainDialog.MONO_MODE) ? "Mono" : "Accurate";
       IJ.log("Registration mode	    : " + sMode);

       // Produce side information
       int     imagePyramidDepth=max_scale_deformation-min_scale_deformation+1;
       int     min_scale_image = 0;
       double  stopThreshold = 1e-2;  // originally -2
       int     outputLevel = -1;
       boolean showMarquardtOptim = false;       

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
       
       BSplineModel target = new BSplineModel(targetImp.getProcessor(), true, 
    		   												(int) Math.pow(2, max_subsamp_fact));
       
       target.setPyramidDepth(imagePyramidDepth + min_scale_image);
       target.startPyramids();
  
  
       
       Mask targetMsk = new Mask(targetImp.getProcessor(),false);
       
       if (fn_target_mask.equalsIgnoreCase(new String("NULL")) == false)
           targetMsk.readFile(fn_target_mask);
       
       PointHandler targetPh = null;

       // Open source
       boolean bIsReverse = true;

       ImagePlus sourceImp = opener.openImage(fn_source);   

       BSplineModel source = new BSplineModel(sourceImp.getProcessor(), bIsReverse, 
    		   												(int) Math.pow(2, max_subsamp_fact));

       source.setPyramidDepth(imagePyramidDepth + min_scale_image);
       source.startPyramids();

       Mask sourceMsk = new Mask(sourceImp.getProcessor(), false);

       if (fn_source_mask.equalsIgnoreCase(new String("NULL")) == false)
           sourceMsk.readFile(fn_source_mask);
       
       PointHandler sourcePh=null;

       // Load landmarks
       if (fn_landmark.equals("") == false)
       {
          Stack<Point> sourceStack = new Stack<Point>();
          Stack<Point> targetStack = new Stack<Point>();
          MiscTools.loadPoints(fn_landmark, sourceStack, targetStack);

          sourcePh  = new PointHandler(sourceImp);
          targetPh  = new PointHandler(targetImp);

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
           MiscTools.loadAffineMatrix(fn_affine_1, sourceAffineMatrix);
       }
       double[][] targetAffineMatrix = null;
       if(fn_affine_2.equals("") == false && fn_affine_2.equalsIgnoreCase(new String("NULL")) == false)
       {
           targetAffineMatrix = new double[2][3];
           MiscTools.loadAffineMatrix(fn_affine_2, targetAffineMatrix);
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
       ImagePlus output_ip_1 = null; //new ImagePlus();
       ImagePlus output_ip_2 = null; //new ImagePlus();
       MainDialog dialog = null;
       
       ImageProcessor originalSourceIP = sourceImp.getProcessor();
       ImageProcessor originalTargetIP = targetImp.getProcessor();


       final Transformation warp = new Transformation(
         sourceImp, targetImp, source, target, sourcePh, targetPh,
         sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
         min_scale_deformation, max_scale_deformation, min_scale_image, divWeight, 
         curlWeight, landmarkWeight, imageWeight, consistencyWeight, stopThreshold, 
         outputLevel, showMarquardtOptim, accurate_mode, fn_tnf_1, fn_tnf_2, output_ip_1, output_ip_2, dialog,
         originalSourceIP, originalTargetIP);

       IJ.log("\nRegistering...\n");
       
       long start = System.currentTimeMillis(); // start timing
       
       if(accurate_mode == MainDialog.MONO_MODE)
    	   warp.doUnidirectionalRegistration();
       else
    	   warp.doBidirectionalRegistration();
       
       long stop = System.currentTimeMillis(); // stop timing
       IJ.log("Registration time: " + (stop - start) + "ms"); // print execution time


       // Save results (only the registered image, the target 
       // and mask images are trashed)
       output_ip_1 = warp.getDirectResults();
       output_ip_1.getStack().deleteLastSlice();
       output_ip_1.getStack().deleteLastSlice();
       
             
       
       FileSaver fs = new FileSaver(output_ip_1);
       fs.saveAsTiff(fn_out_1);
       
       if((accurate_mode != MainDialog.MONO_MODE))
       {
    	   output_ip_2 = warp.getInverseResults();
    	   output_ip_2.getStack().deleteLastSlice();
           output_ip_2.getStack().deleteLastSlice();                      
           
    	   fs = new FileSaver(output_ip_2);
    	   fs.saveAsTiff(fn_out_2);
       }
       
    } // end alignImagesCommandLine

    //------------------------------------------------------------------
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

    //------------------------------------------------------------------
    /**
     * Method to write the syntax of the program in the command line.
     */
    private static void dumpSyntax () 
    {
       IJ.log("Purpose: Consistent and elastic registration of two images.");
       IJ.log(" ");
       IJ.log("Usage: bUnwarpj_ ");
       IJ.log("  -help                       : SHOW THIS MESSAGE");
       IJ.log("");
       IJ.log("  -align                      : ALIGN TWO IMAGES");
       IJ.log("          target_image        : In any image format");
       IJ.log("          target_mask         : In any image format");
       IJ.log("          source_image        : In any image format");
       IJ.log("          source_mask         : In any image format");
       IJ.log("          min_scale_def       : Scale of the coarsest deformation");
       IJ.log("                                0 is the coarsest possible");
       IJ.log("          max_scale_def       : Scale of the finest deformation");
       IJ.log("          max_subsamp_fact    : Maximum subsampling factor (power of 2: [0, 1, 2 ... 7]");
       IJ.log("          Div_weight          : Weight of the divergence term");
       IJ.log("          Curl_weight         : Weight of the curl term");
       IJ.log("          Image_weight        : Weight of the image term");
       IJ.log("          Consistency_weight  : Weight of the deformation consistency");
       IJ.log("          Output image 1      : Output result 1 in TIFF");
       IJ.log("          Output image 2      : Output result 2 in TIFF");
       IJ.log("          Optional parameters :");
       IJ.log("             -landmarks        ");
       IJ.log("                   Landmark_weight  : Weight of the landmarks");
       IJ.log("                   Landmark_file    : Landmark file");
       IJ.log("             OR -affine        ");
       IJ.log("                   Affine_file_1    : Initial source affine matrix transformation");
       IJ.log("                   Affine_file_2    : Initial target affine matrix transformation");
       IJ.log("             OR -mono    : Unidirectional registration (source to target)");      
       IJ.log("");
       IJ.log("  -elastic_transform          : TRANSFORM A SOURCE IMAGE WITH A GIVEN ELASTIC DEFORMATION");
       IJ.log("          target_image        : In any image format");
       IJ.log("          source_image        : In any image format");
       IJ.log("          transformation_file : As saved by bUnwarpJ in elastic format");
       IJ.log("          Output image        : Output result in TIFF");
       IJ.log("");
       IJ.log("  -raw_transform              : TRANSFORM A SOURCE IMAGE WITH A GIVEN RAW DEFORMATION");
       IJ.log("          target_image        : In any image format");
       IJ.log("          source_image        : In any image format");
       IJ.log("          transformation_file : As saved by bUnwarpJ in raw format");
       IJ.log("          Output image        : Output result in TIFF");
       IJ.log("");
       IJ.log("  -compare_elastic                   : COMPARE 2 OPPOSITE ELASTIC DEFORMATIONS (BY WARPING INDEX)");
       IJ.log("          target_image               : In any image format");
       IJ.log("          source_image               : In any image format");
       IJ.log("          target_transformation_file : As saved by bUnwarpJ");
       IJ.log("          source_transformation_file : As saved by bUnwarpJ");
       IJ.log("");
       IJ.log("  -compare_elastic_raw                : COMPARE AN ELASTIC DEFORMATION WITH A RAW DEFORMATION (BY WARPING INDEX)");
       IJ.log("          target_image                : In any image format");
       IJ.log("          source_image                : In any image format");
       IJ.log("          Elastic Transformation File : As saved by bUnwarpJ in elastic format");
       IJ.log("          Raw Transformation File     : As saved by bUnwarpJ in raw format");
       IJ.log("");
       IJ.log("  -compare_raw                       : COMPARE 2 ELASTIC DEFORMATIONS (BY WARPING INDEX)");
       IJ.log("          target_image               : In any image format");
       IJ.log("          source_image               : In any image format");
       IJ.log("          Raw Transformation File 1  : As saved by bUnwarpJ in raw format");
       IJ.log("          Raw Transformation File 2  : As saved by bUnwarpJ in raw format");
       IJ.log("");
       IJ.log("  -convert_to_raw                           : CONVERT AN ELASTIC DEFORMATION INTO RAW FORMAT");
       IJ.log("          target_image                      : In any image format");
       IJ.log("          source_image                      : In any image format");
       IJ.log("          Input Elastic Transformation File : As saved by bUnwarpJ in elastic format");
       IJ.log("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.log("");
       IJ.log("  -compose_elastic                          : COMPOSE TWO ELASTIC DEFORMATIONS");
       IJ.log("          target_image                      : In any image format");
       IJ.log("          source_image                      : In any image format");
       IJ.log("          Elastic Transformation File 1     : As saved by bUnwarpJ in elastic format");
       IJ.log("          Elastic Transformation File 2     : As saved by bUnwarpJ in elastic format");
       IJ.log("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.log("");
       IJ.log("  -compose_raw                              : COMPOSE TWO RAW DEFORMATIONS");
       IJ.log("          target_image                      : In any image format");
       IJ.log("          source_image                      : In any image format");
       IJ.log("          Raw Transformation File 1         : As saved by bUnwarpJ in raw format");
       IJ.log("          Raw Transformation File 2         : As saved by bUnwarpJ in raw format");
       IJ.log("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.log("");
       IJ.log("  -compose_raw_elastic                      : COMPOSE A RAW DEFORMATION WITH AN ELASTIC DEFORMATION");
       IJ.log("          target_image                      : In any image format");
       IJ.log("          source_image                      : In any image format");
       IJ.log("          Raw Transformation File           : As saved by bUnwarpJ in raw format");
       IJ.log("          Elastic Transformation File       : As saved by bUnwarpJ in elastic format");       
       IJ.log("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.log("");
       IJ.log("  -adapt_transform                           : ADAPT AN ELASTIC DEFORMATION GIVEN A NEW IMAGE SIZE");
       IJ.log("          target_image                       : In any image format");
       IJ.log("          source_image                       : In any image format");
       IJ.log("          Input Elastic Transformation File  : As saved by bUnwarpJ in elastic format");
       IJ.log("          Output Elastic Transformation File : As saved by bUnwarpJ in elastic format");
       IJ.log("          Image Size Factor                  : Integer (2, 4, 8...)");
       IJ.log("");
       IJ.log("Examples:");
       IJ.log("Align two images without landmarks and without mask (no subsampling)");
       IJ.log("   bUnwarpj_ -align target.jpg NULL source.jpg NULL 0 2 0 0.1 0.1 1 10 output_1.tif output_2.tif");
       IJ.log("Align two images with landmarks and mask (no subsampling)");
       IJ.log("   bUnwarpj_ -align target.tif target_mask.tif source.tif source_mask.tif 0 2 0 0.1 0.1 1 10 output_1.tif output_2.tif -landmarks 1 landmarks.txt");
       IJ.log("Align two images with landmarks and initial affine transformations (no subsampling)");
       IJ.log("   bUnwarpj_ -align target.tif target_mask.tif source.tif source_mask.tif 0 2 0 0.1 0.1 1 10 output_1.tif output_2.tif -affine affine_mat1.txt affine_mat2.txt");       
       IJ.log("Align two images using only landmarks (no subsampling)");
       IJ.log("   bUnwarpj_ -align target.jpg NULL source.jpg NULL 0 2 0 0.1 0.1 0 0 output.tif_1 output_2.tif -landmarks 1 landmarks.txt");
       IJ.log("Transform the source image with a previously computed elastic transformation");
       IJ.log("   bUnwarpj_ -elastic_transform target.jpg source.jpg elastic_transformation.txt output.tif");       
       IJ.log("Transform the source image with a previously computed raw transformation");
       IJ.log("   bUnwarpj_ -raw_transform target.jpg source.jpg raw_transformation.txt output.tif");
       IJ.log("Calculate the warping index of two opposite elastic transformations");
       IJ.log("   bUnwarpj_ -compare_elastic target.jpg source.jpg source_transformation.txt target_transformation.txt");
       IJ.log("Calculate the warping index between an elastic transformation and a raw transformation");
       IJ.log("   bUnwarpj_ -compare_elastic_raw target.jpg source.jpg elastic_transformation.txt raw_transformation.txt");
       IJ.log("Calculate the warping index between two raw transformations");
       IJ.log("   bUnwarpj_ -compare_raw target.jpg source.jpg raw_transformation_1.txt raw_transformation_2.txt");
       IJ.log("Convert an elastic transformation into raw format");
       IJ.log("   bUnwarpj_ -convert_to_raw target.jpg source.jpg elastic_transformation.txt output_raw_transformation.txt");
       IJ.log("Compose two elastic transformations ");
       IJ.log("   bUnwarpj_ -compose_elastic target.jpg source.jpg elastic_transformation_1.txt elastic_transformation_2.txt output_raw_transformation.txt");
       IJ.log("Compose two raw transformations ");
       IJ.log("   bUnwarpj_ -compose_raw target.jpg source.jpg raw_transformation_1.txt raw_transformation_2.txt output_raw_transformation.txt");
       IJ.log("Compose a raw transformation with an elastic transformation ");
       IJ.log("   bUnwarpj_ -compose_raw_elastic target.jpg source.jpg raw_transformation.txt elastic_transformation.txt output_raw_transformation.txt");
       IJ.log("Adapt an elastic transformation to a new image size ");
       IJ.log("   bUnwarpj_ -adapt_transform target.jpg source.jpg input_transformation.txt output_transformation.txt 2");
    } /* end dumpSyntax */

    //------------------------------------------------------------------
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
       IJ.log("Target image                 : " + fn_target);
       IJ.log("Source image                 : " + fn_source);
       IJ.log("Input Transformation file    : " + fn_tnf);
       IJ.log("Output Transformation file   : " + fn_out);
       IJ.log("Image Size Factor            : " + sImageSizeFactor);

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
       
       BSplineModel source = new BSplineModel(sourceImp.getProcessor(), false, 1);
       source.setPyramidDepth(0);       
       source.startPyramids();

       // Load transformation
       int intervals = MiscTools.numberOfIntervalsOfTransformation(fn_tnf);
       double [][]cx = new double[intervals+3][intervals+3];
       double [][]cy = new double[intervals+3][intervals+3];
       MiscTools.loadTransformation(fn_tnf, cx, cy);

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
       MiscTools.saveElasticTransformation(intervals, cx, cy, fn_out);
       
    } /* end adaptCoefficientsCommandLine */    
    
    //------------------------------------------------------------------
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
       IJ.log("Target image           : " + fn_target);
       IJ.log("Source image           : " + fn_source);
       IJ.log("Transformation file    : " + fn_tnf);
       IJ.log("Output:                : " + fn_out);

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
   
       BSplineModel source = null;

       source = new BSplineModel(sourceImp.getProcessor(), false, 1);
       source.setPyramidDepth(0);
       //source.startPyramids();
       source.startPyramids();
       

       // Load transformation
       int intervals=MiscTools.numberOfIntervalsOfTransformation(fn_tnf);
       double [][]cx=new double[intervals+3][intervals+3];
       double [][]cy=new double[intervals+3][intervals+3];
       MiscTools.loadTransformation(fn_tnf, cx, cy);

       // Join threads

       try {
    	   source.getThread().join();
       } catch (InterruptedException e) {
    	   IJ.error("Unexpected interruption exception " + e);
       }


       // Apply transformation to source
       MiscTools.applyTransformationToSourceMT(
    		   sourceImp, targetImp, source, intervals, cx, cy);

       // Save results
       FileSaver fs = new FileSaver(sourceImp);
       boolean ret = fs.saveAsTiff(fn_out);
       if(ret == false)
    	   System.out.println("Error when saving file " + fn_out);
       else
    	   System.out.println("Saved file " + fn_out);
       
    } /* end elasticTransformIMageCommandLine */

    //------------------------------------------------------------------
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
       IJ.log("Target image           : " + fn_target);
       IJ.log("Source image           : " + fn_source);
       IJ.log("Transformation file    : " + fn_tnf);
       IJ.log("Output:                : " + fn_out);

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
       
       BSplineModel source = new BSplineModel(sourceImp.getProcessor(), false, 1);
       source.setPyramidDepth(0);
       //source.getThread().start();
       source.startPyramids();

       double [][]transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][]transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];

       MiscTools.loadRawTransformation(fn_tnf, transformation_x, transformation_y);

       // Apply transformation
       MiscTools.applyRawTransformationToSource(sourceImp, targetImp, source, transformation_x, transformation_y);

       // Save results
       FileSaver fs = new FileSaver(sourceImp);
       boolean ret = fs.saveAsTiff(fn_out);
       if(ret == false)
    	   System.out.println("Error when saving file " + fn_out);
       else
    	   System.out.println("Saved file " + fn_out);
       
    } /* end rawTransformImageCommandLine */    
    
    //------------------------------------------------------------------
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
       IJ.log("Target image                  : " + fn_target);
       IJ.log("Source image                  : " + fn_source);
       IJ.log("Target Transformation file    : " + fn_tnf_1);
       IJ.log("Source Transformation file    : " + fn_tnf_2);

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
       int intervals = MiscTools.numberOfIntervalsOfTransformation(fn_tnf_2);

       double [][]cx_direct = new double[intervals+3][intervals+3];
       double [][]cy_direct = new double[intervals+3][intervals+3];

       MiscTools.loadTransformation(fn_tnf_2, cx_direct, cy_direct);      

       intervals = MiscTools.numberOfIntervalsOfTransformation(fn_tnf_1);

       double [][]cx_inverse = new double[intervals+3][intervals+3];
       double [][]cy_inverse = new double[intervals+3][intervals+3];

       MiscTools.loadTransformation(fn_tnf_1, cx_inverse, cy_inverse);
       
       double warpingIndex = MiscTools.warpingIndex(sourceImp, targetImp, intervals, cx_direct, cy_direct, cx_inverse, cy_inverse);

       if(warpingIndex != -1)
           IJ.log(" Warping index = " + warpingIndex);             
       else
           IJ.log(" Warping index could not be evaluated because not a single pixel matched after the deformation!");             
       
    } /* end method compareElasticTransformationsCommandLine */
    
    //------------------------------------------------------------------
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
       IJ.log("Target image                  : " + fn_target);
       IJ.log("Source image                  : " + fn_source);
       IJ.log("Elastic Transformation file   : " + fn_tnf_elastic);
       IJ.log("Raw Transformation file       : " + fn_tnf_raw);

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
       
       int intervals = MiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx_direct = new double[intervals+3][intervals+3];
       double [][]cy_direct = new double[intervals+3][intervals+3];

       MiscTools.loadTransformation(fn_tnf_elastic, cx_direct, cy_direct);
      
       // We load the transformation raw file.
       double[][] transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
       MiscTools.loadRawTransformation(fn_tnf_raw, transformation_x, 
               transformation_y);
       
       double warpingIndex = MiscTools.rawWarpingIndex(sourceImp, targetImp, 
               intervals, cx_direct, cy_direct, transformation_x, transformation_y);

       if(warpingIndex != -1)
           IJ.log(" Warping index = " + warpingIndex);             
       else
           IJ.log(" Warping index could not be evaluated because not a single pixel matched after the deformation!");             
       
    } /* end method compareElasticRawTransformationCommandLine */    
    
    //------------------------------------------------------------------
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
       IJ.log("Target image                  : " + fn_target);
       IJ.log("Source image                  : " + fn_source);
       IJ.log("Target Transformation file    : " + fn_tnf_1);
       IJ.log("Source Transformation file    : " + fn_tnf_2);

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
       MiscTools.loadRawTransformation(fn_tnf_1, transformation_x_1, transformation_y_1);
       
       // We load the transformation raw file.
       double[][] transformation_x_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       MiscTools.loadRawTransformation(fn_tnf_2, transformation_x_2, transformation_y_2);
       
       double warpingIndex = MiscTools.rawWarpingIndex(sourceImp, targetImp, 
               transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2);

       if(warpingIndex != -1)
           IJ.log(" Warping index = " + warpingIndex);             
       else
           IJ.log(" Warping index could not be evaluated because not a single pixel matched after the deformation!");            
       
    } /* end method compareRawTransformationsCommandLine */

    //------------------------------------------------------------------
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
       IJ.log("Target image                      : " + fn_target);
       IJ.log("Source image                      : " + fn_source);
       IJ.log("Input Elastic Transformation file : " + fn_tnf_elastic);
       IJ.log("Ouput Raw Transformation file     : " + fn_tnf_raw);

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
       
       int intervals = MiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx = new double[intervals+3][intervals+3];
       double [][]cy = new double[intervals+3][intervals+3];

       MiscTools.loadTransformation(fn_tnf_elastic, cx, cy);
       
       
       // We load the transformation raw file.
       double[][] transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
       
       MiscTools.convertElasticTransformationToRaw(targetImp, intervals, cx, cy, transformation_x, transformation_y); 
       
       MiscTools.saveRawTransformation(fn_tnf_raw, targetImp.getWidth(), targetImp.getHeight(), transformation_x, transformation_y);
       
    } /* end method convertToRawTransformationCommandLine */    

    //------------------------------------------------------------------
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
       IJ.log("Target image                      : " + fn_target);
       IJ.log("Source image                      : " + fn_source);
       IJ.log("Input Raw Transformation file 1   : " + fn_tnf_raw_1);
       IJ.log("Input Raw Transformation file 2   : " + fn_tnf_raw_2);
       IJ.log("Output Raw Transformation file    : " + fn_tnf_raw_out);

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
       MiscTools.loadRawTransformation(fn_tnf_raw_1, transformation_x_1, transformation_y_1);
              
       // We load the second transformation raw file.
       double[][] transformation_x_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       MiscTools.loadRawTransformation(fn_tnf_raw_2, transformation_x_2, transformation_y_2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       MiscTools.composeRawTransformations(targetImp.getWidth(), targetImp.getHeight(), 
               transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2, 
               outputTransformation_x, outputTransformation_y);
              
       MiscTools.saveRawTransformation(fn_tnf_raw_out, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);
       
    } /* end method composeRawTransformationsCommandLine */     
    
    //------------------------------------------------------------------
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
       IJ.log("Target image                        : " + fn_target);
       IJ.log("Source image                        : " + fn_source);
       IJ.log("Input Elastic Transformation file 1 : " + fn_tnf_elastic_1);
       IJ.log("Input Elastic Transformation file 2 : " + fn_tnf_elastic_2);
       IJ.log("Output Raw Transformation file      : " + fn_tnf_raw);

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
       
       int intervals = MiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic_1);

       double [][]cx1 = new double[intervals+3][intervals+3];
       double [][]cy1 = new double[intervals+3][intervals+3];

       MiscTools.loadTransformation(fn_tnf_elastic_1, cx1, cy1);

       intervals = MiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic_2);

       double [][]cx2 = new double[intervals+3][intervals+3];
       double [][]cy2 = new double[intervals+3][intervals+3];

       MiscTools.loadTransformation(fn_tnf_elastic_2, cx2, cy2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       MiscTools.composeElasticTransformations(targetImp, intervals, 
               cx1, cy1, cx2, cy2, outputTransformation_x, outputTransformation_y);
       
       
       MiscTools.saveRawTransformation(fn_tnf_raw, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);       
       
    } /* end method composeElasticTransformationsCommandLine */
    
    //------------------------------------------------------------------
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
       IJ.log("Target image                      : " + fn_target);
       IJ.log("Source image                      : " + fn_source);
       IJ.log("Input Raw Transformation file     : " + fn_tnf_raw_in);
       IJ.log("Input Elastic Transformation file : " + fn_tnf_elastic);
       IJ.log("Output Raw Transformation file    : " + fn_tnf_raw_out);

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
       MiscTools.loadRawTransformation(fn_tnf_raw_in, transformation_x_1, transformation_y_1);              

       int intervals = MiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx2 = new double[intervals+3][intervals+3];
       double [][]cy2 = new double[intervals+3][intervals+3];

       MiscTools.loadTransformation(fn_tnf_elastic, cx2, cy2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       MiscTools.composeRawElasticTransformations(targetImp, intervals, 
               transformation_x_1, transformation_y_1, cx2, cy2, outputTransformation_x, outputTransformation_y);
       
       
       MiscTools.saveRawTransformation(fn_tnf_raw_out, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);       
       
    } /* end method composeRawElasticTransformationsCommandLine */
    
    //------------------------------------------------------------------
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

    //------------------------------------------------------------------
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
    
    //------------------------------------------------------------------
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
    
    //------------------------------------------------------------------
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
    
    //------------------------------------------------------------------
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
    
    //------------------------------------------------------------------
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
    
    //------------------------------------------------------------------
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
    
    //------------------------------------------------------------------
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
    
    //------------------------------------------------------------------
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
    
    //------------------------------------------------------------------
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