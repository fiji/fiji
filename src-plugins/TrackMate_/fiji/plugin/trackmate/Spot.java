package fiji.plugin.trackmate;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import com.mxgraph.util.mxBase64;

import fiji.plugin.trackmate.util.TMUtils;

/**
 * Interface for objects that can store and retrieve feature values.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Sep 2010 - 2011
 *
 */
public interface Spot {

	/*
	 * PUBLIC UTILITY CONSTANTS
	 */

	/** A comparator used to sort spots by ascending time feature. */ 
	public final static Comparator<Spot> frameComparator = new Comparator<Spot>() {
		@Override
		public int compare(Spot o1, Spot o2) {
			final float diff = o2.diffTo(o1, POSITION_T);
			if (diff == 0) 
				return 0;
			else if (diff < 0)
				return 1;
			else 
				return -1;
		}
	};

	/** The name of the spot quality feature. */
	public static final String QUALITY = TMUtils.QUALITY;
	/** The name of the radius spot feature. */
	public static final String RADIUS = TMUtils.RADIUS;
	/** The name of the spot X position feature. */
	public static final String POSITION_X = TMUtils.POSITION_X;
	/** The name of the spot Y position feature. */
	public static final String POSITION_Y = TMUtils.POSITION_Y;
	/** The name of the spot Z position feature. */
	public static final String POSITION_Z = TMUtils.POSITION_Z;
	/** The name of the spot T position feature. */
	public static final String POSITION_T = TMUtils.POSITION_T;

	/** The position features. */
	public final static String[] POSITION_FEATURES = TMUtils.POSITION_FEATURES;
	/** The 6 privileged spot features that must be set by a spot segmenter. */
	public final static Collection<String> FEATURES = TMUtils.FEATURES;
	/** The 6 privileged spot feature names. */
	public final static Map<String, String> FEATURE_NAMES = TMUtils.FEATURE_NAMES;
	/** The 6 privileged spot feature short names. */
	public final static Map<String, String> FEATURE_SHORT_NAMES = TMUtils.FEATURE_SHORT_NAMES;
	/** The 6 privileged spot feature dimensions. */
	public final static Map<String, Dimension> FEATURE_DIMENSIONS = TMUtils.FEATURE_DIMENSIONS;
	
	
	

	/*
	 * INTERFACE
	 */
	
	
	/**
	 * Specify the numerical value of a feature for this spot.
	 */
	public void putFeature(String feature, float value);

	/**
	 * Returns the value mapped to the given spot feature.
	 * @param feature The spot feature name to retrieve the stored value for.
	 * @return The value corresponding to this {@link SpotFeature}, 
	 * <code>null</code> if it has not been set.
	 */
	public Float getFeature(String feature);

	/**
	 * Returns the Map of spot features  for this Spot.
	 * @return  a Map with spot feature names as keys. 
	 */
	public Map<String, Float> getFeatures();

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
	public Float diffTo(Spot s, String feature);

	/**
	 * Return the absolute normalized difference of the feature value of this spot 
	 * with the one of the given spot.
	 * <p>
	 * If <code>a</code> and <code>b</code> are the feature values, then the absolute
	 * normalized difference is defined as <code> Math.abs( a - b) / ( (a+b)/2 )</code>.
	 * <p>
	 * By construction, this operation is symmetric (A.normalizeDiffTo(B) = B.normalizeDiffTo(A)).
	 */
	public Float normalizeDiffTo(Spot s, String feature);

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
