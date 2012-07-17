package fiji.plugin.trackmate.features.spot;

import java.util.Collection;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

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
 * The image data to operate on is set using the {@link #setTarget(ImgPlus<T>)} method. This 
 * allow the concrete implementation to have an empty constructor.
 * <p>
 * The spot collection to operate on is given through the method {@link #process(Collection)},
 * and it must update the feature map of each spot directly, calling {@link Spot#putFeature(String, double)}.
 */
public interface SpotFeatureAnalyzer<T> extends FeatureAnalyzer {
	
	
	/**
	 * Sets the image data this analyzer will operate on to grab the features it generates.
	 * The spatial calibration will be taken from the source {@link ImgPlus}.
	 */
	public void setTarget(ImgPlus<T> img);
	
	
	/**
	 * Compute all the spot features this analyzer can deal with 
	 * on the given collection of spots. The spots have their
	 * feature map updated by this method.
	 * @param spots  the spots to evaluate. 
	 */
	public void process(Collection<Spot> spots);
	

}
