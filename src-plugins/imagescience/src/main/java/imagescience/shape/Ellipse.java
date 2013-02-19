package imagescience.shape;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.FMath;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

/** An ellipse in the x-y plane. The convention for this class is that the "major radius" always applies to the x-axis of the ellipse (that is, the x-axis of the coordinate system after rotation over the "angle" of the ellipse), and the "minor radius" applies to the y-axis of the ellipse (that is, the y-axis of the coordinate system after rotation), regardless of whether the major radius is really the larger than the minor radius or not. The rotation angle of the ellipse is defined in terms of a right-handed coordinate system, meaning that a positive angle rotates points on the positive x-axis towards the positive y-axis. */
public class Ellipse implements Shape {
	
	private double x = 0.0;
	private double y = 0.0;
	private double major = 1.0;
	private double minor = 1.0;
	private double angle = 0.0;
	
	/** Default constructor. Results in an ellipse with center position at the origin, unit radii, and zero angle. That is, a unit circle. */
	public Ellipse() { }
	
	/** Constructs an ellipse with given center position, major and minor radii, and angle.
		
		@param x the x-coordinate of the center position of the ellipse.
		
		@param y the y-coordinate of the center position of the ellipse.
		
		@param major the major radius of the ellipse.
		
		@param minor the minor radius of the ellipse.
		
		@param angle the angle (in radians) of the ellipse.
		
		@exception IllegalArgumentException if {@code major} or {@code minor} is less than {@code 0}.
	*/
	public Ellipse(final double x, final double y, final double major, final double minor, final double angle) {
		
		if (major < 0) throw new IllegalArgumentException("Major radius less than 0");
		if (minor < 0) throw new IllegalArgumentException("Minor radius less than 0");
		this.x = x;
		this.y = y;
		this.major = major;
		this.minor = minor;
		this.angle = angle;
	}
	
	/** Copy constructor.
		
		@param ellipse the ellipse to be copied. All information is copied and no memory is shared with this ellipse.
		
		@exception NullPointerException if {@code ellipse} is {@code null}.
	*/
	public Ellipse(final Ellipse ellipse) {
		
		this.x = ellipse.x;
		this.y = ellipse.y;
		this.major = ellipse.major;
		this.minor = ellipse.minor;
		this.angle = ellipse.angle;
	}
	
	/** Sets the x-coordinate of the center position of the ellipse.
		
		@param x the x-coordinate of the center position of the ellipse.
	*/
	public void x(final double x) { this.x = x; }
	
	/** Returns the x-coordinate of the center position of the ellipse.
		
		@return the x-coordinate of the center position of the ellipse.
	*/
	public double x() { return x; }
	
	/** Sets the y-coordinate of the center position of the ellipse.
		
		@param y the y-coordinate of the center position of the ellipse.
	*/
	public void y(final double y) { this.y = y; }
	
	/** Returns the y-coordinate of the center position of the ellipse.
		
		@return the y-coordinate of the center position of the ellipse.
	*/
	public double y() { return y; }
	
	/** Sets the center position of the ellipse. The same as method {@link #position(double,double)}.
		
		@param x the x-coordinate of the center position of the ellipse.
		
		@param y the y-coordinate of the center position of the ellipse.
	*/
	public void center(final double x, final double y) {
		
		this.x = x;
		this.y = y;
	}
	
	/** Sets the center position of the ellipse. The same as method {@link #center(double,double)}.
		
		@param x the x-coordinate of the center position of the ellipse.
		
		@param y the y-coordinate of the center position of the ellipse.
	*/
	public void position(final double x, final double y) {
		
		this.x = x;
		this.y = y;
	}
	
	/** Sets the major radius of the ellipse.
		
		@param major the major radius of the ellipse.
		
		@exception IllegalArgumentException if {@code major} is less than {@code 0}.
	*/
	public void major(final double major) {
		
		if (major < 0) throw new IllegalArgumentException("Major radius less than 0");
		this.major = major;
	}
	
	/** Returns the major radius of the ellipse.
		
		@return the major radius of the ellipse.
	*/
	public double major() { return major; }
	
	/** Sets the minor radius of the ellipse.
		
		@param minor the minor radius of the ellipse.
		
		@exception IllegalArgumentException if {@code minor} is less than {@code 0}.
	*/
	public void minor(final double minor) {
		
		if (minor < 0) throw new IllegalArgumentException("Minor radius less than 0");
		this.minor = minor;
	}
	
	/** Returns the minor radius of the ellipse.
		
		@return the minor radius of the ellipse.
	*/
	public double minor() { return minor; }
	
