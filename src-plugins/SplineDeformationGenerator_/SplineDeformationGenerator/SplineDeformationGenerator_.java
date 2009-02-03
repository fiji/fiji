package SplineDeformationGenerator;

/**
 * @(#)SplineDeformationGenerator_.java
 *
 * This work is an extension by Ignacio Arganda-Carreras 
 * of the previous SplineDeformationGenerator project by 
 * Carlos Oscar Sanchez Sorzano.
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/SplineDeformationGenerator/
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

/*=======================================================================
 *
 * Version: March 30th, 2007
 *
 * http://biocomp.cnb.csic.es/%7Eiarganda/SplineDeformationGenerator/
 *
 * Updated June 12th 2008: Fiji structure (static inner classes, package
                           and GNU license)
\========================================================================*/

import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.JpegWriter;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.WindowManager;
import ij.io.Opener;
import ij.io.FileSaver;
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
import java.util.Stack;
import java.util.Vector;
import java.util.Random;
import java.util.StringTokenizer;



/*====================================================================
|	SplineDeformationGenerator_
\===================================================================*/
/**
 * Main class.
 * <p>
 * This is the one called by ImageJ
 *
 * <p>
 * SplineDeformationGenerator_ allows 5 types of image
 * deformations: elastic, fisheye, perspective, barrel/pincushion
 * and 2D gels (smile effect).
 *
 * <p>
 * This work is an extension by Ignacio Arganda-Carreras 
 * of the previous SplineDeformationGenerator project by 
 * Carlos Oscar Sanchez Sorzano.
 *
 * @version 03/30/2007
 * @author Ignacio Arganda-Carreras
 */
public class SplineDeformationGenerator_
	implements
		PlugIn

