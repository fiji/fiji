package fiji.plugin.trackmate.action;

import ij3d.Image3DUniverse;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;

public class LinkNew3DViewerAction extends AbstractTMAction {


	public static final String NAME = "Link with new 3D viewer";
	public static final String INFO_TEXT = "<html>" +
			"This action opens a new 3D viewer, containing only the overlay (spot and tracks), <br> " +
			"properly linked to the current controller." +
			"<p>" +
			"Useful to have synchronized 2D vs 3D views." +
			"</html>" ;
	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_white_link.png"));
	
	public LinkNew3DViewerAction(TrackMate trackmate, TrackMateGUIController controller) {
		super(trackmate, controller);
		this.icon = ICON;
	}
	
	@Override
	public void execute() {
		new Thread("TrackMate new 3D viewer thread") {
			public void run() {
				logger.log("Rendering 3D overlay...\n");
				Image3DUniverse universe = new Image3DUniverse();
				universe.show();
				SpotDisplayer3D newDisplayer = new SpotDisplayer3D(trackmate.getModel(), controller.getSelectionModel(), universe );
				for (String key : controller.getGuimodel().getDisplaySettings().keySet()) {
					newDisplayer.setDisplaySettings(key, controller.getGuimodel().getDisplaySettings().get(key));
				}
				controller.getGuimodel().addView(newDisplayer);
				newDisplayer.render();
				logger.log("Done.\n");
			}
		}.start();
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