	/** Sets the radii of the ellipse.
		
		@param major the major radius of the ellipse.
		
		@param minor the minor radius of the ellipse.
		
		@exception IllegalArgumentException if {@code major} or {@code minor} is less than {@code 0}.
	*/
	public void radii(final double major, final double minor) {
		
		if (major < 0) throw new IllegalArgumentException("Major radius less than 0");
		if (minor < 0) throw new IllegalArgumentException("Minor radius less than 0");
		this.major = major;
		this.minor = minor;
	}
	
	/** Sets the angle of the ellipse.
		
		@param angle the angle of the ellipse.
	*/
	public void angle(final double angle) { this.angle = angle; }
	
	/** Returns the angle of the ellipse.
		
		@return the angle of the ellipse.
	*/
	public double angle() { return angle; }
	
	/** Translates the ellipse over the given distance.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy) {
		
		this.x += dx;
		this.y += dy;
	}
	
	/** Increases the angle of the ellipse by the given amount.
		
		@param angle the angle (in radians) over which to rotate the ellipse.
	*/
	public void rotate(final double angle) {
		
		this.angle += angle;
	}
	
	public Shape duplicate() { return new Ellipse(this); }
	
	/** Sets the center position, major and minor radii, and the angle of the ellipse.
		
		@param x the x-coordinate of the center position of the ellipse.
		
		@param y the y-coordinate of the center position of the ellipse.
		
		@param major the major radius of the ellipse.
		
		@param minor the minor radius of the ellipse.
		
		@param angle the angle (in radians) of the ellipse.
		
		@exception IllegalArgumentException if {@code major} or {@code minor} is less than {@code 0}.
	*/
	public void set(final double x, final double y, final double major, final double minor, final double angle) {
		
		if (major < 0) throw new IllegalArgumentException("Major radius less than 0");
		if (minor < 0) throw new IllegalArgumentException("Minor radius less than 0");
		this.x = x;
		this.y = y;
		this.major = major;
		this.minor = minor;
		this.angle = angle;
	}
	
	/** Returns the eccentricity of the ellipse.
		
		@return the eccentricity of the ellipse. The returned value is always in the range {@code [0,1)}, with {@code 0} corresponding to a circle (including the degenerate case of a point), and {@code 1} to a line (completely flat ellipse).
	*/
	public double eccentricity() {
		
		double max = major;
		double min = minor;
		if (minor > major) {
			max = minor;
			min = major;
		}
		if (max == 0) return 0;
		return Math.sqrt(1 - (min*min)/(max*max));
	}
	
	/** Returns the perimeter of the ellipse.
		
		@return the perimeter of the ellipse. The perimeter is approximated using the second formula of S. Ramanujan, "Modular Equations and Approximations to Pi", Quarterly Journal of Pure and Applied Mathematics, vol. 45, 1914, pp. 350-372.
	*/
	public double perimeter() {
		
		final double xpy = major + minor;
		final double xmy = major - minor;
		if (xpy == 0) return 0;
		final double h = xmy*xmy/(xpy*xpy);
		return Math.PI*xpy*(1 + 3*h/(10 + Math.sqrt(4 - 3*h)));
	}
	
	/** Returns the area spanned by the ellipse.
		
		@return the area spanned by the ellipse.
	*/
	public double area() { return (Math.PI*major*minor); }
	
	/** Indicates the position of a point relative to the ellipse.
		
		@param point the point whose position relative to the ellipse is to be tested. The point is treated as a 2D point. That is, only its x- and y-coordinate values are considered.
		
		@return the value {@link #contains(double,double) contains(point.x,point.y)}.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public boolean contains(final Point point) { return contains(point.x,point.y); }
	
	/** Indicates the position of a point relative to the ellipse.
		
		@param x the x-coordinate of the point.
		
		@param y the y-coordinate of the point.
		
		@return {@code true} if the point is on or inside the ellipse; {@code false} if it is outside the ellipse.
	*/
	public boolean contains(final double x, final double y) {
		
		final double dx = x - this.x;
		final double dy = y - this.y;
		final double sina = Math.sin(angle);
		final double cosa = Math.cos(angle);
		final double dxr = cosa*dx + sina*dy;
		final double dyr = cosa*dy - sina*dx;
		if (major > 0 && minor > 0) return (dxr*dxr/(major*major) + dyr*dyr/(minor*minor) <= 1);
		else if (major > 0) return (dyr == 0 && dxr*dxr <= major*major);
		else if (minor > 0) return (dxr == 0 && dyr*dyr <= minor*minor);
		return (dxr == 0 && dyr == 0);
	}
	
