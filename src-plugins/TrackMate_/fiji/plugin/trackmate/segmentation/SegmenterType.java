package fiji.plugin.trackmate.segmentation;

import fiji.plugin.trackmate.InfoTextable;

public enum SegmenterType implements InfoTextable {
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

	/**
	 * Create a new {@link SegmenterSettings} object suited to the {@link SpotSegmenter} referenced by this enum.
	 */
	public SegmenterSettings createSettings() {
		switch(this) {
		case LOG_SEGMENTER: {
			LogSegmenterSettings s =  new LogSegmenterSettings();
			s.segmenterType = LOG_SEGMENTER;
			return s;
		}
		case PEAKPICKER_SEGMENTER:
		case DOG_SEGMENTER: 
		{
			SegmenterSettings s = new SegmenterSettings();
			s.segmenterType = this;
			return s;
		}
		}
		return null;
	}

	@Override
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
