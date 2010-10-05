package fiji.plugin.trackmate;


public enum Feature {
	QUALITY,
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
	POSITION_X,
	POSITION_Y,
	POSITION_Z,
	POSITION_T,
	ELLIPSOIDFIT_SEMIAXISLENGTH_A,
	ELLIPSOIDFIT_SEMIAXISLENGTH_B,
	ELLIPSOIDFIT_SEMIAXISLENGTH_C,
	ELLIPSOIDFIT_AXISPHI_A,
	ELLIPSOIDFIT_AXISPHI_B,
	ELLIPSOIDFIT_AXISPHI_C,
	ELLIPSOIDFIT_AXISTHETA_A,
	ELLIPSOIDFIT_AXISTHETA_B,
	ELLIPSOIDFIT_AXISTHETA_C	;
	
	public String toString() {
		switch(this) {
		case POSITION_X:
			return "X";
		case POSITION_Y:
			return "Y";
		case POSITION_Z:
			return "Z";
		case POSITION_T:
			return "T";
		case VARIANCE:
			return "Variance";
		case QUALITY:
			return "Quality";
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
		case ELLIPSOIDFIT_AXISPHI_A:
			return "Ellipsoid A axis φ azimuth (rad)";
		case ELLIPSOIDFIT_AXISPHI_B:
			return "Ellipsoid B axis φ azimuth (rad)";
		case ELLIPSOIDFIT_AXISPHI_C:
			return "Ellipsoid C axis φ azimuth (rad)";
		case ELLIPSOIDFIT_AXISTHETA_A:
			return "Ellipsoid A axis θ zenith (rad)";
		case ELLIPSOIDFIT_AXISTHETA_B:
			return "Ellipsoid B axis θ zenith (rad)";
		case ELLIPSOIDFIT_AXISTHETA_C:
			return "Ellipsoid C axis θ zenith (rad)";
		case ELLIPSOIDFIT_SEMIAXISLENGTH_A:
			return "Ellipsoid A semi-axis length";
		case ELLIPSOIDFIT_SEMIAXISLENGTH_B:
			return "Ellipsoid B semi-axis length";
		case ELLIPSOIDFIT_SEMIAXISLENGTH_C:
			return "Ellipsoid C semi-axis length";
		default:
			return null;
		}
	}

}
