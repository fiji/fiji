package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureModel;

public class EdgeTimeLocationAnalyzer<T extends RealType<T> & NativeType<T>> implements EdgeFeatureAnalyzer {

	public static final String KEY = "TIME_LOCATION";
	/*
	 * FEATURE NAMES 
	 */
	private static final String TIME = "TIME";
	private static final String X_LOCATION = "X_LOCATION";
	private static final String Y_LOCATION = "Y_LOCATION";
	private static final String Z_LOCATION = "Z_LOCATION";

	private final TrackMateModel<T> model;
	private final FeatureModel<T> featureModel;

	public static final List<String> FEATURES = new ArrayList<String>(2);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(2);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(2);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(2);

	static {
		FEATURES.add(TIME);
		FEATURES.add(X_LOCATION);
		FEATURES.add(Y_LOCATION);
		FEATURES.add(Z_LOCATION);

		FEATURE_NAMES.put(TIME, "Time (mean)");
		FEATURE_NAMES.put(X_LOCATION, "X Location (mean)");
		FEATURE_NAMES.put(Y_LOCATION, "Y Location (mean)");
		FEATURE_NAMES.put(Z_LOCATION, "Z Location (mean)");

		FEATURE_SHORT_NAMES.put(TIME, "T");
		FEATURE_SHORT_NAMES.put(X_LOCATION, "X");
		FEATURE_SHORT_NAMES.put(Y_LOCATION, "Y");
		FEATURE_SHORT_NAMES.put(Z_LOCATION, "Z");

		FEATURE_DIMENSIONS.put(TIME, Dimension.TIME);
		FEATURE_DIMENSIONS.put(X_LOCATION, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(Y_LOCATION, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(Z_LOCATION, Dimension.POSITION);
	}

	/*
	 * CONSTRUCTOR
	 */

	public EdgeTimeLocationAnalyzer(final TrackMateModel<T> model) {
		this.model = model;
		this.featureModel = model.getFeatureModel();
	}



	@Override
	public void process(DefaultWeightedEdge edge) {
		Spot source = model.getEdgeSource(edge);
		Spot target = model.getEdgeTarget(edge);
		
		double x = 0.5 * ( source.getFeature(Spot.POSITION_X) + target.getFeature(Spot.POSITION_X) ); 
		double y = 0.5 * ( source.getFeature(Spot.POSITION_Y) + target.getFeature(Spot.POSITION_Y) ); 
		double z = 0.5 * ( source.getFeature(Spot.POSITION_Z) + target.getFeature(Spot.POSITION_Z) ); 
		double t = 0.5 * ( source.getFeature(Spot.POSITION_T) + target.getFeature(Spot.POSITION_T) ); 
		
		featureModel.putEdgeFeature(edge, TIME, t);
		featureModel.putEdgeFeature(edge, X_LOCATION, x);
		featureModel.putEdgeFeature(edge, Y_LOCATION, y);
		featureModel.putEdgeFeature(edge, Z_LOCATION, z);
	}
}
