package fiji.plugin.nperry;

import fiji.plugin.nperry.features.FeatureFacade;
import fiji.plugin.nperry.gui.SpotDisplayer;
import fiji.plugin.nperry.gui.ThresholdGuiPanel;
import fiji.plugin.nperry.segmentation.SpotSegmenter;
import fiji.util.SplitString;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij3d.Image3DUniverse;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * <p>The Embryo_Tracker class runs on the currently active time lapse image (2D or 3D) of embryo development
 * and both identifies and tracks fluorescently stained nuclei over time.</p>
 * 
 * <p><b>Required input:</b> A 2D or 3D time-lapse image of embryo development.</p>
 * <p><b>Output:</b> Detailed information regarding embryo development. TODO add more</p>
 * 
 * <p>There are two landmark steps for the Embryo_Tracker class:</p>
 * 
 * <ol>
 * <li>Segmentation (nuclei/object detection).</li>
 * <li>Object tracking.</li>
 * </ol>
 * 
 * <p><u>Segmentation:</u><p>
 * <p>The segmentation step includes many pre-processing techniques before the actual object-detection
 * algorithm is run.</p>
 * 
 * <ol>
 * <li>The image is down-sampled in order to: [a] reduce the processing time of the other pre-processing
 * techniques; and [b] reduce the scale of the 'objects' (in our case, nuclei) to be a uniform diameter in
 * all dimensions. Thus, each dimension is scaled separately to reduce the diameter in that dimension
 * to the uniform diameter (default, 10 pixels), if possible, since it is possible the original diameter
 * is less than 10 pixels to start, in which case no down-sampling is performed in that dimension.</li>
 * 
 * <li>An optional median filter is applied to reduce salt-and-pepper noise in hopes
 * of false-positive reduction (the user specifies whether to run a median filter).</li>
 * 
 * <li>The newly down-sized, and optionally median filtered, image is then convolved with a Gaussian kernel,
 * with <code>sigma = 1.55</code>. Using this <code>sigma</code>, the Gaussian kernel is about the size of our objects (nuclei)
 * in volume, and therefore will (hopefully) make the center of the nuclei the brightest pixel in the convolved 
 * image, while at the same time further eliminating noise. The convolution itself is performed using the Fourier Transform
 * approach to convolution, which resulted in a faster computation.</li>
 * 
 * <li>The image is then convoluted with a modified Laplacian kernel (again, via a Fourier Transform). The kernel is modified
 * such that the center matrix cell is actually positive, and the surrounding cells are negative. The result of this
 * modification is an image where the brightest regions in the input image are also the brightest regions in the output image.
 * The purpose of this convolution is to accentuate the areas on the image where the bright pixels are "flowing," which should
 * represent object centers following the Gaussian convolution.</li>
 * 
 * <li>Finally, the entire, newly processed image is searched for regional maxima, which are defined as pixels (or groups of equally-valued
 * intensity pixels) that are strictly brighter than their adjacent neighbors. Following the pre-processing steps above,
 * these regional maxima likely represent the center of the objects we would like to find and track (the nuclei).</li>
 * 
 * <li>Because of the presence of noise, it is possible that some of the regional maxima found in the preivous step have
 * identified noise, rather than objects of interest. As such, we allow the user at this stage to 'threshold' the identified
 * objects based on different features that were extracted from the object's location in the image. Example features include:
 * value of the maxima following the LoG; total brightness in the object's volume; contrast at the object's edges, etc.</li>
 * </ol>
 * 
 * <p><u>Object Tracking:</u><p>
 * TODO write a description of the object tracking
 * 
 * @author nperry
 *
 * @param <T>
 */
public class Embryo_Tracker<T extends RealType<T>> implements PlugIn {
	
	/*
	 * FIELDS
	 */
	
	private static final String DIAMETER_KEY = "diameter";
	private static final String MEDIAN_FILTER_KEY = "median";
	private static final String ALLOW_EDGE_KEY = "edge";
	private static final Map<String, String> settings = new HashMap<String, String>();
	{
		settings.put(DIAMETER_KEY, "6.5");
		settings.put(MEDIAN_FILTER_KEY, "false");
		settings.put(ALLOW_EDGE_KEY, "false");
	}
	
	
	/*
	 * RUN METHOD
	 */
	
