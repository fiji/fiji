package fiji.plugin.trackmate.features.spot;

import java.util.Collection;

import fiji.plugin.trackmate.Spot;

/**
 * Abstract class for spot feature analyzer for which features can be computed for one spot
 * independently of all other spots.
 * @author Jean-Yves Tinevez, 2010-2011
 *
 */
public abstract class IndependentSpotFeatureAnalyzer extends AbstractSpotFeatureAnalyzer {

	@Override
	public void process(Collection<Spot> spots) {
		for (Spot spot : spots)
			process(spot);
	}
	
	/**
	 * Compute all the features this analyzer can on the single spot given.
	 * @param spot  the spot to evaluate
	 */
	public abstract void process(Spot spot);
}
