package fiji.plugin.spottracker.features;

import java.util.Collection;

import fiji.plugin.spottracker.Spot;

public abstract class IndependentFeatureAnalyzer implements FeatureAnalyzer {

	@Override
	public void process(Collection<Spot> spots) {
		for (Spot spot : spots) {
			process(spot);
		}
	}
}
