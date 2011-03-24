package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModelInterface;

/**
 * This interface describe a track mate action, that can be run on a 
 * {@link TrackMateModelInterface} object to change its content or properties.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Mar 21, 2011
 *
 */
public interface TrackMateAction  extends InfoTextable {
	
	/**
	 * Execute this action.
	 */
	public void execute(final TrackMateModelInterface model);

	/**
	 * Set the logger that will receive logs when this action is executed.
	 */
	public void setLogger(Logger logger);
		
}
