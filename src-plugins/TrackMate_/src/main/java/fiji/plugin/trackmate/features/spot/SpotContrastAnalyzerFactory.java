package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class SpotContrastAnalyzerFactory<T extends RealType<T> & NativeType<T>> implements SpotFeatureAnalyzerFactory<T> {

	/*
	 * FIELDS
	 */
	
	/** The single feature key name that this analyzer computes. */
	public static final String						KEY = "CONTRAST";
	private static final ArrayList<String> 			FEATURES = new ArrayList<String>(1);
	private static final HashMap<String, String> 	FEATURE_NAMES = new HashMap<String, String>(1);
	private static final HashMap<String, String> 	FEATURE_SHORT_NAMES = new HashMap<String, String>(1);
	private static final HashMap<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(1);
	static {
		FEATURES.add(KEY);
		FEATURE_NAMES.put(KEY, "Contrast");
		FEATURE_SHORT_NAMES.put(KEY, "Contrast");
		FEATURE_DIMENSIONS.put(KEY, Dimension.NONE);
	}
	
	private final TrackMateModel model;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public SpotContrastAnalyzerFactory(final TrackMateModel model) {
		this.model = model;
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public final SpotContrastAnalyzer<T> getAnalyzer(final int frame, final int channel) {
		final ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(model.getSettings().imp);
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img, channel);
		final ImgPlus<T> imgCT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);
		final List<Spot> spots = model.getSpots().get(frame);
		return new SpotContrastAnalyzer<T>(imgCT, spots);
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
