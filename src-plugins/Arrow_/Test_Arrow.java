import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ShapeRoi;

import java.awt.BasicStroke;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;

public class Test_Arrow extends AbstractTool {
	
	GeneralPath arrow;
	/**
	 * Double array that contains the coordinates in pixels of the arrow points.
	 */
	double[] points = new double[2*5];
	/**
	 * Length of the arrow head, in pixels. Could be set by user/
	 */
	double length = 10.0;
	/**
	 * Tip angle (in degrees) of the arrow head.
	 */
	double tip = 60.0;
	/**
	 * Base angle (in degrees) of the arrow head.
	 */
	double base = 20.0;
	ImagePlus imp;
	BasicStroke stroke = new BasicStroke();
	
	public void run(String arg) {
		super.run(arg);
		imp = WindowManager.getCurrentImage();
		arrow = new GeneralPath();
		if (toolID >= 0)
			IJ.showStatus("selected " + getToolName() + " Tool(" + toolID + ")");
	}

	public String getToolName() {
		return "Test_Arrow";
	}

	public String getToolIcon() {
		return "CbooP51b1f5fbbf5f1b15510T5c10X";
	}

	public void handleMousePress(MouseEvent e) {
		ImageCanvas canvas = (ImageCanvas) e.getSource();
		points[0] = canvas.offScreenXD(e.getX());        
		points[1] = canvas.offScreenYD(e.getY());
	}

	public void handleMouseDrag(MouseEvent e) {
		ImageCanvas canvas = (ImageCanvas) e.getSource();
		points[2*3] = canvas.offScreenXD(e.getX());        
		points[2*3+1] = canvas.offScreenYD(e.getY()); 
		calculatePoints();
		makePath();	
		imp.setRoi(new ShapeRoi(arrow));
	}
	
	private void calculatePoints() {
		// P1 = P3 - length*alpha
		double alpha = Math.atan2(points[2*3+1] - points[1], points[2*3] - points[0]);
		points[1*2]   = points[2*3]   - length*Math.cos(alpha);
		points[1*2+1] = points[2*3+1] - length*Math.sin(alpha);

		// SL (sideLength) = length*cos(tip) + length * sin(tip) * cos(base-tip) / sin(base-tip) 
		double SL = length*Math.cos(tip) + length*Math.sin(tip) * Math.cos(base-tip) / Math.sin(base-tip);

		// P2 = P3 - SL*alpha+tip
		points[2*2]   = points[2*3]   - SL*Math.cos(alpha+tip);
		points[2*2+1] = points[2*3+1] - SL*Math.sin(alpha+tip);

		// P4 = P3 - SL*alpha-tip
		points[2*4]   = points[2*3]   - SL*Math.cos(alpha-tip);
		points[2*4+1] = points[2*3+1] - SL*Math.sin(alpha-tip);		
	}
	
	private void makePath() {
		arrow.reset();
		// Line
		arrow.moveTo(points[0], points[1]);
		arrow.lineTo(points[2*1], points[2*1+1]);
		arrow.lineTo(points[2*2], points[2*2+1]);
		arrow.lineTo(points[2*3], points[2*3+1]);
		arrow.lineTo(points[2*4], points[2*4+1]);
		arrow.lineTo(points[2*1], points[2*1+1]);
	}

}
