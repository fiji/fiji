package imagescience.shape;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.FMath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;

/** A rectangle in the x-y plane. */
public class Rectangle implements Shape {
	
	private double x = 0.0;
	private double y = 0.0;
	private double width = 1.0;
	private double height = 1.0;
	
	/** Default constructor. Results in a rectangle with top-left corner at the origin and with unit width and height. */
	public Rectangle() { }

	/** Constructs a rectangle with given top-left corner position, width and height.
		
		@param x the x-coordinate of the top-left position of the rectangle.
		
		@param y the y-coordinate of the top-left position of the rectangle.
		
		@param width the width of the rectangle.
		
		@param height the height of the rectangle.
		
		@exception IllegalArgumentException if {@code width} or {@code height} is less than {@code 0}.
	*/
	public Rectangle(final double x, final double y, final double width, final double height) {
		
		if (width < 0) throw new IllegalArgumentException("Width less than 0");
		if (height < 0) throw new IllegalArgumentException("Height less than 0");
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	/** Copy constructor.
		
		@param rectangle the rectangle to be copied. All information is copied and no memory is shared with this rectangle.
		
		@exception NullPointerException if {@code rectangle} is {@code null}.
	*/
	public Rectangle(final Rectangle rectangle) {
		
		this.x = rectangle.x;
		this.y = rectangle.y;
		this.width = rectangle.width;
		this.height = rectangle.height;
	}
	
	/** Sets the x-coordinate of the top-left corner of the rectangle.
		
		@param x the x-coordinate of the top-left position of the rectangle.
	*/
	public void x(final double x) { this.x = x; }
	
	/** Sets the y-coordinate of the top-left corner of the rectangle.
		
		@param y the y-coordinate of the top-left position of the rectangle.
	*/
	public void y(final double y) { this.y = y; }
	
	/** Sets the width of the rectangle.
		
		@param width the width of the rectangle.
		
		@exception IllegalArgumentException if {@code width} is less than {@code 0}.
	*/
	public void width(final double width) {
		
		if (width < 0) throw new IllegalArgumentException("Width less than 0");
		this.width = width;
	}
	
	/** Sets the height of the rectangle.
		
		@param height the height of the rectangle.
		
		@exception IllegalArgumentException if {@code height} is less than {@code 0}.
	*/
	public void height(final double height) {
		
		if (height < 0) throw new IllegalArgumentException("Height less than 0");
		this.height = height;
	}
	
	/** Returns the x-coordinate of the top-left corner of the rectangle.
		
		@return the x-coordinate of the top-left corner of the rectangle.
	*/
	public double x() { return x; }
	
	/** Returns the y-coordinate of the top-left corner of the rectangle.
		
		@return the y-coordinate of the top-left corner of the rectangle.
	*/
	public double y() { return y; }
	
	/** Returns the width of the rectangle.
		
		@return the width of the rectangle.
	*/
	public double width() { return width; }

	/** Returns the height of the rectangle.
		
		@return the height of the rectangle.
	*/
	public double height() { return height; }
	
	/** Returns the perimeter of the rectangle.
		
		@return the perimeter of the rectangle.
	*/
	public double perimeter() {	return 2*(width + height); }

	/** Returns the area spanned by the rectangle.
		
		@return the area spanned by the rectangle.
	*/
	public double area() { return (width*height); }

	/** Sets the top-left corner position of the rectangle.
		
		@param x the x-coordinate of the top-left position of the rectangle.
		
		@param y the y-coordinate of the top-left position of the rectangle.
	*/
	public void position(final double x, final double y) { this.x = x; this.y = y; }

	/** Translates the rectangle over the given distance.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy) { this.x += dx; this.y += dy; }

	public Shape duplicate() { return new Rectangle(this); }

	/** Sets the top-left corner position and size of the rectangle.
		
		@param x the x-coordinate of the top-left position of the rectangle.
		
		@param y the y-coordinate of the top-left position of the rectangle.
		
		@param width the width of the rectangle.
		
		@param height the height of the rectangle.
		
		@exception IllegalArgumentException if {@code width} or {@code height} is less than {@code 0}.
	*/
	public void set(final double x, final double y, final double width, final double height) {
		
		if (width < 0) throw new IllegalArgumentException("Width less than 0");
		if (height < 0) throw new IllegalArgumentException("Height less than 0");
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	/** Sets the size of the rectangle.
		
		@param width the width of the rectangle.
		
		@param height the height of the rectangle.
		
		@exception IllegalArgumentException if {@code width} or {@code height} is less than {@code 0}.
	*/
	public void size(final double width, final double height) {
		
		if (width < 0) throw new IllegalArgumentException("Width less than 0");
		if (height < 0) throw new IllegalArgumentException("Height less than 0");
		this.width = width;
		this.height = height;
	}
	
	/** Indicates the position of a point relative to the rectangle.
		
		@param point the point whose position relative to the rectangle is to be tested. The point is treated as a 2D point. That is, only its x- and y-coordinate values are considered.
		
		@return the value {@link #contains(double,double) contains(point.x,point.y)}.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public boolean contains(final Point point) { return contains(point.x,point.y); }

	/** Indicates the position of a point relative to the rectangle.
		
		@param x the x-coordinate of the point.
		
		@param y the y-coordinate of the point.
		
		@return {@code true} if the point is on or inside the rectangle; {@code false} if it is outside the rectangle.
	*/
	public boolean contains(final double x, final double y) {
		
		final double dx = x - this.x;
		final double dy = y - this.y;
		
		if (dx >= 0 && dx <= width && dy >= 0 && dy <= height) return true;
		else return false;
	}
	
	/** Returns the contour of the rectangle.
		
		@return a new {@code GeneralPath} object containing the contour of the rectangle.
	*/
	public GeneralPath contour() { return new GeneralPath(new Rectangle2D.Double(x,y,width,height)); }
	
	public Image bitmap(final boolean binary) {
		
		// Initialize bitmap:
		final double xpw = x + width;
		final double yph = y + height;
		final int minx = FMath.floor(x);
		final int miny = FMath.floor(y);
		final int xdim = 1 + FMath.floor(xpw) - minx;
		final int ydim = 1 + FMath.floor(yph) - miny;
		final ByteImage bitmap = new ByteImage(new Dimensions(xdim,ydim));
		
		if (width > 0 && height > 0) {
			
			// Initialize variables:
			final int xis = FMath.ceil(x);
			final int yis = FMath.ceil(y);
			final int xie = FMath.floor(xpw);
			final int yie = FMath.floor(yph);
			final double tfrac = FMath.min(miny+1,yph) - y;
			final double bfrac = yph - FMath.max(yie,y);
			final double lfrac = FMath.min(minx+1,xpw) - x;
			final double rfrac = xpw - FMath.max(xie,x);
			final Coordinates c = new Coordinates();
			double val = 0;
			
			// Fill horizontal elements:
			if (xie > xis) {
				
				// Fill interior elements:
				final double[] row = new double[xdim];
				for (int xi=xis, i=xis-minx; xi<xie; ++xi, ++i) row[i] = 255;
				c.y = yis - miny; bitmap.axes(Axes.X);
				for (int yi=yis; yi<yie; ++yi, ++c.y) bitmap.set(c,row);
				
				// Fill top elements:
				if (y > miny) {
					val = 255*tfrac;
					if (binary) val = (tfrac >= 0.5) ? 255 : 0;
					for (int xi=xis, i=xis-minx; xi<xie; ++xi, ++i) row[i] = val;
					c.y = 0; bitmap.set(c,row);
				}
				// Fill bottom elements:
				if (yph > yie) {
					val = 255*bfrac;
					if (binary) val = (bfrac >= 0.5) ? 255 : 0;
					for (int xi=xis, i=xis-minx; xi<xie; ++xi, ++i) row[i] = val;
					c.y = yie - miny; bitmap.set(c,row);
				}
			}
			
			// Fill vertical elements:
			if (yie > yis) {
				
				// Initialize column variables:
				final double[] col = new double[ydim];
				c.y = 0; bitmap.axes(Axes.Y);
				
				// Fill left elements:
				if (x > minx) {
					val = 255*lfrac;
					if (binary) val = (lfrac >= 0.5) ? 255 : 0;
					for (int yi=yis, j=yis-miny; yi<yie; ++yi, ++j) col[j] = val;
					c.x = 0; bitmap.set(c,col);
				}
				// Fill right elements:
				if (xpw > xie) {
					val = 255*rfrac;
					if (binary) val = (rfrac >= 0.5) ? 255 : 0;
					for (int yi=yis, j=yis-miny; yi<yie; ++yi, ++j) col[j] = val;
					c.x = xie - minx; bitmap.set(c,col);
				}
				bitmap.axes(Axes.X); // Reset axes
			}
			
			// Fill corner elements:
			c.x = c.y = 0;
			val = 255*tfrac*lfrac;
			if (binary) val = (tfrac*lfrac >= 0.5) ? 255 : 0;
			bitmap.set(c,val);
			
			c.y = yie - miny;
			val = 255*bfrac*lfrac;
			if (binary) val = (bfrac*lfrac >= 0.5) ? 255 : 0;
			bitmap.set(c,val);
			
			c.x = xie - minx;
			val = 255*bfrac*rfrac;
			if (binary) val = (bfrac*rfrac >= 0.5) ? 255 : 0;
			bitmap.set(c,val);
			
			c.y = 0;
			val = 255*tfrac*rfrac;
			if (binary) val = (tfrac*rfrac >= 0.5) ? 255 : 0;
			bitmap.set(c,val);
		}
		
		return bitmap;
	}
	
	public Bounds bounds() {
		
		final Bounds bounds = new Bounds();
		bounds.lower.x = x;
		bounds.upper.x = x + width;
		bounds.lower.y = y;
		bounds.upper.y = y + height;
		
		return bounds;
	}
	
	public boolean equals(final Shape shape) {
		
		if (shape != null) {
			if (shape instanceof Rectangle) {
				final Rectangle rectangle = (Rectangle)shape;
				if (x == rectangle.x && y == rectangle.y && width == rectangle.width && height == rectangle.height) return true;
			}
		}
		
		return false;
	}
	
	public boolean empty() {
		
		if (area() == 0) return true;
		return false;
	}
	
}
