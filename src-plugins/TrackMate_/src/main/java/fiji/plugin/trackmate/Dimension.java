package fiji.plugin.trackmate;

public enum Dimension {
	NONE,
	QUALITY,
	INTENSITY,
	INTENSITY_SQUARED,
	POSITION,
	VELOCITY,
	LENGTH,   // we separate length and position so that x,y,z are plotted on a different graph from spot sizes
	TIME,
	ANGLE, 
	STRING; // for non-numeric features


}