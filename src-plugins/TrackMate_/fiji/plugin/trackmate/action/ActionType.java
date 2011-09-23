package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.TrackMateFrame;

public enum ActionType implements InfoTextable {
	
	COPY_OVERLAY_TO 		(new CopyOverlayAction()), 
	LINK_TO_NEW_3D_VIEWER 	(new LinkNew3DViewerAction()),
	SET_RADIUS_TO_ESTIMATED (new RadiusToEstimatedAction()),
	RESET_RADIUS_TO_EXPECTED (new ResetRadiusAction()),
	RESET_SPOT_TIME 		(new ResetSpotTimeFeatureAction()),
	RECOMPUTE_ALL_FEATURES 	(new RecalculateFeatureAction()),
	GRAB_SPOT_IMAGES		(new GrabSpotImageAction()),
	CAPTURE_OVERLAY			(new CaptureOverlayAction()),
	PLOT_NSPOTS				(new PlotNSpotsVsTimeAction());
	
	private TrackMateAction action;

	private ActionType(TrackMateAction action) {
		this.action = action;
	}
	
	
	public TrackMateAction getAction() {
		return action;
	}
	
	@Override
	public String getInfoText() {
		return action.getInfoText();
	}
	
	@Override
	public String toString() {
		return action.toString();
	}
	
	public void execute(final TrackMateModel model) {
		action.execute(model);
	}


	public void setLogger(Logger logger) {
		action.setLogger(logger);
	}


	public void setView(TrackMateFrame view) {
		action.setView(view);
	}

}
