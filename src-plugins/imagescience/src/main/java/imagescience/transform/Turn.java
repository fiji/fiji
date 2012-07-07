package imagescience.transform;

import imagescience.image.Aspects;
import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Rotates images multiples of 90 degrees around its principal axes. The advantage of this class over {@link Rotate} is that no interpolations are performed: it merely shuffles the image elements. */
public class Turn {
	
	/** Default constructor. */
	public Turn() { }
	
	/** Turns an image.
		
		@param image the image to be turned.
		
		@param times90z {@code times90y} - {@code times90x} - the number of times the image should be rotated 90 degrees around the z-, y-, and x-axis, respectively. The order of rotation is the same as the parameters. That is to say, first {@code times90z} times 90 degrees around the z-axis, then {@code times90y} times 90 degrees around the y-axis, and finally {@code times90x} times 90 degrees around the x-axis. The algorithm is applied to every x-y-z subimage in a 5D image and assumes a right-handed coordinate system, with the origin in the center of each subimage. A rotation of +90 degrees around the z-axis implies that the positive x-axis is mapped onto the positive y-axis. Similarly, a rotation of +90 degrees around the y-axis maps the positive z-axis onto the positive x-axis, and a rotation of +90 degrees around the x-axis maps the positive y-axis onto the positive z-axis.
		
		@return a new image containing a turned version of the input image. The returned image is of the same type as the input image.
		
		@exception NullPointerException if {@code image} is {@code null}.
		
		@exception UnknownError if for any reason the output image could not be created. In most cases this will be due to insufficient free memory.
	*/
	public Image run(final Image image, final int times90z, final int times90y, final int times90x) {
		
		messenger.log(ImageScience.prelude()+"Turn");
		
		// Initialize timer:
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Check parameters:
		messenger.log("Checking parameters");
		
		// Map the parameters back to 0,1,2, or 3:
		final int nr90z = (times90z < 0) ? (((times90z + 1) % 4) + 3) : (times90z % 4);
		final int nr90y = (times90y < 0) ? (((times90y + 1) % 4) + 3) : (times90y % 4);
		final int nr90x = (times90x < 0) ? (((times90x + 1) % 4) + 3) : (times90x % 4);
		
		final int nr90zyx = nr90z*100 + nr90y*10 + nr90x;
		
		// Determine the size of the turned image:
		final Dimensions idims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+idims.x+","+idims.y+","+idims.z+","+idims.t+","+idims.c+")");
		
		final Dimensions tdimsturnz = (nr90z == 1 || nr90z == 3) ?
			new Dimensions(idims.y,idims.x,idims.z,idims.t,idims.c) :
			idims.duplicate();
		
		final Dimensions tdimsturny = (nr90y == 1 || nr90y == 3) ?
			new Dimensions(tdimsturnz.z,tdimsturnz.y,tdimsturnz.x,idims.t,idims.c) :
			tdimsturnz.duplicate();
		
		final Dimensions tdims = (nr90x == 1 || nr90x == 3) ?
			new Dimensions(tdimsturny.x,tdimsturny.z,tdimsturny.y,idims.t,idims.c) :
			tdimsturny.duplicate();
		
		messenger.log("Output image dimensions: (x,y,z,t,c) = ("+tdims.x+","+tdims.y+","+tdims.z+","+tdims.t+","+tdims.c+")");
		
		final Coordinates tdimsm1 = new Coordinates(tdims.x-1,tdims.y-1,tdims.z-1,tdims.t-1,tdims.c-1);
		
		// Determine aspect values of the turned image:
		final Aspects iasps = image.aspects();
		messenger.log("Input image aspects: (x,y,z,t,c) = ("+iasps.x+","+iasps.y+","+iasps.z+","+iasps.t+","+iasps.c+")");
		
		final Aspects taspsturnz = (nr90z == 1 || nr90z == 3) ?
			new Aspects(iasps.y,iasps.x,iasps.z,iasps.t,iasps.c) :
			iasps.duplicate();
		
		final Aspects taspsturny = (nr90y == 1 || nr90y == 3) ?
			new Aspects(taspsturnz.z,taspsturnz.y,taspsturnz.x,iasps.t,iasps.c) :
			taspsturnz.duplicate();
		
