package fiji.plugin.trackmate.features.spot;

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;

import com.mxgraph.util.mxBase64;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;

/**
 * This class is used to take a snapshot of a {@link Spot} object (or collection) from 
 * its coordinates and an {@link ImagePlus} that contain the pixel data.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Dec 2010 - 2012
 */
public class SpotIconGrabber extends IndependentSpotFeatureAnalyzer {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void  process(Spot spot) {
		// Get crop coordinates
		final float radius = spot.getFeature(Spot.RADIUS); // physical units, REQUIRED!
		int x = Math.round((spot.getFeature(Spot.POSITION_X) - radius) / calibration[0]); 
		int y = Math.round((spot.getFeature(Spot.POSITION_Y) - radius) / calibration[1]);
		int width = Math.round(2 * radius / calibration[0]);
		int height = Math.round(2 * radius / calibration[1]);
		
		// Copy cropped view
		Image crop = img.createNewImage(new int[] {width, height});
		LocalizableByDimCursor sourceCursor = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory(img.createType()));
		LocalizableByDimCursor targetCursor = crop.createLocalizableByDimCursor();
		if (img.getNumDimensions() > 2) {
			int slice = 0;
			slice = Math.round(spot.getFeature(Spot.POSITION_Z) / calibration[2]);
			if (slice < 0) {
				slice = 0;
			}
			if (slice >= img.getDimension(2)) {
				slice = img.getDimension(2) -1;
			}
			sourceCursor.setPosition(slice, 2);
		}
		
		try {
			for (int i = 0; i < width; i++) {
				sourceCursor.setPosition(i + x, 0);
				targetCursor.setPosition(i, 0);
				for (int j = 0; j < height; j++) {
					sourceCursor.setPosition(j + y, 1);
					targetCursor.setPosition(j, 1);
					targetCursor.getType().set(sourceCursor.getType());
				}
			}
			// Convert to ImagePlus
			ImagePlus imp = ImageJFunctions.copyToImagePlus(crop);
			ImageProcessor ip = imp.getProcessor();
			ip.resetMinAndMax();

			// Convert to base64
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			BufferedImage img = ip.getBufferedImage();
			try {
				ImageIO.write(img, "png", bos);
				spot.setImageString(mxBase64.encodeToString(bos.toByteArray(), false));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (ArrayIndexOutOfBoundsException aioe) {
			// Do nothing, we do not set the icon field
		} finally {
			targetCursor.close();
			sourceCursor.close();
		}
		
	}
	
	
	/*
	 * FEATURE OUTPUT
	 * We always return the empty list or map, because we do not want the spot icon feature
	 * or whatever it would be to appear in the normal, numerical feature list.
	 */
	
	@Override
	public Collection<String> getFeatures() {
		return new ArrayList<String>();
	}

	@Override
	public Map<String, String> getFeatureShortNames() {
		return new HashMap<String, String>();
	}

	@Override
	public Map<String, String> getFeatureNames() {
		return new HashMap<String, String>();
	}

	@Override
	public Map<String, Dimension> getFeatureDimensions() {
		return new HashMap<String, Dimension>();
	}
}
