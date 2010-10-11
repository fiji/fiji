
import java.util.Arrays;

import ij.IJ;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;

import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;

public class StochasticDenoise<T extends NumericType<T>> {

	private int   numSamples;
	private double minLogProb;
	private double variance;

	private int[] dimensions;
	private int   numDimensions;
	private int   numNeighbors;
	private int   numChannels;

	private double[] neighborProbs; // thread local
	private double[] neighborLogProbs; // thread local
	int             localNeighborIndex; // thread local
	int             localInitialValueIndex; // thread local
	int             localLastValueIndex; // thread local

	private final int MaxPathLength = Integer.MAX_VALUE;

	// buffer to accumulate the pixel values
	private double[][] denoiseBuffer;
	// buffer that contains the source image data (gray scale or rgb)
	private int[][]   sourceBuffer;
	// local window buffer of the source image for faster access
	private int[][]   localSourceBuffer; // thread local
	private int       localMisses; // thread local
	private int       windowWidth;
	private int       windowSize;
	private int[]     windowDimensions;
	private int[]     windowOffsets;
	private int[][]   windowRelativePositions;

	// index offsets of neighbor pixels within the local window
	private int[]     localNeighborOffsets;
	private int[][]   localNeighborRelativePositions;

	private double[] probabilities;
	private double[] logProbabilities;
	private int maxDistance2;
	private int numMisses = 0;
	private int numAccess = 0;

	private final boolean debug = false;

	private class OffsetsPostions {

		public int[]   offsets;
		public int[][] positions;
	}

	/**
	 * Set all parameters of the algorithm.
	 *
	 * @param numSamples The number of random walks for each pixel
	 * @param minProb    The minimal allowed probability of a path
	 * @param sigma      The standard deviation for the probability function
	 */
	public final void setParameters(int numSamples, double minProb, double sigma) {

		this.numSamples = numSamples;
		this.minLogProb = Math.log(minProb);
		this.variance   = Math.pow(sigma, 2);
	}

