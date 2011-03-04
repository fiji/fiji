package fiji.plugin.trackmate.visualization;

import java.util.EventListener;

public interface SpotCollectionEditListener extends EventListener {

	/**
	 * Called whenever the target spot collection is changed.
	 */
	public void collectionChanged(SpotCollectionEditEvent event);
	
}
