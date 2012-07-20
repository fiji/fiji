package fiji.plugin.trackmate.detection;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ManualDetector <T extends RealType<T>  & NativeType<T>> extends AbstractSpotDetector<T> {

	@Override
	public DetectorSettings<T> createDefaultSettings() {
		return new BasicDetectorSettings<T>();
	}

	@Override
	public boolean process() {
		return true; // do nothing
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"Selecting this will skip the automatic detection phase, and jump directly <br>" +
				"to manual segmentation. A default spot size will be asked for. " +
				"</html>";
	}

	@Override
	public String toString() {
		return "Manual segmentation";
	}
	
	@Override
	public SpotDetector<T> createNewDetector() {
		return new ManualDetector<T>();
	}
	
}
