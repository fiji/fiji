package fiji.plugin.nperry.features;

import java.util.Collection;

import fiji.plugin.nperry.Spot;

public abstract class IndependentFeatureAnalyzer implements FeatureAnalyzer {

	@Override
	public void process(Collection<Spot> spots) {
		for (Spot spot : spots) {
			process(spot);
		}
	}
}
