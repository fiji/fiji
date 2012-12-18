package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureModel;

public class TrackLocationAnalyzer<T extends RealType<T> & NativeType<T>> implements TrackFeatureAnalyzer<T> {

	/*
	 * FEATURE NAMES 
	 */
	public static final String KEY = "LOCATION";
	public static final String TRACK_ID = "TRACK_ID";
	public static final String X_LOCATION = "X_LOCATION";
	public static final String Y_LOCATION = "Y_LOCATION";
	public static final String Z_LOCATION = "Z_LOCATION";

	public static final List<String> FEATURES = new ArrayList<String>(2);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(2);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(2);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(2);

	static {
		FEATURES.add(TRACK_ID);
		FEATURES.add(X_LOCATION);
		FEATURES.add(Y_LOCATION);
		FEATURES.add(Z_LOCATION);

		FEATURE_NAMES.put(TRACK_ID, "Track ID");
		FEATURE_NAMES.put(X_LOCATION, "X Location (mean)");
		FEATURE_NAMES.put(Y_LOCATION, "Y Location (mean)");
		FEATURE_NAMES.put(Z_LOCATION, "Z Location (mean)");

		FEATURE_SHORT_NAMES.put(TRACK_ID, "ID");
		FEATURE_SHORT_NAMES.put(X_LOCATION, "X");
		FEATURE_SHORT_NAMES.put(Y_LOCATION, "Y");
		FEATURE_SHORT_NAMES.put(Z_LOCATION, "Z");

		FEATURE_DIMENSIONS.put(TRACK_ID, Dimension.NONE);
		FEATURE_DIMENSIONS.put(X_LOCATION, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(Y_LOCATION, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(Z_LOCATION, Dimension.POSITION);
	}

	/*
	 * CONSTRUCTOR
	 */



	@Override
	public void process(final TrackMateModel<T> model) {
		final FeatureModel<T> fm = model.getFeatureModel();
		
		Map<Integer, Set<Spot>> trackSpots = model.getTrackSpots();
		
		for (Integer trackID : trackSpots.keySet()) {
			
			double x = 0;
			double y = 0;
			double z = 0;
			
			for(Spot spot : trackSpots.get(trackID)) {
				x += spot.getFeature(Spot.POSITION_X);
				y += spot.getFeature(Spot.POSITION_Y);
				z += spot.getFeature(Spot.POSITION_Z);
			}
			int nspots = trackSpots.get(trackID).size();
			x /= nspots;
			y /= nspots;
			z /= nspots;

			fm.putTrackFeature(trackID, TRACK_ID, Double.valueOf(trackID));
			fm.putTrackFeature(trackID, X_LOCATION, x);
			fm.putTrackFeature(trackID, Y_LOCATION, y);
			fm.putTrackFeature(trackID, Z_LOCATION, z);
		}
	}

}
