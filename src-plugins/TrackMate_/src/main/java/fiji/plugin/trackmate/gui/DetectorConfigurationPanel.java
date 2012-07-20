package fiji.plugin.trackmate.gui;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.DetectorSettings;

/**
 * The mother class for all the configuration panels that can configure a certain
 * sub-class of {@link DetectorSettings}.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011
 *
 */
public abstract class DetectorConfigurationPanel <T extends RealType<T> & NativeType<T>> extends ActionListenablePanel {

	private static final long serialVersionUID = -3740053698736400575L;

	/**
	 * Echo the parameters of the given instance of {@link DetectorSettings} on
	 * this panel. For convenience, we pass the whole model to this panel;
	 * the configuration panel is expected to work only on the {@link Settings#detectorSettings}
	 * field of the settings object in the model.
	 * But some specialized settings might require to access the declared 
	 * features or other data to generate a proper settings object. 
	 */
	public abstract void setDetectorSettings(TrackMateModel<T> model);
	
	/**
	 * @return  the {@link DetectorSettings} object with its field values set
	 * by this panel.
	 */
	public abstract DetectorSettings<T> getDetectorSettings();
	
	
}
