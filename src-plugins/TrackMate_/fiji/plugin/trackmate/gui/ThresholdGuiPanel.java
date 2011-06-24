package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.SpotFilter;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMate_;

/**
 *
 */
public class ThresholdGuiPanel extends ActionListenablePanel implements ChangeListener {

	private static final long serialVersionUID = 1307749013344373051L;
	private final ChangeEvent CHANGE_EVENT = new ChangeEvent(this);
	/** Will be set to the value of the {@link JPanelSpotColorGUI}. */
	public ActionEvent COLOR_FEATURE_CHANGED = null;
	
	private static final String ADD_ICON = "images/add.png";
	private static final String REMOVE_ICON = "images/delete.png";
	


	private JPanel jPanelBottom;
	private JPanelSpotColorGUI jPanelSpotColorGUI;

	private JScrollPane jScrollPaneThresholds;
	private JButton jButtonRemoveThreshold;
	private JPanel jPanelAllThresholds;
	private JButton jButtonAddThreshold;
	private JPanel jPanelButtons;

	private Stack<ThresholdPanel<SpotFeature>> thresholdPanels = new Stack<ThresholdPanel<SpotFeature>>();
	private Stack<Component> struts = new Stack<Component>();
	private EnumMap<SpotFeature, double[]> featureValues = new EnumMap<SpotFeature, double[]>(SpotFeature.class);
	private int newFeatureIndex;
	
	private List<SpotFilter> featureThresholds = new ArrayList<SpotFilter>();
	private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
	
	
	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * CONSTRUCTORS
	 */

	public ThresholdGuiPanel(EnumMap<SpotFeature, double[]> featureValues, List<SpotFilter> featureThresholds) {
		super();
		this.featureValues = featureValues;
		initGUI();
		if (null != featureValues) {
			
			if (null != featureThresholds) {
				
				for (SpotFilter ft : featureThresholds)
					addThresholdPanel(ft);
				if (featureThresholds.isEmpty())
					newFeatureIndex = 0;
				else
					newFeatureIndex = featureThresholds.get(featureThresholds.size()-1).feature.ordinal();

			}
		}
	}
	
	public ThresholdGuiPanel(EnumMap<SpotFeature, double[]> featureValues) {
		this(featureValues, null);
	}
	
	public ThresholdGuiPanel() {
		this(null);
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Called when one of the {@link ThresholdPanel} is changed by the user.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		featureThresholds = new ArrayList<SpotFilter>(thresholdPanels.size());
		for (ThresholdPanel<SpotFeature> tp : thresholdPanels) {
			featureThresholds.add(new SpotFilter(tp.getKey(), new Float(tp.getThreshold()), tp.isAboveThreshold()));
		}
		fireThresholdChanged(e);
	}

	/**
	 * Return the thresholds currently set by this GUI.
	 */
	public List<SpotFilter> getFeatureThresholds() {
		return featureThresholds;
	}
	
	
	/**
	 * Return the feature selected in the "Set color by feature" comb-box. 
	 * Return <code>null</code> if the item "Default" is selected.
	 */
	public SpotFeature getColorByFeature() {
		return jPanelSpotColorGUI.setColorByFeature;
	}
	
	/**
	 * Add an {@link ChangeListener} to this panel. The {@link ChangeListener} will
	 * be notified when a change happens to the thresholds displayed by this panel, whether
	 * due to the slider being move, the auto-threshold button being pressed, or
	 * the combo-box selection being changed.
	 */
	public void addChangeListener(ChangeListener listener) {
		changeListeners.add(listener);
	}
	
	/**
	 * Remove a ChangeListener from this panel. 
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeChangeListener(ChangeListener listener) {
		return changeListeners.remove(listener);
	}
	
	public Collection<ChangeListener> getChangeListeners() {
		return changeListeners;
	}
	
	/*
	 * PRIVATE METHODS
	 */
		
	private void fireThresholdChanged(ChangeEvent e) {
		for (ChangeListener cl : changeListeners) 
			cl.stateChanged(e);
	}
	
	public void addThresholdPanel() {
		addThresholdPanel(SpotFeature.values()[newFeatureIndex]);		
	}
	
	public void addThresholdPanel(SpotFilter threshold) {
		if (null == threshold)
			return;
		ThresholdPanel<SpotFeature> tp = new ThresholdPanel<SpotFeature>(featureValues, threshold.feature);
		tp.setThreshold(threshold.value);
		tp.setAboveThreshold(threshold.isAbove);		
		tp.addChangeListener(this);
		newFeatureIndex++;
		if (newFeatureIndex >= SpotFeature.values().length) 
			newFeatureIndex = 0;
		Component strut = Box.createVerticalStrut(5);
		struts.push(strut);
		thresholdPanels.push(tp);
		jPanelAllThresholds.add(tp);
		jPanelAllThresholds.add(strut);
		jPanelAllThresholds.revalidate();
		stateChanged(CHANGE_EVENT);
	}
		
	
	public void addThresholdPanel(SpotFeature feature) {
		if (null == featureValues)
			return;
		ThresholdPanel<SpotFeature> tp = new ThresholdPanel<SpotFeature>(featureValues, feature);
		tp.addChangeListener(this);
		newFeatureIndex++;
		if (newFeatureIndex >= SpotFeature.values().length) 
			newFeatureIndex = 0;
		Component strut = Box.createVerticalStrut(5);
		struts.push(strut);
		thresholdPanels.push(tp);
		jPanelAllThresholds.add(tp);
		jPanelAllThresholds.add(strut);
		jPanelAllThresholds.revalidate();
		stateChanged(CHANGE_EVENT);
	}
	
