package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_NAMES;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOTS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_RADIUS_RATIO;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACKS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_MODE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.TRACK_SCHEME_ICON;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.DisplaySettingsEvent;
import fiji.plugin.trackmate.gui.DisplaySettingsListener;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel.Category;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * A configuration panel used to tune the aspect of spots and tracks in multiple {@link AbstractTrackMateModelView}.
 * This GUI takes the role of a controller.
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr>   -  2010 - 2011
 */
public class ConfigureViewsPanel extends ActionListenablePanel {

	private static final long serialVersionUID = 1L;

	private static final Icon DO_ANALYSIS_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/calculator.png"));
	public ActionEvent TRACK_SCHEME_BUTTON_PRESSED 	= new ActionEvent(this, 0, "TrackSchemeButtonPushed");
	public ActionEvent DO_ANALYSIS_BUTTON_PRESSED 	= new ActionEvent(this, 1, "DoAnalysisButtonPushed");

	private static final String ANALYSIS_BUTTON_TOOLTIP = "<html>"
			+ "Export all spot, edge and track features <br>"
			+ "to ImageJ tables.</html>";
	private static final String TRACKSCHEME_BUTTON_TOOLTIP = "<html>"
			+ "Launch a new instance of TrackScheme.</html>";

	/** A map of String/Object that configures the look and feel of the views.	 */
	protected Map<String, Object> displaySettings = new HashMap<String, Object>();


	protected JButton jButtonShowTrackScheme;
	protected JButton jButtonDoAnalysis;
	private JLabel jLabelTrackDisplayMode;
	private JComboBox jComboBoxDisplayMode;
	private JLabel jLabelDisplayOptions;
	private JPanel jPanelSpotOptions;
	private JCheckBox jCheckBoxDisplaySpots;
	private JPanel jPanelTrackOptions;
	private JCheckBox jCheckBoxDisplayTracks;
	private JCheckBox jCheckBoxLimitDepth;
	private JTextField jTextFieldFrameDepth;
	private JLabel jLabelFrameDepth;
	private ColorByFeatureGUIPanel jPanelSpotColor;
	private JNumericTextField jTextFieldSpotRadius;
	private JCheckBox jCheckBoxDisplayNames;
	private ColorByFeatureGUIPanel trackColorGUI;

	private final Collection<DisplaySettingsListener> listeners = new HashSet<DisplaySettingsListener>();
	private final Model model;

	private PerTrackFeatureColorGenerator trackColorGenerator;
	private PerEdgeFeatureColorGenerator edgeColorGenerator;
	private FeatureColorGenerator<Spot> spotColorGenerator;


	/*
	 * CONSTRUCTOR
	 */

