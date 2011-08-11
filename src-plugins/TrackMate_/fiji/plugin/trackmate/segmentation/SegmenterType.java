package fiji.plugin.trackmate.segmentation;

import fiji.plugin.trackmate.InfoTextable;

public enum SegmenterType implements InfoTextable {
	PEAKPICKER_SEGMENTER,
	LOG_SEGMENTER,
	DOG_SEGMENTER, 
	MANUAL_SEGMENTER;

	@Override
	public String toString() {
		switch(this) {
		case MANUAL_SEGMENTER:
			return "Manual segmentation";
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
		case DOG_SEGMENTER: {
			DogSegmenterSettings s = new DogSegmenterSettings();
			s.segmenterType = DOG_SEGMENTER;
			return s;
		}
		case PEAKPICKER_SEGMENTER:
		case MANUAL_SEGMENTER: { // We will return a classic segmenter settings, but only exploit the expected radius
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
			"other are suppressed. " +
			"</html>";
		case LOG_SEGMENTER:
			return "<html>" +
			"This segmenter is basically identical to the LoG segmenter, except <br>" +
			"that images are downsampled before filtering, giving it a small <br>" +
			"kick in speed, particularly for large spot sizes. It is the fastest for <br>" +
			"large spot sizes (>&nbsp;~20 pixels) " +
			"</html>";
		case DOG_SEGMENTER:
			return "<html>" +
			"This segmenter is based on an approximation of the LoG operator <br> " +
			"by differences of gaussian (DoG). Computations are made in direct space. <br>" +
			"It is the quickest for small spot sizes (< ~5 pixels). " +
			"<p> " +
			"Spots found too close are suppressed. This segmenter can do sub-pixel <br>" +
			"localization of spots. It is based on the scale-space framework <br>" +
			"made by Stephan Preibisch for ImgLib. " +
			"</html>";
		case MANUAL_SEGMENTER:
			return "<html>" +
			"Selecting this will skip the automatic segmentation phase, and jump directly <br>" +
			"to manual segmentation. A default spot size will be asked for. " +
			"</html>";
		}
		return null;
	}
}
