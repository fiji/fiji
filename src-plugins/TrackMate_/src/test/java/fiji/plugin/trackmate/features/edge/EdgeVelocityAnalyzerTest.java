package fiji.plugin.trackmate.features.edge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;

public class EdgeVelocityAnalyzerTest {

	private static final int N_TRACKS = 10;
	private static final int DEPTH = 9; // must be at least 6 to avoid tracks too shorts - may make this test fail sometimes
	private TrackMateModel model;
	private HashMap<DefaultWeightedEdge, Double> edgeD;
	private HashMap<DefaultWeightedEdge, Double> edgeV;
	private Spot aspot;

	@Before
	public void setUp() {
		edgeV = new HashMap<DefaultWeightedEdge, Double>();
		edgeD = new HashMap<DefaultWeightedEdge, Double>();

		model = new TrackMateModel();
		model.beginUpdate();
		
		final String[] posFeats = Spot.POSITION_FEATURES;
		
		try {

			for (int i = 0; i < N_TRACKS; i++) {

				Spot previous = null;

				for (int j = 0; j <= DEPTH; j++) {
					Spot spot = new Spot(new double[3]); 
					spot.putFeature(posFeats[i%3], i+j); // rotate displacement dimension
					spot.putFeature(Spot.POSITION_T, 2*j);
					model.addSpotTo(spot, j);
					if (null != previous) {
						DefaultWeightedEdge edge = model.addEdge(previous, spot, j);
						double d = spot.getFeature(posFeats[i%3]).doubleValue() - previous.getFeature(posFeats[i%3]).doubleValue();
						edgeD.put(edge, d);
						edgeV.put(edge, d/2);

					}
					previous = spot;
					
					// save one middle spot
					if (i == 0 && j == DEPTH/2) {
						aspot = spot;
					}
				}
			}

		} finally {
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess() {
		// Process model
		EdgeVelocityAnalyzer analyzer = new EdgeVelocityAnalyzer(model);
		analyzer.process(model.getTrackModel().edgeSet());

		// Collect features
		for (DefaultWeightedEdge edge :model.getTrackModel().edgeSet()) {
			assertEquals(edgeV.get(edge).doubleValue(), model.getFeatureModel().getEdgeFeature(edge, EdgeVelocityAnalyzer.VELOCITY).doubleValue(), Double.MIN_VALUE);
			assertEquals(edgeD.get(edge).doubleValue(), model.getFeatureModel().getEdgeFeature(edge, EdgeVelocityAnalyzer.DISPLACEMENT).doubleValue(), Double.MIN_VALUE);
		}
	}

	@Test
	public final void testModelChanged() {
		// Initial calculation
		final TestEdgeVelocityAnalyzer analyzer = new TestEdgeVelocityAnalyzer(model);
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

		// Move one spot
		model.beginUpdate();
		try {
			aspot.putFeature(Spot.POSITION_X, -1000d);
			model.updateFeatures(aspot);
		} finally {
			model.endUpdate();
		}
		
		// We must have received 2 edges to analyzer
		assertTrue(analyzer.hasBeenRun);
		assertEquals(2, analyzer.edges.size());
	}

	private static class TestEdgeVelocityAnalyzer extends EdgeVelocityAnalyzer {

		private boolean hasBeenRun = false;
		private Collection<DefaultWeightedEdge> edges;

		public TestEdgeVelocityAnalyzer(TrackMateModel model) {
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
