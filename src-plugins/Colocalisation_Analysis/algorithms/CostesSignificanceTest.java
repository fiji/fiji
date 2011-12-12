package algorithms;

import gadgets.DataContainer;
import gadgets.DataContainer.MaskType;
import gadgets.Statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution3;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import results.ResultHandler;

public class CostesSignificanceTest<T extends RealType<T>> extends Algorithm<T> {
	// radius of the PSF in pixels, its size *must* for now be three
	protected int[] psfRadius = new int[3];
	// the lists of cursor blocks, representing the images
	List<RegionOfInterestCursor<T>> blocks, outputBlocks;
	// indicates if the shuffled images should be shown as a result
	boolean showShuffledImages = false;
	// the number of randomization tests
	int nrRandomizations;
	// the shuffled image last worked on
	Image<T> smoothedShuffledImage;
	// the Pearson's algorithm (that should have been run before)
	PearsonsCorrelation<T> pearsonsCorrelation;
	// a list of resulting Pearsons values from the randomized images
	List<Double> shuffledPearsonsResults;
	/* the amount of Pearson's values with shuffled data
	 * that has the value of the original one or is larger.
	 */
	int shuffledPearsonsNotLessOriginal = 0;
	// The mean of the shuffled Pearson values
	double shuffledMean = 0.0;
	// The standard derivation of the shuffled Pearson values
	double shuffledStdDerivation = 0.0;
	/* The Costes P-Value which is the probability that
	 * Pearsons r is different from the mean of the randomized
	 * r values.
	 */
	double costesPValue;
	// the maximum retries in case of Pearson numerical errors
	protected final int maxErrorRetries = 3;


	/**
	 * Creates a new Costes significance test object by using a
	 * cube block with the given edge length.
	 *
	 * @param psfRadiusInPixels The edge width of the 3D cube block.
	 */
	public CostesSignificanceTest(PearsonsCorrelation<T> pc, int psfRadiusInPixels,
			int nrRandomizations, boolean showShuffledImages) {
		super("Costes significance test");
		this.pearsonsCorrelation = pc;
		Arrays.fill(psfRadius, psfRadiusInPixels);
		this.nrRandomizations = nrRandomizations;
		this.showShuffledImages = showShuffledImages;
	}

	/**
	 * Builds a list of blocks that represent the images. To
	 * do so we create a list image ROI cursors. If a block
	 * does not fit into the image it will get a out-of-bounds
	 * strategy.
	 */
	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		final Image<T> img1 = container.getSourceImage1();
		final Image<T> img2 = container.getSourceImage2();
		final Image<BitType> mask = container.getMask();

		/* To determine the number of needed blocks, we need
		 * the effective dimensions of the image. Since the
		 * mask is responsible for this, we ask for its size.
		 */
		int[] dimensions = container.getMaskBBSize();
		int nrDimensions = dimensions.length;

		// calculate the needed number of blocks per image
		int nrBlocksPerImage = 1;
		int[] nrBlocksPerDimension = new int[3];
		for (int i = 0; i < nrDimensions; i++) {
			// add the amount of full fitting blocks to the counter
			nrBlocksPerDimension[i] = dimensions[i] / psfRadius[i];
			// if there is the need for a out-of-bounds block, increase count
			if ( dimensions[i] % psfRadius[i] != 0 )
				nrBlocksPerDimension[i]++;
			// increase total count
			nrBlocksPerImage *= nrBlocksPerDimension[i];
		}

		// initialize block lists with correct size of blocks
		blocks = new ArrayList<RegionOfInterestCursor<T>>( nrBlocksPerImage );

		// generate the input blocks for shuffling
		OutOfBoundsStrategyFactory<T> oobFactory =
				new OutOfBoundsStrategyMirrorFactory<T>();
		generateBlocks( img1, blocks, container.getMaskBBOffset(),
				dimensions, oobFactory);

		/* Create a new image to contain the shuffled data and with
		 * same dimensions as the original data.
		 */
		Image<T> shuffledImage = img1.createNewImage(
				img1.getDimensions(), "Shuffled Image");

