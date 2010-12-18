package fiji.plugin.trackmate.gui;

import javax.swing.JPanel;

import fiji.plugin.trackmate.tracking.TrackerSettings;

public abstract class TrackerSettingsPanel extends JPanel {

	private static final long serialVersionUID = 6489221290360334663L;

	/**
	 * Update the {@link TrackerSettings} object given at the creation of this panel with the
	 * settings entered by the user on this panel. 
	 */
	public abstract TrackerSettings getSettings();
	
}
