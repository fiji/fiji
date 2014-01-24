package tests;

import static org.junit.Assume.assumeNotNull;
import gadgets.MaskFactory;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.algorithm.math.ImageStatistics;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import algorithms.MissingPreconditionException;

/**
 * A class containing some testing helper methods. It allows
 * to open Tiffs from within the Jar file and can generate noise
 * images.
 *
 * @author Dan White & Tom Kazimiers
 */
public class TestImageAccessor {
	/* a static opener for opening images without the
	 * need for creating every time a new opener
	 */
	static Opener opener = new Opener();

	/**
	 * Loads a Tiff file from within the jar. The given path is treated
	 * as relative to this tests-package (i.e. "Data/test.tiff" refers
	 * to the test.tiff in sub-folder Data).
	 *
	 * @param <T> The wanted output type.
	 * @param relPath The relative path to the Tiff file.
	 * @return The file as ImgLib image.
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> loadTiffFromJar(String relPath) {
		InputStream is = TestImageAccessor.class.getResourceAsStream(relPath);
		BufferedInputStream bis = new BufferedInputStream(is);

		ImagePlus imp = opener.openTiff(bis, "The Test Image");
		assumeNotNull(imp);
		return ImagePlusAdapter.wrap(imp);
	}

	/**
	 * Creates a noisy image that is created by repeatedly adding points
	 * with random intensity to the canvas. That way it tries to mimic the
	 * way a microscope produces images. This convenience method uses the
	 * default values of a point size of 3.0 and produces 5000 points.
	 * After the creation the image is smoothed with a sigma of one in each
	 * direction.
	 *
	 * @param <T> The wanted output type.
	 * @param width The image width.
	 * @param height The image height.
	 * @return The noise image.
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> produceNoiseImageSmoothed(T type, int width, int height) {
		return produceNoiseImageSmoothed(type, width, height, 3.0f, 5000, new double[] {1.0,1.0});
	}

	/**
	 * Creates a noisy image that is created by repeatedly adding points
	 * with random intensity to the canvas. That way it tries to mimic the
	 * way a microscope produces images.
	 *
	 * @param <T> The wanted output type.
	 * @param width The image width.
	 * @param height The image height.
	 * @param dotSize The size of the dots.
	 * @param numDots The number of dots.
	 * @param smoothingSigma The two dimensional sigma for smoothing.
	 * @return The noise image.
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> produceNoiseImage(int width,
			int height, float dotSize, int numDots) {
		/* For now (probably until ImageJ2 is out) we use an
		 * ImageJ image to draw circles.
		 */
		int options = NewImage.FILL_BLACK + NewImage.CHECK_AVAILABLE_MEMORY;
	        ImagePlus img = NewImage.createByteImage("Noise", width, height, 1, options);
		ImageProcessor imp = img.getProcessor();

		float dotRadius = dotSize * 0.5f;
		int dotIntSize = (int) dotSize;

		for (int i=0; i < numDots; i++) {
			int x = (int) (Math.random() * width - dotRadius);
			int y = (int) (Math.random() * height - dotRadius);
			imp.setColor(Color.WHITE);
			imp.fillOval(x, y, dotIntSize, dotIntSize);
		}
		// we changed the data, so update it
		img.updateImage();
		// create the new image
		RandomAccessibleInterval<T> noiseImage = ImagePlusAdapter.wrap(img);

