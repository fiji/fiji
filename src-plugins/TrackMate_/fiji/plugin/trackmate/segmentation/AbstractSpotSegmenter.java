package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import mpicbg.imglib.image.Image;
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
		Feature[] features = new Feature[] {Feature.POSITION_X, Feature.POSITION_Y, Feature.POSITION_Z}; 
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
	}
		
	@Override
	public void setCalibration(float[] calibration) {
		this.calibration = calibration;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage ;
	}
	
}
