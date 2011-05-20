package fiji.plugin.trackmate.action;

import ij3d.Image3DUniverse;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.gui.TrackMateFrame;
import fiji.plugin.trackmate.visualization.SpotDisplayer3D;

public class LinkNew3DViewerAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/page_white_link.png"));
	
	public LinkNew3DViewerAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(final TrackMateModelInterface model) {
		new Thread("TrackMate copying thread") {
			public void run() {
				
				final Image3DUniverse universe = new Image3DUniverse();
				universe.show();
			
				SpotDisplayer3D newDisplayer = new SpotDisplayer3D(universe);
				newDisplayer.render();
				
				newDisplayer.setSpots(model.getSpots());
				newDisplayer.setSpotsToShow(model.getSelectedSpots());
				newDisplayer.setTrackGraph(model.getTrackGraph());

				DisplayerPanel displayerPanel = (DisplayerPanel) view.getPanelFor(TrackMateFrame.PanelCard.ACTION_PANEL_KEY);
				if (null != displayerPanel)
					displayerPanel.register(newDisplayer);
			}
		}.start();
	}

	@Override
	public String getInfoText() {
		return "<html>" +
		"This action opens a new 3D viewer, containing only the overlay (spot and tracks), <br> " +
		"properly linked to the current controler." +
		"<p>" +
		"Useful to have synchronized 2D vs 3D views." +
		"</html>" ;
	}

	@Override
	public String toString() {
		return "Link with new 2D viewer";
	}

}
