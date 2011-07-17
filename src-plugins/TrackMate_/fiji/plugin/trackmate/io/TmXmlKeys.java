package fiji.plugin.trackmate.io;


/**
 * Contains the key string used for xml marshaling.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep - Dec, 2010
  */
public interface TmXmlKeys {
	
	/*
	 * GENERIC ATTRIBUTES
	 */
	
	public static final String FRAME_ATTRIBUTE_NAME 				= "frame"; 
	public static final String SPOT_ID_ATTRIBUTE_NAME 				= "ID";
	public static final String SPOT_NAME_ATTRIBUTE_NAME 			= "name";
	
	/*
	 * ROOT ELEMENT
	 */

	public static final String ROOT_ELEMENT_KEY 					= "TrackMate";
	public static final String PLUGIN_VERSION_ATTRIBUTE_NAME		= "version";
	
	
	/*
	 * SETTINGS elements
	 */
	
	public static final String SETTINGS_ELEMENT_KEY 				= "BasicSettings";
	public static final String SETTINGS_XSTART_ATTRIBUTE_NAME 		= "xstart";
	public static final String SETTINGS_YSTART_ATTRIBUTE_NAME 		= "ystart";
	public static final String SETTINGS_ZSTART_ATTRIBUTE_NAME 		= "zstart";
	public static final String SETTINGS_TSTART_ATTRIBUTE_NAME 		= "tstart";
	public static final String SETTINGS_XEND_ATTRIBUTE_NAME 		= "xend";
	public static final String SETTINGS_YEND_ATTRIBUTE_NAME 		= "yend";
	public static final String SETTINGS_ZEND_ATTRIBUTE_NAME 		= "zend";
	public static final String SETTINGS_TEND_ATTRIBUTE_NAME 		= "tend";

	/*
	 * SEGMENTER SETTINGS
	 */
	
	public static final String SEGMENTER_SETTINGS_ELEMENT_KEY 						= "SegmenterSettings";
	public static final String SEGMENTER_SETTINGS_SEGMENTER_TYPE_ATTRIBUTE_NAME 	= "segmentertype";
	public static final String SEGMENTER_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME 	= "expectedradius";
	public static final String SEGMENTER_SETTINGS_UNITS_ATTRIBUTE_NAME 				= "units";
	public static final String SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME 			= "threshold";
	public static final String SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME			= "usemedianfilter";

	/*
	 * TRACKER SETTINGS
	 */
	
	public static final String TRACKER_SETTINGS_ELEMENT_KEY							= "TrackerSettings";
	public static final String TRACKER_SETTINGS_TRACKER_TYPE_ATTRIBUTE_NAME			= "trackertype";
	public static final String TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME					= "allowed";
	// Alternative costs & blocking
	public static final String TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME 		= "alternatecostfactor";
	public static final String TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME			= "cutoffpercentile";
	public static final String TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME				= "blockingvalue";
	// Units
	public static final String TRACKER_SETTINGS_TIME_UNITS_ATTNAME 	= "timeunits";
	public static final String TRACKER_SETTINGS_SPACE_UNITS_ATTNAME = "spaceunits";
	// Cutoff elements
	public static final String TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT					= "TimeCutoff";
	public static final String TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME					= "value";
	public static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT				= "DistanceCutoff";
	public static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME				= "value";
	public static final String TRACKER_SETTINGS_FEATURE_ELEMENT						= "FeatureCondition";
	public static final String TRACKER_SETTINGS_LINKING_ELEMENT						= "LinkingCondition";
	public static final String TRACKER_SETTINGS_GAP_CLOSING_ELEMENT					= "GapClosingCondition";
	public static final String TRACKER_SETTINGS_MERGING_ELEMENT						= "MergingCondition";
	public static final String TRACKER_SETTINGS_SPLITTING_ELEMENT					= "SplittingCondition";
	
	/*
	 * IMAGE element
	 */
	
	public static final String IMAGE_ELEMENT_KEY 					= "ImageData";
	public static final String IMAGE_FILENAME_ATTRIBUTE_NAME 		= "filename";
	public static final String IMAGE_FOLDER_ATTRIBUTE_NAME 			= "folder";
	public static final String IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME		= "pixelwidth";
	public static final String IMAGE_WIDTH_ATTRIBUTE_NAME 			= "width";
	public static final String IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME 	= "pixelheight";
	public static final String IMAGE_HEIGHT_ATTRIBUTE_NAME 			= "height";
	public static final String IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME 	= "voxeldepth";
	public static final String IMAGE_NSLICES_ATTRIBUTE_NAME 		= "nslices";
	public static final String IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME 	= "timeinterval";
	public static final String IMAGE_NFRAMES_ATTRIBUTE_NAME 		= "nframes";
	public static final String IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME 	= "spatialunits";
	public static final String IMAGE_TIME_UNITS_ATTRIBUTE_NAME 		= "timeunits";
	
	/*
	 * ALL SPOTS element
	 */

	public static final String SPOT_COLLECTION_ELEMENT_KEY 			= "AllSpots";
	public static final String SPOT_FRAME_COLLECTION_ELEMENT_KEY 	= "SpotsInFrame"; 
	public static final String SPOT_ELEMENT_KEY 					= "Spot"; 

	/*
	 * INITIAL SPOT FILTER element
	 */
	
	public static final String INITIAL_SPOT_FILTER_ELEMENT_KEY		= "InitialSpotFilter";
	
	/*
	 * FILTERS element for SPOTS and TRACKS
	 */
	
	public static final String SPOT_FILTER_COLLECTION_ELEMENT_KEY		= "SpotFilterCollection";
	public static final String TRACK_FILTER_COLLECTION_ELEMENT_KEY		= "TrackFilterCollection";
	public static final String FILTER_ELEMENT_KEY						= "Filter";
	public static final String FILTER_FEATURE_ATTRIBUTE_NAME 			= "feature";
	public static final String FILTER_VALUE_ATTRIBUTE_NAME 				= "value";
	public static final String FILTER_ABOVE_ATTRIBUTE_NAME 				= "isabove";
	
	/*
	 * SPOT FILTERED elements
	 */
	
	public static final String FILTERED_SPOT_ELEMENT_KEY 				= "FilteredSpots";
	public static final String FILTERED_SPOT_COLLECTION_ELEMENT_KEY 	= "FilteredSpotsInFrame";
	public static final String SPOT_ID_ELEMENT_KEY 						= "SpotID";

	/*
	 * TRACK elements
	 */
	
	public static final String TRACK_COLLECTION_ELEMENT_KEY			= "AllTracks";
	public static final String TRACK_ELEMENT_KEY 					= "Track";
	public static final String TRACK_ID_ATTRIBUTE_NAME 				= "trackID";

	public static final String TRACK_EDGE_ELEMENT_KEY				= "Edge";
	public static final String TRACK_EDGE_SOURCE_ATTRIBUTE_NAME	 	= "sourceID";
	public static final String TRACK_EDGE_TARGET_ATTRIBUTE_NAME	 	= "targetID";
	public static final String TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME	 	= "weight";

	
	/*
	 * TRACK FILTERED elements
	 */
	
	public static final String FILTERED_TRACK_ELEMENT_KEY 				= "FilteredTracks";
	public static final String TRACK_ID_ELEMENT_KEY 					= "TrackID";
	
}
