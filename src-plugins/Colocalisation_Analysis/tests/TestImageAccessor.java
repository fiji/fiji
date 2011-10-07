package tests;

import gadgets.MaskFactory;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.InputStream;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution3;
import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
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
	public static <T extends RealType<T>> Image<T> loadTiffFromJar(String relPath) {
		InputStream is = TestImageAccessor.class.getResourceAsStream(relPath);
		BufferedInputStream bis = new BufferedInputStream(is);

		ImagePlus imp = opener.openTiff(bis, "The Test Image");
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
	public static <T extends RealType<T>> Image<T> produceNoiseImageSmoothed(T type, int width, int height) {
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
	public static <T extends RealType<T>> Image<T> produceNoiseImage(int width,
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
		Image<T> noiseImage = ImagePlusAdapter.wrap(img);

		return noiseImage;
	}

	public static <T extends RealType<T>> Image<T> produceNoiseImageSmoothed(T type, int width,
			int height, float dotSize, int numDots, double[] smoothingSigma) {
		Image<T> noiseImage = produceNoiseImage(width, height, dotSize, numDots);
		ImageFactory<T> imgFactory = new ImageFactory<T>(type, new ArrayContainerFactory());

		return gaussianSmooth(noiseImage, imgFactory, smoothingSigma);
	}

	/**
	 * This method creates a noise image that has a specified mean.
	 * Every pixel has a value uniformly distributed around mean with
	 * the maximum spread specified.
	 *
	 * @return a new noise image
	 * @throws MissingPreconditionException if specified means and spreads are not valid
	 */
	public static <T extends RealType<T>> Image<T> produceMeanBasedNoiseImage(T type, int width,
			int height, double mean, double spread, double[] smoothingSigma) throws MissingPreconditionException {
		if (mean < spread || (mean + spread) > type.getMaxValue()) {
			throw new MissingPreconditionException("Mean must be larger than spread, and mean plus spread must be smaller than max of the type");
		}
		// create the new image
		ImageFactory<T> imgFactory = new ImageFactory<T>(type, new ArrayContainerFactory());
		Image<T> noiseImage = imgFactory.createImage( new int[] {width, height}, "Noise image");

		for (T value : noiseImage) {
			value.setReal( mean + ( (Math.random() - 0.5) * spread ) );
		}

		return gaussianSmooth(noiseImage, imgFactory, smoothingSigma);
	}

	/**
	 * This method creates a noise image that is made of many little
	 * sticks oriented in a random direction. How many of them and
	 * what the length of them are can be specified.
	 *
	 * @return a new noise image that is not smoothed
	 */
	public static <T extends RealType<T>> Image<T> produceSticksNoiseImage(int width,
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
	public static <T extends RealType<T>> Image<T> produceSticksNoiseImageSmoothed(T type, int width,
			int height, int numSticks, int lineWidth, double maxLength, double[] smoothingSigma) {

		Image<T> noiseImage = produceSticksNoiseImage(width, height, numSticks, lineWidth, maxLength);
		ImageFactory<T> imgFactory = new ImageFactory<T>(type, new ArrayContainerFactory());

		return gaussianSmooth(noiseImage, imgFactory, smoothingSigma);
	}

	/**
	 * Generates a Perlin noise image. It is based on Ken Perlin's
	 * reference implementation (ImprovedNoise class) and a small
	 * bit of Kas Thomas' sample code (http://asserttrue.blogspot.com/).
	 */
	public static <T extends RealType<T>> Image<T> producePerlinNoiseImage(T type, int width,
			int height, double z, double scale) {
		// create the new image
		ImageFactory<T> imgFactory = new ImageFactory<T>(type, new ArrayContainerFactory());
		Image<T> noiseImage = imgFactory.createImage( new int[] {width, height}, "Noise image");
		LocalizableCursor<T> noiseCursor = noiseImage.createLocalizableCursor();

		double xOffset = Math.random() * (width*width);
		double yOffset = Math.random() * (height*height);

		while (noiseCursor.hasNext()) {
			noiseCursor.fwd();
			double x = (noiseCursor.getPosition(0) + xOffset) * scale;
			double y = (noiseCursor.getPosition(1) + yOffset) * scale;

			float t = (float)ImprovedNoise.noise( x, y, z);

			// ImprovedNoise.noise returns a float in the range [-1..1],
			// whereas we want a float in the range [0..1], so:
                        t = (1 + t) * 0.5f;

                        noiseCursor.getType().setReal(t);
		}

		noiseCursor.close();

		//return gaussianSmooth(noiseImage, imgFactory, smoothingSigma);
		return noiseImage;
	}

	/**
	 * Gaussian Smooth of the input image using intermediate float format. An
	 * ArrayContainerFactory is used to produce new images.
	 *
	 * @return
	 */
	public static <T extends RealType<T>> Image<T> gaussianSmooth(Image<T> img, double[] sigma) {
		ImageFactory<T> imgFactory = new ImageFactory<T>(img.createType(), new ArrayContainerFactory());
		return gaussianSmooth(img, imgFactory, sigma);
	}

	/**
	 * Gaussian Smooth of the input image using intermediate float format.
	 * @param <T>
	 * @param img
	 * @param factory
	 * @param sigma
	 * @return
	 */
	public static <T extends RealType<T>> Image<T> gaussianSmooth(Image<T> img, ImageFactory<T> factory, double[] sigma) {
		// create a Gaussian smoothing algorithm
		ImageFactory<FloatType> imgFactoryProcess
			= new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		OutOfBoundsStrategyFactory<FloatType> smootherOobFactory
			= new OutOfBoundsStrategyMirrorFactory<FloatType>();
		Converter<T, FloatType> typeConverterIn = new RealTypeConverter<T, FloatType>();
		Converter<FloatType, T> typeConverterOut = new RealTypeConverter<FloatType, T>();
		GaussianConvolution3<T, FloatType, T> smoother
			= new GaussianConvolution3<T, FloatType, T>(img, imgFactoryProcess, factory,
					smootherOobFactory, typeConverterIn, typeConverterOut, sigma );

		// smooth the image
		if ( smoother.checkInput() && smoother.process() ) {
			return smoother.getResult();
		} else {
			throw new RuntimeException(smoother.getErrorMessage());
		}
	}

	/**
	 * Inverts an image.
	 *
	 * @param <T> The images data type.
	 * @param image The image to convert.
	 * @return The inverted image.
	 */
	public static <T extends RealType<T>> Image<T> invertImage(Image<T> image) {
		LocalizableCursor<T> imgCursor = image.createLocalizableCursor();
		// invert the image
		Image<T> invImg = image.createNewImage("Inverted " + image.getName());
		LocalizableByDimCursor<T> invCursor = invImg.createLocalizableByDimCursor();

		while (imgCursor.hasNext()) {
			imgCursor.fwd();
			invCursor.setPosition(imgCursor);
			invCursor.getType().setReal( imgCursor.getType().getMaxValue() - imgCursor.getType().getRealDouble() );
		}

		imgCursor.close();
		invCursor.close();

		return invImg;
	}

	/**
	 * Converts an arbitrary image to a black/white version of it.
	 * All image data lower or equal 0.5 times the maximum value
	 * of the image type will get black, the rest will turn white.
	 */
	public static <T extends RealType<T>> Image<T> makeBinaryImage(Image<T> image) {
		T binSplitValue = image.createType();
		binSplitValue.setReal( binSplitValue.getMaxValue() * 0.5 );
		return TestImageAccessor.makeBinaryImage(image, binSplitValue);
	}

	/**
	 * Converts an arbitrary image to a black/white version of it.
	 * All image data lower or equal the splitValue will get black,
	 * the rest will turn white.
	 */
	public static <T extends RealType<T>> Image<T> makeBinaryImage(Image<T> image, T splitValue) {
		LocalizableCursor<T> imgCursor = image.createLocalizableCursor();
		// make a new image of the same type, but binary
		Image<T> binImg = image.createNewImage("Binary image of " + image.getName());
		LocalizableByDimCursor<T> invCursor = binImg.createLocalizableByDimCursor();

		while (imgCursor.hasNext()) {
			imgCursor.fwd();
			invCursor.setPosition(imgCursor);
			T currentValue = invCursor.getType();
			if (currentValue.compareTo(splitValue) > 0)
				currentValue.setReal(  currentValue.getMaxValue() );
			else
				currentValue.setZero();
		}

		imgCursor.close();
		invCursor.close();

		return binImg;
	}

	/**
	 * A method to combine a foreground image and a background image.
	 * If data on the foreground image is above zero, it will be
	 * placed on the background. While doing that, the image data from
	 * the foreground is scaled to be in range of the background.
	 */
	public static <T extends RealType<T>> void combineImages(Image<T> background, Image<T> foreground) {
		Image<BitType> alwaysTrueMask = MaskFactory.createMask(
				background.getDimensions(), true);
		TwinCursor<T> cursor = new TwinCursor<T>(
				background.createLocalizableByDimCursor(),
				foreground.createLocalizableByDimCursor(),
				alwaysTrueMask.createLocalizableCursor());
		// find a scaling factor for scale forground range into background
		double bgMin = ImageStatistics.getImageMin(background).getRealDouble();
		double bgMax = ImageStatistics.getImageMax(background).getRealDouble();
		double fgMin = ImageStatistics.getImageMin(foreground).getRealDouble();
		double fgMax = ImageStatistics.getImageMax(foreground).getRealDouble();

		double scaling = (bgMax - bgMin ) / (fgMax - fgMin);
		// iterate over both images
		while (cursor.hasNext()) {
			cursor.fwd();
			T bgData = cursor.getChannel1();
			double fgData = cursor.getChannel2().getRealDouble() * scaling;
			if (fgData > 0.01) {
				/* if the foreground data is above zero, copy
				 * it to the background.
				 */
				bgData.setReal(fgData);
			}
		}
		cursor.close();
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
	public static <T extends RealType<T>> Image<T> createRectengularMaskImage(int width,
			int height, int[] offset, int[] size) {
		/* For now (probably until ImageJ2 is out) we use an
		 * ImageJ image to draw lines.
		 */
		int options = NewImage.FILL_BLACK + NewImage.CHECK_AVAILABLE_MEMORY;
	        ImagePlus img = NewImage.createByteImage("Noise", width, height, 1, options);
		ImageProcessor imp = img.getProcessor();
		imp.setColor(Color.WHITE);
		Roi rect = new Roi(offset[0], offset[1], size[0], size[1]);

		imp.fill(rect);
		// we changed the data, so update it
		img.updateImage();

		return ImagePlusAdapter.wrap(img);
	}
}
