package ai;

import java.util.ArrayList;
import java.util.Random;

import weka.classifiers.AbstractClassifier;
import weka.core.Instances;
import weka.core.Randomizable;
import weka.core.Utils;

public class BalancedRandomForest extends AbstractClassifier implements Randomizable
{
		
	/** serialization UID */
	private static final long serialVersionUID = 1L;
	
	/** random seed */
	int randomSeed = 1;
	
	/** number of trees */
	int numOfTrees = 200;
	
	/** number of features used on each node of the trees */
	int numOfFeatures = 0;
	
	/** array of random trees that form the forest */
	BalancedRandomTree[] tree = null;
	
	/**
	 * Build Balanced Random Forest
	 */
	public void buildClassifier(Instances data) throws Exception 
	{
		// If number of features is 0 then set it to log2 of M (number of attributes)
		if (numOfFeatures < 1) 
			numOfFeatures = (int) Utils.log2(data.numAttributes())+1;
		
		// Initialize array of trees
		tree = new BalancedRandomTree[ numOfTrees ];
		
		// total number of instances
		final int numInstances = data.numInstances();
		// total number of classes
		final int numClasses = data.numClasses();
		
		final ArrayList<Integer>[] indexSample = new ArrayList[ numClasses ];
				
		// fill indexSample with the indices of each class
		for(int i = 0 ; i < numInstances; i++)
			indexSample[ data.get(i).classIndex() ].add( i );
		
		Random random = new Random(randomSeed);
		

		for(int i = 0; i < numOfTrees; i++)
		{
			ArrayList<Integer> bagIndices = new ArrayList<Integer>(); 
			
			for(int j = 0 ; j < numInstances; j++)
			{
				// Randomly select the indices in a balanced way
				int randInt = random.nextInt( numClasses );				
				bagIndices.add( indexSample[ randInt ].get( random.nextInt( indexSample[randInt].size() ) ) );
			}
			
			// Create random tree
			
			
		}
		
	}

	public int getSeed() 
	{
		return randomSeed;
	}

	public void setSeed(int seed) 
	{
		this.randomSeed = seed;
	}

}
