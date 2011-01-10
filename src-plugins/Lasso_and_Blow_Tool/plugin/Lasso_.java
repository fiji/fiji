package plugin;

import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;

import ij.IJ;
import ij.ImagePlus;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import plugin.Lasso.Mode;

public class Lasso_ extends AbstractTool
		implements MouseListener, MouseMotionListener, ToolWithOptions {
	public String getToolName() {
		return "Lasso/Blow Tool";
	}

	public String getToolIcon() {
		return "C000Pdaa79796a6c4c2a1613215276998a6a70";
	}

	protected Mode mode = Mode.BLOW;
	protected Lasso lasso;

	public void mousePressed(MouseEvent e) {
		ImagePlus image = getImagePlus(e);
		if (image == null)
			return;
		if (lasso == null || image != lasso.getImage())
			lasso = new Lasso(image, mode);
		int x = getOffscreenX(e);
		int y = getOffscreenY(e);
		lasso.initDijkstra(x, y, IJ.shiftKeyDown());
	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mouseDragged(MouseEvent e) {
		int x = getOffscreenX(e);
		int y = getOffscreenY(e);
		if (lasso.getMode() == Mode.BLOW)
			lasso.moveBlow(x, y);
		else
			lasso.moveLasso(x, y);
	}

	public void mouseMoved(MouseEvent e) {}

	public void setMode(Mode mode) {
		this.mode = mode;
		if (lasso != null)
			lasso.setMode(mode);
		IJ.showStatus(mode.toString());
	}

	public void setMode(String mode) {
		setMode(Mode.valueOf(mode));
	}

	public void setMode(int mode) {
		setMode(Mode.valueOf(mode));
	}

	public void toggleMode() {
		setMode((mode.ordinal() + 1) % Mode.labels.length);
	}

	public void showOptionDialog() {
		if (lasso == null)
			lasso = new Lasso(IJ.getImage());
		lasso.optionDialog();
		mode = lasso.getMode();
	}
}