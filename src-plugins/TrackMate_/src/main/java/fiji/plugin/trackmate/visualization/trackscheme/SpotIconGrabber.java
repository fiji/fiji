package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import com.mxgraph.util.mxBase64;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * This class is used to take a snapshot of a {@link Spot} object (or collection) from 
 * its coordinates and an {@link ImagePlus} that contain the pixel data.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Dec 2010 - 2012
 */
public class SpotIconGrabber<T extends RealType<T>> {

	private final ImgPlus<T> img;

	public SpotIconGrabber(ImgPlus<T> img) {
		this.img = img;
	}

	/**
	 * @return the image string for the specified spot. The spot x,y,z and radius coordinates
	 * are used to get a location on the image given at construction. Physical coordinates
	 * are transformed in pixel coordinates thanks to the calibration stored in the {@link ImgPlus}.
	 */
	public String getImageString(Spot spot) {
		// Get crop coordinates
		final double[] calibration = TMUtils.getSpatialCalibration(img);
		final double radius = spot.getFeature(Spot.RADIUS); // physical units, REQUIRED!
		long x = Math.round((spot.getFeature(Spot.POSITION_X) - radius) / calibration[0]); 
		long y = Math.round((spot.getFeature(Spot.POSITION_Y) - radius) / calibration[1]);
		long width = Math.round(2 * radius / calibration[0]);
		long height = Math.round(2 * radius / calibration[1]);

		// Copy cropped view
		long slice = 0;
		if (img.numDimensions() > 2) {
			slice = Math.round(spot.getFeature(Spot.POSITION_Z) / calibration[2]);
			if (slice < 0) {
				slice = 0;
			}
			if (slice >= img.dimension(2)) {
				slice = img.dimension(2) -1;
			}
		}

		Img<T> crop = grabImage(x, y, slice, width, height);

		// Convert to ImagePlus
		ImagePlus imp = ImageJFunctions.wrap(crop, crop.toString());
		ImageProcessor ip = imp.getProcessor();
		ip.resetMinAndMax();

		// Convert to base64
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BufferedImage img = ip.getBufferedImage();
		try {
			ImageIO.write(img, "png", bos);
			return mxBase64.encodeToString(bos.toByteArray(), false);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public final Img<T> grabImage(long x, long y, long slice, long width, long height) {

		// Copy cropped view
		Img<T> crop = img.factory().create(new long[] { width, height }, img.firstElement().copy());

		T zeroType = img.firstElement().createVariable();
		zeroType.setZero();

		OutOfBoundsConstantValueFactory<T, Img<T>> oobf 
		= new OutOfBoundsConstantValueFactory<T, Img<T>>(zeroType);
		RandomAccess<T> sourceCursor = Views.extend(img, oobf).randomAccess();
		RandomAccess<T> targetCursor = crop.randomAccess();

		if (img.numDimensions() > 2) {
			sourceCursor.setPosition(slice, 2);
		}
		for (int i = 0; i < width; i++) {
			sourceCursor.setPosition(i + x, 0);
			targetCursor.setPosition(i, 0);
			for (int j = 0; j < height; j++) {
				sourceCursor.setPosition(j + y, 1);
				targetCursor.setPosition(j, 1);
				targetCursor.get().set(sourceCursor.get());
			}
		}
		return crop;
	}
}
