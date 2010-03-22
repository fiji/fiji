package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ(C) and Fiji.
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

/*====================================================================
|   Transformation
\===================================================================*/
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.SaveDialog;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.Arrays;
import java.util.Vector;


/**
 * Class to perform the transformation for bUnwarpJ:
 * <p>
 * <ul>
 * 	<li>It stores the information about the bidirectional or unidirectional registration.</li>
 * 	<li>It produces the registration results and the corresponding output if called from dialog.</li>
 * 	<li>The main registration methods are <code>doBidirectionalRegistration</code> (bidirectional) and <code>doUnidirectionalRegistration</code> (unidirectional).</li>
 * 	<li>The intermediate states of the registration are displayed in separate windows (depending on the value of <code>outputLevel</code>.</li>
 * </ul>
 */
public class Transformation
{ /* begin class Transformation */

	/*....................................................................
       Private variables
    ....................................................................*/

	/** float epsilon */
	private final double FLT_EPSILON = (double)Float.intBitsToFloat((int)0x33FFFFFF);
	/** pyramid flag to indicate the image information is taken from the pyramid */
	private final boolean PYRAMID  = true;
	/** original flag to indicate the image information is taken from the original image */
	private final boolean ORIGINAL = false;
	/** degree of the B-splines involved in the transformation */
	private final int transformationSplineDegree = 3;

	// Some useful references
	/** reference to the first output image */
	private ImagePlus output_ip_1;
	/** reference to the second output image */
	private ImagePlus output_ip_2;
	/** pointer to the dialog of the bUnwarpJ interface */
	private MainDialog dialog;

	// Images
	/** pointer to the source image representation */
	private ImagePlus sourceImp;
	/** pointer to the target image representation */
	private ImagePlus targetImp;
	/** pointer to the source image model */
	private BSplineModel source;
	/** pointer to the target image model */
	private BSplineModel target;

	// Original image processors
	/** initial source image processor */
	private ImageProcessor originalSourceIP;
	/** initial target image processor */
	private ImageProcessor originalTargetIP;

	// Landmarks
	/** pointer to the source point handler */
	private PointHandler sourcePh;
	/** pointer to the target point handler */
	private PointHandler targetPh;

	// Masks for the images
	/** pointer to the source mask */
	private Mask sourceMsk;
	/** pointer to the target mask */
	private Mask targetMsk;

	// Initial Affine Matrices
	/** initial affine matrix for the source image */
	private double[][] sourceAffineMatrix = null;
	/** initial affine matrix for the target image */
	private double[][] targetAffineMatrix = null;
	
	// Initial affine matrix pre-process
	/** percentage of shear correction in initial matrix */
	private double tweakShear = 0.0;
	/** percentage of scale correction in initial matrix */
	private double tweakScale = 0.0;
	/** percentage of anisotropy correction in initial matrix */
	private double tweakIso = 0.0;

	// Image size
	/** source image height */
	private int     sourceHeight;
	/** source image width */
	private int     sourceWidth;
	/** target image height */
	private int     targetHeight;
	/** source image width */
	private int     targetWidth;
	/** target image current height */
	private int     targetCurrentHeight;
	/** target image current width */
	private int     targetCurrentWidth;
	/** source image current height */
	private int     sourceCurrentHeight;
	/** source image current width */
	private int     sourceCurrentWidth;
	/** height factor in the target image*/
	private double  targetFactorHeight;
	/** width factor in the target image*/
	private double  targetFactorWidth;
	/** height factor in the source image*/
	private double  sourceFactorHeight;
	/** width factor in the source image*/
	private double  sourceFactorWidth;

	// Display variables
	/** direct similarity error for the current iteration */
	private double partialDirectSimilarityError;
	/** direct regularization error for the current iteration */
	private double partialDirectRegularizationError;
	/** direct landmarks error for the current iteration */
	private double partialDirectLandmarkError;
	/** direct consistency error for the current iteration */
	private double partialDirectConsitencyError;
	
	/** direct similarity error at the end of the registration */
	private double finalDirectSimilarityError;
	/** direct regularization error at the end of the registration */
	private double finalDirectRegularizationError;
	/** direct landmarks error at the end of the registration */
	private double finalDirectLandmarkError;
	/** direct consistency error at the end of the registration */
	private double finalDirectConsistencyError;
	
	/** inverse similarity error for the current iteration */
	private double partialInverseSimilarityError;
	/** inverse regularization error for the current iteration */
	private double partialInverseRegularizationError;
	/** inverse landmarks error for the current iteration */
	private double partialInverseLandmarkError;
	/** inverse consistency error for the current iteration */
	private double partialInverseConsitencyError;
	
	/** inverse similarity error at the end of the registration */
	private double finalInverseSimilarityError;
	/** inverse regularization error at the end of the registration */
	private double finalInverseRegularizationError;
	/** inverse landmarks error at the end of the registration */
	private double finalInverseLandmarkError;
	/** inverse consistency error at the end of the registration */
	private double finalInverseConsistencyError;
	

	// Transformation parameters
	/** minimum scale deformation */
	private int     min_scale_deformation;
	/** maximum scale deformation */
	private int     max_scale_deformation;
	/** minimum scale image */
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
	/** stopping threshold  */
	private double  stopThreshold;
	/** level of accuracy */
	private int     accurate_mode;
	/** direct transformation file name */
	private String  fn_tnf_1;
	/** inverse transformation file name */
	private String  fn_tnf_2;

	// Transformation estimate
	/** number of intervals to place B-spline coefficients */
	private int     intervals;
	/** x- B-spline coefficients keeping the transformation from source to target */
	private double  [][]cxSourceToTarget = null;
	/** y- B-spline coefficients keeping the transformation from source to target */
	private double  [][]cySourceToTarget = null;
	/** x- B-spline coefficients keeping the transformation from target to source */
	private double  [][]cxTargetToSource = null;
	/** y- B-spline coefficients keeping the transformation from target to source */
	private double  [][]cyTargetToSource = null;

	/** image model to interpolate the cxSourceToTarget coefficients */
	private BSplineModel swxSourceToTarget = null;
	/** image model to interpolate the cySourceToTarget coefficients */
	private BSplineModel swySourceToTarget = null;
	/** image model to interpolate the cxTargetToSource coefficients */
	private BSplineModel swxTargetToSource = null;
	/** image model to interpolate the cyTargetToSource coefficients */
	private BSplineModel swyTargetToSource = null;

	// Regularization temporary variables
	/** regularization P11 (source to target) matrix */
	private double  [][]P11_SourceToTarget;
	/** regularization P22 (source to target) matrix */
	private double  [][]P22_SourceToTarget;
	/** regularization P12 (source to target) matrix */
	private double  [][]P12_SourceToTarget;

	/** regularization P11 (target to source) matrix */
	private double  [][]P11_TargetToSource;
	/** regularization P22 (target to source) matrix */
	private double  [][]P22_TargetToSource;
	/** regularization P12 (target to source) matrix */
	private double  [][]P12_TargetToSource;

	/*....................................................................
       Public methods
    ....................................................................*/

	//------------------------------------------------------------------
	/**
	 * Create an instance of Transformation.
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
	 * @param targetAffineMatrix source initial affine matrix
	 * @param min_scale_deformation minimum scale deformation
	 * @param max_scale_deformation maximum scale deformation
	 * @param min_scale_image minimum image scale
	 * @param divWeight divergence weight
	 * @param curlWeight curl weight
	 * @param landmarkWeight landmark weight
	 * @param imageWeight weight for image similarity
	 * @param consistencyWeight weight for the deformations consistency
	 * @param stopThreshold stopping threshold
	 * @param outputLevel flag to specify the level of resolution in the output
	 * @param showMarquardtOptim flag to show the optimizer
	 * @param accurate_mode level of accuracy
	 * @param fn_tnf_1 direct transformation file name
	 * @param fn_tnf_2 inverse transformation file name
	 * @param output_ip_1 pointer to the first output image
	 * @param output_ip_2 pointer to the second output image
	 * @param dialog pointer to the dialog of the bUnwarpJ interface
	 */
	public Transformation (
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
			final int accurate_mode,
			final String fn_tnf_1,
			final String fn_tnf_2,
			final ImagePlus output_ip_1,
			final ImagePlus output_ip_2,
			final MainDialog dialog)
	{
		this.sourceImp	      = sourceImp;
		this.targetImp	      = targetImp;
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
		this.accurate_mode         		= accurate_mode;
		this.fn_tnf_1              		= fn_tnf_1;
		this.fn_tnf_2              		= fn_tnf_2;
		this.output_ip_1           		= output_ip_1;
		this.output_ip_2           		= output_ip_2;
		this.dialog                		= dialog;

		this.originalSourceIP	  = this.dialog.getOriginalSourceIP();
		this.originalTargetIP	  = this.dialog.getOriginalTargetIP();

		this.sourceWidth           = source.getWidth();
		this.sourceHeight          = source.getHeight();
		this.targetWidth           = target.getWidth();
		this.targetHeight          = target.getHeight();
	} /* end Transformation */

	
	//------------------------------------------------------------------
	/**
	 * Create an instance of Transformation.
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
	 * @param targetAffineMatrix source initial affine matrix
	 * @param min_scale_deformation minimum scale deformation
	 * @param max_scale_deformation maximum scale deformation
	 * @param min_scale_image minimum image scale
	 * @param divWeight divergence weight
	 * @param curlWeight curl weight
	 * @param landmarkWeight landmark weight
	 * @param imageWeight weight for image similarity
	 * @param consistencyWeight weight for the deformations consistency
	 * @param stopThreshold stopping threshold
	 * @param outputLevel flag to specify the level of resolution in the output
	 * @param showMarquardtOptim flag to show the optimizer
	 * @param accurate_mode level of accuracy
	 * @param fn_tnf_1 direct transformation file name
	 * @param fn_tnf_2 inverse transformation file name
	 * @param output_ip_1 pointer to the first output image
	 * @param output_ip_2 pointer to the second output image
	 * @param dialog pointer to the dialog of the bUnwarpJ interface
	 */
	public Transformation (
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
			final int accurate_mode,
			final String fn_tnf_1,
			final String fn_tnf_2,
			final ImagePlus output_ip_1,
			final ImagePlus output_ip_2,
			final MainDialog dialog,
			final ImageProcessor originalSourceIP,
			final ImageProcessor originalTargetIP)
	{
		this.sourceImp	      = sourceImp;
		this.targetImp	      = targetImp;
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
		this.accurate_mode         		= accurate_mode;
		this.fn_tnf_1              		= fn_tnf_1;
		this.fn_tnf_2              		= fn_tnf_2;
		this.output_ip_1           		= output_ip_1;
		this.output_ip_2           		= output_ip_2;
		this.dialog                		= dialog;

		this.originalSourceIP	  = originalSourceIP;
		this.originalTargetIP	  = originalTargetIP;

		this.sourceWidth           = source.getWidth();
		this.sourceHeight          = source.getHeight();
		this.targetWidth           = target.getWidth();
		this.targetHeight          = target.getHeight();
	} // end Transformation
	
