package fiji.plugin.nperry.scoring;

import java.util.Collection;

import fiji.plugin.nperry.Spot;

public interface Scorer {
	
	/**
	 * Score a collection of spots.
	 * @param spots
	 */
	public void score(Collection<Spot> spots);
	
	/**
	 * Score a single spot.
	 * @param spot
	 */
	public void score(Spot spot);

	/**
	 * Return the (human-readable) name of this scorer. 
	 * @return
	 */
	public String getName();
	
	/**
	 * Return true if this scorer is normalized, that is, if the scores it returns are between
	 * 0 and 1, 1 being the best score.
	 */
	public boolean isNormalized();
	
}
