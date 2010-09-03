package fiji.plugin.nperry.visualization;

import java.util.Collection;

import javax.media.j3d.Switch;

import fiji.plugin.nperry.Spot;

import ij3d.ContentInstant;
import ij3d.ContentNode;

public class SpotGroupContentInstant extends Switch {

	private Collection<Spot> spots;


	public SpotGroupContentInstant(Collection<Spot> spots) {
		this.spots = spots;
	}

	
	public void add(ContentNode node) {
		
	}
	
}