	public ConfigureViewsPanel(final Model model) {
		this.model = model;
		initGUI();
		refreshGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Adds the specified {@link DisplaySettingsListener} to the collection of listeners
	 * that will be notified when a display settings change is made on this GUI.
	 * @param listener the listener to add.
	 */
	public void addDisplaySettingsChangeListener(final DisplaySettingsListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes the specified {@link DisplaySettingsListener} from the collection of listeners
	 * of this GUI.
	 * @param listener the listener to remove.
	 * @return <code>true</code> if the listener belonged to the list of registered listener
	 * and was successfully removed.
	 */
	public boolean removeDisplaySettingsChangeListener(final DisplaySettingsListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Exposes the {@link JButton} that should trigger the launch of TrackScheme.
	 * @return the TrackScheme {@link JButton}.
	 */
	public JButton getTrackSchemeButton() {
		return jButtonShowTrackScheme;
	}

	/**
	 * Exposes the {@link JButton} that should trigger the launch of analysis.
	 * @return the analysis {@link JButton}.
	 */
	public JButton getDoAnalysisButton() {
		return jButtonDoAnalysis;
	}

	/**
	 * Overrides the track color generator configured in this panel, allowing to share instances.
	 * @param trackColorGenerator the new color generator. The previous one will be terminated.
	 */
	public void setTrackColorGenerator(final PerTrackFeatureColorGenerator trackColorGenerator) {
		if (null != this.trackColorGenerator) {
			this.trackColorGenerator.terminate();
		}
		this.trackColorGenerator = trackColorGenerator;
	}

	/**
	 * Overrides the edge color generator configured in this panel, allowing to share instances.
	 * @param edgeColorGenerator the new color generator. The previous one will be terminated.
	 */
	public void setEdgeColorGenerator(final PerEdgeFeatureColorGenerator edgeColorGenerator) {
		if (null != this.edgeColorGenerator) {
			this.edgeColorGenerator.terminate();
		}
		this.edgeColorGenerator = edgeColorGenerator;
	}

	/**
	 * Overrides the spot color generator configured in this panel, allowing to share instances.
	 * @param spotColorGenerator the new color generator.
	 */
	public void setSpotColorGenerator(final FeatureColorGenerator<Spot> spotColorGenerator) {
		if (null != this.spotColorGenerator) {
			this.spotColorGenerator.terminate();
		}
		this.spotColorGenerator = spotColorGenerator;
	}

	public void refreshColorFeatures() {
		jPanelSpotColor.setColorFeature(spotColorGenerator.getFeature());
		trackColorGUI.setColorFeature(trackColorGenerator.getFeature());
	}


	/**
	 * Refreshes some components of this GUI with current values of the model.
	 */
	public void refreshGUI() {

		/*
		 * Spot coloring
		 */

		if (null != jPanelSpotColor) {
			jPanelSpotOptions.remove(jPanelSpotColor);
		}
		jPanelSpotColor = new ColorByFeatureGUIPanel(model, Arrays.asList(new Category[] { Category.SPOTS, Category.DEFAULT } ));
		jPanelSpotColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (null == spotColorGenerator) {
					return;
				}
				spotColorGenerator.setFeature(jPanelSpotColor.getColorFeature());
				final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_SPOT_COLORING, spotColorGenerator, spotColorGenerator);
				fireDisplaySettingsChange(event);
			}
		});
		jPanelSpotOptions.add(jPanelSpotColor);

		/*
		 * Track coloring
		 */

		if (null != trackColorGUI) {
			jPanelTrackOptions.remove(trackColorGUI);
		}

