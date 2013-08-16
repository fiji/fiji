package fiji.plugin.trackmate.action;

import java.util.Iterator;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class RadiusToEstimatedAction extends AbstractTMAction {


	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/lightbulb.png"));
	public static final String NAME = "Set radius to estimated value";
	public static final String INFO_TEXT =  "<html>" +
			"This action changes the radius feature of all retained spots <br> " +
			"to its estimated value, calculated with the radius estimator <br> " +
			"</html>" ;

	public RadiusToEstimatedAction(TrackMate trackmate, TrackMateGUIController controller) {
		super(trackmate, controller);
		this.icon = ICON;
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public void execute() {
		logger.log("Setting all spot radiuses to their estimated value.\n");
		Model model = trackmate.getModel();
		SpotCollection spots = model.getSpots();
		int valid = 0;
		int invalid = 0;

		model.beginUpdate();
		try {
			for (Iterator<Spot> iterator = spots.iterator(true); iterator.hasNext(); ) {
				Spot spot = iterator.next();
				Double diameter = spot.getFeature(SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER);
				if (null == diameter || diameter == 0) {
					invalid++;
				} else {
					spot.putFeature(Spot.RADIUS, diameter/2);
					model.updateFeatures(spot);
					valid++;
				}
			}
		} finally {
			model.endUpdate();
		}
		if (invalid == 0) {
			logger.log(String.format("%d spots changed.\n", valid));
		} else if (valid == 0 ){
			logger.log("All spots miss the "+SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER+" feature.\n");
			logger.log("No modification made.\n");
		} else {
			logger.log("Some spots miss the "+SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER+" feature.\n");
			logger.log(String.format("Updated %d spots, left %d spots unchanged.\n", valid, invalid));
		}
		logger.log("Done.\n");
	}
}
