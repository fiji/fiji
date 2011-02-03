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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import imageware.Builder;
import imageware.ImageWare;
import surfacemap.SurfaceMap3D;
import edf.AbstractEdfAlgorithm;
import edf.Color2BW;
import edf.EdfComplexWavelets;
import edf.EdfRealWavelets;
import edf.EdfSobel;
import edf.EdfVariance;
import edf.LogSingleton;
import edf.MorphologicalOperators;
import edf.PostProcessing;
import edf.Tools;

public class ExtendedDepthOfField {

	static final int SOBEL = 0;
	static final int VARIANCE = 1;
	static final int REAL_WAVELETS = 2;
	static final int COMPLEX_WAVELETS = 3;

	Parameters parameters = null;
	ImagePlus imp = null;


	/**
	 * Constructor.
	 */
	public ExtendedDepthOfField (ImagePlus imp, Parameters parameters) {
		this.parameters = parameters;
		this.imp = imp;
	}


	/**
	 * Run the main processing.
	 */
	public void process() {

		LogSingleton log = LogSingleton.getInstance();
		log.setStartTime((double)System.currentTimeMillis());
		log.append("Start processing....");
		log.setProgessLength(0);

		boolean isExtended = false;
		boolean waveletMethod = (parameters.edfMethod == REAL_WAVELETS || parameters.edfMethod == COMPLEX_WAVELETS);

		ImageStack stackConverted;
		ImagePlus impConverted;
		ImagePlus impBW = imp;

		if (parameters.color) {

			log.start("Color conversion...");

			impConverted = new ImagePlus();
			switch (parameters.colorConversionMethod) {
			case 0:
				stackConverted = Color2BW.C2BFixedWeights(imp.getStack(), true);
				break;
			case 1:
				stackConverted = Color2BW.C2BPrincipalComponents(imp.getStack());
				break;
			case 2:
				stackConverted = Color2BW.C2BMean(imp.getStack());
				break;
			default:
				throw new RuntimeException("Unknown error");
			}
			impConverted.setStack(null,stackConverted);
			impBW = impConverted;

			log.acknowledge();
		}

		System.gc();

		// Start the main EDF process.

		ImageWare imageStack = Builder.wrap(impBW);

		// Check sizes.

		int[] scaleAndSizes = new int[3];
		int nx = imageStack.getWidth();
		int ny = imageStack.getHeight();


		if (waveletMethod) {
			if(!Tools.isPowerOf2(nx) || !Tools.isPowerOf2(ny)) {
				scaleAndSizes = Tools.computeScaleAndPowerTwoSize(nx,ny);
				log.start("Extend images to "+ scaleAndSizes[1]+ "x" + scaleAndSizes[2] + " pixels...");
				imageStack = Tools.extend(imageStack,scaleAndSizes[1],scaleAndSizes[2]);
				isExtended = true;
				log.acknowledge();
			}
			System.gc();
		}

		log.start("Sharpen estimation...");

		AbstractEdfAlgorithm edf;
		ImageWare[] ima = new ImageWare[2];

		switch(parameters.edfMethod) {
		case REAL_WAVELETS :
			if( parameters.doDenoising )
				edf = new EdfRealWavelets((int)parameters.splineOrder, parameters.nScales,
						parameters.subBandCC,parameters.majCC, parameters.rateDenoising);
			else
				edf = new EdfRealWavelets((int)parameters.splineOrder, parameters.nScales,
						parameters.subBandCC,parameters.majCC);
			ima = edf.process(imageStack);
			break;
		case COMPLEX_WAVELETS :
			edf = new EdfComplexWavelets(parameters.daubechielength, parameters.nScales,
					parameters.subBandCC,parameters.majCC);
			ima = edf.process(imageStack);
			break;
		case VARIANCE :
			edf = new EdfVariance(parameters.varWindowSize);
			ima = edf.process(imageStack);
			break;
		case SOBEL :
			edf = new EdfSobel();
			ima = edf.process(imageStack);
			break;
		default:
			throw new RuntimeException("Invalid Option.");
		}
		System.gc();

		log.acknowledge();
		log.setProgessLength(80);

		// Crop to original images.
		if (waveletMethod && isExtended) {
			log.start("Crop to original size...");
			imageStack = Tools.crop(imageStack,nx,ny);
			ima[0] = Tools.crop(ima[0],nx,ny);
			ima[1] = Tools.crop(ima[1],nx,ny);
			System.gc();
			log.acknowledge();
		}

		if(parameters.reassignment) {
			log.start("Reassignment to original pixel values...");
			ima[1] = PostProcessing.reassignment(ima[0],imageStack);
			System.gc();
			log.acknowledge();
			log.setProgessLength(95);
		}

		if(parameters.doDenoising && !waveletMethod) {
			log.start("Denoising (Gaussian smoothing)...");
			ima[0].smoothGaussian(parameters.sigmaDenoising);
			System.gc();
			log.acknowledge();
			log.setProgessLength(95);
		}

		ImagePlus impComposite = null;
		ImagePlus impHeightMap = null;

		if(parameters.color && parameters.outputColorMap==Parameters.COLOR_RGB) {
			ColorProcessor cp;
			if ( (waveletMethod && parameters.reassignment) || !waveletMethod){
				cp = PostProcessing.reassignmentColor(ima[1],imp.getStack());
				impComposite = new ImagePlus("Output",cp);
			}else {
				impComposite = new ImagePlus("Output", ima[0].buildImageStack());
			}
		}
		else {
			impComposite = new ImagePlus("Output", ima[0].buildImageStack());
		}

		// Topology post-processing.
		if ( (waveletMethod && parameters.reassignment) || (!waveletMethod)) {

			if (parameters.showTopology) {

				if(parameters.doMedian) {
					log.start("Median filter...");

					ima[1] = MorphologicalOperators.doMedian(ima[1], parameters.medianWindowSize);
					log.acknowledge();

				}

				if(parameters.doMorphoClose) {
					log.start("Morphological close...");

					ima[1] = MorphologicalOperators.doClose(ima[1]);

					log.acknowledge();
				}

				if(parameters.doMorphoOpen) {
					log.start("Morphological open ...");

					ima[1] = MorphologicalOperators.doOpen(ima[1]);

					log.acknowledge();
				}

				if (parameters.doGaussian) {
					log.start("Post-processing on the map: Gaussian filter of sigma: " + parameters.sigma);
					ima[1].smoothGaussian(parameters.sigma);
					log.acknowledge();
				}

				impHeightMap = new ImagePlus("Height-Map", ima[1].buildImageStack());
				impHeightMap.show();
				impHeightMap.updateAndDraw();

				if(parameters.show3dView){
					SurfaceMap3D viewer = new SurfaceMap3D(impHeightMap, impComposite);
					Thread thread = new Thread(viewer);
					thread.start();
				}
			}
		}

		impComposite.show();
		impComposite.updateAndDraw();

		log.start("Finished.");
		log.setProgessLength(100);
		log.append("");
	}
}
