package fiji.plugin.trackmate;

import java.util.Comparator;
import java.util.EnumMap;

import com.mxgraph.util.mxBase64;

/**
 * Interface for objects that can store and retrieve feature values.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Sep 2010
 *
 */
public interface Spot {

	/** The position features. */
	public final static SpotFeature[] POSITION_FEATURES = new SpotFeature[] { SpotFeature.POSITION_X, SpotFeature.POSITION_Y, SpotFeature.POSITION_Z };
	
	/** A comparator used to sort spots by ascending time feature. */ 
	public final static Comparator<Spot> frameComparator = new Comparator<Spot>() {
			@Override
			public int compare(Spot o1, Spot o2) {
				final float diff = o2.diffTo(o1, SpotFeature.POSITION_T);
				if (diff == 0) 
					return 0;
				else if (diff < 0)
					return 1;
				else 
					return -1;
			}
		};
	
	/**
	 * Adds a {@link SpotFeature} and it's corresponding value to this object {@link SpotFeature} list.
	 * @param feature The {@link SpotFeature}.
	 * @param value The {@link SpotFeature}'s associated value.
	 */
	public void putFeature(SpotFeature feature, float value);
	
	/**
	 * Returns the value mapped to this {@link SpotFeature}.
	 * @param feature The {@link SpotFeature} to retrieve the stored value for.
	 * @return The value corresponding to this {@link SpotFeature}, 
	 * <code>null</code> if it has not been set.
	 */
	public Float getFeature(SpotFeature feature);
	
	/**
	 * Returns an {@link EnumMap} of {@link SpotFeature}s for this Spot.
	 * @return A EnumMap with a {@link SpotFeature} as a key, and the value of the {@link SpotFeature} as the value. 
	 */
	public EnumMap<SpotFeature, Float> getFeatures();
	
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
	 * By construction, this operation is anti-symmetric (A.diffTo(B) = - B.diffTo(A)).
	 */
	public Float diffTo(Spot s, SpotFeature feature);
	
	/**
	 * Return the absolute normalized difference of the feature value of this spot 
	 * with the one of the given spot.
	 * <p>
	 * If <code>a</code> and <code>b</code> are the feature values, then the absolute
	 * normalized difference is dfefined as <code> Math.abs( a - b) / ( (a+b)/2 )</code>.
	 * <p>
	 * By construction, this operation is symmetric (A.normalizeDiffTo(B) = B.normalizeDiffTo(A)).
	 */
	public Float normalizeDiffTo(Spot s, SpotFeature feature);
	
	/**
	 * Return the ID number of the spot, use for saving. 
	 * This number should be unique.
	 */
	public int ID();

	/**
	 * Return a new copy of this spot object. The new spot will have the same feature values that of this spot, 
	 * and the same {@link #ID()}. Because the {@link #ID()} can be used for indexing purpose in
	 * other classes of this package, keeping a spot and its clone in the same collection can be hazardous. 
	 */
	public Spot clone();
	
	/**
	 * Returns the name of this Spot.
	 * @return The String name corresponding to this Spot.
	 */
	public String getName();
	
	/**
	 * Set the name of this Spot.
	 * @param name
	 */
	public void setName(String name);
	
	/**
	 * Set the image, encoded as a Base64 string, that is used to represent this spot. 
	 * @see mxBase64 
	 */
	public void setImageString(String str);
	
	/**
	 * Get the image, encoded as a Base64 string, that is used to represent this spot. 
	 * @see mxBase64 
	 */
	public String getImageString();
	
}
