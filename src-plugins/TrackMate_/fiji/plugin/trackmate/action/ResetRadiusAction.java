package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

public class ResetRadiusAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/lightbulb_off.png"));

	public ResetRadiusAction() {
	this.icon = ICON;
	}
	
	@Override
	public void execute(final TrackMateModel model) {
		final SegmenterSettings segSettings = model.getSettings().segmenterSettings;
		final float radius = segSettings.expectedRadius;
		logger.log(String.format("Setting all spot radiuses to %.1f "+segSettings.spaceUnits+"\n", radius));
		SpotCollection spots = model.getFilteredSpots();
		for(Spot spot : spots)
			spot.putFeature(SpotFeature.RADIUS, radius);
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
