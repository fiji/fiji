package tests;

import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.InputStream;

import algorithms.MissingPreconditionException;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution3;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.real.FloatType;

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
	public static <T extends RealType<T>> Image<T> produceNoiseImage(T type, int width, int height) {
		return produceNoiseImage(type, width, height, 3.0f, 5000, new double[] {1.0,1.0});
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
	public static <T extends RealType<T>> Image<T> produceNoiseImage(T type, int width,
			int height, float dotSize, int numDots, double[] smoothingSigma) {
		// create the new image
		ImageFactory<T> imgFactory = new ImageFactory<T>(type, new ArrayContainerFactory());
		Image<T> noiseImage = imgFactory.createImage( new int[] {width, height}, "Noise image");

		/* for now (probably until ImageJ2 is out) we must convert
		 * the ImgLib image to an ImageJ one to draw circles on it.
		 */
		ImagePlus img = ImageJFunctions.displayAsVirtualStack( noiseImage );
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
		return gaussianSmooth(noiseImage, imgFactory, smoothingSigma);
	}

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
	 * @return a new noise image
	 */
	public static <T extends RealType<T>> Image<T> produceSticksNoiseImage(T type, int width,
			int height, int numSticks, int lineWidth, double maxLength, double[] smoothingSigma) {
		// create the new image
		ImageFactory<T> imgFactory = new ImageFactory<T>(type, new ArrayContainerFactory());
		Image<T> noiseImage = imgFactory.createImage( new int[] {width, height}, "Noise image");

		/* for now (probably until ImageJ2 is out) we must convert
		 * the ImgLib image to an ImageJ one to draw circles on it.
		 */
		ImagePlus img = ImageJFunctions.copyToImagePlus( noiseImage );
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

		return gaussianSmooth(noiseImage, imgFactory, smoothingSigma);
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
}
