package fiji.plugin.trackmate.features.spot;

import java.util.Collection;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureAnalyzer;

/**
 * Interface for a class that can compute feature on a collection of spots.
 * <p>
 * Concrete implementation must declare what features they can compute numerically, 
 * using the method {@link #getFeatures()}. The names and dimension of these 
 * features are also specified in 3 maps: {@link #getFeatureNames()}, {@link #getFeatureShortNames()}
 * and {@link #getFeatureDimensions()}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 * <p>
 * The image data to operate on is set using the {@link #setTarget(Image, float[])} method. This 
 * allow the concrete implementation to have an empty constructor.
 * <p>
 * The spot collection to operate on is given through the method {@link #process(Collection)},
 * and it must update the feature map of each spot directly, calling {@link Spot#putFeature(String, float)}.
 */
public interface SpotFeatureAnalyzer extends FeatureAnalyzer {
	
	
	/**
	 * Sets the image data this analyzer will operate on to grab the features it generates.
	 * We require the spatial calibration to be given as well, for we do not trust the  
	 * {@link Image#getCalibration()} field.
	 */
	public void setTarget(Image<? extends RealType<?>> img, float[] calibration);
	
	
	/**
	 * Compute all the spot features this analyzer can deal with 
	 * on the given collection of spots. The spots have their
	 * feature map updated by this method.
	 * @param spots  the spots to evaluate. 
	 */
	public void process(Collection<Spot> spots);
	

}