	/** Ask for parameters and then execute. */
	public void run(String arg) {
		/* 1 - Obtain the currently active image */
		ImagePlus imp = IJ.getImage();
		if (null == imp) return;
		
		/* 1.5 - Check macro params */
		try {
			Map<String, String> options = SplitString.splitMacroOptions(arg);
			for(String key : options.keySet())
				for(String setting : settings.keySet())
					if (key.equals(setting))
						settings.put(key, options.get(key));
		} catch (ParseException e) {}
		
		float diam = 5;
		boolean useMedFilt = false;
		boolean allowEdgeMax = false;
		try {
			diam = Float.parseFloat(settings.get(DIAMETER_KEY));
			useMedFilt = Boolean.parseBoolean(settings.get(MEDIAN_FILTER_KEY));			
			allowEdgeMax = Boolean.parseBoolean(settings.get(ALLOW_EDGE_KEY));
		} catch (Exception e) {
			IJ.error(e.toString());
		}
		
		/* 2 - Ask for parameters */
		int nslices = imp.getNSlices();
		GenericDialog gd = new GenericDialog("Embryo Tracker");
		gd.addNumericField("Expected blob diameter:", diam, 2, 5, imp.getCalibration().getUnits());  	// get the expected blob size (in pixels).
		gd.addMessage("Verify calibration settings:");
		gd.addNumericField("Pixel width:", imp.getCalibration().pixelWidth, 3);		// used to calibrate the image for 3D rendering
		gd.addNumericField("Pixel height:", imp.getCalibration().pixelHeight, 3);	// used to calibrate the image for 3D rendering
		if (nslices > 1)
			gd.addNumericField("Voxel depth:", imp.getCalibration().pixelDepth, 3);		// used to calibrate the image for 3D rendering
		gd.addCheckbox("Use median filter", useMedFilt);
		gd.addCheckbox("Allow edge maxima", allowEdgeMax);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		/* 3 - Retrieve parameters from dialog */
		diam = (float)gd.getNextNumber();
		float pixelWidth = (float)gd.getNextNumber();
		float pixelHeight = (float)gd.getNextNumber();
		float pixelDepth = 0;
		if (nslices > 1)
			pixelDepth = (float)gd.getNextNumber();
		useMedFilt = (boolean)gd.getNextBoolean();
		allowEdgeMax = (boolean)gd.getNextBoolean();
		float[] calibration;
		if (nslices > 1)
			calibration = new float[] {pixelWidth, pixelHeight, pixelDepth}; // 3D case
		else 
			calibration = new float[] {pixelWidth, pixelHeight}; // 2D case
		
		/* 4 - Execute! */
		exec(imp, diam, useMedFilt, allowEdgeMax, calibration);
		System.out.println("Done executing!");	
		
	}
	
	
	/** 
	 * Execute the plugin functionality: 
	 */
	public Object[] exec(ImagePlus imp, float diam, boolean useMedFilt, boolean allowEdgeMax, float[] calibration) {
		
		if (null == imp) return null;
		
		
		/* 1 -- Initialize local variables */
		final int numFrames = imp.getNFrames();
		final SpotSegmenter<T> segmenter = new SpotSegmenter<T>(null, diam, calibration, useMedFilt, allowEdgeMax);				
		final ArrayList<Spot> spotsAllFrames = new ArrayList<Spot>(numFrames);
		List<Spot> spots;
		Image<T> filteredImage;
		
		// For each frame...
		for (int i = 0; i < numFrames; i++) {
			
			/* 1 - Prepare stack for use with Imglib. */
			
			IJ.showStatus("Frame "+(i+1)+": Converting to ImgLib...");
			Image<T> img = Utils.getSingleFrameAsImage(imp, i);
			
			/* 2 Segment it */

			IJ.showStatus("Frame "+(i+1)+": Segmenting...");
			segmenter.setImage(img);
			if (segmenter.checkInput() && segmenter.process()) {
				filteredImage = segmenter.getFilteredImage();
				spots = segmenter.getResult();
				for (Spot spot : spots) {
					spot.setFrame(i);
					spot.setDisplayRadius(diam/2);
				}
				spotsAllFrames.addAll(spots);
			} else {
				IJ.error(segmenter.getErrorMessage());
				return null;
			}
			
			/* 3 - Extract features for the spot collection */
			IJ.showStatus("Frame "+(i+1)+": Calculating features...");
			final FeatureFacade<T> featureCalculator = new FeatureFacade<T>(img, filteredImage, diam, calibration);
			featureCalculator.processFeature(Feature.MEAN_INTENSITY, spots);
			
		} // Finished looping over frames
				
		// Launch renderer
		IJ.showStatus("Found "+spotsAllFrames.size() +" spots. Preparing renderer...");
		final SpotDisplayer displayer = new SpotDisplayer(spotsAllFrames);

		final Image3DUniverse universe = new Image3DUniverse();
		displayer.render(universe);
		universe.addVoltex(imp);
		
		// Launch threshold GUI
		final ThresholdGuiPanel gui = new ThresholdGuiPanel(spotsAllFrames);

		// Set listeners
		gui.addChangeListener(new ChangeListener() {
			private double[] t = null;
			private boolean[] is = null;
			private Feature[] f = null;
			@Override
			public void stateChanged(ChangeEvent e) {
				f = gui.getFeatures();
				is = gui.getIsAbove();
				t = gui.getThresholds();				
				displayer.threshold(f, t, is);
				universe.getCurrentTimepoint();
			}
		});
		
		// Display GUI
		JFrame frame = new JFrame();
		frame.getContentPane().add(gui);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		// Add a panel
		gui.addThresholdPanel(Feature.MEAN_INTENSITY);
		
		universe.show();
		
		return new Object[] {spotsAllFrames};
	}

	
}
