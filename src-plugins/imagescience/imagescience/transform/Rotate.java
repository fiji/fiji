package imagescience.transform;

import imagescience.image.Axes;
import imagescience.image.Borders;
import imagescience.image.ColorImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.utility.FMath;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Rotates images using different interpolation schemes.
	
	<dt><b>References:</b></dt>
	
	<dd><table border="0" cellspacing="0" cellpadding="0">
	
	<tr><td valign="top">[1]</td><td width="10"></td><td>R. G. Keys, "Cubic Convolution Interpolation for Digital Image Processing", <em>IEEE Transactions on Acoustics, Speech, and Signal Processing</em>, vol. 29, no. 6, 1981, pp. 1153-1160.</td></tr>

	<tr><td valign="top">[2]</td><td width="10"></td><td>M. Unser, "Splines: A Perfect Fit for Signal and Image Processing", <em>IEEE Signal Processing Magazine</em>, vol. 16, no. 6, 1999, pp. 22-38.</td></tr>
	
	<tr><td valign="top">[3]</td><td width="10"></td><td>P. Thevenaz, T. Blu, M. Unser, "Interpolation Revisited", <em>IEEE Transactions on Medical Imaging</em>, vol. 19, no. 7, 2000, pp.739-758.</td></tr>
	
	<tr><td valign="top">[4]</td><td width="10"></td><td>E. Meijering, W. Niessen, M. Viergever, "Quantitative Evaluation of Convolution-Based Methods for Medical Image Interpolation", <em>Medical Image Analysis</em>, vol. 5, no. 2, 2001, pp. 111-126.</td></tr>
	
	<tr><td valign="top">[5]</td><td width="10"></td><td>T. Blu, P. Thevenaz, M. Unser, "MOMS: Maximal-Order Interpolation of Minimal Support", <em>IEEE Transactions on Image Processing</em>, vol. 10, no. 7, 2001, pp. 1069-1080.</td></tr>
	
	</table></dd>
*/
public class Rotate {
	
	/** Nearest-neighbor interpolation. */
	public static final int NEAREST = 0;
	
	/** Linear interpolation. */
	public static final int LINEAR = 1;
	
	/** Cubic convolution interpolation [1]. */
	public static final int CUBIC = 2;
	
	/** Cubic B-spline interpolation [2,3,4]. */
	public static final int BSPLINE3 = 3;
	
	/** Cubic O-MOMS interpolation [5]. */
	public static final int OMOMS3 = 4;
	
	/** Quintic B-spline interpolation [2,3,4]. */
	public static final int BSPLINE5 = 5;
	
	/** Default constructor. */
	public Rotate() { }
	
	/** Rotates an image.
		
		@param image the input image to be rotated. For images of type {@link ColorImage}, the color components are processed separately by the method.
		
		@param zangle {@code yangle} - {@code xangle} - the rotation angles (in degrees) around the z-, y-, and x-axis of the image. The order of rotation around the different axes is the same as that of the parameters. The rotations are applied to every x-y-z subimage in a 5D image. The origin of the right-handed coordinate system in which the rotation is carried out is taken in the center of each subimage.
		
		@param scheme the interpolation scheme to be used. Must be equal to one of the static fields of this class.
		
		@param fit if {@code true}, the size of the output image is adjusted to fit the entire rotated image; if {@code false}, the size of the output image will be equal to that of the input image.
		
		@param antialias if {@code true}, the method attempts to reduce the "stair-casing" effect at the transitions from image to background.
		
		@return a new image containing a rotated version of the input image. The returned image is of the same type as the input image.
		
		@exception IllegalArgumentException if the requested interpolation {@code scheme} is not supported.
		
		@exception NullPointerException if {@code image} is {@code null}.
		
		@exception UnknownError if for any reason the output image could not be created. In most cases this will be due to insufficient free memory.
	*/
	public synchronized Image run(
		final Image image,
		final double zangle,
		final double yangle,
		final double xangle,
		final int scheme,
		final boolean fit,
		final boolean antialias
	) {
		
		messenger.log(ImageScience.prelude()+"Rotate");
		
		// Initialize:
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Check and initialize parameters:
		checkup(image.dimensions(),zangle,yangle,xangle,scheme,fit,antialias);
		
		// Rotate:
		messenger.log("Rotating "+image.type());
		Image rotated = null;
		if (image instanceof ColorImage) {
			messenger.log("Processing RGB-color components separately");
			final ColorImage cimage = (ColorImage)image;
			
			progressor.range(0,0.33);
			component = " red component";
			messenger.log("Rotating"+component);
			cimage.component(ColorImage.RED);
			Image comp = cimage.get(); comp = rotate(comp);
			final ColorImage crotated = new ColorImage(comp.dimensions());
			crotated.component(ColorImage.RED);
			crotated.set(comp);
			
			progressor.range(0.33,0.67);
			component = " green component";
			messenger.log("Rotating"+component);
			cimage.component(ColorImage.GREEN);
			comp = cimage.get(); comp = rotate(comp);
			crotated.component(ColorImage.GREEN);
			crotated.set(comp);
			
			progressor.range(0.67,1);
			component = " blue component";
			messenger.log("Rotating"+component);
			cimage.component(ColorImage.BLUE);
			comp = cimage.get(); comp = rotate(comp);
			crotated.component(ColorImage.BLUE);
			crotated.set(comp);
			
			rotated = crotated;
			
		} else {
			component = "";
			progressor.range(0,1);
			rotated = rotate(image);
		}
		
		// Finish up:
		rotated.name(image.name()+" rotated");
		rotated.aspects(image.aspects().duplicate());
		
		messenger.status("");
		
		timer.stop();
		
		return rotated;
	}
	
