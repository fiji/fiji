package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_NAMES;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOTS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLOR_FEATURE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_RADIUS_RATIO;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACKS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_MODE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.TRACK_SCHEME_ICON;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import fiji.util.NumberParser;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * A configuration panel used to tune the aspect of spots and tracks in multiple {@link AbstractTrackMateModelView}.
 * This GUI takes the role of a controller.
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr>   -  2010 - 2011
 */
public class DisplayerPanel extends ActionListenablePanel implements WizardPanelDescriptor {

	private static final long serialVersionUID = 1L;

	public static final String DESCRIPTOR = "DisplayerPanel";

	private static final Icon DO_ANALYSIS_ICON = new ImageIcon(DisplayerPanel.class.getResource("images/calculator.png"));
	public ActionEvent TRACK_SCHEME_BUTTON_PRESSED 	= new ActionEvent(this, 0, "TrackSchemeButtonPushed");
	public ActionEvent DO_ANALYSIS_BUTTON_PRESSED 	= new ActionEvent(this, 1, "DoAnalysisButtonPushed");

	JButton jButtonShowTrackScheme;
	JButton jButtonDoAnalysis;
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
	private JPanelColorByFeatureGUI jPanelSpotColor;
	private JNumericTextField jTextFieldSpotRadius;
	private JCheckBox jCheckBoxDisplayNames;

	/**
	 * The set of {@link TrackMateModelView} views controlled by this controller.
	 */
	private Set<TrackMateModelView> views = new HashSet<TrackMateModelView>();
	private TrackMate_ plugin;
	private TrackMateWizard wizard;

	private TrackColorByFeatureGUI trackColorGUI;


	/*
	 * CONSTRUCTOR 
	 */

	public DisplayerPanel() {
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
		setModel(plugin.getModel());
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
		return GrapherPanel.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return TrackFilterDescriptor.DESCRIPTOR;
	}


	@Override
	public void aboutToDisplayPanel() {
		setModel(plugin.getModel());
		register(wizard.getDisplayer());
	}

	@Override
	public void displayingPanel() { 
		wizard.setNextButtonEnabled(true);
	}


	@Override
	public void aboutToHidePanel() { }

	/**
	 * Add the given {@link TrackMateModelView} to the list managed by this controller.
	 */
	public void register(final TrackMateModelView view) {
		if (!views.contains(view)) {
			views.add(view);
		}
	}

	@Override
	protected void fireAction(final ActionEvent event) {
		new Thread("TrackMate dispatch displayer panel action thread") {
			@Override
			public void run() {
				// Intercept event coming from the JPanelSpotColorGUI, and translate it for views
				if (event == jPanelSpotColor.COLOR_FEATURE_CHANGED) {
					for (TrackMateModelView view : views) {
						view.setDisplaySettings(KEY_SPOT_COLOR_FEATURE, jPanelSpotColor.setColorByFeature);
						view.refresh();
					}
				} if (event == trackColorGUI.TRACK_COLOR_FEATURE_CHANGED) {

					for (TrackMateModelView view : views) {
						view.setDisplaySettings(KEY_TRACK_COLORING, trackColorGUI.getColorGenerator());
						view.refresh();
					}

				} else if (event == TRACK_SCHEME_BUTTON_PRESSED) {

					// Display Track scheme
					jButtonShowTrackScheme.setEnabled(false);
					new Thread("TrackMate_ laucnhing TrackScheme thread") {
						public void run() {	
							try {
								TrackScheme trackScheme = new TrackScheme(plugin.getModel());
								Map<String, Object> displaySettings = new HashMap<String, Object>();
								updateDisplaySettings(displaySettings);
								for (String settingKey : displaySettings.keySet()) {
									trackScheme.setDisplaySettings(settingKey, displaySettings.get(settingKey));
								}
								trackScheme.render();
								register(trackScheme);
							} finally {
								jButtonShowTrackScheme.setEnabled(true);
							}
						}
					}.start();

				} else if (event == DO_ANALYSIS_BUTTON_PRESSED) {

					jButtonDoAnalysis.setEnabled(false);
					wizard.disableButtonsAndStoreState();
					wizard.showDescriptorPanelFor(LoadDescriptor.DESCRIPTOR);

					new Thread("TrackMate export analysis to IJ thread.") {
						public void run() {
							try {
								ExportStatsToIJAction action = new ExportStatsToIJAction();
								action.execute(plugin);
							} finally {
								wizard.showDescriptorPanelFor(DisplayerPanel.DESCRIPTOR);
								wizard.restoreButtonsState();
								jButtonDoAnalysis.setEnabled(true);
							}
						};
					}.start();

				} else {
					DisplayerPanel.super.fireAction(event);
				} 
			}
		}.start();
	}

