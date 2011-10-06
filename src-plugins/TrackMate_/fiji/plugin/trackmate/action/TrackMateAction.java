package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.TrackMateFrame;

/**
 * This interface describe a track mate action, that can be run on a 
 * {@link TrackMateModel} object to change its content or properties.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Mar 21, 2011
 *
 */
public interface TrackMateAction  extends InfoTextable {
	
	/**
	 * Execute this action on the given model
	 */
	public void execute(final TrackMateModel model);

	/**
	 * Set the logger that will receive logs when this action is executed.
	 */
	public void setLogger(Logger logger);
	
	/**
	 * Return the icon for this action. Can be null.
	 */
	public ImageIcon getIcon();

	/**
	 * Set the view linked to this action, in case the action needs accessing it or updating it.
	 */
	public void setView(TrackMateFrame view);

	/**
	 * Set the plugin controlled by the launcher of this action.
	 */
	public void setPlugin(TrackMate_ plugin);
		
}
