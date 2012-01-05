package fiji.plugin.trackmate.gui;

import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * The model for the Wizard component, which tracks the text, icons, and enabled state
 * of each of the buttons, as well as the current panel that is displayed.
 */
public class WizardModel {

    
    private WizardPanelDescriptor currentPanel;
    private HashMap<Object, WizardPanelDescriptor> panelHashmap;
    
    /* DEFAULT FIELDS - make sure they are initialized by the wizard. */
    JButton nextButton;
    JButton previousButton;
    JButton loadButton;
    JButton saveButton;
    
    /*
     * CONSRUCTOR
     */    
    
    public WizardModel() {
        this.panelHashmap = new HashMap<Object, WizardPanelDescriptor>();
        
    }
    
    /*
     * METHODS
     */
    		
    
    
    /**
     * Returns the currently displayed WizardPanelDescriptor.
     * @return The currently displayed WizardPanelDescriptor
     */    
    WizardPanelDescriptor getCurrentPanelDescriptor() {
        return currentPanel;
    }
    
    /**
     * Registers the WizardPanelDescriptor in the model using the Object-identifier specified.
     * @param id Object-based identifier
     * @param descriptor WizardPanelDescriptor that describes the panel
     */    
     void registerPanel(Object id, WizardPanelDescriptor descriptor) {
        
        //  Place a reference to it in a hashtable so we can access it later
        //  when it is about to be displayed.
        
        panelHashmap.put(id, descriptor);
        
    }  
    
    /**
     * Sets the current panel to that identified by the Object passed in.
     * @param id Object-based panel identifier
     * @return boolean indicating success or failure
     */    
     boolean setCurrentPanel(Object id) {

        //  First, get the hashtable reference to the panel that should
        //  be displayed.
        
        WizardPanelDescriptor nextPanel = panelHashmap.get(id);
        
        //  If we couldn't find the panel that should be displayed, return
        //  false.
        
        if (nextPanel == null) {
        	return false;
        }

        WizardPanelDescriptor oldPanel = currentPanel;
        currentPanel = nextPanel;
        
        if (oldPanel != currentPanel) {
//            firePropertyChange(CURRENT_PANEL_DESCRIPTOR_PROPERTY, oldPanel, currentPanel);
        }
        return true;
    }

	public void setNextButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { nextButton.setEnabled(b); }
		});
	}

	public void setPreviousButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { previousButton.setEnabled(b); }
		});
	}
	
	public void setSaveButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { saveButton.setEnabled(b); }
		});
	}
	
	public void setLoadButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { loadButton.setEnabled(b); }
		});
	}




}
