package fiji.plugin.trackmate;

public enum TrackFeature {
	TRACK_DURATION,
	TRACK_START,
	TRACK_STOP,
	TRACK_DISPLACEMENT,
	NUMBER_SPOTS,
	NUMBER_MERGES,
	NUMBER_SPLITS,
	NUMBER_COMPLEX, 
	TRACK_MEDIAN_SPEED, 
	TRACK_MIN_SPEED, 
	TRACK_MAX_SPEED, 
	TRACK_MEAN_SPEED, 
	TRACK_SPEED_STANDARD_DEVIATION, 
	TRACK_SPEED_KURTOSIS, 
	TRACK_SPEED_SKEWNESS;
	
	
	@Override
	public String toString() {
		switch (this) {
		case NUMBER_COMPLEX:
			return "Complex points";
		case NUMBER_MERGES:
			return "Number of merge events";
		case NUMBER_SPLITS:
			return "Number of split events";
		case NUMBER_SPOTS:
			return "Number of spots in track";
		case TRACK_DURATION:
			return "Duration of track";
		case TRACK_START:
			return "Track start";
		case TRACK_STOP:
			return "Track stop";
		case TRACK_DISPLACEMENT:
			return "Track displacement";
		case TRACK_MAX_SPEED:
			return "Maximal velocity";
		case TRACK_MEAN_SPEED:
			return "Mean velocity";
		case TRACK_MEDIAN_SPEED:
			return "Median velocity";
		case TRACK_MIN_SPEED:
			return "Minimal velocity";
		case TRACK_SPEED_KURTOSIS:
			return "Velocity kurtosis";
		case TRACK_SPEED_SKEWNESS:
			return "Velocity skewness";
		case TRACK_SPEED_STANDARD_DEVIATION:
			return "Velocity standard deviation";
		default: 
			return null;
		}
	}
	
	public String shortName() {
		switch (this) {
		case NUMBER_COMPLEX:
			return "Complex";
		case NUMBER_MERGES:
			return "Merges";
		case NUMBER_SPLITS:
			return "Splits";
		case NUMBER_SPOTS:
			return "N spots";
		case TRACK_DURATION:
			return "Duration";
		case TRACK_START:
			return "T start";
		case TRACK_STOP:
			return "T stop";
		case TRACK_DISPLACEMENT:
			return "Displacement";
		case TRACK_MAX_SPEED:
			return "Max V";
		case TRACK_MEAN_SPEED:
			return "Mean V";
		case TRACK_MEDIAN_SPEED:
			return "Median V";
		case TRACK_MIN_SPEED:
			return "Min V";
		case TRACK_SPEED_KURTOSIS:
			return "V kurtosis";
		case TRACK_SPEED_SKEWNESS:
			return "V skewness";
		case TRACK_SPEED_STANDARD_DEVIATION:
			return "V std";
		default:
			return null;
		}
	}
	
	public Dimension getDimension() {
		switch (this) {
		default:
			return null;
		case NUMBER_COMPLEX:
		case NUMBER_MERGES:
		case NUMBER_SPLITS:
		case NUMBER_SPOTS:
			return Dimension.NONE;
		case TRACK_DURATION:
		case TRACK_START:
		case TRACK_STOP:
			return Dimension.TIME;
		case TRACK_DISPLACEMENT:
			return Dimension.LENGTH;
		case TRACK_MAX_SPEED:
		case TRACK_MEAN_SPEED:
		case TRACK_MEDIAN_SPEED:
		case TRACK_MIN_SPEED:
		case TRACK_SPEED_STANDARD_DEVIATION:
			return Dimension.VELOCITY;
		case TRACK_SPEED_KURTOSIS:
		case TRACK_SPEED_SKEWNESS:
			return Dimension.NONE;
			
		}
	}
}