	/**
	 * Update the values of the given map to reflect the user settings made in this panel.
	 */
	public void updateDisplaySettings(final Map<String, Object> displaySettings) {
		displaySettings.put(KEY_TRACK_DISPLAY_MODE, jComboBoxDisplayMode.getSelectedIndex());
		displaySettings.put(KEY_TRACKS_VISIBLE, jCheckBoxDisplayTracks.isSelected());
		displaySettings.put(KEY_DISPLAY_SPOT_NAMES, jCheckBoxDisplayNames.isSelected());
		displaySettings.put(KEY_SPOT_COLOR_FEATURE, jPanelSpotColor.setColorByFeature);
		displaySettings.put(KEY_SPOT_RADIUS_RATIO, (float) jTextFieldSpotRadius.getValue());
		displaySettings.put(KEY_SPOTS_VISIBLE, jCheckBoxDisplaySpots.isSelected());
		displaySettings.put(KEY_TRACK_COLORING, trackColorGUI.getColorGenerator());
		int depth;
		if (jCheckBoxLimitDepth.isSelected())
			depth = NumberParser.parseInteger(jTextFieldFrameDepth.getText());
		else
			depth = Integer.MAX_VALUE;
		displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, depth);
	}

	/*
	 * PRIVATE METHODS
	 */

	private void setModel(TrackMateModel model) {
		Map<String, double[]> featureValues = model.getFeatureModel().getSpotFeatureValues();
		List<String> features = model.getFeatureModel().getSpotFeatures();
		Map<String, String> featureNames = model.getFeatureModel().getSpotFeatureNames();

		if (jPanelSpotColor == null) {
			jPanelSpotColor = new JPanelColorByFeatureGUI(features, featureNames, this);
			jPanelSpotOptions.add(jPanelSpotColor);
			jPanelSpotColor.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for(TrackMateModelView view : views) {
						view.setDisplaySettings(KEY_SPOT_COLOR_FEATURE, jPanelSpotColor.setColorByFeature);
						view.refresh();
					}							
				}
			});
		}
		jPanelSpotColor.setFeatureValues(featureValues);

		if (trackColorGUI != null) {
			jPanelTrackOptions.remove(trackColorGUI);
		}
		trackColorGUI = new TrackColorByFeatureGUI(plugin.getModel(), this);
		trackColorGUI.setPreferredSize(new java.awt.Dimension(265, 45));
		jPanelTrackOptions.add(trackColorGUI);
	}

	private void initGUI() {
		try {
			this.setPreferredSize(new Dimension(300, 469));
			this.setSize(300, 500);
			this.setLayout(null);
			{
				jPanelTrackOptions = new JPanel() {
					private static final long serialVersionUID = -1805693239189343720L;
					public void setEnabled(boolean enabled) {
						for(Component c : getComponents())
							c.setEnabled(enabled);
					};
				};
				FlowLayout jPanelTrackOptionsLayout = new FlowLayout();
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
					String[] keyNames = TrackMateModelView.TRACK_DISPLAY_MODE_DESCRIPTION;
					ComboBoxModel jComboBoxDisplayModeModel = new DefaultComboBoxModel(keyNames);
					jComboBoxDisplayMode = new JComboBox();
					jPanelTrackOptions.add(jComboBoxDisplayMode);
					jComboBoxDisplayMode.setModel(jComboBoxDisplayModeModel);
					jComboBoxDisplayMode.setSelectedIndex(0);
					jComboBoxDisplayMode.setFont(SMALL_FONT);
					jComboBoxDisplayMode.setPreferredSize(new java.awt.Dimension(265, 27));
					jComboBoxDisplayMode.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_TRACK_DISPLAY_MODE, jComboBoxDisplayMode.getSelectedIndex());
								view.refresh();
							}
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
						public void actionPerformed(ActionEvent e) {
							int depth;
							if (jCheckBoxLimitDepth.isSelected())
								depth = NumberParser.parseInteger(jTextFieldFrameDepth.getText());
							else
								depth = (int) 1e9;
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_TRACK_DISPLAY_DEPTH, depth);
								view.refresh();
							}
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
						public void actionPerformed(ActionEvent e) {
							int depth = NumberParser.parseInteger(jTextFieldFrameDepth.getText());
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_TRACK_DISPLAY_DEPTH, depth);
								view.refresh();
							}
						}
					});
				}
				{

					// Color GUI will be added later, when we receive the plugin object. It's like that.

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
					public void actionPerformed(ActionEvent e) {
						boolean isSelected = jCheckBoxDisplayTracks.isSelected();
						jPanelTrackOptions.setEnabled(isSelected);
						for(TrackMateModelView view : views) {
							view.setDisplaySettings(KEY_TRACKS_VISIBLE, isSelected);
							view.refresh();
						}
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
					public void actionPerformed(ActionEvent e) {
						boolean isSelected = jCheckBoxDisplaySpots.isSelected();
						jPanelSpotOptions.setEnabled(isSelected);
						for(TrackMateModelView view : views) {
							view.setDisplaySettings(KEY_SPOTS_VISIBLE, isSelected);
							view.refresh();
						}
					}
				});
			}
			{
				jPanelSpotOptions = new JPanel() {
					private static final long serialVersionUID = 3259314983744108471L;
					public void setEnabled(boolean enabled) {
						for(Component c : getComponents())
							c.setEnabled(enabled);
					};
				};
				FlowLayout jPanelSpotOptionsLayout = new FlowLayout();
				jPanelSpotOptionsLayout.setAlignment(FlowLayout.LEFT);
				jPanelSpotOptions.setLayout(jPanelSpotOptionsLayout);
				this.add(jPanelSpotOptions);
				jPanelSpotOptions.setBounds(10, 63, 280, 110);
				jPanelSpotOptions.setBorder(new LineBorder(new java.awt.Color(192,192,192), 1, true));
				{
					JLabel jLabelSpotRadius = new JLabel();
					jLabelSpotRadius.setText("  Spot display radius ratio:");
					jLabelSpotRadius.setFont(SMALL_FONT);
					jPanelSpotOptions.add(jLabelSpotRadius);

					jTextFieldSpotRadius = new JNumericTextField("1");
					jTextFieldSpotRadius.setPreferredSize(new java.awt.Dimension(34, 20));
					jTextFieldSpotRadius.setFont(SMALL_FONT);
					jPanelSpotOptions.add(jTextFieldSpotRadius);
					jTextFieldSpotRadius.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_SPOT_RADIUS_RATIO, (float) jTextFieldSpotRadius.getValue());
								view.refresh();
							}
						}
					});
					jTextFieldSpotRadius.addFocusListener(new FocusListener() {
						@Override
						public void focusLost(FocusEvent e) {
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_SPOT_RADIUS_RATIO, (float) jTextFieldSpotRadius.getValue());
								view.refresh();
							}							
						}
						@Override
						public void focusGained(FocusEvent e) {}
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
						public void actionPerformed(ActionEvent e) {
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_DISPLAY_SPOT_NAMES, jCheckBoxDisplayNames.isSelected());
								view.refresh();
							}
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
				jButtonShowTrackScheme.setBounds(10, 411, 120, 30);
				jButtonShowTrackScheme.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						fireAction(TRACK_SCHEME_BUTTON_PRESSED);
					}
				});
				this.add(jButtonShowTrackScheme);
			}
			{
				jButtonDoAnalysis = new JButton("Analysis");
				jButtonDoAnalysis.setFont(FONT);
				jButtonDoAnalysis.setIcon(DO_ANALYSIS_ICON);
				jButtonDoAnalysis.setBounds(145, 411, 120, 30);
				jButtonDoAnalysis.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						fireAction(DO_ANALYSIS_BUTTON_PRESSED);
					}
				});
				this.add(jButtonDoAnalysis);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
