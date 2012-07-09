package fiji.plugin.trackmate.features.spot;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractSpotFeatureAnalyzer implements SpotFeatureAnalyzer {
	
	/** The image data to operate on. */
	protected Img<? extends RealType<?>> img;
	/** The spatial calibration of the field {@link #img} */
	protected  float[] calibration;

	@Override
	public void setTarget(Img<? extends RealType<?>> image, float[] calibration) {
		this.img = image;
		this.calibration = calibration;
	}

}
