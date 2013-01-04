/**
 * 
 */
package fiji.plugin.trackmate;

import java.util.EventListener;

/**
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Jan 29, 2011
 */
public interface SelectionChangeListener extends EventListener {

	/**
	 * Called whenever the value of the selection changes.
	 * @param event  the event that characterizes the change.
	 */
	public void selectionChanged(SelectionChangeEvent event);


}
