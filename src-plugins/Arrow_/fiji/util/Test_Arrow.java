package fiji.util;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ShapeRoi;

import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

public class Test_Arrow extends fiji.util.AbstractTool implements ActionListener {
	
	/**
	 * How close we have to be from control points to drag them.
	 */
	private final static double DRAG_TOLERANCE = 5.0;
	
	private Arrow arrow;
	private BasicStroke stroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private ShapeRoi roi;
	/**
	 * End and start point coordinates of the arrow.
	 */
	private double start_X, start_Y, end_X, end_Y;
	/**
	 * To monitor how much we drag for user interaction.
	 */
	private double start_drag_X, start_drag_Y;

	private ImagePlus imp;
	private ImageCanvas canvas;
	private enum InteractionStatus { NO_ARROW, FREE, DRAGGING_ARROW_HEAD, DRAGGING_ARROW_BASE, DRAGGING_LINE};
	private InteractionStatus status;
	
	
	public void run(String arg) {
		super.run(arg);
		imp = WindowManager.getCurrentImage();
		canvas = imp.getCanvas();
		arrow = new Arrow();
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
		arrow.setStartPoint(new Point2D.Double(start_X,start_Y));
		arrow.setEndPoint(new Point2D.Double(end_X,end_Y));
		paint();
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
			imp.killRoi();
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
		final ActionListener current_instance = this;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ArrowOptionPanel inst;
				if (status == InteractionStatus.NO_ARROW) {
					inst = new ArrowOptionPanel();
				} else {
					inst = new ArrowOptionPanel(arrow, stroke);
				}
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
				inst.addActionListener(current_instance);
			}
		});
		
	}
		
	public void actionPerformed(ActionEvent e) {
		ArrowOptionPanel panel = (ArrowOptionPanel) e.getSource();
		arrow.setLength(panel.getLength());
		arrow.setStyle(panel.getStyle());
		stroke = panel.getStroke();
		paint();
	}


	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Paint the arrow roi.
	 */
	private void paint() {
		if (status != InteractionStatus.NO_ARROW) {
			roi = new ShapeRoi(arrow);
			roi.setStroke(stroke);
			imp.setRoi(roi);
		}
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
