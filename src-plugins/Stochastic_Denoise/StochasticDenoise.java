
import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;

import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;

public class StochasticDenoise<T extends NumericType<T>> {

	// algorithm parameters
	private int    numSamples;
	private double minLogProb;
	private double variance;

	// image statistics
	private int[] dimensions;
	private int   numDimensions;
	private int   numNeighbors;
	private int   numChannels;

	// list of threads for parallel execution
	private ArrayList<DenoiseThread> threads;
	private int                      numThreads;

	// the maximum length of a random walk path
	private final int MaxPathLength = Integer.MAX_VALUE;

	// buffer that contains the source image data (gray scale or rgb)
	private int[][]   sourceBuffer;
	private int       imageSize;

	// parameters of the local window
	private int       windowWidth;
	private int       windowSize;
	private int[]     windowDimensions;
	private int[]     windowOffsets;
	private int[][]   windowRelativePositions;
	private int       localMisses;

	// index offsets of neighbor pixels within the local window
	private int[]     localNeighborOffsets;
	private int[][]   localNeighborRelativePositions;

	// probability cache
	private double[] probabilities;
	private double[] logProbabilities;
	private int numMisses = 0;
	private int numAccess = 0;

	// the maximum squared distance between two pixels
	private int maxDistance2;

	private class DenoiseThread extends Thread {

		// access to the source and target data
		private int sourceIndex;
		private int targetIndex;
		private int[] sourcePosition;
		private LocalizableByDimCursor<T> sourceCursor;
		private LocalizableByDimCursor<T> targetCursor;
	
		private double[] neighborProbs;
		private double[] neighborLogProbs;
		int              localNeighborIndex;
		int              localInitialValueIndex;
		int              localLastValueIndex;

		// buffer to accumulate the denoised pixel value
		private double[]  denoisedPixel;

		// local window buffer of the source image for faster access
		private int[][]   localSourceBuffer;
		private double[]  localWeightBuffer;

		// the number of this thread
		private int threadNum;

		public void init(LocalizableByDimCursor<T> sourceCursor,
		                 LocalizableByDimCursor<T> targetCursor,
		                 int threadNum) {

			neighborProbs     = new double[numNeighbors];
			neighborLogProbs  = new double[numNeighbors];
			denoisedPixel     = new double[numChannels];
			localSourceBuffer = new int[windowSize][numChannels];
			localWeightBuffer = new double[windowSize];

			sourcePosition    = new int[numDimensions];

			this.sourceCursor = sourceCursor;
			this.targetCursor = targetCursor;

			this.threadNum = threadNum;
		}

