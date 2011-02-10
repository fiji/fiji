package imagescience.transform;

import imagescience.image.Axes;
import imagescience.image.Borders;
import imagescience.image.ColorImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.shape.Point;
import imagescience.utility.FMath;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Affine transforms images using different interpolation schemes.
	
	<dt><b>References:</b></dt>
	
	<dd><table border="0" cellspacing="0" cellpadding="0">
	
	<tr><td valign="top">[1]</td><td width="10"></td><td>R. G. Keys, "Cubic Convolution Interpolation for Digital Image Processing", <em>IEEE Transactions on Acoustics, Speech, and Signal Processing</em>, vol. 29, no. 6, 1981, pp. 1153-1160.</td></tr>
	
	<tr><td valign="top">[2]</td><td width="10"></td><td>M. Unser, "Splines: A Perfect Fit for Signal and Image Processing", <em>IEEE Signal Processing Magazine</em>, vol. 16, no. 6, 1999, pp. 22-38.</td></tr>
	
	<tr><td valign="top">[3]</td><td width="10"></td><td>P. Thevenaz, T. Blu, M. Unser, "Interpolation Revisited", <em>IEEE Transactions on Medical Imaging</em>, vol. 19, no. 7, 2000, pp.739-758.</td></tr>
	
	<tr><td valign="top">[4]</td><td width="10"></td><td>E. Meijering, W. Niessen, M. Viergever, "Quantitative Evaluation of Convolution-Based Methods for Medical
	Image Interpolation", <em>Medical Image Analysis</em>, vol. 5, no. 2, 2001, pp. 111-126.</td></tr>
	
	<tr><td valign="top">[5]</td><td width="10"></td><td>T. Blu, P. Thevenaz, M. Unser, "MOMS: Maximal-Order Interpolation of Minimal Support", <em>IEEE Transactions on Image Processing</em>, vol. 10, no. 7, 2001, pp. 1069-1080.</td></tr>
	
	</table></dd>
*/
public class Affine {
	
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
	public Affine() { }
	
	/** Affine transforms an image.
		
		@param image the input image to be affine transformed. For images of type {@link ColorImage}, the color components are processed separately by the method.
		
		@param transform the affine transformation to be applied. The transformation is applied to every x-y-z subimage in a 5D image. The origin of the right-handed coordinate system in which the transformation is carried out is taken in the center of each subimage.
		
		@param scheme the interpolation scheme to be used. Must be equal to one of the static fields of this class.
		
		@param fit if {@code true}, the size of the output image is adjusted to fit the entire affine transformed image; if {@code false}, the size of the output image will be equal to that of the input image.
		
		@param antialias if {@code true}, the method attempts to reduce the "stair-casing" effect at the transitions from image to background.
		
		@return a new image containing an affine transformed version of the input image. The returned image is of the same type as the input image.
		
		@exception IllegalArgumentException if {@code transform} is not invertible or if the requested interpolation {@code scheme} is not supported.
		
		@exception NullPointerException if {@code image} or {@code transform} is {@code null}.
		
		@exception UnknownError if for any reason the output image could not be created. In most cases this will be due to insufficient free memory.
	*/
	public synchronized Image run(final Image image, final Transform transform, final int scheme, final boolean fit, final boolean antialias) {
		
		messenger.log(ImageScience.prelude()+"Affine");
		
		// Initialize:
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Check and initialize parameters:
		checkup(image.dimensions(),transform,scheme,fit,antialias);
		
		// Affine transform:
		messenger.log("Affine transforming "+image.type());
		Image affined = null;
		if (image instanceof ColorImage) {
			messenger.log("Processing RGB-color components separately");
			final ColorImage cimage = (ColorImage)image;
			
			progressor.range(0,0.33);
			component = " red component";
			messenger.log("Affine transforming"+component);
			cimage.component(ColorImage.RED);
			Image comp = cimage.get(); comp = affine(comp);
			final ColorImage caffined = new ColorImage(comp.dimensions());
			caffined.component(ColorImage.RED);
			caffined.set(comp);
			
			progressor.range(0.33,0.67);
			component = " green component";
			messenger.log("Affine transforming"+component);
			cimage.component(ColorImage.GREEN);
			comp = cimage.get(); comp = affine(comp);
			caffined.component(ColorImage.GREEN);
			caffined.set(comp);
			
			progressor.range(0.67,1);
			component = " blue component";
			messenger.log("Affine transforming"+component);
			cimage.component(ColorImage.BLUE);
			comp = cimage.get(); comp = affine(comp);
			caffined.component(ColorImage.BLUE);
			caffined.set(comp);
			
			affined = caffined;
			
		} else {
			component = "";
			progressor.range(0,1);
			affined = affine(image);
		}
		
		// Finish up:
		affined.name(image.name()+" affined");
		affined.aspects(image.aspects().duplicate());
		
		messenger.status("");
		
		timer.stop();
		
		return affined;
	}
	
