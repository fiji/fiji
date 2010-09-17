package imagescience.shape;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.FMath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;

/** A square in the x-y plane. */
public class Square implements Shape {
	
	private double x = 0.0;
	private double y = 0.0;
	private double edge = 1.0;
	
	/** Default constructor. Results in a square with top-left position at the origin and with unit edge length. */
	public Square() { }
	
	/** Constructs a square with top-left position and edge length.
		
		@param x the x-coordinate of the top-left position of the square.
		
		@param y the y-coordinate of the top-left position of the square.
		
		@param edge the edge length of the square.
		
		@exception IllegalArgumentException if {@code edge} is less than {@code 0}.
	*/
	public Square(final double x, final double y, final double edge) {
		
		if (edge < 0) throw new IllegalArgumentException("Edge length less than 0");
		this.x = x;
		this.y = y;
		this.edge = edge;
	}
	
	/** Copy constructor.
		
		@param square the square to be copied. All information is copied and no memory is shared with this square.
		
		@exception NullPointerException if {@code square} is {@code null}.
	*/
	public Square(final Square square) {
		
		this.x = square.x;
		this.y = square.y;
		this.edge = square.edge;
	}
	
	/** Sets the x-coordinate of the top-left position of the square.
		
		@param x the x-coordinate of the top-left position of the square.
	*/
	public void x(final double x) { this.x = x; }
	
	/** Sets the y-coordinate of the top-left position of the square.
		
		@param y the y-coordinate of the top-left position of the square.
	*/
	public void y(final double y) { this.y = y; }
	
	/** Sets the edge length of the square.
		
		@param edge the edge length of the square.
		
		@exception IllegalArgumentException if {@code edge} is less than {@code 0}.
	*/
	public void edge(final double edge) {
		
		if (edge < 0) throw new IllegalArgumentException("Edge length less than 0");
		this.edge = edge;
	}
	
	/** Returns the x-coordinate of the top-left position of the square.
		
		@return the x-coordinate of the top-left position of the square.
	*/
	public double x() { return x; }
	
	/** Returns the y-coordinate of the top-left position of the square.
		
		@return the y-coordinate of the top-left position of the square.
	*/
	public double y() { return y; }
	
	/** Returns the edge length of the square.
		
		@return the edge length of the square.
	*/
	public double edge() { return edge; }
	
	/** Returns the perimeter of the square.
		
		@return the perimeter of the square.
	*/
	public double perimeter() {	return (4*edge); }
	
	/** Returns the area spanned by the square.
		
		@return the area spanned by the square.
	*/
	public double area() { return (edge*edge); }
	
	/** Sets the top-left position of the square.
		
		@param x the x-coordinate of the top-left position of the square.
		
		@param y the y-coordinate of the top-left position of the square.
	*/
	public void position(final double x, final double y) { this.x = x; this.y = y; }
	
	/** Translates the square over the given distance.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy) { this.x += dx; this.y += dy; }
	
	public Shape duplicate() { return new Square(this); }
	
	/** Sets the top-left position and edge length of the square.
		
		@param x the x-coordinate of the top-left position of the square.
		
		@param y the y-coordinate of the top-left position of the square.
		
		@param edge the edge length of the square.
		
		@exception IllegalArgumentException if {@code edge} is less than {@code 0}.
	*/
	public void set(final double x, final double y, final double edge) {
		
		if (edge < 0) throw new IllegalArgumentException("Edge length less than 0");
		this.x = x;
		this.y = y;
		this.edge = edge;
	}
	
	/** Indicates the position of a point relative to the square.
		
		@param point the point whose position relative to the square is to be tested. The point is treated as a 2D point. That is, only its x- and y-coordinate values are considered.
		
		@return the value {@link #contains(double,double) contains(point.x,point.y)}.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public boolean contains(final Point point) { return contains(point.x,point.y); }
	
	/** Indicates the position of a point relative to the square.
		
		@param x the x-coordinate of the point.
		
		@param y the y-coordinate of the point.
		
		@return {@code true} if the point is on or inside the square; {@code false} if it is outside the square.
	*/
	public boolean contains(final double x, final double y) {
		
		final double dx = x - this.x;
		final double dy = y - this.y;
		
		if (dx >= 0 && dx <= edge && dy >= 0 && dy <= edge) return true;
		else return false;
	}
	
	/** Returns the contour of the square.
		
		@return a new {@code GeneralPath} object containing the contour of the square.
	*/
	public GeneralPath contour() { return new GeneralPath(new Rectangle2D.Double(x,y,edge,edge)); }
	
	public Image bitmap(final boolean binary) {
		
		// Initialize bitmap:
		final double xpe = x + edge;
		final double ype = y + edge;
		final int minx = FMath.floor(x);
		final int miny = FMath.floor(y);
		final int xdim = 1 + FMath.floor(xpe) - minx;
		final int ydim = 1 + FMath.floor(ype) - miny;
		final ByteImage bitmap = new ByteImage(new Dimensions(xdim,ydim));
		
		if (edge > 0) {
			
			// Initialize variables:
			final int xis = FMath.ceil(x);
			final int yis = FMath.ceil(y);
			final int xie = FMath.floor(xpe);
			final int yie = FMath.floor(ype);
			final double tfrac = FMath.min(miny+1,ype) - y;
			final double bfrac = ype - FMath.max(yie,y);
			final double lfrac = FMath.min(minx+1,xpe) - x;
			final double rfrac = xpe - FMath.max(xie,x);
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
				if (ype > yie) {
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
				if (xpe > xie) {
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
		bounds.upper.x = x + edge;
		bounds.lower.y = y;
		bounds.upper.y = y + edge;
		
		return bounds;
	}
	
	public boolean equals(final Shape shape) {
		
		if (shape != null)
			if (shape instanceof Square) {
				final Square square = (Square)shape;
				if (x == square.x && y == square.y && edge == square.edge)
					return true;
			}
		
		return false;
	}
	
	public boolean empty() {
		
		if (area() == 0) return true;
		return false;
	}
	
}
