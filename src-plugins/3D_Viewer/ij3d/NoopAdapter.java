package ij3d;

public class NoopAdapter implements UIAdapter {

	public boolean isHandTool() {
		return true;
	}

	public boolean isPointTool() {
		return false;
	}

	public boolean isMagnifierTool() {
		return false;
	}

	public boolean isRoiTool() {
		return false;
	}

	public int getToolId() {
		return 0;
	}

	public void setTool(int id) {
	}

	public void setHandTool() {
	}

	public void setPointTool() {
	}

	public void showStatus(String status) {
	}

	public void showProgress(int a, int b) {
	}
}


