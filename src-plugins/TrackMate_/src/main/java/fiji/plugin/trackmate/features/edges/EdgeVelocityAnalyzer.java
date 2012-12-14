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

public class EdgeVelocityAnalyzer<T extends RealType<T> & NativeType<T>> implements EdgeFeatureAnalyzer {

	public static final String KEY = "VELOCITY";
	/*
	 * FEATURE NAMES 
	 */
	private static final String VELOCITY = "VELOCITY";
	private static final String DISPLACEMENT = "DISPLACEMENT";

	private final TrackMateModel<T> model;
	private final FeatureModel<T> featureModel;

	public static final List<String> FEATURES = new ArrayList<String>(2);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(2);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(2);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(2);

	static {
		FEATURES.add(VELOCITY);
		FEATURES.add(DISPLACEMENT);

		FEATURE_NAMES.put(VELOCITY, "Velocity");
		FEATURE_NAMES.put(DISPLACEMENT, "Displacement");

		FEATURE_SHORT_NAMES.put(VELOCITY, "V");
		FEATURE_SHORT_NAMES.put(DISPLACEMENT, "D");

		FEATURE_DIMENSIONS.put(VELOCITY, Dimension.VELOCITY);
		FEATURE_DIMENSIONS.put(DISPLACEMENT, Dimension.LENGTH);
	}

	/*
	 * CONSTRUCTOR
	 */

	public EdgeVelocityAnalyzer(final TrackMateModel<T> model) {
		this.model = model;
		this.featureModel = model.getFeatureModel();
	}



	@Override
	public void process(DefaultWeightedEdge edge) {
		Spot source = model.getEdgeSource(edge);
		Spot target = model.getEdgeTarget(edge);
		
		double dx = target.diffTo(source, Spot.POSITION_X);
		double dy = target.diffTo(source, Spot.POSITION_Y);
		double dz = target.diffTo(source, Spot.POSITION_Z);
		double dt = target.diffTo(source, Spot.POSITION_T);
		double D = Math.sqrt(dx*dx + dy*dy + dz*dz);
		double V = D / Math.abs(dt);
		
		featureModel.putEdgeFeature(edge, VELOCITY, V);
		featureModel.putEdgeFeature(edge, DISPLACEMENT, D);
	}
}
