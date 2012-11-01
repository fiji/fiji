package fiji.plugin.trackmate.gui;

import ij.IJ;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class WizardController implements ActionListener {

	/*
	 * FIELDS
	 */

	private static final boolean DEBUG = false;
	protected Logger logger;
	/** The plugin piloted here. */
	protected TrackMate_ plugin;
	/** The GUI controlled by this controller.  */
	protected TrackMateWizard wizard;

	/**
	 * Is used to determine how to react to a 'next' button push. If it is set to true, then we are
	 * normally processing through the GUI, and pressing 'next' should update the GUI and process the
	 * data. If is is set to false, then we are currently loading/saving the data, and we should simply
	 * re-generate the data.
	 */
	boolean actionFlag = true;
	private boolean oldNextButtonState;
	private boolean oldPreviousButtonState;

	/*
	 * CONSTRUCTOR
	 */

	public WizardController(final TrackMate_ plugin) {

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

		this.plugin = plugin;
		Component window;
		if (plugin.getModel().getSettings().imp != null && plugin.getModel().getSettings().imp.getWindow() != null ) {
			window = plugin.getModel().getSettings().imp.getWindow();
		} else {
			window = null;
		}
		this.wizard = new TrackMateWizard(window);
		this.logger = wizard.getLogger();

		plugin.setLogger(logger);
		wizard.setVisible(true);
		wizard.addActionListener(this);

		// Instantiate and pass panel descriptors to the wizard
		List<WizardPanelDescriptor> descriptors = createWizardPanelDescriptorList();
		for(WizardPanelDescriptor descriptor : descriptors) {
			descriptor.setPlugin(plugin);
			descriptor.setWizard(wizard);
			wizard.registerWizardDescriptor(descriptor.getDescriptorID(), descriptor);
		}

		init();
	}

	/*
	 * PUBLIC METHODS 
	 */

	/**
	 * Expose the {@link TrackMateWizard} instance controlled here.
	 */
	public TrackMateWizard getWizard() {
		return wizard;
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Display the first panel
	 */
	protected void init() {
		// We need to listen to events happening on the DisplayerPanel
		DisplayerPanel displayerPanel = (DisplayerPanel) wizard.getPanelDescriptorFor(DisplayerPanel.DESCRIPTOR);
		displayerPanel.addActionListener(this);

		// Get start panel id
		wizard.setPreviousButtonEnabled(false);
		String id = StartDialogPanel.DESCRIPTOR;
		WizardPanelDescriptor panelDescriptor  = wizard.getPanelDescriptorFor(id);

		// Execute about to be displayed action of the new one
		panelDescriptor.aboutToDisplayPanel();

		// Display matching panel
		wizard.showDescriptorPanelFor(id);

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
		descriptors.add(new SegmenterChoiceDescriptor());
		//		descriptors.add(new SegmenterConfigurationPanelDescriptor()); // will be instantiated on the fly, see SegmenterChoiceDescriptor
		descriptors.add(new SegmentationDescriptor());
		descriptors.add(new InitFilterPanel());
		descriptors.add(new DisplayerChoiceDescriptor());
		descriptors.add(new LaunchDisplayerDescriptor());
		descriptors.add(new SpotFilterDescriptor());
		descriptors.add(new TrackerChoiceDescriptor());
		//		descriptors.add(new TrackerConfigurationPanelDescriptor());  // will be instantiated on the fly, see TrackerChoiceDescriptor
		descriptors.add(new TrackingDescriptor());
		descriptors.add(new TrackFilterDescriptor());
		descriptors.add(new DisplayerPanel());
		descriptors.add(new ActionChooserPanel(plugin));

		descriptors.add(new LoadDescriptor());
		descriptors.add(new SaveDescriptor());
		return descriptors;
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

			new Thread("TrackMate moving to next step thread.") {
				public void run() {
					next();
				};
			}.start();


		} else if (event == wizard.PREVIOUS_BUTTON_PRESSED && actionFlag) {

			new Thread("TrackMate moving to previous step thread.") {
				public void run() {
					previous();
				};
			}.start();

		} else if (event == wizard.LOAD_BUTTON_PRESSED && actionFlag) {

			actionFlag = false;
			wizard.jButtonNext.setText("Resume");
			wizard.setSaveButtonEnabled(false);
			wizard.setLoadButtonEnabled(false);
			wizard.setPreviousButtonEnabled(false);
			wizard.setNextButtonEnabled(false);

			new Thread("TrackMate moving to load state thread.") {
				public void run() {
					load();
				};
			}.start();

		} else if (event == wizard.SAVE_BUTTON_PRESSED && actionFlag) {

			actionFlag = false;
			oldNextButtonState = wizard.jButtonNext.isEnabled();
			oldPreviousButtonState = wizard.jButtonPrevious.isEnabled();
			wizard.jButtonNext.setText("Resume");
			wizard.setSaveButtonEnabled(false);
			wizard.setLoadButtonEnabled(false);
			wizard.setPreviousButtonEnabled(false);
			wizard.setNextButtonEnabled(false);

			new Thread("TrackMate moving to save state thread.") {
				public void run() {
					save();
				};
			}.start();

		} else if ((event == wizard.NEXT_BUTTON_PRESSED || 
				event == wizard.PREVIOUS_BUTTON_PRESSED || 
				event == wizard.LOAD_BUTTON_PRESSED ||
				event == wizard.SAVE_BUTTON_PRESSED) && !actionFlag ) {

			// Display old panel, but do not execute its actions
			actionFlag = true;
			String id = wizard.getCurrentPanelDescriptor().getNextDescriptorID();
			wizard.showDescriptorPanelFor(id);

			// Put back buttons
			wizard.jButtonNext.setText("Next");
			wizard.setLoadButtonEnabled(true);
			wizard.setSaveButtonEnabled(true);
			wizard.setNextButtonEnabled(oldNextButtonState);
			wizard.setPreviousButtonEnabled(oldPreviousButtonState);

		} else if (event == displayerPanel.TRACK_SCHEME_BUTTON_PRESSED) {

			// Display Track scheme
			displayerPanel.jButtonShowTrackScheme.setEnabled(false);

			try {
				TrackSchemeFrame trackScheme = new TrackSchemeFrame(plugin.getModel());
				trackScheme.setVisible(true);
			} finally {
				displayerPanel.jButtonShowTrackScheme.setEnabled(true);
			}

		}
	}


	private void next() {

		wizard.setNextButtonEnabled(false);
		try {

			// Execute leave action of the old panel
			WizardPanelDescriptor oldDescriptor = wizard.getCurrentPanelDescriptor();
			if (oldDescriptor != null) {
				oldDescriptor.aboutToHidePanel();
			}

			String id = oldDescriptor.getNextDescriptorID();
			WizardPanelDescriptor panelDescriptor  = wizard.getPanelDescriptorFor(id);

			// Check if the new panel has a next panel. If not, disable the next button
			if (null == panelDescriptor.getNextDescriptorID()) {
				wizard.setNextButtonEnabled(false);
			}

			// Re-enable the previous button, in case it was disabled
			wizard.setPreviousButtonEnabled(true);

			// Execute about to be displayed action of the new one
			panelDescriptor.aboutToDisplayPanel();

			// Display matching panel
			wizard.showDescriptorPanelFor(id);

			//  Show the panel in the dialog, and execute action after display
			panelDescriptor.displayingPanel();

		} finally {
			
			
		}

	}

	private void previous() {
		// Move to previous panel, but do not execute its actions
		WizardPanelDescriptor descriptor = wizard.getCurrentPanelDescriptor();
		String backPanelDescriptor = descriptor.getPreviousDescriptorID();        
		wizard.showDescriptorPanelFor(backPanelDescriptor);

		// Check if the new panel has a next panel. If not, disable the next button
		WizardPanelDescriptor previousPanel = wizard.getPanelDescriptorFor(backPanelDescriptor);
		if (null == previousPanel.getPreviousDescriptorID()) {
			wizard.setPreviousButtonEnabled(false);
		}

		// Re-enable the previous button, in case it was disabled
		wizard.setNextButtonEnabled(true);
	}

	private void load() {
		// Store current state
		WizardPanelDescriptor oldDescriptor = wizard.getCurrentPanelDescriptor();

		// Move to load state and show log panel
		final LoadDescriptor loadDescriptor = (LoadDescriptor) wizard.getPanelDescriptorFor(LoadDescriptor.DESCRIPTOR);
		loadDescriptor.setTargetNextID(oldDescriptor.getDescriptorID());
		loadDescriptor.aboutToDisplayPanel();
		wizard.showDescriptorPanelFor(LoadDescriptor.DESCRIPTOR);

		// Instantiate GuiReader, ask for file, and load it in memory
		loadDescriptor.displayingPanel();

		// Get the loaded data back
		plugin = loadDescriptor.plugin;

		// Enable or disable next and previous button depending on the target descriptor.
		WizardPanelDescriptor nextPanel = wizard.getPanelDescriptorFor(loadDescriptor.getNextDescriptorID());
		if (nextPanel.getNextDescriptorID() == null) {
			oldNextButtonState = false;
		} else {
			oldNextButtonState = true;
		}
		if (nextPanel.getPreviousDescriptorID() == null) {
			oldPreviousButtonState = false;
		} else {
			oldPreviousButtonState = true;
		}

	}

	private void save() {
		// Store current state
		WizardPanelDescriptor oldDescriptor = wizard.getCurrentPanelDescriptor();

		// Move to save state and execute
		SaveDescriptor saveDescriptor = (SaveDescriptor) wizard.getPanelDescriptorFor(SaveDescriptor.DESCRIPTOR);
		saveDescriptor.setTargetNextID(oldDescriptor.getDescriptorID());
		saveDescriptor.aboutToDisplayPanel();
		wizard.showDescriptorPanelFor(SaveDescriptor.DESCRIPTOR);
		saveDescriptor.displayingPanel();
	}

}