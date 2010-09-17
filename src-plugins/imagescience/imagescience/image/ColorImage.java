package imagescience.image;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import imagescience.utility.FMath;

/** An image containing up to 5D elements of type {@code int} representing aRGB-color values. The color components are stored in an {@code int} as follows:<br>
<ul>
<li>bits 0-7 (the first and least significant byte) = the blue component (represented by {@link #BLUE}),</li>
<li>bits 8-15 (the second byte) = the green component (represented by {@link #GREEN}),</li>
<li>bits 16-23 (the third byte) = the red component (represented by {@link #RED}),</li>
<li>bits 24-31 (the fourth and most significant byte) = the alpha component (represented by {@link #ALPHA}).</li>
</ul>
Element values are taken in the unsigned sense, and therefore the {@code get} methods always return numbers in the range [0,255] if a single component is activated (using method {@link #component(int)}). Insertion of floating-point numbers in any of these components by using the {@code set} methods is done by first clipping to the range [0,255] and then rounding to the nearest 8-bit integer value. It is also possible (by setting the active component to {@link #FULL}) to {@code get} and {@code set} the full 32-bit integer value rather than a single component. */
public class ColorImage extends Image {
	
	/** The alpha component (stored in bits 24-31). */
	public final static int ALPHA = 0;
	
	/** The red component (stored in bits 16-23). */
	public final static int RED = 1;
	
	/** The green component (stored in bits 8-15). */
	public final static int GREEN = 2;
	
	/** The blue component (stored in bits 0-7). */
	public final static int BLUE = 3;
	
	/** The full 32-bit integer value. */
	public final static int FULL = 4;
	
	int component = FULL;
	int getmask, setmask, bitshift;
	double min, max;
	
	final int[][][][] elements;
	
	/** Dimensions constructor.
		
		@param dims the dimensions of the new image.
		
		@exception NullPointerException if {@code dims} is {@code null}.
	*/
	public ColorImage(final Dimensions dims) {
		
		super(dims.duplicate());
		
		elements = new int[dims.c][dims.t][dims.z][dims.y*dims.x];
		
		component(component);
	}
	
	/** Wrapper constructor.
		
		@param imageplus the {@code ImagePlus} object whose image data is to be wrapped. The actual image data is not copied but shared.
		
		@exception IllegalArgumentException if the image elements of {@code imageplus} are not of type {@code int}.
		
		@exception NullPointerException if {@code imageplus} is {@code null}.
	*/
	public ColorImage(final ImagePlus imageplus) {
		
		super(
			new Dimensions(
				imageplus.getWidth(),
				imageplus.getHeight(),
				imageplus.getNSlices(),
				imageplus.getNFrames(),
				imageplus.getNChannels()
			)
		);
		
		final Calibration cal = imageplus.getCalibration();
		aspects = new Aspects(
			cal.pixelWidth,
			cal.pixelHeight,
			cal.pixelDepth,
			cal.frameInterval,
			1
		);
		
		component(component);
		
		name = imageplus.getTitle();
		
		final Object[] slices = imageplus.getImageStack().getImageArray();
		if (!(slices[0] instanceof int[])) throw new IllegalArgumentException("Wrong input type");
		
		elements = new int[dims.c][dims.t][dims.z][];
		for (int t=0; t<dims.t; ++t)
			for (int z=0; z<dims.z; ++z)
				for (int c=0; c<dims.c; ++c)
					elements[c][t][z] = (int[])slices[t*dims.z*dims.c + z*dims.c + c];
	}
	
	/** Copy constructor.
		
		@param image the image to copy from. Image element values are copied using a {@code get} method of the given image and the corresponding {@code set} method of this image. This enables copying from images that are of different type than this image. Be aware, however, of the value conversion rules of the respective methods when copying from images that are not of the same type as this image.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public ColorImage(final Image image) {
		
		super(image.dimensions().duplicate());
		aspects = image.aspects().duplicate();
		elements = new int[dims.c][dims.t][dims.z][dims.y*dims.x];
		component(component);
		name = new String(image.name);
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int iorgaxes = image.axes;
		image.axes = axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						image.get(c,v);
						set(c,v);
					}
		image.axes = axes = iorgaxes;
	}
	
	/** Copy constructor that allows adding borders. Creates a new image whose size in each dimension is equal to that of the given image plus twice the given border size in that dimension.
		
		@param image the image to copy from. Image element values are copied using a {@code get} method of the given image and the corresponding {@code set} method of this image, taking into account the new border sizes. This enables copying from images that are of a different type than this image. Be aware, however, of the value conversion rules of the respective methods when copying from images that are not of the same type as this image.
		
		@param borders specifies the border size in each dimension of the new image.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public ColorImage(final Image image, final Borders borders) {
		
		super(
			new Dimensions(
				image.dimensions().x + 2*borders.x,
				image.dimensions().y + 2*borders.y,
				image.dimensions().z + 2*borders.z,
				image.dimensions().t + 2*borders.t,
				image.dimensions().c + 2*borders.c
			)
		);
		
		aspects = image.aspects().duplicate();
		elements = new int[dims.c][dims.t][dims.z][dims.y*dims.x];
		component(component);
		name = new String(image.name);
		
		final Dimensions idims = image.dimensions();
		final Coordinates ic = new Coordinates();
		final Coordinates c = new Coordinates();
		final double[] v = new double[idims.x];
		final int iorgaxes = image.axes;
		image.axes = axes = Axes.X;
		c.x = borders.x;
		for (ic.c=0, c.c=borders.c; ic.c<idims.c; ++ic.c, ++c.c)
			for (ic.t=0, c.t=borders.t; ic.t<idims.t; ++ic.t, ++c.t)
				for (ic.z=0, c.z=borders.z; ic.z<idims.z; ++ic.z, ++c.z)
					for (ic.y=0, c.y=borders.y; ic.y<idims.y; ++ic.y, ++c.y) {
						image.get(ic,v);
						set(c,v);
					}
		image.axes = axes = iorgaxes;
	}
	
	public Image border(final Borders borders) {
		
		final int orgcomp = component; component(FULL);
		final ColorImage cimg = new ColorImage(this,borders);
		component(orgcomp); cimg.component(orgcomp);
		
		return cimg;
	}
	
	/** Indicates the currently active component.
		
		@return the currently active component. The returned value is one of the static fields of this class. By default this is {@link #FULL}.
	*/
	public int component() { return component; }
	
