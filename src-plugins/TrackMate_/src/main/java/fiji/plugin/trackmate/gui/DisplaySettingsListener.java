package fiji.plugin.trackmate.gui;


/**
 * Interface for classes that listen to display settings changes on a GUI.
 * @author Jean-Yves Tinevez - 2013
 */
public interface DisplaySettingsListener {
	
	/**
	 * Called when a display settings is changed on a GUI.
	 * @param event  the {@link DisplaySettingsEvent} containing the settings change.
	 */
	public void displaySettingsChanged(DisplaySettingsEvent event);
}