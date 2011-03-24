package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModelInterface;

public abstract class AbstractTMAction implements TrackMateAction {

	protected TrackMateModelInterface model = null;
	protected Logger logger = Logger.VOID_LOGGER;
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

}
