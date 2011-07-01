package fiji.plugin.trackmate.segmentation;

/** 
 * Empty interface to pass settings to the concrete implementations of {@link SpotSegmenter}.
 * The concrete derivation of this interface should be matched to the concrete implementation
 * of {@link SpotSegmenter}, and contain only public fields.
 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 27, 2010
 *
 */
public class SegmenterSettings {
	
	private static final float DEFAULT_EXPECTED_DIAMETER	= 10f;

	
	
	/** The expected blob diameter in physical units. */
	public float 	expectedRadius = DEFAULT_EXPECTED_DIAMETER/2;
	/** The pixel value under which any peak will be discarded from further analysis. */
	public float 	threshold = 		0;
	/** If true, a median filter will be applied before segmenting. */
	public boolean useMedianFilter;
	/** The physical units for {@link #expectedRadius}. */
	public String spaceUnits= "";
	/** To what segmenter type this settings apply to. This field is here just for reference. */
	public SegmenterType segmenterType;
	
	
	/*
	 * METHODS
	 */
	
	
	
	@Override
	public String toString() {
		String 	str = "Segmenter: "+ segmenterType.toString()+'\n';
		str += String.format("  Expected radius: %f %s\n", expectedRadius, spaceUnits);
		str += String.format("  Threshold: %f\n", threshold);
		str += "  Median filter: "+useMedianFilter+'\n';
		return str;
	}
	
}
