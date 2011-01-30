/**
 * 
 */
package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.EventListener;

/**
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Jan 29, 2011
 */
public interface SpotSelectionListener extends EventListener {

	/**
	 * Called whenever the value of the selection changes.
	 * @param e the event that characterizes the change.
	 */
	public void valueChanged(SpotSelectionEvent e);
}
