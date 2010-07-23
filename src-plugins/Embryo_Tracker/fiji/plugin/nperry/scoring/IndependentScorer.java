package fiji.plugin.nperry.scoring;

import java.util.Collection;

import fiji.plugin.nperry.Spot;

public abstract class IndependentScorer implements Scorer {

	@Override
	public void score(Collection<Spot> spots) {
		for (Spot spot : spots) {
			score(spot);
		}
	}

	
}
