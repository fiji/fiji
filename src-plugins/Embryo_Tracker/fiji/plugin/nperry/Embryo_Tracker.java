package fiji.plugin.nperry;

import fiji.plugin.nperry.features.BlobBrightness;
import fiji.plugin.nperry.features.BlobContrast;
import fiji.plugin.nperry.features.BlobMorphology;
import fiji.plugin.nperry.features.BlobVariance;
import fiji.plugin.nperry.features.LoG;
import fiji.plugin.nperry.tracking.ObjectTracker;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DMenubar;
import ij3d.Image3DUniverse;

import java.awt.Checkbox;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Stack;

import vib.BenesNamedPoint;
import vib.PointList;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.extremafinder.RegionalExtremaFactory;
import mpicbg.imglib.algorithm.extremafinder.RegionalExtremaFinder;
import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionRealType;
import mpicbg.imglib.algorithm.laplace.LoGKernelFactory;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.algorithm.roi.DirectConvolution;
import mpicbg.imglib.algorithm.roi.MedianFilter;
import mpicbg.imglib.algorithm.roi.StructuringElement;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.ShortType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * 
 * @author Nick Perry
 *
 * @param <T>
 */
public class Embryo_Tracker<T extends RealType<T>> implements PlugIn {
	
	/** The number of dimensions in the image. */
	protected int numDim;
	/** The goal diameter of blobs in <b>pixels</b> following downsizing. The image will be 
	 * downsized such that the blob has this diameter (or smaller) in all directions. 
	 * 10 pixels was chosen because trial and error showed that it gave good results.*/
	final static protected float GOAL_DOWNSAMPLED_BLOB_DIAM = 10f;
	/** This is the sigma which is used for the Gaussian convolution. Based on trial and error, it
	 * performed best when applied to images with blobs of diameter 10 pixels. */
	final static protected double IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM = 1.55f;
	/** The index of the <b>shown</b> points in the 3D rendering of all the extrema found in the selectedPoints ArrayList. */
	final static protected int SHOWN = 0;
	/** The index of the <b>not shown</b> points in the 3D rendering of all the extrema found in the selectedPoints ArrayList. */
	final static protected int NOT_SHOWN = 1;
	
	//protected float[] calibration;
	
	/** Ask for parameters and then execute. */
	@SuppressWarnings("unchecked")
	public void run(String arg) {
		/* 1 - Obtain the currently active image */
		ImagePlus ip = IJ.getImage();
		if (null == ip) return;
		
		/* 2 - Ask for parameters */
		GenericDialog gd = new GenericDialog("Track");
		gd.addNumericField("Generic blob diameter:", 7.3, 2, 5, ip.getCalibration().getUnits());  	// get the expected blob size (in pixels).
		gd.addMessage("Verify calibration settings:");
		gd.addNumericField("Pixel width:", ip.getCalibration().pixelWidth, 3);		// used to calibrate the image for 3D rendering
		gd.addNumericField("Pixel height:", ip.getCalibration().pixelHeight, 3);	// used to calibrate the image for 3D rendering
		gd.addNumericField("Voxel depth:", ip.getCalibration().pixelDepth, 3);		// used to calibrate the image for 3D rendering
		gd.addCheckbox("Use median filter", false);
		gd.addCheckbox("Allow edge maxima", false);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		/* 3 - Retrieve parameters from dialogue */
		float diam = (float)gd.getNextNumber();
		float pixelWidth = (float)gd.getNextNumber();
		float pixelHeight = (float)gd.getNextNumber();
		float pixelDepth = (float)gd.getNextNumber();
		boolean useMedFilt = (boolean)gd.getNextBoolean();
		boolean allowEdgeMax = (boolean)gd.getNextBoolean();
		float[] calibration = new float[] {pixelWidth, pixelHeight, pixelDepth};

		/* 4 - Execute! */
		Object[] result = exec(ip, diam, useMedFilt, allowEdgeMax, calibration);
		System.out.println("Done executing!");	
		
		/* 5 - Display new image and overlay maxima */
		if (null != result) {
			System.out.println("Rendering...!");
			ArrayList< ArrayList<Spot> > extremaAllFrames = (ArrayList< ArrayList<Spot> >) result[0];
			if (numDim == 3) {	// If original image is 3D, create a 3D rendering of the image and overlay maxima
				//renderIn3DViewer(extremaAllFrames, ip, calibration, diam);
			} else {
				//PointRoi roi = (PointRoi) result[0];
				//imp.setRoi(roi);
				//imp.updateAndDraw();
			}
		}
	}
	
