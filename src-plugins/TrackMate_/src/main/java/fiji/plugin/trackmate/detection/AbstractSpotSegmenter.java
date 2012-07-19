package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.util.MedianFilter3x3;

/**
 * This abstract class for spot segmented plainly implements the {@link SpotSegmenter}
 * interface and offer convenience methods and protected fields.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 27, 2010
 */
public abstract class AbstractSpotSegmenter <T extends RealType<T> & NativeType<T>> implements SpotSegmenter<T> {
	
	/*
	 * PROTECTED FIELDS
	 */
	
	protected String baseErrorMessage = "";
	
	/** The image to segment. Will not modified. */
	protected ImgPlus<T> img;
	/** The list of {@link Spot} that will be populated by this segmenter. */
	protected List<Spot> spots = new ArrayList<Spot>(); // because this implementation is fast to add elements at the end of the list
	/** The error message generated when something goes wrong. */
	protected String errorMessage = null;
	/** The settings for this segmenter. Contains all parameters needed to perform segmentation
	 * for the concrete segmenter implementation. */
	protected SegmenterSettings<T> settings;
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
		return true;
	};
	
		
	@Override
	public List<Spot> getResult() {
		return spots;
	}
		
	@Override
	public void setTarget(final ImgPlus<T> image, final SegmenterSettings<T> settings) {
		this.spots = new ArrayList<Spot>();
		this.img = image;
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
	
	protected Img<T> applyMedianFilter(Img<T> image) {
		final MedianFilter3x3<T> medFilt = new MedianFilter3x3<T>(image); 
		if (!medFilt.checkInput() && !medFilt.process()) {
			errorMessage = baseErrorMessage + "Failed in applying median filter";
			return null;
		}
		return medFilt.getResult(); 
	}
	
}
