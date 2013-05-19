package imagescience.shape;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.FMath;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

/** A closed, interpolating, C&#178;-continuous, cubic spline curve in the x-y plane. It uses a uniform parametrization and knot vector. Although this may not be optimal in some cases, as pointed out by L. Piegl and W. Tiller, The NURBS Book, 2nd Edition, Springer-Verlag, Berlin, 1997, it allows for an extremely efficient implementation and computation of several features. Thus the user should be aware that this class works best for (approximately) equally spaced vertices. Also, although higher-dimensional vertices are accepted, only the x- and y-coordinates of the vertices are actually considered by all methods. */
public class Spline implements Shape {
	
	/** Default constructor. Results in a spline with no vertices. */
	public Spline() { }
	
	/** Constructs a spline from the given vertices.
		
		@param vertices the vertices that define the spline. The handles of the {@code Point} objects are copied. Any subsequent modifications of the vertices become effective only after calling the {@link #update()} method.
		
		@exception NullPointerException if {@code vertices} or any of its elements is {@code null}.
	*/
	public Spline(final Vector<Point> vertices) {
		
		set(vertices);
	}
	
	/** Copy constructor.
		
		@param spline the spline to be copied. All information is copied and no memory is shared with this spline.
		
		@exception NullPointerException if {@code spline} is {@code null}.
	*/
	public Spline(final Spline spline) {
		
		np = spline.np;
		final int minlen = np + 1;
		if (minlen > p.length)
			p = new Point[minlen+MINCAP];
		for (int i=0; i<np; ++i)
			p[i] = spline.p[i].duplicate();
		update();
	}
	
	/** Sets the vertices of the spline to the given vertices.
		
		@param vertices the vertices to be copied. The handles of the {@code Point} objects are copied. Any subsequent modifications of the vertices become effective only after calling the {@link #update()} method.
		
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
		update();
	}
	
	/** Sets the vertices of the spline to the given vertices. Alias of method {@link #set(Vector)}.
		
		@param vertices the vertices to be copied. The handles of the {@code Point} objects are copied. Any subsequent modifications of the vertices become effective only after calling the {@link #update()} method.
		
		@exception NullPointerException if {@code vertices} or any of its elements is {@code null}.
	*/
	public void vertices(final Vector<Point> vertices) {
		
		set(vertices);
	}
	
	/** Returns a new {@code Vector} object containing the handles of the vertices of the spline.
		
		@return a new {@code Vector} object containing the handles of the vertices of the spline. Any subsequent modifications of the vertices become effective only after calling the {@link #update()} method.
	*/
	public Vector<Point> vertices() {
		
		final Vector<Point> v = new Vector<Point>(np);
		for (int i=0; i<np; ++i) v.add(p[i]);
		return v;
	}
	
	public Shape duplicate() {
		
		return new Spline(this);
	}
	
	/** Translates the spline over the given distance. This is done by translating all the vertices over the given distance and updating the spline accordingly.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy) {
		
		for (int i=0; i<np; ++i)
			p[i].translate(dx,dy);
		update();
	}
	
	private static final double PERIMETER_ERROR = 0.001;
	
	/** Returns the perimeter of the spline.
		
		@return the perimeter of the spline. The perimeter is approximated using the rapidly converging estimator described by J. Gravesen, "Adaptive Subdivision and the Length and Energy of B&eacute;zier Curves", Computational Geometry, vol. 8, no. 1, June 1997, pp. 13-31. The absolute difference between the true perimeter and the approximation returned by this method is guaranteed to be (much) less than 0.001.
	*/
	public double perimeter() {
		
		double perimeter = 0;
		
		if (np > 1) {
			Point p0 = p[0], p1;
			Point c0 = c[2], c1;
			p[np] = p0;
			final double maxerror = PERIMETER_ERROR/np;
			for (int i=1; i<=np; ++i) {
				p1 = p[i]; c1 = c[i+2];
				perimeter += length(
					p0.x, p0.y,
					(2*c0.x + c1.x)/3,
					(2*c0.y + c1.y)/3,
					(c0.x + 2*c1.x)/3,
					(c0.y + 2*c1.y)/3,
					p1.x, p1.y,
					maxerror);
				p0 = p1; c0 = c1;
			}
		}
		
		return perimeter;
	}
	
