package fiji.plugin.trackmate.action;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Listable;

public class ActionFactory implements Listable<TrackMateAction> {
	
	public TrackMateAction radiusToEstimatedValue		= new RadiusToEstimatedAction();
	public TrackMateAction resetRadiusToExpectedValue	= new ResetRadiusAction();
	public TrackMateAction recomputeAllFeatures			= new RecalculateFeatureAction();
	
	protected ArrayList<TrackMateAction> actions;


	public ActionFactory() {
		this.actions = createActionList();
	}
	
	
	@Override
	public List<TrackMateAction> getList() {
		return actions;
	}
	
	protected ArrayList<TrackMateAction> createActionList() {
		ArrayList<TrackMateAction> list = new ArrayList<TrackMateAction>();
		list.add(radiusToEstimatedValue);
		list.add(resetRadiusToExpectedValue);
		list.add(recomputeAllFeatures);
		return list;
	}
	
}
