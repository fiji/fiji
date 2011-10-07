package algorithms;

import gadgets.DataContainer;
import gadgets.Statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution3;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import results.ResultHandler;

public class CostesSignificanceTest<T extends RealType<T>> extends Algorithm<T> {
	protected int psfRadiusInPixels[] = new int[3];
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
	public CostesSignificanceTest(PearsonsCorrelation<T> pc, int psfRadiusInPixels, int nrRandomizations) {
		this.pearsonsCorrelation = pc;
		this.psfRadiusInPixels[0] = psfRadiusInPixels;
		this.psfRadiusInPixels[1] = psfRadiusInPixels;
		this.psfRadiusInPixels[2] = psfRadiusInPixels;
		this.nrRandomizations = nrRandomizations;
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		/* Build a list of blocks that represent the images. To
		 * do so we create a list image ROI cursors. If a block
		 * does not fit into the image it will get a out-of-bounds
		 * strategy.
		 */

		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();
		/* We expect two images of the same size, so we only
		 * get dimensions from one image.
		 */
		int[] dimensions = img1.getDimensions();
		int nrDimensions = img1.getNumDimensions();

		// calculate the number of blocks per image
		int nrBlocksPerImage = 1;
		int[] nrBlocksPerDimension = new int[3];
		for (int i = 0; i < nrDimensions; i++) {
			// add the amount of full fitting blocks to the counter
			nrBlocksPerDimension[i] = dimensions[i] / psfRadiusInPixels[i];
			// if there is the need for a out-of-bounds block, increase count
			if ( dimensions[i] % psfRadiusInPixels[i] != 0 )
				nrBlocksPerDimension[i]++;
			// increase total count
			nrBlocksPerImage *= nrBlocksPerDimension[i];
		}

		// initialize block lists with correct size of blocks
		blocks = new ArrayList<RegionOfInterestCursor<T>>( nrBlocksPerImage );

		// generate the input blocks for shuffling
		OutOfBoundsStrategyFactory<T> oobFactory = new OutOfBoundsStrategyMirrorFactory<T>();
		generateBlocks( img1, blocks, oobFactory);

		/* Create a new image to contain the shuffled data and with
		 * same dimensions as the original data.
		 */
		Image<T> shuffledImage = img1.createNewImage(img1.getDimensions(), "Shuffled Image");

		/* create a list of output blocks for the shuffled image
		 * which will be used to write out the shuffled original
		 * blocks to the new image.
		 */
		outputBlocks = new ArrayList<RegionOfInterestCursor<T>>( nrBlocksPerImage );

		// generate the output blocks for writing data into a new image
		generateBlocks( shuffledImage, outputBlocks, new OutOfBoundsStrategyValueFactory<T>() );

		// make sure we have the same amount of input and output blocks
		assert(blocks.size() == outputBlocks.size());

		// create a double version of the PSF for the smoothing
		double[] psfRadius = new double[nrDimensions];
		for (int i = 0; i < nrDimensions; i++) {
			psfRadius[i] = (double) psfRadiusInPixels[i];
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
			if (container.isMaskInUse()) {
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
				// iterate over both blocks
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
						smootherOobFactory, typeConverterIn, typeConverterOut, psfRadius );
			// smooth the image
			if ( smoother.checkInput() && smoother.process() ) {
				smoothedShuffledImage = smoother.getResult();
			} else {
				throw new MissingPreconditionException( smoother.getErrorMessage() );
			}

			// allow the potential addition of a mask
			smoothedShuffledImage = container.maskImageIfNeeded( smoothedShuffledImage );

			try {
				// calculate correlation value...
				double pValue = pearsonsCorrelation.calculatePearsons( smoothedShuffledImage, img2);
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
					throw new MissingPreconditionException("Costes: Maximum retries have been made, but errors keep on coming: " + e.getMessage());
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
			OutOfBoundsStrategyFactory<T> outOfBoundsFactory) throws MissingPreconditionException {
		/* since we start at the first pixel, the offset in all
		 *  directions is zero.
		 */
		int offset[] = {0, 0, 0};
		// get the dimensions of the image
		int[] dimensions = img.getDimensions();
		// get the number of dimensions
		int nrDimensions = img.getNumDimensions();

		if (nrDimensions == 2)
		{ // for a 2D image...
			generateBlocksXY(img, blockList, offset, outOfBoundsFactory, false);
		}
		else if (nrDimensions == 3)
		{ // for a 3D image...
			int z;
			// go through the depth in steps of block depth
			for ( z = psfRadiusInPixels[2]; z <= dimensions[2]; z += psfRadiusInPixels[2] ) {
				offset[2] = z - psfRadiusInPixels[2];
				generateBlocksXY(img, blockList, offset, outOfBoundsFactory, false);
			}
			// check is we need to add a out of bounds strategy cursor
			if (z > dimensions[2]) {
				offset[2] = z - psfRadiusInPixels[2];
				generateBlocksXY(img, blockList, offset, outOfBoundsFactory, true);
			}
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
			int[] offset, OutOfBoundsStrategyFactory<T> outOfBoundsFactory, boolean forceOutOfBounds) {
		// get image height
		int height = img.getDimension(1);
		// go through the height in steps of block width
		int y;
		for ( y = psfRadiusInPixels[1]; y <= height; y += psfRadiusInPixels[1] ) {
			offset[1] = y - psfRadiusInPixels[1];
			generateBlocksX(img, blockList, offset, outOfBoundsFactory, forceOutOfBounds);
		}
		// check is we need to add a out of bounds strategy cursor
		if (y > height) {
			offset[1] = y - psfRadiusInPixels[1];
			generateBlocksX(img, blockList, offset, outOfBoundsFactory, true);
		}
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
			int[] offset, OutOfBoundsStrategyFactory<T> outOfBoundsFactory, boolean forceOutOfBounds) {
		// get image width
		int width = img.getDimension(0);
		// go through the width in steps of block width
		int x;
		for ( x = psfRadiusInPixels[0]; x <= width; x += psfRadiusInPixels[0] ) {
			offset[0] = x - psfRadiusInPixels[0];

			LocalizableByDimCursor<T> locCursor;
			if (forceOutOfBounds)
				locCursor = img.createLocalizableByDimCursor( outOfBoundsFactory );
			else
				locCursor = img.createLocalizableByDimCursor();

			RegionOfInterestCursor<T> roiCursor
				= locCursor.createRegionOfInterestCursor( offset, psfRadiusInPixels );
			blockList.add(roiCursor);
		}
		// check is we need to add a out of bounds strategy cursor
		if (x > width) {
			offset[0] = x - psfRadiusInPixels[0];
			LocalizableByDimCursor<T> locCursor
				= img.createLocalizableByDimCursor( outOfBoundsFactory );
			RegionOfInterestCursor<T> roiCursor
				= locCursor.createRegionOfInterestCursor( offset, psfRadiusInPixels );
			blockList.add(roiCursor);
		}
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
