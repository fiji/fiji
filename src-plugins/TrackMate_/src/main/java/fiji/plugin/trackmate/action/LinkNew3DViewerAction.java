package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;

public class LinkNew3DViewerAction extends AbstractTMAction {

	public static final String NAME = "Link with new 3D viewer";
	public static final String INFO_TEXT = "<html>" +
			"This action opens a new 3D viewer, containing only the overlay (spot and tracks), <br> " +
			"properly linked to the current controller." +
			"<p>" +
			"Useful to have synchronized 2D vs 3D views." +
			"</html>" ;
	public static final ImageIcon ICON = new ImageIcon(ConfigureViewsPanel.class.getResource("images/page_white_link.png"));
	
	public LinkNew3DViewerAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(final TrackMate trackmate) {
		new Thread("TrackMate new 3D viewer thread") {
			public void run() {
				logger.log("Rendering 3D overlay...\n");
				SpotDisplayer3D newDisplayer = new SpotDisplayer3D(trackmate.getModel(), trackmate.getSettings());
				newDisplayer.setRenderImageData(false);
				ConfigureViewsPanel displayerPanel = (ConfigureViewsPanel) wizard.getPanelDescriptorFor(ConfigureViewsPanel.DESCRIPTOR);
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
		return INFO_TEXT;
	}

	@Override
	public String toString() {
		return NAME;
	}

}
