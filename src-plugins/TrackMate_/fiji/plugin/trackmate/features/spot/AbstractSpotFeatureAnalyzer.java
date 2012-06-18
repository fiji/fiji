package fiji.plugin.trackmate.features.spot;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

public abstract class AbstractSpotFeatureAnalyzer implements SpotFeatureAnalyzer {
	
	/** The image data to operate on. */
	protected Image<? extends RealType<?>> img;
	/** The spatial calibration of the field {@link #img} */
	protected  float[] calibration;

	@Override
	public void setTarget(Image<? extends RealType<?>> image, float[] calibration) {
		this.img = image;
		this.calibration = calibration;
	}

}
