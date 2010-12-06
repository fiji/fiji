package ai;

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.Instance;
import weka.core.Instances;

public abstract class SplitFunction implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int index;
	double threshold;
	boolean allSame;
	public abstract void init(final Instances data, final ArrayList<Integer> indices);
	public abstract boolean evaluate(final Instance instance);
	public abstract SplitFunction newInstance();
}
