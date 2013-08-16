package imagescience.image;

import ij.ImagePlus;
import java.lang.Class;
import java.lang.Math;
import java.lang.reflect.Constructor;

/** Abstract base class for images containing up to 5D elements. */
public abstract class Image {
	
	// Properties:
	final Dimensions dims;
	Aspects aspects = new Aspects();
	String name = "";
	int axes = Axes.NONE;
	static int lastID = 0;
	final int thisID;
	
	// Constructor:
	Image(final Dimensions dims) {
		
		this.dims = dims;
		thisID = ++lastID;
	}
	
	/** Returns the image type.
		
		@return the image type. The returned {@code String} contains the fully qualified name of the subclass which this image is an object of.
	*/
	public abstract String type();
	
	/** Returns a bordered copy of this image.
		
		@param borders specifies the border size in each dimension.
		
		@return a bordered copy of this image. All information is copied to the newly created image and no memory is shared.
		
		@exception NullPointerException if {@code borders} is {@code null}.
	*/
	public abstract Image border(final Borders borders);
	
	/** Creates a new {@code Image} of given dimensions and type.
		
		@param dims the dimensions of the new image.
		
		@param type the type of image to be created. Must be a {@code String} containing the fully qualified name of the subclass which the new image must be an instance of.
		
		@return a new {@code Image} of given dimensions and type.
		
		@exception IllegalArgumentException if the given {@code type} is not supported.
		
		@exception NullPointerException if any of the parameters is {@code null}.
		
		@exception UnknownError if for any reason the requested image could not be created. In most cases this will be due to insufficient free memory.
	*/
	public static Image create(final Dimensions dims, final String type) {
		
		try {
			final Class imgcls = Class.forName(type);
			final Class[] params = new Class[1]; params[0] = dims.getClass();
			final Constructor imgcon = imgcls.getConstructor(params);
			final Object[] initargs = new Object[1]; initargs[0] = dims;
			return (Image)imgcon.newInstance(initargs);
			
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Non-supported image type");
			
		} catch (Throwable e) {
			throw new UnknownError("Could not create new image");
		}
	}
	
	/** Creates a wrapper {@code Image} of the given {@code ImagePlus} object. This is done by checking the element type and calling the wrapper constructor of the corresponding {@code Image} subclass.
		
		@param imageplus the {@code ImagePlus} object whose image data is to be wrapped.
		
		@return a wrapper {@code Image} object of the given {@code ImagePlus} object. The two objects share the actual image data.
		
		@exception IllegalArgumentException if the elements of {@code imageplus} are of a non-supported type.
		
		@exception NullPointerException if {@code imageplus} is {@code null}.
	*/
	public static Image wrap(final ImagePlus imageplus) {
		
		final Object[] slices = imageplus.getImageStack().getImageArray();
		if (slices[0] instanceof byte[]) return new ByteImage(imageplus);
		if (slices[0] instanceof short[]) return new ShortImage(imageplus);
		if (slices[0] instanceof float[]) return new FloatImage(imageplus);
		if (slices[0] instanceof int[]) return new ColorImage(imageplus);
		throw new IllegalArgumentException("Non-supported data type");
	}
	
	/** Returns this image in the form of an {@code ImagePlus} object.
		
		@return a new {@code ImagePlus} object of this {@code Image} object. The two objects share the actual image data.
	*/
	public abstract ImagePlus imageplus();
	
	/** Returns the number of elements in the image.
		
		@return the number of elements in the image.
	*/
	public int elements() { return dims.x*dims.y*dims.z*dims.t*dims.c; }
	
	/** Sets the aspect sizes of the image elements.
		
		@param aspects specifies the aspect size in each dimension.
		
		@exception NullPointerException if {@code aspects} is {@code null}.
	*/
	public void aspects(final Aspects aspects) {
		
		if (aspects == null) throw new NullPointerException();
		this.aspects = aspects;
	}
	
	/** Returns the aspect sizes of the image elements.
		
		@return the aspect sizes of the image elements.
	*/
	public Aspects aspects() { return aspects; }
	
	/** Returns the image's ID.
		
		@return the image's ID. Image IDs are positive integer numbers.
	*/
	public int id() { return thisID; }
	
	/** Returns the amount of memory occupied by the image data.
		
		@return the amount of memory occupied by the image data. The amount is expressed in bytes.
	*/
	public abstract int memory();
	
	/** Returns the dimensions of the image.
		
		@return the dimensions of the image.
	*/
	public Dimensions dimensions() { return dims; }
	
