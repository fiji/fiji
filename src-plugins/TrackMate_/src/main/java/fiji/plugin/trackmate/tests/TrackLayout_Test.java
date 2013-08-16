package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class TrackLayout_Test {

	public static void main(String[] args) {
		
		Model model = Graph_Test.getExampleModel();
		
		TrackScheme trackScheme = new TrackScheme(model, new SelectionModel(model));
		trackScheme.render();
		
	}

}
