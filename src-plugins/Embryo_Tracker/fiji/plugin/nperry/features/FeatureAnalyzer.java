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

	/**
	 * Return the enum Feature for this feature.
	 * @return
	 */
	public Feature getFeature();	
	
}
