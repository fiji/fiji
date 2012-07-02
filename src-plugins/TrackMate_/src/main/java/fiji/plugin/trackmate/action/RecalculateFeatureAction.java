package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.DisplayerPanel;

public class RecalculateFeatureAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/calculator.png"));
	
	public RecalculateFeatureAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(TrackMate_ plugin) {
		logger.log("Recalculating all features.\n");
		TrackMateModel model = plugin.getModel();
		Logger oldLogger = model.getLogger();
		model.setLogger(logger);
		model.getFeatureModel().computeSpotFeatures(model.getSpots());
		model.setLogger(oldLogger);
		logger.log("Done.\n");
	}

	@Override
	public String getInfoText() {
		return "<html>" +
			"Calling this action causes the model to recompute all the feautures <br>" +
			"for all the un-filtered spots." +
			"</html>";
	}
	
	@Override
	public String toString() {
		return "Recompute all spot features";
	}
}
