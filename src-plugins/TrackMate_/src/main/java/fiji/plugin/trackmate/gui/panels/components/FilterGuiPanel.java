package fiji.plugin.trackmate.gui.panels.components;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
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

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;
import fiji.plugin.trackmate.gui.panels.FilterPanel;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel.Category;
import fiji.plugin.trackmate.util.OnRequestUpdater;

public class FilterGuiPanel extends ActionListenablePanel implements ChangeListener {

	private static final boolean DEBUG = false;
	private static final long serialVersionUID = -1L;
	private static final String ADD_ICON = "images/add.png";
	private static final String REMOVE_ICON = "images/delete.png";

	private final ChangeEvent CHANGE_EVENT = new ChangeEvent(this);
	/** Will be set to the value of the {@link JPanelColorByFeatureGUI}. */
	public ActionEvent COLOR_FEATURE_CHANGED = null;

	private JPanel jPanelBottom;
	private ColorByFeatureGUIPanel jPanelColorByFeatureGUI;
	private JScrollPane jScrollPaneThresholds;
	private JPanel jPanelAllThresholds;
	private JPanel jPanelButtons;
	private JButton jButtonRemoveThreshold;
	private JButton jButtonAddThreshold;
	private JLabel jLabelInfo;
	private JLabel jTopLabel;
	private OnRequestUpdater updater;

	private Stack<FilterPanel> thresholdPanels = new Stack<FilterPanel>();
	private Stack<Component> struts = new Stack<Component>();
	private int newFeatureIndex;

	private List<FeatureFilter> featureFilters = new ArrayList<FeatureFilter>();
	private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

	private final Map<String, String> featureNames;
	private final Category category;
	private final Model model;
	private final List<String> features;
	/** Holds the map of feature values. Is made final so that the instance can be shared with the components
	 * of this panel. */
	private final Map<String, double[]> featureValues;

	/*
	 * CONSTRUCTOR
	 */

	public FilterGuiPanel(Model model, Category category) {
		this.model = model;

		model.addModelChangeListener(new ModelChangeListener() {

			@Override
			public void modelChanged(ModelChangeEvent event) {
				if (event.getEventID() == ModelChangeEvent.MODEL_MODIFIED) {
					refreshDisplayedFeatureValues();
					for (FilterPanel fp : thresholdPanels) {
						fp.refresh();
					}
				}
			}
		});

		this.category = category;
		switch (category) {
		case SPOTS:
			this.features = new ArrayList<String>(model.getFeatureModel().getSpotFeatures());
			this.featureNames = model.getFeatureModel().getSpotFeatureNames();
			break;
		case EDGES:
			this.features = new ArrayList<String>(model.getFeatureModel().getEdgeFeatures());
			this.featureNames = model.getFeatureModel().getEdgeFeatureNames();
			break;
		case TRACKS:
			this.features = new ArrayList<String>(model.getFeatureModel().getTrackFeatures());
			this.featureNames = model.getFeatureModel().getTrackFeatureNames();
			break;
		default:
			throw new IllegalArgumentException("Unkown actegory: " + category);
		}
		this.updater = new OnRequestUpdater(new OnRequestUpdater.Refreshable() {
			@Override
			public void refresh() {
				FilterGuiPanel.this.refresh();
			}
		});

		this.featureValues = new HashMap<String, double[]>();
		refreshDisplayedFeatureValues();
		initGUI();
	}


	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Calls the re-calculation of the feature values displayed in the filter
	 * panels. 
	 */
	public void refreshDisplayedFeatureValues() {
		Map<String, double[]> fv;
		switch (category) {
		case SPOTS:
			fv = model.getSpots().collectValues(model.getFeatureModel().getSpotFeatures(), false);
			break;
		case TRACKS:
			fv = model.getFeatureModel().getTrackFeatureValues();
			break;
		default:
			throw new IllegalArgumentException("Don't know what to do with category: " + category);
		}
		featureValues.clear();
		featureValues.putAll(fv);
	}

