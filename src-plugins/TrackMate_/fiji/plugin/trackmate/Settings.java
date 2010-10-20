package fiji.plugin.trackmate;

import java.util.List;
import java.util.TreeMap;

import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.gui.SegmenterSettingsPanel;
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
	
	public int tstart;
	public int tend;
	public int xstart;
	public int xend;
	public int ystart;
	public int yend;
	public int zstart;
	public int zend;
	
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
		LAP_TRACKER;
		
		@Override
		public String toString() {
			switch(this) {
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
			return new LAPTracker(spots, trackerSettings);
		}
		return null;
	}
	
	
	@Override
	public String toString() {
		String str = "For image: "+imp.getShortTitle()+'\n';
		str += String.format("  X = %4d - %4d\n", xstart, xend);
		str += String.format("  Y = %4d - %4d\n", ystart, yend);
		str += String.format("  Z = %4d - %4d\n", zstart, zend);
		str += String.format("  T = %4d - %4d\n", tstart, tend);
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
			return new TrackerSettingsPanel(trackerSettings);
		}
		return null;
	}
	
	
	
	
	
	
	
}