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
public class ArrowShape implements Shape {
	
	public static enum ArrowStyle {
	DELTA 	,
	THICK   ,
	THIN 	,
	CIRCLE  ;
	}
	
	private GeneralPath path = new GeneralPath();
	private Point2D start, end;
	/** Style of the arrow	 */
	private ArrowStyle style;
	/** Length of the arrow head, in pixels. */
	private double length;
	/** Points coordinates for DELTA, THICK and THIN styles.  */
	private float[] points = new float[2*5];

	/*
	 * CONSTRUCTORS
	 */
	
	public ArrowShape() {
		this(ArrowStyle.DELTA);
	}
	
	public ArrowShape(ArrowStyle _style) {
		this(_style, 10.0);
	}
	
	public ArrowShape(ArrowStyle _style, double _length) {
		style = _style;
		length  = _length;
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Return a new Arrow object, with identical properties that of the caller.
	 */
	public ArrowShape clone() {
		ArrowShape new_arrow = new ArrowShape(style, length);
		new_arrow.setStartPoint(start);
		new_arrow.setEndPoint(end);
		return new_arrow;
	}
	
	public String toString() {
		return String.format("ArrowShape: start=(%.1f,%.1f), end=(%.1f,%.1f), style=%s, length=%.0f", 
				start.getX(), start.getY(), end.getX(), end.getY(), style, length);
	}
		
	/*
	 * SETTERS AND GETTERS
	 */

	public void setStartPoint(Point2D start) { 		this.start = start;	}
	public Point2D getStartPoint() {		return start; 	}
	public void setEndPoint(Point2D end) {		this.end = end; 	}
	public Point2D getEndPoint() { 		return end; 	}
	public void setLength(double _length) { this.length = _length; }
	public double getLength() { return length; }
	public ArrowStyle getStyle() { return style; }
	public void setStyle(ArrowStyle _style) { this.style = _style; }
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private Shape getPath() {
		path.reset();
		if ( (start != null) || ( end != null) ) {
			switch (style) {
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
		double tip = 0.0;
		double base;
		// Start and end point
		points[0] = (float) start.getX();
		points[1] = (float) start.getY();
		points[2*3] = (float) end.getX();
		points[2*3+1] = (float) end.getY();
		final double alpha = Math.atan2(points[2*3+1] - points[1], points[2*3] - points[0]);
		double SL = 0.0;
		switch (style) {
		case DELTA:
			tip = Math.toRadians(20.0);
			base = Math.toRadians(90.0);
			points[1*2]   = (float) (points[2*3]   - length*Math.cos(alpha));
			points[1*2+1] = (float) (points[2*3+1] - length*Math.sin(alpha));
			SL = length * Math.sin(base) / Math.sin(base+tip);;
			break;
		case THICK:
			tip = Math.toRadians(20);
			base = Math.toRadians(120);
			points[1*2]   = (float) (points[2*3]   - length*Math.cos(alpha));
			points[1*2+1] = (float) (points[2*3+1] - length*Math.sin(alpha));
			SL = length * Math.sin(base) / Math.sin(base+tip);;
			break;
		case THIN:
			tip = Math.toRadians(30);
			points[1*2]   = points[2*3];
			points[1*2+1] = points[2*3+1];
			SL = length;
		}
		// P2 = P3 - SL*alpha+tip
		points[2*2]   = (float) (points[2*3]   - SL*Math.cos(alpha+tip));
		points[2*2+1] = (float) (points[2*3+1] - SL*Math.sin(alpha+tip));
		// P4 = P3 - SL*alpha-tip
		points[2*4]   = (float) (points[2*3]   - SL*Math.cos(alpha-tip));
		points[2*4+1] = (float) (points[2*3+1] - SL*Math.sin(alpha-tip));		
	}
	
	/**
	 * Calculate path from the point coordinates store in instance field.
	 */
	private void getPathFromPoints() {
		path.moveTo(points[0], points[1]); // tail
		path.lineTo(points[2 * 1], points[2 * 1 + 1]); // head back
		path.moveTo(points[2 * 1], points[2 * 1 + 1]); // head back
		if (style == ArrowStyle.THIN) {
			path.moveTo(points[2 * 2], points[2 * 2 + 1]);
		} else {			
			path.lineTo(points[2 * 2], points[2 * 2 + 1]); // left point
		}
		path.lineTo(points[2 * 3], points[2 * 3 + 1]); // head tip
		path.lineTo(points[2 * 4], points[2 * 4 + 1]); // right point
		path.lineTo(points[2 * 1], points[2 * 1 + 1]); // back to the head back
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
