package fiji.plugin.trackmate.gui;

import java.util.EventObject;

import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * An event object fired when the user changes one of the display settings through
 * this GUI panel. It stores the display settings key related to this display settings,
 * its new values and its old value.
 * @author Jean-Yves Tinevez - 2013
 * @see TrackMateModelView
 */
public class DisplaySettingsEvent extends EventObject {

	private static final long serialVersionUID = 4259460590261659068L;
	private final String key;
	private final Object newValue;
	private final Object oldValue;

	public DisplaySettingsEvent(Object source, String key, Object newValue, Object oldValue) {
		super(source);
		this.key = key;
		this.newValue = newValue;
		this.oldValue = oldValue;
	}
	
	public String getKey() {
		return key;
	}
	
	public Object getOldValue() {
		return oldValue;
	}
	
	public Object getNewValue() {
		return newValue;
	}
	
	@Override
	public String toString() {
		String str = "DisplaySettingsEvent[source=" + source.getClass() + ", key="+key +", new="+newValue +", old="+oldValue;
		return str;
	}
}