		return noiseImage;
	}

	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> produceNoiseImageSmoothed(T type, int width,
			int height, float dotSize, int numDots, double[] smoothingSigma) {
		RandomAccessibleInterval<T> noiseImage = produceNoiseImage(width, height, dotSize, numDots);

		return gaussianSmooth(noiseImage, smoothingSigma);
	}

	/**
	 * This method creates a noise image that has a specified mean.
	 * Every pixel has a value uniformly distributed around mean with
	 * the maximum spread specified.
	 *
	 * @return a new noise image
	 * @throws MissingPreconditionException if specified means and spreads are not valid
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> produceMeanBasedNoiseImage(T type, int width,
			int height, double mean, double spread, double[] smoothingSigma) throws MissingPreconditionException {
		if (mean < spread || (mean + spread) > type.getMaxValue()) {
			throw new MissingPreconditionException("Mean must be larger than spread, and mean plus spread must be smaller than max of the type");
		}
		// create the new image
		ImgFactory<T> imgFactory = new ArrayImgFactory<T>();
		RandomAccessibleInterval<T> noiseImage = imgFactory.create( new int[] {width, height}, type); // "Noise image");

		for (T value : Views.iterable(noiseImage)) {
			value.setReal( mean + ( (Math.random() - 0.5) * spread ) );
		}

		return gaussianSmooth(noiseImage, smoothingSigma);
	}

	/**
	 * This method creates a noise image that is made of many little
	 * sticks oriented in a random direction. How many of them and
	 * what the length of them are can be specified.
	 *
	 * @return a new noise image that is not smoothed
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> produceSticksNoiseImage(int width,
			int height, int numSticks, int lineWidth, double maxLength) {
		/* For now (probably until ImageJ2 is out) we use an
		 * ImageJ image to draw lines.
		 */
		int options = NewImage.FILL_BLACK + NewImage.CHECK_AVAILABLE_MEMORY;
	        ImagePlus img = NewImage.createByteImage("Noise", width, height, 1, options);
		ImageProcessor imp = img.getProcessor();
		imp.setColor(Color.WHITE);
		imp.setLineWidth(lineWidth);

		for (int i=0; i < numSticks; i++) {
			// find random starting point
			int x = (int) (Math.random() * width);
			int y = (int) (Math.random() * height);
			// create random stick length and direction
			double length = Math.random() * maxLength;
			double angle = Math.random() * 2 * Math.PI;
			// calculate random point on circle, for the direction
			int destX = x + (int) (length * Math.cos(angle));
			int destY = y + (int) (length * Math.sin(angle));
			// now draw the line
			imp.drawLine(x, y, destX, destY);
		}
		// we changed the data, so update it
		img.updateImage();

		return ImagePlusAdapter.wrap(img);
	}

	/**
	 * This method creates a smoothed noise image that is made of
	 * many little sticks oriented in a random direction. How many
	 * of them and what the length of them are can be specified.
	 *
	 * @return a new noise image that is smoothed
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> produceSticksNoiseImageSmoothed(T type, int width,
			int height, int numSticks, int lineWidth, double maxLength, double[] smoothingSigma) {

		RandomAccessibleInterval<T> noiseImage = produceSticksNoiseImage(width, height, numSticks, lineWidth, maxLength);

		return gaussianSmooth(noiseImage, smoothingSigma);
	}

	/**
	 * Generates a Perlin noise image. It is based on Ken Perlin's
	 * reference implementation (ImprovedNoise class) and a small
	 * bit of Kas Thomas' sample code (http://asserttrue.blogspot.com/).
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> producePerlinNoiseImage(T type, int width,
			int height, double z, double scale) {
		// create the new image
		ImgFactory<T> imgFactory = new ArrayImgFactory<T>();
		RandomAccessibleInterval<T> noiseImage = imgFactory.create( new int[] {width, height}, type);
		Cursor<T> noiseCursor = Views.iterable(noiseImage).localizingCursor();

		double xOffset = Math.random() * (width*width);
		double yOffset = Math.random() * (height*height);

		while (noiseCursor.hasNext()) {
			noiseCursor.fwd();
			double x = (noiseCursor.getDoublePosition(0) + xOffset) * scale;
			double y = (noiseCursor.getDoublePosition(1) + yOffset) * scale;

			float t = (float)ImprovedNoise.noise( x, y, z);

			// ImprovedNoise.noise returns a float in the range [-1..1],
			// whereas we want a float in the range [0..1], so:
                        t = (1 + t) * 0.5f;

                        noiseCursor.get().setReal(t);
		}

		//return gaussianSmooth(noiseImage, imgFactory, smoothingSigma);
		return noiseImage;
	}

	/**
	 * Gaussian Smooth of the input image using intermediate float format.
	 * @param <T>
	 * @param img
	 * @param sigma
	 * @return
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> gaussianSmooth(
			RandomAccessibleInterval<T> img, double[] sigma) {
		Interval interval = Views.iterable(img);

		ImgFactory<T> outputFactory = new ArrayImgFactory<T>();
		final long[] dim = new long[ img.numDimensions() ];
		img.dimensions(dim);
		RandomAccessibleInterval<T> output = outputFactory.create( dim,
				Util.getTypeFromRandomAccess(img).createVariable() );

		final long[] pos = new long[ img.numDimensions() ];
		Arrays.fill(pos, 0);
		Localizable origin = new Point(pos);

		ImgFactory<FloatType> tempFactory = new ArrayImgFactory<FloatType>();
		RandomAccessible<T> input = Views.extendMirrorSingle(img);
		Gauss.inFloat(sigma, input, interval, output, origin, tempFactory);

		return output;
	}

	/**
	 * Inverts an image.
	 *
	 * @param <T> The images data type.
	 * @param image The image to convert.
	 * @return The inverted image.
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> invertImage(
			RandomAccessibleInterval<T> image) {
		Cursor<T> imgCursor = Views.iterable(image).localizingCursor();
		// invert the image
		long[] dim = new long[ image.numDimensions() ];
		image.dimensions(dim);
		ArrayImgFactory<T> imgFactory = new ArrayImgFactory<T>();
		RandomAccessibleInterval<T> invImg = imgFactory.create(
				dim, Util.getTypeFromRandomAccess(image).createVariable() ); // "Inverted " + image.getName());
		RandomAccess<T> invCursor = invImg.randomAccess();

		while (imgCursor.hasNext()) {
			imgCursor.fwd();
			invCursor.setPosition(imgCursor);
			invCursor.get().setReal( imgCursor.get().getMaxValue() - imgCursor.get().getRealDouble() );
		}

		return invImg;
	}

	/**
	 * Converts an arbitrary image to a black/white version of it.
	 * All image data lower or equal 0.5 times the maximum value
	 * of the image type will get black, the rest will turn white.
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> makeBinaryImage(
			RandomAccessibleInterval<T> image) {
		T binSplitValue = Util.getTypeFromRandomAccess(image).createVariable();
		binSplitValue.setReal( binSplitValue.getMaxValue() * 0.5 );
		return TestImageAccessor.makeBinaryImage(image, binSplitValue);
	}

	/**
	 * Converts an arbitrary image to a black/white version of it.
	 * All image data lower or equal the splitValue will get black,
	 * the rest will turn white.
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> makeBinaryImage(
			RandomAccessibleInterval<T> image, T splitValue) {
		Cursor<T> imgCursor = Views.iterable(image).localizingCursor();
		// make a new image of the same type, but binary
		long[] dim = new long[ image.numDimensions() ];
		image.dimensions(dim);
		ArrayImgFactory<T> imgFactory = new ArrayImgFactory<T>();
		RandomAccessibleInterval<T> binImg = imgFactory.create( dim,
				Util.getTypeFromRandomAccess(image).createVariable() ); // "Binary image of " + image.getName());
		RandomAccess<T> invCursor = binImg.randomAccess();

		while (imgCursor.hasNext()) {
			imgCursor.fwd();
			invCursor.setPosition(imgCursor);
			T currentValue = invCursor.get();
			if (currentValue.compareTo(splitValue) > 0)
				currentValue.setReal(  currentValue.getMaxValue() );
			else
				currentValue.setZero();
		}

		return binImg;
	}

	/**
	 * A method to combine a foreground image and a background image.
	 * If data on the foreground image is above zero, it will be
	 * placed on the background. While doing that, the image data from
	 * the foreground is scaled to be in range of the background.
	 */
	public static <T extends RealType<T>> void combineImages(RandomAccessibleInterval<T> background,
			RandomAccessibleInterval<T> foreground) {
		final long[] dim = new long[ background.numDimensions() ];
		background.dimensions(dim);
		RandomAccessibleInterval<BitType> alwaysTrueMask = MaskFactory.createMask(dim, true);
		TwinCursor<T> cursor = new TwinCursor<T>(
				background.randomAccess(),
				foreground.randomAccess(),
				Views.iterable(alwaysTrueMask).localizingCursor());
		// find a scaling factor for scale forground range into background
		double bgMin = ImageStatistics.getImageMin(background).getRealDouble();
		double bgMax = ImageStatistics.getImageMax(background).getRealDouble();
		double fgMin = ImageStatistics.getImageMin(foreground).getRealDouble();
		double fgMax = ImageStatistics.getImageMax(foreground).getRealDouble();

		double scaling = (bgMax - bgMin ) / (fgMax - fgMin);
		// iterate over both images
		while (cursor.hasNext()) {
			cursor.fwd();
			T bgData = cursor.getFirst();
			double fgData = cursor.getSecond().getRealDouble() * scaling;
			if (fgData > 0.01) {
				/* if the foreground data is above zero, copy
				 * it to the background.
				 */
				bgData.setReal(fgData);
			}
		}
	}

	/**
	 * Creates a mask image with a black background and a white
	 * rectangular foreground.
	 *
	 * @param width The width of the result image.
	 * @param height The height of the result image.
	 * @param offset The offset of the rectangular mask.
	 * @param size The size of the rectangular mask.
	 * @return A black image with a white rectangle on it.
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> createRectengularMaskImage(
			long width, long height, long[] offset, long[] size) {
		/* For now (probably until ImageJ2 is out) we use an
		 * ImageJ image to draw lines.
		 */
		int options = NewImage.FILL_BLACK + NewImage.CHECK_AVAILABLE_MEMORY;
	        ImagePlus img = NewImage.createByteImage("Noise", (int)width, (int)height, 1, options);
		ImageProcessor imp = img.getProcessor();
		imp.setColor(Color.WHITE);
		Roi rect = new Roi(offset[0], offset[1], size[0], size[1]);

		imp.fill(rect);
		// we changed the data, so update it
		img.updateImage();

		return ImagePlusAdapter.wrap(img);
	}
}
