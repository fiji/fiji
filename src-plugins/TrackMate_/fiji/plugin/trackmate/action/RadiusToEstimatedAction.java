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
		int valid = 0;
		int invalid = 0;
		for(Spot spot : spots) {
			Float diameter = spot.getFeature(RadiusEstimator.ESTIMATED_DIAMETER);
			if (null == diameter || diameter == 0) {
				invalid++;
			} else {
				spot.putFeature(Spot.RADIUS, diameter/2);
				valid++;
			}
		}
		if (invalid == 0) {
			logger.log(String.format("%d spots changed.\n", valid));
		} else if (valid == 0 ){
			logger.log("All spots miss the "+RadiusEstimator.ESTIMATED_DIAMETER+" feature.\n");
			logger.log("No modification made.\n");
		} else {
			logger.log("Some spots miss the "+RadiusEstimator.ESTIMATED_DIAMETER+" feature.\n");
			logger.log(String.format("Updated %d spots, left %d spots unchanged.\n", valid, invalid));
		}
		logger.log("Done.\n");
		controller.getModelView().refresh();
	}
}