	/** Returns the contour of the ellipse.
		
		@return a new {@code GeneralPath} object containing the contour of the ellipse.
	*/
	public GeneralPath contour() {
		
		final GeneralPath gp = new GeneralPath(new Ellipse2D.Double(x-major,y-minor,2*major,2*minor));
		final AffineTransform at = new AffineTransform();
		at.rotate(angle,x,y);
		gp.transform(at);
		return gp;
	}
	
	private static final int CONTOUR_RESOLUTION = 10; // Samples per dimension per image element
	
	public Image bitmap(final boolean binary) {
		
		// Initialize bitmap:
		final Bounds bounds = bounds();
		final int minx = FMath.floor(bounds.lower.x);
		final int miny = FMath.floor(bounds.lower.y);
		final int xdim = 1 + FMath.floor(bounds.upper.x) - minx;
		final int ydim = 1 + FMath.floor(bounds.upper.y) - miny;
		final ByteImage bitmap = new ByteImage(new Dimensions(xdim,ydim));
		
		if (major > 0 && minor > 0) {
			// Fill interior elements:
			final double sina = Math.sin(angle);
			final double cosa = Math.cos(angle);
			final double major2 = major*major;
			final double minor2 = minor*minor;
			final double xc = x - minx - 0.5;
			final double yc = y - miny - 0.5;
			final Coordinates c = new Coordinates();
			final double[] row = new double[xdim];
			bitmap.axes(Axes.X);
			for (int j=0; j<ydim; ++j, ++c.y) {
				final double dy = j - yc;
				final double sinady = sina*dy;
				final double cosady = cosa*dy;
				for (int i=0; i<xdim; ++i) {
					final double dx = i - xc;
					final double dxr = cosa*dx + sinady;
					final double dyr = cosady - sina*dx;
					if (dxr*dxr/major2 + dyr*dyr/minor2 <= 1) row[i] = 255;
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
					final double cx = major*Math.cos(contourangle);
					final double cy = minor*Math.sin(contourangle);
					cc.x = FMath.floor(x + cosa*cx - sina*cy);
					cc.y = FMath.floor(y + sina*cx + cosa*cy);
					if (cc.x != pc.x || cc.y != pc.y) {
						int hits = 0;
						for (int yi=0; yi<CONTOUR_RESOLUTION; ++yi) {
							final double dy = cc.y + samplesites[yi] - y;
							for (int xi=0; xi<CONTOUR_RESOLUTION; ++xi) {
								final double dx = cc.x + samplesites[xi] - x;
								final double dxr = cosa*dx + sina*dy;
								final double dyr = cosa*dy - sina*dx;
								if (dxr*dxr/major2 + dyr*dyr/minor2 <= 1) ++hits;
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
		
		double mx0 = 0, my0 = 0;
		if (angle == 0 || angle == Math.PI) {
			mx0 = major;
			my0 = minor;
		} else if (major == 0) {
			mx0 = minor*Math.sin(angle);
			my0 = minor*Math.cos(angle);
		} else if (minor == 0) {
			mx0 = major*Math.cos(angle);
			my0 = major*Math.sin(angle);
		} else {
			final double tan = Math.tan(angle);
			final double tx = Math.atan(-minor*tan/major);
			final double ty = Math.atan(minor/(tan*major));
			final double sina = Math.sin(angle);
			final double cosa = Math.cos(angle);
			mx0 = major*cosa*Math.cos(tx) - minor*sina*Math.sin(tx);
			my0 = major*sina*Math.cos(ty) + minor*cosa*Math.sin(ty);
		}
		if (mx0 < 0) mx0 = -mx0;
		if (my0 < 0) my0 = -my0;
		
		final Bounds bounds = new Bounds();
		bounds.lower.x = x - mx0;
		bounds.lower.y = y - my0;
		bounds.upper.x = x + mx0;
		bounds.upper.y = y + my0;
		
		return bounds;
	}
	
	public boolean equals(final Shape shape) {
		
		if (shape != null) {
			if (shape instanceof Ellipse) {
				final Ellipse ellipse = (Ellipse)shape;
				if (x == ellipse.x && y == ellipse.y && major == ellipse.major && minor == ellipse.minor && angle == ellipse.angle) return true;
			}
		}
		
		return false;
	}
	
	public boolean empty() {
		
		if (area() == 0) return true;
		return false;
	}
	
}
