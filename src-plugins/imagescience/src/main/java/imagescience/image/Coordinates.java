package imagescience.image;

/** Contains 5D integer coordinates. */
public class Coordinates {
	
	/** The x-coordinate. The default value is {@code 0}. */
	public int x = 0;
	
	/** The y-coordinate. The default value is {@code 0}. */
	public int y = 0;
	
	/** The z-coordinate. The default value is {@code 0}. */
	public int z = 0;
	
	/** The t-coordinate. The default value is {@code 0}. */
	public int t = 0;
	
	/** The c-coordinate. The default value is {@code 0}. */
	public int c = 0;
	
	/** Default constructor. */
	public Coordinates() {}
	
	/** One-dimensional constructor.
		
		@param x the x-coordinate.
	*/
	public Coordinates(final int x) {
		
		this.x = x;
	}
	
	/** Two-dimensional constructor.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
	*/
	public Coordinates(final int x, final int y) {
		
		this.x = x;
		this.y = y;
	}
	
	/** Three-dimensional constructor.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
	*/
	public Coordinates(final int x, final int y, final int z) {
		
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/** Four-dimensional constructor.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
		
		@param t the t-coordinate.
	*/
	public Coordinates(final int x, final int y, final int z, final int t) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
	}
	
	/** Five-dimensional constructor.
		
		@param x the x-coordinate.
		
		@param y the y-coordinate.
		
		@param z the z-coordinate.
		
		@param t the t-coordinate.
		
		@param c the c-coordinate.
	*/
	public Coordinates(final int x, final int y, final int z, final int t, final int c) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.c = c;
	}
	
	/** Copy constructor.
		
		@param coords the coordinates to be copied.
	*/
	public Coordinates(final Coordinates coords) {
		
		this.x = coords.x;
		this.y = coords.y;
		this.z = coords.z;
		this.t = coords.t;
		this.c = coords.c;
	}
	
	/** Resets the coordinates to their default values. */
	public void reset() { this.x = this.y = this.z = this.t = this.c = 0; }
	
	/** Sets the coordinates to the given coordinates.
		
		@param coords the coordinates to be copied.
		
		@exception NullPointerException if {@code coords} is {@code null}.
	*/
	public void set(final Coordinates coords) {
		
		this.x = coords.x;
		this.y = coords.y;
		this.z = coords.z;
		this.t = coords.t;
		this.c = coords.c;
	}
	
	/** Duplicates this object.
		
		@return a new {@code Coordinates} object that is an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public Coordinates duplicate() { return new Coordinates(x,y,z,t,c); }
	
	/** Indicates whether this object is in the same state as the given object.
		
		@param coords the object to compare this object with.
	
		@return {@code true} if the given object is not {@code null} and its coordinates are equal to the corresponding coordinates of this object; {@code false} if this is not the case.
	*/
	public boolean equals(final Coordinates coords) {
		
		if (coords == null) return false;
		if (x==coords.x && y==coords.y && z==coords.z && t==coords.t && c==coords.c) return true;
		return false;
	}
	
}
