package fiji.plugin.nperry;

import java.util.ArrayList;
import java.util.EnumMap;

import mpicbg.imglib.algorithm.math.MathLib;

public class Spot {
	
	/*
	 * FIELDS
	 */
	
	/** Store the individual features, and their values. */
	private EnumMap<Feature, Float> features = new EnumMap<Feature, Float>(Feature.class);
	/** Physical coordinates of this spot. Can have a time component. */
	private float[] coordinates; 
	/** A user-supplied name for this spot. */
	private String name;
	/** The frame to which this Spot belongs. (Same as a t coordinate) */
	private int frame;
	/** A reference to the previous Spots belonging to the same track. */
	private ArrayList<Spot> prev;
	/** A reference to the subsequent Spots belonging to the same track. */
	private ArrayList<Spot> next;

	
	/*
	 * CONSTRUCTORS
	 */
	
	public Spot(float[] coordinates, String name) {
		this.coordinates = coordinates;
		this.name = name;
		this.prev = new ArrayList<Spot>();
		this.next = new ArrayList<Spot>();
	}
	
	public Spot(float[] coordinates) {
		this(coordinates, null);
		this.prev = new ArrayList<Spot>();
		this.next = new ArrayList<Spot>();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Return a reference to the coordinate array of this Spot.
	 */
	public float[] getCoordinates() {
		return this.coordinates;
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
		if (null == name) 
			s.append("Spot: <no name>\n");
		else
			s.append("Spot: "+name+"\n");
		s.append("Frame: "+frame+'\n');
		if (null == coordinates)
			s.append("Position: <no coordinates>\n");
		else
			s.append("Position: "+MathLib.printCoordinates(coordinates)+"\n");
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
		this.frame = frame;
	}
	
	public int getFrame() {
		return this.frame;
	}
	
	/*
	 * FEATURE RELATED METHODS
	 */
	
	
	/**
	 * Returns an {@link EnumMap} of {@link Feature}s for this Spot.
	 * @return A EnumMap with a {@link Feature} as a key, and the value of the {@link Feature} as the value. 
	 */
	public EnumMap<Feature, Float> getFeatures() {
		return features;
	}
	
	/**
	 * Returns the value mapped to this {@link Feature}.
	 * @param feature The {@link Feature} to retrieve the stored value for.
	 * @return The value corresponding to this {@link Feature}. 
	 */
	public final Float getFeature(final Feature feature) {
		return features.get(feature);
	}
	
	/**
	 * Adds a {@link Feature} and it's corresponding value to this Spot's {@link Feature} list.
	 * @param feature The {@link Feature}.
	 * @param value The {@link Feature}'s associated value.
	 */
	public final void putFeature(final Feature feature, final float value) {
		features.put(feature, value);
	}

	public void addPrev(Spot prev) {
		this.prev.add(prev);
	}

	public ArrayList<Spot> getPrev() {
		return prev;
	}

	public void addNext(Spot next) {
		this.next.add(next);
	}

	public ArrayList<Spot> getNext() {
		return next;
	}
}
