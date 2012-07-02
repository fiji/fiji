package imagescience.shape;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.FMath;
import java.awt.geom.GeneralPath;
import java.util.Vector;

/** A polygon in the x-y plane. Although higher-dimensional vertices are accepted, only the x- and y-coordinates of the vertices are actually considered by all methods. */
public class Polygon implements Shape {
	
	/** Default constructor. Results in a polygon with no vertices. */
	public Polygon() { }
	
	/** Constructs a polygon from the given vertices.
		
		@param vertices the vertices that define the polygon. The handles of the {@code Point} objects are copied.
		
		@exception NullPointerException if {@code vertices} or any of its elements is {@code null}.
	*/
	public Polygon(final Vector<Point> vertices) {
		
		set(vertices);
	}
	
	/** Copy constructor.
		
		@param polygon the polygon to be copied. All information is copied and no memory is shared with this polygon.
		
		@exception NullPointerException if {@code polygon} is {@code null}.
	*/
	public Polygon(final Polygon polygon) {
		
		np = polygon.np;
		final int minlen = np + 1;
		if (minlen > p.length)
			p = new Point[minlen+MINCAP];
		for (int i=0; i<np; ++i)
			p[i] = polygon.p[i].duplicate();
		p[np] = p[0];
	}
	
	/** Sets the vertices of the polygon to the given vertices.
		
		@param vertices the vertices to be copied. The handles of the {@code Point} objects are copied.
	
		@exception NullPointerException if {@code vertices} or any of its elements is {@code null}.
	*/
	public void set(final Vector<Point> vertices) {
		
		if (vertices == null)
			throw new NullPointerException("Vertices is null");
		np = vertices.size();
		final int minlen = np + 1;
		if (minlen > p.length)
			p = new Point[minlen+MINCAP];
		for (int i=0; i<np; ++i) {
			final Point pi = vertices.get(i);
			if (pi == null)
				throw new NullPointerException("Vertex "+i+" is null");
			else p[i] = pi;
		}
		p[np] = p[0];
	}
	
	/** Sets the vertices of the polygon to the given vertices. Alias of method {@link #set(Vector)}.
		
		@param vertices the vertices to be copied. The handles of the {@code Point} objects are copied.
	
		@exception NullPointerException if {@code vertices} or any of its elements is {@code null}.
	*/
	public void vertices(final Vector<Point> vertices) {
		
		set(vertices);
	}
	
	/** Returns a new {@code Vector} object containing the handles of the vertices of the polygon.
		
		@return a new {@code Vector} object containing the handles of the vertices of the polygon.
	*/
	public Vector<Point> vertices() {
		
		final Vector<Point> v = new Vector<Point>(np);
		for (int i=0; i<np; ++i) v.add(p[i]);
		return v;
	}
	
	public Shape duplicate() {
		
		return new Polygon(this);
	}
	
	/** Translates the polygon over the given distance. This is done by translating all the vertices over the given distance.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy) {
		
		for (int i=0; i<np; ++i)
			p[i].translate(dx,dy);
	}
	
	/** Returns the perimeter of the polygon.
		
		@return the perimeter of the polygon. The perimeter is computed straightforwardly by adding the Euclidean distances between the subsequent vertices, including the distance between the last and the first vertex.
	*/
	public double perimeter() {
		
		double perimeter = 0;
		
		if (np > 1) {
			for (int im1=0, i=1; i<=np; ++i, ++im1) {
				final double dx = p[i].x - p[im1].x;
				final double dy = p[i].y - p[im1].y;
				perimeter += Math.sqrt(dx*dx + dy*dy);
			}
		}
		
		return perimeter;
	}
	
	/** Returns the area spanned by the polygon.
		
		@return the area of the polygon. The result is correct for all simple polygons, whether convex or concave, but may not be correct for self-intersecting polygons, except for special cases of self-overlapping polyons, where the overlap area is counted twice.
	*/
	public double area() {
		
		double area = 0;
		
		if (np > 2) {
			for (int i=0, ip1=1; i<np; ++i, ++ip1)
				area += p[i].x*p[ip1].y - p[ip1].x*p[i].y;
		}
		
		return (area < 0) ? -0.5*area : 0.5*area;
	}
	
