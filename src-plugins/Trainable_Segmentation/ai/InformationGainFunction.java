package ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import weka.core.ContingencyTables;
import weka.core.Instance;
import weka.core.Instances;

public class InformationGainFunction extends SplitFunction
{	
	/** generated serial version id */
	private static final long serialVersionUID = 1L;
	/** number of random features to use */
	int numOfFeatures;
	/** random number generator */
	final Random random;
	
	// Attribute-class pair comparator (by attribute value)
	private static final Comparator<AttributeClassPair> comp = new Comparator<AttributeClassPair>(){
		public int compare(AttributeClassPair o1, AttributeClassPair o2)
		{
			final double diff = o2.attributeValue - o1.attributeValue; 
			if(diff < 0)
				return 1;
			else if(diff == 0)
				return 0;
			else
				return -1;
		}
		public boolean equals(Object o)
		{
			return false;
		}
	};
	
	/**
	 * Construct an information gain split function (it needs 
	 * to be initialize with the corresponding data and indices)
	 * 
	 * @param numOfFeatures number of random features to use
	 * @param random random number generator
	 */
	public InformationGainFunction(
			int numOfFeatures, 
			final Random random)
	{
		this.numOfFeatures = numOfFeatures;
		this.random = random;
	}
	
	
	/**
	 * Initialize the function given a specific data set
	 * 
	 * @param data link to the original data
	 * @param indices indices of the samples to use
	 */	 
	public void init(Instances data, ArrayList<Integer> indices) 
	{
		if(indices.size() == 0)
		{
			this.index = 0;
			this.threshold = 0;
			this.allSame = true;
			return;
		}
		
		final int len = data.numAttributes();
		final int numElements = indices.size();	
		final int numClasses = data.numClasses();
				
		// Create and shuffle indices of features to use
		ArrayList<Integer> allIndices = new ArrayList<Integer>();
		for(int i=0; i<len; i++)
			if(i != data.classIndex())
				allIndices.add(i);
		Collections.shuffle(allIndices, random);
		
		double bestGain = Double.MIN_VALUE;
		
		for(int i=0; i < numOfFeatures; i++)
		{
			// Select the random feature
			final int featureToUse = allIndices.get(i); //random.nextInt(allIndices.size());
			
			// Calculate probabilities for right list
			double[] initialProb = new double[ numClasses ];
			for(int n = 0; n < numElements; n++)
				initialProb[(int)data.get(n).classValue()] ++;
			final double initialEntropy = ContingencyTables.entropy(initialProb);

			initialProb = null;

			// Get the maximum information gain
		
			// Create list with pairs attribute-class
			final ArrayList<AttributeClassPair> list = new ArrayList<AttributeClassPair>();
			for(int j=0; j<numElements; j++)
			{
				list.add(
						new AttributeClassPair(	data.get(indices.get(j)).value(featureToUse), 
								(int) data.get(indices.get(j)).classValue() ));
			}

			// Sort pairs in increasing order
			Collections.sort(list, comp);
			
			final double[] probLeft  = new double[numClasses];
			final double[] probRight = new double[numClasses];
			// initial probabilities (all samples on the right)
			for(int n = 0; n < list.size(); n++)
				probRight[list.get(n).classValue] ++;
			
			// Try all splitting points, from position 0 to the end
			int splitPoint = 0;
			do
			{
				final int rightNumElements = numElements - splitPoint;

				// Calculate entropy
				for(int nClass = 0; nClass < numClasses; nClass++)
				{	
					// Divide by the number of elements to get probabilities
					if(splitPoint != 0)
						probLeft[nClass] /= (double) splitPoint;
				
					// Divide by the number of elements to get probabilities
					if(rightNumElements != 0)
						probRight[nClass] /= (double) rightNumElements;
				}
				
				// Calculate entropy;				
				final double entropyLeft = ContingencyTables.entropy(probLeft);
				final double entropyRight = ContingencyTables.entropy(probRight);
				
				// Total entropy value
				final double totalEntropy =	entropyLeft * splitPoint / (double) numElements + 
											entropyRight * rightNumElements / (double) numElements;

				final double currInfGain = initialEntropy - totalEntropy;

				// Save values of maximum information gain
				if( currInfGain > bestGain )
				{
					bestGain = currInfGain;
					this.index = featureToUse;
					this.threshold = list.get(splitPoint).attributeValue;
				}

				// update probabilities for next iteration
				probLeft[list.get(splitPoint).classValue] ++;
				probRight[list.get(splitPoint).classValue] --;
				
				splitPoint++;
				
			}while ( splitPoint < numElements );

			list.clear();
			//System.out.println("Maximum information gain values: index= " + this.index + " threshold= " + this.threshold);

		}
		
		// free list of possible indices to help garbage collector
		allIndices.clear();
		allIndices = null;				
	}

	/**
	 * Evaluate a single instance based on the current 
	 * state of the split function
	 * 
	 * @param instance sample to evaluate
	 * @return false if the instance is on the right of the splitting point, true if it's on the left 
	 */
	public boolean evaluate(Instance instance) 
	{
		if(allSame)
			return true;
		else
			return instance.value(this.index) < this.threshold;
	}

	@Override
	public SplitFunction newInstance() 
	{		
		return new InformationGainFunction(this.numOfFeatures, this.random);
	}
	
	
}
