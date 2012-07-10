package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Spot;

/**
 * This abstract class for spot segmented plainly implements the {@link SpotSegmenter}
 * interface and offer convenience methods and protected fields.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 27, 2010
 */
public abstract class AbstractSpotSegmenter <T extends RealType<T>> implements SpotSegmenter<T> {
	
	/*
	 * PROTECTED FIELDS
	 */
	
	protected String baseErrorMessage = "";
	
	/**
	 * The image to segment. Will not modified.
	 */
	protected Img<T> img;
	/** 
	 * The calibration array to convert pixel coordinates in physical spot coordinates.
	 * Negative or zero values ill generate an error.
	 */
	protected float[] calibration = new float[] {1, 1, 1}; // always 3d;
	/**
	 * The list of {@link Spot} that will be populated by this segmenter.
	 */
	protected List<Spot> spots = new ArrayList<Spot>(); // because this implementation is fast to add elements at the end of the list
	/**
	 * The error message generated when something goes wrong.
	 */
	protected String errorMessage = null;

	/**
	 * The settings for this segmenter. Contains all parameters needed to perform segmentation
	 * for the concrete segmenter implementation.
	 */
	protected SegmenterSettings settings;

	private StructuringElement strel;
	/** The processing time in ms. */
	protected long processingTime;
	
	/*
	 * SPOTSEGMENTER METHODS
	 */
		
	@Override
	public boolean checkInput() {
		
		if (null == img) {
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if (!(img.numDimensions() == 2 || img.numDimensions() == 3)) {
			errorMessage = baseErrorMessage + "Image must be 2D or 3D, got " + img.numDimensions() +"D.";
			return false;
		}
		if (calibration == null) {
			errorMessage = baseErrorMessage + "Calibration array is null";
			return false;
		}
		for (int i = 0; i < calibration.length; i++) {
			if (calibration[i] <= 0) {
				errorMessage = baseErrorMessage + "Calibration array has negative or 0 elements.";
				return false;
			}
		}
		return true;
	};
	
		
	@Override
	public List<Spot> getResult() {
		return spots;
	}
		
	@Override
	public void setTarget(Img<T> image, float[] calibration, SegmenterSettings settings) {
		this.spots = new ArrayList<Spot>();
		this.img = image;
		this.calibration = calibration;
		this.settings = settings;
	}
		
	@Override
	public String getErrorMessage() {
		return errorMessage ;
	}
	

	@Override
	public long getProcessingTime() {
		return processingTime;
	}
	
	/*
	 * PROTECTED METHODS
	 */
	
	/**
	 * Apply a median filter to the {@link #intermediateImage} field, which gets updated.
	 */
	protected Img<T> applyMedianFilter(Img<T> image) {
		createSquareStrel();
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final MedianFilter<T> medFilt = new MedianFilter(image, strel, new OutOfBoundsSingleMirrorFactory()); 
		if (!medFilt.process()) {
			errorMessage = baseErrorMessage + "Failed in applying median filter";
			return null;
		}
		return medFilt.getResult(); 
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Creates the structuring element that will be used if the user request to have
	 * a median filter applied.
	 */
	private void createSquareStrel() {
		int numDim = img.numDimensions();
		// Need to figure out the dimensionality of the image in order to create a StructuringElement of the correct dimensionality (StructuringElement needs to have same dimensionality as the image):
		if (numDim == 3) {  // 3D case
			strel = new StructuringElement(new int[]{3, 3, 1}, "3D Square");  // unoptimized shape for 3D case. Note here that we manually are making this shape (not using a class method). This code is courtesy of Larry Lindsey
			Cursor<BitType> c = strel.createCursor();  // in this case, the shape is manually made, so we have to manually set it, too.
			while (c.hasNext()) { 
			    c.fwd(); 
			    c.get().setOne(); 
			} 
			c.close(); 
		} else if (numDim == 2)  			// 2D case
			strel = StructuringElement.createCube(2, 3);  // unoptimized shape
	}
	
}
