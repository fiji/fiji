package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureModel;

public class TrackIndexAnalyzer<T extends RealType<T> & NativeType<T>> implements TrackFeatureAnalyzer<T> {

	public static final String 		KEY = "Track index";
	public static final String TRACK_INDEX = "TRACK_INDEX";
	public static final List<String> FEATURES = new ArrayList<String>(1);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(1);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(1);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(1);
	
	static {
		FEATURES.add(TRACK_INDEX);
		FEATURE_NAMES.put(TRACK_INDEX, "Track index");
		FEATURE_SHORT_NAMES.put(TRACK_INDEX, "Index");
		FEATURE_DIMENSIONS.put(TRACK_INDEX, Dimension.NONE);
	}
	
	private final TrackMateModel<T> model;
	private long processingTime;


	
	public TrackIndexAnalyzer(final TrackMateModel<T> model) {
		this.model = model;
	}
	
	
	@Override
	public void process(Collection<Integer> trackIDs) {
		long start = System.currentTimeMillis();
		FeatureModel<T> fm = model.getFeatureModel();
		int index = 0;
		for (Integer trackID : trackIDs) {
			fm.putTrackFeature(trackID, TRACK_INDEX, Double.valueOf(index++));
		}
		long end = System.currentTimeMillis();
		processingTime = end - start;
	}


	@Override
	public long getProcessingTime() {
		return processingTime;
	}
	
	@Override
	public String toString() {
		return KEY;
	}
}
