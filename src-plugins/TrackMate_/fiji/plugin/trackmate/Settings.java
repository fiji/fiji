package fiji.plugin.trackmate;

import java.util.List;
import java.util.TreeMap;

import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.gui.ActionListenablePanel;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.SegmenterSettingsPanel;
import fiji.plugin.trackmate.gui.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.TrackerSettingsPanel;
import fiji.plugin.trackmate.segmentation.DogSegmenter;
import fiji.plugin.trackmate.segmentation.LogSegmenter;
import fiji.plugin.trackmate.segmentation.LogSegmenterSettings;
import fiji.plugin.trackmate.segmentation.PeakPickerSegmenter;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import ij.ImagePlus;

/**
 * This class is used to store user settings for the {@link TrackMate_} plugin.
 * It is simply made of public fields 
 */
public class Settings {
	
	
			
	/** The ImagePlus to operate on. */
	public ImagePlus imp;
	// Crop cube
	public int tstart;
	public int tend;
	public int xstart;
	public int xend;
	public int ystart;
	public int yend;
	public int zstart;
	public int zend;
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
	
	public SegmenterType segmenterType = SegmenterType.PEAKPICKER_SEGMENTER;
	public TrackerType trackerType = TrackerType.LAP_TRACKER;
	
	
	public enum SegmenterType {
		PEAKPICKER_SEGMENTER,
		LOG_SEGMENTER,
		DOG_SEGMENTER;
		
		@Override
		public String toString() {
			switch(this) {
			case LOG_SEGMENTER:
				return "Downsample LoG segmenter";
			case PEAKPICKER_SEGMENTER:
				return "LoG segmenter";
			case DOG_SEGMENTER:
				return "DoG segmenter";
			}
			return null;
		}

		/**
		 * Create a new {@link SegmenterSettings} object suited to the {@link SpotSegmenter} referenced by this enum.
		 */
		public SegmenterSettings createSettings() {
			switch(this) {
			case LOG_SEGMENTER: {
				LogSegmenterSettings s =  new LogSegmenterSettings();
				s.segmenterType = LOG_SEGMENTER;
				return s;
			}
			case PEAKPICKER_SEGMENTER:
			case DOG_SEGMENTER: 
			{
				SegmenterSettings s = new SegmenterSettings();
				s.segmenterType = this;
				return s;
			}
			}
			return null;
		}

		public String getInfoText() {
			switch(this) {
			case PEAKPICKER_SEGMENTER:
				return "<html>" +
						"This segmenter applies a LoG (Laplacian of Gaussian) filter <br>" +
						"to the image, with a sigma suited to the blob estimated size.<br>" +
						"Calculations are made in the Fourier space. The maxima in the <br>" +
						"filtered image are searched for, and maxima too close from each <br>" +
						"other are suppressed." +
						"</html>";
			case LOG_SEGMENTER:
				return "<html>" +
						"This segmenter is basically identical to the LoG segmenter, except <br>" +
						"that images are downsampled before filtering, giving it a small <br>" +
						"kick in speed." +
						"</html>";
			case DOG_SEGMENTER:
				return "<html>" +
						"This segmented is based on an approximation of the LoG operator <br>" +
						"by differences of gaussian (DoG)." +
						"</html>";
			}
			return null;
		}
	}
	
	
	public enum TrackerType {
		SIMPLE_LAP_TRACKER,
		LAP_TRACKER;
		
		@Override
		public String toString() {
			switch(this) {
			case SIMPLE_LAP_TRACKER:
				return "Simple LAP tracker";
			case LAP_TRACKER:
				return "LAP tracker";
			}
			return null;
		}

		/**
		 * Create a new {@link TrackerSettings} object suited to the {@link SpotTracker} referenced by this enum.
		 */
		public TrackerSettings createSettings() {
			switch(this) {
			case LAP_TRACKER:
				return new TrackerSettings();
			case SIMPLE_LAP_TRACKER:
				TrackerSettings ts = new TrackerSettings();
				ts.allowMerging = false;
				ts.allowSplitting = false;
				return ts;
			}
			return null;
		}

		public String getInfoText() {
			switch(this) {
			case LAP_TRACKER:
				return "<html>" +
						"This tracker is based on the Linear Assignment Problem mathematical framework. <br>" +
						"Its implementation is derived from the following paper: <br>" +
						"<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>" +
						"Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>" +
						" </html>";
			case SIMPLE_LAP_TRACKER:
				return "<html>" +
					"This tracker is identical to the LAP tracker present in this plugin, except that it <br>" +
					"proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" +
					"a distance and time condition. Track splitting and merging are not allowed, resulting <br>" +
					"in having non-branching tracks." +
				" </html>";
			}
			
			return null;
		}
	}
	
	
	public static final Settings DEFAULT = new Settings();
	
	public SegmenterSettings segmenterSettings = new SegmenterSettings();
	public TrackerSettings trackerSettings = new TrackerSettings();
	
	/*
	 * METHODS
	 */
	
	/**
	 * Return a new {@link SpotSegmenter} as selected in this settings object.
	 */
	public <T extends RealType<T>> SpotSegmenter<T> getSpotSegmenter() {
		switch(segmenterType) {
		case LOG_SEGMENTER:
			return new LogSegmenter<T>((LogSegmenterSettings) segmenterSettings);
		case PEAKPICKER_SEGMENTER:
			return new PeakPickerSegmenter<T>(segmenterSettings);
		case DOG_SEGMENTER:
			return new DogSegmenter<T>(segmenterSettings);
		}
		return null;
	}
	
	/**
	 * Return a new {@link SpotTracker} as selected in this settings object. 
	 */
	public SpotTracker getSpotTracker(TreeMap<Integer, List<Spot>> spots) {
		switch(trackerType) {
		case LAP_TRACKER:
		case SIMPLE_LAP_TRACKER:
			return new LAPTracker(spots, trackerSettings);
		}
		return null;
	}
	
	
	@Override
	public String toString() {
		String str = ""; 
		if (null == imp) {
			str = "Image with:\n";
		} else {
			str = "For image: "+imp.getShortTitle()+'\n';			
		}
		str += String.format("  X = %4d - %4d, dx = %.1f %s\n", xstart, xend, dx, spaceUnits);
		str += String.format("  Y = %4d - %4d, dy = %.1f %s\n", ystart, yend, dy, spaceUnits);
		str += String.format("  Z = %4d - %4d, dz = %.1f %s\n", zstart, zend, dz, spaceUnits);
		str += String.format("  T = %4d - %4d, dt = %.1f %s\n", tstart, tend, dt, timeUnits);
		return str;
	}

	/**
	 * Return a {@link SegmenterSettingsPanel} that is able to configure the {@link SpotSegmenter}
	 * selected in this settings object.
	 */
	public SegmenterSettingsPanel createSegmenterSettingsPanel() {
		switch (segmenterType) {
		case DOG_SEGMENTER:
		case LOG_SEGMENTER:
		case PEAKPICKER_SEGMENTER:
			return new SegmenterSettingsPanel(segmenterSettings);
		}
		return null;
	}

	/** 
	 * Return a {@link TrackerSettingsPanel} that is able to configure the {@link SpotTracker}
	 * selected in this settings object.
	 */
	public TrackerSettingsPanel createTrackerSettingsPanel() {
		switch (trackerType) {
		case LAP_TRACKER:
			return new LAPTrackerSettingsPanel(trackerSettings);
		case SIMPLE_LAP_TRACKER:
			return new SimpleLAPTrackerSettingsPanel(trackerSettings);
		}
		return null;
	}
	
	
	
	
	
	
	
}