	private double length(
		final double p0x, final double p0y,
		final double p1x, final double p1y,
		final double p2x, final double p2y,
		final double p3x, final double p3y,
		final double maxerror) {
		
		double length = 0;
		
		// Compute middle point using de Casteljau's subdivision scheme:
		final double s0x = (p1x + p2x)/2;
		final double s0y = (p1y + p2y)/2;
		final double q1x = (p0x + p1x)/2;
		final double q1y = (p0y + p1y)/2;
		final double q2x = (q1x + s0x)/2;
		final double q2y = (q1y + s0y)/2;
		final double r2x = (p2x + p3x)/2;
		final double r2y = (p2y + p3y)/2;
		final double r1x = (s0x + r2x)/2;
		final double r1y = (s0y + r2y)/2;
		final double mpx = (q2x + r1x)/2;
		final double mpy = (q2y + r1y)/2;
		
		{ // Check left-half of segment:
			final double indx0 = mpx - p0x;
			final double indy0 = mpy - p0y;
			final double inlen = Math.sqrt(indx0*indx0 + indy0*indy0);
			final double outdx0 = q1x - p0x;
			final double outdy0 = q1y - p0y;
			final double outdx1 = q2x - q1x;
			final double outdy1 = q2y - q1y;
			final double outdx2 = mpx - q2x;
			final double outdy2 = mpy - q2y;
			final double outlen = (Math.sqrt(outdx0*outdx0 + outdy0*outdy0) +
									Math.sqrt(outdx1*outdx1 + outdy1*outdy1) +
									Math.sqrt(outdx2*outdx2 + outdy2*outdy2));
			if (outlen - inlen < maxerror) length += (inlen + outlen)/2;
			else length += length(p0x,p0y,q1x,q1y,q2x,q2y,mpx,mpy,maxerror/2);
		}
		
		{ // Check right-half of segment:
			final double indx0 = p3x - mpx;
			final double indy0 = p3y - mpy;
			final double inlen = Math.sqrt(indx0*indx0 + indy0*indy0);
			final double outdx0 = r1x - mpx;
			final double outdy0 = r1y - mpy;
			final double outdx1 = r2x - r1x;
			final double outdy1 = r2y - r1y;
			final double outdx2 = p3x - r2x;
			final double outdy2 = p3y - r2y;
			final double outlen = (Math.sqrt(outdx0*outdx0 + outdy0*outdy0) +
									Math.sqrt(outdx1*outdx1 + outdy1*outdy1) +
									Math.sqrt(outdx2*outdx2 + outdy2*outdy2));
			if (outlen - inlen < maxerror) length += (inlen + outlen)/2;
			else length += length(mpx,mpy,r1x,r1y,r2x,r2y,p3x,p3y,maxerror/2);
		}
		
		return length;
	}
	
	/** Returns the area spanned by the spline.
		
		@return the area spanned by the spline. The area is computed using the exact (up to numerical rounding errors) method described by M. Jacob, T. Blu, M. Unser, "An Exact Method for Computing the Area Moments of Wavelet and Spline Curves", IEEE Transactions on Pattern Analysis and Machine Intelligence, vol. 23, no. 6, June 2001, pp. 633-642. The result is correct only for non-self-intersecting splines.
	*/
	public double area() {
		
		double area = 0;
		
		if (np > 2) {
			final int max = np + 1;
			for (int j=2; j<=max; ++j) {
				double tmp = 0;
				for (int i=2, imj=i-j; i<=max; ++i, ++imj)
					tmp += c[i].y * (g0(imj) + g0(imj-np) + g0(imj+np));
				area += c[j].x * tmp;
			}
		}
		
		return (area < 0) ? -area : area;
	}
	
