package fiji.plugin.nperry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import mpicbg.imglib.algorithm.math.MathLib;

public class Spot {
	
	/*
	 * FIELDS
	 */
	
	/** Store the individual features, and their values. */
	private Map<Feature, Float> features = new HashMap<Feature, Float>();
	/** Physical coordinates of this spot. Can have a time component. */
	private float[] coordinates; 
	/** A user-supplied name for this spot. */
	private String name;
	/** The parents of this Spot, once Spots have been linked. */
	private ArrayList<Spot> parents;
	/** The children of this Spot, once Spots have been linked. */
	private ArrayList<Spot> children;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public Spot(float[] coordinates, String name) {
		this.coordinates = coordinates;
		this.name = name;
		this.parents = new ArrayList<Spot>();
		this.children = new ArrayList<Spot>();
	}
	
	public Spot(float[] coordinates) {
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
		if (null == coordinates)
			s.append("Position: <no coordinates>\n");
		else
			s.append("Position: "+MathLib.printCoordinates(coordinates)+"\n");
		if (null == features || features.size() < 1) 
			s.append("No features calculated\n");
		else {
			s.append("Feature list:\n");
			float val;
			for (Feature key : new TreeSet<Feature>(features.keySet())) {
				s.append("\t"+key.getName()+": ");
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
	 * Returns a map of {@link Feature}s for this Spot.
	 * @return A map with a {@link Feature} as a key, and the value of the {@link Feature} as the value. 
	 */
	public Map<Feature, Float> getFeatures() {
		return this.features;
	}
	
	/**
	 * Returns the value mapped to this {@link Feature}.
	 * @param feature The {@link Feature} to retrieve the stored value for.
	 * @return The value corresponding to this {@link Feature}. 
	 */
	public float getFeature(Feature feature) {
		return this.features.get(feature);
	}
	
	/**
	 * Adds a {@link Feature} and it's corresponding value to this Spot's {@link Feature} list.
	 * @param feature The {@link Feature}.
	 * @param value The {@link Feature}'s associated value.
	 */
	public void addFeature(Feature feature, float value) {
		this.features.put(feature, value);
	}
	
	
	/*
	 * TRACKING RELATED METHODS
	 */
	
	/**
	 * Adds a reference to the parent Spot to this Spot's list of parents.
	 * @param parent
	 */
	public void addParent(Spot parent) {
		this.parents.add(parent);
	}
	
	/**
	 * Adds a reference to the child Spot to this Spot's list of children.
	 * @param child
	 */
	public void addChild(Spot child) {
		this.children.add(child);
	}
	
	/**
	 * Returns the ArrayList holding the list of this Spot's parents.
	 * @return An ArrayList which contains elements of type Spot. Each Spot in this
	 * list is a child of this Spot. Returns null if this Spot has no parents (a root).
	 */
	public ArrayList<Spot> getParents() {
		if (parents.size() == 0) return null;
		return this.parents;
	}
	
	/** 
	 * Returns the ArrayList holding the list of this Spot's children.
	 * @return An ArrayList which contains elements of type Spot. Each Spot in this
	 * list is a child of this Spot. Returns null if this Spot has no children (a leaf).
	 */
	public ArrayList<Spot> getChildren() {
		if (children.size() == 0) return null; 
		return this.children;
	}
}
