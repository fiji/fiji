package fiji.util;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * 
 */

/**
 * @author Jean-Yves Tinevez 
 *
 */
public enum Arrow implements Shape {
	DELTA 	( 	10, 	20, 	90),
	THICK   ( 	10, 	20,		120),
	THIN 	( 	10, 	30,		30),
	CIRCLE  ( 	10, 	Double.NaN, Double.NaN);
	
	private GeneralPath path = new GeneralPath();
	private Point2D start, end;
	/**
	 * Length of the arrow head, in pixels.
	 */
	private double length;
	/**
	 * Tip angle (in degrees) of the arrow head.
	 */
	private double tip;
	/**
	 * Base angle (in degrees) of the arrow head.
	 */
	private double base;

	private double[] points = new double[2*5];

	/*
	 * CONSTRUCTOR
	 */
	
	private Arrow(double _length, double _tip, double _base) {
		length  = _length;
		tip = Math.toRadians(_tip); // We store in radians
		base = Math.toRadians(_base);
	}
		
	
	

	/*
	 * SETTERS AND GETTERS
	 */

	public void setStartPoint(Point2D start) { 		this.start = start;	}
	public Point2D getStartPoint() {		return start; 	}
	public void setEndPoint(Point2D end) {		this.end = end; 	}
	public Point2D getEndPoint() { 		return end; 	}
	public void setLength(double _length) { this.length = _length; }
	
	/*
	 * PRIVATE METHODS
	 */
	
	private Shape getPath() {
		path.reset();
		if ( (start != null) || ( end != null) ) {
			switch (this) {
			case DELTA:
			case THICK:
			case THIN:
				calculatePoints();
				getPathFromPoints();
				break;
			case CIRCLE:
			{
				final double y = end.getY() - start.getY();
				final double x = end.getX() - start.getX();
				final double alpha = Math.atan2(y, x);
				final double end_x = start.getX()+x-length/2*Math.cos(alpha);
				final double end_y = start.getY()+y-length/2*Math.sin(alpha);
				path.append(new Line2D.Double(start.getX(), start.getY(), end_x, end_y).getPathIterator(null), false);
				Ellipse2D circle = new Ellipse2D.Double(end.getX()-length/2, end.getY()-length/2, length, length); 
				path.append(circle, false);
			}
			}	
		}
		return path;
	}
	
	/**
	 * Computes the coordinates of the arrow point, and updates the field points with them.
	 */
	private void calculatePoints() {
		// Start and end point
		points[0] = start.getX();
		points[1] = start.getY();
		points[2*3] = end.getX();
		points[2*3+1] = end.getY();
		final double alpha = Math.atan2(points[2*3+1] - points[1], points[2*3] - points[0]);
		double SL = 0;
		// P1 = P3 - length*alpha
		switch (this) {
		case DELTA:
		case THICK:
			points[1*2]   = points[2*3]   - length*Math.cos(alpha);
			points[1*2+1] = points[2*3+1] - length*Math.sin(alpha);
			SL = length * Math.sin(base) / Math.sin(base+tip);;
			break;
		case THIN:
			points[1*2]   = points[2*3];
			points[1*2+1] = points[2*3+1];
			SL = length;
		}
		// P2 = P3 - SL*alpha+tip
		points[2*2]   = points[2*3]   - SL*Math.cos(alpha+tip);
		points[2*2+1] = points[2*3+1] - SL*Math.sin(alpha+tip);
		// P4 = P3 - SL*alpha-tip
		points[2*4]   = points[2*3]   - SL*Math.cos(alpha-tip);
		points[2*4+1] = points[2*3+1] - SL*Math.sin(alpha-tip);		
	}
	
	/**
	 * Calculate path from the point coordinates store in instance field.
	 */
	private void getPathFromPoints() {
		path.moveTo(points[0], points[1]); // tail
		path.lineTo(points[2 * 1], points[2 * 1 + 1]); // head back
		path.lineTo(points[2 * 2], points[2 * 2 + 1]); // left point
		path.lineTo(points[2 * 3], points[2 * 3 + 1]); // head tip
		path.lineTo(points[2 * 4], points[2 * 4 + 1]); // right point
		path.lineTo(points[2 * 1], points[2 * 1 + 1]); // back to the head back
	}
	
	/*
	 * STATIC METHODS
	 */
	
	public static String[] getStyleStrings() {
		final Arrow[] styles = Arrow.values();
		String[] list = new String[styles.length];
		for (int i = 0; i < list.length; i++) {	list[i] = styles[i].name();	}
		return list;
	}


	/*
	 * SHAPE METHODS
	 */

	public boolean contains(Point2D p) {
		return getPath().contains(p);
	}



	public boolean contains(Rectangle2D r) {
		return getPath().contains(r);
	}



	public boolean contains(double x, double y) {
		return getPath().contains(x, y);
	}

	public boolean contains(double x, double y, double w, double h) {
		return getPath().contains(x, y, w, h);
	}


	public Rectangle getBounds() {
		return getPath().getBounds();
	}

	public Rectangle2D getBounds2D() {
		return getPath().getBounds2D();
	}

	public PathIterator getPathIterator(AffineTransform at) {
		return getPath().getPathIterator(at);
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return getPath().getPathIterator(at, flatness);
	}

	public boolean intersects(Rectangle2D r) {
		return getPath().intersects(r);
	}


	public boolean intersects(double x, double y, double w, double h) {
		return getPath().intersects(x, y, w, h);
	}
}
