package fiji.plugin.nperry;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.plugin.PlugIn;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.Image3DUniverse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import vib.PointList;
import view4d.Timeline;
import view4d.TimelineGUI;

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
	/** Class/Instance variables */
	protected Image<T> img;
	final static protected float GOAL_DOWNSAMPLED_BLOB_DIAM = 10f;				  // trial and error showed that downsizing images so that the blobs have a diameter of 10 pixels performs best (least errors, and most correct finds, by eyeball analysis).
	final static protected double IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM = 1.55f;  // trial and error proved this to be approximately the best sigma for a blob of 10 pixels in diameter.
	
	/** Ask for parameters and then execute. */
	@SuppressWarnings("unchecked")
	public void run(String arg) {
		// 1 - Obtain the currently active image:
		ImagePlus imp = IJ.getImage();
		if (null == imp) return;
		
		// 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Track");
		gd.addNumericField("Generic blob diameter:", 7.3, 2, 5, imp.getCalibration().getUnits());  	// get the expected blob size (in pixels).
		gd.addMessage("Verify calibration settings:");
		gd.addNumericField("Pixel width:", imp.getCalibration().pixelWidth, 3);		// used to calibrate the image for 3D rendering
		gd.addNumericField("Pixel height:", imp.getCalibration().pixelHeight, 3);	// used to calibrate the image for 3D rendering
		gd.addNumericField("Voxel depth:", imp.getCalibration().pixelDepth, 3);		// used to calibrate the image for 3D rendering
		gd.addCheckbox("Over time", true);
		gd.addCheckbox("Use median filter", false);
		gd.addCheckbox("Allow edge maxima", false);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		// 3 - Retrieve parameters from dialogue:
		float diam = (float)gd.getNextNumber();
		float pixelWidth = (float)gd.getNextNumber();
		float pixelHeight = (float)gd.getNextNumber();
		float pixelDepth = (float)gd.getNextNumber();
		boolean overTime = (boolean)gd.getNextBoolean();
		boolean useMedFilt = (boolean)gd.getNextBoolean();
		boolean allowEdgeMax = (boolean)gd.getNextBoolean();

		// 4 - Execute!
		Object[] result = exec(imp, diam, useMedFilt, allowEdgeMax, pixelWidth, pixelHeight, pixelDepth, overTime);
		
		// 5 - Display new image and overlay maxima
		if (null != result) {
			ArrayList< ArrayList<double[]> > extremaAllFrames = (ArrayList< ArrayList<double[]> >) result[0];
			if (img.getNumDimensions() == 3) {	// If original image is 3D, create a 3D rendering of the image and overlay maxima
				ij.plugin.Duplicator d = new ij.plugin.Duplicator();  // Make a duplicate image so we don't alter the users image when displaying 3D (requires 8-bit, etc).
				ImagePlus duplicatedImp = d.run(imp);
				render3DAndOverlayExtrema(extremaAllFrames, duplicatedImp, pixelWidth, pixelHeight, pixelDepth, createDownsampledDim(pixelWidth, pixelHeight, pixelDepth, diam), diam, overTime);
			} else {
				//PointRoi roi = (PointRoi) result[0];
				//imp.setRoi(roi);
				//imp.updateAndDraw();
			}
		}
	}
	
	/** Execute the plugin functionality: apply a median filter (for salt and pepper noise), a Gaussian blur, and then find maxima. */
	public Object[] exec(ImagePlus imp, float diam, boolean useMedFilt, boolean allowEdgeMax, float pixelWidth, float pixelHeight, float pixelDepth, boolean overTime) {
		// 0 - Check validity of parameters, initialize local variables:
		if (null == imp) return null;
		ArrayList< ArrayList <double[]> > extremaAllFrames = new ArrayList< ArrayList <double[]> >();
		float downsampleFactors[] = null;
		                        
		// 1 - Create separate ImagePlus's for each frame
		ImageStack stack = imp.getImageStack();
		int numSlices = imp.getNSlices();
		int numFrames = 1;  // At least one frame, since an image is open.
		if (overTime) {
			numFrames = imp.getNFrames();
		}
		
		// For each frame...
		for (int i = 0; i < numFrames; i++) {
			ImageStack frame = imp.createEmptyStack();
			
			// ...create the slice by combining the ImageProcessors, one for each Z in the stack.
			for (int j = 1; j <= numSlices; j++) {
				frame.addSlice(Integer.toString(j + (i * numSlices)), stack.getProcessor(j + (i * numSlices)));
			}
			ImagePlus impSingleFrame = new ImagePlus("Frame " + Integer.toString(i + 1), frame);
			
			// 2 - Prepare stack for use with Imglib
			IJ.log("---Frame " + Integer.toString(i+1) + "---");
			System.out.println("---Frame " + (i+1) + "---");
			img = ImagePlusAdapter.wrap(impSingleFrame);
			int numDim = img.getNumDimensions();
		
			// 3 - Downsample to improve run time. The image is downsampled by the factor necessary to achieve a resulting blob size of about 10 pixels in diameter in all dimensions.
			IJ.log("Downsampling...");
			IJ.showStatus("Downsampling...");
			int dim[] = img.getDimensions();
			downsampleFactors = createDownsampledDim(pixelWidth, pixelHeight, pixelDepth, diam);	// factors for x,y,z that we need for scaling image down
			int downsampledDim[] = (numDim == 3) ? new int[]{(int)(dim[0] / downsampleFactors[0]), (int)(dim[1] / downsampleFactors[1]), (int)(dim[2] / downsampleFactors[2])} : new int[]{(int)(dim[0] / downsampleFactors[0]), (int)(dim[1] / downsampleFactors[1])};  // downsampled image dimensions once the downsampleFactors have been applied to their respective image dimensions
			final DownSample<T> downsampler = new DownSample<T>(img, downsampledDim, 0.5f, 0.5f);	// optimal sigma is defined by 0.5f, as mentioned here: http://pacific.mpi-cbg.de/wiki/index.php/Downsample
			if (downsampler.checkInput() && downsampler.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				img = downsampler.getResult(); 
			} else { 
		        System.out.println(downsampler.getErrorMessage()); 
		        System.out.println("Bye.");
		        return null;
			}
			
			// 4 - Apply a median filter, to get rid of salt and pepper noise which could be mistaken for maxima in the algorithm (only applied if requested by user explicitly):
			if (useMedFilt) {
				IJ.log("Applying median filter...");
				IJ.showStatus("Applying median filter...");
				StructuringElement strel;
				
				// 4.1 - Need to figure out the dimensionality of the image in order to create a StructuringElement of the correct dimensionality (StructuringElement needs to have same dimensionality as the image):
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
				
				// 4.2 - Apply the median filter:
				final MedianFilter<T> medFilt = new MedianFilter<T>(img, strel, new OutOfBoundsStrategyMirrorFactory<T>()); 
				/** note: add back medFilt.checkInput() when it's fixed */
				if (medFilt.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
					img = medFilt.getResult(); 
				} else { 
			        System.out.println(medFilt.getErrorMessage()); 
			        System.out.println("Bye.");
			        return null;
				}
			}
			
			// #---------------------------------------#
			// #------        Time Trials       -------#
			// #---------------------------------------#
			long overall = 0;
			long numIterations = 1;
			for (int k = 0; k < numIterations; k++) {
				long startTime = System.currentTimeMillis();	
			/** Approach 1: L x (G x I ) */
			// 5 - Apply a Gaussian filter (code courtesy of Stephan Preibisch). Theoretically, this will make the center of blobs the brightest, and thus easier to find:
			/*IJ.log("Applying Gaussian filter...");
			IJ.showStatus("Applying Gaussian filter...");
			final GaussianConvolutionRealType<T> convGaussian = new GaussianConvolutionRealType<T>(img, new OutOfBoundsStrategyMirrorFactory<T>(), IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM);
			if (convGaussian.checkInput() && convGaussian.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				img = convGaussian.getResult(); 
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
				convLaplacian = new DirectConvolution<T, FloatType, T>(img.createType(), img, laplacianKernel);;
			} else {
				float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
				ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
				Image<FloatType> laplacianKernel = factory.createImage(new int[]{3, 3}, "Laplacian");
				quickKernel2D(laplacianArray, laplacianKernel);
				convLaplacian = new DirectConvolution<T, FloatType, T>(img.createType(), img, laplacianKernel);;
			}
			//if(convLaplacian.checkInput() && convLaplacian.process()) {
			if(convLaplacian.process()) {
				img = convLaplacian.getResult();
				//ImagePlus test = ImageJFunctions.copyToImagePlus(img);
				//test.show();
			} else {
				System.out.println(convLaplacian.getErrorMessage());
				System.out.println("Bye.");
				return null;
			}*/
			
			/** Approach 2: F(L) x F(G) x F(I) */
			
			// Gauss
			IJ.log("Applying Gaussian filter...");
			IJ.showStatus("Applying Gaussian filter...");
			ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
			Image<FloatType> gaussKernel = FourierConvolution.getGaussianKernel(factory, IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM, numDim);
			FourierConvolution<T, FloatType> fConvGauss = new FourierConvolution<T, FloatType>(img, gaussKernel);
			if (!fConvGauss.checkInput() || !fConvGauss.process()) {
				System.out.println( "Fourier Convolution failed: " + fConvGauss.getErrorMessage() );
				return null;
			}
			img = fConvGauss.getResult();
			
			// Laplace
			IJ.log("Applying Laplacian convolution...");
			IJ.showStatus("Applying Laplacian convolution...");
			Image<FloatType> laplacianKernel;
			if (numDim == 3) {
				float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
				laplacianKernel = factory.createImage(new int[]{3, 3, 3}, "Laplacian");
				quickKernel3D(laplacianArray, laplacianKernel);
			} else {
				float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
				laplacianKernel = factory.createImage(new int[]{3, 3}, "Laplacian");
				quickKernel2D(laplacianArray, laplacianKernel);
			}
			FourierConvolution<T, FloatType> fConvLaplacian = new FourierConvolution<T, FloatType>(img, laplacianKernel);
			if (!fConvLaplacian.checkInput() || !fConvLaplacian.process()) {
				System.out.println( "Fourier Convolution failed: " + fConvLaplacian.getErrorMessage() );
				return null;
			}
			img = fConvLaplacian.getResult();		
			
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
			
			
			// 7 - Find extrema of newly convoluted image:
			IJ.log("Finding extrema...");
			IJ.showStatus("Finding extrema...");
			RegionalExtremaFactory<T> extremaFactory = new RegionalExtremaFactory<T>(img, overTime);
			RegionalExtremaFinder<T> findExtrema = extremaFactory.createRegionalMaximaFinder(true);
			findExtrema.allowEdgeExtrema(allowEdgeMax);
			if (!findExtrema.checkInput() || !findExtrema.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				System.out.println( "Extrema Finder failed: " + findExtrema.getErrorMessage() );
				return null;
			}
			ArrayList< double[] > centeredExtrema = findExtrema.getRegionalExtremaCenters(false);
			extremaAllFrames.add(centeredExtrema);
			System.out.println("Find Maxima Run Time: " + findExtrema.getProcessingTime());
			System.out.println("Num regional maxima: " + centeredExtrema.size());
	

		}
		
		return new Object[] {extremaAllFrames};
	}
	
	/**
	 * 
	 * @param pixelWidth
	 * @param pixelHeight
	 * @param pixelDepth
	 * @param diam
	 * @return
	 */
	public float[] createDownsampledDim(float pixelWidth, float pixelHeight, float pixelDepth, float diam) {
		float widthFactor = (diam / pixelWidth) > GOAL_DOWNSAMPLED_BLOB_DIAM ? (diam / pixelWidth) / GOAL_DOWNSAMPLED_BLOB_DIAM : 1;
		float heightFactor = (diam / pixelHeight) > GOAL_DOWNSAMPLED_BLOB_DIAM ? (diam / pixelHeight) / GOAL_DOWNSAMPLED_BLOB_DIAM : 1;
		float depthFactor = (img.getNumDimensions() == 3 && (diam / pixelDepth) > GOAL_DOWNSAMPLED_BLOB_DIAM) ? (diam / pixelDepth) / GOAL_DOWNSAMPLED_BLOB_DIAM : 1;								
		float downsampleFactors[] = new float[]{widthFactor, heightFactor, depthFactor};
		
		return downsampleFactors;
	}
	
	/**
	 * 
	 * @param vals
	 * @param kern
	 */
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
	 * so I reproduced it here to avoid instantiating an object.
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
	
	/**
	 * 
	 * @param maxima
	 * @param downsamplingFactor
	 * @return
	 */
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
	
	/**
	 * 
	 * @param maxima
	 * @param scaled
	 * @param pixelWidth
	 * @param pixelHeight
	 * @param pixelDepth
	 */
	public void render3DAndOverlayExtrema(ArrayList< ArrayList<double[]> > maxima, ImagePlus dup, float pixelWidth, float pixelHeight, float pixelDepth, float downsampleFactors[], float diam, boolean overTime) {
		// Adjust calibration
		dup.getCalibration().pixelWidth = pixelWidth;
		dup.getCalibration().pixelHeight = pixelHeight;
		dup.getCalibration().pixelDepth = pixelDepth;
		
		// Convert to a usable format
		new StackConverter(dup).convertToGray8();

		// Create a universe, but do not show it
		Image3DUniverse univ = new Image3DUniverse();
		univ.show();
		
		// Add the image as a volume rendering
		Content c = univ.addVoltex(dup);

		/*
		// Change the size of the points
		float curr = c.getLandmarkPointSize();
		//c.setLandmarkPointSize(curr/9);
		c.setLandmarkPointSize(diam);
		
		// Retrieve the point list
		PointList pl = c.getPointList();
		
		// Add maxima as points to the point list
		Iterator< ArrayList<double[]> > frameItr = maxima.listIterator();
		int frameNum = 0;
		while (frameItr.hasNext()) {
			ArrayList<double[]> frame = frameItr.next();
			Iterator<double[]> itr = frame.listIterator();
			while (itr.hasNext()) {
				double maxCoords[] = itr.next();
				//int debug[] = new int[] {(int)maxCoords[0], (int)maxCoords[1], (int)maxCoords[2]};
				//IJ.log(MathLib.printCoordinates(debug));
				pl.add(maxCoords[0] * downsampleFactors[0] * pixelWidth, maxCoords[1] * downsampleFactors[1] * pixelHeight, maxCoords[2] * downsampleFactors[2] * pixelDepth);
			}
			frameNum++;
		}

		// Make the point list visible
		c.showPointList(true);
		*/
	}
}
