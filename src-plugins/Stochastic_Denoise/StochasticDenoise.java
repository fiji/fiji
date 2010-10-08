
import java.util.Arrays;

import ij.IJ;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;

import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;

public class StochasticDenoise<T extends NumericType<T>> {

	private int   numSamples;
	private float minProb;
	private float variance;

	private int[] dimensions;
	private int   numDimensions;
	private int   numNeighbors;
	private int   numChannels;

	private final int MaxPathLength = Integer.MAX_VALUE;

	// buffer to accumulate the pixel values
	private float[][] denoiseBuffer;
	// buffer that contains the source image data (gray scale or rgb)
	private int[][]   sourceBuffer;

	// buffer to contain the relative positions of neighbors
	private int[][]   relativeNeighborPositions;

	private float[] probabilities;
	private int maxDistance2;
	private int numMisses = 0;
	private int numAccess = 0;

	private final boolean debug = false;

	/**
	 * Set all parameters of the algorithm.
	 *
	 * @param numSamples The number of random walks for each pixel
	 * @param minProb    The minimal allowed probability of a path
	 * @param sigma      The standard deviation for the probability function
	 */
	public void setParameters(int numSamples, float minProb, float sigma) {

		this.numSamples = numSamples;
		this.minProb    = minProb;
		this.variance   = (float)Math.pow(sigma, 2);
	}

	/**
	 * Run the algorithm.
	 *
	 * @param image    The noisy input image
	 * @param denoised The denoised ouput image
	 */
	public void process(Image<T> image, Image<T> denoised) {

		dimensions    = image.getDimensions();
		numDimensions = dimensions.length;
		numNeighbors  = (int)Math.pow(3, numDimensions) - 1;

		// create relative neighbor positions
		relativeNeighborPositions = new int[numNeighbors][numDimensions];
		int[][] cube              = new int[numNeighbors+1][numDimensions];
		Arrays.fill(cube[0], -1);
		for (int i = 1; i < numNeighbors + 1; i++) {
			System.arraycopy(cube[i-1], 0,
							 cube[i], 0, numDimensions);
			for (int d = numDimensions - 1; d >= 0; d--) {
				cube[i][d]++;
				if (cube[i][d] <= 1)
					break;
				cube[i][d] = -1;
			}
		}
		for (int i = 0; i < numNeighbors/2; i++)
			relativeNeighborPositions[i] = cube[i];
		for (int i = numNeighbors/2; i < numNeighbors; i++)
			relativeNeighborPositions[i] = cube[i+1];

		// create image cursors
		LocalizableByDimCursor<T> sourceCursor = image.createLocalizableByDimCursor();
		LocalizableByDimCursor<T> targetCursor = denoised.createLocalizableByDimCursor();

		// setup buffers
		if (sourceCursor.getType() instanceof RealType<?>) {

			IJ.log("Image is a gray scale image");
			// allocate denoise and source buffer
			numChannels   = 1;
			denoiseBuffer = new float[numChannels][image.size()];
			sourceBuffer  = new int[numChannels][image.size()];
			// copy image data to source buffer
			while (sourceCursor.hasNext()) {
				sourceCursor.fwd();
				sourceBuffer[0][sourceCursor.getArrayIndex()] =
					(int)((RealType<?>)sourceCursor.getType()).getRealFloat();
			}
			sourceCursor.reset();
			// TODO: handle more that 8-bit images
			maxDistance2  = 255*255;
		}
		else if (sourceCursor.getType() instanceof RGBALegacyType) {

			IJ.log("Image is an RGBA image");
			numChannels   = 3;
			denoiseBuffer = new float[numChannels][image.size()];
			sourceBuffer  = new int[numChannels][image.size()];
			// copy image data to source buffer
			while (sourceCursor.hasNext()) {
				sourceCursor.fwd();
				int value = ((RGBALegacyType)sourceCursor.getType()).get();
				sourceBuffer[0][sourceCursor.getArrayIndex()] = RGBALegacyType.red(value);
				sourceBuffer[1][sourceCursor.getArrayIndex()] = RGBALegacyType.green(value);
				sourceBuffer[2][sourceCursor.getArrayIndex()] = RGBALegacyType.blue(value);
			}
			sourceCursor.reset();
			// TODO: handle more that 8-bit images
			maxDistance2  = (255*255)*3;
		} else {

			IJ.log("Image type " + sourceCursor.getType().getClass() + " not supported!");
			return;
		}

		if (debug) {
			sourceCursor.setPosition(new int[]{61, 85});
			while(targetCursor.hasNext()) {
				targetCursor.fwd();
				targetCursor.getType().setZero();
			}
			System.out.println("Starting denoising...");
		}

		// allocate hash table for probabilities
		probabilities = new float[maxDistance2 + 1];
		Arrays.fill(probabilities, -1.0f);

		// iterate over all pixels
		IJ.showProgress(0, image.size());
		while (sourceCursor.hasNext()) {
			sourceCursor.fwd();
			targetCursor.setPosition(sourceCursor);

			// perform the random walks and accumulate the target pixel values
			float sumWeights = 0.0f;
			for (int i = 0; i < numSamples; i++)
				sumWeights += randomWalk(sourceCursor, targetCursor);

			// normalize the target pixel value
			int index = targetCursor.getArrayIndex();
			for (int c = 0; c < denoiseBuffer.length; c++)
				denoiseBuffer[c][index] /= sumWeights;

			if (debug) {
				System.out.println("accum. value : " + denoiseBuffer[0][index]*sumWeights);
				System.out.println("total weights: " + sumWeights);
				System.out.println("final value  : " + denoiseBuffer[0][index]);
				System.out.println("final px value: " + targetCursor.getType().toString());

				break;
			}

			if (index % 50 == 0)
				IJ.showProgress(index, image.size());
		}

		// copy content of denoise buffer to target
		targetCursor.reset();
		while (targetCursor.hasNext()) {
			targetCursor.fwd();

			int index = targetCursor.getArrayIndex();

			if (targetCursor.getType() instanceof RealType<?>)
				((RealType<?>)targetCursor.getType()).setReal(denoiseBuffer[0][index]);
			if (targetCursor.getType() instanceof RGBALegacyType)
				((RGBALegacyType)targetCursor.getType()).set(
					RGBALegacyType.rgba(denoiseBuffer[0][index],
				                        denoiseBuffer[1][index],
				                        denoiseBuffer[2][index],
				                        255.0f));
		}

		IJ.log("percentage of cache hits for probabilities: " + (float)(numAccess - numMisses)/numAccess);
		IJ.log("size of hash table of probabilities       : " + probabilities.length);
	}

