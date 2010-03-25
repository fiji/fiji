package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ/Fiji.
 * Copyright (C) 2005-2010 Ignacio Arganda-Carreras and Jan Kybic 
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

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
/**
 * Class to launch the registration in bUnwarpJ.
 * This is the class called from the main plugin class after
 * recollecting the registration parameters from the main dialog.
 */
public class FinalAction implements Runnable
{
    /*....................................................................
       Private variables
    ....................................................................*/

    /** thread to run the registration method */
    private Thread t;
    /** dialog for bUnwarpJ interface */
    private MainDialog dialog;

    // Images
    /** image representation for the source */
    private ImagePlus                      sourceImp;
    /** image representation for the target */
    private ImagePlus                      targetImp;
    /** source image model */
    private BSplineModel   source;
    /** target image model */
    private BSplineModel   target;

    // Landmarks
    /** point handler for the landmarks in the source image*/
    private PointHandler sourcePh;
    /** point handler for the landmarks in the target image*/
    private PointHandler targetPh;

    // Masks for the images
    /** source image mask */
    private Mask sourceMsk;
    /** target image mask */
    private Mask targetMsk;

    // Initial affine matrices
    /** source initial affine matrix */
    private double[][] sourceAffineMatrix;
    /** target initial affine matrix */
    private double[][] targetAffineMatrix;

    // Transformation parameters
    /** minimum scale deformation */
    private int     min_scale_deformation;
    /** maximum scale deformation */
    private int     max_scale_deformation;
    /** minimum image scale */
    private int     min_scale_image;
    /** flag to specify the level of resolution in the output */
    private int     outputLevel;
    /** flag to show the optimizer */
    private boolean showMarquardtOptim;
    /** divergence weight */
    private double  divWeight;
    /** curl weight */
    private double  curlWeight;
    /** landmark weight */
    private double  landmarkWeight;
    /** weight for image similarity */
    private double  imageWeight;
    /** weight for the deformations consistency */
    private double  consistencyWeight;
    /** stopping threshold */
    private double  stopThreshold;
    /** level of accuracy */
    private int     accurate_mode;

    /*....................................................................
       Public methods
    ....................................................................*/
    
    /**
     * Start a thread under the control of the main event loop. This thread
     * has access to the progress bar, while methods called directly from
     * within <code>MainDialog</code> do not because they are
     * under the control of its own event loop.
     */
    public FinalAction (final MainDialog dialog)
    {
       this.dialog = dialog;
       t = new Thread(this);
       t.setDaemon(true);
    }    
    
    /* ------------------------------------------------------------------------ */
    /**
     * Get the thread.
     *
     * @return the thread associated with this <code>FinalAction</code>
     *         object
     */
    public Thread getThread ()
    {
       return(t);
    } // end getThread 

    /* ------------------------------------------------------------------------ */
    /**
     * Perform the registration
     */
    public void run ()
    {
    	// Start pyramids
    	IJ.showStatus("Starting image pyramids...");
    	if(target.getWidth() > BSplineModel.MAX_OUTPUT_SIZE || target.getHeight() > BSplineModel.MAX_OUTPUT_SIZE
    		|| source.getWidth() > BSplineModel.MAX_OUTPUT_SIZE || source.getHeight() > BSplineModel.MAX_OUTPUT_SIZE)
    		IJ.log("Starting image pyramids...");
    	
		source.startPyramids();
		target.startPyramids();
		
		// Wait for the pyramids to be done
		dialog.joinThreads();
		
        // Create output image (source-target)
		final ImagePlus [] output_ip = initializeOutputIPs();
		
        // If mono mode, reset consistency weight
        if(this.accurate_mode == MainDialog.MONO_MODE)
        	this.consistencyWeight = 0.0;
        
        //IJ.log("FinalAction: maxImageSubsamplingFactor = " + maxImageSubsamplingFactor);
        
        // Prepare registration parameters
        final Transformation warp = new Transformation(
          sourceImp, targetImp, source, target, sourcePh, targetPh,
          sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
          min_scale_deformation, max_scale_deformation,
          min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
          consistencyWeight, stopThreshold, outputLevel, showMarquardtOptim, accurate_mode, "", "", 
          output_ip[0], output_ip[1], dialog);        
				
        
        // Perform the registration
        IJ.showStatus("Registering...");
        
        long start = System.currentTimeMillis(); // start timing
        
        if(this.accurate_mode == MainDialog.MONO_MODE)     
        {
        	// Do unidirectional registration
        	warp.doUnidirectionalRegistration();
        	// Save transformation
        	if(this.dialog.isSaveTransformationSet())
        		warp.saveDirectTransformation();
        	// Show results.
    		if(source.isSubOutput())
    		{
    			IJ.log("Calculating final transformed source image");
    		}
    		warp.showDirectResults();
    		    		
        }
        else
        {
        	// Do bidirectional registration
        	warp.doBidirectionalRegistration();
        	// Save transformations
        	if(this.dialog.isSaveTransformationSet())
        	{
        		warp.saveDirectTransformation();
        		warp.saveInverseTransformation();
        	}
        	if(source.isSubOutput())
    		{
    			IJ.log("Calculating final transformed source image");
    		}
    		warp.showDirectResults();
        	if(target.isSubOutput())
    		{
    			IJ.log("Calculating final transformed target image");			
    		}
    		warp.showInverseResults();
        }
        
        long stop = System.currentTimeMillis(); // stop timing
        if(outputLevel == 2)
        	IJ.write("\nRegistration time: " + (stop - start) + "ms"); // print execution time

        dialog.restoreAll();
        dialog.freeMemory();
    }

