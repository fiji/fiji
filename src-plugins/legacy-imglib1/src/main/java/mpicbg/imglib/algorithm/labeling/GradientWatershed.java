/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.labeling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.math.ComputeMinMax;
import mpicbg.imglib.algorithm.math.ImageConverter;
import mpicbg.imglib.algorithm.math.PickImagePeaks;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.labeling.LabelingType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

/**
 * This class labels an image where the objects in question have
 * edges that are defined by sharp intensity gradients and have
 * centers of high intensity and a low intensity background.
 * 
 * The algorithm:
 * Smooth the image by convolving with a Gaussian of sigma = sigma2.
 * Find and label local minima and maxima of the given scale (in pixels).
 * Label the minima with a single label.
 * Take the difference of Gaussians (DoG) - convolve the original image with
 * the kernel, G(sigma2) - G(sigma1). The original method uses the Sobel
 * transform of a smoothed image which is much the same.
 * Perform a seeded watershed using the minima/maxima labels on
 * the DoG image.
 * Remove the labeling from the pixels labeled as background.
 * 
 * The method is adapted from Wahlby & Bengtsson, Segmentation of
 * Cell Nuclei in Tissue by Combining Seeded Watersheds with Gradient
 * Information, Image Analysis: 13th Scandinavian Conference Proceedings,
 * pp 408-414, 2003
 *  
 * @param <T> The type of the image.
 * @param <L> The type of the labeling, typically Integer
 *
 */
