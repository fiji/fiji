package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModelInterface;

public enum ActionType implements InfoTextable {
	SET_RADIUS_TO_ESTIMATED (new RadiusToEstimatedAction()),
	RESET_RADIUS_TO_EXPECTED (new ResetRadiusAction()),
	RECOMPUTE_ALL_FEATURES (new RecalculateFeatureAction());
	
	private TrackMateAction action;


	private ActionType(TrackMateAction action) {
		this.action = action;
	}
	
	
	@Override
	public String getInfoText() {
		return action.getInfoText();
	}
	
	@Override
	public String toString() {
		return action.toString();
	}
	
	public void execute(final TrackMateModelInterface model) {
		action.execute(model);
	}


	public void setLogger(Logger logger) {
		action.setLogger(logger);
	}

}
