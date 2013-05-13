package fiji.plugin.trackmate;

import java.util.EventListener;

/**
 * An interface for listeners that will be notified when a {@link TrackMateModel}
 * is been changed.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> May 30, 2011
 *
 */
public interface ModelChangeListener extends EventListener {

	/**
	 * This notification is fired when a {@link TrackMateModel} has been changed.
	 */
	public void modelChanged(final ModelChangeEvent event);
	
}
