package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.EdgeFeatureGrapher;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.features.TrackFeatureGrapher;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

public class GrapherPanel extends ActionListenablePanel implements WizardPanelDescriptor {

	private static final ImageIcon SPOT_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/SpotIcon_small.png"));
	private static final ImageIcon EDGE_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/EdgeIcon_small.png"));
	private static final ImageIcon TRACK_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/TrackIcon_small.png"));
	
	private static final long serialVersionUID = 1L;
	public static final String DESCRIPTOR = "GrapherPanel";

	private TrackMateModel model;
	private JPanel panelSpot;
	private JPanel panelEdges;
	private JPanel panelTracks;
	private FeaturePlotSelectionPanel spotFeatureSelectionPanel;
	private FeaturePlotSelectionPanel edgeFeatureSelectionPanel;
	private FeaturePlotSelectionPanel trackFeatureSelectionPanel;
	private TrackMateWizard wizard;

	/*
	 * CONSTRUCTOR
	 */

	public GrapherPanel() {
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
	}

	private void refresh() {
		
		// regen spot features
		panelSpot.removeAll();
		List<String> spotFeatures = model.getFeatureModel().getSpotFeatures();
		Map<String, String> spotFeatureNames = model.getFeatureModel().getSpotFeatureNames();
		spotFeatureSelectionPanel = new FeaturePlotSelectionPanel(Spot.POSITION_T, spotFeatures, spotFeatureNames);
		panelSpot.add(spotFeatureSelectionPanel);
		spotFeatureSelectionPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				plotSpotFeatures();
			}
		});
		
		// regen edge features
		panelEdges.removeAll();
		List<String> edgeFeatures = model.getFeatureModel().getEdgeFeatures();
		Map<String, String> edgeFeatureNames = model.getFeatureModel().getEdgeFeatureNames();
		edgeFeatureSelectionPanel = new FeaturePlotSelectionPanel(EdgeTimeLocationAnalyzer.TIME, edgeFeatures, edgeFeatureNames);
		panelEdges.add(edgeFeatureSelectionPanel);
		edgeFeatureSelectionPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				plotEdgeFeatures();
			}
		});
		
		// regen trak features
		panelTracks.removeAll();
		List<String> trackFeatures = model.getFeatureModel().getTrackFeatures();
		Map<String, String> trackFeatureNames = model.getFeatureModel().getTrackFeatureNames();
		trackFeatureSelectionPanel = new FeaturePlotSelectionPanel(TrackIndexAnalyzer.TRACK_INDEX, trackFeatures, trackFeatureNames);
		panelTracks.add(trackFeatureSelectionPanel);
		trackFeatureSelectionPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				plotTrackFeatures();
			}
		});
	}
	
	private void plotSpotFeatures() {
		String xFeature = spotFeatureSelectionPanel.getXKey();
		Set<String> yFeatures = spotFeatureSelectionPanel.getYKeys();
		
		// Collect only the spots that are in tracks
		List<Spot> spots = new ArrayList<Spot>();
		for(Integer trackID : model.getTrackModel().getTrackIDs()) {
			spots.addAll(model.getTrackModel().getTrackSpots(trackID));
		}
		
		SpotFeatureGrapher grapher = new SpotFeatureGrapher(xFeature, yFeatures, spots, model);
		grapher.render();
	}
	
	private void plotEdgeFeatures() {
		// Collect edges in filtered tracks
		List<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>();
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
			edges.addAll(model.getTrackModel().getTrackEdges(trackID));
		}
		// Recompute edge features
		model.getFeatureModel().computeEdgeFeatures(edges, true);
		// Prepare grapher
		String xFeature = edgeFeatureSelectionPanel.getXKey();
		Set<String> yFeatures = edgeFeatureSelectionPanel.getYKeys();
		EdgeFeatureGrapher grapher = new EdgeFeatureGrapher(xFeature, yFeatures, edges , model);
		grapher.render();
	}
	
	private void plotTrackFeatures() {
		// Recompute track features
		model.getFeatureModel().computeTrackFeatures(model.getTrackModel().getFilteredTrackIDs(), true);
		// Prepare grapher
		String xFeature = trackFeatureSelectionPanel.getXKey();
		Set<String> yFeatures = trackFeatureSelectionPanel.getYKeys();
		TrackFeatureGrapher grapher = new TrackFeatureGrapher(xFeature, yFeatures, model);
		grapher.render();
	}

	/*
	 * WIZARDPANELDESCRIPTOR METHODS	
	 */

	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.model = plugin.getModel(); 
		refresh();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return ActionChooserPanel.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return DisplayerPanel.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() { }

	@Override
	public void displayingPanel() {
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() { }

}
