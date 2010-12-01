package imagescience.transform;

import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Embeds images into larger images. */
public class Embed {
	
	/** Background filling with value {@code 0}. */
	public static final int ZERO = 0;
	
	/** Background filling with the minimum value in the input image. */
	public static final int MINIMUM = 1;
	
	/** Background filling with the maximum value in the input image. */
	public static final int MAXIMUM = 2;
	
	/** Background filling with the value of {@link #background}. */
	public static final int BACKGROUND = 3;
	
	/** Background filling by repeating the values in the input image. */
	public static final int REPEAT = 4;
	
	/** Background filling by mirroring the values in the input image. */
	public static final int MIRROR = 5;
	
	/** Background filling by clamping the values in the input image. */
	public static final int CLAMP = 6;
	
	/** Default constructor. */
	public Embed() { }
	
	/** Embeds an image.
		
		@param image the image to be embedded.
		
		@param dims the dimensions of the new image in which the input image is embedded.
		
		@param pos the starting position of the input image in the new image.
		
		@param type the type of filling of background elements. Must be equal to one of the static fields of this class.
		
		@return a new image with the given dimensions containing a copy of the input image at the given starting position and with the background elements filled as specified. The returned image is of the same type as the input image.
		
		@exception IllegalArgumentException if the starting position coordinates are such that the input image does not fit entirely in the new image, or if the specified type of background filling is not supported.
		
		@exception NullPointerException if any of the first three parameters is {@code null}.
		
		@exception UnknownError if for any reason the output image could not be created. In most cases this will be due to insufficient free memory.
	*/
	public Image run(final Image image, final Dimensions dims, final Coordinates pos, final int type) {
		
		messenger.log(ImageScience.prelude()+"Embed");
		
		// Initialize timer:
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Check parameters:
		messenger.log("Checking parameters");
		final Dimensions idims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+idims.x+","+idims.y+","+idims.z+","+idims.t+","+idims.c+")");
		messenger.log("Output image dimensions: (x,y,z,t,c) = ("+dims.x+","+dims.y+","+dims.z+","+dims.t+","+dims.c+")");
		
		if (pos.x < 0 || (pos.x + idims.x) > dims.x) throw new IllegalArgumentException("Input image does not fit in x-dimension");
		if (pos.y < 0 || (pos.y + idims.y) > dims.y) throw new IllegalArgumentException("Input image does not fit in y-dimension");
		if (pos.z < 0 || (pos.z + idims.z) > dims.z) throw new IllegalArgumentException("Input image does not fit in z-dimension");
		if (pos.t < 0 || (pos.t + idims.t) > dims.t) throw new IllegalArgumentException("Input image does not fit in t-dimension");
		if (pos.c < 0 || (pos.c + idims.c) > dims.c) throw new IllegalArgumentException("Input image does not fit in c-dimension");
		
		messenger.log("Position of input image: (x,y,z,t,c) = ("+pos.x+","+pos.y+","+pos.z+","+pos.t+","+pos.c+")");
		
		if (type < 0 || type > 6) throw new IllegalArgumentException("Non-supported type of background filling");
		
		// Embed with requested type of filling:
		messenger.log("Embedding "+image.type());
		final Image embedded = Image.create(dims,image.type());
		image.axes(Axes.X); embedded.axes(Axes.X);
		messenger.status("Embedding...");
		
		double bgval;
		switch (type) {
			case ZERO: bgval = 0; break;
			case MINIMUM: bgval = image.minimum(); break;
			case MAXIMUM: bgval = image.maximum(); break;
			case BACKGROUND: bgval = background; break;
			default: bgval = background;
		}
		
