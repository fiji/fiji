package imagescience.image;

/** Contains aspect-ratio or size information of up to 5D image elements. */
public class Aspects {
	
	/** The absolute or relative aspect size in the x-dimension. The default value is {@code 1}. */
	public final double x;
	
	/** The absolute or relative aspect size in the y-dimension. The default value is {@code 1}. */
	public final double y;
	
	/** The absolute or relative aspect size in the z-dimension. The default value is {@code 1}. */
	public final double z;
	
	/** The absolute or relative aspect size in the t-dimension. The default value is {@code 1}. */
	public final double t;
	
	/** The absolute or relative aspect size in the c-dimension. The default value is {@code 1}. */
	public final double c;
	
	/** Default constructor. The aspect size in each dimension is set to its default value. */
	public Aspects() {
		
		this.x = 1;
		this.y = 1;
		this.z = 1;
		this.t = 1;
		this.c = 1;
	}
	
	/** One-dimensional constructor.
		
		@param x the aspect size in the x-dimension.
	*/
	public Aspects(final double x) {
		
		this.x = x;
		this.y = 1;
		this.z = 1;
		this.t = 1;
		this.c = 1;
	}
	
	/** Two-dimensional constructor.
		
		@param x the aspect size in the x-dimension.
		
		@param y the aspect size in the y-dimension.
	*/
	public Aspects(final double x, final double y) {
		
		this.x = x;
		this.y = y;
		this.z = 1;
		this.t = 1;
		this.c = 1;
	}
	
	/** Three-dimensional constructor.
		
		@param x the aspect size in the x-dimension.
		
		@param y the aspect size in the y-dimension.
		
		@param z the aspect size in the z-dimension.
	*/
	public Aspects(final double x, final double y, final double z) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = 1;
		this.c = 1;
	}
	
	/** Four-dimensional constructor.
		
		@param x the aspect size in the x-dimension.
		
		@param y the aspect size in the y-dimension.
		
		@param z the aspect size in the z-dimension.
		
		@param t the aspect size in the t-dimension.
	*/
	public Aspects(final double x, final double y, final double z, final double t) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.c = 1;
	}
	
	/** Five-dimensional constructor.
		
		@param x the aspect size in the x-dimension.
		
		@param y the aspect size in the y-dimension.
		
		@param z the aspect size in the z-dimension.
		
		@param t the aspect size in the t-dimension.
		
		@param c the aspect size in the c-dimension.
	*/
	public Aspects(final double x, final double y, final double z, final double t, final double c) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.c = c;
	}
	
	/** Duplicates this object.
		
		@return a new {@code Aspects} object that is an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public Aspects duplicate() { return new Aspects(x,y,z,t,c); }
	
	/** Indicates whether this object is in the same state as the given object.
		
		@param aspects the object to compare this object with.
		
		@return {@code true} if the given object is not {@code null} and its aspect size in each dimension is equal to the corresponding aspect size of this object; {@code false} if this is not the case.
	*/
	public boolean equals(final Aspects aspects) {
		
		if (aspects == null) return false;
		if (x==aspects.x && y==aspects.y && z==aspects.z && t==aspects.t && c==aspects.c) return true;
		return false;
	}
	
}
