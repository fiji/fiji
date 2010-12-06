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
	/** initial random seed */
	final int seed;
	
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
	 * @param seed random seed for the number generator
	 */
	public InformationGainFunction(
			int numOfFeatures, 
			final int seed)
	{
		this.numOfFeatures = numOfFeatures;
		this.seed = seed;
		this.random = new Random(seed);
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
		
		final int[] featureToUse = new int [ numOfFeatures ];
		
		// Create and shuffle indices of features to use
		ArrayList<Integer> allIndices = new ArrayList<Integer>();
		for(int i=0; i<len; i++)
			if(i != data.classIndex())
				allIndices.add(i);
		Collections.shuffle(allIndices, random);
		// Select the random features
		for(int i=0; i < featureToUse.length; i++)
		{
			featureToUse[ i ] = random.nextInt(allIndices.size());
		}
		// free list of possible indices to help garbage collector
		allIndices.clear();
		allIndices = null;
		
		
		final int numElements = indices.size();		
		double bestGain = Double.MIN_VALUE;
		
		// Calculate probabilities for right list
		double[] initialProb = new double[data.numClasses()];
		for(int n = 0; n < numElements; n++)
			initialProb[(int)data.get(n).classValue()] ++;
		final double initialEntropy = ContingencyTables.entropy(initialProb);
		
		initialProb = null;
				
		// Get the maximum information gain
		for(int i=0; i < featureToUse.length; i++)
		{
			//System.out.println("Feature to use: " + featureToUse[i]);			
			// Create list with pairs attribute-class
			final ArrayList<AttributeClassPair> list = new ArrayList<AttributeClassPair>();
			for(int j=0; j<numElements; j++)
			{
				list.add(
						new AttributeClassPair(	data.get(indices.get(j)).value(featureToUse[i]), 
								(int) data.get(indices.get(j)).classValue() ));
			}

			// Sort pairs in increasing order
			Collections.sort(list, comp);
			/*		
			System.out.println("Sorted attribute-class pairs: ");			
			for(final AttributeClassPair att: list)
			{
				System.out.println("att-class: [" + att.attributeValue + ", " + att.classValue  + " ]");
			}
			 */
			
			//this.threshold = Double.NaN;
			
			for(int splitPoint = 0; splitPoint<numElements; splitPoint++)
			{
				// Skip samples with the same attribute value
				//if( this.threshold == list.get(splitPoint).attributeValue )
				//	continue;
				
				double[] probLeft = new double[data.numClasses()];
				
				// Calculate probabilities for left list
				for(int n = 0; n < splitPoint; n++)
					probLeft[list.get(n).classValue] ++;
				
				// Calculate entropy
				for(int nClass = 0; nClass < data.numClasses(); nClass++)
				{	
					// Divide by the number of elements to get probabilities
					if(splitPoint != 0)
						probLeft[nClass] /= (double) splitPoint;
				}
				final double entropyLeft = ContingencyTables.entropy(probLeft);
												
				// Calculate probabilities for right list
				double[] probRight = new double[data.numClasses()];
				for(int n = splitPoint; n < list.size(); n++)
					probRight[list.get(n).classValue] ++;
				
				// Calculate entropy;
				final int rightNumElements = numElements - splitPoint;
				for(int nClass = 0; nClass < data.numClasses(); nClass++)
				{	
					// Divide by the number of elements to get probabilities
					if(rightNumElements != 0)
						probRight[nClass] /= (double) rightNumElements;
				}
				final double entropyRight = ContingencyTables.entropy(probRight);
				// Total entropy value
				final double totalEntropy =	entropyLeft * splitPoint / (double) numElements + 
								entropyRight * rightNumElements / (double) numElements;
				
				final double currInfGain = initialEntropy - totalEntropy;
				
				// Save values of maximum information gain
				if( currInfGain > bestGain )
				{
					bestGain = currInfGain;
					this.index = featureToUse[i];
					this.threshold = list.get(splitPoint).attributeValue;
				}
				
			}
			list.clear();
			//System.out.println("Maximum information gain values: index= " + this.index + " threshold= " + this.threshold);
			
		}
		
		
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
		return new InformationGainFunction(this.numOfFeatures, this.seed);
	}
	
	
}
