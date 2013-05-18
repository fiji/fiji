package fiji.plugin.trackmate.gui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

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

	/** The collection of views instantiated and registered in the GUI. 
	 * We need to keep track of them so that we can forward them the 
	 * changes in display settings.	 */
	Collection<TrackMateModelView> views = new HashSet<TrackMateModelView>();

	/** The current display settings, that will be passed to any new view
	 * registered in the GUI.  */
	private Map<String, Object> displaySettings;

	/**
	 * Returns the collection of views instantiated and registered in the GUI. 
	 * @return the collection of {@link TrackMateModelView}.
	 */
	public Collection<TrackMateModelView> getViews() {
		return views;
	}
	
	/**
	 * Adds the specified {@link TrackMateModelView} to the collection of views
	 * managed by this GUI.
	 * @param view the {@link TrackMateModelView} to add.
	 */
	public void addView(TrackMateModelView view) {
		views.add(view);
	}
	
	/**
	 * Removes the specified {@link TrackMateModelView} to the collection of views
	 * managed by this GUI.
	 * @param view the {@link TrackMateModelView} to remove.
	 */
	public void removeView(TrackMateModelView view) {
		views.remove(view);
	}

	public Map<String, Object> getDisplaySettings() {
		return displaySettings;
	}
	
	public void setDisplaySettings(Map<String, Object> displaySettings) {
		this.displaySettings = displaySettings;
	}
	
	/**
	 * Returns the key string of the current descriptor, representing the "state"
	 * of the GUI. Load, Save and Log descriptors are not returned; the descriptor
	 * that is displayed prior to call any of these 3 descriptor stands as the GUI
	 * state.
	 * @return  the state string.
	 */
	public String getGUIStateString() {
		return currentDescriptor.getKey();
	}
}
