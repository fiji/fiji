package fiji.plugin.nperry;

public enum Feature {
	LOG_VALUE,
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
	SKEWNESS,
	ELLIPSOIDFIT_SEMIAXISLENGTH_A,
	ELLIPSOIDFIT_SEMIAXISLENGTH_B,
	ELLIPSOIDFIT_SEMIAXISLENGTH_C,
	ELLIPSOIDFIT_AXISPHI_A,
	ELLIPSOIDFIT_AXISPHI_B,
	ELLIPSOIDFIT_AXISPHI_C,
	ELLIPSOIDFIT_AXISTHETA_A,
	ELLIPSOIDFIT_AXISTHETA_B,
	ELLIPSOIDFIT_AXISTHETA_C	;
	
	public String getName() {
		switch(this) {
		case VARIANCE:
			return "Variance";
		case LOG_VALUE:
			return "LoG Value";
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
