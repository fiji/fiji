package fiji.plugin.trackmate.segmentation;

import mpicbg.imglib.type.numeric.RealType;


/** 
 * Empty interface to pass settings to the concrete implementations of {@link SpotSegmenter}.
 * The concrete derivation of this interface should be matched to the concrete implementation
 * of {@link SpotSegmenter}, and contain only public fields.
 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 27, 2010
 *
 */
public class SegmenterSettings {
	
	private static final float 		DEFAULT_EXPECTED_DIAMETER	= 10f;

	public enum SegmenterType {
		LOG_SEGMENTER,
		PEAKPICKER_SEGMENTER,
		DOG_SEGMENTER;
	}
	
	/** The expected blob diameter in physical units. */
	public float 	expectedRadius = DEFAULT_EXPECTED_DIAMETER/2;
	/** The pixel value under which any peak will be discarded from further analysis. */
	public float 	threshold = 		0;
	/** The segmenter type selected. */
	public SegmenterType segmenterType = SegmenterType.DOG_SEGMENTER;
	
	
	/*
	 * METHODS
	 */
	
	public static <T extends RealType<T>> SpotSegmenter<T> getSpotSegmenter(SegmenterSettings settings) {
		switch(settings.segmenterType) {
		case LOG_SEGMENTER:
			return new LogSegmenter<T>((LogSegmenterSettings) settings);
		case PEAKPICKER_SEGMENTER:
			return new PeakPickerSegmenter<T>(settings);
		case DOG_SEGMENTER:
			return new DogSegmenter<T>(settings);
		}
		return null;
	}
	
}
