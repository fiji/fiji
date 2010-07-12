/** 
 * Author: Nick Perry 
 */

package fiji.plugin.nperry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.plugin.PlugIn;
import ij.*;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.Image3DUniverse;
import vib.PointList;
import mpicbg.imglib.algorithm.findmax.FindLocalMaximaFactory;
import mpicbg.imglib.algorithm.findmax.LocalMaximaFinder;
import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionRealType;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.algorithm.roi.MedianFilter;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.algorithm.roi.StructuringElement;

public class Embryo_Tracker<T extends RealType<T>> implements PlugIn {
	/** Class/Instance variables */
	
	/* Stores the image used by Imglib */
	protected Image<T> img;
	/* Bitmasks used in the findMaxima algorithm to perform quick checks */
	final static byte VISITED = (byte)1;	// pixel has been added to the lake, but not had neighbors inspected (explored, but not searched)
	final static byte PROCESSED = (byte)2;	// pixel has been added to the lake, and had neighbors inspected (explored, and searched)
	final static float GOAL_DOWNSAMPLED_BLOB_DIAM_3D = 10f;
	//final static float GOAL_DOWNSAMPLED_BLOB_DIAM_2D = 20f;
	final static double IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM_3D = 1.55f;
	//final static double IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM_2D = 6f;
	
	/** Ask for parameters and then execute. */
	public void run(String arg) {
		// 1 - Obtain the currently active image:
		ImagePlus imp = IJ.getImage();
		if (null == imp) return;
		
		// 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Track");
		gd.addNumericField("Generic blob diameter:", 20, 0, 5, imp.getCalibration().getUnits());  				// get the expected blob size (in pixels).
		gd.addMessage("Verify calibration settings:");
		gd.addNumericField("Pixel width:", imp.getCalibration().pixelWidth, 3);		// used to calibrate the image for 3D rendering
		gd.addNumericField("Pixel height:", imp.getCalibration().pixelHeight, 3);	// used to calibrate the image for 3D rendering
		gd.addNumericField("Voxel depth:", imp.getCalibration().pixelDepth, 3);		// used to calibrate the image for 3D rendering
		gd.addCheckbox("Use median filter", false);
		gd.addCheckbox("Allow edge maxima", false);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		// 3 - Retrieve parameters from dialogue:
		float diam = (float)gd.getNextNumber();
		float pixelWidth = (float)gd.getNextNumber();
		float pixelHeight = (float)gd.getNextNumber();
		float pixelDepth = (float)gd.getNextNumber();
		boolean useMedFilt = (boolean)gd.getNextBoolean();
		boolean allowEdgeMax = (boolean)gd.getNextBoolean();
		
		// 3.5 - Allow user to draw blobs
		
		// 4 - Execute!
		//int downsampledDim[] = createDownsampledDim(pixelWidth, pixelHeight, pixelDepth, diam);
		float downsamplingFactor;
		downsamplingFactor = (float) 1f / ((float)diam / GOAL_DOWNSAMPLED_BLOB_DIAM_3D);
		Object[] result = exec(imp, diam, useMedFilt, allowEdgeMax, downsamplingFactor, pixelWidth, pixelHeight, pixelDepth);
		
		// 5 - Display new image and overlay maxima
		if (null != result) {
			if (img.getNumDimensions() == 3) {	// If original image is 3D, create a 3D rendering of the image and overlay maxima
				Image3DUniverse univFin = (Image3DUniverse) result[0];
				univFin.show();
			} else {
				PointRoi roi = (PointRoi) result[0];
				imp.setRoi(roi);
				imp.updateAndDraw();
			}
		}
	}
	