	/** Sets the active component.
		
		@param component the component to be activated. Must be one of the static fields of this class. The {@code get} and {@code set} methods consider only the currently active component. By default this is {@link #FULL}.
		
		@exception IllegalArgumentException if {@code component} is not one of the static fields of this class.
	*/
	public void component(final int component) {
		
		this.component = component;
		
		switch (component) {
			case ALPHA:
				getmask = 0xFF000000;
				setmask = 0x00FFFFFF;
				bitshift = 24;
				max = 0xFF;
				min = 0;
				break;
			case RED:
				getmask = 0x00FF0000;
				setmask = 0xFF00FFFF;
				bitshift = 16;
				max = 0xFF;
				min = 0;
				break;
			case GREEN:
				getmask = 0x0000FF00;
				setmask = 0xFFFF00FF;
				bitshift = 8;
				max = 0xFF;
				min = 0;
				break;
			case BLUE:
				getmask = 0x000000FF;
				setmask = 0xFFFFFF00;
				bitshift = 0;
				max = 0xFF;
				min = 0;
				break;
			case FULL:
				getmask = 0xFFFFFFFF;
				setmask = 0x00000000;
				bitshift = 0;
				max = Integer.MAX_VALUE;
				min = Integer.MIN_VALUE;
				break;
			default:
				throw new IllegalArgumentException("Non-supported component");
		}
	}
	
	public ImagePlus imageplus() {
		
		final ImageStack imagestack = new ImageStack(dims.x,dims.y);
		for (int t=0; t<dims.t; ++t)
			for (int z=0; z<dims.z; ++z)
				for (int c=0; c<dims.c; ++c)
					imagestack.addSlice("",elements[c][t][z]);
		
		final Calibration cal = new Calibration();
		cal.pixelWidth = aspects.x;
		cal.pixelHeight = aspects.y;
		cal.pixelDepth = aspects.z;
		cal.frameInterval = aspects.t;
		
		ImagePlus imageplus = new ImagePlus(name,imagestack);
		imageplus.setDimensions(dims.c,dims.z,dims.t);
		imageplus.setCalibration(cal);
		
		return imageplus;
	}
	
	public String type() { return "imagescience.image.ColorImage"; }
	
	public int memory() { return 4*elements(); }
	
	public Image duplicate() {
		
		final ColorImage dupimg = new ColorImage(dims);
		dupimg.aspects = aspects.duplicate();
		dupimg.name = new String(name);
		dupimg.axes = axes;
		dupimg.component(component);
		
		final int dimsxy = dims.x*dims.y;
		for (int c=0; c<dims.c; ++c)
			for (int t=0; t<dims.t; ++t)
				for (int z=0; z<dims.z; ++z) {
					final int[] thiselms = elements[c][t][z];
					final int[] dupelms = dupimg.elements[c][t][z];
					for (int xy=0; xy<dimsxy; ++xy)
						dupelms[xy] = thiselms[xy];
				}
		
		return dupimg;
	}
	
	/** Returns the currently active component as a new {@code Image}.
		
		@return a new {@code Image} containing a copy of the currently active component. If the active component is one of {@link #ALPHA}, {@link #RED}, {@link #GREEN}, {@link #BLUE}, the returned object is an instance of {@link ByteImage} and contains the element values of the corresponding component. If it is {@link #FULL}, the returned object is an instance of {@link ColorImage} and contains a copy of this image.
	*/
	public Image get() {
		
		switch (component) {
			case ALPHA:
			case RED:
			case GREEN:
			case BLUE: return new ByteImage(this);
			case FULL: return duplicate();
		}
		
		return null;
	}
	
	public double get(final Coordinates coords) {
		
		return (getmask&elements[coords.c][coords.t][coords.z][dims.x*coords.y + coords.x])>>bitshift;
	}
	
