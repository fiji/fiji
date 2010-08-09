
package fiji.plugin.nperry.scoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class AverageScoreAggregator implements FeatureScorer {

	private ArrayList<Feature> features;

	/* Constructor */
	public AverageScoreAggregator() {
		features = new ArrayList<Feature>();
	}
	
	@Override
	public void scoreFeatures(Collection<Spot> spots) {
		Collection<Spot> normalized = new ArrayList<Spot>(spots); // clone the collection
		for (Feature feature : features) {
			
			// Find min - max over the colletion of spots, for this feature
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			double currentScore;
			for (Spot spot : normalized) {
				currentScore = spot.getFeatures().get(feature);
				currentScore = feature.getScore(currentScore);
				if (min > currentScore) {
					min = currentScore;
				}
				if (max < currentScore) {
					max = currentScore;
				}
			}
			// Update scores for this scorer
			double scaledScore;
			for (Spot spot : normalized) {
				currentScore = spot.getFeatures().get(feature);
				currentScore = feature.getScore(currentScore);
				scaledScore = (currentScore - min) / (max - min);
				spot.getFeatures().put(feature, scaledScore);
			}
		}
		
		// Average score
		for (Spot spot : normalized) {
			double mean = 0.0;
			int counter = 0;
			Iterator<Double> it = spot.getFeatures().values().iterator();  // getFeatures() instead of getScores()
			while (it.hasNext()) {
				
				mean += it.next();
				counter++;
			}
			spot.setAggregatedScore(mean / counter);
		}
	}
	
	public void add(Feature feature) {
		features.add(feature);
	}
}
