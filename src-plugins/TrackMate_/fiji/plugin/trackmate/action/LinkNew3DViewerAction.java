package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;

public class LinkNew3DViewerAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/page_white_link.png"));
	
	public LinkNew3DViewerAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(final TrackMate_ plugin) {
		new Thread("TrackMate new 3D viewer thread") {
			public void run() {
				logger.log("Rendering 3D overlay...\n");
				SpotDisplayer3D newDisplayer = new SpotDisplayer3D();
				newDisplayer.setRenderImageData(false);
				newDisplayer.setModel(plugin.getModel());
				DisplayerPanel displayerPanel = (DisplayerPanel) wizard.getPanelDescriptorFor(DisplayerPanel.DESCRIPTOR);
				if (null != displayerPanel) {
					displayerPanel.register(newDisplayer);
					displayerPanel.updateDisplaySettings(newDisplayer.getDisplaySettings());
				}
				newDisplayer.render();
				logger.log("Done.\n");
			}
		}.start();
	}

	@Override
	public String getInfoText() {
		return "<html>" +
		"This action opens a new 3D viewer, containing only the overlay (spot and tracks), <br> " +
		"properly linked to the current controller." +
		"<p>" +
		"Useful to have synchronized 2D vs 3D views." +
		"</html>" ;
	}

	@Override
	public String toString() {
		return "Link with new 3D viewer";
	}

}
