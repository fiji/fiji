package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;

/**
 * This interface describe a track mate action, that can be run on a 
 * {@link TrackMate} object to change its content or properties.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011-2013
 */
public interface TrackMateAction extends InfoTextable {
	
	/**
	 * Execute this action using the given trackmate
	 */
	public void execute();

	/**
	 * Set the logger that will receive logs when this action is executed.
	 */
	public void setLogger(Logger logger);
	
	/**
	 * Return the icon for this action. Can be null.
	 */
	public ImageIcon getIcon();

}
