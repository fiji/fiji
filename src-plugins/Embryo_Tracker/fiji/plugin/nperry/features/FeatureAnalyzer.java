package fiji.plugin.nperry.features;

import java.util.Collection;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public interface FeatureAnalyzer {
	
	/**
	 * Score a collection of spots.
	 * @param spots
	 */
	public void process(Collection<Spot> spots);
	
	/**
	 * Score a single spot.
	 * @param spot
	 */
	public void process(Spot spot);

	public Feature getFeature();
	
	/**
	 * Return true if this scorer is normalized, that is, if the scores it returns are between
	 * 0 and 1, 1 being the best score.
	 */
	public boolean isNormalized();
	
}