	private Image affine(final Image image) {
		
		// Duplicate input image if matrix is identity:
		if (fwd.identity()) {
			messenger.log("The matrix is identity");
			messenger.log("Returning a copy of the input image");
			return image.duplicate();
		}
		
		// Affine transform using specified interpolation scheme:
		messenger.log("Allocating memory output image");
		final Image affined = Image.create(newdims,image.type());
		switch (scheme) {
			case NEAREST: {
				if (xytrans) affine_nearest_xy(image,affined);
				else affine_nearest_xyz(image,affined);
				break;
			}
			case LINEAR: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = image.border(borders);
				if (xytrans) affine_linear_xy(bordered,affined);
				else affine_linear_xyz(bordered,affined);
				break;
			}
			case CUBIC: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = image.border(borders);
				if (xytrans) affine_cubic_xy(bordered,affined);
				else affine_cubic_xyz(bordered,affined);
				break;
			}
			case BSPLINE3: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = new FloatImage(image,borders);
				if (xytrans) affine_bspline3_xy(bordered,affined);
				else affine_bspline3_xyz(bordered,affined);
				break;
			}
			case OMOMS3: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = new FloatImage(image,borders);
				if (xytrans) affine_omoms3_xy(bordered,affined);
				else affine_omoms3_xyz(bordered,affined);
				break;
			}
			case BSPLINE5: {
				messenger.log("Creating bordered copy of input");
				final Image bordered = new FloatImage(image,borders);
				if (xytrans) affine_bspline5_xy(bordered,affined);
				else affine_bspline5_xyz(bordered,affined);
				break;
			}
		}
		
		return affined;
	}
	
	private void checkup(final Dimensions indims, final Transform transform, final int scheme, final boolean fit, final boolean antialias) {
		
		messenger.log("Checking parameters");
		
		// Compute center of image:
		this.indims = indims;
		pmax = new Point(indims.x-1, indims.y-1, indims.z-1);
		pc = new Point(pmax.x/2, pmax.y/2, pmax.z/2);
		
		// Copy and check transform:
		fwd = transform.duplicate();
		
		if (fit) {
			messenger.log("Adjusting output image dimensions to fit result");
			messenger.log("Therefore ignoring translation component");
			fwd.axt = fwd.ayt = fwd.azt = 0;
		}
		
		messenger.log("Resultant forward affine transformation matrix:");
		messenger.log(fwd.string());
		
		final double det = fwd.determinant();
		if (det == 0) throw new IllegalArgumentException("Non-invertible transformation matrix");
		bwd = fwd.duplicate(); bwd.invert();
		
		messenger.log("Resultant backward affine transformation matrix:");
		messenger.log(bwd.string());
		
		// Compute the size of the affine transformed image:
		final Point pnewmin = new Point(-0.5, -0.5, -0.5);
		final Point pnewmax = new Point(pmax.x+0.5, pmax.y+0.5, pmax.z+0.5);
		
		if (fit) {
			// Compute the new positions of the eight corner points:
			final Point p1 = new Point(-pc.x-0.5, -pc.y-0.5, -pc.z-0.5);
			final Point p2 = new Point(pmax.x-pc.x+0.5, -pc.y-0.5, -pc.z-0.5);
			final Point p3 = new Point(-pc.x-0.5, pmax.y-pc.y+0.5, -pc.z-0.5);
			final Point p4 = new Point(pmax.x-pc.x+0.5, pmax.y-pc.y+0.5, -pc.z-0.5);
			final Point p5 = new Point(-pc.x-0.5, -pc.y-0.5, pmax.z-pc.z+0.5);
			final Point p6 = new Point(pmax.x-pc.x+0.5, -pc.y-0.5, pmax.z-pc.z+0.5);
			final Point p7 = new Point(-pc.x-0.5, pmax.y-pc.y+0.5, pmax.z-pc.z+0.5);
			final Point p8 = new Point(pmax.x-pc.x+0.5, pmax.y-pc.y+0.5, pmax.z-pc.z+0.5);
			
			// Use the forward affine transformation matrix:
			fwd.transform(p1); fwd.transform(p2); fwd.transform(p3); fwd.transform(p4);
			fwd.transform(p5); fwd.transform(p6); fwd.transform(p7); fwd.transform(p8);
			
			pnewmin.x = FMath.min(p1.x,p2.x,p3.x,p4.x,p5.x,p6.x,p7.x,p8.x);
			pnewmin.y = FMath.min(p1.y,p2.y,p3.y,p4.y,p5.y,p6.y,p7.y,p8.y);
			pnewmin.z = FMath.min(p1.z,p2.z,p3.z,p4.z,p5.z,p6.z,p7.z,p8.z);
			
			pnewmax.x = FMath.max(p1.x,p2.x,p3.x,p4.x,p5.x,p6.x,p7.x,p8.x);
			pnewmax.y = FMath.max(p1.y,p2.y,p3.y,p4.y,p5.y,p6.y,p7.y,p8.y);
			pnewmax.z = FMath.max(p1.z,p2.z,p3.z,p4.z,p5.z,p6.z,p7.z,p8.z);
			
		} else {
			messenger.log("Not adjusting image dimensions");
		}
		
		int newdimsx = FMath.round(pnewmax.x - pnewmin.x); if (newdimsx <= 0) newdimsx = 1;
		int newdimsy = FMath.round(pnewmax.y - pnewmin.y); if (newdimsy <= 0) newdimsy = 1;
		int newdimsz = FMath.round(pnewmax.z - pnewmin.z); if (newdimsz <= 0) newdimsz = 1;
		
		newdims = new Dimensions(newdimsx,newdimsy,newdimsz,indims.t,indims.c);
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+indims.x+","+indims.y+","+indims.z+","+indims.t+","+indims.c+")");
		messenger.log("Output image dimensions: (x,y,z,t,c) = ("+newdims.x+","+newdims.y+","+newdims.z+","+newdims.t+","+newdims.c+")");
		
		// Determine the offset of the center of the affine transformed image
		// with respect to the input image:
		pcoff = new Point(
			(newdims.x - indims.x)/2.0 + pc.x,
			(newdims.y - indims.y)/2.0 + pc.y,
			(newdims.z - indims.z)/2.0 + pc.z
		);
		
		// Check if requested type of interpolation is applicable:
		messenger.log("Requested interpolation scheme: "+schemes(scheme));
		if (scheme < 0 || scheme > 5) throw new IllegalArgumentException("Non-supported interpolation scheme");
		this.scheme = scheme;
		
		// Show background filling value:
		messenger.log("Background filling with value "+background);
		
		// Set border sizes based on interpolation type and matrix content:
		int bsize = 0;
		switch (scheme) {
			case NEAREST: bsize = 0; break;
			case LINEAR: bsize = 1; break;
			case CUBIC: bsize = 2; break;
			case BSPLINE3: bsize = 2; break;
			case OMOMS3: bsize = 2; break;
			case BSPLINE5: bsize = 3; break;
		}
		
		// If the third row in the forward matrix is equal to that of the
		// identity matrix, the transformation is limited to the x-y-plane:
		if (fwd.azx == 0 && fwd.azy == 0 && fwd.azz == 1 && fwd.azt == 0) {
			xytrans = true;
			borders = new Borders(bsize,bsize,0);
		} else {
			xytrans = false;
			borders = new Borders(bsize,bsize,bsize);
		}
		
		// Store anti-alias choice:
		this.antialias = antialias;
		if (antialias) messenger.log("Anti-aliasing image-background transitions");
	}
	
	private void affine_nearest_xy(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming only in x-y");
		messenger.log("Using nearest-neighbor sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		
		// Affine transform using the backward transformation matrix: (note that
		// when this method is called, the border size of image is 0)
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							cin.x = FMath.round(pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt);
							cin.y = FMath.round(pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt);
							if (cin.x < 0 || cin.x > pmax.x || cin.y < 0 || cin.y > pmax.y) anew[x] = background;
							else anew[x] = image.get(cin);
						}
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_nearest_xyz(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming in x-y-z");
		messenger.log("Using nearest-neighbor sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		
		// Affine transform using the backward transformation matrix: (note that
		// when this method is called, the border size of image is 0)
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							cin.x = FMath.round(pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt);
							cin.y = FMath.round(pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt);
							cin.z = FMath.round(pc.z + dx*bwd.azx + dy*bwd.azy + dz*bwd.azz + bwd.azt);
							if (cin.x < 0 || cin.x > pmax.x || cin.y < 0 || cin.y > pmax.y || cin.z < 0 || cin.z > pmax.z) anew[x] = background;
							else anew[x] = image.get(cin);
						}
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_linear_xy(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming only in x-y");
		messenger.log("Using linear sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y) anew[x] = background;
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
								anew[x] = ymdiff*xmdiff*in00 + ymdiff*xdiff*in01 + ydiff*xmdiff*in10 + ydiff*xdiff*in11;
							}
						}
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_linear_xyz(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming in x-y-z");
		messenger.log("Using linear sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final double tz = pc.z + dx*bwd.azx + dy*bwd.azy + dz*bwd.azz + bwd.azt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y || tz <= -1 || iz > pmax.z) anew[x] = background;
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
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_cubic_xy(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming only in x-y");
		messenger.log("Using cubic convolution sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y) anew[x] = background;
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
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_cubic_xyz(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming in x-y-z");
		messenger.log("Using cubic convolution sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		if (antialias) image.set(borders,background);
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final double tz = pc.z + dx*bwd.azx + dy*bwd.azy + dz*bwd.azz + bwd.azt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y || tz <= -1 || iz > pmax.z) anew[x] = background;
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
								final double in220 = image.get(cin); ++cin.x;
								final double in210 = image.get(cin); ++cin.y;
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
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_bspline3_xy(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming only in x-y");
		messenger.log("Applying cubic B-spline prefilter and sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline3(image,new Axes(true,true,false),borders);
		
		if (antialias) {
			// If any of the dimensions equals 1, the prefiltering operation
			// will not have been carried out in that dimension. Subsequent
			// application of the cubic B-spline kernel in that dimension will
			// result in an overall down-scaling of the grey-values, which
			// should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE3X0;
			if (indims.y == 1) scale /= BSPLINE3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
		}
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y) anew[x] = background;
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
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_bspline3_xyz(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming in x-y-z");
		messenger.log("Applying cubic B-spline prefilter and sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline3(image,new Axes(true,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals 1, the prefiltering operation
			// will not have been carried out in that dimension.  Subsequent
			// application of the cubic B-spline kernel in that dimension will
			// result in an overall down-scaling of the grey-values, which
			// should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE3X0;
			if (indims.y == 1) scale /= BSPLINE3X0;
			if (indims.z == 1) scale /= BSPLINE3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
		}
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final double tz = pc.z + dx*bwd.azx + dy*bwd.azy + dz*bwd.azz + bwd.azt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y || tz <= -1 || iz > pmax.z) anew[x] = background;
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
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_omoms3_xy(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming only in x-y");
		messenger.log("Applying cubic O-MOMS prefilter and sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.omoms3(image,new Axes(true,true,false),borders);
		
		if (antialias) {
			// If any of the dimensions equals 1, the prefiltering operation
			// will not have been carried out in that dimension. Subsequent
			// application of the cubic O-MOMS kernel in that dimension will
			// result in an overall down-scaling of the grey-values, which
			// should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= OMOMS3X0;
			if (indims.y == 1) scale /= OMOMS3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
		}
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y) anew[x] = background;
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
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_omoms3_xyz(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming in x-y-z");
		messenger.log("Applying cubic O-MOMS prefilter and sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.omoms3(image,new Axes(true,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals 1, the prefiltering operation
			// will not have been carried out in that dimension. Subsequent
			// application of the cubic O-MOMS kernel in that dimension will
			// result in an overall down-scaling of the grey-values, which
			// should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= OMOMS3X0;
			if (indims.y == 1) scale /= OMOMS3X0;
			if (indims.z == 1) scale /= OMOMS3X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
		}
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final double tz = pc.z + dx*bwd.azx + dy*bwd.azy + dz*bwd.azz + bwd.azt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y || tz <= -1 || iz > pmax.z) anew[x] = background;
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
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_bspline5_xy(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming only in x-y");
		messenger.log("Applying quintic B-spline prefilter and sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline5(image,new Axes(true,true,false),borders);
		
		if (antialias) {
			// If any of the dimensions equals 1, the prefiltering operation
			// will not have been carried out in that dimension. Subsequent
			// application of the quintic B-spline kernel in that dimension will
			// result in an overall down-scaling of the grey-values, which
			// should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE5X0;
			if (indims.y == 1) scale /= BSPLINE5X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
		}
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[] anew = new double[newdims.x];
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=cin.z=0; cnew.z<newdims.z; ++cnew.z, ++cin.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y) anew[x] = background;
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
						affined.set(cnew,anew);
						progressor.step();
					}
				}
			}
		}
		progressor.stop();
	}
	
	private void affine_bspline5_xyz(final Image image, final Image affined) {
		
		// Initialization:
		messenger.log("Affine transforming in x-y-z");
		messenger.log("Applying quintic B-spline prefilter and sampling function");
		messenger.status("Affine transforming"+component+"...");
		progressor.steps(newdims.c*newdims.t*newdims.z*newdims.y);
		prefilter.bspline5(image,new Axes(true,true,true),borders);
		
		if (antialias) {
			// If any of the dimensions equals 1, the prefiltering operation
			// will not have been carried out in that dimension. Subsequent
			// application of the quintic B-spline kernel in that dimension will
			// result in an overall down-scaling of the grey-values, which
			// should be corrected for:
			double scale = 1;
			if (indims.x == 1) scale /= BSPLINE5X0;
			if (indims.y == 1) scale /= BSPLINE5X0;
			if (indims.z == 1) scale /= BSPLINE5X0;
			if (scale != 1) {
				messenger.log("Correction scaling with factor "+scale);
				image.multiply(scale);
			}
			image.set(borders,background);
		}
		else image.mirror(borders);
		
		// Affine transform using the backward transformation matrix:
		final Coordinates cin = new Coordinates();
		final Coordinates cnew = new Coordinates();
		final double[][][] ain = new double[6][6][6];
		final double[] anew = new double[newdims.x];
		image.axes(Axes.X+Axes.Y+Axes.Z);
		affined.axes(Axes.X);
		
		progressor.start();
		for (cnew.c=cin.c=0; cnew.c<newdims.c; ++cnew.c, ++cin.c) {
			for (cnew.t=cin.t=0; cnew.t<newdims.t; ++cnew.t, ++cin.t) {
				for (cnew.z=0; cnew.z<newdims.z; ++cnew.z) {
					final double dz = cnew.z - pcoff.z;
					for (cnew.y=0; cnew.y<newdims.y; ++cnew.y) {
						final double dy = cnew.y - pcoff.y;
						for (int x=0; x<newdims.x; ++x) {
							final double dx = x - pcoff.x;
							final double tx = pc.x + dx*bwd.axx + dy*bwd.axy + dz*bwd.axz + bwd.axt;
							final double ty = pc.y + dx*bwd.ayx + dy*bwd.ayy + dz*bwd.ayz + bwd.ayt;
							final double tz = pc.z + dx*bwd.azx + dy*bwd.azy + dz*bwd.azz + bwd.azt;
							final int ix = FMath.floor(tx);
							final int iy = FMath.floor(ty);
							final int iz = FMath.floor(tz);
							if (tx <= -1 || ix > pmax.x || ty <= -1 || iy > pmax.y || tz <= -1 || iz > pmax.z) anew[x] = background;
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
										wyp3*(wxm2*ain[2][5][0] + wxm1*ain[2][5][1] + wx00*ain[2][5][2] + wxp1*ain[2][5][3] + wxp2*ain[2][5][4] + wxp3*ain[2][5][5])
									) +
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
						affined.set(cnew,anew);
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
	
	private String component = "";
	private Dimensions indims, newdims;
	private Borders borders;
	private int scheme;
	private Transform fwd, bwd;
	private Point pc, pmax, pcoff;
	private boolean xytrans, antialias;
	
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
