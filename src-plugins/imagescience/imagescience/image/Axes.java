package imagescience.image;

/** Contains information about the activity of the coordinate axes of up to 5D image data objects. */
public class Axes {
	
	/** The x-axis. */
	public final static int X=1;
	
	/** The y-axis. */
	public final static int Y=2;
	
	/** The z-axis. */
	public final static int Z=4;
	
	/** The t-axis. */
	public final static int T=8;
	
	/** The c-axis. */
	public final static int C=16;
	
	final static int NONE=0, YX=3, ZX=5, ZY=6, ZYX=7, TX=9, TY=10,
	TYX=11, TZ=12, TZX=13, TZY=14, TZYX=15, CX=17, CY=18, CYX=19,
	CZ=20, CZX=21, CZY=22, CZYX=23, CT=24, CTX=25, CTY=26, CTYX=27,
	CTZ=28, CTZX=29, CTZY=30, CTZYX=31, ALL=31;
	
	/** Indicates whether the x-axis is active. By default it is not active. */
	public final boolean x;
	
	/** Indicates whether the y-axis is active. By default it is not active. */
	public final boolean y;
	
	/** Indicates whether the z-axis is active. By default it is not active. */
	public final boolean z;
	
	/** Indicates whether the t-axis is active. By default it is not active. */
	public final boolean t;
	
	/** Indicates whether the c-axis is active. By default it is not active. */
	public final boolean c;
	
	/** Default constructor. */
	public Axes() {
		
		x = y = z = t = c = false;
	}
	
	/** One-dimensional constructor.
		
		@param x the x-axis activity.
	*/
	public Axes(final boolean x) {
		
		this.x = x;
		y = z = t = c = false;
	}
	
	/** Two-dimensional constructor.
		
		@param x the x-axis activity.
		
		@param y the y-axis activity.
	*/
	public Axes(final boolean x, final boolean y) {
		
		this.x = x;
		this.y = y;
		z = t = c = false;
	}
	
	/** Three-dimensional constructor.
		
		@param x the x-axis activity.
		
		@param y the y-axis activity.
		
		@param z the z-axis activity.
	*/
	public Axes(final boolean x, final boolean y, final boolean z) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		t = c = false;
	}
	
	/** Four-dimensional constructor.
		
		@param x the x-axis activity.
		
		@param y the y-axis activity.
		
		@param z the z-axis activity.
		
		@param t the t-axis activity.
	*/
	public Axes(final boolean x, final boolean y, final boolean z, final boolean t) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		c = false;
	}
	
	/** Five-dimensional constructor.
		
		@param x the x-axis activity.
		
		@param y the y-axis activity.
		
		@param z the z-axis activity.
		
		@param t the t-axis activity.
		
		@param c the c-axis activity.
	*/
	public Axes(final boolean x, final boolean y, final boolean z, final boolean t, final boolean c) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.c = c;
	}
	
	/** Five-dimensional constructor.
		
		@param axes an integer number indicating which axes are active. Must be one or a combination (by addition) of {@link #X}, {@link #Y}, {@link #Z}, {@link #T}, {@link #C}.
		
		@exception IllegalArgumentException if {@code axes} does not correspond to a valid combination of axes.
	*/
	public Axes(final int axes) {
		
		if (axes < NONE || axes > ALL) throw new IllegalArgumentException("Non-supported combination of axes");
		
		if ((axes & X) > 0) x = true; else x = false;
		if ((axes & Y) > 0) y = true; else y = false;
		if ((axes & Z) > 0) z = true; else z = false;
		if ((axes & T) > 0) t = true; else t = false;
		if ((axes & C) > 0) c = true; else c = false;
	}
	
	/** Duplicates this object.
		
		@return a new {@code Axes} object that is an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public Axes duplicate() { return new Axes(x,y,z,t,c); }
	
	/** Indicates whether this object is in the same state as the given object.
		
		@param axes the object to compare this object with.
		
		@return {@code true} if the given object is not {@code null} and has the same axes (de)activated as this object; {@code false} if this is not the case.
	*/
	public boolean equals(final Axes axes) {
		
		if (axes == null) return false;
		if (x==axes.x && y==axes.y && z==axes.z && t==axes.t && c==axes.c) return true;
		return false;
	}
	
}
