package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModelInterface;

public class RadiusToEstimatedAction implements TrackMateAction { // extends AbstractTMAction {

	@Override
	public String getInfoText() {
		return "This action changes the radius feature of all retained spots" +
				"to its estimated value, calculated with the radius estimator";
	}
	
	@Override
	public String toString() {
		return "Set radius to estimated value";
	}

	@Override
	public void execute(final TrackMateModelInterface model) {
		SpotCollection spots = model.getSelectedSpots();
		for(Spot spot : spots)
			spot.putFeature(Feature.RADIUS, spot.getFeature(Feature.ESTIMATED_DIAMETER) / 2);
	}
}
