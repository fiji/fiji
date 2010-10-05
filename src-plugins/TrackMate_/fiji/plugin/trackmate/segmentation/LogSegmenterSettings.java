package fiji.plugin.trackmate.segmentation;

public class LogSegmenterSettings extends SegmenterSettings {

	private static final boolean	DEFAULT_USE_MEDIAN_FILTER 	= false;
	private static final boolean	DEFAULT_ALLOW_EDGE_MAXIMA	= false;
	
	/** If true, a 3x3 median filter will be applied to the image before segmentation. */
	public boolean 	useMedianFilter = DEFAULT_USE_MEDIAN_FILTER;
	/** If true, blob found at the edge of the image will not be discarded. */
	public boolean 	allowEdgeMaxima = DEFAULT_ALLOW_EDGE_MAXIMA;
	
}
