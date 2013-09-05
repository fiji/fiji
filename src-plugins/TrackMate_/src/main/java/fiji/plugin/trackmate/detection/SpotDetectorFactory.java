package fiji.plugin.trackmate.detection;

import java.util.Map;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Mother interface for {@link SpotDetector} factories.
 * <p>
 * These classes are able to configure a {@link SpotDetector} to operate on a target
 * {@link Img}. 
 */
public interface SpotDetectorFactory<T extends RealType<T> & NativeType<T>> {
	
	/**
	 * Returns a new {@link SpotDetector} configured to operate on the given target frame.
	 * This factory must be first given the {@link ImgPlus} and the settings map,
	 * through the {@link #setTarget(ImgPlus, Map)} method.  
	 * @param frame  the frame index in the source image to operate on
	 */
	public SpotDetector<T> getDetector(final int frame);
	
	/**
	 * Configure this factory to operate on the given source image (possibly 5D), 
	 * with the given settings map.
	 */
	public void setTarget(final ImgPlus<T> img, final Map<String, Object> settings);
	
	/**Returns a unique String identifier for this factory. */
	public String getKey();

	/** 
	 * Checks the validity of the given settings map for this factory.
	 * If check fails, and error message can be obtained through {@link #getErrorMessage()}.  
	 */
	public boolean checkInput();
	
	/**
	 * Returns a meaningful error message when {@link #checkInput()} failed.
	 */
	public String getErrorMessage();
}