	private double g0(final int i) {
		
		switch (i) {
			case -3: return -G03;
			case -2: return -G02;
			case -1: return -G01;
			case  1: return  G01;
			case  2: return  G02;
			case  3: return  G03;
			default: return  0.0;
		}
	}
	
	/** Indicates the position of a point relative to the spline.
		
		@param point the point whose position relative to the spline is to be tested. The point is treated as a 2D point. That is, only its x- and y-coordinate values are considered.
		
		@return the value {@link #contains(double,double) contains(point.x,point.y)}.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public boolean contains(final Point point) {
		
		return contains(point.x,point.y);
	}
	
	/** Indicates the position of a point relative to the spline.
		
		@param x the x-coordinate of the point.
		
		@param y the y-coordinate of the point.
		
		@return {@code true} if the point is on or inside the spline; {@code false} if it is outside the spline.
	*/
	public boolean contains(final double x, final double y) {
		
		return contour.contains(x,y);
	}
	
	/** Returns the contour of the spline.
		
		@return a new {@code GeneralPath} object containing the contour of the spline. The interior of the returned path is defined by the non-zero winding rule.
	*/
	public GeneralPath contour() {
		
		final GeneralPath path = new GeneralPath();
		
		// Express the spline as a Bezier curve:
		if (np > 0) {
			Point pi = p[0]; p[np] = pi;
			Point ci1 = c[2], ci2;
			path.moveTo((float)pi.x,(float)pi.y);
			for (int i=1; i<=np; ++i) {
				pi = p[i]; ci2 = c[i+2];
				path.curveTo(
					(float)(2*ci1.x + ci2.x)/3,
					(float)(2*ci1.y + ci2.y)/3,
					(float)(ci1.x + 2*ci2.x)/3,
					(float)(ci1.y + 2*ci2.y)/3,
					(float)pi.x,
					(float)pi.y);
				ci1 = ci2;
			}
			path.closePath();
		}
		
		return path;
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
			final Coordinates co = new Coordinates();
			final double[] row = new double[xdim];
			bitmap.axes(Axes.X);
			for (co.y=0; co.y<ydim; ++co.y) {
				final double yco = miny + co.y + 0.5;
				for (int x=0; x<xdim; ++x) {
					if (contour.contains(minx + x + 0.5, yco)) row[x] = 255;
					else row[x] = 0;
				}
				bitmap.set(co,row);
			}
			// Fill contour elements:
			if (!binary) {
				Point p0 = p[0]; p[np] = p0;
				Point cm = c[1], c0 = c[2], c1 = c[3];
				final double[] samplesites = new double[CONTOUR_RESOLUTION];
				for (int i=0; i<CONTOUR_RESOLUTION; ++i) samplesites[i] = (i + 1.0)/(CONTOUR_RESOLUTION + 1.0);
				final double samplesites2 = CONTOUR_RESOLUTION*CONTOUR_RESOLUTION;
				final Coordinates pc = new Coordinates(Integer.MIN_VALUE,Integer.MIN_VALUE);
				final Coordinates cc = new Coordinates();
				final Coordinates bc = new Coordinates();
				for (int i=1; i<=np; ++i) {
					final Point p1 = p[i];
					final Point c2 = c[i+3];
					final double len = length(
						p0.x, p0.y,
						(2*c0.x + c1.x)/3,
						(2*c0.y + c1.y)/3,
						(c0.x + 2*c1.x)/3,
						(c0.y + 2*c1.y)/3,
						p1.x, p1.y,
						0.5/CONTOUR_RESOLUTION);
					if (len > 0) {
						final int steps = FMath.ceil(CONTOUR_RESOLUTION*len);
						final double step = 1.0/steps;
						for (int j=0; j<steps; ++j) {
							final double t = j*step;
							final double it = 1 - t;
							final double wm = D1O6*it*it*it;
							final double w0 = D2O3 + (D1O2*t - 1)*t*t;
							final double w1 = D2O3 + (D1O2*it - 1)*it*it;
							final double w2 = D1O6*t*t*t;
							cc.x = FMath.floor(wm*cm.x + w0*c0.x + w1*c1.x + w2*c2.x);
							cc.y = FMath.floor(wm*cm.y + w0*c0.y + w1*c1.y + w2*c2.y);
							if (cc.x != pc.x || cc.y != pc.y) {
								int hits = 0;
								for (int yj=0; yj<CONTOUR_RESOLUTION; ++yj) {
									final double yss = cc.y + samplesites[yj];
									for (int xj=0; xj<CONTOUR_RESOLUTION; ++xj) {
										if (contour.contains(cc.x + samplesites[xj], yss))
											++hits;
									}
								}
								bc.x = cc.x - minx; bc.y = cc.y - miny;
								bitmap.set(bc,255*hits/samplesites2);
								pc.x = cc.x; pc.y = cc.y;
							}
						}
					}
					p0 = p1; cm = c0; c0 = c1; c1 = c2;
				}
			}
		}
		