		trackColorGUI = new ColorByFeatureGUIPanel(model, Arrays.asList(new Category[] { Category.TRACKS, Category.EDGES, Category.DEFAULT } ));
		//		trackColorGUI.setPreferredSize(new java.awt.Dimension(265, 45));
		trackColorGUI.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final TrackColorGenerator oldValue = (TrackColorGenerator) displaySettings.get(KEY_TRACK_COLORING);
				TrackColorGenerator newValue;
				final Category category = trackColorGUI.getColorGeneratorCategory();
				switch (category) {
				case TRACKS:
					newValue = trackColorGenerator;
					newValue.setFeature(trackColorGUI.getColorFeature());
					break;
				case EDGES:
					newValue = edgeColorGenerator;
					newValue.setFeature(trackColorGUI.getColorFeature());
					break;
				case DEFAULT:
					newValue = trackColorGenerator;
					newValue.setFeature(null);
					break;
				default:
					throw new IllegalArgumentException("Unknow track color generator category: " + category);
				}
				displaySettings.put(KEY_TRACK_COLORING, newValue);
				// new value vs old value does not really hold.
				final DisplaySettingsEvent event = new DisplaySettingsEvent(trackColorGUI, KEY_TRACK_COLORING, newValue, oldValue);
				fireDisplaySettingsChange(event);
			}
		});
		jPanelTrackOptions.add(trackColorGUI);


		if (spotColorGenerator != null) {
			jPanelSpotColor.setColorFeature(spotColorGenerator.getFeature());
		}
		if (trackColorGenerator != null) {
			trackColorGUI.setColorFeature(trackColorGenerator.getFeature());
		}

	}

	/*
	 * PRIVATE METHODS
	 */

	private void fireDisplaySettingsChange(final DisplaySettingsEvent event) {
		for (final DisplaySettingsListener listener : listeners) {
			listener.displaySettingsChanged(event);
		}
	}

	private void initGUI() {
		try {
			this.setPreferredSize(new Dimension(300, 469));
			this.setSize(300, 500);
			this.setLayout(null);
			{
				jPanelTrackOptions = new JPanel() {
					private static final long serialVersionUID = -1805693239189343720L;
					@Override
					public void setEnabled(final boolean enabled) {
						for(final Component c : getComponents())
							c.setEnabled(enabled);
					};
				};
				final FlowLayout jPanelTrackOptionsLayout = new FlowLayout();
				jPanelTrackOptionsLayout.setAlignment(FlowLayout.LEFT);
				jPanelTrackOptions.setLayout(jPanelTrackOptionsLayout);
				this.add(jPanelTrackOptions);
				jPanelTrackOptions.setBounds(10, 212, 280, 188);
				jPanelTrackOptions.setBorder(new LineBorder(new java.awt.Color(192,192,192), 1, true));
				{
					jLabelTrackDisplayMode = new JLabel();
					jPanelTrackOptions.add(jLabelTrackDisplayMode);
					jLabelTrackDisplayMode.setText("  Track display mode:");
					jLabelTrackDisplayMode.setBounds(10, 163, 268, 15);
					jLabelTrackDisplayMode.setFont(FONT);
					jLabelTrackDisplayMode.setPreferredSize(new java.awt.Dimension(261, 14));
				}
				{
					final String[] keyNames = TrackMateModelView.TRACK_DISPLAY_MODE_DESCRIPTION;
					final ComboBoxModel jComboBoxDisplayModeModel = new DefaultComboBoxModel(keyNames);
					jComboBoxDisplayMode = new JComboBox();
					jPanelTrackOptions.add(jComboBoxDisplayMode);
					jComboBoxDisplayMode.setModel(jComboBoxDisplayModeModel);
					jComboBoxDisplayMode.setSelectedIndex(0);
					jComboBoxDisplayMode.setFont(SMALL_FONT);
					jComboBoxDisplayMode.setPreferredSize(new java.awt.Dimension(265, 27));
					jComboBoxDisplayMode.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							final Integer oldValue = (Integer) displaySettings.get(KEY_TRACK_DISPLAY_MODE);
							final Integer newValue = jComboBoxDisplayMode.getSelectedIndex();
							displaySettings.put(KEY_TRACK_DISPLAY_MODE, newValue);

							final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_TRACK_DISPLAY_MODE, newValue, oldValue);
							fireDisplaySettingsChange(event);
						}
					});
				}
				{
					jCheckBoxLimitDepth = new JCheckBox();
					jPanelTrackOptions.add(jCheckBoxLimitDepth);
					jCheckBoxLimitDepth.setText("Limit frame depth");
					jCheckBoxLimitDepth.setBounds(6, 216, 272, 23);
					jCheckBoxLimitDepth.setFont(FONT);
					jCheckBoxLimitDepth.setSelected(true);
					jCheckBoxLimitDepth.setPreferredSize(new java.awt.Dimension(259, 23));
					jCheckBoxLimitDepth.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							Integer depth;
							if (jCheckBoxLimitDepth.isSelected()) {
								depth = Integer.parseInt(jTextFieldFrameDepth.getText());
							} else {
								depth = (int) 1e9;
							}
							final Integer oldValue = (Integer) displaySettings.get(KEY_TRACK_DISPLAY_DEPTH);
							displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, depth);

							final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_TRACK_DISPLAY_DEPTH, depth, oldValue);
							fireDisplaySettingsChange(event);
						}
					});
				}
				{
					jLabelFrameDepth = new JLabel();
					jPanelTrackOptions.add(jLabelFrameDepth);
					jLabelFrameDepth.setText("  Frame depth:");
					jLabelFrameDepth.setFont(SMALL_FONT);
					jLabelFrameDepth.setPreferredSize(new java.awt.Dimension(103, 14));
				}
				{
					jTextFieldFrameDepth = new JTextField();
					jPanelTrackOptions.add(jTextFieldFrameDepth);
					jTextFieldFrameDepth.setFont(SMALL_FONT);
					jTextFieldFrameDepth.setText(""+TrackMateModelView.DEFAULT_TRACK_DISPLAY_DEPTH);
					jTextFieldFrameDepth.setPreferredSize(new java.awt.Dimension(34, 20));
					jTextFieldFrameDepth.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							final Integer depth = Integer.parseInt(jTextFieldFrameDepth.getText());
							final Integer oldValue = (Integer) displaySettings.get(KEY_TRACK_DISPLAY_DEPTH);
							displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, depth);

							final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_TRACK_DISPLAY_DEPTH, depth, oldValue);
							fireDisplaySettingsChange(event);
						}
					});
				}
			}
			{
				jCheckBoxDisplayTracks = new JCheckBox();
				this.add(jCheckBoxDisplayTracks);
				jCheckBoxDisplayTracks.setText("Display tracks");
				jCheckBoxDisplayTracks.setFont(FONT);
				jCheckBoxDisplayTracks.setBounds(10, 188, 233, 23);
				jCheckBoxDisplayTracks.setSelected(true);
				jCheckBoxDisplayTracks.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						final Boolean oldValue = (Boolean) displaySettings.get(KEY_TRACKS_VISIBLE);
						final Boolean newValue =  jCheckBoxDisplayTracks.isSelected();
						displaySettings.put(KEY_TRACKS_VISIBLE, newValue);

						final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_TRACKS_VISIBLE, newValue, oldValue);
						fireDisplaySettingsChange(event);
					}
				});
			}
			{
				jCheckBoxDisplaySpots = new JCheckBox();
				this.add(jCheckBoxDisplaySpots);
				jCheckBoxDisplaySpots.setText("Display spots");
				jCheckBoxDisplaySpots.setFont(FONT);
				jCheckBoxDisplaySpots.setBounds(10, 38, 280, 23);
				jCheckBoxDisplaySpots.setSelected(true);
				jCheckBoxDisplaySpots.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						final Boolean oldValue = (Boolean) displaySettings.get(KEY_SPOTS_VISIBLE);
						final Boolean newValue =  jCheckBoxDisplaySpots.isSelected();
						displaySettings.put(KEY_SPOTS_VISIBLE, newValue);

						final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_SPOTS_VISIBLE, newValue, oldValue);
						fireDisplaySettingsChange(event);
					}
				});
			}
			{
				jPanelSpotOptions = new JPanel() {
					private static final long serialVersionUID = 1L;
					@Override
					public void setEnabled(final boolean enabled) {
						for(final Component c : getComponents())
							c.setEnabled(enabled);
					};
				};
				final FlowLayout jPanelSpotOptionsLayout = new FlowLayout();
				jPanelSpotOptionsLayout.setAlignment(FlowLayout.LEFT);
				jPanelSpotOptions.setLayout(jPanelSpotOptionsLayout);
				this.add(jPanelSpotOptions);
				jPanelSpotOptions.setBounds(10, 63, 280, 110);
				jPanelSpotOptions.setBorder(new LineBorder(new java.awt.Color(192,192,192), 1, true));
				{
					final JLabel jLabelSpotRadius = new JLabel();
					jLabelSpotRadius.setText("  Spot display radius ratio:");
					jLabelSpotRadius.setFont(SMALL_FONT);
					jPanelSpotOptions.add(jLabelSpotRadius);

					jTextFieldSpotRadius = new JNumericTextField("1");
					jTextFieldSpotRadius.setPreferredSize(new java.awt.Dimension(34, 20));
					jTextFieldSpotRadius.setFont(SMALL_FONT);
					jPanelSpotOptions.add(jTextFieldSpotRadius);
					jTextFieldSpotRadius.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							final Float oldValue = (Float) displaySettings.get(KEY_SPOT_RADIUS_RATIO);
							final Float newValue =  (float) jTextFieldSpotRadius.getValue();
							displaySettings.put(KEY_SPOT_RADIUS_RATIO, newValue);

							final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_SPOT_RADIUS_RATIO, newValue, oldValue);
							fireDisplaySettingsChange(event);
						}
					});
					jTextFieldSpotRadius.addFocusListener(new FocusListener() {
						@Override
						public void focusLost(final FocusEvent e) {
							final Float oldValue = (Float) displaySettings.get(KEY_SPOT_RADIUS_RATIO);
							final Float newValue =  (float) jTextFieldSpotRadius.getValue();
							displaySettings.put(KEY_SPOT_RADIUS_RATIO, newValue);

							final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_SPOT_RADIUS_RATIO, newValue, oldValue);
							fireDisplaySettingsChange(event);
						}
						@Override
						public void focusGained(final FocusEvent e) {}
					});
				}
				{
					jCheckBoxDisplayNames = new JCheckBox();
					jCheckBoxDisplayNames.setText("Display spot names");
					jCheckBoxDisplayNames.setFont(SMALL_FONT);
					jCheckBoxDisplayNames.setSelected(false);
					jPanelSpotOptions.add(jCheckBoxDisplayNames);
					jCheckBoxDisplayNames.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							final Boolean oldValue = (Boolean) displaySettings.get(KEY_DISPLAY_SPOT_NAMES);
							final Boolean newValue = jCheckBoxDisplayNames.isSelected();
							displaySettings.put(KEY_DISPLAY_SPOT_NAMES, newValue);

							final DisplaySettingsEvent event = new DisplaySettingsEvent(ConfigureViewsPanel.this, KEY_DISPLAY_SPOT_NAMES, newValue, oldValue);
							fireDisplaySettingsChange(event);
						}
					});
				}
			}
			{
				jLabelDisplayOptions = new JLabel();
				jLabelDisplayOptions.setText("Display options");
				jLabelDisplayOptions.setFont(BIG_FONT);
				jLabelDisplayOptions.setBounds(20, 11, 280, 20);
				jLabelDisplayOptions.setHorizontalAlignment(SwingConstants.LEFT);
				this.add(jLabelDisplayOptions);
			}
			{
				jButtonShowTrackScheme = new JButton();
				jButtonShowTrackScheme.setText("Track scheme");
				jButtonShowTrackScheme.setIcon(TRACK_SCHEME_ICON);
				jButtonShowTrackScheme.setFont(FONT);
				jButtonShowTrackScheme.setToolTipText(TRACKSCHEME_BUTTON_TOOLTIP);
				jButtonShowTrackScheme.setBounds(10, 411, 120, 30);
				jButtonShowTrackScheme.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						fireAction(TRACK_SCHEME_BUTTON_PRESSED);
					}
				});
				this.add(jButtonShowTrackScheme);
			}
			{
				jButtonDoAnalysis = new JButton("Analysis");
				jButtonDoAnalysis.setFont(FONT);
				jButtonDoAnalysis.setIcon(DO_ANALYSIS_ICON);
				jButtonDoAnalysis.setToolTipText(ANALYSIS_BUTTON_TOOLTIP);
				jButtonDoAnalysis.setBounds(145, 411, 120, 30);
				jButtonDoAnalysis.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent arg0) {
						fireAction(DO_ANALYSIS_BUTTON_PRESSED);
					}
				});
				this.add(jButtonDoAnalysis);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}


}
