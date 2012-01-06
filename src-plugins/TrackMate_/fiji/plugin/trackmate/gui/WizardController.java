package fiji.plugin.trackmate.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class WizardController implements ActionListener {

	/*
	 * FIELDS
	 */

	private static final boolean DEBUG = false;
	private Logger logger;
	private File file;
	/** The plugin piloted here. */
	private TrackMate_ plugin;
	/** The GUI controlled by this controller.  */
	private TrackMateWizard wizard;

	/**
	 * Is used to determine how to react to a 'next' button push. If it is set to true, then we are
	 * normally processing through the GUI, and pressing 'next' should update the GUI and process the
	 * data. If is is set to false, then we are currently loading/saving the data, and we should simply
	 * re-generate the data.
	 */
	boolean actionFlag = true;
	
	/*
	 * CONSTRUCTOR
	 */

	public WizardController(final TrackMate_ plugin) {
		this.plugin = plugin;
		this.wizard = new TrackMateWizard(plugin);
		this.logger = wizard.getLogger();

		plugin.setLogger(logger);
		wizard.setVisible(true);
		wizard.addActionListener(this);
	}

	/*
	 * PROTECTED METHODS
	 */
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the list of {@link WizardPanelDescriptor}s that will be displayed in this GUI.
	 * By extending this method to return another list, you can add, remove or change 
	 * some panels. 
	 */
	protected List<WizardPanelDescriptor> createWizardPanelDescriptorList() {
		
	}
	
	/*
	 * ACTION LISTENER
	 */

	@Override
	public void actionPerformed(ActionEvent event) {
		if (DEBUG)
			System.out.println("[TrackMateFrameController] Caught event "+event);
		final DisplayerPanel displayerPanel = (DisplayerPanel) wizard.getPanelDescriptorFor(DisplayerPanel.DESCRIPTOR);

		if (event == wizard.NEXT_BUTTON_PRESSED && actionFlag) {

			WizardPanelDescriptor descriptor = wizard.getCurrentPanelDescriptor();
			Object nextPanelDescriptor = descriptor.getNextPanelDescriptor();
			wizard.setCurrentPanel(nextPanelDescriptor);

		} else if (event == wizard.PREVIOUS_BUTTON_PRESSED && actionFlag) {

			WizardPanelDescriptor descriptor = wizard.getCurrentPanelDescriptor();
			Object backPanelDescriptor = descriptor.getBackPanelDescriptor();        
			wizard.setCurrentPanel(backPanelDescriptor);

		} else if (event == wizard.LOAD_BUTTON_PRESSED && actionFlag) {

			load();

		} else if (event == wizard.SAVE_BUTTON_PRESSED && actionFlag) {

			save();

		} else if ((event == wizard.NEXT_BUTTON_PRESSED || 
				event == wizard.PREVIOUS_BUTTON_PRESSED || 
				event == wizard.LOAD_BUTTON_PRESSED ||
				event == wizard.SAVE_BUTTON_PRESSED) && !actionFlag ) {

			actionFlag = true;

		} else if (event == displayerPanel.TRACK_SCHEME_BUTTON_PRESSED) {

			// Display Track scheme
			displayerPanel.jButtonShowTrackScheme.setEnabled(false);
			new Thread("TrackScheme launching thread") {
				public void run() {
					try {
						TrackSchemeFrame trackScheme = new TrackSchemeFrame(plugin.getModel());
						trackScheme.setVisible(true);
					} finally {
						displayerPanel.jButtonShowTrackScheme.setEnabled(true);
					}
				}
			}.start();


		}
	}


	private void load() {
		try {

			actionFlag = false;
//			setMainButtonsFor(null);
			wizard.displayPanel(PanelCard.LOG_PANEL_KEY);

			// New model to feed
			TrackMateModel newModel = new TrackMateModel();
			newModel.setLogger(logger);

			if (null == file) {
				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				try {
					file = new File(folder.getPath() + File.separator + plugin.getModel().getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}

			GuiReader reader = new GuiReader(this);
			File tmpFile = reader.askForFile(file);
			if (null == tmpFile) {
//				setMainButtonsFor(GuiState.LOAD_SAVE);
				return;
			}
			file = tmpFile;
			plugin = new TrackMate_(reader.loadFile(file));
			plugin.computeTrackFeatures();

		} finally {
//			setMainButtonsFor(GuiState.LOAD_SAVE);
		}
	}

	private void save() {
		try {

//			setMainButtonsFor(null);
			wizard.displayPanel(PanelCard.LOG_PANEL_KEY);

			logger.log("Saving data...\n", Logger.BLUE_COLOR);
			if (null == file ) {
				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				try {
					file = new File(folder.getPath() + File.separator + plugin.getModel().getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}

			plugin.computeTrackFeatures();
			GuiSaver saver = new GuiSaver(this);
			File tmpFile = saver.askForFile(file);
			if (null == tmpFile) {
				actionFlag = false;
//				setMainButtonsFor(GuiState.LOAD_SAVE);
				return;
			}
			file = tmpFile;
			saver.writeFile(file, state);

		}	finally {

			actionFlag = false;
//			setMainButtonsFor(GuiState.LOAD_SAVE);

		}
	}

}