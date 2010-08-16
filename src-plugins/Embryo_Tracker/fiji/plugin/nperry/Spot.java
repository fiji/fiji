package fiji.plugin.nperry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Spot {
	
	/*
	 * FIELDS
	 */
	
	/** Store the individual features, and their values. */
	private Map<Feature, Double> features = new HashMap<Feature, Double>();
	/** Physical coordinates of this spot. Can have a time component. */
	private double[] coordinates; 
	/** A user-supplied name for this spot. */
	private String name;
	/** The parents of this Spot, once Spots have been linked. */
	private ArrayList<Spot> parents;
	/** The children of this Spot, once Spots have been linked. */
	private ArrayList<Spot> children;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public Spot(double[] coordinates, String name) {
		this.coordinates = coordinates;
		this.name = name;
		this.parents = new ArrayList<Spot>();
		this.children = new ArrayList<Spot>();
	}
	
	public Spot(double[] coordinates) {
		this(coordinates, null);
		this.parents = new ArrayList<Spot>();
		this.children = new ArrayList<Spot>();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Return a reference to the coordinate array of this Spot.
	 */
	public double[] getCoordinates() {
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
	 * Returns a map of {@link Feature}s for this Spot.
	 * @return A map with a {@link Feature} as a key, and the value of the {@link Feature} as the value. 
	 */
	public Map<Feature, Double> getFeatures() {
		return this.features;
	}
	
	/**
	 * Returns the {@link Double} value mapped to this {@link Feature}.
	 * @param feature The {@link Feature} to retrieve the stored value for.
	 * @return The {@link Double} value corresponding to this {@link Feature}. 
	 */
	public double getFeature(Feature feature) {
		return this.features.get(feature).doubleValue();
	}
	
	/**
	 * Adds a {@link Feature} and it's corresponding value to this Spot's {@link Feature} list.
	 * @param feature The {@link Feature}.
	 * @param value The {@link Feature}'s associated value.
	 */
	public void addFeature(Feature feature, double value) {
		this.features.put(feature, value);
	}
}
