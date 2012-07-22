package fiji.plugin.trackmate.detection;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ManualDetector <T extends RealType<T>  & NativeType<T>> extends AbstractSpotDetector<T> {

	public static final String NAME = "Manual segmentation";
	public static final String INFO_TEXT = "<html>" +
			"Selecting this will skip the automatic detection phase, and jump directly <br>" +
			"to manual segmentation. A default spot size will be asked for. " +
			"</html>";

	@Override
	public boolean process() {
		return true; // do nothing
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public String toString() {
		return NAME;
	}
	
}
