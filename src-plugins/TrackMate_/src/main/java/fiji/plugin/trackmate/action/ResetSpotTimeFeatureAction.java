/**
 * 
 */
package fiji.plugin.trackmate.action;

import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.DisplayerPanel;

/**
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 23, 2011
 *
 */
public class ResetSpotTimeFeatureAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/time.png"));
	public static final String NAME = "Reset spot time";
	public static final String INFO_TEXT = "<html>" +
			"Reset the time feature of all spots: it is set to the frame number "  +
			"times the time resolution. " +
			"</html>";
	
	public ResetSpotTimeFeatureAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(TrackMate_ plugin) {
		logger.log("Reset spot time.\n");
		double dt = plugin.getModel().getSettings().dt;
		if (dt == 0) {
			dt = 1;
		}
		Set<Integer> frames = plugin.getModel().getSpots().keySet();
		for(int frame : frames) {
			List<Spot> spots = plugin.getModel().getSpots().get(frame);
			for(Spot spot : spots) {
				spot.putFeature(Spot.POSITION_T, frame * dt); 
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
