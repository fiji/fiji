package fiji.plugin.trackmate;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class TrackMatePlugIn_ implements PlugIn {

	protected TrackMate trackmate;
	protected Settings settings;
	
	
	
	@Override
	public void run(String arg0) {
		
		ImagePlus imp = WindowManager.getCurrentImage(); 
		settings 	= createSettings(imp);
		trackmate 	= createTrackMate();
		
		/*
		 * Launch GUI.
		 */
		
		TrackMateGUIController controller = new TrackMateGUIController(trackmate);
		if (imp != null) {
			GuiUtils.positionWindow(controller.getGUI(), imp.getWindow());
		}
	}


	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Settings} instance that will be used to tune 
	 * the {@link TrackMate} instance. It is iniatialized by default with
	 * values taken from the current {@link ImagePlus}.
	 * @return a new {@link Settings} instance.
	 */
	protected Settings createSettings(ImagePlus imp) {
		Settings settings = new Settings();
		settings.setFrom(imp);
		return settings;
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the TrackMate instance that will be controlled in the GUI.
	 * @return a new {@link TrackMate} instance.
	 */
	protected TrackMate createTrackMate() {
		return new TrackMate(settings);
	}

	
	
	/*
	 * MAIN METHOD
	 */
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ImageJ.main(args);
		new TrackMatePlugIn_().run(null);
	}

}
