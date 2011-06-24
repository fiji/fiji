package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import mpicbg.imglib.algorithm.roi.MedianFilter;
import mpicbg.imglib.algorithm.roi.StructuringElement;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;

/**
 * This abstract class for spot segmented plainly implements the {@link SpotSegmenter}
 * interface and offer convenience methods and protected fields.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 27, 2010
 */
public abstract class AbstractSpotSegmenter <T extends RealType<T>> implements SpotSegmenter<T> {

	/*
	 * CCONSTRUCTORS
	 */
	
	protected AbstractSpotSegmenter(SegmenterSettings segmenterSettings) {
		this.settings = segmenterSettings;
	}
	
	/*
	 * PROTECTED FIELDS
	 */
	
	protected String baseErrorMessage = "";
	
	/**
	 * The image to segment. Will not modified.
	 */
	protected Image<T> img;
	/** 
	 * The calibration array to convert pixel coordinates in physical spot coordinates.
	 * Negative or zero values ill generate an error.
	 */
	protected float[] calibration = new float[] {1, 1, 1}; // always 3d;
	/**
	 * The lsit of {@link Spot} that will be populated by this segmenter.
	 */
	protected List<Spot> spots = new ArrayList<Spot>(); // because this implementation is fast to add elements at the end of the list
	/**
	 * Most segmenters use an intermediate image that can be of use downstream to get feature values. 
	 * This provides a link to it.
	 */
	protected Image<T> intermediateImage;
	/**
	 * The error message generated when somthing goes wrong.
	 */
	protected String errorMessage = null;

	/**
	 * The settings for this segmenter. Contains all parameters needed to perform segmentation.
	 */
	protected SegmenterSettings settings;

	private StructuringElement strel;
	
	/*
	 * SPOTSEGMENTER METHODS
	 */
	@Override
	public boolean checkInput() {
		
		if (null == img) {
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if (!(img.getNumDimensions() == 2 || img.getNumDimensions() == 3)) {
			errorMessage = baseErrorMessage + "Image must be 2D or 3D, got " + img.getNumDimensions() +"D.";
			return false;
		}
		if (settings.expectedRadius <= 0) {
			errorMessage = baseErrorMessage + "Search diameter is negative or 0.";
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
	public SegmenterSettings getSettings() {
		return settings;
	}
	
	@Override
	public Image<T> getIntermediateImage() {
		return intermediateImage;
	}

	@Override
	public List<Spot> getResult() {
		return spots;
	}
	
	@Override
	public List<Spot> getResult(Settings settings) {
		ArrayList<Spot> translatedSpots = new ArrayList<Spot>(spots.size());
		Spot newSpot;
		float dx = (settings.xstart-1)*calibration[0];
		float dy = (settings.ystart-1)*calibration[1];
		float dz = (settings.zstart-1)*calibration[2];
		float[] dval = new float[] {dx, dy, dz};
		SpotFeature[] features = new SpotFeature[] {SpotFeature.POSITION_X, SpotFeature.POSITION_Y, SpotFeature.POSITION_Z}; 
		Float val;
		for(Spot spot : spots) {
			newSpot = spot.clone();
			for (int i = 0; i < features.length; i++) {
				val = newSpot.getFeature(features[i]);
				if (null != val)
					newSpot.putFeature(features[i], val+dval[i]);
			}
			translatedSpots.add(newSpot);
		}
		return translatedSpots;
	}
	
	@Override
	public void setImage(Image<T> image) {
		this.spots = new ArrayList<Spot>();
		this.intermediateImage = null;
		this.img = image;
		if (settings.useMedianFilter)
			createSquareStrel();
	}
		
	@Override
	public void setCalibration(float[] calibration) {
		this.calibration = calibration;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage ;
	}
	
	
	/*
	 * PROTECTED METHODS
	 */
	
	/**
	 * Apply a median filter to the {@link #intermediateImage} field, which gets updated.
	 */
	protected boolean applyMedianFilter() {
		final MedianFilter<T> medFilt = new MedianFilter<T>(intermediateImage, strel, new OutOfBoundsStrategyMirrorFactory<T>()); 
		if (!medFilt.process()) {
			errorMessage = baseErrorMessage + "Failed in applying median filter";
			return false;
		}
		intermediateImage = medFilt.getResult(); 
		return true;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Creates the structuring element that will be used if the user request to have
	 * a median filter applied.
	 */
	private void createSquareStrel() {
		int numDim = img.getNumDimensions();
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
	}
	
}
