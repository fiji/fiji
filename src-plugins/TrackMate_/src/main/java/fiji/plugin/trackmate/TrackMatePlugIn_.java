package fiji.plugin.trackmate;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.numeric.NumericType;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import ij.IJ;
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
		if (null == imp) {
			IJ.error("No imaage selected.", "Please select an image first.");
		}
		ImgPlus<?> img = ImagePlusAdapter.wrapImgPlus(imp);
		
		settings 	= createSettings(); 
		trackmate 	= createTrackMate();
		
		TrackMateModel model = trackmate.getModel();
		
		/*
		 * Spot features
		 */
		
		SpotAnalyzerProvider spotAnalyzerProvider = createSpotAnalyzerProvider(model, img);
		for (String analyzername : spotAnalyzerProvider.getAvailableSpotFeatureAnalyzers()) {
			SpotAnalyzerFactory<?> analyzer = spotAnalyzerProvider.getSpotFeatureAnalyzer(analyzername);
			settings.addSpotAnalyzerFactory(analyzer);
		}
		
		
		
		/*
		 * Launch GUI
		 */
		
		
		TrackMateGUIController controller = new TrackMateGUIController(trackmate);
		
	}
	
	/**
	 * Hook for subclassers: <br>
	 * Creates the provider for {@link SpotAnalyzerFactory}s, which will be in turned used
	 * to compute spot features.
	 * @param model the model this provider will operate on.
	 * @param img the source {@link ImgPlus} for calculation.
	 * @return a new {@link SpotAnalyzerProvider} instance.
	 */
	protected SpotAnalyzerProvider createSpotAnalyzerProvider(TrackMateModel model, ImgPlus<?> img) {
		return new SpotAnalyzerProvider(model, img);
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Settings} instance that will be used to tune 
	 * the {@link TrackMate} instance.
	 * @return a new {@link Settings} instance.
	 */
	protected Settings createSettings() {
		return new Settings();
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
