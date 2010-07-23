package fiji.plugin.nperry.scoring;

import java.util.Collection;

import fiji.plugin.nperry.Spot;

public interface ScoreAggregator extends Collection<Scorer> {
	
	public void aggregate(Collection<Spot> spots);
	
//	public Collection<Spot> threshold(Collection<Spot> spots, double threshold);
	
}
