package fiji.plugin.trackmate.gui;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.TrackerSettings;

/**
 * Mother class for tracker settings panel. 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011
 *
 */
public abstract class TrackerSettingsPanel extends ActionListenablePanel {

	private static final long serialVersionUID = -5752429080747619688L;

	/**
	 * Update the {@link TrackerSettings} object given at the creation of this panel with the
	 * settings entered by the user on this panel. 
	 */
	public abstract TrackerSettings getTrackerSettings();
	
	/**
	 * Echo the parameters of the given instance of {@link TrackerSettings} on
	 * this panel. For convenience, we pass the whole model to this panel;
	 * the configuration panel is expected to work only on the {@link Settings#trackerSettings}
	 * field of the settings object in the model.
	 * But some specialized settings might require to access the declared 
	 * features or other data to generate a proper settings object. 
	 */
	public abstract void setTrackerSettings(TrackMateModel model);
}
