package fiji.plugin.trackmate.detection;

/**
 * A class to store key names for parameters of the current {@link SpotDetector}s.
 */
public class DetectorKeys {

	/**
	 * The attribute name for the {@link SpotDetectorFactory} key when
	 * marshalling to or unmarhsalling from XML. 
	 */
	public static final String XML_ATTRIBUTE_DETECTOR_NAME = "DETECTOR_NAME";
	
	/** The key identifying the parameter setting the target channel for detection
	 * in a possible multi-channel image. Channels are here 1-numbered, meaning 
	 * that "1" is the first available channel (and all images have at least this
	 * channel). Expected valkues are {@link Integer}s greater than 1.
	 * <p>
	 * Currently used by:
	 * <ul>
	 * 	<li> {@link LogDetector}
	 * 	<li> {@link DogDetector}
	 * 	<li> {@link DownsampleLogDetector}
	 * <ul>*/
	public static final String KEY_TARGET_CHANNEL = "TARGET_CHANNEL";
	
	/** A defayult value for the {@link #KEY_TARGET_CHANNEL} parameter. */
	public static final int DEFAULT_TARGET_CHANNEL = 1;
	
	/** The key identifying the parameter that sets the target radius for the detector. 
	 * Expected values are {@link Double}s.
	 * <p>
	 * Currently used by:
	 * <ul>
	 * 	<li> {@link LogDetector}
	 * 	<li> {@link DogDetector}
	 * 	<li> {@link DownsampleLogDetector}
	 * 	<li> {@link ManualDetectorFactory}
	 * <ul>*/
	public static final String KEY_RADIUS = "RADIUS";
	
	/** A default value for the {@link #KEY_RADIUS} parameter. */
	public static final double DEFAULT_RADIUS = 5d;

	/** The key identifying the parameter that sets the threshold for the LoG detector.
	 * Spot found with a filtered value lowered than this threshold will not be retained.  
	 * Expected values are {@link Double}s.
	 * <p>
	 * Currently used by:
	 * <ul>
	 * 	<li> {@link LogDetector}
	 * 	<li> {@link DogDetector}
	 * 	<li> {@link DownsampleLogDetector}
	 * </ul>
	 */
	public static final String KEY_THRESHOLD = "THRESHOLD";
	
	/** A default value for the {@link #KEY_THRESHOLD} parameter. */
	public static final double DEFAULT_THRESHOLD = 0d;

	/** The key identifying the parameter that sets the downsampling factor applied 
	 * to the source image prior to segmentation. Expected values are {@link Integer}s
	 * greater than 1. 
	 * <p>
	 * Currently used by {@link DownsampleLogDetector}
	 */
	public static final String KEY_DOWNSAMPLE_FACTOR = "DOWNSAMPLE_FACTOR";
	
	/** A default value for the {@link #KEY_DOWNSAMPLE_FACTOR} parameter. */
	public static final int DEFAULT_DOWNSAMPLE_FACTOR = 4;
	
	/** The key identifying the parameter setting whether we pre-filter the target image 
	 * with a median filter or not. Expected values are {@link Boolean}s. 
	 * <p>
	 * Currently used by:
	 * <ul>
	 * 	<li> {@link LogDetector}
	 * 	<li> {@link DogDetector}
	 * </ul>
	 */
	public static final String KEY_DO_MEDIAN_FILTERING = "DO_MEDIAN_FILTERING";
	
	/** A default value for the {@link #KEY_DO_MEDIAN_FILTERING} parameter. */
	public static final boolean DEFAULT_DO_MEDIAN_FILTERING = false;
	
	/** The key identifying the parameter setting whether we use sub-pixel
	 * localization for spot position. Accepted values are {@link Boolean}s. 
	 * <p>
	 * Currently used by:
	 * <ul>
	 * 	<li> {@link LogDetector}
	 * 	<li> {@link DogDetector}
	 * </ul>
	 */
	public static final String KEY_DO_SUBPIXEL_LOCALIZATION = "DO_SUBPIXEL_LOCALIZATION";
	
	/** A default value for the {@link #KEY_DO_SUBPIXEL_LOCALIZATION} parameter. */
	public static final boolean DEFAULT_DO_SUBPIXEL_LOCALIZATION = true;

}
