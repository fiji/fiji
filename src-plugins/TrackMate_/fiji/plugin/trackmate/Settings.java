package fiji.plugin.trackmate;

import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.Rectangle;

import mpicbg.imglib.type.numeric.RealType;

/**
 * This class is used to store user settings for the {@link TrackMate_} plugin.
 * It is simply made of public fields 
 */
public class Settings {
	
	public static final Settings DEFAULT = new Settings();
			
	/** The ImagePlus to operate on. */
	public ImagePlus imp;
	// Crop cube
	/** The time-frame index, <b>0-based</b>, of the first time-point to process. */
	public int tstart;
	/** The time-frame index, <b>0-based</b>, of the last time-point to process. */
	public int tend;
	/** The lowest pixel X position, <b>0-based</b>, of the volume to process. */
	public int xstart;
	/** The highest pixel X position, <b>0-based</b>, of the volume to process. */
	public int xend;
	/** The lowest pixel Y position, <b>0-based</b>, of the volume to process. */
	public int ystart;
	/** The lowest pixel Y position, <b>0-based</b>, of the volume to process. */
	public int yend;
	/** The lowest pixel Z position, <b>0-based</b>, of the volume to process. */
	public int zstart;
	/** The lowest pixel Z position, <b>0-based</b>, of the volume to process. */
	public int zend;
	/** Target channel for segmentation, <b>1-based</b>. */
	public int segmentationChannel = 1;
	// Image info
	public float dt 	= 1;
	public float dx 	= 1;
	public float dy 	= 1;
	public float dz 	= 1;
	public int width;
	public int height;
	public int nslices;
	public int nframes;
	public String imageFolder 		= "";
	public String imageFileName 	= "";
	public String timeUnits 		= "frames";
	public String spaceUnits 		= "pixels";
	
	public SpotSegmenter<? extends RealType<?>> segmenter;
	public SpotTracker tracker;
	
	public SegmenterSettings segmenterSettings = null;
	public TrackerSettings trackerSettings = null;
	
	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Default empty constructor.
	 */
	public Settings() {	}
	
	/**
	 * Create a new settings object, with some fields set according to the given imp.
	 * @param imp
	 */
	public Settings(ImagePlus imp) {
		// Source image
		this.imp = imp;
		// File info
		this.imageFileName = imp.getFileInfo().fileName;
		this.imageFolder = imp.getFileInfo().directory;
		// Image size
		this.width = imp.getWidth();
		this.height = imp.getHeight();
		this.nslices = imp.getNSlices();
		this.nframes = imp.getNFrames();
		this.dx = (float) imp.getCalibration().pixelWidth;
		this.dy = (float) imp.getCalibration().pixelHeight;
		this.dz = (float) imp.getCalibration().pixelDepth;
		this.dt = (float) imp.getCalibration().frameInterval;
		this.spaceUnits = imp.getCalibration().getUnit();
		this.timeUnits = imp.getCalibration().getTimeUnit();
		
		if (dt == 0) {
			dt = 1;
			timeUnits = "frame";
		}
		
		// Crop cube
		this.zstart = 0;
		this.zend = imp.getNSlices()-1;
		this.tstart = 0; 
		this.tend = imp.getNFrames()-1;
		Roi roi = imp.getRoi();
		if (roi == null) {
			this.xstart = 0;
			this.xend = width-1;
			this.ystart = 0;
			this.yend = height-1;
		} else {
			Rectangle boundingRect = roi.getBounds();
			this.xstart = boundingRect.x; 
			this.xend = boundingRect.width;
			this.ystart = boundingRect.y;
			this.yend = boundingRect.height+boundingRect.y;
		}
		// The rest is left to the user
	}
	
	/*
	 * METHODS
	 */
		
	/**
	 * A utility method that returns a new float array with the 3 elements building the spatial calibration
	 * (pixel size).
	 */
	public float[] getCalibration() {
		return new float[] {dx, dy, dz};
	}
	
	
	@Override
	public String toString() {
		String str = ""; 
		if (null == imp) {
			str = "Image with:\n";
		} else {
			str = "For image: "+imp.getShortTitle()+'\n';			
		}
		str += String.format("  X = %4d - %4d, dx = %g %s\n", xstart, xend, dx, spaceUnits);
		str += String.format("  Y = %4d - %4d, dy = %g %s\n", ystart, yend, dy, spaceUnits);
		str += String.format("  Z = %4d - %4d, dz = %g %s\n", zstart, zend, dz, spaceUnits);
		str += String.format("  T = %4d - %4d, dt = %g %s\n", tstart, tend, dt, timeUnits);
		str += String.format("  Target channel for segmentation: %d\n", segmentationChannel);
		return str;
	}

}