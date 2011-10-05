package fiji.plugin.trackmate.segmentation;

import mpicbg.imglib.type.numeric.RealType;

public class ManualSegmenter <T extends RealType<T>> extends AbstractSpotSegmenter<T> {

	@Override
	public SegmenterSettings createDefaultSettings() {
		return new SegmenterSettings();
	}

	@Override
	public boolean process() {
		return true; // do nothing
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"Selecting this will skip the automatic segmentation phase, and jump directly <br>" +
				"to manual segmentation. A default spot size will be asked for. " +
				"</html>";
	}

	@Override
	public String toString() {
		return "Manual segmentation";
	}
	
	@Override
	public SpotSegmenter<T> createNewSegmenter() {
		return new ManualSegmenter<T>();
	}
	
}
