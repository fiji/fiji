package fiji.plugin.trackmate;

/**
 * A helper class to store track feature filters. It is just made of 3 public fields.
 */
public class TrackFilter {
	public TrackFeature feature;
	public Float value;
	public boolean isAbove;
	
	
	public TrackFilter(TrackFeature feature, Float value, boolean isAbove) {
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
