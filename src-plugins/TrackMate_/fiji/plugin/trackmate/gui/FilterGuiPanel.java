package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.FeatureFilter;

public class FilterGuiPanel extends ActionListenablePanel implements ChangeListener {

	private static final long serialVersionUID = 1307749013344373051L;
	private final ChangeEvent CHANGE_EVENT = new ChangeEvent(this);
	/** Will be set to the value of the {@link JPanelColorByFeatureGUI}. */
	public ActionEvent COLOR_FEATURE_CHANGED = null;

	private static final String ADD_ICON = "images/add.png";
	private static final String REMOVE_ICON = "images/delete.png";

	private JPanel jPanelBottom;
	private JPanelColorByFeatureGUI jPanelColorByFeatureGUI;
	private JScrollPane jScrollPaneThresholds;
	private JPanel jPanelAllThresholds;
	private JPanel jPanelButtons;
	private JButton jButtonRemoveThreshold;
	private JButton jButtonAddThreshold;
	private JLabel jLabelInfo;

	private Stack<FilterPanel> thresholdPanels = new Stack<FilterPanel>();
	private Stack<Component> struts = new Stack<Component>();
	private Map<String, double[]> featureValues;
	private int newFeatureIndex;

	private List<FeatureFilter> featureFilters = new ArrayList<FeatureFilter>();
	private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
	private List<String> features;
	private String objectDescription;
	private Map<String, String> featureNames;

	/*
	 * CONSTRUCTORS
	 */

	public FilterGuiPanel(List<String> features, List<FeatureFilter> filters,  Map<String, String> featureNames, Map<String, double[]> featureValues, String objectDescription) {
		super();
		this.features = features;
		this.featureNames = featureNames;
		this.featureValues = featureValues;
		this.objectDescription = objectDescription;
		initGUI();
		if (null != featureValues) {

			if (null != filters) {

				for (FeatureFilter ft : filters) {
					addFilterPanel(ft);
				}
				if (filters.isEmpty())
					newFeatureIndex = 0;
				else
					newFeatureIndex = features.indexOf(filters.get(filters.size()-1).feature);

			}
		}
		updateInfoText();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Called when one of the {@link FilterPanel} is changed by the user.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		featureFilters = new ArrayList<FeatureFilter>(thresholdPanels.size());
		for (FilterPanel tp : thresholdPanels) {
			featureFilters.add(new FeatureFilter(tp.getKey(), new Float(tp.getThreshold()), tp.isAboveThreshold()));
		}
		fireThresholdChanged(e);
		updateInfoText();
	}

	/**
	 * Return the thresholds currently set by this GUI.
	 */
	public List<FeatureFilter> getFeatureFilters() {
		return featureFilters;
	}


	/**
	 * Return the feature selected in the "Set color by feature" comb-box. 
	 * Return <code>null</code> if the item "Default" is selected.
	 */
	public String getColorByFeature() {
		return jPanelColorByFeatureGUI.setColorByFeature;
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

	public void addFilterPanel() {
		addFilterPanel(features.get(newFeatureIndex));		
	}

	public void addFilterPanel(FeatureFilter filter) {
		if (null == filter)
			return;
		FilterPanel tp = new FilterPanel(features, featureNames, featureValues, newFeatureIndex);
		tp.setThreshold(filter.value);
		tp.setAboveThreshold(filter.isAbove);		
		tp.addChangeListener(this);
		newFeatureIndex++;
		if (newFeatureIndex >= features.size()) 
			newFeatureIndex = 0;
		Component strut = Box.createVerticalStrut(5);
		struts.push(strut);
		thresholdPanels.push(tp);
		jPanelAllThresholds.add(tp);
		jPanelAllThresholds.add(strut);
		jPanelAllThresholds.revalidate();
		stateChanged(CHANGE_EVENT);
	}


	public void addFilterPanel(String feature) {
		if (null == featureValues)
			return;
		FilterPanel tp = new FilterPanel(features, featureNames, featureValues, features.indexOf(feature));
		tp.addChangeListener(this);
		newFeatureIndex++;
		if (newFeatureIndex >= features.size()) 
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
			FilterPanel tp = thresholdPanels.pop();
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
		if (featureFilters == null || featureFilters.isEmpty()) {
			info = "Keep all "+nobjects+" "+objectDescription+".";
		} else {
			int nselected = 0;
			double val;
			for (int i = 0; i < nobjects; i++) {
				boolean ok = true;
				for (FeatureFilter filter : featureFilters) {
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
			info = "Keep "+nselected+" "+objectDescription+" out of  "+nobjects+".";
		}
		jLabelInfo.setText(info);

	}


	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			this.setLayout(thisLayout);
			setPreferredSize(new Dimension(270, 500));
			{
				JLabel jTopLabel = new JLabel();
				jTopLabel.setFont(BIG_FONT);
				jTopLabel.setText("      Set filters on "+objectDescription);
				jTopLabel.setPreferredSize(new Dimension(300, 40));
				this.add(jTopLabel, BorderLayout.NORTH);
			}
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
								addFilterPanel();
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
					jPanelColorByFeatureGUI = new JPanelColorByFeatureGUI(features, featureNames, this);
					COLOR_FEATURE_CHANGED = jPanelColorByFeatureGUI.COLOR_FEATURE_CHANGED;
					jPanelColorByFeatureGUI.featureValues = featureValues;
					jPanelBottom.add(jPanelColorByFeatureGUI, BorderLayout.CENTER);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
