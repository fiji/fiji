package fiji.plugin.nperry.visualization;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import fiji.plugin.nperry.Spot;

import ij3d.Content;

public class SpotGroupContent extends Content {
	
	private final static String DEFAULT_NAME = "SpotGroup";
	private Collection<Spot> spots;

	public SpotGroupContent(Collection<Spot> spots) {
		super(DEFAULT_NAME);
		this.spots = spots;
		buildContent();
	}
	
	
	private void buildContent() {
		// Gather all possible time-points
		Set<Integer> timepoints = new TreeSet<Integer>();
		for (Spot spot : spots)
			timepoints.add(spot.getFrame());
	}

}
