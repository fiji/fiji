package fiji.plugin.trackmate.visualization;

import java.util.EventListener;

public interface TMModelEditListener extends EventListener {

	/**
	 * Called whenever the target spot collection is changed.
	 */
	public void modelChanged(TMModelEditEvent event);
	
}
