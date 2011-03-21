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
		SpotCollection spots = model.getSelectedSpots();
		for(Spot spot : spots)
			spot.putFeature(Feature.RADIUS, radius);
	}

	@Override
	public String getInfoText() {
		return "This action resets the radius of all retained spots back to the value" +
				"given in the segmenter settings.";
	}
	
	@Override
	public String toString() {
		return "Reset radius to expected value";
	}

}
