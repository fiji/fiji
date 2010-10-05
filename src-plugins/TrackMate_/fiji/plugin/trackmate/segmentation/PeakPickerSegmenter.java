package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.PickImagePeaks;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class PeakPickerSegmenter<T extends RealType<T>> extends AbstractSpotSegmenter<T> {

	/*
	 * FIELDS
	 */
	
	public final static String BASE_ERROR_MESSAGE = "PeakPickerSegmenter: ";
	
	
	
	/*
	 * CONSTRUCTORS
	 */
	
	public PeakPickerSegmenter(SegmenterSettings segmenterSettings) {
		super(segmenterSettings);
		baseErrorMessage = BASE_ERROR_MESSAGE;
	}
	
	/*
	 * METHODS
	 */
	

	@Override
	public boolean process() {
		
		float radius = settings.expectedRadius;
		PickImagePeaks<T> peakPicker = new PickImagePeaks<T>(intermediateImage); // TODO
		
		double[] suppressionRadiuses = new double[img.getNumDimensions()];
		for (int i = 0; i < img.getNumDimensions(); i++) 
			suppressionRadiuses[i] = radius / calibration[i];
		peakPicker.setSuppression(suppressionRadiuses); // in pixels
		
		if (!peakPicker.checkInput() || !peakPicker.process()) {
			errorMessage = baseErrorMessage +"Could not run the peak picker algorithm:\n" + peakPicker.getErrorMessage();
			return false;
		}
		
		// Create spots
		LocalizableByDimCursor<T> cursor = intermediateImage.createLocalizableByDimCursor();
		ArrayList<int[]> peaks = peakPicker.getPeakList();
		spots.clear();
		for(int[] peak : peaks) {
			cursor.setPosition(peak);
			if (cursor.getType().getRealFloat() < settings.threshold)
				break; // because peaks are sorted, we can exit loop here
			float[] coords = new float[3];
			for (int i = 0; i < img.getNumDimensions(); i++) 
				coords[i] = peak[i] * calibration[i];
			Spot spot = new SpotImp(coords);
			spots.add(spot);
		}
		
		return true;
	}

}