		switch (type) {
			case ZERO:
			case MINIMUM:
			case MAXIMUM:
			case BACKGROUND: {
				messenger.log("Background filling with value " + bgval);
				progressor.steps(idims.c*idims.t*idims.z*idims.y);
				final Coordinates ci = new Coordinates();
				final Coordinates ce = pos.duplicate();
				final double[] a = new double[idims.x];
				embedded.set(bgval);
				progressor.start();
				
				for (ci.c=0, ce.c=pos.c; ci.c<idims.c; ++ci.c, ++ce.c)
					for (ci.t=0, ce.t=pos.t; ci.t<idims.t; ++ci.t, ++ce.t)
						for (ci.z=0, ce.z=pos.z; ci.z<idims.z; ++ci.z, ++ce.z)
							for (ci.y=0, ce.y=pos.y; ci.y<idims.y; ++ci.y, ++ce.y) {
								image.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
				
				break;
			}
			case REPEAT: {
				messenger.log("Background filling by repeating the input image");
				progressor.steps(dims.c*dims.t*dims.z*dims.y);
				final Coordinates ci = new Coordinates();
				final Coordinates ce = new Coordinates();
				final Coordinates posm1 = new Coordinates(pos.x-1,pos.y-1,pos.z-1,pos.t-1,pos.c-1);
				final Coordinates posidims = new Coordinates(pos.x+idims.x,pos.y+idims.y,pos.z+idims.z,pos.t+idims.t,pos.c+idims.c);
				final Coordinates posidimsm1 = new Coordinates(posidims.x-1,posidims.y-1,posidims.z-1,posidims.t-1,posidims.c-1);
				final double[] a = new double[dims.x];
				progressor.start();
				
				// Copy and repeat within input slices:
				for (ci.x=-pos.x, ci.c=0, ce.c=pos.c; ci.c<idims.c; ++ci.c, ++ce.c)
					for (ci.t=0, ce.t=pos.t; ci.t<idims.t; ++ci.t, ++ce.t)
						for (ci.z=0, ce.z=pos.z; ci.z<idims.z; ++ci.z, ++ce.z)
							for (ci.y=0, ce.y=pos.y; ci.y<idims.y; ++ci.y, ++ce.y) {
								image.get(ci,a);
								for (int x=posm1.x, x0=posidimsm1.x; x>=0; --x, --x0) a[x] = a[x0];
								for (int x=posidims.x, x0=pos.x; x<dims.x; ++x, ++x0) a[x] = a[x0];
								embedded.set(ce,a);
								progressor.step();
							}
				for (ci.x=0, ci.c=ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c)
					for (ci.t=ce.t=pos.t; ce.t<posidims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=pos.z; ce.z<posidims.z; ++ci.z, ++ce.z) {
							for (ci.y=posidimsm1.y, ce.y=posm1.y; ce.y>=0; --ci.y, --ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
							for (ci.y=pos.y, ce.y=posidims.y; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
						}
				
				// Repeat slices within each input volume:
				for (ci.c=ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c)
					for (ci.t=ce.t=pos.t; ce.t<posidims.t; ++ci.t, ++ce.t) {
						for (ci.z=posidimsm1.z, ce.z=posm1.z; ce.z>=0; --ci.z, --ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
							for (ci.z=pos.z, ce.z=posidims.z; ce.z<dims.z; ++ci.z, ++ce.z)
								for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
									embedded.get(ci,a);
									embedded.set(ce,a);
									progressor.step();
								}
					}
				
				// Repeat volumes in the t-dimension:
				for (ci.c=ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c) {
					for (ci.t=posidimsm1.t, ce.t=posm1.t; ce.t>=0; --ci.t, --ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
							for (ci.t=pos.t, ce.t=posidims.t; ce.t<dims.t; ++ci.t, ++ce.t)
								for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
									for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
										embedded.get(ci,a);
										embedded.set(ce,a);
										progressor.step();
									}
				}
				
				// Repeat in the c-dimension:
				for (ci.c=posidimsm1.c, ce.c=posm1.c; ce.c>=0; --ci.c, --ce.c)
					for (ci.t=ce.t=0; ce.t<dims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
				for (ci.c=pos.c, ce.c=posidims.c; ce.c<dims.c; ++ci.c, ++ce.c)
					for (ci.t=ce.t=0; ce.t<dims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
				
				break;
			}
			case MIRROR: {
				messenger.log("Background filling by mirroring the input image");
				progressor.steps(dims.c*dims.t*dims.z*dims.y);
				final Coordinates ci = new Coordinates();
				final Coordinates ce = new Coordinates();
				final Coordinates posm1 = new Coordinates(pos.x-1,pos.y-1,pos.z-1,pos.t-1,pos.c-1);
				final Coordinates posidims = new Coordinates(pos.x+idims.x,pos.y+idims.y,pos.z+idims.z,pos.t+idims.t,pos.c+idims.c);
				final Coordinates idimsm1 = new Coordinates(idims.x-1,idims.y-1,idims.z-1,idims.t-1,idims.c-1);
				final double[] a = new double[dims.x];
				int ifx=2, ify=2, ifz=2, ift=2, ifc=2;
				progressor.start();
				
				// Mirroring in any dimension doesn't make sense if the size
				// in that dimension equals 1. In that case, the
				// parameters must be adjusted so that in that dimension the
				// values are repeated rather than mirrored:
				if (idims.x == 1) { ++idimsm1.x; ifx = 1; }
				if (idims.y == 1) { ++idimsm1.y; ify = 1; }
				if (idims.z == 1) { ++idimsm1.z; ifz = 1; }
				if (idims.t == 1) { ++idimsm1.t; ift = 1; }
				if (idims.c == 1) { ++idimsm1.c; ifc = 1; }
				
				// Copy and mirror within input slices:
				for (ci.x=-pos.x, ci.c=0, ce.c=pos.c; ci.c<idims.c; ++ci.c, ++ce.c)
					for (ci.t=0, ce.t=pos.t; ci.t<idims.t; ++ci.t, ++ce.t)
						for (ci.z=0, ce.z=pos.z; ci.z<idims.z; ++ci.z, ++ce.z)
							for (ci.y=0, ce.y=pos.y; ci.y<idims.y; ++ci.y, ++ce.y) {
								image.get(ci,a);
								for (int x=posm1.x; x>=0; --x) {
									final int xdiff = x - pos.x;
									int x0 = xdiff/idimsm1.x; x0 += x0 % ifx;
									a[x] = a[pos.x + Math.abs(xdiff - x0*idimsm1.x)];
								}
								for (int x=posidims.x; x<dims.x; ++x) {
									final int xdiff = x - pos.x;
									int x0 = xdiff/idimsm1.x; x0 += x0 % ifx;
									a[x] = a[pos.x + Math.abs(xdiff - x0*idimsm1.x)];
								}
								embedded.set(ce,a);
								progressor.step();
							}
				for (ci.x=0, ci.c=ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c)
					for (ci.t=ce.t=pos.t; ce.t<posidims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=pos.z; ce.z<posidims.z; ++ci.z, ++ce.z) {
							for (ce.y=posm1.y; ce.y>=0; --ce.y) {
								final int ydiff = ce.y - pos.y;
								int y0 = ydiff/idimsm1.y; y0 += y0 % ify;
								ci.y = pos.y + Math.abs(ydiff - y0*idimsm1.y);
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
							for (ce.y=posidims.y; ce.y<dims.y; ++ce.y) {
								final int ydiff = ce.y - pos.y;
								int y0 = ydiff/idimsm1.y; y0 += y0 % ify;
								ci.y = pos.y + Math.abs(ydiff - y0*idimsm1.y);
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
						}
				
				// Mirror slices within each input volume:
				for (ci.c=ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c)
					for (ci.t=ce.t=pos.t; ce.t<posidims.t; ++ci.t, ++ce.t) {
						for (ce.z=posm1.z; ce.z>=0; --ce.z) {
							final int zdiff = ce.z - pos.z;
							int z0 = zdiff/idimsm1.z; z0 += z0 % ifz;
							ci.z = pos.z + Math.abs(zdiff - z0*idimsm1.z);
							for (ci.y=0, ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
						}
						for (ce.z=posidims.z; ce.z<dims.z; ++ce.z) {
							final int zdiff = ce.z - pos.z;
							int z0 = zdiff/idimsm1.z; z0 += z0 % ifz;
							ci.z = pos.z + Math.abs(zdiff - z0*idimsm1.z);
							for (ci.y=0, ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
						}
					}
				
				// Mirror volumes in the t-dimension:
				for (ci.c=ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c) {
					for (ce.t=posm1.t; ce.t>=0; --ce.t) {
						final int tdiff = ce.t - pos.t;
						int t0 = tdiff/idimsm1.t; t0 += t0 % ift;
						ci.t = pos.t + Math.abs(tdiff - t0*idimsm1.t);
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
					}
					for (ce.t=posidims.t; ce.t<dims.t; ++ce.t) {
						final int tdiff = ce.t - pos.t;
						int t0 = tdiff/idimsm1.t; t0 += t0 % ift;
						ci.t = pos.t + Math.abs(tdiff - t0*idimsm1.t);
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
					}
				}
				
				// Mirror in the c-dimension:
				for (ce.c=posm1.c; ce.c>=0; --ce.c) {
					final int cdiff = ce.c - pos.c;
					int c0 = cdiff/idimsm1.c; c0 += c0 % ifc;
					ci.c = pos.c + Math.abs(cdiff - c0*idimsm1.c);
					for (ci.t=ce.t=0; ce.t<dims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
				}
				for (ce.c=posidims.c; ce.c<dims.c; ++ce.c) {
					final int cdiff = ce.c - pos.c;
					int c0 = cdiff/idimsm1.c; c0 += c0 % ifc;
					ci.c = pos.c + Math.abs(cdiff - c0*idimsm1.c);
					for (ci.t=ce.t=0; ce.t<dims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
				}
				
				break;
			}
			case CLAMP: {
				messenger.log("Background filling by clamping the input image");
				progressor.steps(dims.c*dims.t*dims.z*dims.y);
				final Coordinates ci = new Coordinates();
				final Coordinates ce = new Coordinates();
				final Coordinates posm1 = new Coordinates(pos.x-1,pos.y-1,pos.z-1,pos.t-1,pos.c-1);
				final Coordinates posidims = new Coordinates(pos.x+idims.x,pos.y+idims.y,pos.z+idims.z,pos.t+idims.t,pos.c+idims.c);
				final Coordinates posidimsm1 = new Coordinates(posidims.x-1,posidims.y-1,posidims.z-1,posidims.t-1,posidims.c-1);
				final double[] a = new double[dims.x];
				progressor.start();
				
				// Copy and clamp within input slices:
				for (ci.x=-pos.x, ci.c=0, ce.c=pos.c; ci.c<idims.c; ++ci.c, ++ce.c)
					for (ci.t=0, ce.t=pos.t; ci.t<idims.t; ++ci.t, ++ce.t)
						for (ci.z=0, ce.z=pos.z; ci.z<idims.z; ++ci.z, ++ce.z)
							for (ci.y=0, ce.y=pos.y; ci.y<idims.y; ++ci.y, ++ce.y) {
								image.get(ci,a);
								final double valb = a[pos.x];
								final double vale = a[posidimsm1.x];
								for (int x=posm1.x; x>=0; --x) a[x] = valb;
								for (int x=posidims.x; x<dims.x; ++x) a[x] = vale;
								embedded.set(ce,a);
								progressor.step();
							}
				for (ci.x=0, ci.c=pos.c, ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c)
					for (ci.t=ce.t=pos.t; ce.t<posidims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=pos.z; ce.z<posidims.z; ++ci.z, ++ce.z) {
							ci.y=pos.y;
							embedded.get(ci,a);
							for (ce.y=posm1.y; ce.y>=0; --ce.y) {
								embedded.set(ce,a);
								progressor.step();
							}
							ci.y=posidimsm1.y;
							embedded.get(ci,a);
							for (ce.y=posidims.y; ce.y<dims.y; ++ce.y) {
								embedded.set(ce,a);
								progressor.step();
							}
						}
				
				// Clamp slices within each input volume:
				for (ci.c=ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c)
					for (ci.t=ce.t=pos.t; ce.t<posidims.t; ++ci.t, ++ce.t) {
						for (ci.z=pos.z, ce.z=posm1.z; ce.z>=0; --ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
						for (ci.z=posidimsm1.z, ce.z=posidims.z; ce.z<dims.z; ++ce.z)
							for (ci.y=0, ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
					}
				
				// Clamp volumes in the t-dimension:
				for (ci.c=ce.c=pos.c; ce.c<posidims.c; ++ci.c, ++ce.c) {
					for (ci.t=pos.t, ce.t=posm1.t; ce.t>=0; --ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
					for (ci.t=posidimsm1.t, ce.t=posidims.t; ce.t<dims.t; ++ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
				}
				
				// Clamp in the c-dimension:
				for (ci.c=pos.c, ce.c=posm1.c; ce.c>=0; --ce.c)
					for (ci.t=ce.t=0; ce.t<dims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
				for (ci.c=posidimsm1.c, ce.c=posidims.c; ce.c<dims.c; ++ce.c)
					for (ci.t=ce.t=0; ce.t<dims.t; ++ci.t, ++ce.t)
						for (ci.z=ce.z=0; ce.z<dims.z; ++ci.z, ++ce.z)
							for (ci.y=ce.y=0; ce.y<dims.y; ++ci.y, ++ce.y) {
								embedded.get(ci,a);
								embedded.set(ce,a);
								progressor.step();
							}
				
				break;
			}
		}
		
		// Finish up:
		embedded.name(image.name()+" embedded");
		embedded.aspects(image.aspects().duplicate());
		messenger.status("");
		progressor.stop();
		timer.stop();
		
		return embedded;
	}
	
	/** The value used when the background filling is of type {@link #BACKGROUND}. The default value is {@code 0}. */
	public double background = 0;
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
}
