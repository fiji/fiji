package imagescience.image;

/** Contains the dimensions of the borders of up to 5D image data objects. */
public class Borders {
	
	/** The border size in the x-dimension. The default value is {@code 0}. */
	public final int x;
	
	/** The border size in the y-dimension. The default value is {@code 0}. */
	public final int y;
	
	/** The border size in the z-dimension. The default value is {@code 0}. */
	public final int z;
	
	/** The border size in the t-dimension. The default value is {@code 0}. */
	public final int t;
	
	/** The border size in the c-dimension. The default value is {@code 0}. */
	public final int c;
	
	/** Default constructor. */
	public Borders() {
		
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.t = 0;
		this.c = 0;
	}
	
	/** One-dimensional constructor.
		
		@param x the border size in the x-dimension.
		
		@exception IllegalArgumentException if the parameter value is less than {@code 0}.
	*/
	public Borders(final int x) {
		
		if (x < 0) throw new IllegalArgumentException("Border size less than 0");
		this.x = x;
		this.y = 0;
		this.z = 0;
		this.t = 0;
		this.c = 0;
	}
	
	/** Two-dimensional constructor.
		
		@param x the border size in the x-dimension.
		
		@param y the border size in the y-dimension.
		
		@exception IllegalArgumentException if any of the parameter values is less than {@code 0}.
	*/
	public Borders(final int x, final int y) {
		
		if (x < 0 || y < 0) throw new IllegalArgumentException("Border size(s) less than 0");
		this.x = x;
		this.y = y;
		this.z = 0;
		this.t = 0;
		this.c = 0;
	}
	
	/** Three-dimensional constructor.
		
		@param x the border size in the x-dimension.
		
		@param y the border size in the y-dimension.
		
		@param z the border size in the z-dimension.
		
		@exception IllegalArgumentException if any of the parameter values is less than {@code 0}.
	*/
	public Borders(final int x, final int y, final int z) {
		
		if (x < 0 || y < 0 || z < 0) throw new IllegalArgumentException("Border size(s) less than 0");
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = 0;
		this.c = 0;
	}
	
	/** Four-dimensional constructor.
		
		@param x the border size in the x-dimension.
		
		@param y the border size in the y-dimension.
		
		@param z the border size in the z-dimension.
		
		@param t the border size in the t-dimension.
		
		@exception IllegalArgumentException if any of the parameter values is less than {@code 0}.
	*/
	public Borders(final int x, final int y, final int z, final int t) {
		
		if (x < 0 || y < 0 || z < 0 || t < 0) throw new IllegalArgumentException("Border size(s) less than 0");
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.c = 0;
	}
	
	/** Five-dimensional constructor.
		
		@param x the border size in the x-dimension.
		
		@param y the border size in the y-dimension.
		
		@param z the border size in the z-dimension.
		
		@param t the border size in the t-dimension.
		
		@param c the border size in the c-dimension.
		
		@exception IllegalArgumentException if any of the parameter values is less than {@code 0}.
	*/
	public Borders(final int x, final int y, final int z, final int t, final int c) {
		
		if (x < 0 || y < 0 || z < 0 || t < 0 || c < 0) throw new IllegalArgumentException("Border size(s) less than 0");
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.c = c;
	}
	
	/** Duplicates this object.
		
		@return a new {@code Borders} object that is an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public Borders duplicate() { return new Borders(x,y,z,t,c); }
	
	/** Indicates whether this object is in the same state as the given object.
		
		@param borders the object to compare this object with.
		
		@return {@code true} if the given object is not {@code null} and has the same border size in each dimension as this object; {@code false} if this is not the case.
	*/
	public boolean equals(final Borders borders) {
		
		if (borders == null) return false;
		if (x==borders.x && y==borders.y && z==borders.z && t==borders.t && c==borders.c) return true;
		return false;
	}
	
}
