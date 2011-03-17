package fiji.plugin.trackmate;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.EnumMap;

/**
 * Interface for objects that can store and retrieve feature values.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Sep 2010
 *
 */
public interface Spot {

	/** The position features. */
	public final static Feature[] POSITION_FEATURES = new Feature[] { Feature.POSITION_X, Feature.POSITION_Y, Feature.POSITION_Z };
	
	/** A comparator used to sort spots by ascending time feature. */ 
	public final static Comparator<Spot> frameComparator = new Comparator<Spot>() {
			@Override
			public int compare(Spot o1, Spot o2) {
				final float diff = o2.diffTo(o1, Feature.POSITION_T);
				if (diff == 0) 
					return 0;
				else if (diff < 0)
					return 1;
				else 
					return -1;
			}
		};
	
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
	 * By construction, this operation is anti-symmetric (A.diffTo(B) = - B.diffTo(A)).
	 */
	public Float diffTo(Spot s, Feature feature);
	
	/**
	 * Return the absolute normalized difference of the feature value of this spot 
	 * with the one of the given spot.
	 * <p>
	 * If <code>a</code> and <code>b</code> are the feature values, then the absolute
	 * normalized difference is dfefined as <code> Math.abs( a - b) / ( (a+b)/2 )</code>.
	 * <p>
	 * By construction, this operation is symmetric (A.normalizeDiffTo(B) = B.normalizeDiffTo(A)).
	 */
	public Float normalizeDiffTo(Spot s, Feature feature);
	
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
	 * Get the image used to display this spot.
	 */
	public BufferedImage getImage();
	
	/**
	 * Set the image used to display / represent this spot. 
	 */
	public void setImage(BufferedImage image);
}
