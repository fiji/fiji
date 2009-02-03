package bunwarpj;

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

import ij.ImagePlus;
import ij.process.FloatProcessor;
/**
 * Class to launch the registration in bUnwarpJ.
 */
public class bUnwarpJFinalAction implements Runnable
{
    /*....................................................................
       Private variables
    ....................................................................*/

    /** thread to run the registration method */
    private Thread t;
    /** dialog for bUnwarpJ interface */
    private bUnwarpJDialog dialog;

    // Images
    /** image representation for the source */
    private ImagePlus                      sourceImp;
    /** image representation for the target */
    private ImagePlus                      targetImp;
    /** source image model */
    private bUnwarpJImageModel   source;
    /** target image model */
    private bUnwarpJImageModel   target;

    // Landmarks
    /** point handler for the landmarks in the source image*/
    private bUnwarpJPointHandler sourcePh;
    /** point handler for the landmarks in the target image*/
    private bUnwarpJPointHandler targetPh;

    // Masks for the images
    /** source image mask */
    private bUnwarpJMask sourceMsk;
    /** target image mask */
    private bUnwarpJMask targetMsk;

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
     * within <code>bUnwarpJDialog</code> do not because they are
     * under the control of its own event loop.
     */
    public bUnwarpJFinalAction (final bUnwarpJDialog dialog)
    {
       this.dialog = dialog;
       t = new Thread(this);
       t.setDaemon(true);
    }    
    
    /**
     * Get the thread.
     *
     * @return the thread associated with this <code>bUnwarpJFinalAction</code>
     *         object
     */
    public Thread getThread ()
    {
       return(t);
    } /* end getThread */

    /**
     * Perform the registration
     */
    public void run ()
    {
        // Create output image (source-target)
        int Ydimt = target.getHeight();
        int Xdimt = target.getWidth();
        int Xdims = source.getWidth();

        final FloatProcessor fp = new FloatProcessor(Xdimt, Ydimt);

        for (int i=0; i<Ydimt; i++)
        	for (int j=0; j<Xdimt; j++)
        		if (sourceMsk.getValue(j, i) && targetMsk.getValue(j, i))
        			fp.putPixelValue(j, i, (target.getImage())[i*Xdimt+j]-
        					(source.getImage())[i*Xdims+j]);
        		else
        		{
        			fp.putPixelValue(j, i, 0);
        		}
        fp.resetMinAndMax();
        final ImagePlus      ip1 = new ImagePlus("Output Source-Target", fp);
        ip1.updateImage();
        ip1.show();

        // Create output image (target-source)
        int Ydims = source.getHeight();

        final FloatProcessor fp2 = new FloatProcessor(Xdims, Ydims);

        for (int i=0; i<Ydims; i++)
           for (int j=0; j<Xdims; j++)
               if (targetMsk.getValue(j, i) && sourceMsk.getValue(j, i))
                  fp2.putPixelValue(j, i, (source.getImage())[i*Xdims+j]-
                                          (target.getImage())[i*Xdimt+j]);
               else fp2.putPixelValue(j, i, 0);
        fp2.resetMinAndMax();
        final ImagePlus      ip2 = new ImagePlus("Output Target-Source", fp2);
        ip2.updateImage();
        ip2.show();

        // Perform the registration
        final bUnwarpJTransformation warp = new bUnwarpJTransformation(
          sourceImp, targetImp, source, target, sourcePh, targetPh,
          sourceMsk, targetMsk, sourceAffineMatrix, targetAffineMatrix,
          min_scale_deformation, max_scale_deformation,
          min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
          consistencyWeight, stopThreshold, outputLevel, showMarquardtOptim, accurate_mode,
          dialog.isSaveTransformationSet(), "", "", ip1, ip2, dialog);

        warp.doRegistration();

        dialog.restoreAll();
        dialog.freeMemory();
    }

    /**
     * Pass parameter from <code>bUnwarpJDialog</code> to
     * <code>bUnwarpJFinalAction</code>.
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
       final bUnwarpJImageModel source,
       final bUnwarpJImageModel target,
       final bUnwarpJPointHandler sourcePh,
       final bUnwarpJPointHandler targetPh,
       final bUnwarpJMask sourceMsk,
       final bUnwarpJMask targetMsk,
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


} /* end bUnwarpJFinalAction*/