public class GradientWatershed<T extends RealType<T>, L extends Comparable<L>>
implements OutputAlgorithm<LabelingType<L>>
{
	protected Image<T> input;
	protected Labeling<L> output;
	protected double [] scale;
	protected double [] sigma1;
	protected double [] sigma2;
	protected Iterator<L> names;
	protected double minBackgroundPeakHeight = 0;
	protected double minForegroundPeakHeight = 0;
	protected boolean wantsToQuantize = true;
	protected int numQuanta = 100;
	protected int [][] structuringElement;
	protected String error_message;
	protected ImageFactory<LabelingType<L>> labelingFactory;
	protected Image<FloatType> floatImage;
	protected ImageFactory<FloatType> floatFactory;
	/**
	 * Constructor
	 * 
	 * @param image The intensity image to be labeled
	 * @param scale the minimum distance between maxima of objects. Less
	 * technically, this should be the diameter of the smallest object. 
	 * @param sigma1 the standard deviation for the larger smoothing. The
	 * difference between sigma1 and sigma2 should be roughly the width
	 * of the desired edge in the DoG image. A larger difference will obscure
	 * small, faint edges.
	 * @param sigma2 the standard deviation for the smaller smoothing. This
	 * should be on the order of the largest insignificant feature in the image.
	 * @param names - an iterator that generates names of type L for the labels.
	 * The iterator will waste the last name taken on the background label.
	 * You can use AllConnectedComponents.getIntegerNames() as your name
	 * generator if you don't care about names.
	 */
	public GradientWatershed(
			Image<T> input,
			double [] scale,
			double [] sigma1,
			double [] sigma2,
			Iterator<L> names) {
		this.input = input;
		this.scale = scale;
		this.sigma1 = sigma1;
		this.sigma2 = sigma2;
		structuringElement = AllConnectedComponents.getStructuringElement(input.getNumDimensions());
		this.names = names;
		labelingFactory = new ImageFactory<LabelingType<L>>(
				new LabelingType<L>(), input.getContainerFactory());
	}
	
	/**
	 * Get the current structuring element. The structuring element is an
	 * array of coordinate tuple arrays where each coordinate is the offset
	 * to a component to connect using the watershed algorithm.
	 * 
	 * @return the structuring element that defines connectivity
	 */
	public int [][] getStructuringElement() {
		return cloneStructuringElement(structuringElement);
	}
	
	static protected int [][] cloneStructuringElement(int [][] structuringElement) {
		int [][] result = structuringElement.clone();
		for (int i=0; i<result.length; i++)
			result[i] = result[i].clone();
		return result;
	}
	
	/**
	 * Set the current structuring element. 
	 * See {@link AllConnectedComponents.getStructuringElement} for a
	 * discussion of the structuring element format. The structuring element
	 * determines which pixels are considered neighbors of a newly-added
	 * pixel in the watershed algorithm.
	 * 
	 * By default, an 8-connected (neighbors + diagonals) or its N-dimensional
	 * analog structuring element is used.
	 * @param structuringElement - the new structuring element.
	 */
	public void setStructuringElement(int [][] structuringElement) {
		this.structuringElement = cloneStructuringElement(structuringElement);
	}
	/**
	 * Return the scale of the objects to be found. This should be the
	 * diameter of a typical object. The scale is a N-dimensional vector
	 * to take into account possible differences in axis scaling.
	 * @return - the scale
	 */
	public double [] getScale() {
		return scale.clone();
	}
	/**
	 * Set the scale of the objects to be found. This should be the diameter
	 * of a typical object.
	 * @param scale
	 */
	public void setScale(double [] scale) {
		this.scale = scale.clone();
	}
	/**
	 * The algorithm finds both foreground (local maxima) and
	 * background peaks (local minima) to act as seeds for the watershed. 
	 * This can result in false peaks in areas where there is little
	 * difference in intensity. The MinBackgroundPeakHeight is the minimum difference
	 * in intensity between the intensity at the peak and the intensity
	 * of the strongest signal within 1/2 of the scale of the peak. 
	 * The algorithm will ignore peaks if the intensity difference is
	 * less than this minimum value.
	 * @param height - the minimum intensity difference between the peak
	 * and the highest intensity pixel in its neighborhood
	 */
	public void setMinBackgroundPeakHeight(double height) {
		minBackgroundPeakHeight = height;
	}
	
	/**
	 * See setMinBackgroundPeakHeight
	 * @return the current minimum background peak height.
	 */
	public double getMinBackgroundPeakHeight() {
		return minBackgroundPeakHeight;
	}
	/**
	 * The algorithm finds both foreground (local maxima) and
	 * background peaks (local minima) to act as seeds for the watershed. 
	 * This can result in false peaks in areas where there is little
	 * difference in intensity. The MinForegroundPeakHeight is the minimum difference
	 * in intensity between the intensity at the peak and the intensity
	 * of the weakest signal within 1/2 of the scale of the peak. 
	 * The algorithm will ignore peaks if the intensity difference is
	 * less than this minimum value.
	 * @param height - the minimum intensity difference between the peak
	 * and the lowest intensity pixel in its neighborhood
	 */
	public void setMinForegroundPeakHeight(double height) {
		minForegroundPeakHeight = height;
	}
	
	/**
	 * See setMinForegroundPeakHeight
	 * @return the current minimum background peak height.
	 */
	public double getMinForegroundPeakHeight() {
		return minForegroundPeakHeight;
	}
	
	/**
	 * The watershed repetitively evaluates the lowest intensity unlabeled pixel
	 * starting from the seeds. If two pixels have identical intensities,
	 * it picks the first one that was put on the list of pixels to process.
	 * The effect of this is to propagate out from the seeds in a consistent
	 * and predictable manner in regions that have identical intensities.
	 * 
	 * This setting enables or disables quantizing of the gradient image.
	 * Quantizing divides the intensities in the image into a fixed number
	 * of levels so that pixels of roughly the same intensity generally
	 * have the same value and the algorithm will propagate the seeds in
	 * those regions rather than rely on small and insignificant gradient
	 * differences to direct the watershed operation.
	 * 
	 * @param enable - true to enable quantization, false to disable.
	 */
	public void enableQuantization(boolean enable) {
		wantsToQuantize = enable;
	}
	
	/**
	 * see enableQuantization
	 * @return
	 */
	public boolean isQuantized() {
		return wantsToQuantize;
	}
	/**
	 * See enableQuantization
	 * You must enableQuantization as well as set the number of quanta.
	 * @param numQuanta - the number of quanta in the quantization
	 */
	public void setNumQuanta(int numQuanta) {
		this.numQuanta = numQuanta;
	}
	/**
	 * Set the image factory to use when creating the Labeling.
	 * 
	 * By default, we use an image factory that has the same container
	 * factory as the input image, but you can customize by supplying
	 * an alternate factory.
	 * 
	 * @param factory - a customized factory for generating the labeling
	 * containers.
	 */
	public void setOutputImageFactory(ImageFactory<LabelingType<L>> factory) {
		labelingFactory = factory;
	}
	
	public ImageFactory<LabelingType<L>> getOutputImageFactory() {
		return labelingFactory;
	}
	
	/**
	 * Supply a labeling that will be filled with labelings by the image.
	 * 
	 * This method allows the caller to reuse a labeling container
	 * rather than create one from scratch for each use.
	 * @param labeling - a labeling container that will hold the labeling
	 * of the image after process() is called.
	 */
	public void setOutputLabeling(Labeling<L> labeling) {
		output = labeling;
	}
	/**
	 * See setNumQuanta
	 * @return
	 */
	public int getNumQuanta() {
		return this.numQuanta;
	}
	@Override
	public boolean checkInput() {
		if (error_message.length() > 0) return false;
		if (input == null) {
			error_message = "The input image is null.";
			return false;
		}
		if (scale == null) {
			error_message = "The scale is null.";
			return false;
		}
		if (sigma1 == null) {
			error_message = "The first smoothing standard deviation (sigma1) is null.";
			return false;
		}
		if (sigma2 == null) {
			error_message = "The second smoothing standard deviation (sigma2) is null.";
			return false;
		}
		if (structuringElement == null) {
			error_message = "The structuring element is null.";
			return false;
		}
		if (names == null) {
			error_message = "The names iterator is null.";
		}
		if (!checkDimensions(scale)) {
			error_message = "The dimensions of the scale do not match those of the image";
			return false;
		}
		if (!checkDimensions(sigma1)) {
			error_message = "The dimensions of sigma1 do not match those of the image";
			return false;
		}
		if (!checkDimensions(sigma2)) {
			error_message = "The dimensions of sigma2 do not match those of the image";
			return false;
		}
		for (int [] coord:structuringElement) {
			if (coord == null) {
				error_message = "One of the coordinates of the structuring element is null.";
				return false;
			}
			if (! checkDimensions(coord)) {
				error_message = "The dimensions of one of the coordinates of the structuring element does not match those of the image.";
				return false;
			}
		}
		if (wantsToQuantize && (numQuanta < 2)) {
			error_message = String.format("The number of quanta is %d, but must be at least 2 (and ideally > 20).", numQuanta);
			return false;
		}
		for (int i=0;i<sigma1.length; i++) {
			if (sigma1[i] <= sigma2[i]) {
				error_message = String.format("All values of sigma1 should be greater than sigma2. For dimension %d, sigma1=%f, sigma2=%f", i, sigma1[i], sigma2[i]);
				return false;
			}
		}
		if (output != null) {
			int [] dimensions = output.getDimensions();
			if (! checkDimensions(dimensions)) {
				error_message = "The labeling container does not have the correct number of dimensions";
				return false;
			}
			for (int i=0; i<dimensions.length; i++) {
				if (dimensions[i] != input.getDimension(i)) {
					error_message = String.format(
							"The labeling container is not the same size as the image: dimension %d, labeling = %d, image = %d",
							i, dimensions[i], input.getDimension(i));
					return false;
				}
			}
		}
		return true;
	}

	protected boolean checkDimensions(int [] array) {
		return array.length == input.getNumDimensions();
	}
	protected boolean checkDimensions(double [] array) {
		return array.length == input.getNumDimensions();
	}
	@Override
	public boolean process() {
		floatImage = null;
		if (output == null) {
			output = new Labeling<L>(labelingFactory, input.getDimensions(), null);
		} else {
			/*
			 * Initialize the output to all background
			 */
			LocalizableCursor<LabelingType<L>> c = output.createLocalizableCursor();
			List<L> background = c.getType().intern(new ArrayList<L>());
			for (LabelingType<L> t:c) {
				t.setLabeling(background);
			}
			c.close();
		}
		/*
		 * Get the smoothed image.
		 */
		Image<FloatType> kernel = FourierConvolution.createGaussianKernel(
				input.getContainerFactory(), scale);
		FourierConvolution<FloatType, FloatType> convolution = 
			new FourierConvolution<FloatType, FloatType>(getFloatImage(), kernel);
		if (! convolution.process()) return false;
		Image<FloatType> smoothed = convolution.getResult();
		
		/*
		 * Find the local maxima and label them individually.
		 */
		PickImagePeaks<FloatType> peakPicker = new PickImagePeaks<FloatType>(smoothed);
		peakPicker.setSuppression(scale);
		peakPicker.process();
		Labeling<L> seeds = output.createNewLabeling();
		LocalizableByDimCursor<LabelingType<L>> lc = 
			seeds.createLocalizableByDimCursor();
		LocalizableByDimCursor<FloatType> imageCursor = smoothed.createLocalizableByDimCursor();
		int [] dimensions = input.getDimensions();
		for (int[] peak:peakPicker.getPeakList()) {
			if (! filterPeak(imageCursor, peak, dimensions,  false))
				continue;
			lc.setPosition(peak);
			lc.getType().setLabel(names.next());
		}
		imageCursor.close();
		/*
		 * Find the local minima and label them all the same.
		 */
		List<L> background = lc.getType().intern(names.next());
		Converter<FloatType, FloatType> invert = new Converter<FloatType,FloatType>() {

			@Override
			public void convert(FloatType input, FloatType output) {
				output.setReal(-input.getRealFloat());
			}
		};
		ImageConverter<FloatType, FloatType> invSmoothed = 
			new ImageConverter<FloatType, FloatType>(smoothed, smoothed, invert);
		invSmoothed.process();
		peakPicker = new PickImagePeaks<FloatType>(smoothed);
		peakPicker.setSuppression(scale);
		peakPicker.process();
		imageCursor = smoothed.createLocalizableByDimCursor();
		for (int [] peak: peakPicker.getPeakList()){
			if (! filterPeak(imageCursor, peak, dimensions, true))
				continue;
			lc.setPosition(peak);
			lc.getType().setLabeling(background);
		}
		lc.close();
		imageCursor.close();
		smoothed = null;
		invSmoothed = null;
		Image<FloatType> gradientImage = getGradientImage();
		if (gradientImage == null) return false;
		/*
		 * Run the seeded watershed on the image.
		 */
		Watershed.seededWatershed(gradientImage, seeds, structuringElement, output);
		return true;
	}
	
	protected ImageFactory<FloatType> getFloatFactory() {
		if (floatFactory == null) {
			floatFactory = new ImageFactory<FloatType>(
					new FloatType(), input.getContainerFactory());
		}
		return floatFactory;
	}

	protected Image<FloatType> getFloatImage() {
		if (floatImage == null) {
			ImageConverter<T, FloatType> convertToFloat =
				new ImageConverter<T, FloatType>(
						input, getFloatFactory(),
						new RealTypeConverter<T, FloatType>());
			if (! convertToFloat.process()) return null;
			floatImage = convertToFloat.getResult();
		}
		return floatImage;
	}
	@Override
	public String getErrorMessage() {
		return error_message;
	}

	@Override
	public Image<LabelingType<L>> getResult() {
		// TODO Auto-generated method stub
		return output;
	}
	protected boolean filterPeak(
			LocalizableByDimCursor<FloatType> imageCursor, 
			int [] peak, 
			int [] dimensions,
			boolean find_minimum) {
		double limit = minForegroundPeakHeight;
		if (find_minimum) limit = minBackgroundPeakHeight;
		int [] offset = new int [peak.length];
		int [] size = new int [peak.length];
		int [] position = new int [peak.length];
		for (int i=0; i<scale.length; i++) {
			int iscale = (int)(scale[i] / 2) * 2 + 1;
			offset[i] = peak[i] - iscale / 2;
			size[i] = iscale;
			if (offset[i] < 0) {
				size[i] += offset[i];
				offset[i] = 0;
			}
			if (offset[i] + size[i] >= dimensions[i]) {
				size[i] = dimensions[i] - offset[i] - 1;
			}
		}
		imageCursor.setPosition(peak);
		float valueAtPeak = imageCursor.getType().get();
		RegionOfInterestCursor<FloatType> rc = new RegionOfInterestCursor<FloatType>(imageCursor, offset, size);
		for (@SuppressWarnings("unused") FloatType t:rc) {
			double distanceSquared = 0;
			rc.getPosition(position);
			for (int i=0; i<position.length; i++) {
				double dPosition = (double)(position[i] + offset[i] - peak[i]) / scale[i];
				distanceSquared += dPosition * dPosition;
			}
			if (distanceSquared <= 1.0) {
				if (find_minimum) {
					if (rc.getType().get() > valueAtPeak + limit) {
						return true;
					}
				} else if (rc.getType().get() < valueAtPeak - limit) {
					return true;
				}
			}
		}
		System.err.format("Filtered at %d, %d\n", peak[0], peak[1]);
		rc.close();
		return false;
	}
	
	/**
	 * Return a difference of gaussian image that measures the gradient
	 * at a scale defined by the two sigmas of the gaussians.
	 * @param image
	 * @param sigma1
	 * @param sigma2
	 * @return
	 */
	public Image<FloatType> getGradientImage() {
		/*
		 * Create the DoG kernel.
		 */
		double [][] kernels1d1 = new double[input.getNumDimensions()][];
		double [][] kernels1d2 = new double[input.getNumDimensions()][];
		int [] kernelDimensions = input.createPositionArray();
		int [] offset = input.createPositionArray();
		for (int i=0; i<kernels1d1.length; i++) {
			kernels1d1[i] = Util.createGaussianKernel1DDouble(sigma1[i], true);
			kernels1d2[i] = Util.createGaussianKernel1DDouble(sigma2[i], true);
			kernelDimensions[i] = kernels1d1[i].length;
			offset[i] = (kernels1d1[i].length - kernels1d2[i].length) / 2;
		}
		Image<FloatType> kernel = getFloatFactory().createImage(kernelDimensions);
		LocalizableCursor<FloatType> kc = kernel.createLocalizableCursor();
		int [] position = input.createPositionArray();
		for (FloatType t:kc) {
			kc.getPosition(position);
			double value1 = 1;
			double value2 = 1;
			for (int i=0; i<kernels1d1.length; i++) {
				value1 *= kernels1d1[i][position[i]];
				int position2 = position[i] - offset[i];
				if ((position2 >= 0) && (position2 < kernels1d2[i].length)) {
					value2 *= kernels1d2[i][position2];
				} else {
					value2 = 0;
				}
			}
			t.setReal(value1 - value2);
		}
		kc.close();
		/*
		 * Apply the kernel to the image.
		 */
		FourierConvolution<FloatType, FloatType> convolution = 
			new FourierConvolution<FloatType, FloatType>(getFloatImage(), kernel);
		if (! convolution.process()) return null;
		Image<FloatType> result = convolution.getResult();
		/*
		 * Quantize the image.
		 */
		ComputeMinMax<FloatType> computeMinMax = new ComputeMinMax<FloatType>(result);
		computeMinMax.process();
		final float min = computeMinMax.getMin().get();
		final float max = computeMinMax.getMax().get();
		if (max == min) return result;
		
		ImageConverter<FloatType, FloatType> quantizer = 
			new ImageConverter<FloatType, FloatType>(
					result, result.getImageFactory(),
					new Converter<FloatType,FloatType>() {

						@Override
						public void convert(FloatType input, FloatType output) {
							float value = (input.get() - min) / (max - min);
							value = Math.round(value * 100);
							output.set(value);
						}
					});
		quantizer.process();
		return quantizer.getResult();
	}

}
