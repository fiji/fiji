package imagescience.shape;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.FMath;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

/** A circle in the x-y plane. */
public class Circle implements Shape {
	
	private double x = 0.0;
	private double y = 0.0;
	private double radius = 1.0;
	
	/** Default constructor. Results in a circle with center position at the origin and with unit radius. */
	public Circle() { }
	
	/** Constructs a circle with given center position and radius.
		
		@param x the x-coordinate of the center position of the circle.
		
		@param y the y-coordinate of the center position of the circle.
		
		@param radius the radius of the circle.
		
		@exception IllegalArgumentException if {@code radius} is less than {@code 0}.
	*/
	public Circle(final double x, final double y, final double radius) {
		
		if (radius < 0) throw new IllegalArgumentException("Radius less than 0");
		this.x = x;
		this.y = y;
		this.radius = radius;
	}
	
	/** Copy constructor.
		
		@param circle the circle to be copied. All information is copied and no memory is shared with this circle.
		
		@exception NullPointerException if {@code circle} is {@code null}.
	*/
	public Circle(final Circle circle) {
		
		this.x = circle.x;
		this.y = circle.y;
		this.radius = circle.radius;
	}
	
	/** Sets the x-coordinate of the center position of the circle.
		
		@param x the x-coordinate of the center position of the circle.
	*/
	public void x(final double x) { this.x = x; }
	
	/** Sets the y-coordinate of the center position of the circle.
		
		@param y the y-coordinate of the center position of the circle.
	*/
	public void y(final double y) { this.y = y; }
	
	/** Sets the center position of the circle. The same as method {@link #position(double,double)}.
		
		@param x the x-coordinate of the center position of the circle.
		
		@param y the y-coordinate of the center position of the circle.
	*/
	public void center(final double x, final double y) { this.x = x; this.y = y; }
	
	/** Sets the center position of the circle. The same as method {@link #center(double,double)}.
		
		@param x the x-coordinate of the center position of the circle.
		
		@param y the y-coordinate of the center position of the circle.
	*/
	public void position(final double x, final double y) { this.x = x; this.y = y; }
	
	/** Translates the circle over the given distance.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy) { this.x += dx; this.y += dy; }
	
	public Shape duplicate() { return new Circle(this); }
	
	/** Sets the center position and radius of the circle.
		
		@param x the x-coordinate of the center position of the circle.
		
		@param y the y-coordinate of the center position of the circle.
		
		@param radius the radius of the circle.
		
		@exception IllegalArgumentException if {@code radius} is less than {@code 0}.
	*/
	public void set(final double x, final double y, final double radius) {
		
		if (radius < 0) throw new IllegalArgumentException("Radius less than 0");
		this.x = x;
		this.y = y;
		this.radius = radius;
	}
	
	/** Sets the radius of the circle.
		
		@param radius the radius of the circle.
		
		@exception IllegalArgumentException if {@code radius} is less than {@code 0}.
	*/
	public void radius(final double radius) {
		
		if (radius < 0) throw new IllegalArgumentException("Radius less than 0");
		this.radius = radius;
	}
	
	/** Returns the x-coordinate of the center position of the circle.
		
		@return the x-coordinate of the center position of the circle.
	*/
	public double x() { return x; }
	
	/** Returns the y-coordinate of the center position of the circle.
		
		@return the y-coordinate of the center position of the circle.
	*/
	public double y() { return y; }
	
	/** Returns the radius of the circle.
		
		@return the radius of the circle.
	*/
	public double radius() { return radius; }
	
	/** Returns the perimeter of the circle.
		
		@return the perimeter of the circle.
	*/
	public double perimeter() {	return (2.0*Math.PI*radius); }
	
	/** Returns the area spanned by the circle.
		
		@return the area of the circle.
	*/
	public double area() { return (Math.PI*radius*radius); }
	
	/** Indicates the position of a point relative to the circle.
		
		@param point the point whose position relative to the circle is to be tested. The point is treated as a 2D point. That is, only its x- and y-coordinate values are considered.
		
		@return the value {@link #contains(double,double) contains(point.x,point.y)}.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public boolean contains(final Point point) { return contains(point.x,point.y); }
	
	/** Indicates the position of a point relative to the circle.
		
		@param x the x-coordinate of the point.
		
		@param y the y-coordinate of the point.
		
		@return {@code true} if the point is on or inside the circle; {@code false} if it is outside the circle.
	*/
	public boolean contains(final double x, final double y) {
		
		final double dx = x - this.x;
		final double dy = y - this.y;
		final double dis2 = dx*dx + dy*dy;
		final double rad2 = radius*radius;
		
		if (dis2 <= rad2) return true;
		else return false;
	}
	
