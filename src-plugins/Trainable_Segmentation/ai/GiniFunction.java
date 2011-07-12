package ai;

/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), 
 * 			Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import weka.core.Instance;
import weka.core.Instances;

/**
 * This class implements a split function based on the Gini coefficient
 *
 */
public class GiniFunction extends SplitFunction 
{
	
	/** Servial version ID */
	private static final long serialVersionUID = 9707184791345L;
	/** index of the splitting attribute */
	int index;
	/** threshold value of the splitting point */
	double threshold;
	/** flag to identify when all samples belong to the same class */
	boolean allSame;
	/** number of random features to use */
	int numOfFeatures;
	/** random number generator */
	final Random random;
		
	/**
	 * Constructs a Gini function (initialize it)
	 * 
	 * @param numOfFeatures number of features to use
	 * @param random random number generator
	 */
	public GiniFunction(int numOfFeatures, final Random random)
	{
		this.numOfFeatures = numOfFeatures;
		this.random = random;
	}
	
	/**
	 * Create split function based on Gini coefficient
	 * 
	 * @param data original data
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
		final int classIndex = data.classIndex();
				

		/** Attribute-class pair comparator (by attribute value) */
		final Comparator<AttributeClassPair> comp = new Comparator<AttributeClassPair>(){
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
		
		// Create and shuffle indices of features to use
		ArrayList<Integer> allIndices = new ArrayList<Integer>();
		for(int i=0; i<len; i++)
			if(i != classIndex)
				allIndices.add(i);
		
		double minimumGini = Double.MAX_VALUE;
				
		for(int i=0; i < numOfFeatures; i++)		
		{
			// Select the random feature
			final int index = random.nextInt( allIndices.size() );
			final int featureToUse = allIndices.get( index );
			allIndices.remove( index ); // remove that element to prevent from repetitions
				
			// Get the smallest Gini coefficient
		
			// Create list with pairs attribute-class
			final ArrayList<AttributeClassPair> list = new ArrayList<AttributeClassPair>();
			for(int j=0; j<numElements; j++)
			{
				final Instance ins = data.get(indices.get(j));
				list.add(
						new AttributeClassPair(	ins.value( featureToUse ), 
												(int) ins.value( classIndex ) ));
			}
						
			// Sort pairs in increasing order
			Collections.sort(list, comp);
						
			final double[] probLeft  = new double[numClasses];
			final double[] probRight = new double[numClasses];
			// initial probabilities (all samples on the right)
			for(int n = 0; n < list.size(); n++)
				probRight[list.get(n).classValue] ++;
			
			// Try all splitting points, from position 0 to the end
			for(int splitPoint=0; splitPoint < numElements; splitPoint++)
			{								
				// Calculate Gini coefficient
				double giniLeft = 0;
				double giniRight = 0;
				final int rightNumElements = numElements - splitPoint;

				for(int nClass = 0; nClass < numClasses; nClass++)
				{	
					// left set
					double prob = probLeft[nClass];
					// Divide by the number of elements to get probabilities
					if(splitPoint != 0)
						prob /= (double) splitPoint;
					giniLeft += prob * prob;

					// right set
					prob = probRight[nClass];
					// Divide by the number of elements to get probabilities
					if(rightNumElements != 0)
						prob /= (double) rightNumElements;
					giniRight += prob * prob;
				}

				// Total Gini value
				final double gini = ( (1.0 - giniLeft) * splitPoint 
									+ (1.0 - giniRight) * rightNumElements ) 
									/ (double) numElements;

				// Save values of minimum Gini coefficient
				if( gini < minimumGini )
				{
					minimumGini = gini;
					this.index = featureToUse;
					this.threshold = list.get(splitPoint).attributeValue;
				}

				// update probabilities for next iteration
				probLeft[list.get(splitPoint).classValue] ++;
				probRight[list.get(splitPoint).classValue] --;
			}				
		}
		
		// free list of possible indices to help garbage collector
		//allIndices.clear();
		//allIndices = null;
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
		return new GiniFunction(this.numOfFeatures, this.random);
	}

}
