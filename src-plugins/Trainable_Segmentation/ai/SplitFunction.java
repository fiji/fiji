package ai;

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.Instance;
import weka.core.Instances;

public abstract class SplitFunction implements Serializable
{
	public abstract void createFunction(final Instances data, final ArrayList<Integer> indices);
	public abstract boolean evaluate(final Instance instance);
}
