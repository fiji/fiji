package fiji.plugin.trackmate;

/**
 * A helper class to store a feature filter. It is just made of 3 public fields.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 23, 2010
 *
 */
public class FeatureFilter {
	public String feature;
	public Double value;
	public boolean isAbove;
	
	
	public FeatureFilter(String feature, Double value, boolean isAbove) {
		this.feature = feature;
		this.value = value;
		this.isAbove = isAbove;
	}
	
	@Override
	public String toString() {
		String str = feature.toString();
		if (isAbove)
			str += " > ";
		else 
			str += " < ";
		str += value.toString();
		return str;
	}

}
