/**
 * 
 */
package fiji.plugin.trackmate.action;

import java.util.Iterator;
import java.util.Set;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;

/**
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 23, 2011
 *
 */
public class ResetSpotTimeFeatureAction extends AbstractTMAction {


	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/time.png"));
	public static final String NAME = "Reset spot time";
	public static final String INFO_TEXT = "<html>" +
			"Reset the time feature of all spots: it is set to the frame number "  +
			"times the time resolution. " +
			"</html>";
	
	public ResetSpotTimeFeatureAction(TrackMate trackmate, TrackMateGUIController controller) {
		super(trackmate, controller);
		this.icon = ICON;
	}
	
	@Override
	public void execute() {
		logger.log("Reset spot time.\n");
		double dt = trackmate.getSettings().dt;
		if (dt == 0) {
			dt = 1;
		}
		SpotCollection spots = trackmate.getModel().getSpots(); 
		Set<Integer> frames = spots.keySet();
		for(int frame : frames) {
			for (Iterator<Spot> iterator = spots.iterator(frame, true); iterator.hasNext();) {
				iterator.next().putFeature(Spot.POSITION_T, frame * dt); 
			}
			logger.setProgress((double) (frame + 1) / frames.size());
		}
		logger.log("Done.\n");
		logger.setProgress(0);
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
