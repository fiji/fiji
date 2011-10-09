package fiji.plugin.trackmate.gui;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

/**
 * The mother class for all the configuration panels that can configure a certain
 * sub-class of {@link SegmenterSettings}.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011
 *
 */
public abstract class SegmenterConfigurationPanel extends ActionListenablePanel {

	private static final long serialVersionUID = -3740053698736400575L;

	/**
	 * Echo the parameters of the given instance of {@link SegmenterSettings} on
	 * this panel. For convenience, we pass the whole model to this panel;
	 * the configuration panel is expected to work only on the {@link Settings#segmenterSettings}
	 * field of the settings object in the model.
	 * But some specialized settings might require to access the declared 
	 * features or other data to generate a proper settings object. 
	 */
	public abstract void setSegmenterSettings(TrackMateModel model);
	
	/**
	 * @return  the {@link SegmenterSettings} object with its field values set
	 * by this panel.
	 */
	public abstract SegmenterSettings getSegmenterSettings();
	
	
}
