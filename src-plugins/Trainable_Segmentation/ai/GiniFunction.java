package ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import weka.core.Instance;
import weka.core.Instances;

public class GiniFunction extends SplitFunction 
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int index;
	double threshold;
	boolean allSame;
	int numOfFeatures;
	final Random random;
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
	 * 
	 * @param numOfFeatures
	 */
	public GiniFunction(int numOfFeatures, final int seed)
	{
		this.numOfFeatures = numOfFeatures;
		this.seed = seed;
		this.random = new Random(seed);
	}
	
	/**
	 * Create split function based on Gini coefficient
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
		double minimumGini = Double.MAX_VALUE;
				
		// Get the smallest Gini coefficient
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
				
				// Calculate Gini coefficient
				double giniLeft = 0;
				for(int nClass = 0; nClass < data.numClasses(); nClass++)
				{	
					// Divide by the number of elements to get probabilities
					if(splitPoint != 0)
						probLeft[nClass] /= (double) splitPoint;
					giniLeft += probLeft[nClass] * probLeft[nClass]; 					
				}
				giniLeft = 1.0 - giniLeft;
												
				// Calculate probabilities for right list
				double[] probRight = new double[data.numClasses()];
				for(int n = splitPoint; n < list.size(); n++)
					probRight[list.get(n).classValue] ++;
				
				// Calculate Gini coefficient
				double giniRight = 0;
				final int rightNumElements = numElements - splitPoint;
				for(int nClass = 0; nClass < data.numClasses(); nClass++)
				{	
					// Divide by the number of elements to get probabilities
					if(rightNumElements != 0)
						probRight[nClass] /= (double) rightNumElements;
					giniRight += probRight[nClass] * probRight[nClass]; 					
				}
				giniRight = 1.0 - giniRight;
				
				// Total Gini value
				double gini =	giniLeft * splitPoint / (double) numElements + 
								giniRight * rightNumElements / (double) numElements;
				
				// Save values of minimum Gini coefficient
				if( gini < minimumGini)
				{
					minimumGini = gini;
					this.index = featureToUse[i];
					this.threshold = list.get(splitPoint).attributeValue;
				}
				
			}
			list.clear();
			//System.out.println("Minimum gini values: index= " + this.index + " threshold= " + this.threshold);
			
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
		return new GiniFunction(this.numOfFeatures, this.seed);
	}

}
