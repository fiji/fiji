package ai;

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.Instances;

public class Splitter implements Serializable
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private SplitFunction template;
	
	public Splitter(SplitFunction sfn)
	{
		this.template = sfn;
	}
	
	public SplitFunction getSplitFunction(
			final Instances data,
			final ArrayList<Integer> indices)
	{
		try {
			SplitFunction sf = template.newInstance();
			sf.init(data, indices);
			return sf;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
