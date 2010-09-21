package imagescience.shape;

import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.image.ByteImage;
import imagescience.utility.FMath;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

/** An arbitrarily but discretely shaped region in the x-y plane. This class can be used to represent and manipulate shapes with boundaries consisting of straight-line segments constrained to the Cartesian 2D integer grid. The class uses {@code java.awt.geom.Area} as a back-end. */
public class Region implements Shape {
	
	private final Area area = new Area();
	
	/** Default constructor. Results in an empty region. */
	public Region() { }
	
	/** Image constructor.
		
		@param image an image defining the region. The resulting region is the union of all unit squares corresponding to elements in the image with value larger than {@code 0}. If the image is not a 2D but a higher-dimensional image, only the elements in the x-y plane are considered for which the higher coordinates are {@code 0}.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Region(final Image image) {
		
		final GeneralPath gp = new GeneralPath();
		final Coordinates c = new Coordinates();
		final Dimensions dims = image.dimensions();
		final double[] row = new double[dims.x];
		final int curaxes = image.axes();
		image.axes(Axes.X); int sx;
		for (c.y=0; c.y<dims.y; ++c.y) {
			image.get(c,row); sx = -1;
			// Find and add runs of positive elements:
			for (int x=0; x<dims.x; ++x) {
				if (row[x] > 0) {
					if (sx < 0) sx = x;
				} else if (sx >= 0) {
					gp.moveTo(sx,c.y);
					gp.lineTo(x,c.y);
					gp.lineTo(x,c.y+1);
					gp.lineTo(sx,c.y+1);
					gp.closePath();
					sx = -1;
				}
			}
			// In case the last run extended to the end of the row:
			if (sx >= 0) {
				gp.moveTo(sx,c.y);
				gp.lineTo(dims.x,c.y);
				gp.lineTo(dims.x,c.y+1);
				gp.lineTo(sx,c.y+1);
				gp.closePath();
			}
		}
		area.add(new Area(gp));
		image.axes(curaxes);
	}
	
	/** Copy constructor.
		
		@param region the region to be copied. All information is copied and no memory is shared with this region.
		
		@exception NullPointerException if {@code region} is {@code null}.
	*/
	public Region(final Region region) { area.add(region.area); }
	
	/** Adds the shape of the given region to the shape of this region. Addition is achieved through union.
		
		@param region the region to be added to this region.
		
		@exception NullPointerException if {@code region} is {@code null}.
	*/
	public void add(final Region region) { area.add(region.area); }
	
	/** Sets the shape of this region to the union of its current shape and the shape of the given region, minus their intersection.
		
		@param region the region to be joined with this region as described.
		
		@exception NullPointerException if {@code region} is {@code null}.
	*/
	public void exclusive(final Region region) { area.exclusiveOr(region.area); }
	
	/** Sets the shape of this region to the intersection of its current shape and the shape of the given region.
		
		@param region the region to be intersected with this region.
		
		@exception NullPointerException if {@code region} is {@code null}.
	*/
	public void intersect(final Region region) { area.intersect(region.area); }
	
	/** Subtracts the shape of the given region from the shape of this region.
		
		@param region the region to be subtracted from this region.
		
		@exception NullPointerException if {@code region} is {@code null}.
	*/
	public void subtract(final Region region) { area.subtract(region.area); }
	
	/** Restores this region to an empty region. */
	public void reset() { area.reset(); }
	
	/** Returns the area covered by the region.
		
		@return the area covered by the region.
	*/
	public double area() {
		
		int area = 0;
		final Image bitmap = bitmap(true);
		final Coordinates c = new Coordinates();
		final Dimensions dims = bitmap.dimensions();
		final double[] row = new double[dims.x];
		bitmap.axes(Axes.X);
		for (c.y=0; c.y<dims.y; ++c.y) {
			bitmap.get(c,row);
			for (int x=0; x<dims.x; ++x)
			if (row[x] > 0) ++area;
		}
		
		return area;
	}
	
	public Image bitmap(final boolean binary) {
		
		final Bounds bounds = bounds();
		final int minx = FMath.floor(bounds.lower.x);
		final int miny = FMath.floor(bounds.lower.y);
		final int xran = FMath.round(bounds.upper.x) - minx;
		final int yran = FMath.round(bounds.upper.y) - miny;
		final int xdim = (xran > 0) ? xran : 1;
		final int ydim = (yran > 0) ? yran : 1;
		final BufferedImage bi = new BufferedImage(xdim,ydim,BufferedImage.TYPE_BYTE_GRAY);
		final Graphics2D g2d = bi.createGraphics();
		g2d.setColor(new Color(255,255,255));
		final AffineTransform at = new AffineTransform();
		at.translate(-minx,-miny);
		g2d.fill(at.createTransformedShape(contour()));
		final Raster raster = bi.getRaster();
		final double[] row = new double[xdim];
		final ByteImage bitmap = new ByteImage(new Dimensions(xdim,ydim));
		bitmap.axes(Axes.X);
		final Coordinates c = new Coordinates();
		for (c.y=0; c.y<ydim; ++c.y) {
			raster.getPixels(0,c.y,xdim,1,row);
			bitmap.set(c,row);
		}
		
		return bitmap;
	}
	
	/** Translates the region over the given distance.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
	*/
	public void translate(final int dx, final int dy) {
		
		area.transform(new AffineTransform(1,0,0,1,dx,dy));
	}
	
	public Shape duplicate() { return new Region(this); }
	
	/** Indicates the position of a point relative to the region.
		
		@param point the point whose position relative to the region is to be tested. The point is treated as a 2D point. That is, only its x- and y-coordinate values are considered.
		
		@return the value {@link #contains(double,double) contains(point.x,point.y)}.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public boolean contains(final Point point) { return area.contains(point.x,point.y); }
	
	/** Indicates the position of a point relative to the region.
		
		@param x the x-coordinate of the point.
		
		@param y the y-coordinate of the point.
		
		@return {@code true} if the point is on or inside the region; {@code false} if it is outside the region.
	*/
	public boolean contains(final double x, final double y) { return area.contains(x,y); }
	
	/** Returns the contour of the region.
		
		@return a new {@code GeneralPath} object containing the contour of the region.
	*/
	public GeneralPath contour() { return new GeneralPath(area); }
	
	public Bounds bounds() {
		
		final Rectangle2D abounds = area.getBounds();
		final Bounds bounds = new Bounds();
		bounds.lower.x = abounds.getMinX();
		bounds.upper.x = abounds.getMaxX();
		bounds.lower.y = abounds.getMinY();
		bounds.upper.y = abounds.getMaxY();
		
		return bounds;
	}
	
	public boolean equals(final Shape shape) {
		
		if (shape != null)
			if (shape instanceof Region)
				if (area.equals(((Region)shape).area))
					return true;
		
		return false;
	}
	
	public boolean empty() { return area.isEmpty(); }
	
}
