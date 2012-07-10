package fiji.plugin.trackmate.features.spot;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractSpotFeatureAnalyzer<T extends RealType<T>> implements SpotFeatureAnalyzer<T> {
	
	/** The image data to operate on. */
	protected Img<T> img;
	/** The spatial calibration of the field {@link #img} */
	protected  float[] calibration;

	@Override
	public void setTarget(Img<T> image, float[] calibration) {
		this.img = image;
		this.calibration = calibration;
	}

}