	/**
	 * Perform the random walk and assign the target pixel values on the fly.
	 *
	 * @param sourceCursor The input pixel cursor
	 * @param targetCursor The output pixel cursor
	 * @return the sum of the weights used to mix the target pixel value
	 */
	protected float randomWalk(LocalizableByDimCursor<T> sourceCursor,
	                           LocalizableByDimCursor<T> targetCursor) {

		int initialValueIndex = sourceCursor.getArrayIndex();
		int lastValueIndex    = initialValueIndex;

		float pathProbability = 1.0f;

		int   pathLength = 0;
		float sumWeights = 0.0f;

		int[] initialPosition = new int[numDimensions];
		sourceCursor.getPosition(initialPosition);

		for (int k = 0; k < MaxPathLength; k++) {
			// get probabilities of going to neighbors
			float[] probabilities = getNeighborProbabilities(sourceCursor,
			                                                 initialValueIndex,
			                                                 lastValueIndex);

			if (debug) {
				System.out.println("neighbor probs:");
				for (int i = 0; i < numNeighbors; i++)
					System.out.println("" + probabilities[i]);
			}

			// sample a neighbor pixel
			int newNeighbor = sampleNeighbor(probabilities);

			if (debug)
				System.out.println("new neighbor: " + newNeighbor);

			// update path probability
			pathProbability *= probabilities[newNeighbor];

			if (debug)
				System.out.println("path prob: " + pathProbability + " (minimum " + minProb + ")");

			// abort if the path is getting too unlikely
			if (pathProbability < minProb)
				break;

			pathLength++;

			// set source cursor to new position
			sourceCursor.moveRel(relativeNeighborPositions[newNeighbor]);

			if (debug) {
				int[] sourcePosition = new int[numDimensions];
				sourceCursor.getPosition(sourcePosition);
				// visualise the random path
				targetCursor.setPosition(sourcePosition);
				T one = targetCursor.getType().clone();
				one.setOne();
				targetCursor.getType().add(one);
				targetCursor.setPosition(initialPosition);

				System.out.println("reading value from: ");
				for (int d = 0; d < numDimensions; d++)
					System.out.println("" + sourcePosition[d]);
			}

			// calculate weight
			float weight = (float)Math.pow(pathProbability, 1.0/pathLength);
			sumWeights  += weight;

			if (debug) {
				System.out.println("weight: " + weight + " (total " + sumWeights + ")");
			}

			// update target pixel value
			for (int c = 0; c < numChannels; c++)
				denoiseBuffer[c][targetCursor.getArrayIndex()] +=
					weight*sourceBuffer[c][sourceCursor.getArrayIndex()];
			
			// update last pixel value
			lastValueIndex = sourceCursor.getArrayIndex();
		};

		// reset source cursor
		sourceCursor.setPosition(initialPosition);

		return sumWeights;
	}

