package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.detection.BasicDetectorSettings;
import fiji.plugin.trackmate.detection.DetectorSettings;
import fiji.plugin.trackmate.detection.DogDetector;
import fiji.plugin.trackmate.detection.DownSampleLogDetectorConfigurationPanel;
import fiji.plugin.trackmate.detection.DownSampleLogDetectorSettings;
import fiji.plugin.trackmate.detection.DownsampleLogDetector;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.detection.LogDetectorSettings;
import fiji.plugin.trackmate.detection.ManualDetector;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.gui.BasicDetectorConfigurationPanel;
import fiji.plugin.trackmate.gui.DetectorConfigurationPanel;
import fiji.plugin.trackmate.gui.LogDetectorConfigurationPanel;

public class DetectorFactory <T extends RealType<T> & NativeType<T>> {

	
	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant detector classes.  */
	protected List<String> names;
	
	/*
	 * BLANK CONSTRUCTOR
	 */
	
	/**
	 * This factory provides the GUI with the spot detectors currently available in the 
	 * TrackMate plugin. Each detector is identified by a key String, which can be used 
	 * to retrieve new instance of the detector, settings for the target detector and a 
	 * GUI panel able to configure these settings.
	 * <p>
	 * If you want to add custom detectors to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom detectors and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public DetectorFactory() {
		registerDetectors();
	}
	
	
	/*
	 * METHODS
	 */
	
	/**
	 * Register the standard detectors shipped with TrackMate.
	 */
	protected void registerDetectors() {
		// Names
		names = new ArrayList<String>(4);
		names.add(LogDetector.NAME);
		names.add(DogDetector.NAME);
		names.add(DownsampleLogDetector.NAME);
		names.add(ManualDetector.NAME);
	}
	
	/**
	 * @return a new instance of the target detector identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public SpotDetector<T> getDetector(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new LogDetector<T>();
		case 1:
			return new DogDetector<T>();
		case 2:
			return new DownsampleLogDetector<T>();
		case 3:
			return new ManualDetector<T>();
		default:
			return null;
		}
	}
	
	/**
	 * @return a new instance of settings suitable for the target detector identified by 
	 * the key parameter. Settings are instantiated with default values.  
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public DetectorSettings<T> getDefaultSettings(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
		case 1:
			return new LogDetectorSettings<T>();
		case 2:
			return new DownSampleLogDetectorSettings<T>();
		case 3:
			return new BasicDetectorSettings<T>();
		default:
			return null;
		}
	}
	
	/**
	 * @return the html String containing a descriptive information about the target detector,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return LogDetector.INFO_TEXT;
		case 1:
			return DogDetector.INFO_TEXT;
		case 2:
			return DownsampleLogDetector.INFO_TEXT;
		case 3:
			return ManualDetector.INFO_TEXT;
		default:
			return null;
		}
	}
	
	/**
	 * @return a new GUI panel able to configure the settings suitable for the target detector 
	 * identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */

	public DetectorConfigurationPanel<T> getDetectorConfigurationPanel(String key) 	{
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new LogDetectorConfigurationPanel<T>(LogDetector.INFO_TEXT);
		case 1:
			return new LogDetectorConfigurationPanel<T>(DogDetector.INFO_TEXT);
		case 2:
			return new DownSampleLogDetectorConfigurationPanel<T>();
		case 3:
			return new BasicDetectorConfigurationPanel<T>(ManualDetector.INFO_TEXT);
		default:
			return null;
		}
	}

	/**
	 * @return a list of the detector names available through this factory.
	 */
	public List<String> getAvailableDetectors() {
		return names;
	}

}