	/** Execute the plugin functionality: apply a median filter (for salt and pepper noise), a Gaussian blur, and then find maxima. */
	public Object[] exec(ImagePlus imp, double diam, boolean useMedFilt, boolean allowEdgeMax, float downsamplingFactor, float pixelWidth, float pixelHeight, float pixelDepth) {
		// 0 - Check validity of parameters:
		if (null == imp) return null;
		
		// 1 - Make a copy of the image, and prepare for use with Imglib:
		img = ImagePlusAdapter.wrap(imp);
		int numDim = img.getNumDimensions();
		
		// 2 - Downsample to improve run time. The image is downsampled by the factor necessary to achieve a resulting blob size of about 10 pixels (therefore, downsample factor depends on the blob size inputed by the user).
		if (downsamplingFactor != 1) {  // downsampling factor of 1 indicates no downsampling to be done
			IJ.log("Downsampling...");
			IJ.showStatus("Downsampling...");
			final DownSample<T> downSampler = new DownSample<T>(img, downsamplingFactor);
			if (downSampler.checkInput() && downSampler.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				img = downSampler.getResult(); 
			} else { 
		        System.out.println(downSampler.getErrorMessage()); 
		        return null;
			}
		}
		
		// 3 - Apply a median filter, to get rid of salt and pepper noise which could be mistaken for maxima in the algorithm:
		if (useMedFilt) {
			IJ.log("Applying median filter...");
			IJ.showStatus("Applying median filter...");
			StructuringElement strel;
			
			// 3.1 - Need to figure out the dimensionality of the image in order to create a StructuringElement of the correct dimensionality (StructuringElement needs to have same dimensionality as the image):
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
			
			// 3.2 - Apply the median filter:
			final MedianFilter<T> medFilt = new MedianFilter<T>(img, strel, new OutOfBoundsStrategyMirrorFactory<T>()); 
			// ***note: add back medFilt.checkInput() when it's fixed ***
			if (medFilt.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				img = medFilt.getResult(); 
			} else { 
		        System.out.println(medFilt.getErrorMessage()); 
		        return null;
			}
		}
		
		// 4 - Apply a Gaussian filter (code courtesy of Stephan Preibisch). Theoretically, this will make the center of blobs the brightest, and thus easier to find:
		IJ.log("Applying Gaussian filter...");
		IJ.showStatus("Applying Gaussian filter...");
		final GaussianConvolutionRealType<T> conv;
		conv = new GaussianConvolutionRealType<T>(img, new OutOfBoundsStrategyMirrorFactory<T>(), IDEAL_SIGMA_FOR_DOWNSAMPLED_BLOB_DIAM_3D);
		if (conv.checkInput() && conv.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
			img = conv.getResult(); 
		} else { 
	        System.out.println(conv.getErrorMessage()); 
	        return null;
		}
		
		// 5 - Find maxima of newly convoluted image:
		IJ.log("Finding maxima...");
		IJ.showStatus("Finding maxima...");
		ArrayList< double[] > maxima;
		/*if (numDim == 2) {
			FindMaxima2D<T> findMax = new FindMaxima2D<T>(img, new OutOfBoundsStrategyMirrorFactory<T>(), allowEdgeMax);
			if (findMax.checkInput() && findMax.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				maxima = findMax.getResult(); 
			} else { 
		        System.out.println(findMax.getErrorMessage()); 
		        return null;
			}
		} else {
			FindMaxima3D<T> findMax = new FindMaxima3D<T>(img, new OutOfBoundsStrategyMirrorFactory<T>(), allowEdgeMax);
			if (findMax.checkInput() && findMax.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
				maxima = findMax.getResult(); 
			} else { 
		        System.out.println(findMax.getErrorMessage()); 
		        return null;
			}
		}*/
		FindLocalMaximaFactory<T> maxFactory = new FindLocalMaximaFactory<T>();
		LocalMaximaFinder findMax = maxFactory.createLocalMaximaFinder(img, new OutOfBoundsStrategyMirrorFactory<T>(), allowEdgeMax);
		if (findMax.checkInput() && findMax.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
			maxima = findMax.getLocalMaxima(); 
		} else { 
	        System.out.println(findMax.getErrorMessage()); 
	        return null;
		}
		
		// 6 - Setup for displaying results
		if (numDim == 3) {  // prepare 3D render
			ImagePlus scaled = imp;
			Image3DUniverse univ = 	render3DAndOverlayMaxima(maxima, scaled, pixelWidth, pixelHeight, pixelDepth, downsamplingFactor);
			return new Object[]{univ};
		} else {
			PointRoi roi = preparePointRoi(maxima, downsamplingFactor);
			return new Object[]{roi};
		}
		
	}
	
