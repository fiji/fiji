package fiji.plugin.trackmate.detection;

import java.util.Map;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ManualDetectorFactory <T extends RealType<T>  & NativeType<T>> implements SpotDetectorFactory<T> {

	public static final String DETECTOR_KEY = "MANUL_DETECTOR";
	public static final String NAME = "Manual annotation";
	public static final String INFO_TEXT = "<html>" +
			"Selecting this will skip the automatic detection phase, and jump directly <br>" +
			"to manual segmentation. A default spot size will be asked for. " +
			"</html>";

	@Override
	public SpotDetector<T> getDetector(int frame) {
		// Return nothing
		return null;
	}

	@Override
	public void setTarget(ImgPlus<T> img, Map<String, Object> settings) {}

	@Override
	public String getKey() {
		return DETECTOR_KEY;
	}
	
	@Override
	public String toString() {
		return NAME;
	}
}
