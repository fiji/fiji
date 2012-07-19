package fiji.plugin.trackmate.detection;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ManualSegmenter <T extends RealType<T>  & NativeType<T>> extends AbstractSpotSegmenter<T> {

	@Override
	public SegmenterSettings<T> createDefaultSettings() {
		return new BasicSegmenterSettings<T>();
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
