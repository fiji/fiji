package ij3d;

import ij.gui.Toolbar;
import ij.IJ;

public class IJAdapter implements UIAdapter {

	public boolean isHandTool() {
		return Toolbar.getToolId() == Toolbar.HAND;
	}

	public boolean isPointTool() {
		return Toolbar.getToolId() == Toolbar.POINT;
	}

	public boolean isMagnifierTool() {
		return Toolbar.getToolId() == Toolbar.MAGNIFIER;
	}

	public boolean isRoiTool() {
		int tool = Toolbar.getToolId();
		return tool == Toolbar.RECTANGLE || tool == Toolbar.OVAL
			|| tool == Toolbar.POLYGON || tool == Toolbar.FREEROI
			|| tool == Toolbar.LINE || tool == Toolbar.POLYLINE
			|| tool == Toolbar.FREELINE || tool == Toolbar.POINT
			|| tool == Toolbar.WAND;
	}

	public int getToolId() {
		return Toolbar.getToolId();
	}

	public void setTool(int id) {
		Toolbar.getInstance().setTool(id);
	}

	public void setHandTool() {
		setTool(Toolbar.HAND);
	}

	public void setPointTool() {
		setTool(Toolbar.POINT);
	}

	public void showStatus(String status) {
		IJ.showStatus(status);
	}

	public void showProgress(int a, int b) {
		IJ.showProgress(a, b);
	}
}

