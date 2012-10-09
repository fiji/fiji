package fiji.plugin.trackmate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.util.Util;

/**
 * Plain implementation of the {@link Spot} interface.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 16, 2010, 2012
 *
 */
public class SpotImp implements Spot {
	
	/*
	 * FIELDS
	 */
	
	private static String DEFAULT_IMAGE_STRING = "";
	
	public static AtomicInteger IDcounter = new AtomicInteger(0); 
	
	/** Store the individual features, and their values. Note that we use an actual
	 * non-concurrent hash map. Therefore, you cannot put all the features of a single
	 * spot in a multi-threaded fashion. */
	private HashMap<String, Double> features = new HashMap<String, Double>();
	/** A user-supplied name for this spot. */
	private String name;
	/** This spot ID */
	private int ID;
	/** This spot's image. */
	private String imageString = DEFAULT_IMAGE_STRING;

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
	public SpotImp(double[] coordinates, String name) {
		this.ID = IDcounter.getAndIncrement();
		for (int i = 0; i < 3; i++)
			putFeature(POSITION_FEATURES[i], coordinates[i]);
		if (null == name)
			this.name = "ID"+ID;
		else
			this.name = name;
	}
	
	public SpotImp(double[] coordinates) {
		this(coordinates, null);
	}
	
	public SpotImp(RealLocalizable localizable, String name) {
		this.ID = IDcounter.getAndIncrement();
		if (null == name)
			this.name = "ID"+ID;
		else
			this.name = name;
		setPosition(localizable);
	}

	public SpotImp(RealLocalizable localizable) {
		this(localizable, null);
	}

	
	/**
	 * Blank constructor meant to be used when loading a spot collection from a file. <b>Will</b> mess with
	 * the {@link #IDcounter} field, so this constructor should not be used for normal spot creation. 
	 * @param ID  the spot ID to set
	 */
	public SpotImp(int ID) {
		this.ID = ID;
		if (SpotImp.IDcounter.get() < ID) {
			SpotImp.IDcounter.set(ID+1);
		}
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public Spot clone() {
		SpotImp newSpot = new SpotImp(ID);
		// Deal with features
		Double val;
		for(String key : features.keySet()) {
			val = features.get(key);
			if (null != val)
				val = new Double(val);
			newSpot.putFeature(key, val);
		}
		// Deal with name
		newSpot.name = name;
		return newSpot;
	};
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
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
		s.append("Time: "+getFeature(Spot.POSITION_T)+'\n');

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
	
	public Map<String,Double> getFeatures() {
		return features;
	}
	
	@Override
	public final Double getFeature(final String feature) {
		return features.get(feature);
	}
	
	@Override
	public final void putFeature(final String feature, final double value) {
		features.put(feature, value);
	}

	@Override
	public Double diffTo(Spot s, String feature) {
		Double f1 = features.get(feature);
		Double f2 = s.getFeature(feature);
		if (f1 == null || f2 == null)
			return null;
		return f1 - f2;
	}
	
	@Override
	public Double normalizeDiffTo(Spot s, String feature) {
		final Double a = features.get(feature);
		final Double b = s.getFeature(feature);
		if (a == -b)
			return 0d;
		else
			return Math.abs(a-b)/((a+b)/2);
	}

	@Override
	public Double squareDistanceTo(Spot s) {
		Double sumSquared = 0d;
		Double thisVal, otherVal;
		
		for (String f : POSITION_FEATURES) {
			thisVal = features.get(f);
			otherVal = s.getFeature(f);
			sumSquared += ( otherVal - thisVal ) * ( otherVal - thisVal ); 
		}
		return sumSquared;
	}

	@Override
	public void setImageString(String str) {
		this.imageString = str;
	}

	@Override
	public String getImageString() {
		return imageString;
	}

	@Override
	public void move(float distance, int d) {
		String targetFeature = POSITION_FEATURES[d];
		double val = features.get(targetFeature) + distance;
		features.put(targetFeature, val);
	}

	@Override
	public void move(double distance, int d) {
		String targetFeature = POSITION_FEATURES[d];
		double val = features.get(targetFeature) + distance;
		features.put(targetFeature, val);
	}

	@Override
	public void move(RealLocalizable localizable) {
		for (int d = 0; d < localizable.numDimensions(); d++) {
			String targetFeature = POSITION_FEATURES[d];
			double val = features.get(targetFeature) + localizable.getDoublePosition(d);
			features.put(targetFeature, val);
		}
	}

	@Override
	public void move(float[] distance) {
		for (int d = 0; d < distance.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			double val = features.get(targetFeature) + distance[d];
			features.put(targetFeature, val);
		}
	}

	@Override
	public void move(double[] distance) {
		for (int d = 0; d < distance.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			double val = features.get(targetFeature) + distance[d];
			features.put(targetFeature, val);
		}
	}

	@Override
	public void setPosition(RealLocalizable localizable) {
		for (int d = 0; d < localizable.numDimensions(); d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, localizable.getDoublePosition(d));
		}
	}

