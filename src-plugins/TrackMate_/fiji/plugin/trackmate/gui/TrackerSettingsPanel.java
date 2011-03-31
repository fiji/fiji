package fiji.plugin.trackmate.gui;

import fiji.plugin.trackmate.tracking.TrackerSettings;

/**
 * Mother class for tracker settings panel. Also offer a factory method to instantiate 
 * the correct panel pointed by a tracker type.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Jan 12, 2011
 *
 */
public abstract class TrackerSettingsPanel extends ActionListenablePanel {

	private static final long serialVersionUID = 6489221290360334663L;
	
	/**
	 * Update the {@link TrackerSettings} object given at the creation of this panel with the
	 * settings entered by the user on this panel. 
	 */
	public abstract TrackerSettings getSettings();
	
}
