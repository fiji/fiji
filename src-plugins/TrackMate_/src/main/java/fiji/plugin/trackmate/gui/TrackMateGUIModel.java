package fiji.plugin.trackmate.gui;

public class TrackMateGUIModel {


	/**
	 * Is used to determine how to react to a 'next' button push. If it is set to true, then we are
	 * normally processing through the GUI, and pressing 'next' should update the GUI and process the
	 * data. If is is set to false, then we are currently loading/saving the data, and we should simply
	 * re-generate the data.
	 */
	boolean actionFlag = true;
	
	/** Is used to determine whether we are currently displaying the log panel after the
	 * user has pressed the log button. */
	boolean logButtonState = false;
	
	boolean loadButtonState;
	
	boolean saveButtonState;
	
	boolean nextButtonState;
	
	boolean previousButtonState;

	/** Used to store the ID of the previous descriptor before the user pressed the log button.	 */
	String previousPanelID;
	
	/** The panel descriptor currently displayed. */ 
	WizardPanelDescriptor currentDescriptor;
	

	
}