	@Override
	public void setPosition(float[] position) {
		for (int d = 0; d < position.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, (double) position[d]);
		}
	}

	@Override
	public void setPosition(double[] position) {
		for (int d = 0; d < position.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, position[d]);
		}
	}

	@Override
	public void setPosition(float position, int d) {
		String targetFeature = POSITION_FEATURES[d];
		features.put(targetFeature, (double) position);
	}

	@Override
	public void setPosition(double position, int d) {
		String targetFeature = POSITION_FEATURES[d];
		features.put(targetFeature, position);
	}

	@Override
	public void fwd(int d) {
		String targetFeature = POSITION_FEATURES[d];
		features.put(targetFeature, features.get(targetFeature) + 1d );
	}

	@Override
	public void bck(int d) {
		String targetFeature = POSITION_FEATURES[d];
		features.put(targetFeature, features.get(targetFeature) - 1d );
	}

	@Override
	public void move(int distance, int d) {
		String targetFeature = POSITION_FEATURES[d];
		features.put(targetFeature, features.get(targetFeature) + distance );
	}

	@Override
	public void move(long distance, int d) {
		String targetFeature = POSITION_FEATURES[d];
		features.put(targetFeature, features.get(targetFeature) + distance );
	}

	@Override
	public void move(Localizable localizable) {
		for (int d = 0; d < localizable.numDimensions(); d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, features.get(targetFeature) + localizable.getDoublePosition(d) );
		}
	}

	@Override
	public void move(int[] distance) {
		for (int d = 0; d < distance.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, features.get(targetFeature) + distance[d] );
		}
	}

	@Override
	public void move(long[] distance) {
		for (int d = 0; d < distance.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, features.get(targetFeature) + distance[d] );
		}
	}

	@Override
	public void setPosition(Localizable localizable) {
		for (int d = 0; d < localizable.numDimensions(); d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, localizable.getDoublePosition(d));
		}
	}

	@Override
	public void setPosition(int[] position) {
		for (int d = 0; d < position.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, (double) position[d]);
		}
	}

	@Override
	public void setPosition(long[] position) {
		for (int d = 0; d < position.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			features.put(targetFeature, (double) position[d]);
		}
	}

	@Override
	public void setPosition(int position, int d) {
		String targetFeature = POSITION_FEATURES[d];
		features.put(targetFeature, (double) position);
	}

	@Override
	public void setPosition(long position, int d) {
		String targetFeature = POSITION_FEATURES[d];
		features.put(targetFeature, (double) position);
	}

	@Override
	public int numDimensions() {
		return 3; // is always 3D
	}

	@Override
	public void localize(float[] position) {
		for (int d = 0; d < position.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			position[d] = getFeature(targetFeature).floatValue();
		}
	}

	@Override
	public void localize(double[] position) {
		for (int d = 0; d < position.length; d++) {
			String targetFeature = POSITION_FEATURES[d];
			position[d] = getFeature(targetFeature);
		}
	}

	@Override
	public float getFloatPosition(int d) {
		String targetFeature = POSITION_FEATURES[d];
		return getFeature(targetFeature).floatValue();
	}

	@Override
	public double getDoublePosition(int d) {
		String targetFeature = POSITION_FEATURES[d];
		return getFeature(targetFeature);
	}
}
