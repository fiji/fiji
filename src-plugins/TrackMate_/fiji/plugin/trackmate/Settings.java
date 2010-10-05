package fiji.plugin.trackmate;

import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import ij.ImagePlus;

/**
 * This class is used to store user settings for the {@link TrackMate_} plugin.
 * It is simply made of public fields 
 */
public class Settings {
	
	
			
	/** The ImagePlus to operate on. */
	public ImagePlus imp;
	
	public int tstart;
	public int tend;
	public int xstart;
	public int xend;
	public int ystart;
	public int yend;
	public int zstart;
	public int zend;
	
	
	public static final Settings DEFAULT = new Settings();
	
	public SegmenterSettings segmenterSettings = new SegmenterSettings();
	public TrackerSettings trackerSettings = new TrackerSettings();
}