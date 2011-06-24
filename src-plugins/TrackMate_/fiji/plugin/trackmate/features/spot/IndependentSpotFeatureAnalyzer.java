package fiji.plugin.trackmate.features.spot;

import java.util.Collection;

import fiji.plugin.trackmate.Spot;

public abstract class IndependentSpotFeatureAnalyzer implements SpotFeatureAnalyzer {

	@Override
	public void process(Collection<? extends Spot> spots) {
		for (Spot spot : spots)
			process(spot);
	}
}