	private void removeThresholdPanel() {
		try {
			ThresholdPanel<SpotFeature> tp = thresholdPanels.pop();
			tp.removeChangeListener(this);
			Component strut = struts.pop();
			jPanelAllThresholds.remove(strut);
			jPanelAllThresholds.remove(tp);
			jPanelAllThresholds.repaint();
			stateChanged(CHANGE_EVENT);
		} catch (EmptyStackException ese) {	}
	}
	
	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			this.setLayout(thisLayout);
			setPreferredSize(new Dimension(270, 500));
			{
				jScrollPaneThresholds = new JScrollPane();
				this.add(jScrollPaneThresholds, BorderLayout.CENTER);
				jScrollPaneThresholds.setPreferredSize(new java.awt.Dimension(250, 389));
				jScrollPaneThresholds.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneThresholds.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				{
					jPanelAllThresholds = new JPanel();
					BoxLayout jPanelAllThresholdsLayout = new BoxLayout(jPanelAllThresholds, BoxLayout.Y_AXIS);
					jPanelAllThresholds.setLayout(jPanelAllThresholdsLayout);
					jScrollPaneThresholds.setViewportView(jPanelAllThresholds);
				}
			}
			{
				jPanelBottom = new JPanel();
				BorderLayout jPanelBottomLayout = new BorderLayout();
				jPanelBottom.setLayout(jPanelBottomLayout);
				this.add(jPanelBottom, BorderLayout.SOUTH);
				jPanelBottom.setPreferredSize(new java.awt.Dimension(270, 71));
				{
					jPanelButtons = new JPanel();
					jPanelBottom.add(jPanelButtons, BorderLayout.NORTH);
					BoxLayout jPanelButtonsLayout = new BoxLayout(jPanelButtons, javax.swing.BoxLayout.X_AXIS);
					jPanelButtons.setLayout(jPanelButtonsLayout);
					jPanelButtons.setPreferredSize(new java.awt.Dimension(270, 22));
					jPanelButtons.setSize(270, 25);
					jPanelButtons.setMaximumSize(new java.awt.Dimension(32767, 25));
					{
						jPanelButtons.add(Box.createHorizontalStrut(5));
						jButtonAddThreshold = new JButton();
						jPanelButtons.add(jButtonAddThreshold);
						jButtonAddThreshold.setIcon(new ImageIcon(getClass().getResource(ADD_ICON)));
						jButtonAddThreshold.setFont(SMALL_FONT);
						jButtonAddThreshold.setPreferredSize(new java.awt.Dimension(24, 24));
						jButtonAddThreshold.setSize(24, 24);
						jButtonAddThreshold.setMinimumSize(new java.awt.Dimension(24, 24));
						jButtonAddThreshold.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								addThresholdPanel();
							}
						});
					}
					{
						jPanelButtons.add(Box.createHorizontalStrut(5));
						jButtonRemoveThreshold = new JButton();
						jPanelButtons.add(jButtonRemoveThreshold);
						jButtonRemoveThreshold.setIcon(new ImageIcon(getClass().getResource(REMOVE_ICON)));
						jButtonRemoveThreshold.setFont(SMALL_FONT);
						jButtonRemoveThreshold.setPreferredSize(new java.awt.Dimension(24, 24));
						jButtonRemoveThreshold.setSize(24, 24);
						jButtonRemoveThreshold.setMinimumSize(new java.awt.Dimension(24, 24));
						jButtonRemoveThreshold.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								removeThresholdPanel();
							}
						});
						jPanelButtons.add(Box.createHorizontalGlue());
					}
				}
				{
					jPanelSpotColorGUI = new JPanelSpotColorGUI(this);
					COLOR_FEATURE_CHANGED = jPanelSpotColorGUI.COLOR_FEATURE_CHANGED;
					jPanelSpotColorGUI.featureValues = featureValues;
					jPanelBottom.add(jPanelSpotColorGUI, BorderLayout.CENTER);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * MAIN METHOD
	 */
	
	/**
	* Auto-generated main method to display this 
	* JPanel inside a new JFrame.
	 * @throws IOException 
	*/
	public static void main(String[] args) throws IOException {
		// Generate fake Spot data
		final int NSPOT = 100;
		final Random rn = new Random();
		ArrayList<Spot> spots = new ArrayList<Spot>(NSPOT);
		Spot spot;
		for (int i = 0; i < NSPOT; i++) {
			spot = new SpotImp(new float[] {0, 0, 0});
			for (SpotFeature feature : SpotFeature.values())
				spot.putFeature(feature, (float) (rn.nextGaussian()+5));
			spots.add(spot);
		}
		
		// Generate GUI
		TrackMate_ trackmate = new TrackMate_();
		System.out.println("Type <Enter> to ad spots to this");
		System.in.read();
		SpotCollection allSpots = new SpotCollection();
		allSpots.put(0, spots);
		trackmate.setSpots(allSpots, false);
		
		ThresholdGuiPanel gui = new ThresholdGuiPanel(trackmate.getFeatureValues());
		JFrame frame = new JFrame();
		frame.getContentPane().add(gui);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
		
	}
}
