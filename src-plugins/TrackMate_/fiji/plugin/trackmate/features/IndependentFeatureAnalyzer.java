package fiji.plugin.trackmate.features;

import java.util.Collection;

import fiji.plugin.trackmate.Spot;

public abstract class IndependentFeatureAnalyzer implements FeatureAnalyzer {

	@Override
	public void process(Collection<? extends Spot> spots) {
		for (Spot spot : spots)
			process(spot);
	}
}
