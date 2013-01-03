package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.DisplayerPanel;

public class RecalculateFeatureAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/calculator.png"));
	public static final String NAME = "Recompute all spot features";
	public static final String INFO_TEXT = "<html>" +
			"Calling this action causes the model to recompute all the feautures <br>" +
			"for all the un-filtered spots." +
			"</html>";
	
	public RecalculateFeatureAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(TrackMate_ plugin) {
		logger.log("Recalculating all features.\n");
		TrackMateModel model = plugin.getModel();
		Logger oldLogger = model.getLogger();
		model.setLogger(logger);
		model.getFeatureModel().computeSpotFeatures(model.getSpots(), true);
		model.setLogger(oldLogger);
		logger.log("Done.\n");
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}
}
