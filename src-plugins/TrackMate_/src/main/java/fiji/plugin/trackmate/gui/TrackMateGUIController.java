package fiji.plugin.trackmate.gui;

import ij.IJ;
import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.util.TMUtils;

public class TrackMateGUIController implements ActionListener {
	
	/*
	 * FIELDS
	 */

	private static final boolean DEBUG = false;
	private final Logger logger;
	/** The trackmate piloted here. */
	private final TrackMate trackmate;
	/** The GUI controlled by this controller.  */
	private final TrackMateWizard gui;
	private final TrackMateGUIModel guimodel;


	/*
	 * CONSTRUCTOR
	 */

	public TrackMateGUIController(final TrackMate trackmate, ImagePlus imp) {

		// I can't stand the metal look. If this is a problem, contact me (jeanyves.tinevez@gmail.com)
		if (IJ.isMacOSX() || IJ.isWindows()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		}
		
		this.guimodel = new TrackMateGUIModel();
		this.gui = new TrackMateWizard(this);
		this.logger = gui.getLogger();
		this.trackmate = trackmate;

		trackmate.getModel().setLogger(logger);
		gui.setVisible(true);
		GuiUtils.positionWindow(gui, imp.getWindow());
		gui.addActionListener(this);

		init();
	}

	/*
	 * PUBLIC METHODS 
	 */

	/**
	 * Expose the {@link TrackMateWizard} instance controlled here.
	 */
	public TrackMateWizard getWizard() {
		return gui;
	}