	public void get(final Coordinates coords, final double[] values) {
		
		switch (axes) {
			case Axes.X: {
				int vxstart = 0;
				int exstart = coords.x;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				int exstop = coords.x + values.length;
				if (exstop > dims.x) exstop = dims.x;
				final int[] elms = elements[coords.c][coords.t][coords.z];
				for (int x=exstart, ex=coords.y*dims.x+exstart, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
					values[vx] = (getmask&elms[ex])>>bitshift;
				break;
			}
			case Axes.Y: {
				int vystart = 0;
				int eystart = coords.y;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				int eystop = coords.y + values.length;
				if (eystop > dims.y) eystop = dims.y;
				final int[] elms = elements[coords.c][coords.t][coords.z];
				for (int y=eystart, ey=eystart*dims.x+coords.x, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
					values[vy] = (getmask&elms[ey])>>bitshift;
				break;
			}
			case Axes.Z: {
				int vzstart = 0;
				int ezstart = coords.z;
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				int ezstop = coords.z + values.length;
				if (ezstop > dims.z) ezstop = dims.z;
				final int exy = coords.y*dims.x + coords.x;
				final int[][] elms = elements[coords.c][coords.t];
				for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
					values[vz] = (getmask&elms[ez][exy])>>bitshift;
				break;
			}
			case Axes.T: {
				int vtstart = 0;
				int etstart = coords.t;
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int etstop = coords.t + values.length;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = coords.y*dims.x + coords.x;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					values[vt] = (getmask&elements[coords.c][et][coords.z][exy])>>bitshift;
				break;
			}
			case Axes.C: {
				int vcstart = 0;
				int ecstart = coords.c;
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int ecstop = coords.c + values.length;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					values[vc] = (getmask&elements[ec][coords.t][coords.z][exy])>>bitshift;
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	public void get(final Coordinates coords, final double[][] values) {
		
		switch (axes) {
			case Axes.YX: {
				int vxstart = 0;
				int vystart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				int exstop = coords.x + values[0].length;
				int eystop = coords.y + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				final int[] elms = elements[coords.c][coords.t][coords.z];
				for (int y=eystart, ey=eystart*dims.x+exstart, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
					for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
						values[vy][vx] = (getmask&elms[ex])>>bitshift;
				break;
			}
			case Axes.ZX: {
				int vxstart = 0;
				int vzstart = 0;
				int exstart = coords.x;
				int ezstart = coords.z;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				int exstop = coords.x + values[0].length;
				int ezstop = coords.z + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ezstop > dims.z) ezstop = dims.z;
				final int exy = coords.y*dims.x + exstart;
				final int[][] elms = elements[coords.c][coords.t];
				for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
					for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
						values[vz][vx] = (getmask&elms[ez][ex])>>bitshift;
				break;
			}
			case Axes.ZY: {
				int vystart = 0;
				int vzstart = 0;
				int eystart = coords.y;
				int ezstart = coords.z;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				int eystop = coords.y + values[0].length;
				int ezstop = coords.z + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				final int exy = eystart*dims.x + coords.x;
				final int[][] elms = elements[coords.c][coords.t];
				for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						values[vz][vy] = (getmask&elms[ez][ey])>>bitshift;
				break;
			}
			case Axes.TX: {
				int vxstart = 0;
				int vtstart = 0;
				int exstart = coords.x;
				int etstart = coords.t;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int exstop = coords.x + values[0].length;
				int etstop = coords.t + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = coords.y*dims.x + exstart;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
						values[vt][vx] = (getmask&elements[coords.c][et][coords.z][ex])>>bitshift;
				break;
			}
			case Axes.TY: {
				int vystart = 0;
				int vtstart = 0;
				int eystart = coords.y;
				int etstart = coords.t;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int eystop = coords.y + values[0].length;
				int etstop = coords.t + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = eystart*dims.x + coords.x;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						values[vt][vy] = (getmask&elements[coords.c][et][coords.z][ey])>>bitshift;
				break;
			}
			case Axes.TZ: {
				int vzstart = 0;
				int vtstart = 0;
				int ezstart = coords.z;
				int etstart = coords.t;
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int ezstop = coords.z + values[0].length;
				int etstop = coords.t + values.length;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = coords.y*dims.x + coords.x;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						values[vt][vz] = (getmask&elements[coords.c][et][ez][exy])>>bitshift;
				break;
			}
			case Axes.CX: {
				int vxstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
						values[vc][vx] = (getmask&elements[ec][coords.t][coords.z][ex])>>bitshift;
				break;
			}
			case Axes.CY: {
				int vystart = 0;
				int vcstart = 0;
				int eystart = coords.y;
				int ecstart = coords.c;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int eystop = coords.y + values[0].length;
				int ecstop = coords.c + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						values[vc][vy] = (getmask&elements[ec][coords.t][coords.z][ey])>>bitshift;
				break;
			}
			case Axes.CZ: {
				int vzstart = 0;
				int vcstart = 0;
				int ezstart = coords.z;
				int ecstart = coords.c;
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int ezstop = coords.z + values[0].length;
				int ecstop = coords.c + values.length;
				if (ezstop > dims.z) ezstop = dims.z;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						values[vc][vz] = (getmask&elements[ec][coords.t][ez][exy])>>bitshift;
				break;
			}
			case Axes.CT: {
				int vtstart = 0;
				int vcstart = 0;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						values[vc][vt] = (getmask&elements[ec][et][coords.z][exy])>>bitshift;
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	public void get(final Coordinates coords, final double[][][] values) {
		
		switch (axes) {
			case Axes.ZYX: {
				int vxstart = 0;
				int vystart = 0;
				int vzstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ezstart = coords.z;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				int exstop = coords.x + values[0][0].length;
				int eystop = coords.y + values[0].length;
				int ezstop = coords.z + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				final int exy = eystart*dims.x + exstart;
				final int[][] elms = elements[coords.c][coords.t];
				for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
							values[vz][vy][vx] = (getmask&elms[ez][ex])>>bitshift;
				break;
			}
			case Axes.TYX: {
				int vxstart = 0;
				int vystart = 0;
				int vtstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int etstart = coords.t;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int exstop = coords.x + values[0][0].length;
				int eystop = coords.y + values[0].length;
				int etstop = coords.t + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = eystart*dims.x + exstart;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
							values[vt][vy][vx] = (getmask&elements[coords.c][et][coords.z][ex])>>bitshift;
				break;
			}
			case Axes.CYX: {
				int vxstart = 0;
				int vystart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0].length;
				int eystop = coords.y + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
							values[vc][vy][vx] = (getmask&elements[ec][coords.t][coords.z][ex])>>bitshift;
				break;
			}
			case Axes.TZX: {
				int vxstart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int exstart = coords.x;
				int ezstart = coords.z;
				int etstart = coords.t;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int exstop = coords.x + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int etstop = coords.t + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = coords.y*dims.x + exstart;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
							values[vt][vz][vx] = (getmask&elements[coords.c][et][ez][ex])>>bitshift;
				break;
			}
			case Axes.CZX: {
				int vxstart = 0;
				int vzstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int ezstart = coords.z;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ezstop > dims.z) ezstop = dims.z;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
							values[vc][vz][vx] = (getmask&elements[ec][coords.t][ez][ex])>>bitshift;
				break;
			}
			case Axes.CTX: {
				int vxstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
							values[vc][vt][vx] = (getmask&elements[ec][et][coords.z][ex])>>bitshift;
				break;
			}
			case Axes.TZY: {
				int vystart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int eystart = coords.y;
				int ezstart = coords.z;
				int etstart = coords.t;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int eystop = coords.y + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int etstop = coords.t + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = eystart*dims.x + coords.x;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							values[vt][vz][vy] = (getmask&elements[coords.c][et][ez][ey])>>bitshift;
				break;
			}
			case Axes.CZY: {
				int vystart = 0;
				int vzstart = 0;
				int vcstart = 0;
				int eystart = coords.y;
				int ezstart = coords.z;
				int ecstart = coords.c;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int eystop = coords.y + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int ecstop = coords.c + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							values[vc][vz][vy] = (getmask&elements[ec][coords.t][ez][ey])>>bitshift;
				break;
			}
			case Axes.CTY: {
				int vystart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int eystart = coords.y;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int eystop = coords.y + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							values[vc][vt][vy] = (getmask&elements[ec][et][coords.z][ey])>>bitshift;
				break;
			}
			case Axes.CTZ: {
				int vzstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int ezstart = coords.z;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int ezstop = coords.z + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
							values[vc][vt][vz] = (getmask&elements[ec][et][ez][exy])>>bitshift;
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	public void get(final Coordinates coords, final double[][][][] values) {
		
		switch (axes) {
			case Axes.TZYX: {
				int vxstart = 0;
				int vystart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ezstart = coords.z;
				int etstart = coords.t;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int exstop = coords.x + values[0][0][0].length;
				int eystop = coords.y + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int etstop = coords.t + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = eystart*dims.x + exstart;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
								values[vt][vz][vy][vx] = (getmask&elements[coords.c][et][ez][ex])>>bitshift;
				break;
			}
			case Axes.CZYX: {
				int vxstart = 0;
				int vystart = 0;
				int vzstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ezstart = coords.z;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0][0].length;
				int eystop = coords.y + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
								values[vc][vz][vy][vx] = (getmask&elements[ec][coords.t][ez][ex])>>bitshift;
				break;
			}
			case Axes.CTYX: {
				int vxstart = 0;
				int vystart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0][0].length;
				int eystop = coords.y + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
								values[vc][vt][vy][vx] = (getmask&elements[ec][et][coords.z][ex])>>bitshift;
				break;
			}
			case Axes.CTZX: {
				int vxstart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int ezstart = coords.z;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0][0].length;
				int ezstop = coords.z + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
							for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
								values[vc][vt][vz][vx] = (getmask&elements[ec][et][ez][ex])>>bitshift;
				break;
			}
			case Axes.CTZY: {
				int vystart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int eystart = coords.y;
				int ezstart = coords.z;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int eystop = coords.y + values[0][0][0].length;
				int ezstop = coords.z + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
							for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
								values[vc][vt][vz][vy] = (getmask&elements[ec][et][ez][ey])>>bitshift;
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	public void get(final Coordinates coords, final double[][][][][] values) {
		
		switch (axes) {
			case Axes.CTZYX: {
				int vxstart = 0;
				int vystart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ezstart = coords.z;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0][0][0].length;
				int eystop = coords.y + values[0][0][0].length;
				int ezstop = coords.z + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
							for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
								for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx)
									values[vc][vt][vz][vy][vx] = (getmask&elements[ec][et][ez][ex])>>bitshift;
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	/** Sets the currently active component to the given image.
		
		@param image the image to set the currently active component to. Image element values are copied using a {@code get} method of the given image and the corresponding {@code set} method of this image. Be aware of the value conversion rules of the respective methods.
		
		@exception IllegalStateException if the size of the given image is not equal to that of this image in every dimension.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public void set(final Image image) {
		
		if (!image.dimensions().equals(dims)) throw new IllegalStateException("Unequal image dimensions");
		
		final Coordinates c = new Coordinates();
		final double[] v = new double[dims.x];
		final int iorgaxes = image.axes; image.axes = Axes.X;
		final int orgaxes = axes; axes = Axes.X;
		for (c.c=0; c.c<dims.c; ++c.c)
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						image.get(c,v);
						set(c,v);
					}
		image.axes = iorgaxes;
		axes = orgaxes;
	}
	
	public void set(final double value) {
		
		final int dimsxy = dims.x*dims.y;
		final int ivalue = FMath.round((value>max)?max:((value<min)?min:value))<<bitshift;
		for (int c=0; c<dims.c; ++c)
			for (int t=0; t<dims.t; ++t)
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
	}
	
	public void mirror(final Borders borders) {
		
		if (borders.x > (dims.x-1)/2 ||
			borders.y > (dims.y-1)/2 ||
			borders.z > (dims.z-1)/2 ||
			borders.t > (dims.t-1)/2 ||
			borders.c > (dims.c-1)/2)
			throw new IllegalArgumentException("Border(s) too large");
		
		// Initialize:
		final int dimsxb = dims.x - borders.x;
		final int dimsyb = dims.y - borders.y;
		final int dimszb = dims.z - borders.z;
		final int dimstb = dims.t - borders.t;
		final int dimscb = dims.c - borders.c;
		
		final int dimsx2b = dims.x - 2*borders.x;
		final int dimsy2b = dims.y - 2*borders.y;
		final int dimsz2b = dims.z - 2*borders.z;
		final int dimst2b = dims.t - 2*borders.t;
		final int dimsc2b = dims.c - 2*borders.c;
		
		final int dimsx2b1 = (dimsx2b == 1) ? dimsx2b : (dimsx2b - 1);
		final int dimsy2b1 = (dimsy2b == 1) ? dimsy2b : (dimsy2b - 1);
		final int dimsz2b1 = (dimsz2b == 1) ? dimsz2b : (dimsz2b - 1);
		final int dimst2b1 = (dimst2b == 1) ? dimst2b : (dimst2b - 1);
		final int dimsc2b1 = (dimsc2b == 1) ? dimsc2b : (dimsc2b - 1);
		
		final int ix = (dimsx2b == 1) ? 1 : 2;
		final int iy = (dimsy2b == 1) ? 1 : 2;
		final int iz = (dimsz2b == 1) ? 1 : 2;
		final int it = (dimst2b == 1) ? 1 : 2;
		final int ic = (dimsc2b == 1) ? 1 : 2;
		
		final int dimsxy = dims.x*dims.y;
		
		// Mirror borders in x-dimension:
		final int dimsxby = borders.y*dims.x;
		for (int c=borders.c; c<dimscb; ++c)
			for (int t=borders.t; t<dimstb; ++t)
				for (int z=borders.z; z<dimszb; ++z) {
					for (int x=0; x<borders.x; ++x) {
						final int xdiff = x - borders.x;
						int x0 = xdiff/dimsx2b1; x0 += x0%ix;
						final int xmap = borders.x + Math.abs(xdiff - x0*dimsx2b1);
						for (int y=borders.y, xy=dimsxby+x, xymap=dimsxby+xmap; y<dimsyb; ++y, xy+=dims.x, xymap+=dims.x)
							elements[c][t][z][xy] = elements[c][t][z][xymap];
					}
					for (int x=dimsxb; x<dims.x; ++x) {
						final int xdiff = x - borders.x;
						int x0 = xdiff/dimsx2b1; x0 += x0%ix;
						final int xmap = borders.x + Math.abs(xdiff - x0*dimsx2b1);
						for (int y=borders.y, xy=dimsxby+x, xymap=dimsxby+xmap; y<dimsyb; ++y, xy+=dims.x, xymap+=dims.x)
							elements[c][t][z][xy] = elements[c][t][z][xymap];
					}
				}
		
		// Mirror borders in y-dimension:
		for (int c=borders.c; c<dimscb; ++c)
			for (int t=borders.t; t<dimstb; ++t)
				for (int z=borders.z; z<dimszb; ++z) {
					for (int y=0; y<borders.y; ++y) {
						final int ydiff = y - borders.y;
						int y0 = ydiff/dimsy2b1; y0 += y0%iy;
						final int ymap = borders.y + Math.abs(ydiff - y0*dimsy2b1);
						for (int x=0, xy=y*dims.x, xymap=ymap*dims.x; x<dims.x; ++x, ++xy, ++xymap)
							elements[c][t][z][xy] = elements[c][t][z][xymap];
					}
					for (int y=dimsyb; y<dims.y; ++y) {
						final int ydiff = y - borders.y;
						int y0 = ydiff/dimsy2b1; y0 += y0%iy;
						final int ymap = borders.y + Math.abs(ydiff - y0*dimsy2b1);
						for (int x=0, xy=y*dims.x, xymap=ymap*dims.x; x<dims.x; ++x, ++xy, ++xymap)
							elements[c][t][z][xy] = elements[c][t][z][xymap];
					}
				}
		
		// Mirror borders in z-dimension:
		for (int c=borders.c; c<dimscb; ++c)
			for (int t=borders.t; t<dimstb; ++t) {
				for (int z=0; z<borders.z; ++z) {
					final int zdiff = z - borders.z;
					int z0 = zdiff/dimsz2b1; z0 += z0%iz;
					final int zmap = borders.z + Math.abs(zdiff - z0*dimsz2b1);
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = elements[c][t][zmap][xy];
				}
				for (int z=dimszb; z<dims.z; ++z) {
					final int zdiff = z - borders.z;
					int z0 = zdiff/dimsz2b1; z0 += z0%iz;
					final int zmap = borders.z + Math.abs(zdiff - z0*dimsz2b1);
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = elements[c][t][zmap][xy];
				}
			}
		
		// Mirror borders in t-dimension:
		for (int c=borders.c; c<dimscb; ++c) {
			for (int t=0; t<borders.t; ++t) {
				final int tdiff = t - borders.t;
				int t0 = tdiff/dimst2b1; t0 += t0%it;
				final int tmap = borders.t + Math.abs(tdiff - t0*dimst2b1);
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = elements[c][tmap][z][xy];
			}
			for (int t=dimstb; t<dims.t; ++t) {
				final int tdiff = t - borders.t;
				int t0 = tdiff/dimst2b1; t0 += t0%it;
				final int tmap = borders.t + Math.abs(tdiff - t0*dimst2b1);
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = elements[c][tmap][z][xy];
			}
		}
		
		// Mirror borders in c-dimension:
		for (int c=0; c<borders.c; ++c) {
			final int cdiff = c - borders.c;
			int c0 = cdiff/dimsc2b1; c0 += c0%ic;
			final int cmap = borders.c + Math.abs(cdiff - c0*dimsc2b1);
			for (int t=0; t<dims.t; ++t)
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = elements[cmap][t][z][xy];
		}
		for (int c=dimscb; c<dims.c; ++c) {
			final int cdiff = c - borders.c;
			int c0 = cdiff/dimsc2b1; c0 += c0%ic;
			final int cmap = borders.c + Math.abs(cdiff - c0*dimsc2b1);
			for (int t=0; t<dims.t; ++t)
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = elements[cmap][t][z][xy];
		}
	}
	
	public void set(final Borders borders, final double value) {
		
		if (borders.x > dims.x/2 ||
			borders.y > dims.y/2 ||
			borders.z > dims.z/2 ||
			borders.t > dims.t/2 ||
			borders.c > dims.c/2)
			throw new IllegalArgumentException("Border(s) too large");
		
		// Initialize:
		final int dimscb = dims.c - borders.c;
		final int dimstb = dims.t - borders.t;
		final int dimszb = dims.z - borders.z;
		final int dimsyb = dims.y - borders.y;
		final int dimsxb = dims.x - borders.x;
		final int dimsxy = dims.x*dims.y;
		
		final int ivalue = FMath.round((value>max)?max:((value<min)?min:value))<<bitshift;
		
		// Fill borders in c-dimension:
		for (int c=0; c<borders.c; ++c)
			for (int t=0; t<dims.t; ++t)
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
		for (int c=dimscb; c<dims.c; ++c)
			for (int t=0; t<dims.t; ++t)
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
		
		// Fill borders in t-dimension:
		for (int c=borders.c; c<dimscb; ++c) {
			for (int t=0; t<borders.t; ++t)
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
			for (int t=dimstb; t<dims.t; ++t)
				for (int z=0; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
		}
		
		// Fill borders in z-dimension:
		for (int c=borders.c; c<dimscb; ++c)
			for (int t=borders.t; t<dimstb; ++t) {
				for (int z=0; z<borders.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
				for (int z=dimszb; z<dims.z; ++z)
					for (int xy=0; xy<dimsxy; ++xy)
						elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
			}
		
		// Fill borders in y-dimension:
		for (int c=borders.c; c<dimscb; ++c)
			for (int t=borders.t; t<dimstb; ++t)
				for (int z=borders.z; z<dimszb; ++z) {
					for (int y=0, xy=0; y<borders.y; ++y)
						for (int x=0; x<dims.x; ++x, ++xy)
							elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
					for (int y=dimsyb, xy=dimsyb*dims.x; y<dims.y; ++y)
						for (int x=0; x<dims.x; ++x, ++xy)
							elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
				}
		
		// Fill borders in x-dimension:
		for (int c=borders.c; c<dimscb; ++c)
			for (int t=borders.t; t<dimstb; ++t)
				for (int z=borders.z; z<dimszb; ++z)
					for (int y=borders.y; y<dimsyb; ++y) {
						for (int x=0, xy=y*dims.x; x<borders.x; ++x, ++xy)
							elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
						for (int x=dimsxb, xy=y*dims.x+dimsxb; x<dims.x; ++x, ++xy)
							elements[c][t][z][xy] = (setmask&elements[c][t][z][xy])|ivalue;
					}
	}
	
	public void set(final Coordinates coords, final double value) {
		
		elements[coords.c][coords.t][coords.z][coords.y*dims.x + coords.x] =
			(setmask&elements[coords.c][coords.t][coords.z][coords.y*dims.x + coords.x])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
	}
	
	public void set(final Coordinates coords, final double[] values) {
		
		switch (axes) {
			case Axes.X: {
				int vxstart = 0;
				int exstart = coords.x;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				int exstop = coords.x + values.length;
				if (exstop > dims.x) exstop = dims.x;
				final int[] elms = elements[coords.c][coords.t][coords.z];
				for (int x=exstart, ex=coords.y*dims.x+exstart, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
					final double value = values[vx];
					elms[ex] = (setmask&elms[ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
				}
				break;
			}
			case Axes.Y: {
				int vystart = 0;
				int eystart = coords.y;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				int eystop = coords.y + values.length;
				if (eystop > dims.y) eystop = dims.y;
				final int[] elms = elements[coords.c][coords.t][coords.z];
				for (int y=eystart, ey=eystart*dims.x+coords.x, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy) {
					final double value = values[vy];
					elms[ey] = (setmask&elms[ey])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
				}
				break;
			}
			case Axes.Z: {
				int vzstart = 0;
				int ezstart = coords.z;
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				int ezstop = coords.z + values.length;
				if (ezstop > dims.z) ezstop = dims.z;
				final int exy = coords.y*dims.x + coords.x;
				final int[][] elms = elements[coords.c][coords.t];
				for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz) {
					final double value = values[vz];
					elms[ez][exy] = (setmask&elms[ez][exy])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
				}
				break;
			}
			case Axes.T: {
				int vtstart = 0;
				int etstart = coords.t;
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int etstop = coords.t + values.length;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = coords.y*dims.x + coords.x;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt) {
					final double value = values[vt];
					elements[coords.c][et][coords.z][exy] =
						(setmask&elements[coords.c][et][coords.z][exy])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
				}
				break;
			}
			case Axes.C: {
				int vcstart = 0;
				int ecstart = coords.c;
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int ecstop = coords.c + values.length;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc) {
					final double value = values[vc];
					elements[ec][coords.t][coords.z][exy] =
						(setmask&elements[ec][coords.t][coords.z][exy])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
				}
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	public void set(final Coordinates coords, final double[][] values) {
		
		switch (axes) {
			case Axes.YX: {
				int vxstart = 0;
				int vystart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				int exstop = coords.x + values[0].length;
				int eystop = coords.y + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				final int[] elms = elements[coords.c][coords.t][coords.z];
				for (int y=eystart, ey=eystart*dims.x+exstart, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
					for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
						final double value = values[vy][vx];
						elms[ex] = (setmask&elms[ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.ZX: {
				int vxstart = 0;
				int vzstart = 0;
				int exstart = coords.x;
				int ezstart = coords.z;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				int exstop = coords.x + values[0].length;
				int ezstop = coords.z + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ezstop > dims.z) ezstop = dims.z;
				final int exy = coords.y*dims.x + exstart;
				final int[][] elms = elements[coords.c][coords.t];
				for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
					for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
						final double value = values[vz][vx];
						elms[ez][ex] = (setmask&elms[ez][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.TX: {
				int vxstart = 0;
				int vtstart = 0;
				int exstart = coords.x;
				int etstart = coords.t;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int exstop = coords.x + values[0].length;
				int etstop = coords.t + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = coords.y*dims.x + exstart;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
						final double value = values[vt][vx];
						elements[coords.c][et][coords.z][ex] =
							(setmask&elements[coords.c][et][coords.z][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.ZY: {
				int vystart = 0;
				int vzstart = 0;
				int eystart = coords.y;
				int ezstart = coords.z;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				int eystop = coords.y + values[0].length;
				int ezstop = coords.z + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				final int exy = eystart*dims.x + coords.x;
				for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy) {
						final double value = values[vz][vy];
						elements[coords.c][coords.t][ez][ey] =
							(setmask&elements[coords.c][coords.t][ez][ey])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.TY: {
				int vystart = 0;
				int vtstart = 0;
				int eystart = coords.y;
				int etstart = coords.t;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int eystop = coords.y + values[0].length;
				int etstop = coords.t + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = eystart*dims.x + coords.x;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy) {
						final double value = values[vt][vy];
						elements[coords.c][et][coords.z][ey] =
							(setmask&elements[coords.c][et][coords.z][ey])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.TZ: {
				int vzstart = 0;
				int vtstart = 0;
				int ezstart = coords.z;
				int etstart = coords.t;
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int ezstop = coords.z + values[0].length;
				int etstop = coords.t + values.length;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = coords.y*dims.x + coords.x;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz) {
						final double value = values[vt][vz];
						elements[coords.c][et][ez][exy] =
							(setmask&elements[coords.c][et][ez][exy])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.CX: {
				int vxstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
						final double value = values[vc][vx];
						elements[ec][coords.t][coords.z][ex] =
							(setmask&elements[ec][coords.t][coords.z][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.CY: {
				int vystart = 0;
				int vcstart = 0;
				int eystart = coords.y;
				int ecstart = coords.c;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int eystop = coords.y + values[0].length;
				int ecstop = coords.c + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy) {
						final double value = values[vc][vy];
						elements[ec][coords.t][coords.z][ey] =
							(setmask&elements[ec][coords.t][coords.z][ey])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.CZ: {
				int vzstart = 0;
				int vcstart = 0;
				int ezstart = coords.z;
				int ecstart = coords.c;
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int ezstop = coords.z + values[0].length;
				int ecstop = coords.c + values.length;
				if (ezstop > dims.z) ezstop = dims.z;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz) {
						final double value = values[vc][vz];
						elements[ec][coords.t][ez][exy] =
							(setmask&elements[ec][coords.t][ez][exy])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			case Axes.CT: {
				int vtstart = 0;
				int vcstart = 0;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt) {
						final double value = values[vc][vt];
						elements[ec][et][coords.z][exy] =
							(setmask&elements[ec][et][coords.z][exy])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
					}
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	public void set(final Coordinates coords, final double[][][] values) {
		
		switch (axes) {
			case Axes.ZYX: {
				int vxstart = 0;
				int vystart = 0;
				int vzstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ezstart = coords.z;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				int exstop = coords.x + values[0][0].length;
				int eystop = coords.y + values[0].length;
				int ezstop = coords.z + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				final int exy = eystart*dims.x + exstart;
				final int[][] elms = elements[coords.c][coords.t];
				for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
							final double value = values[vz][vy][vx];
							elms[ez][ex] = (setmask&elms[ez][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.TYX: {
				int vxstart = 0;
				int vystart = 0;
				int vtstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int etstart = coords.t;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int exstop = coords.x + values[0][0].length;
				int eystop = coords.y + values[0].length;
				int etstop = coords.t + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = eystart*dims.x + exstart;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
							final double value = values[vt][vy][vx];
							elements[coords.c][et][coords.z][ex] =
								(setmask&elements[coords.c][et][coords.z][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.CYX: {
				int vxstart = 0;
				int vystart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0].length;
				int eystop = coords.y + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
						for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
							final double value = values[vc][vy][vx];
							elements[ec][coords.t][coords.z][ex] =
								(setmask&elements[ec][coords.t][coords.z][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.TZX: {
				int vxstart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int exstart = coords.x;
				int ezstart = coords.z;
				int etstart = coords.t;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int exstop = coords.x + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int etstop = coords.t + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = coords.y*dims.x + exstart;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
							final double value = values[vt][vz][vx];
							elements[coords.c][et][ez][ex] =
								(setmask&elements[coords.c][et][ez][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.CZX: {
				int vxstart = 0;
				int vzstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int ezstart = coords.z;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ezstop > dims.z) ezstop = dims.z;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
							final double value = values[vc][vz][vx];
							elements[ec][coords.t][ez][ex] =
								(setmask&elements[ec][coords.t][ez][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.CTX: {
				int vxstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
							final double value = values[vc][vt][vx];
							elements[ec][et][coords.z][ex] =
								(setmask&elements[ec][et][coords.z][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.TZY: {
				int vystart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int eystart = coords.y;
				int ezstart = coords.z;
				int etstart = coords.t;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int eystop = coords.y + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int etstop = coords.t + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = eystart*dims.x + coords.x;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy) {
							final double value = values[vt][vz][vy];
							elements[coords.c][et][ez][ey] =
								(setmask&elements[coords.c][et][ez][ey])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.CZY: {
				int vystart = 0;
				int vzstart = 0;
				int vcstart = 0;
				int eystart = coords.y;
				int ezstart = coords.z;
				int ecstart = coords.c;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int eystop = coords.y + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int ecstop = coords.c + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy) {
							final double value = values[vc][vz][vy];
							elements[ec][coords.t][ez][ey] =
								(setmask&elements[ec][coords.t][ez][ey])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.CTY: {
				int vystart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int eystart = coords.y;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int eystop = coords.y + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy) {
							final double value = values[vc][vt][vy];
							elements[ec][et][coords.z][ey] =
								(setmask&elements[ec][et][coords.z][ey])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			case Axes.CTZ: {
				int vzstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int ezstart = coords.z;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int ezstop = coords.z + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz) {
							final double value = values[vc][vt][vz];
							elements[ec][et][ez][exy] =
								(setmask&elements[ec][et][ez][exy])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
						}
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	public void set(final Coordinates coords, final double[][][][] values) {
		
		switch (axes) {
			case Axes.TZYX: {
				int vxstart = 0;
				int vystart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ezstart = coords.z;
				int etstart = coords.t;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				int exstop = coords.x + values[0][0][0].length;
				int eystop = coords.y + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int etstop = coords.t + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				final int exy = eystart*dims.x + exstart;
				for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
								final double value = values[vt][vz][vy][vx];
								elements[coords.c][et][ez][ex] =
									(setmask&elements[coords.c][et][ez][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
							}
				break;
			}
			case Axes.CZYX: {
				int vxstart = 0;
				int vystart = 0;
				int vzstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ezstart = coords.z;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0][0].length;
				int eystop = coords.y + values[0][0].length;
				int ezstop = coords.z + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
								final double value = values[vc][vz][vy][vx];
								elements[ec][coords.t][ez][ex] =
									(setmask&elements[ec][coords.t][ez][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
							}
				break;
			}
			case Axes.CTYX: {
				int vxstart = 0;
				int vystart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0][0].length;
				int eystop = coords.y + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
							for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
								final double value = values[vc][vt][vy][vx];
								elements[ec][et][coords.z][ex] =
									(setmask&elements[ec][et][coords.z][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
							}
				break;
			}
			case Axes.CTZX: {
				int vxstart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int ezstart = coords.z;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0][0].length;
				int ezstop = coords.z + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = coords.y*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
							for (int x=exstart, ex=exy, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
								final double value = values[vc][vt][vz][vx];
								elements[ec][et][ez][ex] =
									(setmask&elements[ec][et][ez][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
							}
				break;
			}
			case Axes.CTZY: {
				int vystart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int eystart = coords.y;
				int ezstart = coords.z;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int eystop = coords.y + values[0][0][0].length;
				int ezstop = coords.z + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + coords.x;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
							for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy) {
								final double value = values[vc][vt][vz][vy];
								elements[ec][et][ez][ey] =
									(setmask&elements[ec][et][ez][ey])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
							}
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
	public void set(final Coordinates coords, final double[][][][][] values) {
		
		switch (axes) {
			case Axes.CTZYX: {
				int vxstart = 0;
				int vystart = 0;
				int vzstart = 0;
				int vtstart = 0;
				int vcstart = 0;
				int exstart = coords.x;
				int eystart = coords.y;
				int ezstart = coords.z;
				int etstart = coords.t;
				int ecstart = coords.c;
				if (exstart < 0) { exstart = 0; vxstart = -coords.x; }
				if (eystart < 0) { eystart = 0; vystart = -coords.y; }
				if (ezstart < 0) { ezstart = 0; vzstart = -coords.z; }
				if (etstart < 0) { etstart = 0; vtstart = -coords.t; }
				if (ecstart < 0) { ecstart = 0; vcstart = -coords.c; }
				int exstop = coords.x + values[0][0][0][0].length;
				int eystop = coords.y + values[0][0][0].length;
				int ezstop = coords.z + values[0][0].length;
				int etstop = coords.t + values[0].length;
				int ecstop = coords.c + values.length;
				if (exstop > dims.x) exstop = dims.x;
				if (eystop > dims.y) eystop = dims.y;
				if (ezstop > dims.z) ezstop = dims.z;
				if (etstop > dims.t) etstop = dims.t;
				if (ecstop > dims.c) ecstop = dims.c;
				final int exy = eystart*dims.x + exstart;
				for (int ec=ecstart, vc=vcstart; ec<ecstop; ++ec, ++vc)
					for (int et=etstart, vt=vtstart; et<etstop; ++et, ++vt)
						for (int ez=ezstart, vz=vzstart; ez<ezstop; ++ez, ++vz)
							for (int y=eystart, ey=exy, vy=vystart; y<eystop; ++y, ey+=dims.x, ++vy)
								for (int x=exstart, ex=ey, vx=vxstart; x<exstop; ++x, ++ex, ++vx) {
									final double value = values[vc][vt][vz][vy][vx];
									elements[ec][et][ez][ex] =
										(setmask&elements[ec][et][ez][ex])|(FMath.round((value>max)?max:((value<min)?min:value))<<bitshift);
								}
				break;
			}
			default:
				throw new IllegalStateException("Wrong number of active axes");
		}
	}
	
}
