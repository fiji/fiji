package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

public class ResetRadiusAction extends AbstractTMAction {

	@Override
	public void execute(final TrackMateModelInterface model) {
		final SegmenterSettings segSettings = model.getSettings().segmenterSettings;
		final float radius = segSettings.expectedRadius;
		logger.log(String.format("Setting all spot radiuses to %.1f "+segSettings.spaceUnits+"\n", radius));
		SpotCollection spots = model.getSelectedSpots();
		for(Spot spot : spots)
			spot.putFeature(Feature.RADIUS, radius);
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This action resets the radius of all retained spots back to the value <br> " +
				"given in the segmenter settings. " +
				"</html>";
	}
	
	@Override
	public String toString() {
		return "Reset radius to expected value";
	}

}
