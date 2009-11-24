package fiji.roi;
import java.awt.*;
import java.awt.geom.*;

/***
 * Defines an Arrow shape
 * 
 * @author Jean-Yves Tinevez, from debugging Mark Donszelmann initial work 
 */
public class Arrow implements Shape {

	private double points[];
	private double length;
	private double tip;
	private double base;
	private boolean open;

	private ArrowPathIterator pathIterator;

	public Arrow(Point2D start, Point2D end, double length, double tip, double base, boolean open) {
		this(start.getX(), start.getY(), end.getX(), end.getY(), length, tip, base, open);
	}

	public Arrow(double xStart, double yStart, double xEnd, double yEnd, double _length, double _tip, double _base, boolean _open) {
		points = new double[2*5];

		// P0
		points[0] = xStart;        
		points[1] = yStart;        

		// P3
		points[2*3] = xEnd;        
		points[2*3+1] = yEnd;        
		
		this.length = _length;
		this.base = Math.toRadians(_base);
		this.tip = Math.toRadians(_tip);
		this.open = _open;		

		pathIterator = new ArrowPathIterator(points);

		calculatePoints();
	}

	public boolean contains(double x, double y) {
		return new Area(this).contains(x, y);
	}

	public boolean contains(double x, double y, double w, double h) {
		return contains(x, y) && contains(x+w,y) &&
		contains(x,y+h) && contains(x+w,y+h);
	}

	public boolean contains(Point2D p) {
		return contains(p.getX(), p.getY());
	}

	public boolean contains(Rectangle2D r) {
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/*** Returns true, if at least one of the points is contained by the shape. */
	public boolean intersects(double x, double y, double w, double h) {
		return contains(x, y) || contains(x+w,y) ||
		contains(x,y+h) || contains(x+w,y+h);
	}

	public boolean intersects(Rectangle2D r) {
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return getPathIterator(at);
	}

	public Rectangle2D getBounds2D() {
		return new Area(this).getBounds2D();
	}

	public Rectangle getBounds() {
		return getBounds2D().getBounds();
	}

	public PathIterator getPathIterator(AffineTransform t) {
		if (t != null) {
			t.transform(points, 0, pathIterator.path_points, 0, points.length/2);
		} 
		pathIterator.reset();
		return pathIterator;
	}

	public String toString() {
		return getClass()+": ("+points[0]+","+points[1]+")-("+points[2*3]+","+points[2*3+1]+") length="+length+"; "
		+"tip="+Math.toDegrees(tip)+"; base="+Math.toDegrees(base)+"; open="+open+";";
	}
	
	
	
	/*
	 * SETTERS AND GETTERS
	 */
	
	public void setStartPoint(Point2D sp) {
		points[0] = sp.getX();        
		points[1] = sp.getY();  
		calculatePoints();
	}
	
	public void setStartPoint(double x, double y) {
		points[0] = x;        
		points[1] = y;  
		calculatePoints();		
	}
	
	
	
	public void setEndPoint(Point2D ep) {
		points[2*3] = ep.getX();        
		points[2*3+1] = ep.getY(); 
		calculatePoints();		
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
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
	

	/***
	 * Returns a straight line shape, followed by the arrowhead.
	 */
	
	private class ArrowPathIterator implements PathIterator {

		private double[] path_points;
		private int currentPoint = 0;

		private ArrowPathIterator(double[] _points) {
			this.path_points = _points;
		}

		public boolean isDone() {
			int nPoints = open ? path_points.length/2 : path_points.length/2+2;
			return currentPoint >= nPoints;
		}

		public void next() {
			currentPoint++;
		}

		public int currentSegment(double[] coords) {
			switch (currentPoint) {
			default:
				coords[0] = path_points[2*currentPoint];
				coords[1] = path_points[2*currentPoint+1];
				return ((currentPoint == 0) || (currentPoint == 2)) ? SEG_MOVETO : SEG_LINETO;
			case 5:
				coords[0] = path_points[2];
				coords[1] = path_points[3];                
				return SEG_LINETO;
			case 6:
				return SEG_CLOSE;
			}
		}

		public int currentSegment(float[] coords) {
			switch (currentPoint) {
			default:
				coords[0] = (float)path_points[2*currentPoint];
				coords[1] = (float)path_points[2*currentPoint+1];
				return ((currentPoint == 0) || (currentPoint == 2)) ? SEG_MOVETO : SEG_LINETO;
			case 5:
				coords[0] = (float)path_points[2];
				coords[1] = (float)path_points[3];                
				return SEG_LINETO;
			case 6:
				return SEG_CLOSE;
			}
		}

		public int getWindingRule() {
			return PathIterator.WIND_NON_ZERO;
		}

		private void reset() {
			currentPoint = 0;
		}

		private void done() {
			currentPoint = open ? path_points.length/2 : path_points.length/2+2;
		}
	}
}