		public void run() {
	
			IJ.showProgress(0, imageSize);
	
			// iterate over all pixels
			while (sourceCursor.hasNext()) {

				sourceIndex = sourceCursor.getArrayIndex();
				targetIndex = sourceIndex;
	
				// get source position
				sourceCursor.getPosition(sourcePosition);
	
				// setup local window buffer
				for (int i = 0; i < windowSize; i++)
					if (canAdd(sourcePosition, windowRelativePositions[i], dimensions)) {
						System.arraycopy(sourceBuffer[sourceIndex + windowOffsets[i]], 0, localSourceBuffer[i], 0, numChannels);
					} else
						Arrays.fill(localSourceBuffer[i], -1);
	
				// clear local weight buffer
				Arrays.fill(localWeightBuffer, 0.0);
	
				// perform the random walks and accumulate the target pixel values
				double sumWeights = 0.0;
				for (int m = 0; m < numSamples; m++)
					sumWeights += randomWalk(sourceIndex, sourcePosition, targetIndex);
	
				// agglomerate target pixel value from weights
				Arrays.fill(denoisedPixel, 0.0);
				for (int i = 0; i < windowSize; i++)
					for (int c = 0; c < numChannels; c++)
						denoisedPixel[c] +=
							localWeightBuffer[i]*localSourceBuffer[i][c]/sumWeights;
	
				// copy content of denoise buffer to target
				if (targetCursor.getType() instanceof RealType<?>)
					((RealType<?>)targetCursor.getType()).setReal(denoisedPixel[0]);
				if (RGBALegacyType.class.isInstance(targetCursor.getType()))
					RGBALegacyType.class.cast(targetCursor.getType()).set(
						RGBALegacyType.rgba(denoisedPixel[0],
						                    denoisedPixel[1],
						                    denoisedPixel[2],
						                    255.0));
	
				if (threadNum == 0 && sourceIndex % 500 == 0)
					IJ.showProgress(sourceIndex, imageSize);

				// proceed to next pixel
				for (int i = 0; i < numThreads; i++) {
					if (sourceCursor.hasNext()) {
						sourceCursor.fwd();
						targetCursor.fwd();
					}
				}
	
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
	
				// sample a neighbor pixel
				int newNeighbor = sampleNeighbor();
	
				// update log path probability
				logPathProbability += neighborLogProbs[newNeighbor];
	
				// abort if the path is getting too unlikely
				if (logPathProbability < minLogProb)
					break;
	
				pathLength++;
	
				// set source index and position to new position
				localSourceIndex += localNeighborOffsets[newNeighbor];
				for (int d = 0; d < numDimensions; d++)
					localSourcePosition[d] += localNeighborRelativePositions[newNeighbor][d];
	
				// calculate weight
				double weight = Math.exp(logPathProbability/pathLength);
				sumWeights  += weight;
	
				// update weight value
				localWeightBuffer[localSourceIndex] += weight;
				
				// update last pixel index
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
			// fallback for numerical instabilities
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
	
			neighborProbs[neighbor]    = probInitial*probLast;
			neighborLogProbs[neighbor] = logProbInitial + logProbLast;
		}
	}

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
		this.variance   = sigma*sigma;
	}

	/**
	 * Run the algorithm.
	 *
	 * @param image    The noisy input image
	 * @param denoised The denoised ouput image
	 */
	public final void process(Image<T> image, Image<T> denoised) {

		imageSize         = image.size();
		dimensions        = image.getDimensions();
		numDimensions     = dimensions.length;
		numNeighbors      = (int)Math.pow(3, numDimensions) - 1;

		// TODO: find this value automatically
		windowWidth       = 9;
		windowSize        = (int)Math.pow(windowWidth, numDimensions);
		windowDimensions  = new int[numDimensions];
		Arrays.fill(windowDimensions, windowWidth);

		// create image cursor
		LocalizableByDimCursor<T> sourceCursor = image.createLocalizableByDimCursor();

		// find the number of channels in the image
		if (sourceCursor.getType() instanceof RealType<?>) {

			IJ.log("Image is a gray scale image");

			// allocate denoise and source buffer
			numChannels       = 1;
			sourceBuffer      = new int[image.size()][numChannels];

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
			sourceBuffer      = new int[image.size()][numChannels];

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

		// setup threads
		numThreads = Runtime.getRuntime().availableProcessors() + 1;
		threads    = new ArrayList<DenoiseThread>(numThreads);
		for (int t = 0; t < numThreads; t++) {

			DenoiseThread thread = new DenoiseThread();
			threads.add(thread);

			LocalizableByDimCursor<T> threadSourceCursor = image.createLocalizableByDimCursor();
			LocalizableByDimCursor<T> threadTargetCursor = denoised.createLocalizableByDimCursor();

			// set cursors to first pixel for respective thread
			for (int i = 0; i <= t; i++) {
				threadSourceCursor.fwd();
				threadTargetCursor.fwd();
			}

			thread.init(threadSourceCursor, threadTargetCursor, t);
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

		// allocate hash table for probabilities
		probabilities    = new double[maxDistance2 + 1];
		logProbabilities = new double[maxDistance2 + 1];
		Arrays.fill(probabilities, -1.0);

		for (DenoiseThread thread : threads)
			thread.start();

		for (DenoiseThread thread : threads)
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

		IJ.showProgress(1.0);

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
