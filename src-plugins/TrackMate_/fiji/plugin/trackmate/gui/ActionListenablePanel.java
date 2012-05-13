package fiji.plugin.trackmate.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JPanel;

public class ActionListenablePanel extends JPanel {

	private static final long serialVersionUID = -1732282609704990375L;

	protected ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener>();
	
	/**
	 * Add an {@link ActionListener} to this panel. These listeners will be notified when
	 * a button is pushed or when the feature to color is changed.
	 */
	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
	}
	
	/**
	 * Remove an ActionListener from this panel. 
	 * @return true if the listener was in the ActionListener collection of this instance.
	 */
	public boolean removeActionListener(ActionListener listener) {
		return actionListeners.remove(listener);
	}
	
	public Collection<ActionListener> getActionListeners() {
		return actionListeners;
	}
	

	/** 
	 * Forward the given {@link ActionEvent} to all the {@link ActionListener} of this panel.
	 */
	protected void fireAction(ActionEvent e) {
		for (ActionListener l : actionListeners)
			l.actionPerformed(e);
	}
	
}