    /* ------------------------------------------------------------------------ */
    /**
     * Pass parameter from <code>MainDialog</code> to
     * <code>FinalAction</code>.
     *
     * @param sourceImp image representation for the source
     * @param targetImp image representation for the target
     * @param source source image model
     * @param target target image model
     * @param sourcePh point handler for the landmarks in the source image
     * @param targetPh point handler for the landmarks in the target image
     * @param sourceMsk source image mask
     * @param targetMsk target image mask
     * @param sourceAffineMatrix source initial affine matrix
     * @param targetAffineMatrix target initial affine matrix
     * @param min_scale_deformation minimum scale deformation
     * @param max_scale_deformation maximum scale deformation
     * @param min_scale_image minimum image scale
     * @param outputLevel flag to specify the level of resolution in the output
     * @param showMarquardtOptim flag to show the optimizer
     * @param divWeight divergence weight
     * @param curlWeight curl weight
     * @param landmarkWeight landmark weight
     * @param imageWeight weight for image similarity
     * @param consistencyWeight weight for the deformations consistency
     * @param stopThreshold stopping threshold
     * @param accurate_mode level of accuracy
     */
    public void setup (
       final ImagePlus sourceImp,
       final ImagePlus targetImp,
       final BSplineModel source,
       final BSplineModel target,
       final PointHandler sourcePh,
       final PointHandler targetPh,
       final Mask sourceMsk,
       final Mask targetMsk,
       final double[][] sourceAffineMatrix,
       final double[][] targetAffineMatrix,
       final int min_scale_deformation,
       final int max_scale_deformation,
       final int min_scale_image,
       final double divWeight,
       final double curlWeight,
       final double landmarkWeight,
       final double imageWeight,
       final double consistencyWeight,
       final double stopThreshold,
       final int outputLevel,
       final boolean showMarquardtOptim,
       final int accurate_mode)
    {
       this.sourceImp             = sourceImp;
       this.targetImp             = targetImp;
       this.source                = source;
       this.target                = target;
       this.sourcePh              = sourcePh;
       this.targetPh              = targetPh;
       this.sourceMsk             = sourceMsk;
       this.targetMsk             = targetMsk;
       this.sourceAffineMatrix    = sourceAffineMatrix;
       this.targetAffineMatrix    = targetAffineMatrix;
       this.min_scale_deformation = min_scale_deformation;
       this.max_scale_deformation = max_scale_deformation;
       this.min_scale_image       = min_scale_image;
       this.divWeight             = divWeight;
       this.curlWeight            = curlWeight;
       this.landmarkWeight        = landmarkWeight;
       this.imageWeight           = imageWeight;
       this.consistencyWeight     = consistencyWeight;
       this.stopThreshold         = stopThreshold;
       this.outputLevel           = outputLevel;
       this.showMarquardtOptim    = showMarquardtOptim;
       this.accurate_mode         = accurate_mode;
    } /* end setup */

