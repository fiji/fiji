package fiji.plugin.trackmate.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class ExtractTrackStackAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_save.png"));
	
	
	/*
	 * CONSTRUCTOR
	 */

	public ExtractTrackStackAction() {
		this.icon = ICON;
	}

	/*
	 * METHODS
	 */
	
	@Override
	public void execute(TrackMate_ plugin) {
		logger.log("Capturing track stack.\n");
		
		TrackMateModel model = plugin.getModel();
		Set<Spot> selection = model.getSpotSelection();
		int nspots = selection.size();
		if (nspots != 2) {
			logger.error("Expected 2 spots in the selection, got "+nspots+".\nAborting.\n");
			return;
		}
		
		// Get start & end
		Spot tmp1, tmp2, start, end;
		Iterator<Spot> it = selection.iterator();
		tmp1 = it.next();
		tmp2 = it.next();
		if (tmp1.getFeature(Spot.POSITION_T) > tmp2.getFeature(Spot.POSITION_T)) {
			end = tmp1;
			start = tmp2;
		} else {
			end = tmp2;
			start = tmp1;
		}
		
		// Find path
		List<DefaultWeightedEdge> edges = model.dijkstraShortestPath(start, end);
		if (null == edges) {
			logger.error("The 2 spots are not connected.\nAborting\n");
			return;
		}
		
		// Build spot list
		List<Spot> path = new ArrayList<Spot>(edges.size());
		path.add(start);
		Spot previous = start;
		Spot current;
		for (DefaultWeightedEdge edge : edges) {
			current = model.getEdgeSource(edge);
			if (current != previous) {
				current = model.getEdgeTarget(edge); // We have to check both in case of bad oriented edges
			}
			path.add(current);
			previous = current;
		}
		
		// Get largest diameter

	}

	@Override
	public String getInfoText() {
		return "<html> " +
				"Generate a stack of images taken from the track " +
				"that joins two selected spots. " +
				"<p>" +
				"There must be exactly 2 spots selected for this action " +
				"to work, and they must belong to a track that connects " +
				"them." +
				"<p>" +
				"A stack of images will be generated from the spots that join " +
				"them, defining the image size with the largest spot encountered. " +
				"The central spot slice is taken in case of 3D data." +
				"</html>";
	}
	
	@Override
	public String toString() {
		return "Extract track stack";
	}

}