	/** Duplicates this image.
		
		@return a new {@code Image} object that is an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public abstract Image duplicate();
	
	/** Returns the extreme values in the image.
		
		@return a new array containing the extreme values in the image. The array contains only two elements:<br>
		{@code [0]} = the minimum value in the image,<br>
		{@code [1]} = the maximum value in the image.
	*/
	public double[] extrema() {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x) {
							final double val = v[x];
							if (val < min) min = val;
							if (val > max) max = val;
						}
					}
		axes = orgaxes;
		
		return new double[] {min,max};
	}
	
	/** Returns the minimum value in the image.
		
		@return the minimum value in the image.
	*/
	public double minimum() {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		double min = Double.MAX_VALUE;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							if (v[x] < min) min = v[x];
					}
		axes = orgaxes;
		
		return min;
	}
	
	/** Returns the maximum value in the image.
		
		@return the maximum value in the image.
	*/
	public double maximum() {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		double max = -Double.MAX_VALUE;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							if (v[x] > max) max = v[x];
					}
		axes = orgaxes;
		
		return max;
	}
	
	/** Sets the currently active Cartesian coordinate axis or axes. This method affects the operation of the {@code get} and {@code set} methods for reading and writing values.
		
		@param axes an integer number indicating which axis or axes are to be actived. Must be one or a combination (by addition) of {@link Axes#X}, {@link Axes#Y}, {@link Axes#Z}, {@link Axes#T}, {@link Axes#C}.
		
		@exception IllegalArgumentException if {@code axes} does not correspond to a valid combination of axes.
	*/
	public void axes(final int axes) {
		
		if (axes < Axes.NONE || axes > Axes.ALL) throw new IllegalArgumentException("Invalid (combination of) axes");
		
		this.axes = axes;
	}
	
	/** Returns the current Cartesian coordinate axes activity.
		
		@return an integer number indicating which axes are currently active. The returned value is one or a combination (by addition) of {@link Axes#X}, {@link Axes#Y}, {@link Axes#Z}, {@link Axes#T}, {@link Axes#C}.
	*/
	public int axes() { return axes; }
	
	/** Returns the value of the image element at the given coordinates.
		
		@param coords the coordinates of the image element whose value is to be obtained. Coordinates must be within the range determined by the image dimensions. For consistency with the other {@code get} methods, none of the Cartesian coordinate axes should be active. However, this condition is not strictly necessary in order for this method to behave unambiguously and is therefore not checked, so as to increase efficiency.
		
		@return the value of the image element at the given coordinates.
		
		@exception ArrayIndexOutOfBoundsException if any of the coordinates is out of range.
		
		@exception NullPointerException if {@code coords} is {@code null}.
	*/
	public abstract double get(final Coordinates coords);
	
	/** Reads values in one dimension from the image into an array. The dimension from which values are read is determined by the currently active Cartesian coordinate axis. Exactly one axis must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for reading image values into the array. The coordinate corresponding to the active axis is allowed to be outside the range determined by the image size in the corresponding dimension. Out-of-range positions in the active dimension are not actually read but simply skipped.
		
		@param values the array to which the image values are copied.
		
		@exception IllegalStateException if more than one coordinate axis is active.
		
		@exception ArrayIndexOutOfBoundsException if any of the coordinates corresponding to the non-active axes is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void get(final Coordinates coords, final double[] values);
	
	/** Reads values in two dimensions from the image into an array. The dimensions from which values are read are determined by the currently active Cartesian coordinate axes. Exactly two axes must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for reading image values into the given array. The coordinates corresponding to the active axes are allowed to be outside the range determined by the image size in the corresponding dimensions. Out-of-range positions in the active dimensions are not actually read but simply skipped.
		
		@param values the array to which the image values are copied.
		
		@exception IllegalStateException if not exactly two coordinate axes are active.
		
		@exception ArrayIndexOutOfBoundsException if any of the coordinates corresponding to the non-active axes is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void get(final Coordinates coords, final double[][] values);
	
	/** Reads values in three dimensions from the image into an array. The dimensions from which values are read are determined by the currently active Cartesian coordinate axes. Exactly three axes must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for reading image values into the given array. The coordinates corresponding to the active axes are allowed to be outside the range determined by the image size in the corresponding dimensions. Out-of-range positions in the active dimensions are not actually read but simply skipped.
		
		@param values the array to which the image values are copied.
		
		@exception IllegalStateException if not exactly three coordinate axes are active.
		
		@exception ArrayIndexOutOfBoundsException if any of the coordinates corresponding to the non-active axes is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void get(final Coordinates coords, final double[][][] values);
	
	/** Reads values in four dimensions from the image into the given array. The dimensions from which values are read are determined by the currently active Cartesian coordinate axes. Exactly four axes must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for reading image values into the given array. The coordinates corresponding to the active axes are allowed to be outside the range determined by the image size in the corresponding dimensions. Out-of-range positions in the active dimensions are not actually read but simply skipped.
		
		@param values the array to which the image values are copied.
		
		@exception IllegalStateException if not exactly four coordinate axes are active.
		
		@exception ArrayIndexOutOfBoundsException if the coordinate corresponding to the non-active axis is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void get(final Coordinates coords, final double[][][][] values);
	
	/** Reads values in all five dimensions from the image into an array. All five axes must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for reading image values into the given array. Coordinates are allowed to be outside the range determined by the image size. Out-of-range positions are not actually read but simply skipped.
		
		@param values the array to which the image values are copied.
		
		@exception IllegalStateException if not exactly five coordinate axes are active.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void get(final Coordinates coords, final double[][][][][] values);
	
	/** Sets every image element to the given value.
		
		@param value the value to which every image element is to be set.
	*/
	public abstract void set(final double value);
	
	/** Sets the image elements in the borders by mirroring the remainder of the image.
		
		@param borders specifies the size of the border in each dimension.
		
		@exception IllegalArgumentException if the given border size is larger than or equal to half the image size in any dimension.
		
		@exception NullPointerException if {@code borders} is {@code null}.
	*/
	public abstract void mirror(final Borders borders);
	
	/** Sets every image element in the borders to the given value.
		
		@param borders specifies the size of the border in each dimension.
		
		@param value the value to which every border element is to be set.
		
		@exception IllegalArgumentException if the given border size is larger than half the image size in any dimension.
		
		@exception NullPointerException if the first parameter is {@code null}.
	*/
	public abstract void set(final Borders borders, final double value);
	
	/** Sets the image element at the given coordinates to the given value.
		
		@param coords the coordinates of the image element to be set. Coordinates must be within the range determined by the image size. For consistency with the other {@code set} methods, none of the Cartesian coordinate axes should be active. However, this condition is not strictly necessary in order for this method to behave unambiguously and is therefore not checked, so as to increase efficiency.
		
		@param value the value to which the image element is to be set.
		
		@exception ArrayIndexOutOfBoundsException if any of the coordinates is out of range.
		
		@exception NullPointerException if {@code coords} is {@code null}.
	*/
	public abstract void set(final Coordinates coords, final double value);
	
	/** Writes values in one dimension from an array into the image. The dimension into which values are written is determined by the currently active Cartesian coordinate axis. Exactly one axis must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for writing values into the image. The coordinate corresponding to the active axis is allowed to be outside the range determined by the image size in the corresponding dimension. Out-of-range positions in the active dimension are not actually written but simply skipped.
		
		@param values the array from which values are copied to the image.
		
		@exception IllegalStateException if more than one coordinate axis is active.
		
		@exception ArrayIndexOutOfBoundsException if any of the coordinates corresponding to the non-active axes is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void set(final Coordinates coords, final double[] values);
	
	/** Writes values in two dimensions from an array into the image. The dimensions into which values are written are determined by the currently active Cartesian coordinate axes. Exactly two axes must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for writing values into the image. The coordinates corresponding to the active axes are allowed to be outside the range determined by the image size in the corresponding dimensions. Out-of-range positions in the active dimensions are not actually written but simply skipped.
		
		@param values the array from which values are copied to the image.
		
		@exception IllegalStateException if not exactly two coordinate axes are active.
		
		@exception ArrayIndexOutOfBoundsException if any of the coordinates corresponding to the non-active axes is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void set(final Coordinates coords, final double[][] values);
	
	/** Writes values in three dimensions from an array into the image. The dimensions into which values are written are determined by the currently active Cartesian coordinate axes. Exactly three axes must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for writing values into the image. The coordinates corresponding to the active axes are allowed to be outside the range determined by the image size in the corresponding dimensions. Out-of-range positions in the active dimensions are not actually written but simply skipped.
		
		@param values the array from which values are copied to the image.
		
		@exception IllegalStateException if not exactly three coordinate axes are active.
		
		@exception ArrayIndexOutOfBoundsException if any of the coordinates corresponding to the non-active axes is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void set(final Coordinates coords, final double[][][] values);
	
	/** Writes values in four dimensions from an array into the image. The dimensions into which values are written are determined by the currently active Cartesian coordinate axes. Exactly four axes must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for writing values into the image. The coordinates corresponding to the active axes are allowed to be outside the range determined by the image size in the corresponding dimensions. Out-of-range positions in the active dimensions are not actually written but simply skipped.
		
		@param values the array from which values are copied to the image.
		
		@exception IllegalStateException if not exactly four coordinate axes are active.
		
		@exception ArrayIndexOutOfBoundsException if the coordinate corresponding to the non-active axis is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void set(final Coordinates coords, final double[][][][] values);

	/** Writes values in all five dimensions from an array into the image. All five axes must be active in order for this method to work.
		
		@param coords the coordinates of the starting position in the image for writing values into the image. Coordinates are allowed to be outside the range determined by the image size. Out-of-range positions are not actually written but simply skipped.
		
		@param values the array from which values are copied to the image.
		
		@exception IllegalStateException if not all five coordinate axes are active.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public abstract void set(final Coordinates coords, final double[][][][][] values);
	
	/** Replaces every image element by its absolute value. This is done simply by applying method {@code Math.abs()} from the Java language to every image element. */
	public void absolute() {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] = Math.abs(v[x]);
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Adds a value to every image element.
		
		@param value the value to be added to every image element.
	*/
	public void add(final double value) {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] += value;
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Adds an image element-by-element to this image.
		
		@param image the image to be added to this image.
		
		@exception IllegalStateException if the dimensions of the given image are not equal to those of this image.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public void add(final Image image) {
		
		if (!image.dimensions().equals(dims)) throw new IllegalStateException("Unequal image dimensions");
		
		final Coordinates c = new Coordinates();
		final double[] iv = new double[dims.x];
		final double[] v = new double[dims.x];
		final int iorgaxes = image.axes; image.axes = Axes.X;
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						image.get(c,iv);
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] += iv[x];
						set(c,v);
					}
		image.axes = iorgaxes;
		axes = orgaxes;
	}
	
	/** Subtracts a value from every image element.
		
		@param value the value to be subtracted from every image element.
	*/
	public void subtract(final double value) {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] -= value;
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Subtracts an image element-by-element from this image.
		
		@param image the image to be subtracted from this image.
		
		@exception IllegalStateException if the dimensions of the given image are not equal to those of this image.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public void subtract(final Image image) {
		
		if (!image.dimensions().equals(dims)) throw new IllegalStateException("Unequal image dimensions");
		
		final Coordinates c = new Coordinates();
		final double[] iv = new double[dims.x];
		final double[] v = new double[dims.x];
		final int iorgaxes = image.axes; image.axes = Axes.X;
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						image.get(c,iv);
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] -= iv[x];
						set(c,v);
					}
		image.axes = iorgaxes;
		axes = orgaxes;
	}
	
	/** Multiplies every image element with a value.
		
		@param value the value to multiply every image element with.
	*/
	public void multiply(final double value) {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] *= value;
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Multiplies this image element-by-element with an image.
		
		@param image the image to multiply this image with.
		
		@exception IllegalStateException if the dimensions of the given image are not equal to those of this image.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public void multiply(final Image image) {
		
		if (!image.dimensions().equals(dims)) throw new IllegalStateException("Unequal image dimensions");
		
		final Coordinates c = new Coordinates();
		final double[] iv = new double[dims.x];
		final double[] v = new double[dims.x];
		final int iorgaxes = image.axes; image.axes = Axes.X;
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						image.get(c,iv);
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] *= iv[x];
						set(c,v);
					}
		image.axes = iorgaxes;
		axes = orgaxes;
	}
	
	/** Divides every image element by a value.
		
		@param value the value to divide every image element by.
	*/
	public void divide(final double value) {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] /= value;
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Divides this image element-by-element by an image.
		
		@param image the image to divide this image by.
		
		@exception IllegalStateException if the dimensions of the given image are not equal to those of this image.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public void divide(final Image image) {
		
		if (!image.dimensions().equals(dims)) throw new IllegalStateException("Unequal image dimensions");
		
		final Coordinates c = new Coordinates();
		final double[] iv = new double[dims.x];
		final double[] v = new double[dims.x];
		final int iorgaxes = image.axes; image.axes = Axes.X;
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						image.get(c,iv);
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] /= iv[x];
						set(c,v);
					}
		image.axes = iorgaxes;
		axes = orgaxes;
	}
	
	/** Replaces every image element by its square value. */
	public void square() {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] *= v[x];
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Replaces every image element by its square root value. This is done simply by applying the method {@code Math.sqrt()} from the Java language to every image element. */
	public void squareroot() {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] = Math.sqrt(v[x]);
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Replaces every image element by its value raised to a power. This is done simply by applying the method {@code Math.pow()} from the Java language to every image element.
		
		@param power the power to raise every image element to.
	*/
	public void power(final double power) {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] = Math.pow(v[x],power);
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Replaces every image element by its inverted value. Inversion is constrained to the range defined by the minimum and the maximum value in the image. That is, after this operation, the original minimum has become the maximum, and vice versa, with all other values transformed accordingly. */
	public void invert() {
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final double[] mm = extrema();
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						get(c,v);
						for (int x=0; x<dims.x; ++x)
							v[x] = mm[1] - (v[x] - mm[0]);
						set(c,v);
					}
		axes = orgaxes;
	}
	
	/** Sets the name of this image.
		
		@param name the new name of the image. If this parameter is {@code null}, it is replaced by an empty string.
	*/
	public void name(final String name) {
		
		if (name == null) this.name = "";
		else this.name = name;
	}
	
	/** Returns the name of this image.
		
		@return the name of this image.
	*/
	public String name() { return name; }
	
}
