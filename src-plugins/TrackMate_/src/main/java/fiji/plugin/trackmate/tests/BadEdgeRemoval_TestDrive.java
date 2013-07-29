package fiji.plugin.trackmate.tests;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import org.jdom2.JDOMException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class BadEdgeRemoval_TestDrive {

	public static void main(final String[] args) throws JDOMException, IOException, InterruptedException {

		ij.ImageJ.main(args);

		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		final TmXmlReader reader = new TmXmlReader(file);

		final Model model = reader.getModel();
		final Settings settings = new Settings();
		reader.readSettings(settings, null, null, new SpotAnalyzerProvider(model), new EdgeAnalyzerProvider(model), new TrackAnalyzerProvider(model));
		final ImagePlus imp = settings.imp;

		new ModelFeatureUpdater(model, settings);
		final SelectionModel selectionModel = new SelectionModel(model);
		final HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp);
		displayer.render();
		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE);
		displayer.setDisplaySettings(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES, true);
		displayer.setDisplaySettings(TrackMateModelView.KEY_SPOTS_VISIBLE, true);

		final TrackScheme trackScheme = new TrackScheme(model, selectionModel);
		trackScheme.render();

		// Remove edge
		Thread.sleep(2000);
		final SpotCollection sc = model.getSpots();
		final Spot spot1 = sc.search(2702);
		final Spot spot2 = sc.search(75);

		System.out.println(model.getTrackModel().echo());// DEBUG
		System.out.println("Edge found: " + model.getTrackModel().containsEdge(spot1, spot2));
		System.out.println("Removing edge: " + model.removeEdge(spot1, spot2));
		System.out.println("Edge found: " + model.getTrackModel().containsEdge(spot1, spot2));
		System.out.println(model.getTrackModel().echo());// DEBUG

		DefaultWeightedEdge edge;
		System.out.println("Re-creating the edge: " + (edge = model.addEdge(spot1, spot2, -1)));
		System.out.println("Edge found: " + model.getTrackModel().containsEdge(spot1, spot2) + ", with ID " + model.getTrackModel().trackIDOf(edge));

	}
}