	/**
	 * Run the algorithm.
	 *
	 * @param image    The noisy input image
	 * @param denoised The denoised ouput image
	 */
	public final void process(Image<T> image, Image<T> denoised) {

		dimensions        = image.getDimensions();
		numDimensions     = dimensions.length;
		numNeighbors      = (int)Math.pow(3, numDimensions) - 1;
		neighborProbs     = new double[numNeighbors];
		neighborLogProbs  = new double[numNeighbors];
		// TODO: find this value automatically
		windowWidth       = 9;
		windowSize        = (int)Math.pow(windowWidth, numDimensions);
		windowDimensions  = new int[numDimensions];
		Arrays.fill(windowDimensions, windowWidth);

		// create image cursors
		LocalizableByDimCursor<T> sourceCursor = image.createLocalizableByDimCursor();
		LocalizableByDimCursor<T> targetCursor = denoised.createLocalizableByDimCursor();

		// setup buffers
		if (sourceCursor.getType() instanceof RealType<?>) {

			IJ.log("Image is a gray scale image");

			// allocate denoise and source buffer
			numChannels       = 1;
			denoiseBuffer     = new double[image.size()][numChannels];
			sourceBuffer      = new int[image.size()][numChannels];
			localSourceBuffer = new int[windowSize][numChannels];

			// copy image data to source buffer
			while (sourceCursor.hasNext()) {
				sourceCursor.fwd();
				sourceBuffer[sourceCursor.getArrayIndex()][0] =
					(int)((RealType<?>)sourceCursor.getType()).getRealFloat();
			}
			// TODO: handle more that 8-bit images
			maxDistance2  = 255*255;
		}
		else if (RGBALegacyType.class.isInstance(sourceCursor.getType())) {

			IJ.log("Image is an RGBA image");

			// allocate denoise and source buffer
			numChannels       = 3;
			denoiseBuffer     = new double[image.size()][numChannels];
			sourceBuffer      = new int[image.size()][numChannels];
			localSourceBuffer = new int[windowSize][numChannels];

			// copy image data to source buffer
			while (sourceCursor.hasNext()) {
				sourceCursor.fwd();
				int value = RGBALegacyType.class.cast(sourceCursor.getType()).get();
				sourceBuffer[sourceCursor.getArrayIndex()][0] = RGBALegacyType.red(value);
				sourceBuffer[sourceCursor.getArrayIndex()][1] = RGBALegacyType.green(value);
				sourceBuffer[sourceCursor.getArrayIndex()][2] = RGBALegacyType.blue(value);
			}
			// TODO: handle more that 8-bit images
			maxDistance2  = (255*255)*3;
		} else {

			IJ.log("Image type " + sourceCursor.getType().getClass() + " not supported!");
			return;
		}

		// create neighbor offsets within local window
		OffsetsPostions localNeighborhood = new OffsetsPostions();
		createIndexOffsets(windowDimensions, 3, false, localNeighborhood);
		localNeighborOffsets           = localNeighborhood.offsets;
		localNeighborRelativePositions = localNeighborhood.positions;

		// create window offsets within source image
		OffsetsPostions windowNeighborhood = new OffsetsPostions();
		createIndexOffsets(dimensions, windowWidth, true, windowNeighborhood);
		windowOffsets           = windowNeighborhood.offsets;
		windowRelativePositions = windowNeighborhood.positions;

		if (debug) {
			for (int i = 0; i < numNeighbors; i++) {
				System.out.print(localNeighborOffsets[i]);
				System.out.print("\t\t");
				for (int d = 0; d < numDimensions; d++)
					System.out.print(" " + localNeighborRelativePositions[i][d]);
				System.out.println();
			}
			System.out.println();
			System.out.println();
			for (int i = 0; i < windowSize; i++) {
				System.out.println(windowOffsets[i]);
				System.out.print("\t\t");
				for (int d = 0; d < numDimensions; d++)
					System.out.print(" " + windowRelativePositions[i][d]);
				System.out.println();
			}
			System.out.println();
			System.out.println();
		}

		// allocate hash table for probabilities
		probabilities    = new double[maxDistance2 + 1];
		logProbabilities = new double[maxDistance2 + 1];
		Arrays.fill(probabilities, -1.0);

		int sourceIndex;
		int targetIndex;

		if (debug) {
			numSamples  = 1;
			sourceCursor.setPosition(new int[]{51, 164});
			while(targetCursor.hasNext()) {
				targetCursor.fwd();
				targetCursor.getType().setZero();
			}
			System.out.println("Starting denoising of pixel " + sourceCursor.getArrayIndex());
		}

		int[] sourcePosition = new int[numDimensions];

		IJ.showProgress(0, image.size());
		if (!debug)
			sourceCursor.reset();

		// iterate over all pixels
		while (sourceCursor.hasNext()) {
			sourceCursor.fwd();

			sourceIndex = sourceCursor.getArrayIndex();
			targetIndex = sourceIndex;

			// get source position
			sourceCursor.getPosition(sourcePosition);

			// perform the random walks and accumulate the target pixel values
			double sumWeights = 0.0;
			for (int m = 0; m < numSamples; m++)
				sumWeights += randomWalk(sourceIndex, sourcePosition, targetIndex);

			// normalize the target pixel value
			for (int c = 0; c < numChannels; c++)
				denoiseBuffer[targetIndex][c] /= sumWeights;

			if (debug) {
				System.out.println("done with pixel " + targetIndex);
				System.out.print("accum. value : ");
				for (int c = 0; c < numChannels; c++)
					System.out.print(" " + denoiseBuffer[targetIndex][c]*sumWeights);
				System.out.println();
				System.out.println("total weights: " + sumWeights);
				System.out.println("final value  : ");
				for (int c = 0; c < numChannels; c++)
					System.out.print(" " + denoiseBuffer[targetIndex][c]);
				System.out.println();

				break;
			}

			if (sourceIndex % 500 == 0)
				IJ.showProgress(sourceIndex, image.size());
		}
		IJ.showProgress(1.0);

		// copy content of denoise buffer to target
		targetCursor.reset();
		while (targetCursor.hasNext()) {
			targetCursor.fwd();

			targetIndex = targetCursor.getArrayIndex();

			if (targetCursor.getType() instanceof RealType<?>)
				((RealType<?>)targetCursor.getType()).setReal(denoiseBuffer[targetIndex][0]);
			if (RGBALegacyType.class.isInstance(targetCursor.getType()))
				RGBALegacyType.class.cast(targetCursor.getType()).set(
					RGBALegacyType.rgba(denoiseBuffer[targetIndex][0],
				                        denoiseBuffer[targetIndex][1],
				                        denoiseBuffer[targetIndex][2],
				                        255.0));
		}

		IJ.log("percentage of cache hits for probabilities: " + (double)(numAccess - numMisses)/numAccess);
		IJ.log("size of hash table of probabilities       : " + probabilities.length);
		IJ.log("number of misses in local window          : " + localMisses);
	}

