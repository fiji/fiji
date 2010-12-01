package ai;

import java.util.ArrayList;

import weka.core.Instances;

public class Splitter 
{
	
	private SplitFunction splitFunction;
	
	public Splitter(SplitFunction sfn)
	{
		this.splitFunction = sfn;
	}
	
	public SplitFunction getSplitFunction(
			final Instances data,
			final ArrayList<Integer> indices)
	{
		splitFunction.createFunction(data, indices);
		return splitFunction;
	}

}
