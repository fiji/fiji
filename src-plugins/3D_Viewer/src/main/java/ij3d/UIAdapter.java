package ij3d;

public interface UIAdapter {

	public boolean isHandTool();
	public boolean isPointTool();
	public boolean isMagnifierTool();
	public boolean isRoiTool();

	public int getToolId();
	public void setTool(int id);
	public void setHandTool();
	public void setPointTool();

	public void showStatus(String status);
	public void showProgress(int a, int b);
}

