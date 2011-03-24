package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModelInterface;

public class RadiusToEstimatedAction extends AbstractTMAction {

	@Override
	public String getInfoText() {
		return "<html>" +
				"This action changes the radius feature of all retained spots <br> " +
				"to its estimated value, calculated with the radius estimator <br> " +
				"</html>" ;
	}
	
	@Override
	public String toString() {
		return "Set radius to estimated value";
	}

	@Override
	public void execute(final TrackMateModelInterface model) {
		logger.log("Setting all spot radiuses to their estimated value.\n");
		SpotCollection spots = model.getSelectedSpots();
		for(Spot spot : spots)
			spot.putFeature(Feature.RADIUS, spot.getFeature(Feature.ESTIMATED_DIAMETER) / 2);
	}
}
