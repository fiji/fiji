package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.util.mxCellRenderer;

public class TrackSchemeToolbar extends JToolBar {
	
	private static final long serialVersionUID = 3442140463984241266L;
	private static final ImageIcon LINKING_ON_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/connect.png")); 
	private static final ImageIcon LINKING_OFF_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/forbid_connect.png")); 
	private static final ImageIcon RESET_ZOOM_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom.png")); 
	private static final ImageIcon ZOOM_IN_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom_in.png")); 
	private static final ImageIcon ZOOM_OUT_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom_out.png")); 
	private static final ImageIcon REFRESH_ICON		= new ImageIcon(TrackSchemeFrame.class.getResource("resources/refresh.png"));
	private static final ImageIcon PLOT_ICON		= new ImageIcon(TrackSchemeFrame.class.getResource("resources/plots.png"));
	private static final ImageIcon CAPTURE_UNDECORATED_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/camera_go.png"));
	private static final ImageIcon CAPTURE_DECORATED_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/camera_edit.png"));


	private TrackSchemeFrame frame;

	public TrackSchemeToolbar(TrackSchemeFrame frame) {
		super("Track Scheme toolbat", JToolBar.HORIZONTAL);
		this.frame = frame;
		init();
	}
	
	@SuppressWarnings("serial")
	private void init() {
		
		setFloatable(false);

		// Toggle Connect Mode
		final Action toggleLinkingAction = new AbstractAction(null, LINKING_ON_ICON) {
			public void actionPerformed(ActionEvent e) {
				boolean enabled = frame.graphComponent.getConnectionHandler().isEnabled();
				ImageIcon connectIcon;
				if (enabled)
					connectIcon = LINKING_OFF_ICON;
				else
					connectIcon = LINKING_ON_ICON;
				putValue(SMALL_ICON, connectIcon);
				frame.graphComponent.getConnectionHandler().setEnabled(!enabled);
			}
			
		};
		final JButton toggleLinkingButton = new JButton(toggleLinkingAction);
		toggleLinkingButton.setToolTipText("Toggle linking");
		add(toggleLinkingButton);

		// Separator
		addSeparator();

		// Zoom 
		final Action zoomInAction;
		final Action zoomOutAction;
		final Action resetZoomAction;
		final JButton zoomInButton = new JButton();
		final JButton zoomOutButton = new JButton();
		final JButton  resetZoomButton = new JButton();

		zoomInAction = new AbstractAction(null, ZOOM_IN_ICON) {
			public void actionPerformed(ActionEvent e) {
				frame.graphComponent.zoomIn();
			}
		};
		zoomOutAction = new AbstractAction(null, ZOOM_OUT_ICON) {
			public void actionPerformed(ActionEvent e) {
				frame.graphComponent.zoomOut();
			}
		};
		resetZoomAction  = new AbstractAction(null, RESET_ZOOM_ICON) {
			public void actionPerformed(ActionEvent e) {
				frame.graphComponent.zoomTo(1.0, false);
			}
		};

		zoomInButton.setAction(zoomInAction);
		zoomOutButton.setAction(zoomOutAction);
		resetZoomButton.setAction(resetZoomAction);
		zoomInButton.setToolTipText("Zoom in 2x");
		zoomOutButton.setToolTipText("Zoom out 2x");
		resetZoomButton.setToolTipText("Reset zoom");
		add(zoomInButton);
		add(zoomOutButton);
		add(resetZoomButton);

		// Separator
		addSeparator();

		// Redo layout
		final Action redoLayoutAction = new AbstractAction(null, REFRESH_ICON) {
			public void actionPerformed(ActionEvent e) {
				frame.doTrackLayout();
			}
		};
		final JButton redoLayoutButton = new JButton(redoLayoutAction);
		redoLayoutButton.setToolTipText("Redo layout");
		add(redoLayoutButton);

		// Separator
		addSeparator();

		// Plot selection data
		final Action plotSelection = new AbstractAction(null, PLOT_ICON) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.plotSelectionData();
			}
		};
		final JButton plotSelectionButton = new JButton(plotSelection);
		plotSelectionButton.setToolTipText("Plot selection data");

		// Separator
		addSeparator();

		// Capture 
		
		// TODO BUGGY BUGGY
		final Action captureUndecoratedAction = new AbstractAction(null, CAPTURE_UNDECORATED_ICON) {			
			@Override
			public void actionPerformed(ActionEvent e) {
				BufferedImage image = mxCellRenderer.createBufferedImage(frame.getGraph(), null, 1, Color.WHITE, true, null, frame.graphComponent.getCanvas());
				ImagePlus imp = new ImagePlus("Track scheme capture", image);
				imp.show();
			}
		};
		final Action captureDecoratedAction = new AbstractAction(null, CAPTURE_DECORATED_ICON) {			
			@Override
			public void actionPerformed(ActionEvent e) {
				JViewport view = frame.graphComponent.getViewport();
				Point currentPos = view.getViewPosition();
				view.setViewPosition(new Point(0, 0)); // We have to do that otherwise, top left is not painted
				Dimension size = view.getViewSize();
				BufferedImage image =  (BufferedImage) view.createImage(size.width, size.height);
				Graphics2D captureG = image.createGraphics();
				view.paintComponents(captureG);
				view.setViewPosition(currentPos);
				ImagePlus imp = new ImagePlus("Track scheme capture", image);
				imp.show();
			}
		};
		final JButton captureUndecoratedButton = new JButton(captureUndecoratedAction);
		final JButton captureDecoratedButton = new JButton(captureDecoratedAction);
		captureUndecoratedButton.setToolTipText("Capture undecorated track scheme");
		captureDecoratedButton.setToolTipText("Capture decorated track scheme");
		add(captureUndecoratedButton);
		add(captureDecoratedButton);

	}
}
