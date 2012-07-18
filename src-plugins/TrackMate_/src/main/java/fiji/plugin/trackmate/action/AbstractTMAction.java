package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public abstract class AbstractTMAction<T extends RealType<T> & NativeType<T>> implements TrackMateAction<T> {

	protected Logger logger = Logger.VOID_LOGGER;
	protected ImageIcon icon = null;
	protected TrackMateWizard wizard;
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public ImageIcon getIcon() {
		return icon ;
	}
	
	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}
}
