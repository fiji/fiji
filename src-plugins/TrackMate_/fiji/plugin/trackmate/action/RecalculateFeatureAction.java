package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.gui.DisplayerPanel;

public class RecalculateFeatureAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/calculator.png"));
	
	public RecalculateFeatureAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(TrackMateModelInterface model) {
		logger.log("Recalculating all features.\n");
		Logger oldLogger = model.getLogger();
		model.setLogger(logger);
		model.computeFeatures();
		model.setLogger(oldLogger);
	}

	@Override
	public String getInfoText() {
		return "<html>" +
			"Calling this action causes the model to recompute all the feautures <br>" +
			"for all the spots retained for tracking." +
			"</html>";
	}
	
	@Override
	public String toString() {
		return "Recompute all features";
	}

}