		return bitmap;
	}
	
	public Bounds bounds() {
		
		final Point min = new Point();
		final Point max = new Point();
		
		if (np > 0) {
			p[np] = p[0];
			Point p0 = p[0];
			Point cm = c[1];
			Point c0 = c[2];
			Point c1 = c[3];
			min.x = max.x = p0.x;
			min.y = max.y = p0.y;
			for (int i=1; i<=np; ++i) {
				final Point p1 = p[i];
				final Point c2 = c[i+3];
				// Check end-point of segment:
				if (p1.x > max.x) max.x = p1.x;
				else if (p1.x < min.x) min.x = p1.x;
				if (p1.y > max.y) max.y = p1.y;
				else if (p1.y < min.y) min.y = p1.y;
				// Check extrema within segment:
				final double a0x = (cm.x + 4*c0.x + c1.x)/6;
				final double a0y = (cm.y + 4*c0.y + c1.y)/6;
				final double a1x = (c1.x - cm.x)/2;
				final double a1y = (c1.y - cm.y)/2;
				final double a2x = (cm.x - 2*c0.x + c1.x)/2;
				final double a2y = (cm.y - 2*c0.y + c1.y)/2;
				final double a3x = (c2.x - cm.x + 3*(c0.x - c1.x))/6;
				final double a3y = (c2.y - cm.y + 3*(c0.y - c1.y))/6;
				final double dax = 3*a3x;
				final double day = 3*a3y;
				final double dbx = 2*a2x;
				final double dby = 2*a2y;
				final double dcx = a1x;
				final double dcy = a1y;
				final double dqx = ((dbx > 0 ? -1 : 1)*Math.sqrt(dbx*dbx - 4*dax*dcx) - dbx)/2;
				final double dqy = ((dby > 0 ? -1 : 1)*Math.sqrt(dby*dby - 4*day*dcy) - dby)/2;
				final double t1x = (dax != 0) ? dqx/dax : -1;
				final double t1y = (day != 0) ? dqy/day : -1;
				final double t2x = (dqx != 0) ? dcx/dqx : -1;
				final double t2y = (dqy != 0) ? dcy/dqy : -1;
				if (t1x > 0 && t1x < 1) {
					final double xt1 = a0x + t1x*(a1x + t1x*(a2x + t1x*a3x));
					if (xt1 > max.x) max.x = xt1;
					else if (xt1 < min.x) min.x = xt1;
				}
				if (t1y > 0 && t1y < 1) {
					final double yt1 = a0y + t1y*(a1y + t1y*(a2y + t1y*a3y));
					if (yt1 > max.y) max.y = yt1;
					else if (yt1 < min.y) min.y = yt1;
				}
				if (t2x > 0 && t2x < 1) {
					final double xt2 = a0x + t2x*(a1x + t2x*(a2x + t2x*a3x));
					if (xt2 > max.x) max.x = xt2;
					else if (xt2 < min.x) min.x = xt2;
				}
				if (t2y > 0 && t2y < 1) {
					final double yt2 = a0y + t2y*(a1y + t2y*(a2y + t2y*a3y));
					if (yt2 > max.y) max.y = yt2;
					else if (yt2 < min.y) min.y = yt2;
				}
				// Pass control points to next iteration:
				cm = c0; c0 = c1; c1 = c2;
			}
		}
		
		return new Bounds(min,max);
	}
	
	public boolean equals(final Shape shape) {
		
		if (shape != null) {
			if (shape instanceof Spline) {
				final Spline spline = (Spline)shape;
				if (spline.np == np) {
					for (int i=0, ip2=2; i<np; ++i, ++ip2)
						if (!spline.p[i].equals(p[i]) || !spline.c[ip2].equals(c[ip2]))
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
	
	/** Makes the spline recompute its shape from its vertices. */
	public void update() {
		
		if (np == 0) {
			recontrol(4);
			for (int i=0; i<4; ++i) {
				c[i].x = 0.0;
				c[i].y = 0.0;
			}
		} else if (np == 1) {
			recontrol(5);
			for (int i=0; i<5; ++i) {
				c[i].x = p[0].x;
				c[i].y = p[0].y;
			}
		} else {
			recontrol(np + 4);
			
			// Initialization:
			final int max = np - 1;
			for (int i=0; i<=max; ++i) {
				c[i].x = p[i].x;
				c[i].y = p[i].y;
			}
			double Dx = CBSP*c[max].x;
			double Dy = CBSP*c[max].y;
			double Ex = CBSP*c[0].x;
			double Ey = CBSP*c[0].y;
			double zin = CBSP;
			for (int i=1; i<=max; ++i) {
				zin *= CBSP;
				Dx += zin*c[max-i].x;
				Dy += zin*c[max-i].y;
				Ex += zin*c[i].x;
				Ey += zin*c[i].y;
			}
			final double tmp1 = 1.0 - zin;
			final double tmp2 = (1.0 - CBSP*CBSP)*tmp1;
			
			// Causal filter:
			c[0].x += Dx/tmp1;
			c[0].y += Dy/tmp1;
			for (int i=1; i<max; ++i) {
				c[i].x += CBSP*c[i-1].x;
				c[i].y += CBSP*c[i-1].y;
			}
			
			// Anti-causal filter:
			c[max].x = -(Dx + CBSP*Ex)/tmp2;
			c[max].y = -(Dy + CBSP*Ey)/tmp2;
			for (int i=max-1; i>=0; --i) {
				c[i].x = CBSP*(c[i+1].x - c[i].x);
				c[i].y = CBSP*(c[i+1].y - c[i].y);
			}
			
			// Scale, shift, and periodize:
			for (int i1=max, i2=max+2; i1>=0; --i1, --i2) {
				c[i2].x = CBSF*c[i1].x;
				c[i2].y = CBSF*c[i1].y;
			}
			c[0].x = c[np].x;
			c[0].y = c[np].y;
			c[1].x = c[np+1].x;
			c[1].y = c[np+1].y;
			c[np+2].x = c[2].x;
			c[np+2].y = c[2].y;
			c[np+3].x = c[3].x;
			c[np+3].y = c[3].y;
		}
		
		contour = contour();
	}
	
	private void recontrol(final int len) {
		
		if (c == null || len > c.length) {
			final int clen = len + MINCAP;
			c = new Point[clen];
			for (int i=0; i<clen; ++i)
				c[i] = new Point();
		}
	}
	
	private GeneralPath contour = null;
	
	private static final int MINCAP = 10;
	private Point[] p = new Point[MINCAP];
	private Point[] c = null;
	private int np = 0;
	
	private static final double CBSP = -0.267949192431;
	private static final double CBSF = 6.0;
	private static final double D1O2 = 1.0/2.0;
	private static final double D1O6 = 1.0/6.0;
	private static final double D2O3 = 2.0/3.0;
	private static final double G01 = -49.0/144.0;
	private static final double G02 = -7.0/90.0;
	private static final double G03 = -1.0/720.0;
	
}
