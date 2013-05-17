package fiji.plugin.trackmate.tests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

public class SpotFilteringPerf_TestDrive {

	public static void main(String[] args) {

		//		ImageJ.main(args);

//		File file = new File("/Users/tinevez/Desktop/RECEPTOR.xml");
		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = reader.getModel();
		Settings settings = reader.getSettings(null, null, 
				new SpotAnalyzerProvider(model), new EdgeAnalyzerProvider(model), new TrackAnalyzerProvider(model));
		TrackMate trackmate = new TrackMate(model, settings);

		for (int i = 0; i < 5; i++) {

			{
				// Remove filter
				settings.getSpotFilters().clear();

				long start = System.currentTimeMillis();
				int nspots0 = model.getSpots().getNSpots(true);
				trackmate.execSpotFiltering(true);
				long end = System.currentTimeMillis();
				int nspots1 = model.getSpots().getNSpots(true);

				System.out.println("Moved from " + nspots0 + " spots to " + nspots1 + " in " + (end-start) + " ms.");
			}

			{
				// add filter
				settings.getSpotFilters().add(new FeatureFilter(Spot.QUALITY, 60d, true));

				long start = System.currentTimeMillis();
				int nspots0 = model.getSpots().getNSpots(true);
				trackmate.execSpotFiltering(true);
				long end = System.currentTimeMillis();
				int nspots1 = model.getSpots().getNSpots(true);

				System.out.println("Moved from " + nspots0 + " spots to " + nspots1 + " in " + (end-start) + " ms.");
			}

		}
		/*
		final HyperStackDisplayer displayer = new HyperStackDisplayer(model);
		displayer.render();

		final FilterGuiPanel component = new FilterGuiPanel();
		component.setTarget(model.getFeatureModel().getSpotFeatures(), model.getSettings().getSpotFilters(),  
				model.getFeatureModel().getSpotFeatureNames(), model.getFeatureModel().getSpotFeatureValues(), "spots");
		JFrame frame = new JFrame("Spot filters");
		frame.getContentPane().add(component);
		frame.setSize(600, 800);
		frame.setVisible(true);

		component.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				System.out.println("stateChanged caught.");
				trackmate.getModel().getSettings().setSpotFilters(component.getFeatureFilters());
				long start = System.currentTimeMillis();
				trackmate.execSpotFiltering(true);
				System.out.println("Filtering done in " + (System.currentTimeMillis()-start) + " ms.");
				displayer.refresh();
			}
		});
		 */

	}

}
