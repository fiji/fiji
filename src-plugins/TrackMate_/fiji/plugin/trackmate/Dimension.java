package fiji.plugin.trackmate;

public enum Dimension {
	NONE,
	QUALITY,
	INTENSITY,
	INTENSITY_SQUARED,
	POSITION,
	SIZE,   // we separate size and dimension so that x,y,z are plotted on a different graph from spot sizes
	TIME,
	ANGLE;


}