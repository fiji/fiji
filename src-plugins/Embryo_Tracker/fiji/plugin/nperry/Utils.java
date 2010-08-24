package fiji.plugin.nperry;

import java.util.ArrayList;
import java.util.Iterator;

import ij.ImagePlus;
import ij.ImageStack;
import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.algorithm.roi.StructuringElement;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * List of static utilities for the {@link Embryo_Tracker} plugin
 */
public class Utils {
	
	/**
	 * Return the down-sampling factors that should be applied to the image so that 
	 * the diameter given (in physical units) would have a pixel size (diameter) set
	 * by the static field {@link Embryo_Tracker#GOAL_DOWNSAMPLED_BLOB_DIAM}.
	 * @param calibration  the physical calibration (pixel size)
	 * @param diam  the physical object diameter
	 * @return  a float array of down-sampling factors, for usage in {@link DownSample}
	 * @see #downSampleByFactor(Image, float[])
	 */
	public static float[] createDownsampledDim(final float[] calibration, final float diam) {
		float goal = Embryo_Tracker.GOAL_DOWNSAMPLED_BLOB_DIAM;
		int numDim = calibration.length;
		float widthFactor;
		if ( (diam / calibration[0]) > goal) {
			widthFactor = (diam / calibration[0]) / goal; // scale down to reach goal size
		} else{
			widthFactor = 1; // do not scale down
		}
		float heightFactor;
		if ( (diam / calibration[1]) > goal) {
			heightFactor = (diam / calibration[1]) / goal;
		} else {
			heightFactor = 1;
		}
		float depthFactor;
		if ( (numDim == 3 && (diam / calibration[2]) > goal) ) {
			depthFactor = (diam / calibration[2]) / goal; 
		} else {
			depthFactor = 1;								
		}
		float downsampleFactors[] = new float[]{widthFactor, heightFactor, depthFactor};
		return downsampleFactors;
	}
	
	/**
	 * Return a 3D stack or a 2D slice as an {@link Image} corresponding to the frame number <code>iFrame</code>
	 * in the given 4D or 3D {@link ImagePlus}.
	 * @param imp  the 4D or 3D source ImagePlus
	 * @param iFrame  the frame number to extract, 0-based
	 * @return  a 3D or 2D {@link Image} with the single timepoint required 
	 */
	public static <T extends RealType<T>> Image<T> getSingleFrameAsImage(ImagePlus imp, int iFrame) {
		ImageStack frame = imp.createEmptyStack();
		ImageStack stack = imp.getImageStack();
		int numSlices = imp.getNSlices();
		
		// ...create the slice by combining the ImageProcessors, one for each Z in the stack.
		for (int j = 1; j <= numSlices; j++) 
			frame.addSlice(Integer.toString(j + (iFrame * numSlices)), stack.getProcessor(j + (iFrame * numSlices)));
		
		ImagePlus ipSingleFrame = new ImagePlus("Frame " + Integer.toString(iFrame + 1), frame);
		return ImagePlusAdapter.wrap(ipSingleFrame);
	}
	
	/**
	 * Return a down-sampled copy of the source image, where every dimension has been shrunk 
	 * by the down-sampling factors given in argument.
	 * @param source  the image to down-sample
	 * @param downsampleFactors  the shrinking factor
	 * @return  a down-sampled copy of the source image
	 * @see #createDownsampledDim(float[], float)
	 */
	public static <T extends RealType<T>> Image<T> downSampleByFactor(final Image<T> source, final float[] downsampleFactors) {
		final int dim[] = source.getDimensions();
		for (int j = 0; j < dim.length; j++)
			dim[j] = (int) (dim[j] / downsampleFactors[j]);
	
		final DownSample<T> downsampler = new DownSample<T>(source, dim, 0.5f, 0.5f);	// optimal sigma is defined by 0.5f, as mentioned here: http://pacific.mpi-cbg.de/wiki/index.php/Downsample
		if (!downsampler.checkInput() || !downsampler.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
			System.out.println(downsampler.getErrorMessage()); 
	        System.out.println("Bye.");
	        return null;
		}
		return downsampler.getResult();
	}
	
	/**
	 * Takes the down-sampled coordinates of a list of {@link Spots}, and scales them back to be coordinates of the
	 * original image using the downsample factors.
	 * 
	 * @param spots The list of Spots to convert the coordinates for.
	 * @param downsampleFactors The downsample factors used for each dimension.
	 */
	public static void downsampledCoordsToOrigCoords(ArrayList<Spot> spots, float downsampleFactors[]) {
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
	
	/**
	 * Build a 3x3 square {@link StructuringElement}. The actual structure vary whether we get a 2D image
	 * or a 3D one, this is why the dimension number is required here. 
	 * @param numDim  the number of dimension of the target image 
	 * @return  a square structuring element suitable for the target image dimensionality, <code>null</code>
	 * if the dimensionality is not 2 or 3
	 */
	public static StructuringElement makeSquareStrel(int numDim) {
		StructuringElement strel;		
		// Need to figure out the dimensionality of the image in order to create a StructuringElement of the correct dimensionality (StructuringElement needs to have same dimensionality as the image):
		if (numDim == 3) {  // 3D case
			strel = new StructuringElement(new int[]{3, 3, 1}, "3D Square");  // unoptimized shape for 3D case. Note here that we manually are making this shape (not using a class method). This code is courtesy of Larry Lindsey
			Cursor<BitType> c = strel.createCursor();  // in this case, the shape is manually made, so we have to manually set it, too.
			while (c.hasNext()) { 
			    c.fwd(); 
			    c.getType().setOne(); 
			} 
			c.close(); 
		} else if (numDim == 2)  			// 2D case
			strel = StructuringElement.createCube(2, 3);  // unoptimized shape
		else 
			return null;
		return strel;
	}
	
	/**
	 * Return a new laplacian kernel suitable for convolution, in 2D or 3D. If the dimensionality
	 * given is not 2 or 3, <code>null<code> is returned.
	 */
	public static Image<FloatType> createLaplacianKernel(int numDim) {
		Image<FloatType> laplacianKernel;
		final ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		if (numDim == 3) {
			final float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.createImage(new int[]{3, 3, 3}, "Laplacian");
			quickKernel3D(laplacianArray, laplacianKernel);
		} else if (numDim == 2) {
			final float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.createImage(new int[]{3, 3}, "Laplacian");
			quickKernel2D(laplacianArray, laplacianKernel);
		} else 
			return null;
		return laplacianKernel;
	}
	
	
	
	
	
	
	/*
	 * PRIVATE METHODS
	 */

	private static void quickKernel3D(float[][][] vals, Image<FloatType> kern)	{
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
	private static void quickKernel2D(float[][] vals, Image<FloatType> kern)	{
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
	
	
}
