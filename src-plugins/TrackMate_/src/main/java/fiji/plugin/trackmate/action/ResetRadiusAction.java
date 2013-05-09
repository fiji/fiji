package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;

import java.util.Iterator;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;

public class ResetRadiusAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(ConfigureViewsPanel.class.getResource("images/lightbulb_off.png"));
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
	public void execute(final TrackMate trackmate) {
		Double radius = (Double) trackmate.getSettings().detectorSettings.get(KEY_RADIUS);
		if (null == radius) {
			radius = FALL_BACK_RADIUS;
			logger.error("Could not determine expected radius from settings. Falling back to "+FALL_BACK_RADIUS+" "
					 + trackmate.getSettings().spaceUnits);
		}
		
		logger.log(String.format("Setting all spot radiuses to %.1f "+trackmate.getSettings().spaceUnits+"\n", radius));
		SpotCollection spots = trackmate.getModel().getSpots();
		for (Iterator<Spot> iterator = spots.iterator(true); iterator.hasNext();) {
			iterator.next().putFeature(Spot.RADIUS, radius);
		}
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
