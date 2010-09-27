package fiji.plugin.trackmate;

import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import ij.ImagePlus;

/**
 * This class is used to store user settings for the {@link TrackMate_} plugin.
 * It is simply made of public fields 
 */
public class Settings {
	
	private static final float 		DEFAULT_EXPECTED_DIAMETER	= 6.5f;
	
			
	/** The ImagePlus to operate on. */
	public ImagePlus imp;
	/** Stores the expected blob diameter in physical units. */
	public float 	expectedDiameter = DEFAULT_EXPECTED_DIAMETER;
	
	public int tstart;
	public int tend;
	public int xstart;
	public int xend;
	public int ystart;
	public int yend;
	public int zstart;
	public int zend;
	
	
	public static final Settings DEFAULT = new Settings();
	
	public SegmenterSettings segmenterSettings = null;
}