	private final void createIndexOffsets(int[] dimensions, int width, boolean includeCenter,
	                                      OffsetsPostions op) {

		int[] dimensionOffset = new int[numDimensions];
		for (int d = 0; d < numDimensions; d++) {
			int offset = 1;
			for (int e = 0; e < d; e++)
				offset *= dimensions[e];
			dimensionOffset[d] = offset;
		}

		int numOffsets = (int)Math.pow(width, numDimensions);

		int[]   tmpOffsets           = new int[numOffsets];
		int[][] tmpRelativePositions = new int[numOffsets][numDimensions];

		Arrays.fill(tmpRelativePositions[0], -width/2);

		int index = 0;

		for (int d = 0; d < numDimensions; d++)
			index += tmpRelativePositions[0][d]*dimensionOffset[d];

		tmpOffsets[0] = index;

		for (int i = 1; i < numOffsets; i++) {

			System.arraycopy(tmpRelativePositions[i-1], 0, tmpRelativePositions[i], 0, numDimensions);

			for (int d = numDimensions - 1; d >= 0; d--) {
				tmpRelativePositions[i][d]++;
				if (tmpRelativePositions[i][d] <= width/2)
					break;
				tmpRelativePositions[i][d] = -width/2;
			}

			index = 0;
			for (int d = 0; d < numDimensions; d++)
				index += tmpRelativePositions[i][d]*dimensionOffset[d];

			tmpOffsets[i] = index;
		}

		if (includeCenter) {
			op.offsets   = tmpOffsets;
			op.positions = tmpRelativePositions;
			return;
		}

		op.offsets   = new int[numOffsets - 1];
		op.positions = new int[numOffsets - 1][numDimensions];

		for (int i = 0; i < numOffsets/2; i++) {
			op.offsets[i] = tmpOffsets[i];
			System.arraycopy(tmpRelativePositions[i], 0, op.positions[i], 0, numDimensions);
		}
		for (int i = numOffsets/2; i < numOffsets - 1; i++) {
			op.offsets[i] = tmpOffsets[i+1];
			System.arraycopy(tmpRelativePositions[i+1], 0, op.positions[i], 0, numDimensions);
		}
	}

