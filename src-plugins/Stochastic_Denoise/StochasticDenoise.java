
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

	private float[][] denoiseBuffer;

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

		// find suitable type for averaging
		if (sourceCursor.getType() instanceof RealType<?>) {
			IJ.log("Image is a gray scale image");
			// allocate denoise buffer
			denoiseBuffer = new float[image.size()][1];
		}
		if (sourceCursor.getType() instanceof RGBALegacyType) {
			IJ.log("Image is an RGBA image");
			denoiseBuffer = new float[image.size()][3];
		}

		if (debug) {
			sourceCursor.setPosition(new int[]{61, 85});
			while(targetCursor.hasNext()) {
				targetCursor.fwd();
				targetCursor.getType().setZero();
			}
			System.out.println("Starting denoising...");
		}

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

			if (targetCursor.getType() instanceof RealType<?>)
				((RealType<?>)targetCursor.getType()).setReal(denoiseBuffer[index][0]/sumWeights);
			if (targetCursor.getType() instanceof RGBALegacyType)
				((RGBALegacyType)targetCursor.getType()).set(
					RGBALegacyType.rgba(denoiseBuffer[index][0]/sumWeights,
				                        denoiseBuffer[index][1]/sumWeights,
				                        denoiseBuffer[index][2]/sumWeights,
				                        255.0f));

			if (debug) {
				System.out.println("accum. value : " + denoiseBuffer[index]);
				System.out.println("total weights: " + sumWeights);
				System.out.println("final value  : " + denoiseBuffer[index][0]/sumWeights);
				System.out.println("final px value: " + targetCursor.getType().toString());

				break;
			}

			if (index % 50 == 0)
				IJ.showProgress(index, image.size());
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

		T initialValue = sourceCursor.getType().clone();
		T lastValue    = initialValue.clone();

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
			float[] add;
			if (sourceCursor.getType() instanceof RealType<?>) {
				add    = new float[1];
				add[0] = weight*((RealType<?>)sourceCursor.getType()).getRealFloat();
			}
			//if (sourceCursor.getType() instanceof RGBALegacyType) {
			else {
				add    = new float[3];
				int value = ((RGBALegacyType)sourceCursor.getType()).get();
				add[0] = weight*RGBALegacyType.red(value);
				add[1] = weight*RGBALegacyType.green(value);
				add[2] = weight*RGBALegacyType.blue(value);
			}
			int index  = targetCursor.getArrayIndex();
			for (int b = 0; b < denoiseBuffer[index].length; b++)
				denoiseBuffer[index][b] += add[b];

			// update last pixel value
			lastValue = sourceCursor.getType().clone();

			if (debug) {
				System.out.println("added to pixel: " + add.toString());
				System.out.println("target value  : " + denoiseBuffer[index]);
			}

			// update last pixel value
			lastValue = sourceCursor.getType().clone();
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
											 T initialValue, T lastValue) {

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
			T neighborValue  = cursor.getType();

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
		return probabilities.length - 1;
	}

	private float neighborProbability(T initialValue, T lastValue, T neighborValue) {

		float probInitial;
		float probLast;

		//System.out.println("initial value: " + initialValue);
		//System.out.println("last value   : " + lastValue);
		//System.out.println("neighborValue: " + neighborValue);

		probInitial = (float)Math.exp(-Math.pow(dist(initialValue, neighborValue), 2)/(2*variance));
		probLast    = (float)Math.exp(-Math.pow(dist(lastValue   , neighborValue), 2)/(2*variance));

		//System.out.println("dist initial: " + dist(initialValue, neighborValue));
		//System.out.println("dist last   : " + dist(lastValue   , neighborValue));
		//System.out.println("prob initial: " + probInitial);
		//System.out.println("prob last   : " + probLast);
		//System.out.println("normalizer  : " + normalizer);

		return probInitial*probLast;
	}

	private final float dist(T value1, T value2) {
		if (value1 instanceof RealType<?>)
			return (float)((((RealType<?>)value1).getRealFloat() - ((RealType<?>)value2).getRealFloat())/255.0);
		else {

			int v1 = ((RGBALegacyType)value1).get();
			int v2 = ((RGBALegacyType)value2).get();
			//System.out.println("value1: " + v1);
			//System.out.println("value2: " + v2);
			return (float)Math.sqrt(
				Math.pow(RGBALegacyType.red(v1)/255.0   - RGBALegacyType.red(v2)/255.0,   2.0) +
				Math.pow(RGBALegacyType.green(v1)/255.0 - RGBALegacyType.green(v2)/255.0, 2.0) +
				Math.pow(RGBALegacyType.blue(v1)/255.0  - RGBALegacyType.blue(v2)/255.0,  2.0));
		}
	}
}