	/** Returns the contour of the circle.
		
		@return a new {@code GeneralPath} object containing the contour of the circle.
	*/
	public GeneralPath contour() { return new GeneralPath(new Ellipse2D.Double(x-radius,y-radius,2*radius,2*radius)); }

	private static final int CONTOUR_RESOLUTION = 10; // Samples per dimension per image element

	public Image bitmap(final boolean binary) {
		
		// Initialize bitmap:
		final Bounds bounds = bounds();
		final int minx = FMath.floor(bounds.lower.x);
		final int miny = FMath.floor(bounds.lower.y);
		final int xdim = 1 + FMath.floor(bounds.upper.x) - minx;
		final int ydim = 1 + FMath.floor(bounds.upper.y) - miny;
		final ByteImage bitmap = new ByteImage(new Dimensions(xdim,ydim));
		
		if (radius > 0) {
			// Fill interior elements:
			final double xc = x - minx - 0.5;
			final double yc = y - miny - 0.5;
			final double r2 = radius*radius;
			final Coordinates c = new Coordinates();
			final double[] row = new double[xdim];
			bitmap.axes(Axes.X);
			for (int j=0; j<ydim; ++j, ++c.y) {
				final double dy = j - yc;
				final double dy2 = dy*dy;
				for (int i=0; i<xdim; ++i) {
					final double dx = i - xc;
					if (dx*dx + dy2 <= r2) row[i] = 255;
					else row[i] = 0;
				}
				bitmap.set(c,row);
			}
			// Fill contour elements:
			if (!binary) {
				final int contoursteps = FMath.ceil(CONTOUR_RESOLUTION*perimeter());
				final double contouranglestep = 2*Math.PI/contoursteps;
				final double[] samplesites = new double[CONTOUR_RESOLUTION];
				for (int i=0; i<CONTOUR_RESOLUTION; ++i) samplesites[i] = (i + 1.0)/(CONTOUR_RESOLUTION + 1.0);
				final double samplesites2 = CONTOUR_RESOLUTION*CONTOUR_RESOLUTION;
				final Coordinates pc = new Coordinates(Integer.MIN_VALUE,Integer.MIN_VALUE);
				final Coordinates cc = new Coordinates();
				final Coordinates bc = new Coordinates();
				for (int i=0; i<contoursteps; ++i) {
					final double contourangle = contouranglestep*i;
					cc.x = FMath.floor(x + radius*Math.cos(contourangle));
					cc.y = FMath.floor(y + radius*Math.sin(contourangle));
					if (cc.x != pc.x || cc.y != pc.y) {
						int hits = 0;
						for (int yi=0; yi<CONTOUR_RESOLUTION; ++yi) {
							final double dy = cc.y + samplesites[yi] - y;
							final double dy2 = dy*dy;
							for (int xi=0; xi<CONTOUR_RESOLUTION; ++xi) {
								final double dx = cc.x + samplesites[xi] - x;
								if (dx*dx + dy2 <= r2) ++hits;
							}
						}
						bc.x = cc.x - minx; bc.y = cc.y - miny;
						bitmap.set(bc,255*hits/samplesites2);
						pc.x = cc.x; pc.y = cc.y;
					}
				}
			}
		}
		
		return bitmap;
	}

	public Bounds bounds() {
		
		final Bounds bounds = new Bounds();
		bounds.lower.x = x - radius;
		bounds.upper.x = x + radius;
		bounds.lower.y = y - radius;
		bounds.upper.y = y + radius;
		
		return bounds;
	}

	public boolean equals(final Shape shape) {
		
		if (shape != null) {
			if (shape instanceof Circle) {
				final Circle circle = (Circle)shape;
				if (x == circle.x && y == circle.y && radius == circle.radius) return true;
			}
		}
		return false;
	}

	public boolean empty() {
		
		if (area() == 0) return true;
		return false;
	}
	
}
