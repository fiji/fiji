package fiji.plugin.trackmate.gui;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.TrackerSettings;

/**
 * Mother class for tracker settings panel. 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011
 *
 */
public abstract class TrackerSettingsPanel extends ActionListenablePanel {

	private static final long serialVersionUID = -3943164177107531996L;

	/**
	 * Update the {@link TrackerSettings} object given at the creation of this panel with the
	 * settings entered by the user on this panel. 
	 */
	public abstract TrackerSettings getSettings();
	
	/**
	 * Echo the parameters of the given instance of {@link TrackerSettings} on
	 * this panel. Also for convenience, we pass the whole model to this panel.
	 * Indeed, some specialized settings might require to access the declared 
	 * features or other data to generate a proper settings object. 
	 */
	public abstract void setTrackerSettings(TrackerSettings settings, TrackMateModel model);
}
