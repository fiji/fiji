package fiji.plugin.trackmate.features.track;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackCollection;
import fiji.plugin.trackmate.TrackFeature;
import fiji.plugin.trackmate.util.TrackSplitter;

public class TrackBranchingAnalyzer implements TrackFeatureAnalyzer {

	/** Frame interval in physical units. */
	private float dt;


	public TrackBranchingAnalyzer(final float dt) {
		this.dt = dt;
	}
	
	
	@Override
	public void process(final TrackCollection tracks) {
		final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = tracks.getGraph();
		for (int i = 0; i < tracks.size(); i++) {
			final Set<Spot> track = tracks.getTrackSpot(i);
			int ngaps = 0;
			int nmerges = 0;
			int nsplits = 0;
			int ncomplex = 0;
			for (Spot spot : track) {
				int type = TrackSplitter.getVertexType(graph, spot);
				switch(type) {
				case TrackSplitter.MERGING_POINT:
				case TrackSplitter.MERGING_END:
					nmerges++;
					break;
				case TrackSplitter.SPLITTING_START:
				case TrackSplitter.SPLITTING_POINT:
					nsplits++;
					break;
				case TrackSplitter.COMPLEX_POINT:
					ncomplex++;
					break;
				case TrackSplitter.BRIDGE:
					// Identify gaps in the model.
					// Complicated: we must seek the other spot and measure how far it is.
					Set<DefaultWeightedEdge> edges = graph.edgesOf(spot);
					Iterator<DefaultWeightedEdge> it = edges.iterator();
					DefaultWeightedEdge edge = it.next();
					Spot other = graph.getEdgeSource(edge);
					if (other == spot)
						other = graph.getEdgeTarget(edge);
					
					float t0 = spot.getFeature(SpotFeature.POSITION_T);
					float t1 = other.getFeature(SpotFeature.POSITION_T);
					if (Math.abs(t1-t0) > dt) 
						ngaps++;
				}
			}
			// We have been counting gaps twice:
			ngaps /= 2;
			// Put feature data
			tracks.putFeature(i, TrackFeature.NUMBER_SPLITS, (float) nsplits);
			tracks.putFeature(i, TrackFeature.NUMBER_MERGES, (float) nmerges);
			tracks.putFeature(i, TrackFeature.NUMBER_GAPS, (float) ngaps);
			tracks.putFeature(i, TrackFeature.NUMBER_SPOTS, (float) track.size());
		}
	
	}


	@Override
	public Set<TrackFeature> getFeatures() {
		Set<TrackFeature> featureList = new HashSet<TrackFeature>(3);
		featureList.add(TrackFeature.NUMBER_SPLITS);
		featureList.add(TrackFeature.NUMBER_MERGES);
		featureList.add(TrackFeature.NUMBER_GAPS);
		featureList.add(TrackFeature.NUMBER_SPOTS);
		return featureList ;
	}

}
