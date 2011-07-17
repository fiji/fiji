package fiji.plugin.trackmate;

/**
 * Enumerate the supported feature for spots.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010-2011
 *
 */
public enum SpotFeature {
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
	RADIUS, 
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
		case RADIUS:
			return "Radius";
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
			return "Ellipsoid A axis φ azimuth";
		case ELLIPSOIDFIT_AXISPHI_B:
			return "Ellipsoid B axis φ azimuth";
		case ELLIPSOIDFIT_AXISPHI_C:
			return "Ellipsoid C axis φ azimuth";
		case ELLIPSOIDFIT_AXISTHETA_A:
			return "Ellipsoid A axis θ zenith";
		case ELLIPSOIDFIT_AXISTHETA_B:
			return "Ellipsoid B axis θ zenith";
		case ELLIPSOIDFIT_AXISTHETA_C:
			return "Ellipsoid C axis θ zenith";
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
	
	public String shortName() {
		switch(this) {
		case CONTRAST:
			return "Contrast";
		case ELLIPSOIDFIT_AXISPHI_A:
			return "φa";
		case ELLIPSOIDFIT_AXISPHI_B:
			return "φb";
		case ELLIPSOIDFIT_AXISPHI_C:
			return "φc";
		case ELLIPSOIDFIT_AXISTHETA_A:
			return "θb";
		case ELLIPSOIDFIT_AXISTHETA_B:
			return "θb";
		case ELLIPSOIDFIT_AXISTHETA_C:
			return "θc";
		case ELLIPSOIDFIT_SEMIAXISLENGTH_A:
			return "la";
		case ELLIPSOIDFIT_SEMIAXISLENGTH_B:
			return "lb";
		case ELLIPSOIDFIT_SEMIAXISLENGTH_C:
			return "lc";
		case ESTIMATED_DIAMETER:
			return "Diam.";
		case KURTOSIS:
			return "Kurtosis";
		case MAX_INTENSITY:
			return "Max";
		case MEAN_INTENSITY:
			return "Mean";
		case MEDIAN_INTENSITY:
			return "Median";
		case MIN_INTENSITY:
			return "Min";
		case MORPHOLOGY:
			return "Morpho.";
		case POSITION_T:
			return "T";
		case POSITION_X:
			return "X";
		case POSITION_Y:
			return "Y";
		case POSITION_Z:
			return "Z";
		case RADIUS:
			return "R";
		case QUALITY:
			return "Quality";
		case SKEWNESS:
			return "Skewness";
		case STANDARD_DEVIATION:
			return "Stdev.";
		case TOTAL_INTENSITY:
			return "Total int.";
		case VARIANCE:
			return "Var.";
		default:
			return null;
		}
	}
	
	public Dimension getDimension() {
		switch (this) {
		case POSITION_X:
		case POSITION_Y:
		case POSITION_Z:
			return  Dimension.POSITION;
		case QUALITY:
			return Dimension.QUALITY;
		case MORPHOLOGY:
		case CONTRAST:
		case KURTOSIS:
		case SKEWNESS:
		default:
			return Dimension.NONE;
		case MEAN_INTENSITY:
		case MEDIAN_INTENSITY:
		case MIN_INTENSITY:
		case MAX_INTENSITY:
		case TOTAL_INTENSITY:
		case STANDARD_DEVIATION:
			return Dimension.INTENSITY;
		case VARIANCE:
			return Dimension.INTENSITY_SQUARED;
		case ELLIPSOIDFIT_AXISPHI_A:
		case ELLIPSOIDFIT_AXISPHI_B:
		case ELLIPSOIDFIT_AXISPHI_C:
		case ELLIPSOIDFIT_AXISTHETA_A:
		case ELLIPSOIDFIT_AXISTHETA_B:
		case ELLIPSOIDFIT_AXISTHETA_C:
			return Dimension.ANGLE;
		case ELLIPSOIDFIT_SEMIAXISLENGTH_A:
		case ELLIPSOIDFIT_SEMIAXISLENGTH_B:
		case ELLIPSOIDFIT_SEMIAXISLENGTH_C:
		case ESTIMATED_DIAMETER:
		case RADIUS:
			return Dimension.LENGTH;
		case POSITION_T:
			return Dimension.TIME;		
		}
	}

}
