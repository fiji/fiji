package fiji.plugin.trackmate.features.spot;

import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.SpotAnalyzerProvider;
import fiji.plugin.trackmate.TrackMateModel;

/** 
 * Interface for factories that can generate a {@link SpotAnalyzer} configured
 * to operate on a specific frame of a model.
 * <p> 
 * Concrete implementation should declare what features they can compute numerically,
 * and make this info available in the {@link SpotAnalyzerProvider} that returns 
 * them.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 * 
 * @author Jean-Yves Tinevez - 2012
 */
public interface SpotFeatureAnalyzerFactory<T extends RealType<T> & NativeType<T>> {
	
	/**
	 * @return  a configured {@link SpotAnalyzer} ready to operate on the given frame
	 * (0-based) and given channel (0-based). 
	 * The target frame image and the target spots are retrieved from the {@link TrackMateModel}
	 * thanks to the given frame and channel index.
	 * 
	 * @param frame  the target frame to operate on.
	 * @param channel the target channel to operate on.
	 */
	public SpotAnalyzer<T> getAnalyzer(int frame, int channel);
	
	/** @return a unique String identifier for this factory. */
	public String getKey();
	
	/**
	 * @return  the list of features this factory analyzers can compute.
	 */
	public List<String> getFeatures();
	
	/**
	 * @return the map of short names for any feature the analyzers
	 * can compute.
	 */
	public Map<String, String> getFeatureShortNames();
	
	/**
	 * @return the map of names for any feature this factory analyzers can compute.
	 */
	public Map<String, String> getFeatureNames();
	
	/**
	 * @return the map of feature dimension this factory analyzers can compute.
	 */
	public Map<String, Dimension> getFeatureDimensions();

}