	/**
	 * Calculates the probabilities for each neighbor pixel to continue the
	 * path.
	 *
	 * @param cursor The current end point of the path
	 * @param initialValue The value of the first pixel in the path
	 * @param lastValue    The value of the last pixel in the path
	 * @return An array of probabilities
	 */
	private float[] getNeighborProbabilities(LocalizableByDimCursor<T> cursor,
											 int initialValueIndex, int lastValueIndex) {

		float[] probabilities    = new float[numNeighbors];
		int[]   centerPosition   = cursor.getPosition();
		int[]   neighborPosition = new int[numDimensions];

		float sum = 0.0f;

		for (int i = 0; i < numNeighbors; i++) {

			// get neighbor position
			for (int d = 0; d < numDimensions; d++)
				neighborPosition[d] = centerPosition[d] + relativeNeighborPositions[i][d];

			// don't walk out of the image
			boolean donext = false;
			for (int d = 0; d < numDimensions; d++) {
				if (neighborPosition[d] < 0 || neighborPosition[d] >= dimensions[d]) {
					probabilities[i] = 0.0f;
					donext = true;
				}
			}
			if (donext)
				continue;

			cursor.setPosition(neighborPosition);
			int index = cursor.getArrayIndex();
			int[] initialValue  = new int[numChannels];
			int[] lastValue     = new int[numChannels];
			int[] neighborValue = new int[numChannels];
			for (int c = 0; c < numChannels; c++) {
				initialValue[c]  = sourceBuffer[c][initialValueIndex];
				lastValue[c]     = sourceBuffer[c][lastValueIndex];
				neighborValue[c] = sourceBuffer[c][index];
			}

			probabilities[i] = neighborProbability(initialValue, lastValue, neighborValue);
			sum             += probabilities[i];
		}

		// reset cursor
		cursor.setPosition(centerPosition);

		// normalize probabilities to sum up to one
		for (int i = 0; i < numNeighbors; i++)
			probabilities[i] /= sum;

		return probabilities;
	}

	private int sampleNeighbor(float[] probabilities) {

		// draw a random number between 0 and 1
		float rand = (float)Math.random();

		float sumProbs = 0.0f;
		for (int i = 0; i < numNeighbors; i++) {
			sumProbs += probabilities[i];
			if (rand <= sumProbs)
				return i;
		}
		return numNeighbors - 1;
	}

	private float neighborProbability(int[] initialValue, int[] lastValue, int[] neighborValue) {

		int dist2Initial = dist2(initialValue, neighborValue);
		int dist2Last    = dist2(lastValue,    neighborValue);

		float probInitial = probabilities[dist2Initial];
		float probLast    = probabilities[dist2Last];

		if (probInitial < 0.0f) {
			probInitial = (float)Math.exp(-dist2Initial/(maxDistance2*2*variance)); 
			probabilities[dist2Initial] = probInitial;
			numMisses++;
		}
		if (probLast < 0.0f) {
			probLast = (float)Math.exp(-dist2Last/(maxDistance2*2*variance)); 
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

		return probInitial*probLast;
	}

	private final int dist2(int[] value1, int[] value2) {

		int dist2 = 0;
		for (int c = 0; c < numChannels; c++)
			dist2 += (value1[c] - value2[c])*(value1[c] - value2[c]);
		return dist2;
	}
}
