package ai;

/**
 * Inner class to order attributes while preserving class indices 
 *
 */
public class AttributeClassPair
{
	/** real value of the corresponding attribute */
	double attributeValue;
	/** index of the class associated to this pair */
	int classValue;
	/**
	 * Create pair attribute-class
	 * 
	 * @param attributeValue real attribute value
	 * @param classIndex index of the class associated to this sample
	 */
	AttributeClassPair(double attributeValue, int classIndex)
	{
		this.attributeValue = attributeValue;
		this.classValue = classIndex;
	}
}
