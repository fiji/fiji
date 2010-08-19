package fiji.plugin.nperry;

public enum Feature {
	LOG_VALUE,
	BRIGHTNESS,
	CONTRAST,
	ESTIMATED_DIAMETER,
	MORPHOLOGY,
	MEAN_INTENSITY,
	MEDIAN_INTENSITY,
	MIN_INTENSITY,
	MAX_INTENSITY,
	TOTAL_INTENSITY,
	VARIANCE,
	STANDARD_DEVIATION,
	KURTOSIS,
	SKEWNESS;
	
	public double getValue(double value) {
		switch (this) {
			case VARIANCE:
				return 1/value;
			case LOG_VALUE:
				return value;
			case BRIGHTNESS:
				return value;
			case CONTRAST:
				return value;
			case ESTIMATED_DIAMETER:
				return value;
			default:
				return Double.NaN; // for non implemented
		}
	}
	
	public String getName() {
		switch(this) {
		case VARIANCE:
			return "Variance";
		case LOG_VALUE:
			return "LoG Value";
		case BRIGHTNESS:
			return "Brightness";
		case CONTRAST:
			return "Contrast";
		case ESTIMATED_DIAMETER:
			return "Estimated diameter";
		case MORPHOLOGY:
			return "Morphology";
		case KURTOSIS:
			return "Kurtosis";
		case SKEWNESS:
			return "Skewness";
		case MAX_INTENSITY:
			return "Maximal intensity";
		case MIN_INTENSITY:
			return "Minimal intensity";
		case MEDIAN_INTENSITY:
			return "Median intensity";
		case MEAN_INTENSITY:
			return "Mean intensity";
		case TOTAL_INTENSITY:
			return "Total intensity";
		case STANDARD_DEVIATION:
			return "Standard deviation";
		default:
			return null;
		}
	}
}