	/**
	 * Expose the {@link TrackMate} trackmate piloted by the wizard.
	 */
	public TrackMate getPlugin() {
		return trackmate;
	}



	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Display the first panel
	 */
	protected void init() {
		// We need to listen to events happening on the DisplayerPanel
		DisplayerPanel displayerPanel = (DisplayerPanel) gui.getPanelDescriptorFor(DisplayerPanel.DESCRIPTOR);
		displayerPanel.addActionListener(this);

		// Get start panel id
		gui.setPreviousButtonEnabled(false);
		String id = StartDialogPanel.DESCRIPTOR;
		WizardPanelDescriptor panelDescriptor  = gui.getPanelDescriptorFor(id);
		
		String welcomeMessage = TrackMate.PLUGIN_NAME_STR + "v" + TrackMate.PLUGIN_NAME_VERSION + " started on:\n" + 
				TMUtils.getCurrentTimeString() + '\n';
		// Log GUI processing start
		gui.getLogger().log(welcomeMessage, Logger.BLUE_COLOR);

		// Execute about to be displayed action of the new one
		panelDescriptor.aboutToDisplayPanel();

		// Display matching panel
		gui.showDescriptorPanelFor(id);

		//  Show the panel in the dialog, and execute action after display
		panelDescriptor.displayingPanel();  
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the list of {@link WizardPanelDescriptor}s that will be displayed in this GUI.
	 * By extending this method to return another list, you can add, remove or change 
	 * some panels. 
	 */
	protected List<WizardPanelDescriptor> createWizardPanelDescriptorList() {
		List<WizardPanelDescriptor> descriptors = new ArrayList<WizardPanelDescriptor>(14);
		
		
		descriptors.add(new StartDialogPanel());
		descriptors.add(new DetectorChoiceDescriptor());
		//		descriptors.add(new DetectorConfigurationPanelDescriptor()); // will be instantiated on the fly, see DetectorChoiceDescriptor
		descriptors.add(new DetectorDescriptor());
		descriptors.add(new InitFilterDescriptor());
		descriptors.add(new DisplayerChoiceDescriptor());
		descriptors.add(new LaunchDisplayerDescriptor());
		descriptors.add(new SpotFilterDescriptor());
		descriptors.add(new TrackerChoiceDescriptor());
		//		descriptors.add(new TrackerConfigurationPanelDescriptor());  // will be instantiated on the fly, see TrackerChoiceDescriptor
		descriptors.add(new TrackingDescriptor());
		descriptors.add(new TrackFilterDescriptor());
		descriptors.add(new DisplayerPanel());
		descriptors.add(new GrapherPanel());
		descriptors.add(ActionChooserPanel.instantiateForPlugin(trackmate));

		descriptors.add(new LoadDescriptor());
		descriptors.add(new SaveDescriptor());
		
		WizardPanelDescriptor logPanelDescriptor = new LogPanelDescriptor(gui.getLogPanel());
		descriptors.add(logPanelDescriptor);
		return descriptors;
	}

	/*
	 * ACTION LISTENER
	 */

	@Override
	public void actionPerformed(ActionEvent event) {
		if (DEBUG)
			System.out.println("[TrackMateFrameController] Caught event "+event);

		if (event == gui.NEXT_BUTTON_PRESSED && guimodel.actionFlag) {

			new Thread("TrackMate moving to next step thread.") {
				public void run() {
					next();
				};
			}.start();


		} else if (event == gui.PREVIOUS_BUTTON_PRESSED && guimodel.actionFlag) {

			new Thread("TrackMate moving to previous step thread.") {
				public void run() {
					previous();
				};
			}.start();

		} else if (event == gui.LOAD_BUTTON_PRESSED && guimodel.actionFlag) {

			guimodel.actionFlag = false;
			gui.jButtonNext.setText("Resume");
			disableButtonsAndStoreState();

			new Thread("TrackMate moving to load state thread.") {
				public void run() {
					load();
				};
			}.start();

		} else if (event == gui.SAVE_BUTTON_PRESSED && guimodel.actionFlag) {

			guimodel.actionFlag = false;
			gui.jButtonNext.setText("Resume");
			disableButtonsAndStoreState();

			new Thread("TrackMate moving to save state thread.") {
				public void run() {
					save();
				};
			}.start();

		} else if ((event == gui.NEXT_BUTTON_PRESSED || 
				event == gui.PREVIOUS_BUTTON_PRESSED || 
				event == gui.LOAD_BUTTON_PRESSED ||
				event == gui.SAVE_BUTTON_PRESSED) && !guimodel.actionFlag ) {

			// Display old panel, but do not execute its actions
			guimodel.actionFlag = true;
			String id = gui.getCurrentPanelDescriptor().getNextDescriptorID();
			gui.showDescriptorPanelFor(id);

			// Put back buttons
			gui.jButtonNext.setText("Next");
			restoreButtonsState();

		} else if (event == gui.LOG_BUTTON_PRESSED) {
			
			if (guimodel.logButtonState) {
				
				restoreButtonsState();
				gui.showDescriptorPanelFor(guimodel.previousPanelID);
				
			} else {
				
				disableButtonsAndStoreState();
				guimodel.previousPanelID = gui.getCurrentPanelDescriptor().getComponentID();
				gui.showDescriptorPanelFor(LogPanelDescriptor.DESCRIPTOR);
				
			}
			guimodel.logButtonState = !guimodel.logButtonState;
			
		}
	}


	private void next() {

		gui.setNextButtonEnabled(false);
		try {

			// Execute leave action of the old panel
			WizardPanelDescriptor oldDescriptor = gui.getCurrentPanelDescriptor();
			if (oldDescriptor != null) {
				oldDescriptor.aboutToHidePanel();
			}

			String id = oldDescriptor.getNextDescriptorID();
			WizardPanelDescriptor panelDescriptor  = gui.getPanelDescriptorFor(id);

			// Check if the new panel has a next panel. If not, disable the next button
			if (null == panelDescriptor.getNextDescriptorID()) {
				gui.setNextButtonEnabled(false);
			}

			// Re-enable the previous button, in case it was disabled
			gui.setPreviousButtonEnabled(true);

			// Execute about to be displayed action of the new one
			panelDescriptor.aboutToDisplayPanel();

			// Display matching panel
			gui.showDescriptorPanelFor(id);

			//  Show the panel in the dialog, and execute action after display
			panelDescriptor.displayingPanel();

		} finally {


		}

	}

	private void previous() {
		// Move to previous panel, but do not execute its actions
		WizardPanelDescriptor descriptor = gui.getCurrentPanelDescriptor();
		descriptor.aboutToHidePanel();
		String backPanelDescriptor = descriptor.getPreviousDescriptorID();        
		gui.showDescriptorPanelFor(backPanelDescriptor);

		// Check if the new panel has a next panel. If not, disable the next button
		WizardPanelDescriptor previousPanel = gui.getPanelDescriptorFor(backPanelDescriptor);
		if (null == previousPanel.getPreviousDescriptorID()) {
			gui.setPreviousButtonEnabled(false);
		}

		// Re-enable the previous button, in case it was disabled
		gui.setNextButtonEnabled(true);
	}

	private void load() {
		// Store current state
		WizardPanelDescriptor oldDescriptor = gui.getCurrentPanelDescriptor();

		// Move to load state and show log panel
		final LoadDescriptor loadDescriptor = (LoadDescriptor) gui.getPanelDescriptorFor(LoadDescriptor.DESCRIPTOR);
		loadDescriptor.setTargetNextID(oldDescriptor.getDescriptorID());
		loadDescriptor.aboutToDisplayPanel();
		gui.showDescriptorPanelFor(LoadDescriptor.DESCRIPTOR);

		// Instantiate GuiReader, ask for file, and load it in memory
		loadDescriptor.displayingPanel();

		// Enable or disable next and previous button depending on the target descriptor.
		WizardPanelDescriptor nextPanel = gui.getPanelDescriptorFor(loadDescriptor.getNextDescriptorID());
		if (nextPanel.getNextDescriptorID() == null) {
			guimodel.nextButtonState = false;
		} else {
			guimodel.nextButtonState = true;
		}
		if (nextPanel.getPreviousDescriptorID() == null) {
			guimodel.previousButtonState = false;
		} else {
			guimodel.previousButtonState = true;
		}

	}

	private void save() {
		// Store current state
		WizardPanelDescriptor oldDescriptor = gui.getCurrentPanelDescriptor();

		// Move to save state and execute
		SaveDescriptor saveDescriptor = (SaveDescriptor) gui.getPanelDescriptorFor(SaveDescriptor.DESCRIPTOR);
		saveDescriptor.setTargetNextID(oldDescriptor.getDescriptorID());
		saveDescriptor.aboutToDisplayPanel();
		gui.showDescriptorPanelFor(SaveDescriptor.DESCRIPTOR);
		gui.getLogger().log(TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR);
		saveDescriptor.displayingPanel();
	}
	

	/**
	 * Disable the 4 bottom buttons and memorize their state to that they
	 * can be restored when calling {@link #restoreButtonsState()}. 
	 */
	private void disableButtonsAndStoreState() {
		guimodel.loadButtonState 		= gui.jButtonLoad.isEnabled();
		guimodel.saveButtonState 		= gui.jButtonSave.isEnabled();
		guimodel.previousButtonState 	= gui.jButtonPrevious.isEnabled();
		guimodel.nextButtonState 		= gui.jButtonNext.isEnabled();
		gui.setLoadButtonEnabled(false);
		gui.setSaveButtonEnabled(false);
		gui.setPreviousButtonEnabled(false);
		gui.setNextButtonEnabled(false);
	}

	/**
	 * Restore the button state saved when calling {@link #disableButtonsAndStoreState()}.
	 * Do nothing if {@link #disableButtonsAndStoreState()} was not called before. 
	 */
	private void restoreButtonsState() {
		gui.setLoadButtonEnabled(guimodel.loadButtonState);
		gui.setSaveButtonEnabled(guimodel.saveButtonState);
		gui.setPreviousButtonEnabled(guimodel.previousButtonState);
		gui.setNextButtonEnabled(guimodel.nextButtonState);
	}

}