		final Aspects tasps = (nr90x == 1 || nr90x == 3) ?
			new Aspects(taspsturny.x,taspsturny.z,taspsturny.y,iasps.t,iasps.c) :
			taspsturny.duplicate();
		
		messenger.log("Output image aspects: (x,y,z,t,c) = ("+tasps.x+","+tasps.y+","+tasps.z+","+tasps.t+","+tasps.c+")");
		
		// Initialize:
		messenger.status("Turning...");
		progressor.steps(idims.c*idims.t*idims.z);
		progressor.start();
		
		messenger.log("Turning "+image.type());
		messenger.log(
			(nr90z*90)+" degrees around z-axis\n"+
			(nr90y*90)+" degrees around y-axis\n"+
			(nr90x*90)+" degrees around x-axis"
		);
		
		final Image turned = Image.create(tdims,image.type());
		final Coordinates ci = new Coordinates();
		final Coordinates ct = new Coordinates();
		final double[] a = new double[idims.x];
		image.axes(Axes.X);
		
		// Turn: (Note that of the 4x4x4=64 cases, several of them yield
		// the same result. Effectively only 24 routines are required.)
		switch (nr90zyx) {
			case 222: case 0: {
				turned.axes(Axes.X);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.z=0; ci.z<idims.z; ++ci.z, ++ct.z) {
							for (ci.y=0, ct.y=0; ci.y<idims.y; ++ci.y, ++ct.y) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 322: case 100: {
				turned.axes(Axes.Y);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.z=0; ci.z<idims.z; ++ci.z, ++ct.z) {
							for (ci.y=0, ct.x=tdimsm1.x; ci.y<idims.y; ++ci.y, --ct.x) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 22: case 200: {
				turned.axes(Axes.X);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.z=0; ci.z<idims.z; ++ci.z, ++ct.z) {
							for (ci.y=0, ct.y=tdimsm1.y; ci.y<idims.y; ++ci.y, --ct.y) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 122: case 300: {
				turned.axes(Axes.Y);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.z=0; ci.z<idims.z; ++ci.z, ++ct.z) {
							for (ci.y=0, ct.x=0; ci.y<idims.y; ++ci.y, ++ct.x) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 311: case 212: case 113: case 10: {
				turned.axes(Axes.Z);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.x=0; ci.z<idims.z; ++ci.z, ++ct.x) {
							for (ci.y=0, ct.y=0; ci.y<idims.y; ++ci.y, ++ct.y) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 11: case 312: case 213: case 110: {
				turned.axes(Axes.Y);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.x=0; ci.z<idims.z; ++ci.z, ++ct.x) {
							for (ci.y=0, ct.z=0; ci.y<idims.y; ++ci.y, ++ct.z) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 111: case 12: case 313: case 210: {
				turned.axes(Axes.Z);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.x=0; ci.z<idims.z; ++ci.z, ++ct.x) {
							for (ci.y=0, ct.y=tdimsm1.y; ci.y<idims.y; ++ci.y, --ct.y) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 211: case 112: case 13: case 310: {
				turned.axes(Axes.Y);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.x=0; ci.z<idims.z; ++ci.z, ++ct.x) {
							for (ci.y=0, ct.z=tdimsm1.z; ci.y<idims.y; ++ci.y, --ct.z) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 202: case 20: {
				turned.axes(Axes.X);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.z=tdimsm1.z; ci.z<idims.z; ++ci.z, --ct.z) {
							for (ci.y=0, ct.y=0; ci.y<idims.y; ++ci.y, ++ct.y) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 302: case 120: {
				turned.axes(Axes.Y);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.z=tdimsm1.z; ci.z<idims.z; ++ci.z, --ct.z) {
							for (ci.y=0, ct.x=0; ci.y<idims.y; ++ci.y, ++ct.x) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 2: case 220: {
				turned.axes(Axes.X);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.z=tdimsm1.z; ci.z<idims.z; ++ci.z, --ct.z) {
							for (ci.y=0, ct.y=tdimsm1.y; ci.y<idims.y; ++ci.y, --ct.y) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 102: case 320: {
				turned.axes(Axes.Y);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.z=tdimsm1.z; ci.z<idims.z; ++ci.z, --ct.z) {
							for (ci.y=0, ct.x=tdimsm1.x; ci.y<idims.y; ++ci.y, --ct.x) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 131: case 232: case 333: case 30: {
				turned.axes(Axes.Z);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.x=tdimsm1.x; ci.z<idims.z; ++ci.z, --ct.x) {
							for (ci.y=0, ct.y=0; ci.y<idims.y; ++ci.y, ++ct.y) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 231: case 332: case 33: case 130: {
				turned.axes(Axes.Y);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.x=tdimsm1.x; ci.z<idims.z; ++ci.z, --ct.x) {
							for (ci.y=0, ct.z=tdimsm1.z; ci.y<idims.y; ++ci.y, --ct.z) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 331: case 32: case 133: case 230: {
				turned.axes(Axes.Z);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.x=tdimsm1.x; ci.z<idims.z; ++ci.z, --ct.x) {
							for (ci.y=0, ct.y=tdimsm1.y; ci.y<idims.y; ++ci.y, --ct.y) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 31: case 132: case 233: case 330: {
				turned.axes(Axes.Y);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.x=tdimsm1.x; ci.z<idims.z; ++ci.z, --ct.x) {
							for (ci.y=0, ct.z=0; ci.y<idims.y; ++ci.y, ++ct.z) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 223: case 1: {
				turned.axes(Axes.X);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.y=tdimsm1.y; ci.z<idims.z; ++ci.z, --ct.y) {
							for (ci.y=0, ct.z=0; ci.y<idims.y; ++ci.y, ++ct.z) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 323: case 101: {
				turned.axes(Axes.Z);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.y=tdimsm1.y; ci.z<idims.z; ++ci.z, --ct.y) {
							for (ci.y=0, ct.x=tdimsm1.x; ci.y<idims.y; ++ci.y, --ct.x) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 23: case 201: {
				turned.axes(Axes.X);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.y=tdimsm1.y; ci.z<idims.z; ++ci.z, --ct.y) {
							for (ci.y=0, ct.z=tdimsm1.z; ci.y<idims.y; ++ci.y, --ct.z) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 123: case 301: {
				turned.axes(Axes.Z);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.y=tdimsm1.y; ci.z<idims.z; ++ci.z, --ct.y) {
							for (ci.y=0, ct.x=0; ci.y<idims.y; ++ci.y, ++ct.x) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 203: case 21: {
				turned.axes(Axes.X);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.y=0; ci.z<idims.z; ++ci.z, ++ct.y) {
							for (ci.y=0, ct.z=0; ci.y<idims.y; ++ci.y, ++ct.z) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 303: case 121: {
				turned.axes(Axes.Z);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.y=0; ci.z<idims.z; ++ci.z, ++ct.y) {
							for (ci.y=0, ct.x=0; ci.y<idims.y; ++ci.y, ++ct.x) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 3: case 221: {
				turned.axes(Axes.X);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.y=0; ci.z<idims.z; ++ci.z, ++ct.y) {
							for (ci.y=0, ct.z=tdimsm1.z; ci.y<idims.y; ++ci.y, --ct.z) {
								image.get(ci,a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
			case 103: case 321: {
				turned.axes(Axes.Z);
				for (ci.c=0, ct.c=0; ci.c<idims.c; ++ci.c, ++ct.c)
					for (ci.t=0, ct.t=0; ci.t<idims.t; ++ci.t, ++ct.t)
						for (ci.z=0, ct.y=0; ci.z<idims.z; ++ci.z, ++ct.y) {
							for (ci.y=0, ct.x=tdimsm1.x; ci.y<idims.y; ++ci.y, --ct.x) {
								image.get(ci,a);
								reverse(a);
								turned.set(ct,a);
							}
							progressor.step();
						}
				break;
			}
		}
		
		// Finish up:
		turned.name(image.name()+" turned");
		turned.aspects(tasps);
		messenger.status("");
		progressor.stop();
		timer.stop();
		
		return turned;
	}
	
	private void reverse(final double[] a) {
		
		final int halflength = a.length / 2;
		for (int b=0, e=a.length-1; b<halflength; ++b, --e) {
			final double tmp = a[e];
			a[e] = a[b];
			a[b] = tmp;
		}
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
}