	/** Indicates the position of a point relative to the polygon.
		
		@param point the point whose position relative to the polygon is to be tested. The point is treated as a 2D point. That is, only its x- and y-coordinate values are considered.
		
		@return the value {@link #contains(double,double) contains(point.x,point.y)}.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public boolean contains(final Point point) {
		
		return contains(point.x,point.y);
	}
	
	/** Indicates the position of a point relative to the polygon. The method is an implementation of Algorithm 7 described by K. Hormann and A. Agathos, "The Point in Polygon Problem for Arbitrary Polygons", Computational Geometry - Theory and Applications, vol. 20, no. 3, 2001, pp. 131-144. It is based on the non-zero winding rule.
		
		@param x the x-coordinate of the point.
		
		@param y the y-coordinate of the point.
		
		@return {@code true} if the point is on or inside the polygon; {@code false} if it is outside the polygon.
	*/
	public boolean contains(final double x, final double y) {
		
		if (np == 0) return false;
		if (p[0].y == y && p[0].x == x) return true;
		double det = 0.0; int w = 0;
		for (int i=0, ip1=1; i<np; ++i, ++ip1) {
			if (p[ip1].y == y && (p[ip1].x == x || (p[i].y == y && (p[ip1].x > x) == (p[i].x < x)))) return true;
			if (p[i].y < y != p[ip1].y < y) {
				if (p[i].x >= x) {
					if (p[ip1].x > x) {
						if (p[ip1].y > p[i].y) ++w;
						else --w;
					} else {
						det = ((p[i].x - x)*(p[ip1].y - y) - (p[ip1].x - x)*(p[i].y - y));
						if (det == 0.0) return true;
						if ((det > 0) == (p[ip1].y > p[i].y)) {
							if (p[ip1].y > p[i].y) ++w;
							else --w;
						}
					}
				} else if (p[ip1].x > x) {
					det = ((p[i].x - x)*(p[ip1].y - y) - (p[ip1].x - x)*(p[i].y - y));
					if (det == 0.0) return true;
					if ((det > 0) == (p[ip1].y > p[i].y)) {
						if (p[ip1].y > p[i].y) ++w;
						else --w;
					}
				}
			}
		}
		return (w == 0) ? false : true;
	}
	
	/** Returns the contour of the polygon.
		
		@return a new {@code GeneralPath} object containing the contour of the polygon. The interior of the returned path is defined by the non-zero winding rule.
	*/
	public GeneralPath contour() {
		
		final GeneralPath contour = new GeneralPath();
		
		if (np > 0) {
			contour.moveTo((float)p[0].x,(float)p[0].y);
			for (int i=0; i<np; ++i)
				contour.lineTo((float)p[i].x,(float)p[i].y);
			contour.closePath();
		}
		
		return contour;
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
		
		if (np > 2) {
			// Fill interior elements:
			final Coordinates c = new Coordinates();
			final double[] row = new double[xdim];
			bitmap.axes(Axes.X);
			for (c.y=0; c.y<ydim; ++c.y) {
				final double yc = miny + c.y + 0.5;
				for (int x=0; x<xdim; ++x) {
					if (contains(minx + x + 0.5, yc)) row[x] = 255;
					else row[x] = 0;
				}
				bitmap.set(c,row);
			}
			// Fill contour elements:
			if (!binary) {
				final double[] samplesites = new double[CONTOUR_RESOLUTION];
				for (int i=0; i<CONTOUR_RESOLUTION; ++i) samplesites[i] = (i + 1.0)/(CONTOUR_RESOLUTION + 1.0);
				final double samplesites2 = CONTOUR_RESOLUTION*CONTOUR_RESOLUTION;
				final Coordinates pc = new Coordinates(Integer.MIN_VALUE,Integer.MIN_VALUE);
				final Coordinates cc = new Coordinates();
				final Coordinates bc = new Coordinates();
				for (int i=0, ip1=1; i<np; ++i, ++ip1) {
					final Point pi = p[i];
					final double dx = p[ip1].x - pi.x;
					final double dy = p[ip1].y - pi.y;
					final double len = Math.sqrt(dx*dx + dy*dy);
					if (len > 0) {
						final int steps = FMath.ceil(CONTOUR_RESOLUTION*len);
						final double sx = dx/steps;
						final double sy = dy/steps;
						for (int j=0; j<steps; ++j) {
							cc.x = FMath.floor(pi.x + j*sx);
							cc.y = FMath.floor(pi.y + j*sy);
							if (cc.x != pc.x || cc.y != pc.y) {
								int hits = 0;
								for (int yj=0; yj<CONTOUR_RESOLUTION; ++yj) {
									final double yss = cc.y + samplesites[yj];
									for (int xj=0; xj<CONTOUR_RESOLUTION; ++xj) {
										if (contains(cc.x + samplesites[xj], yss)) ++hits;
									}
								}
								bc.x = cc.x - minx; bc.y = cc.y - miny;
								bitmap.set(bc,255*hits/samplesites2);
								pc.x = cc.x; pc.y = cc.y;
							}
						}
					}
				}
			}
		}
		
		return bitmap;
	}
	
	public Bounds bounds() {
		
		final Point min = new Point();
		final Point max = new Point();
		
		if (np > 0) {
			min.x = max.x = p[0].x;
			min.y = max.y = p[0].y;
			for (int i=1; i<np; ++i) {
				final Point pi = p[i];
				if (pi.x > max.x) max.x = pi.x;
				else if (pi.x < min.x) min.x = pi.x;
				if (pi.y > max.y) max.y = pi.y;
				else if (pi.y < min.y) min.y = pi.y;
			}
		}
		
		return new Bounds(min,max);
	}
	
	public boolean equals(final Shape shape) {
		
		if (shape != null) {
			if (shape instanceof Polygon) {
				final Polygon polygon = (Polygon)shape;
				if (polygon.np == np) {
					for (int i=0; i<np; ++i)
						if (!polygon.p[i].equals(p[i]))
							return false;
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean empty() {
		
		if (area() == 0) return true;
		return false;
	}
	
	private static final int MINCAP = 10;
	private Point[] p = new Point[MINCAP];
	private int np = 0;
	
}
