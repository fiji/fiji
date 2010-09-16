package fiji.plugin.trackmate.features;

import java.util.Collection;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Feature;

public interface FeatureAnalyzer {
	
	/**
	 * Score a collection of spots.
	 * @param spots
	 */
	public void process(Collection<? extends Spot> spots);
	
	/**
	 * Score a single spot.
	 * @param spot
	 */
	public void process(Spot spot);

	/**
	 * Return the enum Feature for this feature.
	 * @return
	 */
	public Feature getFeature();	
	
}
