package fiji.plugin.trackmate.gui;

import ij.gui.ImageWindow;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * A view for the TrackMate_ plugin, strongly inspired from the spots segmentation GUI of the Imaris® software 
 * from Bitplane ({@link http://www.bitplane.com/}).
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - September 2010 - 2011
 */
public class TrackMateFrame extends javax.swing.JFrame implements ActionListener {

	/*
	 * DEFAULT VISIBILITY & PUBLIC CONSTANTS
	 */

	public static final Font FONT = new Font("Arial", Font.PLAIN, 10);
	public static final Font BIG_FONT = new Font("Arial", Font.PLAIN, 14);
	public static final Font SMALL_FONT = FONT.deriveFont(8);
	static final Dimension TEXTFIELD_DIMENSION = new Dimension(40,18);

	/*
	 * PRIVATE CONSTANTS
	 */

	private static final long serialVersionUID = -4092131926852771798L;
	private static final Icon NEXT_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/arrow_right.png"));
	private static final Icon PREVIOUS_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/arrow_left.png"));
	private static final Icon LOAD_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/page_go.png"));
	private static final Icon SAVE_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/page_save.png"));

	/*
	 * DEFAULT VISIBILITY FIELDS
	 */

	/** This {@link ActionEvent} is fired when the 'next' button is pressed. */
	final ActionEvent NEXT_BUTTON_PRESSED = new ActionEvent(this, 0, "NextButtonPressed");
	/** This {@link ActionEvent} is fired when the 'previous' button is pressed. */
	final ActionEvent PREVIOUS_BUTTON_PRESSED = new ActionEvent(this, 1, "PreviousButtonPressed");
	/** This {@link ActionEvent} is fired when the 'load' button is pressed. */
	final ActionEvent LOAD_BUTTON_PRESSED = new ActionEvent(this, 2, "LoadButtonPressed");
	/** This {@link ActionEvent} is fired when the 'save' button is pressed. */
	final ActionEvent SAVE_BUTTON_PRESSED = new ActionEvent(this, 3, "SaveButtonPressed");

	JButton jButtonSave;
	JButton jButtonLoad;
	JButton jButtonPrevious;
	JButton jButtonNext;

	StartDialogPanel startDialogPanel;
	SegmenterConfigurationPanel segmenterSettingsPanel;
	InitFilterPanel initThresholdingPanel;
	@SuppressWarnings("rawtypes")
	ListChooserPanel<SpotSegmenter> segmenterChoicePanel;
	ListChooserPanel<TrackMateModelView> displayerChooserPanel;
	ListChooserPanel<SpotTracker> trackerChoicePanel;
	FilterGuiPanel spotFilterGuiPanel;
	FilterGuiPanel trackFilterGuiPanel;
	TrackerSettingsPanel trackerSettingsPanel;
	DisplayerPanel displayerPanel;

	/*
	 * FIELDS
	 */

	private TrackMateModel model;
	private TrackMate_ plugin;
	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

	private JPanel jPanelButtons;
	private JPanel jPanelMain;
	private LogPanel logPanel;
	private CardLayout cardLayout;
	private ActionChooserPanel actionPanel;

	/*
	 * ENUM
	 */

	public enum PanelCard {
		START_DIALOG_KEY,
		SEGMENTER_CHOICE_KEY,
		TUNE_SEGMENTER_KEY,
		INITIAL_THRESHOLDING_KEY,
		DISPLAYER_CHOICE_KEY,
		SPOT_FILTER_GUI_KEY,
		TRACKER_CHOICE_KEY,
		TUNE_TRACKER_KEY,
		LOG_PANEL_KEY,
		TRACK_FILTER_GUI_KEY,
		DISPLAYER_PANEL_KEY, 
		ACTION_PANEL_KEY;
	}

	/*
	 * CONSTRUCTOR
	 */

	public TrackMateFrame(TrackMate_ plugin) {
		this.model = plugin.getModel();
		this.plugin = plugin;
		initGUI();
		positionWindow();

	}

	/*
	 * PUBLIC METHODS
	 */

	public void setModel(TrackMateModel model) {
		this.model = model;
		model.setLogger(getLogger());
	}


	/**
	 * Display the panel whose key is given. If needed, instantiate it or update it by getting 
	 * required parameters from the model this view represent.
	 */
	@SuppressWarnings("rawtypes")
	public void displayPanel(PanelCard key) {

		if (key == PanelCard.LOG_PANEL_KEY) {
			cardLayout.show(jPanelMain, PanelCard.LOG_PANEL_KEY.name());
			return;
		}

		ActionListenablePanel panel = null;
		switch (key) {

		case START_DIALOG_KEY:
			startDialogPanel = new StartDialogPanel(model.getSettings(), jButtonNext);
			panel = startDialogPanel;
			break;

		case SEGMENTER_CHOICE_KEY:
			if (null == segmenterChoicePanel)
				segmenterChoicePanel = new ListChooserPanel<SpotSegmenter>(plugin.getAvailableSpotSegmenters(), "segmenter");
			panel = segmenterChoicePanel;
			break;

		case TUNE_SEGMENTER_KEY: {
			if (null != segmenterSettingsPanel)
				jPanelMain.remove(segmenterSettingsPanel);
			Settings settings = model.getSettings();
			segmenterSettingsPanel = settings.segmenterSettings.createConfigurationPanel();
			segmenterSettingsPanel.setSegmenterSettings(model);
			panel = segmenterSettingsPanel;
			break;
		}

		case INITIAL_THRESHOLDING_KEY:
			if (null != initThresholdingPanel)
				jPanelMain.remove(initThresholdingPanel);
			initThresholdingPanel = new InitFilterPanel(model.getFeatureModel().getSpotFeatureValues(), model.getInitialSpotFilterValue());
			panel = initThresholdingPanel;
			break;

		case DISPLAYER_CHOICE_KEY:
			if (null != displayerChooserPanel)
				jPanelMain.remove(displayerChooserPanel);
			displayerChooserPanel = new ListChooserPanel<TrackMateModelView>(plugin.getAvailableTrackMateModelViews(), "displayer");
			panel = displayerChooserPanel;
			break;

		case SPOT_FILTER_GUI_KEY:
			if (null != spotFilterGuiPanel) 
				jPanelMain.remove(spotFilterGuiPanel);
			spotFilterGuiPanel = new  FilterGuiPanel(model.getFeatureModel().getSpotFeatures(), model.getSpotFilters(),  
					model.getFeatureModel().getSpotFeatureNames(), model.getFeatureModel().getSpotFeatureValues(), "spots"); 
			panel = spotFilterGuiPanel;
			break;

		case TRACKER_CHOICE_KEY:
			if (null == trackerChoicePanel)
				trackerChoicePanel = new ListChooserPanel<SpotTracker>(plugin.getAvailableSpotTrackers(), "tracker");
			panel = trackerChoicePanel;
			break;

		case TUNE_TRACKER_KEY:
			if (null != trackerSettingsPanel)
				jPanelMain.remove(trackerSettingsPanel);
			trackerSettingsPanel = model.getSettings().trackerSettings.createConfigurationPanel();
			trackerSettingsPanel.setTrackerSettings(model);
			panel = trackerSettingsPanel;
			break;

		case TRACK_FILTER_GUI_KEY:
			if (null != trackFilterGuiPanel) 
				jPanelMain.remove(trackFilterGuiPanel);
			trackFilterGuiPanel = new FilterGuiPanel(model.getFeatureModel().getTrackFeatures(), model.getTrackFilters(),
					model.getFeatureModel().getTrackFeatureNames(), model.getFeatureModel().getTrackFeatureValues(), "tracks");
			panel = trackFilterGuiPanel;
			break;

		case DISPLAYER_PANEL_KEY:
			if (null == displayerPanel) {
				displayerPanel = new DisplayerPanel(model);
				displayerPanel.addActionListener(this);
			}
			panel = displayerPanel;
			break;

		case ACTION_PANEL_KEY:
			if (null == actionPanel)
				actionPanel = new ActionChooserPanel(model, this, plugin);
			panel = actionPanel;
			break;
		}

		jPanelMain.add(panel, key.name());
		cardLayout.show(jPanelMain, key.name());
	}

	/** 
	 * Add an {@link ActionListener} to the list of listeners of this GUI, that will be notified 
	 * when one the of push buttons is pressed.
	 */
	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}

	/** 
	 * Remove an {@link ActionListener} from the list of listeners of this GUI.
	 * @return  true if the listener was present in the list for this GUI and was sucessfully removed from it.
	 */
	public boolean removeActionListener(ActionListener listener) {
		return listeners.remove(listener);
	}

	/** 
	 * Return a {@link Logger} suitable for use with this view.
	 */
	public Logger getLogger() {
		return logPanel.getLogger();
	}


	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Try to position the GUI cleverly...
	 */
	private void positionWindow() {
		if (null != model.getSettings().imp && model.getSettings().imp.getWindow() != null) {
			
			// Get total size of all screens
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gs = ge.getScreenDevices();
			int screenWidth = 0;
			for (int i=0; i<gs.length; i++) {
			    DisplayMode dm = gs[i].getDisplayMode();
			    screenWidth += dm.getWidth();
			}
			
			ImageWindow window = model.getSettings().imp.getWindow();
			Point windowLoc = window.getLocation();
			Dimension windowSize = window.getSize();
			Dimension guiSize = this.getSize();
			if (guiSize.width > windowLoc.x) {
				if (guiSize.width > screenWidth - (windowLoc.x + windowSize.width)) {
					setLocationRelativeTo(null); // give up
				} else {
					setLocation(windowLoc.x+windowSize.width, windowLoc.y); // put it to the right
				}
			} else {
				setLocation(windowLoc.x-guiSize.width, windowLoc.y); // put it to the left
			}
			
		} else {
			setLocationRelativeTo(null);
		}
	}

	/**
	 * Forward the given {@link ActionEvent} to the listeners of this GUI.
	 */
	private void fireAction(ActionEvent event) {
		synchronized (event) {
			for (ActionListener listener : listeners)
				listener.actionPerformed(event);
		}
	}

	/**
	 * Layout this GUI.
	 */
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			this.setTitle(fiji.plugin.trackmate.TrackMate_.PLUGIN_NAME_STR + " v"+fiji.plugin.trackmate.TrackMate_.PLUGIN_NAME_VERSION);
			this.setResizable(false);
			{
				jPanelMain = new JPanel();
				cardLayout = new CardLayout();
				getContentPane().add(jPanelMain, BorderLayout.CENTER);
				jPanelMain.setLayout(cardLayout);
				jPanelMain.setPreferredSize(new java.awt.Dimension(300, 461));
			}
			{
				jPanelButtons = new JPanel();
				getContentPane().add(jPanelButtons, BorderLayout.SOUTH);
				jPanelButtons.setLayout(null);
				jPanelButtons.setSize(300, 30);
				jPanelButtons.setPreferredSize(new java.awt.Dimension(300, 30));
				{
					jButtonNext = new JButton();
					jPanelButtons.add(jButtonNext);
					jButtonNext.setText("Next");
					jButtonNext.setIcon(NEXT_ICON);
					jButtonNext.setFont(FONT);
					jButtonNext.setBounds(216, 2, 76, 25);
					jButtonNext.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(NEXT_BUTTON_PRESSED);
						}
					});
				}
				{
					jButtonPrevious = new JButton();
					jPanelButtons.add(jButtonPrevious);
					jButtonPrevious.setIcon(PREVIOUS_ICON);
					jButtonPrevious.setFont(FONT);
					jButtonPrevious.setBounds(177, 2, 40, 25);
					jButtonPrevious.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(PREVIOUS_BUTTON_PRESSED);
						}
					});
				}
				{
					jButtonLoad = new JButton();
					jPanelButtons.add(jButtonLoad);
					jButtonLoad.setText("Load");
					jButtonLoad.setIcon(LOAD_ICON);
					jButtonLoad.setFont(FONT);
					jButtonLoad.setBounds(7, 2, 78, 25);
					jButtonLoad.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(LOAD_BUTTON_PRESSED);
						}
					});
				}
				{
					jButtonSave = new JButton();
					jPanelButtons.add(jButtonSave);
					jButtonSave.setText("Save");
					jButtonSave.setIcon(SAVE_ICON);
					jButtonSave.setFont(FONT);
					jButtonSave.setBounds(86, 2, 78, 25);
					jButtonSave.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(SAVE_BUTTON_PRESSED);
						}
					});
				}
			}
			pack();
			this.setSize(300, 520);
			// Only instantiate the logger panel, the rest will be done by the controller
			{
				logPanel = new LogPanel();
				jPanelMain.add(logPanel, PanelCard.LOG_PANEL_KEY.name());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		repaint();
		validate();
	}

	/**
	 * Return the GUI panel associated with the given {@link PanelCard}. 
	 * Warning: Some panels may not be instantiated (yet) at the time when this method is called
	 */
	public ActionListenablePanel getPanelFor(PanelCard card) {
		switch(card) {

		case ACTION_PANEL_KEY:
			return displayerPanel;
		case DISPLAYER_CHOICE_KEY:
			return displayerChooserPanel;
		case DISPLAYER_PANEL_KEY:
			return displayerPanel;
		case INITIAL_THRESHOLDING_KEY:
			return initThresholdingPanel;
		case LOG_PANEL_KEY:
			return logPanel;
		case SEGMENTER_CHOICE_KEY:
			return segmenterChoicePanel;
		case START_DIALOG_KEY:
			return startDialogPanel;
		case SPOT_FILTER_GUI_KEY:
			return spotFilterGuiPanel;
		case TRACKER_CHOICE_KEY:
			return trackerChoicePanel;
		case TUNE_SEGMENTER_KEY:
			return segmenterSettingsPanel;
		case TUNE_TRACKER_KEY:
			return trackerSettingsPanel;
		default:
			return null;
		}

	}

	/** 
	 * Simply forward the caught event to listeners of this main frame.
	 */
	@Override
	public void actionPerformed(final ActionEvent event) {
		fireAction(event);
	}
}