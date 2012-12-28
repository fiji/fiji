package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

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
	
	private static final boolean DEBUG = false;
	private static final long serialVersionUID = -1L;
	private static final String ADD_ICON = "images/add.png";
	private static final String REMOVE_ICON = "images/delete.png";
	
	private final ChangeEvent CHANGE_EVENT = new ChangeEvent(this);
	/** Will be set to the value of the {@link JPanelColorByFeatureGUI}. */
	public ActionEvent COLOR_FEATURE_CHANGED = null;

	private JPanel jPanelBottom;
	JPanelColorByFeatureGUI jPanelColorByFeatureGUI;
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
	private JLabel jTopLabel;
	private Updater updater;

	/*
	 * CONSTRUCTOR
	 */

	public FilterGuiPanel() {
		this.updater = new Updater();
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the feature filters to display and layout this panel. Calling this method
	 * re-instantiate some components, so that they reflect the passed arguments.
	 * But the core components are not regenerated.
	 * 
	 * @param features  the list of all feature that can be chosen from in {@link FilterPanel}s
	 * @param filters  the list of {@link FeatureFilter}s that should be already present in the GUI 
	 * (for loading purpose). Can be <code>null</code> or empty.
	 * @param featureNames  a mapping linking the feature with a string to represent them.
	 * @param featureValues  a mapping linking the features to their value array.
	 * @param objectDescription  a single word description of the object to filter
	 */
	public void setTarget(List<String> features, List<FeatureFilter> filters,  Map<String, String> featureNames, Map<String, double[]> featureValues, String objectDescription) {
		this.features = features;
		this.featureNames = featureNames;
		this.featureValues = featureValues;
		this.objectDescription = objectDescription;

		// Clean current panels
		int n_panels = thresholdPanels.size();
		for (int i = 0; i < n_panels; i++) {
			removeThresholdPanel();
		}

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

		// Color panel
		if (jPanelColorByFeatureGUI != null) {
			jPanelBottom.remove(jPanelColorByFeatureGUI);
		}
		jPanelColorByFeatureGUI = new JPanelColorByFeatureGUI(features, featureNames, this);
		COLOR_FEATURE_CHANGED = jPanelColorByFeatureGUI.COLOR_FEATURE_CHANGED;
		jPanelColorByFeatureGUI.setFeatureValues(featureValues);
		jPanelBottom.add(jPanelColorByFeatureGUI, BorderLayout.CENTER);

		// Title
		jTopLabel.setText("      Set filters on "+objectDescription);

		// Info text
		updateInfoText();
	}

	/**
	 * Called when one of the {@link FilterPanel} is changed by the user.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		updater.doUpdate();
	}

	/**
	 * @eeturn the thresholds currently set by this GUI.
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
			info = "Keep all "+nobjects+" "+objectDescription+".";
		} else {
			int nselected = 0;
			double val;
			for (int i = 0; i < nobjects; i++) {
				boolean ok = true;
				for (FeatureFilter filter : featureFilters) {
					double[] values =  featureValues.get(filter.feature);
					if (values.length == 0) { // bulletproof against unspecified features, which are signaled by empty arrays
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
				jTopLabel = new JLabel();
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This is a helper class that delegates the
	 * repainting of the destination window to another thread.
	 * 
	 * @author Albert Cardona
	 */
	private class Updater extends Thread {
		private long request = 0;

		// Constructor autostarts thread
		Updater() {
			super("TrackMate FilterGuiPanel repaint thread");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		@SuppressWarnings("unused")
		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call update from this thread
					if (r > 0) {
						refresh();
					}
						
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {
				}
			}
		}
	}
}
