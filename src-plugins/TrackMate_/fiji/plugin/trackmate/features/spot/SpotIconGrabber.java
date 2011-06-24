package fiji.plugin.trackmate.features.spot;

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

import com.mxgraph.util.mxBase64;

import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;

/**
 * This class is used to take a snapshot of a {@link Spot} object (or collection) from 
 * its coordinates and an {@link ImagePlus} that contain the pixel data.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Dec 2010
 */
public class SpotIconGrabber <T extends RealType<T>> extends IndependentSpotFeatureAnalyzer {

	private float[] calibration;
	private Image<T> img;
	
	public SpotIconGrabber(Image<T> originalImage, float[] calibration) {
		this.img = originalImage;
		this.calibration = calibration;
	}
	
	@Override
	public void process(Spot spot) {
		// Get crop coordinates
		final float radius = spot.getFeature(SpotFeature.RADIUS); // physical units, REQUIRED!
		int x = Math.round((spot.getFeature(SpotFeature.POSITION_X) - radius) / calibration[0]); 
		int y = Math.round((spot.getFeature(SpotFeature.POSITION_Y) - radius) / calibration[1]);
		int width = Math.round(2 * radius / calibration[0]);
		int height = Math.round(2 * radius / calibration[1]);
		
		// Copy cropped view
		Image<T> crop = img.createNewImage(new int[] {width, height});
		LocalizableByDimCursor<T> sourceCursor = img.createLocalizableByDimCursor();
		LocalizableByDimCursor<T> targetCursor = crop.createLocalizableByDimCursor();
		if (img.getNumDimensions() > 2) {
			int slice = 0;
			slice = Math.round(spot.getFeature(SpotFeature.POSITION_Z) / calibration[2]);
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

	@Override
	public SpotFeature getFeature() {
		return null;
	}
}
