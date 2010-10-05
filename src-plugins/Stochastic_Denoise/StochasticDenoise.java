
import java.util.Arrays;

import ij.IJ;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;

import mpicbg.imglib.type.numeric.RealType;

public class StochasticDenoise<T extends RealType<T>> {

	private int   numSamples;
	private float minProb;
	private float variance;
	private float normalizer;

	private int[] dimensions;
	private int   numDimensions;
	private int   numNeighbors;

	private float[] denoiseBuffer;

	private final int MaxPathLength = Integer.MAX_VALUE;

	private final boolean debug = false;

	private int[][] relativeNeighborPositions;
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
		this.normalizer = 1.0f/((float)Math.sqrt(2*Math.PI*(variance/2.0f)));
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

		// allocate denoise buffer
		denoiseBuffer = new float[image.size()];

		LocalizableByDimCursor<T> sourceCursor = image.createLocalizableByDimCursor();
		LocalizableByDimCursor<T> targetCursor = denoised.createLocalizableByDimCursor();

		if (debug) {
			sourceCursor.setPosition(new int[]{61, 85});
			while(targetCursor.hasNext()) {
				targetCursor.fwd();
				targetCursor.getType().setReal(0.0f);
			}
		}

		while (sourceCursor.hasNext()) {
			sourceCursor.fwd();
			targetCursor.setPosition(sourceCursor);

			// perform the random walks and accumulate the target pixel values
			float sumWeights = 0.0f;
			for (int i = 0; i < numSamples; i++)
				sumWeights += randomWalk(sourceCursor, targetCursor);

			// normalize the target pixel value
			int index = targetCursor.getArrayIndex();
			targetCursor.getType().setReal(denoiseBuffer[index]/sumWeights);

			if (debug) {
				IJ.log("accum. value : " + denoiseBuffer[index]);
				IJ.log("total weights: " + sumWeights);
				IJ.log("final value   : " + denoiseBuffer[index]/sumWeights);
				IJ.log("final px value: " + targetCursor.getType().getRealFloat());

				break;
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
	protected float randomWalk(LocalizableByDimCursor<T> sourceCursor,
							   LocalizableByDimCursor<T> targetCursor) {

		float initialValue = sourceCursor.getType().getRealFloat();
		float lastValue    = initialValue;

		float pathProbability = 1.0f;

		int   pathLength = 0;
		float sumWeights = 0.0f;

		int[] initialPosition = new int[numDimensions];
		sourceCursor.getPosition(initialPosition);

		for (int k = 0; k < MaxPathLength; k++) {
			// get probabilities of going to neighbors
			float[] probabilities = getNeighborProbabilities(sourceCursor,
			                                                 initialValue,
			                                                 lastValue);

			if (debug) {
				IJ.log("neighbor probs:");
				for (int i = 0; i < numNeighbors; i++)
					IJ.log("" + probabilities[i]);
			}

			// sample a neighbor pixel
			int newNeighbor = sampleNeighbor(probabilities);

			if (debug)
				IJ.log("new neighbor: " + newNeighbor);

			// update path probability
			pathProbability *= probabilities[newNeighbor];

			if (debug)
				IJ.log("path prob: " + pathProbability + " (minimum " + minProb + ")");

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
				targetCursor.getType().inc();
				targetCursor.setPosition(initialPosition);

				IJ.log("reading value from: ");
				for (int d = 0; d < numDimensions; d++)
					IJ.log("" + sourcePosition[d]);
			}

			// calculate weight
			float weight = (float)Math.pow(pathProbability, 1.0/pathLength);
			sumWeights  += weight;

			if (debug) {
				IJ.log("weight: " + weight + " (total " + sumWeights + ")");
			}

			// update target pixel value
			float source = sourceCursor.getType().getRealFloat();
			float add    = source*weight;
			int   index  = targetCursor.getArrayIndex();
			denoiseBuffer[index] += add;

			if (debug) {
				IJ.log("added to pixel: " + add + " (" + source + ")");
				IJ.log("target value  : " + denoiseBuffer[index]);
			}
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
											 float initialValue, float lastValue) {

		float[] probabilities    = new float[numNeighbors];
		int[]   centerPosition   = cursor.getPosition();
		int[]   neighborPosition = new int[numDimensions];

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
			float neighborValue  = cursor.getType().getRealFloat();

			probabilities[i]     = neighborProbability(initialValue, lastValue, neighborValue);
		}

		// reset cursor
		cursor.setPosition(centerPosition);

		return probabilities;
	}

	private int sampleNeighbor(float[] probabilities) {

		float sumProbs = 0.0f;
		for (float prob : probabilities)
			sumProbs += prob;

		// draw a random number between 0 and sum of all probabilities
		float rand = (float)Math.random()*sumProbs;

		sumProbs = 0.0f;
		for (int i = 0; i < probabilities.length; i++) {
			sumProbs += probabilities[i];
			if (rand <= sumProbs)
				return i;
		}
		return probabilities.length - 1;
	}

	private float neighborProbability(float initialValue, float lastValue, float neighborValue) {

		float probInitial;
		float probLast;

		//IJ.log("initial value: " + initialValue);
		//IJ.log("last value   : " + lastValue);
		//IJ.log("neighborValue: " + neighborValue);

		probInitial = (float)Math.exp(-Math.pow(dist(initialValue, neighborValue), 2)/(2*variance));
		probLast    = (float)Math.exp(-Math.pow(dist(lastValue   , neighborValue), 2)/(2*variance));

		//IJ.log("prob initial: " + probInitial);
		//IJ.log("prob last   : " + probLast);
		//IJ.log("normalizer  : " + normalizer);

		return normalizer*probInitial*probLast;
	}

	private final float dist(float value1, float value2) {
		return (float)((value1 - value2)/255.0);
	}
}
