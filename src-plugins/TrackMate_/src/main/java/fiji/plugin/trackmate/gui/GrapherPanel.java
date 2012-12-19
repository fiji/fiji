package fiji.plugin.trackmate.gui;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.EdgeFeatureGrapher;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.features.TrackFeatureGrapher;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

public class GrapherPanel<T extends RealType<T> & NativeType<T>> extends ActionListenablePanel implements WizardPanelDescriptor<T> {

	private static final ImageIcon SPOT_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/SpotIcon_small.png"));
	private static final ImageIcon EDGE_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/EdgeIcon_small.png"));
	private static final ImageIcon TRACK_ICON 		= new ImageIcon(GrapherPanel.class.getResource("images/TrackIcon_small.png"));
	
	private static final long serialVersionUID = 1L;
	public static final String DESCRIPTOR = "GrapherPanel";

	private TrackMateModel<T> model;
	private JPanel panelSpot;
	private JPanel panelEdges;
	private JPanel panelTracks;
	private FeaturePlotSelectionPanel spotFeatureSelectionPanel;
	private FeaturePlotSelectionPanel edgeFeatureSelectionPanel;
	private FeaturePlotSelectionPanel trackFeatureSelectionPanel;
	private TrackMateWizard<T> wizard;

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
		for(Integer trackID : model.getTrackIDs()) {
			spots.addAll(model.getTrackSpots(trackID));
		}
		
		SpotFeatureGrapher<T> grapher = new SpotFeatureGrapher<T>(xFeature, yFeatures, spots, model);
		grapher.render();
	}
	
	private void plotEdgeFeatures() {
		// Collect edges in filtered tracks
		List<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>();
		for (Integer trackID : model.getFilteredTrackIDs()) {
			edges.addAll(model.getTrackEdges(trackID));
		}
		// Recompute edge features
		model.getFeatureModel().computeEdgeFeatures(edges, true);
		// Prepare grapher
		String xFeature = edgeFeatureSelectionPanel.getXKey();
		Set<String> yFeatures = edgeFeatureSelectionPanel.getYKeys();
		EdgeFeatureGrapher<T> grapher = new EdgeFeatureGrapher<T>(xFeature, yFeatures, edges , model);
		grapher.render();
	}
	
	private void plotTrackFeatures() {
		// Recompute track features
		model.getFeatureModel().computeTrackFeatures(model.getFilteredTrackIDs(), true);
		// Prepare grapher
		String xFeature = trackFeatureSelectionPanel.getXKey();
		Set<String> yFeatures = trackFeatureSelectionPanel.getYKeys();
		TrackFeatureGrapher<T> grapher = new TrackFeatureGrapher<T>(xFeature, yFeatures, model);
		grapher.render();
	}

	/*
	 * WIZARDPANELDESCRIPTOR METHODS	
	 */

	@Override
	public void setWizard(TrackMateWizard<T> wizard) {
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
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



	/*
	 * STATIC METHODS
	 */

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {
		
		if (IJ.isMacOSX() || IJ.isWindows()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		}
		
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		GrapherPanel<T> panel = new GrapherPanel<T>();
		panel.setPlugin(plugin);
		frame.getContentPane().add(panel);
		frame.setSize(300, 500);
		frame.setVisible(true);

	}
}
