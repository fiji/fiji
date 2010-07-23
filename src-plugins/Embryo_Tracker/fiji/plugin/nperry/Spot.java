package fiji.plugin.nperry;

import java.util.HashMap;
import java.util.Map;

public class Spot implements Comparable<Spot> {
	
	/*
	 * FIELDS
	 */
	
	/** Overall score of this spot, as defined by some quality of segmentation, calculated by average
	 *  of all scores.*/
	private double score;
	/** Store the individual scores, indexed by the name of the scorer that generated the score. */
	private Map<String, Double> scores = new HashMap<String, Double>();
	/** Physical coordinates of this spot. Can have a time component. */
	private double[] coordinates; 
	
	/*
	 * CONSTRUCTORS
	 */
	
	public Spot(double[] coordinates, double score) {
		this.coordinates = coordinates;
		this.score = score;
	}
	
	public Spot(double[] coordinates) {
		this(coordinates, Double.NaN);
	}
	
	
	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Compare this Spot overall score to the spot overall score given in argument.
	 */
	@Override
	public int compareTo(Spot o) {
		return (int) (this.score - o.getAggregatedScore());
	}
	
	
	public double getAggregatedScore() {
		return score;
	}

	public void setAggregatedScore(double overallScore) {
		this.score = overallScore;
	}
	
	/**
	 * Return a reference to the coordinate array of this Spot.
	 */
	public double[] getCoordinates() {
		return this.coordinates;
	}
	
	/**
	 * Return the scores Map.
	 */
	public Map<String, Double> getScores() {
		return this.scores;
	}
	
	/**
	 * Add the score for a given scoring method.
	 * @param scoringMethodName the name of the scoring method used to compute the score
	 * @param score the score itself
	 */
	public void addScore(String scoringMethodName, double score) {
		this.scores.put(scoringMethodName, score);
	}
	
	
}