		/* create a list of output blocks for the shuffled image
		 * which will be used to write out the shuffled original
		 * blocks to the new image.
		 */
		outputBlocks = new ArrayList<RegionOfInterestCursor<T>>(
				nrBlocksPerImage );

		// generate the output blocks for writing data into a new image
		generateBlocks( shuffledImage, outputBlocks, container.getMaskBBOffset(),
				dimensions, new OutOfBoundsStrategyValueFactory<T>() );

		// make sure we have the same amount of input and output blocks
		assert(blocks.size() == outputBlocks.size());

		// create a double version of the PSF for the smoothing
		double[] smoothingPsfRadius = new double[nrDimensions];
		for (int i = 0; i < nrDimensions; i++) {
			smoothingPsfRadius[i] = (double) psfRadius[i];
		}
		/* Create new type converters and image factories for ImgLib
		 * GaussionConvolution3. There is no need to construct them
		 * all the time from scratch.
		 * This is done because we want to make sure that the smoothing
		 * calculations are done with a floating point type.
		 */
		Converter<T, FloatType> typeConverterIn = new RealTypeConverter<T, FloatType>();
		Converter<FloatType, T> typeConverterOut = new RealTypeConverter<FloatType, T>();
		// TODO: Check if there are factories that are faster
		ImageFactory<FloatType> imageFactoryIn = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		ImageFactory<T> imageFactoryOut = img1.getImageFactory();
		/* Since we operate on a potentially different type while
		 * processing the smoothing, we need a second out of bounds
		 * strategy, too.
		 */
		OutOfBoundsStrategyFactory<FloatType> smootherOobFactory = new OutOfBoundsStrategyMirrorFactory<FloatType>();

		// the retry count for error cases
		int retries = 0;

		shuffledPearsonsResults = new ArrayList<Double>();
		for (int i=0; i < nrRandomizations; i++) {
			// shuffle the list
			Collections.shuffle( blocks );

			// check if a mask is in use and further actions are needed
			if (container.getMaskType() == MaskType.Irregular) {
				// black the whole intermediate image, just in case we have irr. masks
				for(int j=0; j < outputBlocks.size(); j++) {
					RegionOfInterestCursor<T> output = outputBlocks.get( j );
					// iterate over output blocks
					while (output.hasNext()) {
						output.fwd();
						// write black
						output.getType().setZero();
					}
					// reset the output cursor
					output.reset();
				}
			}

			// write out the shuffled input blocks into the output blocks
			for(int j=0; j < blocks.size(); j++) {
				RegionOfInterestCursor<T> input = blocks.get( j );
				RegionOfInterestCursor<T> output = outputBlocks.get( j );
				/* Iterate over both blocks. Theoretically the iteration
				 * order could be different. Because we are dealing with
				 * randomized data anyway, this is not a problem here.
				 */
				while (input.hasNext() && output.hasNext()) {
					input.fwd();
					output.fwd();
					// write the data
					output.getType().set( input.getType() );
				}
			}

			/* Reset all input and output cursors. If we
			 * would not do that, the image contents would
			 * not change on the next pass.
			 */
			for(RegionOfInterestCursor<T> c : blocks)
				c.reset();
			for(RegionOfInterestCursor<T> c : outputBlocks)
				c.reset();

			// create a Gaussian smoothing algorithm
			GaussianConvolution3<T, FloatType, T> smoother
				= new GaussianConvolution3<T, FloatType, T>(shuffledImage, imageFactoryIn, imageFactoryOut,
						smootherOobFactory, typeConverterIn, typeConverterOut, smoothingPsfRadius );
			// smooth the image
			if ( smoother.checkInput() && smoother.process() ) {
				smoothedShuffledImage = smoother.getResult();
			} else {
				throw new MissingPreconditionException( smoother.getErrorMessage() );
			}

			try {
				// calculate correlation value...
				double pValue = pearsonsCorrelation.calculatePearsons( smoothedShuffledImage, img2, mask);
				// ...and add it to the results list
				shuffledPearsonsResults.add( pValue );
			} catch (MissingPreconditionException e) {
				/* if the randomized input data does not suit due to numerical
				 * problems, try it three times again and then fail.
				 */
				if (retries < maxErrorRetries) {
					// increase retry count and the number of randomizations
					retries++;
					nrRandomizations++;
				} else {
					throw new MissingPreconditionException("Maximum retries have been made (" +
							+ retries + "), but errors keep on coming: " + e.getMessage(), e);
				}
			}
		}

