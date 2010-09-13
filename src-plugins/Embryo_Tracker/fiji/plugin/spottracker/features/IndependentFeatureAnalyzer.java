package fiji.plugin.spottracker.features;

import java.util.Collection;

import fiji.plugin.spottracker.Featurable;

public abstract class IndependentFeatureAnalyzer implements FeatureAnalyzer {

	@Override
	public void process(Collection<? extends Featurable> spots) {
		for (Featurable spot : spots)
			process(spot);
	}
}
