package ai;

import java.util.ArrayList;
import java.util.Random;

import weka.core.Instance;
import weka.core.Instances;

public class GiniFunction extends SplitFunction 
{
	int index;
	double threshold;
	boolean allSame;
	int numOfFeatures;
	
	public GiniFunction(int numOfFeatures)
	{
		this.numOfFeatures = numOfFeatures;
	}
	
	/**
	 * 
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
		
		
		
	}

	@Override
	public boolean evaluate(Instance instance) 
	{
		// TODO Auto-generated method stub
		return false;
	}

}
