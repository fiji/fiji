package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.util.Util;
import fiji.plugin.trackmate.util.AlphanumComparator;

/**
 * Plain implementation of the {@link Spot} interface.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 16, 2010, 2012
 *
 */
public class Spot {

	/*
	 * FIELDS
	 */

	public static AtomicInteger IDcounter = new AtomicInteger(0); 

	/** Store the individual features, and their values. */
	private final ConcurrentHashMap<String, Double> features = new ConcurrentHashMap<String, Double>();
	/** A user-supplied name for this spot. */
	private String name;
	/** This spot ID */
	private int ID;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Instantiate a Spot. 
	 * <p>
	 * The given coordinate double array <b>must</b> have 3 elements. If the 3rd one is not
	 * used (2D case), it can be set to a constant value 0. This constructor ensures that
	 * none of the {@link Spot#POSITION_FEATURES} will be <code>null</code>, and ensure relevance
	 * when calculating distances and so on.
	 */
	public Spot(double[] coordinates, String name) {
		this.ID = IDcounter.getAndIncrement();
		for (int i = 0; i < 3; i++)
			putFeature(POSITION_FEATURES[i], coordinates[i]);
		if (null == name)
			this.name = "ID"+ID;
		else
			this.name = name;
	}

	public Spot(double[] coordinates) {
		this(coordinates, null);
	}

	/**
	 * Blank constructor meant to be used when loading a spot collection from a file. <b>Will</b> mess with
	 * the {@link #IDcounter} field, so this constructor should not be used for normal spot creation. 
	 * @param ID  the spot ID to set
	 */
	public Spot(int ID) {
		this.ID = ID;
		if (IDcounter.get() < ID) {
			IDcounter.set(ID+1);
		}
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
     * @return the name for this Spot.
     */
	public String getName() {
		return this.name;
	}

	/**
     * Set the name of this Spot.
     */
	public void setName(String name) {
		this.name = name;
	}

	public int ID() {
		return ID;
	}

	@Override
	public String toString() {
		String str;
		if (null == name || name.equals(""))
			str = "ID"+ID;
		else 
			str = name;
		return str;
	}

	/**
	 * Return a string representation of this spot, with calculated features.
	 */
	public String echo() {
		StringBuilder s = new StringBuilder();

		// Name
		if (null == name) 
			s.append("Spot: <no name>\n");
		else
			s.append("Spot: "+name+"\n");

		// Frame
		s.append("Time: "+getFeature(POSITION_T)+'\n');

		// Coordinates
		double[] coordinates = new double[3];
		//		localize(coordinates);
		s.append("Position: "+Util.printCoordinates(coordinates)+"\n");

		// Feature list
		if (null == features || features.size() < 1) 
			s.append("No features calculated\n");
		else {
			s.append("Feature list:\n");
			double val;
			for (String key : features.keySet()) {
				s.append("\t"+key.toString()+": ");
				val = features.get(key);
				if (val >= 1e4)
					s.append(String.format("%.1g", val));
				else
					s.append(String.format("%.1f", val));
				s.append('\n');
			}
		}
		return s.toString();
	}

	/*
	 * FEATURE RELATED METHODS
	 */

	/**
     * @return and exposes the storage Map of features for this spot. 
     */
	public Map<String,Double> getFeatures() {
		return features;
	}

	/**
	 * @return The value corresponding to the specified spot feature. 
     * @param feature The feature string to retrieve the stored value for.
     * <code>null</code> if it has not been set.
     */
	public final Double getFeature(final String feature) {
		return features.get(feature);
	}

	/**
     * Store the specified feature value for this spot.
     */
	public final void putFeature(final String feature, final double value) {
		features.put(feature, value);
	}

	 /**
     * @return the difference of the feature value of this spot with the one of the given spot.
     * By construction, this operation is anti-symmetric (A.diffTo(B) = - B.diffTo(A)).
     */
	public double diffTo(Spot s, String feature) {
		double f1 = features.get(feature).doubleValue();
		double f2 = s.getFeature(feature).doubleValue();
		return f1 - f2;
	}

	/**
     * @return the absolute normalized difference of the feature value of this spot 
     * with the one of the given spot.
     * <p>
     * If <code>a</code> and <code>b</code> are the feature values, then the absolute
     * normalized difference is defined as <code> Math.abs( a - b) / ( (a+b)/2 )</code>.
     * <p>
     * By construction, this operation is symmetric (A.normalizeDiffTo(B) = B.normalizeDiffTo(A)).
     */
	public double normalizeDiffTo(Spot s, String feature) {
		final double a = features.get(feature).doubleValue();
		final double b = s.getFeature(feature).doubleValue();
		if (a == -b)
			return 0d;
		else
			return Math.abs(a-b)/((a+b)/2);
	}

	/**
     * @return the square distance from this spot to another, using the x,y,z position features.
     */
	public double squareDistanceTo(Spot s) {
		double sumSquared = 0d;
		double thisVal, otherVal;

		for (String f : POSITION_FEATURES) {
			thisVal = features.get(f).doubleValue();
			otherVal = s.getFeature(f).doubleValue();
			sumSquared += ( otherVal - thisVal ) * ( otherVal - thisVal ); 
		}
		return sumSquared;
	}

	/*
	 * PUBLIC UTILITY CONSTANTS
	 */

	/** A comparator used to sort spots by ascending time feature. */ 
	public final static Comparator<Spot> timeComparator = new Comparator<Spot>() {
		@Override
		public int compare(Spot o1, Spot o2) {
			final double diff = o2.diffTo(o1, POSITION_T);
			if (diff == 0) 
				return 0;
			else if (diff < 0)
				return 1;
			else 
				return -1;
		}

	};

	/** A comparator used to sort spots by ascending frame. */ 
	public final static Comparator<Spot> frameComparator = new Comparator<Spot>() {
		@Override
		public int compare(Spot o1, Spot o2) {
			final double diff = o2.diffTo(o1, FRAME);
			if (diff == 0) 
				return 0;
			else if (diff < 0)
				return 1;
			else 
				return -1;
		}
	};

	/** A comparator used to sort spots by name. The comparison uses numerical natural sorting,
	 * So that "Spot_4" comes before "Spot_122". */ 
	public final static Comparator<Spot> nameComparator = new Comparator<Spot>() {
		private final AlphanumComparator comparator = new AlphanumComparator();
		@Override
		public int compare(Spot o1, Spot o2) {
			return comparator.compare(o1.getName(), o2.getName());
		}
	};
	
	
	/*
	 * STATIC KEYS
	 */
	

	/** The name of the spot quality feature. */
	public static final String QUALITY = "QUALITY";
	/** The name of the radius spot feature. */
	public static final String RADIUS = "RADIUS";
	/** The name of the spot X position feature. */
	public static final String POSITION_X = "POSITION_X";
	/** The name of the spot Y position feature. */
	public static final String POSITION_Y = "POSITION_Y";
	/** The name of the spot Z position feature. */
	public static final String POSITION_Z = "POSITION_Z";
	/** The name of the spot T position feature. */
	public static final String POSITION_T = "POSITION_T";
	/** The name of the frame feature. */
	public static final String FRAME = "FRAME";

	/** The position features. */
	public final static String[] POSITION_FEATURES = new String[] { POSITION_X, POSITION_Y, POSITION_Z };
	/** The 6 privileged spot features that must be set by a spot detector. */
	public final static Collection<String> FEATURES = new ArrayList<String>(6);
	/** The 6 privileged spot feature names. */
	public final static Map<String, String> FEATURE_NAMES = new HashMap<String, String>(6);
	/** The 6 privileged spot feature short names. */
	public final static Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(6);
	/** The 6 privileged spot feature dimensions. */
	public final static Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(6);

	static {
		FEATURES.add(QUALITY);
		FEATURES.add(POSITION_X);
		FEATURES.add(POSITION_Y);
		FEATURES.add(POSITION_Z);
		FEATURES.add(POSITION_T);
		FEATURES.add(FRAME);
		FEATURES.add(RADIUS);

		FEATURE_NAMES.put(POSITION_X, "X");
		FEATURE_NAMES.put(POSITION_Y, "Y");
		FEATURE_NAMES.put(POSITION_Z, "Z");
		FEATURE_NAMES.put(POSITION_T, "T");
		FEATURE_NAMES.put(FRAME, "Frame");
		FEATURE_NAMES.put(RADIUS, "Radius");
		FEATURE_NAMES.put(QUALITY, "Quality");

		FEATURE_SHORT_NAMES.put(POSITION_X, "X");
		FEATURE_SHORT_NAMES.put(POSITION_Y, "Y");
		FEATURE_SHORT_NAMES.put(POSITION_Z, "Z");
		FEATURE_SHORT_NAMES.put(POSITION_T, "T");
		FEATURE_SHORT_NAMES.put(FRAME, "Frame");
		FEATURE_SHORT_NAMES.put(RADIUS, "R");
		FEATURE_SHORT_NAMES.put(QUALITY, "Quality");

		FEATURE_DIMENSIONS.put(POSITION_X, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(POSITION_Y, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(POSITION_Z, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(POSITION_T, Dimension.TIME);
		FEATURE_DIMENSIONS.put(FRAME, Dimension.NONE);
		FEATURE_DIMENSIONS.put(RADIUS, Dimension.LENGTH);
		FEATURE_DIMENSIONS.put(QUALITY, Dimension.QUALITY);
	}



}
