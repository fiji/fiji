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

	private static final long serialVersionUID = -1805603327110848271L;

	/**
	 * Echo the parameters of the given instance of {@link SegmenterSettings} on
	 * this panel.
	 */
	public abstract void setSegmenterSettings(SegmenterSettings settings);
	
	/**
	 * @return  the {@link SegmenterSettings} object with its field values set
	 * by this panel.
	 */
	public abstract SegmenterSettings getSegmenterSettings();
	
	
}
