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
	
	private static final float DEFAULT_EXPECTED_DIAMETER	= 10f;

	public enum SegmenterType {
		PEAKPICKER_SEGMENTER,
		LOG_SEGMENTER,
		DOG_SEGMENTER;
		
		@Override
		public String toString() {
			switch(this) {
			case LOG_SEGMENTER:
				return "Downsample LoG segmenter";
			case PEAKPICKER_SEGMENTER:
				return "LoG segmenter";
			case DOG_SEGMENTER:
				return "DoG segmenter";
			}
			return null;
		}

		public SegmenterSettings createSettings() {
			switch(this) {
			case LOG_SEGMENTER:
				return new LogSegmenterSettings();
			case PEAKPICKER_SEGMENTER:
			case DOG_SEGMENTER:
				return new SegmenterSettings();
			}
			return null;
		}

		public String getInfoText() {
			switch(this) {
			case PEAKPICKER_SEGMENTER:
				return "<html>" +
						"This segmenter applies a LoG (Laplacian of Gaussian) filter <br>" +
						"to the image, with a sigma suited to the blob estimated size.<br>" +
						"Calculations are made in the Fourier space. The maxima in the <br>" +
						"filtered image are searched for, and maxima too close from each <br>" +
						"other are suppressed." +
						"</html>";
			case LOG_SEGMENTER:
				return "<html>" +
						"This segmenter is basically identical to the LoG segmenter, except <br>" +
						"that images are downsampled before filtering, giving it a small <br>" +
						"kick in speed." +
						"</html>";
			case DOG_SEGMENTER:
				return "<html>" +
						"This segmented is based on an approximation of the LoG operator <br>" +
						"by differences of gaussian (DoG)." +
						"</html>";
			}
			return null;
		}
	}
	
	/** The expected blob diameter in physical units. */
	public float 	expectedRadius = DEFAULT_EXPECTED_DIAMETER/2;
	/** The pixel value under which any peak will be discarded from further analysis. */
	public float 	threshold = 		0;
	/** The segmenter type selected. */
	public SegmenterType segmenterType = SegmenterType.PEAKPICKER_SEGMENTER;
	public boolean useMedianFilter;
	
	
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