	/**
	 * Set the feature filters to display and layout in this panel. 
	 * @param filters  the list of {@link FeatureFilter}s that should be already present in the GUI 
	 * (for loading purpose). Can be <code>null</code> or empty.
	 */
	public void setFilters(List<FeatureFilter> filters) {
		// Clean current panels
		int n_panels = thresholdPanels.size();
		for (int i = 0; i < n_panels; i++) {
			removeThresholdPanel();
		}

		if (null != filters) {

			for (FeatureFilter ft : filters) {
				addFilterPanel(ft);
			}
			if (filters.isEmpty())
				newFeatureIndex = 0;
			else
				newFeatureIndex = this.features.indexOf(filters.get(filters.size()-1).feature);

		}
	}

	/**
	 * Called when one of the {@link FilterPanel} is changed by the user.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		updater.doUpdate();
	}

	/**
	 * Returns the thresholds currently set by this GUI.
	 */
	public List<FeatureFilter> getFeatureFilters() {
		return featureFilters;
	}

	/**
	 * Returns the feature selected in the "color by feature" comb-box. 
	 * Returns <code>null</code> if the item "Default" is selected.
	 */
	public String getColorFeature() {
		return jPanelColorByFeatureGUI.getColorFeature();
	}

	public void setColorFeature(String feature) {
		jPanelColorByFeatureGUI.setColorFeature(feature);
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
		for (ChangeListener cl : changeListeners)  {
			cl.stateChanged(e);
		}
	}

	public void addFilterPanel() {
		addFilterPanel(features.get(newFeatureIndex));		
	}

	public void addFilterPanel(FeatureFilter filter) {
		if (null == filter)
			return;

		int filterIndex = features.indexOf(filter.feature);
		FilterPanel tp = new FilterPanel(features, featureNames, featureValues, filterIndex);
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

	private void refresh() {
		if (DEBUG) {
			System.out.println("[FilterGuiPanel] #refresh()");
		}
		featureFilters = new ArrayList<FeatureFilter>(thresholdPanels.size());
		for (FilterPanel tp : thresholdPanels) {
			featureFilters.add(new FeatureFilter(tp.getKey(), new Double(tp.getThreshold()), tp.isAboveThreshold()));
		}
		fireThresholdChanged(null);
		updateInfoText();
	}

	private void updateInfoText() {
		String info = "";
		int nobjects = 0;

		for (double[] values : featureValues.values()) { // bulletproof against unspecified features, which are signaled by empty arrays
			if (values.length > 0) {
				nobjects = values.length;
				break;
			}
		}

		if (nobjects == 0)	{
			info = "No objects.";
		} else if (featureFilters == null || featureFilters.isEmpty() ) {
			info = "Keep all "+nobjects+" "+category+".";
		} else {
			int nselected = 0;
			double val;
			for (int i = 0; i < nobjects; i++) {
				boolean ok = true;
				for (FeatureFilter filter : featureFilters) {
					double[] values =  featureValues.get(filter.feature);
					if (i >= values.length || values.length == 0) { // bulletproof 
						continue;
					}
					val = values[i];
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
			info = "Keep "+nselected+" "+category+" out of  "+nobjects+".";
		}
		jLabelInfo.setText(info);

	}


	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			this.setLayout(thisLayout);
			setPreferredSize(new Dimension(270, 500));
			{
				jTopLabel = new JLabel();
				jTopLabel.setText("      Set filters on " + category.toString());
				jTopLabel.setFont(BIG_FONT);
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
						jButtonAddThreshold.setIcon(new ImageIcon(TrackMateWizard.class.getResource(ADD_ICON)));
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
						jButtonRemoveThreshold.setIcon(new ImageIcon(TrackMateWizard.class.getResource(REMOVE_ICON)));
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
					jPanelColorByFeatureGUI = new ColorByFeatureGUIPanel(model, Arrays.asList(new Category[] { category } ));
					COLOR_FEATURE_CHANGED = jPanelColorByFeatureGUI.COLOR_FEATURE_CHANGED;
					jPanelColorByFeatureGUI.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							fireAction(COLOR_FEATURE_CHANGED);
						}
					});
					jPanelBottom.add(jPanelColorByFeatureGUI);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
