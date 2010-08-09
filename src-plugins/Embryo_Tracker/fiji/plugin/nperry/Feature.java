package fiji.plugin.nperry;

public enum Feature {
	VARIANCE,
	LOG_VALUE,
	BRIGHTNESS,
	CONTRAST;
	
	public double getScore(double value) {
		switch (this) {
			case VARIANCE:
				return 1/value;
			case LOG_VALUE:
				return value;
			case BRIGHTNESS:
				return value;
			case CONTRAST:
				return value;
			default:
				return 0;
		}
	}
}
