package fiji.plugin.spottracker;

import java.util.EnumMap;

import mpicbg.imglib.algorithm.math.MathLib;

public class Spot implements Featurable {
	
	/*
	 * FIELDS
	 */
	
	
	/** Store the individual features, and their values. */
	private EnumMap<Feature, Float> features = new EnumMap<Feature, Float>(Feature.class);
	/** Physical coordinates of this spot.  */
	private float[] coordinates; 
	/** A user-supplied name for this spot. */
	private String name;

	/*
	 * CONSTRUCTORS
	 */
	
	public Spot(float[] coordinates, String name) {
		this.coordinates = coordinates;
		for (int i = 0; i < coordinates.length; i++)
			putFeature(POSITION_FEATURES[i], coordinates[i]);
		this.name = name;
	}
	
	public Spot(float[] coordinates) {
		this(coordinates, null);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Convenience method that returns the X, Y and optionally Z feature in a float array.
	 */
	public void getCoordinates(float[] coords) {
		for (int i = 0; i < coords.length; i++)
			coords[i] = getFeature(POSITION_FEATURES[i]);
	}
	
	/**
	 * Returns the name of this Spot.
	 * @return The String name corresponding to this Spot.
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Set the name of this Spot.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	/**
	 * Return a string representation of this spot, with calculated features.
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		
		// Name
		if (null == name) 
			s.append("Spot: <no name>\n");
		else
			s.append("Spot: "+name+"\n");
		
		// Frame
		s.append("Frame: "+getFeature(Feature.POSITION_T)+'\n');

		// Coordinates
		if (null == coordinates)
			s.append("Position: <no coordinates>\n");
		else 
			s.append("Position: "+MathLib.printCoordinates(getPosition(null))+"\n");
		
		// Feature list
		if (null == features || features.size() < 1) 
			s.append("No features calculated\n");
		else {
			s.append("Feature list:\n");
			float val;
			for (Feature key : features.keySet()) {
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
	
	public void setFrame(int frame) {
		putFeature(Feature.POSITION_T, frame);
	}
	
	@Override
	public float[] getPosition(float[] position) {
		if (null == position) {
			int ndim = 0;
			for (int i = 0; i < POSITION_FEATURES.length; i++)
				if (features.get(POSITION_FEATURES[i]) != null)
					ndim++;
			position = new float[ndim];
		}
		Float val;
		int index = 0;
		for (int i = 0; i < position.length; i++) {
			val = features.get(POSITION_FEATURES[i]);
			if (null == val)
				continue;
			position[index] = getFeature(POSITION_FEATURES[i]);
			index++;
		}
		return position;
	}
	
	/*
	 * FEATURE RELATED METHODS
	 */
	
	
	@Override
	public EnumMap<Feature, Float> getFeatures() {
		return features;
	}
	
	@Override
	public final Float getFeature(final Feature feature) {
		return features.get(feature);
	}
	
	@Override
	public final void putFeature(final Feature feature, final float value) {
		features.put(feature, value);
	}

}
