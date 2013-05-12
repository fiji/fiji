package fiji.plugin.trackmate.gui;

import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;

public class TrackMateGUIModel {


	/**
	 * Is used to determine how to react to a 'next' button push. If it is set to true, then we are
	 * normally processing through the GUI, and pressing 'next' should update the GUI and process the
	 * data. If is is set to false, then we are currently loading/saving the data, and we should simply
	 * re-generate the data.
	 */
	boolean actionFlag = true;
	
	/** Is used to determine whether we are currently displaying the log panel after the
	 * user has pressed the log button. If true, then our state is "currently displaying log". */
	boolean logButtonState = false;
	
	boolean loadButtonState;
	
	boolean saveButtonState;
	
	boolean nextButtonState;
	
	boolean previousButtonState;

	/** The panel descriptor currently displayed. */ 
	WizardPanelDescriptor currentDescriptor;

	/** The panel descriptor previously displayed, e.g. before the user pressed the save button. */ 
	WizardPanelDescriptor previousDescriptor;

	
	/** The display settings configuring the look and feel of all the TrackMateModelViews controlled
	 * by this GUI.  */
	Map<String, Object> displaySettings = new HashMap<String, Object>(); 
	

	
}
