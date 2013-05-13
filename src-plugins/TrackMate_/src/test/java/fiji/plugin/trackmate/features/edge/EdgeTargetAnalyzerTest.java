package fiji.plugin.trackmate.features.edge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;

public class EdgeTargetAnalyzerTest {

	private static final int N_TRACKS = 10;
	private static final int DEPTH = 9; // must be at least 6 to avoid tracks too shorts - may make this test fail sometimes
	private TrackMateModel model;
	private HashMap<DefaultWeightedEdge, Spot> edgeTarget;
	private HashMap<DefaultWeightedEdge, Spot> edgeSource;
	private HashMap<DefaultWeightedEdge, Double> edgeCost;

	@Before
	public void setUp() {
		edgeSource = new HashMap<DefaultWeightedEdge, Spot>();
		edgeTarget = new HashMap<DefaultWeightedEdge, Spot>();
		edgeCost = new HashMap<DefaultWeightedEdge, Double>();

		model = new TrackMateModel();
		model.beginUpdate();
		try {

			for (int i = 0; i < N_TRACKS; i++) {

				Spot previous = null;

				for (int j = 0; j <= DEPTH; j++) {
					Spot spot = new Spot(new double[3]);
					model.addSpotTo(spot, j);
					if (null != previous) {
						DefaultWeightedEdge edge = model.addEdge(previous, spot, j);
						edgeSource.put(edge, previous);
						edgeTarget.put(edge, spot);
						edgeCost.put(edge, Double.valueOf(j));


					}
					previous = spot;
				}
			}

		} finally {
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess() {
		// Process model
		EdgeTargetAnalyzer analyzer = new EdgeTargetAnalyzer(model);
		analyzer.process(model.getTrackModel().edgeSet());

		// Collect features
		for (DefaultWeightedEdge edge :model.getTrackModel().edgeSet()) {
			assertEquals(edgeSource.get(edge).ID(), model.getFeatureModel().getEdgeFeature(edge, EdgeTargetAnalyzer.SPOT_SOURCE_ID).intValue());
			assertEquals(edgeTarget.get(edge).ID(), model.getFeatureModel().getEdgeFeature(edge, EdgeTargetAnalyzer.SPOT_TARGET_ID).intValue());
			assertEquals(edgeCost.get(edge).doubleValue(), model.getFeatureModel().getEdgeFeature(edge, EdgeTargetAnalyzer.EDGE_COST).doubleValue(), Double.MIN_VALUE);
		}
	}

	@Test
	public final void testModelChanged() {
		// Initial calculation
		final TestEdgeTargetAnalyzer analyzer = new TestEdgeTargetAnalyzer(model);
		analyzer.process(model.getTrackModel().edgeSet());

		// Prepare listener
		model.addTrackMateModelChangeListener(new ModelChangeListener() {
			@Override
			public void modelChanged(ModelChangeEvent event) {
				HashSet<DefaultWeightedEdge> edgesToUpdate = new HashSet<DefaultWeightedEdge>();
				for (DefaultWeightedEdge edge : event.getEdges()) {
					if (event.getEdgeFlag(edge) != ModelChangeEvent.FLAG_EDGE_REMOVED) {
						edgesToUpdate.add(edge);
					}
				}
				if (analyzer.isLocal()) {

					analyzer.process(edgesToUpdate );

				} else {

					// Get the all the edges of the track they belong to
					HashSet<DefaultWeightedEdge> globalEdgesToUpdate = new HashSet<DefaultWeightedEdge>();
					for (DefaultWeightedEdge edge : edgesToUpdate) {
						Integer motherTrackID = model.getTrackModel().getTrackIDOf(edge);
						globalEdgesToUpdate.addAll(model.getTrackModel().getTrackEdges(motherTrackID));
					}
					analyzer.process(globalEdgesToUpdate);
				}
			}
		});

		// Change the cost of one edge
		DefaultWeightedEdge edge = edgeSource.keySet().iterator().next();
		double val = 67.43;
		model.beginUpdate();
		try {
			model.setEdgeWeight(edge, val);
		} finally {
			model.endUpdate();
		}
		
		// We must have received only one edge to analyzer
		assertTrue(analyzer.hasBeenRun);
		assertEquals(1, analyzer.edges.size());
		DefaultWeightedEdge analyzedEdge = analyzer.edges.iterator().next();
		assertEquals(edge, analyzedEdge);
		assertEquals(val, model.getFeatureModel().getEdgeFeature(edge, EdgeTargetAnalyzer.EDGE_COST).doubleValue(), Double.MIN_VALUE);

	}

	private static class TestEdgeTargetAnalyzer extends EdgeTargetAnalyzer {

		private boolean hasBeenRun = false;
		private Collection<DefaultWeightedEdge> edges;

		public TestEdgeTargetAnalyzer(TrackMateModel model) {
			super(model);
		}

		@Override
		public void process(Collection<DefaultWeightedEdge> edges) {
			this.hasBeenRun = true;
			this.edges = edges;
			super.process(edges);
		}

	}	
}
