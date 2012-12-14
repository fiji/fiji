package fiji.plugin.trackmate.gui;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;

public class GrapherPanel<T extends RealType<T> & NativeType<T>> extends ActionListenablePanel implements WizardPanelDescriptor<T> {

	private static final long serialVersionUID = 1L;
	public static final String DESCRIPTOR = "GrapherPanel";

	private TrackMateModel<T> model;
	private JPanel panelSpot;
	private FeaturePlotSelectionPanel spotFeatureSelectionPanel;

	/*
	 * CONSTRUCTOR
	 */

	public GrapherPanel() {
		setLayout(new BorderLayout(0, 0));

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		add(tabbedPane, BorderLayout.CENTER);

		panelSpot = new JPanel();
		tabbedPane.addTab("Spots", null, panelSpot, null);
		panelSpot.setLayout(new BorderLayout(0, 0));

		JPanel panelEdges = new JPanel();
		tabbedPane.addTab("Links", null, panelEdges, null);

		JPanel panelTracks = new JPanel();
		tabbedPane.addTab("Tracks", null, panelTracks, null);
	}

	private void refresh() {
		panelSpot.removeAll();
		List<String> features = model.getFeatureModel().getSpotFeatures();
		Map<String, String> featureNames = model.getFeatureModel().getSpotFeatureShortNames();
		spotFeatureSelectionPanel = new FeaturePlotSelectionPanel(Spot.POSITION_T, features, featureNames);
		panelSpot.add(spotFeatureSelectionPanel);
	}

	/*
	 * WIZARDPANELDESCRIPTOR METHODS	
	 */

	@Override
	public void setWizard(TrackMateWizard<T> wizard) { }

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
		return null;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return null;
	}

	@Override
	public String getPreviousDescriptorID() {
		return ActionChooserPanel.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() { }

	@Override
	public void displayingPanel() { }

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
		panel.spotFeatureSelectionPanel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(e);
			}
		});
		frame.getContentPane().add(panel);
		frame.setSize(300, 500);
		frame.setVisible(true);

	}
}
