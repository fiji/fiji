//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
//==============================================================================

package edfgui;

public class Parameters {

	public static final int QUALITY_LOW = 0;
	public static final int QUALITY_LM = 1;
	public static final int QUALITY_MEDIUM = 2;
	public static final int QUALITY_MH = 3;
	public static final int QUALITY_HIGH = 4;

	public static final int SMOOTH_TOPO_NO = 0;
	public static final int SMOOTH_TOPO_NM = 1;
	public static final int SMOOTH_TOPO_MEDIUM = 2;
	public static final int SMOOTH_TOPO_MV = 3;
	public static final int SMOOTH_TOPO_VERY = 4;


	public static final int COLOR_RGB = 0;
	public static final int GRAYSCALE = 1;

	public boolean color;
	public int edfMethod;
	public int outputColorMap;

	public double sigma;
	public double rateDenoising;
	public double sigmaDenoising;

	public int daubechielength;
	public int splineOrder;
	public int nScales;
	public int varWindowSize;
	public int medianWindowSize;
	public int colorConversionMethod;

	public boolean reassignment;
	public boolean subBandCC;
	public boolean majCC;
	public boolean doMorphoOpen;
	public boolean doMorphoClose;
	public boolean doGaussian;
	public boolean doDenoising;
	public boolean doMedian;
	public boolean showTopology;
	public boolean show3dView;
	public boolean log;


	public int maxScales = 1;

	/**
	 * Constructor.
	 *
	 */
	public Parameters(){
		reset();
	}

	/**
	 * Reset to default parameters.
	 *
	 */
	public void reset(){

		colorConversionMethod = 0;
		edfMethod = ExtendedDepthOfField.REAL_WAVELETS;
		outputColorMap = GRAYSCALE;

		sigma = 2.0;
		sigmaDenoising = 2.0;
		rateDenoising = 10.0;

		daubechielength = 6;
		splineOrder = 3;
		nScales = maxScales;
		varWindowSize = 3;
		medianWindowSize = 3;

		reassignment = false;
		subBandCC = false;
		majCC = false;
		doMorphoOpen = false;
		doMorphoClose = false;
		doGaussian = false;
		doDenoising = false;
		doMedian = false;
		showTopology = false;
		show3dView = false;
		log = false;
	}

	/**
	 *
	 * @param settings
	 */
	public void setQualitySettings(int settings){

		switch(settings){
		case QUALITY_LOW:
			edfMethod = ExtendedDepthOfField.SOBEL;
			reassignment = false;
			break;
		case QUALITY_LM:
			edfMethod = ExtendedDepthOfField.VARIANCE;
			varWindowSize = 5;
			reassignment = false;
			break;
		case QUALITY_MEDIUM:
			edfMethod = ExtendedDepthOfField.REAL_WAVELETS;
			nScales = ((maxScales-2) > 1 ?  maxScales : 1);
			splineOrder = 3;
			subBandCC = true;
			majCC = false;
			reassignment = true;
			break;
		case QUALITY_MH:
			edfMethod = ExtendedDepthOfField.REAL_WAVELETS;
			nScales = (maxScales > 1 ?  maxScales : 1);
			splineOrder = 3;
			subBandCC = true;
			majCC = true;
			reassignment = true;
			break;
		case QUALITY_HIGH:
			edfMethod = ExtendedDepthOfField.COMPLEX_WAVELETS;
			nScales = (maxScales > 1 ?  maxScales : 1 );
			daubechielength = 14;
			subBandCC = true;
			majCC = true;
			reassignment = true;
			break;
		default:
			throw new RuntimeException("Unknown Error: in settings.");
		}
	}

	/**
	 *
	 * @param settings
	 */
	public void setTopologySettings(int settings){
		switch(settings) {
		case SMOOTH_TOPO_NO:
			doMedian = false;
			doMorphoOpen = false;
			doMorphoClose = false;
			doGaussian = false;
			break;
		case SMOOTH_TOPO_NM:
			doMedian = true;
			medianWindowSize = 3;
			doMorphoOpen = false;
			doMorphoClose = false;
			doGaussian = false;
			break;
		case SMOOTH_TOPO_MEDIUM:
			doMedian = true;
			medianWindowSize = 3;
			doMorphoOpen = false;
			doMorphoClose = false;
			doGaussian = true;
			sigma = 1;
			break;
		case SMOOTH_TOPO_MV:
			doMedian = true;
			medianWindowSize = 3;
			doMorphoOpen = false;
			doMorphoClose = true;
			doGaussian = true;
			sigma = 2;
			break;
		case SMOOTH_TOPO_VERY:
			doMedian = true;
			medianWindowSize = 5;
			doMorphoOpen = true;
			doMorphoClose = true;
			doGaussian = true;
			sigma = 4;
			break;
		default:
			throw new RuntimeException("Unknown Error: in parameter settings.");
		}
	}

}
