package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;

/**
 * A base descriptor class used to reference a Component panel for the Wizard, as
 * well as provide general rules as to how the panel should behave.
 */
public interface WizardPanelDescriptor {


	/**
	 * Returns a unique key that identifies the GUI state this descriptor represents.
	 */
	public String getKey();

    /**
     * Returns a java.awt.Component that serves as the actual panel.
     */
    public Component getComponent();

    /**
     * This method is used to provide functionality that will be performed just before
     * the panel is to be displayed.
     */
    public void aboutToDisplayPanel();

    /**
     * This method is used to perform functionality when the panel itself is displayed.
     * It is the responsibility of concrete implementations to set the state of the GUI,
     * such as the 'Next' button enabled/disabled. The 'Next' button is disabled by the
     * GUI at the start of each 'Next' action. Concrete implementations must re-enable
     * it before this method returns. For instance, you want to disable the 'Next' button
     * while an intense calculation is ongoing, then re-enable it.
     */
    public void displayingPanel();

    /**
     * This method is used to perform functionality just before the panel is to be hidden.
     */
    public void aboutToHidePanel();
}