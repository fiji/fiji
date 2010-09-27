package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.PickImagePeaks;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class PeakPickerSegmenter<T extends RealType<T>> extends AbstractSpotSegmenter<T> {

	/*
	 * FIELDS
	 */
	
	public final static String BASE_ERROR_MESSAGE = "PeakPickerSegmenter: ";
	
	
	
	/*
	 * CONSTRUCTOR
	 */
	
	public PeakPickerSegmenter() {
		baseErrorMessage = BASE_ERROR_MESSAGE;
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public boolean checkInput() {
		return super.checkInput();
	}

	@Override
	public boolean process() {
		
		PickImagePeaks<T> peakPicker = new PickImagePeaks<T>(img);
		
		double[] suppressionRadiuses = new double[img.getNumDimensions()];
		for (int i = 0; i < img.getNumDimensions(); i++) 
			suppressionRadiuses[i] = radius / calibration[i];
		peakPicker.setSuppression(suppressionRadiuses); // in pixels
		
		if (!peakPicker.checkInput() || !peakPicker.process()) {
			errorMessage = baseErrorMessage +"Could not run the peak picker algorithm:\n" + peakPicker.getErrorMessage();
			return false;
		}
		
		// Create spots
		ArrayList<int[]> peaks = peakPicker.getPeakList();
		spots.clear();
		for(int[] peak : peaks) {
			float[] coords = new float[3];
			for (int i = 0; i < img.getNumDimensions(); i++) 
				coords[i] = peak[i] * calibration[i];
			Spot spot = new SpotImp(coords);
			spots.add(spot);
		}
		
		// Deal with intermediate image: return spot location as bit image
		intermediateImage = img.createNewImage();
		Image<BitType> bitImage = peakPicker.getResult();
		LocalizableCursor<T> imgCursor = img.createLocalizableCursor();
		LocalizableByDimCursor<BitType> bitCursor = bitImage.createLocalizableByDimCursor();
		while(imgCursor.hasNext()) {
			imgCursor.fwd();
			bitCursor.setPosition(imgCursor);
			imgCursor.getType().setReal(bitCursor.getType().getRealFloat());
		}
		imgCursor.close();
		bitCursor.close();
		
		return true;
	}

}
