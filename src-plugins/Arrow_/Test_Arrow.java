import fiji.roi.Arrow;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.MouseEvent;

public class Test_Arrow extends AbstractTool {
	
	Arrow arr;
	ImagePlus imp;
	BasicStroke stroke = new BasicStroke();
	
	public void run(String arg) {
		super.run(arg);
		imp = WindowManager.getCurrentImage();
		arr = new Arrow(0, 0, 0, 0, 10, 30, 60, false);
		imp.setDisplayList(arr, Color.GREEN, stroke);
		if (toolID >= 0)
			IJ.showStatus("selected " + getToolName()
					+ " Tool(" + toolID + ")");
	}

	public String getToolName() {
		return "Test_Arrow";
	}

	public String getToolIcon() {
		return "CbooP51b1f5fbbf5f1b15510T5c10X";
	}

	public void handleMousePress(MouseEvent e) {
		IJ.showStatus("press noticed by " + getToolName() + " Tool");
		arr.setStartPoint(e.getPoint());
	}

	public void handleMouseDrag(MouseEvent e) {
		IJ.showStatus("moved to " + e.getX() + ", " + e.getY());	
		arr.setEndPoint(e.getPoint());
		imp.setDisplayList(arr, Color.GREEN, stroke);
		
	}
}
