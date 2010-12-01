package imagescience.shape;

/** A point in 5D space. */
public class Point {
	
	/** The x-coordinate of the point. The default value is {@code 0}. */
	public double x = 0.0;
	
	/** The y-coordinate of the point. The default value is {@code 0}. */
	public double y = 0.0;
	
	/** The z-coordinate of the point. The default value is {@code 0}. */
	public double z = 0.0;
	
	/** The t-coordinate of the point. The default value is {@code 0}. */
	public double t = 0.0;
	
	/** The c-coordinate of the point. The default value is {@code 0}. */
	public double c = 0.0;
	
	/** Default constructor. All coordinates are set to their default values. */
	public Point() { }
	
	/** One-dimensional constructor. The x-coordinate is set to the given value while the remaining coordinates are set to their default values.
		
		@param x the x-coordinate.
	*/
	public Point(final double x) {
		
		this.x = x;
	}
	
	/** Two-dimensional constructor. The x- and y-coordinates are set to the given values while the remaining coordinates are set to their default values.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
	*/
	public Point(final double x, final double y) {
		
		this.x = x;
		this.y = y;
	}
	
	/** Three-dimensional constructor. The x-, y-, and z-coordinates are set to the given values while the remaining coordinates are set to their default values.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
	*/
	public Point(final double x, final double y, final double z) {
		
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/** Four-dimensional constructor. The x-, y-, z-, and t-coordinates are set to the given values while the remaining coordinate is set to its default value.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
		
		@param t the t-coordinate.
	*/
	public Point(final double x, final double y, final double z, final double t) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
	}
	
	/** Five-dimensional constructor. The x-, y-, z-, t-, and c-coordinates are set to the given values.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
		
		@param t the t-coordinate.
		
		@param c the c-coordinate.
	*/
	public Point(final double x, final double y, final double z, final double t, final double c) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.c = c;
	}
	
	/** Copy constructor.
		
		@param point the point to be copied. All information is copied and no memory is shared with this point.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public Point(final Point point) {
		
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
		this.t = point.t;
		this.c = point.c;
	}
	
	/** Sets the one-dimensional position of the point. The x-coordinate is set to the given value and the remaining coordinates are not changed.
		
		@param x the x-coordinate.
	*/
	public void set(final double x) {
		
		this.x = x;
	}
	
	/** Sets the two-dimensional position of the point. The x- and y-coordinates are set to the given values and the remaining coordinates are not changed.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
	*/
	public void set(final double x, final double y) {
		
		this.x = x;
		this.y = y;
	}
	
	/** Sets the three-dimensional position of the point. The x-, y-, and z-coordinates are set to the given values and the remaining coordinates are not changed.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
	*/
	public void set(final double x, final double y, final double z) {
		
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/** Sets the four-dimensional position of the point. The x-, y-, z-, and t-coordinates are set to the given values and the remaining coordinate is not changed.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
		
		@param t the t-coordinate.
	*/
	public void set(final double x, final double y, final double z, final double t) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
	}
	
	/** Sets the five-dimensional position of the point. The x-, y-, z-, t-, and c-coordinates are set to the given values.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
		
		@param t the t-coordinate.
		
		@param c the c-coordinate.
	*/
	public void set(final double x, final double y, final double z, final double t, final double c) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.c = c;
	}
	
	/** Sets the coordinates of this point to the coordinates of the given point.
		
		@param point the point whose coordinates are to be copied.
		
		@exception NullPointerException if {@code point} is <code>null</code>.
	*/
	public void set(final Point point) {
		
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
		this.t = point.t;
		this.c = point.c;
	}
	
	/** Translates the point in one dimension. The x-coordinate is increased by the given amount while the remaining coordinates are not changed.
		
		@param dx the distance in the x-dimension over which to translate.
	*/
	public void translate(final double dx) {
		
		this.x += dx;
	}
	
	/** Translates the point in two dimensions. The x- and y-coordinates are increased by the given amounts while the remaining coordinates are not changed.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy) {
		
		this.x += dx;
		this.y += dy;
	}
	
	/** Translates the point in three dimensions. The x-, y-, and z-coordinates are increased by the given amounts while the remaining coordinates are not changed.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
		
		@param dz the distance in the z-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy, final double dz) {
		
		this.x += dx;
		this.y += dy;
		this.z += dz;
	}
	
	/** Translates the point in four dimensions. The x-, y-, z-, and t-coordinates are increased by the given amounts while the remaining coordinate is not changed.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
		
		@param dz the distance in the z-dimension over which to translate.
		
		@param dt the distance in the t-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy, final double dz, final double dt) {
		
		this.x += dx;
		this.y += dy;
		this.z += dz;
		this.t += dt;
	}
	
	/** Translates the point in five dimensions. The x-, y-, z-, t-, and c-coordinates are increased by the given amounts.
		
		@param dx the distance in the x-dimension over which to translate.
		
		@param dy the distance in the y-dimension over which to translate.
		
		@param dz the distance in the z-dimension over which to translate.
		
		@param dt the distance in the t-dimension over which to translate.
		
		@param dc the distance in the c-dimension over which to translate.
	*/
	public void translate(final double dx, final double dy, final double dz, final double dt, final double dc) {
		
		this.x += dx;
		this.y += dy;
		this.z += dz;
		this.t += dt;
		this.c += dc;
	}
	
	/** Returns an exact copy of this object.
		
		@return an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public Point duplicate() { return new Point(this); }
	
	/** Indicates whether this object represents the same point as the given object.
		
		@param point the point to compare this point with.
		
		@return {@code true} if {@code point} is not {@code null}, and each of its coordinates is equal to the corresponding coordinate of this object; {@code false} if this is not the case.
	*/
	public boolean equals(final Point point) {
		
		if (point == null) return false;
		if (x==point.x && y==point.y && z==point.z && t==point.t && c==point.c) return true;
		return false;
	}
	
}
