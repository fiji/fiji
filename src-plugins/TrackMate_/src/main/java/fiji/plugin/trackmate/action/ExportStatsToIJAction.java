package fiji.plugin.trackmate.action;

import ij.measure.ResultsTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.FeatureModel;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class ExportStatsToIJAction<T extends RealType<T> & NativeType<T>> extends AbstractTMAction<T> {

	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/calculator.png"));
	public static final String NAME = "Export statistics to tables";
	public static final String INFO_TEXT = "<html>" +
				"Compute and export all statistics to 3 ImageJ results table." +
				"Statistisc are separated in features computed for:" +
				"<ol>" +
				"	<li> spots in filtered tracks;" +
				"	<li> links between those spots;" +
				"	<li> filtered tracks." +
				"</ol>" +
				"For tracks and links, they are recalculated prior to exporting. Note " +
				"that spots and links that are not in a filtered tracks are not part" +
				"of this export." +
				"</html>";

	public ExportStatsToIJAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(final TrackMate_<T> plugin) {
		logger.log("Exporting statistics.\n");
		
		// Compute links features Links
		logger.log("  - Calculating statistics on links...");
		plugin.computeEdgeFeatures();
		logger.log(" Done.\n");
		
		// Compute track features
		logger.log("  - Calculating statistics on tracks...");
		plugin.computeTrackFeatures();

		// Model
		final TrackMateModel<T> model = plugin.getModel();
		final FeatureModel<T> fm = model.getFeatureModel();
		
		// Export spots
		logger.log("  - Exporting spot statistics...");
		// Sort them by track first
		ResultsTable spotTable = new ResultsTable();

		Set<Integer> trackIndices = model.getVisibleTrackIndices();
		for (Integer trackIndex : trackIndices) {
			
			Set<Spot> track = model.getTrackSpots(trackIndex);
			for (Spot spot : track) {
				spotTable.incrementCounter();
				spotTable.addLabel(spot.getName());
				spotTable.addValue("ID", spot.ID());
				spotTable.addValue("TRACK", trackIndex);
				Map<String, Double> features = spot.getFeatures();
				for (String feature : features.keySet()) {
					spotTable.addValue(feature, features.get(feature));
				}
			}
		}
		logger.log(" Done.\n");
		
		// Export edges
		logger.log("  - Exporting links statistics...");
		// Yield available edge feature
		
		
		// Sort them by track first
		ResultsTable edgeTable = new ResultsTable();

		for (Integer trackIndex : trackIndices) {
			
			Set<DefaultWeightedEdge> track = model.getTrackEdges(trackIndex);
			for (DefaultWeightedEdge edge : track) {
				spotTable.incrementCounter();
				spotTable.addValue("TRACK", trackIndex);
//				Map<String, Double> features = fm.getEdgeFeature(edge, featureName); // TODO TODO FIXME
			}
		}
		logger.log(" Done.\n");
		
		// Show tables
		spotTable.show("Spots in tracks");
		
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}

}
