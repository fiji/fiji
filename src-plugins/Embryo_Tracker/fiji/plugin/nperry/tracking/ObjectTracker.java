package fiji.plugin.nperry.tracking;

import java.util.ArrayList;

import fiji.plugin.nperry.Spot;

public class ObjectTracker {

	/** Holds the extrema for each frame. Each index is a separate frame. */
	ArrayList< ArrayList<Spot> > extrema;
	/** Returned to the user. Each inner ArrayList constitutes a track, and holds a double[] of length four, which contains the coordinates [x,y,z,t] */
	ArrayList< ArrayList<double[] > > tracks; 
	/** Holds the scores for each link in the track. There are t-1 arraylists, such that index i contains the score for connected frame i to frame i+1. */
	ArrayList< ArrayList<double[] > > scores; 
	
	public ObjectTracker(ArrayList< ArrayList<Spot> >extrema) {
		this.extrema = extrema;
	}
	
	public ArrayList< ArrayList<double[] > > getTracks() {
		return tracks;
	}
	
	public void process() {
		
	}
	
}