	public int[] createDownsampledDim(float pixelWidth, float pixelHeight, float pixelDepth, float diam) {
		int dWidth;
		int dHeight;
		int dDepth;
		int downsampledDim[] = new int[3];
		
		return downsampledDim;
	}
	
	/**
	 * 
	 * @param maxima
	 * @param downsamplingFactor
	 * @return
	 */
	public PointRoi preparePointRoi (ArrayList< double[] > maxima, float downsamplingFactor) {
		int numPoints = maxima.size();
		int ox[] = new int[numPoints];
		int oy[] = new int[numPoints];
		ListIterator< double[] > itr = maxima.listIterator();
		int index = 0;
		while (itr.hasNext()) {
			double curr[] = itr.next();
			ox[index] = (int) (curr[0] / downsamplingFactor);
			oy[index] = (int) (curr[1] / downsamplingFactor);
			index++;
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
	public Image3DUniverse render3DAndOverlayMaxima(ArrayList< double[] > maxima, ImagePlus scaled, double pixelWidth, double pixelHeight, double pixelDepth, float downsamplingFactor) {
		// Adjust image properties for 3D rendering
		scaled.getCalibration().pixelWidth = pixelWidth;
		scaled.getCalibration().pixelHeight = pixelHeight;
		scaled.getCalibration().pixelDepth = pixelDepth;
		
		// Convert to a usable format
		new StackConverter(scaled).convertToGray8();
		
		// Create a universe, but do not show it
		Image3DUniverse univ = new Image3DUniverse();
		
		// Add the image as a volume rendering
		Content c = univ.addVoltex(scaled);

		// Change the size of the points
		float curr = c.getLandmarkPointSize();
		c.setLandmarkPointSize(curr/9);
		
		// Retrieve the point list
		PointList pl = c.getPointList();
		
		// Add maxima as points to the point list
		Iterator< double[] > itr = maxima.listIterator();
		while (itr.hasNext()) {
			double maxCoords[] = itr.next();
			pl.add(maxCoords[0] / downsamplingFactor * pixelWidth, maxCoords[1] / downsamplingFactor * pixelHeight, maxCoords[2] / downsamplingFactor * pixelDepth);
		}

		// Make the point list visible
		c.showPointList(true);
		
		return univ;
	}
}

/** ---------------------------- */
/**         archived code        */
/** ---------------------------- */

/** Tried implementing findMaxima using a trick from Michael Schmid's version of 'Find Maxima' that comes with ImageJ to speed my version up. Ultimately, my version above and "his version" (not quite his, but as best I could) produced the same performance.  */
/*public void findMaxima2D(Image<T> img) {
long start = System.currentTimeMillis();
// 1 - Initialize local variables, cursors
final LocalizableByDimCursor<T> curr = img.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>()); // this cursor is the main cursor which iterates over all the pixels in the image.  
LocalizableByDimCursor<T> local = img.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>());		// this cursor is used to search a connected "lake" of pixels, or pixels with the same value
LocalNeighborhoodCursor<T> neighbors = new LocalNeighborhoodCursor<T>(local);										// this cursor is used to search the immediate neighbors of a pixel
ArrayList< int[] > maxCoordinates = new ArrayList< int[] >();  	// holds the positions of the local maxima
T currentValue = img.createType();  							// holds the value of the current pixel's intensity. We use createType() here because getType() gives us a pointer to the cursor's object, but since the neighborhood moves the parent cursor, when we check the neighbors, we actually change the object stored here, or the pixel we are trying to compare to. see fiji-devel list for further explanation.
T neighborValue; 												// holds the value of the neighbor's intensity
int width = img.getDimensions()[0];								// width of the image. needed for storing info in the visited and visitedLakeMember arrays correctly
byte visited[] = new byte[img.getNumPixels()];					// stores whether or not this pixel has been searched either by the main cursor, or directly in a lake search.
boolean isMax;													// flag which tells us whether the current lake is a local max or not
int currCoords[] = new int[2];									// coords of outer, main loop
int neighborCoords[] = new int[2];								// coords of neighbor
int averagedMaxPos[] = new int[2];								// for a local max lake, this stores the 'center' of the lake's position.

int pList[] = new int[img.getNumPixels()];

// 2 - Search all pixels for LOCAL maxima. A local maximum is a pixel that is the brightest in its immediate neighborhood (so the pixel is brighter or as bright as the 26 direct neighbors of it's cube-shaped neighborhood if 3D). If neighboring pixels have the same value as the current pixel, then the pixels are treated as a local "lake" and the lake is searched to determine whether it is a maximum "lake" or not.

// 2.1 - Iterate over all pixels in the image.
while(curr.hasNext()) { 
	int listI = 0;
	int listLen = 1;
	curr.fwd();
	curr.getPosition(currCoords);
	if ((visited[getIndexOfPosition2D(currCoords, width)] & PROCESSED) != 0) {	// if we've already seen this pixel, then we've already decided if its a max or not, so skip it
		continue;
	}
	isMax = true;  				// this pixel could be a max
	pList[listI] = getIndexOfPosition2D(currCoords, width);
	// 2.2 - Iterate through queue which contains the pixels of the "lake"
	do {
		int offset = pList[listI];
		int x = offset % width;
		int y = offset / width;
		if ((visited[offset] & PROCESSED) != 0) {	// prevents us from just searching the lake infinitely
			listI++;
			continue;
		} else {	// if we've never seen, add to visited list, and add to searched list.
			visited[offset] |= PROCESSED;	
		}
		local.setPosition(new int[] {x, y});		// set the local cursors position to the next member of the lake, so that the neighborhood cursor can search its neighbors.
		currentValue.set(local.getType());  // store the value of this pixel in a variable
		neighbors.update();
		while(neighbors.hasNext()) {
			neighbors.fwd();
			neighborCoords = local.getPosition();
			neighborValue = neighbors.getType();
			
			// Case 1: neighbor's value is strictly larger than ours, so ours cannot be a local maximum.
			if (neighborValue.compareTo(currentValue) > 0) {
				isMax = false;
			}
			
			// Case 2: neighbor's value is strictly equal to ours, which means we could still be at a maximum, but the max value is a blob, not just a single point. We must check the area. In other words, we have a lake.
			else if (neighborValue.compareTo(currentValue) == 0 && isWithinImageBounds2D(neighborCoords) && (visited[getIndexOfPosition2D(neighborCoords, width)] & VISITED) == 0) {
				pList[listLen] = getIndexOfPosition2D(neighborCoords, width);
				visited[getIndexOfPosition2D(neighborCoords, width)] |= VISITED;  // prevents us from adding the same thing to the pList multiple times.
				listLen++;
			}
		}
		neighbors.reset();
		listI++;
	} while (listI < listLen);
	if (isMax) {	// if we get here, we've searched the entire lake, so find the average point and call that a max by adding to results list
		averagedMaxPos = findAveragePosition2D(pList, listLen, width);
		maxCoordinates.add(averagedMaxPos);
	}
}
//IJ.log("done searching!");
curr.close();
neighbors.close();

long deltaT = System.currentTimeMillis() - start;

// 3 - Print out list of maxima, set up for point display (FOR TESTING):
ox = new int[maxCoordinates.size()];
oy = new int[maxCoordinates.size()];
points = maxCoordinates.size();
int index = 0;
String img_dim = MathLib.printCoordinates(img.getDimensions());
IJ.log("Image dimensions: " + img_dim);
Iterator<int[]> itr = maxCoordinates.iterator();
while (itr.hasNext()) {
	int coords[] = itr.next();
	ox[index] = coords[0];
	oy[index] = coords[1];
	String pos_str = MathLib.printCoordinates(coords);
	IJ.log(pos_str);
	index++;
}
}*/

/*public int[] findAveragePosition2D(int pList[], int listLen, int width) {
int count = 0;
int avgX = 0, avgY = 0;
for (int i = 0; i < listLen; i++) {
	int curr = pList[i];
	avgX += curr % width;
	avgY += curr / width;
	count++;
}
return new int[] {avgX/count, avgY/count};
}*/