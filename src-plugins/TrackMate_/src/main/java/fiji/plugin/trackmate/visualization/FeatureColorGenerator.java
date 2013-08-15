package fiji.plugin.trackmate.visualization;

import java.awt.Color;

/**
 * Interface for color generator that can color objects based on a 
 * feature identified by a String.
 * @author Jean-Yves Tinevez - 2013
 *
 * @param <K> the type of object to color.
 */
public interface FeatureColorGenerator<K> {

	/** 
	 * Returns a color for the given object.
	 * @param the object to color.
	 * @return a color for this object.
	 */
	public abstract Color color(K obj);
	
	/**
	 * Sets the feature to generate the color from.
	 * @param feature  the feature.
	 */
	public void setFeature(String feature);

	/**
	 * Returns the feature that this color generator use.
	 * @return the feature set.
	 */
	public String getFeature();

	/**
	 * When this color generator is replaced by another one, calling this method ensures
	 * that it gets correctly unregistered and cleaned, should it be a model listener
	 * or have a heavy memory footprint. 
	 */
	public abstract void terminate();


}