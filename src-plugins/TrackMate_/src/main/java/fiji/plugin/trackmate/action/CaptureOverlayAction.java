package fiji.plugin.trackmate.action;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.process.ColorProcessor;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class CaptureOverlayAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/camera_go.png"));
	public static final String NAME = "Capture overlay";
	public static final String INFO_TEXT = "<html>" +
			"If the current displayer is the HyperstackDisplayer, this action <br>" +
			"will capture the TrackMate overlay with current display settings. <br>" +
			"That is: a new RGB stack will be created (careful with large data) where <br>" +
			"each frame contains a RGB snapshot of the TrackMate display. " +
			"<p>" +
			"It can take long since we pause between each frame to ensure the whole <br>" +
			"overlay is redrawn. The current zoom is taken into account. <br>" +
			"Also, make sure nothing is moved over the image while capturing. "+
			"</html>";

	public CaptureOverlayAction() {
		this.icon = ICON;
	}

	@Override
	public void execute(TrackMate_ plugin) {
		logger.log("Capturing TrackMate overlay.\n");
		logger.log("  Preparing and allocating memory...");
		try {
			final TrackMateModel model = plugin.getModel();
			final ImagePlus imp =  model.getSettings().imp;
			final ImageWindow win = imp.getWindow();
			win.toFront();
			final Point loc = win.getLocation();
			final ImageCanvas ic = win.getCanvas();
			final Rectangle bounds = ic.getBounds();
			loc.x += bounds.x;
			loc.y += bounds.y;
			final Rectangle r = new Rectangle(loc.x, loc.y, bounds.width, bounds.height);
			ImageStack stack = new ImageStack(bounds.width, bounds.height);
			Robot robot;
			try {
				robot = new Robot();
			} catch (AWTException e) {
				logger.error("Problem creating the image grabber:\n"+e.getLocalizedMessage());
				return;
			}
			logger.log(" done.\n");

			logger.log("  Performing capture...");
			for (int i = 0; i < imp.getStackSize(); i++) {
				logger.setProgress((float) i / imp.getStackSize());
				imp.setPosition(i+1);
				IJ.wait(200);
				final Image image = robot.createScreenCapture(r);
				final ColorProcessor cp = new ColorProcessor(image);
				stack.addSlice(null, cp);
			}
			new ImagePlus("TrackMate capture", stack).show();
			logger.log(" done.\n");
		} finally {
			logger.setProgress(0);
		}
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
