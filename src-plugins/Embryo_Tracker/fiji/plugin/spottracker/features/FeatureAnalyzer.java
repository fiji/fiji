package fiji.plugin.spottracker.features;

import java.util.Collection;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Featurable;

public interface FeatureAnalyzer {
	
	/**
	 * Score a collection of spots.
	 * @param spots
	 */
	public void process(Collection<? extends Featurable> spots);
	
	/**
	 * Score a single spot.
	 * @param spot
	 */
	public void process(Featurable spot);

	/**
	 * Return the enum Feature for this feature.
	 * @return
	 */
	public Feature getFeature();	
	
}
