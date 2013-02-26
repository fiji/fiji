package fiji.plugin.trackmate;

import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * This class is used to store user settings for the {@link TrackMate_} plugin.
 * It is simply made of public fields 
 */
public class Settings {
	
	/** The ImagePlus to operate on. Will also be used by some {@link TrackMateModelView} 
	 * as a GUI target. */
	public ImagePlus imp;
	/** The polygon of interest. This will be used to crop the image and to discard 
	 * found spots out of the polygon. If <code>null</code>, the whole image is 
	 * considered. */
	public Polygon polygon;
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
	/** Target channel for detection, <b>1-based</b>. */
//	public int detectionChannel = 1;
	// Image info
	public double dt 	= 1;
	public double dx 	= 1;
	public double dy 	= 1;
	public double dz 	= 1;
	public int width;
	public int height;
	public int nslices;
	public int nframes;
	public String imageFolder 		= "";
	public String imageFileName 	= "";
	public String timeUnits 		= "frames";
	public String spaceUnits 		= "pixels";
	
	/** The name of the detector factory to use. It will be used to generate {@link SpotDetector}
	 * for each target frame. */
	public SpotDetectorFactory<?> detectorFactory;
	/** The the tracker to use. */
	public SpotTracker tracker;
	
	public Map<String, Object> detectorSettings = new HashMap<String, Object>();
	public Map<String, Object> trackerSettings = new HashMap<String, Object>();
	
	// Filters
	
	/**
	 * The feature filter list that is used to generate {@link #filteredSpots}
	 * from {@link #spots}.
	 */
	protected List<FeatureFilter> spotFilters = new ArrayList<FeatureFilter>();
	/**
	 * The initial quality filter value that is used to clip spots of low
	 * quality from {@link TrackMateModel#spots}.
	 */
	public Double initialSpotFilterValue;
	/** The track filter list that is used to prune track and spots. */
	protected List<FeatureFilter> trackFilters = new ArrayList<FeatureFilter>();
	protected String errorMessage;
	
	
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
		
		if (null == imp) {
			return; // we leave field default values
		}
		
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
			this.polygon = null;
		} else {
			Rectangle boundingRect = roi.getBounds();
			this.xstart = boundingRect.x; 
			this.xend = boundingRect.width;
			this.ystart = boundingRect.y;
			this.yend = boundingRect.height+boundingRect.y;
			this.polygon = roi.getPolygon();
			
		}
		// The rest is left to the user
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * @return a string description of the target image.
	 */
	public String toStringImageInfo() {
		StringBuilder str = new StringBuilder(); 
		
		str.append("Image data:\n");
		if (null == imp) {
			str.append("Source image not set.\n");
		} else {
			str.append("For the image named: "+imp.getTitle() + ".\n");			
		}
		if (imageFileName == null || imageFileName == "") {
			str.append("Not matching any file.\n");
		} else {
			str.append("Matching file " + imageFileName + " ");
			if (imageFolder == null || imageFolder == "") {
				str.append("in current folder.\n");
			} else {
				str.append("in folder: "+imageFolder + "\n");
			}
		}
		
		str.append("Geometry:\n");
		str.append(String.format("  X = %4d - %4d, dx = %g %s\n", xstart, xend, dx, spaceUnits));
		str.append(String.format("  Y = %4d - %4d, dy = %g %s\n", ystart, yend, dy, spaceUnits));
		str.append(String.format("  Z = %4d - %4d, dz = %g %s\n", zstart, zend, dz, spaceUnits));
		str.append(String.format("  T = %4d - %4d, dt = %g %s\n", tstart, tend, dt, timeUnits));

		return str.toString();
	}
	
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(); 
		
		str.append(toStringImageInfo());
		
		str.append('\n');
		str.append("Spot detection:\n");
		if (null == detectorFactory) {
			str.append("No detector factory set.\n");
		} else {
			str.append("Detector: " + detectorFactory.toString() + ".\n");
			if (null == detectorSettings) {
				str.append("No detector settings found.\n");
			} else {
				str.append("Detector settings:\n");
				str.append(detectorSettings);
				str.append('\n');
			}
		}
		
		str.append('\n');
		str.append("Initial spot filter:\n");
		if (null == initialSpotFilterValue) {
			str.append("No initial quality filter.\n");
		} else {
			str.append("Initial quality filter value: " + initialSpotFilterValue + ".\n");
		}

		str.append('\n');
		str.append("Spot feature filters:\n");
		if (spotFilters == null || spotFilters.size() == 0) {
			str.append("No spot feature filters.\n");
		} else {
			str.append("Set with "+spotFilters.size()+" spot feature filters:\n");
			for (FeatureFilter featureFilter : spotFilters) {
				str.append(" - "+featureFilter + "\n");
			}
		}
		
		str.append('\n');
		str.append("Particle linking:\n");
		if (null == tracker) {
			str.append("No spot tracker set.\n");
		} else {
			str.append("Tracker: " + tracker.toString() + ".\n");
			if (null == trackerSettings) {
				str.append("No tracker settings found.\n");
			} else {
				str.append("Tracker settings:\n");
				str.append(trackerSettings);
				str.append('\n');
			}
		}
		
		str.append('\n');
		str.append("Track feature filters:\n");
		if (trackFilters == null || trackFilters.size() == 0) {
			str.append("No track feature filters.\n");
		} else {
			str.append("Set with "+trackFilters.size()+" track feature filters:\n");
			for (FeatureFilter featureFilter : trackFilters) {
				str.append(" - "+featureFilter + "\n");
			}
		}
		
		return str.toString();
	}
	
	public boolean checkValidity() {
		if (null == imp) {
			errorMessage = "The source image is null.\n";
			return false;
		}
		if (null == detectorFactory) {
			errorMessage = "The detector factory is null.\n";
			return false;
		}
		if (null == detectorSettings) {
			errorMessage = "The detector settings is null.\n";
			return false;
		}
		if (null == initialSpotFilterValue) {
			errorMessage = "Initial spot quality threshold is not set.\n";
			return false;
		}
		if (null == tracker) {
			errorMessage = "The tracker in settings is null.\n";
			return false;
		}
		if (!tracker.checkInput()) {
			errorMessage = "The tracker has invalid input:\n"+tracker.getErrorMessage();
			return false;
		}
		return true;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	
	/*
	 * FEATURE FILTERS
	 */

	/**
	 * Add a filter to the list of spot filters to deal with when executing
	 * {@link #execFiltering()}.
	 */
	public void addSpotFilter(final FeatureFilter filter) {
		spotFilters.add(filter);
	}

	public void removeSpotFilter(final FeatureFilter filter) {
		spotFilters.remove(filter);
	}

	/** Remove all spot filters stored in this model. */
	public void clearSpotFilters() {
		spotFilters.clear();
	}

	public List<FeatureFilter> getSpotFilters() {
		return spotFilters;
	}

	public void setSpotFilters(List<FeatureFilter> spotFilters) {
		this.spotFilters = spotFilters;
	}

	/** Add a filter to the list of track filters. */
	public void addTrackFilter(final FeatureFilter filter) {
		trackFilters.add(filter);
	}

	public void removeTrackFilter(final FeatureFilter filter) {
		trackFilters.remove(filter);
	}

	/** Remove all track filters stored in this model. */
	public void clearTrackFilters() {
		trackFilters.clear();
	}

	public List<FeatureFilter> getTrackFilters() {
		return trackFilters;
	}

	public void setTrackFilters(List<FeatureFilter> trackFilters) {
		this.trackFilters = trackFilters;
	}


}