package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.TrackMate_;



/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class TrackMateFrame extends javax.swing.JFrame {

	/*
	 * DEFAULT VISIBILITY CONSTANTS
	 */
	
	static final Font FONT = new Font("Arial", Font.PLAIN, 10);
	static final Font SMALL_FONT = FONT.deriveFont(8);
	static final Dimension TEXTFIELD_DIMENSION = new Dimension(40,18);
	
	public enum PanelCard {
		START_DIALOG_KEY,
		TUNE_SEGMENTER_KEY,
		INITIAL_THRESHOLDING_KEY,
		THRESHOLD_GUI_KEY,
		TUNE_TRACKER_KEY,
		LOG_PANEL_KEY,
		DISPLAYER_PANEL_KEY;
	}
	
	
	
	/*
	 * CONSTANTS
	 */
	
	private static final Icon NEXT_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/arrow_right.png"));
	private static final Icon PREVIOUS_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/arrow_left.png"));
	private static final Icon LOAD_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/page_go.png"));
	private static final Icon SAVE_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/page_save.png"));

	/*
	 * PUBLIC FIELDS
	 */
	
	/** This {@link ActionEvent} is fired when the 'next' button is pressed. */
	public final ActionEvent NEXT_BUTTON_PRESSED = new ActionEvent(this, 0, "NextButtonPressed");
	/** This {@link ActionEvent} is fired when the 'previous' button is pressed. */
	public final ActionEvent PREVIOUS_BUTTON_PRESSED = new ActionEvent(this, 1, "PreviousButtonPressed");
	/** This {@link ActionEvent} is fired when the 'load' button is pressed. */
	public final ActionEvent LOAD_BUTTON_PRESSED = new ActionEvent(this, 2, "LoadButtonPressed");
	/** This {@link ActionEvent} is fired when the 'save' button is pressed. */
	public final ActionEvent SAVE_BUTTON_PRESSED = new ActionEvent(this, 3, "SaveButtonPressed");
	
	/*
	 * FIELDS
	 */
	
	private StartDialogPanel startDialogPanel;
	private LogPanel logPanel;
	private DisplayerPanel displayerPanel;
	private CardLayout cardLayout;
	JButton jButtonSave;
	JButton jButtonLoad;
	JButton jButtonPrevious;
	JButton jButtonNext;
	private JPanel jPanelButtons;
	private JPanel jPanelMain;
	private SegmenterSettingsPanel segmenterSettingsPanel;
	private TrackerSettingsPanel trackerSettingsPanel;
	ThresholdGuiPanel thresholdGuiPanel;
	InitThresholdPanel initThresholdingPanel;
	
	private TrackMateFrameController controller;
	private TrackMateModelInterface model;
	
	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
	
	
	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * CONSTRUCTOR
	 */
	
	public TrackMateFrame(TrackMateFrameController controller, TrackMateModelInterface model) {
		this.controller = controller;
		this.model = model;
		initGUI();
		
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Display the panel whose key is given. If needed, instantiate it or update it by getting 
	 * required parameters from the model this view represent.
	 */
	public void displayPanel(PanelCard key) {
		ActionListenablePanel panel = null;
		
		switch (key) {
		
		case LOG_PANEL_KEY:
			panel = logPanel;
			break;
		
		case START_DIALOG_KEY:
			if (null != startDialogPanel)
				jPanelButtons.remove(startDialogPanel);
			startDialogPanel = new StartDialogPanel(model.getSettings());
			panel = startDialogPanel;
			break;
			
		case TUNE_SEGMENTER_KEY:
			if (null != segmenterSettingsPanel)
				jPanelMain.remove(segmenterSettingsPanel);
			segmenterSettingsPanel = model.getSettings().createSegmenterSettingsPanel();
			panel = segmenterSettingsPanel;
			break;
			
		case INITIAL_THRESHOLDING_KEY:
			if (null != initThresholdingPanel)
				jPanelMain.remove(initThresholdingPanel);
			initThresholdingPanel = new InitThresholdPanel(model.getFeatureValues());
			panel = initThresholdingPanel;
			break;
			
		case THRESHOLD_GUI_KEY:
			if (null != thresholdGuiPanel) 
				jPanelMain.remove(thresholdGuiPanel);
			thresholdGuiPanel = new ThresholdGuiPanel(model.getFeatureValues());
			panel = thresholdGuiPanel;
			break;
			
		case TUNE_TRACKER_KEY:
			if (null != trackerSettingsPanel)
				jPanelMain.remove(trackerSettingsPanel);
			trackerSettingsPanel = model.getSettings().createTrackerSettingsPanel();
			panel = trackerSettingsPanel;
			break;
			
		case DISPLAYER_PANEL_KEY:
			if (null != displayerPanel)
				jPanelMain.remove(displayerPanel);
			displayerPanel = new DisplayerPanel(model.getFeatureValues());
			panel = displayerPanel;
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
	
	/**
	 * Layout this GUI.
	 */
	public void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			this.setTitle(TrackMate_.PLUGIN_NAME_STR + " v"+TrackMate_.PLUGIN_NAME_VERSION);
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
					jButtonNext.setBounds(216, 3, 76, 25);
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
					jButtonPrevious.setBounds(177, 3, 40, 25);
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
					jButtonLoad.setBounds(0, 2, 76, 25);
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
					jButtonSave.setBounds(75, 2, 78, 25);
					jButtonSave.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(SAVE_BUTTON_PRESSED);
						}
					});
				}
			}
			pack();
			this.setSize(300, 520);
			// Only instantiate the logger panel, the reset will be done by the controller
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
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Forward the given {@link ActionEvent} to the listeners of this GUI.
	 */
	private void fireAction(ActionEvent event) {
		for (ActionListener listener : listeners)
			listener.actionPerformed(event);
	}


	/*
	 * MAIN METHODS
	 */

	public static <T extends RealType<T>> void main(String[] args) {
		ij.ImageJ.main(args);
		TrackMateModelInterface model = new TrackMate_<T>();
		TrackMateControllerInterface controller = new TrackMateFrameController(model);
	}
	
}
