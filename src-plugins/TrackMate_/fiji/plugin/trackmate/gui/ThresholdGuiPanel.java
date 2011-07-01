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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMate_;

/**
 *
 */
public class ThresholdGuiPanel<K extends Enum<K>> extends ActionListenablePanel implements ChangeListener {

	private static final long serialVersionUID = 1307749013344373051L;
	private final ChangeEvent CHANGE_EVENT = new ChangeEvent(this);
	/** Will be set to the value of the {@link JPanelSpotColorGUI}. */
	public ActionEvent COLOR_FEATURE_CHANGED = null;

	private static final String ADD_ICON = "images/add.png";
	private static final String REMOVE_ICON = "images/delete.png";

	private JPanel jPanelBottom;
	private JPanelSpotColorGUI<K> jPanelSpotColorGUI;
	private JScrollPane jScrollPaneThresholds;
	private JPanel jPanelAllThresholds;
	private JPanel jPanelButtons;
	private JButton jButtonRemoveThreshold;
	private JButton jButtonAddThreshold;
	private JLabel jLabelInfo;

	private Stack<ThresholdPanel<K>> thresholdPanels = new Stack<ThresholdPanel<K>>();
	private Stack<Component> struts = new Stack<Component>();
	private EnumMap<K, double[]> featureValues;
	private int newFeatureIndex;

	private List<FeatureFilter<K>> featureThresholds = new ArrayList<FeatureFilter<K>>();
	private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
	private K[] values;
	private K featureType;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * @param featureType  used to instantiate the class with the correct parameter.
	 */
	public ThresholdGuiPanel(final K featureType, EnumMap<K, double[]> featureValues, List<FeatureFilter<K>> featureThresholds) {
		super();
		this.featureValues = featureValues;
		this.featureType = featureType;
		initGUI();
		values = featureType.getDeclaringClass().getEnumConstants();
		if (null != featureValues) {

			if (null != featureThresholds) {

				for (FeatureFilter<K> ft : featureThresholds)
					addThresholdPanel(ft);
				if (featureThresholds.isEmpty())
					newFeatureIndex = 0;
				else
					newFeatureIndex = featureThresholds.get(featureThresholds.size()-1).feature.ordinal();

			}
		}
		updateInfoText();
	}

	public ThresholdGuiPanel(final K featureType, EnumMap<K, double[]> featureValues) {
		this(featureType, featureValues, null);
	}

	public ThresholdGuiPanel(final K featureType) {
		this(featureType, null);
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Called when one of the {@link ThresholdPanel} is changed by the user.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		featureThresholds = new ArrayList<FeatureFilter<K>>(thresholdPanels.size());
		for (ThresholdPanel<K> tp : thresholdPanels) {
			featureThresholds.add(new FeatureFilter<K>(tp.getKey(), new Float(tp.getThreshold()), tp.isAboveThreshold()));
		}
		fireThresholdChanged(e);
		updateInfoText();
	}

	/**
	 * Return the thresholds currently set by this GUI.
	 */
	public List<FeatureFilter<K>> getFeatureThresholds() {
		return featureThresholds;
	}


	/**
	 * Return the feature selected in the "Set color by feature" comb-box. 
	 * Return <code>null</code> if the item "Default" is selected.
	 */
	public K getColorByFeature() {
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
		addThresholdPanel(values[newFeatureIndex]);		
	}

	public void addThresholdPanel(FeatureFilter<K> threshold) {
		if (null == threshold)
			return;
		ThresholdPanel<K> tp = new ThresholdPanel<K>(featureValues, threshold.feature);
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


	public void addThresholdPanel(K feature) {
		if (null == featureValues)
			return;
		ThresholdPanel<K> tp = new ThresholdPanel<K>(featureValues, feature);
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
			ThresholdPanel<K> tp = thresholdPanels.pop();
			tp.removeChangeListener(this);
			Component strut = struts.pop();
			jPanelAllThresholds.remove(strut);
			jPanelAllThresholds.remove(tp);
			jPanelAllThresholds.repaint();
			stateChanged(CHANGE_EVENT);
		} catch (EmptyStackException ese) {	}
	}


	private void updateInfoText() {
		String info = "";
		int nobjects = featureValues.values().iterator().next().length;
		if (featureThresholds == null || featureThresholds.isEmpty()) {
			info = "Keep all "+nobjects+" objects.";
		} else {
			int nselected = 0;
			double val;
			for (int i = 0; i < nobjects; i++) {
				boolean ok = true;
				for (FeatureFilter<K> filter : featureThresholds) {
					val = featureValues.get(filter.feature)[i];
					if (filter.isAbove) {
						if (val < filter.value) {
							ok = false;
							break;
						}
					} else {
						if (val > filter.value) {
							ok = false;
							break;
						}
					}
				}
				if (ok)
					nselected++;
			}
			info = "Keep "+nselected+" objects out of  "+nobjects+".";
		}
		jLabelInfo.setText(info);

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
					{
						jPanelButtons.add(Box.createHorizontalStrut(5));
						jLabelInfo = new JLabel();
						jLabelInfo.setFont(SMALL_FONT);
						jPanelButtons.add(jLabelInfo);
					}
				}
				{
					jPanelSpotColorGUI = new JPanelSpotColorGUI<K>(featureType, this);
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

		ThresholdGuiPanel<SpotFeature> gui = new ThresholdGuiPanel<SpotFeature>(SpotFeature.QUALITY, trackmate.getFeatureValues());
		JFrame frame = new JFrame();
		frame.getContentPane().add(gui);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);


	}
}
