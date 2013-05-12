package fiji.plugin.trackmate.io;


/**
 * Contains the key string used for xml marshaling.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>  2010-2011
  */
class TmXmlKeys {
	
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
	public static final String MODEL_ELEMENT_KEY 					= "Model";
	public static final String SETTINGS_ELEMENT_KEY 				= "Settings";
	public static final String SPATIAL_UNITS_ATTRIBUTE_NAME 		= "spatialunits";
	public static final String TIME_UNITS_ATTRIBUTE_NAME 			= "timeunits";

	
	
	/*
	 * LOG
	 */
	
	public static final String LOG_ELEMENT_KEY 						= "Log";
	
	/*
	 * SETTINGS elements
	 */
	
	public static final String CROP_ELEMENT_KEY 				= "BasicSettings";
	public static final String CROP_XSTART_ATTRIBUTE_NAME 		= "xstart";
	public static final String CROP_YSTART_ATTRIBUTE_NAME 		= "ystart";
	public static final String CROP_ZSTART_ATTRIBUTE_NAME 		= "zstart";
	public static final String CROP_TSTART_ATTRIBUTE_NAME 		= "tstart";
	public static final String CROP_XEND_ATTRIBUTE_NAME 		= "xend";
	public static final String CROP_YEND_ATTRIBUTE_NAME 		= "yend";
	public static final String CROP_ZEND_ATTRIBUTE_NAME 		= "zend";
	public static final String CROP_TEND_ATTRIBUTE_NAME 		= "tend";
	public static final String CROP_DETECTION_CHANNEL_ATTRIBUTE_NAME 		= "detectionchannel";

	/*
	 * DETECTOR SETTINGS
	 */
	
	public static final String DETECTOR_SETTINGS_ELEMENT_KEY 			= "DetectorSettings";

	/*
	 * TRACKER SETTINGS
	 */
	
	public static final String TRACKER_SETTINGS_ELEMENT_KEY				= "TrackerSettings";
	public static final String TRACKER_SETTINGS_CLASS_ATTRIBUTE_NAME	= "trackersettingsclass";
	public static final String TRACKER_ATTRIBUTE_NAME					= "trackername";
	
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
	
	/*
	 * ALL SPOTS element
	 */

	public static final String SPOT_COLLECTION_ELEMENT_KEY 				= "AllSpots";
	public static final String SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME 	= "nspots";
	public static final String SPOT_FRAME_COLLECTION_ELEMENT_KEY 		= "SpotsInFrame"; 
	public static final String SPOT_ELEMENT_KEY 						= "Spot"; 

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
	 * TRACK elements
	 */
	
	public static final String TRACK_COLLECTION_ELEMENT_KEY			= "AllTracks";
	public static final String TRACK_ELEMENT_KEY 					= "Track";
//	public static final String TRACK_ID_ATTRIBUTE_NAME 				= "trackID";
	public static final String TRACK_NAME_ATTRIBUTE_NAME 			= "name";

	public static final String TRACK_EDGE_ELEMENT_KEY				= "Edge";
//	public static final String TRACK_EDGE_SOURCE_ATTRIBUTE_NAME	 	= "sourceID";
//	public static final String TRACK_EDGE_TARGET_ATTRIBUTE_NAME	 	= "targetID";
//	public static final String TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME	 	= "weight";

	
	/*
	 * TRACK FILTERED elements
	 */
	
	public static final String FILTERED_TRACK_ELEMENT_KEY 				= "FilteredTracks";
	public static final String TRACK_ID_ELEMENT_KEY 					= "TrackID";
	
}
