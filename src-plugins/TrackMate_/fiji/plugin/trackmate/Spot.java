package fiji.plugin.trackmate;

import java.util.EnumMap;

/**
 * Interface for objects that can store and retrieve feature values.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Sep 2010
 *
 */
public interface Spot {

	/** The position features. */
	public final static Feature[] POSITION_FEATURES = new Feature[] { Feature.POSITION_X, Feature.POSITION_Y, Feature.POSITION_Z };
	
	
	/**
	 * Adds a {@link Feature} and it's corresponding value to this object {@link Feature} list.
	 * @param feature The {@link Feature}.
	 * @param value The {@link Feature}'s associated value.
	 */
	public void putFeature(Feature feature, float value);
	
	/**
	 * Returns the value mapped to this {@link Feature}.
	 * @param feature The {@link Feature} to retrieve the stored value for.
	 * @return The value corresponding to this {@link Feature}, 
	 * <code>null</code> if it has not been set.
	 */
	public Float getFeature(Feature feature);
	
	/**
	 * Returns an {@link EnumMap} of {@link Feature}s for this Spot.
	 * @return A EnumMap with a {@link Feature} as a key, and the value of the {@link Feature} as the value. 
	 */
	public EnumMap<Feature, Float> getFeatures();
	
	/**
	 * Utility method that store the position features in the 3 elements float array.
	 * If the given array is <code>null</code>, a new array is created.
	 */
	public float[] getPosition(float[] position);
	
	/**
	 * Return the square distance from this spot to another, using the position features
	 */
	public Float squareDistanceTo(Spot s);
	
	/**
	 * Return the difference of the feature value of this spot with the one of the given spot.
	 */
	public Float diffTo(Spot s, Feature feature);
}
