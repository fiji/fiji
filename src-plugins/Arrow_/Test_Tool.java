import ij.IJ;

import java.awt.event.MouseEvent;

public class Test_Tool extends AbstractTool {
	public void run(String arg) {
		super.run(arg);
		if (toolID >= 0)
			IJ.showStatus("selected " + getToolName()
					+ " Tool(" + toolID + ")");
	}

	public String getToolName() {
		return "Test";
	}

	public String getToolIcon() {
		return "CbooP51b1f5fbbf5f1b15510T5c10X";
	}

	public void handleMousePress(MouseEvent e) {
		IJ.showStatus("press noticed by " + getToolName() + " Tool");
	}

	public void handleMouseDrag(MouseEvent e) {
		IJ.showStatus("moved to " + e.getX() + ", " + e.getY());
	}
}