	private Image rotate(final Image image) {
		
		// Duplicate input image if all angles are zero:
		final boolean ax0 = (xangle == 0);
		final boolean ay0 = (yangle == 0);
		final boolean az0 = (zangle == 0);
		if (ax0 && ay0 && az0) {
			messenger.log("All rotation angles are zero");
			messenger.log("Returning a copy of the input image");
			return image.duplicate();
		}
		
		// Rotate using specified interpolation scheme:
		messenger.log("Allocating memory output image");
		final Image rotated = Image.create(newdims,image.type());
		switch (scheme) {
			case NEAREST: {
				if (ax0 && ay0) rotate_nearest_z(image,rotated);
				else if (ax0 && az0) rotate_nearest_y(image,rotated);
				else if (ay0 && az0) rotate_nearest_x(image,rotated);
				else rotate_nearest_zyx(image,rotated);
				break;
			}
			case LINEAR: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = image.border(borders);
				if (ax0 && ay0) rotate_linear_z(bordered,rotated);
				else if (ax0 && az0) rotate_linear_y(bordered,rotated);
				else if (ay0 && az0) rotate_linear_x(bordered,rotated);
				else rotate_linear_zyx(bordered,rotated);
				break;
			}
			case CUBIC: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = image.border(borders);
				if (ax0 && ay0) rotate_cubic_z(bordered,rotated);
				else if (ax0 && az0) rotate_cubic_y(bordered,rotated);
				else if (ay0 && az0) rotate_cubic_x(bordered,rotated);
				else rotate_cubic_zyx(bordered,rotated);
				break;
			}
			case BSPLINE3: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = new FloatImage(image,borders);
				if (ax0 && ay0) rotate_bspline3_z(bordered,rotated);
				else if (ax0 && az0) rotate_bspline3_y(bordered,rotated);
				else if (ay0 && az0) rotate_bspline3_x(bordered,rotated);
				else rotate_bspline3_zyx(bordered,rotated);
				break;
			}
			case OMOMS3: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = new FloatImage(image,borders);
				if (ax0 && ay0) rotate_omoms3_z(bordered,rotated);
				else if (ax0 && az0) rotate_omoms3_y(bordered,rotated);
				else if (ay0 && az0) rotate_omoms3_x(bordered,rotated);
				else rotate_omoms3_zyx(bordered,rotated);
				break;
			}
			case BSPLINE5: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = new FloatImage(image,borders);
				if (ax0 && ay0) rotate_bspline5_z(bordered,rotated);
				else if (ax0 && az0) rotate_bspline5_y(bordered,rotated);
				else if (ay0 && az0) rotate_bspline5_x(bordered,rotated);
				else rotate_bspline5_zyx(bordered,rotated);
				break;
			}
		}
		
		return rotated;
	}
	
	private void checkup(
		final Dimensions indims,
		final double zangle,
		final double yangle,
		final double xangle,
		final int scheme,
		final boolean fit,
		final boolean antialias
	) {
		
		messenger.log("Checking parameters");
		
		// Compute center of image:
		this.indims = indims;
		maxx = indims.x - 1;
		maxy = indims.y - 1;
		maxz = indims.z - 1;
		
		xc = maxx/2;
		yc = maxy/2;
		zc = maxz/2;
		
		// Store the rotation angles:
		this.zangle = zangle;
		this.yangle = yangle;
		this.xangle = xangle;
		
		messenger.log("Rotation angle around z-axes: "+zangle);
		messenger.log("Rotation angle around y-axes: "+yangle);
		messenger.log("Rotation angle around x-axes: "+xangle);
		
		// Precompute the sines and cosines of the rotation angles:
		final double axrad = xangle*Math.PI/180;
		final double ayrad = yangle*Math.PI/180;
		final double azrad = zangle*Math.PI/180;
		
		cosax = Math.cos(axrad);
		sinax = Math.sin(axrad);
		cosay = Math.cos(ayrad);
		sinay = Math.sin(ayrad);
		cosaz = Math.cos(azrad);
		sinaz = Math.sin(azrad);
		
		// Precompute the elements of the inverse rotation matrix:
		invxx = cosay*cosaz;
		invxy = cosaz*sinax*sinay+cosax*sinaz;
		invxz = -cosax*cosaz*sinay+sinax*sinaz;
		
		invyx = -cosay*sinaz;
		invyy = cosax*cosaz-sinax*sinay*sinaz;
		invyz = cosaz*sinax+cosax*sinay*sinaz;
		
		invzx = sinay;
		invzy = -cosay*sinax;
		invzz = cosax*cosay;
		
		// Compute the size of the rotated image:
		double newminx = -0.5;
		double newminy = -0.5;
		double newminz = -0.5;
		double newmaxx = maxx + 0.5;
		double newmaxy = maxy + 0.5;
		double newmaxz = maxz + 0.5;
		
		if (fit) {
			messenger.log("Adjusting output image dimensions to fit result");
			
			// Compute the new positions of the eight corner points:
			final double x1 = -xc - 0.5;
			final double y1 = -yc - 0.5;
			final double z1 = -zc - 0.5;
			
			final double x2 = maxx - xc + 0.5;
			final double y2 = -yc - 0.5;
			final double z2 = -zc - 0.5;
			
			final double x3 = -xc - 0.5;
			final double y3 = maxy - yc + 0.5;
			final double z3 = -zc - 0.5;
			
			final double x4 = maxx - xc + 0.5;
			final double y4 = maxy - yc + 0.5;
			final double z4 = -zc - 0.5;
			
			final double x5 = -xc - 0.5;
			final double y5 = -yc - 0.5;
			final double z5 = maxz - zc + 0.5;
			
			final double x6 = maxx - xc + 0.5;
			final double y6 = -yc - 0.5;
			final double z6 = maxz - zc + 0.5;
			
			final double x7 = -xc - 0.5;
			final double y7 = maxy - yc + 0.5;
			final double z7 = maxz - zc + 0.5;
			
			final double x8 = maxx - xc + 0.5;
			final double y8 = maxy - yc + 0.5;
			final double z8 = maxz - zc + 0.5;
			
			// Use the forward compound rotation matrix:
			final double fxx = cosay*cosaz;
			final double fyx = -cosay*sinaz;
			final double fzx = sinay;
			final double fxy = cosaz*sinax*sinay + cosax*sinaz;
			final double fyy = cosax*cosaz - sinax*sinay*sinaz;
			final double fzy = -cosay*sinax;
			final double fxz = -cosax*cosaz*sinay + sinax*sinaz;
			final double fyz = cosaz*sinax + cosax*sinay*sinaz;
			final double fzz = cosax*cosay;
			
			final double newx1 = x1*fxx + y1*fyx + z1*fzx;
			final double newy1 = x1*fxy + y1*fyy + z1*fzy;
			final double newz1 = x1*fxz + y1*fyz + z1*fzz;
			
			final double newx2 = x2*fxx + y2*fyx + z2*fzx;
			final double newy2 = x2*fxy + y2*fyy + z2*fzy;
			final double newz2 = x2*fxz + y2*fyz + z2*fzz;
			
			final double newx3 = x3*fxx + y3*fyx + z3*fzx;
			final double newy3 = x3*fxy + y3*fyy + z3*fzy;
			final double newz3 = x3*fxz + y3*fyz + z3*fzz;
			
			final double newx4 = x4*fxx + y4*fyx + z4*fzx;
			final double newy4 = x4*fxy + y4*fyy + z4*fzy;
			final double newz4 = x4*fxz + y4*fyz + z4*fzz;
			
			final double newx5 = x5*fxx + y5*fyx + z5*fzx;
			final double newy5 = x5*fxy + y5*fyy + z5*fzy;
			final double newz5 = x5*fxz + y5*fyz + z5*fzz;
			
			final double newx6 = x6*fxx + y6*fyx + z6*fzx;
			final double newy6 = x6*fxy + y6*fyy + z6*fzy;
			final double newz6 = x6*fxz + y6*fyz + z6*fzz;
			
			final double newx7 = x7*fxx + y7*fyx + z7*fzx;
			final double newy7 = x7*fxy + y7*fyy + z7*fzy;
			final double newz7 = x7*fxz + y7*fyz + z7*fzz;
			
			final double newx8 = x8*fxx + y8*fyx + z8*fzx;
			final double newy8 = x8*fxy + y8*fyy + z8*fzy;
			final double newz8 = x8*fxz + y8*fyz + z8*fzz;
			
			newminx = FMath.min(newx1,newx2,newx3,newx4,newx5,newx6,newx7,newx8);
			newminy = FMath.min(newy1,newy2,newy3,newy4,newy5,newy6,newy7,newy8);
			newminz = FMath.min(newz1,newz2,newz3,newz4,newz5,newz6,newz7,newz8);
			
			newmaxx = FMath.max(newx1,newx2,newx3,newx4,newx5,newx6,newx7,newx8);
			newmaxy = FMath.max(newy1,newy2,newy3,newy4,newy5,newy6,newy7,newy8);
			newmaxz = FMath.max(newz1,newz2,newz3,newz4,newz5,newz6,newz7,newz8);
			
		} else {
			messenger.log("Not adjusting image dimensions");
		}
		
		newdims = new Dimensions(
			FMath.round(newmaxx - newminx),
			FMath.round(newmaxy - newminy),
			FMath.round(newmaxz - newminz),
			indims.t,
			indims.c);
		
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+indims.x+","+indims.y+","+indims.z+","+indims.t+","+indims.c+")");
		
		messenger.log("Output image dimensions: (x,y,z,t,c) = ("+newdims.x+","+newdims.y+","+newdims.z+","+newdims.t+","+newdims.c+")");
		
		// Determine the offset of the center of the rotated image with respect to the input image:
		newxoffset = (newdims.x - indims.x)/2;
		newyoffset = (newdims.y - indims.y)/2;
		newzoffset = (newdims.z - indims.z)/2;
		
		newxoffsetxc = newxoffset + xc;
		newyoffsetyc = newyoffset + yc;
		newzoffsetzc = newzoffset + zc;
		
		// Check if requested type of interpolation is applicable:
		messenger.log("Requested interpolation scheme: "+schemes(scheme));
		if (scheme < 0 || scheme > 5) throw new IllegalArgumentException("Non-supported interpolation scheme");
		this.scheme = scheme;
		
		// Show background filling value:
		messenger.log("Background filling with value "+background);
		
		// Set border sizes based on interpolation scheme and rotation angles:
		int bsize = 0;
		switch (scheme) {
			case NEAREST: bsize = 0; break;
			case LINEAR: bsize = 1; break;
			case CUBIC: bsize = 2; break;
			case BSPLINE3: bsize = 2; break;
			case OMOMS3: bsize = 2; break;
			case BSPLINE5: bsize = 3; break;
		}
		// Note: if the rotations angles are all zero, the actual rotation
		// functions are never called, so we only have to check the other cases here:
		if (xangle == 0 && yangle == 0) { borders = new Borders(bsize,bsize,0); }
		else if (xangle == 0 && zangle == 0) { borders = new Borders(bsize,0,bsize); }
		else if (yangle == 0 && zangle == 0) { borders = new Borders(0,bsize,bsize); }
		else { borders = new Borders(bsize,bsize,bsize); }
		
		// Store anti-alias choice:
		this.antialias = antialias;
		if (antialias) messenger.log("Anti-aliasing image-background transitions");
	}
	
	private void rotate_nearest_z(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around z-axis");
		messenger.log("Using nearest-neighbor sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		
		// Rotate using the inverse of the rotation matrix: (note that when this
		// method is called, the border size of image is zero)
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdysinaz = xc + dy*sinaz;
						final double ycdycosaz = yc + dy*cosaz;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							cin.x = FMath.round(xcdysinaz + dx*cosaz);
							cin.y = FMath.round(ycdycosaz - dx*sinaz);
							if (cin.x < 0 || cin.x > maxx || cin.y < 0 || cin.y > maxy) anew[x] = background;
							else anew[x] = image.get(cin);
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_nearest_y(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around y-axis");
		messenger.log("Using nearest-neighbor sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		
		// Rotate using the inverse of the rotation matrix: (note that when this
		// method is called, the border size of image is zero)
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzsinay = xc - dz*sinay;
					final double zcdzcosay = zc + dz*cosay;
					for (cnew.y=cin.y=0; cnew.y<newdims.y; ++cnew.y, ++cin.y) {
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							cin.x = FMath.round(xcdzsinay + dx*cosay);
							cin.z = FMath.round(zcdzcosay + dx*sinay);
							if (cin.x < 0 || cin.x > maxx || cin.z < 0 || cin.z > maxz) anew[x] = background;
							else anew[x] = image.get(cin);
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_nearest_x(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around x-axis");
		messenger.log("Using nearest-neighbor sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		
		// Rotate using the inverse of the rotation matrix: (note that when this
		// method is called, the border size of image is zero)
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double ycdzsinax = yc + dz*sinax;
					final double zcdzcosax = zc + dz*cosax;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						cin.y = FMath.round(ycdzsinax + dy*cosax);
						cin.z = FMath.round(zcdzcosax - dy*sinax);
						cin.x = 0;
						if (cin.y < 0 || cin.y > maxy || cin.z < 0 || cin.z > maxz) for (int x=0; x<newdims.x; ++x) anew[x] = background;
						else for (int x=0; x<newdims.x; ++x, ++cin.x) anew[x] = image.get(cin);
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_nearest_zyx(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating around z-, y-, and x-axes");
		messenger.log("Using nearest-neighbor sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		
		// Rotate using the inverse of the rotation matrix: (note that when this
		// method is called, the border size of image is zero)
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzinvxz = xc + dz*invxz;
					final double ycdzinvyz = yc + dz*invyz;
					final double zcdzinvzz = zc + dz*invzz;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdzinvxzdyinvxy = xcdzinvxz + dy*invxy;
						final double ycdzinvyzdyinvyy = ycdzinvyz + dy*invyy;
						final double zcdzinvzzdyinvzy = zcdzinvzz + dy*invzy;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							cin.x = FMath.round(xcdzinvxzdyinvxy + dx*invxx);
							cin.y = FMath.round(ycdzinvyzdyinvyy + dx*invyx);
							cin.z = FMath.round(zcdzinvzzdyinvzy + dx*invzx);
							if (cin.x < 0 || cin.x > maxx || cin.y < 0 || cin.y > maxy || cin.z < 0 || cin.z > maxz) anew[x] = background;
							else anew[x] = image.get(cin);
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_linear_z(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around z-axis");
		messenger.log("Using linear sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdysinaz = xc + dy*sinaz;
						final double ycdycosaz = yc + dy*cosaz;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdysinaz + dx*cosaz;
							final double ty = ycdycosaz - dx*sinaz;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double ydiff = ty - iy;
								final double xmdiff = 1 - xdiff;
								final double ymdiff = 1 - ydiff;
								cin.x = borders.x + ix;
								cin.y = borders.y + iy;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.y;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin);
								anew[x] = (
									ymdiff*xmdiff*in00 +
									ymdiff*xdiff*in01 +
									ydiff*xmdiff*in10 +
									ydiff*xdiff*in11
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_linear_y(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around y-axis");
		messenger.log("Using linear sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzsinay = xc - dz*sinay;
					final double zcdzcosay = zc + dz*cosay;
					for (cnew.y=cin.y=0; cnew.y<newdims.y; ++cnew.y, ++cin.y) {
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzsinay + dx*cosay;
							final double tz = zcdzcosay + dx*sinay;
							final int ix = FMath.floor(tx);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double zdiff = tz - iz;
								final double xmdiff = 1 - xdiff;
								final double zmdiff = 1 - zdiff;
								cin.x = borders.x + ix;
								cin.z = borders.z + iz;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.z;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin);
								anew[x] = (
									zmdiff*xmdiff*in00 +
									zmdiff*xdiff*in01 +
									zdiff*xmdiff*in10 +
									zdiff*xdiff*in11
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_linear_x(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around x-axis");
		messenger.log("Using linear sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double ycdzsinax = yc + dz*sinax;
					final double zcdzcosax = zc + dz*cosax;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double ty = ycdzsinax + dy*cosax;
						final double tz = zcdzcosax - dy*sinax;
						final int iy = FMath.floor(ty);
						final int iz = FMath.floor(tz);
						if (iy < -1 || iy > maxy || iz < -1 || iz > maxz)
							for (int x=0; x<newdims.x; ++x) anew[x] = background;
						else {
							final double ydiff = ty - iy;
							final double zdiff = tz - iz;
							final double ymdiff = 1 - ydiff;
							final double zmdiff = 1 - zdiff;
							cin.y = borders.y + iy;
							cin.z = borders.z + iz;
							cin.x = 0;
							for (int x=0; x<newdims.x; ++x, ++cin.x) {
								final double in00 = image.get(cin); ++cin.y;
								final double in01 = image.get(cin); ++cin.z;
								final double in11 = image.get(cin); --cin.y;
								final double in10 = image.get(cin); --cin.z;
								anew[x] = (
									zmdiff*ymdiff*in00 +
									zmdiff*ydiff*in01 +
									zdiff*ymdiff*in10 +
									zdiff*ydiff*in11
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_linear_zyx(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating around z-, y-, and x-axes");
		messenger.log("Using linear sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzinvxz = xc + dz*invxz;
					final double ycdzinvyz = yc + dz*invyz;
					final double zcdzinvzz = zc + dz*invzz;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdzinvxzdyinvxy = xcdzinvxz + dy*invxy;
						final double ycdzinvyzdyinvyy = ycdzinvyz + dy*invyy;
						final double zcdzinvzzdyinvzy = zcdzinvzz + dy*invzy;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzinvxzdyinvxy + dx*invxx;
							final double ty = ycdzinvyzdyinvyy + dx*invyx;
							final double tz = zcdzinvzzdyinvzy + dx*invzx;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double ydiff = ty - iy;
								final double zdiff = tz - iz;
								final double xmdiff = 1 - xdiff;
								final double ymdiff = 1 - ydiff;
								final double zmdiff = 1 - zdiff;
								cin.x = borders.x + ix;
								cin.y = borders.y + iy;
								cin.z = borders.z + iz;
								final double in000 = image.get(cin); ++cin.x;
								final double in001 = image.get(cin); ++cin.y;
								final double in011 = image.get(cin); --cin.x;
								final double in010 = image.get(cin); ++cin.z;
								final double in110 = image.get(cin); ++cin.x;
								final double in111 = image.get(cin); --cin.y;
								final double in101 = image.get(cin); --cin.x;
								final double in100 = image.get(cin);
								anew[x] = (
									zmdiff*ymdiff*xmdiff*in000 +
									zmdiff*ymdiff*xdiff*in001 +
									zmdiff*ydiff*xmdiff*in010 +
									zmdiff*ydiff*xdiff*in011 +
									zdiff*ymdiff*xmdiff*in100 +
									zdiff*ymdiff*xdiff*in101 +
									zdiff*ydiff*xmdiff*in110 +
									zdiff*ydiff*xdiff*in111
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_cubic_z(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around z-axis");
		messenger.log("Using cubic convolution sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdysinaz = xc + dy*sinaz;
						final double ycdycosaz = yc + dy*cosaz;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdysinaz + dx*cosaz;
							final double ty = ycdycosaz - dx*sinaz;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = fm1o2*xdiff*xmdiff*xmdiff;
								final double wx00 = 1 + (f3o2*xdiff - f5o2)*xdiff*xdiff;
								final double wxp1 = 1 + (f3o2*xmdiff - f5o2)*xmdiff*xmdiff;
								final double wxp2 = fm1o2*xmdiff*xdiff*xdiff;
								final double ydiff = ty - iy;
								final double ymdiff = 1 - ydiff;
								final double wym1 = fm1o2*ydiff*ymdiff*ymdiff;
								final double wy00 = 1 + (f3o2*ydiff - f5o2)*ydiff*ydiff;
								final double wyp1 = 1 + (f3o2*ymdiff - f5o2)*ymdiff*ymdiff;
								final double wyp2 = fm1o2*ymdiff*ydiff*ydiff;
								cin.x = borders.x + ix - 1;
								cin.y = borders.y + iy - 1;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.x;
								final double in02 = image.get(cin); ++cin.x;
								final double in03 = image.get(cin); ++cin.y;
								final double in13 = image.get(cin); --cin.x;
								final double in12 = image.get(cin); --cin.x;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin); ++cin.y;
								final double in20 = image.get(cin); ++cin.x;
								final double in21 = image.get(cin); ++cin.x;
								final double in22 = image.get(cin); ++cin.x;
								final double in23 = image.get(cin); ++cin.y;
								final double in33 = image.get(cin); --cin.x;
								final double in32 = image.get(cin); --cin.x;
								final double in31 = image.get(cin); --cin.x;
								final double in30 = image.get(cin);
								anew[x] = (
									wym1*(wxm1*in00 + wx00*in01 + wxp1*in02 + wxp2*in03) +
									wy00*(wxm1*in10 + wx00*in11 + wxp1*in12 + wxp2*in13) +
									wyp1*(wxm1*in20 + wx00*in21 + wxp1*in22 + wxp2*in23) +
									wyp2*(wxm1*in30 + wx00*in31 + wxp1*in32 + wxp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_cubic_y(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around y-axis");
		messenger.log("Using cubic convolution sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzsinay = xc - dz*sinay;
					final double zcdzcosay = zc + dz*cosay;
					for (cnew.y=cin.y=0; cnew.y<newdims.y; ++cnew.y, ++cin.y) {
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzsinay + dx*cosay;
							final double tz = zcdzcosay + dx*sinay;
							final int ix = FMath.floor(tx);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = fm1o2*xdiff*xmdiff*xmdiff;
								final double wx00 = 1 + (f3o2*xdiff - f5o2)*xdiff*xdiff;
								final double wxp1 = 1 + (f3o2*xmdiff - f5o2)*xmdiff*xmdiff;
								final double wxp2 = fm1o2*xmdiff*xdiff*xdiff;
								final double zdiff = tz - iz;
								final double zmdiff = 1 - zdiff;
								final double wzm1 = fm1o2*zdiff*zmdiff*zmdiff;
								final double wz00 = 1 + (f3o2*zdiff - f5o2)*zdiff*zdiff;
								final double wzp1 = 1 + (f3o2*zmdiff - f5o2)*zmdiff*zmdiff;
								final double wzp2 = fm1o2*zmdiff*zdiff*zdiff;
								cin.x = borders.x + ix - 1;
								cin.z = borders.z + iz - 1;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.x;
								final double in02 = image.get(cin); ++cin.x;
								final double in03 = image.get(cin); ++cin.z;
								final double in13 = image.get(cin); --cin.x;
								final double in12 = image.get(cin); --cin.x;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin); ++cin.z;
								final double in20 = image.get(cin); ++cin.x;
								final double in21 = image.get(cin); ++cin.x;
								final double in22 = image.get(cin); ++cin.x;
								final double in23 = image.get(cin); ++cin.z;
								final double in33 = image.get(cin); --cin.x;
								final double in32 = image.get(cin); --cin.x;
								final double in31 = image.get(cin); --cin.x;
								final double in30 = image.get(cin);
								anew[x] = (
									wzm1*(wxm1*in00 + wx00*in01 + wxp1*in02 + wxp2*in03) +
									wz00*(wxm1*in10 + wx00*in11 + wxp1*in12 + wxp2*in13) +
									wzp1*(wxm1*in20 + wx00*in21 + wxp1*in22 + wxp2*in23) +
									wzp2*(wxm1*in30 + wx00*in31 + wxp1*in32 + wxp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_cubic_x(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around x-axis");
		messenger.log("Using cubic convolution sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double ycdzsinax = yc + dz*sinax;
					final double zcdzcosax = zc + dz*cosax;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double ty = ycdzsinax + dy*cosax;
						final double tz = zcdzcosax - dy*sinax;
						final int iy = FMath.floor(ty);
						final int iz = FMath.floor(tz);
						if (iy < -1 || iy > maxy || iz < -1 || iz > maxz)
							for (int x=0; x<newdims.x; ++x) anew[x] = background;
						else {
							final double ydiff = ty - iy;
							final double ymdiff = 1 - ydiff;
							final double wym1 = fm1o2*ydiff*ymdiff*ymdiff;
							final double wy00 = 1 + (f3o2*ydiff - f5o2)*ydiff*ydiff;
							final double wyp1 = 1 + (f3o2*ymdiff - f5o2)*ymdiff*ymdiff;
							final double wyp2 = fm1o2*ymdiff*ydiff*ydiff;
							final double zdiff = tz - iz;
							final double zmdiff = 1 - zdiff;
							final double wzm1 = fm1o2*zdiff*zmdiff*zmdiff;
							final double wz00 = 1 + (f3o2*zdiff - f5o2)*zdiff*zdiff;
							final double wzp1 = 1 + (f3o2*zmdiff - f5o2)*zmdiff*zmdiff;
							final double wzp2 = fm1o2*zmdiff*zdiff*zdiff;
							cin.y = borders.y + iy - 1;
							cin.z = borders.z + iz - 1;
							cin.x = 0;
							for (int x=0; x<newdims.x; ++x, ++cin.x) {
								final double in00 = image.get(cin); ++cin.y;
								final double in01 = image.get(cin); ++cin.y;
								final double in02 = image.get(cin); ++cin.y;
								final double in03 = image.get(cin); ++cin.z;
								final double in13 = image.get(cin); --cin.y;
								final double in12 = image.get(cin); --cin.y;
								final double in11 = image.get(cin); --cin.y;
								final double in10 = image.get(cin); ++cin.z;
								final double in20 = image.get(cin); ++cin.y;
								final double in21 = image.get(cin); ++cin.y;
								final double in22 = image.get(cin); ++cin.y;
								final double in23 = image.get(cin); ++cin.z;
								final double in33 = image.get(cin); --cin.y;
								final double in32 = image.get(cin); --cin.y;
								final double in31 = image.get(cin); --cin.y;
								final double in30 = image.get(cin); cin.z -= 3;
								anew[x] = (
									wzm1*(wym1*in00 + wy00*in01 + wyp1*in02 + wyp2*in03) +
									wz00*(wym1*in10 + wy00*in11 + wyp1*in12 + wyp2*in13) +
									wzp1*(wym1*in20 + wy00*in21 + wyp1*in22 + wyp2*in23) +
									wzp2*(wym1*in30 + wy00*in31 + wyp1*in32 + wyp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_cubic_zyx(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating around z-, y-, and x-axes");
		messenger.log("Using cubic convolution sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzinvxz = xc + dz*invxz;
					final double ycdzinvyz = yc + dz*invyz;
					final double zcdzinvzz = zc + dz*invzz;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdzinvxzdyinvxy = xcdzinvxz + dy*invxy;
						final double ycdzinvyzdyinvyy = ycdzinvyz + dy*invyy;
						final double zcdzinvzzdyinvzy = zcdzinvzz + dy*invzy;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzinvxzdyinvxy + dx*invxx;
							final double ty = ycdzinvyzdyinvyy + dx*invyx;
							final double tz = zcdzinvzzdyinvzy + dx*invzx;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = fm1o2*xdiff*xmdiff*xmdiff;
								final double wx00 = 1 + (f3o2*xdiff - f5o2)*xdiff*xdiff;
								final double wxp1 = 1 + (f3o2*xmdiff - f5o2)*xmdiff*xmdiff;
								final double wxp2 = fm1o2*xmdiff*xdiff*xdiff;
								final double ydiff = ty - iy;
								final double ymdiff = 1 - ydiff;
								final double wym1 = fm1o2*ydiff*ymdiff*ymdiff;
								final double wy00 = 1 + (f3o2*ydiff - f5o2)*ydiff*ydiff;
								final double wyp1 = 1 + (f3o2*ymdiff - f5o2)*ymdiff*ymdiff;
								final double wyp2 = fm1o2*ymdiff*ydiff*ydiff;
								final double zdiff = tz - iz;
								final double zmdiff = 1 - zdiff;
								final double wzm1 = fm1o2*zdiff*zmdiff*zmdiff;
								final double wz00 = 1 + (f3o2*zdiff - f5o2)*zdiff*zdiff;
								final double wzp1 = 1 + (f3o2*zmdiff - f5o2)*zmdiff*zmdiff;
								final double wzp2 = fm1o2*zmdiff*zdiff*zdiff;
								cin.x = borders.x + ix - 1;
								cin.y = borders.y + iy - 1;
								cin.z = borders.z + iz - 1;
								final double in000 = image.get(cin); ++cin.x;
								final double in001 = image.get(cin); ++cin.x;
								final double in002 = image.get(cin); ++cin.x;
								final double in003 = image.get(cin); ++cin.y;
								final double in013 = image.get(cin); --cin.x;
								final double in012 = image.get(cin); --cin.x;
								final double in011 = image.get(cin); --cin.x;
								final double in010 = image.get(cin); ++cin.y;
								final double in020 = image.get(cin); ++cin.x;
								final double in021 = image.get(cin); ++cin.x;
								final double in022 = image.get(cin); ++cin.x;
								final double in023 = image.get(cin); ++cin.y;
								final double in033 = image.get(cin); --cin.x;
								final double in032 = image.get(cin); --cin.x;
								final double in031 = image.get(cin); --cin.x;
								final double in030 = image.get(cin); ++cin.z;
								final double in130 = image.get(cin); ++cin.x;
								final double in131 = image.get(cin); ++cin.x;
								final double in132 = image.get(cin); ++cin.x;
								final double in133 = image.get(cin); --cin.y;
								final double in123 = image.get(cin); --cin.x;
								final double in122 = image.get(cin); --cin.x;
								final double in121 = image.get(cin); --cin.x;
								final double in120 = image.get(cin); --cin.y;
								final double in110 = image.get(cin); ++cin.x;
								final double in111 = image.get(cin); ++cin.x;
								final double in112 = image.get(cin); ++cin.x;
								final double in113 = image.get(cin); --cin.y;
								final double in103 = image.get(cin); --cin.x;
								final double in102 = image.get(cin); --cin.x;
								final double in101 = image.get(cin); --cin.x;
								final double in100 = image.get(cin); ++cin.z;
								final double in200 = image.get(cin); ++cin.x;
								final double in201 = image.get(cin); ++cin.x;
								final double in202 = image.get(cin); ++cin.x;
								final double in203 = image.get(cin); ++cin.y;
								final double in213 = image.get(cin); --cin.x;
								final double in212 = image.get(cin); --cin.x;
								final double in211 = image.get(cin); --cin.x;
								final double in210 = image.get(cin); ++cin.y;
								final double in220 = image.get(cin); ++cin.x;
								final double in221 = image.get(cin); ++cin.x;
								final double in222 = image.get(cin); ++cin.x;
								final double in223 = image.get(cin); ++cin.y;
								final double in233 = image.get(cin); --cin.x;
								final double in232 = image.get(cin); --cin.x;
								final double in231 = image.get(cin); --cin.x;
								final double in230 = image.get(cin); ++cin.z;
								final double in330 = image.get(cin); ++cin.x;
								final double in331 = image.get(cin); ++cin.x;
								final double in332 = image.get(cin); ++cin.x;
								final double in333 = image.get(cin); --cin.y;
								final double in323 = image.get(cin); --cin.x;
								final double in322 = image.get(cin); --cin.x;
								final double in321 = image.get(cin); --cin.x;
								final double in320 = image.get(cin); --cin.y;
								final double in310 = image.get(cin); ++cin.x;
								final double in311 = image.get(cin); ++cin.x;
								final double in312 = image.get(cin); ++cin.x;
								final double in313 = image.get(cin); --cin.y;
								final double in303 = image.get(cin); --cin.x;
								final double in302 = image.get(cin); --cin.x;
								final double in301 = image.get(cin); --cin.x;
								final double in300 = image.get(cin);
								anew[x] = (
									wzm1*(
										wym1*(wxm1*in000 + wx00*in001 + wxp1*in002 + wxp2*in003) +
										wy00*(wxm1*in010 + wx00*in011 + wxp1*in012 + wxp2*in013) +
										wyp1*(wxm1*in020 + wx00*in021 + wxp1*in022 + wxp2*in023) +
										wyp2*(wxm1*in030 + wx00*in031 + wxp1*in032 + wxp2*in033)
									) +
									wz00*(
										wym1*(wxm1*in100 + wx00*in101 + wxp1*in102 + wxp2*in103) +
										wy00*(wxm1*in110 + wx00*in111 + wxp1*in112 + wxp2*in113) +
										wyp1*(wxm1*in120 + wx00*in121 + wxp1*in122 + wxp2*in123) +
										wyp2*(wxm1*in130 + wx00*in131 + wxp1*in132 + wxp2*in133)
									) +
									wzp1*(
										wym1*(wxm1*in200 + wx00*in201 + wxp1*in202 + wxp2*in203) +
										wy00*(wxm1*in210 + wx00*in211 + wxp1*in212 + wxp2*in213) +
										wyp1*(wxm1*in220 + wx00*in221 + wxp1*in222 + wxp2*in223) +
										wyp2*(wxm1*in230 + wx00*in231 + wxp1*in232 + wxp2*in233)
									) +
									wzp2*(
										wym1*(wxm1*in300 + wx00*in301 + wxp1*in302 + wxp2*in303) +
										wy00*(wxm1*in310 + wx00*in311 + wxp1*in312 + wxp2*in313) +
										wyp1*(wxm1*in320 + wx00*in321 + wxp1*in322 + wxp2*in323) +
										wyp2*(wxm1*in330 + wx00*in331 + wxp1*in332 + wxp2*in333)
									)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_bspline3_z(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around z-axis");
		messenger.log("Applying cubic B-spline prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline3(image,new Axes(true,true,false),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the cubic B-spline kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE3X0;
			if (indims.y == 1) scale /= BSPLINE3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdysinaz = xc + dy*sinaz;
						final double ycdycosaz = yc + dy*cosaz;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdysinaz + dx*cosaz;
							final double ty = ycdycosaz - dx*sinaz;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = f1o6*xmdiff*xmdiff*xmdiff;
								final double wx00 = f2o3 + (f1o2*xdiff - 1)*xdiff*xdiff;
								final double wxp1 = f2o3 + (f1o2*xmdiff - 1)*xmdiff*xmdiff;
								final double wxp2 = f1o6*xdiff*xdiff*xdiff;
								final double ydiff = ty - iy;
								final double ymdiff = 1 - ydiff;
								final double wym1 = f1o6*ymdiff*ymdiff*ymdiff;
								final double wy00 = f2o3 + (f1o2*ydiff - 1)*ydiff*ydiff;
								final double wyp1 = f2o3 + (f1o2*ymdiff - 1)*ymdiff*ymdiff;
								final double wyp2 = f1o6*ydiff*ydiff*ydiff;
								cin.x = borders.x + ix - 1;
								cin.y = borders.y + iy - 1;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.x;
								final double in02 = image.get(cin); ++cin.x;
								final double in03 = image.get(cin); ++cin.y;
								final double in13 = image.get(cin); --cin.x;
								final double in12 = image.get(cin); --cin.x;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin); ++cin.y;
								final double in20 = image.get(cin); ++cin.x;
								final double in21 = image.get(cin); ++cin.x;
								final double in22 = image.get(cin); ++cin.x;
								final double in23 = image.get(cin); ++cin.y;
								final double in33 = image.get(cin); --cin.x;
								final double in32 = image.get(cin); --cin.x;
								final double in31 = image.get(cin); --cin.x;
								final double in30 = image.get(cin);
								anew[x] = (
									wym1*(wxm1*in00 + wx00*in01 + wxp1*in02 + wxp2*in03) +
									wy00*(wxm1*in10 + wx00*in11 + wxp1*in12 + wxp2*in13) +
									wyp1*(wxm1*in20 + wx00*in21 + wxp1*in22 + wxp2*in23) +
									wyp2*(wxm1*in30 + wx00*in31 + wxp1*in32 + wxp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_bspline3_y(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around y-axis");
		messenger.log("Applying cubic B-spline prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline3(image,new Axes(true,false,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the cubic B-spline kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE3X0;
			if (indims.z == 1) scale /= BSPLINE3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzsinay = xc - dz*sinay;
					final double zcdzcosay = zc + dz*cosay;
					for (cnew.y=cin.y=0; cnew.y<newdims.y; ++cnew.y, ++cin.y) {
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzsinay + dx*cosay;
							final double tz = zcdzcosay + dx*sinay;
							final int ix = FMath.floor(tx);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = f1o6*xmdiff*xmdiff*xmdiff;
								final double wx00 = f2o3 + (f1o2*xdiff - 1)*xdiff*xdiff;
								final double wxp1 = f2o3 + (f1o2*xmdiff - 1)*xmdiff*xmdiff;
								final double wxp2 = f1o6*xdiff*xdiff*xdiff;
								final double zdiff = tz - iz;
								final double zmdiff = 1 - zdiff;
								final double wzm1 = f1o6*zmdiff*zmdiff*zmdiff;
								final double wz00 = f2o3 + (f1o2*zdiff - 1)*zdiff*zdiff;
								final double wzp1 = f2o3 + (f1o2*zmdiff - 1)*zmdiff*zmdiff;
								final double wzp2 = f1o6*zdiff*zdiff*zdiff;
								cin.x = borders.x + ix - 1;
								cin.z = borders.z + iz - 1;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.x;
								final double in02 = image.get(cin); ++cin.x;
								final double in03 = image.get(cin); ++cin.z;
								final double in13 = image.get(cin); --cin.x;
								final double in12 = image.get(cin); --cin.x;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin); ++cin.z;
								final double in20 = image.get(cin); ++cin.x;
								final double in21 = image.get(cin); ++cin.x;
								final double in22 = image.get(cin); ++cin.x;
								final double in23 = image.get(cin); ++cin.z;
								final double in33 = image.get(cin); --cin.x;
								final double in32 = image.get(cin); --cin.x;
								final double in31 = image.get(cin); --cin.x;
								final double in30 = image.get(cin);
								anew[x] = (
									wzm1*(wxm1*in00 + wx00*in01 + wxp1*in02 + wxp2*in03) +
									wz00*(wxm1*in10 + wx00*in11 + wxp1*in12 + wxp2*in13) +
									wzp1*(wxm1*in20 + wx00*in21 + wxp1*in22 + wxp2*in23) +
									wzp2*(wxm1*in30 + wx00*in31 + wxp1*in32 + wxp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_bspline3_x(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around x-axis");
		messenger.log("Applying cubic B-spline prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline3(image,new Axes(false,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the cubic B-spline kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.y == 1) scale /= BSPLINE3X0;
			if (indims.z == 1) scale /= BSPLINE3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double ycdzsinax = yc + dz*sinax;
					final double zcdzcosax = zc + dz*cosax;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double ty = ycdzsinax + dy*cosax;
						final double tz = zcdzcosax - dy*sinax;
						final int iy = FMath.floor(ty);
						final int iz = FMath.floor(tz);
						if (iy < -1 || iy > maxy || iz < -1 || iz > maxz)
							for (int x=0; x<newdims.x; ++x) anew[x] = background;
						else {
							final double ydiff = ty - iy;
							final double ymdiff = 1 - ydiff;
							final double wym1 = f1o6*ymdiff*ymdiff*ymdiff;
							final double wy00 = f2o3 + (f1o2*ydiff - 1)*ydiff*ydiff;
							final double wyp1 = f2o3 + (f1o2*ymdiff - 1)*ymdiff*ymdiff;
							final double wyp2 = f1o6*ydiff*ydiff*ydiff;
							final double zdiff = tz - iz;
							final double zmdiff = 1 - zdiff;
							final double wzm1 = f1o6*zmdiff*zmdiff*zmdiff;
							final double wz00 = f2o3 + (f1o2*zdiff - 1)*zdiff*zdiff;
							final double wzp1 = f2o3 + (f1o2*zmdiff - 1)*zmdiff*zmdiff;
							final double wzp2 = f1o6*zdiff*zdiff*zdiff;
							cin.y = borders.y + iy - 1;
							cin.z = borders.z + iz - 1;
							cin.x = 0;
							for (int x=0; x<newdims.x; ++x, ++cin.x) {
								final double in00 = image.get(cin); ++cin.y;
								final double in01 = image.get(cin); ++cin.y;
								final double in02 = image.get(cin); ++cin.y;
								final double in03 = image.get(cin); ++cin.z;
								final double in13 = image.get(cin); --cin.y;
								final double in12 = image.get(cin); --cin.y;
								final double in11 = image.get(cin); --cin.y;
								final double in10 = image.get(cin); ++cin.z;
								final double in20 = image.get(cin); ++cin.y;
								final double in21 = image.get(cin); ++cin.y;
								final double in22 = image.get(cin); ++cin.y;
								final double in23 = image.get(cin); ++cin.z;
								final double in33 = image.get(cin); --cin.y;
								final double in32 = image.get(cin); --cin.y;
								final double in31 = image.get(cin); --cin.y;
								final double in30 = image.get(cin); cin.z -= 3;
								anew[x] = (
									wzm1*(wym1*in00 + wy00*in01 + wyp1*in02 + wyp2*in03) +
									wz00*(wym1*in10 + wy00*in11 + wyp1*in12 + wyp2*in13) +
									wzp1*(wym1*in20 + wy00*in21 + wyp1*in22 + wyp2*in23) +
									wzp2*(wym1*in30 + wy00*in31 + wyp1*in32 + wyp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_bspline3_zyx(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating around z-, y-, and x-axes");
		messenger.log("Applying cubic B-spline prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline3(image,new Axes(true,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the cubic B-spline kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE3X0;
			if (indims.y == 1) scale /= BSPLINE3X0;
			if (indims.z == 1) scale /= BSPLINE3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzinvxz = xc + dz*invxz;
					final double ycdzinvyz = yc + dz*invyz;
					final double zcdzinvzz = zc + dz*invzz;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdzinvxzdyinvxy = xcdzinvxz + dy*invxy;
						final double ycdzinvyzdyinvyy = ycdzinvyz + dy*invyy;
						final double zcdzinvzzdyinvzy = zcdzinvzz + dy*invzy;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzinvxzdyinvxy + dx*invxx;
							final double ty = ycdzinvyzdyinvyy + dx*invyx;
							final double tz = zcdzinvzzdyinvzy + dx*invzx;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = f1o6*xmdiff*xmdiff*xmdiff;
								final double wx00 = f2o3 + (f1o2*xdiff - 1)*xdiff*xdiff;
								final double wxp1 = f2o3 + (f1o2*xmdiff - 1)*xmdiff*xmdiff;
								final double wxp2 = f1o6*xdiff*xdiff*xdiff;
								final double ydiff = ty - iy;
								final double ymdiff = 1 - ydiff;
								final double wym1 = f1o6*ymdiff*ymdiff*ymdiff;
								final double wy00 = f2o3 + (f1o2*ydiff - 1)*ydiff*ydiff;
								final double wyp1 = f2o3 + (f1o2*ymdiff - 1)*ymdiff*ymdiff;
								final double wyp2 = f1o6*ydiff*ydiff*ydiff;
								final double zdiff = tz - iz;
								final double zmdiff = 1 - zdiff;
								final double wzm1 = f1o6*zmdiff*zmdiff*zmdiff;
								final double wz00 = f2o3 + (f1o2*zdiff - 1)*zdiff*zdiff;
								final double wzp1 = f2o3 + (f1o2*zmdiff - 1)*zmdiff*zmdiff;
								final double wzp2 = f1o6*zdiff*zdiff*zdiff;
								cin.x = borders.x + ix - 1;
								cin.y = borders.y + iy - 1;
								cin.z = borders.z + iz - 1;
								final double in000 = image.get(cin); ++cin.x;
								final double in001 = image.get(cin); ++cin.x;
								final double in002 = image.get(cin); ++cin.x;
								final double in003 = image.get(cin); ++cin.y;
								final double in013 = image.get(cin); --cin.x;
								final double in012 = image.get(cin); --cin.x;
								final double in011 = image.get(cin); --cin.x;
								final double in010 = image.get(cin); ++cin.y;
								final double in020 = image.get(cin); ++cin.x;
								final double in021 = image.get(cin); ++cin.x;
								final double in022 = image.get(cin); ++cin.x;
								final double in023 = image.get(cin); ++cin.y;
								final double in033 = image.get(cin); --cin.x;
								final double in032 = image.get(cin); --cin.x;
								final double in031 = image.get(cin); --cin.x;
								final double in030 = image.get(cin); ++cin.z;
								final double in130 = image.get(cin); ++cin.x;
								final double in131 = image.get(cin); ++cin.x;
								final double in132 = image.get(cin); ++cin.x;
								final double in133 = image.get(cin); --cin.y;
								final double in123 = image.get(cin); --cin.x;
								final double in122 = image.get(cin); --cin.x;
								final double in121 = image.get(cin); --cin.x;
								final double in120 = image.get(cin); --cin.y;
								final double in110 = image.get(cin); ++cin.x;
								final double in111 = image.get(cin); ++cin.x;
								final double in112 = image.get(cin); ++cin.x;
								final double in113 = image.get(cin); --cin.y;
								final double in103 = image.get(cin); --cin.x;
								final double in102 = image.get(cin); --cin.x;
								final double in101 = image.get(cin); --cin.x;
								final double in100 = image.get(cin); ++cin.z;
								final double in200 = image.get(cin); ++cin.x;
								final double in201 = image.get(cin); ++cin.x;
								final double in202 = image.get(cin); ++cin.x;
								final double in203 = image.get(cin); ++cin.y;
								final double in213 = image.get(cin); --cin.x;
								final double in212 = image.get(cin); --cin.x;
								final double in211 = image.get(cin); --cin.x;
								final double in210 = image.get(cin); ++cin.y;
								final double in220 = image.get(cin); ++cin.x;
								final double in221 = image.get(cin); ++cin.x;
								final double in222 = image.get(cin); ++cin.x;
								final double in223 = image.get(cin); ++cin.y;
								final double in233 = image.get(cin); --cin.x;
								final double in232 = image.get(cin); --cin.x;
								final double in231 = image.get(cin); --cin.x;
								final double in230 = image.get(cin); ++cin.z;
								final double in330 = image.get(cin); ++cin.x;
								final double in331 = image.get(cin); ++cin.x;
								final double in332 = image.get(cin); ++cin.x;
								final double in333 = image.get(cin); --cin.y;
								final double in323 = image.get(cin); --cin.x;
								final double in322 = image.get(cin); --cin.x;
								final double in321 = image.get(cin); --cin.x;
								final double in320 = image.get(cin); --cin.y;
								final double in310 = image.get(cin); ++cin.x;
								final double in311 = image.get(cin); ++cin.x;
								final double in312 = image.get(cin); ++cin.x;
								final double in313 = image.get(cin); --cin.y;
								final double in303 = image.get(cin); --cin.x;
								final double in302 = image.get(cin); --cin.x;
								final double in301 = image.get(cin); --cin.x;
								final double in300 = image.get(cin);
								anew[x] = (
									wzm1*(
										wym1*(wxm1*in000 + wx00*in001 + wxp1*in002 + wxp2*in003) +
										wy00*(wxm1*in010 + wx00*in011 + wxp1*in012 + wxp2*in013) +
										wyp1*(wxm1*in020 + wx00*in021 + wxp1*in022 + wxp2*in023) +
										wyp2*(wxm1*in030 + wx00*in031 + wxp1*in032 + wxp2*in033)
									) +
									wz00*(
										wym1*(wxm1*in100 + wx00*in101 + wxp1*in102 + wxp2*in103) +
										wy00*(wxm1*in110 + wx00*in111 + wxp1*in112 + wxp2*in113) +
										wyp1*(wxm1*in120 + wx00*in121 + wxp1*in122 + wxp2*in123) +
										wyp2*(wxm1*in130 + wx00*in131 + wxp1*in132 + wxp2*in133)
									) +
									wzp1*(
										wym1*(wxm1*in200 + wx00*in201 + wxp1*in202 + wxp2*in203) +
										wy00*(wxm1*in210 + wx00*in211 + wxp1*in212 + wxp2*in213) +
										wyp1*(wxm1*in220 + wx00*in221 + wxp1*in222 + wxp2*in223) +
										wyp2*(wxm1*in230 + wx00*in231 + wxp1*in232 + wxp2*in233)
									) +
									wzp2*(
										wym1*(wxm1*in300 + wx00*in301 + wxp1*in302 + wxp2*in303) +
										wy00*(wxm1*in310 + wx00*in311 + wxp1*in312 + wxp2*in313) +
										wyp1*(wxm1*in320 + wx00*in321 + wxp1*in322 + wxp2*in323) +
										wyp2*(wxm1*in330 + wx00*in331 + wxp1*in332 + wxp2*in333)
									)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_omoms3_z(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around z-axis");
		messenger.log("Applying cubic O-MOMS prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.omoms3(image,new Axes(true,true,false),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the cubic O-MOMS kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= OMOMS3X0;
			if (indims.y == 1) scale /= OMOMS3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdysinaz = xc + dy*sinaz;
						final double ycdycosaz = yc + dy*cosaz;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdysinaz + dx*cosaz;
							final double ty = ycdycosaz - dx*sinaz;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = xmdiff*(f1o42 + f1o6*xmdiff*xmdiff);
								final double wx00 = f13o21 + xdiff*(f1o14 + xdiff*(f1o2*xdiff - 1));
								final double wxp1 = f13o21 + xmdiff*(f1o14 + xmdiff*(f1o2*xmdiff - 1));
								final double wxp2 = xdiff*(f1o42 + f1o6*xdiff*xdiff);
								final double ydiff = ty - iy;
								final double ymdiff = 1 - ydiff;
								final double wym1 = ymdiff*(f1o42 + f1o6*ymdiff*ymdiff);
								final double wy00 = f13o21 + ydiff*(f1o14 + ydiff*(f1o2*ydiff - 1));
								final double wyp1 = f13o21 + ymdiff*(f1o14 + ymdiff*(f1o2*ymdiff - 1));
								final double wyp2 = ydiff*(f1o42 + f1o6*ydiff*ydiff);
								cin.x = borders.x + ix - 1;
								cin.y = borders.y + iy - 1;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.x;
								final double in02 = image.get(cin); ++cin.x;
								final double in03 = image.get(cin); ++cin.y;
								final double in13 = image.get(cin); --cin.x;
								final double in12 = image.get(cin); --cin.x;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin); ++cin.y;
								final double in20 = image.get(cin); ++cin.x;
								final double in21 = image.get(cin); ++cin.x;
								final double in22 = image.get(cin); ++cin.x;
								final double in23 = image.get(cin); ++cin.y;
								final double in33 = image.get(cin); --cin.x;
								final double in32 = image.get(cin); --cin.x;
								final double in31 = image.get(cin); --cin.x;
								final double in30 = image.get(cin);
								anew[x] = (
									wym1*(wxm1*in00 + wx00*in01 + wxp1*in02 + wxp2*in03) +
									wy00*(wxm1*in10 + wx00*in11 + wxp1*in12 + wxp2*in13) +
									wyp1*(wxm1*in20 + wx00*in21 + wxp1*in22 + wxp2*in23) +
									wyp2*(wxm1*in30 + wx00*in31 + wxp1*in32 + wxp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_omoms3_y(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around y-axis");
		messenger.log("Applying cubic O-MOMS prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.omoms3(image,new Axes(true,false,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the cubic O-MOMS kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= OMOMS3X0;
			if (indims.z == 1) scale /= OMOMS3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzsinay = xc - dz*sinay;
					final double zcdzcosay = zc + dz*cosay;
					for (cnew.y=cin.y=0; cnew.y<newdims.y; ++cnew.y, ++cin.y) {
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzsinay + dx*cosay;
							final double tz = zcdzcosay + dx*sinay;
							final int ix = FMath.floor(tx);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = xmdiff*(f1o42 + f1o6*xmdiff*xmdiff);
								final double wx00 = f13o21 + xdiff*(f1o14 + xdiff*(f1o2*xdiff - 1));
								final double wxp1 = f13o21 + xmdiff*(f1o14 + xmdiff*(f1o2*xmdiff - 1));
								final double wxp2 = xdiff*(f1o42 + f1o6*xdiff*xdiff);
								final double zdiff = tz - iz;
								final double zmdiff = 1 - zdiff;
								final double wzm1 = zmdiff*(f1o42 + f1o6*zmdiff*zmdiff);
								final double wz00 = f13o21 + zdiff*(f1o14 + zdiff*(f1o2*zdiff - 1));
								final double wzp1 = f13o21 + zmdiff*(f1o14 + zmdiff*(f1o2*zmdiff - 1));
								final double wzp2 = zdiff*(f1o42 + f1o6*zdiff*zdiff);
								cin.x = borders.x + ix - 1;
								cin.z = borders.z + iz - 1;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.x;
								final double in02 = image.get(cin); ++cin.x;
								final double in03 = image.get(cin); ++cin.z;
								final double in13 = image.get(cin); --cin.x;
								final double in12 = image.get(cin); --cin.x;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin); ++cin.z;
								final double in20 = image.get(cin); ++cin.x;
								final double in21 = image.get(cin); ++cin.x;
								final double in22 = image.get(cin); ++cin.x;
								final double in23 = image.get(cin); ++cin.z;
								final double in33 = image.get(cin); --cin.x;
								final double in32 = image.get(cin); --cin.x;
								final double in31 = image.get(cin); --cin.x;
								final double in30 = image.get(cin);
								anew[x] = (
									wzm1*(wxm1*in00 + wx00*in01 + wxp1*in02 + wxp2*in03) +
									wz00*(wxm1*in10 + wx00*in11 + wxp1*in12 + wxp2*in13) +
									wzp1*(wxm1*in20 + wx00*in21 + wxp1*in22 + wxp2*in23) +
									wzp2*(wxm1*in30 + wx00*in31 + wxp1*in32 + wxp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_omoms3_x(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around x-axis");
		messenger.log("Applying cubic O-MOMS prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.omoms3(image,new Axes(false,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the cubic O-MOMS kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.y == 1) scale /= OMOMS3X0;
			if (indims.z == 1) scale /= OMOMS3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double ycdzsinax = yc + dz*sinax;
					final double zcdzcosax = zc + dz*cosax;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double ty = ycdzsinax + dy*cosax;
						final double tz = zcdzcosax - dy*sinax;
						final int iy = FMath.floor(ty);
						final int iz = FMath.floor(tz);
						if (iy < -1 || iy > maxy || iz < -1 || iz > maxz)
							for (int x=0; x<newdims.x; ++x) anew[x] = background;
						else {
							final double ydiff = ty - iy;
							final double ymdiff = 1 - ydiff;
							final double wym1 = ymdiff*(f1o42 + f1o6*ymdiff*ymdiff);
							final double wy00 = f13o21 + ydiff*(f1o14 + ydiff*(f1o2*ydiff - 1));
							final double wyp1 = f13o21 + ymdiff*(f1o14 + ymdiff*(f1o2*ymdiff - 1));
							final double wyp2 = ydiff*(f1o42 + f1o6*ydiff*ydiff);
							final double zdiff = tz - iz;
							final double zmdiff = 1 - zdiff;
							final double wzm1 = zmdiff*(f1o42 + f1o6*zmdiff*zmdiff);
							final double wz00 = f13o21 + zdiff*(f1o14 + zdiff*(f1o2*zdiff - 1));
							final double wzp1 = f13o21 + zmdiff*(f1o14 + zmdiff*(f1o2*zmdiff - 1));
							final double wzp2 = zdiff*(f1o42 + f1o6*zdiff*zdiff);
							cin.y = borders.y + iy - 1;
							cin.z = borders.z + iz - 1;
							cin.x = 0;
							for (int x=0; x<newdims.x; ++x, ++cin.x) {
								final double in00 = image.get(cin); ++cin.y;
								final double in01 = image.get(cin); ++cin.y;
								final double in02 = image.get(cin); ++cin.y;
								final double in03 = image.get(cin); ++cin.z;
								final double in13 = image.get(cin); --cin.y;
								final double in12 = image.get(cin); --cin.y;
								final double in11 = image.get(cin); --cin.y;
								final double in10 = image.get(cin); ++cin.z;
								final double in20 = image.get(cin); ++cin.y;
								final double in21 = image.get(cin); ++cin.y;
								final double in22 = image.get(cin); ++cin.y;
								final double in23 = image.get(cin); ++cin.z;
								final double in33 = image.get(cin); --cin.y;
								final double in32 = image.get(cin); --cin.y;
								final double in31 = image.get(cin); --cin.y;
								final double in30 = image.get(cin); cin.z -= 3;
								anew[x] = (
									wzm1*(wym1*in00 + wy00*in01 + wyp1*in02 + wyp2*in03) +
									wz00*(wym1*in10 + wy00*in11 + wyp1*in12 + wyp2*in13) +
									wzp1*(wym1*in20 + wy00*in21 + wyp1*in22 + wyp2*in23) +
									wzp2*(wym1*in30 + wy00*in31 + wyp1*in32 + wyp2*in33)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_omoms3_zyx(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating around z-, y-, and x-axes");
		messenger.log("Applying cubic O-MOMS prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.omoms3(image,new Axes(true,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the cubic O-MOMS kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= OMOMS3X0;
			if (indims.y == 1) scale /= OMOMS3X0;
			if (indims.z == 1) scale /= OMOMS3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzinvxz = xc + dz*invxz;
					final double ycdzinvyz = yc + dz*invyz;
					final double zcdzinvzz = zc + dz*invzz;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdzinvxzdyinvxy = xcdzinvxz + dy*invxy;
						final double ycdzinvyzdyinvyy = ycdzinvyz + dy*invyy;
						final double zcdzinvzzdyinvzy = zcdzinvzz + dy*invzy;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzinvxzdyinvxy + dx*invxx;
							final double ty = ycdzinvyzdyinvyy + dx*invyx;
							final double tz = zcdzinvzzdyinvzy + dx*invzx;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xmdiff = 1 - xdiff;
								final double wxm1 = xmdiff*(f1o42 + f1o6*xmdiff*xmdiff);
								final double wx00 = f13o21 + xdiff*(f1o14 + xdiff*(f1o2*xdiff - 1));
								final double wxp1 = f13o21 + xmdiff*(f1o14 + xmdiff*(f1o2*xmdiff - 1));
								final double wxp2 = xdiff*(f1o42 + f1o6*xdiff*xdiff);
								final double ydiff = ty - iy;
								final double ymdiff = 1 - ydiff;
								final double wym1 = ymdiff*(f1o42 + f1o6*ymdiff*ymdiff);
								final double wy00 = f13o21 + ydiff*(f1o14 + ydiff*(f1o2*ydiff - 1));
								final double wyp1 = f13o21 + ymdiff*(f1o14 + ymdiff*(f1o2*ymdiff - 1));
								final double wyp2 = ydiff*(f1o42 + f1o6*ydiff*ydiff);
								final double zdiff = tz - iz;
								final double zmdiff = 1 - zdiff;
								final double wzm1 = zmdiff*(f1o42 + f1o6*zmdiff*zmdiff);
								final double wz00 = f13o21 + zdiff*(f1o14 + zdiff*(f1o2*zdiff - 1));
								final double wzp1 = f13o21 + zmdiff*(f1o14 + zmdiff*(f1o2*zmdiff - 1));
								final double wzp2 = zdiff*(f1o42 + f1o6*zdiff*zdiff);
								cin.x = borders.x + ix - 1;
								cin.y = borders.y + iy - 1;
								cin.z = borders.z + iz - 1;
								final double in000 = image.get(cin); ++cin.x;
								final double in001 = image.get(cin); ++cin.x;
								final double in002 = image.get(cin); ++cin.x;
								final double in003 = image.get(cin); ++cin.y;
								final double in013 = image.get(cin); --cin.x;
								final double in012 = image.get(cin); --cin.x;
								final double in011 = image.get(cin); --cin.x;
								final double in010 = image.get(cin); ++cin.y;
								final double in020 = image.get(cin); ++cin.x;
								final double in021 = image.get(cin); ++cin.x;
								final double in022 = image.get(cin); ++cin.x;
								final double in023 = image.get(cin); ++cin.y;
								final double in033 = image.get(cin); --cin.x;
								final double in032 = image.get(cin); --cin.x;
								final double in031 = image.get(cin); --cin.x;
								final double in030 = image.get(cin); ++cin.z;
								final double in130 = image.get(cin); ++cin.x;
								final double in131 = image.get(cin); ++cin.x;
								final double in132 = image.get(cin); ++cin.x;
								final double in133 = image.get(cin); --cin.y;
								final double in123 = image.get(cin); --cin.x;
								final double in122 = image.get(cin); --cin.x;
								final double in121 = image.get(cin); --cin.x;
								final double in120 = image.get(cin); --cin.y;
								final double in110 = image.get(cin); ++cin.x;
								final double in111 = image.get(cin); ++cin.x;
								final double in112 = image.get(cin); ++cin.x;
								final double in113 = image.get(cin); --cin.y;
								final double in103 = image.get(cin); --cin.x;
								final double in102 = image.get(cin); --cin.x;
								final double in101 = image.get(cin); --cin.x;
								final double in100 = image.get(cin); ++cin.z;
								final double in200 = image.get(cin); ++cin.x;
								final double in201 = image.get(cin); ++cin.x;
								final double in202 = image.get(cin); ++cin.x;
								final double in203 = image.get(cin); ++cin.y;
								final double in213 = image.get(cin); --cin.x;
								final double in212 = image.get(cin); --cin.x;
								final double in211 = image.get(cin); --cin.x;
								final double in210 = image.get(cin); ++cin.y;
								final double in220 = image.get(cin); ++cin.x;
								final double in221 = image.get(cin); ++cin.x;
								final double in222 = image.get(cin); ++cin.x;
								final double in223 = image.get(cin); ++cin.y;
								final double in233 = image.get(cin); --cin.x;
								final double in232 = image.get(cin); --cin.x;
								final double in231 = image.get(cin); --cin.x;
								final double in230 = image.get(cin); ++cin.z;
								final double in330 = image.get(cin); ++cin.x;
								final double in331 = image.get(cin); ++cin.x;
								final double in332 = image.get(cin); ++cin.x;
								final double in333 = image.get(cin); --cin.y;
								final double in323 = image.get(cin); --cin.x;
								final double in322 = image.get(cin); --cin.x;
								final double in321 = image.get(cin); --cin.x;
								final double in320 = image.get(cin); --cin.y;
								final double in310 = image.get(cin); ++cin.x;
								final double in311 = image.get(cin); ++cin.x;
								final double in312 = image.get(cin); ++cin.x;
								final double in313 = image.get(cin); --cin.y;
								final double in303 = image.get(cin); --cin.x;
								final double in302 = image.get(cin); --cin.x;
								final double in301 = image.get(cin); --cin.x;
								final double in300 = image.get(cin);
								anew[x] = (
									wzm1*(
										wym1*(wxm1*in000 + wx00*in001 + wxp1*in002 + wxp2*in003) +
										wy00*(wxm1*in010 + wx00*in011 + wxp1*in012 + wxp2*in013) +
										wyp1*(wxm1*in020 + wx00*in021 + wxp1*in022 + wxp2*in023) +
										wyp2*(wxm1*in030 + wx00*in031 + wxp1*in032 + wxp2*in033)
									) +
									wz00*(
										wym1*(wxm1*in100 + wx00*in101 + wxp1*in102 + wxp2*in103) +
										wy00*(wxm1*in110 + wx00*in111 + wxp1*in112 + wxp2*in113) +
										wyp1*(wxm1*in120 + wx00*in121 + wxp1*in122 + wxp2*in123) +
										wyp2*(wxm1*in130 + wx00*in131 + wxp1*in132 + wxp2*in133)
									) +
									wzp1*(
										wym1*(wxm1*in200 + wx00*in201 + wxp1*in202 + wxp2*in203) +
										wy00*(wxm1*in210 + wx00*in211 + wxp1*in212 + wxp2*in213) +
										wyp1*(wxm1*in220 + wx00*in221 + wxp1*in222 + wxp2*in223) +
										wyp2*(wxm1*in230 + wx00*in231 + wxp1*in232 + wxp2*in233)
									) +
									wzp2*(
										wym1*(wxm1*in300 + wx00*in301 + wxp1*in302 + wxp2*in303) +
										wy00*(wxm1*in310 + wx00*in311 + wxp1*in312 + wxp2*in313) +
										wyp1*(wxm1*in320 + wx00*in321 + wxp1*in322 + wxp2*in323) +
										wyp2*(wxm1*in330 + wx00*in331 + wxp1*in332 + wxp2*in333)
									)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_bspline5_z(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around z-axis");
		messenger.log("Applying quintic B-spline prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline5(image,new Axes(true,true,false),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the quintic B-spline kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE5X0;
			if (indims.y == 1) scale /= BSPLINE5X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0, cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdysinaz = xc + dy*sinaz;
						final double ycdycosaz = yc + dy*cosaz;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdysinaz + dx*cosaz;
							final double ty = ycdycosaz - dx*sinaz;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xdiff2 = xdiff*xdiff;
								final double xmdiff = 1 - xdiff;
								final double xmdiff2 = xmdiff*xmdiff;
								final double wxm2 = f1o120*xmdiff2*xmdiff2*xmdiff;
								final double wxm1 = f1o120 + f1o24*xmdiff*(1 + xmdiff*(2 + xmdiff*(2 + xmdiff - xmdiff2)));
								final double wx00 = f11o20 + xdiff2*((f1o4 - f1o12*xdiff)*xdiff2 - f1o2);
								final double wxp1 = f11o20 + xmdiff2*((f1o4 - f1o12*xmdiff)*xmdiff2 - f1o2);
								final double wxp2 = f1o120 + f1o24*xdiff*(1 + xdiff*(2 + xdiff*(2 + xdiff - xdiff2)));
								final double wxp3 = f1o120*xdiff2*xdiff2*xdiff;
								final double ydiff = ty - iy;
								final double ydiff2 = ydiff*ydiff;
								final double ymdiff = 1 - ydiff;
								final double ymdiff2 = ymdiff*ymdiff;
								final double wym2 = f1o120*ymdiff2*ymdiff2*ymdiff;
								final double wym1 = f1o120 + f1o24*ymdiff*(1 + ymdiff*(2 + ymdiff*(2 + ymdiff - ymdiff2)));
								final double wy00 = f11o20 + ydiff2*((f1o4 - f1o12*ydiff)*ydiff2 - f1o2);
								final double wyp1 = f11o20 + ymdiff2*((f1o4 - f1o12*ymdiff)*ymdiff2 - f1o2);
								final double wyp2 = f1o120 + f1o24*ydiff*(1 + ydiff*(2 + ydiff*(2 + ydiff - ydiff2)));
								final double wyp3 = f1o120*ydiff2*ydiff2*ydiff;
								cin.x = borders.x + ix - 2;
								cin.y = borders.y + iy - 2;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.x;
								final double in02 = image.get(cin); ++cin.x;
								final double in03 = image.get(cin); ++cin.x;
								final double in04 = image.get(cin); ++cin.x;
								final double in05 = image.get(cin); ++cin.y;
								final double in15 = image.get(cin); --cin.x;
								final double in14 = image.get(cin); --cin.x;
								final double in13 = image.get(cin); --cin.x;
								final double in12 = image.get(cin); --cin.x;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin); ++cin.y;
								final double in20 = image.get(cin); ++cin.x;
								final double in21 = image.get(cin); ++cin.x;
								final double in22 = image.get(cin); ++cin.x;
								final double in23 = image.get(cin); ++cin.x;
								final double in24 = image.get(cin); ++cin.x;
								final double in25 = image.get(cin); ++cin.y;
								final double in35 = image.get(cin); --cin.x;
								final double in34 = image.get(cin); --cin.x;
								final double in33 = image.get(cin); --cin.x;
								final double in32 = image.get(cin); --cin.x;
								final double in31 = image.get(cin); --cin.x;
								final double in30 = image.get(cin); ++cin.y;
								final double in40 = image.get(cin); ++cin.x;
								final double in41 = image.get(cin); ++cin.x;
								final double in42 = image.get(cin); ++cin.x;
								final double in43 = image.get(cin); ++cin.x;
								final double in44 = image.get(cin); ++cin.x;
								final double in45 = image.get(cin); ++cin.y;
								final double in55 = image.get(cin); --cin.x;
								final double in54 = image.get(cin); --cin.x;
								final double in53 = image.get(cin); --cin.x;
								final double in52 = image.get(cin); --cin.x;
								final double in51 = image.get(cin); --cin.x;
								final double in50 = image.get(cin);
								anew[x] = (
									wym2*(wxm2*in00 + wxm1*in01 + wx00*in02 + wxp1*in03 + wxp2*in04 + wxp3*in05) +
									wym1*(wxm2*in10 + wxm1*in11 + wx00*in12 + wxp1*in13 + wxp2*in14 + wxp3*in15) +
									wy00*(wxm2*in20 + wxm1*in21 + wx00*in22 + wxp1*in23 + wxp2*in24 + wxp3*in25) +
									wyp1*(wxm2*in30 + wxm1*in31 + wx00*in32 + wxp1*in33 + wxp2*in34 + wxp3*in35) +
									wyp2*(wxm2*in40 + wxm1*in41 + wx00*in42 + wxp1*in43 + wxp2*in44 + wxp3*in45) +
									wyp3*(wxm2*in50 + wxm1*in51 + wx00*in52 + wxp1*in53 + wxp2*in54 + wxp3*in55)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_bspline5_y(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around y-axis");
		messenger.log("Applying quintic B-spline prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline5(image,new Axes(true,false,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the quintic B-spline kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE5X0;
			if (indims.z == 1) scale /= BSPLINE5X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzsinay = xc - dz*sinay;
					final double zcdzcosay = zc + dz*cosay;
					for (cnew.y=cin.y=0; cnew.y<newdims.y; ++cnew.y, ++cin.y) {
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzsinay + dx*cosay;
							final double tz = zcdzcosay + dx*sinay;
							final int ix = FMath.floor(tx);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xdiff2 = xdiff*xdiff;
								final double xmdiff = 1 - xdiff;
								final double xmdiff2 = xmdiff*xmdiff;
								final double wxm2 = f1o120*xmdiff2*xmdiff2*xmdiff;
								final double wxm1 = f1o120 + f1o24*xmdiff*(1 + xmdiff*(2 + xmdiff*(2 + xmdiff - xmdiff2)));
								final double wx00 = f11o20 + xdiff2*((f1o4 - f1o12*xdiff)*xdiff2 - f1o2);
								final double wxp1 = f11o20 + xmdiff2*((f1o4 - f1o12*xmdiff)*xmdiff2 - f1o2);
								final double wxp2 = f1o120 + f1o24*xdiff*(1 + xdiff*(2 + xdiff*(2 + xdiff - xdiff2)));
								final double wxp3 = f1o120*xdiff2*xdiff2*xdiff;
								final double zdiff = tz - iz;
								final double zdiff2 = zdiff*zdiff;
								final double zmdiff = 1 - zdiff;
								final double zmdiff2 = zmdiff*zmdiff;
								final double wzm2 = f1o120*zmdiff2*zmdiff2*zmdiff;
								final double wzm1 = f1o120 + f1o24*zmdiff*(1 + zmdiff*(2 + zmdiff*(2 + zmdiff - zmdiff2)));
								final double wz00 = f11o20 + zdiff2*((f1o4 - f1o12*zdiff)*zdiff2 - f1o2);
								final double wzp1 = f11o20 + zmdiff2*((f1o4 - f1o12*zmdiff)*zmdiff2 - f1o2);
								final double wzp2 = f1o120 + f1o24*zdiff*(1 + zdiff*(2 + zdiff*(2 + zdiff - zdiff2)));
								final double wzp3 = f1o120*zdiff2*zdiff2*zdiff;
								cin.x = borders.x + ix - 2;
								cin.z = borders.z + iz - 2;
								final double in00 = image.get(cin); ++cin.x;
								final double in01 = image.get(cin); ++cin.x;
								final double in02 = image.get(cin); ++cin.x;
								final double in03 = image.get(cin); ++cin.x;
								final double in04 = image.get(cin); ++cin.x;
								final double in05 = image.get(cin); ++cin.z;
								final double in15 = image.get(cin); --cin.x;
								final double in14 = image.get(cin); --cin.x;
								final double in13 = image.get(cin); --cin.x;
								final double in12 = image.get(cin); --cin.x;
								final double in11 = image.get(cin); --cin.x;
								final double in10 = image.get(cin); ++cin.z;
								final double in20 = image.get(cin); ++cin.x;
								final double in21 = image.get(cin); ++cin.x;
								final double in22 = image.get(cin); ++cin.x;
								final double in23 = image.get(cin); ++cin.x;
								final double in24 = image.get(cin); ++cin.x;
								final double in25 = image.get(cin); ++cin.z;
								final double in35 = image.get(cin); --cin.x;
								final double in34 = image.get(cin); --cin.x;
								final double in33 = image.get(cin); --cin.x;
								final double in32 = image.get(cin); --cin.x;
								final double in31 = image.get(cin); --cin.x;
								final double in30 = image.get(cin); ++cin.z;
								final double in40 = image.get(cin); ++cin.x;
								final double in41 = image.get(cin); ++cin.x;
								final double in42 = image.get(cin); ++cin.x;
								final double in43 = image.get(cin); ++cin.x;
								final double in44 = image.get(cin); ++cin.x;
								final double in45 = image.get(cin); ++cin.z;
								final double in55 = image.get(cin); --cin.x;
								final double in54 = image.get(cin); --cin.x;
								final double in53 = image.get(cin); --cin.x;
								final double in52 = image.get(cin); --cin.x;
								final double in51 = image.get(cin); --cin.x;
								final double in50 = image.get(cin);
								anew[x] = (
									wzm2*(wxm2*in00 + wxm1*in01 + wx00*in02 + wxp1*in03 + wxp2*in04 + wxp3*in05) +
									wzm1*(wxm2*in10 + wxm1*in11 + wx00*in12 + wxp1*in13 + wxp2*in14 + wxp3*in15) +
									wz00*(wxm2*in20 + wxm1*in21 + wx00*in22 + wxp1*in23 + wxp2*in24 + wxp3*in25) +
									wzp1*(wxm2*in30 + wxm1*in31 + wx00*in32 + wxp1*in33 + wxp2*in34 + wxp3*in35) +
									wzp2*(wxm2*in40 + wxm1*in41 + wx00*in42 + wxp1*in43 + wxp2*in44 + wxp3*in45) +
									wzp3*(wxm2*in50 + wxm1*in51 + wx00*in52 + wxp1*in53 + wxp2*in54 + wxp3*in55)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_bspline5_x(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating only around x-axis");
		messenger.log("Applying quintic B-spline prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline5(image,new Axes(false,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the quintic B-spline kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.y == 1) scale /= BSPLINE5X0;
			if (indims.z == 1) scale /= BSPLINE5X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double ycdzsinax = yc + dz*sinax;
					final double zcdzcosax = zc + dz*cosax;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double ty = ycdzsinax + dy*cosax;
						final double tz = zcdzcosax - dy*sinax;
						final int iy = FMath.floor(ty);
						final int iz = FMath.floor(tz);
						if (iy < -1 || iy > maxy || iz < -1 || iz > maxz)
							for (int x=0; x<newdims.x; ++x) anew[x] = background;
						else {
							final double ydiff = ty - iy;
							final double ydiff2 = ydiff*ydiff;
							final double ymdiff = 1 - ydiff;
							final double ymdiff2 = ymdiff*ymdiff;
							final double wym2 = f1o120*ymdiff2*ymdiff2*ymdiff;
							final double wym1 = f1o120 + f1o24*ymdiff*(1 + ymdiff*(2 + ymdiff*(2 + ymdiff - ymdiff2)));
							final double wy00 = f11o20 + ydiff2*((f1o4 - f1o12*ydiff)*ydiff2 - f1o2);
							final double wyp1 = f11o20 + ymdiff2*((f1o4 - f1o12*ymdiff)*ymdiff2 - f1o2);
							final double wyp2 = f1o120 + f1o24*ydiff*(1 + ydiff*(2 + ydiff*(2 + ydiff - ydiff2)));
							final double wyp3 = f1o120*ydiff2*ydiff2*ydiff;
							final double zdiff = tz - iz;
							final double zdiff2 = zdiff*zdiff;
							final double zmdiff = 1 - zdiff;
							final double zmdiff2 = zmdiff*zmdiff;
							final double wzm2 = f1o120*zmdiff2*zmdiff2*zmdiff;
							final double wzm1 = f1o120 + f1o24*zmdiff*(1 + zmdiff*(2 + zmdiff*(2 + zmdiff - zmdiff2)));
							final double wz00 = f11o20 + zdiff2*((f1o4 - f1o12*zdiff)*zdiff2 - f1o2);
							final double wzp1 = f11o20 + zmdiff2*((f1o4 - f1o12*zmdiff)*zmdiff2 - f1o2);
							final double wzp2 = f1o120 + f1o24*zdiff*(1 + zdiff*(2 + zdiff*(2 + zdiff - zdiff2)));
							final double wzp3 = f1o120*zdiff2*zdiff2*zdiff;
							cin.y = borders.y + iy - 2;
							cin.z = borders.z + iz - 2;
							cin.x = 0;
							for (int x=0; x<newdims.x; ++x, ++cin.x) {
								final double in00 = image.get(cin); ++cin.y;
								final double in01 = image.get(cin); ++cin.y;
								final double in02 = image.get(cin); ++cin.y;
								final double in03 = image.get(cin); ++cin.y;
								final double in04 = image.get(cin); ++cin.y;
								final double in05 = image.get(cin); ++cin.z;
								final double in15 = image.get(cin); --cin.y;
								final double in14 = image.get(cin); --cin.y;
								final double in13 = image.get(cin); --cin.y;
								final double in12 = image.get(cin); --cin.y;
								final double in11 = image.get(cin); --cin.y;
								final double in10 = image.get(cin); ++cin.z;
								final double in20 = image.get(cin); ++cin.y;
								final double in21 = image.get(cin); ++cin.y;
								final double in22 = image.get(cin); ++cin.y;
								final double in23 = image.get(cin); ++cin.y;
								final double in24 = image.get(cin); ++cin.y;
								final double in25 = image.get(cin); ++cin.z;
								final double in35 = image.get(cin); --cin.y;
								final double in34 = image.get(cin); --cin.y;
								final double in33 = image.get(cin); --cin.y;
								final double in32 = image.get(cin); --cin.y;
								final double in31 = image.get(cin); --cin.y;
								final double in30 = image.get(cin); ++cin.z;
								final double in40 = image.get(cin); ++cin.y;
								final double in41 = image.get(cin); ++cin.y;
								final double in42 = image.get(cin); ++cin.y;
								final double in43 = image.get(cin); ++cin.y;
								final double in44 = image.get(cin); ++cin.y;
								final double in45 = image.get(cin); ++cin.z;
								final double in55 = image.get(cin); --cin.y;
								final double in54 = image.get(cin); --cin.y;
								final double in53 = image.get(cin); --cin.y;
								final double in52 = image.get(cin); --cin.y;
								final double in51 = image.get(cin); --cin.y;
								final double in50 = image.get(cin); cin.z -= 5;
								anew[x] = (
									wzm2*(wym2*in00 + wym1*in01 + wy00*in02 + wyp1*in03 + wyp2*in04 + wyp3*in05) +
									wzm1*(wym2*in10 + wym1*in11 + wy00*in12 + wyp1*in13 + wyp2*in14 + wyp3*in15) +
									wz00*(wym2*in20 + wym1*in21 + wy00*in22 + wyp1*in23 + wyp2*in24 + wyp3*in25) +
									wzp1*(wym2*in30 + wym1*in31 + wy00*in32 + wyp1*in33 + wyp2*in34 + wyp3*in35) +
									wzp2*(wym2*in40 + wym1*in41 + wy00*in42 + wyp1*in43 + wyp2*in44 + wyp3*in45) +
									wzp3*(wym2*in50 + wym1*in51 + wy00*in52 + wyp1*in53 + wyp2*in54 + wyp3*in55)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void rotate_bspline5_zyx(final Image image, final Image rotated) {
		
		// Initialization:
		messenger.log("Rotating around z-, y-, and x-axes");
		messenger.log("Applying quintic B-spline prefilter and sampling function");
		messenger.status("Rotating"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline5(image,new Axes (true,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals one, the prefiltering
			// operation will not have been carried out in that dimension.
			// Subsequent application of the quintic B-spline kernel in that
			// dimension will result in an overall down-scaling of the
			// grey-values, which should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE5X0;
			if (indims.y == 1) scale /= BSPLINE5X0;
			if (indims.z == 1) scale /= BSPLINE5X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
			
		} else image.mirror(borders);
		
		// Rotate using the inverse of the rotation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[][][] ain = new double[6][6][6];
		final double[] anew = new double[newdims.x];
		image.axes(Axes.X+Axes.Y+Axes.Z);
		rotated.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - newzoffsetzc;
					final double xcdzinvxz = xc + dz*invxz;
					final double ycdzinvyz = yc + dz*invyz;
					final double zcdzinvzz = zc + dz*invzz;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - newyoffsetyc;
						final double xcdzinvxzdyinvxy = xcdzinvxz + dy*invxy;
						final double ycdzinvyzdyinvyy = ycdzinvyz + dy*invyy;
						final double zcdzinvzzdyinvzy = zcdzinvzz + dy*invzy;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - newxoffsetxc;
							final double tx = xcdzinvxzdyinvxy + dx*invxx;
							final double ty = ycdzinvyzdyinvyy + dx*invyx;
							final double tz = zcdzinvzzdyinvzy + dx*invzx;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (ix < -1 || ix > maxx || iy < -1 || iy > maxy || iz < -1 || iz > maxz)
								anew[x] = background;
							else {
								final double xdiff = tx - ix;
								final double xdiff2 = xdiff*xdiff;
								final double xmdiff = 1 - xdiff;
								final double xmdiff2 = xmdiff*xmdiff;
								final double wxm2 = f1o120*xmdiff2*xmdiff2*xmdiff;
								final double wxm1 = f1o120 + f1o24*xmdiff*(1 + xmdiff*(2 + xmdiff*(2 + xmdiff - xmdiff2)));
								final double wx00 = f11o20 + xdiff2*((f1o4 - f1o12*xdiff)*xdiff2 - f1o2);
								final double wxp1 = f11o20 + xmdiff2*((f1o4 - f1o12*xmdiff)*xmdiff2 - f1o2);
								final double wxp2 = f1o120 + f1o24*xdiff*(1 + xdiff*(2 + xdiff*(2 + xdiff - xdiff2)));
								final double wxp3 = f1o120*xdiff2*xdiff2*xdiff;
								final double ydiff = ty - iy;
								final double ydiff2 = ydiff*ydiff;
								final double ymdiff = 1 - ydiff;
								final double ymdiff2 = ymdiff*ymdiff;
								final double wym2 = f1o120*ymdiff2*ymdiff2*ymdiff;
								final double wym1 = f1o120 + f1o24*ymdiff*(1 + ymdiff*(2 + ymdiff*(2 + ymdiff - ymdiff2)));
								final double wy00 = f11o20 + ydiff2*((f1o4 - f1o12*ydiff)*ydiff2 - f1o2);
								final double wyp1 = f11o20 + ymdiff2*((f1o4 - f1o12*ymdiff)*ymdiff2 - f1o2);
								final double wyp2 = f1o120 + f1o24*ydiff*(1 + ydiff*(2 + ydiff*(2 + ydiff - ydiff2)));
								final double wyp3 = f1o120*ydiff2*ydiff2*ydiff;
								final double zdiff = tz - iz;
								final double zdiff2 = zdiff*zdiff;
								final double zmdiff = 1 - zdiff;
								final double zmdiff2 = zmdiff*zmdiff;
								final double wzm2 = f1o120*zmdiff2*zmdiff2*zmdiff;
								final double wzm1 = f1o120 + f1o24*zmdiff*(1 + zmdiff*(2 + zmdiff*(2 + zmdiff - zmdiff2)));
								final double wz00 = f11o20 + zdiff2*((f1o4 - f1o12*zdiff)*zdiff2 - f1o2);
								final double wzp1 = f11o20 + zmdiff2*((f1o4 - f1o12*zmdiff)*zmdiff2 - f1o2);
								final double wzp2 = f1o120 + f1o24*zdiff*(1 + zdiff*(2 + zdiff*(2 + zdiff - zdiff2)));
								final double wzp3 = f1o120*zdiff2*zdiff2*zdiff;
								cin.x = borders.x + ix - 2;
								cin.y = borders.y + iy - 2;
								cin.z = borders.z + iz - 2;
								image.get(cin,ain);
								anew[x] = (
									wzm2*(
										wym2*(wxm2*ain[0][0][0] + wxm1*ain[0][0][1] + wx00*ain[0][0][2] + wxp1*ain[0][0][3] + wxp2*ain[0][0][4] + wxp3*ain[0][0][5]) +
										wym1*(wxm2*ain[0][1][0] + wxm1*ain[0][1][1] + wx00*ain[0][1][2] + wxp1*ain[0][1][3] + wxp2*ain[0][1][4] + wxp3*ain[0][1][5]) +
										wy00*(wxm2*ain[0][2][0] + wxm1*ain[0][2][1] + wx00*ain[0][2][2] + wxp1*ain[0][2][3] + wxp2*ain[0][2][4] + wxp3*ain[0][2][5]) +
										wyp1*(wxm2*ain[0][3][0] + wxm1*ain[0][3][1] + wx00*ain[0][3][2] + wxp1*ain[0][3][3] + wxp2*ain[0][3][4] + wxp3*ain[0][3][5]) +
										wyp2*(wxm2*ain[0][4][0] + wxm1*ain[0][4][1] + wx00*ain[0][4][2] + wxp1*ain[0][4][3] + wxp2*ain[0][4][4] + wxp3*ain[0][4][5]) +
										wyp3*(wxm2*ain[0][5][0] + wxm1*ain[0][5][1] + wx00*ain[0][5][2] + wxp1*ain[0][5][3] + wxp2*ain[0][5][4] + wxp3*ain[0][5][5])
									) +
									wzm1*(
										wym2*(wxm2*ain[1][0][0] + wxm1*ain[1][0][1] + wx00*ain[1][0][2] + wxp1*ain[1][0][3] + wxp2*ain[1][0][4] + wxp3*ain[1][0][5]) +
										wym1*(wxm2*ain[1][1][0] + wxm1*ain[1][1][1] + wx00*ain[1][1][2] + wxp1*ain[1][1][3] + wxp2*ain[1][1][4] + wxp3*ain[1][1][5]) +
										wy00*(wxm2*ain[1][2][0] + wxm1*ain[1][2][1] + wx00*ain[1][2][2] + wxp1*ain[1][2][3] + wxp2*ain[1][2][4] + wxp3*ain[1][2][5]) +
										wyp1*(wxm2*ain[1][3][0] + wxm1*ain[1][3][1] + wx00*ain[1][3][2] + wxp1*ain[1][3][3] + wxp2*ain[1][3][4] + wxp3*ain[1][3][5]) +
										wyp2*(wxm2*ain[1][4][0] + wxm1*ain[1][4][1] + wx00*ain[1][4][2] + wxp1*ain[1][4][3] + wxp2*ain[1][4][4] + wxp3*ain[1][4][5]) +
										wyp3*(wxm2*ain[1][5][0] + wxm1*ain[1][5][1] + wx00*ain[1][5][2] + wxp1*ain[1][5][3] + wxp2*ain[1][5][4] + wxp3*ain[1][5][5])
									) +
									wz00*(
										wym2*(wxm2*ain[2][0][0] + wxm1*ain[2][0][1] + wx00*ain[2][0][2] + wxp1*ain[2][0][3] + wxp2*ain[2][0][4] + wxp3*ain[2][0][5]) +
										wym1*(wxm2*ain[2][1][0] + wxm1*ain[2][1][1] + wx00*ain[2][1][2] + wxp1*ain[2][1][3] + wxp2*ain[2][1][4] + wxp3*ain[2][1][5]) +
										wy00*(wxm2*ain[2][2][0] + wxm1*ain[2][2][1] + wx00*ain[2][2][2] + wxp1*ain[2][2][3] + wxp2*ain[2][2][4] + wxp3*ain[2][2][5]) +
										wyp1*(wxm2*ain[2][3][0] + wxm1*ain[2][3][1] + wx00*ain[2][3][2] + wxp1*ain[2][3][3] + wxp2*ain[2][3][4] + wxp3*ain[2][3][5]) +
										wyp2*(wxm2*ain[2][4][0] + wxm1*ain[2][4][1] + wx00*ain[2][4][2] + wxp1*ain[2][4][3] + wxp2*ain[2][4][4] + wxp3*ain[2][4][5]) +
										wyp3*(wxm2*ain[2][5][0] + wxm1*ain[2][5][1] + wx00*ain[2][5][2] + wxp1*ain[2][5][3] + wxp2*ain[2][5][4] + wxp3*ain[2][5][5])) +
									wzp1*(
										wym2*(wxm2*ain[3][0][0] + wxm1*ain[3][0][1] + wx00*ain[3][0][2] + wxp1*ain[3][0][3] + wxp2*ain[3][0][4] + wxp3*ain[3][0][5]) +
										wym1*(wxm2*ain[3][1][0] + wxm1*ain[3][1][1] + wx00*ain[3][1][2] + wxp1*ain[3][1][3] + wxp2*ain[3][1][4] + wxp3*ain[3][1][5]) +
										wy00*(wxm2*ain[3][2][0] + wxm1*ain[3][2][1] + wx00*ain[3][2][2] + wxp1*ain[3][2][3] + wxp2*ain[3][2][4] + wxp3*ain[3][2][5]) +
										wyp1*(wxm2*ain[3][3][0] + wxm1*ain[3][3][1] + wx00*ain[3][3][2] + wxp1*ain[3][3][3] + wxp2*ain[3][3][4] + wxp3*ain[3][3][5]) +
										wyp2*(wxm2*ain[3][4][0] + wxm1*ain[3][4][1] + wx00*ain[3][4][2] + wxp1*ain[3][4][3] + wxp2*ain[3][4][4] + wxp3*ain[3][4][5]) +
										wyp3*(wxm2*ain[3][5][0] + wxm1*ain[3][5][1] + wx00*ain[3][5][2] + wxp1*ain[3][5][3] + wxp2*ain[3][5][4] + wxp3*ain[3][5][5])
									) +
									wzp2*(
										wym2*(wxm2*ain[4][0][0] + wxm1*ain[4][0][1] + wx00*ain[4][0][2] + wxp1*ain[4][0][3] + wxp2*ain[4][0][4] + wxp3*ain[4][0][5]) +
										wym1*(wxm2*ain[4][1][0] + wxm1*ain[4][1][1] + wx00*ain[4][1][2] + wxp1*ain[4][1][3] + wxp2*ain[4][1][4] + wxp3*ain[4][1][5]) +
										wy00*(wxm2*ain[4][2][0] + wxm1*ain[4][2][1] + wx00*ain[4][2][2] + wxp1*ain[4][2][3] + wxp2*ain[4][2][4] + wxp3*ain[4][2][5]) +
										wyp1*(wxm2*ain[4][3][0] + wxm1*ain[4][3][1] + wx00*ain[4][3][2] + wxp1*ain[4][3][3] + wxp2*ain[4][3][4] + wxp3*ain[4][3][5]) +
										wyp2*(wxm2*ain[4][4][0] + wxm1*ain[4][4][1] + wx00*ain[4][4][2] + wxp1*ain[4][4][3] + wxp2*ain[4][4][4] + wxp3*ain[4][4][5]) +
										wyp3*(wxm2*ain[4][5][0] + wxm1*ain[4][5][1] + wx00*ain[4][5][2] + wxp1*ain[4][5][3] + wxp2*ain[4][5][4] + wxp3*ain[4][5][5])
									) +
									wzp3*(
										wym2*(wxm2*ain[5][0][0] + wxm1*ain[5][0][1] + wx00*ain[5][0][2] + wxp1*ain[5][0][3] + wxp2*ain[5][0][4] + wxp3*ain[5][0][5]) +
										wym1*(wxm2*ain[5][1][0] + wxm1*ain[5][1][1] + wx00*ain[5][1][2] + wxp1*ain[5][1][3] + wxp2*ain[5][1][4] + wxp3*ain[5][1][5]) +
										wy00*(wxm2*ain[5][2][0] + wxm1*ain[5][2][1] + wx00*ain[5][2][2] + wxp1*ain[5][2][3] + wxp2*ain[5][2][4] + wxp3*ain[5][2][5]) +
										wyp1*(wxm2*ain[5][3][0] + wxm1*ain[5][3][1] + wx00*ain[5][3][2] + wxp1*ain[5][3][3] + wxp2*ain[5][3][4] + wxp3*ain[5][3][5]) +
										wyp2*(wxm2*ain[5][4][0] + wxm1*ain[5][4][1] + wx00*ain[5][4][2] + wxp1*ain[5][4][3] + wxp2*ain[5][4][4] + wxp3*ain[5][4][5]) +
										wyp3*(wxm2*ain[5][5][0] + wxm1*ain[5][5][1] + wx00*ain[5][5][2] + wxp1*ain[5][5][3] + wxp2*ain[5][5][4] + wxp3*ain[5][5][5])
									)
								);
							}
						}
						rotated.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private String schemes(final int scheme) {
		
		switch (scheme) {
			case NEAREST: return "nearest-neighbor interpolation";
			case LINEAR: return "linear interpolation";
			case CUBIC: return "cubic convolution interpolation";
			case BSPLINE3: return "cubic B-spline interpolation";
			case OMOMS3: return "cubic O-MOMS interpolation";
			case BSPLINE5: return "quintic B-spline interpolation";
		}
		
		return "";
	}
	
	/** The value used for background filling. The default value is {@code 0}. */
	public double background = 0;
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
	private final Prefilter prefilter = new Prefilter();
	
	private Dimensions indims, newdims;
	
	private int maxx, maxy, maxz;
	private int scheme;
	
	private double xc, yc, zc;
	private double zangle, yangle, xangle;
	private double cosax, sinax, cosay, sinay, cosaz, sinaz;
	private double invxx, invxy, invxz, invyx, invyy, invyz, invzx, invzy, invzz;
	private double newxoffset, newyoffset, newzoffset;
	private double newxoffsetxc, newyoffsetyc, newzoffsetzc;
	
	private Borders borders;
	
	private boolean antialias;
	
	private String component = "";
	
	private final double f1o2 = 1.0/2.0;
	private final double f1o4 = 1.0/4.0;
	private final double f1o6 = 1.0/6.0;
	private final double f1o12 = 1.0/12.0;
	private final double f1o14 = 1.0/14.0;
	private final double f1o24 = 1.0/24.0;
	private final double f1o42 = 1.0/42.0;
	private final double f1o120 = 1.0/120.0;
	private final double f2o3 = 2.0/3.0;
	private final double f3o2 = 3.0/2.0;
	private final double f5o2 = 5.0/2.0;
	private final double f11o20 = 11.0/20.0;
	private final double f13o21 = 13.0/21.0;
	private final double fm1o2 = -1.0/2.0;
	
	private final double BSPLINE3X0 = 0.666666666667;
	private final double BSPLINE5X0 = 0.55;
	private final double OMOMS3X0 = 0.619047619048;
	
}
