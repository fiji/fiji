package fiji.plugin.trackmate.features.spot;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

public abstract class AbstractSpotFeatureAnalyzer<T> implements SpotFeatureAnalyzer<T> {
	
	/** The image data to operate on. */
	protected ImgPlus<T> img;

	@Override
	public void setTarget(ImgPlus<T> image) {
		this.img = image;
	}

}
