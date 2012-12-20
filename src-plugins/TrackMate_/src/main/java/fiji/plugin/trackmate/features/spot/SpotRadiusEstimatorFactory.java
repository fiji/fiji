package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;

public class SpotRadiusEstimatorFactory<T extends RealType<T> & NativeType<T>>  implements SpotFeatureAnalyzerFactory<T> {
	
	/*
	 * CONSTANT
	 */
	
	/** The single feature key name that this analyzer computes. */
	public static final String						ESTIMATED_DIAMETER = "ESTIMATED_DIAMETER";
	public static final ArrayList<String> 			FEATURES = new ArrayList<String>(1);
	public static final HashMap<String, String> 	FEATURE_NAMES = new HashMap<String, String>(1);
	public static final HashMap<String, String> 	FEATURE_SHORT_NAMES = new HashMap<String, String>(1);
	public static final HashMap<String, Dimension> 	FEATURE_DIMENSIONS = new HashMap<String, Dimension>(1);
	static {
		FEATURES.add(ESTIMATED_DIAMETER);
		FEATURE_NAMES.put(ESTIMATED_DIAMETER, "Estimated diameter");
		FEATURE_SHORT_NAMES.put(ESTIMATED_DIAMETER, "Diam.");
		FEATURE_DIMENSIONS.put(ESTIMATED_DIAMETER, Dimension.LENGTH);
	}
	public static final String KEY = "Spot radius estimator";
	private final TrackMateModel model;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public SpotRadiusEstimatorFactory(final TrackMateModel model) {
		this.model = model;
	}
	
	/*
	 * METHODS
	 */
	@Override
	public SpotRadiusEstimator<T> getAnalyzer(int frame, int channel) {
		final ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(model.getSettings().imp);
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img, channel);
		final ImgPlus<T> imgCT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);
		final List<Spot> spots = model.getSpots().get(frame);
		return new SpotRadiusEstimator<T>(imgCT, spots);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public List<String> getFeatures() {
		return FEATURES;
	}

	@Override
	public Map<String, String> getFeatureShortNames() {
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map<String, String> getFeatureNames() {
		return FEATURE_NAMES;
	}

	@Override
	public Map<String, Dimension> getFeatureDimensions() {
		return FEATURE_DIMENSIONS;
	}
	
}