	/**
	 * Perform the random walk and assign the target pixel values on the fly.
	 *
	 * @param sourceCursor The input pixel cursor
	 * @param targetCursor The output pixel cursor
	 * @return the sum of the weights used to mix the target pixel value
	 */
	private final double randomWalk(int sourceIndex, int[] sourcePosition, int targetIndex) {

		// setup local window buffer
		for (int i = 0; i < windowSize; i++)
			if (canAdd(sourcePosition, windowRelativePositions[i], dimensions)) {
				if (debug) {
					System.out.print("adding value: ");
					for (int c = 0; c < numChannels; c++)
						System.out.print(" " + sourceBuffer[sourceIndex + windowOffsets[i]][c]);
					System.out.println();
				}
				System.arraycopy(sourceBuffer[sourceIndex + windowOffsets[i]], 0, localSourceBuffer[i], 0, numChannels);
			} else
				Arrays.fill(localSourceBuffer[i], -1);

		// initialise indices and positions
		localInitialValueIndex    = localSourceBuffer.length/2;
		localLastValueIndex       = localSourceBuffer.length/2;
		int   localSourceIndex    = localSourceBuffer.length/2;
		int[] localSourcePosition = new int[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			localSourcePosition[d] = windowDimensions[d]/2;

		double logPathProbability = 0.0;

		int   pathLength = 0;
		double sumWeights = 0.0;

		for (int k = 0; k < MaxPathLength; k++) {

			// get probabilities of going to neighbors
			getNeighborProbabilities(localSourceIndex, localSourcePosition);

			if (debug) {
				System.out.println("neighbor probs:");
				for (int i = 0; i < numNeighbors; i++)
					System.out.println("" + neighborProbs[i]);
			}

			// sample a neighbor pixel
			int newNeighbor = sampleNeighbor();

			if (debug)
				System.out.println("new neighbor: " + newNeighbor);

			// update log path probability
			logPathProbability += neighborLogProbs[newNeighbor];

			if (debug)
				System.out.println("path prob: " + Math.exp(logPathProbability) + " (minimum " + Math.exp(minLogProb) + ")");

			// abort if the path is getting too unlikely
			if (logPathProbability < minLogProb)
				break;

			pathLength++;

			// set source index and position to new position
			localSourceIndex += localNeighborOffsets[newNeighbor];
			for (int d = 0; d < numDimensions; d++)
				localSourcePosition[d] += localNeighborRelativePositions[newNeighbor][d];

			if (debug) {
				System.out.print("reading value from:");
				for (int d = 0; d < numDimensions; d++)
					System.out.print(" " + localSourcePosition[d]);
				System.out.println();
				System.out.print("which is          :");
				for (int c = 0; c < numChannels; c++)
					System.out.print(" " + localSourceBuffer[localSourceIndex][c]);
				System.out.println();
			}

			// calculate weight
			double weight = Math.exp(logPathProbability/pathLength);
			sumWeights  += weight;

			if (debug) {
				System.out.println("weight: " + weight + " (total " + sumWeights + ")");
			}

			// update target pixel value
			for (int c = 0; c < numChannels; c++)
				denoiseBuffer[targetIndex][c] +=
					weight*localSourceBuffer[localSourceIndex][c];
			
			// update last pixel value
			localLastValueIndex = localSourceIndex;
		};

		return sumWeights;
	}

	/**
	 * Calculates the probabilities for each neighbor pixel to continue the
	 * path.
	 *
	 * @param localSourceIndex The current end point of the path
	 * @param localSourcePosition The current end point of the path
	 */
	private final void getNeighborProbabilities(int localSourceIndex, int[] localSourcePosition) {

		double sum = 0.0;

		for (int i = 0; i < numNeighbors; i++) {

			// get neighbor position
			localNeighborIndex = localSourceIndex + localNeighborOffsets[i];

			// don't walk out of the window
			if (!canAdd(localSourcePosition, localNeighborRelativePositions[i], windowDimensions)) {
				localMisses++;
				neighborProbs[i]    = 0.0;
				neighborLogProbs[i] = -100.0; // any value...
				continue;
			}

			// don't walk out of the image
			if (localSourceBuffer[localNeighborIndex][0] < 0) {
				neighborProbs[i] = 0.0;
				neighborLogProbs[i] = -100.0; // any value...
				continue;
			}

			computeNeighborProbability(i);

			sum += neighborProbs[i];
		}

		// normalize probabilities to sum up to one
		double logSum = Math.log(sum);
		for (int i = 0; i < numNeighbors; i++) {
			neighborProbs[i]    /= sum;
			neighborLogProbs[i] -= logSum;
		}
	}

	private final int sampleNeighbor() {

		// draw a random number between 0 and 1
		double rand = Math.random();

		double sumProbs = 0.0;
		for (int i = 0; i < numNeighbors; i++) {
			sumProbs += neighborProbs[i];
			if (rand <= sumProbs)
				return i;
		}
		System.out.println("Whoops.");
		System.out.println("rand: " + rand);
		System.out.println("sumProbs: " + sumProbs);
		return numNeighbors - 1;
	}

	private final void computeNeighborProbability(int neighbor) {

		int dist2Initial = dist2(localSourceBuffer[localInitialValueIndex], localSourceBuffer[localNeighborIndex]);
		int dist2Last    = dist2(localSourceBuffer[localLastValueIndex],    localSourceBuffer[localNeighborIndex]);

		double probInitial    = probabilities[dist2Initial];
		double probLast       = probabilities[dist2Last];
		double logProbInitial = logProbabilities[dist2Initial];
		double logProbLast    = logProbabilities[dist2Last];

		if (probInitial < 0.0) {
			logProbInitial = -dist2Initial/(maxDistance2*2*variance);
			logProbabilities[dist2Initial] = logProbInitial;
			probInitial = Math.exp(logProbInitial); 
			probabilities[dist2Initial] = probInitial;
			numMisses++;
		}
		if (probLast < 0.0) {
			logProbLast = -dist2Last/(maxDistance2*2*variance);
			logProbabilities[dist2Last] = logProbLast;
			probLast = Math.exp(logProbLast); 
			probabilities[dist2Last] = probLast;
			numMisses++;
		}
		numAccess += 2;

		//System.out.println("initial value: " + initialValue);
		//System.out.println("last value   : " + lastValue);
		//System.out.println("neighborValue: " + neighborValue);
		//System.out.println("dist initial: " + dist(initialValue, neighborValue));
		//System.out.println("dist last   : " + dist(lastValue   , neighborValue));
		//System.out.println("prob initial: " + probInitial);
		//System.out.println("prob last   : " + probLast);
		//System.out.println("normalizer  : " + normalizer);

		neighborProbs[neighbor]    = probInitial*probLast;
		neighborLogProbs[neighbor] = logProbInitial + logProbLast;
	}

	private final int dist2(int[] value1, int[] value2) {

		int dist2 = 0;
		for (int c = 0; c < numChannels; c++)
			dist2 += (value1[c] - value2[c])*(value1[c] - value2[c]);
		return dist2;
	}

	/**
	 * checks whether delta can be added to index without leaving the image
	 *
	 * @param index The original index
	 * @param delte The value to add to the index
	 * @param dimensions The size of the image
	 * @return true, if delta can be added to index without leaving the image
	 */
	private final boolean canAdd(int[] position, int[] delta, int[] dimensions) {

		int component;

		for (int d = 0; d < numDimensions; d++) {

			component = position[d] + delta[d];

			if (component < 0 || component >= dimensions[d])
				return false;
		}

		return true;
	}
}
