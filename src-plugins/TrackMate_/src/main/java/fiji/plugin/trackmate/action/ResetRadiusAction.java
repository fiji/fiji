package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.DisplayerPanel;

public class ResetRadiusAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/lightbulb_off.png"));
	public static final String NAME = "Reset radius to default value";
	public static final String INFO_TEXT = "<html>" +
				"This action resets the radius of all retained spots back to the value <br> " +
				"given in the detector settings. " +
				"</html>";
	private static final double FALL_BACK_RADIUS = 5;

	public ResetRadiusAction() {
	this.icon = ICON;
	}
	
	@Override
	public void execute(final TrackMate_ plugin) {
		Double radius = (Double) plugin.getModel().getSettings().detectorSettings.get(KEY_RADIUS);
		if (null == radius) {
			radius = FALL_BACK_RADIUS;
			logger.error("Could not determine expected radius from settings. Falling back to "+FALL_BACK_RADIUS+" "
					 + plugin.getModel().getSettings().spaceUnits);
		}
		
		logger.log(String.format("Setting all spot radiuses to %.1f "+plugin.getModel().getSettings().spaceUnits+"\n", radius));
		SpotCollection spots = plugin.getModel().getFilteredSpots();
		for(Spot spot : spots)
			spot.putFeature(Spot.RADIUS, radius);
		wizard.getDisplayer().refresh();
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