	//------------------------------------------------------------------
	/**
	 * Registration method. It applies the consistent and elastic registration
	 * algorithm to the selected source and target images.
	 */
	public void doBidirectionalRegistration ()
	{	
		// This function can only be applied with splines of an odd order

		// Bring into consideration the image/coefficients at the smallest scale
		source.popFromPyramid();
		target.popFromPyramid();

		// size correction factor
		int sizeCorrectionFactor = 0; // this.targetHeight / (1024  * (int) Math.pow(2, this.maxImageSubsamplingFactor));
		//System.out.println("Size correction factor = " + sizeCorrectionFactor);
		
		targetCurrentHeight = target.getCurrentHeight();
		targetCurrentWidth  = target.getCurrentWidth();

		targetFactorHeight  = target.getFactorHeight();
		targetFactorWidth   = target.getFactorWidth();

		sourceCurrentHeight = source.getCurrentHeight();
		sourceCurrentWidth  = source.getCurrentWidth();

		sourceFactorHeight  = source.getFactorHeight();
		sourceFactorWidth   = source.getFactorWidth();

		// Ask memory for the transformation coefficients
		intervals = (int)Math.pow(2, min_scale_deformation + sizeCorrectionFactor);
		
		cxTargetToSource = new double[intervals+3][intervals+3];
		cyTargetToSource = new double[intervals+3][intervals+3];

		// Build matrices for computing the regularization
		buildRegularizationTemporary(intervals, false);
		buildRegularizationTemporary(intervals, true);

		// Ask for memory for the residues
		final int K;
		if (targetPh!=null) K = targetPh.getPoints().size();
		else                K = 0;
		double [] dxTargetToSource = new double[K];
		double [] dyTargetToSource = new double[K];
		computeInitialResidues(dxTargetToSource,dyTargetToSource, false);
		computeInitialResidues(dxTargetToSource,dyTargetToSource, false);

		// Compute the affine transformation FROM THE TARGET TO THE SOURCE coordinates
		// Notice that this matrix is independent of the scale (unless it was loaded from
		// file), but the residues are not
		double[][] affineMatrix = null;
		if(this.sourceAffineMatrix != null)
		{
			affineMatrix = this.sourceAffineMatrix;
			// Scale translations in the matrix.
			affineMatrix[0][2] *= this.sourceFactorWidth;
			affineMatrix[1][2] *= this.sourceFactorHeight;
		}
		else
		{
			// NOTE: after version 1.1 the landmarks are always used to calculate
			// an initial affine transformation (whether the landmarks weight is 0 or not).
			affineMatrix = computeAffineMatrix(false);
		}

		// Incorporate the affine transformation into the spline coefficient
		for (int i= 0; i<intervals + 3; i++)
		{
			final double v = (double)((i - 1) * (targetCurrentHeight - 1)) / (double)intervals;
			final double xv = affineMatrix[0][2] + affineMatrix[0][1] * v;
			final double yv = affineMatrix[1][2] + affineMatrix[1][1] * v;
			for (int j = 0; j < intervals + 3; j++)
			{
				final double u = (double)((j - 1) * (targetCurrentWidth - 1)) / (double)intervals;
				cxTargetToSource[i][j] = xv + affineMatrix[0][0] * u;
				cyTargetToSource[i][j] = yv + affineMatrix[1][0] * u;
			}
		}

	

		// Compute the affine transformation FROM THE SOURCE TO THE TARGET coordinates
		// Notice again that this matrix is independent of the scale, but the residues are not
		// Ask for memory for the residues
		final int K2;
		if (sourcePh!=null) K2 = sourcePh.getPoints().size();
		else                K2 = 0;
		double [] dxSourceToTarget = new double[K2];
		double [] dySourceToTarget = new double[K2];
		computeInitialResidues(dxSourceToTarget,dySourceToTarget, true);
		computeInitialResidues(dxSourceToTarget,dySourceToTarget, true);

		cxSourceToTarget = new double[intervals+3][intervals+3];
		cySourceToTarget = new double[intervals+3][intervals+3];

		if(this.targetAffineMatrix != null)
		{
			affineMatrix = this.targetAffineMatrix;
			// Scale translations in the matrix.
			affineMatrix[0][2] *= this.targetFactorWidth;
			affineMatrix[1][2] *= this.targetFactorHeight;
		}
		else
		{
			// NOTE: after version 1.1 the landmarks are always used to calculate
			// an initial affine transformation (whether the landmarks weight is 0 or not).			
			affineMatrix = computeAffineMatrix(true);
		}

		// Incorporate the affine transformation into the spline coefficient
		for (int i= 0; i<intervals + 3; i++)
		{
			final double v = (double)((i - 1) * (sourceCurrentHeight - 1)) / (double)intervals;
			final double xv = affineMatrix[0][2] + affineMatrix[0][1] * v;
			final double yv = affineMatrix[1][2] + affineMatrix[1][1] * v;
			for (int j = 0; j < intervals + 3; j++)
			{
				final double u = (double)((j - 1) * (sourceCurrentWidth - 1)) / (double)intervals;
				cxSourceToTarget[i][j] = xv + affineMatrix[0][0] * u;
				cySourceToTarget[i][j] = yv + affineMatrix[1][0] * u;
			}
		}

		// Now refine with the different scales
		int state;   // state=-1 --> Finish
		// state= 0 --> Increase deformation detail
		// state= 1 --> Increase image detail
		// state= 2 --> Do nothing until the finest image scale
		if (min_scale_deformation==max_scale_deformation) state=1;
		else                                              state=0;
		int s = min_scale_deformation;
		int step = 0;
		computeTotalWorkload();

		while (state != -1)
		{
			int currentDepth = target.getCurrentDepth();

			// Update the deformation coefficients only in states 0 and 1
			if (state==0 || state==1)
			{
				// Update the deformation coefficients with the error of the landmarks
				// The following conditional is now useless but it is there to allow
				// easy changes like applying the landmarks only in the coarsest deformation
				if (s>=min_scale_deformation)
				{
					// Number of intervals at this scale and ask for memory
					intervals = (int) Math.pow(2, s + sizeCorrectionFactor);
					final double[][] newcxTargetToSource = new double[intervals+3][intervals+3];
					final double[][] newcyTargetToSource = new double[intervals+3][intervals+3];

					final double[][] newcxSourceToTarget = new double[intervals+3][intervals+3];
					final double[][] newcySourceToTarget = new double[intervals+3][intervals+3];

					// Compute the coefficients at this scale
					boolean underconstrained = true;
					// FROM TARGET TO SOURCE.
					if (divWeight==0 && curlWeight==0)
						underconstrained=
							computeCoefficientsScale(intervals, dxTargetToSource, dyTargetToSource, newcxTargetToSource, newcyTargetToSource, false);
					else
						underconstrained=
							computeCoefficientsScaleWithRegularization(
									intervals, dxTargetToSource, dyTargetToSource, newcxTargetToSource, newcyTargetToSource, false);

					// Incorporate information from the previous scale
					if (!underconstrained || (step==0 && landmarkWeight!=0))
					{
						for (int i=0; i < intervals+3; i++)
							for (int j=0; j < intervals+3; j++) {
								cxTargetToSource[i][j] += newcxTargetToSource[i][j];
								cyTargetToSource[i][j] += newcyTargetToSource[i][j];
							}
					}

					// FROM SOURCE TO TARGET.
					underconstrained = true;
					if (divWeight==0 && curlWeight==0)
						underconstrained=
							computeCoefficientsScale(intervals, dxSourceToTarget, dySourceToTarget, newcxSourceToTarget, newcySourceToTarget, true);
					else
						underconstrained=
							computeCoefficientsScaleWithRegularization(
									intervals, dxSourceToTarget, dySourceToTarget, newcxSourceToTarget, newcySourceToTarget, true);

					// Incorporate information from the previous scale
					if (!underconstrained || (step==0 && landmarkWeight!=0))
					{
						for (int i=0; i < intervals+3; i++)
							for (int j=0; j < intervals+3; j++)
							{
								cxSourceToTarget[i][j]+= newcxSourceToTarget[i][j];
								cySourceToTarget[i][j]+= newcySourceToTarget[i][j];
							}
					}
				}

				// Optimize deformation coefficients
				//if (imageWeight!=0)
				optimizeCoeffs(intervals, stopThreshold, cxTargetToSource, cyTargetToSource, cxSourceToTarget, cySourceToTarget);
			}

			// Prepare for next iteration
			step++;
			switch (state)
			{
			case 0:
				// Finer details in the deformation
				if (s < max_scale_deformation)
				{
					cxTargetToSource = propagateCoeffsToNextLevel(intervals, cxTargetToSource, 1);
					cyTargetToSource = propagateCoeffsToNextLevel(intervals, cyTargetToSource, 1);
					cxSourceToTarget = propagateCoeffsToNextLevel(intervals, cxSourceToTarget, 1);
					cySourceToTarget = propagateCoeffsToNextLevel(intervals, cySourceToTarget, 1);
					s++;
					intervals*=2;

					// Prepare matrices for the regularization term
					buildRegularizationTemporary(intervals, false);
					buildRegularizationTemporary(intervals, true);

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
				if (currentDepth!=0)
				{
					double oldTargetCurrentHeight = targetCurrentHeight;
					double oldTargetCurrentWidth  = targetCurrentWidth;
					double oldSourceCurrentHeight = sourceCurrentHeight;
					double oldSourceCurrentWidth  = sourceCurrentWidth;

					source.popFromPyramid();
					target.popFromPyramid();

					targetCurrentHeight = target.getCurrentHeight();
					targetCurrentWidth  = target.getCurrentWidth();
					targetFactorHeight = target.getFactorHeight();
					targetFactorWidth  = target.getFactorWidth();

					sourceCurrentHeight = source.getCurrentHeight();
					sourceCurrentWidth  = source.getCurrentWidth();
					sourceFactorHeight = source.getFactorHeight();
					sourceFactorWidth  = source.getFactorWidth();

					// Adapt the transformation to the new image size
					double targetFactorY = (targetCurrentHeight-1) / (oldTargetCurrentHeight-1);
					double targetFactorX = (targetCurrentWidth -1) / (oldTargetCurrentWidth -1);
					double sourceFactorY = (sourceCurrentHeight-1) / (oldSourceCurrentHeight-1);
					double sourceFactorX = (sourceCurrentWidth -1) / (oldSourceCurrentWidth -1);

					for (int i=0; i<intervals+3; i++)
						for (int j=0; j<intervals+3; j++)
						{
							cxTargetToSource[i][j] *= targetFactorX;
							cyTargetToSource[i][j] *= targetFactorY;
							cxSourceToTarget[i][j] *= sourceFactorX;
							cySourceToTarget[i][j] *= sourceFactorY;
						}

					// Prepare matrices for the regularization term
					buildRegularizationTemporary(intervals, false);
					buildRegularizationTemporary(intervals, true);
				}
				break;
			}

			// In accurate_mode reduce the stopping threshold for the last iteration
			if ((state==0 || state==1) && s==max_scale_deformation &&
					currentDepth==min_scale_image+1 && accurate_mode==1)
				stopThreshold /= 10;

		}// end while (state != -1).

		// Adapt coefficients if necessary
		if(source.getOriginalImageWidth() > this.sourceCurrentWidth)
		{
			if(source.isSubOutput() || target.isSubOutput())
				IJ.log("Adapting coefficients from " + this.sourceCurrentWidth + " to " + source.getOriginalImageWidth() +"...");
			// Adapt the transformation to the new image size
			double targetFactorY = (target.getOriginalImageHeight() - 1) / (targetCurrentHeight-1);
			double targetFactorX = (target.getOriginalImageWidth()  - 1) / (targetCurrentWidth -1);
			double sourceFactorY = (source.getOriginalImageHeight() - 1) / (sourceCurrentHeight-1);
			double sourceFactorX = (source.getOriginalImageWidth()  - 1) / (sourceCurrentWidth -1);

			for (int i=0; i<intervals+3; i++)
				for (int j=0; j<intervals+3; j++)
				{
					cxTargetToSource[i][j] *= targetFactorX;
					cyTargetToSource[i][j] *= targetFactorY;
					cxSourceToTarget[i][j] *= sourceFactorX;
					cySourceToTarget[i][j] *= sourceFactorY;
				}
			this.targetCurrentHeight = target.getOriginalImageHeight();
			this.targetCurrentWidth  = target.getOriginalImageWidth();
			this.sourceCurrentHeight = source.getOriginalImageHeight();
			this.sourceCurrentWidth  = source.getOriginalImageWidth();
		}

		// Display final errors.		
		if(this.outputLevel == 2)
		{
			if(this.imageWeight != 0)
			{
				IJ.write(" Optimal direct similarity error = " + this.finalDirectSimilarityError);
				IJ.write(" Optimal inverse similarity error = " + this.finalInverseSimilarityError);
			}
			if(this.curlWeight != 0 || this.divWeight != 0)
			{
				IJ.write(" Optimal direct regularization error = " + this.finalDirectRegularizationError);
				IJ.write(" Optimal inverse regularization error = " + this.finalInverseRegularizationError);
			}
			if(this.landmarkWeight != 0)
			{
				IJ.write(" Optimal direct landmark error = " + this.finalDirectLandmarkError);
				IJ.write(" Optimal inverse landmark error = " + this.finalInverseLandmarkError);
			}
			if(this.consistencyWeight != 0)
			{
				IJ.write(" Optimal direct consistency error = " + this.finalDirectConsistencyError);
				IJ.write(" Optimal inverse consistency error = " + this.finalInverseConsistencyError);
			}
		}
		
	} // end doBidirectionalRegistration

	//------------------------------------------------------------------
	/**
	 * Unidirectional registration method. It applies unidirectional
	 * elastic registration to the selected source and target images.
	 */
	public void doUnidirectionalRegistration ()
	{		
		// This function can only be applied with splines of an odd order

		// Bring into consideration the image/coefficients at the smallest scale
		source.popFromPyramid();
		target.popFromPyramid();

		targetCurrentHeight = target.getCurrentHeight();
		targetCurrentWidth  = target.getCurrentWidth();

		targetFactorHeight  = target.getFactorHeight();
		targetFactorWidth   = target.getFactorWidth();

		sourceCurrentHeight = source.getCurrentHeight();
		sourceCurrentWidth  = source.getCurrentWidth();

		sourceFactorHeight  = source.getFactorHeight();
		sourceFactorWidth   = source.getFactorWidth();						

		// size correction factor
		int sizeCorrectionFactor = 0; //this.targetHeight / (1024  * (int) Math.pow(2, this.maxImageSubsamplingFactor));
		//System.out.println("Size correction factor = " + sizeCorrectionFactor);
		
		// Ask memory for the transformation coefficients
		intervals = (int)Math.pow(2, min_scale_deformation + sizeCorrectionFactor);
		
		cxTargetToSource = new double[intervals+3][intervals+3];
		cyTargetToSource = new double[intervals+3][intervals+3];

		// Build matrices for computing the regularization
		buildRegularizationTemporary(intervals, false);

		// Ask for memory for the residues
		final int K;
		if (targetPh!=null) K = targetPh.getPoints().size();
		else                K = 0;
		double [] dxTargetToSource = new double[K];
		double [] dyTargetToSource = new double[K];
		computeInitialResidues(dxTargetToSource,dyTargetToSource, false);

		// Compute the affine transformation FROM THE TARGET TO THE SOURCE coordinates
		// Notice that this matrix is independent of the scale (unless it was loaded from
		// file), but the residues are not
		double[][] affineMatrix = null;
		if(this.sourceAffineMatrix != null)
		{
			affineMatrix = this.sourceAffineMatrix;
			// Scale translations in the matrix.
			affineMatrix[0][2] *= this.sourceFactorWidth;
			affineMatrix[1][2] *= this.sourceFactorHeight;
		}
		else
		{
			// NOTE: after version 1.1 the landmarks are always used to calculate
			// an initial affine transformation (whether the landmarks weight is 0 or not).
			
			affineMatrix = computeAffineMatrix(false);			
		}

		//MiscTools.printMatrix("source affine matrix", affineMatrix);
		
		
		// Incorporate the affine transformation into the spline coefficient
		for (int i= 0; i<intervals + 3; i++)
		{
			final double v = (double)((i - 1) * (targetCurrentHeight - 1)) / (double)intervals;
			final double xv = affineMatrix[0][2] + affineMatrix[0][1] * v;
			final double yv = affineMatrix[1][2] + affineMatrix[1][1] * v;
			for (int j = 0; j < intervals + 3; j++)
			{
				final double u = (double)((j - 1) * (targetCurrentWidth - 1)) / (double)intervals;
				cxTargetToSource[i][j] = xv + affineMatrix[0][0] * u;
				cyTargetToSource[i][j] = yv + affineMatrix[1][0] * u;
			}
		}		
		
		// Now refine with the different scales
		int state;   // state=-1 --> Finish
		// state= 0 --> Increase deformation detail
		// state= 1 --> Increase image detail
		// state= 2 --> Do nothing until the finest image scale
		if (min_scale_deformation==max_scale_deformation) state=1;
		else                                              state=0;
		int s = min_scale_deformation;
		int step = 0;
		computeTotalWorkload();

		while (state != -1)
		{
			int currentDepth = target.getCurrentDepth();

			// Update the deformation coefficients only in states 0 and 1
			if (state==0 || state==1)
			{
				// Update the deformation coefficients with the error of the landmarks
				// The following conditional is now useless but it is there to allow
				// easy changes like applying the landmarks only in the coarsest deformation
				if (s>=min_scale_deformation)
				{
					// Number of intervals at this scale and ask for memory
					intervals = (int) Math.pow(2, s + sizeCorrectionFactor);
					final double[][] newcxTargetToSource = new double[intervals+3][intervals+3];
					final double[][] newcyTargetToSource = new double[intervals+3][intervals+3];
					
					// Compute the coefficients at this scale
					boolean underconstrained = true;
					// FROM TARGET TO SOURCE.
					if (divWeight==0 && curlWeight==0)
						underconstrained=
							computeCoefficientsScale(intervals, dxTargetToSource, dyTargetToSource, newcxTargetToSource, newcyTargetToSource, false);
					else
						underconstrained=
							computeCoefficientsScaleWithRegularization(
									intervals, dxTargetToSource, dyTargetToSource, newcxTargetToSource, newcyTargetToSource, false);

					// Incorporate information from the previous scale
					if (!underconstrained || (step==0 && landmarkWeight!=0))
					{
						for (int i=0; i<intervals+3; i++)
							for (int j=0; j<intervals+3; j++) {
								cxTargetToSource[i][j]+=newcxTargetToSource[i][j];
								cyTargetToSource[i][j]+=newcyTargetToSource[i][j];
							}
					}

				}
						
				// Optimize deformation coefficients
				optimizeCoeffs(intervals, stopThreshold, cxTargetToSource, cyTargetToSource);
				
				
				
			}

			// Prepare for next iteration
			step++;
			switch (state)
			{
			case 0:
				// Finer details in the deformation
				if (s<max_scale_deformation)
				{
					cxTargetToSource = propagateCoeffsToNextLevel(intervals, cxTargetToSource, 1);
					cyTargetToSource = propagateCoeffsToNextLevel(intervals, cyTargetToSource, 1);
					s++;
					intervals*=2;

					// Prepare matrices for the regularization term
					buildRegularizationTemporary(intervals, false);

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
				if (currentDepth!=0)
				{
					double oldTargetCurrentHeight = targetCurrentHeight;
					double oldTargetCurrentWidth  = targetCurrentWidth;

					source.popFromPyramid();
					target.popFromPyramid();

					targetCurrentHeight = target.getCurrentHeight();
					targetCurrentWidth  = target.getCurrentWidth();
					targetFactorHeight = target.getFactorHeight();
					targetFactorWidth  = target.getFactorWidth();

					sourceCurrentHeight = source.getCurrentHeight();
					sourceCurrentWidth  = source.getCurrentWidth();
					sourceFactorHeight = source.getFactorHeight();
					sourceFactorWidth  = source.getFactorWidth();

					// Adapt the transformation to the new image size
					double targetFactorY = (targetCurrentHeight-1) / (oldTargetCurrentHeight-1);
					double targetFactorX = (targetCurrentWidth -1) / (oldTargetCurrentWidth -1);

					for (int i=0; i<intervals+3; i++)
						for (int j=0; j<intervals+3; j++)
						{
							cxTargetToSource[i][j] *= targetFactorX;
							cyTargetToSource[i][j] *= targetFactorY;
						}

					// Prepare matrices for the regularization term
					buildRegularizationTemporary(intervals, false);
				}
				break;
			}

			// In accurate_mode reduce the stopping threshold for the last iteration
			if ((state==0 || state==1) && s==max_scale_deformation &&
					currentDepth==min_scale_image+1 && accurate_mode==1)
				stopThreshold /= 10;

		}// end while (state != -1).
		
		// Adapt coefficients if necessary
		if(source.getOriginalImageWidth() > this.targetCurrentWidth)
		{
			if(source.isSubOutput() || target.isSubOutput())
				IJ.log("Adapting coefficients from " + this.sourceCurrentWidth + " to " + this.originalSourceIP.getWidth() + "...");
			// Adapt the transformation to the new image size
			double targetFactorY = (target.getOriginalImageHeight() - 1) / (targetCurrentHeight-1);
			double targetFactorX = (target.getOriginalImageWidth()  - 1) / (targetCurrentWidth -1);

			for (int i=0; i<intervals+3; i++)
				for (int j=0; j<intervals+3; j++)
				{
					cxTargetToSource[i][j] *= targetFactorX;
					cyTargetToSource[i][j] *= targetFactorY;					
				}
			this.targetCurrentHeight = target.getOriginalImageHeight();
			this.targetCurrentWidth  = target.getOriginalImageWidth();
			this.sourceCurrentHeight = source.getOriginalImageHeight();
			this.sourceCurrentWidth  = source.getOriginalImageWidth();
		}

		// Display final errors.		
		if(this.outputLevel == 2)
		{
			if(this.imageWeight != 0)
			{
				IJ.write(" Optimal direct similarity error = " + this.finalDirectSimilarityError);
			}
			if(this.curlWeight != 0 || this.divWeight != 0)
			{
				IJ.write(" Optimal direct regularization error = " + this.finalDirectRegularizationError);
			}
			if(this.landmarkWeight != 0)
			{
				IJ.write(" Optimal direct landmark error = " + this.finalDirectLandmarkError);
			}
			if(this.consistencyWeight != 0)
			{
				IJ.write(" Optimal direct consistency error = " + this.finalDirectConsistencyError);
			}
		}
		
	} /* end doUnidirectionalRegistration */
	

	/*--------------------------------------------------------------------------*/
	/**
	 * Evaluate the similarity between the images.
	 *
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 * @return image similarity value
	 */
	public double evaluateImageSimilarity(boolean bIsReverse)
	{
		int int3 = intervals+3;
		int halfM = int3*int3;
		int M = halfM*2;

		double   []x            = new double   [M];
		double   []grad         = new double   [M];

		// Variables to allow registering in both directions.
		BSplineModel auxTarget = target;
		BSplineModel swx = swxTargetToSource;
		BSplineModel swy = swyTargetToSource;
		double [][]cx = cxTargetToSource;
		double [][]cy = cyTargetToSource;

		if(bIsReverse)
		{
			auxTarget = source;
			swx = swxSourceToTarget;
			swy = swySourceToTarget;
			cx = cxSourceToTarget;
			cy = cySourceToTarget;
		}

		for (int i= 0, p=0; i<intervals + 3; i++)
			for (int j = 0; j < intervals + 3; j++, p++)
			{
				x[      p] = cx[i][j];
				x[halfM+p] = cy[i][j];
			}

		if (swx==null)
		{
			swx = new BSplineModel(cx);
			swy = new BSplineModel(cy);
			swx.precomputed_prepareForInterpolation(
					auxTarget.getCurrentHeight(), auxTarget.getCurrentWidth(), intervals);
			swy.precomputed_prepareForInterpolation(
					auxTarget.getCurrentHeight(), auxTarget.getCurrentWidth(), intervals);
		}

		if (swx.precomputed_getWidth() != auxTarget.getCurrentWidth())
		{
			swx.precomputed_prepareForInterpolation(
					auxTarget.getCurrentHeight(), auxTarget.getCurrentWidth(), intervals);
			swy.precomputed_prepareForInterpolation(
					auxTarget.getCurrentHeight(), auxTarget.getCurrentWidth(), intervals);
		}
		//return evaluateSimilarity(x, intervals, grad, true, false, bIsReverse);
		//double f = evaluateSimilarity(x, intervals, grad, true, false, bIsReverse);
		
		double f2 = evaluateSimilarityMultiThread(x, intervals, grad, true, bIsReverse);
		
		//IJ.log("f = " + f + " f2 = " + f2);
		return f2;
	}


	//------------------------------------------------------------------
	/**
	 * Get the deformation from the corresponding coefficients.
	 *
	 * @param transformation_x matrix to store the x- transformation
	 * @param transformation_y matrix to store the y- transformation
	 * @param bIsReverse flag to choose the deformation coefficients
	 *                   (source-target=TRUE or target-source=FALSE)
	 */
	public void getDeformation(
			final double [][]transformation_x,
			final double [][]transformation_y,
			boolean bIsReverse)
	{
		// Variables to allow registering in both directions.
		double [][]cx = cxTargetToSource;
		double [][]cy = cyTargetToSource;
		if(bIsReverse)
		{
			cx = cxSourceToTarget;
			cy = cySourceToTarget;
		}
		computeDeformation(intervals, cx, cy,
				transformation_x,transformation_y, bIsReverse);
		
	} // end getDeformation

	//------------------------------------------------------------------
	/**
	 * Get the direct deformation X coefficients.
	 *
	 * @return coefficients array
	 */
	public double[][] getDirectDeformationCoefficientsX()
	{
		return this.cxTargetToSource;
		
	} // end getDirectDeformationCoefficientsX	
	
	//------------------------------------------------------------------
	/**
	 * Get the direct deformation Y coefficients.
	 *
	 * @return coefficients array
	 */
	public double[][] getDirectDeformationCoefficientsY()
	{
		return this.cyTargetToSource;
		
	} // end getDirectDeformationCoefficientsY
	
	//------------------------------------------------------------------
	/**
	 * Get the inverse deformation X coefficients.
	 *
	 * @return coefficients array
	 */
	public double[][] getInverseDeformationCoefficientsX()
	{
		return this.cxSourceToTarget;
		
	} // end getInverseDeformationCoefficientsX
	
	//------------------------------------------------------------------
	/**
	 * Get the inverse deformation Y coefficients.
	 *
	 * @return coefficients array
	 */
	public double[][] getInverseDeformationCoefficientsY()
	{
		return this.cySourceToTarget;
		
	} // end getInverseDeformationCoefficientsY

	//------------------------------------------------------------------
	/**
	 * Get the current number of intervals between B-spline coefficients.
	 *
	 * @return coefficients array
	 */
	public int getIntervals()
	{
		return this.intervals;
		
	} // end getIntervals

	//------------------------------------------------------------------
	/**
	 * Apply the current transformation to a given point.
	 *
	 * @param u input, x- point coordinate
	 * @param v input, y- point coordinate
	 * @param xyF output, transformed point
	 * @param bIsReverse flag to decide the transformation direction (direct-inverse)
	 *                   (source-target=TRUE or target-source=FALSE)
	 */
	public void transform(double u, double v, double []xyF, boolean bIsReverse)
	{
		// Variables to allow registering in both directions.
		BSplineModel auxTarget = target;
		BSplineModel swx = swxTargetToSource;
		BSplineModel swy = swyTargetToSource;

		if(bIsReverse)
		{
			auxTarget = source;
			swx = swxSourceToTarget;
			swy = swySourceToTarget;
		}

		final double tu = (u * intervals) / (double)(auxTarget.getCurrentWidth()  - 1) + 1.0F;
		final double tv = (v * intervals) / (double)(auxTarget.getCurrentHeight() - 1) + 1.0F;

		final boolean ORIGINAL = false;
		swx.prepareForInterpolation(tu,tv,ORIGINAL);
		xyF[0] = swx.interpolateI();
		swy.prepareForInterpolation(tu,tv,ORIGINAL);
		xyF[1] = swy.interpolateI();
	}

	/*....................................................................
       Private methods
    ....................................................................*/

	//------------------------------------------------------------------
	/**
	 * Build the matrix for the landmark interpolation.
	 *
	 * @param intervals Intervals in the deformation
	 * @param K Number of landmarks
	 * @param B System matrix of the landmark interpolation
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 */
	private void build_Matrix_B(
			int intervals,    // Intervals in the deformation
			int K,            // Number of landmarks
			double [][]B,     // System matrix of the landmark interpolation
			boolean bIsReverse)
	{

		// Auxiliary variables to calculate inverse transformation
		PointHandler auxTargetPh = this.targetPh;
		double auxFactorWidth = this.targetFactorWidth;
		double auxFactorHeight = this.targetFactorHeight;

		if(bIsReverse)
		{
			auxTargetPh = this.sourcePh;
			auxFactorWidth = this.sourceFactorWidth;
			auxFactorHeight = this.sourceFactorHeight;
		}

		Vector <Point> targetVector = null;
		if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
		for (int k = 0; k<K; k++)
		{
			final Point targetPoint = (Point)targetVector.elementAt(k);
			double x = auxFactorWidth * (double)targetPoint.x;
			double y = auxFactorHeight * (double)targetPoint.y;
			final double[] bx = xWeight(x, intervals, true, bIsReverse);
			final double[] by = yWeight(y, intervals, true, bIsReverse);
			for (int i=0; i<intervals+3; i++)
				for (int j=0; j<intervals+3; j++)
					B[k][(intervals+3)*i+j] = by[i] * bx[j];
		}
	}

	//------------------------------------------------------------------
	/**
	 * Build matrix Rq1q2.
	 */
	private void build_Matrix_Rq1q2(
			int intervals,
			double weight,
			int q1, int q2,
			double [][]R,
			boolean bIsReverse)
	{build_Matrix_Rq1q2q3q4(intervals, weight, q1, q2, q1, q2, R, bIsReverse);}

	//------------------------------------------------------------------
	/**
	 * Build matrix Rq1q2q3q4.
	 */
	private void build_Matrix_Rq1q2q3q4(
			int intervals,
			double weight,
			int q1, int q2, int q3, int q4,
			double [][]R,
			boolean bIsReverse)
	{
		/* Let's define alpha_q as the q-th derivative of a B-spline

                         q   n
                       d    B (x)
          alpha_q(x)= --------------
                             q
                           dx

          eta_q1q2(x,s1,s2)=integral_0^Xdim alpha_q1(x/h-s1) alpha_q2(x/h-s2)

		 */
		double [][]etaq1q3 = new double[16][16];
		int Ydim = target.getCurrentHeight();
		int Xdim = target.getCurrentWidth();

		if(bIsReverse)
		{
			Ydim = source.getCurrentHeight();
			Xdim = source.getCurrentWidth();
		}

		build_Matrix_R_geteta(etaq1q3, q1, q3, Xdim, intervals);

		double [][]etaq2q4=null;
		if (q2!=q1 || q4!=q3 || Ydim!=Xdim)
		{
			etaq2q4=new double[16][16];
			build_Matrix_R_geteta(etaq2q4, q2, q4, Ydim, intervals);
		}
		else etaq2q4 = etaq1q3;

		int M=intervals+1;
		int Mp=intervals+3;
		for (int l=-1; l<=M; l++)
			for (int k=-1; k<=M; k++)
				for (int n=-1; n<=M; n++)
					for (int m=-1; m<=M; m++) {
						int []ip=new int[2];
						int []jp=new int[2];
						boolean valid_i = build_Matrix_R_getetaindex(l, n, intervals, ip);
						boolean valid_j = build_Matrix_R_getetaindex(k, m, intervals, jp);
						if (valid_i && valid_j)
						{
							int mn=(n+1)*Mp+(m+1);
							int kl=(l+1)*Mp+(k+1);
							R[kl][mn]+=weight*etaq1q3[jp[0]][jp[1]]*etaq2q4[ip[0]][ip[1]];
						}
					}
	}

	//------------------------------------------------------------------
	/**
	 * Compute the following integral
	 *<P>
	 *<PRE>
	 *           xF d^q1      3  x        d^q2    3  x
	 *  integral    -----   B  (--- - s1) ----- B  (--- - s2) dx
	 *           x0 dx^q1        h        dx^q2      h
	 *<PRE>
	 */

	private double build_Matrix_R_computeIntegral_aa(
			double x0,
			double xF,
			double s1,
			double s2,
			double h,
			int    q1,
			int    q2)
	{
		// Computes the following integral
		//
		//           xF d^q1      3  x        d^q2    3  x
		//  integral    -----   B  (--- - s1) ----- B  (--- - s2) dx
		//           x0 dx^q1        h        dx^q2      h

		// Form the spline coefficients
		double [][]C = new double [3][3];
		int    [][]d = new int    [3][3];
		double [][]s = new double [3][3];
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
		for (int k=0; k<3; k++)
		{
			double ck=C[q1][k]; if (ck==0) continue;
			for (int l=0; l<3; l++)
			{
				double cl=C[q2][l]; if (cl==0) continue;
				integral+=ck*cl*build_matrix_R_computeIntegral_BB(
						x0,xF,s1+s[q1][k],s2+s[q2][l],h,d[q1][k],d[q2][l]);
			}
		}
		return integral;
	}

	//------------------------------------------------------------------
	/**
	 * Compute the following integral
	 *<PRE>
	 *           xF   n1  x          n2  x
	 *  integral     B  (--- - s1)  B  (--- - s2) dx
	 *           x0       h              h
	 *</PRE>
	 */
	private double build_matrix_R_computeIntegral_BB(
			double x0,
			double xF,
			double s1,
			double s2,
			double h,
			int    n1,
			int    n2)
	{
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
			c1[k]=sign*MathTools.nchoosek(n1+1,k)/fact_n1;

		double []c2=new double [n2+2];
		double fact_n2=1; for (int k=2; k<=n2; k++) fact_n2*=k;
		sign=1;
		for (int k=0; k<=n2+1; k++, sign*=-1)
			c2[k]=sign*MathTools.nchoosek(n2+1,k)/fact_n2;

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

	//------------------------------------------------------------------
	/**
	 *<P>
	 *<PRE>
	 * Computation of the integral:
	 *             xF          q1       q2
	 *    integral       (x-s1)   (x-s2)     dx
	 *             x0          +        +
	 *</PRE>
	 */
	private double build_matrix_R_computeIntegral_xx(
			double x0,
			double xF,
			double s1,
			double s2,
			int    q1,
			int    q2)
	{
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

		// Move x0 to the first point where both integrals
		// are distinct from 0
		x0p=Math.max(x0p,Math.max(s2p,0));
		if (x0p>xFp) return 0;

		// There is something to integrate
		// Evaluate the primitive at xF and x0
		double IxFp=0;
		double Ix0p=0;
		for (int k=0; k<=q2; k++) 
		{
			double aux=MathTools.nchoosek(q2,k)/(q1+k+1)*
			Math.pow(-s2p,q2-k);
			IxFp+=Math.pow(xFp,q1+k+1)*aux;
			Ix0p+=Math.pow(x0p,q1+k+1)*aux;
		}

		return IxFp-Ix0p;
	}

	//------------------------------------------------------------------
	/**
	 * Build matrix R, get eta.
	 */
	private void build_Matrix_R_geteta(
			double [][]etaq1q2,
			int q1,
			int q2,
			int dim,
			int intervals)
	{
		boolean [][]done = new boolean[16][16];
		// Clear
		for (int i=0; i<16; i++)
			for (int j=0; j<16; j++)
			{
				etaq1q2[i][j]=0;
				done[i][j]=false;
			}

		// Compute each integral we need
		int M = intervals+1;
		double h = (double) dim/intervals;
		for (int ki1=-1; ki1<=M; ki1++)
			for (int ki2=-1; ki2<=M; ki2++)
			{
				int []ip = new int[2];
				boolean valid_i = build_Matrix_R_getetaindex(ki1, ki2, intervals, ip);
				if (valid_i && !done[ip[0]][ip[1]])
				{
					etaq1q2[ip[0]][ip[1]]=
						build_Matrix_R_computeIntegral_aa(0,dim,ki1,ki2,h,q1,q2);
					done[ip[0]][ip[1]]=true;
				}
			}
	}

	//------------------------------------------------------------------
	/**
	 * Build matrix R, get eta index.
	 */
	private boolean build_Matrix_R_getetaindex(
			int ki1,
			int ki2,
			int intervals,
			int []ip)
	{
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
		for (int i=0; i<=3; i++, two_i*=2)
		{
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

	//------------------------------------------------------------------
	/**
	 * Build regularization temporary.
	 *
	 * @param intervals intervals in the deformation
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 */
	private void buildRegularizationTemporary(int intervals, boolean bIsReverse)
	{
		// M is the number of spline coefficients per row
		int M = intervals+3;
		int M2 = M * M;

		// P11
		double[][] P11 = new double[M2][M2];
		if(bIsReverse) P11_SourceToTarget = P11;
		else           P11_TargetToSource = P11;

		for (int i=0; i<M2; i++)
			for (int j=0; j<M2; j++) P11[i][j]=0.0;
		build_Matrix_Rq1q2(intervals, divWeight           , 2, 0, P11, bIsReverse);
		build_Matrix_Rq1q2(intervals, divWeight+curlWeight, 1, 1, P11, bIsReverse);
		build_Matrix_Rq1q2(intervals,           curlWeight, 0, 2, P11, bIsReverse);

		// P22
		double[][] P22 = new double[M2][M2];
		if(bIsReverse) P22_SourceToTarget = P22;
		else           P22_TargetToSource = P22;

		for (int i=0; i<M2; i++)
			for (int j=0; j<M2; j++) P22[i][j]=0.0;
		build_Matrix_Rq1q2(intervals, divWeight           , 0, 2, P22, bIsReverse);
		build_Matrix_Rq1q2(intervals, divWeight+curlWeight, 1, 1, P22, bIsReverse);
		build_Matrix_Rq1q2(intervals,           curlWeight, 2, 0, P22, bIsReverse);

		// P12
		double[][] P12 = new double[M2][M2];
		if(bIsReverse) P12_SourceToTarget = P12;
		else           P12_TargetToSource = P12;

		for (int i=0; i<M2; i++)
			for (int j=0; j<M2; j++) P12[i][j]=0.0;
		build_Matrix_Rq1q2q3q4(intervals, 2*divWeight , 2, 0, 1, 1, P12, bIsReverse);
		build_Matrix_Rq1q2q3q4(intervals, 2*divWeight , 1, 1, 0, 2, P12, bIsReverse);
		build_Matrix_Rq1q2q3q4(intervals,-2*curlWeight, 0, 2, 1, 1, P12, bIsReverse);
		build_Matrix_Rq1q2q3q4(intervals,-2*curlWeight, 1, 1, 2, 0, P12, bIsReverse);
	}

	//------------------------------------------------------------------
	/**
	 * Compute the affine matrix.
	 *
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 */
	private double[][] computeAffineMatrix (boolean bIsReverse)
	{
		boolean adjust_size = false;

		final double[][] D = new double[3][3];
		final double[][] H = new double[3][3];
		final double[][] U = new double[3][3];
		final double[][] V = new double[3][3];
		final double[][] X = new double[2][3];
		final double[] W = new double[3];

		// Auxiliary variables to calculate inverse transformation
		PointHandler auxSourcePh = sourcePh;
		PointHandler auxTargetPh = targetPh;
		BSplineModel auxSource = source;
		BSplineModel auxTarget = target;
		double auxFactorWidth = this.targetFactorWidth;
		double auxFactorHeight = this.targetFactorHeight;

		if(bIsReverse)
		{
			auxSourcePh = targetPh;
			auxTargetPh = sourcePh;
			auxSource = target;
			auxTarget = source;
			auxFactorWidth = this.sourceFactorWidth;
			auxFactorHeight = this.sourceFactorHeight;
		}

		Vector <Point> sourceVector=null;
		if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
		else                   sourceVector = new Vector <Point>();

		Vector <Point> targetVector = null;
		if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
		else                   targetVector = new Vector <Point>();

		int removeLastPoint = 0;
					
		int n = targetVector.size();
		switch (n)
		{
		case 0:
			for (int i = 0; (i < 2); i++)
				for (int j = 0; (j < 3); j++) X[i][j]=0.0;
			if (adjust_size)
			{
				// Make both images of the same size
				X[0][0] = (double)auxSource.getCurrentWidth () / auxTarget.getCurrentWidth ();
				X[1][1] = (double)auxSource.getCurrentHeight() / auxTarget.getCurrentHeight();
			}
			else
			{
				// Make both images to be centered
				X[0][0] = X[1][1] = 1;
				X[0][2] = ((double)auxSource.getCurrentWidth () - auxTarget.getCurrentWidth ())/2;
				X[1][2] = ((double)auxSource.getCurrentHeight() - auxTarget.getCurrentHeight())/2;
			}
			break;
		case 1:
			for (int i = 0; (i < 2); i++) {
				for (int j = 0; (j < 2); j++) {
					X[i][j] = (i == j) ? (1.0F) : (0.0F);
				}
			}
			X[0][2] = auxFactorWidth * (double)(((Point)sourceVector.firstElement()).x
					- ((Point)targetVector.firstElement()).x);
			X[1][2] = auxFactorHeight * (double)(((Point)sourceVector.firstElement()).y
					- ((Point)targetVector.firstElement()).y);
			break;
		case 2:
			final double x0 = auxFactorWidth  * ((Point)sourceVector.elementAt(0)).x;
			final double y0 = auxFactorHeight * ((Point)sourceVector.elementAt(0)).y;
			final double x1 = auxFactorWidth  * ((Point)sourceVector.elementAt(1)).x;
			final double y1 = auxFactorHeight * ((Point)sourceVector.elementAt(1)).y;
			final double u0 = auxFactorWidth  * ((Point)targetVector.elementAt(0)).x;
			final double v0 = auxFactorHeight * ((Point)targetVector.elementAt(0)).y;
			final double u1 = auxFactorWidth  * ((Point)targetVector.elementAt(1)).x;
			final double v1 = auxFactorHeight * ((Point)targetVector.elementAt(1)).y;
			sourceVector.addElement(new Point((int)(x1 + y0 - y1), (int)(x1 + y1 - x0)));
			targetVector.addElement(new Point((int)(u1 + v0 - v1), (int)(u1 + v1 - u0)));
			removeLastPoint=1;
			n = 3;
		default:
			for (int i = 0; (i < 3); i++)
			{
				for (int j = 0; (j < 3); j++)
				{
					H[i][j] = 0.0F;
				}
			}
		for (int k = 0; (k < n); k++)
		{
			final Point sourcePoint = (Point)sourceVector.elementAt(k);
			final Point targetPoint = (Point)targetVector.elementAt(k);
			final double sx = auxFactorWidth * (double)sourcePoint.x;
			final double sy = auxFactorHeight* (double)sourcePoint.y;
			final double tx = auxFactorWidth * (double)targetPoint.x;
			final double ty = auxFactorHeight* (double)targetPoint.y;
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
		MathTools.singularValueDecomposition(H, W, V);
		if ((Math.abs(W[0]) < FLT_EPSILON) || (Math.abs(W[1]) < FLT_EPSILON)
				|| (Math.abs(W[2]) < FLT_EPSILON)) {
			return(computeRotationMatrix(bIsReverse));
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
				auxSourcePh.getPoints().removeElementAt(n-i);
				auxTargetPh.getPoints().removeElementAt(n-i);
			}
		}
		
		
		double centerX = (double) auxSource.getCurrentWidth();
		double centerY = (double) auxSource.getCurrentHeight();

		final AffineTransform matrix = new AffineTransform(X[0][0], X[1][0],
		                                             X[0][1], X[1][1],
		                                             X[0][2], X[1][2]);
		
		//MiscTools.printMatrix("source affine matrix before reg", X);
		
		// "Rigidize" matrix based on the user preferences
		regularizeMatrix(matrix, centerX, centerY);
		
		
		X[0][0] = matrix.getScaleX();
		X[0][1] = matrix.getShearX();
		
		X[1][0] = matrix.getShearY();
		X[1][1] = matrix.getScaleY();
		
		X[0][2] = matrix.getTranslateX();
		X[1][2] = matrix.getTranslateY();
		
		//MiscTools.printMatrix("source affine matrix after reg", X);
		
		
		return(X);
	} // end computeAffineMatrix

	// -------------------------------------------------------------------
	/**
	 * Regularize matrix to remove sharing, scaling, etc.
	 * @param a affine transform
	 * @param centerX image center x- coordinate
	 * @param centerX image center y- coordinate
	 */
	public void regularizeMatrix(
			AffineTransform a,
            double centerX, 
            double centerY)
	{
		
		// Move to the center of the image
		a.translate(centerX, centerY);

		/*
			IJ.log(" A: " + a.getScaleX() + " " + a.getShearY() + " " + a.getShearX()
					+ " " + a.getScaleY() + " " + a.getTranslateX() + " " + 
					+ a.getTranslateY() );
		 */

		// retrieves scaling, shearing, rotation and translation from an affine
		// transformation matrix A (which has translation values in the right column)
		// by Daniel Berger for MIT-BCS Seung, April 19 2009

		// We assume that sheary=0
		// scalex=sqrt(A(1,1)*A(1,1)+A(2,1)*A(2,1));
		final double a11 = a.getScaleX();
		final double a21 = a.getShearY();
		final double scaleX = Math.sqrt( a11 * a11 + a21 * a21 );
		// rotang=atan2(A(2,1)/scalex,A(1,1)/scalex);
		final double rotang = Math.atan2( a21/scaleX, a11/scaleX);

		// R=[[cos(-rotang) -sin(-rotang)];[sin(-rotang) cos(-rotang)]];

		// rotate back shearx and scaley
		//v=R*[A(1,2) A(2,2)]';
		final double a12 = a.getShearX();
		final double a22 = a.getScaleY();
		final double shearX = Math.cos(-rotang) * a12 - Math.sin(-rotang) * a22;
		final double scaleY = Math.sin(-rotang) * a12 + Math.cos(-rotang) * a22;

		// rotate back translation
		// v=R*[A(1,3) A(2,3)]';
		final double transX = Math.cos(-rotang) * a.getTranslateX() - Math.sin(-rotang) * a.getTranslateY();
		final double transY = Math.sin(-rotang) * a.getTranslateX() + Math.cos(-rotang) * a.getTranslateY();

		// TWEAK	
		
		

		final double new_shearX = shearX * (1.0 - tweakShear); 
		//final double new_shearY = 0; // shearY * (1.0 - tweakShear);

		final double avgScale = (scaleX + scaleY)/2;
		final double aspectRatio = scaleX / scaleY;
		final double regAvgScale = avgScale * (1.0 - tweakScale) + 1.0  * tweakScale;
		final double regAspectRatio = aspectRatio * (1.0 - tweakIso) + 1.0 * tweakIso;

		//IJ.log("avgScale = " + avgScale + " aspectRatio = " + aspectRatio + " regAvgScale = " + regAvgScale + " regAspectRatio = " + regAspectRatio);

		final double new_scaleY = (2.0 * regAvgScale) / (regAspectRatio + 1.0);
		final double new_scaleX = regAspectRatio * new_scaleY;

		final AffineTransform b = makeAffineMatrix(new_scaleX, new_scaleY, new_shearX, 0, rotang, transX, transY);

		//IJ.log("new_scaleX = " + new_scaleX + " new_scaleY = " + new_scaleY + " new_shearX = " + new_shearX + " new_shearY = " + new_shearY);		    		    		    

		// Move back the center
		b.translate(-centerX, -centerY);
		
		a.setTransform(b);

		
										
	}// end method regularize
	
	//---------------------------------------------------------------------------------
	/**
	 * Makes an affine transformation matrix from the given scale, shear,
	 * rotation and translation values
     * if you want a uniquely retrievable matrix, give sheary=0
     * 
	 * @param scalex scaling in x
	 * @param scaley scaling in y
	 * @param shearx shearing in x
	 * @param sheary shearing in y
	 * @param rotang angle of rotation (in radians)
	 * @param transx translation in x
	 * @param transy translation in y
	 * @return affine transformation matrix
	 */
	public static AffineTransform makeAffineMatrix(
			final double scalex, 
			final double scaley, 
			final double shearx, 
			final double sheary, 
			final double rotang, 
			final double transx, 
			final double transy)
	{
		/*
		%makes an affine transformation matrix from the given scale, shear,
		%rotation and translation values
		%if you want a uniquely retrievable matrix, give sheary=0
		%by Daniel Berger for MIT-BCS Seung, April 19 2009

		A=[[scalex shearx transx];[sheary scaley transy];[0 0 1]];
		A=[[cos(rotang) -sin(rotang) 0];[sin(rotang) cos(rotang) 0];[0 0 1]] * A;
		*/
		
		final double m00 = Math.cos(rotang) * scalex - Math.sin(rotang) * sheary;
		final double m01 = Math.cos(rotang) * shearx - Math.sin(rotang) * scaley;
		final double m02 = Math.cos(rotang) * transx - Math.sin(rotang) * transy;
		
		final double m10 = Math.sin(rotang) * scalex + Math.cos(rotang) * sheary;
		final double m11 = Math.sin(rotang) * shearx + Math.cos(rotang) * scaley;
		final double m12 = Math.sin(rotang) * transx + Math.cos(rotang) * transy;
		
		return new AffineTransform( m00,  m10,  m01,  m11,  m02,  m12);		
	} // end method makeAffineMatrix	
	
	
	//------------------------------------------------------------------
	/**
	 * Compute the affine residues for the landmarks.
	 * <p>
	 * NOTE: The output vectors should be already resized
	 *
	 * @param affineMatrix Input
	 * @param dx Output, difference in x for each landmark
	 * @param dy Output, difference in y for each landmark
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 * @deprecated
	 */
	private void computeAffineResidues(
			final double[][] affineMatrix,               // Input
			final double[] dx,                           // output, difference in x for each landmark
			final double[] dy,                           // output, difference in y for each landmark
			// The output vectors should be already resized
			boolean bIsReverse)
	{
		// Auxiliary variables to allow registering in both directions
		double auxFactorWidth = target.getFactorWidth();
		double auxFactorHeight = target.getFactorHeight();
		PointHandler auxSourcePh = this.sourcePh;
		PointHandler auxTargetPh = this.targetPh;
		if(bIsReverse)
		{
			auxFactorWidth = source.getFactorWidth();
			auxFactorHeight = source.getFactorHeight();
			auxSourcePh = this.targetPh;
			auxTargetPh = this.sourcePh;
		}

		Vector <Point> sourceVector=null;
		if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
		else                   sourceVector = new Vector <Point>();

		Vector <Point> targetVector = null;
		if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
		else                   targetVector = new Vector <Point>();

		final int K = auxTargetPh.getPoints().size();
		for (int k=0; k<K; k++)
		{
			final Point sourcePoint = (Point)sourceVector.elementAt(k);
			final Point targetPoint = (Point)targetVector.elementAt(k);

			double u = auxFactorWidth  * (double)targetPoint.x;
			double v = auxFactorHeight * (double)targetPoint.y;

			final double x = affineMatrix[0][2]
			                                 + affineMatrix[0][0] * u + affineMatrix[0][1] * v;

			final double y = affineMatrix[1][2]
			                                 + affineMatrix[1][0] * u + affineMatrix[1][1] * v;

			dx[k] = auxFactorWidth  * (double)sourcePoint.x - x;
			dy[k] = auxFactorHeight * (double)sourcePoint.y - y;
		}
	}

	//------------------------------------------------------------------
	/**
	 * Compute the coefficients at this scale.
	 *
	 * @param intervals input, number of intervals at this scale
	 * @param dx input, x residue so far
	 * @param dy input, y residue so far
	 * @param cx output, x coefficients for splines
	 * @param cy output, y coefficients for splines
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 * @return under-constrained flag
	 */
	private boolean computeCoefficientsScale(
			final int intervals,                       // input, number of intervals at this scale
			final double []dx,                         // input, x residue so far
			final double []dy,                         // input, y residue so far
			final double [][]cx,                       // output, x coefficients for splines
			final double [][]cy,                       // output, y coefficients for splines
			boolean bIsReverse)
	{

		PointHandler auxTargetPh = (bIsReverse) ? this.sourcePh : this.targetPh;

		int K=0;
		if (auxTargetPh!=null) K = auxTargetPh.getPoints().size();
		boolean underconstrained=false;

		if (0<K) {
			// Form the equation system Bc=d
			final double[][] B = new double[K][(intervals + 3) * (intervals + 3)];
			build_Matrix_B(intervals, K, B, bIsReverse);

			// "Invert" the matrix B
			int Nunk=(intervals+3)*(intervals+3);
			double [][] iB=new double[Nunk][K];
			underconstrained=MathTools.invertMatrixSVD(K,Nunk,B,iB);

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
	//------------------------------------------------------------------
	/**
	 * Compute the coefficients scale with regularization.
	 *
	 * @param intervals input, number of intervals at this scale
	 * @param dx input, x residue so far
	 * @param dy input, y residue so far
	 * @param cx output, x coefficients for splines
	 * @param cy output, y coefficients for splines
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 * @return under-constrained flag
	 */
	private boolean computeCoefficientsScaleWithRegularization(
			final int intervals,                       // input, number of intervals at this scale
			final double []dx,                         // input, x residue so far
			final double []dy,                         // input, y residue so far
			final double [][]cx,                       // output, x coefficients for splines
			final double [][]cy,                       // output, y coefficients for splines
			boolean bIsReverse)
	{

		double P11[][] = this.P11_TargetToSource;
		double P12[][] = this.P12_TargetToSource;
		double P22[][] = this.P22_TargetToSource;

		PointHandler auxTargetPh = this.targetPh;
		if(bIsReverse)
		{
			auxTargetPh = this.sourcePh;
			P11 = this.P11_SourceToTarget;
			P12 = this.P12_SourceToTarget;
			P22 = this.P22_SourceToTarget;
		}

		boolean underconstrained=true;
		int K = 0;
		if (auxTargetPh!=null) K = auxTargetPh.getPoints().size();

		if (0<K) {
			// M is the number of spline coefficients per row
			int M=intervals+3;
			int M2=M*M;

			// Create A and b for the system Ac=b
			final double[][] A = new double[2*M2][2*M2];
			final double[]   b = new double[2*M2];
			for (int i=0; i<2*M2; i++) {
				b[i]=0.0;
				for (int j=0; j<2*M2; j++) A[i][j]=0.0;
			}

			// Get the matrix related to the landmarks
			final double[][] B = new double[K][M2];
			build_Matrix_B(intervals, K, B, bIsReverse);

			// Fill the part of the equation system related to the landmarks
			// Compute 2 * B^t * B
			for (int i=0; i<M2; i++) {
				for (int j=i; j<M2; j++) {
					double bitbj=0; // bi^t * bj, i.e., column i x column j
					for (int l=0; l<K; l++)
						bitbj+=B[l][i]*B[l][j];
					bitbj*=2;
//					int ij=i*M2+j;
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
			underconstrained=MathTools.invertMatrixSVD(2*M2,2*M2,A,iA);

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

	//------------------------------------------------------------------
	/**
	 * Compute the initial residues for the landmarks.
	 * <p>
	 * NOTE: The output vectors should be already resized
	 *
	 * @param dx output, difference in x for each landmark
	 * @param dy output, difference in y for each landmark
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 */
	private void computeInitialResidues(
			final double[] dx,                           // output, difference in x for each landmark
			final double[] dy,                           // output, difference in y for each landmark
			// The output vectors should be already resized
			boolean bIsReverse)
	{

		// Auxiliary variables for registering in both directions.
		double auxFactorWidth = target.getFactorWidth();
		double auxFactorHeight = target.getFactorHeight();
		PointHandler auxSourcePh = this.sourcePh;
		PointHandler auxTargetPh = this.targetPh;

		if(bIsReverse)
		{
			auxFactorWidth = source.getFactorWidth();
			auxFactorHeight = source.getFactorHeight();
			auxSourcePh = this.targetPh;
			auxTargetPh = this.sourcePh;
		}


		Vector <Point> sourceVector=null;
		if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
		else                   sourceVector = new Vector <Point>();

		Vector <Point> targetVector = null;
		if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
		else                   targetVector = new Vector <Point>();
		int K = 0;

		if (auxTargetPh!=null) auxTargetPh.getPoints().size();

		for (int k=0; k<K; k++)
		{
			final Point sourcePoint = (Point)sourceVector.elementAt(k);
			final Point targetPoint = (Point)targetVector.elementAt(k);
			dx[k] = auxFactorWidth  * (sourcePoint.x - targetPoint.x);
			dy[k] = auxFactorHeight * (sourcePoint.y - targetPoint.y);
		}
	}

	//------------------------------------------------------------------
	/**
	 * Compute the deformation.
	 *
	 * @param intervals input, number of intervals
	 * @param cx input, X B-spline coefficients
	 * @param cy input, Y B-spline coefficients
	 * @param transformation_x output, X transformation map
	 * @param transformation_y output, Y transformation map
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 */
	private void computeDeformation(
			final int intervals,
			final double [][]cx,
			final double [][]cy,
			final double [][]transformation_x,
			final double [][]transformation_y,
			boolean bIsReverse)
	{

		int auxTargetCurrentHeight = this.targetCurrentHeight;
		int auxTargetCurrentWidth = this.targetCurrentWidth;

		if(bIsReverse)
		{
			auxTargetCurrentHeight = this.sourceCurrentHeight;
			auxTargetCurrentWidth  = this.sourceCurrentWidth;
		}

/*
		// Set these coefficients to an interpolator
		BSplineModel swx = new BSplineModel(cx);
		BSplineModel swy = new BSplineModel(cy);

		
		// Compute the transformation mapping
		for (int v=0; v<auxTargetCurrentHeight; v++) {
			final double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;
			for (int u = 0; u<auxTargetCurrentWidth; u++)
			{
				final double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth - 1) + 1.0F;
				swx.prepareForInterpolation(tu, tv, ORIGINAL);
				transformation_x[v][u] = swx.interpolateI();
				swy.prepareForInterpolation(tu, tv, ORIGINAL);
				transformation_y[v][u] = swy.interpolateI();
			}
		}
		
		*/
		
		Thread x_thread = new Thread(new ConcurrentDeformation(cx,	auxTargetCurrentHeight, auxTargetCurrentWidth,
			 		  								 			transformation_x, intervals));
		
		Thread y_thread = new Thread(new ConcurrentDeformation(cy,	auxTargetCurrentHeight, auxTargetCurrentWidth,
		 			transformation_y, intervals));
		
		x_thread.start();
		y_thread.start();
		
		try{
			x_thread.join();
			y_thread.join();
			x_thread = null;
			y_thread = null;
		} catch (InterruptedException e) {
				e.printStackTrace();					
		}
	}

	/* ------------------------------------------------------------------------ */
	/**
	 *  Class to concurrently calculate the two deformation mapping tables
	 * 	 
	 */	
	private class ConcurrentDeformation extends Thread
	{
		final double [][]c;			
		final int auxTargetCurrentHeight;
		final int auxTargetCurrentWidth;		
		final double[][] transformation;
		final int intervals;
		
		ConcurrentDeformation(double [][]c, 
		 		  			  int auxTargetCurrentHeight,
		 		  			  int auxTargetCurrentWidth,
		 		  			  double[][] transformation,
		 		  			  int intervals)
		{
			this.c = c;	
			this.auxTargetCurrentWidth = auxTargetCurrentWidth;
			this.auxTargetCurrentHeight = auxTargetCurrentHeight;
			this.transformation = transformation;
			this.intervals = intervals;
		}
	
		//------------------------------------------------------------------
		/**
		 * Run method to calculate the corresponding X or Y transformation
		 * table.
		 */
		public void run() 
		{
			// Set these coefficients to an interpolator
			BSplineModel sw = new BSplineModel(c);
			
			// Compute the transformation mapping
			for (int v=0; v<auxTargetCurrentHeight; v++) 
			{
				final double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;
				for (int u = 0; u<auxTargetCurrentWidth; u++)
				{
					final double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth - 1) + 1.0F;
					transformation[v][u] = sw.prepareForInterpolationAndInterpolateI(tu, tv, false, ORIGINAL);
				}
			}			
		} /* end run */
	} /* end ConcurrentDeformation class */
	
	
	//------------------------------------------------------------------
	/**
	 * Compute the rotation matrix.
	 *
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 * @return rotation matrix
	 */
	private double[][] computeRotationMatrix (boolean bIsReverse)
	{
		final double[][] X = new double[2][3];
		final double[][] H = new double[2][2];
		final double[][] V = new double[2][2];
		final double[] W = new double[2];


		double auxFactorWidth = this.targetFactorWidth;
		double auxFactorHeight = this.targetFactorHeight;
		PointHandler auxSourcePh = this.sourcePh;
		PointHandler auxTargetPh = this.targetPh;
		if(bIsReverse)
		{
			auxFactorWidth = this.sourceFactorWidth;
			auxFactorHeight = this.sourceFactorHeight;
			auxSourcePh = this.targetPh;
			auxTargetPh = this.sourcePh;
		}

		Vector <Point> sourceVector = null;
		if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
		else                   sourceVector = new Vector <Point>();

		Vector <Point> targetVector = null;
		if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
		else                   targetVector = new Vector <Point>();

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
			X[0][2] = auxFactorWidth  * (double)(((Point)sourceVector.firstElement()).x
					- ((Point)targetVector.firstElement()).x);
			X[1][2] = auxFactorHeight * (double)(((Point)sourceVector.firstElement()).y
					- ((Point)targetVector.firstElement()).y);
			break;
		default:
			double xTargetAverage = 0.0F;
		double yTargetAverage = 0.0F;

		for (int i = 0; (i < n); i++)
		{
			final Point p = (Point)targetVector.elementAt(i);
			xTargetAverage += auxFactorWidth  * (double)p.x;
			yTargetAverage += auxFactorHeight * (double)p.y;
		}

		xTargetAverage /= (double)n;
		yTargetAverage /= (double)n;

		final double[] xCenteredTarget = new double[n];
		final double[] yCenteredTarget = new double[n];

		for (int i = 0; (i < n); i++)
		{
			final Point p = (Point)targetVector.elementAt(i);
			xCenteredTarget[i] = auxFactorWidth *(double)p.x - xTargetAverage;
			yCenteredTarget[i] = auxFactorHeight*(double)p.y - yTargetAverage;
		}

		double xSourceAverage = 0.0F;
		double ySourceAverage = 0.0F;

		for (int i = 0; (i < n); i++)
		{
			final Point p = (Point)sourceVector.elementAt(i);
			xSourceAverage += auxFactorWidth *(double)p.x;
			ySourceAverage += auxFactorHeight*(double)p.y;
		}

		xSourceAverage /= (double)n;
		ySourceAverage /= (double)n;

		final double[] xCenteredSource = new double[n];
		final double[] yCenteredSource = new double[n];

		for (int i = 0; (i < n); i++)
		{
			final Point p = (Point)sourceVector.elementAt(i);
			xCenteredSource[i] = auxFactorWidth *(double)p.x - xSourceAverage;
			yCenteredSource[i] = auxFactorHeight*(double)p.y - ySourceAverage;
		}

		for (int i = 0; (i < 2); i++) {
			for (int j = 0; (j < 2); j++) {
				H[i][j] = 0.0F;
			}
		}

		for (int k = 0; (k < n); k++)
		{
			H[0][0] += xCenteredTarget[k] * xCenteredSource[k];
			H[0][1] += xCenteredTarget[k] * yCenteredSource[k];
			H[1][0] += yCenteredTarget[k] * xCenteredSource[k];
			H[1][1] += yCenteredTarget[k] * yCenteredSource[k];
		}
		// COSS: Watch out that this H is the transpose of the one
		// defined in the text. That is why X=V*U^t is the inverse
		// of the rotation matrix.
		MathTools.singularValueDecomposition(H, W, V);
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

	//------------------------------------------------------------------
	/**
	 * Compute the scale residues.
	 * <p>
	 * NOTE: At the input dx and dy have the residues so far, at the output these
	 * residues are modified to account for the model at the new scale
	 *
	 * @param intervals input, number of intervals
	 * @param cx input, X B-spline coefficients
	 * @param cy input, Y B-spline coefficients
	 * @param dx input/output, X residues
	 * @param dy input/output, Y residues
	 * @param bIsReverse determines the transformation direction (target-source=FALSE or source-target=TRUE)
	 * @deprecated
	 */
	private void computeScaleResidues(
			int intervals,                                  // input, number of intervals
			final double [][]cx,                            // Input, spline coefficients
			final double [][]cy,
			final double []dx,                              // Input/Output. At the input it has the
			final double []dy,                              // residue so far, at the output this
			// residue is modified to account for
			// the model at the new scale
			boolean bIsReverse)
	{

		double auxFactorWidth = target.getFactorWidth();
		double auxFactorHeight = target.getFactorHeight();
		PointHandler auxSourcePh = this.sourcePh;
		PointHandler auxTargetPh = this.targetPh;
		int auxTargetCurrentWidth = this.targetCurrentWidth;
		int auxTargetCurrentHeight = this.targetCurrentHeight;

		if(bIsReverse)
		{
			auxFactorWidth = source.getFactorWidth();
			auxFactorHeight = source.getFactorHeight();
			auxSourcePh = this.targetPh;
			auxTargetPh = this.sourcePh;
			auxTargetCurrentWidth = this.sourceCurrentWidth;
			auxTargetCurrentHeight = this.sourceCurrentHeight;
		}

		// Set these coefficients to an interpolator
		BSplineModel swx = new BSplineModel(cx);
		BSplineModel swy = new BSplineModel(cy);

		// Get the list of landmarks
		Vector <Point> sourceVector=null;
		if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
		else                   sourceVector = new Vector <Point> ();

		Vector <Point> targetVector = null;
		if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
		else                   targetVector = new Vector <Point> ();
		final int K = targetVector.size();

		for (int k=0; k<K; k++)
		{
			// Get the landmark coordinate in the target image
			final Point sourcePoint = (Point)sourceVector.elementAt(k);
			final Point targetPoint = (Point)targetVector.elementAt(k);
			double u = auxFactorWidth  * (double)targetPoint.x;
			double v = auxFactorHeight * (double)targetPoint.y;

			// Express it in "spline" units
			double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth  - 1) + 1.0F;
			double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;

			// Transform this coordinate to the source image
			swx.prepareForInterpolation(tu, tv, false); double x=swx.interpolateI();
			swy.prepareForInterpolation(tu, tv, false); double y=swy.interpolateI();

			// Substract the result from the residual
			dx[k] = auxFactorWidth *(double)sourcePoint.x - x;
			dy[k] = auxFactorHeight*(double)sourcePoint.y - y;
		}
	}

	/*--------------------------------------------------------------------------*/
	/**
	 * This code is an excerpt from doBidirectionalRegistration() to compute the exact
	 * number of steps.
	 */
	private void computeTotalWorkload()
	{
		// This code is an excerpt from doBidirectionalRegistration() to compute the exact
		// number of steps

		// Now refine with the different scales
		int state;  // state=-1 --> Finish
		// state= 0 --> Increase deformation detail
		// state= 1 --> Increase image detail
		// state= 2 --> Do nothing until the finest image scale
		if (min_scale_deformation==max_scale_deformation) state=1;
		else                                              state=0;
		int s=min_scale_deformation;
		int currentDepth = target.getCurrentDepth();
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
		ProgressBar.resetProgressBar();
		ProgressBar.addWorkload(workload);
	}

	/*--------------------------------------------------------------------------*/
	/**
	 * Calculate the geometric error between the source-target and target-source
	 * deformations (the corresponding coefficients are assumed to be at swxTargetToSource,
	 * swyTargetToSource, swxSourceToTarget and swySourceToTarget).
	 *
	 * @param intervals Input: Number of intervals for the deformation
	 * @param grad Output: Gradient of the function
	 * 
	 * @return geometric error between the source-target and target-source deformations.
	 * @deprecated
	 */

	private double evaluateConsistency(
			final int intervals,
			double []grad)
	{

		int cYdim = intervals+3;
		int cXdim = cYdim;
		int Nk = cYdim * cXdim;
		int twiceNk = 2 * Nk;

		// Initialize gradient
		for (int k=0; k<grad.length; k++)
			grad[k]=0.0F;

		// Compute the deformation
		// Set these coefficients to an interpolator
		BSplineModel swx_direct = this.swxTargetToSource; 
		BSplineModel swy_direct = this.swyTargetToSource;  

		BSplineModel swx_inverse = this.swxSourceToTarget; 
		BSplineModel swy_inverse = this.swySourceToTarget; 

		// *********** Compute the geometric error and gradient (DIRECT) ***********       
		double f_direct = 0;
		int n_direct = 0;
		for (int v=0; v<this.targetCurrentHeight; v++)
			for (int u=0; u<this.targetCurrentWidth; u++)
			{
				// Check if this point is in the target mask
				if (this.targetMsk.getValue(u/this.targetFactorWidth, v/this.targetFactorHeight))
				{

					final int x = (int) Math.round(swx_direct.precomputed_interpolateI(u,v));
					final int y = (int) Math.round(swy_direct.precomputed_interpolateI(u,v));					 					

					if (x>=0 && x<this.sourceCurrentWidth && y>=0 && y<this.sourceCurrentHeight)
					{
						final double x2 = swx_inverse.precomputed_interpolateI(x,y);
						final double y2 = swy_inverse.precomputed_interpolateI(x,y);
						double aux1 = u - x2;
						double aux2 = v - y2;

						f_direct += aux1 * aux1 + aux2 * aux2;

						// Compute the derivative with respect to all the c coefficients
						// Derivatives from direct coefficients.
						for (int l=0; l<4; l++)
							for (int m=0; m<4; m++)
							{
								if (swx_direct.prec_yIndex[v][l]==-1 || swx_direct.prec_xIndex[u][m]==-1)
									continue;

								double dddx = swx_direct.precomputed_getWeightI(l, m, u, v);
								double dixx = swx_inverse.precomputed_getWeightDx(l, m, x, y);
								double diyy = swy_inverse.precomputed_getWeightDy(l, m, x, y);

								double weightIx = (dixx + diyy) * dddx;

								double dddy = swy_direct.precomputed_getWeightI(l, m, u, v);
								double dixy = swx_inverse.precomputed_getWeightDy(l, m, x, y);
								double diyx = swy_inverse.precomputed_getWeightDx(l, m, x, y);

								double weightIy = (diyx + dixy) * dddy;

								int k = swx_direct.prec_yIndex[v][l] * cYdim + swx_direct.prec_xIndex[u][m];

								// Derivative related to X deformation
								grad[k]        += -aux1 * weightIx;

								// Derivative related to Y deformation
								grad[k+twiceNk]+= -aux2 * weightIy;
							}


						// Derivatives from inverse coefficients.
						for (int l=0; l<4; l++)
							for (int m=0; m<4; m++)
							{
								// d inverse(direct(x)) / d c_inverse
								if (swx_inverse.prec_yIndex[y][l]==-1 || swx_inverse.prec_xIndex[x][m]==-1)
									continue;

								double weightI = swx_inverse.precomputed_getWeightI(l, m, x, y);

								int k = swx_inverse.prec_yIndex[y][l] * cYdim + swx_inverse.prec_xIndex[x][m];

								// Derivative related to X deformation
								grad[k+Nk]        += -aux1 * weightI;

								// Derivative related to Y deformation
								grad[k+Nk+twiceNk]+= -aux2 * weightI;
							}


						n_direct++; // Another point has been successfully evaluated
					}
				}// end if mask.
			}

		if (n_direct != 0)
		{
			f_direct /= (double) n_direct;

			// Average the image related terms
			double aux = consistencyWeight * 2.0 / n_direct;  // This is the 2 coming from the
			// derivative that I would do later
			for (int k=0; k<grad.length; k++)
				grad[k] *= aux;
		}

		// Inverse gradient
		double []vgrad = new double[grad.length];

		// Initialize gradient
		for (int k=0; k<vgrad.length; k++)
			vgrad[k] = 0.0F;

		// *********** Compute the geometric error and gradient (INVERSE) ***********
		double f_inverse = 0;
		int n_inverse = 0;
		for (int v=0; v<this.sourceCurrentHeight; v++)
			for (int u=0; u<this.sourceCurrentWidth; u++)
			{
				// Check if this point is in the target mask
				if (this.sourceMsk.getValue(u/this.sourceFactorWidth, v/this.sourceFactorHeight))
				{
					final int x = (int) Math.round( swx_inverse.precomputed_interpolateI(u, v));
					final int y = (int) Math.round( swy_inverse.precomputed_interpolateI(u, v));

					if (x>=0 && x<this.targetCurrentWidth && y>=0 && y<this.targetCurrentHeight)
					{
						final double x2 = swx_direct.precomputed_interpolateI(x, y);
						final double y2 = swy_direct.precomputed_interpolateI(x, y);
						double aux1 = u - x2;
						double aux2 = v - y2;

						f_inverse += aux1 * aux1 + aux2 * aux2;

						// Compute the derivative with respect to all the c coefficients
						// Derivatives from direct coefficients.
						for (int l=0; l<4; l++)
							for (int m=0; m<4; m++)
							{
								// d direct(inverse(x)) / d c_direct
								if (swx_direct.prec_yIndex[y][l]==-1 || swx_direct.prec_xIndex[x][m]==-1)
									continue;

								double weightI = swx_direct.precomputed_getWeightI(l, m, x, y);

								int k = swx_direct.prec_yIndex[y][l] * cYdim + swx_direct.prec_xIndex[x][m];

								// Derivative related to X deformation
								vgrad[k]        += -aux1 * weightI;

								// Derivative related to Y deformation
								vgrad[k+twiceNk]+= -aux2 * weightI;
							}
						// Derivatives from inverse coefficients.
						for (int l=0; l<4; l++)
							for (int m=0; m<4; m++)
							{
								if (swx_inverse.prec_yIndex[v][l]==-1 || swx_inverse.prec_xIndex[u][m]==-1)
									continue;

								double diix = swx_inverse.precomputed_getWeightI(l, m, u, v);
								double ddxx = swx_direct.precomputed_getWeightDx(l, m, x, y);
								double ddyy = swy_direct.precomputed_getWeightDy(l, m, x, y);

								double weightIx = (ddxx + ddyy) * diix;

								double diiy = swy_inverse.precomputed_getWeightI(l, m, u, v);
								double ddxy = swx_direct.precomputed_getWeightDy(l, m, x, y);
								double ddyx = swy_direct.precomputed_getWeightDx(l, m, x, y);

								double weightIy = (ddyx + ddxy) * diiy;


								int k = swx_inverse.prec_yIndex[v][l] * cYdim + swx_inverse.prec_xIndex[u][m];

								// Derivative related to X deformation
								vgrad[k+Nk]        += -aux1 * weightIx;

								// Derivative related to Y deformation
								vgrad[k+Nk+twiceNk]+= -aux2 * weightIy;
							}

						n_inverse++; // Another point has been successfully evaluated
					}
				} // end if mask
			}

		if (n_inverse != 0)
		{
			f_inverse /= (double) n_inverse;

			// Average the image related terms
			double aux = consistencyWeight * 2.0 / n_inverse;  // This is the 2 coming from the
			// derivative that I would do later
			for(int k=0; k<vgrad.length; k++)
				vgrad[k] *= aux;
		}

		// Sum of both gradients (direct and inverse)
		for(int k=0; k<grad.length; k++)
			grad[k] += vgrad[k];


		this.partialDirectConsitencyError = this.consistencyWeight * f_direct;
		this.partialInverseConsitencyError = this.consistencyWeight * f_inverse;


		double consistencyDirectError = (n_direct == 0) ? 1.0/FLT_EPSILON : (this.consistencyWeight * f_direct);
		double consistencyInverseError = (n_inverse == 0) ? 1.0/FLT_EPSILON : (this.consistencyWeight * f_inverse);

		if (showMarquardtOptim)
		{
			IJ.write("    Consistency Error (s-t): " + consistencyDirectError);
			IJ.write("    Consistency Error (t-s): " + consistencyInverseError);
		}


		if(n_direct == 0 || n_inverse == 0)
			return 1/FLT_EPSILON;
		return (this.consistencyWeight * (f_direct + f_inverse));
	}	 


	/*--------------------------------------------------------------------------*/
	/**
	 * Calculate the geometric error between the source-target and target-source
	 * deformations.
	 *
	 * @deprecated
	 * 
	 * @param c_direct Input: Direct deformation coefficients
	 * @param c_inverse Input: Inverse deformation coefficients
	 * @param intervals Input: Number of intervals for the deformation
	 * @param grad Output: Gradient of the function
	 * @return error function value
	 */

	private double evaluateConsistency(
			final double []c_direct,
			final double []c_inverse,
			final int intervals,
			double []grad)
	{

		int cYdim = intervals+3;
		int cXdim = cYdim;
		int Nk = cYdim * cXdim;
		int twiceNk = 2 * Nk;

		// Initialize gradient
		for (int k=0; k<grad.length; k++)
			grad[k]=0.0F;

		// Compute the deformation
		// Set these coefficients to an interpolator
		BSplineModel swx_direct = this.swxTargetToSource; 
		BSplineModel swy_direct = this.swyTargetToSource;  

		BSplineModel swx_inverse = this.swxSourceToTarget; 
		BSplineModel swy_inverse = this.swySourceToTarget; 


		// Set the transformation coefficients to the interpolator
		swx_direct.setCoefficients(c_direct, cYdim, cXdim, 0);
		swy_direct.setCoefficients(c_direct, cYdim, cXdim, Nk);
		swx_inverse.setCoefficients(c_inverse, cYdim, cXdim, 0);
		swy_inverse.setCoefficients(c_inverse, cYdim, cXdim, Nk);

		// *********** Compute the geometric error and gradient (DIRECT) ***********       
		double f_direct = 0;
		int n_direct = 0;
		for (int v=0; v<this.targetCurrentHeight; v++)
			for (int u=0; u<this.targetCurrentWidth; u++)
			{
				// Check if this point is in the target mask
				if (this.targetMsk.getValue(u/this.targetFactorWidth, v/this.targetFactorHeight))
				{

					final int x = (int) Math.round(swx_direct.precomputed_interpolateI(u,v));
					final int y = (int) Math.round(swy_direct.precomputed_interpolateI(u,v));

					if (x>=0 && x<this.sourceCurrentWidth && y>=0 && y<this.sourceCurrentHeight)
					{
						final double x2 = swx_inverse.precomputed_interpolateI(x,y);
						final double y2 = swy_inverse.precomputed_interpolateI(x,y);
						double aux1 = u - x2;
						double aux2 = v - y2;

						f_direct += aux1 * aux1 + aux2 * aux2;

						// Compute the derivative with respect to all the c coefficients
						// Derivatives from direct coefficients.
						for (int l=0; l<4; l++)
							for (int m=0; m<4; m++)
							{
								if (swx_direct.prec_yIndex[v][l]==-1 || swx_direct.prec_xIndex[u][m]==-1)
									continue;

								double dddx = swx_direct.precomputed_getWeightI(l, m, u, v);
								double dixx = swx_inverse.precomputed_getWeightDx(l, m, x, y);
								double diyy = swy_inverse.precomputed_getWeightDy(l, m, x, y);

								double weightIx = (dixx + diyy) * dddx;

								double dddy = swy_direct.precomputed_getWeightI(l, m, u, v);
								double dixy = swx_inverse.precomputed_getWeightDy(l, m, x, y);
								double diyx = swy_inverse.precomputed_getWeightDx(l, m, x, y);

								double weightIy = (diyx + dixy) * dddy;

								int k = swx_direct.prec_yIndex[v][l] * cYdim + swx_direct.prec_xIndex[u][m];

								// Derivative related to X deformation
								grad[k]        += -aux1 * weightIx;

								// Derivative related to Y deformation
								grad[k+twiceNk]+= -aux2 * weightIy;
							}


						// Derivatives from inverse coefficients.
						for (int l=0; l<4; l++)
							for (int m=0; m<4; m++)
							{
								// d inverse(direct(x)) / d c_inverse
								if (swx_inverse.prec_yIndex[y][l]==-1 || swx_inverse.prec_xIndex[x][m]==-1)
									continue;

								double weightI = swx_inverse.precomputed_getWeightI(l, m, x, y);

								int k = swx_inverse.prec_yIndex[y][l] * cYdim + swx_inverse.prec_xIndex[x][m];

								// Derivative related to X deformation
								grad[k+Nk]        += -aux1 * weightI;

								// Derivative related to Y deformation
								grad[k+Nk+twiceNk]+= -aux2 * weightI;
							}


						n_direct++; // Another point has been successfully evaluated
					}
				}// end if mask.
			}
		if(n_direct != 0)
			f_direct /= (double) n_direct;

		// Average the image related terms
		if (n_direct != 0)
		{
			double aux = consistencyWeight * 2.0 / n_direct;  // This is the 2 coming from the
			// derivative that I would do later
			for (int k=0; k<grad.length; k++)
				grad[k] *= aux;
		}

		// Inverse gradient
		double []vgrad = new double[grad.length];

		// Initialize gradient
		for (int k=0; k<vgrad.length; k++)
			vgrad[k] = 0.0F;

		// *********** Compute the geometric error and gradient (INVERSE) ***********
		double f_inverse = 0;
		int n_inverse = 0;
		for (int v=0; v<this.sourceCurrentHeight; v++)
			for (int u=0; u<this.sourceCurrentWidth; u++)
			{
				// Check if this point is in the target mask
				if (this.sourceMsk.getValue(u/this.sourceFactorWidth, v/this.sourceFactorHeight))
				{
					final int x = (int) Math.round( swx_inverse.precomputed_interpolateI(u, v));
					final int y = (int) Math.round( swy_inverse.precomputed_interpolateI(u, v));

					if (x>=0 && x<this.targetCurrentWidth && y>=0 && y<this.targetCurrentHeight)
					{
						final double x2 = swx_direct.precomputed_interpolateI(x, y);
						final double y2 = swy_direct.precomputed_interpolateI(x, y);
						double aux1 = u - x2;
						double aux2 = v - y2;

						f_inverse += aux1 * aux1 + aux2 * aux2;

						// Compute the derivative with respect to all the c coefficients
						// Derivatives from direct coefficients.
						for (int l=0; l<4; l++)
							for (int m=0; m<4; m++)
							{
								// d direct(inverse(x)) / d c_direct
								if (swx_direct.prec_yIndex[y][l]==-1 || swx_direct.prec_xIndex[x][m]==-1)
									continue;

								double weightI = swx_direct.precomputed_getWeightI(l, m, x, y);

								int k = swx_direct.prec_yIndex[y][l] * cYdim + swx_direct.prec_xIndex[x][m];

								// Derivative related to X deformation
								vgrad[k]        += -aux1 * weightI;

								// Derivative related to Y deformation
								vgrad[k+twiceNk]+= -aux2 * weightI;
							}
						// Derivatives from inverse coefficients.
						for (int l=0; l<4; l++)
							for (int m=0; m<4; m++)
							{
								if (swx_inverse.prec_yIndex[v][l]==-1 || swx_inverse.prec_xIndex[u][m]==-1)
									continue;

								double diix = swx_inverse.precomputed_getWeightI(l, m, u, v);
								double ddxx = swx_direct.precomputed_getWeightDx(l, m, x, y);
								double ddyy = swy_direct.precomputed_getWeightDy(l, m, x, y);

								double weightIx = (ddxx + ddyy) * diix;

								double diiy = swy_inverse.precomputed_getWeightI(l, m, u, v);
								double ddxy = swx_direct.precomputed_getWeightDy(l, m, x, y);
								double ddyx = swy_direct.precomputed_getWeightDx(l, m, x, y);

								double weightIy = (ddyx + ddxy) * diiy;


								int k = swx_inverse.prec_yIndex[v][l] * cYdim + swx_inverse.prec_xIndex[u][m];

								// Derivative related to X deformation
								vgrad[k+Nk]        += -aux1 * weightIx;

								// Derivative related to Y deformation
								vgrad[k+Nk+twiceNk]+= -aux2 * weightIy;
							}

						n_inverse++; // Another point has been successfully evaluated
					}
				} // end if mask
			}

		if(n_inverse != 0)
			f_inverse /= (double) n_inverse;


		// Average the image related terms
		if (n_inverse != 0)
		{
			double aux = consistencyWeight * 2.0 / n_inverse;  // This is the 2 coming from the
			// derivative that I would do later
			for(int k=0; k<vgrad.length; k++)
				vgrad[k] *= aux;
		}

		// Sum of both gradients (direct and inverse)
		for(int k=0; k<grad.length; k++)
			grad[k] += vgrad[k];


		this.partialDirectConsitencyError = f_direct;
		this.partialInverseConsitencyError = f_inverse;

		double consistencyDirectError = (n_direct == 0) ? 1.0/FLT_EPSILON : (this.consistencyWeight * f_direct);
		double consistencyInverseError = (n_inverse == 0) ? 1.0/FLT_EPSILON : (this.consistencyWeight * f_inverse);

		if (showMarquardtOptim)
		{
			IJ.write("    Consistency Error (s-t): " + consistencyDirectError);
			IJ.write("    Consistency Error (t-s): " + consistencyInverseError);
		}


		if(n_direct == 0 || n_inverse == 0)
			return 1/FLT_EPSILON;
		return (this.consistencyWeight * (f_direct + f_inverse));
	}

	/*--------------------------------------------------------------------------*/
	/**
	 * Energy function to be minimized by the optimizer in the bidirectional case.
	 *
	 * @param c Input: Deformation coefficients
	 * @param intervals Input: Number of intervals for the deformation
	 * @param grad Output: Gradient of the function
	 * @param only_image Input: if true, only the image term is considered and not the regularization
	 * @param show_error Input: if true, an image is shown with the error
	 * @return value of the energy function for these deformation coefficients
	 */
	private double energyFunction(
			final double []c,
			final int      intervals,
			double []grad,
			final boolean  only_image,
			final boolean  show_error)
	{
		final int M = c.length / 2;
		final int halfM = M / 2;
		double []x1 = new double [M];
		double []auxGrad1 = new double [M];
		double []auxGrad2 = new double [M];

		for(int i = 0, p = 0; i<halfM; i++, p++)
		{
			x1[p] = c[i];
			x1[p + halfM] = c[i + M];
		}

		// Source to Target evaluation (Similarity + Landmarks + Regularization)
		//double f = evaluateSimilarity(x1, intervals, auxGrad1, only_image, show_error, false);
		double f = evaluateSimilarityMultiThread(x1, intervals, auxGrad1, only_image, false);

		double []x2 = new double [M];
		for(int i = halfM, p = 0; i<M; i++, p++)
		{
			x2[p] = c[i];
			x2[p + halfM] = c[i + M];
		}

		// Target to Source evaluation (Similarity + Landmarks + Regularization)
		//f += evaluateSimilarity(x2, intervals, auxGrad2, only_image, show_error, true);
		f += evaluateSimilarityMultiThread(x2, intervals, auxGrad2, only_image, true);

		// Gradient composition.
		for(int i = 0, p = 0; i<halfM; i++, p++)
		{
			grad[p] = auxGrad1[i];
			grad[p + halfM] = auxGrad2[i];
			grad[p + M] = auxGrad1[i + halfM];
			grad[p + M + halfM] = auxGrad2[i + halfM];
		}

		double f_consistency = 0;

		// Consistency term
		if(this.consistencyWeight != 0)
		{
			// Consistency gradient.
			double []vgradcons = new double[grad.length];

			//f_consistency = evaluateConsistency(intervals, vgradcons);
			f_consistency = evaluateConsistencyMultiThread(intervals, vgradcons);

			// Update gradient.
			for(int i = 0; i < grad.length; i++)
				grad[i] += vgradcons[i];
		}

		return f + f_consistency;
	
	}

	/*--------------------------------------------------------------------------*/
	/**
	 * Evaluate the similarity between the source and the target images but also
	 * the transformation regularization and and landmarks energy term if necessary.
	 *
	 * @param c Input: Deformation coefficients
	 * @param intervals Input: Number of intervals for the deformation
	 * @param grad Output: Gradient of the similarity
	 * @param only_image Input: if true, only the image term is considered and not the regularization
	 * @param show_error Input: if true, an image is shown with the error
	 * @param bIsReverse Input: flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 * @return images similarity value
	 * 
	 * @deprecated
	 */
	private double evaluateSimilarity(
			final double []c,
			final int      intervals,
			double []grad,
			final boolean  only_image,

			final boolean  show_error,
			boolean bIsReverse)
	{

		// Auxiliary variables for changing from source to target and inversely
		BSplineModel auxTarget = target;
		BSplineModel auxSource = source;

		Mask auxTargetMsk = targetMsk;
		Mask auxSourceMsk = sourceMsk;

		PointHandler auxTargetPh = targetPh;
		PointHandler auxSourcePh = sourcePh;

		BSplineModel swx = swxTargetToSource;
		BSplineModel swy = swyTargetToSource;

		double auxFactorWidth = this.target.getFactorWidth();
		double auxFactorHeight = this.target.getFactorHeight();

		double P11[][] = this.P11_TargetToSource;
		double P12[][] = this.P12_TargetToSource;
		double P22[][] = this.P22_TargetToSource;

		int auxTargetCurrentWidth = this.targetCurrentWidth;
		int auxTargetCurrentHeight = this.targetCurrentHeight;

		// Change if necessary
		if(bIsReverse)
		{
			auxSource = target;
			auxTarget = source;

			auxSourceMsk = targetMsk;
			auxTargetMsk = sourceMsk;

			auxSourcePh = targetPh;
			auxTargetPh = sourcePh;

			swx = swxSourceToTarget;
			swy = swySourceToTarget;

			auxFactorWidth = this.sourceFactorWidth;
			auxFactorHeight = this.sourceFactorHeight;

			P11 = this.P11_SourceToTarget;
			P12 = this.P12_SourceToTarget;
			P22 = this.P22_SourceToTarget;

			auxTargetCurrentWidth = this.sourceCurrentWidth;
			auxTargetCurrentHeight = this.sourceCurrentHeight;
		}

		int cYdim = intervals+3;
		int cXdim = cYdim;
		int Nk = cYdim * cXdim;
		int twiceNk = 2 * Nk;
		double []vgradreg = new double[grad.length];
		double []vgradland = new double[grad.length];

		// Set the transformation coefficients to the interpolator
		swx.setCoefficients(c, cYdim, cXdim, 0);
		swy.setCoefficients(c, cYdim, cXdim, Nk);

		// Initialize gradient
		for (int k=0; k<twiceNk; k++) vgradreg[k]=vgradland[k]=grad[k]=0.0F;

		// Estimate the similarity and gradient between both images
		double imageSimilarity=0.0;
		int Ydim = auxTarget.getCurrentHeight();
		int Xdim = auxTarget.getCurrentWidth();

		// Prepare to show
		double [][]error_image=null;
		double [][]div_error_image=null;
		double [][]curl_error_image=null;
		double [][]laplacian_error_image=null;
		double [][]jacobian_error_image=null;
		if (show_error)
		{
			error_image = new double[Ydim][Xdim];
			div_error_image = new double[Ydim][Xdim];
			curl_error_image = new double[Ydim][Xdim];
			laplacian_error_image = new double[Ydim][Xdim];
			jacobian_error_image = new double[Ydim][Xdim];
			for (int v=0; v<Ydim; v++)
				for (int u=0; u<Xdim; u++)
					error_image[v][u]=div_error_image[v][u]=curl_error_image[v][u]=
						laplacian_error_image[v][u]=jacobian_error_image[v][u]=-1.0;
		}

		// Loop over all points in the source image
		int n=0;
		if (imageWeight!=0 || show_error)
		{
			double []xD2 = new double[3]; // Some space for the second derivatives
			double []yD2 = new double[3]; // of the transformation
			double []xD  = new double[2]; // Some space for the second derivatives
			double []yD  = new double[2]; // of the transformation
			double []I1D = new double[2]; // Space for the first derivatives of I1
			double hx = (Xdim-1)/intervals;   // Scale in the X axis
			double hy = (Ydim-1)/intervals;   // Scale in the Y axis

			double []targetCurrentImage = auxTarget.getCurrentImage();


			int uv=0;
			for (int v=0; v<Ydim; v++)
			{
				for (int u=0; u<Xdim; u++, uv++)
				{
					// Compute image term .....................................................

					// Check if this point is in the target mask
					if (auxTargetMsk.getValue(u/auxFactorWidth, v/auxFactorHeight))
					{
						// Compute value in the source image
						double I2 = targetCurrentImage[uv];

						// Compute the position of this point in the target
						double x = swx.precomputed_interpolateI(u,v);
						double y = swy.precomputed_interpolateI(u,v);

						// Check if this point is in the source mask
						if (auxSourceMsk.getValue(x/auxFactorWidth, y/auxFactorHeight))
						{
							// Compute the value of the target at that point
							auxSource.prepareForInterpolation(x, y, PYRAMID);
							double I1 = auxSource.interpolateI();
							auxSource.interpolateD(I1D);
							double I1dx = I1D[0], I1dy = I1D[1];

							double error = I2 - I1;
							double error2 = error*error;
							if (show_error) error_image[v][u]=error;
							imageSimilarity+=error2;

							// Compute the derivative with respect to all the c coefficients
							// Cost of the derivatives = 16*(3 mults + 2 sums)
							// Current cost= 359 mults + 346 sums
							for (int l=0; l<4; l++)
								for (int m=0; m<4; m++)
								{
									if (swx.prec_yIndex[v][l]==-1 || swx.prec_xIndex[u][m]==-1) continue;

									// Note: It's the same to take the indexes and weightI from swx than from swy
									double weightI = swx.precomputed_getWeightI(l,m,u,v);

									int k = swx.prec_yIndex[v][l] * cYdim + swx.prec_xIndex[u][m];

									// Compute partial result
									// There's also a multiplication by 2 that I will
									// do later
									double aux = -error * weightI;

									// Derivative related to X deformation
									grad[k]   += aux*I1dx;

									// Derivative related to Y deformation
									grad[k+Nk]+= aux*I1dy;
								}
							n++; // Another point has been successfully evaluated
						}
					}

					// Show regularization images ...........................................
					if (show_error)
					{
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

						double graddiv = graddivx*graddivx + graddivy*graddivy;
						double errorgraddiv = divWeight*graddiv;

						if (divWeight!=0) div_error_image [v][u]=errorgraddiv;
						else              div_error_image [v][u]=graddiv;

						// Compute error in the curl
						gradcurlx = -xdxdy+ydxdx;
						gradcurly = -xdydy+ydxdy;
						double gradcurl = gradcurlx*gradcurlx + gradcurly*gradcurly;
						double errorgradcurl = curlWeight*gradcurl;

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
		if (n!=0)
		{
			imageSimilarity *= imageWeight/n;
			double aux = imageWeight * 2.0/n; // This is the 2 coming from the
			// derivative that I would do later
			for (int k=0; k<twiceNk; k++) grad[k]*=aux;
		} else
			if (imageWeight==0) imageSimilarity = 0;
			else                imageSimilarity = 1/FLT_EPSILON;

		// Compute regularization term ..............................................
		double regularization = 0.0;
		if (!only_image)
		{
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
		if (auxTargetPh!=null) K = auxTargetPh.getPoints().size();
		if (landmarkWeight != 0)
		{
			Vector <Point> sourceVector=null;
			if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
			else                   sourceVector = new Vector <Point> ();
			Vector <Point> targetVector = null;
			if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
			else                   targetVector = new Vector <Point> ();

			for (int kp=0; kp<K; kp++)
			{
				// Get the landmark coordinate in the target image
				final Point sourcePoint = (Point)sourceVector.elementAt(kp);
				final Point targetPoint = (Point)targetVector.elementAt(kp);
				double u = auxFactorWidth *(double)targetPoint.x;
				double v = auxFactorHeight*(double)targetPoint.y;

				// Express it in "spline" units
				double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth  - 1) + 1.0F;
				double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;

				// Transform this coordinate to the source image
				swx.prepareForInterpolation(tu, tv, false);
				double x=swx.interpolateI();
				swy.prepareForInterpolation(tu, tv, false);
				double y=swy.interpolateI();

				// Substract the result from the residual
				double dx=auxFactorWidth *(double)sourcePoint.x - x;
				double dy=auxFactorHeight*(double)sourcePoint.y - y;

				// Add to landmark error
				landmarkError += dx*dx + dy*dy;

				// Compute the derivative with respect to all the c coefficients
				for (int l=0; l<4; l++)
					for (int m=0; m<4; m++)
					{
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

		if (K!=0)
		{
			landmarkError *= landmarkWeight/K;
			double aux = 2.0 * landmarkWeight/K;
			// This is the 2 coming from the derivative
			// computation that I would do at the end
			for (int k=0; k<twiceNk; k++) 
				vgradland[k] *= aux;
		}
		if (only_image) landmarkError = 0;

		// Finish computations .............................................................
		// Add all gradient terms
		for (int k=0; k<twiceNk; k++)
			grad[k]+=vgradreg[k]+vgradland[k];

		if (show_error)
		{
			MiscTools.showImage("Error",error_image);
			MiscTools.showImage("Divergence Error",div_error_image);
			MiscTools.showImage("Curl Error",curl_error_image);
			MiscTools.showImage("Laplacian Error",laplacian_error_image);
			MiscTools.showImage("Jacobian Error",jacobian_error_image);
		}

		if (showMarquardtOptim)
		{
			String s = bIsReverse ? new String("(t-s)") : new String("(s-t)");
			if (imageWeight != 0) 
			{
				IJ.write("    Image          error " + s + ": " + imageSimilarity);
				if(bIsReverse)
					this.partialInverseSimilarityError = imageSimilarity;
				else
					this.partialDirectSimilarityError = imageSimilarity;

			}
			if (landmarkWeight != 0)               
			{
				IJ.write("    Landmark       error " + s + ": " + landmarkError);
				if(bIsReverse)
					this.partialInverseLandmarkError = landmarkError;
				else
					this.partialDirectLandmarkError = landmarkError;
			}
			if (divWeight != 0 || curlWeight != 0)
			{
				IJ.write("    Regularization error " + s + ": " + regularization);
				if(bIsReverse)
					this.partialInverseRegularizationError = regularization;
				else
					this.partialDirectRegularizationError = regularization;
					
			}
		}
		return imageSimilarity + landmarkError + regularization;
	}

	/*--------------------------------------------------------------------------*/
	/**
	 * Evaluate the energy function in one direction (direct or inverse) and
	 * calculates its gradient.
	 * <p>Energy function:</p>
	 * <p>E = w_i * E_similarity + w_l * E_landmarks + (w_r * E_rotational + w_d * E_divergence) + w_c * E_consistency</p>
	 *
	 * @param c Input: Deformation coefficients
	 * @param intervals Input: Number of intervals for the deformation
	 * @param grad_direct Output: Gradient of the energy function (direct direction)
	 * @param grad_inverse Output: Gradient of the energy function (inverse direction)
	 * @param only_image Input: if true, only the image term is considered and not the regularization
	 * @param show_error Input: if true, an image is shown with the error
	 * @param bIsReverse Input: flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 * @return images similarity value
	 */
	private double evaluatePartialEnergy(
			final double []c,
			final int      intervals,
			double []grad_direct,
			double []grad_inverse,
			final boolean  only_image,

			final boolean  show_error,
			boolean bIsReverse)
	{

		// Auxiliary variables for changing from source to target and inversely
		BSplineModel auxTarget = target;
		BSplineModel auxSource = source;

		Mask auxTargetMsk = targetMsk;
		Mask auxSourceMsk = sourceMsk;

		PointHandler auxTargetPh = targetPh;
		PointHandler auxSourcePh = sourcePh;

		BSplineModel swx = swxTargetToSource;
		BSplineModel swy = swyTargetToSource;
		
		BSplineModel swx_inverse = swxSourceToTarget;
		BSplineModel swy_inverse = swySourceToTarget;

		double auxFactorWidth = this.target.getFactorWidth();
		double auxFactorHeight = this.target.getFactorHeight();

		double P11[][] = this.P11_TargetToSource;
		double P12[][] = this.P12_TargetToSource;
		double P22[][] = this.P22_TargetToSource;

		int auxTargetCurrentWidth = this.targetCurrentWidth;
		int auxTargetCurrentHeight = this.targetCurrentHeight;

		// Change if necessary
		if(bIsReverse)
		{
			auxSource = target;
			auxTarget = source;

			auxSourceMsk = targetMsk;
			auxTargetMsk = sourceMsk;

			auxSourcePh = targetPh;
			auxTargetPh = sourcePh;

			swx = swxSourceToTarget;
			swy = swySourceToTarget;
			
			swx_inverse = swxTargetToSource;
			swy_inverse = swyTargetToSource;

			auxFactorWidth = this.sourceFactorWidth;
			auxFactorHeight = this.sourceFactorHeight;

			P11 = this.P11_SourceToTarget;
			P12 = this.P12_SourceToTarget;
			P22 = this.P22_SourceToTarget;

			auxTargetCurrentWidth = this.sourceCurrentWidth;
			auxTargetCurrentHeight = this.sourceCurrentHeight;
		}

		int cYdim = intervals+3;
		int cXdim = cYdim;
		int Nk = cYdim * cXdim;
		int twiceNk = 2 * Nk;
		double []vgradimg = new double[twiceNk];
		double []vgradcons = new double[twiceNk];
		double []vgradreg = new double[twiceNk];
		double []vgradland = new double[twiceNk];

		// Set the transformation coefficients to the interpolator
		swx.setCoefficients(c, cYdim, cXdim, 0);
		swy.setCoefficients(c, cYdim, cXdim, Nk);

		// Initialize gradient
		for (int k=0; k<twiceNk; k++) 
			vgradcons[k] = vgradreg[k] = vgradland[k] = vgradimg[k] = grad_direct[k] =grad_inverse[k] = 0.0F;

		// Estimate the energy and gradient between both images
		double imageSimilarity = 0.0;
		double consistencyError = 0.0;
		int Ydim = auxTarget.getCurrentHeight();
		int Xdim = auxTarget.getCurrentWidth();

		// Prepare to show
		double [][]error_image = null;
		double [][]div_error_image = null;
		double [][]curl_error_image = null;
		double [][]laplacian_error_image = null;
		double [][]jacobian_error_image = null;
		if (show_error)
		{
			error_image = new double[Ydim][Xdim];
			div_error_image = new double[Ydim][Xdim];
			curl_error_image = new double[Ydim][Xdim];
			laplacian_error_image = new double[Ydim][Xdim];
			jacobian_error_image = new double[Ydim][Xdim];
			for (int v=0; v<Ydim; v++)
				for (int u=0; u<Xdim; u++)
					error_image[v][u]=div_error_image[v][u]=curl_error_image[v][u]=
						laplacian_error_image[v][u]=jacobian_error_image[v][u]=-1.0;
		}

		// Loop over all points in the source image
		int n = 0;
		if (imageWeight!=0 || show_error)
		{
			double []xD2 = new double[3]; // Some space for the second derivatives
			double []yD2 = new double[3]; // of the transformation
			double []xD  = new double[2]; // Some space for the second derivatives
			double []yD  = new double[2]; // of the transformation
			double []I1D = new double[2]; // Space for the first derivatives of I1
			double hx = (Xdim-1)/intervals;   // Scale in the X axis
			double hy = (Ydim-1)/intervals;   // Scale in the Y axis

			double []targetCurrentImage = auxTarget.getCurrentImage();


			int uv=0;
			for (int v=0; v<Ydim; v++)
			{
				for (int u=0; u<Xdim; u++, uv++)
				{
					// Compute similarity and consistency terms .....................................................

					// Check if this point is in the target mask
					if (auxTargetMsk.getValue(u/auxFactorWidth, v/auxFactorHeight))
					{
						// Compute value in the source image
						double I2 = targetCurrentImage[uv];

						// Compute the position of this point in the target
						double x = swx.precomputed_interpolateI(u,v);
						double y = swy.precomputed_interpolateI(u,v);
						
						final int ix = (int)Math.round(x);
						final int iy = (int)Math.round(y);

						// Check if this point is in the source mask
						if (auxSourceMsk.getValue((double)ix/auxFactorWidth, (double)iy/auxFactorHeight))
						{
							// Similarity term: Compute the image value of the target at that point
							auxSource.prepareForInterpolation(x, y, PYRAMID);
							double I1 = auxSource.interpolateI();
							auxSource.interpolateD(I1D);
							double I1dx = I1D[0], I1dy = I1D[1];

							double error = I2 - I1;
							double error2 = error * error;
							if (show_error) 
								error_image[v][u] = error;
							imageSimilarity += error2;

							// Consistency term: Compute coordinate applying the inverse transformation.														
							final double x2 = swx_inverse.precomputed_interpolateI(ix, iy);
							final double y2 = swy_inverse.precomputed_interpolateI(ix, iy);
							
							double aux1 = u - x2;
							double aux2 = v - y2;

							consistencyError += aux1 * aux1 + aux2 * aux2;
						
							
							// Compute the derivative with respect to all the c coefficients							
							for (int l=0; l<4; l++)
								for (int m=0; m<4; m++)
								{
									if (swx.prec_yIndex[v][l]==-1 || swx.prec_xIndex[u][m]==-1) continue;

									// --- Similarity term derivative ---
									
									// Note: It's the same to take the indexes and weightI from swx as from swy
									double weightI = swx.precomputed_getWeightI(l,m,u,v);

									int k = swx.prec_yIndex[v][l] * cYdim + swx.prec_xIndex[u][m];

									// Compute partial result
									// There's also a multiplication by 2 that I will
									// do later
									double aux = -error * weightI;

									// Derivative related to X deformation
									vgradimg[k]   += aux*I1dx;

									// Derivative related to Y deformation
									vgradimg[k+Nk]+= aux*I1dy;
									
									// --- Consistency term derivative ---
									// Compute the derivative with respect to all the c coefficients
									// Derivatives from direct coefficients.
									double dddx = weightI;
									double dixx = swx_inverse.precomputed_getWeightDx(l, m, ix, iy);
									double diyy = swy_inverse.precomputed_getWeightDy(l, m, ix, iy);

									double weightIx = (dixx + diyy) * dddx;

									double dddy = weightI;
									double dixy = swx_inverse.precomputed_getWeightDy(l, m, ix, iy);
									double diyx = swy_inverse.precomputed_getWeightDx(l, m, ix, iy);

									double weightIy = (diyx + dixy) * dddy;

									// Derivative related to X deformation
									vgradcons[k]   += -aux1 * weightIx;

									// Derivative related to Y deformation
									vgradcons[k+Nk]+= -aux2 * weightIy;
								}
							
							// Consistency term: Derivatives from inverse coefficients.
							for (int l=0; l<4; l++)
								for (int m=0; m<4; m++)
								{
									// d inverse(direct(x)) / d c_inverse
									if (swx_inverse.prec_yIndex[iy][l]==-1 || swx_inverse.prec_xIndex[ix][m]==-1)
										continue;

									double weightI = swx_inverse.precomputed_getWeightI(l, m, ix, iy);

									int k = swx_inverse.prec_yIndex[iy][l] * cYdim + swx_inverse.prec_xIndex[ix][m];

									// Derivative related to X deformation
									grad_inverse[k]    += -aux1 * weightI;

									// Derivative related to Y deformation
									grad_inverse[k+Nk] += -aux2 * weightI;
								}
							
							n++; // Another point has been successfully evaluated
						}
					}

					// Show regularization images ...........................................
					if (show_error)
					{
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

						double graddiv = graddivx*graddivx + graddivy*graddivy;
						double errorgraddiv = divWeight*graddiv;

						if (divWeight!=0) div_error_image [v][u]=errorgraddiv;
						else              div_error_image [v][u]=graddiv;

						// Compute error in the curl
						gradcurlx = -xdxdy+ydxdx;
						gradcurly = -xdydy+ydxdy;
						double gradcurl = gradcurlx*gradcurlx + gradcurly*gradcurly;
						double errorgradcurl = curlWeight*gradcurl;

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

				
		
		if (n!=0)
		{
			// Average the consistency related terms
			consistencyError *= this.consistencyWeight / n;

			// Average the image related terms
			double aux = this.consistencyWeight * 2.0 / n;  // This is the 2 coming from the
															 // derivative that I would do later
			for (int k=0; k<vgradcons.length; k++)
			{
				vgradcons[k] *= aux;
				grad_inverse[k] *= aux;
			}
		
			
			// Average the image related terms
			imageSimilarity *= this.imageWeight/n;
			aux = imageWeight * 2.0/n; 	// This is the 2 coming from the
												// derivative that I would do later
			for (int k=0; k<twiceNk; k++) 
				vgradimg[k]*=aux;
		} 
		else
		{
			if (this.imageWeight==0) 
				imageSimilarity = 0;
			else                
				imageSimilarity = 1/FLT_EPSILON;
			
			if(this.consistencyWeight == 0) 
				consistencyError = 0;
			else 
				consistencyError = 1/FLT_EPSILON;
		}

		// Compute regularization term ..............................................
		double regularization = 0.0;
		if (!only_image)
		{
			for (int i=0; i<Nk; i++)
				for (int j=0; j<Nk; j++) 
				{
					regularization += c[   i]*P11[i][j]*c[   j]+// c1^t P11 c1
									  c[Nk+i]*P22[i][j]*c[Nk+j]+// c2^t P22 c2
									  c[   i]*P12[i][j]*c[Nk+j];// c1^t P12 c2
					vgradreg[   i] += 2*P11[i][j]*c[j];         // 2 P11 c1
					vgradreg[Nk+i] += 2*P22[i][j]*c[Nk+j];      // 2 P22 c2
					vgradreg[   i] +=  P12[i][j]*c[Nk+j];      //   P12 c2
					vgradreg[Nk+i] +=  P12[j][i]*c[   j];      //   P12^t c1
				}
			regularization *= 1.0/(Ydim*Xdim);
			
			for (int k=0; k<twiceNk; k++) 
				vgradreg [k] *= 1.0/(Ydim*Xdim);
		}

		// Compute landmark error and derivative ...............................
		// Get the list of landmarks
		double landmarkError=0.0;
		int K = 0;
		if (auxTargetPh!=null) K = auxTargetPh.getPoints().size();
		if (landmarkWeight != 0)
		{
			Vector <Point> sourceVector=null;
			if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
			else                   sourceVector = new Vector <Point> ();
			Vector <Point> targetVector = null;
			if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
			else                   targetVector = new Vector <Point> ();

			for (int kp=0; kp<K; kp++)
			{
				// Get the landmark coordinate in the target image
				final Point sourcePoint = (Point)sourceVector.elementAt(kp);
				final Point targetPoint = (Point)targetVector.elementAt(kp);
				double u = auxFactorWidth *(double)targetPoint.x;
				double v = auxFactorHeight*(double)targetPoint.y;

				// Express it in "spline" units
				double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth  - 1) + 1.0F;
				double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;

				// Transform this coordinate to the source image
				swx.prepareForInterpolation(tu, tv, false);
				double x=swx.interpolateI();
				swy.prepareForInterpolation(tu, tv, false);
				double y=swy.interpolateI();

				// Substract the result from the residual
				double dx=auxFactorWidth *(double)sourcePoint.x - x;
				double dy=auxFactorHeight*(double)sourcePoint.y - y;

				// Add to landmark error
				landmarkError += dx*dx + dy*dy;

				// Compute the derivative with respect to all the c coefficients
				for (int l=0; l<4; l++)
					for (int m=0; m<4; m++)
					{
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

		if (K!=0)
		{
			landmarkError *= landmarkWeight/K;
			double aux = 2.0 * landmarkWeight/K;
			// This is the 2 coming from the derivative
			// computation that I would do at the end
			for (int k=0; k<twiceNk; k++) 
				vgradland[k] *= aux;
		}
		if (only_image) landmarkError = 0;

		// Finish computations .............................................................
		// Add all gradient terms
		for (int k=0; k<twiceNk; k++)
			grad_direct[k] += vgradimg[k] +  + vgradcons[k] + vgradreg[k] + vgradland[k];

		if (show_error)
		{
			MiscTools.showImage("Error",error_image);
			MiscTools.showImage("Divergence Error",div_error_image);
			MiscTools.showImage("Curl Error",curl_error_image);
			MiscTools.showImage("Laplacian Error",laplacian_error_image);
			MiscTools.showImage("Jacobian Error",jacobian_error_image);
		}

		if (showMarquardtOptim)
		{
			String s = bIsReverse ? new String("(t-s)") : new String("(s-t)");
			if (imageWeight != 0) 
			{
				IJ.write("    Image          error " + s + ": " + imageSimilarity);
				if(bIsReverse)
					this.partialInverseSimilarityError = imageSimilarity;
				else
					this.partialDirectSimilarityError = imageSimilarity;

			}
			if (consistencyWeight != 0) 
			{
				IJ.write("    Consistency          error " + s + ": " + consistencyError);
				if(bIsReverse)
					this.partialInverseConsitencyError = consistencyError;
				else
					this.partialDirectConsitencyError = consistencyError;

			}
			if (landmarkWeight != 0)               
			{
				IJ.write("    Landmark       error " + s + ": " + landmarkError);
				if(bIsReverse)
					this.partialInverseLandmarkError = landmarkError;
				else
					this.partialDirectLandmarkError = landmarkError;
			}
			if (divWeight != 0 || curlWeight != 0)
			{
				IJ.write("    Regularization error " + s + ": " + regularization);
				if(bIsReverse)
					this.partialInverseRegularizationError = regularization;
				else
					this.partialDirectRegularizationError = regularization;
					
			}
		}
		
		return imageSimilarity + consistencyError + landmarkError + regularization;
		
	} /* end evaluatePartialEnergy */
	
	
	/*--------------------------------------------------------------------------*/
	/**
	 * In this function the system (H+lambda*Diag(H))*update=gradient
	 *     is solved for update.
	 *     H is the hessian of the function f,
	 *     gradient is the gradient of the function f,
	 *     Diag(H) is a matrix with the diagonal of H.
	 */
	private void Marquardt_it (
			double   []x,
			boolean  []optimize,
			double   []gradient,
			double   []Hessian,
			double     lambda)
	{
//		final double TINY  = FLT_EPSILON;
		final int   M      = x.length;

		// Find the threshold for the most important components
		double []sortedgradient= new double [M];
		for (int i = 0; i < M; i++)
			sortedgradient[i] = Math.abs(gradient[i]);
		Arrays.sort(sortedgradient);

		double largestGradient = sortedgradient[M-1];

		// We set the threshold gradient at 9% of the largest value.
		double gradient_th = 0.09 * largestGradient;

		// We count the number of values over the threshold.
		int Mused = 0;
		for(int i = 0; i < M; i++)
			if(sortedgradient[i] >= gradient_th)
				Mused++;


		double [][] u         = new double  [Mused][Mused];
//		double [][] v         = null; //new double  [Mused][Mused];
//		double   [] w         = null; //new double  [Mused];
		double   [] g         = new double  [Mused];
		double   [] update    = new double  [Mused];
		boolean  [] optimizep = new boolean [M];

		System.arraycopy(optimize,0,optimizep,0,M);

		lambda+=1.0F;


		int m = 0, i;

		// Take the Mused components with big gradients
		for (i=0; i<M; i++)
			if  (optimizep[i] && Math.abs(gradient[i])>=gradient_th) {
				m++;
				if (m==Mused) break;
			}
			else
				optimizep[i]=false;
		// Set the rest to 0
		for (i=i+1; i<M; i++)
			optimizep[i]=false;


		// Gradient descent
		//for (int i=0; i<M; i++) if (optimizep[i]) x[i]-=0.01*gradient[i];
		//if (true) return;

		/* u will be a copy of the Hessian where we take only those
           components corresponding to variables being optimized */
		int kr=0, iw=0;
		for (int ir = 0; ir<M; kr=kr+M,ir++)
		{
			if (optimizep[ir])
			{
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
		update = MathTools.linearLeastSquares(u,g);
		if(update == null)
		{
			IJ.log("Error when calculating linear least square solution...");
			return;
		}

		/* x = x - update */
		kr=0;
		for (int kw = 0; kw<M; kw++)
			if (optimizep[kw]) x[kw] -=  update[kr++];

	} /* end Marquardt_it */

	/*--------------------------------------------------------------------------*/
	/**
	 * Optimize the B-spline coefficients (bidirectional method).
	 *
	 * @param intervals number of intervals in the deformation
	 * @param thChangef
	 * @param cxTargetToSource x- B-spline coefficients storing the target to source deformation
	 * @param cyTargetToSource y- B-spline coefficients storing the target to source deformation
	 * @param cxSourceToTarget x- B-spline coefficients storing the source to target deformation
	 * @param cySourceToTarget y- B-spline coefficients storing the source to target deformation
	 * @return energy function value
	 */
	private double optimizeCoeffs(
			int intervals,
			double thChangef,
			double [][]cxTargetToSource,
			double [][]cyTargetToSource,
			double [][]cxSourceToTarget,
			double [][]cySourceToTarget)
	{
		if (dialog!=null && dialog.isStopRegistrationSet())
			return 0.0;

		if(source.isSubOutput())
		{
			IJ.log(" -----\n Intervals = " + intervals + "x" + intervals);
		    IJ.log(" Source Image Size = " + this.sourceCurrentWidth + "x" + this.sourceCurrentHeight);
		}
		
		final double TINY               = FLT_EPSILON;
		final double EPS                = 3.0e-8F;
		final double FIRSTLAMBDA        = 1;
		final int    MAXITER_OPTIMCOEFF = 300;
		final int    CUMULATIVE_SIZE    = 5;

		int int3 = intervals + 3;
		int halfM = 2 * int3 * int3;
		int quarterM = halfM / 2;
		int threeQuarterM = quarterM * 3;
		int M = halfM * 2;

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
//		double   []safehess     = new double   [M*M];
		double   []proposedHess = new double   [M*M];
		boolean  []optimize     = new boolean  [M];
		int        i, j, p, iter = 1;
		boolean    skip_update, ill_hessian;
		double     improvementx = (double)Math.sqrt(TINY),
		lambda = FIRSTLAMBDA, max_normx, distx, aux, gmax;
		double     fac, fae, dgdx, dxHdx, sumdiffg, sumdiffx;

		CumulativeQueue lastBest=
			new CumulativeQueue(CUMULATIVE_SIZE);

		for (i=0; i<M; i++) optimize[i] = true;

		/* Form the vector with the current guess for the optimization */
		for (i= 0, p=0; i < intervals + 3; i++)
			for (j = 0; j < intervals + 3; j++, p++)
			{
				x[         p] = cxTargetToSource[i][j];
				x[quarterM+p] = cxSourceToTarget[i][j];

				x[halfM        +p] = cyTargetToSource[i][j];
				x[threeQuarterM+p] = cySourceToTarget[i][j];
			}

		/* Prepare the precomputed weights for interpolation */
		this.swxTargetToSource = new BSplineModel(x, intervals+3, intervals+3, 0);
		this.swyTargetToSource = new BSplineModel(x, intervals+3, intervals+3, halfM);
		this.swxTargetToSource.precomputed_prepareForInterpolation(
				target.getCurrentHeight(), target.getCurrentWidth(), intervals);
		this.swyTargetToSource.precomputed_prepareForInterpolation(
				target.getCurrentHeight(), target.getCurrentWidth(), intervals);

		this.swxSourceToTarget = new BSplineModel(x, intervals+3, intervals+3, quarterM);
		this.swySourceToTarget = new BSplineModel(x, intervals+3, intervals+3, threeQuarterM);
		this.swxSourceToTarget.precomputed_prepareForInterpolation(
				source.getCurrentHeight(), source.getCurrentWidth(), intervals);
		this.swySourceToTarget.precomputed_prepareForInterpolation(
				source.getCurrentHeight(), source.getCurrentWidth(), intervals);


		/* First computation of the energy */
		f = energyFunction(x, intervals, grad, false, false);

		if (showMarquardtOptim) IJ.write("f(1)="+f);

		/* Initially the hessian is the identity matrix multiplied by
          the first function value */
		for (i=0,p=0; i<M; i++)
			for (j=0; j<M; j++,p++)
				if (i==j) hess[p]=1.0F;
				else hess[p]=0.0F;

		rescuedf    = f;
		for (i=0,p=0; i<M; i++) {
			rescuedx[i]=x[i];
			rescuedgrad[i]=grad[i];
			for (j=0; j<M; j++,p++)
				rescuedhess[p]=hess[p];
		}

		// Maximum iteration number
		int maxiter = MAXITER_OPTIMCOEFF * (source.getCurrentDepth() + 1);

		ProgressBar.stepProgressBar();

		int last_successful_iter=0;

		boolean stop = dialog != null && dialog.isStopRegistrationSet();

		while (iter < maxiter && !stop)
		{
			/* Compute new x ------------------------------------------------- */
			Marquardt_it(x, optimize, grad, hess, lambda);

			/* Stopping criteria --------------------------------------------- */
			/* Compute difference with the previous iteration */
			max_normx = improvementx = 0;
			for (i=0; i<M; i++)
			{
				diffx[i] = x[i] - rescuedx[i];
				distx = Math.abs(diffx[i]);
				improvementx += distx*distx;
				aux = Math.abs(rescuedx[i]) < Math.abs(x[i]) ? x[i] : rescuedx[i];
				max_normx += aux*aux;
			}

			if (TINY < max_normx) 
				improvementx = improvementx/max_normx;

			improvementx = (double) Math.sqrt(Math.sqrt(improvementx));

			/* If there is no change with respect to the old geometry then
             finish the iterations */
			if (improvementx < Math.sqrt(TINY)) break;

			/* Estimate the new function value -------------------------------- */
			f = energyFunction(x, intervals, grad, false, false);
			iter++;
			if (showMarquardtOptim) 
				IJ.write("f("+iter+")="+f+" lambda="+lambda);
			ProgressBar.stepProgressBar();

			/* Update lambda -------------------------------------------------- */
			if (rescuedf > f)
			{
				// We save the last energy terms values in order to be displayed.
				this.finalDirectConsistencyError = this.partialDirectConsitencyError;
				this.finalDirectSimilarityError  = this.partialDirectSimilarityError;
				this.finalDirectRegularizationError = this.partialDirectRegularizationError;
				this.finalDirectLandmarkError      = this.partialDirectLandmarkError;
				
				this.finalInverseConsistencyError = this.partialInverseConsitencyError;
				this.finalInverseSimilarityError  = this.partialInverseSimilarityError;
				this.finalInverseRegularizationError = this.partialInverseRegularizationError;
				this.finalInverseLandmarkError      = this.partialInverseLandmarkError;

				/* Check if the improvement is only residual */
				lastBest.push_back(rescuedf-f);
				if (lastBest.currentSize() == CUMULATIVE_SIZE && lastBest.getSum()/f < thChangef)
					break;				 

				/* If we have improved then estimate the hessian,
                 update the geometry, and decrease the lambda */
				/* Estimate the hessian ....................................... */
				if (showMarquardtOptim) 
					IJ.write("  Accepted");
				if ((last_successful_iter++%10)==0 && outputLevel>-1)
					update_outputs(x, intervals);

				/* Estimate the difference between gradients */
				for (i=0; i<M; i++) 
					diffgrad[i] = grad[i]-rescuedgrad[i];

				/* Multiply this difference by the current inverse of the hessian */
				for (i=0, p=0; i<M; i++) 
				{
					Hdx[i] = 0.0F;
					for (j=0; j<M; j++, p++) 
						Hdx[i] += hess[p]*diffx[j];
				}

				/* Calculate dot products for the denominators ................ */
				dgdx = dxHdx = sumdiffg = sumdiffx = 0.0F;
				skip_update = true;
				for (i=0; i<M; i++) 
				{
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
				if (dgdx>Math.sqrt(EPS*sumdiffg*sumdiffx) && !skip_update) 
				{
					fae=1.0F/dxHdx;
					fac=1.0F/dgdx;

					/* Update the hessian after BFGS formula */
					for (i=0, p=0; i<M; i++)
						for (j=0; j<M; j++, p++) 
						{
							if (i<=j) proposedHess[p]=hess[p]+
							fac*diffgrad[i]*diffgrad[j]
							                         -fae*(Hdx[i]*Hdx[j]);
							else proposedHess[p]=proposedHess[j*M+i];
						}

					ill_hessian=false;
					if (!ill_hessian) 
					{
						for (i=0, p=0; i<M; i++)
							for (j=0; j<M; j++,p++)
								hess[p]= proposedHess[p];
					} 
					else
						if (showMarquardtOptim)
							IJ.write("Hessian cannot be safely updated, ill-conditioned");

				} else
					if (showMarquardtOptim)
						IJ.write("Hessian cannot be safely updated");

				/* Update geometry and lambda ................................. */
				rescuedf = f;
				for (i=0, p=0; i<M; i++) 
				{
					rescuedx[i]=x[i];
					rescuedgrad[i]=grad[i];
					for (j=0; j<M; j++,p++) 
						rescuedhess[p]=hess[p];
				}
				if (1e-4 < lambda) 
					lambda = lambda/10;
			} 
			else 
			{
				/* else, if it is worse, then recover the last geometry
             		and increase lambda, saturate lambda with FIRSTLAMBDA */
				for (i=0,p=0; i<M; i++) 
				{
					x[i] = rescuedx[i];
					grad[i] = rescuedgrad[i];
					for (j=0; j<M; j++,p++) 
						hess[p] = rescuedhess[p];
				}
				if (lambda < 1.0/TINY) 
					lambda*=10;
				else 
					break;
				if (lambda < FIRSTLAMBDA) 
					lambda = FIRSTLAMBDA;
			}

			stop = dialog!=null && dialog.isStopRegistrationSet();
		}

		// Copy the values back to the input arrays
		for (i= 0, p=0; i<intervals + 3; i++)
			for (j = 0; j < intervals + 3; j++, p++)
			{
				cxTargetToSource[i][j] = x[      p];
				cxSourceToTarget[i][j] = x[quarterM+p];

				cyTargetToSource[i][j] = x[halfM+p];
				cySourceToTarget[i][j] = x[threeQuarterM+p];
			}

		ProgressBar.skipProgressBar(maxiter-iter);
		return f;
	}

	
	/*--------------------------------------------------------------------------*/
	/**
	 * Optimize the B-spline coefficients (unidirectional method).
	 *
	 * @param intervals number of intervals in the deformation
	 * @param thChangef
	 * @param cxTargetToSource x- B-spline coefficients storing the target to source deformation
	 * @param cyTargetToSource y- B-spline coefficients storing the target to source deformation
	 * @return energy function value
	 */
	private double optimizeCoeffs(
			int intervals,
			double thChangef,
			double [][]cxTargetToSource,
			double [][]cyTargetToSource)
	{
		if(source.isSubOutput())
		{
			IJ.log(" -----\n Intervals = " + intervals + "x" + intervals);
			IJ.log(" Source Image Size = " + this.sourceCurrentWidth + "x" + this.sourceCurrentHeight);
		}
		
		if (dialog!=null && dialog.isStopRegistrationSet())
			return 0.0;

		final double TINY               = FLT_EPSILON;
		final double EPS                = 3.0e-8F;
		final double FIRSTLAMBDA        = 1;
		final int    MAXITER_OPTIMCOEFF = 300;
		final int    CUMULATIVE_SIZE    = 5;

		int int3 = intervals + 3;
		int halfM = int3 * int3;
		int M = halfM * 2;

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
		double   []proposedHess = new double   [M*M];
		boolean  []optimize     = new boolean  [M];
		int        i, j, p, iter = 1;
		boolean    skip_update, ill_hessian;
		double     improvementx = (double)Math.sqrt(TINY),
		lambda = FIRSTLAMBDA, max_normx, distx, aux, gmax;
		double     fac, fae, dgdx, dxHdx, sumdiffg, sumdiffx;

		CumulativeQueue lastBest=
			new CumulativeQueue(CUMULATIVE_SIZE);

		for (i=0; i<M; i++) 
			optimize[i] = true;

		/* Form the vector with the current guess for the optimization */
		for (i= 0, p=0; i < intervals + 3; i++)
			for (j = 0; j < intervals + 3; j++, p++)
			{
				x[         p] = cxTargetToSource[i][j];
				x[halfM  + p] = cyTargetToSource[i][j];
			}

		/* Prepare the precomputed weights for interpolation */
		this.swxTargetToSource = new BSplineModel(x, intervals+3, intervals+3, 0);
		this.swyTargetToSource = new BSplineModel(x, intervals+3, intervals+3, halfM);
		this.swxTargetToSource.precomputed_prepareForInterpolation(
				target.getCurrentHeight(), target.getCurrentWidth(), intervals);
		this.swyTargetToSource.precomputed_prepareForInterpolation(
				target.getCurrentHeight(), target.getCurrentWidth(), intervals);

		// First computation of the energy (similarity + landmarks + regularization)
		//f = evaluateSimilarity(x, intervals, grad, false, false, false);
		f = evaluateSimilarityMultiThread(x, intervals, grad, false, false);

		if (showMarquardtOptim) IJ.write("f(1)="+f);

		/* Initially the hessian is the identity matrix multiplied by
          the first function value */
		for (i=0,p=0; i<M; i++)
			for (j=0; j<M; j++,p++)
				if (i==j) hess[p]=1.0F;
				else hess[p]=0.0F;

		rescuedf    = f;
		for (i=0,p=0; i<M; i++) {
			rescuedx[i]=x[i];
			rescuedgrad[i]=grad[i];
			for (j=0; j<M; j++,p++)
				rescuedhess[p]=hess[p];
		}

		// Maximum iteration number
		int maxiter = MAXITER_OPTIMCOEFF * (source.getCurrentDepth() + 1);

		ProgressBar.stepProgressBar();

		int last_successful_iter = 0;

		boolean stop = dialog != null && dialog.isStopRegistrationSet();

		while (iter < maxiter && !stop)
		{
			/* Compute new x ------------------------------------------------- */
			Marquardt_it(x, optimize, grad, hess, lambda);

			/* Stopping criteria --------------------------------------------- */
			/* Compute difference with the previous iteration */
			max_normx = improvementx = 0;
			for (i=0; i<M; i++)
			{
				diffx[i] = x[i] - rescuedx[i];
				distx = Math.abs(diffx[i]);
				improvementx += distx*distx;
				aux = Math.abs(rescuedx[i]) < Math.abs(x[i]) ? x[i] : rescuedx[i];
				max_normx += aux*aux;
			}

			if (TINY < max_normx) 
				improvementx = improvementx/max_normx;

			improvementx = (double) Math.sqrt(Math.sqrt(improvementx));

			/* If there is no change with respect to the old geometry then
             finish the iterations */
			if (improvementx < Math.sqrt(TINY)) break;

			/* Estimate the new function value -------------------------------- */
			//f = evaluateSimilarity(x, intervals, grad, false, false, false);
			f = evaluateSimilarityMultiThread(x, intervals, grad, false, false);
			
			iter++;
			if (showMarquardtOptim) 
				IJ.write("f("+iter+")="+f+" lambda="+lambda);
			ProgressBar.stepProgressBar();

			/* Update lambda -------------------------------------------------- */
			if (rescuedf > f)
			{
				// We save the last energy terms values in order to be displayed.
				this.finalDirectConsistencyError = this.partialDirectConsitencyError;
				this.finalDirectSimilarityError  = this.partialDirectSimilarityError;
				this.finalDirectRegularizationError = this.partialDirectRegularizationError;
				this.finalDirectLandmarkError      = this.partialDirectLandmarkError;
				
				/* Check if the improvement is only residual */
				lastBest.push_back(rescuedf-f);
				if (lastBest.currentSize() == CUMULATIVE_SIZE && lastBest.getSum()/f < thChangef)
					break;				 

				/* If we have improved then estimate the hessian,
                 update the geometry, and decrease the lambda */
				/* Estimate the hessian ....................................... */
				if (showMarquardtOptim) 
					IJ.write("  Accepted");
				if ((last_successful_iter++%10)==0 && outputLevel>-1)
					update_current_output(x,intervals, false);

				/* Estimate the difference between gradients */
				for (i=0; i<M; i++) 
					diffgrad[i] = grad[i]-rescuedgrad[i];

				/* Multiply this difference by the current inverse of the hessian */
				for (i=0, p=0; i<M; i++) 
				{
					Hdx[i] = 0.0F;
					for (j=0; j<M; j++, p++) 
						Hdx[i] += hess[p]*diffx[j];
				}

				/* Calculate dot products for the denominators ................ */
				dgdx = dxHdx = sumdiffg = sumdiffx = 0.0F;
				skip_update = true;
				for (i=0; i<M; i++) 
				{
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
				if (dgdx>Math.sqrt(EPS*sumdiffg*sumdiffx) && !skip_update) 
				{
					fae=1.0F/dxHdx;
					fac=1.0F/dgdx;

					/* Update the hessian after BFGS formula */
					for (i=0, p=0; i<M; i++)
						for (j=0; j<M; j++, p++) 
						{
							if (i<=j) proposedHess[p]=hess[p]+
							fac*diffgrad[i]*diffgrad[j]
							                         -fae*(Hdx[i]*Hdx[j]);
							else proposedHess[p]=proposedHess[j*M+i];
						}

					ill_hessian=false;
					if (!ill_hessian) 
					{
						for (i=0, p=0; i<M; i++)
							for (j=0; j<M; j++,p++)
								hess[p]= proposedHess[p];
					} 
					else
						if (showMarquardtOptim)
							IJ.write("Hessian cannot be safely updated, ill-conditioned");

				} else
					if (showMarquardtOptim)
						IJ.write("Hessian cannot be safely updated");

				/* Update geometry and lambda ................................. */
				rescuedf = f;
				for (i=0, p=0; i<M; i++) 
				{
					rescuedx[i]=x[i];
					rescuedgrad[i]=grad[i];
					for (j=0; j<M; j++,p++) 
						rescuedhess[p]=hess[p];
				}
				if (1e-4 < lambda) 
					lambda = lambda/10;
			} 
			else 
			{
				/* else, if it is worse, then recover the last geometry
             		and increase lambda, saturate lambda with FIRSTLAMBDA */
				for (i=0,p=0; i<M; i++) 
				{
					x[i] = rescuedx[i];
					grad[i] = rescuedgrad[i];
					for (j=0; j<M; j++,p++) 
						hess[p] = rescuedhess[p];
				}
				if (lambda < 1.0/TINY) 
					lambda*=10;
				else 
					break;
				if (lambda < FIRSTLAMBDA) 
					lambda = FIRSTLAMBDA;
			}

			stop = dialog!=null && dialog.isStopRegistrationSet();
		}

		// Copy the values back to the input arrays
		for (i= 0, p=0; i<intervals + 3; i++)
			for (j = 0; j < intervals + 3; j++, p++)
			{
				cxTargetToSource[i][j] = x[      p];

				cyTargetToSource[i][j] = x[halfM+p];
			}

		ProgressBar.skipProgressBar(maxiter-iter);
		return f;
	}
	
	
	/*-----------------------------------------------------------------------------*/
	/**
	 * Propagate deformation coefficients to the next level.
	 *
	 * @param intervals number of intervals in the deformation
	 * @param c B-spline coefficients
	 * @param expansionFactor due to the change of size in the represented image
	 * @return propagated coefficients
	 */
	private double [][] propagateCoeffsToNextLevel(
			int intervals,
			final double [][]c,
			double expansionFactor)
	{
		// Expand the coefficients for the next scale
		intervals*=2;
		double [][] cs_expand = new double[intervals+7][intervals+7];

		// Upsample
		for (int i=0; i<intervals+7; i++)
			for (int j=0; j<intervals+7; j++) 
			{
				// If it is not in an even sample then set it to 0
				if (i%2 ==0 || j%2 ==0) cs_expand[i][j]=0.0F;
				else 
				{
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
	
	//------------------------------------------------------------------
	/**
	 * Save source transformation. GUI mode. 
	 */
	public void saveDirectTransformation()
	{
		saveTransformation(intervals, cxTargetToSource, cyTargetToSource, false);		
	} 
	//------------------------------------------------------------------
	/**
	 * Save target transformation. GUI mode. 
	 */
	public void saveInverseTransformation()
	{
		saveTransformation(intervals, cxSourceToTarget, cySourceToTarget, true);
	}
	
	//------------------------------------------------------------------
	/**
	 * Save the transformation.
	 *
	 * @param intervals number of intervals in the deformation
	 * @param cx x- deformation coefficients
	 * @param cy y- deformation coefficients
	 * @param bIsReverse flat to determine the transformation direction
	 */
	private void saveTransformation(
			int intervals,
			double [][]cx,
			double [][]cy,
			boolean bIsReverse)
	{
		String filename = fn_tnf_1;

		if(bIsReverse) filename = fn_tnf_2;

		if (filename.equals(""))
		{
			// Get the filename to save
			File dir=new File(".");
			String path="";
			try
			{
				path=dir.getCanonicalPath()+"/";
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			filename = (bIsReverse) ? targetImp.getTitle() : sourceImp.getTitle();
			String sDirection = (bIsReverse) ? "_inverse" : "_direct";
			String new_filename="";
			int dot = filename.lastIndexOf('.');
			if (dot == -1) new_filename = filename + sDirection + "_transf";
			else           new_filename = filename.substring(0, dot) + sDirection + "_transf";
			filename=path+filename;

			if (outputLevel > -1 && this.dialog != null && this.dialog.isMacroCall() == false)
			{
				final SaveDialog sd = new SaveDialog("Save Transformation", new_filename, ".txt");

				path = sd.getDirectory();
				filename = sd.getFileName();
				if ((path == null) || (filename == null)) 
					return;
				filename = path + filename;
			} 
			else
				filename = new_filename;
		}

		// Save the file
		if(this.dialog != null && this.dialog.isMacroCall())
			filename += ".txt";
		MiscTools.saveElasticTransformation(intervals, cx, cy, filename);
	}

	//------------------------------------------------------------------
	/**
	 * Compute and draw the final deformation grid.
	 *
	 * @param intervals number of intervals in the deformation
	 * @param cx x- deformation coefficients
	 * @param cy y- deformation coefficients
	 * @param is image stack where we want to show the deformation grid
	 * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 */
	private void computeDeformationGrid(
			int intervals,
			double [][]cx,
			double [][]cy,
			ImageStack is,
			boolean bIsReverse)
	{

		int auxTargetCurrentHeight = this.targetCurrentHeight;
		int auxTargetCurrentWidth = this.targetCurrentWidth;

		if(bIsReverse)
		{
			auxTargetCurrentHeight = sourceCurrentHeight;
			auxTargetCurrentWidth = sourceCurrentWidth;
		}
		// Initialize output image
		int stepv = Math.min(Math.max(10, auxTargetCurrentHeight/15), 30);
		int stepu = Math.min(Math.max(10, auxTargetCurrentWidth/15), 30);
		final double transformedImage [][] = new double [auxTargetCurrentHeight][auxTargetCurrentWidth];
		for (int v=0; v<auxTargetCurrentHeight; v++)
			for (int u=0; u<auxTargetCurrentWidth; u++)
				transformedImage[v][u] = 255;

		// Ask for memory for the transformation
		double [][] transformation_x=new double [auxTargetCurrentHeight][auxTargetCurrentWidth];
		double [][] transformation_y=new double [auxTargetCurrentHeight][auxTargetCurrentWidth];

		// Compute the deformation
		computeDeformation(intervals,cx,cy,transformation_x,transformation_y, bIsReverse);

		// Show deformed grid ........................................
		// Show deformation vectors
		for (int v=0; v<auxTargetCurrentHeight; v+=stepv)
			for (int u=0; u<auxTargetCurrentWidth; u+=stepu) {
				final double x = transformation_x[v][u];
				final double y = transformation_y[v][u];
				// Draw horizontal line
				int uh=u+stepu;
				if (uh<auxTargetCurrentWidth) {
					final double xh = transformation_x[v][uh];
					final double yh = transformation_y[v][uh];
					MiscTools.drawLine(
							transformedImage,
							(int)Math.round(x) ,(int)Math.round(y),
							(int)Math.round(xh),(int)Math.round(yh),0);
				}

				// Draw vertical line
				int vv = v+stepv;
				if (vv<auxTargetCurrentHeight) {
					final double xv = transformation_x[vv][u];
					final double yv = transformation_y[vv][u];
					MiscTools.drawLine(
							transformedImage,
							(int)Math.round(x) ,(int)Math.round(y),
							(int)Math.round(xv),(int)Math.round(yv),0);
				}
			}

		// Set it to the image stack
		FloatProcessor fp=new FloatProcessor(auxTargetCurrentWidth, auxTargetCurrentHeight);
		for (int v=0; v<auxTargetCurrentHeight; v++)
			for (int u=0; u<auxTargetCurrentWidth; u++)
				fp.putPixelValue(u, v, transformedImage[v][u]);

		// Gray scale images
		if(!(this.originalSourceIP instanceof ColorProcessor))
			is.addSlice("Deformation Grid",fp);
		else // Color images
			is.addSlice("Deformation Grid",fp.convertToRGB());
	}

	//------------------------------------------------------------------
	/**
	 * Compute and draw the final deformation vectors.
	 *
	 * @param intervals number of intervals in the deformation
	 * @param cx x- deformation coefficients
	 * @param cy y- deformation coefficients
	 * @param is image stack where we want to show the deformation vectors
	 * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 */
	private void computeDeformationVectors(
			int intervals,
			double [][]cx,
			double [][]cy,
			ImageStack is,
			boolean bIsReverse)
	{
		// Auxiliar variables for changing from source to target and inversely
		Mask auxTargetMsk = this.targetMsk;
		Mask auxSourceMsk = this.sourceMsk;
		int auxTargetCurrentHeight = this.targetCurrentHeight;
		int auxTargetCurrentWidth = this.targetCurrentWidth;       

		// Change if necessary
		if(bIsReverse)
		{
			auxTargetMsk = this.sourceMsk;
			auxSourceMsk = this.targetMsk;
			auxTargetCurrentHeight = this.sourceCurrentHeight;
			auxTargetCurrentWidth = this.sourceCurrentWidth;
		}

		// Initialize output image
		int stepv = Math.min(Math.max(10, auxTargetCurrentHeight/15),30);
		int stepu = Math.min(Math.max(10, auxTargetCurrentWidth/15),30);

		final double transformedImage [][] = new double [auxTargetCurrentHeight][auxTargetCurrentWidth];

		for (int v=0; v<auxTargetCurrentHeight; v++)
			for (int u=0; u<auxTargetCurrentWidth; u++)
				transformedImage[v][u]=255;

		// Ask for memory for the transformation
		double [][] transformation_x=new double [auxTargetCurrentHeight][auxTargetCurrentWidth];
		double [][] transformation_y=new double [auxTargetCurrentHeight][auxTargetCurrentWidth];

		// Compute the deformation
		computeDeformation(intervals, cx, cy, transformation_x, transformation_y, bIsReverse);

		// Show shift field ........................................
		// Show deformation vectors
		for (int v=0; v<auxTargetCurrentHeight; v+=stepv)
			for (int u=0; u<auxTargetCurrentWidth; u+=stepu)
				if (auxTargetMsk.getValue(u,v)) 
				{
					final double x = transformation_x[v][u];
					final double y = transformation_y[v][u];
					if (auxSourceMsk.getValue(x,y))
						MiscTools.drawArrow(
								transformedImage,
								u,v,(int)Math.round(x),(int)Math.round(y),0,2);
				}

		// Set it to the image stack
		FloatProcessor fp=new FloatProcessor(auxTargetCurrentWidth,auxTargetCurrentHeight);
		for (int v=0; v<auxTargetCurrentHeight; v++)
			for (int u=0; u<auxTargetCurrentWidth; u++)
				fp.putPixelValue(u, v, transformedImage[v][u]);

		// Gray scale images
		if(!(this.originalSourceIP instanceof ColorProcessor))
			is.addSlice("Deformation Field",fp);
		else // Color images
			is.addSlice("Deformation Field",fp.convertToRGB());
	}
	/*-------------------------------------------------------------------*/
	/**
	 * Show the direct transformation results(multi-thread version).
	 */
	public void showDirectResults()
	{
		showTransformationMultiThread(intervals, cxTargetToSource, cyTargetToSource, false);
	}
	
	/*-------------------------------------------------------------------*/
	/**
	 * Show the inverse transformation results (multi-thread version).
	 */
	public void showInverseResults()
	{
		showTransformationMultiThread(intervals, cxSourceToTarget, cySourceToTarget, true);
	}
	
	/*-------------------------------------------------------------------*/
	/**
	 * Show the transformation (multi-thread version).
	 *
	 * @param intervals number of intervals in the deformation
	 * @param cx x- deformation coefficients
	 * @param cy y- deformation coefficients
	 * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 */
	private void showTransformationMultiThread(
			final int   intervals,
			final double [][]cx,    // Input, spline coefficients
			final double [][]cy,
			boolean bIsReverse)
	{
		
		ImagePlus output_ip = (!bIsReverse) ? this.output_ip_1 : this.output_ip_2;
		
		// Calculate tranformation results 
		IJ.showStatus("Calculating result window...");
		ImagePlus result_imp = applyTransformationMultiThread(intervals, cx, cy, bIsReverse);
		
		output_ip.close();
		output_ip = result_imp;
			
		output_ip.show();				
		output_ip.updateAndRepaintWindow();
				
	} /* end showTransformationMultiThread */

	/*-------------------------------------------------------------------*/
	/**
	 * Get direct results window
	 * 
	 * @return direct registration results
	 */
	public ImagePlus getDirectResults()
	{
		return applyTransformationMultiThread(intervals, cxTargetToSource, cyTargetToSource, false);
	}
	
	/*-------------------------------------------------------------------*/
	/**
	 * Get inverse results window
	 * 
	 * @return inverse registration results
	 */
	public ImagePlus getInverseResults()	
	{
		if(this.cySourceToTarget != null && this.cxSourceToTarget != null)
			return applyTransformationMultiThread(intervals, cxSourceToTarget, cySourceToTarget, true);
		return null;
	}
	
	/*-------------------------------------------------------------------*/
	/**
	 * Apply the final transformation (multi-thread version).
	 *
	 * @param intervals number of intervals in the deformation
	 * @param cx x- deformation coefficients
	 * @param cy y- deformation coefficients
	 * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 * 
	 * @return output images (depending on the output level)
	 */
	private ImagePlus applyTransformationMultiThread(
			final int   intervals,
			final double [][]cx,    // Input, spline coefficients
			final double [][]cy,
			boolean bIsReverse)
	{
		BSplineModel auxTarget = target;
		BSplineModel auxSource = source;
		Mask auxTargetMsk = targetMsk;
		Mask auxSourceMsk = sourceMsk;
		int auxTargetWidth = this.originalTargetIP.getWidth();
		int auxTargetHeight = this.originalTargetIP.getHeight();		
		ImageProcessor originalIP = this.originalSourceIP;

		// Change if necessary
		if(bIsReverse)
		{
			auxTarget = source;
			auxSource = target;
			auxTargetMsk = sourceMsk;
			auxSourceMsk = targetMsk;
			auxTargetWidth = this.originalSourceIP.getWidth();
			auxTargetHeight = this.originalSourceIP.getHeight();
			originalIP = this.originalTargetIP;
		}
		
		ImagePlus output_ip = null;	
		final ImageStack is = new ImageStack(auxTargetWidth, auxTargetHeight);
		final String s = bIsReverse ? new String("Target") : new String("Source");
		
		// Create transformation B-spline models
		BSplineModel swx = new BSplineModel(cx);
		BSplineModel swy = new BSplineModel(cy);

		// We compute the deformation (transformation_x and transformation_y) on the fly

		/* GRAY SCALE IMAGES */
		if(!(originalIP instanceof ColorProcessor))
		{
			final FloatProcessor fp = new FloatProcessor(auxTargetWidth, auxTargetHeight);
			final FloatProcessor fp_mask = new FloatProcessor(auxTargetWidth, auxTargetHeight);
			final FloatProcessor fp_target = new FloatProcessor(auxTargetWidth, auxTargetHeight, auxTarget.getOriginalImage());
			
			
			// take original processor if necessary
			if(auxSource.getOriginalImageWidth() > auxSource.getWidth())
			{
				auxSource = new BSplineModel( originalIP, false, 1);
				auxSource.setPyramidDepth(0);
				auxSource.startPyramids();			

				// Join thread
				try {
					auxSource.getThread().join();				
				} catch (InterruptedException e) {
					IJ.error("Unexpected interruption exception " + e);
				}
			}

			// Check the number of processors in the computer 
			int nproc = Runtime.getRuntime().availableProcessors();

			// We will use threads to display parts of the output image
			int block_height = auxTargetHeight / nproc;
			if (auxTargetHeight % 2 != 0) 
				block_height++;
			
			
			int nThreads = nproc; /*(nproc > 1) ? (nproc / 2) : 1;
			if (this.accurate_mode == MainDialog.MONO_MODE)
				nThreads *= 2;*/
			
						
			Thread[] threads  = new Thread[nThreads];
			Rectangle[] rects = new Rectangle[nThreads];
			FloatProcessor[] fp_tile = new FloatProcessor[nThreads];
			FloatProcessor[] fp_mask_tile = new FloatProcessor[nThreads];
			
			for (int i=0; i<nThreads; i++) 
			{
				// last block size is the rest of the window
				int y_start = i*block_height;
				
				if (nThreads-1 == i) 
					block_height = auxTargetHeight - i*block_height;
								
				rects[i] = new Rectangle(0, y_start, auxTargetWidth, block_height);
				
				//IJ.log("block = 0 " + (i*block_height) + " " + auxTargetWidth + " " + block_height );
				
				fp_tile[i] = new FloatProcessor(rects[i].width, rects[i].height);
				fp_mask_tile[i] = new FloatProcessor(rects[i].width, rects[i].height);
				
				threads[i] = new Thread(new GrayscaleResultTileMaker(swx, swy, auxSource, 
															auxTargetWidth, auxTargetHeight,
															auxTargetMsk, auxSourceMsk, 
															rects[i], fp_tile[i], fp_mask_tile[i]));
				threads[i].start();
			}
			
			for (int i=0; i<nThreads; i++) 
			{
				try {
					threads[i].join();
					threads[i] = null;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}			
			}
			
			for (int i=0; i<nThreads; i++) 
			{
				fp.insert(fp_tile[i], rects[i].x, rects[i].y);
				fp_tile[i] = null;
				fp_mask.insert(fp_mask_tile[i], rects[i].x, rects[i].y);
				fp_mask_tile[i] = null;
				rects[i] = null;
			}
								
			fp.resetMinAndMax();

			// Add slices to result stack
			is.addSlice("Registered " + s + " Image", fp);
			//if (outputLevel > -1)
				is.addSlice("Target Image", fp_target);
			//if (outputLevel > -1)
				is.addSlice("Warped Source Mask",fp_mask);					
		}
		else /* COLOR IMAGES */
		{			
			// red
			BSplineModel sourceR = new BSplineModel( ((ColorProcessor) originalIP).toFloat(0, null), false, 1);
			sourceR.setPyramidDepth(0);
			sourceR.startPyramids();
			// green
			BSplineModel sourceG = new BSplineModel( ((ColorProcessor) originalIP).toFloat(1, null), false, 1);
			sourceG.setPyramidDepth(0);
			sourceG.startPyramids();
			//blue
			BSplineModel sourceB = new BSplineModel( ((ColorProcessor) originalIP).toFloat(2, null), false, 1);
			sourceB.setPyramidDepth(0);
			sourceB.startPyramids();
			
			// Join threads
			try {
				sourceR.getThread().join();
				sourceG.getThread().join();
				sourceB.getThread().join();
			} catch (InterruptedException e) {
				IJ.error("Unexpected interruption exception " + e);
			}

			// Calculate warped RGB image
			ColorProcessor cp = new ColorProcessor(auxTargetWidth, auxTargetHeight);
			FloatProcessor fpR 		= new FloatProcessor(auxTargetWidth, auxTargetHeight);
			FloatProcessor fpG 		= new FloatProcessor(auxTargetWidth, auxTargetHeight);
			FloatProcessor fpB 		= new FloatProcessor(auxTargetWidth, auxTargetHeight);
			ColorProcessor cp_mask	= new ColorProcessor(auxTargetWidth, auxTargetHeight);			
			
			// Check the number of processors in the computer 
			int nproc = Runtime.getRuntime().availableProcessors();

			// We will use threads to display parts of the output image
			int block_height = auxTargetHeight / nproc;
			if (auxTargetHeight % 2 != 0) 
				block_height++;
			
			
			int nThreads = nproc; /*(nproc > 1) ? (nproc / 2) : 1;
			if (this.accurate_mode == MainDialog.MONO_MODE)
				nThreads *= 2;*/
			
						
			Thread[] threads  = new Thread[nThreads];
			Rectangle[] rects = new Rectangle[nThreads];
			FloatProcessor[] fpR_tile 		= new FloatProcessor[nThreads];
			FloatProcessor[] fpG_tile 		= new FloatProcessor[nThreads];
			FloatProcessor[] fpB_tile 		= new FloatProcessor[nThreads];
			ColorProcessor[] cp_mask_tile 	= new ColorProcessor[nThreads];
			
			for (int i=0; i<nThreads; i++) 
			{
				// last block size is the rest of the window
				int y_start = i*block_height;
				
				if (nThreads-1 == i) 
					block_height = auxTargetHeight - i*block_height;
								
				rects[i] = new Rectangle(0, y_start, auxTargetWidth, block_height);
				
				//IJ.log("block = 0 " + (i*block_height) + " " + auxTargetWidth + " " + block_height );
				
				fpR_tile[i] 	= new FloatProcessor(rects[i].width, rects[i].height);
				fpG_tile[i] 	= new FloatProcessor(rects[i].width, rects[i].height);
				fpB_tile[i] 	= new FloatProcessor(rects[i].width, rects[i].height);
				cp_mask_tile[i] = new ColorProcessor(rects[i].width, rects[i].height);
				
				threads[i] = new Thread(new ColorResultTileMaker(swx, swy, sourceR, sourceG, sourceB, 
															auxTargetWidth, auxTargetHeight,
															auxTargetMsk, auxSourceMsk, 
															rects[i], fpR_tile[i],
															fpG_tile[i],fpB_tile[i],
															cp_mask_tile[i]));
				threads[i].start();
			}
			
			for (int i=0; i<nThreads; i++) 
			{
				try {
					threads[i].join();
					threads[i] = null;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}			
			}
			
			for (int i=0; i<nThreads; i++) 
			{
				fpR.insert(fpR_tile[i], rects[i].x, rects[i].y);
				fpG.insert(fpG_tile[i], rects[i].x, rects[i].y);
				fpB.insert(fpB_tile[i], rects[i].x, rects[i].y);
				
				fpR_tile[i] = null;
				fpG_tile[i] = null;
				fpB_tile[i] = null;
				
				cp_mask.insert(cp_mask_tile[i], rects[i].x, rects[i].y);
				cp_mask_tile[i] = null;
				rects[i] = null;
			}
								
		
			cp.setPixels(0, fpR);
			cp.setPixels(1, fpG);
			cp.setPixels(2, fpB);            
			cp.resetMinAndMax();			
			
			// Add slices to result stack
			is.addSlice("Registered " + s + " Image", cp);
			//if (outputLevel > -1)
			is.addSlice("Target Image", bIsReverse ? this.originalSourceIP : this.originalTargetIP);  		   
			//if (outputLevel > -1)
			is.addSlice("Warped Source Mask", cp_mask);			
								
		} // end caculate warped color image
		
		if (outputLevel == 2)
		{
			computeDeformationVectors(intervals, cx, cy, is, bIsReverse);
			computeDeformationGrid(intervals, cx, cy, is, bIsReverse);
		}
		
		output_ip = new ImagePlus("Registered " + s + " Image", is);
		
		output_ip.setSlice(1);
		output_ip.getProcessor().resetMinAndMax();	
		
		return output_ip;
	} /* end applyTransformationMultiThread */

	/* ------------------------------------------------------------------------ */
	/**
	 *  Class to run concurrent tile windows for final results (grayscale)
	 * 	 
	 */	
	private class GrayscaleResultTileMaker implements Runnable 
	{
		final BSplineModel swx;
		final BSplineModel swy;		
		final BSplineModel auxSource;
		final int auxTargetCurrentWidth;
		final int auxTargetCurrentHeight;
		final Mask auxTargetMsk;
		final Mask auxSourceMsk;
		final Rectangle rect;
		final private FloatProcessor fp;
		final private FloatProcessor fp_mask;
		
		//------------------------------------------------------------------
		/**
		 * Grayscale result tile maker constructor
		 * 
		 * @param swx B-spline interpolator for transformation in x-
		 * @param swy B-spline interpolator for transformation in y-
		 * @param auxSource source image
		 * @param auxTargetCurrentWidth current target width
		 * @param auxTargetCurrentHeight current target height
		 * @param auxTargetMsk target mask
		 * @param auxSourceMsk source mask
		 * @param rect retangle with the coordinates of the output image to be updated
		 * @param fp image processor to be updated
		 * @param fp_mask mask processor to be updated
		 */
		GrayscaleResultTileMaker(BSplineModel swx, 
		 		  BSplineModel swy, 
		 		  BSplineModel auxSource,
		 		  int auxTargetCurrentWidth,
		 		  int auxTargetCurrentHeight,
		 		  Mask auxTargetMsk,
		 		  Mask auxSourceMsk,
				  Rectangle rect, 
				  FloatProcessor fp,
				  FloatProcessor fp_mask)
		{
			this.swx = swx;
			this.swy = swy;
			this.auxSource = auxSource;
			this.auxTargetCurrentWidth = auxTargetCurrentWidth;
			this.auxTargetCurrentHeight = auxTargetCurrentHeight;
			this.auxTargetMsk = auxTargetMsk;
			this.auxSourceMsk = auxSourceMsk;
			this.rect = rect;
			this.fp = fp;
			this.fp_mask = fp_mask;
		}
	
		//------------------------------------------------------------------
		/**
		 * Run method to update the intermediate window. Only the part defined by
		 * the rectangle will be updated (in this thread).
		 */
		public void run()
		{
			// Compute the warped image
			int uv = rect.y * rect.width + rect.x;
			int auxTargetHeight = rect.y + rect.height;
			int auxTargetWidth = rect.x + rect.width;
			
			
			float [] fp_array = (float[]) fp.getPixels();
			float [] fp_mask_array = (float[]) fp_mask.getPixels();
			
			for (int v_rect = 0, v=rect.y; v<auxTargetHeight; v++, v_rect++)
			{
				final int v_offset = v_rect * rect.width;
				final double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;
				
				for (int u_rect = 0, u=rect.x; u<auxTargetWidth; u++, uv++, u_rect++) 
				{
			
			
					final double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth - 1) + 1.0F;			
					final double transformation_x_v_u = swx.prepareForInterpolationAndInterpolateI(tu, tv, false, ORIGINAL);
					final double transformation_y_v_u = swy.prepareForInterpolationAndInterpolateI(tu, tv, false, ORIGINAL);
									
					if (!auxTargetMsk.getValue(u,v))
					{
						//fp.putPixelValue(u,v,0);
						fp_array[u_rect + v_offset] = 0;
						//fp_mask.putPixelValue(u,v,0);
						fp_mask_array[u_rect + v_offset] = 0;
					}
					else
					{
						final double x = transformation_x_v_u;
						final double y = transformation_y_v_u;
						if (auxSourceMsk.getValue(x,y))
						{
							fp_array[u_rect + v_offset] = (float) auxSource.prepareForInterpolationAndInterpolateI(x, y, false, ORIGINAL);							
							fp_mask_array[u_rect + v_offset] = 255;
						}
						else
						{
							//fp.putPixelValue(u,v,0);
							fp_array[u_rect + v_offset] = 0;
							//fp_mask.putPixelValue(u,v,0);
							fp_mask_array[u_rect + v_offset] = 0;
						}
					}
				}
			}
		} /* end run method */
		
	} /* end GrayscaleResultTileMaker class */
	
	
	/* ------------------------------------------------------------------------ */
	/**
	 *  Class to run concurrent tile windows for final results (color)
	 * 	 
	 */	
	private class ColorResultTileMaker implements Runnable 
	{
		final BSplineModel swx;
		final BSplineModel swy;		
		final BSplineModel sourceR;
		final BSplineModel sourceG;
		final BSplineModel sourceB;
		final int auxTargetCurrentWidth;
		final int auxTargetCurrentHeight;
		final Mask auxTargetMsk;
		final Mask auxSourceMsk;
		final Rectangle rect;
		final private FloatProcessor fpR;
		final private FloatProcessor fpG;
		final private FloatProcessor fpB;
		final private ColorProcessor cp_mask;
				
		//------------------------------------------------------------------
		/**
		 * Color result tile maker constructor
		 * 
		 * @param swx B-spline interpolator for transformation in x-
		 * @param swy B-spline interpolator for transformation in y-
		 * @param sourceR red source image
		 * @param sourceG green source image
		 * @param sourceB blue source image
		 * @param auxTargetCurrentWidth current target height
		 * @param auxTargetCurrentHeight current target height
		 * @param auxTargetMsk target mask
		 * @param auxSourceMsk source mask
		 * @param rect retangle with the coordinates of the output image to be updated
		 * @param fpR red channel processor to be updated
		 * @param fpG green channel processor to be updated
		 * @param fpB blue channel processor to be updated
		 * @param cp_mask mask color processor to be updated
		 */
		ColorResultTileMaker(BSplineModel swx, 
		 		  BSplineModel swy, 
		 		  BSplineModel sourceR,
		 		  BSplineModel sourceG,
		 		  BSplineModel sourceB,
		 		  int auxTargetCurrentWidth,
		 		  int auxTargetCurrentHeight,
		 		  Mask auxTargetMsk,
		 		  Mask auxSourceMsk,
				  Rectangle rect, 
				  FloatProcessor fpR,
				  FloatProcessor fpG,
				  FloatProcessor fpB,
				  ColorProcessor cp_mask)
		{
			this.swx = swx;
			this.swy = swy;
			this.sourceR = sourceR;
			this.sourceG = sourceG;
			this.sourceB = sourceB;
			this.auxTargetCurrentWidth = auxTargetCurrentWidth;
			this.auxTargetCurrentHeight = auxTargetCurrentHeight;
			this.auxTargetMsk = auxTargetMsk;
			this.auxSourceMsk = auxSourceMsk;
			this.rect = rect;
			this.fpR = fpR;
			this.fpG = fpG;
			this.fpB = fpB;
			this.cp_mask = cp_mask;
		}
	
		//------------------------------------------------------------------
		/**
		 * Run method to update the intermediate window. Only the part defined by
		 * the rectangle will be updated (in this thread).
		 */
		public void run()
		{
			// Compute the warped image
			int uv = rect.y * rect.width + rect.x;
			int auxTargetHeight = rect.y + rect.height;
			int auxTargetWidth = rect.x + rect.width;
			
			
			float [] fpR_array = (float[]) fpR.getPixels();
			float [] fpG_array = (float[]) fpG.getPixels();
			float [] fpB_array = (float[]) fpB.getPixels();
			
			
			for (int v_rect = 0, v=rect.y; v<auxTargetHeight; v++, v_rect++)
			{
				final int v_offset = v_rect * rect.width;
				final double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;
				
				for (int u_rect = 0, u=rect.x; u<auxTargetWidth; u++, uv++, u_rect++) 
				{
						
					final double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth - 1) + 1.0F;			
					final double transformation_x_v_u = swx.prepareForInterpolationAndInterpolateI(tu, tv, false, ORIGINAL);
					final double transformation_y_v_u = swy.prepareForInterpolationAndInterpolateI(tu, tv, false, ORIGINAL);
									
					if (!auxTargetMsk.getValue(u,v))
					{
						//fpR.putPixelValue(u, v, 0);
						fpR_array[u_rect + v_offset] = 0;
						//fpG.putPixelValue(u, v, 0);
						fpG_array[u_rect + v_offset] = 0;
						//fpB.putPixelValue(u, v, 0);
						fpB_array[u_rect + v_offset] = 0;
						
						cp_mask.putPixelValue(u_rect, v_rect, 0);	
						
					}
					else
					{
						
						final double x = transformation_x_v_u;
						final double y = transformation_y_v_u;
						if (auxSourceMsk.getValue(x,y))
						{
							//sourceR.prepareForInterpolation(x, y, ORIGINAL);
							//fpR.putPixelValue(u, v, sourceR.interpolateI());
							fpR_array[u_rect + v_offset] = (float) (sourceR.prepareForInterpolationAndInterpolateI(x, y, false, ORIGINAL));

							//sourceG.prepareForInterpolation(x, y, ORIGINAL);
							//fpG.putPixelValue(u, v, sourceG.interpolateI());
							fpG_array[u_rect + v_offset] = (float) (sourceG.prepareForInterpolationAndInterpolateI(x, y, false, ORIGINAL));

							//sourceB.prepareForInterpolation(x, y, ORIGINAL);
							//fpB.putPixelValue(u, v, sourceB.interpolateI());
							fpB_array[u_rect + v_offset] = (float) (sourceB.prepareForInterpolationAndInterpolateI(x, y, false, ORIGINAL));
							
							cp_mask.putPixelValue(u_rect, v_rect, 255);							
						}
						else
						{
							//fpR.putPixelValue(u, v, 0);
							fpR_array[u_rect + v_offset] = 0;
							//fpG.putPixelValue(u, v, 0);
							fpG_array[u_rect + v_offset] = 0;
							//fpB.putPixelValue(u, v, 0);
							fpB_array[u_rect + v_offset] = 0;						
							
							cp_mask.putPixelValue(u_rect, v_rect, 0);
						}
					}
				}
			}
			/*
			(new ImagePlus("Red", fpR)).show();
			(new ImagePlus("Green", fpG)).show();
			(new ImagePlus("Blue", fpB)).show();*/
			
		} /* end run method */
		
	} /* end ColorResultTileMaker class */	
	
	/*-------------------------------------------------------------------*/
	/**
	 * Show the transformation (calculating entire transformation tables).
	 *
	 * @deprecated
	 * 
	 * @param intervals number of intervals in the deformation
	 * @param cx x- deformation coefficients
	 * @param cy y- deformation coefficients
	 * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 */
	private void showTransformation(
			final int   intervals,
			final double [][]cx,    // Input, spline coefficients
			final double [][]cy,
			boolean bIsReverse)
	{
		boolean show_deformation = false;

		BSplineModel auxTarget = target;
		BSplineModel auxSource = source;
		Mask auxTargetMsk = targetMsk;
		Mask auxSourceMsk = sourceMsk;
		int auxTargetWidth = this.targetWidth;
		int auxTargetHeight = this.targetHeight;
		ImagePlus output_ip = this.output_ip_1;
		ImageProcessor originalIP = this.originalSourceIP;

		// Change if necessary
		if(bIsReverse)
		{
			auxTarget = source;
			auxSource = target;
			auxTargetMsk = sourceMsk;
			auxSourceMsk = targetMsk;
			auxTargetWidth = this.sourceWidth;
			auxTargetHeight = this.sourceHeight;
			output_ip = this.output_ip_2;
			originalIP = this.originalTargetIP;
		}
		
		if(auxSource.isSubOutput())
		{
			output_ip.close();
			output_ip = new ImagePlus();		
		}

		// Ask for memory for the transformation
		double [][] transformation_x = new double [auxTargetHeight][auxTargetWidth];
		double [][] transformation_y = new double [auxTargetHeight][auxTargetWidth];

		// Compute the deformation
		computeDeformation(intervals, cx, cy, transformation_x, transformation_y, bIsReverse);

		if (show_deformation)
		{
			MiscTools.showImage("Transf. X", transformation_x);
			MiscTools.showImage("Transf. Y", transformation_y);
		}

		/* GRAY SCALE IMAGES */
		if(!(originalIP instanceof ColorProcessor))
		{
			// Compute the warped image
			final FloatProcessor fp = new FloatProcessor(auxTargetWidth, auxTargetHeight);
			float[] fp_array = (float[]) fp.getPixels();
			final FloatProcessor fp_mask = new FloatProcessor(auxTargetWidth, auxTargetHeight);
			float[] fp_mask_array = (float[]) fp_mask.getPixels();
			final FloatProcessor fp_target = new FloatProcessor(auxTargetWidth, auxTargetHeight);
			float[] fp_target_array = (float[]) fp_target.getPixels();

			int uv = 0;

			for (int v=0; v<auxTargetHeight; v++)
			{
				int v_offset = v * auxTargetWidth;
				for (int u=0; u<auxTargetWidth; u++,uv++)
				{
					//fp_target.putPixelValue(u, v, auxTarget.getImage()[uv]);
					fp_target_array[u + v_offset] = (float) auxTarget.getImage()[uv];
					
					if (!auxTargetMsk.getValue(u,v))
					{
						//fp.putPixelValue(u,v,0);
						fp_array[u + v_offset] = 0;
						//fp_mask.putPixelValue(u,v,0);
						fp_mask_array[u + v_offset] = 0;
					}
					else
					{
						final double x = transformation_x[v][u];
						final double y = transformation_y[v][u];
						if (auxSourceMsk.getValue(x,y))
						{
							auxSource.prepareForInterpolation(x,y,ORIGINAL);
							double sval = auxSource.interpolateI();
							//fp.putPixelValue(u,v,sval);
							fp_array[u + v_offset] = (float) sval;
							//fp_mask.putPixelValue(u,v,255);
							fp_mask_array[u + v_offset] = 255;
						}
						else
						{
							//fp.putPixelValue(u,v,0);
							fp_array[u + v_offset] = 0;
							//fp_mask.putPixelValue(u,v,0);
							fp_mask_array[u + v_offset] = 0;
						}
					}
				}
			}
			fp.resetMinAndMax();
			final ImageStack is = new ImageStack(auxTargetWidth, auxTargetHeight);

			String s = bIsReverse ? new String("Target") : new String("Source");
			is.addSlice("Registered " + s + " Image", fp);
			if (outputLevel > -1)
				is.addSlice("Target Image", fp_target);
			if (outputLevel > -1)
				is.addSlice("Warped Source Mask",fp_mask);

			if (outputLevel == 2)
			{
				computeDeformationVectors(intervals, cx, cy, is, bIsReverse);
				computeDeformationGrid(intervals, cx, cy, is, bIsReverse);
			}
			output_ip.setStack("Registered " + s + " Image", is);
			output_ip.setSlice(1);
			output_ip.getProcessor().resetMinAndMax();
			if (outputLevel > -1)
				output_ip.updateAndRepaintWindow();
		}
		else /* COLOR IMAGES */
		{
			// Compute the warped image
			// red
			BSplineModel sourceR = new BSplineModel( ((ColorProcessor) originalIP).toFloat(0, null), false, 1);
			sourceR.setPyramidDepth(0);
			sourceR.startPyramids();
			// green
			BSplineModel sourceG = new BSplineModel( ((ColorProcessor) originalIP).toFloat(1, null), false, 1);
			sourceG.setPyramidDepth(0);
			sourceG.startPyramids();
			//blue
			BSplineModel sourceB = new BSplineModel( ((ColorProcessor) originalIP).toFloat(2, null), false, 1);
			sourceB.setPyramidDepth(0);
			sourceB.startPyramids();

			// Join threads
			try {
				sourceR.getThread().join();
				sourceG.getThread().join();
				sourceB.getThread().join();
			} catch (InterruptedException e) {
				IJ.error("Unexpected interruption exception " + e);
			}

			// Calculate warped RGB image
			ColorProcessor cp = new ColorProcessor(auxTargetWidth, auxTargetHeight);
			FloatProcessor fpR = new FloatProcessor(auxTargetWidth, auxTargetHeight);
			float[] fpR_array = (float[]) fpR.getPixels();
			FloatProcessor fpG = new FloatProcessor(auxTargetWidth, auxTargetHeight);
			float[] fpG_array = (float[]) fpG.getPixels();
			FloatProcessor fpB = new FloatProcessor(auxTargetWidth, auxTargetHeight);
			float[] fpB_array = (float[]) fpB.getPixels();
			ColorProcessor cp_mask = new ColorProcessor(auxTargetWidth, auxTargetHeight);			
			
			for (int v=0; v<targetHeight; v++)
			{
				int v_offset = v * auxTargetWidth;
				
				for (int u=0; u<targetWidth; u++)
				{
					if (!auxTargetMsk.getValue(u,v))
					{
						//fpR.putPixelValue(u, v, 0);
						fpR_array[u + v_offset] = 0;
						//fpG.putPixelValue(u, v, 0);
						fpG_array[u + v_offset] = 0;
						//fpB.putPixelValue(u, v, 0);
						fpB_array[u + v_offset] = 0;
						
						cp_mask.putPixelValue(u,v,0);						
					}
					else 
					{
						final double x = transformation_x[v][u];
						final double y = transformation_y[v][u];

						if (auxSourceMsk.getValue(x,y))
						{                	 
							sourceR.prepareForInterpolation(x, y, ORIGINAL);
							//fpR.putPixelValue(u, v, sourceR.interpolateI());
							fpR_array[u + v_offset] = (float) sourceR.interpolateI();

							sourceG.prepareForInterpolation(x, y, ORIGINAL);
							//fpG.putPixelValue(u, v, sourceG.interpolateI());
							fpG_array[u + v_offset] = (float) sourceG.interpolateI();

							sourceB.prepareForInterpolation(x, y, ORIGINAL);
							//fpB.putPixelValue(u, v, sourceB.interpolateI());
							fpB_array[u + v_offset] = (float) sourceB.interpolateI();
							
							cp_mask.putPixelValue(u,v,255);							
						}
						else
						{
							//fpR.putPixelValue(u, v, 0);
							fpR_array[u + v_offset] = 0;
							//fpG.putPixelValue(u, v, 0);
							fpG_array[u + v_offset] = 0;
							//fpB.putPixelValue(u, v, 0);
							fpB_array[u + v_offset] = 0;						
							
							cp_mask.putPixelValue(u,v,0);							
						}
					}
				}
			}
			cp.setPixels(0, fpR);
			cp.setPixels(1, fpG);
			cp.setPixels(2, fpB);            
			cp.resetMinAndMax();


			final ImageStack is = new ImageStack(auxTargetWidth, auxTargetHeight);

			String s = bIsReverse ? new String("Target") : new String("Source");
			is.addSlice("Registered " + s + " Image", cp);
			if (outputLevel > -1)
				is.addSlice("Target Image", bIsReverse ? this.originalSourceIP : this.originalTargetIP);    		   
			if (outputLevel > -1)
				is.addSlice("Warped Source Mask", cp_mask);

			if (outputLevel == 2)
			{
				computeDeformationVectors(intervals, cx, cy, is, bIsReverse);
				computeDeformationGrid(intervals, cx, cy, is, bIsReverse);
			}
			output_ip.setStack("Registered " + s + " Image", is);
			output_ip.setSlice(1);
			output_ip.getProcessor().resetMinAndMax();
			if (outputLevel > -1)
				output_ip.updateAndRepaintWindow();					
		} // end caculate warped color image
		
		if(auxSource.isSubOutput())
			output_ip.show();
		
	}	// end showTransformation 
	
	//------------------------------------------------------------------
	/**
	 * Method to update both current outputs
	 * (source-target and target-source).
	 *
	 * @param c B-spline coefficients
	 * @param intervals number of intervals in the deformation
	 */
	private void update_outputs(
			final double []c,
			int intervals)
	{
		final int M = c.length / 2;
		final int halfM = M / 2;
		double []x1 = new double [M];

		for(int i = 0, p = 0; i<halfM; i++, p++)
		{
			x1[p] = c[i];
			x1[p + halfM] = c[i + M];
		}

		double []x2 = new double [M];
		for(int i = halfM, p = 0; i<M; i++, p++)
		{
			x2[p] = c[i];
			x2[p + halfM] = c[i + M];
		}
		// Updates.
		update_current_output(x1, intervals, false);
		update_current_output(x2, intervals, true);
	}
	//------------------------------------------------------------------
	/**
	 * Method to update a current output (multi-thread).
	 *
	 * @param c B-spline coefficients
	 * @param intervals number of intervals in the deformation
	 * @param bIsReverse flag to decide the deformation direction (source-target, target-source)
	 */
	private void update_current_output(
			final double []c,
			int intervals,
			boolean bIsReverse)
	{
		// Set the coefficients to an interpolator
		int cYdim = intervals+3;
		int cXdim = cYdim;
		int Nk = cYdim*cXdim;

		BSplineModel auxTarget = target;
		BSplineModel auxSource = source;
		Mask auxTargetMsk = targetMsk;
		Mask auxSourceMsk = sourceMsk;
		BSplineModel swx = swxTargetToSource;
		BSplineModel swy = swyTargetToSource;
		int auxTargetWidth = this.targetWidth;
		int auxTargetHeight = this.targetHeight;
		int auxTargetCurrentWidth = this.targetCurrentWidth;
		int auxTargetCurrentHeight = this.targetCurrentHeight;
		int auxSourceWidth = this.sourceWidth;
		int auxSourceHeight = this.sourceHeight;
		ImagePlus auxSourceImp = this.sourceImp;
		ImagePlus output_ip = this.output_ip_1;
		double auxFactorWidth = this.targetFactorWidth;
		double auxFactorHeight = this.targetFactorHeight;
		double subFactorWidth = target.isSubOutput() ? (target.getWidth() / target.getSubWidth()) : 1;
		double subFactorHeight = target.isSubOutput() ? (target.getHeight() / target.getSubHeight()) : 1;
		
		// Change if necessary
		if(bIsReverse)
		{
			auxTarget = source;
			auxSource = target;
			auxTargetMsk = sourceMsk;
			auxSourceMsk = targetMsk;
			swx = swxSourceToTarget;
			swy = swySourceToTarget;
			auxTargetWidth = this.sourceWidth;
			auxTargetHeight = this.sourceHeight;
			auxTargetCurrentWidth = sourceCurrentWidth;
			auxTargetCurrentHeight = sourceCurrentHeight;
			auxSourceWidth = this.targetWidth;
			auxSourceHeight = this.targetHeight;
			auxSourceImp = this.targetImp;
			output_ip = this.output_ip_2;
			auxFactorWidth = this.sourceFactorWidth;
			auxFactorHeight = this.sourceFactorHeight;
			subFactorWidth = source.isSubOutput() ? (source.getWidth() / source.getSubWidth()) : 1;
			subFactorHeight = source.isSubOutput() ? (source.getHeight() / source.getSubHeight()) : 1;
		}

		swx.setCoefficients(c, cYdim, cXdim, 0);
		swy.setCoefficients(c, cYdim, cXdim, Nk);

		// Compute the deformed image
		FloatProcessor fp = (FloatProcessor) output_ip.getProcessor();
		
		int uv = 0;

		// Check the number of processors in the computer 
		int nproc = Runtime.getRuntime().availableProcessors();

		// We will use threads to display parts of the output image
		int block_height = auxTargetHeight / ((int)subFactorHeight * nproc);
		if (auxTargetHeight % 2 != 0) 
			block_height++;
		
		
		int nThreads = nproc; /*(nproc > 1) ? (nproc / 2) : 1;
		if (this.accurate_mode == MainDialog.MONO_MODE)
			nThreads *= 2;*/
		
		Thread[] threads  = new Thread[nThreads];
		Rectangle[] rects = new Rectangle[nThreads];
		FloatProcessor[] fp_tile = new FloatProcessor[nThreads];
		for (int i=0; i<nThreads; i++) 
		{
			// Last block goes to the end of the window
			int x_start = i * block_height;
			if (nThreads-1 == i) 
				block_height = auxTargetHeight / (int)subFactorWidth - i*block_height;
			/*
			IJ.log("block height " + block_height);
			IJ.log("Update : 0 " + " "+ x_start  +" " + (auxTargetWidth / (int)subFactorWidth) + " " + block_height);
			IJ.log("auxFactorWidth = " + auxFactorWidth + " auxFactorHeight = " + auxFactorHeight);
			*/
			rects[i] = new Rectangle(0, x_start, auxTargetWidth / (int)subFactorWidth, block_height);
			
			fp_tile[i] = new FloatProcessor(rects[i].width, rects[i].height);
			
			threads[i] = new Thread(new OutputTileMaker(swx, swy, auxSource, auxTarget,
			 		  								auxSourceMsk, auxTargetMsk, 
			 		  								auxFactorWidth * subFactorWidth, 
			 		  								auxFactorHeight * subFactorHeight,
			 		  								auxTargetCurrentHeight, auxTargetCurrentWidth,
			 		  								rects[i], fp_tile[i]));
			threads[i].start();
		}
		for (int i=0; i<nThreads; i++) 
		{
			try {
				threads[i].join();
				threads[i] = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		
		for (int i=0; i<nThreads; i++) 
		{
			fp.insert(fp_tile[i], rects[i].x, rects[i].y);
			fp_tile[i] = null;
			rects[i] = null;
		}


		double min_val = output_ip.getProcessor().getMin();
		double max_val = output_ip.getProcessor().getMax();
		fp.setMinAndMax(min_val, max_val);
		
		output_ip.updateAndDraw();
		
		// Draw the grid on the target image ...............................
		
		// We take the values from the original image
		auxTargetHeight = bIsReverse ? this.originalSourceIP.getHeight() : this.originalTargetIP.getHeight();
		auxTargetWidth = bIsReverse ? this.originalSourceIP.getWidth() : this.originalTargetIP.getWidth();
		auxSourceHeight = bIsReverse ? this.originalTargetIP.getHeight() : this.originalSourceIP.getHeight();
		auxSourceWidth = bIsReverse ? this.originalTargetIP.getWidth() : this.originalSourceIP.getWidth();
		
		auxFactorWidth = (double) auxTargetCurrentWidth / auxTargetWidth;
		auxFactorHeight = (double) auxTargetCurrentHeight / auxTargetHeight;
		
			
		
		// Some initialization
		int stepv = Math.min(Math.max(10, auxTargetHeight/15), 60);
		int stepu = Math.min(Math.max(10, auxTargetWidth/15), 60);
		final double transformedImage [][] = new double [auxSourceHeight][auxSourceWidth];
		double grid_colour = -1e-10;
		uv = 0;
		for (int v=0; v<auxSourceHeight; v++)
			for (int u=0; u<auxSourceWidth; u++,uv++)
			{
				transformedImage[v][u] = auxSource.getOriginalImage()[uv];
				if (transformedImage[v][u]>grid_colour) 
					grid_colour = transformedImage[v][u];
			}

		// Draw grid
		for (int v=0; v<auxTargetHeight+stepv; v+=stepv)
			for (int u=0; u<auxTargetWidth+stepu; u+=stepu)
			{
				// down_u/v are the coordinates in the current image
				double down_u = u * auxFactorWidth;
				double down_v = v * auxFactorHeight;
				
				// tv,tu are the corresponding coordinates in the interpolator 
				final double tv = (double)(down_v * intervals) / (double)(auxTargetCurrentHeight-1) + 1.0F;
				final double tu = (double)(down_u * intervals) / (double)(auxTargetCurrentWidth -1) + 1.0F;
				
				// x,y are the coordinates after the transformation
				swx.prepareForInterpolation(tu,tv,ORIGINAL);
				double x = swx.interpolateI();
				swy.prepareForInterpolation(tu,tv,ORIGINAL);
				double y = swy.interpolateI();
				
				// up_x, up_y are the transformed coordinates in the original image
				double up_x = x / auxFactorWidth;
				double up_y = y / auxFactorHeight;

				// Draw horizontal line
				int uh=u+stepu;
				if (uh<auxTargetWidth+stepu)
				{
					final double down_uh = uh * auxFactorWidth;
					final double tuh = (double)(down_uh * intervals)/(double)(auxTargetCurrentWidth -1) + 1.0F;
					swx.prepareForInterpolation(tuh,tv,ORIGINAL);
					final double xh = swx.interpolateI();
					swy.prepareForInterpolation(tuh,tv,ORIGINAL);
					final double yh = swy.interpolateI();
					final double up_xh = xh / auxFactorWidth;
					final double up_yh = yh / auxFactorHeight;
					MiscTools.drawLine(
							transformedImage,
							(int)Math.round(up_x) ,(int)Math.round(up_y),
							(int)Math.round(up_xh),(int)Math.round(up_yh),grid_colour);
				}

				// Draw vertical line
				int vv=v+stepv;
				if (vv<auxTargetHeight+stepv) 
				{
					double down_vv= vv * auxFactorHeight;
					final double tvv = (double)(down_vv * intervals)/(double)(auxTargetCurrentHeight-1) + 1.0F;
					swx.prepareForInterpolation(tu,tvv,ORIGINAL); 
					double xv=swx.interpolateI();
					swy.prepareForInterpolation(tu,tvv,ORIGINAL); 
					double yv=swy.interpolateI();
					double up_xv=xv / auxFactorWidth;
					double up_yv=yv / auxFactorHeight;
					MiscTools.drawLine(
							transformedImage,
							(int)Math.round(up_x) , (int)Math.round(up_y),
							(int)Math.round(up_xv), (int)Math.round(up_yv), grid_colour);
				}
			}

		// Update the target image plus
		FloatProcessor fpg=new FloatProcessor(auxSourceWidth, auxSourceHeight);
		for (int v=0; v<auxSourceHeight; v++)
			for (int u=0; u<auxSourceWidth; u++)
				fpg.putPixelValue(u,v,transformedImage[v][u]);
		
		min_val = auxSourceImp.getProcessor().getMin();
		max_val = auxSourceImp.getProcessor().getMax();
		fpg.setMinAndMax(min_val,max_val);
		auxSourceImp.setProcessor(auxSourceImp.getTitle(),fpg);
		auxSourceImp.updateImage();
	}
	
	/* ------------------------------------------------------------------------ */
	/**
	 *  Class to run concurrent tile windows updaters (for intermediate results)
	 * 	 
	 */	
	private class OutputTileMaker implements Runnable 
	{
		final BSplineModel swx;
		final BSplineModel swy;		
		final BSplineModel auxSource;	
		final BSplineModel auxTarget;
		final Mask auxTargetMsk;
		final Mask auxSourceMsk;
		final double auxFactorWidth;
		final double auxFactorHeight;
		final int auxTargetCurrentHeight;
		final int auxTargetCurrentWidth;		
		final Rectangle rect;
		final private FloatProcessor fp;
		
		//------------------------------------------------------------------
		/**
		 * Output tile maker constructor
		 * 
		 * @param swx B-spline interpolator for transformation in x-
		 * @param swy B-spline interpolator for transformation in y-
		 * @param auxSource source image
		 * @param auxTarget target image
		 * @param auxSourceMsk source mask
		 * @param auxTargetMsk target mask		 
		 * @param auxFactorWidth width factor
		 * @param auxFactorHeight height factor
		 * @param auxTargetCurrentHeight current target height
		 * @param auxTargetCurrentWidth current target width
		 * @param rect retangle with the coordinates of the output image to be updated
		 * @param fp processor to be updated 
		 */
		OutputTileMaker(
				final BSplineModel swx, 
		 		final BSplineModel swy, 
		 		final BSplineModel auxSource,
		 		final BSplineModel auxTarget,		 		  
		 		final Mask auxSourceMsk,
		 		final Mask auxTargetMsk,
		 		final double auxFactorWidth,
		 		final double auxFactorHeight,
		 		final int auxTargetCurrentHeight,
		 		final int auxTargetCurrentWidth,	
		 		final Rectangle rect, 
		 		final FloatProcessor fp)
		{
			this.swx = swx;
			this.swy = swy;
			this.auxSource = auxSource;
			this.auxTarget = auxTarget;
			this.auxTargetMsk = auxTargetMsk;
			this.auxSourceMsk = auxSourceMsk;
			this.auxFactorWidth = auxFactorWidth;
			this.auxFactorHeight = auxFactorHeight;
			this.auxTargetCurrentWidth = auxTargetCurrentWidth;
			this.auxTargetCurrentHeight = auxTargetCurrentHeight;
			this.rect = rect;
			this.fp = fp;
		}
	
		//------------------------------------------------------------------
		/**
		 * Run method to update the intermediate window. Only the part defined by
		 * the rectangle will be updated (in this thread).
		 */
		public void run() 
		{
			int uv = rect.y * rect.width + rect.x;
			int auxTargetHeight = rect.y + rect.height;
			int auxTargetWidth = rect.x + rect.width;
			
			// Subsampling (output) factors
			// Note: we get them from original image, since they will be used in the masks and the
			//       masks store the information relative to the original sizes (without scaling).
			final int subFactorT = this.auxTarget.getOriginalImageWidth() / this.auxTarget.getSubWidth();
			final int subFactorS = this.auxSource.getOriginalImageWidth() / this.auxSource.getSubWidth();
			
			final boolean fromSubT = (auxTarget.isSubOutput());
			final boolean fromSubS = (auxSource.isSubOutput());
			
			final double[]tImage = fromSubT ? auxTarget.getSubImage() : auxTarget.getImage();
			final float [] f_array = (float[]) fp.getPixels();
			
			for (int v_rect = 0, v=rect.y; v<auxTargetHeight; v++, v_rect++)
			{
				final int v_offset = v_rect * rect.width;
				
				for (int u_rect = 0, u=rect.x; u<auxTargetWidth; u++, uv++, u_rect++) 
				{
					if (auxTargetMsk.getValue(u * subFactorT, v * subFactorT)) 
					{
						double down_u = u*auxFactorWidth;
						double down_v = v*auxFactorHeight;
						final double tv = (double)(down_v * intervals)/(double)(auxTargetCurrentHeight-1) + 1.0F;
						final double tu = (double)(down_u * intervals)/(double)(auxTargetCurrentWidth -1) + 1.0F;						
						double x = swx.prepareForInterpolationAndInterpolateI(tu, tv, fromSubT, ORIGINAL);						
						double y = swy.prepareForInterpolationAndInterpolateI(tu, tv, fromSubT, ORIGINAL);			
						double up_x = x/auxFactorWidth;
						double up_y = y/auxFactorHeight;
						if (auxSourceMsk.getValue(up_x * subFactorS, up_y * subFactorS)) 
						{
							double sourceValue = auxSource.prepareForInterpolationAndInterpolateI(up_x, up_y, fromSubS, ORIGINAL);										
							//fp.putPixelValue(u_rect, v_rect, tImage[uv] - sourceValue);
							f_array[u_rect + v_offset] = (float) (tImage[uv] - sourceValue);
						} 
						else
							//fp.putPixelValue(u_rect, v_rect, 0);
							f_array[u_rect + v_offset] = 0;
					} else
						//fp.putPixelValue(u_rect, v_rect, 0);
						f_array[u_rect + v_offset] = 0;
				}
			}
			
		} // end run
	} // end OutputTileMaker class 

	//------------------------------------------------------------------
	/**
	 * Calculate the cubic B-spline x weight.
	 *
	 * @param x x- value
	 * @param xIntervals x- number of intervals
	 * @param extended extended flat
	 * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 * @return weights
	 */
	private double[] xWeight(
			final double x,
			final int xIntervals,
			final boolean extended,
			boolean bIsReverse)
	{
		int auxTargetCurrentWidth = (bIsReverse) ? this.sourceCurrentWidth : this.targetCurrentWidth;

		int length = xIntervals+1;
		int j0=0, jF = xIntervals;
		if (extended)
		{
			length+=2;
			j0--;
			jF++;
		}
		final double[] b = new double[length];
		final double interX = (double)xIntervals / (double)(auxTargetCurrentWidth - 1);
		for (int j=j0; j<=jF; j++) {
			b[j-j0] = MathTools.Bspline03(x * interX - (double)j);
		}
		return(b);
	} /* end xWeight */

	//------------------------------------------------------------------
	/**
	 * Calculate the cubic B-spline y weight.
	 *
	 * @param y y- value
	 * @param yIntervals y- number of intervals
	 * @param extended extended flat
	 * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 * @return weights
	 */
	private double[] yWeight(
			final double y,
			final int yIntervals,
			final boolean extended,
			boolean bIsReverse)
	{

		int auxTargetCurrentHeight = (bIsReverse) ? this.sourceCurrentHeight : this.targetCurrentHeight;

		int length=yIntervals+1;
		int i0=0, iF=yIntervals;
		if (extended)
		{
			length+=2;
			i0--;
			iF++;
		}
		final double[] b = new double[length];
		final double interY = (double)yIntervals / (double)(auxTargetCurrentHeight - 1);
		for (int i = i0; i<=iF; i++) {
			b[i-i0] = MathTools.Bspline03(y * interY - (double)i);
		}
		return(b);
	} /* end yWeight */

	
	/*--------------------------------------------------------------------------*/
	/**
	 * Evaluate the similarity between the source and the target images but also
	 * the transformation regularization and and landmarks energy term if necessary.
	 * Multi-threading version.
	 *
	 * @param c Input: Deformation coefficients
	 * @param intervals Input: Number of intervals for the deformation
	 * @param grad Output: Gradient of the similarity
	 * @param only_image Input: if true, only the image term is considered and not the regularization
	 * @param bIsReverse Input: flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
	 * @return images similarity value
	 */
	
	private double evaluateSimilarityMultiThread(
			final double []c,
			final int intervals,
			double []grad,
			final boolean only_image,
			boolean bIsReverse)
	{

		// Auxiliary variables for changing from source to target and inversely
		final BSplineModel auxTarget = (!bIsReverse) ?  target : source;
		final BSplineModel auxSource = (!bIsReverse) ? source : target;

		final Mask auxTargetMsk = (!bIsReverse) ? targetMsk : sourceMsk;
		final Mask auxSourceMsk = (!bIsReverse) ? sourceMsk : targetMsk;

		final PointHandler auxTargetPh = (!bIsReverse) ? targetPh : sourcePh;
		final PointHandler auxSourcePh = (!bIsReverse) ? sourcePh : targetPh;

		final BSplineModel swx = (!bIsReverse) ? swxTargetToSource : swxSourceToTarget;
		final BSplineModel swy = (!bIsReverse) ? swyTargetToSource : swySourceToTarget;

		final double auxFactorWidth = (!bIsReverse) ? this.target.getFactorWidth() : this.sourceFactorWidth;
		final double auxFactorHeight = (!bIsReverse) ? this.target.getFactorHeight() : this.sourceFactorHeight;

		final double P11[][] = (!bIsReverse) ? this.P11_TargetToSource : this.P11_SourceToTarget;
		final double P12[][] = (!bIsReverse) ? this.P12_TargetToSource : this.P12_SourceToTarget;
		final double P22[][] = (!bIsReverse) ? this.P22_TargetToSource : this.P12_SourceToTarget;

		final int auxTargetCurrentWidth = (!bIsReverse) ? this.targetCurrentWidth : this.sourceCurrentWidth;
		final int auxTargetCurrentHeight = (!bIsReverse) ? this.targetCurrentHeight : this.sourceCurrentHeight;


		final int cYdim = intervals+3;
		final int cXdim = cYdim;
		final int Nk = cYdim * cXdim;
		final int twiceNk = 2 * Nk;
		
		final double []vgradreg = new double[grad.length];
		final double []vgradland = new double[grad.length];

		// Set the transformation coefficients to the interpolator
		swx.setCoefficients(c, cYdim, cXdim, 0);
		swy.setCoefficients(c, cYdim, cXdim, Nk);

		// Initialize gradient
		for (int k=0; k<twiceNk; k++) 
			vgradreg[k]=vgradland[k]=grad[k]=0.0F;

		// Estimate the similarity and gradient between both images
		double imageSimilarity = 0.0;
		
		//final int Ydim = auxTarget.getCurrentHeight();
		//final int Xdim = auxTarget.getCurrentWidth();


		// Image similarity calculated in a concurrent way
		if(imageWeight != 0)
		{
			// Check the number of processors in the computer 
			final int nproc = Runtime.getRuntime().availableProcessors();

			// We will use threads to calculate the similarity of the different 
			// parts of the target and source image
			int block_height = auxTargetCurrentHeight / nproc;
			if (auxTargetCurrentHeight % 2 != 0) 
				block_height++;
			
			// We use as many threads as processors
			final int nThreads = nproc; 
			
			Thread[] threads  = new Thread[nThreads];
			Rectangle[] rects = new Rectangle[nThreads];
			
			// Every thread will provide the corresponding similarity value and
			// gradient
			final double [][]grad_thread = new double[nThreads][grad.length];
			// Result array:
			// First result is the partial image similarity and second the number of pixels
			final double [][]result = new double[nThreads][2];
			// Number of processed pixels (taking into account the masks)
			int n = 0;
			
			for (int i=0; i<nThreads; i++) 
			{
				// Last block goes to the end of the window
				int y_start = i * block_height;
				if (nThreads-1 == i) 
					block_height = auxTargetCurrentHeight  - i * block_height;
				
				// Corresponding rectangle
				rects[i] = new Rectangle(0, y_start, auxTargetCurrentWidth, block_height);
				
				
				// Create threads and start them.
				threads[i] = new Thread(new EvaluateSimilarityTile(auxTarget, auxSource, auxTargetMsk,
							   										auxSourceMsk, swx, swy, auxFactorWidth, auxFactorHeight,
							   										intervals, grad_thread[i], result[i],
							   										rects[i]));
				threads[i].start();
			}
			
			// Wait for the threads to finish
			for (int i=0; i<nThreads; i++) 
			{
				try {
					threads[i].join();
					threads[i] = null;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}			
			}
			
			// Accumulate results
			for (int i=0; i<nThreads; i++) 
			{
				imageSimilarity += result[i][0];
				n += result[i][1];								
			}
			
			// Average image similarity
			imageSimilarity /= n;
			// Average gradients
			for(int i = 0; i<nThreads; i++)
			{
				for(int j = 0; j < grad.length; j++)
					grad[j] += (grad_thread[i][j]/n);
			}
			
		}
		
		

		// Compute regularization term ..............................................
		double regularization = 0.0;
		if (!only_image)
		{
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
			regularization*=1.0/(auxTargetCurrentHeight * auxTargetCurrentWidth);
			for (int k=0; k<twiceNk; k++) 
				vgradreg [k]*=1.0/(auxTargetCurrentHeight * auxTargetCurrentWidth);
		}

		// Compute landmark error and derivative ...............................
		// Get the list of landmarks
		double landmarkError = 0.0;
		int K = 0;
		if (auxTargetPh!=null) 
			K = auxTargetPh.getPoints().size();
		
		if (landmarkWeight != 0)
		{
			Vector <Point> sourceVector = null;
			if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
			else                   sourceVector = new Vector <Point> ();
			Vector <Point> targetVector = null;
			if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
			else                   targetVector = new Vector <Point> ();

			for (int kp=0; kp<K; kp++)
			{
				// Get the landmark coordinate in the target image
				final Point sourcePoint = (Point)sourceVector.elementAt(kp);
				final Point targetPoint = (Point)targetVector.elementAt(kp);
				double u = auxFactorWidth *(double)targetPoint.x;
				double v = auxFactorHeight*(double)targetPoint.y;

				// Express it in "spline" units
				double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth  - 1) + 1.0F;
				double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;

				// Transform this coordinate to the source image
				swx.prepareForInterpolation(tu, tv, false);
				double x = swx.interpolateI();
				swy.prepareForInterpolation(tu, tv, false);
				double y = swy.interpolateI();

				// Substract the result from the residual
				double dx = auxFactorWidth  * (double)sourcePoint.x - x;
				double dy = auxFactorHeight * (double)sourcePoint.y - y;

				// Add to landmark error
				landmarkError += dx*dx + dy*dy;

				// Compute the derivative with respect to all the c coefficients
				for (int l=0; l<4; l++)
					for (int m=0; m<4; m++)
					{
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

		if (K!=0)
		{
			landmarkError *= landmarkWeight/K;
			double aux = 2.0 * landmarkWeight/K;
			// This is the 2 coming from the derivative
			// computation that I would do at the end
			for (int k=0; k<twiceNk; k++) 
				vgradland[k] *= aux;
		}
		if (only_image) landmarkError = 0;
		
		
		
		// Finish computations .............................................................
		// Add all gradient terms (similarity + regularization + landmarks)
		for (int k=0; k<twiceNk; k++)
			grad[k] += vgradreg[k] + vgradland[k];


		if (showMarquardtOptim)
		{
			String s = bIsReverse ? new String("(t-s)") : new String("(s-t)");
			if (imageWeight != 0) 
			{
				IJ.write("    Image          error " + s + ": " + imageSimilarity);
				if(bIsReverse)
					this.partialInverseSimilarityError = imageSimilarity;
				else
					this.partialDirectSimilarityError = imageSimilarity;

			}
			if (landmarkWeight != 0)               
			{
				IJ.write("    Landmark       error " + s + ": " + landmarkError);
				if(bIsReverse)
					this.partialInverseLandmarkError = landmarkError;
				else
					this.partialDirectLandmarkError = landmarkError;
			}
			if (divWeight != 0 || curlWeight != 0)
			{
				IJ.write("    Regularization error " + s + ": " + regularization);
				if(bIsReverse)
					this.partialInverseRegularizationError = regularization;
				else
					this.partialDirectRegularizationError = regularization;
					
			}
		}
		return imageSimilarity + landmarkError + regularization;
	}
	
	/* ------------------------------------------------------------------------ */
	/**
	 *  Class to run concurrent similarity evaluation
	 * 	 
	 */		
	private class EvaluateSimilarityTile implements Runnable 
	{
		// Fields
		/** current target image */
		final BSplineModel auxTarget;
		/** current source image */
		final BSplineModel auxSource;
		/** target mask */
		final Mask auxTargetMsk;
		/** source mask */
		final Mask auxSourceMsk;
		/** B-spline deformation in x */
		final BSplineModel swx;
		/** B-spline deformation in y */
		final BSplineModel swy;
		/** factor width */
		final double auxFactorWidth;
		/** factor height */
		final double auxFactorHeight;
		/** number of intervals between B-spline coefficients */
		final int intervals;
		/** similarity gradient */
		final double[] grad;
		/** evaluation results: image similarity value for the current rectangle and number of pixels that have been evaluated */
		final double[] result;
		/** rectangle containing the area of the image to be evaluated */
		final Rectangle rect;
		
		/**
		 * Evaluate similarity tile constructor 
		 * 
		 * @param auxTarget current target image
		 * @param auxSource current source image
		 * @param auxTargetMsk target mask
		 * @param auxSourceMsk source mask
		 * @param swx B-spline deformation in x
		 * @param swy B-spline deformation in y
		 * @param auxFactorWidth factor width
		 * @param auxFactorHeight factor height
		 * @param intervals number of intervals between B-spline coefficients
		 * @param grad similarity gradient (output)
		 * @param result output results: image similarity value for the current rectangle and number of pixels that have been evaluated
		 * @param rect rectangle containing the area of the image to be evaluated
		 */
		EvaluateSimilarityTile(final BSplineModel auxTarget,
							   final BSplineModel auxSource,
							   final Mask auxTargetMsk,
							   final Mask auxSourceMsk,
							   final BSplineModel swx,
							   final BSplineModel swy,
							   final double auxFactorWidth,
							   final double auxFactorHeight,
							   final int intervals,
							   final double[] grad,
							   final double[] result,
							   final Rectangle rect)
		{
			this.auxTarget = auxTarget;
			this.auxSource = auxSource;
			
			this.auxTargetMsk = auxTargetMsk;
			this.auxSourceMsk = auxSourceMsk;
			
			this.swx = swx;
			this.swy = swy;
			
			this.auxFactorWidth = auxFactorWidth;
			this.auxFactorHeight = auxFactorHeight;
					
			this.intervals = intervals;
			
			this.grad = grad;
			
			this.result = result;
		
			this.rect = rect;
		}

		//------------------------------------------------------------------
		/**
		 * Run method to evaluate the similarity of source and target images. 
		 * Only the part defined by the rectangle will be evaluated.
		 */
	
		public void run() 
		{
			final int cYdim = intervals+3;
			final int cXdim = cYdim;
			final int Nk = cYdim * cXdim;
			final int twiceNk = 2 * Nk;
			
			double imageSimilarity = 0.0;
			
			// The rectangle marks the area of the image to be treated.
			int uv = rect.y * rect.width + rect.x;
			final int Ydim = rect.y + rect.height;
			final int Xdim = rect.x + rect.width;						
			
			// Loop over all points in the source image (rectangle)
			int n = 0;
			

			final double []I1D = new double[2]; // Space for the first derivatives of I1

			final double []targetCurrentImage = auxTarget.getCurrentImage();

			for (int v=rect.y; v<Ydim; v++)
			{										
				for (int u=rect.x; u<Xdim; u++, uv++) 
				{
					// Compute image term .....................................................

					// Check if this point is in the target mask
					if (auxTargetMsk.getValue(u/auxFactorWidth, v/auxFactorHeight))
					{
						// Compute value in the source image
						final double I2 = targetCurrentImage[uv];

						// Compute the position of this point in the target
						double x = swx.precomputed_interpolateI(u,v);
						double y = swy.precomputed_interpolateI(u,v);

						// Check if this point is in the source mask
						if (auxSourceMsk.getValue(x/auxFactorWidth, y/auxFactorHeight))
						{
							// Compute the value of the target at that point
							final double I1 = auxSource.prepareForInterpolationAndInterpolateIAndD(x, y, I1D, false, PYRAMID);							

							final double I1dx = I1D[0], I1dy = I1D[1];

							final double error = I2 - I1;
							final double error2 = error*error;							
							imageSimilarity += error2;

							// Compute the derivative with respect to all the c coefficients
							// Cost of the derivatives = 16*(3 mults + 2 sums)
							// Current cost = 359 mults + 346 sums
							for (int l=0; l<4; l++)
								for (int m=0; m<4; m++)
								{
									if (swx.prec_yIndex[v][l]==-1 || swx.prec_xIndex[u][m]==-1) continue;

									// Note: It's the same to take the indexes and weightI from swx than from swy
									double weightI = swx.precomputed_getWeightI(l,m,u,v);

									int k = swx.prec_yIndex[v][l] * cYdim + swx.prec_xIndex[u][m];

									// Compute partial result
									// There's also a multiplication by 2 that I will
									// do later
									double aux = -error * weightI;

									// Derivative related to X deformation
									grad[k]   += aux * I1dx;

									// Derivative related to Y deformation
									grad[k+Nk]+= aux * I1dy;
								}
							n++; // Another point has been successfully evaluated
						}
					}
				}
			}
			

			// Average the image related terms (now i do the 1/n outside)
			if (n!=0)
			{
				imageSimilarity *= imageWeight;
				double aux = imageWeight * 2.0; // This is the 2 coming from the
												   // derivative that I would do later
				for (int k=0; k<twiceNk; k++) 
					grad[k] *= aux;
			} 
			else
				imageSimilarity = 1/FLT_EPSILON;


			// Set result (image similarity value for the current rectangle
			// and number of pixels that have been evaluated)
			this.result[0] = imageSimilarity;		
			this.result[1] = n;			
			
		} // end run method
		
	} // end class EvaluateSimilarityTile

	
	/*--------------------------------------------------------------------------*/
	/**
	 * Calculate the geometric error between the source-target and target-source
	 * deformations. The corresponding coefficients are assumed to be at swxTargetToSource,
	 * swyTargetToSource, swxSourceToTarget and swySourceToTarget.
	 * Multi-thread version.
	 *
	 * @param intervals Input: Number of intervals for the deformation
	 * @param grad Output: Gradient of the function
	 * 
	 * @return geometric error between the source-target and target-source deformations.
	 */

	private double evaluateConsistencyMultiThread(
			final int intervals,
			double []grad)
	{
		// Consistency values
		double f_direct = 0.0;
		double f_inverse = 0.0;
		
		
		// Check the number of processors in the computer 
		final int nproc = Runtime.getRuntime().availableProcessors();

		// We will use threads to calculate the similarity of the different 
		// parts of the target and source image
		int block_height_target = this.targetCurrentHeight / nproc;
		if (this.targetCurrentHeight % 2 != 0) 
			block_height_target++;
		
		int block_height_source = this.sourceCurrentHeight / nproc;
		if (this.sourceCurrentHeight % 2 != 0) 
			block_height_source++;
		
		// We use as many threads as processors
		final int nThreads = nproc; 
		
		Thread[] threads  = new Thread[nThreads];
		Rectangle[] rect_target = new Rectangle[nThreads];
		Rectangle[] rect_source = new Rectangle[nThreads];
		
		// Every thread will provide the corresponding consistency value and
		// gradient
		final double [][]grad_direct = new double[nThreads][grad.length];
		final double [][]grad_inverse = new double[nThreads][grad.length];
		// Result array:
		// First result is the direct partial consistency, second the number of pixels (direct),
		// third the inverse partical consistency and fourth the number of pixels (inverse)
		final double [][]result = new double[nThreads][4];
		// Number of processed pixels (taking into account the masks)
		int n_direct = 0;
		int n_inverse = 0;
		
		for (int i=0; i<nThreads; i++) 
		{
			// Last block goes to the end of the window
			int y_start_target = i * block_height_target;
			int y_start_source = i * block_height_source;
			if (nThreads-1 == i) 
			{
				block_height_target = this.targetCurrentHeight - i * block_height_target;
				block_height_source = this.sourceCurrentHeight - i * block_height_source;
			}
			
			// Corresponding rectangles
			rect_target[i] = new Rectangle(0, y_start_target, this.targetCurrentHeight, block_height_target);
			rect_source[i] = new Rectangle(0, y_start_source, this.sourceCurrentHeight, block_height_source);
			
			// Create threads and start them.
			threads[i] = new Thread(new EvaluateConsistencyTile(this, grad_direct[i], grad_inverse[i], result[i],
						   										rect_target[i], rect_source[i]));
			threads[i].start();
		}
		
		// Wait for the threads to finish
		for (int i=0; i<nThreads; i++) 
		{
			try {
				threads[i].join();
				threads[i] = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		
		// Accumulate results
		for (int i=0; i<nThreads; i++) 
		{
			f_direct += result[i][0];
			n_direct += result[i][1];								
			f_inverse += result[i][2];
			n_inverse += result[i][3];
		}
		
		// Average consistency
		f_direct /= n_direct;
		f_inverse /= n_inverse;
		
		// Average and combine gradients
		for(int i = 0; i<nThreads; i++)
		{
			for(int j = 0; j < grad.length; j++)				
				grad[j] += (grad_direct[i][j]/n_direct) + (grad_inverse[i][j]/n_inverse);
		}
		
		
		this.partialDirectConsitencyError = this.consistencyWeight * f_direct;
		this.partialInverseConsitencyError = this.consistencyWeight * f_inverse;


		double consistencyDirectError = (n_direct == 0) ? 1.0/FLT_EPSILON : (this.consistencyWeight * f_direct);
		double consistencyInverseError = (n_inverse == 0) ? 1.0/FLT_EPSILON : (this.consistencyWeight * f_inverse);

		if (showMarquardtOptim)
		{
			IJ.write("    Consistency Error (s-t): " + consistencyDirectError);
			IJ.write("    Consistency Error (t-s): " + consistencyInverseError);
		}


		if(n_direct == 0 || n_inverse == 0)
			return 1/FLT_EPSILON;
		return (this.consistencyWeight * (f_direct + f_inverse));
	}
	
	/* ------------------------------------------------------------------------ */
	/**
	 *  Class to run concurrent consistency evaluation
	 * 	 
	 */		
	private class EvaluateConsistencyTile implements Runnable 
	{

		/**transformation object, it contains all the registration information  */
		final Transformation transf;
		/** output direct gradient array */
		final double[] grad_direct;
		/** output inverse gradient array */
		final double[] grad_inverse;
		/** direct and inverse consistency error values and number of pixels (f_dir, n_dir, f_inv, n_inv) */
		final double[] result;
		/** rectangle marking the target area to be evaluated */
		final Rectangle rect_target;
		/** rectangle marking the source area to be evaluated */
		final Rectangle rect_source;
		
		/**
		 * Evaluate consistency tile constructor
		 * 
		 * @param transf transformation object, it contains all the registration information
		 * @param grad_direct output direct gradient array
		 * @param grad_inverse output inverse gradient array
		 * @param result direct and inverse consistency error values and number of pixels (f_dir, n_dir, f_inv, n_inv)
		 * @param rect_target rectangle marking the target area to be evaluated
		 * @param rect_source rectangle marking the source area to be evaluated
		 */
		EvaluateConsistencyTile(Transformation transf,
								double[] grad_direct,
								double[] grad_inverse,
								double[] result,
								Rectangle rect_target,
								Rectangle rect_source)
		{
			this.transf = transf;
			
			this.grad_direct = grad_direct;
			this.grad_inverse = grad_inverse;

			this.result = result;

			this.rect_target = rect_target;
			this.rect_source = rect_source;
		}
		
		/**
		 * Run method to evaluate the transformation consistency between source and target images. 
		 * Only the part defined by the rectangle will be evaluated.
		 */
		public void run() 
		{
			final int cYdim = this.transf.intervals+3;
			final int cXdim = cYdim;
			final int Nk = cYdim * cXdim;
			final int twiceNk = 2 * Nk;

			// Initialize gradient
			for (int k = 0; k<this.grad_direct.length; k++)
				this.grad_direct[k] = 0.0F;

			// The target rectangle marks the area of the target image to be treated.			
			final int YdimT = rect_target.y + rect_target.height;
			final int XdimT = rect_target.x + rect_target.width;	
			
			
			// Compute the deformation
			// Set these coefficients to an interpolator
			final BSplineModel swx_direct = this.transf.swxTargetToSource; 
			final BSplineModel swy_direct = this.transf.swyTargetToSource;  

			final BSplineModel swx_inverse = this.transf.swxSourceToTarget; 
			final BSplineModel swy_inverse = this.transf.swySourceToTarget;

			// *********** Compute the geometric error and gradient (DIRECT) ***********       
			double f_direct = 0;
			int n_direct = 0;						
			
			for (int v=rect_target.y; v<YdimT; v++)
				for (int u=rect_target.x; u<XdimT; u++)
				{
					// Check if this point is in the target mask
					if (this.transf.targetMsk.getValue(u/this.transf.targetFactorWidth, v/this.transf.targetFactorHeight))
					{

						final int x = (int) Math.round(swx_direct.precomputed_interpolateI(u,v));
						final int y = (int) Math.round(swy_direct.precomputed_interpolateI(u,v));					 					

						if (x>=0 && x<this.transf.sourceCurrentWidth && y>=0 && y<this.transf.sourceCurrentHeight)
						{
							final double x2 = swx_inverse.precomputed_interpolateI(x,y);
							final double y2 = swy_inverse.precomputed_interpolateI(x,y);
							double aux1 = u - x2;
							double aux2 = v - y2;

							f_direct += aux1 * aux1 + aux2 * aux2;

							// Compute the derivative with respect to all the c coefficients
							// Derivatives from direct coefficients.
							for (int l=0; l<4; l++)
								for (int m=0; m<4; m++)
								{
									if (swx_direct.prec_yIndex[v][l]==-1 || swx_direct.prec_xIndex[u][m]==-1)
										continue;

									double dddx = swx_direct.precomputed_getWeightI(l, m, u, v);
									double dixx = swx_inverse.precomputed_getWeightDx(l, m, x, y);
									double diyy = swy_inverse.precomputed_getWeightDy(l, m, x, y);

									double weightIx = (dixx + diyy) * dddx;

									double dddy = swy_direct.precomputed_getWeightI(l, m, u, v);
									double dixy = swx_inverse.precomputed_getWeightDy(l, m, x, y);
									double diyx = swy_inverse.precomputed_getWeightDx(l, m, x, y);

									double weightIy = (diyx + dixy) * dddy;

									int k = swx_direct.prec_yIndex[v][l] * cYdim + swx_direct.prec_xIndex[u][m];

									// Derivative related to X deformation
									this.grad_direct[k]        += -aux1 * weightIx;

									// Derivative related to Y deformation
									this.grad_direct[k+twiceNk]+= -aux2 * weightIy;
								}


							// Derivatives from inverse coefficients.
							for (int l=0; l<4; l++)
								for (int m=0; m<4; m++)
								{
									// d inverse(direct(x)) / d c_inverse
									if (swx_inverse.prec_yIndex[y][l]==-1 || swx_inverse.prec_xIndex[x][m]==-1)
										continue;

									double weightI = swx_inverse.precomputed_getWeightI(l, m, x, y);

									int k = swx_inverse.prec_yIndex[y][l] * cYdim + swx_inverse.prec_xIndex[x][m];

									// Derivative related to X deformation
									this.grad_direct[k+Nk]        += -aux1 * weightI;

									// Derivative related to Y deformation
									this.grad_direct[k+Nk+twiceNk]+= -aux2 * weightI;
								}


							n_direct++; // Another point has been successfully evaluated
						}
					}// end if mask.
				}

			if (n_direct != 0)
			{				
				// Average the image related terms
				double aux = consistencyWeight * 2.0;  // This is the 2 coming from the
				// derivative that I would do later
				for (int k=0; k<grad_direct.length; k++)
					grad_direct[k] *= aux;
			}

			// Inverse gradient
			// Initialize 
			for (int k = 0; k<this.grad_inverse.length; k++)
				this.grad_inverse[k] = 0.0F;

			// *********** Compute the geometric error and gradient (INVERSE) ***********
			// The source rectangle marks the area of the source image to be treated.			
			final int YdimS = rect_source.y + rect_source.height;
			final int XdimS = rect_source.x + rect_source.width;
			
			double f_inverse = 0;
			int n_inverse = 0;
			for (int v=rect_source.y; v<YdimS; v++)
				for (int u=rect_source.x; u<XdimS; u++)
				{
					// Check if this point is in the target mask
					if (this.transf.sourceMsk.getValue(u/this.transf.sourceFactorWidth, v/this.transf.sourceFactorHeight))
					{
						final int x = (int) Math.round( swx_inverse.precomputed_interpolateI(u, v));
						final int y = (int) Math.round( swy_inverse.precomputed_interpolateI(u, v));

						if (x>=0 && x<this.transf.targetCurrentWidth && y>=0 && y<this.transf.targetCurrentHeight)
						{
							final double x2 = swx_direct.precomputed_interpolateI(x, y);
							final double y2 = swy_direct.precomputed_interpolateI(x, y);
							double aux1 = u - x2;
							double aux2 = v - y2;

							f_inverse += aux1 * aux1 + aux2 * aux2;

							// Compute the derivative with respect to all the c coefficients
							// Derivatives from direct coefficients.
							for (int l=0; l<4; l++)
								for (int m=0; m<4; m++)
								{
									// d direct(inverse(x)) / d c_direct
									if (swx_direct.prec_yIndex[y][l]==-1 || swx_direct.prec_xIndex[x][m]==-1)
										continue;

									double weightI = swx_direct.precomputed_getWeightI(l, m, x, y);

									int k = swx_direct.prec_yIndex[y][l] * cYdim + swx_direct.prec_xIndex[x][m];

									// Derivative related to X deformation
									this.grad_inverse[k]        += -aux1 * weightI;

									// Derivative related to Y deformation
									this.grad_inverse[k+twiceNk]+= -aux2 * weightI;
								}
							// Derivatives from inverse coefficients.
							for (int l=0; l<4; l++)
								for (int m=0; m<4; m++)
								{
									if (swx_inverse.prec_yIndex[v][l]==-1 || swx_inverse.prec_xIndex[u][m]==-1)
										continue;

									double diix = swx_inverse.precomputed_getWeightI(l, m, u, v);
									double ddxx = swx_direct.precomputed_getWeightDx(l, m, x, y);
									double ddyy = swy_direct.precomputed_getWeightDy(l, m, x, y);

									double weightIx = (ddxx + ddyy) * diix;

									double diiy = swy_inverse.precomputed_getWeightI(l, m, u, v);
									double ddxy = swx_direct.precomputed_getWeightDy(l, m, x, y);
									double ddyx = swy_direct.precomputed_getWeightDx(l, m, x, y);

									double weightIy = (ddyx + ddxy) * diiy;


									int k = swx_inverse.prec_yIndex[v][l] * cYdim + swx_inverse.prec_xIndex[u][m];

									// Derivative related to X deformation
									this.grad_inverse[k+Nk]        += -aux1 * weightIx;

									// Derivative related to Y deformation
									this.grad_inverse[k+Nk+twiceNk]+= -aux2 * weightIy;
								}

							n_inverse++; // Another point has been successfully evaluated
						}
					} // end if mask
				} // end inverse geometric error calculation

			if (n_inverse != 0)
			{				
				// Average the image related terms
				double aux = consistencyWeight * 2.0;  // This is the 2 coming from the
				// derivative that I would do later
				for(int k=0; k<this.grad_inverse.length; k++)
					this.grad_inverse[k] *= aux;
			}
			
			// Save results
			this.result[0] = f_direct;
			this.result[1] = n_direct;
			this.result[2] = f_inverse;
			this.result[3] = n_inverse;
						
		} // end run method
				
		
	}
	
	//------------------------------------------------------------------------------------------
	/**
	 * Correct shear on initial affine transform
	 * @param shearCorrection percentage of shear correction (0.0 - 1.0)
	 */
	public void setShearCorrection(double shearCorrection)
	{
		if(shearCorrection >= 0 && shearCorrection <= 1.0)
			this.tweakShear = shearCorrection;
	}
	
	//------------------------------------------------------------------------------------------
	/**
	 * Correct scale on initial affine transform
	 * @param scaleCorrection percentage of scale correction (0.0 - 1.0)
	 */
	public void setScaleCorrection(double scaleCorrection)
	{
		if(scaleCorrection >= 0 && scaleCorrection <= 1.0)
			this.tweakScale = scaleCorrection;
	}
	
	//------------------------------------------------------------------------------------------
	/**
	 * Correct anisotropy on initial affine transform
	 * @param isoCorrection percentage of anisotropy correction (0.0 - 1.0)
	 */
	public void setAnisotropyCorrection(double isoCorrection)
	{
		if(isoCorrection >= 0 && isoCorrection <= 1.0)
			this.tweakIso = isoCorrection;
	}
	
} // end class Transformation
