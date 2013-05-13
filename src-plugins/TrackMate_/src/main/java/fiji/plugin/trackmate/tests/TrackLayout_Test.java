package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class TrackLayout_Test {

	public static void main(String[] args) {
		
		TrackMateModel model = Graph_Test.getExampleModel();
		
		TrackScheme trackScheme = new TrackScheme(model);
		trackScheme.render();
		
	}

}
