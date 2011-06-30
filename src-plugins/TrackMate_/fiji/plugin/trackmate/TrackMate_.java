package fiji.plugin.trackmate;

import fiji.plugin.trackmate.gui.TrackMateFrameController;
import ij.IJ;
import ij.WindowManager;
import ij.plugin.PlugIn;


/**
 * <p>The TrackMate_ class runs on the currently active time lapse image (2D or 3D) 
 * and both identifies and tracks bright blobs over time.</p>
 * 
 * <p><b>Required input:</b> A 2D or 3D time-lapse image with bright blobs.</p>
 *
 * @author Nicholas Perry, Jean-Yves Tinevez - Institut Pasteur - July 2010 - 2011
 *
 */
public class TrackMate_ extends TrackMateModel implements PlugIn {
	
	public static final String PLUGIN_NAME_STR = "Track Mate";
	public static final String PLUGIN_NAME_VERSION = ".beta_2011-06-30";
	
	/*
	 * CONSTRUCTORS
	 */
	
	public TrackMate_() {
		this.settings = new Settings();
	}
	
	public TrackMate_(Settings settings) {
		this.settings = settings;
	}
	
	
	/*
	 * RUN METHOD
	 */
	
	/** 
	 * Launch the GUI.
	 */
	public void run(String arg) {
		settings.imp = WindowManager.getCurrentImage();
		new TrackMateFrameController(this);
	}
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		ij.ImageJ.main(args);
		IJ.open("/Users/tinevez/Desktop/Data/FakeTracks.tif");
		TrackMate_ model = new TrackMate_();
		model.run(null);
	}
	
	
}
