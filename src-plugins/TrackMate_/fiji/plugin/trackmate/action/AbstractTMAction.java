package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.WizardController;

public abstract class AbstractTMAction implements TrackMateAction {

	protected TrackMateModel model = null;
	protected Logger logger = Logger.VOID_LOGGER;
	protected ImageIcon icon = null;
	protected WizardController controller = null;
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public ImageIcon getIcon() {
		return icon ;
	}
	
	@Override
	public void setController(WizardController controller) {
		this.controller  = controller;
	}
	
}
