package fiji.plugin.trackmate;

public enum TrackFeature {
	TRACK_DURATION,
	NUMBER_SPOTS,
	NUMBER_MERGES,
	NUMBER_SPLITS,
	NUMBER_GAPS;
	
	
	@Override
	public String toString() {
		switch (this) {
		case NUMBER_GAPS:
			return "Number of gaps";
		case NUMBER_MERGES:
			return "Number of merge events";
		case NUMBER_SPLITS:
			return "Number of split events";
		case NUMBER_SPOTS:
			return "Number of spots in track";
		case TRACK_DURATION:
			return "Duration of track";
		default: 
			return null;
		}
	}
	
	public String shortName() {
		switch (this) {
		case NUMBER_GAPS:
			return "Gaps";
		case NUMBER_MERGES:
			return "Merges";
		case NUMBER_SPLITS:
			return "Splits";
		case NUMBER_SPOTS:
			return "N spots";
		case TRACK_DURATION:
			return "Duration"; 
		default:
			return null;
		}
	}
	
	public Dimension getDimension() {
		switch (this) {
		default:
			return Dimension.NONE;
		case TRACK_DURATION:
			return Dimension.TIME;
		}
	}
}
