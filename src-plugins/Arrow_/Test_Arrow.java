import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ShapeRoi;

import java.awt.BasicStroke;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

public class Test_Arrow extends AbstractTool {
	
	private GeneralPath arrow;
	private double start_X, start_Y, end_X, end_Y;
	/**
	 * Double array that contains the coordinates in pixels of the arrow points.
	 */
	private double[] points = new double[2*5];
	/**
	 * Length of the arrow head, in pixels. Could be set by user/
	 */
	private double length = 10.0;
	/**
	 * Tip angle (in degrees) of the arrow head.
	 */
	private double tip = 60.0;
	/**
	 * Base angle (in degrees) of the arrow head.
	 */
	private double base = 20.0;
	private ImagePlus imp;
	private ImageCanvas canvas;
	
	private enum InteractionStatus { NO_ARROW, FREE, DRAGGING_ARROW_HEAD, };
	private InteractionStatus status;
	
	
	public void run(String arg) {
		super.run(arg);
		imp = WindowManager.getCurrentImage();
		canvas = imp.getCanvas();
		arrow = new GeneralPath();
		status = InteractionStatus.NO_ARROW;
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
		double x = canvas.offScreenXD(e.getX());
		double y = canvas.offScreenYD(e.getY());
		if (status == InteractionStatus.NO_ARROW) {
			start_X = x;          
			start_Y = y;
			status = InteractionStatus.FREE;
			} else {
				double dist_to_line = distanceToLine(x, y);
				double dist_to_arrowhead = distanceToArrow(x, y);
				if (dist_to_arrowhead < 0) {
					status = InteractionStatus.DRAGGING_ARROW_HEAD;
				} else {
					status = InteractionStatus.FREE;
					start_X = x;          
					start_Y = y;					
				}
			}
	}
	

	public void handleMouseDrag(MouseEvent e) {
		switch (status) {
		case DRAGGING_ARROW_HEAD:
		case FREE:
			end_X = canvas.offScreenXD(e.getX());        
			end_Y = canvas.offScreenYD(e.getY());
		}
		calculatePoints();
		makePath();	
		imp.setRoi(new ShapeRoi(arrow));
	}
	
	public void handleMouseMove(MouseEvent e) {
		double x = canvas.offScreenXD(e.getX());
		double y = canvas.offScreenYD(e.getY());
		IJ.showStatus(String.format("Dist to line: %.1f - Dist to head: %.1f", 
				distanceToLine(x, y), distanceToArrow(x, y)));		
	}

	
	/**
	 * Computes the coordinates of the arrow point, and updates the field points with them.
	 */
	private void calculatePoints() {
		// Start and end point
		points[0] = start_X;
		points[1] = start_Y;
		points[2*3] = end_X;
		points[2*3+1] = end_Y;
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
	
	/**
	 * Generate the path for the arrow, from the field points.
	 */
	private void makePath() {
		arrow.reset();
		arrow.moveTo(points[0], points[1]);    		// tail
		arrow.lineTo(points[2*1], points[2*1+1]);   // head back
		arrow.lineTo(points[2*2], points[2*2+1]);   // left point
		arrow.lineTo(points[2*3], points[2*3+1]); 	// head tip
		arrow.lineTo(points[2*4], points[2*4+1]);	// right point
		arrow.lineTo(points[2*1], points[2*1+1]); 	// back to the head back
	}
	
	double distanceToLine(double x, double y) {
		// calculate normal vector
		double normX = end_X - start_X;
		double normY = end_Y - start_Y;
		double l = Math.sqrt(normX * normX + normY * normY);
		if (l > 0) {
			normX /= l;
			normY /= l;
		}

		// scalar product between p - p1 and the normal vector
		double distance = (x - start_X) * normX
			+ (y - start_Y) * normY;
		// scalar product between the point projected onto the 
		// arrow and the arrow vector
		double factor = (x - start_X - distance * normX) * -normX
			+ (y - start_Y - distance * normY) * normY;
		if (factor < 0) {
			double diffX = start_X - x;
			double diffY = start_Y - y;
			return Math.sqrt(diffX * diffX + diffY * diffY);
		}
		if (factor > 1) {
			double diffX = end_X - x;
			double diffY = end_Y - y;
			return Math.sqrt(diffX * diffX + diffY * diffY);
		}
		return Math.abs(distance);
	}
	
	/**
	 * Returns a negative distance when inside the approximated head
	 * @param x
	 * @param y
	 * @return
	 */
	double distanceToArrow(double x, double y) {
		double arrowX = end_X - start_X;
		double arrowY = end_Y - start_Y;
		double l = Math.sqrt(arrowX * arrowX + arrowY * arrowY);

		// calculate the center of the arrow head
		double headX = end_X, headY = end_Y;
		if (l > 0) {
			headX -= arrowX * length / l;
			headY -= arrowY * length / l;
		}

		// calculate the distance to the arrow head
		double diffX = x - headX, diffY = y - headY;
		double distance = Math.sqrt(diffX * diffX + diffY * diffY)	- length;

		// compare to the line distance
		return Math.min(distance, distanceToLine(x, y));
	}


}