	/** Execute the plugin functionality: apply a median filter (for salt and pepper noise, if user requests), a Gaussian blur, and then find maxima. */
	public Object[] exec(ImagePlus ip, float diam, boolean useMedFilt, boolean allowEdgeMax, float[] calibration) {		
		/* 0 - Check validity of parameters, initialize local variables */
		if (null == ip) return null;
		ArrayList< ArrayList <Spot> > extremaAllFrames = new ArrayList< ArrayList <Spot> >();
		final float[] downsampleFactors = createDownsampledDim(calibration, diam, numDim);	// factors for x,y,z that we need for scaling image down;
		final ArrayList< Image<T> > frames = new ArrayList< Image<T> >();
		
		/* 1 - Create separate ImagePlus's for each frame */
		ImageStack stack = ip.getImageStack();
		int numSlices = ip.getNSlices();
		int numFrames = ip.getNFrames();
		
		// For each frame...
		for (int i = 0; i < numFrames; i++) {
			ImageStack frame = ip.createEmptyStack();
			
			// ...create the slice by combining the ImageProcessors, one for each Z in the stack.
			for (int j = 1; j <= numSlices; j++) {
				frame.addSlice(Integer.toString(j + (i * numSlices)), stack.getProcessor(j + (i * numSlices)));
			}
			ImagePlus ipSingleFrame = new ImagePlus("Frame " + Integer.toString(i + 1), frame);
			
			/* 2 - Prepare stack for use with Imglib. */
			System.out.println();
			System.out.println("---Frame " + (i+1) + "---");
			Image<T> img = ImagePlusAdapter.wrap(ipSingleFrame);
			frames.add(img);
			Image<T> modImg = img.clone();
			numDim = img.getNumDimensions();
		
			/* 3 - Downsample to improve run time. The image is downsampled by the factor necessary to achieve a resulting blob size of about 10 pixels in diameter in all dimensions. */
			IJ.showStatus("Downsampling...");
			final int dim[] = img.getDimensions();
			for (int j = 0; j < dim.length; j++) {
				dim[j] = (int) (dim[j] / downsampleFactors[j]);
			}
			final DownSample<T> downsampler = new DownSample<T>(modImg, dim, 0.5f, 0.5f);	// optimal sigma is defined by 0.5f, as mentioned here: http://pacific.mpi-cbg.de/wiki/index.php/Downsample
			if (!downsampler.checkInput() || !downsampler.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				System.out.println(downsampler.getErrorMessage()); 
		        System.out.println("Bye.");
		        return null;
			}
			modImg = downsampler.getResult(); 
			
			/* 4 - Apply a median filter, to get rid of salt and pepper noise which could be mistaken for maxima in the algorithm (only applied if requested by user explicitly) */
			if (useMedFilt) {
				IJ.showStatus("Applying median filter...");
				StructuringElement strel;
				
				// Need to figure out the dimensionality of the image in order to create a StructuringElement of the correct dimensionality (StructuringElement needs to have same dimensionality as the image):
				if (numDim == 3) {  // 3D case
					strel = new StructuringElement(new int[]{3, 3, 1}, "3D Square");  // unoptimized shape for 3D case. Note here that we manually are making this shape (not using a class method). This code is courtesy of Larry Lindsey
					Cursor<BitType> c = strel.createCursor();  // in this case, the shape is manually made, so we have to manually set it, too.
					while (c.hasNext()) 
					{ 
					    c.fwd(); 
					    c.getType().setOne(); 
					} 
					c.close(); 
				} else {  			// 2D case
					strel = StructuringElement.createCube(2, 3);  // unoptimized shape
				}
				
				// Apply the median filter:
				final MedianFilter<T> medFilt = new MedianFilter<T>(modImg, strel, new OutOfBoundsStrategyMirrorFactory<T>()); 
				/** note: add back medFilt.checkInput() when it's fixed */
				if (!medFilt.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
					System.out.println(medFilt.getErrorMessage()); 
			        System.out.println("Bye.");
			        return null;
				} 
				modImg = medFilt.getResult(); 
			}
			
			// #---------------------------------------#
			// #------        Time Trials       -------#
			// #---------------------------------------#
			long overall = 0;
			final long numIterations = 1;
			for (int k = 0; k < numIterations; k++) {
				long startTime = System.currentTimeMillis();	
			/** Approach 1: L x (G x I ) */
			/* 5 - Apply a Gaussian filter (code courtesy of Stephan Preibisch). Theoretically, this will make the center of blobs the brightest, and thus easier to find: */
			/*IJ.log("Applying Gaussian filter...");
			IJ.showStatus("Applying Gaussian filter...");
			final GaussianConvolutionRealType<T> convGaussian = new GaussianConvolutionRealType<T>(modImg, new OutOfBoundsStrategyMirrorFactory<T>(), IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM);
			if (convGaussian.checkInput() && convGaussian.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				modImg = convGaussian.getResult(); 
			} else { 
		        System.out.println(convGaussian.getErrorMessage()); 
		        System.out.println("Bye.");
		        return null;
			}
			
			// 6 - Apply a Laplacian convolution to the image.
			IJ.log("Applying Laplacian convolution...");
			IJ.showStatus("Applying Laplacian convolution...");
			// Laplacian kernel construction: everything is negative so that we can use the existing find maxima classes (otherwise, it would be creating minima, and we would need to use find minima). The kernel has everything divided by 18 because we want the highest value to be 1, so that numbers aren't created that beyond the capability of the image type. For example, in a short type, if we had the highest number * 18, the short type can't hold that, and the rest of the value is lost in conversion. This way, we won't create numbers larger than the respective types can hold.
			DirectConvolution<T, FloatType, T> convLaplacian;
			if (numDim == 3) {
				float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
				ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
				Image<FloatType> laplacianKernel = factory.createImage(new int[]{3, 3, 3}, "Laplacian");
				quickKernel3D(laplacianArray, laplacianKernel);
				convLaplacian = new DirectConvolution<T, FloatType, T>(img.createType(), modImg, laplacianKernel);;
			} else {
				float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
				ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
				Image<FloatType> laplacianKernel = factory.createImage(new int[]{3, 3}, "Laplacian");
				quickKernel2D(laplacianArray, laplacianKernel);
				convLaplacian = new DirectConvolution<T, FloatType, T>(img.createType(), modImg, laplacianKernel);;
			}
			//if(convLaplacian.checkInput() && convLaplacian.process()) {
			if(convLaplacian.process()) {
				modImg = convLaplacian.getResult();
				//ImagePlus test = ImageJFunctions.copyToImagePlus(img);
				//test.show();
			} else {
				System.out.println(convLaplacian.getErrorMessage());
				System.out.println("Bye.");
				return null;
			}*/
			
			/** Approach 2: F(L) x F(G) x F(I) */
			
			// Gauss
			IJ.showStatus("Applying Gaussian filter...");
			final ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
			final Image<FloatType> gaussKernel = FourierConvolution.getGaussianKernel(factory, IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM, numDim);
			final FourierConvolution<T, FloatType> fConvGauss = new FourierConvolution<T, FloatType>(modImg, gaussKernel);
			if (!fConvGauss.checkInput() || !fConvGauss.process()) {
				System.out.println( "Fourier Convolution failed: " + fConvGauss.getErrorMessage() );
				return null;
			}
			modImg = fConvGauss.getResult();
			
			// Laplace
			IJ.showStatus("Applying Laplacian convolution...");
			Image<FloatType> laplacianKernel;
			if (numDim == 3) {
				final float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
				laplacianKernel = factory.createImage(new int[]{3, 3, 3}, "Laplacian");
				quickKernel3D(laplacianArray, laplacianKernel);
			} else {
				final float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
				laplacianKernel = factory.createImage(new int[]{3, 3}, "Laplacian");
				quickKernel2D(laplacianArray, laplacianKernel);
			}
			final FourierConvolution<T, FloatType> fConvLaplacian = new FourierConvolution<T, FloatType>(modImg, laplacianKernel);
			if (!fConvLaplacian.checkInput() || !fConvLaplacian.process()) {
				System.out.println( "Fourier Convolution failed: " + fConvLaplacian.getErrorMessage() );
				return null;
			}
			modImg = fConvLaplacian.getResult();	
			
			/** Approach 3: (L x G) x I */
			/*IJ.log("Applying LoG Convolution...");
			Image<FloatType> logKern = LoGKernelFactory.createLoGKernel(IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM, numDim, true, true);
			DirectConvolution<T, FloatType, T> convLoG = new DirectConvolution<T, FloatType, T>(img.createType(), img, logKern);
			//DirectConvolution<T, FloatType, FloatType> convLoG = new DirectConvolution<T, FloatType, FloatType>(new FloatType(), img, logKern);
			if(convLoG.process()) {
				imgResult = convLoG.getResult();
				ImagePlus test = ImageJFunctions.copyToImagePlus(imgResult);
				test.show();
			} else {
				System.out.println(convLoG.getErrorMessage());
				System.out.println("Bye.");
				return null;
			}*/
			
			long runTime = System.currentTimeMillis() - startTime;	
			System.out.println("Laplacian/Gaussian Run Time: " + runTime);
			
			
			/** Approach 4: DoG */
			/** Approach 5: F(DoG) */
				overall += runTime;
			}
			System.out.println("Average run time: " + (long)overall/numIterations);
			// #----------------------------------------#
			// #------        /Time Trials       -------#
			// #----------------------------------------#
			
			
			/* 7 - Find extrema of newly convoluted image */
			IJ.showStatus("Finding extrema...");
			final RegionalExtremaFactory<T> extremaFactory = new RegionalExtremaFactory<T>(modImg, calibration);
			final RegionalExtremaFinder<T> findExtrema = extremaFactory.createRegionalMaximaFinder(true);
			findExtrema.allowEdgeExtrema(allowEdgeMax);
			if (!findExtrema.checkInput() || !findExtrema.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				System.out.println( "Extrema Finder failed: " + findExtrema.getErrorMessage() );
				return null;
			}
			final ArrayList< float[] > centeredExtrema = findExtrema.getRegionalExtremaCenters(false);
			final ArrayList<Spot> spots = findExtrema.convertToSpots(centeredExtrema, calibration);
			downsampledCoordsToOrigCoords(spots, downsampleFactors);
			extremaAllFrames.add(spots);
			System.out.println("Find Maxima Run Time: " + findExtrema.getProcessingTime());
			System.out.println("Num regional maxima: " + centeredExtrema.size());
			
			/* 8 - Extract features for maxima */
			final LoG<T> log = new LoG<T>(modImg, downsampleFactors, calibration);
			//final BlobVariance<T> var = new BlobVariance<T>(img, diam, calibration);
			final BlobBrightness<T> brightness = new BlobBrightness<T>(img, diam, calibration);
			//final BlobContrast<T> contrast = new BlobContrast<T>(img, diam, calibration);
			final BlobMorphology<T> morphology = new BlobMorphology<T>(img, diam, calibration);
			log.process(spots);
			//var.process(spots);
			//brightness.process(spots);
			//contrast.process(spots);
			//morphology.process(spots);
		}
		
		// Render 3D to adjust thresholds...
		ArrayList< HashMap<Feature, Float> > originalThresholdsAllFrames = new ArrayList< HashMap<Feature, Float> >();
		ArrayList< ArrayList< ArrayList<Spot> > > selectedPoints = new ArrayList< ArrayList< ArrayList<Spot> > >();
		Image3DUniverse univ = renderIn3DViewer(extremaAllFrames, ip, calibration, diam, originalThresholdsAllFrames, selectedPoints);
		letUserAdjustThresholds(univ, ip.getTitle(), originalThresholdsAllFrames, selectedPoints, extremaAllFrames, calibration);

		/* 9 - Track */
		ArrayList< ArrayList<Spot> > extremaPostThresholdingAllFrames = new ArrayList< ArrayList<Spot> >();
		for (ArrayList< ArrayList <Spot> > pointsInTimeFrame : selectedPoints) {
			extremaPostThresholdingAllFrames.add(pointsInTimeFrame.get(0));
		}
		
		
		// <debug>
		int counter = 1;
		for (ArrayList<Spot> spots : extremaPostThresholdingAllFrames) {
			BlobMorphology<T> morph = new BlobMorphology<T>(frames.get(counter - 1), diam, new double[] {1,1,1});
			System.out.println("--- Frame " + counter + " ---");
			for (Spot spot : spots) {
				//double[] coords = spot.getCoordinates();
				//System.out.println("[" + coords[0] + ", " + coords[1] + ", " + coords[2] + "] (" + coords[0] * .2 + ", " + coords[1] * .2 + ", " + coords[2] + ")");
				morph.process(spot);
			
			}
			counter++;
			System.out.println();
		}

		// </debug>
		
		
		
		
		ObjectTracker track = new ObjectTracker(extremaPostThresholdingAllFrames);
		track.process();
		track.getResults(); // TODO
		
		return new Object[] {extremaAllFrames};
	}
	
