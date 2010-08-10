package fiji.plugin.nperry;

import java.util.HashMap;
import java.util.Map;

public class Spot implements Comparable<Spot> {
	
	/*
	 * FIELDS
	 */
	
	/** Store the individual features, and their values. */
	private Map<Feature, Double> features = new HashMap<Feature, Double>();
	/** Physical coordinates of this spot. Can have a time component. */
	private double[] coordinates; 
	
	private String name;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public Spot(double[] coordinates, String name) {
		this.coordinates = coordinates;
		this.name = name;
	}
	
	public Spot(double[] coordinates) {
		this(coordinates, null);
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
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Return the scores Map.
	 */
	//public Map<String, Double> getScores() {
	//	return this.scores;
	//}
	
	public Map<Feature, Double> getFeatures() {
		return this.features;
	}
	
	/**
	 * Add the score for a given scoring method.
	 * @param scoringMethodName the name of the scoring method used to compute the score
	 * @param score the score itself
	 */
	//public void addScore(String scoringMethodName, double score) {
	//	this.scores.put(scoringMethodName, score);
	//}
	
	public void addFeature(Feature feature, double score) {
		this.features.put(feature, score);
	}

	public int compareTo(double[] o) {
		for (int i = 0; i < coordinates.length; i++) {
			if (Double.compare(coordinates[i], o[i]) != 0) return -1;
		}
		return 0;
	}

	@Override
	public int compareTo(Spot o) {
		return this.features.get(Feature.LOG_VALUE).compareTo(o.getFeatures().get(Feature.LOG_VALUE));
	}
	
	
}
