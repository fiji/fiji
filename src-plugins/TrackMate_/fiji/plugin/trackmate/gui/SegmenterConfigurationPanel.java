package fiji.plugin.trackmate.gui;

import fiji.plugin.trackmate.segmentation.SegmenterSettings;

/**
 * The mother class for all the configuration panels that can configure a certain
 * sub-class of {@link SegmenterSettings}.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011
 *
 */
public abstract class SegmenterConfigurationPanel extends ActionListenablePanel {

	private static final long serialVersionUID = -4404004531929041752L;

	/**
	 * Echo the parameters of the given instance of {@link SegmenterSettings} on
	 * this panel. Also for convenience, we pass the physical units name
	 * to the panel, so that the user can enter only physical quantities.
	 */
	public abstract void setSegmenterSettings(SegmenterSettings settings, String spaceUnits, String timeUnits);
	
	/**
	 * @return  the {@link SegmenterSettings} object with its field values set
	 * by this panel.
	 */
	public abstract SegmenterSettings getSegmenterSettings();
	
	
}
