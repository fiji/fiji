package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.EdgeFeatureGrapher;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.features.TrackFeatureGrapher;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;
import fiji.plugin.trackmate.gui.panels.components.FeaturePlotSelectionPanel;

public class GrapherPanel extends ActionListenablePanel {

	private static final ImageIcon SPOT_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/SpotIcon_small.png"));
	private static final ImageIcon EDGE_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/EdgeIcon_small.png"));
	private static final ImageIcon TRACK_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/TrackIcon_small.png"));
	
	private static final long serialVersionUID = 1L;

	private final TrackMate trackmate;
	private JPanel panelSpot;
	private JPanel panelEdges;
	private JPanel panelTracks;
	private FeaturePlotSelectionPanel spotFeatureSelectionPanel;
	private FeaturePlotSelectionPanel edgeFeatureSelectionPanel;
	private FeaturePlotSelectionPanel trackFeatureSelectionPanel;

	/*
	 * CONSTRUCTOR
	 */

	public GrapherPanel(TrackMate trackmate) {
		this.trackmate = trackmate;

		setLayout(new BorderLayout(0, 0));

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		add(tabbedPane, BorderLayout.CENTER);

		panelSpot = new JPanel();
		tabbedPane.addTab("Spots", SPOT_ICON, panelSpot, null);
		panelSpot.setLayout(new BorderLayout(0, 0));

		panelEdges = new JPanel();
		tabbedPane.addTab("Links", EDGE_ICON, panelEdges, null);
		panelEdges.setLayout(new BorderLayout(0, 0));

		panelTracks = new JPanel();
		tabbedPane.addTab("Tracks", TRACK_ICON, panelTracks, null);
		panelTracks.setLayout(new BorderLayout(0, 0));
		
		refresh();
	}

	private void refresh() {
		
		// regen spot features
		panelSpot.removeAll();
		Collection<String> spotFeatures = trackmate.getModel().getFeatureModel().getSpotFeatures();
		Map<String, String> spotFeatureNames = trackmate.getModel().getFeatureModel().getSpotFeatureNames();
		spotFeatureSelectionPanel = new FeaturePlotSelectionPanel(Spot.POSITION_T, spotFeatures, spotFeatureNames);
		panelSpot.add(spotFeatureSelectionPanel);
		spotFeatureSelectionPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				spotFeatureSelectionPanel.setEnabled(false);
				new Thread("TrackMate plot spot features thread") {
					@Override
					public void run() {
						plotSpotFeatures();
						spotFeatureSelectionPanel.setEnabled(true);
					}
				}.start();
			}
		});
		
		// regen edge features
		panelEdges.removeAll();
		Collection<String> edgeFeatures = trackmate.getModel().getFeatureModel().getEdgeFeatures();
		Map<String, String> edgeFeatureNames = trackmate.getModel().getFeatureModel().getEdgeFeatureNames();
		edgeFeatureSelectionPanel = new FeaturePlotSelectionPanel(EdgeTimeLocationAnalyzer.TIME, edgeFeatures, edgeFeatureNames);
		panelEdges.add(edgeFeatureSelectionPanel);
		edgeFeatureSelectionPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				edgeFeatureSelectionPanel.setEnabled(false);
				new Thread("TrackMate plot edge features thread") {
					@Override
					public void run() {
						plotEdgeFeatures();
						edgeFeatureSelectionPanel.setEnabled(true);
					}
				}.start();
			}
		});
		
		// regen trak features
		panelTracks.removeAll();
		Collection<String> trackFeatures = trackmate.getModel().getFeatureModel().getTrackFeatures();
		Map<String, String> trackFeatureNames = trackmate.getModel().getFeatureModel().getTrackFeatureNames();
		trackFeatureSelectionPanel = new FeaturePlotSelectionPanel(TrackIndexAnalyzer.TRACK_INDEX, trackFeatures, trackFeatureNames);
		panelTracks.add(trackFeatureSelectionPanel);
		trackFeatureSelectionPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				trackFeatureSelectionPanel.setEnabled(false);
				new Thread("TrackMate plot track features thread") {
					@Override
					public void run() {
						plotTrackFeatures();
						trackFeatureSelectionPanel.setEnabled(true);
					}
				}.start();
			}
		});
	}
	
	private void plotSpotFeatures() {
		String xFeature = spotFeatureSelectionPanel.getXKey();
		Set<String> yFeatures = spotFeatureSelectionPanel.getYKeys();
		// Collect only the spots that are in tracks
		List<Spot> spots = new ArrayList<Spot>(trackmate.getModel().getSpots().getNSpots(true));
		for(Integer trackID : trackmate.getModel().getTrackModel().trackIDs(false)) {
			spots.addAll(trackmate.getModel().getTrackModel().trackSpots(trackID));
		}
		SpotFeatureGrapher grapher = new SpotFeatureGrapher(xFeature, yFeatures, spots, trackmate.getModel());
		grapher.render();
	}
	
	private void plotEdgeFeatures() {
		// Collect edges in filtered tracks
		List<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>();
		for (Integer trackID : trackmate.getModel().getTrackModel().trackIDs(true)) {
			edges.addAll(trackmate.getModel().getTrackModel().trackEdges(trackID));
		}
		// Prepare grapher
		String xFeature = edgeFeatureSelectionPanel.getXKey();
		Set<String> yFeatures = edgeFeatureSelectionPanel.getYKeys();
		EdgeFeatureGrapher grapher = new EdgeFeatureGrapher(xFeature, yFeatures, edges , trackmate.getModel());
		grapher.render();
	}
	
	private void plotTrackFeatures() {
		// Prepare grapher
		String xFeature = trackFeatureSelectionPanel.getXKey();
		Set<String> yFeatures = trackFeatureSelectionPanel.getYKeys();
		TrackFeatureGrapher grapher = new TrackFeatureGrapher(xFeature, yFeatures, trackmate.getModel());
		grapher.render();
	}

}
