package fiji.plugin.nperry.scoring;

import java.util.Collection;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public interface FeatureScorer {
	
	public void scoreFeatures(Collection<Spot> spots);
	
	public void add(Feature feature);
	
//	public Collection<Spot> threshold(Collection<Spot> spots, double threshold);
	
}