    /* ------------------------------------------------------------------------ */
    /**
     *  Initialize output image plus
     *  
     *  @return target and source output image plus
     */
    public ImagePlus[] initializeOutputIPs()
    {
    	int Ydimt = target.getHeight();
        int Xdimt = target.getWidth();
        int Xdims = source.getWidth();
        int Ydims = source.getHeight();
        double[] tImage = target.isSubOutput() ? target.getSubImage() : target.getImage();
        double[] sImage = source.isSubOutput() ? source.getSubImage() : source.getImage(); 
        int sSubFactorX = 1;
        int sSubFactorY = 1;
        int tSubFactorX = 1;
        int tSubFactorY = 1;
        ImagePlus[] outputIP = new ImagePlus[2];
        	
        String extraTitleS = "";
        String extraTitleT = "";

        if(target.isSubOutput() || source.isSubOutput())
        	IJ.log("Initializing output windows...");
        
        // If the output (difference) images are subsampled (because they were
        // larger than the maximum size), update variables.
        if(target.isSubOutput())
        {        	
        	tSubFactorX = Xdimt / target.getSubWidth();
        	tSubFactorY = Ydimt / target.getSubHeight();
        	extraTitleT = " (Subsampled)";
        	Xdimt = target.getSubWidth();
        	Ydimt = target.getSubHeight();        	          	        	       			        	
        }
        
        if(source.isSubOutput())
    	{
    		sSubFactorX = Xdims / source.getSubWidth();
        	sSubFactorY = Ydims / source.getSubHeight();
        	extraTitleS = " (Subsampled)";
    		Xdims = source.getSubWidth();
    		Ydims = source.getSubHeight();
    	} 
        
        // Float processor for the output source-target image.
        final FloatProcessor fp = new FloatProcessor(Xdimt, Ydimt);
        float[] f_array = (float[]) fp.getPixels();               
                        
        for (int i=0; i<Ydimt; i++)
    	{
    		final int i_offset_t = i * Xdimt; 
    		final int i_offset_s = i * Xdims; 
    		final int i_s_sub = i * sSubFactorY;
    		final int i_t_sub = i * tSubFactorY;
    		
    		for (int j=0; j<Xdimt; j++)
    		{
    			    				
    			if (sourceMsk.getValue(j * sSubFactorX, i_s_sub) && targetMsk.getValue(j * tSubFactorX, i_t_sub)
    					&& j < Xdims && i < Ydims)
    				f_array[j + i_offset_t] = (float) (tImage[i_offset_t + j] - sImage[i_offset_s + j]);
    			else
    			{
    				f_array[j + i_offset_t] = 0;
    			}

    		}
    	}
    	fp.resetMinAndMax();

        final ImagePlus ip1 = new ImagePlus("Output Source-Target" + extraTitleS, fp);
        ip1.updateAndDraw();
        ip1.show();
        
        outputIP[0] = ip1;

        // Create output image (target-source) if necessary                        
        
        if(this.accurate_mode != MainDialog.MONO_MODE)
        {
        	final FloatProcessor fp2 = new FloatProcessor(Xdims, Ydims);
        	float[] f_array_2 = (float[]) fp2.getPixels();

        	for (int i=0; i<Ydims; i++)
        	{
        		int i_offset_t = i * Xdimt; 
        		int i_offset_s = i * Xdims; 
        		int i_s_sub = i * sSubFactorY;
        		int i_t_sub = i * tSubFactorY;
        		
        		for (int j=0; j<Xdims; j++)
        			if (targetMsk.getValue(j * tSubFactorX, i_t_sub) && sourceMsk.getValue(j * sSubFactorX, i_s_sub)
        					&& i < Ydimt && j < Xdimt)
        				f_array_2[j + i_offset_s] = (float) (sImage[i_offset_s + j] - tImage[i_offset_t + j]);
        			else 
        				f_array_2[j + i_offset_s] = 0;
        	}
        	fp2.resetMinAndMax();
        	
        	
        	final ImagePlus ip2 = new ImagePlus("Output Target-Source" + extraTitleT, fp2);
        	ip2.updateAndDraw();
        	ip2.show();
        	outputIP[1] = ip2;
        }
        else
        	outputIP[1] = null;
        
        return outputIP;
    }

} /* end FinalAction*/