{ /* begin class SplineDeformationGenerator_ */

    /*....................................................................
	Public variables
    ....................................................................*/
    public static final byte MODE_ELASTIC      = 0;
    public static final byte MODE_FISHEYE      = 1;
    public static final byte MODE_PERSPECTIVE  = 2;
    public static final byte MODE_BARREL       = 3;
    public static final byte MODE_2D_GEL       = 4;
    
    
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
       String options = Macro.getOptions();

       if (!commandLine.equals("")) options = commandLine;

       if (options == null) 
       {
          Runtime.getRuntime().gc();
          final ImagePlus[] imageList = createImageList();
          if (imageList.length < 1) 
          {
             IJ.error("At least one image is required (stack of color images disallowed)");
             return;
          }

          final splineDeformationGeneratorDialog dialog = new splineDeformationGeneratorDialog(IJ.getInstance(), imageList);
          GUI.center(dialog);
          dialog.setVisible(true);
       } 
       else 
       {
          final String[] args = getTokens(options);
          if (args.length<1) 
          {
             dumpSyntax();
             return;
          } 
          else 
          {              
              if      (args[0].equals("-help"))         dumpSyntax();
              else if (args[0].equals("-elastic"))      generateElasticDeformationMacro(args);
              else if (args[0].equals("-fisheye"))      generateFisheyeDeformationMacro(args);
              else if (args[0].equals("-perspective"))  generatePerspectiveDeformationMacro(args);
              else if (args[0].equals("-barrel"))       generateBarrelDeformationMacro(args);
              else if (args[0].equals("-gels"))         generate2DGelsDeformationMacro(args);
          }
          return;
       }
    } /* end run */

    
    /*------------------------------------------------------------------*/
    /**
     * Main method for SlineDeformationGenerator.
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
          if      (args[0].equals("-help"))         dumpSyntax();
          else if (args[0].equals("-elastic"))      generateElasticDeformationMacro(args);
          else if (args[0].equals("-fisheye"))      generateFisheyeDeformationMacro(args);
          else if (args[0].equals("-perspective"))  generatePerspectiveDeformationMacro(args);
          else if (args[0].equals("-barrel"))       generateBarrelDeformationMacro(args);
          else if (args[0].equals("-gels"))         generate2DGelsDeformationMacro(args);
       }
       System.exit(0);
    }
    
    /*....................................................................
            Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Method to write the syntaxis of the program in the command line.
     */
    private static void dumpSyntax () 
    {
       IJ.write("Purpose: Spline deformations generator.");
       IJ.write(" ");
       IJ.write("Usage: SplineDeformationGenerator_ ");
       IJ.write("  -help                       : SHOW THIS MESSAGE");
       IJ.write("");
       IJ.write("  -elastic                    : GENERATE AN ELASTIC DEFORMED VERSION OF THE INPUT IMAGE");
       IJ.write("          source_image        : In any image format");
       IJ.write("          minimum_scale       : Scale of the coarsest deformation");
       IJ.write("                                0 is the coarsest possible");
       IJ.write("          maximum_scale       : Scale of the finest deformation");
       IJ.write("          noise_spline        : noise spline");
       IJ.write("          output_image        : Output result in JPEG");
       IJ.write("          Optional parameters :");
       IJ.write("              output_transformation : Output transformation raw file");
       IJ.write("");
       IJ.write("  -fisheye                    : GENERATE A FISHEYE DEFORMED VERSION OF THE INPUT IMAGE");
       IJ.write("          source_image        : In any image format");
       IJ.write("          num_of_magnifiers   : Number of fisheye magnifiers");
       IJ.write("          magnifier_power     : Magnifier fisheye power");
       IJ.write("          magnifier_size      : Magnifier fisheye diameter");
       IJ.write("          output_image        : Output result in JPEG");
       IJ.write("          Optional parameters :");
       IJ.write("              output_transformation : Output transformation raw file");
       IJ.write("");
       IJ.write("  -perspective                : GENERATE A PERSPECTIVE DEFORMED VERSION OF THE INPUT IMAGE");
       IJ.write("              source_image        : In any image format");
       IJ.write("              noise_scale         : Noise perspective scale");
       IJ.write("              noise_shift         : Noise perspective shift");
       IJ.write("              output_image        : Output result in JPEG");
       IJ.write("              Optional parameters :");
       IJ.write("                  output_transformation : Output transformation raw file");
       IJ.write("");
       IJ.write("  -barrel                    : GENERATE A BARREL/PINCUSHION DEFORMED VERSION OF THE INPUT IMAGE");
       IJ.write("         source_image        : In any image format");
       IJ.write("         noise_K1            : Noise K1");
       IJ.write("         noise_K2            : Noise K2");
       IJ.write("         output_image        : Output result in JPEG");
       IJ.write("         Optional parameters :");
       IJ.write("              output_transformation : Output transformation raw file");
       IJ.write("");
       IJ.write("  -gels                    : GENERATE A 2D-GELS DEFORMED VERSION OF THE INPUT IMAGE");
       IJ.write("       source_image        : In any image format");
       IJ.write("       length_reduction    : Length reduction");
       IJ.write("       max_shift           : Maximum shift");
       IJ.write("       output_image        : Output result in JPEG");
       IJ.write("       Optional parameters :");
       IJ.write("              output_transformation : Output transformation raw file");
       IJ.write(""); 
       IJ.write("Examples:");
       IJ.write("Generate an elastic deformed image saving the raw transformation");
       IJ.write("   SplineDeformationGenerator_ -elastic source.jpg 0 3 10 output.jpg output_transf.txt");
       IJ.write("Generate a fisheye deformed image saving the raw transformation");
       IJ.write("   SplineDeformationGenerator_ -fisheye source.jpg 1 3.0 60.0 output.jpg output_transf.txt");
       IJ.write("Generate a perspective deformed image saving the raw transformation");
       IJ.write("   SplineDeformationGenerator_ -perspective source.jpg 10.0 10.0 output.jpg output_transf.txt");
       IJ.write("Generate a barrel/pincushion deformed image saving the raw transformation");
       IJ.write("   SplineDeformationGenerator_ -barrel source.jpg 0.1 0.05 output.jpg output_transf.txt");
       IJ.write("Generate a 2D-gels deformed image saving the raw transformation");
       IJ.write("   SplineDeformationGenerator_ -gels source.jpg 5.0 30.0 output.jpg output_transf.txt");
    } /* end dumpSyntax */
    
    /*------------------------------------------------------------------*/
    /**
     * Get tokens.
     *
     * @param options options to get the tokens
     * @return tokens
     */
    private String[] getTokens (final String options) 
    {
        StringTokenizer t = new StringTokenizer(options);
        String[] token = new String[t.countTokens()];
        for (int k = 0; (k < token.length); k++) {
                token[k] = t.nextToken();
        }
        return(token);
    } /* end getTokens */
    
    /*------------------------------------------------------------------*/
    /**
     * Method to generate elastic deformations of an input image.
     *
     * @param args method arguments
     */
    private static void generateElasticDeformationMacro (String args[]) 
    {
        if (args.length < 6)
        {
            dumpSyntax();
            System.exit(0);
        }
                        
        // Read input parameters
        String fn_source   = args[1];
        int min_scale      = ((Integer) new Integer(args[2])).intValue();
        int max_scale      = ((Integer) new Integer(args[3])).intValue();
        double noiseSpline = ((Double) new Double(args[4])).doubleValue();
        String fn_out      = args[5];
        String fn_tnf      = "";

        boolean bSaveTransformation = false;
        if (args.length == 7) 
        {
            bSaveTransformation = true;
            fn_tnf = args[6];
        }
        
        int TRANSFORMATIONSPLINEDEGREE = 3;
        // Mode = elastic
        int deformationModelIndex = SplineDeformationGenerator_.MODE_ELASTIC;
        
        // Show parameters
        IJ.write("Source image           : " + fn_source);                
        IJ.write("Min. Scale Deformation : " + min_scale);
        IJ.write("Max. Scale Deformation : " + max_scale);
        IJ.write("Noise Spline           : " + noiseSpline);
        IJ.write("Output image file      : " + fn_out);
        IJ.write("Output transf. file    : " + fn_tnf);
        
        // Open source
        Opener opener = new Opener();
        
        ImagePlus sourceImp = opener.openImage(fn_source);   
        
        splineDeformationGeneratorImageModel source =
               new splineDeformationGeneratorImageModel(sourceImp.getProcessor());

        source.getThread().start();       
        // Join threads.
        try 
        {
            source.getThread().join();
	} 
        catch (InterruptedException e) 
        {
		IJ.error("Unexpected interruption exception");
	}
        
        ImagePlus output_ip = new ImagePlus();
                       
        final splineDeformationGeneratorTransformation warp =
		    new splineDeformationGeneratorTransformation(
			sourceImp, source, deformationModelIndex,
			min_scale, max_scale, noiseSpline, TRANSFORMATIONSPLINEDEGREE,
			0, 0, 0, 0, 0, 0, 0, 0, 0, bSaveTransformation, false, false,
                        fn_out, fn_tnf, output_ip);
	warp.generateDeformation();
    
        // Save result as JPEG
       ImageConverter converter = new ImageConverter(output_ip);
       converter.convertToGray16();
       FileSaver fs = new FileSaver(output_ip);
       JpegWriter js = new JpegWriter();
       js.setQuality(100);
       WindowManager.setTempCurrentImage(output_ip);	
       js.run(fn_out);               
    }
    /*------------------------------------------------------------------*/
    /**
     * Method to generate fisheye deformations of an input image.
     *
     * @param args method arguments
     */
    private static void generateFisheyeDeformationMacro (String args[]) 
    {
        if (args.length < 6)
        {
            dumpSyntax();
            System.exit(0);
        }
         
       
        // Read input parameters
        String fn_source       = args[1];
        int num_of_magnifiers  = ((Integer) new Integer(args[2])).intValue();
        double magnifier_power = ((Double) new Double(args[3])).doubleValue();
        double magnifier_size  = ((Double) new Double(args[4])).doubleValue();
        
        String fn_out      = args[5];
        String fn_tnf      = "";

        boolean bSaveTransformation = false;
        if (args.length == 7) 
        {
            bSaveTransformation = true;
            fn_tnf = args[6];
        }
        
        int TRANSFORMATIONSPLINEDEGREE = 3;
        // Mode = fisheye
        int deformationModelIndex = SplineDeformationGenerator_.MODE_FISHEYE;
        
        // Show parameters
        IJ.write("Source image           : " + fn_source);        
        IJ.write("Number of magnifiers   : " + num_of_magnifiers);
        IJ.write("Magnifier power        : " + magnifier_power);
        IJ.write("Magnifier size         : " + magnifier_size);
        IJ.write("Output image file      : " + fn_out);
        IJ.write("Output transf. file    : " + fn_tnf);
        
        // Open source
        Opener opener = new Opener();
        
        ImagePlus sourceImp = opener.openImage(fn_source);   
        
        splineDeformationGeneratorImageModel source =
               new splineDeformationGeneratorImageModel(sourceImp.getProcessor());

        source.getThread().start();       
        // Join threads.
        try 
        {
            source.getThread().join();
	} 
        catch (InterruptedException e) 
        {
            IJ.error("Unexpected interruption exception");
	}
        
        ImagePlus output_ip = new ImagePlus();
                       
        final splineDeformationGeneratorTransformation warp =
		    new splineDeformationGeneratorTransformation(
			sourceImp, source, deformationModelIndex,
			0, 0, 0, TRANSFORMATIONSPLINEDEGREE,
			num_of_magnifiers, magnifier_power, magnifier_size,
                        0, 0, 0, 0, 0, 0, bSaveTransformation, false, false,
                        fn_out, fn_tnf, output_ip);
	warp.generateDeformation();
    
        // Save result as JPEG
       ImageConverter converter = new ImageConverter(output_ip);
       converter.convertToGray16();
       FileSaver fs = new FileSaver(output_ip);
       JpegWriter js = new JpegWriter();
       js.setQuality(100);
       WindowManager.setTempCurrentImage(output_ip);	
       js.run(fn_out);               
    }
    
    /*------------------------------------------------------------------*/
    /**
     * Method to generate perspective deformations of an input image.
     *
     * @param args method arguments
     */
    private static void generatePerspectiveDeformationMacro (String args[]) 
    {
        if (args.length < 5)
        {
            dumpSyntax();
            System.exit(0);
        }                              
        
        // Read input parameters
        String fn_source   = args[1];
        double noise_scale = ((Double) new Double(args[2])).doubleValue();
        double noise_shift = ((Double) new Double(args[3])).doubleValue();
        String fn_out      = args[4];
        String fn_tnf      = "";

        boolean bSaveTransformation = false;
        if (args.length == 6) 
        {
            bSaveTransformation = true;
            fn_tnf = args[5];
        }
        
        int TRANSFORMATIONSPLINEDEGREE = 3;
        
        // Mode = perspective
        int deformationModelIndex = SplineDeformationGenerator_.MODE_PERSPECTIVE;
       
        // Show parameters
        IJ.write("Source image           : " + fn_source);        
        IJ.write("Noise scale            : " + noise_scale);
        IJ.write("Noise shift            : " + noise_shift);        
        IJ.write("Output image file      : " + fn_out);
        IJ.write("Output transf. file    : " + fn_tnf);
        
        // Open source
        Opener opener = new Opener();
        
        ImagePlus sourceImp = opener.openImage(fn_source);   
        
        splineDeformationGeneratorImageModel source =
               new splineDeformationGeneratorImageModel(sourceImp.getProcessor());

        source.getThread().start();       
        // Join threads.
        try 
        {
            source.getThread().join();
	} 
        catch (InterruptedException e) 
        {
		IJ.error("Unexpected interruption exception");
	}
        
        ImagePlus output_ip = new ImagePlus();
                       
        final splineDeformationGeneratorTransformation warp =
		    new splineDeformationGeneratorTransformation(
			sourceImp, source, deformationModelIndex,
			0, 0, 0, TRANSFORMATIONSPLINEDEGREE,
			0, 0, 0, noise_scale, noise_shift, 0, 0, 0, 0, bSaveTransformation, false, false,
                        fn_out, fn_tnf, output_ip);
	warp.generateDeformation();
    
        // Save result as JPEG
       ImageConverter converter = new ImageConverter(output_ip);
       converter.convertToGray16();
       FileSaver fs = new FileSaver(output_ip);
       JpegWriter js = new JpegWriter();
       js.setQuality(100);
       WindowManager.setTempCurrentImage(output_ip);	
       js.run(fn_out);               
    }
    
    /*------------------------------------------------------------------*/
    /**
     * Method to generate barrel/pincushion deformations of an input image.
     *
     * @param args method arguments
     */
    private static void generateBarrelDeformationMacro (String args[]) 
    {
        if (args.length < 5)
        {
            dumpSyntax();
            System.exit(0);
        }                              
        
        // Read input parameters
        String fn_source  = args[1];
        double noise_K1   = ((Double) new Double(args[2])).doubleValue();
        double noise_K2   = ((Double) new Double(args[3])).doubleValue();
        String fn_out     = args[4];
        String fn_tnf     = "";

        boolean bSaveTransformation = false;
        if (args.length == 6) 
        {
            bSaveTransformation = true;
            fn_tnf = args[5];
        }
        
        int TRANSFORMATIONSPLINEDEGREE = 3;
        
        // Mode = barrel/pincushion
        int deformationModelIndex = SplineDeformationGenerator_.MODE_BARREL;
       
        // Show parameters
        IJ.write("Source image         : " + fn_source);        
        IJ.write("Output image file    : " + fn_out);
        IJ.write("Noise K1             : " + noise_K1);
        IJ.write("Noise K2             : " + noise_K2);        
        IJ.write("Output image file    : " + fn_out);
        IJ.write("Output transf. file  : " + fn_tnf);
        
        // Open source
        Opener opener = new Opener();
        
        ImagePlus sourceImp = opener.openImage(fn_source);   
        
        splineDeformationGeneratorImageModel source =
               new splineDeformationGeneratorImageModel(sourceImp.getProcessor());

        source.getThread().start();       
        // Join threads.
        try 
        {
            source.getThread().join();
	} 
        catch (InterruptedException e) 
        {
		IJ.error("Unexpected interruption exception");
	}
        
        ImagePlus output_ip = new ImagePlus();
                       
        final splineDeformationGeneratorTransformation warp =
		    new splineDeformationGeneratorTransformation(
			sourceImp, source, deformationModelIndex,
			0, 0, 0, TRANSFORMATIONSPLINEDEGREE,
			0, 0, 0, 0, 0, noise_K1, noise_K2,
                        0, 0, bSaveTransformation, false, false,
                        fn_out, fn_tnf, output_ip);
	warp.generateDeformation();
    
        // Save result as JPEG
       ImageConverter converter = new ImageConverter(output_ip);
       converter.convertToGray16();
       FileSaver fs = new FileSaver(output_ip);
       JpegWriter js = new JpegWriter();
       js.setQuality(100);
       WindowManager.setTempCurrentImage(output_ip);	
       js.run(fn_out);               
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Method to generate 2D-gels deformations of an input image.
     *
     * @param args method arguments
     */
    private static void generate2DGelsDeformationMacro (String args[]) 
    {
        if (args.length < 5)
        {
            dumpSyntax();
            System.exit(0);
        }                              
        
        
        // Read input parameters
        String fn_source        = args[1];
        double length_reduction = ((Double) new Double(args[2])).doubleValue();
        double max_shift        = ((Double) new Double(args[3])).doubleValue();
        String fn_out           = args[4];
        String fn_tnf           = "";

        boolean bSaveTransformation = false;
        if (args.length == 6) 
        {
            bSaveTransformation = true;
            fn_tnf = args[5];
        }
        
        int TRANSFORMATIONSPLINEDEGREE = 3;
        
        // Mode = 2D gels
        int deformationModelIndex = SplineDeformationGenerator_.MODE_2D_GEL;
       
        // Show parameters
        IJ.write("Source image         : " + fn_source);        
        IJ.write("Output image file    : " + fn_out);
        IJ.write("Lenght reduction     : " + length_reduction);
        IJ.write("Maximum shift        : " + max_shift);        
        IJ.write("Output image file    : " + fn_out);
        IJ.write("Output transf. file  : " + fn_tnf);
        
        // Open source
        Opener opener = new Opener();
        
        ImagePlus sourceImp = opener.openImage(fn_source);   
        
        splineDeformationGeneratorImageModel source =
               new splineDeformationGeneratorImageModel(sourceImp.getProcessor());

        source.getThread().start();       
        // Join threads.
        try 
        {
            source.getThread().join();
	} 
        catch (InterruptedException e) 
        {
		IJ.error("Unexpected interruption exception");
	}
        
        ImagePlus output_ip = new ImagePlus();
                       
        final splineDeformationGeneratorTransformation warp =
		    new splineDeformationGeneratorTransformation(
			sourceImp, source, deformationModelIndex,
			0, 0, 0, TRANSFORMATIONSPLINEDEGREE,
			0, 0, 0, 0, 0, 0, 0,
                        length_reduction, max_shift, 
                        bSaveTransformation, false, false,
                        fn_out, fn_tnf, output_ip);
	warp.generateDeformation();
    
        // Save result as JPEG
       ImageConverter converter = new ImageConverter(output_ip);
       converter.convertToGray16();
       FileSaver fs = new FileSaver(output_ip);
       JpegWriter js = new JpegWriter();
       js.setQuality(100);
       WindowManager.setTempCurrentImage(output_ip);	
       js.run(fn_out);               
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Create image list
     */
    private ImagePlus[] createImageList () 
    {
        final int[] windowList = WindowManager.getIDList();
        final Stack stack = new Stack();
        for (int k = 0; ((windowList != null) && (k < windowList.length)); k++) 
        {
            final ImagePlus imp = WindowManager.getImage(windowList[k]);
            final int inputType = imp.getType();
            if ((imp.getStackSize() == 1) || (inputType == imp.GRAY8) || (inputType == imp.GRAY16)
                    || (inputType == imp.GRAY32)) 
            {
                stack.push(imp);
            }
        }
        final ImagePlus[] imageList = new ImagePlus[stack.size()];
        int k = 0;
        while (!stack.isEmpty()) 
        {
            imageList[k++] = (ImagePlus)stack.pop();
        }
        return(imageList);
    } /* end createImageList */



    /*====================================================================
    |	splineDeformationGeneratorDialog
    \===================================================================*/

    /*------------------------------------------------------------------*/
    /**
     * Class to define the plugin dialog
     */
    static class splineDeformationGeneratorDialog
            extends
                    Dialog
            implements
                    ActionListener

    { /* begin class splineDeformationGeneratorDialog */

    /*....................................................................
            Private variables
    ....................................................................*/

    /** List of available images in ImageJ */
    private ImagePlus[] imageList;

    /** Image representation (canvas) */
    private ImageCanvas sourceIc;
    /** Image representation (ImagePlus)*/
    private ImagePlus sourceImp;

    /** Image model */
    private splineDeformationGeneratorImageModel source;

    // Dialog related
    /** Index of the source image panel choice */
    private int sourceChoiceIndex = 0;
    /** Deformation model index */
    private int deformationModelIndex = 0;

    /** Minimum scale text field */
    private TextField min_scaleTextField;
    /** Maximum scale text field */
    private TextField max_scaleTextField;
    /** Noise spline text field */
    private TextField noiseSplineTextField;

    /** Number of magnificators text field */
    private TextField number_magTextField;
    /** Magnification power text field */
    private TextField mag_powerTextField;
    /** Magnificators' size */
    private TextField mag_sizeTextField;

    /** Noise perspective scale text field */
    private TextField noisePerspectiveScaleTextField;
    /** Noise perspective shift text field */
    private TextField noisePerspectiveShiftTextField;

    /** K1 noise text field */
    private TextField noiseK1TextField;
    /** K2 noise text field */
    private TextField noiseK2TextField;

    /** Length reduction text field */
    private TextField lengthReductionTextField;
    /** Maximum shift text field */
    private TextField maxShiftTextField;

    /** checkbox for save transformation option */
    private Checkbox ckSaveTransformation;
    /** checkbox for show transformation option */
    private Checkbox ckShowTransformation;

    /** Done button */
    private final Button DoneButton = new Button("Done");

    // Transformation parameters
    /** Transformation spline degree */
    private static int    TRANSFORMATIONSPLINEDEGREE =  3;
    /** Scale of the coarsest deformation */
    private static int    min_scale                  =  0;
    /** Scale of the finest deformation */
    private static int    max_scale                  =  3;
    /** Noise spline */
    private static double noiseSpline                = 10;

    /** Number of fisheye magnificators */
    private static int    number_mag                 =  1;
    /** Magnification power */
    private static double mag_power                  =  3;
    /** Magnificators' size */
    private static double mag_size                   = 60;

    /** Noise perspective scale */
    private static double noisePerspectiveScale      = 10;
    /** Noise perspective shift */
    private static double noisePerspectiveShift      = 10;

    /** K1 noise for barrel/pincushion */
    private static double noiseK1                    =  0.100;
    /** K2 noise for barrel/pincushion */
    private static double noiseK2                    =  0.050;

    /** Length reduction for 2D gel deformation */
    private static double lengthReduction            =  5;
    /** Maximum shift for 2D gel deformation */
    private static double maxShift                   = 30;

    /** Flag for save transformation option */
    private static boolean bSaveTransformation       = false;
    /** Flag for show transformation option */
    private static boolean bShowTransformation       = false;

    /*....................................................................
            Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Action performed method associated to the plugin
     */
    public void actionPerformed (final ActionEvent ae) 
    {
        if (ae.getActionCommand().equals("Cancel")) 
        {
            dispose();
            restoreAll();
        }
        else if (ae.getActionCommand().equals("Done")) 
        {
            dispose();
            joinThreads();
            min_scale   = Integer.valueOf(min_scaleTextField.getText()).intValue();
            max_scale   = Integer.valueOf(max_scaleTextField.getText()).intValue();
            noiseSpline = Double.valueOf(noiseSplineTextField.getText()).doubleValue();
            number_mag  = Integer.valueOf(number_magTextField.getText()).intValue();
            mag_power   = Double.valueOf(mag_powerTextField.getText()).doubleValue();
            mag_size    = Double.valueOf(mag_sizeTextField.getText()).doubleValue();
            noisePerspectiveScale = Double.valueOf(noisePerspectiveScaleTextField.getText()).doubleValue();
            noisePerspectiveShift = Double.valueOf(noisePerspectiveShiftTextField.getText()).doubleValue();
            noiseK1     = Double.valueOf(noiseK1TextField.getText()).doubleValue();
            noiseK2     = Double.valueOf(noiseK2TextField.getText()).doubleValue();
            lengthReduction = Double.valueOf(lengthReductionTextField.getText()).doubleValue();
            maxShift    = Double.valueOf(maxShiftTextField.getText()).doubleValue();

            bSaveTransformation = ckSaveTransformation.getState();
            bShowTransformation = ckShowTransformation.getState();

            ImagePlus output_ip = new ImagePlus();

            final splineDeformationGeneratorTransformation warp =
                    new splineDeformationGeneratorTransformation(
                        sourceImp, source, deformationModelIndex,
                        min_scale, max_scale, noiseSpline, TRANSFORMATIONSPLINEDEGREE,
                        number_mag, mag_power, mag_size,
                        noisePerspectiveScale, noisePerspectiveShift,
                        noiseK1, noiseK2, lengthReduction, maxShift, 
                        bSaveTransformation, bShowTransformation, true,
                        "", "", output_ip);
            warp.generateDeformation();
            restoreAll();
        }
    } /* end actionPerformed */

    /*------------------------------------------------------------------*/
    /**
     * Join threads.
     */
    public void joinThreads () 
    {
        try {
                source.getThread().join();
        } catch (InterruptedException e) {
                IJ.error("Unexpected interruption exception");
        }
    } /* end joinSourceThread */

    /*------------------------------------------------------------------*/
    /**
     * Cancel source image and free memory
     */
    public void restoreAll () 
    {
            cancelSource();
            Runtime.getRuntime().gc();
    } /* end restoreAll */

    /*------------------------------------------------------------------*/
    /**
     *  Spline deformation generator dialog constructor
     */
    public splineDeformationGeneratorDialog (
            final Frame parentWindow,
            final ImagePlus[] imageList) 
    {    
        super(parentWindow, "SplineDeformationGenerator", false);
        this.imageList = imageList;

        // Start concurrent image processing threads
        createSourceImage();

    // Create Source panel
        setLayout(new GridLayout(0, 1));
        final Choice sourceChoice = new Choice();
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
                                        sourceChoiceIndex = newChoiceIndex;
                                        cancelSource();
                                        createSourceImage();
                                }
                                repaint();
                        }
                }
        );
        sourcePanel.add(sourceLabel);
        sourcePanel.add(sourceChoice);

        // Create Deformation model panel
        setLayout(new GridLayout(0, 1));
        final Choice deformationModelChoice = new Choice();
        final Panel deformationModelPanel = new Panel();
        deformationModelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label deformationModelLabel = new Label("Deformation model: ");
        deformationModelChoice.add("Elastic splines");
        deformationModelChoice.add("Fisheye");
        deformationModelChoice.add("Perspective");
        deformationModelChoice.add("Barrel/Pincushion");
        deformationModelChoice.add("2D Gels");
        deformationModelChoice.select(deformationModelIndex);
        deformationModelChoice.addItemListener(
                new ItemListener (
                ) {
                        public void itemStateChanged (
                                final ItemEvent ie
                        ) {
                                deformationModelIndex = deformationModelChoice.getSelectedIndex();
                        }
                }
        );
        deformationModelPanel.add(deformationModelLabel);
        deformationModelPanel.add(deformationModelChoice);	

        // Create min and max scale panel
        final Panel max_scalePanel= new Panel();
        final Panel min_scalePanel= new Panel();
        max_scalePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        min_scalePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_max_scale = new Label();
        final Label label_min_scale = new Label();
        label_min_scale.setText("Minimum scale:");
        label_max_scale.setText("Maximum scale:");
        max_scaleTextField = new TextField("", 5);
        min_scaleTextField = new TextField("", 5);
        max_scaleTextField.setText(""+max_scale);
        min_scaleTextField.setText(""+min_scale);
        max_scaleTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        max_scale = Integer.valueOf(max_scaleTextField.getText()).intValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        min_scaleTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        min_scale = Integer.valueOf(min_scaleTextField.getText()).intValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        min_scalePanel.add(label_min_scale);
        max_scalePanel.add(label_max_scale);
        min_scalePanel.add(min_scaleTextField);
        max_scalePanel.add(max_scaleTextField);
        min_scalePanel.setVisible(true);
        max_scalePanel.setVisible(true);

    // Create noise Spline panel
        final Panel noiseSplinePanel= new Panel();
        noiseSplinePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_noiseSpline = new Label();
        label_noiseSpline.setText("Noise level:");
        noiseSplineTextField = new TextField("", 5);
        noiseSplineTextField.setText(""+noiseSpline);
        noiseSplineTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        noiseSpline = Double.valueOf(noiseSplineTextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        noiseSplinePanel.add(label_noiseSpline);
        noiseSplinePanel.add(noiseSplineTextField);
        noiseSplinePanel.setVisible(true);

    // Create Number of magnifiers panel
        final Panel number_magPanel= new Panel();
        number_magPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_number_mag = new Label();
        label_number_mag.setText("Number of magnifiers:");
        number_magTextField = new TextField("", 5);
        number_magTextField.setText(""+number_mag);
        number_magTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        number_mag = Integer.valueOf(number_magTextField.getText()).intValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        number_magPanel.add(label_number_mag);
        number_magPanel.add(number_magTextField);
        number_magPanel.setVisible(true);

    // Create Magnifier power panel
        final Panel mag_powerPanel= new Panel();
        mag_powerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_mag_power = new Label();
        label_mag_power.setText("Magnifier power:");
        mag_powerTextField = new TextField("", 5);
        mag_powerTextField.setText(""+mag_power);
        mag_powerTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        mag_power = Double.valueOf(mag_powerTextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        mag_powerPanel.add(label_mag_power);
        mag_powerPanel.add(mag_powerTextField);
        mag_powerPanel.setVisible(true);

    // Create Magnifier size panel
        final Panel mag_sizePanel= new Panel();
        mag_sizePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_mag_size = new Label();
        label_mag_size.setText("Magnifier size:");
        mag_sizeTextField = new TextField("", 5);
        mag_sizeTextField.setText(""+mag_size);
        mag_sizeTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        mag_size = Double.valueOf(mag_sizeTextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        mag_sizePanel.add(label_mag_size);
        mag_sizePanel.add(mag_sizeTextField);
        mag_sizePanel.setVisible(true);

    // Create noise perspective panel
        final Panel noisePerspectiveScalePanel= new Panel();
        noisePerspectiveScalePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_noisePerspectiveScale = new Label();
        label_noisePerspectiveScale.setText("Scale noise (%):");
        noisePerspectiveScaleTextField = new TextField("", 5);
        noisePerspectiveScaleTextField.setText(""+noisePerspectiveScale);
        noisePerspectiveScaleTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        noisePerspectiveScale = Double.valueOf(noisePerspectiveScaleTextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        noisePerspectiveScalePanel.add(label_noisePerspectiveScale);
        noisePerspectiveScalePanel.add(noisePerspectiveScaleTextField);
        noisePerspectiveScalePanel.setVisible(true);

    // Create noise perspective panel
        final Panel noisePerspectiveShiftPanel= new Panel();
        noisePerspectiveShiftPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_noisePerspectiveShift = new Label();
        label_noisePerspectiveShift.setText("Shift noise (%):");
        noisePerspectiveShiftTextField = new TextField("", 5);
        noisePerspectiveShiftTextField.setText(""+noisePerspectiveShift);
        noisePerspectiveShiftTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        noisePerspectiveShift = Double.valueOf(noisePerspectiveShiftTextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        noisePerspectiveShiftPanel.add(label_noisePerspectiveShift);
        noisePerspectiveShiftPanel.add(noisePerspectiveShiftTextField);
        noisePerspectiveShiftPanel.setVisible(true);

    // Create noiseK1 panel
        final Panel noiseK1Panel= new Panel();
        noiseK1Panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_noiseK1 = new Label();
        label_noiseK1.setText("Noise K1:");
        noiseK1TextField = new TextField("", 5);
        noiseK1TextField.setText(""+noiseK1);
        noiseK1TextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        noiseK1 = Double.valueOf(noiseK1TextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        noiseK1Panel.add(label_noiseK1);
        noiseK1Panel.add(noiseK1TextField);
        noiseK1Panel.setVisible(true);

    // Create noiseK2 panel
        final Panel noiseK2Panel= new Panel();
        noiseK2Panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_noiseK2 = new Label();
        label_noiseK2.setText("Noise K2:");
        noiseK2TextField = new TextField("", 5);
        noiseK2TextField.setText(""+noiseK2);
        noiseK2TextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        noiseK2 = Double.valueOf(noiseK2TextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        noiseK2Panel.add(label_noiseK2);
        noiseK2Panel.add(noiseK2TextField);
        noiseK2Panel.setVisible(true);

    // Create lengthReduction panel
        final Panel lengthReductionPanel= new Panel();
        lengthReductionPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_lengthReduction = new Label();
        label_lengthReduction.setText("Length Reduction (%):");
        lengthReductionTextField = new TextField("", 5);
        lengthReductionTextField.setText(""+lengthReduction);
        lengthReductionTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        lengthReduction = Double.valueOf(lengthReductionTextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        lengthReductionPanel.add(label_lengthReduction);
        lengthReductionPanel.add(lengthReductionTextField);
        lengthReductionPanel.setVisible(true);

    // Create maxShift panel
        final Panel maxShiftPanel= new Panel();
        maxShiftPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Label label_maxShift = new Label();
        label_maxShift.setText("Maximum shift (%):");
        maxShiftTextField = new TextField("", 5);
        maxShiftTextField.setText(""+maxShift);
        maxShiftTextField.addTextListener(
                new TextListener (
                ) {
                        public void textValueChanged (
                                final TextEvent e
                        ) {
                                boolean validNumber =true;
                                try {
                                        maxShift = Double.valueOf(maxShiftTextField.getText()).doubleValue();
                                } catch (NumberFormatException n) {
                                        validNumber = false;
                                }
                                DoneButton.setEnabled(validNumber);
                        }
                }
        );
        maxShiftPanel.add(label_maxShift);
        maxShiftPanel.add(maxShiftTextField);
        maxShiftPanel.setVisible(true);

        // Create checkbox for saving the transformation
        final Panel saveTransformationPanel = new Panel();
        saveTransformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        ckSaveTransformation = new Checkbox(" Save Transformation", bSaveTransformation);
        saveTransformationPanel.add(ckSaveTransformation);

        // Create checkbox for showing the transformation
        final Panel showTransformationPanel = new Panel();
        showTransformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        ckShowTransformation = new Checkbox(" Show Transformation", bShowTransformation);
        showTransformationPanel.add(ckShowTransformation);

    // Build Done Cancel panel
        final Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        DoneButton.addActionListener(this);
        final Button cancelButton = new Button("Cancel");
        cancelButton.addActionListener(this);
        buttonPanel.add(DoneButton);
        buttonPanel.add(cancelButton);

    // Build separations
        final Label separation1 = new Label("");
        final Label separation2 = new Label("    --------- Elastic splines ---------");
        final Label separation3 = new Label("    ------------ Fisheye ------------");
        final Label separation4 = new Label("    ---------- Perspective ----------");
        final Label separation5 = new Label("    ------- Barrel/Pincushion -------");
        final Label separation6 = new Label("    ------------ 2D Gels ------------");
        final Label separation7 = new Label("");

    // Finally build dialog
        add(sourcePanel);
        add(deformationModelPanel);
        add(saveTransformationPanel);
        add(showTransformationPanel);
        add(separation2);
        add(min_scalePanel);
        add(max_scalePanel);
        add(noiseSplinePanel);
        add(separation3);
        add(number_magPanel);
        add(mag_powerPanel);
        add(mag_sizePanel);
        add(separation4);
        add(noisePerspectiveScalePanel);
        add(noisePerspectiveShiftPanel);
        add(separation5);
        add(noiseK1Panel);
        add(noiseK2Panel);
        add(separation6);
        add(lengthReductionPanel);
        add(maxShiftPanel);        
        add(buttonPanel);
        pack();
    } /* end splineDeformationGeneratorDialog */

    /*....................................................................
            Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Add image list
     */
    private void addImageList (final Choice choice) 
    {
        for (int k = 0; (k < imageList.length); k++)
                choice.add(imageList[k].getTitle());
    } /* end addImageList */

    /*------------------------------------------------------------------*/
    /**
     * Cancel source resources
     */
    private void cancelSource () 
    {
        sourceIc  = null;
        sourceImp.killRoi();
        sourceImp = null;
        source    = null;
        Runtime.getRuntime().gc();
    } /* end cancelSource */

    /*------------------------------------------------------------------*/
    /**
     * Create source image
     */
    private void createSourceImage () 
    {
        sourceImp = imageList[sourceChoiceIndex];
        source    =
           new splineDeformationGeneratorImageModel(sourceImp.getProcessor());
        source.getThread().start();
        sourceIc  = sourceImp.getWindow().getCanvas();
    } /* end createSourceImage */

    /*------------------------------------------------------------------*/
    /**
     * Stop source image thread
     */
    private void stopSourceThread () 
    {
        while (source.getThread().isAlive()) {
                source.getThread().interrupt();
        }
        source.getThread().interrupted();
    } /* end stopSourceThread */

    } /* end class splineDeformationGeneratorDialog */

    /*====================================================================
    |	splineDeformationGeneratorImageModel
    \===================================================================*/

    /*------------------------------------------------------------------*/
    /**
     * Image model class for the spline deformation generator
     */
    static class splineDeformationGeneratorImageModel
            implements
                    Runnable

    { /* begin class splineDeformationGeneratorImageModel */

        /*....................................................................
                Private variables
        ....................................................................*/
        // Thread
        /** image model thread */
        private Thread t;

        // Original image, image spline coefficients, and gradient
        /** original image */
        private double[] image;
        /** spline coefficients */
        private double[] coefficient;

        // Size and other information
        /** image width */
        private int     width;
        /** image height */
        private int     height;
        /** twice image width */
        private int     twiceWidth;
        /** twice image height */
        private int     twiceHeight;
        /** flag to indicate if the spline coefficients are mirrored */
        private boolean coefficientsAreMirrored;

        // Some variables to speedup interpolation
        // All these information is set through prepareForInterpolation()
        /** x- coordinate of the point to interpolate*/
        private double   x;           // Point to interpolate
        /** y- coordinate of the point to interpolate*/
        private double   y;   
        /** x- related indexes */
        private int      xIndex[];    // Indexes related
        /** y- related indexes */
        private int      yIndex[];
        /** x- weight of related splines */
        private double   xWeight[];   // Weights of the splines related
        /** y- weight of related splines */
        private double   yWeight[];
        /** derivative of x- weight of related splines */
        private double   dxWeight[];  // Weights of the splines related
        /** derivative of y- weight of related splines */
        private double   dyWeight[];

        /*....................................................................
                Public methods
        ....................................................................*/

        /*------------------------------------------------------------------*/
        /**
         * Return the full-size image height.      
         */
        public int getHeight () {return(height);}

        /*------------------------------------------------------------------*/
        /** 
         * Return the thread associated. 
         */
        public Thread getThread () {return(t);}

        /*------------------------------------------------------------------*/
        /**
         * Return the full-size image width. 
         */
        public int getWidth () {return(width);}

        /*------------------------------------------------------------------*/
        /**
         * Interpolate the image at a given point. 
         */
        public double interpolateI () {
           // Only SplineDegree=3 is implemented
           double ival=0.0F;
           for (int j = 0; j<4; j++) {
               double s=0.0F;
               int iy=yIndex[j];
               if (iy!=-1) {
                       int p=iy*width;
                       for (int i=0; i<4; i++) {
                               int ix=xIndex[i];
                               if (ix!=-1) 
                                  s += xWeight[i] * coefficient[p + ix];
                       }
                       ival+=yWeight[j] * s;
               }
           }
           return ival;
        } /* end Interpolate Image */

        /*------------------------------------------------------------------*/
        /**
         * Prepare calculation for point interpolation
         *
         * @param x x- point coordinate
         * @param y y- point coordinate
         */
        public void prepareForInterpolation(
           double x, 
           double y) 
        {
            // Remind this point for interpolation
            this.x=x;
            this.y=y;

            int ix=(int)x;
            int iy=(int)y;

            // Set X indexes
            // p is the index of the rightmost influencing spline
                int p = (0.0 <= x) ? (ix + 2) : (ix + 1);
                for (int k = 0; k<4; p--, k++) {
                    if (coefficientsAreMirrored) {
                                int q = (p < 0) ? (-1 - p) : (p);
                                if (twiceWidth <= q) q -= twiceWidth * (q / twiceWidth);
                                xIndex[k] = (width <= q) ? (twiceWidth - 1 - q) : (q);
                        } else
                            xIndex[k] = (p<0 || p>=width) ? (-1):(p);
                }

                // Set Y indexes
                p = (0.0 <= y) ? (iy + 2) : (iy + 1);
                for (int k = 0; k<4; p--, k++) {
                    if (coefficientsAreMirrored) {
                                int q = (p < 0) ? (-1 - p) : (p);
                                if (twiceHeight <= q) q -= twiceHeight * (q / twiceHeight);
                                yIndex[k] = (height <= q) ? (twiceHeight - 1 - q) : (q);
                        } else
                            yIndex[k] = (p<0 || p>=height) ? (-1):(p);
                }

                // Compute how much the sample depart from an integer position
                double ex = x - ((0.0 <= x) ? (ix) : (ix - 1));
                double ey = y - ((0.0 <= y) ? (iy) : (iy - 1));

            // Set X weights for the image and derivative interpolation
                double s = 1.0F - ex;
                dxWeight[0] = 0.5F * ex * ex;
                xWeight[0]  = ex * dxWeight[0] / 3.0F;
                dxWeight[3] = -0.5F * s * s;
                xWeight[3]  = s * dxWeight[3] / -3.0F;
                dxWeight[1] = 1.0F - 2.0F * dxWeight[0] + dxWeight[3];
                xWeight[1]  = 2.0F / 3.0F + (1.0F + ex) * dxWeight[3];
                dxWeight[2] = 1.5F * ex * (ex - 4.0F/ 3.0F);
                xWeight[2]  = 2.0F / 3.0F - (2.0F - ex) * dxWeight[0];

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
        } /* prepareForInterpolation */

        /*------------------------------------------------------------------*/
        /**
         * Start the image precomputations. The computation of the B-spline
         * coefficients of the full-size image is not interruptible; all other
         * methods are. 
         */
        public void run () 
        {
            coefficient = getBasicFromCardinal2D();
            buildCoefficients();
        } /* end run */

        /*------------------------------------------------------------------*/
        /**
         * Converts the pixel array of the incoming ImageProcessor
         * object into a local double array. 
         *
         * @param ip image processor to be converted into a local double array
         */
        public splineDeformationGeneratorImageModel(final ImageProcessor ip) 
        {
            // Initialize thread
                t = new Thread(this);
                t.setDaemon(true);

            // Get image information
                width         = ip.getWidth();
                height        = ip.getHeight();
                twiceWidth    = 2*width;
                twiceHeight   = 2*height;
                coefficientsAreMirrored = true;

                // Copy the pixel array
                int k = 0;
                image = new double[width * height];
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

                // Resize the speedup arrays
                xIndex   = new int[4];
                yIndex   = new int[4];
                xWeight  = new double[4];
                yWeight  = new double[4];
                dxWeight = new double[4];
                dyWeight = new double[4];
        } /* end splineDeformationGeneratorImage */

        /*------------------------------------------------------------------*/
        /**
         * Initialize the model from a set of coefficients 
         *
         * @param c set of coefficients
         */
        public splineDeformationGeneratorImageModel (
                final double [][]c      // Set of B-spline coefficients
        ) 
        {
            // Get the size of the input array
            height      = c.length;
            width       = c[0].length;
            twiceHeight = 2*height;
            twiceWidth  = 2*width;
                coefficientsAreMirrored = false;

            // Copy the array of coefficients
            coefficient=new double[height*width];
            int k=0;
                for (int y=0; y<height; y++, k+= width)
                        System.arraycopy(c[y], 0, coefficient, k, width);

                // Resize the speedup arrays
                xIndex   = new int[4];
                yIndex   = new int[4];
                xWeight  = new double[4];
                yWeight  = new double[4];
                dxWeight = new double[4];
                dyWeight = new double[4];
        }

        /*....................................................................
                Private methods
        ....................................................................*/

        /*------------------------------------------------------------------*/
        /**
         * Change from basic to cardinal (2D)
         *
         * @param basic basic array (input)
         * @param cardinal cardinal array (output)
         * @param width array width
         * @param height array width
         * @param degree n- degree
         */
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
        /**
         * Build spline coefficients
         */
        private void buildCoefficients () 
        {
                int fullWidth;
                int fullHeight;
                double[] fullDual = new double[width * height];
                int halfWidth = width;
                int halfHeight = height;
                basicToCardinal2D(coefficient, fullDual, width, height, 7);
        } /* end buildCoefficients */

        /*------------------------------------------------------------------*/
        /** 
         * Extract a column from a double array
         * 
         * @param array 2D array
         * @param width array's width
         * @param x column index
         * @param column output array
         */
        private void extractColumn (
                final double[] array,
                final int width,
                int x,
                final double[] column) 
        {
                for (int i = 0; (i < column.length); i++, x+=width)
                        column[i] = (double)array[x];
        } /* end extractColumn */

        /*------------------------------------------------------------------*/
        /** 
         * Extract a row from a double array
         * 
         * @param array 2D array
         * @param y row index
         * @param row output array
         */
        private void extractRow (
                final double[] array,
                int y,
                final double[] row) 
        {
                y *= row.length;
                for (int i = 0; (i < row.length); i++)
                        row[i] = (double)array[y++];
        } /* end extractRow */

        /*------------------------------------------------------------------*/
        /**
         * Transform coefficients grom cardinal 2D to basic
         */
        private double[] getBasicFromCardinal2D () 
        {
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
        /**
         * Get initial anti-causal coefficient under mirror-off-bounds
         * conditions
         *
         * @param c array of coefficients
         * @param z pole
         * @param tolerance
         */
        private double getInitialAntiCausalCoefficientMirrorOffBounds (
                final double[] c,
                final double z,
                final double tolerance) 
        {
                return(z * c[c.length - 1] / (z - 1.0));
        } /* end getInitialAntiCausalCoefficientMirrorOffBounds */

        /*------------------------------------------------------------------*/
        /**
         * Get initial causal coefficient poles under mirror-off-bounds
         * conditions
         *
         * @param c array of coefficients
         * @param z pole
         * @param tolerance
         */
        private double getInitialCausalCoefficientMirrorOffBounds (
                final double[] c,
                final double z,
                final double tolerance) 
        {
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
        /** 
         * Put a column into a double array
         * 
         * @param array 2D array
         * @param width array's width
         * @param x column index
         * @param column input 1D array
         */
        private void putColumn (
                final double[] array,
                final int width,
                int x,
                final double[] column) 
        {
            for (int i = 0; (i < column.length); i++, x+=width)
                    array[x] = (double)column[i];
        } /* end putColumn */

        /*------------------------------------------------------------------*/
        /** 
         * Put a row into a double array
         * 
         * @param array 2D array
         * @param y column index
         * @param row input 1D array
         */
        private void putRow (
                final double[] array,
                int y,
                final double[] row) 
        {
            y *= row.length;
            for (int i = 0; (i < row.length); i++)
                    array[y++] = (double)row[i];
        } /* end putRow */

        /*------------------------------------------------------------------*/
        /**
         * Samples to interpolation coefficient (1D).
         *
         * @param c coefficients
         * @param degree n- degree
         * @param tolerance tolerance
         */
        private void samplesToInterpolationCoefficient1D (
                final double[] c,
                final int degree,
                final double tolerance) 
        {
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
        /**
         * Symmetric FIR filter with mirror off bounds (1D) conditions.
         *
         * @param h
         * @param c
         * @param s
         */
        private void symmetricFirMirrorOffBounds1D (
                final double[] h,
                final double[] c,
                final double[] s) 
        {
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

    } /* end class splineDeformationGeneratorImageModel */

    /*====================================================================
    |	splineDeformationGeneratorTransformation
    \===================================================================*/

    /*------------------------------------------------------------------*/
    /**
     * Class to define the transformations that can be generated
     */
    static class splineDeformationGeneratorTransformation

    { /* begin class splineDeformationGeneratorTransformation */

        /*....................................................................
                Private variables
        ....................................................................*/

        // Images
        /** source image model */
        private static splineDeformationGeneratorImageModel   source;

        // Image size & mode
        /** source image height */
        private static int   sourceHeight;
        /** source image width */
        private static int   sourceWidth;
        /** mode that indicates the type of deformation */
        private static int   mode;

        // Elastic spline Transformation parameters
        /** scale of the coarsest deformation */
        private static int    min_scale;
        /** scale of the finest deformation */
        private static int    max_scale;
        /** noise spline */
        private static double noiseSpline;
        /** transformation spline degree */
        private static int    transformationSplineDegree;
        /** spline coefficient intervals */
        private static int    intervals;
        /** x- spline coefficients */
        private static double [][]cx;
        /** y- spline coefficients */
        private static double [][]cy;

        // Fisheye Transformation parameters
        /** number of fisheye magnificators */
        private static int    number_mag;
        /** fisheye magnification power */
        private static double mag_power;
        /** fisheye magnificators' size */
        private static double mag_size;
        /** magnificators' x- origins */
        private static double []mag_x0;
        /** magnificators' y- origins */
        private static double []mag_y0;

        // Perspective transformation parameters
        /** noise perspective scale */
        private static double noisePerspectiveScale;
        /** noise perspective shift */
        private static double noisePerspectiveShift;
        /** perspective array */
        private static double []a;

        // Barrel/Pincushion distortion
        /** K1 noise */
        private static double K1;
        /** K2 noise */
        private static double K2;
        /** K1 noise */
        private static double noiseK1;
        /** K2 noise */
        private static double noiseK2;
        /** maximum r distance */
        private static double r_dist_max;

        // 2D Gels distortion
        /** length reduction */
        private static double lengthReduction;
        /** maximum shift */
        private static double maxShift;
        /** K length */
        private static double Klength;
        /** actual shift */
        private static double actualShift;

        /** save transformation option */
        private static boolean bSaveTransformation;
        /** show transformation option */
        private static boolean bShowTransformation;
        /** show transformation option */
        private static boolean bShowOuputImage;

        /** output image file name */
        private String fn_out;
        /** output raw transformation file name */
        private String fn_tnf;

        /** reference to the output image */
        private final ImagePlus output_ip;

        /*....................................................................
                Public methods
        ....................................................................*/

        /*------------------------------------------------------------------*/
        /**
         * Generate deformation method
         */
        public void generateDeformation () 
        {
            Random rnd_generator=new Random();
            if (mode == SplineDeformationGenerator_.MODE_ELASTIC) 
            {
                // Elastic splines ==========================================
                // Check that the spline order is odd
                if (transformationSplineDegree % 2 != 1) 
                {
                    IJ.error("This function can only be applied with splines of an odd order");
                    return;
                }

                // Ask memory for the transformation coefficients
                intervals = (int)Math.pow(2, min_scale);
                cx = new double[intervals+3][intervals+3];
                cy = new double[intervals+3][intervals+3];

                // Incorporate the identity transformation into the spline coefficient
                for (int i= 0; i<intervals + 3; i++) 
                {
                    final double v = (double)((i - 1) * (sourceHeight - 1)) / (double)intervals;
                    for (int j = 0; j < intervals + 3; j++) 
                    {
                        final double u = (double)((j - 1) * (sourceWidth - 1)) / (double)intervals;
                        cx[i][j] = u;
                        cy[i][j] = v;
                    }
                }

                // Now refine with the different scales
                int s;
                for (s=min_scale; s<=max_scale; s++) 
                {
                    // Number of intervals at this scale and ask for memory
                    intervals=(int) Math.pow(2,s);

                    // Generate noisy coefficients at this level
                    for (int i= 0; i<intervals + 3; i++)
                        for (int j = 0; j < intervals + 3; j++) 
                        {
                            cx[i][j] += rnd_generator.nextGaussian() * noiseSpline;
                            cy[i][j] += rnd_generator.nextGaussian() * noiseSpline;
                        }

                    // Prepare for the next iteration
                    cx = propagateCoeffsToNextLevel(intervals,cx,1.0);
                    cy = propagateCoeffsToNextLevel(intervals,cy,1.0);
                    intervals  *=2;
                    noiseSpline/=2;
                }
                intervals=(int) Math.pow(2,s);
            } 
            else if (mode == SplineDeformationGenerator_.MODE_FISHEYE) 
            {
                // Fisheye ===================================================
                        for (int i=0; i<number_mag; i++) {
                           mag_x0[i]=mag_size+(sourceWidth -2*mag_size)*rnd_generator.nextDouble();
                           mag_y0[i]=mag_size+(sourceHeight-2*mag_size)*rnd_generator.nextDouble();
                        }
                } else if (mode == SplineDeformationGenerator_.MODE_PERSPECTIVE) {
                // Perspective ===============================================
                a[0]=1+rnd_generator.nextGaussian()*noisePerspectiveScale;
                a[1]=  rnd_generator.nextGaussian()*noisePerspectiveScale;
                a[2]=  rnd_generator.nextGaussian()*noisePerspectiveShift;
                a[3]=  rnd_generator.nextGaussian()*noisePerspectiveScale;
                a[4]=1+rnd_generator.nextGaussian()*noisePerspectiveScale;
                a[5]=  rnd_generator.nextGaussian()*noisePerspectiveShift;
                a[6]=  rnd_generator.nextGaussian()*noisePerspectiveScale;
                a[7]=  rnd_generator.nextGaussian()*noisePerspectiveScale;
                } 
                else if (mode==SplineDeformationGenerator_.MODE_BARREL) 
                {
                // Barrel/Pincushion =========================================
                    //K1  =  rnd_generator.nextGaussian()*noiseK1;
                    //K2  =  rnd_generator.nextGaussian()*noiseK2;
                    K1  =  noiseK1;
                    K2  =  noiseK2;
                        for (double r=0; r<=1; r+=0.001) 
                        {
                           double r2 = r*r;
                           double r4 = r2*r2;
                           double r_dist = r*(1+K1*r2+K2*r4);
                           if (r_dist>r_dist_max) 
                               r_dist_max=r_dist;
                        }
                } else if (mode==SplineDeformationGenerator_.MODE_2D_GEL) {
                // 2D Gels ===================================================
                    //Klength     =  1+rnd_generator.nextDouble()*lengthReduction/100;
                    //actualShift =    rnd_generator.nextDouble()*maxShift;
                    Klength     =  1+lengthReduction/100;
                    actualShift =    maxShift;
                }

            // Show results
            showTransformation("Output");
        } /* end generateDeformation */

        /*------------------------------------------------------------------*/
        /**
         * Spline transformation constructor
         * @param sourceImp source image ImagePlus
         * @param source source image reference
         * @param mode type of deformation
         * @param min_scale scale of the coarsest elastic deformation
         * @param max_scale scale of the finest elastic deformation
         * @param noiseSpline noise spline
         * @param transformationSplineDegree trasformation spline degree
         * @param number_mag number of fisheye magnificators
         * @param mag_power magnificators' power
         * @param mag_size magnificators' size
         * @param noisePerspectiveScale noise perspective scale
         * @param noisePerspectiveShift noise perspective shift
         * @param noiseK1 barrel/pincushion K1 noise
         * @param noiseK2 barrel/pincushion K2 noise
         * @param lengthReduction perspective length reduction
         * @param maxShift perspective maximum shift
         * @param bSaveTransformation save transformation flag
         * @param bShowTransformation show transformation flag
         * @param bShowOutputImage show result image flag
         * @param fn_out optional output image file name
         * @param fn_tnf optional result transformation file name
         * @param output_ip result image ImagePlus
         */
        public splineDeformationGeneratorTransformation (
                final ImagePlus sourceImp,
                final splineDeformationGeneratorImageModel source,
                final int mode, 
                final int min_scale,
                final int max_scale,
                final double noiseSpline,
                final int transformationSplineDegree,
                final int number_mag,
                final double mag_power,
                final double mag_size,
                final double noisePerspectiveScale,
                final double noisePerspectiveShift,
                final double noiseK1,
                final double noiseK2,
                final double lengthReduction,
                final double maxShift,
                final boolean bSaveTransformation,
                final boolean bShowTransformation,
                final boolean bShowOutputImage,
                String fn_out,
                String fn_tnf,
                final ImagePlus output_ip) 
        {
            this.source    					= source;
            this.mode                                       = mode;

            this.min_scale 					= min_scale;
            this.max_scale 					= max_scale;
            this.noiseSpline                = noiseSpline;
            this.transformationSplineDegree = transformationSplineDegree;
            intervals                       = 0;
            cx                              = null;
            cy                              = null;

            this.number_mag					= number_mag;
            this.mag_power 					= mag_power;
            this.mag_size                   = mag_size;
            mag_x0                          = new double [number_mag];
            mag_y0                          = new double [number_mag];

            this.noisePerspectiveScale      = noisePerspectiveScale/100;
            this.noisePerspectiveShift      = noisePerspectiveShift/100;
            a                               = new double [8];

            this.noiseK1                    = noiseK1;
            this.noiseK2                    = noiseK2;
            r_dist_max                      = 0;

            this.lengthReduction            = lengthReduction;
            this.maxShift                   = maxShift;

            this.bSaveTransformation        = bSaveTransformation;
            this.bShowTransformation        = bShowTransformation;
            this.bShowOuputImage            = bShowOutputImage;

            this.fn_out                     = fn_out;
            this.fn_tnf                     = fn_tnf;

            this.output_ip                  = output_ip;

            sourceWidth    			= sourceImp.getWidth();
            sourceHeight   			= sourceImp.getHeight();
        } /* end splineDeformationGeneratorTransformation */


        /*....................................................................
                Private methods
        ....................................................................*/

        /*-------------------------------------------------------------------*/
        /**
         * Barrel/pincushion deformation
         *
         * @param x_in input coordinates
         * @param x_out ouput coordinates
         */
        private void camera(final double []x_in, final double []x_out) 
        {
           // Map between -1 and 1
           double xi = 2*x_in[0]/(sourceWidth -1)-1;
           double yi = 2*x_in[1]/(sourceHeight-1)-1;

           // Apply the transformation
           // The model is r_dist = r*(1+K1 r^2)
           // We have to solve for the inverse, given r_dist, what is r?
           double r_dist = Math.sqrt(xi*xi+yi*yi), r;

           r_dist /= Math.sqrt(2); // Normalize so that the corners have r_dist=1

           if (r_dist > r_dist_max) 
               r=2;
           else 
           {
                   double ri_1 = 0, ri = r_dist, improvement;
                   do 
                   {
                      double ri2 = ri*ri;
                      double ri3 = ri2*ri;
                      double ri4 = ri2*ri2;
                      double ri5 = ri3*ri2;
                      ri_1 = ri - (ri+K1*ri3+K2*ri5-r_dist) / (1+3*K1*ri2+5*K2*ri4); // Newton step
                      if (ri_1!=0) 
                          improvement = Math.abs(ri-ri_1)/ri_1;
                      else         
                          improvement = 0;
                      ri = ri_1;
                      //System.out.println("ri="+ri);
                   } while (improvement>1e-5);
                   r = ri;
           }
           double xo = (r_dist == 0) ? xi : xi*r/r_dist;
           double yo = (r_dist == 0) ? yi : yi*r/r_dist;

           // Return to the image coordinates
           x_out[0]=0.5*(sourceWidth-1)*(xo+1);
           x_out[1]=0.5*(sourceHeight-1)*(yo+1);

           //System.out.println(" xin=("+x_in[0]+","+x_in[1]+") "+
           //                   " xi=("+xi+","+yi+") "+
           //                   " r_dist="+r_dist+" --> "+r+
           //                   " xo=("+xo+","+yo+") "+
           //                   " xout=("+x_out[0]+","+x_out[1]+")");
        }

        /*-------------------------------------------------------------------*/
        /**
         * Fisheye deformation
         * 
         * @param x_in input coordinates
         * @param x_out ouput coordinates
         */
        private void fisheye(final double []x_in, final double []x_out) {
           boolean radial=true;
           double xin=x_in[0];
           double yin=x_in[1];
           double xout=xin, yout=yin;

           for (int k=0; k<number_mag; k++) {
                   if (Math.abs(xin-mag_x0[k])>=mag_size || Math.abs(yin-mag_y0[k])>=mag_size) {
                      // Far from the bubble
                      xout=xin;
                      yout=yin;
                   } else {
                      // In the bubble neigbourghood
                          double K=2./(1+Math.exp(-Math.abs(mag_power)))-1; // K=sigmoid(1);

                          // Express the input point with respect to the
                          // magnifier center (between -1 and 1)
                          double xn=(xin-mag_x0[k])/mag_size;
                          double yn=(yin-mag_y0[k])/mag_size;

                      double xo, yo, rn=0, hrn=0, weight=0;
                      if (radial) {
                              // Apply Radial transformation
                                  rn=Math.sqrt(xn*xn+yn*yn);

                                  // Compute radial weight for a continuous transition
                                  // Simple triangle window
                                  // weight=1-rn;
                                  // Raised cosine between r0 and 1
                                  double r0=0.6;
                                  if      (rn<r0) weight=1;
                                  else if (rn<1)  weight=(1+Math.cos(Math.PI*(rn-r0)/(1-r0)))/2;
                                  else            weight=0;

                                  // compute radial magnification
                                  if (rn>1) {
                                     // Outside circle, do not magnify
                                     hrn=rn;
                                  } else if (mag_power>0) {
                                     // Magnifying, apply the inverse of the sigmoid function
                                     double arg=2.0/(rn*K+1.0)-1.0;
                                     if (arg>0) hrn=weight*(-1.0/mag_power*Math.log(arg))+(1-weight)*rn;
                                     else hrn=-1;
                                  } else if (mag_power<0) {
                                     // Compressing, apply the sigmoid function
                                     hrn=weight*1.0/K*(2.0/(1.0+Math.exp(mag_power*rn))-1.0)+(1-weight)*rn;
                                  } else hrn=rn;

                                  // Output coordinates between -1 and 1
                                  if (hrn!=-1) {
                                     if (rn!=0) {
                                             xo=hrn/rn*xn;
                                             yo=hrn/rn*yn;
                                         } else xo=yo=0;
                                  } else xo=yo=-2;
                           } else {
                              // Apply Orthogonal transformation
                                  // Apply transformation to x
                              if (mag_power>0) {
                                     // Magnifying, apply the inverse of the sigmoid function
                                     xo=-1.0/mag_power*Math.log(2.0/(xn*K+1.0)-1.0);
                                  } else if (mag_power<0) {
                                     // Compressing, apply the sigmoid function
                                     xo=1.0/K*(2.0/(1.0+Math.exp(mag_power*xn))-1.0);
                                  } else xo=xn;

                                  // Apply transformation to y
                              if (mag_power>0) {
                                     // Magnifying, apply the inverse of the sigmoid function
                                     yo=-1.0/mag_power*Math.log(2.0/(yn*K+1.0)-1.0);
                                  } else if (mag_power<0) {
                                     // Compressing, apply the sigmoid function
                                     yo=1.0/K*(2.0/(1.0+Math.exp(mag_power*yn))-1.0);
                                  } else yo=yn;
                          }

                          // Return to the image size
                          xout=mag_x0[k]+xo*mag_size;
                          yout=mag_y0[k]+yo*mag_size;

                          //System.out.println(" xi=("+xin+","+yin+") "+
                          //                   " xn=("+xn+","+yn+") "+
                          //                   " rn="+rn+" hrn="+hrn+" "+
                          //                   " xo=("+xo+","+yo+") "+
                          //                   " xout=("+xout+","+yout+")");

                          // Prepare for next iteration
                          xin=xout;
                          yin=yout;
                   }   
           }
           x_out[0]=xout;
           x_out[1]=yout;
        }

        /*-----------------------------------------------------------------------------*/
        /**
         * Perspective deformation
         * 
         * @param x_in input coordinates
         * @param x_out ouput coordinates
         */
        private void perspective(final double []x_in, final double []x_out) 
        {
           // Map between -1 and 1
           double xi=2*x_in[0]/(sourceWidth -1)-1;
           double yi=2*x_in[1]/(sourceHeight-1)-1;

           // Apply transformation
           double den=a[6]*xi+a[7]*yi+1;
           double xo=(a[0]*xi+a[1]*yi+a[2])/den;
           double yo=(a[3]*xi+a[4]*yi+a[5])/den;

           // Return to the image coordinates
           x_out[0]=0.5*(sourceWidth -1)*(xo+1);
           x_out[1]=0.5*(sourceHeight-1)*(yo+1);
        }

        /*-------------------------------------------------------------------*/
        /**
         * 2D gel deformation
         * 
         * @param x_in input coordinates
         * @param x_out ouput coordinates
         */
        private void twoDgel(final double []x_in, final double []x_out)
        {
           // Deformation in X
           x_out[0]=x_in[0];

           // Deformation in Y
           double xdim2=sourceWidth/2.0;
           x_out[1]=// Length reduction effect
                    Klength*x_in[1]-
                    // Smile effect
                    actualShift*(1-(x_in[0]-xdim2)*(x_in[0]-xdim2)/(xdim2*xdim2));
        }

        /*-----------------------------------------------------------------------------*/
        /**
         * Propagate spline coefficients to next level
         *
         * @param intervals number of intervals in the deformation
         * @param c b-spline coefficients
         * @param expansionFactor due to the change of size in the represented image
         * @return propagated coefficients
         */
        private double [][] propagateCoeffsToNextLevel(
           int intervals,
           final double [][]c,
           double expansionFactor     // Due to the change of size in the represented image
        )
        {
           boolean debug=false;

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

           if (debug) {
               System.out.println("Upsampled coefficients");
                   for (int i= 0; i<intervals + 7; i++) {
                           for (int j = 0; j <intervals + 7; j++)
                              System.out.print(cs_expand[i][j]+" ");
                           System.out.println();
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

           if (debug) {
               System.out.println("Upsampled and interpolated(1) coefficients");
                   for (int i= 0; i<intervals + 7; i++) {
                           for (int j = 0; j <intervals + 7; j++)
                              System.out.print(cs_expand_aux[i][j]+" ");
                           System.out.println();
                    }	   
            }

           // Apply the u2n filter to columns
           for (int i=0; i<intervals+7; i++)
                   for (int j=0; j<intervals+7; j++) {
                       cs_expand[i][j]=0.0F;
                       for (int k=-kh; k<=kh; k++)
                           if (i+k>=0 && i+k<=intervals+6)
                              cs_expand[i][j]+=u2n[transformationSplineDegree][k+kh]*cs_expand_aux[i+k][j];
                   }

           if (debug) {
               System.out.println("Upsampled and interpolated coefficients");
                   for (int i= 0; i<intervals + 7; i++) {
                           for (int j = 0; j <intervals + 7; j++)
                              System.out.print(cs_expand[i][j]+" ");
                           System.out.println();
                    }	   
           }

           // Copy the central coefficients to c
           double [][]newc=new double [intervals+3][intervals+3];
           for (int i= 0; i<intervals+3; i++)
                   for (int j = 0; j <intervals + 3; j++)
                       newc[i][j]=cs_expand[i+2][j+2]*expansionFactor;

           // Return the new set of coefficients
           return newc;
        }

        /*-------------------------------------------------------------------*/
        /*------------------------------------------------------------------*/
        /**
         * Show result image in an independent window
         *
         * @param title image title
         * @param array image double values
         */
        private void showImage(
               final String    title,
               final double [][]array) 
            {
                int Ydim=array.length;
                int Xdim=array[0].length;

                final FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
                   for (int i=0; i<Ydim; i++)
                       for (int j=0; j<Xdim; j++)
                          fp.putPixelValue(j,i,array[i][j]);
                fp.resetMinAndMax();

                ImagePlus aux = new ImagePlus(title, fp);

                this.output_ip.setProcessor(title, aux.getProcessor());

                ImageWindow iw = null;
                if(this.bShowOuputImage)
                    iw = new ImageWindow(this.output_ip);

                this.output_ip.updateImage();
            }
        /*------------------------------------------------------------------*/
        /**
         * Generate x- and y- transformation windows
         *
         * @param title window title
         */
        private void showTransformation(final String title)
        {
            boolean show_deformation = false;

            // Ask for memory for the transformation
            double [][] transformation_x = new double [sourceHeight][sourceWidth];
            double [][] transformation_y = new double [sourceHeight][sourceWidth];

            // Compute the transformation mapping
            if (mode==SplineDeformationGenerator_.MODE_ELASTIC) 
            {
                // Elastic splines ...........................................
                        // Set these coefficients to an interpolator
                        splineDeformationGeneratorImageModel swx = new splineDeformationGeneratorImageModel(cx);
                        splineDeformationGeneratorImageModel swy = new splineDeformationGeneratorImageModel(cy);

                        for (int v=0; v<sourceHeight; v++) {
                                final double tv = (double)(v * intervals) / (double)(sourceHeight - 1) + 1.0F;
                                for (int u = 0; u<sourceWidth; u++) {
                                        final double tu = (double)(u * intervals) / (double)(sourceWidth - 1) + 1.0F;
                                        swx.prepareForInterpolation(tu, tv); transformation_x[v][u] = swx.interpolateI();
                                        swy.prepareForInterpolation(tu, tv); transformation_y[v][u] = swy.interpolateI();
                                }
                        }
                } 
            else if (mode == SplineDeformationGenerator_.MODE_FISHEYE || 
                     mode == SplineDeformationGenerator_.MODE_PERSPECTIVE || 
                     mode == SplineDeformationGenerator_.MODE_BARREL || 
                     mode == SplineDeformationGenerator_.MODE_2D_GEL) {
                    // Fisheye ...................................................
                    // Perspective ...............................................
                    // Barrel/Pincushion .........................................
                    // 2D Gels ...................................................
                    double []x_in =new double [2];
                    double []x_out=new double [2];

                        for (int v=0; v<sourceHeight; v++) 
                                for (int u = 0; u<sourceWidth; u++) 
                                {
                                   x_in[0]=u; x_in[1]=v;
                                   switch (mode) 
                                   {
                                      case SplineDeformationGenerator_.MODE_FISHEYE: 
                                          fisheye(x_in,x_out); 
                                          break;
                                      case SplineDeformationGenerator_.MODE_PERSPECTIVE: 
                                          perspective(x_in,x_out); 
                                          break;
                                      case SplineDeformationGenerator_.MODE_BARREL: 
                                          camera(x_in,x_out); 
                                          break;
                                      case SplineDeformationGenerator_.MODE_2D_GEL: 
                                          twoDgel(x_in,x_out); 
                                          break;
                                   }
                                   transformation_x[v][u] = x_out[0];
                                   transformation_y[v][u] = x_out[1];
                                }
                }

            // Show deformation
            if (this.bShowTransformation) 
            {
                splineDeformationGeneratorMiscTools.showImage("Transf. X", transformation_x);
                splineDeformationGeneratorMiscTools.showImage("Transf. Y", transformation_y);
            }

            // Useful variables for applying the transformation
            final double hBound = (double)sourceWidth - 0.5F;
            final double vBound = (double)sourceHeight - 0.5F;

            // Compute the warped image
            final double transformedImage [][] = new double [sourceHeight][sourceWidth];
            for (int v=0; v<sourceHeight; v++)
                for (int u=0; u<sourceWidth; u++) 
                {
                    final double x = transformation_x[v][u];
                    final double y = transformation_y[v][u];
                    if ((-0.5F < x) && (x < hBound) && (-0.5F < y) && (y < vBound)) 
                    {
                       source.prepareForInterpolation(x,y);
                       transformedImage[v][u] = source.interpolateI();
                    } 
                    else
                       transformedImage[v][u] = 0.0F;
                }
            // Show warped image.
            showImage(title, transformedImage);

            // Save transformation.
            if(this.bSaveTransformation)
                splineDeformationGeneratorMiscTools.saveRawTransformation(this.fn_tnf, sourceWidth, sourceHeight, transformation_x, transformation_y);

        }

    } /* end class splineDeformationGeneratorTransformation */

    /*------------------------------------------------------------------*/

    /*====================================================================
    |	Miscellanea tools
    \===================================================================*/
    /**
     * Miscellanea tools class for generating image deformations
     */
    static class splineDeformationGeneratorMiscTools 
    {    
        /*------------------------------------------------------------------*/
        /**
         * Method to display an image in a separate window
         *
         * @param title title of the new window
         * @param array double array with the image information
         * @param Ydim height dimension in pixels
         * @param Xdim width dimension in pixels
         */
        public static void showImage(
           final String    title,
           final double []  array,
           final int       Ydim,
           final int       Xdim) 
        {
            final FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
               for (int i=0; i<Ydim; i++)
                   for (int j=0; j<Xdim; j++)
                      fp.putPixelValue(j,i,array[i*Xdim+j]); 
               fp.resetMinAndMax();
            final ImagePlus      ip=new ImagePlus(title, fp);
            final ImageWindow    iw=new ImageWindow(ip);
               ip.updateImage();
        }

        /*------------------------------------------------------------------*/
        /**
         * Method to display an image in a separate window
         *    
         * @param title title of the new window
         * @param array double array with the image information
         */
        public static void showImage(
           final String    title,
           final double [][]array) 
        {
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

        /*------------------------------------------------------------------*/
        /**
         * Save a raw transformation
         *
         * @param filename raw transformation file name
         * @param width image width
         * @param height image height
         * @param transformation_x transformation coordinates in x-axis
         * @param transformation_y transformation coordinates in y-axis
         *
         * @author Ignacio Arganda-Carreras
         */
        public static void saveRawTransformation(
           String       filename,
           int          width,
           int          height,
           double [][]  transformation_x,
           double [][]  transformation_y) 
        {
            if(filename == null || filename == "")
            {
                String path = "";
                String new_filename = "";

                final Frame f = new Frame();
                final FileDialog fd = new FileDialog(f, "Save Transformation", FileDialog.SAVE);
                fd.setFile(new_filename);
                fd.setVisible(true);
                path = fd.getDirectory();
                filename = fd.getFile();
                if ((path == null) || (filename == null)) return;
                     filename = path+filename;

            }

            // Save the file
            try 
            {
                final FileWriter fw = new FileWriter(filename);
                String aux;
                fw.write("Width=" + width +"\n");
                fw.write("Height=" + height +"\n\n");
                fw.write("X Trans -----------------------------------\n");
                for (int i= 0; i < height; i++) 
                {
                     for (int j = 0; j < width; j++) 
                     {
                        aux="" + transformation_x[i][j];
                        while (aux.length()<21) aux=" "+aux;
                        fw.write(aux+" ");
                     }
                     fw.write("\n");
                }
                fw.write("\n");
                fw.write("Y Trans -----------------------------------\n");
                for (int i= 0; i < height; i++) 
                {
                     for (int j = 0; j < width; j++) 
                     {
                        aux="" + transformation_y[i][j];
                        while (aux.length()<21) aux=" "+aux;
                        fw.write(aux+" ");
                     }
                     fw.write("\n");
                }
                fw.close();
            } 
            catch (IOException e) 
            {
                  IJ.error("IOException exception" + e);
            } 
            catch (SecurityException e) 
            {
                  IJ.error("Security exception" + e);
            }
        }

    } /* End of MiscTools class */


    /*------------------------------------------------------------------*/


} /* end class SplineDeformationGenerator_ */