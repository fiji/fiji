package fiji.plugin.trackmate.visualization.test;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;


public class HyperStackDisplayerTestDrive {

//	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");

	public static void main(String[] args) throws JDOMException, IOException {
		
		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		
		ij.ImageJ.main(args);
		
		final TrackMateModel model = reader.getModel();

		// Grab spot icons
		if (null != model.getSettings().imp)
			model.computeFeatures();
				
		final AbstractTrackMateModelView displayer = AbstractTrackMateModelView.instantiateView(AbstractTrackMateModelView.ViewType.HYPERSTACK_DISPLAYER, model);
//		displayer.setSpots(model.getSpots());
//		displayer.setSpotsToShow(model.getFilteredSpots());
//		displayer.setTrackGraph(model.getTrackGraph());
		displayer.setDisplayTrackMode(TrackDisplayMode.LOCAL_WHOLE_TRACKS, 5);
		displayer.setSpotNameVisible(true);
		
		final TrackSchemeFrame trackScheme = new TrackSchemeFrame(model);
		trackScheme.setVisible(true);
		
	}
	
}
