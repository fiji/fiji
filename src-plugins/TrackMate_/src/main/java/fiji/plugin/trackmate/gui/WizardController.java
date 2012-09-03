package fiji.plugin.trackmate.gui;

import ij.IJ;
import ij.ImagePlus;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class WizardController<T extends RealType<T> & NativeType<T>> implements ActionListener {

	/*
	 * FIELDS
	 */

	private static final boolean DEBUG = false;
	protected Logger logger;
	/** The plugin piloted here. */
	protected TrackMate_<T> plugin;
	/** The GUI controlled by this controller.  */
	protected TrackMateWizard<T> wizard;

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

	public WizardController(final TrackMate_<T> plugin) {

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
		Component window = null;
		ImagePlus imp = plugin.getModel().getSettings().imp;
		if (imp != null && imp.getWindow() != null ) {
			window = imp.getWindow();
		}
		this.wizard = new TrackMateWizard<T>(window, this);
		this.logger = wizard.getLogger();

		plugin.setLogger(logger);
		wizard.setVisible(true);
		wizard.addActionListener(this);

		// Instantiate and pass panel descriptors to the wizard
		List<WizardPanelDescriptor<T>> descriptors = createWizardPanelDescriptorList();
		for(WizardPanelDescriptor<T> descriptor : descriptors) {
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
	public TrackMateWizard<T> getWizard() {
		return wizard;
	}
	
	/**
	 * Expose the {@link TrackMate_} plugin piloted by the wizard.
	 */
	public TrackMate_<T> getPlugin() {
		return plugin;
	}

	
	
	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Display the first panel
	 */
	protected void init() {
		// We need to listen to events happening on the DisplayerPanel
		DisplayerPanel<T> displayerPanel = (DisplayerPanel<T>) wizard.getPanelDescriptorFor(DisplayerPanel.DESCRIPTOR);
		displayerPanel.addActionListener(this);

		// Get start panel id
		wizard.setPreviousButtonEnabled(false);
		String id = StartDialogPanel.DESCRIPTOR;
		WizardPanelDescriptor<T> panelDescriptor  = wizard.getPanelDescriptorFor(id);

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
	protected List<WizardPanelDescriptor<T>> createWizardPanelDescriptorList() {
		List<WizardPanelDescriptor<T>> descriptors = new ArrayList<WizardPanelDescriptor<T>>(14);
		descriptors.add(new StartDialogPanel<T>());
		descriptors.add(new DetectorChoiceDescriptor<T>());
		//		descriptors.add(new DetectorConfigurationPanelDescriptor()); // will be instantiated on the fly, see DetectorChoiceDescriptor
		descriptors.add(new DetectorDescriptor<T>());
		descriptors.add(new InitFilterDescriptor<T>());
		descriptors.add(new DisplayerChoiceDescriptor<T>());
		descriptors.add(new LaunchDisplayerDescriptor<T>());
		descriptors.add(new SpotFilterDescriptor<T>());
		descriptors.add(new TrackerChoiceDescriptor<T>());
		//		descriptors.add(new TrackerConfigurationPanelDescriptor());  // will be instantiated on the fly, see TrackerChoiceDescriptor
		descriptors.add(new TrackingDescriptor<T>());
		descriptors.add(new TrackFilterDescriptor<T>());
		descriptors.add(new DisplayerPanel<T>());
		descriptors.add(ActionChooserPanel.instantiateForPlugin(plugin));

		descriptors.add(new LoadDescriptor<T>());
		descriptors.add(new SaveDescriptor<T>());
		return descriptors;
	}

	/*
	 * ACTION LISTENER
	 */

	@Override
	public void actionPerformed(ActionEvent event) {
		if (DEBUG)
			System.out.println("[TrackMateFrameController] Caught event "+event);
		final DisplayerPanel<T> displayerPanel = (DisplayerPanel<T>) wizard.getPanelDescriptorFor(DisplayerPanel.DESCRIPTOR);

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
				TrackSchemeFrame<T> trackScheme = new TrackSchemeFrame<T>(plugin.getModel());
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
			WizardPanelDescriptor<T> oldDescriptor = wizard.getCurrentPanelDescriptor();
			if (oldDescriptor != null) {
				oldDescriptor.aboutToHidePanel();
			}

			String id = oldDescriptor.getNextDescriptorID();
			WizardPanelDescriptor<T> panelDescriptor  = wizard.getPanelDescriptorFor(id);

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
		WizardPanelDescriptor<T> descriptor = wizard.getCurrentPanelDescriptor();
		String backPanelDescriptor = descriptor.getPreviousDescriptorID();        
		wizard.showDescriptorPanelFor(backPanelDescriptor);

		// Check if the new panel has a next panel. If not, disable the next button
		WizardPanelDescriptor<T> previousPanel = wizard.getPanelDescriptorFor(backPanelDescriptor);
		if (null == previousPanel.getPreviousDescriptorID()) {
			wizard.setPreviousButtonEnabled(false);
		}

		// Re-enable the previous button, in case it was disabled
		wizard.setNextButtonEnabled(true);
	}

	private void load() {
		// Store current state
		WizardPanelDescriptor<T> oldDescriptor = wizard.getCurrentPanelDescriptor();

		// Move to load state and show log panel
		final LoadDescriptor<T> loadDescriptor = (LoadDescriptor<T>) wizard.getPanelDescriptorFor(LoadDescriptor.DESCRIPTOR);
		loadDescriptor.setTargetNextID(oldDescriptor.getDescriptorID());
		loadDescriptor.aboutToDisplayPanel();
		wizard.showDescriptorPanelFor(LoadDescriptor.DESCRIPTOR);

		// Instantiate GuiReader, ask for file, and load it in memory
		loadDescriptor.displayingPanel();

		// Get the loaded data back
		plugin = loadDescriptor.plugin;

		// Enable or disable next and previous button depending on the target descriptor.
		WizardPanelDescriptor<T> nextPanel = wizard.getPanelDescriptorFor(loadDescriptor.getNextDescriptorID());
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
		WizardPanelDescriptor<T> oldDescriptor = wizard.getCurrentPanelDescriptor();

		// Move to save state and execute
		SaveDescriptor<T> saveDescriptor = (SaveDescriptor<T>) wizard.getPanelDescriptorFor(SaveDescriptor.DESCRIPTOR);
		saveDescriptor.setTargetNextID(oldDescriptor.getDescriptorID());
		saveDescriptor.aboutToDisplayPanel();
		wizard.showDescriptorPanelFor(SaveDescriptor.DESCRIPTOR);
		saveDescriptor.displayingPanel();
	}

}