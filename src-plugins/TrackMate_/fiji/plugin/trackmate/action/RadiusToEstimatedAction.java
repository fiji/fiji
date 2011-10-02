package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.spot.RadiusEstimator;
import fiji.plugin.trackmate.gui.DisplayerPanel;

public class RadiusToEstimatedAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/lightbulb.png"));
	
	public RadiusToEstimatedAction() {
		this.icon = ICON;
	}
	
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
	public void execute(final TrackMateModel model) {
		logger.log("Setting all spot radiuses to their estimated value.\n");
		SpotCollection spots = model.getFilteredSpots();
		for(Spot spot : spots)
			spot.putFeature(Spot.RADIUS, spot.getFeature(RadiusEstimator.ESTIMATED_DIAMETER) / 2);
	}
}
