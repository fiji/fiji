package fiji.plugin.trackmate.gui;

import java.awt.event.ActionEvent;
import java.util.Map;

import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;
import fiji.plugin.trackmate.tracking.SpotTracker;

/**
 * The mother class for all the configuration panels that can configure a {@link SpotDetectorFactory},
 * a {@link SpotTracker}, ...
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011 - 2012
 *
 */
public abstract class ConfigurationPanel extends ActionListenablePanel {

	private static final long serialVersionUID = 1L;
	/**
	 * This event is fired when a preview button is fired within this configuration panel.
	 * It is the responsibility of concrete implementation to do whatever they want with it.
	 */
	public final ActionEvent PREVIEW_BUTTON_PUSHED = new ActionEvent(this, 0, "PreviewButtonPushed");

	/**
	 * Echo the parameters of the given instance of {@link DetectorSettings} on
	 * this panel.  
	 */
	public abstract void setSettings(final Map<String, Object> settings);
	
	/**
	 * @return  a new settings map object with its values set
	 * by this panel.
	 */
	public abstract Map<String, Object> getSettings();

}
