package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

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
	 * PROTECTED FIELDS
	 */
	
	/**
	 * The image to segment. Will not modified.
	 */
	protected Image<T> img;
	/**
	 * The estimated radius of the spot to segment. This parameter will be used to tune the segmenter. 
	 */
	protected float radius;
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
	 * The settings for this segmenter. This should be cast to a suitable imeplementation.
	 */
	protected SegmenterSettings settings;
	
	/*
	 * SPOTSEGMENTER METHODS
	 */
	
	@Override
	public void setSettings(SegmenterSettings settings) {
		this.settings = settings;
	}
	
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
	public void setImage(Image<T> image) {
		this.spots = null;
		this.intermediateImage = null;
		this.img = image;
	}
		
	@Override
	public void setCalibration(float[] calibration) {
		this.calibration = calibration;
	}

	@Override
	public void setEstimatedRadius(float radius) {
		this.radius = radius;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage ;
	}
	

	
}
