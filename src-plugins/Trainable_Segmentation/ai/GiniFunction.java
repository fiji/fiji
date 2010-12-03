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
	
	public GiniFunction(int numOfFeatures)
	{
		this.numOfFeatures = numOfFeatures;
	}
	
	/**
	 * Inner class to order attributes while preserving class indices 
	 *
	 */
	class AttributeClassPair
	{
		double attributeValue;
		int classValue;
		
		AttributeClassPair(double attributeValue, int classIndex)
		{
			this.attributeValue = attributeValue;
			this.classValue = classIndex;
		}
	}
	
	/**
	 * Create split function based on Gini coefficient
	 */	 
	public void createFunction(Instances data, ArrayList<Integer> indices) 
	{
		if(indices.size() == 0)
		{
			this.index = 0;
			this.threshold = 0;
			this.allSame = true;
			return;
		}
		
		final int len = data.numAttributes();
		
		final int[] all = new int [ numOfFeatures ];
		
		Random random = new Random();
		
		// Select the random features
		for(int i=0; i < all.length; i++)
		{
			int randInt;
			do{
				randInt = random.nextInt(len); 
			}while(randInt == data.classIndex());
			all[ i ] = randInt;
		}
		
		// Attribute-class pair comparator (by attribute value)
		Comparator<AttributeClassPair> comp = new Comparator<AttributeClassPair>(){
			public int compare(AttributeClassPair o1, AttributeClassPair o2)
			{
				final double diff = o1.attributeValue - o2.attributeValue; 
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
		
		final int numElements = indices.size();
		
		double minimumGini = Double.MAX_VALUE;
		
		
		// Get the smallest Gini coefficient
		for(int i=0; i < all.length; i++)
		{
			// Create list with pairs attribute-class
			final ArrayList<AttributeClassPair> list = new ArrayList<AttributeClassPair>();
			for(int j=0; j<numElements; j++)
			{
				list.add(new AttributeClassPair(data.get(indices.get(j).intValue()).value(all[i]), (int) data.get(indices.get(j).intValue()).classValue() ));
			}
			
			// Sort pairs
			Collections.sort(list, comp);
			
			for(int splitPoint = 0; splitPoint<list.size(); splitPoint++)
			{
				double[] prob = new double[data.numClasses()];
				
				// Calculate probabilities for left list
				for(int n = 0; n < splitPoint; n++)
					prob[list.get(n).classValue] ++;
				
				// Calculate Gini coefficient
				double giniLeft = 0;
				for(int nClass = 0; nClass < data.numClasses(); nClass++)
				{	
					// Divide by the number of elements to get probabilities
					if(splitPoint != 0)
						prob[nClass] /= (double) splitPoint;
					giniLeft += prob[nClass] * prob[nClass]; 					
				}
				giniLeft = 1.0 - giniLeft;
												
				// Calculate probabilities for right list
				prob = new double[data.numClasses()];
				for(int n = splitPoint; n < list.size(); n++)
					prob[list.get(n).classValue] ++;
				
				// Calculate Gini coefficient
				double giniRight = 0;
				for(int nClass = 0; nClass < data.numClasses(); nClass++)
				{	
					// Divide by the number of elements to get probabilities
					if(splitPoint != 0)
						prob[nClass] /= (double) splitPoint;
					giniRight += prob[nClass] * prob[nClass]; 					
				}
				giniRight = 1.0 - giniRight;
				
				// Total Gini value
				double gini = giniLeft * splitPoint / (double)numElements + giniRight * (numElements - splitPoint) / numElements;
				
				// Save values of minimum Gini coefficient
				if( gini < minimumGini)
				{
					this.index = all[i];
					this.threshold = list.get(splitPoint).attributeValue;
				}
				
			}
			
			
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
		return instance.value(this.index) < this.threshold;
	}

}
