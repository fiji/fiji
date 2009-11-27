package fiji.util;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ShapeRoi;

import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;

import javax.swing.SwingUtilities;

public class Test_Arrow extends fiji.util.AbstractTool {
	
	/**
	 * How close we have to be from control points to drag them.
	 */
	private final static double DRAG_TOLERANCE = 5.0;
	
	private GeneralPath arrow;
	/**
	 * End and start point coordinates of the arrow.
	 */
	private double start_X, start_Y, end_X, end_Y;
	/**
	 * To monitor how much we drag for user interaction.
	 */
	private double start_drag_X, start_drag_Y;
	/**
	 * Double array that contains the coordinates in pixels of the arrow points.
	 */
	private double[] points = new double[2*5];
	/**
	 * Length of the arrow head, in pixels. Could be set by user.
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
	private enum InteractionStatus { NO_ARROW, FREE, DRAGGING_ARROW_HEAD, DRAGGING_ARROW_BASE, DRAGGING_LINE};
	private InteractionStatus status;
	
	
	public void run(String arg) {
		super.run(arg);
		imp = WindowManager.getCurrentImage();
		canvas = imp.getCanvas();
		arrow = new GeneralPath();
		status = InteractionStatus.NO_ARROW;
		if (toolID >= 0)
			IJ.showStatus("selected " + getToolName() + " Tool(" + toolID + ")"); // DEBUG
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
				final double dist_to_line = distanceToLine(x, y);
				final double dist_to_arrowhead = distanceToArrowHead(x, y);
				final double dist_to_arrowbase = distanceToArrowBase(x, y);
				if (dist_to_arrowhead < DRAG_TOLERANCE) {
					status = InteractionStatus.DRAGGING_ARROW_HEAD;
				} else if (dist_to_arrowbase < DRAG_TOLERANCE) {
					status = InteractionStatus.DRAGGING_ARROW_BASE;
				} else if (dist_to_line < DRAG_TOLERANCE) {
					status = InteractionStatus.DRAGGING_LINE;
					start_drag_X = x;
					start_drag_Y = y;
				} else {
					status = InteractionStatus.FREE;
					start_X = x;          
					start_Y = y;					
				}
			}
	}
	

	public void handleMouseDrag(MouseEvent e) {
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		switch (status) {
		case DRAGGING_ARROW_HEAD:
		case FREE:
			end_X = x;        
			end_Y = y;
			break;
		case DRAGGING_ARROW_BASE:
			start_X =x;
			start_Y = y;
			break;
		case DRAGGING_LINE:
			final double dx = x-start_drag_X;
			final double dy = y-start_drag_Y;
			start_X += dx;
			start_Y += dy;
			end_X += dx;
			end_Y += dy;
			start_drag_X = x;
			start_drag_Y = y;
			break;
		}
		calculatePoints();
		makePath();	
		imp.setRoi(new ShapeRoi(arrow));
		IJ.showStatus(String.format("Dist to line: %.1f - Dist to head: %.1f - status: %s", 
				distanceToLine(x, y), distanceToArrowHead(x, y), status));		// DEBUG
	}
	
	public void handleMouseMove(MouseEvent e) {
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		IJ.showStatus(String.format("Dist to line: %.1f - Dist to head: %.1f - status: %s", 
				distanceToLine(x, y), distanceToArrowHead(x, y), status));		// DEBUG
	}
	
	public void handleMouseRelease(MouseEvent e) {
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		if  ( (status != InteractionStatus.DRAGGING_ARROW_BASE) && (Math.abs(start_X-x)< 1e-2) && (Math.abs(start_Y-y)< 1e-2) ) {
			// Released close to start: erase arrow
			arrow = new GeneralPath();
			imp.setRoi(new ShapeRoi(arrow));
			status = InteractionStatus.NO_ARROW;
		} else {
			status = InteractionStatus.FREE;
		}
	}
	
	/*
	 * OPTION PANEL
	 */
	
	@Override
	public boolean hasOptionDialog() {
		return true;
	}

	@Override
	public void showOptionDialog() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ArrowOptionPanel inst = new ArrowOptionPanel();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
		
	}


	/*
	 * PRIVATE METHODS
	 */
	
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
	
	/**
	 * Measure the distance to the line between coordinates start_X, start_Y and end_X, end_Y.
	 * If x, y is not in the space defined by the line segment, Inf is returned.
	 */
	private double distanceToLine(final double x, final double y) {
		final double Ax = x-start_X;
		final double Ay = y-start_Y;
		final double Bx = x-end_X;
		final double By = y-end_Y;
		final double Lx = end_X-start_X;
		final double Ly = end_Y-start_Y;
		final double al = Ax*Lx + Ay*Ly;
		final double bl = Bx*Lx + By*Ly;
		if ( (al<0) || (bl>0) ) {
			return Double.POSITIVE_INFINITY; // we are not within the segment space
		}
		final double a_square = Ax*Ax+Ay*Ay;
		final double b_square = Bx*Bx+By*By;
		final double l = Math.sqrt(Lx*Lx+Ly*Ly);
		final double h = Math.sqrt( a_square - al*al/l/l) + Math.sqrt( b_square - bl*bl/l/l);
		return h/2;
	}
	
	/**
	 * Returns the distance to the arrow head
	 */
	private double distanceToArrowHead(final double x, final double y) {
		final double dx = x-end_X;
		final double dy = y-end_Y;
		return Math.sqrt(dx*dx+dy*dy);
	}	
	
	/**
	 * Returns the distance to the arrow base
	 */
	private double distanceToArrowBase(final double x, final double y) {
		final double dx = x-start_X;
		final double dy = y-start_Y;
		return Math.sqrt(dx*dx+dy*dy);
	}


}