	// Code source: http://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html
	public float otsuThreshold(ArrayList<Spot> srcData, Feature feature)
	{
		// Prepare histogram
		int histData[] = histogram(srcData, feature);
		int count = srcData.size();

		// Thresholding
		float sum = 0;
		for (int t=0 ; t<histData.length ; t++) sum += t * histData[t];

		float sumB = 0;
		int wB = 0;
		int wF = 0;

		float varMax = 0;
		int threshold = 0;
		
		for (int t=0 ; t<histData.length ; t++)
		{
			wB += histData[t];					// Weight Background
			if (wB == 0) continue;

			wF = count - wB;					// Weight Foreground
			if (wF == 0) break;

			sumB += (float) (t * histData[t]);

			float mB = sumB / wB;				// Mean Background
			float mF = (sum - sumB) / wF;		// Mean Foreground

			// Calculate Between Class Variance
			float varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);	

			// Check if new maximum found
			if (varBetween > varMax) {
				varMax = varBetween;
				threshold = t;
			}
		}
		
		return (threshold + 1) * (float) getRange(srcData, feature)[0] / (float) histData.length;  // Convert the integer bin threshold to a value
	}
	
	/** Generate a histogram of the specified feature, with a number of bins determined 
	 * from the Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)) 
	 * */
	public int[] histogram (ArrayList<Spot> data, Feature feature) {

		// Calculate number of bins
		final int size = data.size();
		final double[] feature_values = new double[size];
		for (int i = 0; i < feature_values.length; i++) {
			feature_values[i] = data.get(i).getFeature(feature);
		}
		final double q1 = getPercentile(feature_values, 0.25);
		final double q3 = getPercentile(feature_values, 0.75);
		final double iqr = q3 - q1;
		final double binWidth = 2 * iqr * Math.pow(size, -0.33);
		final double[] range = getRange(data, feature);
		final int nBins = (int) ( range[0] / binWidth + 1 ); 

		// Create array for histrogram with nBins
		final int[] hist = new int[nBins];
		int index;
		// Populate the histogram with data
		for (int i = 0; i < feature_values.length; i++) {
			index = Math.min((int) Math.floor((feature_values[i] - range[1]) / binWidth), nBins - 1); // the max value ends up being 1 higher than nBins, so put it in the last bin.
			hist[index]++;
		}
		return hist;
	}
	
	
	/**
     * Returns an estimate of the <code>p</code>th percentile of the values
     * in the <code>values</code> array. Taken from commons-math.
	 */
	private static final double getPercentile(final double[] values, final double p) {

		final int size = values.length;
		if ((p > 1) || (p <= 0)) {
            throw new IllegalArgumentException("invalid quantile value: " + p);
        }
        if (size == 0) {
            return Double.NaN;
        }
        if (size == 1) {
            return values[0]; // always return single value for n = 1
        }
        double n = (double) size;
        double pos = p * (n + 1);
        double fpos = Math.floor(pos);
        int intPos = (int) fpos;
        double dif = pos - fpos;
        double[] sorted = new double[size];
        System.arraycopy(values, 0, sorted, 0, size);
        Arrays.sort(sorted);

        if (pos < 1) {
            return sorted[0];
        }
        if (pos >= n) {
            return sorted[size - 1];
        }
        double lower = sorted[intPos - 1];
        double upper = sorted[intPos];
        return lower + dif * (upper - lower);
	}
	
	/** Returns [range, min, max] */
	public double[] getRange(ArrayList<Spot> data, Feature feature) {
		double min = 0;
		double max = 0;
		
		for (int i = 0; i < data.size(); i++) {
			double value = data.get(i).getFeature(feature);
			if (i == 0) {
				min = value;
				max = value;
			}
			
			else {
				if (value < min) min = value;
				if (value > max) max = value;
			}
		}
		
		return new double[] {(max-min), min, max};
	}
	
	public float[] createDownsampledDim(float[] calibration, float diam, int numDim) {
		float widthFactor = (diam / calibration[0]) > GOAL_DOWNSAMPLED_BLOB_DIAM ? (diam / calibration[0]) / GOAL_DOWNSAMPLED_BLOB_DIAM : 1;
		float heightFactor = (diam / calibration[1]) > GOAL_DOWNSAMPLED_BLOB_DIAM ? (diam / calibration[1]) / GOAL_DOWNSAMPLED_BLOB_DIAM : 1;
		float depthFactor = (numDim == 3 && (diam / calibration[2]) > GOAL_DOWNSAMPLED_BLOB_DIAM) ? (diam / calibration[2]) / GOAL_DOWNSAMPLED_BLOB_DIAM : 1;								
		float downsampleFactors[] = new float[]{widthFactor, heightFactor, depthFactor};
		
		return downsampleFactors;
	}
	
	protected static void quickKernel3D(float[][][] vals, Image<FloatType> kern)
	{
		final LocalizableByDimCursor<FloatType> cursor = kern.createLocalizableByDimCursor();
		final int[] pos = new int[3];

		for (int i = 0; i < vals.length; ++i)
		{
			for (int j = 0; j < vals[i].length; ++j)
			{
				for (int k = 0; k < vals[j].length; ++k)
				{
					pos[0] = i;
					pos[1] = j;
					pos[2] = k;
					cursor.setPosition(pos);
					cursor.getType().set(vals[i][j][k]);
				}
			}
		}
		cursor.close();		
	}
	
	/**
	 * Code courtesy of Larry Lindsey. However, it is protected in the DirectConvolution class,
	 * so I reproduced it here.
	 * 
	 * @param vals
	 * @param kern
	 */
	protected static void quickKernel2D(float[][] vals, Image<FloatType> kern)
	{
		final LocalizableByDimCursor<FloatType> cursor = kern.createLocalizableByDimCursor();
		final int[] pos = new int[2];

		for (int i = 0; i < vals.length; ++i)
		{
			for (int j = 0; j < vals[i].length; ++j)
			{
				pos[0] = i;
				pos[1] = j;
				cursor.setPosition(pos);
				cursor.getType().set(vals[i][j]);
			}
		}
		cursor.close();		
	}
	
	public PointRoi preparePointRoi (ArrayList< ArrayList< double[] > > extrema, float downsampleFactors[], float pixelWidth, float pixelHeight) {
		int numPoints = extrema.size();
		int ox[] = new int[numPoints];
		int oy[] = new int[numPoints];
		ListIterator< ArrayList<double[]> > framesItr = extrema.listIterator();
		
		while (framesItr.hasNext()) {
			ArrayList<double[]> frame= framesItr.next();
			ListIterator< double[] > itr = frame.listIterator();
			while (itr.hasNext()) {
				int index = 0;
				double curr[] = itr.next();
				ox[index] = (int) (curr[0] * downsampleFactors[0]);
				oy[index] = (int) (curr[1] * downsampleFactors[1]);
				index++;
			}
		}
		PointRoi roi = new PointRoi(ox, oy, numPoints);
		return roi;
	}

	public Image3DUniverse renderIn3DViewer(ArrayList< ArrayList<Spot> > extremaAllFrames, ImagePlus ip, float[] calibration, float diam, ArrayList< HashMap<Feature, Float> > thresholdsAllFrames, ArrayList< ArrayList< ArrayList<Spot> > > selectedPoints) {
		
		// 1 - Display points

		// Convert to a usable format
		new StackConverter(ip).convertToGray8();

		// Create a universe
		Image3DUniverse univ = new Image3DUniverse();
		univ.show();
		
		// Add the image as a volume rendering
		Content c = univ.addVoltex(ip);

		// Calculate thresholds, store which points are shown vs. not shown, and add points to the ContentInstant's PointList
		for (int j = 0; j < extremaAllFrames.size(); j++) {
			
			ArrayList<Spot> shown = new ArrayList<Spot>();
			ArrayList<Spot> notShown = new ArrayList<Spot>();
			
			PointList pl = c.getInstant(j).getPointList();
			ArrayList<Spot> framej = extremaAllFrames.get(j);
			
			// Calculate thresholds for each feature of interest.
			HashMap<Feature, Float> thresholds = new HashMap<Feature, Float>();
			//final float logThreshold = otsuThreshold(framej, Feature.LOG_VALUE);  // threshold for frame
			//final float brightnessThreshold = otsuThreshold(framej, Feature.BRIGHTNESS);
			//final float contrastThreshold = otsuThreshold(framej, Feature.CONTRAST);
			//final float varThreshold = otsuThreshold(framej, Feature.VARIANCE);
			//thresholds.put(Feature.LOG_VALUE, logThreshold);
			//thresholds.put(Feature.BRIGHTNESS, brightnessThreshold);
			//thresholds.put(Feature.CONTRAST, contrastThreshold);
			//thresholds.put(Feature.VARIANCE, varThreshold);
			thresholdsAllFrames.add(thresholds);

			// Add the extrema coords to the pointlist
			for (int i = 0; i < framej.size(); i++) {
				final Spot spot = framej.get(i);
				final float coords[] = spot.getCoordinates();
				
				// 1. If the spot passes the threshold
				//if (aboveThresholds(spot, thresholds)) {
					spot.setName(Integer.toString(i));
					pl.add(spot.getName(), coords[0], coords[1], coords[2]);	
					shown.add(spot);
				//}
				
				// 2. If spot doesn't pass threshold
				//else{
				//	spot.setName(Integer.toString(i));
				//	notShown.add(spot);
				//}
			}
			
			// Add the shown and notShown lists of points to the overall list
			ArrayList<ArrayList<Spot> > selectedPointsInFrame = new ArrayList<ArrayList<Spot> >();
			selectedPointsInFrame.add(shown);
			selectedPointsInFrame.add(notShown);
			selectedPoints.add(selectedPointsInFrame);
		}
		
		// Make the point list visible
		c.showPointList(true);
		
		// Make point list window invisible (potentially slowing down thresholding...)
		univ.getPointListDialog().setVisible(false);
		
		// Change the size of the points
		c.setLandmarkPointSize((float) diam / 2);  // Point size determined by radius
	
		return univ;
	}
	
	private boolean aboveThresholds(Spot spot, HashMap<Feature, Float> thresholds) {
		for (Feature feature : thresholds.keySet()) {
			if (spot.getFeature(feature) < thresholds.get(feature)) {
				return false;
			}
		}
		return true;
	}
	
	private void downsampledCoordsToOrigCoords(ArrayList<Spot> spots, float downsampleFactors[]) {
		Iterator<Spot> itr = spots.iterator();
		while (itr.hasNext()) {
			Spot spot = itr.next();
			float[] coords = spot.getCoordinates();
			
			// Undo downsampling
			for (int i = 0; i < coords.length; i++) {
				coords[i] = coords[i] * downsampleFactors[i];
			}
		}
	}
	
	public void letUserAdjustThresholds(final Image3DUniverse univ, final String contentName, ArrayList< HashMap<Feature, Float> > thresholdsAllFrames, ArrayList< ArrayList< ArrayList<Spot> > > selectedPoints, ArrayList< ArrayList< Spot > > extremaAllFrames, float[] calibration) {
		
		// Grab the Content of the universe, which has the name of the IP.
		final Content c = univ.getContent(contentName);
		
		// Grab the current CI in the universe
		final ContentInstant ci = c.getCurrent();
		
		// Set up dialog
		final GenericDialog gd = new GenericDialog("Adjust Thresholds");
		final int t = ci.getTimepoint();	// store the timepoint, which is the for the thresholds, and point lists
		int counter = 0;  					// counter which allows us to attach AdjustmentListener to the correct JSlider

		// For every feature, create a slider that is used to threshold Spots based on that feature
		for (final Feature feature : thresholdsAllFrames.get(t).keySet()) {
			final int curr = counter;  // need this, because needs to be final in order to be used
			
			// Add slider for this Feature to dialog
			final float tr = thresholdsAllFrames.get(t).get(feature);
			double[] range = getRange(extremaAllFrames.get(t), feature);
			gd.addSlider(feature.getName() + " Threshold", range[1], range[2], tr);
			gd.addCheckbox("Auto", true);
			
			// Create a SliderAdjuster for this Feature
			final SliderAdjuster thresh_adjuster = new SliderAdjuster (c, selectedPoints, calibration, tr, thresholdsAllFrames) {
				public synchronized final void setValue(ContentInstant ci, float threshold, float[] calibration) {	

					// for all frames
					for (int i = 0; i < thresholdsAllFrames.size(); i++) {
						PointList pl = c.getInstant(i).getPointList();
						thresholdsAllFrames.get(i).put(feature, threshold);
						ArrayList<Spot> shown = selectedPoints.get(i).get(SHOWN);
						ArrayList<Spot> notShown = selectedPoints.get(i).get(NOT_SHOWN);
						
						if (larger) {
							ci.showPointList(false);
							for (int j = 0; j < shown.size(); j++) {
								Spot spot = shown.get(j);
								if (spot.getFeature(feature) < threshold) {							
									shown.remove(j);
									j--;  // the remove() call above shifted all the remaining elements, so we need to decrement j to not skip an element
									pl.remove(pl.get(spot.getName()));
									notShown.add(spot);
								}
							}
							ci.showPointList(true);
							univ.getPointListDialog().setVisible(false);
						} 
		 				
		 				// 2 - Threshold is lower than previously, we need to add some points that are now above the threshold
		 				else {
							for (int j = 0; j < notShown.size(); j++) {
								Spot spot = notShown.get(j);
								boolean passedThresholds = true;  // initially, assume the point is above all the thresholds
								for (Feature feature : thresholdsAllFrames.get(t).keySet()) {  // for each feature we threshold...
									if (spot.getFeature(feature) < thresholdsAllFrames.get(t).get(feature)) {  // if the spot has a lower value...
										passedThresholds = false;  // mark that it isn't above all the thresholds
										break;
									}	
								}	
								if (passedThresholds) {  // to get past this point, a spot had to have thresholds above all the current threshold values
									notShown.remove(j);
									j--;  // the remove() call above shifted all the remaining elements, so we need to decrement j to not skip an element
									float[] coords = spot.getCoordinates();
									pl.add(spot.getName(), coords[0], coords[1], coords[2]);
									shown.add(spot);
								}
							}
						}		
					}
					univ.fireContentChanged(c);
				}
			};

			// Add an AdjustmentListener to the slider
			((Scrollbar)gd.getSliders().get(curr)).
			addAdjustmentListener(new AdjustmentListener() {
				public void adjustmentValueChanged(final AdjustmentEvent e) {
					// start adjuster and request an action
					((Checkbox)gd.getCheckboxes().get(curr)).setState(false);  // uncheck the 'auto' box because no longer the automatically calculated threshold.
					if (!((Scrollbar)gd.getSliders().get(curr)).getValueIsAdjusting()) { // If the slider is not adjusting...
						if(!thresh_adjuster.go) {
							thresh_adjuster.start();
						}
						thresh_adjuster.exec(e.getValue(), ci, univ);
					}
				}
			});

			// Add an ItemListener to the 'auto' checkbox
			((Checkbox)gd.getCheckboxes().get(curr)).
			addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) { // if the checkbox was selected, change the threshold to the original.
						((Scrollbar)gd.getSliders().get(curr)).setValue((int)tr);
						
						thresh_adjuster.exec((int)tr, ci, univ);
					}
				}
			});
			
			counter++;
		}
		gd.setModal(false);
		// Handle when window closed... (see original changeThreshold code in Executer.class)
		
		gd.showDialog();
		while (!gd.wasOKed()) {  // stay here until the user selects 'ok,' or 'cancels'
			if (gd.wasCanceled()) return;  /* FIX: if canceled, reset to auto-thresholds! */
		}
		univ.close();
	}
	
	/* **********************************************************
	 * Thread which handles the updates of sliders
	 * *********************************************************/
	public abstract class SliderAdjuster extends Thread {
		boolean go = false;
		int newV;
		ContentInstant content;
		Content c;
		ArrayList< ArrayList< ArrayList<Spot> > > selectedPoints;
		Image3DUniverse univ;
		float[] calibration;
		float tr;
		boolean larger;
		ArrayList<HashMap<Feature, Float> > thresholdsAllFrames;
		final Object lock = new Object();

		SliderAdjuster(Content c, ArrayList< ArrayList< ArrayList<Spot> > > selectedPoints, float[] calibration, float origTr, ArrayList<HashMap<Feature, Float> > thresholdsAllFrames) {
			super("VIB-SliderAdjuster");
			setPriority(Thread.NORM_PRIORITY);
			setDaemon(true);
			this.calibration = calibration;
			this.tr = origTr;
			this.c = c;
			this.selectedPoints = selectedPoints;
			this.thresholdsAllFrames = thresholdsAllFrames;
		}

		/*
		 * Set a new event, overwriting previous if any.
		 */
		void exec(final int newV, final ContentInstant content, final Image3DUniverse univ) {
			synchronized (lock) {
				this.newV = newV;
				if (newV >= tr) {
					this.larger = true;
				} else {
					this.larger = false;
				}
				this.tr = newV;  // update current threshold value, so we can compare if new ones are > or <
				this.content = content;
				this.univ = univ;
			}
			synchronized (this) { notify(); }
		}

		public void quit() {
			this.go = false;
			synchronized (this) { notify(); }
		}
		
		/*
		 * This class has to be implemented by subclasses, to define
		 * the specific updating function.
		 */
		protected abstract void setValue(final ContentInstant c, final float v, float[] calibration);

		@Override
		public void run() {
			go = true;
			while (go) {
				try {
					if (null == content) {
						synchronized (this) { wait(); }
					}
					if (!go) return;
					// 1 - cache vars, to free the lock very quickly
					ContentInstant c;
					float v = 0;
					Image3DUniverse u;
					synchronized (lock) {
						c = this.content;
						v = this.newV;
						u = this.univ;
					}
					// 2 - exec cached vars
					if (null != c) {
						setValue(c, v, calibration);
					}
					// 3 - done: reset only if no new request was put
					synchronized (lock) {
						if (c == this.content) {
							this.content = null;
							this.univ = null;
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}
}
