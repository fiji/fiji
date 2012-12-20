package fiji.plugin.trackmate.gui;

import java.awt.CardLayout;
import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;

/**
 * A base descriptor class used to reference a Component panel for the Wizard, as
 * well as provide general rules as to how the panel should behave.
 */
public interface WizardPanelDescriptor {
      
	/** 
	 * Set a reference to the model that will embed the GUI component this descriptor controls.
	 */
	public void setWizard(TrackMateWizard wizard);

	/**
	 * Set a reference to the TrackMate plugin that is managed by the GUI. This
	 * reference can then be used by GUI components to execute actions or change
	 * the data model. 
	 */
	public void setPlugin(TrackMate_ plugin);
	
    /**
     * @return a java.awt.Component that serves as the actual panel.
     */    
    public Component getComponent();
    
    /**
     * @return  a String to serves in the {@link CardLayout} to identify the target
     * component for display. This can be of use in case multiple {@link WizardPanelDescriptor}
     * need to share the same instance of the component.
     */
    public String getComponentID();
    
    /**
     * @return the unique String-based identifier for this descriptor, not to be mixed 
     * with the panel descriptor, which indexes the component.
     */    
    public String getDescriptorID();

    /**
     * Provide the String-based identifier of the panel that the
     * user should traverse to when the Next button is pressed. Note that this method
     * is only called when the button is actually pressed, so that the panel can change
     * the next panel's identifier dynamically at runtime if necessary. Return null if
     * the button should be disabled. 
     * @return Object-based identifier.
     */    
    public String getNextDescriptorID();

    /**
     * Provide the Object-based identifier of the panel that the
     * user should traverse to when the Back button is pressed. Note that this method
     * is only called when the button is actually pressed, so that the panel can change
     * the previous panel's identifier dynamically at runtime if necessary. Return null if
     * the button should be disabled.
     * @return Object-based identifier
     */    
    public String getPreviousDescriptorID();
    
    /**
     * This method is used to provide functionality that will be performed just before
     * the panel is to be displayed.
     */    
    public void aboutToDisplayPanel();
 
    /**
     * This method is used to perform functionality when the panel itself is displayed.
     */    
    public void displayingPanel();
 
    /**
     * This method is used to perform functionality just before the panel is to be hidden.
     */    
    public void aboutToHidePanel();
}