package fiji.plugin.trackmate;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.ManualTrackingGUIController;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.util.Map;

public class ManualTrackingPlugIn_ extends TrackMatePlugIn_ implements PlugIn {


	@Override
	public void run(final String arg) {

		final ImagePlus imp = WindowManager.getCurrentImage();
		if (null == imp) {
			return;
		}

		settings 	= createSettings(imp);
		trackmate 	= createTrackMate();

		/*
		 * Launch GUI.
		 */

		final ManualTrackingGUIController controller = new ManualTrackingGUIController(trackmate);
		if (imp != null) {
			GuiUtils.positionWindow(controller.getGUI(), imp.getWindow());
		}

		/*
		 * Launch view
		 */

		final HyperStackDisplayer view = new HyperStackDisplayer(trackmate.getModel(), controller.getSelectionModel(), imp);
		final Map<String, Object> displaySettings = controller.getGuimodel().getDisplaySettings();
		for (final String key : displaySettings.keySet()) {
			view.setDisplaySettings(key, displaySettings.get(key));
		}
		view.render();
		controller.getGuimodel().addView(view);

	}

}
