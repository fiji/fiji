package trainableSegmentation;

public class FeatureStackList 
{
	private FeatureStack featureStackArray[];
	
	/**
	 * Initialize a feature stack list of a specific size
	 * 
	 * @param num number of elements in the list
	 */
	public FeatureStackList(int num)
	{
		this.featureStackArray = new FeatureStack[num]; 
	}
	
	public int getSize()
	{
		return this.featureStackArray.length;
	}
	
	/**
	 * Get n-th stack in the array (remember n>=0)
	 * @param n position of the stack to get
	 * @return feature stack of the corresponding slice
	 */
	public FeatureStack get(int n)
	{
		return featureStackArray[n];
	}
	
	/**
	 * Update all feature stacks in the list (multi-thread fashion)
	 */
	public void updateFeaturesMT()
	{
		for(int i=0; i<featureStackArray.length; i++)
			if(null != featureStackArray[i])
				featureStackArray[i].updateFeaturesMT();
	}
}