		// close all input and output cursors
		for(RegionOfInterestCursor<T> c : blocks)
			c.close();
		for(RegionOfInterestCursor<T> c : outputBlocks)
			c.close();

		// calculate statistics on the randomized values and the original one
		double originalVal = pearsonsCorrelation.getPearsonsCorrelationValue();
		calculateStatistics(shuffledPearsonsResults, originalVal);
	}

	/**
	 * This method drives the creation of RegionOfInterest-Cursors on the given image.
	 * It does not matter if those generated blocks are used for reading and/or
	 * writing. The resulting blocks are put into the given list and are in the
	 * responsibility of the caller, i.e. he or she must make sure the cursors get
	 * closed on some point in time.
	 *
	 * @param img The image to create cursors on.
	 * @param blockList The list to put newly created cursors into
	 * @param outOfBoundsFactory Defines what to do if a block has parts out of image bounds.
	 */
	protected void generateBlocks(Image<T> img, List<RegionOfInterestCursor<T>> blockList,
			int[] offset, int[] size, OutOfBoundsStrategyFactory<T> outOfBoundsFactory)
			throws MissingPreconditionException {
		// get the number of dimensions
		int nrDimensions = img.getNumDimensions();
		if (nrDimensions == 2)
		{ // for a 2D image...
			generateBlocksXY(img, blockList, offset, size, outOfBoundsFactory, false);
		}
		else if (nrDimensions == 3)
		{ // for a 3D image...
			final int depth = size[2];
			int z;
			int originalZ = offset[2];
			// go through the depth in steps of block depth
			for ( z = psfRadius[2]; z <= depth; z += psfRadius[2] ) {
				offset[2] = originalZ + z - psfRadius[2];
				generateBlocksXY(img, blockList, offset, size, outOfBoundsFactory, false);
			}
			// check is we need to add a out of bounds strategy cursor
			if (z > depth) {
				offset[2] = originalZ + z - psfRadius[2];
				generateBlocksXY(img, blockList, offset, size, outOfBoundsFactory, true);
			}
			offset[2] = originalZ;
		}
		else
			throw new MissingPreconditionException("Currently only 2D and 3D images are supported.");
	}

	/**
	 * Goes stepwise through the y-dimensions of the image data and adds cursors
	 * for each row to the given list. The method does not check if there is a
	 * y-dimensions, so this should be made sure before. you can enforce to
	 * create all cursors as out-of-bounds one.
	 *
	 * @param img The image to get the data and cursors from.
	 * @param blockList The list to put the blocks into.
	 * @param offset The current offset configuration. Only [0] and [1] will be changed.
	 * @param outOfBoundsFactory The factory to create out-of-bounds-cursors with.
	 * @param forceOutOfBounds Indicates if all cursors created should be out-of-bounds ones.
	 */
	protected void generateBlocksXY(Image<T> img, List<RegionOfInterestCursor<T>> blockList,
			int[] offset, int[] size, OutOfBoundsStrategyFactory<T> outOfBoundsFactory,
			boolean forceOutOfBounds) {
		// potentially masked image height
		int height = size[1];
		final int originalY = offset[1];
		// go through the height in steps of block width
		int y;
		for ( y = psfRadius[1]; y <= height; y += psfRadius[1] ) {
			offset[1] = originalY + y - psfRadius[1];
			generateBlocksX(img, blockList, offset, size, outOfBoundsFactory, forceOutOfBounds);
		}
		// check is we need to add a out of bounds strategy cursor
		if (y > height) {
			offset[1] = originalY + y - psfRadius[1];
			generateBlocksX(img, blockList, offset, size, outOfBoundsFactory, true);
		}
		offset[1] = originalY;
	}

	/**
	 * Goes stepwise through a row of image data and adds cursors to the given list.
	 * If there is not enough image data for a whole block, an out-of-bounds cursor
	 * is generated. The creation of out-of-bound cursors could be enforced as well.
	 *
	 * @param img The image to get the data and cursors from.
	 * @param blockList The list to put the blocks into.
	 * @param offset The current offset configuration. Only [0] of it will be changed.
	 * @param outOfBoundsFactory The factory to create out-of-bounds-cursors with.
	 * @param forceOutOfBounds Indicates if all cursors created should be out-of-bounds ones.
	 */
	protected void generateBlocksX(Image<T> img, List<RegionOfInterestCursor<T>> blockList,
			int[] offset, int[] size, OutOfBoundsStrategyFactory<T> outOfBoundsFactory,
			boolean forceOutOfBounds) {
		// potentially masked image width
		int width = size[0];
		final int originalX = offset[0];
		// go through the width in steps of block width
		int x;
		for ( x = psfRadius[0]; x <= width; x += psfRadius[0] ) {
			offset[0] = originalX + x - psfRadius[0];

			LocalizableByDimCursor<T> locCursor;
			if (forceOutOfBounds)
				locCursor = img.createLocalizableByDimCursor( outOfBoundsFactory );
			else
				locCursor = img.createLocalizableByDimCursor();

			RegionOfInterestCursor<T> roiCursor
				= locCursor.createRegionOfInterestCursor( offset, psfRadius );
			blockList.add(roiCursor);
		}
		// check is we need to add a out of bounds strategy cursor
		if (x > width) {
			offset[0] = originalX + x - psfRadius[0];
			LocalizableByDimCursor<T> locCursor
				= img.createLocalizableByDimCursor( outOfBoundsFactory );
			RegionOfInterestCursor<T> roiCursor
				= locCursor.createRegionOfInterestCursor( offset, psfRadius );
			blockList.add(roiCursor);
		}
		offset[0] = originalX;
	}

	protected void calculateStatistics(List<Double> compareValues, double originalVal) {
		shuffledPearsonsNotLessOriginal = 0;
		int iterations = shuffledPearsonsResults.size();
		double compareSum = 0.0;

		for( Double shuffledVal : shuffledPearsonsResults ) {
			double diff = shuffledVal - originalVal;
			/* check if the randomized Pearsons value is equal
			 * or larger than the original one.
			 */
			if( diff > -0.00001 ) {
				shuffledPearsonsNotLessOriginal++;
			}
			compareSum += shuffledVal;
		}

		shuffledMean = compareSum / iterations;
		shuffledStdDerivation = Statistics.stdDeviation(compareValues);

		// get the quantile of the original value in the shuffle distribution
		costesPValue = Statistics.phi(originalVal, shuffledMean, shuffledStdDerivation);

		if (costesPValue > 1.0)
			costesPValue = 1.0;
		else if (costesPValue < 0.0)
			costesPValue = 0.0;
	}

	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		// if desired, show the last shuffled image available
		if ( showShuffledImages ) {
			smoothedShuffledImage.setName("Smoothed & shuffled channel 1");
			handler.handleImage( smoothedShuffledImage );
		}

		handler.handleValue("Costes P-Value", costesPValue, 2);
		handler.handleValue("Costes Shuffled Mean", shuffledMean, 2);
		handler.handleValue("Costes Shuffled Std.D.", shuffledStdDerivation, 2);

		/* give the ratio of results at least as large as the
		 * original value.
		 */
		double ratio = 0.0;
		if (shuffledPearsonsNotLessOriginal > 0) {
			ratio = (double)shuffledPearsonsResults.size() / (double)shuffledPearsonsNotLessOriginal;
		}
		handler.handleValue("Ratio of rand. Pearsons >= actual Pearsons value ", ratio, 2);
	}

	public double getCostesPValue() {
		return costesPValue;
	}

	public double getShuffledMean() {
		return shuffledMean;
	}

	public double getShuffledStdDerivation() {
		return shuffledStdDerivation;
	}

	public double getShuffledPearsonsNotLessOriginal() {
		return shuffledPearsonsNotLessOriginal;
	}
}
