package fiji.plugin.trackmate.features.spot;

import java.util.Collection;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;

public interface SpotFeatureAnalyzer {
	
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
	public SpotFeature getFeature();	
	
}
